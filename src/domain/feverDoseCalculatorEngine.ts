import {
  LIQUID_FORMULATIONS,
  evaluateLiquidDose,
  type LiquidFormulation,
} from './liquidDoseVisualizerEngine.ts';
import { toPersianDigits } from './persianNormalizer.ts';

export type FeverAgeGroupId = 'UNDER_3_MONTHS' | 'THREE_TO_SIX_MONTHS' | 'OVER_6_MONTHS';

export interface FeverAgeGroupOption {
  id: FeverAgeGroupId;
  labelFa: string;
  subtitleFa: string;
  representativeAgeMonths: number;
}

export const FEVER_AGE_GROUPS: FeverAgeGroupOption[] = [
  {
    id: 'UNDER_3_MONTHS',
    labelFa: 'زیر ۳ ماه',
    subtitleFa: 'نوزاد و شیرخوار بسیار کوچک',
    representativeAgeMonths: 2,
  },
  {
    id: 'THREE_TO_SIX_MONTHS',
    labelFa: '۳ تا ۶ ماه',
    subtitleFa: 'فقط استامینوفن مجاز است',
    representativeAgeMonths: 4,
  },
  {
    id: 'OVER_6_MONTHS',
    labelFa: 'بالای ۶ ماه',
    subtitleFa: 'استامینوفن یا ایبوپروفن بر اساس وزن',
    representativeAgeMonths: 18,
  },
];

export type FeverPriorDoseStatusId =
  | 'NONE_IN_6H'
  | 'ACETAMINOPHEN_UNDER_4H'
  | 'ACETAMINOPHEN_4_TO_6H'
  | 'IBUPROFEN_UNDER_4H'
  | 'IBUPROFEN_4_TO_6H';

export interface FeverPriorDoseOption {
  id: FeverPriorDoseStatusId;
  labelFa: string;
}

export const FEVER_PRIOR_DOSE_OPTIONS: FeverPriorDoseOption[] = [
  { id: 'NONE_IN_6H', labelFa: 'خیر، در ۶ ساعت گذشته تب‌بر نداده‌ام' },
  { id: 'ACETAMINOPHEN_UNDER_4H', labelFa: 'بله، استامینوفن (کمتر از ۴ ساعت پیش)' },
  { id: 'ACETAMINOPHEN_4_TO_6H', labelFa: 'بله، استامینوفن (۴ تا ۶ ساعت پیش)' },
  { id: 'IBUPROFEN_UNDER_4H', labelFa: 'بله، ایبوپروفن (کمتر از ۴ ساعت پیش)' },
  { id: 'IBUPROFEN_4_TO_6H', labelFa: 'بله، ایبوپروفن (۴ تا ۶ ساعت پیش)' },
];

export interface FeverMedicineOption {
  formulation: LiquidFormulation;
  titleFa: string;
  localBrandsHintFa: string;
  boxStrengthBadgeFa: string;
  confirmationPromptFa: string;
  isDrops: boolean;
  isIbuprofen: boolean;
}

