export const ONLINE_CONSENT_VERSION = 'v1' as const;
export interface OnlineUser {
  uid: string;
  email: string | null;
  emailVerified: boolean;
  staffRole: 'pharmacist' | 'admin' | null;
  pharmacyId: string | null;
  staffVerified: boolean;
}
export interface OnlineSession {
  generation: number;
  user: OnlineUser | null;
  connected: boolean;
  pending: boolean;
}
export interface TriageInput {
  ageBandId: string;
  symptomFlowId: string;
  ageMonthsExact?: number;
  temperatureCelsius: number | null;
  durationHours: number | null;
  selectedRedFlagIds: string[];
  questionSummaryFa: string;
  hasUrgentFlag: boolean;
}
export interface TriageDecision {
  outcome: 'URGENT_EMERGENCY' | 'NEEDS_TIMELY_REVIEW' | 'NONURGENT';
  eligibleForNonUrgentConsultation: boolean;
  triagePassId: string | null;
  expiresAt: number | null;
  titleFa: string;
  summaryFa: string;
  emergencyActionsFa: string[];
}
export interface PharmacyOption { pharmacyId: string; displayNameFa: string; acceptingCases: boolean }
export interface ParentCase {
  careCard?: import('./caseCareCard.ts').CaseCareCard;
  caseId: string;
  pharmacyId: string;
  questionSummaryFa: string;
  status: 'PENDING' | 'RECEIVED' | 'REVIEWED';
  createdAt: number;
  parentResponse: { replyTextFa: string; respondedAt: number; attestationConfirmed: true } | null;
}
export interface DeletionPolicy {
  available: boolean;
  policyVersion: string | null;
  clinicalRetentionDays: number | null;
  backupRetentionDays: number | null;
  receiptDays: number;
  staffSelfServiceSupported: false;
}
export interface DeletionSummary {
  jobId: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'PARTIAL_FAILURE' | 'COMPLETED_WITH_RETENTION';
  stage: string;
  policyVersion: string;
  requestedAt: number;
  completedAt: number | null;
  retainedUntilMs: number;
  backupRetentionUntilMs: number;
  receiptExpiresAtMs: number;
  deletedDocuments: number;
  retainedScopes: string[];
  failureCode: string | null;
}
export interface DeletionReceipt { format: 'mano-account-deletion'; version: 1; jobId: string; receiptToken: string; policyVersion: string }

export class OnlineBoundaryError extends Error {
  constructor(public readonly code: string) { super(code); this.name = 'OnlineBoundaryError'; }
}

export type CallableName = 'listAcceptingPharmacies' | 'issueConsultationTriagePass' |
  'submitConsultationCase' | 'listParentConsultations' | 'getPharmacyInbox' |
  'reviewConsultationCase' | 'updateCareCard' | 'getAccountDeletionPolicy' | 'requestAccountDeletion' | 'getAccountDeletionStatus';
export interface DispatchIdentity { generation: number; uid: string | null }
export interface CallableTransport { call(name: CallableName, data: unknown, identity: DispatchIdentity): Promise<unknown> }
export interface SessionPort { getSession(): OnlineSession; enabled: boolean }

export function assertDispatchIdentity(port: SessionPort, identity: DispatchIdentity, currentAuthUid: string | null): void {
  const current = port.getSession();
  if (current.pending || current.generation !== identity.generation ||
      (current.user?.uid ?? null) !== identity.uid || currentAuthUid !== identity.uid) {
    throw new OnlineBoundaryError('STALE_SESSION');
  }
}

const CLINICAL_CALLABLES = new Set<CallableName>([
  'listAcceptingPharmacies', 'issueConsultationTriagePass', 'submitConsultationCase',
  'listParentConsultations', 'getPharmacyInbox', 'reviewConsultationCase', 'updateCareCard',
]);

