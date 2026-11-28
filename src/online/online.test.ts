import test from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { readOnlineConfiguration } from './config.ts';
import { OnlineController } from './firebaseClient.ts';
import {
  createDeletionReceiptToken, deletionJobId, guardedCallable, parseDeletionReceipt,
  requestConsentedTriage, submitConsentedCase, type CallableTransport, type OnlineSession,
  type SessionPort, type TriageDecision, type TriageInput,
} from './contracts.ts';
import { buildOnlineTriageInput, type OnlineIntakeDraft } from './intake.ts';
import { ONLINE_FLOWS } from './intake.ts';
import { dispatchFirebaseCallable } from './callableProtocol.ts';
import { parseAcceptingPharmacies, parseDeletionPolicy, parseDeletionSummary, parseParentCases } from './responses.ts';
import { OnlineProvider } from './OnlineProvider.tsx';
import { OnlineAccountPanel } from '../components/OnlineAccountPanel.tsx';
import { OnlineConsultationPanel } from '../components/OnlineConsultationPanel.tsx';

const validEnv = {
  VITE_MANO_ONLINE_ENABLED: 'true', VITE_FIREBASE_PROJECT_ID: 'synthetic-mano',
  VITE_FIREBASE_AUTH_DOMAIN: 'synthetic-mano.firebaseapp.com', VITE_FIREBASE_API_KEY: `AIza${'a'.repeat(30)}`,
  VITE_FIREBASE_APP_ID: '1:12345:web:abc123', VITE_FIREBASE_APPCHECK_SITE_KEY: 'synthetic_recaptcha_site_key', VITE_FIREBASE_FUNCTIONS_REGION: 'us-central1',
};
const input: TriageInput = { ageBandId: 'toddler_1_3y', symptomFlowId: 'otc_question', temperatureCelsius: null, durationHours: null, selectedRedFlagIds: [], questionSummaryFa: 'SYNTHETIC QUESTION ONLY', hasUrgentFlag: false };
const goodDecision: TriageDecision = { outcome: 'NONURGENT', eligibleForNonUrgentConsultation: true, triagePassId: 'synthetic-pass', expiresAt: 10_000, titleFa: 'SYNTHETIC', summaryFa: 'SYNTHETIC', emergencyActionsFa: [] };
const user = { uid: 'synthetic-a', email: 'synthetic@example.invalid', emailVerified: true, staffRole: null, pharmacyId: null, staffVerified: false } as const;
function setup() {
  let session: OnlineSession = { generation: 1, user, connected: true, pending: false };
  const port: SessionPort = { enabled: true, getSession: () => session };
  const calls: { name: string; data: unknown }[] = [];
  const transport: CallableTransport = { call: async (name, data) => { calls.push({ name, data }); return name === 'issueConsultationTriagePass' ? goodDecision : { caseId: 'synthetic-case' }; } };
  return { port, calls, transport, change: (next: OnlineSession) => { session = next; } };
}

test('online is disabled unless the complete explicit public Firebase configuration is valid', () => {
  assert.deepEqual(readOnlineConfiguration(), { enabled: false, reason: 'disabled' });
  assert.deepEqual(readOnlineConfiguration({}), { enabled: false, reason: 'disabled' });
  assert.equal(readOnlineConfiguration({ ...validEnv, VITE_MANO_ONLINE_ENABLED: 'false' }).enabled, false);
  assert.equal(readOnlineConfiguration(validEnv).enabled, true);
  for (const key of ['VITE_FIREBASE_PROJECT_ID', 'VITE_FIREBASE_AUTH_DOMAIN', 'VITE_FIREBASE_API_KEY', 'VITE_FIREBASE_APP_ID', 'VITE_FIREBASE_APPCHECK_SITE_KEY']) {
    assert.equal(readOnlineConfiguration({ ...validEnv, [key]: '' }).enabled, false, key);
  }
  assert.equal(readOnlineConfiguration({ ...validEnv, VITE_FIREBASE_AUTH_DOMAIN: 'attacker.example' }).enabled, false);
  assert.equal(readOnlineConfiguration({ ...validEnv, VITE_FIREBASE_FUNCTIONS_REGION: 'http://127.0.0.1:5001' }).enabled, false);
});

test('offline account and consultation panels render without a browser or Firebase SDK initialization', () => {
  const html = renderToStaticMarkup(React.createElement(OnlineProvider, null,
    React.createElement(React.Fragment, null, React.createElement(OnlineAccountPanel), React.createElement(OnlineConsultationPanel))));
  assert.match(html, /online-unavailable-note/);
  assert.match(html, /ارسال آنلاین در این نسخه فعال نیست/);
  assert.doesNotMatch(html, /online-password-input|type="password"/);
});

