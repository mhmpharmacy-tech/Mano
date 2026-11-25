import {
  LIQUID_FORMULATION_LIST,
  evaluateLiquidDose,
  initialAgeMonthsFromProfile,
  initialWeightKgFromProfile,
  type LiquidDoseVisualizationResult,
  type LiquidFormulation,
} from './liquidDoseVisualizerEngine.ts';
import type {
  AlertSeverity,
  ChildProfile,
  GeminiSyrupScanPayload,
  SyrupDosageForm,
  SyrupIngredientKey,
  SyrupScanConfidence,
} from './models.ts';

export const CUSTOM_UNVERIFIED_ID = 'CUSTOM_UNVERIFIED';

export interface ReconciledSyrupExtraction {
  drugNameFa: string;
  ingredientKey: SyrupIngredientKey;
  dosageForm: SyrupDosageForm;
  mgAmount: number;
  perMlAmount: number;
  mgPerMl: number;
  matchedFormulation: LiquidFormulation | null;
  matchedFormulationIdRaw: string;
  confidence: SyrupScanConfidence;
  concentrationDisplayFa: string;
  safetyNoteFa: string;
  mixUpWarningFa: string | null;
  warningSeverity: AlertSeverity;
  canAutoFillSyringe: boolean;
  dosePreview: LiquidDoseVisualizationResult | null;
}

export function matchCalibratedFormulation(
  ingredientKey: SyrupIngredientKey,
  dosageForm: SyrupDosageForm,
  mgPerMl: number,
): LiquidFormulation | null {
  if (!Number.isFinite(mgPerMl) || mgPerMl <= 0 || dosageForm === 'UNKNOWN') return null;
  return (
    LIQUID_FORMULATION_LIST.find((formulation) => {
      let expectedIngredient: SyrupIngredientKey = 'ACETAMINOPHEN';
      if (formulation.key === 'IBUPROFEN_SYRUP_100_5') expectedIngredient = 'IBUPROFEN';
      if (formulation.key === 'CETIRIZINE_SYRUP_5_5') expectedIngredient = 'CETIRIZINE';

      let matchingForm = false;
      if (formulation.key === 'ACETAMINOPHEN_DROPS_100') {
        matchingForm = dosageForm === 'DROPS';
      } else if (formulation.key === 'CETIRIZINE_SYRUP_5_5') {
        matchingForm = dosageForm === 'SYRUP';
      } else {
        matchingForm = dosageForm === 'SYRUP' || dosageForm === 'SUSPENSION';
      }

      const tolerance = Math.max(0.02, formulation.mgPerMl * 0.005);
      return (
        ingredientKey === expectedIngredient &&
        matchingForm &&
        Math.abs(mgPerMl - formulation.mgPerMl) <= tolerance
      );
    }) ?? null
  );
}

export function defaultNumeratorMg(formulation: LiquidFormulation): number {
  switch (formulation.key) {
    case 'ACETAMINOPHEN_DROPS_100':
      return 100;
    case 'ACETAMINOPHEN_SYRUP_120_5':
      return 120;
    case 'ACETAMINOPHEN_SYRUP_160_5':
      return 160;
    case 'IBUPROFEN_SYRUP_100_5':
      return 100;
    case 'CETIRIZINE_SYRUP_5_5':
      return 5;
  }
}

export function defaultDenominatorMl(formulation: LiquidFormulation): number {
  return formulation.key === 'ACETAMINOPHEN_DROPS_100' ? 1 : 5;
}

export function formatConcentrationDisplay(mgAmount: number, perMlAmount: number): string {
  const mgStr = mgAmount % 1 === 0 ? String(mgAmount.toFixed(0)) : mgAmount.toFixed(1);
  const mlStr = perMlAmount % 1 === 0 ? String(perMlAmount.toFixed(0)) : perMlAmount.toFixed(1);
  return `${mgStr} mg / ${mlStr} mL`;
}

