import { parseClinicalDecimal, parseClinicalInteger } from './clinicalNumberInput.ts';
import { AGE_BANDS, type AlertSeverity, type ChildProfile } from './models.ts';
import { normalizePersian } from './persianNormalizer.ts';

export type LiquidFormulationKey =
  | 'ACETAMINOPHEN_DROPS_100'
  | 'ACETAMINOPHEN_SYRUP_120_5'
  | 'ACETAMINOPHEN_SYRUP_160_5'
  | 'IBUPROFEN_SYRUP_100_5'
  | 'CETIRIZINE_SYRUP_5_5';

export interface LiquidFormulation {
  key: LiquidFormulationKey;
  id: string;
  titleFa: string;
  shortLabelFa: string;
  concentrationLabelFa: string;
  mgPerMl: number;
  minHomeAgeMonths: number;
  maxSyringeScaleMl: number;
  stepMl: number;
  minMgPerKg: number;
  targetMgPerKg: number;
  maxMgPerKg: number;
  maxSingleDoseMg: number;
  intervalFa: string;
  safetyTipFa: string;
}

export const LIQUID_FORMULATIONS: Record<LiquidFormulationKey, LiquidFormulation> = {
  ACETAMINOPHEN_DROPS_100: {
    key: 'ACETAMINOPHEN_DROPS_100',
    id: 'acetaminophen_drops_100',
    titleFa: 'قطره استامینوفن کودکان (۱۰۰ mg/mL)',
    shortLabelFa: 'قطره استامینوفن ۱۰۰',
    concentrationLabelFa: '۱۰۰ میلی‌گرم در هر ۱ mL',
    mgPerMl: 100.0,
    minHomeAgeMonths: 3,
    maxSyringeScaleMl: 3.0,
    stepMl: 0.1,
    minMgPerKg: 10.0,
    targetMgPerKg: 12.5,
    maxMgPerKg: 15.0,
    maxSingleDoseMg: 500.0,
    intervalFa: 'هر ۴ تا ۶ ساعت در صورت نیاز (حداکثر ۵ نوبت در ۲۴ ساعت)',
    safetyTipFa:
      'این قطره ۱۰۰ mg/mL و شربت ۱۲۰/۵ معادل ۲۴ mg/mL است. هرگز قطره را با سرنگ بزرگ یا پیمانه شربت ندهید.',
  },
  ACETAMINOPHEN_SYRUP_120_5: {
    key: 'ACETAMINOPHEN_SYRUP_120_5',
    id: 'acetaminophen_syrup_120_5',
    titleFa: 'شربت استامینوفن (۱۲۰ mg / 5 mL)',
    shortLabelFa: 'شربت استامینوفن ۱۲۰/۵',
    concentrationLabelFa: '۲۴ میلی‌گرم در هر ۱ mL',
    mgPerMl: 24.0,
    minHomeAgeMonths: 3,
    maxSyringeScaleMl: 20.0,
    stepMl: 0.5,
    minMgPerKg: 10.0,
    targetMgPerKg: 12.5,
    maxMgPerKg: 15.0,
    maxSingleDoseMg: 500.0,
    intervalFa: 'هر ۴ تا ۶ ساعت در صورت نیاز (حداکثر ۵ نوبت در ۲۴ ساعت)',
    safetyTipFa: 'هم‌زمان با شربت‌های ترکیبی سرماخوردگی مصرف نشود تا خطر مسمومیت کبدی ایجاد نشود.',
  },
  ACETAMINOPHEN_SYRUP_160_5: {
    key: 'ACETAMINOPHEN_SYRUP_160_5',
    id: 'acetaminophen_syrup_160_5',
    titleFa: 'سوسپانسیون استامینوفن (۱۶۰ mg / 5 mL)',
    shortLabelFa: 'استامینوفن ۱۶۰/۵',
    concentrationLabelFa: '۳۲ میلی‌گرم در هر ۱ mL',
    mgPerMl: 32.0,
    minHomeAgeMonths: 3,
    maxSyringeScaleMl: 16.0,
    stepMl: 0.5,
    minMgPerKg: 10.0,
    targetMgPerKg: 12.5,
    maxMgPerKg: 15.0,
    maxSingleDoseMg: 500.0,
    intervalFa: 'هر ۴ تا ۶ ساعت در صورت نیاز (حداکثر ۵ نوبت در ۲۴ ساعت)',
    safetyTipFa: 'حتماً عدد روی جعبه را چک کنید که ۱۲۰/۵ است یا ۱۶۰/۵؛ حجم mL این دو متفاوت است.',
  },
  IBUPROFEN_SYRUP_100_5: {
    key: 'IBUPROFEN_SYRUP_100_5',
    id: 'ibuprofen_syrup_100_5',
    titleFa: 'سوسپانسیون ایبوپروفن (۱۰۰ mg / 5 mL)',
    shortLabelFa: 'ایبوپروفن ۱۰۰/۵',
    concentrationLabelFa: '۲۰ میلی‌گرم در هر ۱ mL',
    mgPerMl: 20.0,
    minHomeAgeMonths: 6,
    maxSyringeScaleMl: 20.0,
    stepMl: 0.5,
    minMgPerKg: 5.0,
    targetMgPerKg: 7.5,
    maxMgPerKg: 10.0,
    maxSingleDoseMg: 400.0,
    intervalFa: 'هر ۶ تا ۸ ساعت همراه کمی شیر یا غذا (حداکثر ۳ تا ۴ نوبت در ۲۴ ساعت)',
    safetyTipFa: 'شیشه را ۲۰ ثانیه کامل تکان دهید. در کودک زیر ۶ ماه یا دچار استفراغ/کم‌آبی مصرف نشود.',
  },
  CETIRIZINE_SYRUP_5_5: {
    key: 'CETIRIZINE_SYRUP_5_5',
    id: 'cetirizine_syrup_5_5',
    titleFa: 'شربت سیتیریزین (۵ mg / 5 mL)',
    shortLabelFa: 'سیتیریزین ۵/۵',
    concentrationLabelFa: '۱ میلی‌گرم در هر ۱ mL',
    mgPerMl: 1.0,
    minHomeAgeMonths: 24,
    maxSyringeScaleMl: 10.0,
    stepMl: 0.5,
    minMgPerKg: 0.0,
    targetMgPerKg: 0.0,
    maxMgPerKg: 0.0,
    maxSingleDoseMg: 5.0,
    intervalFa: 'معمولاً یک نوبت در ۲۴ ساعت (بر اساس سن و نظر پزشک/داروساز)',
    safetyTipFa:
      'دوز سیتیریزین بر پایه رده سنی تنظیم می‌شود، نه صرفاً وزن؛ در کودکان زیر ۲ سال بدون تجویز پزشک ندهید.',
  },
};