export const IRAN_FEVER_MEDICINES: FeverMedicineOption[] = [
  {
    formulation: LIQUID_FORMULATIONS.ACETAMINOPHEN_DROPS_100,
    titleFa: 'قطره استامینوفن کودکان',
    localBrandsHintFa: 'پاراکید، استا، تایلوفن، تایلنول نوزاد',
    boxStrengthBadgeFa: '۱۰۰ میلی‌گرم در ۱ سی‌سی',
    confirmationPromptFa:
      'لطفاً عدد روی جعبه یا شیشه را چک کنید: آیا دقیقاً «۱۰۰ میلی‌گرم در ۱ سی‌سی (100 mg/mL)» نوشته شده است؟',
    isDrops: true,
    isIbuprofen: false,
  },
  {
    formulation: LIQUID_FORMULATIONS.ACETAMINOPHEN_SYRUP_120_5,
    titleFa: 'شربت استامینوفن استاندارد',
    localBrandsHintFa: 'شربت استامینوفن کودکان، پاراکید، کوریزان ساده',
    boxStrengthBadgeFa: '۱۲۰ میلی‌گرم در ۵ سی‌سی',
    confirmationPromptFa:
      'لطفاً عدد روی جعبه یا شیشه را چک کنید: آیا دقیقاً «۱۲۰ میلی‌گرم در ۵ سی‌سی (120 mg/5mL)» نوشته شده است؟',
    isDrops: false,
    isIbuprofen: false,
  },
  {
    formulation: LIQUID_FORMULATIONS.ACETAMINOPHEN_SYRUP_160_5,
    titleFa: 'شربت / سوسپانسیون استامینوفن غلیظ',
    localBrandsHintFa: 'پاراکید ۱۶۰، سوسپانسیون استامینوفن ۱۶۰',
    boxStrengthBadgeFa: '۱۶۰ میلی‌گرم در ۵ سی‌سی',
    confirmationPromptFa:
      'لطفاً عدد روی جعبه یا شیشه را چک کنید: آیا دقیقاً «۱۶۰ میلی‌گرم در ۵ سی‌سی (160 mg/5mL)» نوشته شده است؟',
    isDrops: false,
    isIbuprofen: false,
  },
  {
    formulation: LIQUID_FORMULATIONS.IBUPROFEN_SYRUP_100_5,
    titleFa: 'شربت ایبوپروفن (پروفن / بروفن کودکان)',
    localBrandsHintFa: 'پروفن کودکان، ادویل، بروفن سوسپانسیون',
    boxStrengthBadgeFa: '۱۰۰ میلی‌گرم در ۵ سی‌سی',
    confirmationPromptFa:
      'لطفاً عدد روی جعبه یا شیشه را چک کنید: آیا دقیقاً «۱۰۰ میلی‌گرم در ۵ سی‌سی (100 mg/5mL)» نوشته شده و شیشه را خوب تکان داده‌اید؟',
    isDrops: false,
    isIbuprofen: true,
  },
];

export interface WeightSafetyNotice {
  isOutlierWarning: boolean;
  outlierMessageFa: string | null;
  showDropsToSyrupSuggestion: boolean;
  dropsSuggestionMessageFa: string | null;
}

export interface PriorDoseConflictEvaluation {
  isHardConflict: boolean;
  isCautionSpacing: boolean;
  titleFa: string;
  bodyFa: string;
  suggestedWaitHours: number;
}

export interface FeverDoseCalculationResult {
  ageGroup: FeverAgeGroupOption;
  formulation: LiquidFormulation;
  weightKg: number;
  isBlocked: boolean;
  blockReasonFa: string | null;
  recommendedMl: number;
  minSafeMl: number;
  maxSafeMl: number;
  recommendedMg: number;
  approxDropsCount: number | null;
  isSafetyCeilingApplied: boolean;
  reminderIntervalHours: number;
  maxDosesPer24h: number;
  clinicalSourceTagFa: string;
}

export const MIN_FEVER_WEIGHT_KG = 3.0;
export const MAX_FEVER_WEIGHT_KG = 40.0;
export const DROPS_TO_SYRUP_WEIGHT_THRESHOLD_KG = 12.0;

export function isFormulationAllowedForAge(
  ageGroupId: FeverAgeGroupId,
  formulation: LiquidFormulation,
): boolean {
  if (ageGroupId === 'UNDER_3_MONTHS') return false;
  if (ageGroupId === 'THREE_TO_SIX_MONTHS') return formulation.key !== 'IBUPROFEN_SYRUP_100_5';
  return true;
}

export function inferAgeGroupFromMonths(ageMonths: number | null | undefined): FeverAgeGroupId | null {
  if (ageMonths === null || ageMonths === undefined) return null;
  if (ageMonths < 3) return 'UNDER_3_MONTHS';
  if (ageMonths < 6) return 'THREE_TO_SIX_MONTHS';
  return 'OVER_6_MONTHS';
}

