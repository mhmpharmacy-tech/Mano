import type { StageTransport } from './independentStageClient.ts';

// Only for the private loopback harness. No gate is embedded in client code.
// A public transport requires a separately reviewed HTTPS identity boundary.
export function privateBrowserTransport(enabled = false): StageTransport {
  return async ({ method, path, bearer, body, signal }) => {
    if (!enabled || typeof window === 'undefined' || window.location.protocol !== 'http:' ||
        window.location.hostname !== '127.0.0.1' || !window.location.port) throw new Error('PRIVATE_HARNESS_ONLY');
    if (!/^\/internal\/mano\/v1\/(?:profiles(?:\/[a-fA-F0-9-]{36}\/shares(?:\/[a-fA-F0-9-]{36})?)?|session\/logout)$/.test(path)) throw new Error('INVALID_PATH');
    const response = await fetch(path, {
      method, signal, credentials: 'same-origin', cache: 'no-store', redirect: 'error',
      headers: { 'x-mano-browser-test': '1', authorization: 'Bearer ' + bearer,
        ...(body ? { 'content-type': 'application/json' } : {}) },
      ...(body ? { body: JSON.stringify(body) } : {}),
    });
    return { status: response.status, data: await response.json() };
  };
}