export const LIQUID_FORMULATION_LIST: LiquidFormulation[] = Object.values(LIQUID_FORMULATIONS);

export interface LiquidDoseVisualizationResult {
  formulation: LiquidFormulation;
  ageMonths: number;
  weightKg: number;
  isBlocked: boolean;
  recommendedMl: number;
  minSafeMl: number;
  maxSafeMl: number;
  recommendedMg: number;
  isWeightBased: boolean;
  alertSeverity: AlertSeverity;
  alertTitleFa: string;
  alertBodyFa: string;
}

export const MIN_WEIGHT_KG = 3.0;
export const MAX_WEIGHT_KG = 40.0;
export const MIN_AGE_MONTHS = 0;
export const MAX_AGE_MONTHS = 144;

export function formatAgeFa(ageMonths: number | null | undefined): string {
  if (ageMonths === null || ageMonths === undefined || ageMonths < 0) return 'سن ثبت نشده';
  const m = Math.min(MAX_AGE_MONTHS, Math.max(MIN_AGE_MONTHS, Math.round(ageMonths)));
  if (m === 0) return 'نوزاد تازه متولدشده';
  if (m < 12) return `${m} ماه`;
  if (m % 12 === 0) return `${m / 12} سال`;
  return `${Math.floor(m / 12)} سال و ${m % 12} ماه`;
}

export function formatMl(ml: number): string {
  return `${ml.toFixed(1)} mL`;
}

export function formatKg(kg: number | null | undefined): string {
  if (kg === null || kg === undefined || !Number.isFinite(kg)) return 'وزن ثبت نشده';
  return `${kg.toFixed(1)} kg`;
}

// Reject blank and internally spaced input instead of coercing or concatenating it.
export function parseLiquidDoseInputs(ageText: string, weightText: string): {
  ageMonths: number | null;
  weightKg: number | null;
} {
  const age = ageText.trim();
  const weight = weightText.trim();
  return {
    ageMonths: /\s/.test(age) ? null : parseClinicalInteger(age),
    weightKg: /\s/.test(weight) ? null : parseClinicalDecimal(weight),
  };
}

export function canRevealLiquidDose(result: LiquidDoseVisualizationResult, confirmed: boolean): boolean {
  return confirmed && !result.isBlocked;
}

