import { ALL_SOURCES } from '../../src/data/sources.ts';
import { getSourceCitationLabel } from '../../src/data/contentRepository.ts';
import { LIQUID_FORMULATION_LIST } from '../../src/domain/liquidDoseVisualizerEngine.ts';
import { matchCalibratedFormulation } from '../../src/domain/syrupConcentrationMatcher.ts';
import type {
  FaqItem,
  SyrupDosageForm,
  SyrupIngredientKey,
  SyrupScanConfidence,
  TriageOutcome,
} from '../../src/domain/models.ts';
import type {
  AiProhibitedAction,
  AiVerificationState,
  RestrictedDocumentedQaPayload,
  RestrictedNutritionPayload,
  RestrictedSafetyChecklistPayload,
  RestrictedSyrupScanPayload,
  VisitDraftStructuredPayload,
} from '../../src/domain/aiGatewayContracts.ts';

export interface SemanticValidationResult<T> {
  valid: boolean;
  data?: T;
  violations: AiProhibitedAction[];
  notes: string[];
}

const CANONICAL_SOURCE_MAP = new Map(ALL_SOURCES.map((s) => [s.id, s]));
const CANONICAL_URL_SET = new Set(
  ALL_SOURCES.map((s) => s.url?.toLowerCase()).filter((u): u is string => Boolean(u)),
);

const DIAGNOSIS_PATTERNS: RegExp[] = [
  /تشخیص\s*(قطعی|نهایی|پزشکی)\s*(عبارت\s*است\s*از|این\s*است\s*که|:|می‌باشد)/i,
  /کودک\s*(شما\s*)?(قطعاً\s*)?(مبتلا\s*به|دچار)\s*(ذات‌الریه|پنومونی|آپاندیسیت|مننژیت|عفونت\s*باکتریایی|سپسیس|دیابت)\s*(است|می‌باشد)/i,
  /\b(definitive\s+diagnosis\s*:|diagnosed\s+with|the\s+child\s+has\s+(pneumonia|appendicitis|meningitis|sepsis|strep))\b/i,
];

const PRESCRIBING_PATTERNS: RegExp[] = [
  /(آموکسی‌سیلین|آزیترومایسین|سفیکسیم|کوآموکسی‌کلاو|پنی‌سیلین|دگزامتازون|پردنیزولون|سالبوتامول|دیفن‌هیدرامین|شربت\s*سرماخوردگی)\s*(را\s*)?(تجویز\s*می‌کنم|شروع\s*کنید|بدهید|مصرف\s*کند)/i,
  /(دوز\s*(مناسب|دقیق|پیشنهادی|نهایی)\s*(تجویز|تعیین)\s*شده|تجویز\s*دارو\s*:)/i,
  /هر\s*[۰-۹\d]+\s*ساعت\s*(یک\s*بار\s*)?[۰-۹\d٫.]+\s*(سی‌سی|میلی‌لیتر|میلی‌گرم|قطره|mL|mg)\s*(بدهید|بخورانید|تجویز)/i,
  /\b(i\s+prescribe|give\s+\d+(\.\d+)?\s*(mg|ml|drops)\s+every\s+\d+\s*hours?|start\s+amoxicillin|prescribed\s+dose\s*:)\b/i,
];

const URGENT_OVERRIDE_PATTERNS: RegExp[] = [
  /نیازی\s*به\s*(اورژانس|پزشک|مراجعه\s*فوری|ویزیت\s*حضوری)\s*نیست/i,
  /(اورژانس\s*لازم\s*نیست|جای\s*نگرانی\s*نیست.*در\s*خانه\s*(صبر|بمانید)|تب\s*نوزاد\s*زیر\s*۳\s*ماه\s*طبیعی\s*است)/i,
  /\b(no\s+emergency\s*(care\s+)?needed|do\s+not\s+go\s+to\s+the\s+er|wait\s+at\s+home\s+instead\s+of\s+er|safe\s+to\s+treat\s+at\s+home)\b/i,
];

const TREATMENT_PLAN_MUTATION_PATTERNS: RegExp[] = [
  /برنامه\s*درمانی\s*(کودک\s*)?(به‌صورت\s*خودکار\s*)?(تغییر|ویرایش|حذف|جایگزین|به‌روزرسانی)\s*(یافت|شد)/i,
  /(داروی\s*جدید\s*به\s*برنامه\s*درمانی\s*اضافه\s*شد|دوز\s*نسخه\s*تغییر\s*داده\s*شد)/i,
  /\b(automatically\s+(modified|updated|changed)\s+(the\s+)?treatment\s+plan|treatment\s+plan\s+overwritten)\b/i,
];