export function evaluateWeightNotices(
  ageGroupId: FeverAgeGroupId,
  weightKg: number,
  formulation: LiquidFormulation,
): WeightSafetyNotice {
  let outlierMessageFa: string | null = null;
  if (ageGroupId === 'THREE_TO_SIX_MONTHS') {
    if (weightKg < 4.5) {
      outlierMessageFa =
        'وزن واردشده برای کودک ۳ تا ۶ ماهه کمتر از بازه معمول است؛ لطفاً عدد وزن را مجدداً بررسی کنید.';
    } else if (weightKg > 11.5) {
      outlierMessageFa =
        'وزن واردشده برای رده سنی ۳ تا ۶ ماه زیاد به نظر می‌رسد؛ لطفاً مطمئن شوید خطای تایپی یا انتخاب رده سنی رخ نداده است.';
    }
  } else if (ageGroupId === 'OVER_6_MONTHS') {
    if (weightKg < 5.5) {
      outlierMessageFa =
        'وزن واردشده برای کودک بالای ۶ ماه بسیار پایین است؛ لطفاً عدد وزن را دوباره بررسی کنید.';
    } else if (weightKg > 38.0) {
      outlierMessageFa =
        'برای وزن‌های بالا سقف ایمنی دوز تک‌نوبت اعمال می‌شود؛ لطفاً وزن دقیق کودک را تأیید کنید.';
    }
  }

  const showDropsToSyrupSuggestion =
    formulation.key === 'ACETAMINOPHEN_DROPS_100' &&
    weightKg > DROPS_TO_SYRUP_WEIGHT_THRESHOLD_KG;

  return {
    isOutlierWarning: outlierMessageFa !== null,
    outlierMessageFa,
    showDropsToSyrupSuggestion,
    dropsSuggestionMessageFa: showDropsToSyrupSuggestion
      ? 'برای کودکان با وزن بالای ۱۲ کیلوگرم، حجم قطره زیاد می‌شود و اندازه‌گیری آن دشوارتر است. پیشنهاد می‌شود از شربت استامینوفن ۱۲۰/۵ یا ۱۶۰/۵ استفاده کنید.'
      : null,
  };
}

