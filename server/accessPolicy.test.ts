import test, { type TestContext } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { createApp } from '../server.ts';
import { resolveGatewayAccessPolicy, type GatewayAccessPolicy } from './accessPolicy.ts';
import { resolveAiGatewayConfig } from './aiGateway/config.ts';
import { AiGatewayService } from './aiGateway/gatewayService.ts';

const ACCESS_TOKEN = Buffer.from('server-only-test-access-secret-32-bytes').toString('base64url');
const ENABLED_POLICY: GatewayAccessPolicy = {
  clinicalServiceEnabled: true,
  accessToken: ACCESS_TOKEN,
};
const AI_ROUTES = ['visit-draft', 'nutrition', 'syrup-scan', 'safety-checklist', 'documented-qa'];

async function startServer(t: TestContext, accessPolicy?: GatewayAccessPolicy) {
  let modelCalls = 0;
  let auditReads = 0;
  const gateway = new AiGatewayService({
    config: resolveAiGatewayConfig({ GEMINI_API_KEY: 'test-key-must-not-appear-in-health' }),
    transport: {
      async generateJson() {
        modelCalls += 1;
        return JSON.stringify({
          titleFa: 'خلاصه آماده ویزیت',
          childAndReasonSummaryFa: 'گزارش والد برای بررسی پزشک.',
          temperatureTrend24hFa: 'اطلاعات دما UNKNOWN است.',
          medicationHistory24hFa: 'اطلاعات دارو UNKNOWN است.',
          observedSymptomsAndFlagsFa: 'علائم گزارش‌شده نیازمند بررسی هستند.',
          questionsForClinicianFa: ['چه اطلاعات بیشتری لازم است؟'],
          missingInformationFlagsFa: ['UNKNOWN'],
        });
      },
    },
  });
  const originalListRecent = gateway.auditTrail.listRecent.bind(gateway.auditTrail);
  gateway.auditTrail.listRecent = (limit?: number) => {
    auditReads += 1;
    return originalListRecent(limit);
  };
  const app = await createApp({ gateway, serveStatic: false, accessPolicy });
  const server = http.createServer(app);
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  t.after(async () => {
    server.closeAllConnections();
    await new Promise<void>((resolve, reject) => server.close((err) => err ? reject(err) : resolve()));
  });
  const address = server.address();
  assert.ok(address && typeof address === 'object');
  return {
    baseUrl: `http://127.0.0.1:${address.port}`,
    counts: () => ({ modelCalls, auditReads }),
  };
}