/** Bound both before dispatch and after response; account switching never exposes old results. */
export async function guardedCallable<T>(
  port: SessionPort, transport: CallableTransport, name: CallableName, data: unknown,
  options: { clinical?: boolean; anonymousReceipt?: boolean; expectedGeneration?: number; consent?: boolean } = {},
): Promise<T> {
  if (!port.enabled) throw new OnlineBoundaryError('OFFLINE_UNAVAILABLE');
  const before = port.getSession();
  if (before.pending) throw new OnlineBoundaryError('STALE_SESSION');
  if (options.expectedGeneration !== undefined && before.generation !== options.expectedGeneration) throw new OnlineBoundaryError('STALE_SESSION');
  if (!before.user && !(options.anonymousReceipt && name === 'getAccountDeletionStatus')) throw new OnlineBoundaryError('STALE_SESSION');
  if ((options.clinical || CLINICAL_CALLABLES.has(name)) && !before.user?.emailVerified) throw new OnlineBoundaryError('EMAIL_NOT_VERIFIED');
  if ((name === 'issueConsultationTriagePass' || name === 'submitConsultationCase') && options.consent !== true) throw new OnlineBoundaryError('CONSENT_REQUIRED');
  const result = await transport.call(name, data, { generation: before.generation, uid: before.user?.uid ?? null });
  const after = port.getSession();
  if (before.generation !== after.generation || before.user?.uid !== after.user?.uid) throw new OnlineBoundaryError('STALE_SESSION');
  return result as T;
}

export interface IssuedTriage {
  generation: number;
  fingerprint: string;
  decision: TriageDecision;
}
function clinicalGeneration(port: SessionPort): number {
  if (!port.enabled) throw new OnlineBoundaryError('OFFLINE_UNAVAILABLE');
  const session = port.getSession();
  if (!session.user) throw new OnlineBoundaryError('STALE_SESSION');
  if (!session.user.emailVerified) throw new OnlineBoundaryError('EMAIL_NOT_VERIFIED');
  return session.generation;
}
function triagePayload(input: TriageInput): TriageInput {
  const keys = ['ageBandId', 'symptomFlowId', 'ageMonthsExact', 'temperatureCelsius', 'durationHours', 'selectedRedFlagIds', 'questionSummaryFa', 'hasUrgentFlag'];
  if (!input || typeof input !== 'object' || Array.isArray(input) || Object.keys(input).some(key => !keys.includes(key)) ||
      !['newborn', 'infant_3_12m', 'toddler_1_3y', 'preschool_3_6y', 'school_6_plus'].includes(input.ageBandId) ||
      !['fever', 'cough', 'runny_nose', 'wet_cough', 'eye', 'ear', 'diarrhea', 'constipation', 'rash', 'otc_question', 'supplement_question', 'general'].includes(input.symptomFlowId) ||
      (input.ageMonthsExact !== undefined && (input.ageBandId !== 'infant_3_12m' || !Number.isInteger(input.ageMonthsExact) || input.ageMonthsExact < 3 || input.ageMonthsExact > 11)) ||
      (input.temperatureCelsius !== null && (!Number.isFinite(input.temperatureCelsius) || input.temperatureCelsius < 34 || input.temperatureCelsius > 43)) ||
      (input.durationHours !== null && (!Number.isInteger(input.durationHours) || input.durationHours < 0 || input.durationHours > 8760)) ||
      !Array.isArray(input.selectedRedFlagIds) || input.selectedRedFlagIds.length > 64 || input.selectedRedFlagIds.some(id => typeof id !== 'string' || !/^[\w-]{1,64}$/.test(id)) ||
      new Set(input.selectedRedFlagIds).size !== input.selectedRedFlagIds.length || typeof input.questionSummaryFa !== 'string' || input.questionSummaryFa.trim().length < 8 || input.questionSummaryFa.length > 600 || typeof input.hasUrgentFlag !== 'boolean') throw new OnlineBoundaryError('INVALID_INPUT');
  return {
    ageBandId: input.ageBandId, symptomFlowId: input.symptomFlowId,
    ...(input.ageMonthsExact !== undefined ? { ageMonthsExact: input.ageMonthsExact } : {}),
    temperatureCelsius: input.temperatureCelsius, durationHours: input.durationHours,
    selectedRedFlagIds: [...input.selectedRedFlagIds], questionSummaryFa: input.questionSummaryFa,
    hasUrgentFlag: input.hasUrgentFlag,
  };
}
export async function requestConsentedTriage(
  port: SessionPort, transport: CallableTransport, input: TriageInput, consent: boolean,
): Promise<IssuedTriage> {
  if (consent !== true) throw new OnlineBoundaryError('CONSENT_REQUIRED');
  const generation = clinicalGeneration(port);
  const payload = triagePayload(input);
  const fingerprint = JSON.stringify(payload);
  const decision = await guardedCallable<TriageDecision>(port, transport, 'issueConsultationTriagePass', payload, { clinical: true, consent: true, expectedGeneration: generation });
  if (!decision || !['URGENT_EMERGENCY', 'NEEDS_TIMELY_REVIEW', 'NONURGENT'].includes(decision.outcome) ||
      typeof decision.titleFa !== 'string' || typeof decision.summaryFa !== 'string' ||
      !Array.isArray(decision.emergencyActionsFa) || decision.emergencyActionsFa.some(s => typeof s !== 'string')) {
    throw new OnlineBoundaryError('INVALID_RESPONSE');
  }
  return { generation, fingerprint, decision };
}
export async function submitConsentedCase(
  port: SessionPort, transport: CallableTransport, input: TriageInput,
  issued: IssuedTriage, consent: boolean, pharmacyId: string, categoryName: string, nowMs = Date.now(),
): Promise<ParentCase> {
  if (consent !== true) throw new OnlineBoundaryError('CONSENT_REQUIRED');
  clinicalGeneration(port);
  const payload = triagePayload(input);
  const { decision } = issued;
  if (issued.generation !== port.getSession().generation || issued.fingerprint !== JSON.stringify(payload) ||
      decision.outcome !== 'NONURGENT' || decision.eligibleForNonUrgentConsultation !== true ||
      typeof decision.triagePassId !== 'string' || !/^[\w-]{1,128}$/.test(decision.triagePassId) ||
      typeof decision.expiresAt !== 'number' || !Number.isFinite(decision.expiresAt) || decision.expiresAt <= nowMs ||
      !/^[\w-]{1,128}$/.test(pharmacyId) || !['SYMPTOMS', 'OTC', 'SKIN', 'SCALP_HAIR', 'SUPPLEMENT'].includes(categoryName) || input.hasUrgentFlag) {
    throw new OnlineBoundaryError('PASS_EXPIRED');
  }
  return guardedCallable<ParentCase>(port, transport, 'submitConsultationCase', {
    ...payload, pharmacyId, categoryName, triagePassId: decision.triagePassId,
    consentAccepted: true, consentVersion: ONLINE_CONSENT_VERSION,
  }, { clinical: true, consent: true, expectedGeneration: issued.generation });
}

