import assert from 'node:assert/strict';
import http from 'node:http';
import { mkdtemp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test, { type TestContext } from 'node:test';
import express from 'express';
import { createApp } from '../server.ts';
import { createWebHostingConfig } from './webHostingConfig.ts';
import {
  closeHttpServer, installWebRuntime, productionAssetCache, productionContentSecurityPolicy,
  resolveFirebaseWebCspOrigins, serveProductionWeb, unknownApiRoute, WEB_SECURITY_HEADERS,
} from './webRuntime.ts';

async function listen(t: TestContext, app: express.Express) {
  const server = http.createServer(app);
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  t.after(async () => {
    server.closeAllConnections();
    await new Promise<void>((resolve) => server.close(() => resolve()));
  });
  const address = server.address();
  assert.ok(address && typeof address === 'object');
  return { server, origin: `http://127.0.0.1:${address.port}` };
}

test('unknown APIs return no-store JSON before either web entrypoint can serve HTML', async (t) => {
  const app = await createApp({ serveStatic: false });
  const { origin } = await listen(t, app);
  for (const method of ['GET', 'POST']) {
    const response = await fetch(`${origin}/api/misspelled-route`, { method, headers: { Accept: 'text/html' } });
    assert.equal(response.status, 404);
    assert.match(response.headers.get('content-type') ?? '', /application\/json/);
    assert.equal(response.headers.get('cache-control'), 'no-store');
    assert.equal((await response.json()).code, 'API_NOT_FOUND');
    assert.equal(response.headers.get('x-powered-by'), null);
    for (const [name, value] of Object.entries(WEB_SECURITY_HEADERS)) assert.equal(response.headers.get(name), value);
  }
});

test('readiness becomes unavailable during draining without changing clinical release state', async (t) => {
  const runtimeState = { shuttingDown: false };
  const app = await createApp({ serveStatic: false, runtimeState });
  const { origin } = await listen(t, app);
  const ready = await fetch(`${origin}/api/ready`);
  assert.equal(ready.status, 200);
  assert.deepEqual(await ready.json(), { ready: true });
  runtimeState.shuttingDown = true;
  const draining = await fetch(`${origin}/api/ready`);
  assert.equal(draining.status, 503);
  assert.deepEqual(await draining.json(), { ready: false });
  assert.equal(draining.headers.get('cache-control'), 'no-store');
});

test('operational failure logs correlate the response without recording clinical data or request identifiers', async (t) => {
  const entries: unknown[] = [];
  const app = express();
  installWebRuntime(app, { shuttingDown: false }, { logFailure: entry => { entries.push(entry); } });
  app.use(express.json());
  app.post('/synthetic-failure', (_req, res) => res.status(503).json({ error: 'unavailable' }));
  app.get('/healthy', (_req, res) => res.json({ ok: true }));
  const { origin } = await listen(t, app);
  const response = await fetch(`${origin}/synthetic-failure?child=SYNTHETIC_PRIVATE_QUERY`, {
    method: 'POST', headers: { 'content-type': 'application/json', 'x-request-id': 'SYNTHETIC_UNTRUSTED_HEADER' },
    body: JSON.stringify({ alias: 'SYNTHETIC_PRIVATE_CHILD', token: 'SYNTHETIC_PRIVATE_TOKEN' }),
  });
  assert.equal(response.status, 503);
  await response.text();
  assert.equal(entries.length, 1);
  assert.deepEqual(Object.keys(entries[0] as object).sort(), ['durationMs', 'event', 'method', 'requestId', 'status']);
  assert.equal((entries[0] as { requestId: string }).requestId, response.headers.get('x-request-id'));
  assert.doesNotMatch(JSON.stringify(entries), /SYNTHETIC_|child|alias|token|127\.0\.0\.1|synthetic-failure/);
  await fetch(`${origin}/healthy`);
  assert.equal(entries.length, 1, 'Ordinary successful use does not create an activity log');
});

test('production static files preserve entrypoints, prevent stale HTML, and never hide missing assets', async (t) => {
  const dist = await mkdtemp(path.join(os.tmpdir(), 'mano-web-runtime-'));
  t.after(() => rm(dist, { recursive: true, force: true }));
  for (const dir of ['preview', 'care', 'pharmacist', 'assets/images', 'fonts']) await mkdir(path.join(dist, dir), { recursive: true });
  const fixtures = {
    'index.html': '<html>React entry</html>',
    'preview/index.html': '<html>Isolated preview</html>',
    'care/index.html': '<html>Care entry</html>',
    'pharmacist/index.html': '<html>Pharmacist entry</html>',
    'assets/app-AbCd1234.js': 'export const built=true;',
    'assets/app-AbCd1234.css': 'body{color:black}',
    'assets/images/mascot.png': 'synthetic image fixture',
    'fonts/local.ttf': 'synthetic font fixture',
    '.env': 'not-public',
  };
  for (const [name, content] of Object.entries(fixtures)) await writeFile(path.join(dist, name), content);
  const app = express();
  installWebRuntime(app, { shuttingDown: false }, { production: true, env: {} });
  app.use('/api', unknownApiRoute);
  serveProductionWeb(app, dist);
  const { origin } = await listen(t, app);
  for (const entry of ['/', '/preview/', '/care/', '/pharmacist/?pharmacy=sample_123']) {
    const response = await fetch(`${origin}${entry}`);
    assert.equal(response.status, 200, entry);
    assert.equal(response.headers.get('cache-control'), 'no-store');
    assert.match(response.headers.get('content-type') ?? '', /text\/html/);
    assert.match(response.headers.get('content-security-policy') ?? '', /frame-ancestors 'none'/);
  }
  const redirect = await fetch(`${origin}/pharmacist?pharmacy=sample_123`, { redirect: 'manual' });
  assert.equal(redirect.status, 301);
  assert.match(redirect.headers.get('location') ?? '', /\/pharmacist\/\?pharmacy=sample_123$/);
  for (const asset of ['assets/app-AbCd1234.js', 'assets/app-AbCd1234.css']) {
    const response = await fetch(`${origin}/${asset}`);
    assert.equal(response.headers.get('cache-control'), 'public, max-age=31536000, immutable');
  }
  for (const asset of ['assets/images/mascot.png', 'fonts/local.ttf']) {
    assert.equal((await fetch(`${origin}/${asset}`)).headers.get('cache-control'), 'no-store');
  }
  for (const missing of ['/assets/missing.js', '/unknown-page', '/.env']) {
    const response = await fetch(`${origin}${missing}`);
    assert.equal(response.status, 404);
    assert.doesNotMatch(await response.text(), /React entry|not-public/);
  }
  assert.doesNotMatch((await fetch(`${origin}/`)).headers.get('content-security-policy') ?? '', /script-src[^;]*'unsafe-inline'/);
  assert.match((await fetch(`${origin}/preview/`)).headers.get('content-security-policy') ?? '', /script-src[^;]*'unsafe-inline'/);
});

test('only content-addressed top-level Vite JS/CSS receives immutable cache policy', () => {
  assert.equal(productionAssetCache('/build/assets/app-aBcd1234.js', '/build'), 'public, max-age=31536000, immutable');
  for (const file of ['/build/index.html', '/build/assets/app.js', '/build/assets/images/photo-aBcd1234.js', '/outside/assets/app-aBcd1234.js']) {
    assert.equal(productionAssetCache(file, '/build'), 'no-store', file);
  }
});

test('CSP extends only configured exact Firebase origins and keeps local preview connections private', () => {
  assert.equal(resolveFirebaseWebCspOrigins({}), undefined);
  const local = productionContentSecurityPolicy('/');
  assert.match(local, /connect-src 'self';/);
  assert.doesNotMatch(local, /googleapis|cloudfunctions/);
  const env = {
    VITE_MANO_ONLINE_ENABLED: 'true', VITE_FIREBASE_PROJECT_ID: 'sample-project',
    VITE_FIREBASE_AUTH_DOMAIN: 'sample-project.firebaseapp.com', VITE_FIREBASE_FUNCTIONS_REGION: 'europe-west1',
  };
  const online = resolveFirebaseWebCspOrigins(env);
  assert.deepEqual(online, { auth: 'https://sample-project.firebaseapp.com', functions: 'https://europe-west1-sample-project.cloudfunctions.net' });
  const policy = productionContentSecurityPolicy('/', online);
  assert.match(policy, /connect-src[^;]*https:\/\/europe-west1-sample-project\.cloudfunctions\.net/);
  assert.doesNotMatch(policy, /https:\/\/\*|connect-src[^;]*(?:https:;|https: )/);
  for (const patch of [
    { VITE_FIREBASE_PROJECT_ID: 'project; connect-src https:' },
    { VITE_FIREBASE_AUTH_DOMAIN: 'safe.example; script-src https:' },
    { VITE_FIREBASE_FUNCTIONS_REGION: 'us-central1.example.test' },
  ]) assert.throws(() => resolveFirebaseWebCspOrigins({ ...env, ...patch }), /Invalid public Firebase/);
});

test('standalone Firebase web config builds the React dist and cannot silently deploy backend/API rewrites', async () => {
  const actual = JSON.parse(await readFile(new URL('../firebase.web.json', import.meta.url), 'utf8'));
  assert.deepEqual(actual, createWebHostingConfig({}));
  assert.equal(actual.hosting.public, 'dist');
  assert.equal('functions' in actual, false);
  assert.equal('rewrites' in actual.hosting, false);
  assert.ok(actual.hosting.ignore.includes('.well-known/**'), 'Do not publish the placeholder Android signature');
  const assetRule = createWebHostingConfig({}).hosting.headers.find(rule => 'regex' in rule);
  assert.ok(assetRule);
  const assetPattern = new RegExp(assetRule.regex);
  assert.ok(assetPattern.test('/assets/app-AbCd1234.js'));
  assert.ok(assetPattern.test('/assets/app-AbCd1234.css'));
  assert.equal(assetPattern.test('/assets/app-AbCd1234Xjs'), false);
  assert.equal(assetPattern.test('/assets/images/app-AbCd1234.js'), false);
  assert.equal(assetPattern.test('/assets/app.js'), false);
});

test('graceful shutdown drains a response before closing the HTTP server', async () => {
  const app = express();
  let received!: () => void;
  const requestReceived = new Promise<void>((resolve) => { received = resolve; });
  app.get('/', (_req, res) => { received(); setTimeout(() => res.send('finished'), 30); });
  const server = http.createServer(app);
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  assert.ok(address && typeof address === 'object');
  const response = fetch(`http://127.0.0.1:${address.port}/`);
  await requestReceived;
  const closing = closeHttpServer(server, 1000);
  assert.equal(await (await response).text(), 'finished');
  assert.deepEqual(await closing, { forced: false });
});

test('graceful shutdown force-closes an unfinished response only at the deadline', async () => {
  let received!: () => void;
  const requestReceived = new Promise<void>((resolve) => { received = resolve; });
  const server = http.createServer((_req, res) => { res.writeHead(200); res.write('unfinished'); received(); });
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  assert.ok(address && typeof address === 'object');
  const request = http.get(`http://127.0.0.1:${address.port}/`, (response) => response.on('error', () => undefined));
  request.on('error', () => undefined);
  await requestReceived;
  const started = Date.now();
  const result = await closeHttpServer(server, 50);
  assert.deepEqual(result, { forced: true });
  assert.ok(Date.now() - started >= 40);
  request.destroy();
});
