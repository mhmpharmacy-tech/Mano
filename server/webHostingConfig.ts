import {
  productionContentSecurityPolicy, resolveFirebaseWebCspOrigins, WEB_SECURITY_HEADERS,
} from './webRuntime.ts';

/** Separate from firebase.json so a web preview cannot deploy Android's backend. */
export function createWebHostingConfig(env: Record<string, string | undefined> = {}) {
  const online = resolveFirebaseWebCspOrigins(env);
  const pageHeaders = (requestPath: string) => [
    { key: 'Cache-Control', value: 'no-store' },
    { key: 'Content-Security-Policy', value: productionContentSecurityPolicy(requestPath, online) },
  ];
  return {
    hosting: {
      public: 'dist',
      // The repository contains a placeholder Android fingerprint. Do not publish it.
      ignore: ['firebase.web*.json', '**/node_modules/**', '.well-known/**'],
      headers: [
        { source: '**', headers: Object.entries(WEB_SECURITY_HEADERS).map(([key, value]) => ({ key, value })) },
        ...['/', '/index.html', '/preview', '/preview/**', '/care', '/care/**', '/pharmacist', '/pharmacist/**']
          .map((source) => ({ source, headers: pageHeaders(source.replace('/**', '/')) })),
        { source: '/api{,/**}', headers: [{ key: 'Cache-Control', value: 'no-store' }] },
        { source: '/assets/images/**', headers: [{ key: 'Cache-Control', value: 'no-cache' }] },
        { source: '/fonts/**', headers: [{ key: 'Cache-Control', value: 'no-cache' }] },
        { source: '**/*.json', headers: [{ key: 'Cache-Control', value: 'no-store' }] },
        { regex: '^/assets/[^/]+-[A-Za-z0-9_-]{8,}\\.(js|css)$',
          headers: [{ key: 'Cache-Control', value: 'public, max-age=31536000, immutable' }] },
      ],
    },
  };
}