export function reconcileEditableSyrup(params: {
  drugNameFa: string;
  ingredientKey: SyrupIngredientKey;
  dosageForm: SyrupDosageForm;
  mgAmount: number;
  perMlAmount: number;
  confidence?: SyrupScanConfidence;
  aiSafetyNoteFa?: string;
  presetOverride?: LiquidFormulation | null;
  isPediatricFeverOrAllergyLiquid?: boolean;
  childProfile?: ChildProfile | null;
  birthDateEpochMs?: number | null;
}): ReconciledSyrupExtraction {
  const {
    drugNameFa,
    ingredientKey,
    dosageForm,
    mgAmount,
    perMlAmount,
    confidence = 'HIGH',
    aiSafetyNoteFa = '',
    presetOverride = null,
    isPediatricFeverOrAllergyLiquid = true,
    childProfile = null,
    birthDateEpochMs = null,
  } = params;

  const validConcentration =
    Number.isFinite(mgAmount) &&
    Number.isFinite(perMlAmount) &&
    mgAmount > 0 &&
    mgAmount <= 2000 &&
    perMlAmount > 0 &&
    perMlAmount <= 100;

  const cleanMg = validConcentration ? mgAmount : 0;
  const cleanPerMl = validConcentration ? perMlAmount : 0;
  const computedMgPerMl = validConcentration ? cleanMg / cleanPerMl : 0;

  const independentlyMatched = validConcentration
    ? matchCalibratedFormulation(ingredientKey, dosageForm, computedMgPerMl)
    : null;

  const matchedFormulation =
    independentlyMatched && (!presetOverride || presetOverride.id === independentlyMatched.id)
      ? independentlyMatched
      : null;

  const ageMonths = initialAgeMonthsFromProfile(childProfile, birthDateEpochMs);
  const weightKg = initialWeightKgFromProfile(childProfile);

  const dosePreview =
    matchedFormulation && isPediatricFeverOrAllergyLiquid
      ? evaluateLiquidDose(matchedFormulation, ageMonths, weightKg)
      : null;

  let mixUpWarningFa: string | null = null;
  if (
    matchedFormulation?.key === 'ACETAMINOPHEN_DROPS_100' ||
    (ingredientKey === 'ACETAMINOPHEN' && computedMgPerMl >= 80)
  ) {
    mixUpWarningFa =
      'هشدار تداخل غلظت قطره و شربت: این بطری غلظت بسیار بالا (~۱۰۰ mg/mL) دارد که حدود ۴ برابر شربت ۱۲۰/۵ است! فقط با قطره‌چکان یا سرنگ کوچک ۱ تا ۳ mL استفاده کنید.';
  } else if (matchedFormulation?.key === 'ACETAMINOPHEN_SYRUP_160_5') {
    mixUpWarningFa =
      'توجه به غلظت دوگانه: این بطری ۱۶۰ mg / 5 mL است (غلیظ‌تر از شربت متداول ۱۲۰/۵)؛ حجم سرنگ دقیقاً بر پایه ۱۶۰/۵ تنظیم می‌شود.';
  } else if (!matchedFormulation || !isPediatricFeverOrAllergyLiquid) {
    mixUpWarningFa =
      'ماده مؤثره، شکل دارویی یا غلظت با فهرست کالیبره مانو تطابق قطعی ندارد؛ محاسبه و پرکردن خودکار سرنگ مسدود است. برچسب را بررسی و با داروساز مشورت کنید.';
  } else if (dosageForm === 'SUSPENSION') {
    mixUpWarningFa =
      'سوسپانسیون خوراکی: پیش از کشیدن دارو با سرنگ، بطری را حداقل ۲۰ ثانیه کامل تکان دهید.';
  }

  let warningSeverity: AlertSeverity = 'success';
  if (dosePreview?.alertSeverity === 'danger') {
    warningSeverity = 'danger';
  } else if (
    matchedFormulation?.key === 'ACETAMINOPHEN_DROPS_100' ||
    (ingredientKey === 'ACETAMINOPHEN' && computedMgPerMl >= 80)
  ) {
    warningSeverity = 'danger';
  } else if (
    !matchedFormulation ||
    !isPediatricFeverOrAllergyLiquid ||
    matchedFormulation?.key === 'ACETAMINOPHEN_SYRUP_160_5' ||
    confidence === 'LOW'
  ) {
    warningSeverity = 'caution';
  }

  const safetyNoteFa = (
    aiSafetyNoteFa.trim() ||
    matchedFormulation?.safetyTipFa ||
    'پیش از مصرف، عدد روی برچسب اصلی بطری را دوباره با چشم خود تطبیق دهید.'
  ).slice(0, 400);

  const canAutoFillSyringe = Boolean(
    matchedFormulation &&
      validConcentration &&
      confidence === 'HIGH' &&
      dosePreview &&
      !dosePreview.isBlocked &&
      dosePreview.recommendedMl > 0,
  );

  return {
    drugNameFa: (drugNameFa.trim() || matchedFormulation?.shortLabelFa || 'داروی اسکن‌شده').slice(0, 140),
    ingredientKey,
    dosageForm,
    mgAmount: cleanMg,
    perMlAmount: cleanPerMl,
    mgPerMl: computedMgPerMl,
    matchedFormulation,
    matchedFormulationIdRaw: matchedFormulation?.id ?? CUSTOM_UNVERIFIED_ID,
    confidence,
    concentrationDisplayFa: formatConcentrationDisplay(cleanMg, cleanPerMl),
    safetyNoteFa,
    mixUpWarningFa,
    warningSeverity,
    canAutoFillSyringe,
    dosePreview,
  };
}