export function evaluatePriorDoseConflict(
  selectedFormulation: LiquidFormulation,
  priorStatus: FeverPriorDoseStatusId,
): PriorDoseConflictEvaluation {
  const selectedIsIbuprofen = selectedFormulation.key === 'IBUPROFEN_SYRUP_100_5';
  switch (priorStatus) {
    case 'NONE_IN_6H':
      return {
        isHardConflict: false,
        isCautionSpacing: false,
        titleFa: 'تداخل زمانی مشاهده نشد',
        bodyFa: 'در ۶ ساعت گذشته تب‌بر مصرف نشده است؛ می‌توانید دوز دقیق این نوبت را مشاهده کنید.',
        suggestedWaitHours: 0,
      };
    case 'ACETAMINOPHEN_UNDER_4H':
      if (!selectedIsIbuprofen) {
        return {
          isHardConflict: true,
          isCautionSpacing: false,
          titleFa: 'هنوز زمان نوبت بعدی استامینوفن نرسیده است',
          bodyFa:
            'از آخرین نوبت استامینوفن کمتر از ۴ ساعت گذشته است. دادن زودهنگام دوز تکراری خطر آسیب کبدی دارد. تا رسیدن به فاصله حداقل ۴ تا ۶ ساعت، از روش‌های ایمن غیردارویی استفاده کنید.',
          suggestedWaitHours: 4,
        };
      }
      return {
        isHardConflict: false,
        isCautionSpacing: true,
        titleFa: 'احتیاط در مصرف متناوب استامینوفن و ایبوپروفن',
        bodyFa:
          'به‌طور معمول مصرف یکی از دو تب‌بر کافی است. تناوب بین استامینوفن و ایبوپروفن تنها در صورت بی‌قراری شدید و با ثبت دقیق ساعت انجام شود تا خطای دوز رخ ندهد.',
        suggestedWaitHours: 0,
      };
    case 'ACETAMINOPHEN_4_TO_6H':
      if (!selectedIsIbuprofen) {
        return {
          isHardConflict: false,
          isCautionSpacing: true,
          titleFa: 'حداقل فاصله ۴ ساعت رعایت شده است',
          bodyFa:
            'اگر کودک همچنان بی‌قرار است و حداقل ۴ ساعت از نوبت قبلی گذشته، می‌توان نوبت جدید استامینوفن را داد (حداکثر ۵ نوبت در ۲۴ ساعت).',
          suggestedWaitHours: 0,
        };
      }
      return {
        isHardConflict: false,
        isCautionSpacing: true,
        titleFa: 'توجه به تغییر نوع تب‌بر',
        bodyFa:
          'اگر پیش‌تر استامینوفن داده‌اید و قصد دادن ایبوپروفن دارید، ساعت دقیق هر دو را ثبت کنید تا از تداخل جلوگیری شود.',
        suggestedWaitHours: 0,
      };
    case 'IBUPROFEN_UNDER_4H':
      if (selectedIsIbuprofen) {
        return {
          isHardConflict: true,
          isCautionSpacing: false,
          titleFa: 'هنوز زمان نوبت بعدی ایبوپروفن نرسیده است',
          bodyFa:
            'حداقل فاصله ایمن بین دو نوبت ایبوپروفن ۶ تا ۸ ساعت است. دادن زودتر از موعد می‌تواند به معده و کلیه کودک آسیب بزند. تا نوبت بعدی از مراقبت‌های ایمن غیردارویی کمک بگیرید.',
          suggestedWaitHours: 6,
        };
      }
      return {
        isHardConflict: false,
        isCautionSpacing: true,
        titleFa: 'احتیاط در ترکیب با نوبت اخیر ایبوپروفن',
        bodyFa:
          'اگر کمتر از ۴ ساعت پیش ایبوپروفن داده‌اید و کودک صرفاً تب خفیف دارد ولی سرحال است، نیازی به داروی دوم نیست. در صورت بی‌قراری زیاد با ثبت دقیق ساعت اقدام کنید.',
        suggestedWaitHours: 0,
      };
    case 'IBUPROFEN_4_TO_6H':
      if (selectedIsIbuprofen) {
        return {
          isHardConflict: true,
          isCautionSpacing: false,
          titleFa: 'تا تکمیل ۶ ساعت از ایبوپروفن صبر کنید',
          bodyFa:
            'فاصله ایمن بین دو نوبت ایبوپروفن حداقل ۶ ساعت است. تا پایان ساعت ششم، از روش‌های ایمن غیردارویی برای آرام کردن کودک استفاده کنید.',
          suggestedWaitHours: 2,
        };
      }
      return {
        isHardConflict: false,
        isCautionSpacing: true,
        titleFa: 'توجه به ثبت ساعت دو داروی متفاوت',
        bodyFa: 'برای جلوگیری از اشتباه نیمه‌شب، حتماً دوز جدید را با دکمه ثبت در تایم‌لاین ذخیره کنید.',
        suggestedWaitHours: 0,
      };
  }
}