function post(baseUrl: string, route: string, authorization?: string, body = JSON.stringify({ promptText: 'گزارش والد برای ویزیت کودک' })) {
  return fetch(`${baseUrl}/api/gemini/${route}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(authorization ? { Authorization: authorization } : {}),
    },
    body,
  });
}

test('access policy defaults off and rejects missing, short or malformed deployment secrets', () => {
  assert.deepEqual(resolveGatewayAccessPolicy({}), { clinicalServiceEnabled: false, accessToken: null });
  assert.equal(resolveGatewayAccessPolicy({ MANO_CLINICAL_SERVICE_ENABLED: 'TRUE' }).clinicalServiceEnabled, false);
  for (const token of ['', 'short', 'a'.repeat(42), `${ACCESS_TOKEN} `, 'a'.repeat(257)]) {
    assert.equal(resolveGatewayAccessPolicy({ MANO_GATEWAY_ACCESS_TOKEN: token }).accessToken, null);
  }
  assert.deepEqual(resolveGatewayAccessPolicy({
    MANO_CLINICAL_SERVICE_ENABLED: 'true',
    MANO_GATEWAY_ACCESS_TOKEN: ACCESS_TOKEN,
  }), ENABLED_POLICY);
});

test('every AI route stays disabled with a configured Gemini key and valid bearer token', async (t) => {
  const { baseUrl, counts } = await startServer(t, { ...ENABLED_POLICY, clinicalServiceEnabled: false });
  for (const route of [...AI_ROUTES, 'future-endpoint']) {
    const response = await post(baseUrl, route, `Bearer ${ACCESS_TOKEN}`);
    assert.equal(response.status, 503, route);
    assert.equal(response.headers.get('cache-control'), 'no-store');
    assert.equal((await response.json()).code, 'CLINICAL_SERVICE_DISABLED');
  }
  assert.deepEqual(counts(), { modelCalls: 0, auditReads: 0 });
});

test('enabled AI routes fail closed when the deployment secret is unconfigured or invalid', async (t) => {
  for (const accessToken of [null, 'weak-secret']) {
    const { baseUrl, counts } = await startServer(t, { clinicalServiceEnabled: true, accessToken });
    const response = await post(baseUrl, 'visit-draft', `Bearer ${ACCESS_TOKEN}`);
    assert.equal(response.status, 503);
    assert.equal((await response.json()).code, 'GATEWAY_ACCESS_UNCONFIGURED');
    assert.deepEqual(counts(), { modelCalls: 0, auditReads: 0 });
  }
});

test('enabled AI routes require the configured bearer token before parsing or invoking AI', async (t) => {
  const { baseUrl, counts } = await startServer(t, ENABLED_POLICY);
  for (const route of AI_ROUTES) {
    for (const authorization of [undefined, `Bearer ${'x'.repeat(43)}`, `Basic ${ACCESS_TOKEN}`]) {
      const response = await post(baseUrl, route, authorization, '{malformed');
      assert.equal(response.status, 401, route);
      assert.equal(response.headers.get('cache-control'), 'no-store');
      assert.equal(response.headers.get('www-authenticate'), 'Bearer');
      assert.equal((await response.json()).code, 'GATEWAY_AUTH_REQUIRED');
    }
  }
  assert.deepEqual(counts(), { modelCalls: 0, auditReads: 0 });
});

test('authorized AI request reaches the injected model while audit remains private', async (t) => {
  const { baseUrl, counts } = await startServer(t, ENABLED_POLICY);
  const publicAudit = await fetch(`${baseUrl}/api/ai-gateway/audit`);
  assert.equal(publicAudit.status, 401);
  assert.deepEqual(counts(), { modelCalls: 0, auditReads: 0 });

  const response = await post(baseUrl, 'visit-draft', `Bearer ${ACCESS_TOKEN}`);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get('cache-control'), 'no-store');
  assert.equal((await response.json()).meta.outcome, 'ALLOWED');
  assert.equal(counts().modelCalls, 1);

  const authorizedAudit = await fetch(`${baseUrl}/api/ai-gateway/audit`, {
    headers: { Authorization: `Bearer ${ACCESS_TOKEN}` },
  });
  assert.equal(authorizedAudit.status, 200);
  assert.equal(authorizedAudit.headers.get('cache-control'), 'no-store');
  assert.equal((await authorizedAudit.json()).entries.length, 1);
  assert.equal(counts().auditReads, 1);
});

test('audit is protected even while clinical service is disabled and fails closed without a secret', async (t) => {
  for (const accessToken of [ACCESS_TOKEN, null]) {
    const { baseUrl, counts } = await startServer(t, { clinicalServiceEnabled: false, accessToken });
    const response = await fetch(`${baseUrl}/api/ai-gateway/audit`);
    assert.equal(response.status, accessToken ? 401 : 503);
    assert.deepEqual(counts(), { modelCalls: 0, auditReads: 0 });
  }
});

test('public health reflects release and access configuration without exposing internals', async (t) => {
  for (const policy of [
    { ...ENABLED_POLICY, clinicalServiceEnabled: false },
    { ...ENABLED_POLICY, accessToken: null },
    ENABLED_POLICY,
  ]) {
    const { baseUrl, counts } = await startServer(t, policy);
    const response = await fetch(`${baseUrl}/api/health`);
    assert.equal(response.status, 200);
    assert.equal(response.headers.get('cache-control'), 'no-store');
    const body = await response.json();
    assert.deepEqual(body, {
      ok: true,
      clinicalServiceEnabled: policy.clinicalServiceEnabled,
      aiServiceAvailable: policy.clinicalServiceEnabled && policy.accessToken !== null,
      offlineEnginesIndependent: true,
    });
    assert.equal(JSON.stringify(body).includes(ACCESS_TOKEN), false);
    assert.deepEqual(counts(), { modelCalls: 0, auditReads: 0 });
  }
});