export function reconcileFromAiPayload(
  payload: GeminiSyrupScanPayload,
  childProfile?: ChildProfile | null,
  birthDateEpochMs?: number | null,
): ReconciledSyrupExtraction {
  const ingUpper = (payload.activeIngredientKey || '').toUpperCase();
  const ingredientKey: SyrupIngredientKey =
    ingUpper === 'ACETAMINOPHEN' || ingUpper === 'IBUPROFEN' || ingUpper === 'CETIRIZINE'
      ? ingUpper
      : 'UNKNOWN';

  const formUpper = (payload.dosageForm || '').toUpperCase();
  const dosageForm: SyrupDosageForm =
    formUpper === 'DROPS' || formUpper === 'SYRUP' || formUpper === 'SUSPENSION'
      ? formUpper
      : 'UNKNOWN';

  const confUpper = (payload.confidenceLevel || '').toUpperCase();
  const confidence: SyrupScanConfidence =
    confUpper === 'HIGH' || confUpper === 'MEDIUM' ? confUpper : 'LOW';

  const effectiveMg =
    Number.isFinite(payload.mgAmount) && payload.mgAmount > 0 && payload.mgAmount <= 2000
      ? payload.mgAmount
      : 0;
  const effectivePerMl =
    Number.isFinite(payload.perMlAmount) && payload.perMlAmount > 0 && payload.perMlAmount <= 100
      ? payload.perMlAmount
      : 0;

  const measuredRatio = effectiveMg > 0 && effectivePerMl > 0 ? effectiveMg / effectivePerMl : 0;
  const separatelyReadRatio =
    Number.isFinite(payload.mgPerMl) && payload.mgPerMl > 0 ? payload.mgPerMl : null;

  const inconsistentOcr =
    separatelyReadRatio !== null &&
    (measuredRatio <= 0 ||
      Math.abs(separatelyReadRatio - measuredRatio) > Math.max(0.02, measuredRatio * 0.005));

  const presetHint = LIQUID_FORMULATION_LIST.find((f) => f.id === payload.matchedFormulationId) ?? null;

  return reconcileEditableSyrup({
    drugNameFa: (payload.detectedDrugNameFa || '').trim() || 'داروی مایع اسکن‌شده',
    ingredientKey,
    dosageForm,
    mgAmount: effectiveMg,
    perMlAmount: effectivePerMl,
    confidence: inconsistentOcr ? 'LOW' : confidence,
    aiSafetyNoteFa: inconsistentOcr
      ? 'دو عدد غلظت خوانده‌شده از برچسب با هم تناقض دارند؛ ورود دستی و بررسی داروساز لازم است.'
      : payload.safetyNoteFa || '',
    presetOverride: presetHint,
    isPediatricFeverOrAllergyLiquid: Boolean(payload.isPediatricFeverOrAllergyLiquid || presetHint),
    childProfile,
    birthDateEpochMs,
  });
}
