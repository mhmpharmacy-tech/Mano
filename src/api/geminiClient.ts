import type {
  GatewayResponseMeta,
  RestrictedDocumentedQaPayload,
  RestrictedSafetyChecklistPayload,
  RestrictedSyrupScanPayload,
  VisitDraftStructuredPayload,
} from '../domain/aiGatewayContracts.ts';
import type { FaqItem, NutritionPlan } from '../domain/models.ts';

export interface HealthStatus {
  ok: boolean;
  clinicalServiceEnabled: boolean;
  aiServiceAvailable: boolean;
  offlineEnginesIndependent: boolean;
}

export interface GatewayClientResult<T> {
  data: T;
  meta: GatewayResponseMeta;
  policyError?: string;
}

export class ApiClientError extends Error {
  status: number;
  code?: string;
  meta?: GatewayResponseMeta;

  constructor(message: string, status: number, code?: string, meta?: GatewayResponseMeta) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.code = code;
    this.meta = meta;
  }
}

export function isRequestCancelledError(err: unknown): boolean {
  if (!err || typeof err !== 'object') return false;
  const anyErr = err as { name?: string; code?: string; status?: number };
  return (
    anyErr.name === 'AbortError' ||
    anyErr.code === 'REQUEST_CANCELLED' ||
    anyErr.status === 499
  );
}

function throwIfRequestCancelled(signal?: AbortSignal): void {
  if (signal?.aborted) {
    throw new ApiClientError('درخواست لغو شد و اطلاعاتی ارسال نشد.', 499, 'REQUEST_CANCELLED');
  }
}

/**
 * The gateway's deployment token authorizes trusted servers, not a caregiver.
 * Browser AI has no authenticated user session or consent integration yet.
 * Keep this boundary unconditional: neither public health flags, build variables
 * nor a browser-supplied deployment token can authorize transmitting child data.
 * A future user-authenticated integration must replace this boundary explicitly.
 */
function rejectBrowserAiRequest(signal?: AbortSignal): never {
  throwIfRequestCancelled(signal);
  throw new ApiClientError(
    'قابلیت‌های هوش مصنوعی در این نسخهٔ مرورگر فعال نیستند؛ هیچ اطلاعاتی ارسال نشد. تا آماده‌شدن ورود امن و رضایت برای ارسال، از ابزارهای محلی یا مشاورهٔ حضوری استفاده کنید.',
    503,
    'BROWSER_AI_UNAVAILABLE',
  );
}

// Public diagnostics only. Availability of a trusted-server endpoint does not
// grant a browser permission to upload clinical information.
export async function fetchApiHealth(signal?: AbortSignal): Promise<HealthStatus> {
  throwIfRequestCancelled(signal);
  const res = await fetch('/api/health', { signal });
  if (!res.ok) {
    throw new ApiClientError('بررسی وضعیت سرویس هوش مصنوعی ناموفق بود.', res.status);
  }
  return (await res.json()) as HealthStatus;
}

export async function requestVisitDraftStream(
  params: {
    promptText: string;
    chiefComplaintFa?: string;
    selectedSymptomsFa?: string[];
    parentNotesFa?: string;
    childContextSummaryFa?: string;
    tempPointsFa?: string[];
    dosePointsFa?: string[];
    signal?: AbortSignal;
  },
  onProgress?: (accumulatedText: string) => void,
): Promise<GatewayClientResult<VisitDraftStructuredPayload>> {
  // Retain the callback contract, but never emit progress for a blocked request.
  void onProgress;
  rejectBrowserAiRequest(params.signal);
}

export async function requestNutritionPlan(params: {
  ageMonths: number;
  weightKg: string;
  allergiesFa: string;
  dietGoalFa: string;
  childContextSummaryFa: string;
  signal?: AbortSignal;
}): Promise<{ plan: NutritionPlan; meta: GatewayResponseMeta; policyError?: string }> {
  rejectBrowserAiRequest(params.signal);
}

export async function requestSyrupScan(params: {
  imageBase64: string;
  mimeType: string;
  childContextSummaryFa: string;
  signal?: AbortSignal;
}): Promise<GatewayClientResult<RestrictedSyrupScanPayload>> {
  rejectBrowserAiRequest(params.signal);
}

export async function requestSafetyChecklist(params: {
  flowId?: string;
  flowTitleFa: string;
  urgencyLevel: string;
  evaluationSummaryFa: string;
  selectedSymptomsFa: string[];
  childContextSummaryFa: string;
  signal?: AbortSignal;
}): Promise<GatewayClientResult<RestrictedSafetyChecklistPayload>> {
  rejectBrowserAiRequest(params.signal);
}

export async function requestDocumentedQa(params: {
  question: string;
  ageBandFa: string;
  matchedFaqs: FaqItem[];
  signal?: AbortSignal;
}): Promise<GatewayClientResult<RestrictedDocumentedQaPayload>> {
  rejectBrowserAiRequest(params.signal);
}
