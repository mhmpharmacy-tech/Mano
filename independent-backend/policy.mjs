import { createHash, timingSafeEqual } from 'node:crypto';
export const tokenPattern = /^[A-Za-z0-9_-]{43}$/;
export const uuidPattern = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i;
export const digest = (value) => createHash('sha256').update(value).digest('hex');
export function sameToken(actual, expected) {
  return typeof actual === 'string' && typeof expected === 'string' &&
    tokenPattern.test(actual) && tokenPattern.test(expected) &&
    timingSafeEqual(Buffer.from(digest(actual),'hex'), Buffer.from(digest(expected),'hex'));
}
export function bearer(header) {
  const match = typeof header === 'string' && /^Bearer ([A-Za-z0-9_-]{43})$/.exec(header);
  return match ? match[1] : null;
}
export function validProfile(body) {
  return body && typeof body === 'object' && !Array.isArray(body) &&
    Object.keys(body).sort().join(',') === 'ageMonths,alias' &&
    typeof body.alias === 'string' && /^demo-[a-z0-9-]{1,40}$/.test(body.alias) &&
    Number.isInteger(body.ageMonths) && body.ageMonths >= 0 && body.ageMonths <= 215;
}
export function validShare(body) {
  return body && typeof body === 'object' && !Array.isArray(body) &&
    Object.keys(body).sort().join(',') === 'consent,pharmacyId' &&
    body.consent === 'synthetic-v1' && typeof body.pharmacyId === 'string' &&
    uuidPattern.test(body.pharmacyId);
}
export function databaseOptions(env) {
  const url = new URL(env.MANO_STAGE_DATABASE_URL || 'invalid:');
  if (!['postgresql:','postgres:'].includes(url.protocol) ||
      url.username !== 'mano_stage_app' || !url.password ||
      url.pathname !== '/mano_stage' || url.search || url.hash) {
    throw new Error('Invalid dedicated staging database configuration');
  }
  let ssl;
  if (env.MANO_STAGE_DB_TLS === 'private-test' && ['mano-db','localhost','127.0.0.1'].includes(url.hostname)) {
    ssl = false;
  } else if (env.MANO_STAGE_DB_TLS === 'verify-full') {
    ssl = { rejectUnauthorized:true, ...(env.MANO_STAGE_DB_CA ? {ca:env.MANO_STAGE_DB_CA} : {}) };
  } else {
    throw new Error('Explicit staging transport policy required');
  }
  return {connectionString:url.href,ssl,max:4,connectionTimeoutMillis:3000,
    idleTimeoutMillis:10000,statement_timeout:3000,query_timeout:4000,
    application_name:'mano-synthetic-stage'};
}