const FORBIDDEN_JSON_MUTATION_KEYS = new Set([
  'treatmentPlan',
  'updatedTreatmentPlan',
  'medicationsToAdd',
  'medicationsToRemove',
  'replaceMedications',
  'autoModifyTreatmentPlan',
  'doseOverride',
  'overrideUrgency',
]);

const UNDER_12M_FORBIDDEN_NUTRITION_TERMS = [
  'عسل',
  'شیر گاو',
  'انگور کامل',
  'آجیل کامل',
  'مغز کامل',
  'پاپ‌کورن',
  'ذرت بوداده',
  'سوسیس',
  'کالباس',
  'نمک افزوده',
  'شکر افزوده',
  'honey',
  "cow's milk",
  'whole grape',
  'whole nut',
];

const FABRICATED_CITATION_PATTERNS: RegExp[] = [
  /\bdoi\s*:\s*10\.\d{4,}/i,
  /\bpmid\s*:\s*\d{4,}/i,
  /\b(lancet|nejm|jama|bmj)\s*(20\d{2}|19\d{2})\b/i,
  /\b(nelson|uptodate|lexicomp)\s*(2[1-9]|3\d)th\b/i,
];

export function parseStrictJsonObject(rawText: string): {
  ok: boolean;
  value?: Record<string, unknown>;
  error?: string;
} {
  const trimmed = rawText.trim();
  if (!trimmed) {
    return { ok: false, error: 'EMPTY_RESPONSE' };
  }

  let candidate = trimmed;
  if (candidate.startsWith('```')) {
    const match = candidate.match(/^```(?:json)?\s*\n([\s\S]*?)\n```$/i);
    if (!match) {
      return { ok: false, error: 'UNCLOSED_CODE_FENCE_JSON' };
    }
    candidate = match[1].trim();
  }

  try {
    const parsed: unknown = JSON.parse(candidate);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return { ok: false, error: 'JSON_NOT_OBJECT' };
    }
    return { ok: true, value: parsed as Record<string, unknown> };
  } catch {
    return { ok: false, error: 'MALFORMED_JSON' };
  }
}

function toStringSafe(val: unknown, maxLen: number): string {
  return typeof val === 'string' ? val.trim().slice(0, maxLen) : '';
}

function toStringArraySafe(val: unknown, maxItems: number, maxLen: number): string[] {
  if (!Array.isArray(val)) return [];
  return val
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .map((item) => item.trim().slice(0, maxLen))
    .slice(0, maxItems);
}

function collectObjectStrings(obj: unknown, depth = 0): string[] {
  if (depth > 6 || obj === null || obj === undefined) return [];
  if (typeof obj === 'string') return [obj];
  if (Array.isArray(obj)) {
    return obj.flatMap((item) => collectObjectStrings(item, depth + 1));
  }
  if (typeof obj === 'object') {
    return Object.values(obj).flatMap((v) => collectObjectStrings(v, depth + 1));
  }
  return [];
}

function checkForbiddenMutationKeys(obj: unknown, depth = 0): boolean {
  if (depth > 6 || !obj || typeof obj !== 'object') return false;
  if (Array.isArray(obj)) {
    return obj.some((item) => checkForbiddenMutationKeys(item, depth + 1));
  }
  for (const [k, v] of Object.entries(obj)) {
    if (FORBIDDEN_JSON_MUTATION_KEYS.has(k)) return true;
    if (checkForbiddenMutationKeys(v, depth + 1)) return true;
  }
  return false;
}