test('unconfigured controller actions reject before importing Firebase or starting any network work', async () => {
  const controller = new OnlineController({ enabled: false, reason: 'disabled' });
  const before = controller.getSession();
  await assert.rejects(controller.signIn('synthetic@example.invalid', 'synthetic-only-password'), { code: 'OFFLINE_UNAVAILABLE' });
  await assert.rejects(controller.resetPassword('synthetic@example.invalid'), { code: 'OFFLINE_UNAVAILABLE' });
  assert.deepEqual(controller.getSession(), before);
});

test('consent, sign-in and verified email are checked before inspecting or serializing child information', async () => {
  const { port, transport, calls, change } = setup();
  const untouched = new Proxy(input, { ownKeys() { throw new Error('Sensitive input inspected too soon'); } });
  await assert.rejects(requestConsentedTriage(port, transport, untouched, false), { code: 'CONSENT_REQUIRED' });
  change({ generation: 2, user: null, connected: true, pending: false });
  await assert.rejects(requestConsentedTriage(port, transport, untouched, true), { code: 'STALE_SESSION' });
  change({ generation: 3, user: { ...user, emailVerified: false }, connected: true, pending: false });
  await assert.rejects(requestConsentedTriage(port, transport, untouched, true), { code: 'EMAIL_NOT_VERIFIED' });
  assert.deepEqual(calls, []);
});

test('account switching discards a response from the previous account', async () => {
  const { port, change } = setup();
  let finish: (value: unknown) => void = () => {};
  const transport: CallableTransport = { call: () => new Promise(resolve => { finish = resolve; }) };
  const result = guardedCallable(port, transport, 'listParentConsultations', {}, { clinical: true });
  change({ generation: 2, user: { ...user, uid: 'synthetic-b' }, connected: true, pending: false });
  finish({ sensitive: 'SYNTHETIC_A_ONLY' });
  await assert.rejects(result, { code: 'STALE_SESSION' });
});

test('a role/token generation change discards a response even when the UID is the same', async () => {
  const { port, change } = setup();
  let finish: (value: unknown) => void = () => {};
  const result = guardedCallable(port, { call: () => new Promise(resolve => { finish = resolve; }) }, 'getPharmacyInbox', {});
  change({ generation: 2, user, connected: true, pending: false }); finish([]);
  await assert.rejects(result, { code: 'STALE_SESSION' });
});

test('unexpected child fields cannot be attached to a consented triage upload', async () => {
  const { port, calls, transport } = setup();
  await assert.rejects(requestConsentedTriage(port, transport, { ...input, childProfile: { allergy: 'SYNTHETIC_SECRET' } } as TriageInput, true), { code: 'INVALID_INPUT' });
  assert.deepEqual(calls, []);
});

test('submission binds consent, server eligibility, expiry, inputs and session generation', async () => {
  const { port, calls, transport } = setup();
  const issued = await requestConsentedTriage(port, transport, input, true);
  await assert.rejects(submitConsentedCase(port, transport, input, issued, false, 'synthetic-pharmacy', 'OTC', 100), { code: 'CONSENT_REQUIRED' });
  await assert.rejects(submitConsentedCase(port, transport, input, issued, true, 'synthetic-pharmacy', 'OTC', 10_000), { code: 'PASS_EXPIRED' });
  await assert.rejects(submitConsentedCase(port, transport, { ...input, questionSummaryFa: 'CHANGED SYNTHETIC QUESTION' }, issued, true, 'synthetic-pharmacy', 'OTC', 100), { code: 'PASS_EXPIRED' });
  await assert.rejects(submitConsentedCase(port, transport, input, { ...issued, decision: { ...goodDecision, outcome: 'URGENT_EMERGENCY' } }, true, 'synthetic-pharmacy', 'OTC', 100), { code: 'PASS_EXPIRED' });
  assert.equal(calls.length, 1);
  await submitConsentedCase(port, transport, input, issued, true, 'synthetic-pharmacy', 'OTC', 100);
  assert.equal(calls[1].name, 'submitConsultationCase');
  assert.deepEqual(calls[1].data, { ...input, pharmacyId: 'synthetic-pharmacy', categoryName: 'OTC', triagePassId: 'synthetic-pass', consentAccepted: true, consentVersion: 'v1' });
});