export function evaluateLiquidDose(
  formulation: LiquidFormulation,
  ageMonths: number | null | undefined,
  weightKg: number | null | undefined,
): LiquidDoseVisualizationResult {
  const makeBlocked = (message: string): LiquidDoseVisualizationResult => ({
    formulation,
    ageMonths: ageMonths ?? -1,
    weightKg: weightKg ?? Number.NaN,
    isBlocked: true,
    recommendedMl: 0,
    minSafeMl: 0,
    maxSafeMl: 0,
    recommendedMg: 0,
    isWeightBased: formulation.key !== 'CETIRIZINE_SYRUP_5_5',
    alertSeverity: 'caution',
    alertTitleFa: 'محاسبه در دسترس نیست',
    alertBodyFa: message,
  });

  if (
    ageMonths === null ||
    ageMonths === undefined ||
    !Number.isFinite(ageMonths) ||
    ageMonths < MIN_AGE_MONTHS ||
    ageMonths > MAX_AGE_MONTHS ||
    weightKg === null ||
    weightKg === undefined ||
    !Number.isFinite(weightKg) ||
    weightKg < MIN_WEIGHT_KG ||
    weightKg > MAX_WEIGHT_KG
  ) {
    return makeBlocked(
      'سن واقعی به ماه و وزن اندازه‌گیری‌شده را وارد کنید. این ابزار برای خارج از محدودهٔ خود تخمین نمی‌زند.',
    );
  }

  const safeAge = ageMonths;
  const safeWeight = weightKg;

  // 1. Strict Newborn / Infant clinical safety gates
  if (safeAge < formulation.minHomeAgeMonths) {
    let severity: AlertSeverity = 'caution';
    let title = `نیازمند تجویز پزشک برای رده سنی زیر ${formulation.minHomeAgeMonths} ماه`;
    let body = `برای این رده سنی (${formatAgeFa(safeAge)})، مقدار دقیق ${formulation.shortLabelFa} باید مستقیماً توسط پزشک یا داروساز تعیین شود.`;

    if (safeAge < 3) {
      severity = 'danger';
      title = 'هشدار اورژانسی نوزاد زیر ۳ ماه — دوز خانگی نمایش داده نمی‌شود';
      body =
        'در نوزاد زیر ۳ ماه، تب ۳۸.۰ درجه یا نیاز به دارو نیازمند معاینه فوری پزشک است. سرنگ روی ۰.۰ mL قفل شده است؛ خودسرانه دارو ندهید.';
    } else if (formulation.key === 'IBUPROFEN_SYRUP_100_5' && safeAge < 6) {
      severity = 'danger';
      title = 'منع مصرف بدون نسخه ایبوپروفن در شیرخوار زیر ۶ ماه';
      body =
        'سوسپانسیون ایبوپروفن برای کودکان زیر ۶ ماه بدون دستور مستقیم پزشک مجاز نیست و سرنگ روی ۰.۰ mL قفل شده است.';
    }

    return {
      formulation,
      ageMonths: safeAge,
      weightKg: safeWeight,
      isBlocked: true,
      recommendedMl: 0,
      minSafeMl: 0,
      maxSafeMl: 0,
      recommendedMg: 0,
      isWeightBased: formulation.key !== 'CETIRIZINE_SYRUP_5_5',
      alertSeverity: severity,
      alertTitleFa: title,
      alertBodyFa: body,
    };
  }

  // 2. Age-banded non-mg/kg dosing (Cetirizine 5 mg / 5 mL)
  if (formulation.key === 'CETIRIZINE_SYRUP_5_5') {
    const ml = safeAge < 72 ? 2.5 : 5.0;
    const maxMl = safeAge < 72 ? 2.5 : 10.0;
    const mg = ml * formulation.mgPerMl;
    return {
      formulation,
      ageMonths: safeAge,
      weightKg: safeWeight,
      isBlocked: false,
      recommendedMl: ml,
      minSafeMl: ml,
      maxSafeMl: maxMl,
      recommendedMg: mg,
      isWeightBased: false,
      alertSeverity: 'info',
      alertTitleFa: `دوز سنی استاندارد (${formatAgeFa(safeAge)})`,
      alertBodyFa: `برای ${formatAgeFa(safeAge)}، دوز رایج ${formatMl(ml)} (${mg} mg) یک‌بار در روز است. برای بیش از ۲.۵ یا ۵ mL تنها با نظر پزشک اقدام کنید.`,
    };
  }

  // 3. Weight-based liquid dosing (Acetaminophen & Ibuprofen)
  const rawMinMg = Math.min(safeWeight * formulation.minMgPerKg, formulation.maxSingleDoseMg);
  const rawTargetMg = Math.min(safeWeight * formulation.targetMgPerKg, formulation.maxSingleDoseMg);
  const rawMaxMg = Math.min(safeWeight * formulation.maxMgPerKg, formulation.maxSingleDoseMg);

  const stepMg = formulation.stepMl * formulation.mgPerMl;
  const lowerTick = Math.ceil(rawMinMg / stepMg - 0.000001);
  const upperTick = Math.floor(rawMaxMg / stepMg + 0.000001);
  const targetTick = Math.round(rawTargetMg / stepMg);

  if (lowerTick > upperTick || rawTargetMg / formulation.mgPerMl > formulation.maxSyringeScaleMl) {
    return makeBlocked(
      'حجم هدف با درجه‌بندی یا ظرفیت این سرنگ سازگار نیست. برای انتخاب ابزار مناسب با داروساز مشورت کنید.',
    );
  }

  const clampedTargetTick = Math.min(upperTick, Math.max(lowerTick, targetTick));
  const round1 = (val: number) => Math.round(val * 10) / 10;
  const minMl = round1(lowerTick * formulation.stepMl);
  const targetMl = round1(clampedTargetTick * formulation.stepMl);
  const maxMl = round1(upperTick * formulation.stepMl);
  const actualMg = Math.round(targetMl * formulation.mgPerMl * 10) / 10;

  const dropsHighWeightCaution =
    formulation.key === 'ACETAMINOPHEN_DROPS_100' && safeWeight >= 14.0;

  return {
    formulation,
    ageMonths: safeAge,
    weightKg: safeWeight,
    isBlocked: false,
    recommendedMl: targetMl,
    minSafeMl: minMl,
    maxSafeMl: maxMl,
    recommendedMg: actualMg,
    isWeightBased: true,
    alertSeverity: dropsHighWeightCaution ? 'caution' : 'success',
    alertTitleFa: dropsHighWeightCaution
      ? `پیشنهاد شکل دارویی مناسب‌تر برای وزن ${formatKg(safeWeight)}`
      : `مقدار تخمینی آموزشی بر پایه وزن (${formatKg(safeWeight)}) و سن (${formatAgeFa(safeAge)})`,
    alertBodyFa: dropsHighWeightCaution
      ? `حجم قطره برای این وزن نسبتاً زیاد است (${formatMl(targetMl)}). معمولاً برای کودکان بالای ۱۲ تا ۱۴ کیلوگرم، شربت ۱۲۰/۵ یا ۱۶۰/۵ راحت‌تر و دقیق‌تر است. ${formulation.safetyTipFa}`
      : `بر پایه محدوده استاندارد ${Math.round(formulation.minMgPerKg)} تا ${Math.round(formulation.maxMgPerKg)} mg/kg، محدوده مجاز هر نوبت ${formatMl(minMl)} تا ${formatMl(maxMl)} است. پیش از کشیدن دارو، غلظت روی جعبه را تطبیق دهید.`,
  };
}

