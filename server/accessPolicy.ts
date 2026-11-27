import { createHash, timingSafeEqual } from 'node:crypto';
import type { RequestHandler } from 'express';

export interface GatewayAccessPolicy {
  readonly clinicalServiceEnabled: boolean;
  readonly accessToken: string | null;
}

// A deployment secret for trusted server callers, not a user identity or a
// browser credential. Generate from at least 32 cryptographically random bytes.
function isValidAccessToken(token: string | null | undefined): token is string {
  return typeof token === 'string' && /^[A-Za-z0-9_-]{43,256}$/.test(token);
}

export function resolveGatewayAccessPolicy(
  env: Record<string, string | undefined> = process.env,
): GatewayAccessPolicy {
  return {
    clinicalServiceEnabled: env.MANO_CLINICAL_SERVICE_ENABLED === 'true',
    accessToken: isValidAccessToken(env.MANO_GATEWAY_ACCESS_TOKEN)
      ? env.MANO_GATEWAY_ACCESS_TOKEN
      : null,
  };
}

export function isGatewayAccessConfigured(policy: GatewayAccessPolicy): boolean {
  return isValidAccessToken(policy.accessToken);
}

export function requireGatewayAccess(
  policy: GatewayAccessPolicy,
  options: { requireClinicalRelease: boolean },
): RequestHandler {
  const configuredToken = policy.accessToken;
  const expectedDigest = isValidAccessToken(configuredToken)
    ? createHash('sha256').update(configuredToken).digest()
    : null;

  return (req, res, next) => {
    res.setHeader('Cache-Control', 'no-store');

    if (options.requireClinicalRelease && !policy.clinicalServiceEnabled) {
      res.status(503).json({
        error: 'سرویس هوش مصنوعی برای استفادهٔ بالینی فعال نشده است.',
        code: 'CLINICAL_SERVICE_DISABLED',
      });
      return;
    }

    if (!expectedDigest) {
      res.status(503).json({
        error: 'دسترسی امن به سرویس هوش مصنوعی تنظیم نشده است.',
        code: 'GATEWAY_ACCESS_UNCONFIGURED',
      });
      return;
    }

    const match = /^Bearer ([A-Za-z0-9_-]{43,256})$/i.exec(req.get('Authorization') ?? '');
    const suppliedDigest = createHash('sha256').update(match?.[1] ?? '').digest();
    // Fixed-length digests keep the secret comparison timing-safe even when the
    // supplied bearer token has a different length from the configured secret.
    if (!timingSafeEqual(suppliedDigest, expectedDigest) || !match) {
      res.setHeader('WWW-Authenticate', 'Bearer');
      res.status(401).json({
        error: 'دسترسی مجاز به سرویس هوش مصنوعی لازم است.',
        code: 'GATEWAY_AUTH_REQUIRED',
      });
      return;
    }

    next();
  };
}
