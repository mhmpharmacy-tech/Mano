import type { AgeBandId, SymptomFlow, TriageEvaluationResult } from './models.ts';
import { ageBandFromMonths } from './clinicalAge.ts';
export { ageBandFromMonths } from './clinicalAge.ts';

export function isExtremeTemperature(celsius: number | null | undefined): boolean {
  return (
    celsius !== null &&
    celsius !== undefined &&
    Number.isFinite(celsius) &&
    celsius >= 34.0 &&
    celsius <= 43.0 &&
    (celsius < 35.0 || celsius >= 40.5)
  );
}

export interface EvaluateTriageInput {
  flow: SymptomFlow;
  ageBand: AgeBandId | null;
  temperatureCelsius: number | null;
  durationHours: number | null;
  selectedRedFlagIds: Set<string>;
  ageMonthsExact?: number | null;
}

export function evaluateTriage({
  flow,
  ageBand,
  temperatureCelsius,
  durationHours,
  selectedRedFlagIds,
  ageMonthsExact = null,
}: EvaluateTriageInput): TriageEvaluationResult {
  // A measured/dated age takes precedence over a saved or selected broad band.
  const exactAgeBand = ageBandFromMonths(ageMonthsExact);
  const effectiveAgeBand = ageMonthsExact === null ? ageBand : exactAgeBand;
  const checkedImmediateFlags = flow.redFlags.filter(
    (rf) => selectedRedFlagIds.has(rf.id) && rf.isImmediateEmergency,
  );

  // Rule 1: Critical red flags always bypass self-care immediately
  if (checkedImmediateFlags.length > 0) {
    return {
      outcome: 'URGENT_EMERGENCY',
      titleFa: 'نیاز به ارزیابی فوری اورژانس',
      summaryFa: `به دلیل وجود نشانه هشدار (${checkedImmediateFlags.map((f) => f.labelFa).join('، ')})، توصیه‌های مراقبت خانگی نمایش داده نمی‌شود و لازم است کودک بدون تأخیر توسط تیم درمانی یا اورژانس ۱۱۵ ارزیابی شود.`,
      immediateActionsFa: [
        'در صورت تنگی نفس، کبودی لب‌ها، تشنج یا کاهش هوشیاری، بلافاصله با اورژانس ۱۱۵ تماس بگیرید یا کودک را به نزدیک‌ترین مرکز اورژانس برسانید.',
        'تا زمان ارزیابی پزشک، از دادن خودسرانه دارو یا خوراندن مایعات در کودک خواب‌آلود خودداری کنید.',
      ],
      escalationCriteriaFa: flow.escalationCriteriaFa,
      relatedArticleSlug: flow.relatedArticleSlug,
      canShowSelfCare: false,
      triggeredRule: {
        id: 'rule_critical_red_flag_bypass',
        descriptionFa: 'وجود علامت هشدار بحرانی، مسیر مراقبت خانگی را متوقف می‌کند.',
        outcome: 'URGENT_EMERGENCY',
        reviewStatus: 'NEEDS_CLINICAL_REVIEW',
      },
      showPharmacistOption: false,
    };
  }

  // Rule 2: Fever >= 38.0 °C (or fever flow without confirmed low temp) in infant < 3 months
  if (
    effectiveAgeBand === 'NEWBORN' &&
    ((temperatureCelsius !== null && temperatureCelsius >= 38.0) ||
      (flow.id === 'fever' && temperatureCelsius === null))
  ) {
    const tempText =
      temperatureCelsius !== null
        ? `دمای ثبت‌شده ${temperatureCelsius} درجه سانتی‌گراد`
        : 'احتمال تب در نوزاد زیر ۳ ماه';
    return {
      outcome: 'URGENT_EMERGENCY',
      titleFa: 'ارزیابی فوری پزشکی برای تب نوزاد زیر ۳ ماه',
      summaryFa: `در نوزادان کوچک‌تر از ۳ ماه، ${tempText} یک وضعیت نیازمند ارزیابی فوری حضوری توسط پزشک یا اورژانس است. مانو در این شرایط هیچ دوز دارویی یا اطمینان‌بخشی خانگی ارائه نمی‌کند.`,
      immediateActionsFa: [
        'نوزاد را بلافاصله به اورژانس اطفال یا پزشک برسانید (و در صورت بی‌حالی یا مشکل تنفسی با اورژانس ۱۱۵ تماس بگیرید).',
        'پیش از معاینه پزشک، خودسرانه تب‌بر به نوزاد ندهید و پوشش نوزاد را بیش از حد ضخیم نکنید.',
        'زمان شروع تب، عدد دقیق دماسنج و میزان شیر خوردن و پوشک خیس نوزاد را برای پزشک یادداشت کنید.',
      ],
      escalationCriteriaFa: flow.escalationCriteriaFa,
      relatedArticleSlug: flow.relatedArticleSlug,
      canShowSelfCare: false,
      triggeredRule: {
        id: 'rule_infant_under_3m_fever_38',
        descriptionFa: 'تب ۳۸ درجه یا بیشتر در شیرخوار زیر ۳ ماه نیازمند ارزیابی فوری پزشکی است.',
        outcome: 'URGENT_EMERGENCY',
        reviewStatus: 'NEEDS_CLINICAL_REVIEW',
      },
      showPharmacistOption: false,
    };
  }

  if (isExtremeTemperature(temperatureCelsius)) {
    return {
      outcome: 'URGENT_EMERGENCY',
      titleFa: 'دمای ثبت‌شده نیاز به ارزیابی فوری دارد',
      summaryFa: `دمای ثبت‌شده ${temperatureCelsius} درجه است. برای ارزیابی فوری حضوری اقدام کنید.`,
      immediateActionsFa: ['کودک را به مرکز درمانی برسانید یا با اورژانس ۱۱۵ تماس بگیرید.'],
      escalationCriteriaFa: flow.escalationCriteriaFa,
      relatedArticleSlug: flow.relatedArticleSlug,
      canShowSelfCare: false,
      triggeredRule: {
        id: 'rule_extreme_temperature_emergency',
        descriptionFa: 'دمای ثبت‌شده در محدوده هشدار فوری است.',
        outcome: 'URGENT_EMERGENCY',
        reviewStatus: 'NEEDS_CLINICAL_REVIEW',
      },
      showPharmacistOption: false,
    };
  }

  const validFlagIds = new Set(flow.redFlags.map((f) => f.id));
  const hasUnknownFlag = Array.from(selectedRedFlagIds).some((id) => !validFlagIds.has(id));
  const invalidTemp =
    temperatureCelsius !== null &&
    (!Number.isFinite(temperatureCelsius) || temperatureCelsius < 34.0 || temperatureCelsius > 43.0);
  const invalidDuration =
    durationHours !== null && (!Number.isFinite(durationHours) || durationHours < 0 || durationHours > 8760);
  const invalidExactAge = ageMonthsExact !== null && exactAgeBand === null;

  if (invalidTemp || invalidDuration || hasUnknownFlag || invalidExactAge) {
    return {
      outcome: 'NEEDS_TIMELY_REVIEW',
      titleFa: 'اطلاعات نیاز به اصلاح دارد',
      summaryFa:
        'ورودی نامعتبر یا علامت ناشناخته دریافت شد. اطلاعات را بررسی کنید؛ این نتیجه به معنی نبود خطر نیست.',
      immediateActionsFa: [
        'در تنفس سخت، تشنج یا اختلال هوشیاری، منتظر اصلاح فرم نمانید و فوراً کمک پزشکی بگیرید.',
      ],
      escalationCriteriaFa: flow.escalationCriteriaFa,
      relatedArticleSlug: flow.relatedArticleSlug,
      canShowSelfCare: false,
      triggeredRule: {
        id: 'rule_invalid_input',
        descriptionFa: 'ورودی ناقص یا نامعتبر؛ بدون نتیجهٔ اطمینان‌بخش',
        outcome: 'NEEDS_TIMELY_REVIEW',
        reviewStatus: 'NEEDS_CLINICAL_REVIEW',
      },
      showPharmacistOption: false,
    };
  }

  if (
    effectiveAgeBand === null ||
    durationHours === null ||
    ((flow.asksTemperature || flow.id === 'fever') && temperatureCelsius === null)
  ) {
    return {
      outcome: 'NEEDS_TIMELY_REVIEW',
      titleFa: 'اطلاعات برای ارزیابی کامل نیست',
      summaryFa:
        'بازه سنی واقعی کودک، مدت علامت و دمای اندازه‌گیری‌شده در صورت درخواست را وارد کنید. تا تکمیل اطلاعات، توصیه مراقبت خانگی یا نتیجه اطمینان‌بخش ارائه نمی‌شود.',
      immediateActionsFa: [
        'اگر کودک تنفس سخت، تشنج یا کاهش هوشیاری دارد، منتظر تکمیل فرم نمانید و فوراً کمک پزشکی بگیرید.',
      ],
      escalationCriteriaFa: flow.escalationCriteriaFa,
      relatedArticleSlug: flow.relatedArticleSlug,
      canShowSelfCare: false,
      triggeredRule: {
        id: 'rule_incomplete_input',
        descriptionFa: 'سن یا اطلاعات ضروری علامت ثبت نشده است؛ بدون توصیه مراقبت خانگی',
        outcome: 'NEEDS_TIMELY_REVIEW',
        reviewStatus: 'NEEDS_CLINICAL_REVIEW',
      },
      showPharmacistOption: false,
    };
  }

  // Rule 3: Timely clinician review flags
  const checkedReviewFlags = flow.redFlags.filter(
    (rf) => selectedRedFlagIds.has(rf.id) && !rf.isImmediateEmergency,
  );
  const prolongedFever = flow.id === 'fever' && (durationHours ?? 0) >= 72;
  const prolongedCough = (flow.id === 'cough' || flow.id === 'wet_cough') && (durationHours ?? 0) >= 24 * 14;
  const youngInfantHighTemp =
    effectiveAgeBand === 'INFANT_3_12M' &&
    (ageMonthsExact === null || ageMonthsExact === undefined || ageMonthsExact <= 6) &&
    (temperatureCelsius ?? 0) >= 39.0;

  const prolongedNasal = flow.id === 'runny_nose' && (durationHours ?? 0) >= 240;
  const youngInfant =
    effectiveAgeBand === 'NEWBORN' && (flow.id === 'eye' || flow.id === 'wet_cough' || flow.id === 'runny_nose');
  const infantEar = flow.id === 'ear' && (effectiveAgeBand === 'NEWBORN' || effectiveAgeBand === 'INFANT_3_12M');
  const persistentEar = flow.id === 'ear' && (durationHours ?? 0) >= 72;
  const earWarning = flow.id === 'ear' && checkedReviewFlags.length > 0;
  const sameDay =
    youngInfant ||
    infantEar ||
    earWarning ||
    checkedReviewFlags.some((f) =>
      ['poor_feeding', 'blood_in_sputum', 'eye_contact', 'nasal_foreign_body'].includes(f.id),
    );

  if (
    infantEar ||
    persistentEar ||
    youngInfant ||
    prolongedNasal ||
    checkedReviewFlags.length > 0 ||
    prolongedFever ||
    prolongedCough ||
    youngInfantHighTemp
  ) {
    const reasonParts: string[] = [];
    if (checkedReviewFlags.length > 0) {
      reasonParts.push(checkedReviewFlags.map((f) => f.labelFa).join('، '));
    }
    if (infantEar) reasonParts.push('گوش‌درد یا ترشح گوش در شیرخوار زیر یک سال');
    if (persistentEar) reasonParts.push('ماندگاری مشکل گوش برای سه روز یا بیشتر');
    if (youngInfant) reasonParts.push('سن کمتر از سه ماه');
    if (prolongedNasal) reasonParts.push('ادامهٔ آبریزش برای ده روز یا بیشتر');
    if (prolongedFever) reasonParts.push('تداوم تب بیش از ۷۲ ساعت');
    if (prolongedCough) reasonParts.push('تداوم سرفه بیش از ۲ هفته');
    if (youngInfantHighTemp) {
      reasonParts.push(
        ageMonthsExact === null || ageMonthsExact === undefined
          ? 'دمای ۳۹ یا بیشتر و سن دقیق شیرخوار نامشخص است؛ احتمال سن ۳ تا ۶ ماه'
          : 'دمای ۳۹ یا بیشتر در شیرخوار ۳ تا ۶ ماه',
      );
    }
    if (flow.id === 'fever' && (durationHours ?? 0) >= 120) {
      reasonParts.push('تب ۵ روز یا بیشتر؛ نیاز به ارزیابی علل تب طولانی');
    }

    return {
      outcome: 'NEEDS_TIMELY_REVIEW',
      titleFa: sameDay
        ? 'امروز کودک را به پزشک نشان دهید'
        : youngInfantHighTemp
          ? 'تماس فوری با پزشک برای ارزیابی سریع'
          : 'نیاز به ارزیابی پزشک',
      summaryFa: `بر اساس اطلاعات واردشده (${reasonParts.join('، ')})، لازم است کودک در اولین فرصت مناسب توسط پزشک معاینه شود. نکات زیر صرفاً برای مراقبت حمایتی تا زمان ویزیت است و جایگزین تشخیص پزشکی نیست.`,
      immediateActionsFa:
        youngInfantHighTemp || sameDay
          ? ['برای ارزیابی سریع با پزشک یا مرکز درمانی تماس بگیرید؛ منتظر پاسخ مشاورهٔ آنلاین نمانید.']
          : flow.educationalStepsFa,
      escalationCriteriaFa: flow.escalationCriteriaFa,
      relatedArticleSlug: flow.relatedArticleSlug,
      canShowSelfCare: !youngInfantHighTemp && !sameDay,
      triggeredRule: {
        id: 'rule_timely_clinician_review',
        descriptionFa: 'وجود عامل خطر غیرفوری یا تداوم علامت نیازمند ویزیت پزشک است.',
        outcome: 'NEEDS_TIMELY_REVIEW',
        reviewStatus: 'NEEDS_CLINICAL_REVIEW',
      },
      showPharmacistOption: false,
    };
  }

  // Rule 4: General educational support
  return {
    outcome: 'GENERAL_EDUCATIONAL',
    titleFa: 'راهنمای آموزشی مراقبت حمایتی',
    summaryFa:
      'در حال حاضر علامت هشدار فوری ثبت نشده است. این راهنما برای آگاهی والدین و پایش وضعیت کودک ارائه می‌شود و تشخیص پزشکی محسوب نمی‌شود؛ در صورت تغییر حال عمومی با پزشک مشورت کنید.',
    immediateActionsFa: flow.educationalStepsFa,
    escalationCriteriaFa: flow.escalationCriteriaFa,
    relatedArticleSlug: flow.relatedArticleSlug,
    canShowSelfCare: true,
    triggeredRule: {
      id: 'rule_general_educational_support',
      descriptionFa: 'نبود علامت هشدار در ارزیابی اولیه؛ نمایش راهنمای آموزشی و معیارهای پیگیری.',
      outcome: 'GENERAL_EDUCATIONAL',
      reviewStatus: 'NEEDS_CLINICAL_REVIEW',
    },
    showPharmacistOption: true,
  };
}