export function calculateFeverDose(
  ageGroupId: FeverAgeGroupId,
  formulation: LiquidFormulation,
  weightKg: number,
): FeverDoseCalculationResult {
  const ageGroup = FEVER_AGE_GROUPS.find((g) => g.id === ageGroupId) ?? FEVER_AGE_GROUPS[2];

  if (ageGroupId === 'UNDER_3_MONTHS') {
    return {
      ageGroup,
      formulation,
      weightKg,
      isBlocked: true,
      blockReasonFa:
        'تب در نوزادان زیر ۳ ماه نیاز به معاینه مستقیم پزشک دارد. پیش از دادن هرگونه داروی تب‌بر با پزشک یا اورژانس ۱۱۵ تماس بگیرید.',
      recommendedMl: 0,
      minSafeMl: 0,
      maxSafeMl: 0,
      recommendedMg: 0,
      approxDropsCount: null,
      isSafetyCeilingApplied: false,
      reminderIntervalHours: 6,
      maxDosesPer24h: 5,
      clinicalSourceTagFa: 'Lexicomp Pediatric 2026 • UpToDate',
    };
  }

  if (!isFormulationAllowedForAge(ageGroupId, formulation)) {
    return {
      ageGroup,
      formulation,
      weightKg,
      isBlocked: true,
      blockReasonFa:
        'مصرف ایبوپروفن در کودکان زیر ۶ ماه مجاز نیست. لطفاً از قطره یا شربت استامینوفن استفاده کنید.',
      recommendedMl: 0,
      minSafeMl: 0,
      maxSafeMl: 0,
      recommendedMg: 0,
      approxDropsCount: null,
      isSafetyCeilingApplied: false,
      reminderIntervalHours: 6,
      maxDosesPer24h: 5,
      clinicalSourceTagFa: 'Lexicomp Pediatric 2026 • UpToDate',
    };
  }

  const safeWeight = Math.min(MAX_FEVER_WEIGHT_KG, Math.max(MIN_FEVER_WEIGHT_KG, weightKg));
  const evalResult = evaluateLiquidDose(
    formulation,
    ageGroup.representativeAgeMonths,
    safeWeight,
  );

  if (evalResult.isBlocked) {
    return {
      ageGroup,
      formulation,
      weightKg: safeWeight,
      isBlocked: true,
      blockReasonFa: evalResult.alertBodyFa,
      recommendedMl: 0,
      minSafeMl: 0,
      maxSafeMl: 0,
      recommendedMg: 0,
      approxDropsCount: null,
      isSafetyCeilingApplied: false,
      reminderIntervalHours: 6,
      maxDosesPer24h: 5,
      clinicalSourceTagFa: 'Lexicomp Pediatric 2026 • UpToDate',
    };
  }

  const rawUncappedTargetMg = safeWeight * formulation.targetMgPerKg;
  const ceilingApplied = rawUncappedTargetMg >= formulation.maxSingleDoseMg - 0.1;
  const approxDropsCount =
    formulation.key === 'ACETAMINOPHEN_DROPS_100'
      ? Math.round(evalResult.recommendedMl * 20)
      : null;
  const isIbuprofen = formulation.key === 'IBUPROFEN_SYRUP_100_5';

  return {
    ageGroup,
    formulation,
    weightKg: safeWeight,
    isBlocked: false,
    blockReasonFa: null,
    recommendedMl: evalResult.recommendedMl,
    minSafeMl: evalResult.minSafeMl,
    maxSafeMl: evalResult.maxSafeMl,
    recommendedMg: evalResult.recommendedMg,
    approxDropsCount,
    isSafetyCeilingApplied: ceilingApplied,
    reminderIntervalHours: 6,
    maxDosesPer24h: isIbuprofen ? 4 : 5,
    clinicalSourceTagFa: 'Lexicomp Pediatric 2026 • UpToDate',
  };
}

export function formatPersianDecimal(value: number): string {
  const formatted = value.toFixed(1).replace(/\.0$/, '');
  return toPersianDigits(formatted.replace('.', '٫'));
}

export function buildCaregiverReportText(
  childAliasFa: string | null | undefined,
  result: FeverDoseCalculationResult,
  givenAtTimeLabelFa: string,
  nextSafeTimeLabelFa: string,
): string {
  const childName = childAliasFa?.trim() ? childAliasFa.trim() : 'کودک';
  const mlFa = formatPersianDecimal(result.recommendedMl);
  const kgFa = formatPersianDecimal(result.weightKg);
  const mgFa = toPersianDigits(Math.round(result.recommendedMg));
  const dropsLine =
    result.approxDropsCount !== null
      ? `\n• معادل تقریبی با قطره‌چکان استاندارد: ${toPersianDigits(result.approxDropsCount)} قطره`
      : '';
  return [
    `گزارش دوز تب‌بر «${childName}» (وب‌اپ مانو کید)`,
    `• دارو: ${result.formulation.titleFa}`,
    `• وزن کودک: ${kgFa} کیلوگرم (${result.ageGroup.labelFa})`,
    `• مقدار داده‌شده با سرنگ مدرج: ${mlFa} سی‌سی (${mgFa} میلی‌گرم)${dropsLine}`,
    `• ساعت مصرف این نوبت: ${givenAtTimeLabelFa}`,
    `• زمان ایمن نوبت بعدی (در صورت نیاز): حدود ساعت ${nextSafeTimeLabelFa}`,
    `• منبع دوزینگ: ${result.clinicalSourceTagFa}`,
  ].join('\n');
}
