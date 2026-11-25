import type {
  DocumentedQaResponse,
  FaqItem,
  GeminiSyrupScanPayload,
  NutritionMealItem,
  NutritionPlan,
  SafetyChecklistReport,
} from '../domain/models.ts';

export interface HealthStatus {
  ok: boolean;
  hasGeminiKey: boolean;
  model: string;
}

export class ApiClientError extends Error {
  status: number;
  code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.code = code;
  }
}

async function parseErrorResponse(res: Response): Promise<never> {
  let msg = 'خطایی در ارتباط با سرور رخ داد. لطفاً دوباره تلاش کنید.';
  let code: string | undefined;
  try {
    const data = (await res.json()) as { error?: string; code?: string };
    if (data?.error) msg = data.error;
    if (data?.code) code = data.code;
  } catch {
    // fallback message
  }
  throw new ApiClientError(msg, res.status, code);
}

export async function fetchApiHealth(): Promise<HealthStatus> {
  const res = await fetch('/api/health');
  if (!res.ok) await parseErrorResponse(res);
  return (await res.json()) as HealthStatus;
}

export async function requestVisitDraftStream(
  promptText: string,
  onProgress?: (accumulatedText: string) => void,
): Promise<string> {
  const res = await fetch('/api/gemini/visit-draft', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ promptText, stream: true }),
  });

  if (!res.ok) {
    await parseErrorResponse(res);
  }

  const contentType = res.headers.get('content-type') || '';
  if (!contentType.includes('text/event-stream') || !res.body) {
    const json = (await res.json()) as { text?: string };
    return (json.text || '').trim();
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let latestText = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split('\n\n');
    buffer = events.pop() || '';
    for (const evt of events) {
      const line = evt
        .split('\n')
        .find((l) => l.startsWith('data:'));
      if (!line) continue;
      const jsonStr = line.slice(5).trim();
      if (!jsonStr) continue;
      const parsed = JSON.parse(jsonStr) as {
        delta?: string;
        text?: string;
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
    }
  }

  if (!latestText.trim()) {
    throw new ApiClientError('پاسخی برای خلاصه ویزیت دریافت نشد.', 502);
  }
  return latestText.trim();
}

export async function requestNutritionPlan(params: {
  ageMonths: number;
  weightKg: string;
  allergiesFa: string;
  dietGoalFa: string;
  childContextSummaryFa: string;
}): Promise<NutritionPlan> {
  const res = await fetch('/api/gemini/nutrition', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  });

  if (!res.ok) await parseErrorResponse(res);

  const raw = (await res.json()) as {
    planTitleFa: string;
    overviewFa: string;
    growthNutritionFocusFa: string;
    hydrationTipFa: string;
    meals: NutritionMealItem[];
  };

  return {
    id: `nut_${Date.now()}`,
    planTitleFa: raw.planTitleFa,
    overviewFa: raw.overviewFa,
    growthNutritionFocusFa: raw.growthNutritionFocusFa,
    hydrationTipFa: raw.hydrationTipFa,
    meals: Array.isArray(raw.meals) ? raw.meals : [],
    ageMonthsSnapshot: params.ageMonths,
    goalSnapshotFa: params.dietGoalFa || 'برنامه غذایی ایمن متناسب با سن',
    createdAtLabelFa: 'هم‌اکنون',
  };
}

export async function requestSyrupScan(params: {
  imageBase64: string;
  mimeType: string;
  childContextSummaryFa: string;
}): Promise<GeminiSyrupScanPayload> {
  const res = await fetch('/api/gemini/syrup-scan', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  });
  if (!res.ok) await parseErrorResponse(res);
  return (await res.json()) as GeminiSyrupScanPayload;
}

export async function requestSafetyChecklist(params: {
  flowTitleFa: string;
  urgencyLevel: string;
  evaluationSummaryFa: string;
  selectedSymptomsFa: string[];
  childContextSummaryFa: string;
}): Promise<SafetyChecklistReport> {
  const res = await fetch('/api/gemini/safety-checklist', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  });
  if (!res.ok) await parseErrorResponse(res);
  const report = (await res.json()) as SafetyChecklistReport;
  return {
    ...report,
    clinicalSourceCitationFa:
      'خروجی کمکی هوش مصنوعی بر اساس اطلاعات ثبت‌شده؛ جایگزین معاینه حضوری پزشک اطفال نیست.',
  };
}

export async function requestDocumentedQa(params: {
  question: string;
  ageBandFa: string;
  matchedFaqs: FaqItem[];
  sourceLookup: (id: string) => string;
}): Promise<DocumentedQaResponse> {
  const payloadFaqs = params.matchedFaqs.slice(0, 4).map((faq) => ({
    question: faq.questionFa,
    shortAnswer: faq.shortAnswerFa,
    clinicalDetail: faq.clinicalDetailFa,
    citation: faq.sourceIds.map(params.sourceLookup).filter(Boolean).join(' • '),
  }));

  const res = await fetch('/api/gemini/documented-qa', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      question: params.question,
      ageBandFa: params.ageBandFa,
      matchedFaqs: payloadFaqs,
    }),
  });
  if (!res.ok) await parseErrorResponse(res);
  const raw = (await res.json()) as DocumentedQaResponse;

  // Deterministic citation override grounded directly in matched FAQs
  const verifiedCitations = Array.from(
    new Set(
      params.matchedFaqs
        .slice(0, 4)
        .flatMap((faq) => faq.sourceIds.map(params.sourceLookup).filter(Boolean)),
    ),
  );

  return {
    ...raw,
    citedSourcesFa: verifiedCitations.length > 0 ? verifiedCitations : raw.citedSourcesFa,
    safetyDisclaimerFa:
      'این پاسخ کمکی صرفاً بر اساس مستندات داخلی مانو کید تنظیم شده و جایگزین ویزیت پزشک یا مشاوره مستقیم داروساز نیست.',
  };
}
