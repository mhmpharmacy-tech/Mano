import { OnlineBoundaryError, type DeletionPolicy, type DeletionSummary, type ParentCase, type PharmacyOption } from './contracts.ts';

import { parseCaseCareCard } from './caseCareCard.ts';

function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new OnlineBoundaryError('INVALID_RESPONSE');
  return value as Record<string, unknown>;
}
function id(value: unknown): value is string { return typeof value === 'string' && /^[\w-]{1,128}$/.test(value); }
function timestamp(value: unknown): value is number { return typeof value === 'number' && Number.isSafeInteger(value) && value >= 0 && value <= 8_640_000_000_000_000; }
function safeText(value: unknown, max: number): value is string { return typeof value === 'string' && value.length > 0 && value.length <= max; }
function policyVersion(value: unknown): value is string { return typeof value === 'string' && /^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/.test(value); }

/** Keep only the fields the view uses; internal staff notes are never retained here. */
export function parseParentCases(raw: unknown, expectedPharmacyId?: string): ParentCase[] {
  if (!Array.isArray(raw) || raw.length > 100) throw new OnlineBoundaryError('INVALID_RESPONSE');
  const identities = new Set<string>();
  return raw.map(value => {
    const item = record(value);
    if (!id(item.caseId) || !id(item.pharmacyId) || (expectedPharmacyId !== undefined && item.pharmacyId !== expectedPharmacyId) ||
        !safeText(item.questionSummaryFa, 600) || !['PENDING', 'RECEIVED', 'REVIEWED'].includes(String(item.status)) ||
        !timestamp(item.createdAt)) throw new OnlineBoundaryError('INVALID_RESPONSE');
    const identity = `${item.pharmacyId}/${item.caseId}`;
    if (identities.has(identity)) throw new OnlineBoundaryError('INVALID_RESPONSE');
    identities.add(identity);
    let parentResponse: ParentCase['parentResponse'] = null;
    if (item.parentResponse !== null) {
      const reply = record(item.parentResponse);
      if (item.status !== 'REVIEWED' || !safeText(reply.replyTextFa, 2000) || reply.respondedByRole !== 'pharmacist' ||
          reply.attestationConfirmed !== true || !timestamp(reply.respondedAt)) throw new OnlineBoundaryError('INVALID_RESPONSE');
      parentResponse = { replyTextFa: reply.replyTextFa, respondedAt: reply.respondedAt, attestationConfirmed: true };
    } else if (item.status === 'REVIEWED') throw new OnlineBoundaryError('INVALID_RESPONSE');
    if (item.careCard !== undefined && item.status !== 'REVIEWED') throw new OnlineBoundaryError('INVALID_RESPONSE');
    const care = item.careCard === undefined ? {} : { careCard: parseCaseCareCard(item.careCard) };
    return { ...care, caseId: item.caseId, pharmacyId: item.pharmacyId, questionSummaryFa: item.questionSummaryFa, status: item.status as ParentCase['status'], createdAt: item.createdAt, parentResponse };
  });
}

export function parseAcceptingPharmacies(raw: unknown): PharmacyOption[] {
  if (!Array.isArray(raw) || raw.length > 20) throw new OnlineBoundaryError('INVALID_RESPONSE');
  const identities = new Set<string>();
  return raw.map(value => {
    const item = record(value);
    if (!id(item.pharmacyId) || !safeText(item.displayNameFa, 200) || item.acceptingCases !== true || identities.has(item.pharmacyId)) throw new OnlineBoundaryError('INVALID_RESPONSE');
    identities.add(item.pharmacyId);
    return { pharmacyId: item.pharmacyId, displayNameFa: item.displayNameFa, acceptingCases: true };
  });
}

export function parseDeletionPolicy(raw: unknown): DeletionPolicy {
  const item = record(raw);
  if (typeof item.available !== 'boolean' || item.staffSelfServiceSupported !== false || !Number.isInteger(item.receiptDays) || (item.receiptDays as number) < 1 || (item.receiptDays as number) > 365) throw new OnlineBoundaryError('INVALID_RESPONSE');
  if (item.available) {
    if (!policyVersion(item.policyVersion) || !Number.isInteger(item.clinicalRetentionDays) || (item.clinicalRetentionDays as number) < 1 || (item.clinicalRetentionDays as number) > 36500 ||
        !Number.isInteger(item.backupRetentionDays) || (item.backupRetentionDays as number) < 0 || (item.backupRetentionDays as number) > 36500) throw new OnlineBoundaryError('INVALID_RESPONSE');
  } else if (item.policyVersion !== null || item.clinicalRetentionDays !== null || item.backupRetentionDays !== null) throw new OnlineBoundaryError('INVALID_RESPONSE');
  return { available: item.available, policyVersion: item.policyVersion as string | null, clinicalRetentionDays: item.clinicalRetentionDays as number | null, backupRetentionDays: item.backupRetentionDays as number | null, receiptDays: item.receiptDays as number, staffSelfServiceSupported: false };
}

export function parseDeletionSummary(raw: unknown, expectedJobId: string): DeletionSummary {
  const item = record(raw);
  if (item.jobId !== expectedJobId || !/^del_[0-9a-f]{40}$/.test(expectedJobId) ||
      !['PENDING', 'IN_PROGRESS', 'PARTIAL_FAILURE', 'COMPLETED_WITH_RETENTION'].includes(String(item.status)) ||
      !['DISABLE_AUTH', 'USER_DATA', 'TRIAGE', 'RATE_LIMITS', 'STORAGE', 'CLINICAL_MARK', 'DELETE_AUTH', 'DONE'].includes(String(item.stage)) ||
      !policyVersion(item.policyVersion) || !timestamp(item.requestedAt) || (item.completedAt !== null && !timestamp(item.completedAt)) ||
      !timestamp(item.retainedUntilMs) || !timestamp(item.backupRetentionUntilMs) || !timestamp(item.receiptExpiresAtMs) ||
      (item.retainedUntilMs as number) < (item.requestedAt as number) || (item.backupRetentionUntilMs as number) < (item.requestedAt as number) ||
      (item.receiptExpiresAtMs as number) <= (item.requestedAt as number) ||
      !Number.isSafeInteger(item.deletedDocuments) || (item.deletedDocuments as number) < 0 ||
      !Array.isArray(item.retainedScopes) || item.retainedScopes.length > 20 || item.retainedScopes.some(scope => !safeText(scope, 200)) ||
      (item.failureCode !== null && item.failureCode !== 'ERASURE_RETRY_REQUIRED' && item.failureCode !== 'INVENTORY_REVIEW_REQUIRED') ||
      (item.status === 'COMPLETED_WITH_RETENTION' && (item.completedAt === null || item.stage !== 'DONE'))) throw new OnlineBoundaryError('INVALID_RESPONSE');
  return {
    jobId: expectedJobId, status: item.status as DeletionSummary['status'], stage: item.stage as string,
    policyVersion: item.policyVersion, requestedAt: item.requestedAt, completedAt: item.completedAt as number | null,
    retainedUntilMs: item.retainedUntilMs, backupRetentionUntilMs: item.backupRetentionUntilMs, receiptExpiresAtMs: item.receiptExpiresAtMs,
    deletedDocuments: item.deletedDocuments as number, retainedScopes: [...item.retainedScopes] as string[], failureCode: item.failureCode as string | null,
  };
}