function runUniversalClinicalChecks(params: {
  rawObj: Record<string, unknown>;
  isUrgentContext: boolean;
  allowedUrls?: Set<string>;
  allowedContextNumbers?: Set<string>;
  disallowNewDosingNumbers?: boolean;
}): { violations: AiProhibitedAction[]; notes: string[] } {
  const violations: AiProhibitedAction[] = [];
  const notes: string[] = [];

  if (checkForbiddenMutationKeys(params.rawObj)) {
    violations.push('AUTO_MODIFY_TREATMENT_PLAN');
    notes.push('forbidden_treatment_plan_mutation_key');
  }

  const allText = collectObjectStrings(params.rawObj).join('\n');

  if (DIAGNOSIS_PATTERNS.some((rx) => rx.test(allText))) {
    violations.push('INDEPENDENT_DIAGNOSIS');
    notes.push('prohibited_diagnosis_claim');
  }

  if (PRESCRIBING_PATTERNS.some((rx) => rx.test(allText))) {
    violations.push('INDEPENDENT_PRESCRIBING_OR_DOSE');
    notes.push('prohibited_prescribing_or_dose_directive');
  }

  if (params.isUrgentContext && URGENT_OVERRIDE_PATTERNS.some((rx) => rx.test(allText))) {
    violations.push('OVERRIDE_URGENT_ESCALATION');
    notes.push('attempted_urgent_escalation_override');
  }

  if (TREATMENT_PLAN_MUTATION_PATTERNS.some((rx) => rx.test(allText))) {
    violations.push('AUTO_MODIFY_TREATMENT_PLAN');
    notes.push('prohibited_treatment_plan_mutation_claim');
  }

  // Check URLs inside output text — never allow invented/unverified external URLs
  const urlsInOutput = allText.match(/https?:\/\/[^\s"'<>)]+/gi) ?? [];
  for (const url of urlsInOutput) {
    const cleanUrl = url.replace(/[.,;!?]+$/, '').toLowerCase();
    const allowedSet = params.allowedUrls ?? CANONICAL_URL_SET;
    if (!allowedSet.has(cleanUrl)) {
      violations.push('FABRICATE_REFERENCES');
      notes.push(`unverified_url:${cleanUrl.slice(0, 60)}`);
    }
  }

  if (FABRICATED_CITATION_PATTERNS.some((rx) => rx.test(allText))) {
    violations.push('FABRICATE_REFERENCES');
    notes.push('fabricated_academic_citation_pattern');
  }

  // Check ungrounded mg/mL dosing claims when strict grounding is active
  if (params.disallowNewDosingNumbers && params.allowedContextNumbers) {
    const doseMatches =
      allText.match(/([۰-۹\d]+(?:[٫.][۰-۹\d]+)?)\s*(?:mg\/kg|mg\/ml|mg|ml|سی‌سی|میلی‌گرم|میلی‌لیتر)/gi) ??
      [];
    for (const match of doseMatches) {
      const numMatch = match.match(/[۰-۹\d]+(?:[٫.][۰-۹\d]+)?/);
      if (!numMatch) continue;
      const asciiNum = normalizeDigitsForCompare(numMatch[0]);
      if (!params.allowedContextNumbers.has(asciiNum)) {
        violations.push('INDEPENDENT_PRESCRIBING_OR_DOSE');
        notes.push(`ungrounded_dose_number:${asciiNum}`);
      }
    }
  }

  return {
    violations: [...new Set(violations)],
    notes,
  };
}

function normalizeDigitsForCompare(raw: string): string {
  const persian = '۰۱۲۳۴۵۶۷۸۹';
  const arabic = '٠١٢٣٤٥٦٧٨٩';
  return raw
    .replace(/[۰-۹]/g, (d) => String(persian.indexOf(d)))
    .replace(/[٠-٩]/g, (d) => String(arabic.indexOf(d)))
    .replace(/٫/g, '.');
}

export function extractAllowedNumbersFromText(text: string): Set<string> {
  const matches = normalizeDigitsForCompare(text).match(/\d+(?:\.\d+)?/g) ?? [];
  return new Set(matches);
}

export function validateVisitDraftModelOutput(params: {
  rawText: string;
  isUrgentContext: boolean;
  sourceInputText: string;
}): SemanticValidationResult<VisitDraftStructuredPayload> {
  const parsed = parseStrictJsonObject(params.rawText);
  if (!parsed.ok || !parsed.value) {
    return {
      valid: false,
      violations: ['MALFORMED_OUTPUT'],
      notes: [parsed.error || 'MALFORMED_JSON'],
    };
  }

  const obj = parsed.value;
  const universal = runUniversalClinicalChecks({
    rawObj: obj,
    isUrgentContext: params.isUrgentContext,
    allowedContextNumbers: extractAllowedNumbersFromText(params.sourceInputText),
    disallowNewDosingNumbers: true,
  });

  if (universal.violations.length > 0) {
    return { valid: false, violations: universal.violations, notes: universal.notes };
  }

  const childAndReasonSummaryFa = toStringSafe(obj.childAndReasonSummaryFa, 800);
  const temperatureTrend24hFa = toStringSafe(obj.temperatureTrend24hFa, 800);
  const medicationHistory24hFa = toStringSafe(obj.medicationHistory24hFa, 800);
  const observedSymptomsAndFlagsFa = toStringSafe(obj.observedSymptomsAndFlagsFa, 800);
  const questionsForClinicianFa = toStringArraySafe(obj.questionsForClinicianFa, 8, 250);
  const missingInformationFlagsFa = toStringArraySafe(obj.missingInformationFlagsFa, 8, 200);

  if (
    !childAndReasonSummaryFa ||
    !temperatureTrend24hFa ||
    !medicationHistory24hFa ||
    !observedSymptomsAndFlagsFa
  ) {
    return {
      valid: false,
      violations: ['MALFORMED_OUTPUT'],
      notes: ['missing_required_visit_draft_sections'],
    };
  }

  const titleFa = toStringSafe(obj.titleFa, 140) || 'خلاصه ساختاریافته آماده ویزیت پزشک';
  const hasMissing =
    missingInformationFlagsFa.length > 0 ||
    [childAndReasonSummaryFa, temperatureTrend24hFa, medicationHistory24hFa, observedSymptomsAndFlagsFa].some(
      (s) => s.includes('UNKNOWN') || s.includes('REQUIRE_REVIEW'),
    );
  const verificationState: AiVerificationState = hasMissing ? 'REQUIRE_REVIEW' : 'ASSISTANT_DRAFT';

  const formattedDraftFa = [
    `۱. خلاصه مشخصات و علت مراجعه:`,
    childAndReasonSummaryFa,
    '',
    `۲. روند ۲۴ ساعته تب و دماهای ثبت‌شده:`,
    temperatureTrend24hFa,
    '',
    `۳. داروهای مصرف‌شده در ۲۴ ساعت گذشته (گزارش والد):`,
    medicationHistory24hFa,
    '',
    `۴. علائم بررسی‌شده و موارد نیازمند توجه:`,
    observedSymptomsAndFlagsFa,
    '',
    `۵. پرسش‌های پیشنهادی برای پزشک یا داروساز:`,
    ...(questionsForClinicianFa.length > 0
      ? questionsForClinicianFa.map((q) => `• ${q}`)
      : ['• ارزیابی نهایی توسط پزشک یا داروساز انجام شود.']),
    ...(missingInformationFlagsFa.length > 0
      ? ['', 'موارد نامشخص / نیازمند بررسی (UNKNOWN / REQUIRE_REVIEW):', ...missingInformationFlagsFa.map((m) => `• ${m}`)]
      : []),
  ].join('\n');

  return {
    valid: true,
    violations: [],
    notes: [],
    data: {
      titleFa,
      childAndReasonSummaryFa,
      temperatureTrend24hFa,
      medicationHistory24hFa,
      observedSymptomsAndFlagsFa,
      questionsForClinicianFa,
      missingInformationFlagsFa,
      verificationState,
      formattedDraftFa,
    },
  };
}

export function validateSyrupScanModelOutput(params: {
  rawText: string;
}): SemanticValidationResult<RestrictedSyrupScanPayload> {
  const parsed = parseStrictJsonObject(params.rawText);
  if (!parsed.ok || !parsed.value) {
    return {
      valid: false,
      violations: ['MALFORMED_OUTPUT'],
      notes: [parsed.error || 'MALFORMED_JSON'],
    };
  }

  const obj = parsed.value;
  const universal = runUniversalClinicalChecks({
    rawObj: obj,
    isUrgentContext: false,
    disallowNewDosingNumbers: false,
  });

  if (universal.violations.length > 0) {
    return { valid: false, violations: universal.violations, notes: universal.notes };
  }

  const rawIng = toStringSafe(obj.activeIngredientKey, 40).toUpperCase();
  const activeIngredientKey: SyrupIngredientKey =
    rawIng === 'ACETAMINOPHEN' || rawIng === 'IBUPROFEN' || rawIng === 'CETIRIZINE'
      ? rawIng
      : 'UNKNOWN';

  const rawForm = toStringSafe(obj.dosageForm, 40).toUpperCase();
  const dosageForm: SyrupDosageForm =
    rawForm === 'DROPS' || rawForm === 'SYRUP' || rawForm === 'SUSPENSION' ? rawForm : 'UNKNOWN';

  const rawConf = toStringSafe(obj.confidenceLevel, 20).toUpperCase();
  const rawMatchedId = toStringSafe(obj.matchedFormulationId, 80) || 'CUSTOM_UNVERIFIED';

  const mgAmount = typeof obj.mgAmount === 'number' && Number.isFinite(obj.mgAmount) ? obj.mgAmount : 0;
  const perMlAmount =
    typeof obj.perMlAmount === 'number' && Number.isFinite(obj.perMlAmount) ? obj.perMlAmount : 0;
  const mgPerMl = typeof obj.mgPerMl === 'number' && Number.isFinite(obj.mgPerMl) ? obj.mgPerMl : 0;

  const missingFields: string[] = [];
  if (activeIngredientKey === 'UNKNOWN') missingFields.push('activeIngredientKey');
  if (dosageForm === 'UNKNOWN') missingFields.push('dosageForm');
  if (mgAmount <= 0 || mgAmount > 2000) missingFields.push('mgAmount');
  if (perMlAmount <= 0 || perMlAmount > 100) missingFields.push('perMlAmount');
  if (mgPerMl <= 0) missingFields.push('mgPerMl');

  // Check mathematical contradiction between mgAmount / perMlAmount and mgPerMl
  const computedRatio = mgAmount > 0 && perMlAmount > 0 ? mgAmount / perMlAmount : 0;
  const mathContradiction =
    computedRatio <= 0 ||
    mgPerMl <= 0 ||
    Math.abs(mgPerMl - computedRatio) > Math.max(0.02, computedRatio * 0.01);

  const deterministicMatch =
    !mathContradiction && activeIngredientKey !== 'UNKNOWN' && dosageForm !== 'UNKNOWN'
      ? matchCalibratedFormulation(activeIngredientKey, dosageForm, computedRatio)
      : null;

  const allowedFormulationIds = new Set([...LIQUID_FORMULATION_LIST.map((f) => f.id), 'CUSTOM_UNVERIFIED']);
  const unknownFormulationId = !allowedFormulationIds.has(rawMatchedId);
  const formulationIdContradiction =
    rawMatchedId !== 'CUSTOM_UNVERIFIED' &&
    (!deterministicMatch || deterministicMatch.id !== rawMatchedId);

  const isFeverOrAllergy = obj.isPediatricFeverOrAllergyLiquid === true;
  const highConfidenceOnUnknown =
    rawConf === 'HIGH' &&
    (missingFields.length > 0 ||
      mathContradiction ||
      !deterministicMatch ||
      !isFeverOrAllergy ||
      formulationIdContradiction);

  if (mathContradiction || unknownFormulationId || formulationIdContradiction || highConfidenceOnUnknown) {
    return {
      valid: false,
      violations: ['CONTRADICTORY_OUTPUT'],
      notes: [
        mathContradiction ? 'mgPerMl_ratio_contradiction' : '',
        unknownFormulationId ? 'unknown_formulation_id' : '',
        formulationIdContradiction ? 'formulation_id_mismatch' : '',
        highConfidenceOnUnknown ? 'high_confidence_with_missing_or_unverified_fields' : '',
      ].filter(Boolean),
    };
  }

  const confidenceLevel: SyrupScanConfidence =
    rawConf === 'HIGH' || rawConf === 'MEDIUM' ? rawConf : 'LOW';
  const resolvedDrugName =
    toStringSafe(obj.detectedDrugNameFa, 140) || 'UNKNOWN — نام دارو خوانا نیست';

  const needsReview =
    missingFields.length > 0 ||
    confidenceLevel !== 'HIGH' ||
    !deterministicMatch ||
    !isFeverOrAllergy;

  const verificationState: AiVerificationState =
    missingFields.length >= 3 ? 'UNKNOWN' : needsReview ? 'REQUIRE_REVIEW' : 'ASSISTANT_DRAFT';

  const concentrationDisplayFa =
    computedRatio > 0
      ? `${mgAmount} mg / ${perMlAmount} mL`
      : toStringSafe(obj.concentrationDisplayFa, 80) || 'UNKNOWN';

  return {
    valid: true,
    violations: [],
    notes: [],
    data: {
      detectedDrugNameFa: resolvedDrugName,
      activeIngredientKey,
      dosageForm,
      mgAmount: mgAmount > 0 ? mgAmount : 0,
      perMlAmount: perMlAmount > 0 ? perMlAmount : 0,
      mgPerMl: computedRatio,
      matchedFormulationId: deterministicMatch ? deterministicMatch.id : 'CUSTOM_UNVERIFIED',
      confidenceLevel: needsReview && confidenceLevel === 'HIGH' ? 'LOW' : confidenceLevel,
      concentrationDisplayFa,
      safetyNoteFa:
        toStringSafe(obj.safetyNoteFa, 320) ||
        'استخراج کمکی برچسب — پیش از هر مصرف، عدد روی بطری را شخصاً بررسی کنید (REQUIRE_REVIEW).',
      isPediatricFeverOrAllergyLiquid: Boolean(isFeverOrAllergy && deterministicMatch),
      verificationState,
      missingFields,
      requiresManualConfirmation: true,
    },
  };
}

export function validateSafetyChecklistModelOutput(params: {
  rawText: string;
  urgencyLevel: TriageOutcome;
  allowedSourceIds: string[];
  allowedContextText: string;
}): SemanticValidationResult<RestrictedSafetyChecklistPayload> {
  const parsed = parseStrictJsonObject(params.rawText);
  if (!parsed.ok || !parsed.value) {
    return {
      valid: false,
      violations: ['MALFORMED_OUTPUT'],
      notes: [parsed.error || 'MALFORMED_JSON'],
    };
  }

  const obj = parsed.value;
  const isUrgent = params.urgencyLevel === 'URGENT_EMERGENCY';
  const isTimely = params.urgencyLevel === 'NEEDS_TIMELY_REVIEW';

  const universal = runUniversalClinicalChecks({
    rawObj: obj,
    isUrgentContext: isUrgent || isTimely,
    allowedContextNumbers: extractAllowedNumbersFromText(params.allowedContextText),
    disallowNewDosingNumbers: true,
  });

  if (universal.violations.length > 0) {
    return { valid: false, violations: universal.violations, notes: universal.notes };
  }

  const checklistTitleFa = toStringSafe(obj.checklistTitleFa, 160);
  const urgencySummaryFa = toStringSafe(obj.urgencySummaryFa, 600);
  const immediateEmergencySignsFa = toStringArraySafe(obj.immediateEmergencySignsFa, 8, 220);
  const sameDayPediatricianVisitSignsFa = toStringArraySafe(
    obj.sameDayPediatricianVisitSignsFa,
    8,
    220,
  );
  const homeMonitoringChecklistFa = toStringArraySafe(obj.homeMonitoringChecklistFa, 8, 220);
  const questionsForPediatricianFa = toStringArraySafe(obj.questionsForPediatricianFa, 8, 220);
  const rawCitedIds = toStringArraySafe(obj.citedSourceIds, 8, 60);

  if (!checklistTitleFa || !urgencySummaryFa || immediateEmergencySignsFa.length === 0) {
    return {
      valid: false,
      violations: ['MALFORMED_OUTPUT'],
      notes: ['missing_required_checklist_fields'],
    };
  }

  const allowedSet = new Set(params.allowedSourceIds);
  const invalidIds = rawCitedIds.filter((id) => !CANONICAL_SOURCE_MAP.has(id) || (allowedSet.size > 0 && !allowedSet.has(id)));
  if (invalidIds.length > 0) {
    return {
      valid: false,
      violations: ['FABRICATE_REFERENCES'],
      notes: [`ungrounded_source_ids:${invalidIds.join(',')}`],
    };
  }

  const resolvedSourceIds =
    rawCitedIds.length > 0 ? rawCitedIds : params.allowedSourceIds.slice(0, 4);

  return {
    valid: true,
    violations: [],
    notes: [],
    data: {
      checklistTitleFa,
      urgencySummaryFa,
      immediateEmergencySignsFa,
      sameDayPediatricianVisitSignsFa,
      homeMonitoringChecklistFa,
      questionsForPediatricianFa,
      clinicalSourceCitationFa: `توضیح دستیار هوشمند بر پایه خروجی قطعی ارزیابی علائم (${resolvedSourceIds.map(getSourceCitationLabel).join(' • ')})؛ جایگزین ویزیت پزشک نیست.`,
      verificationState: 'ASSISTANT_DRAFT',
      preservedUrgencyLevel: params.urgencyLevel,
      citedSourceIds: resolvedSourceIds,
    },
  };
}

export function validateDocumentedQaModelOutput(params: {
  rawText: string;
  matchedFaqs: FaqItem[];
}): SemanticValidationResult<RestrictedDocumentedQaPayload> {
  const parsed = parseStrictJsonObject(params.rawText);
  if (!parsed.ok || !parsed.value) {
    return {
      valid: false,
      violations: ['MALFORMED_OUTPUT'],
      notes: [parsed.error || 'MALFORMED_JSON'],
    };
  }

  const obj = parsed.value;
  const canonicalFaqText = params.matchedFaqs
    .map((f) => [f.questionFa, f.shortAnswerFa, f.clinicalDetailFa, ...f.whatToDoFa, ...f.whenToSeeDoctorFa].join(' '))
    .join('\n');

  const allowedSourceIds = Array.from(
    new Set(params.matchedFaqs.flatMap((f) => f.sourceIds).filter((id) => CANONICAL_SOURCE_MAP.has(id))),
  );
  const allowedUrls = new Set(
    allowedSourceIds
      .map((id) => CANONICAL_SOURCE_MAP.get(id)?.url?.toLowerCase())
      .filter((u): u is string => Boolean(u)),
  );

  const hasHighRiskFaq = params.matchedFaqs.some((f) => f.isHighRisk);
  const universal = runUniversalClinicalChecks({
    rawObj: obj,
    isUrgentContext: hasHighRiskFaq,
    allowedUrls,
    allowedContextNumbers: extractAllowedNumbersFromText(canonicalFaqText),
    disallowNewDosingNumbers: true,
  });

  if (universal.violations.length > 0) {
    return { valid: false, violations: universal.violations, notes: universal.notes };
  }

  const rawCitedIds = toStringArraySafe(obj.citedSourceIds, 8, 60);
  const allowedSourceSet = new Set(allowedSourceIds);
  const invalidIds = rawCitedIds.filter((id) => !allowedSourceSet.has(id));
  if (invalidIds.length > 0) {
    return {
      valid: false,
      violations: ['FABRICATE_REFERENCES'],
      notes: [`unverified_faq_source_ids:${invalidIds.join(',')}`],
    };
  }

  const directAnswerFa = toStringSafe(obj.directAnswerFa, 900);
  const actionableStepsFa = toStringArraySafe(obj.actionableStepsFa, 6, 240);
  const whenToVisitPediatricianFa = toStringArraySafe(obj.whenToVisitPediatricianFa, 6, 240);
  const uncertaintyStatus = toStringSafe(obj.uncertaintyStatus, 30).toUpperCase();

  if (!directAnswerFa) {
    return {
      valid: false,
      violations: ['MALFORMED_OUTPUT'],
      notes: ['empty_direct_answer'],
    };
  }

  const verifiedIds = rawCitedIds.length > 0 ? rawCitedIds : allowedSourceIds;
  const verifiedLabels = verifiedIds.map(getSourceCitationLabel).filter(Boolean);

  const verificationState: AiVerificationState =
    uncertaintyStatus === 'UNKNOWN'
      ? 'UNKNOWN'
      : uncertaintyStatus === 'REQUIRE_REVIEW'
        ? 'REQUIRE_REVIEW'
        : 'ASSISTANT_DRAFT';

  return {
    valid: true,
    violations: [],
    notes: [],
    data: {
      directAnswerFa,
      actionableStepsFa,
      whenToVisitPediatricianFa,
      citedSourcesFa: verifiedLabels,
      citedSourceIds: verifiedIds,
      matchedFaqIds: params.matchedFaqs.map((f) => f.id),
      safetyDisclaimerFa:
        'این پاسخ صرفاً ساده‌سازی مستندات مرور‌شده مانو کید است و جایگزین ویزیت پزشک یا مشاوره داروساز نیست.',
      verificationState,
    },
  };
}

export function validateNutritionModelOutput(params: {
  rawText: string;
  ageMonths: number;
}): SemanticValidationResult<RestrictedNutritionPayload> {
  const parsed = parseStrictJsonObject(params.rawText);
  if (!parsed.ok || !parsed.value) {
    return {
      valid: false,
      violations: ['MALFORMED_OUTPUT'],
      notes: [parsed.error || 'MALFORMED_JSON'],
    };
  }

  const obj = parsed.value;
  const universal = runUniversalClinicalChecks({
    rawObj: obj,
    isUrgentContext: params.ageMonths < 6,
    disallowNewDosingNumbers: true,
    allowedContextNumbers: new Set([String(params.ageMonths), '6', '12']),
  });

  if (universal.violations.length > 0) {
    return { valid: false, violations: universal.violations, notes: universal.notes };
  }

  const planTitleFa = toStringSafe(obj.planTitleFa, 140);
  const overviewFa = toStringSafe(obj.overviewFa, 600);
  const growthNutritionFocusFa = toStringSafe(obj.growthNutritionFocusFa, 400);
  const hydrationTipFa = toStringSafe(obj.hydrationTipFa, 400);
  const rawMeals = Array.isArray(obj.meals) ? obj.meals : [];

  if (!planTitleFa || !overviewFa || rawMeals.length === 0) {
    return {
      valid: false,
      violations: ['MALFORMED_OUTPUT'],
      notes: ['missing_required_nutrition_fields'],
    };
  }

  const validMeals = rawMeals
    .slice(0, 5)
    .map((m) => {
      if (!m || typeof m !== 'object' || Array.isArray(m)) return null;
      const rec = m as Record<string, unknown>;
      const mealTypeFa = toStringSafe(rec.mealTypeFa, 80);
      const titleFa = toStringSafe(rec.titleFa, 120);
      const whySuitableFa = toStringSafe(rec.whySuitableFa, 300);
      const textureAndPortionFa = toStringSafe(rec.textureAndPortionFa, 300);
      const ingredientsFa = toStringArraySafe(rec.ingredientsFa, 12, 80);
      const prepStepsFa = toStringArraySafe(rec.prepStepsFa, 8, 220);
      const safetyAvoidListFa = toStringArraySafe(rec.safetyAvoidListFa, 8, 80);
      const doctorFollowUpHintFa = toStringSafe(rec.doctorFollowUpHintFa, 220);
      if (!mealTypeFa || !titleFa || ingredientsFa.length === 0 || prepStepsFa.length === 0) {
        return null;
      }
      return {
        mealTypeFa,
        titleFa,
        whySuitableFa,
        textureAndPortionFa,
        ingredientsFa,
        prepStepsFa,
        safetyAvoidListFa,
        doctorFollowUpHintFa,
      };
    })
    .filter((m): m is NonNullable<typeof m> => m !== null);

  if (validMeals.length === 0) {
    return {
      valid: false,
      violations: ['MALFORMED_OUTPUT'],
      notes: ['no_valid_structured_meals'],
    };
  }

  if (params.ageMonths < 12) {
    const mealText = validMeals
      .map((m) => [m.titleFa, m.whySuitableFa, m.textureAndPortionFa, ...m.ingredientsFa, ...m.prepStepsFa].join(' '))
      .join(' ')
      .toLowerCase();
    const violatedTerm = UNDER_12M_FORBIDDEN_NUTRITION_TERMS.find((t) => mealText.includes(t.toLowerCase()));
    if (violatedTerm) {
      return {
        valid: false,
        violations: ['CONTRADICTORY_OUTPUT'],
        notes: [`under_12m_forbidden_ingredient:${violatedTerm}`],
      };
    }
  }

  const rawCitedIds = toStringArraySafe(obj.citedSourceIds, 4, 60);
  const allowedNutritionSourceIds = new Set(['nelson8_growth', 'who_growth']);
  if (rawCitedIds.some((id) => !allowedNutritionSourceIds.has(id))) {
    return {
      valid: false,
      violations: ['FABRICATE_REFERENCES'],
      notes: ['unverified_nutrition_source_id'],
    };
  }
  const citedSourceIds = rawCitedIds.length > 0 ? rawCitedIds : ['nelson8_growth', 'who_growth'];

  return {
    valid: true,
    violations: [],
    notes: [],
    data: {
      planTitleFa,
      overviewFa,
      growthNutritionFocusFa,
      hydrationTipFa,
      meals: validMeals,
      verificationState: 'ASSISTANT_DRAFT',
      citedSourceIds,
      citedSourcesFa: citedSourceIds.map(getSourceCitationLabel),
      safetyDisclaimerFa:
        'موارد بالا صرفاً نمونه‌های آموزشی عمومی برای تغذیه تکمیلی هستند و جایگزین توصیه پزشک کودک نیستند.',
    },
  };
}
