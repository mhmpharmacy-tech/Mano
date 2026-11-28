import path from 'node:path';
import { randomUUID } from 'node:crypto';
import type { Server } from 'node:http';
import express, { type Express, type RequestHandler } from 'express';

export interface WebRuntimeState {
  shuttingDown: boolean;
}

export const WEB_SECURITY_HEADERS = {
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'DENY',
  'Referrer-Policy': 'no-referrer',
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=()',
} as const;

export interface FirebaseWebCspOrigins {
  auth: string;
  functions: string;
}

export interface WebRuntimeFailure {
  event: 'http-server-error';
  requestId: string;
  method: string;
  status: number;
  durationMs: number;
}

/** Only operator-provided public Firebase identifiers can extend the policy. */
export function resolveFirebaseWebCspOrigins(
  env: Record<string, string | undefined> = process.env,
): FirebaseWebCspOrigins | undefined {
  if (env.VITE_MANO_ONLINE_ENABLED !== 'true') return undefined;
  const project = env.VITE_FIREBASE_PROJECT_ID ?? '';
  const authDomain = env.VITE_FIREBASE_AUTH_DOMAIN ?? '';
  const region = env.VITE_FIREBASE_FUNCTIONS_REGION || 'us-central1';
  if (!/^[a-z][a-z0-9-]{4,61}[a-z0-9]$/.test(project) ||
      !/^[a-z]+(?:-[a-z0-9]+)+\d$/.test(region) || region.length > 40 ||
      !/^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}$/.test(authDomain)) {
    throw new Error('Invalid public Firebase project, auth domain or functions region for CSP.');
  }
  return {
    auth: `https://${authDomain}`,
    functions: `https://${region}-${project}.cloudfunctions.net`,
  };
}

export function productionContentSecurityPolicy(
  requestPath: string,
  online?: FirebaseWebCspOrigins,
): string {
  const legacyInline = /^\/(?:preview|pharmacist)(?:\/|$)/.test(requestPath);
  const care = /^\/care(?:\/|$)/.test(requestPath);
  const scripts = ["'self'", ...(legacyInline ? ["'unsafe-inline'"] : [])];
  if (care) scripts.push('https://www.gstatic.com/firebasejs/');
  const connects = ["'self'"];
  const frames = ["'none'"];
  if (online) {
    scripts.push('https://www.google.com/recaptcha/', 'https://www.gstatic.com/recaptcha/', 'https://apis.google.com');
    connects.push('https://identitytoolkit.googleapis.com', 'https://securetoken.googleapis.com',
      'https://firebaseappcheck.googleapis.com', 'https://www.google.com/recaptcha/',
      'https://www.gstatic.com/recaptcha/', online.auth, online.functions);
    frames.splice(0, 1, online.auth, 'https://www.google.com/recaptcha/', 'https://recaptcha.google.com/recaptcha/');
  }
  return [
    "default-src 'self'", "base-uri 'none'", "object-src 'none'", "frame-ancestors 'none'",
    "form-action 'self'", `script-src ${scripts.join(' ')}`, "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data: blob:", "font-src 'self'", `connect-src ${connects.join(' ')}`,
    `frame-src ${frames.join(' ')}`, "worker-src 'self' blob:",
  ].join('; ');
}

/** Cache only Vite's content-addressed JS/CSS; images with stable names can change. */
export function productionAssetCache(filePath: string, distDir: string): string {
  const relative = path.relative(distDir, filePath).split(path.sep).join('/');
  return /^assets\/[^/]+-[A-Za-z0-9_-]{8,}\.(?:js|css)$/.test(relative)
    ? 'public, max-age=31536000, immutable'
    : 'no-store';
}

export function installWebRuntime(
  app: Express,
  state: WebRuntimeState,
  options: { production?: boolean; env?: Record<string, string | undefined>; logFailure?: (entry: WebRuntimeFailure) => void } = {},
): void {
  const online = options.production ? resolveFirebaseWebCspOrigins(options.env) : undefined;
  app.disable('x-powered-by');
  app.use((req, res, next) => {
    const requestId = randomUUID();
    const startedAt = performance.now();
    res.setHeader('X-Request-ID', requestId);
    res.once('finish', () => {
      if (res.statusCode < 500) return;
      // No URL, query, IP, account identifier, body or provider response enters
      // operational logs. The fresh request ID supports private troubleshooting.
      const entry: WebRuntimeFailure = {
        event: 'http-server-error', requestId,
        method: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'].includes(req.method) ? req.method : 'OTHER',
        status: res.statusCode, durationMs: Math.max(0, Math.round(performance.now() - startedAt)),
      };
      try {
        (options.logFailure ?? ((failure) => console.error(JSON.stringify(failure))))(entry);
      } catch { /* Logging cannot change or repeat a completed response. */ }
    });
    for (const [name, value] of Object.entries(WEB_SECURITY_HEADERS)) res.setHeader(name, value);
    if (options.production) res.setHeader('Content-Security-Policy', productionContentSecurityPolicy(req.path, online));
    next();
  });
  app.use('/api', (_req, res, next) => {
    res.setHeader('Cache-Control', 'no-store');
    next();
  });
  app.get('/api/ready', (_req, res) => {
    res.status(state.shuttingDown ? 503 : 200).json({ ready: !state.shuttingDown });
  });
}

export const unknownApiRoute: RequestHandler = (_req, res) => {
  res.status(404).json({ code: 'API_NOT_FOUND', error: 'مسیر سرویس یافت نشد.' });
};

/** React uses hash routes: unknown files/routes must not become a 200 HTML response. */
export function serveProductionWeb(app: Express, distDir: string): void {
  app.use(express.static(distDir, {
    dotfiles: 'ignore',
    index: 'index.html',
    setHeaders(res, filePath) {
      res.setHeader('Cache-Control', productionAssetCache(filePath, distDir));
    },
  }));
  app.use((_req, res) => {
    res.setHeader('Cache-Control', 'no-store');
    res.status(404).type('text/plain').send('صفحه یا فایل یافت نشد. برای ورود به مانو، مسیر / را باز کنید.');
  });
}

/** Stop accepting work, drain requests, then force-close only after the deadline. */
export function closeHttpServer(server: Server, deadlineMs = 10_000): Promise<{ forced: boolean }> {
  return new Promise((resolve) => {
    let settled = false;
    const complete = (forced: boolean) => {
      if (settled) return;
      settled = true;
      clearTimeout(deadline);
      clearInterval(idleDrain);
      resolve({ forced });
    };
    const deadline = setTimeout(() => {
      server.closeAllConnections();
      complete(true);
    }, deadlineMs);
    deadline.unref();
    // A response that was active at close() can become an idle keep-alive
    // afterwards. Close only idle sockets while requests are still draining.
    const idleDrain = setInterval(() => server.closeIdleConnections(), 100);
    idleDrain.unref();
    server.close(() => complete(false));
    server.closeIdleConnections();
  });
}