const draft: OnlineIntakeDraft = { ageMonths: '18', flowId: 'fever', temperature: '38.1', duration: '12', question: 'SYNTHETIC QUESTION', selectedFlags: [], flagsReviewed: true };
test('intake requires actual age, relevant measurements and explicit flag review', () => {
  for (const patch of [{ ageMonths: '' }, { temperature: '' }, { duration: '' }, { flagsReviewed: false }, { selectedFlags: ['invented-flag'] }, { ageMonths: '1e2' }]) assert.throws(() => buildOnlineTriageInput({ ...draft, ...patch }));
  assert.equal(buildOnlineTriageInput(draft).temperatureCelsius, 38.1);
});

test('fractional infant ages never round up across clinical boundaries', () => {
  assert.equal(buildOnlineTriageInput({ ...draft, ageMonths: '۲٫۷' }).ageBandId, 'newborn');
  const older = buildOnlineTriageInput({ ...draft, ageMonths: '۶٫۹' });
  assert.equal(older.ageBandId, 'infant_3_12m'); assert.equal(older.ageMonthsExact, 6);
  assert.equal(buildOnlineTriageInput({ ...draft, ageMonths: '11.99' }).ageMonthsExact, 11);
  assert.equal(buildOnlineTriageInput({ ...draft, ageMonths: '12' }).ageBandId, 'toddler_1_3y');
});

test('canonical flow mapping preserves selected warning IDs and does not invent a nonurgent decision', () => {
  const mapped = buildOnlineTriageInput({ ...draft, flowId: 'diaper_rash', selectedFlags: ['fever_blisters'] });
  assert.equal(mapped.symptomFlowId, 'rash'); assert.equal(mapped.hasUrgentFlag, true);
  assert.deepEqual(mapped.selectedRedFlagIds, ['fever_blisters']);
  assert.equal('outcome' in mapped, false);
});

test('private deletion receipt is downloadable before a request and derives the backend job ID without a UID', async () => {
  const token = createDeletionReceiptToken(); assert.match(token, /^[\w-]{43}$/);
  const jobId = await deletionJobId(token); assert.match(jobId, /^del_[0-9a-f]{40}$/);
  assert.equal(await deletionJobId(token), jobId);
  assert.notEqual(await deletionJobId(createDeletionReceiptToken()), jobId);
  const receipt = { format: 'mano-account-deletion', version: 1, jobId, receiptToken: token, policyVersion: 'synthetic-v1' };
  assert.deepEqual(parseDeletionReceipt(JSON.stringify(receipt)), receipt);
  assert.throws(() => parseDeletionReceipt(JSON.stringify({ ...receipt, uid: 'synthetic-a' })));
  assert.throws(() => parseDeletionReceipt('a'.repeat(2049)));
});

test('receipt-only status is available after sign-out without permitting anonymous clinical calls', async () => {
  const { port, transport, calls, change } = setup();
  change({ generation: 2, user: null, connected: true, pending: false });
  await assert.rejects(guardedCallable(port, transport, 'listParentConsultations', {}, { clinical: true }), { code: 'STALE_SESSION' });
  await assert.rejects(guardedCallable(port, transport, 'listParentConsultations', {}, { anonymousReceipt: true }), { code: 'STALE_SESSION' });
  await assert.rejects(guardedCallable(port, transport, 'submitConsultationCase', input, { anonymousReceipt: true }), { code: 'STALE_SESSION' });
  await guardedCallable(port, transport, 'getAccountDeletionStatus', { jobId: 'synthetic', receiptToken: 'synthetic' }, { anonymousReceipt: true });
  assert.equal(calls.length, 1); assert.equal(calls[0].name, 'getAccountDeletionStatus');
});

test('generic callable options cannot bypass verified email, upload consent or a pending account transition', async () => {
  const { port, calls, transport, change } = setup();
  await assert.rejects(guardedCallable(port, transport, 'issueConsultationTriagePass', input), { code: 'CONSENT_REQUIRED' });
  change({ generation: 1, user: { ...user, emailVerified: false }, connected: true, pending: false });
  await assert.rejects(guardedCallable(port, transport, 'getPharmacyInbox', {}), { code: 'EMAIL_NOT_VERIFIED' });
  change({ generation: 2, user, connected: true, pending: true });
  await assert.rejects(guardedCallable(port, transport, 'getAccountDeletionPolicy', {}), { code: 'STALE_SESSION' });
  assert.deepEqual(calls, []);
});