export function parseAgeMonthsFromText(raw: string): number | null {
  const clean = normalizePersian(raw);
  if (!clean) return null;
  if (AGE_BANDS.some((b) => normalizePersian(b.labelFa) === clean)) return null;
  if (/\d+\s*(?:تا|الی|-|–|to)\s*\d+/i.test(clean)) return null;
  const match = clean.match(/^(?:(\d{1,2})\s*سال)?(?:\s*(?:و\s*)?(\d{1,3})\s*ماه)?$/);
  if (!match || (!match[1] && !match[2])) return null;
  const years = match[1] ? Number.parseInt(match[1], 10) : 0;
  const months = match[2] ? Number.parseInt(match[2], 10) : 0;
  const total = 12 * years + months;
  return total >= MIN_AGE_MONTHS && total <= MAX_AGE_MONTHS ? total : null;
}

export function initialAgeMonthsFromProfile(
  profile: ChildProfile | null | undefined,
  birthDateEpochMs?: number | null,
  nowMs = Date.now(),
): number | null {
  const effectiveBirthMs = birthDateEpochMs ?? profile?.birthDateEpochMs ?? null;
  if (effectiveBirthMs && effectiveBirthMs > 0 && effectiveBirthMs <= nowMs) {
    const birth = new Date(effectiveBirthMs);
    const now = new Date(nowMs);
    let months =
      (now.getUTCFullYear() - birth.getUTCFullYear()) * 12 +
      (now.getUTCMonth() - birth.getUTCMonth());
    if (now.getUTCDate() < birth.getUTCDate()) {
      months -= 1;
    }
    if (months >= MIN_AGE_MONTHS && months <= MAX_AGE_MONTHS) {
      return months;
    }
  }
  const explicit = profile?.ageMonthsExact ?? parseAgeMonthsFromText(profile?.birthDateOrAgeFa ?? '');
  if (explicit !== null && explicit !== undefined && explicit >= MIN_AGE_MONTHS && explicit <= MAX_AGE_MONTHS) {
    return explicit;
  }
  return null;
}

export function initialWeightKgFromProfile(profile: ChildProfile | null | undefined): number | null {
  if (!profile?.weightKgText) return null;
  const parsed = parseClinicalDecimal(profile.weightKgText);
  if (parsed !== null && parsed >= MIN_WEIGHT_KG && parsed <= MAX_WEIGHT_KG) {
    return parsed;
  }
  return null;
}