export function createDeletionReceiptToken(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
export async function deletionJobId(receiptToken: string): Promise<string> {
  if (!validReceiptToken(receiptToken)) throw new OnlineBoundaryError('INVALID_RECEIPT');
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(receiptToken));
  return `del_${Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, '0')).join('').slice(0, 40)}`;
}
export function parseDeletionReceipt(text: string): DeletionReceipt {
  if (text.length > 2048) throw new OnlineBoundaryError('INVALID_RECEIPT');
  let raw: unknown;
  try { raw = JSON.parse(text); } catch { throw new OnlineBoundaryError('INVALID_RECEIPT'); }
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) throw new OnlineBoundaryError('INVALID_RECEIPT');
  const receipt = raw as Record<string, unknown>;
  if (Object.keys(receipt).sort().join(',') !== 'format,jobId,policyVersion,receiptToken,version' || receipt.format !== 'mano-account-deletion' || receipt.version !== 1 ||
      typeof receipt.jobId !== 'string' || !/^del_[0-9a-f]{40}$/.test(receipt.jobId) ||
      typeof receipt.policyVersion !== 'string' || !/^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/.test(receipt.policyVersion) ||
      typeof receipt.receiptToken !== 'string' || !validReceiptToken(receipt.receiptToken)) throw new OnlineBoundaryError('INVALID_RECEIPT');
  return receipt as unknown as DeletionReceipt;
}

function validReceiptToken(value: string): boolean {
  if (!/^[A-Za-z0-9_-]{43}$/.test(value)) return false;
  const bytes = atob(value.replace(/-/g, '+').replace(/_/g, '/') + '=');
  return bytes.length === 32 && btoa(bytes).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '') === value;
}