const enabledConfiguration = readOnlineConfiguration(validEnv);
if (!enabledConfiguration.enabled) throw new Error('Invalid synthetic configuration');
const firebaseConfig = enabledConfiguration.firebase;
const identity = { generation: 1, uid: user.uid };
const credentials = { idToken: 'SYNTHETIC_ID_TOKEN_A', appCheckToken: 'SYNTHETIC_APP_CHECK', getAuthUid: () => user.uid };

test('account switching while SDK/token work is pending prevents serialization and dispatch of old clinical data', async () => {
  const { port, change } = setup();
  let finish: (value: typeof credentials) => void = () => {};
  let fetched = false;
  const sensitive = { toJSON() { throw new Error('Old clinical data must not be serialized'); } };
  const pending = dispatchFirebaseCallable(port, firebaseConfig, 'issueConsultationTriagePass', sensitive, identity,
    () => new Promise(resolve => { finish = resolve; }), async () => { fetched = true; throw new Error('No dispatch permitted'); });
  change({ generation: 2, user: { ...user, uid: 'synthetic-b' }, connected: true, pending: false });
  finish(credentials);
  await assert.rejects(pending, { code: 'STALE_SESSION' });
  assert.equal(fetched, false);
});

test('an SDK account change not yet published by the auth observer cannot attach the new token to old data', async () => {
  const { port } = setup();
  let fetched = false;
  await assert.rejects(dispatchFirebaseCallable(port, firebaseConfig, 'issueConsultationTriagePass', input, identity,
    async () => ({ ...credentials, getAuthUid: () => 'synthetic-b' }),
    async () => { fetched = true; throw new Error('No dispatch permitted'); }), { code: 'STALE_SESSION' });
  assert.equal(fetched, false);
});

test('a synchronous session change during serialization also prevents network dispatch', async () => {
  const { port, change } = setup();
  let fetched = false;
  const payload = { toJSON() { change({ generation: 2, user: null, connected: true, pending: false }); return input; } };
  await assert.rejects(dispatchFirebaseCallable(port, firebaseConfig, 'issueConsultationTriagePass', payload, identity,
    async () => credentials, async () => { fetched = true; throw new Error('No dispatch permitted'); }), { code: 'STALE_SESSION' });
  assert.equal(fetched, false);
});

test('callable HTTP uses the captured user credential and exact endpoint without cookies or automatic retries', async () => {
  const { port } = setup();
  const calls: { url: string; options: RequestInit | undefined }[] = [];
  const result = await dispatchFirebaseCallable(port, firebaseConfig, 'issueConsultationTriagePass', input, identity, async () => credentials,
    async (url, options) => { calls.push({ url: String(url), options }); return new Response(JSON.stringify({ result: goodDecision }), { status: 200 }); });
  assert.deepEqual(result, goodDecision);
  assert.equal(calls.length, 1);
  assert.equal(calls[0].url, 'https://us-central1-synthetic-mano.cloudfunctions.net/issueConsultationTriagePass');
  assert.equal(calls[0].options?.credentials, 'omit');
  assert.equal(calls[0].options?.cache, 'no-store');
  assert.equal(calls[0].options?.redirect, 'error');
  assert.deepEqual(calls[0].options?.headers, { 'Content-Type': 'application/json', 'X-Firebase-AppCheck': credentials.appCheckToken, Authorization: `Bearer ${credentials.idToken}` });
  assert.deepEqual(JSON.parse(String(calls[0].options?.body)), { data: input });
});

test('protocol failures are bounded and never expose server error messages or issue automatic retries', async () => {
  const { port } = setup();
  let calls = 0;
  await assert.rejects(dispatchFirebaseCallable(port, firebaseConfig, 'getAccountDeletionPolicy', {}, identity, async () => credentials,
    async () => { calls++; return new Response(JSON.stringify({ error: { status: 'FAILED_PRECONDITION', message: 'SYNTHETIC_PRIVATE_SERVER_DETAIL' } }), { status: 400 }); }),
  error => error instanceof Error && error.message === 'functions/failed-precondition' && !error.message.includes('PRIVATE'));
  assert.equal(calls, 1);
  for (const response of [new Response('not json'), new Response('[]'), new Response('{}'), new Response('x', { headers: { 'content-length': String(2 * 1024 * 1024) } })]) {
    await assert.rejects(dispatchFirebaseCallable(port, firebaseConfig, 'getAccountDeletionPolicy', {}, identity, async () => credentials, async () => response), { code: 'INVALID_RESPONSE' });
  }
});

