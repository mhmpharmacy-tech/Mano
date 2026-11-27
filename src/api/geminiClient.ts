import type {
  GatewayResponseMeta,
  RestrictedDocumentedQaPayload,
  RestrictedNutritionPayload,
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

interface RawEnvelope<T> {
  data?: T;
  meta?: GatewayResponseMeta;
  error?: string;
  code?: string;
}

async function parseEnvelopeResponse<T>(res: Response): Promise<GatewayClientResult<T>> {
  let parsed: RawEnvelope<T> | null;
  try {
    parsed = (await res.json()) as RawEnvelope<T>;
  } catch {
    parsed = null;
  }

  const canUseFallbackEnvelope =
    (res.status === 200 || res.status === 422 || res.status === 429) &&
    parsed?.data !== undefined &&
    parsed?.meta !== undefined;

  if (canUseFallbackEnvelope && parsed?.data !== undefined && parsed?.meta !== undefined) {
    return {
      data: parsed.data,
      meta: parsed.meta,
      policyError: parsed.error,
    };
  }

  const msg = parsed?.error || 'خطایی در ارتباط با سرور رخ داد. لطفاً دوباره تلاش کنید.';
  throw new ApiClientError(msg, res.status, parsed?.code, parsed?.meta);
}

export async function fetchApiHealth(signal?: AbortSignal): Promise<HealthStatus> {
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
  const { signal, ...payloadBody } = params;
  const res = await fetch('/api/gemini/visit-draft', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    signal,
    body: JSON.stringify({ ...payloadBody, stream: true }),
  });

  const contentType = res.headers.get('content-type') || '';
  if (!res.ok || !contentType.includes('text/event-stream') || !res.body) {
    const result = await parseEnvelopeResponse<VisitDraftStructuredPayload>(res);
    if (result.data.formattedDraftFa) {
      onProgress?.(result.data.formattedDraftFa);
    }
    return result;
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let latestText = '';
  let finalData: VisitDraftStructuredPayload | null = null;
  let finalMeta: GatewayResponseMeta | null = null;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split('\n\n');
    buffer = events.pop() || '';
    for (const evt of events) {
      const line = evt.split('\n').find((l) => l.startsWith('data:'));
      if (!line) continue;
      const jsonStr = line.slice(5).trim();
      if (!jsonStr) continue;
      const parsed = JSON.parse(jsonStr) as {
        delta?: string;
        text?: string;
        data?: VisitDraftStructuredPayload;
        meta?: GatewayResponseMeta;
        done?: boolean;
        error?: string;
      };
      if (parsed.error) {
        throw new ApiClientError(parsed.error, 500);
      }
      if (typeof parsed.text === 'string') {
        latestText = parsed.text;
        onProgress?.(latestText);
      }
      if (parsed.data) finalData = parsed.data;
      if (parsed.meta) finalMeta = parsed.meta;
    }
  }

  if (!finalData || !finalMeta || !latestText.trim()) {
    throw new ApiClientError('پاسخ ساختاریافته معتبر برای خلاصه ویزیت دریافت نشد.', 502);
  }

  return {
    data: finalData,
    meta: finalMeta,
  };
}

export async function requestNutritionPlan(params: {
  ageMonths: number;
  weightKg: string;
  allergiesFa: string;
  dietGoalFa: string;
  childContextSummaryFa: string;
  signal?: AbortSignal;
}): Promise<{ plan: NutritionPlan; meta: GatewayResponseMeta; policyError?: string }> {
  const { signal, ...body } = params;
  const res = await fetch('/api/gemini/nutrition', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    signal,
    body: JSON.stringify(body),
  });

  const envelope = await parseEnvelopeResponse<RestrictedNutritionPayload>(res);
  return {
    plan: {
      id: `nut_${Date.now()}`,
      planTitleFa: envelope.data.planTitleFa,
      overviewFa: envelope.data.overviewFa,
      growthNutritionFocusFa: envelope.data.growthNutritionFocusFa,
      hydrationTipFa: envelope.data.hydrationTipFa,
      meals: Array.isArray(envelope.data.meals) ? envelope.data.meals : [],
      ageMonthsSnapshot: params.ageMonths,
      goalSnapshotFa: params.dietGoalFa || 'برنامه غذایی آموزشی متناسب با سن',
      createdAtLabelFa: 'هم‌اکنون',
    },
    meta: envelope.meta,
    policyError: envelope.policyError,
  };
}

export async function requestSyrupScan(params: {
  imageBase64: string;
  mimeType: string;
  childContextSummaryFa: string;
  signal?: AbortSignal;
}): Promise<GatewayClientResult<RestrictedSyrupScanPayload>> {
  const { signal, ...body } = params;
  const res = await fetch('/api/gemini/syrup-scan', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    signal,
    body: JSON.stringify(body),
  });
  return parseEnvelopeResponse<RestrictedSyrupScanPayload>(res);
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
  const { signal, ...body } = params;
  const res = await fetch('/api/gemini/safety-checklist', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    signal,
    body: JSON.stringify(body),
  });
  return parseEnvelopeResponse<RestrictedSafetyChecklistPayload>(res);
}

export async function requestDocumentedQa(params: {
  question: string;
  ageBandFa: string;
  matchedFaqs: FaqItem[];
  signal?: AbortSignal;
}): Promise<GatewayClientResult<RestrictedDocumentedQaPayload>> {
  const res = await fetch('/api/gemini/documented-qa', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    signal: params.signal,
    body: JSON.stringify({
      question: params.question,
      ageBandFa: params.ageBandFa,
      matchedFaqIds: params.matchedFaqs.slice(0, 4).map((faq) => faq.id),
    }),
  });
  return parseEnvelopeResponse<RestrictedDocumentedQaPayload>(res);
}