test('response parsing matches current backend views, drops staff-only extras and rejects wrong-tenant or malformed replies', () => {
  const item = { caseId: 'synthetic-case', pharmacyId: 'synthetic-pharmacy', questionSummaryFa: 'SYNTHETIC QUESTION', status: 'PENDING', createdAt: 100, parentResponse: null, internalNotes: [{ noteText: 'STAFF_ONLY_SYNTHETIC' }] };
  assert.deepEqual(parseParentCases([item]), [{ caseId: item.caseId, pharmacyId: item.pharmacyId, questionSummaryFa: item.questionSummaryFa, status: 'PENDING', createdAt: 100, parentResponse: null }]);
  const reviewed = { ...item, status: 'REVIEWED', parentResponse: { replyTextFa: 'SYNTHETIC PHARMACIST REPLY', respondedByRole: 'pharmacist', respondedAt: 200, attestationConfirmed: true } };
  assert.equal(parseParentCases([reviewed], item.pharmacyId)[0].parentResponse?.replyTextFa, reviewed.parentResponse.replyTextFa);
  for (const invalid of [null, [null], [item, item], [{ ...item, createdAt: -1 }], [{ ...reviewed, parentResponse: null }], [{ ...reviewed, parentResponse: { ...reviewed.parentResponse, respondedByRole: 'admin' } }]]) assert.throws(() => parseParentCases(invalid), { code: 'INVALID_RESPONSE' });
  assert.throws(() => parseParentCases([item], 'different-pharmacy'), { code: 'INVALID_RESPONSE' });
  const pharmacy = { pharmacyId: 'synthetic-pharmacy', displayNameFa: 'SYNTHETIC PHARMACY', acceptingCases: true };
  assert.deepEqual(parseAcceptingPharmacies([pharmacy]), [pharmacy]);
  assert.throws(() => parseAcceptingPharmacies([pharmacy, pharmacy]), { code: 'INVALID_RESPONSE' });
  assert.throws(() => parseAcceptingPharmacies([{ ...pharmacy, acceptingCases: false }]), { code: 'INVALID_RESPONSE' });
});

test('deletion policy and status parsing reject malformed retention claims and unrelated receipts', async () => {
  const unavailable = { available: false, policyVersion: null, clinicalRetentionDays: null, backupRetentionDays: null, receiptDays: 30, staffSelfServiceSupported: false };
  assert.deepEqual(parseDeletionPolicy(unavailable), unavailable);
  assert.throws(() => parseDeletionPolicy({ ...unavailable, available: true }), { code: 'INVALID_RESPONSE' });
  assert.throws(() => parseDeletionPolicy({ ...unavailable, receiptDays: null }), { code: 'INVALID_RESPONSE' });
  const jobId = await deletionJobId(createDeletionReceiptToken());
  const summary = { jobId, status: 'COMPLETED_WITH_RETENTION', stage: 'DONE', policyVersion: 'synthetic-v1', requestedAt: 100, completedAt: 200, retainedUntilMs: 1000, backupRetentionUntilMs: 1000, receiptExpiresAtMs: 2000, deletedDocuments: 10, retainedScopes: ['clinical-case-and-audit-records'], failureCode: null };
  assert.deepEqual(parseDeletionSummary(summary, jobId), summary);
  for (const invalid of [{ ...summary, stage: 'USER_DATA' }, { ...summary, completedAt: null }, { ...summary, retainedUntilMs: -1 }, { ...summary, retainedScopes: null }]) assert.throws(() => parseDeletionSummary(invalid, jobId), { code: 'INVALID_RESPONSE' });
  assert.throws(() => parseDeletionSummary(summary, `del_${'a'.repeat(40)}`), { code: 'INVALID_RESPONSE' });
  assert.throws(() => parseDeletionReceipt('{'), { code: 'INVALID_RECEIPT' });
  await assert.rejects(deletionJobId(`${'A'.repeat(42)}B`), { code: 'INVALID_RECEIPT' });
});

test('all selectable web intake flows map to the current server flow allowlist', () => {
  const allowed = new Set(['fever', 'cough', 'runny_nose', 'wet_cough', 'eye', 'ear', 'diarrhea', 'constipation', 'rash', 'otc_question', 'supplement_question', 'general']);
  assert.ok(ONLINE_FLOWS.length >= 11);
  for (const flow of ONLINE_FLOWS) {
    const built = buildOnlineTriageInput({ ...draft, flowId: flow.id, selectedFlags: [], flagsReviewed: true });
    assert.ok(allowed.has(built.symptomFlowId), flow.id);
    assert.deepEqual(built.selectedRedFlagIds, []);
  }
});
