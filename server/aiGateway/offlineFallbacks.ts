import type {
  RestrictedDocumentedQaPayload,
  RestrictedNutritionPayload,
  RestrictedSafetyChecklistPayload,
  RestrictedSyrupScanPayload,
  VisitDraftStructuredPayload,
} from '../../src/domain/aiGatewayContracts.ts';
import type { FaqItem, SymptomFlow, TriageOutcome } from '../../src/domain/models.ts';
import { getSourceCitationLabel } from '../../src/data/contentRepository.ts';

export function buildOfflineVisitDraftFallback(params: {
  chiefComplaintFa: string;
  selectedSymptomsFa: string[];
  parentNotesFa: string;
  childContextSummaryFa: string;
  tempPointsFa: string[];
  dosePointsFa: string[];
  reasonNoteFa?: string;
}): VisitDraftStructuredPayload {
  const missingInformationFlagsFa: string[] = [];
  const cleanChild = params.childContextSummaryFa.trim();
  const cleanComplaint = params.chiefComplaintFa.trim();
  const cleanNotes = params.parentNotesFa.trim();

  if (!cleanChild) missingInformationFlagsFa.push('UNKNOWN: مشخصات کودک ثبت نشده است.');
  if (!cleanComplaint) missingInformationFlagsFa.push('UNKNOWN: علت مراجعه ثبت نشده است.');
  if (params.tempPointsFa.length === 0) {
    missingInformationFlagsFa.push('UNKNOWN: دمای بدن در ۲۴ ساعت اخیر ثبت نشده است.');
  }
  if (params.dosePointsFa.length === 0) {
    missingInformationFlagsFa.push('UNKNOWN: سابقه مصرف دارو در ۲۴ ساعت اخیر ثبت نشده است.');
  }

  const childAndReasonSummaryFa = [
    cleanChild ? `پرونده: ${cleanChild}` : 'پرونده کودک: UNKNOWN (نیازمند تکمیل توسط والد)',
    cleanComplaint ? `علت مراجعه: ${cleanComplaint}` : 'علت مراجعه: UNKNOWN',
  ].join(' | ');

  const temperatureTrend24hFa =
    params.tempPointsFa.length > 0
      ? params.tempPointsFa.join('؛ ')
      : 'در ۲۴ ساعت گذشته دمایی ثبت نشده است (UNKNOWN).';

  const medicationHistory24hFa =
    params.dosePointsFa.length > 0
      ? params.dosePointsFa.join('؛ ')
      : 'در ۲۴ ساعت گذشته دارویی ثبت نشده است (UNKNOWN).';

  const observedSymptomsAndFlagsFa =
    params.selectedSymptomsFa.length > 0
      ? params.selectedSymptomsFa.join('، ')
      : cleanNotes || 'علامت انتخاب‌شده‌ای ثبت نشده است (REQUIRE_REVIEW).';

  const questionsForClinicianFa = [
    'با توجه به سن و وزن ثبت‌شده، چه علائم هشداری نیازمند مراجعه فوری‌تر است؟',
    'آیا نیاز به تغییر یا ادامه داروهای فعلی توسط پزشک وجود دارد؟',
    ...(cleanNotes ? [`پرسش والد: ${cleanNotes}`] : []),
  ];

  const lines = [
    '۱. خلاصه مشخصات و علت مراجعه (پیش‌نویس آفلاین — نیازمند بررسی):',
    childAndReasonSummaryFa,
    '',
    '۲. روند ۲۴ ساعته تب و دماهای ثبت‌شده:',
    temperatureTrend24hFa,
    '',
    '۳. داروهای مصرف‌شده در ۲۴ ساعت گذشته (گزارش والد):',
    medicationHistory24hFa,
    '',
    '۴. علائم و یادداشت‌های ثبت‌شده:',
    observedSymptomsAndFlagsFa,
    '',
    '۵. پرسش‌های پیشنهادی برای پزشک یا داروساز:',
    ...questionsForClinicianFa.map((q) => `• ${q}`),
    '',
    params.reasonNoteFa
      ? `توضیح سامانه: ${params.reasonNoteFa}`
      : 'توضیح سامانه: این متن یک خلاصه ساختاریافته آفلاین از داده‌های ثبت‌شده شماست و تشخیص یا تجویز محسوب نمی‌شود.',
  ];

  return {
    titleFa: cleanComplaint || 'خلاصه ساختاریافته آماده ویزیت (آفلاین)',
    childAndReasonSummaryFa,
    temperatureTrend24hFa,
    medicationHistory24hFa,
    observedSymptomsAndFlagsFa,
    questionsForClinicianFa,
    missingInformationFlagsFa,
    verificationState: 'REQUIRE_REVIEW',
    formattedDraftFa: lines.join('\n'),
  };
}

export function buildOfflineSyrupScanFallback(reasonFa?: string): RestrictedSyrupScanPayload {
  return {
    detectedDrugNameFa: 'UNKNOWN — نیازمند خواندن مستقیم برچسب توسط والد',
    activeIngredientKey: 'UNKNOWN',
    dosageForm: 'UNKNOWN',
    mgAmount: 0,
    perMlAmount: 0,
    mgPerMl: 0,
    matchedFormulationId: 'CUSTOM_UNVERIFIED',
    confidenceLevel: 'LOW',
    concentrationDisplayFa: 'UNKNOWN (نیازمند ورود دستی)',
    safetyNoteFa:
      reasonFa ||
      'استخراج خودکار برچسب در دسترس یا قابل اتکا نبود (REQUIRE_REVIEW). عدد میلی‌گرم و میلی‌لیتر روی شیشه را مستقیماً بخوانید و دستی انتخاب کنید.',
    isPediatricFeverOrAllergyLiquid: false,
    verificationState: 'REQUIRE_REVIEW',
    missingFields: ['activeIngredientKey', 'dosageForm', 'mgAmount', 'perMlAmount', 'mgPerMl'],
    requiresManualConfirmation: true,
  };
}

export function buildOfflineSafetyChecklistFallback(params: {
  flow?: SymptomFlow | null;
  flowTitleFa: string;
  urgencyLevel: TriageOutcome;
  evaluationSummaryFa: string;
  selectedSymptomsFa: string[];
  reasonNoteFa?: string;
}): RestrictedSafetyChecklistPayload {
  const sourceIds =
    params.flow && params.flow.sourceIds.length > 0 ? params.flow.sourceIds : ['aap_fever', 'nice_ng143'];
  const isUrgent = params.urgencyLevel === 'URGENT_EMERGENCY';

  const immediateEmergencySignsFa = params.flow
    ? params.flow.redFlags.filter((r) => r.isImmediateEmergency).map((r) => r.labelFa)
    : [
        'تنفس سخت، تند یا کبودی لب‌ها',
        'خواب‌آلودگی شدید یا تشنج',
        'تب ۳۸ درجه یا بالاتر در نوزاد زیر ۳ ماه',
      ];

  const sameDaySigns = params.flow
    ? params.flow.escalationCriteriaFa
    : ['تداوم یا تشدید علائم، کاهش واضح ادرار یا ناتوانی در نوشیدن مایعات'];

  const homeChecklist = isUrgent
    ? [
        'ارزیابی فوری اورژانس (۱۱۵ یا مرکز درمانی) اولویت مطلق دارد؛ مراقبت خانگی جایگزین ویزیت فوری نیست.',
        'تا پیش از معاینه پزشک، از دادن خودسرانه دارو خودداری کنید.',
      ]
    : params.flow?.educationalStepsFa ?? [
        'ثبت منظم دما، دفعات ادرار و میزان دریافت مایعات کودک',
        'پرهیز از خوددرمانی یا تغییر خودسرانه دوز دارو',
      ];

  const questions = [
    ...(params.selectedSymptomsFa.length > 0
      ? [`بررسی علائم ثبت‌شده: ${params.selectedSymptomsFa.join('، ')}`]
      : []),
    'چه نشانه‌هایی نشانه نیاز به مراجعه مجدد یا اورژانسی است؟',
    'آیا با توجه به سن و وزن کودک نیاز به بررسی حضوری بیشتر وجود دارد؟',
  ];

  return {
    checklistTitleFa: isUrgent
      ? `اقدام فوری و علائم اورژانس: ${params.flowTitleFa}`
      : `توضیح ساختاریافته و چک‌لیست ایمنی: ${params.flowTitleFa}`,
    urgencySummaryFa:
      params.evaluationSummaryFa ||
      (isUrgent
        ? 'وضعیت ثبت‌شده نیازمند ارزیابی فوری پزشکی یا اورژانس ۱۱۵ است.'
        : 'این فهرست توضیح آفلاین خروجی موتور ارزیابی علائم بدون تغییر سطح فوریت است.'),
    immediateEmergencySignsFa,
    sameDayPediatricianVisitSignsFa: sameDaySigns,
    homeMonitoringChecklistFa: homeChecklist,
    questionsForPediatricianFa: questions,
    clinicalSourceCitationFa:
      params.reasonNoteFa ||
      `برگرفته مستقیم از پروتکل آفلاین مانو کید (${sourceIds.map(getSourceCitationLabel).join(' • ')})`,
    verificationState: 'REQUIRE_REVIEW',
    preservedUrgencyLevel: params.urgencyLevel,
    citedSourceIds: sourceIds,
  };
}

export function buildOfflineDocumentedQaFallback(params: {
  matchedFaqs: FaqItem[];
  reasonNoteFa?: string;
}): RestrictedDocumentedQaPayload {
  if (params.matchedFaqs.length === 0) {
    return {
      directAnswerFa:
        'مورد منطبق مستقیمی در پرسش‌های تاییدشده پایگاه دانش یافت نشد (UNKNOWN). برای جلوگیری از پاسخ غیردقیق، پاسخ جدیدی ساخته نشد.',
      actionableStepsFa: [
        'عبارت پرسش را ساده‌تر جست‌وجو کنید یا به بخش بررسی علائم مراجعه نمایید.',
        'برای سوالات اختصاصی با داروساز یا پزشک کودک مشورت کنید.',
      ],
      whenToVisitPediatricianFa: [
        'در هر علامت شدید، تب نوزاد زیر ۳ ماه یا تردید دارویی مستقیماً با پزشک یا اورژانس ۱۱۵ تماس بگیرید.',
      ],
      citedSourcesFa: [],
      citedSourceIds: [],
      matchedFaqIds: [],
      safetyDisclaimerFa:
        params.reasonNoteFa ||
        'وضعیت: UNKNOWN / REQUIRE_REVIEW — هیچ مرجع یا پاسخ پزشکی خارج از پایگاه دانش ساخته نشد.',
      verificationState: 'UNKNOWN',
    };
  }

  const primary = params.matchedFaqs[0];
  const citedSourceIds = Array.from(
    new Set(params.matchedFaqs.flatMap((f) => f.sourceIds).filter(Boolean)),
  );
  const citedSourcesFa = citedSourceIds.map(getSourceCitationLabel).filter(Boolean);

  return {
    directAnswerFa: params.matchedFaqs
      .slice(0, 2)
      .map((f) => f.shortAnswerFa)
      .join(' '),
    actionableStepsFa: Array.from(
      new Set(params.matchedFaqs.flatMap((f) => f.whatToDoFa).slice(0, 6)),
    ),
    whenToVisitPediatricianFa: Array.from(
      new Set(params.matchedFaqs.flatMap((f) => f.whenToSeeDoctorFa).slice(0, 5)),
    ),
    citedSourcesFa,
    citedSourceIds,
    matchedFaqIds: params.matchedFaqs.map((f) => f.id),
    safetyDisclaimerFa:
      params.reasonNoteFa ||
      `پاسخ مستقیم از مستندات مرور‌شده مانو کید (${primary.clinicalDetailFa}) — نیازمند تطبیق با شرایط کودک توسط والد یا پزشک.`,
    verificationState: 'REQUIRE_REVIEW',
  };
}

export function buildOfflineNutritionFallback(params: {
  ageMonths: number;
  dietGoalFa?: string;
  reasonNoteFa?: string;
}): RestrictedNutritionPayload {
  const isUnder12 = params.ageMonths < 12;
  const citedSourceIds = ['nelson8_growth', 'who_growth'];
  const citedSourcesFa = citedSourceIds.map(getSourceCitationLabel);

  const meals = isUnder12
    ? [
        {
          mealTypeFa: 'صبحانه (نمونه آموزشی ۶ تا ۱۲ ماه)',
          titleFa: 'فرنی ساده آرد برنج با شیر مادر یا شیرخشک',
          whySuitableFa: 'نمونه آموزشی ملایم و بدون آلرژن پرخطر در کنار تغذیه اصلی با شیر',
          textureAndPortionFa: 'بافت نرم و پوره‌ای؛ شروع با ۲ تا ۳ قاشق مرباخوری و افزایش تدریجی',
          ingredientsFa: ['آرد برنج نرم', 'آب جوشیده', 'شیر مادر یا شیرخشک آماده (پس از پخت)'],
          prepStepsFa: ['آرد برنج و آب را بپزید تا غلظت مناسب بگیرد.', 'پس از ولرم شدن با شیر مادر یا شیرخشک رقیق کنید.'],
          safetyAvoidListFa: ['عسل', 'شیر گاو به عنوان نوشیدنی', 'شکر و نمک افزوده'],
          doctorFollowUpHintFa: 'تداوم یا حساسیت پوستی/گوارشی جدید را با پزشک کودک بررسی کنید.',
        },
        {
          mealTypeFa: 'ناهار (نمونه آموزشی ۶ تا ۱۲ ماه)',
          titleFa: 'پوره سیب‌زمینی، هویج و عدس پخته',
          whySuitableFa: 'تأمین ملایم انرژی و آهن گیاهی با بافت ایمن ضدخفگی',
          textureAndPortionFa: 'پوره کاملاً نرم یا له شده با چنگال متناسب با ماه سن',
          ingredientsFa: ['سیب‌زمینی پخته', 'هویج پخته', 'عدس کاملاً پخته و پوست‌کنده'],
          prepStepsFa: ['اقلام را بدون نمک کاملاً بپزید.', 'با آب غذا پوره نرم و یکدست تهیه کنید.'],
          safetyAvoidListFa: ['نمک افزوده', 'حبوبات سفت یا تکه‌های درشت'],
          doctorFollowUpHintFa: 'نیاز به مکمل آهن باید طبق برنامه بهداشت یا دستور پزشک ادامه یابد.',
        },
      ]
    : [
        {
          mealTypeFa: 'صبحانه (نمونه آموزشی بالای ۱۲ ماه)',
          titleFa: 'املت کاملاً پخته با نان نرم و گوجه پوست‌کنده',
          whySuitableFa: 'نمونه متعادل پروتئین و انرژی برای کودک بالای ۱ سال بدون سابقه حساسیت',
          textureAndPortionFa: 'لقمه‌های کوچک و نرم، برش طولی ایمن برای پیشگیری از خفگی',
          ingredientsFa: ['تخم‌مرغ کاملاً پخته', 'نان نرم خردشده', 'کمی روغن زیتون'],
          prepStepsFa: ['تخم‌مرغ را کامل بپزید.', 'به قطعات کوچک و نرم برای کودک آماده کنید.'],
          safetyAvoidListFa: ['آجیل کامل', 'انگور کامل برش‌نخورده'],
          doctorFollowUpHintFa: 'در صورت بی‌اشتهایی پایدار یا افت وزن، نمودار رشد را با پزشک بررسی کنید.',
        },
        {
          mealTypeFa: 'ناهار (نمونه آموزشی بالای ۱۲ ماه)',
          titleFa: 'کته نرم با مرغ ریش‌ریش و سبزیجات بخارپز',
          whySuitableFa: 'نمونه سنتی قابل هضم متناسب با سفره خانواده',
          textureAndPortionFa: 'برنج نرم و تکه‌های ریز ریش‌ریش‌شده مرغ، همیشه در حالت نشسته',
          ingredientsFa: ['برنج کته نرم', 'گوشت مرغ کاملاً پخته و ریش‌ریش', 'کدو یا هویج پخته'],
          prepStepsFa: ['مرغ و سبزیجات را نرم بپزید و کاملاً ریز کنید.', 'در حالت نشسته و تحت نظارت بزرگسال سرو شود.'],
          safetyAvoidListFa: ['تکه‌های سفت و درشت', 'ادویه تند یا نمک زیاد'],
          doctorFollowUpHintFa: 'این نمونه آموزشی جایگزین مشاوره تغذیه تخصصی پزشک نیست.',
        },
      ];

  return {
    planTitleFa: `راهنمای آموزشی آفلاین تغذیه (${params.ageMonths} ماهه — نیازمند بررسی)`,
    overviewFa:
      params.reasonNoteFa ||
      `این موارد صرفاً نمونه‌های آموزشی مرور‌شده متناسب با سن ${params.ageMonths} ماه هستند (هدف: ${params.dietGoalFa || 'تغذیه متعادل'}) و جایگزین توصیه اختصاصی پزشک کودک نیستند.`,
    growthNutritionFocusFa: isUnder12
      ? 'در ۶ تا ۱۲ ماه اولویت همچنان شیر مادر یا شیرخشک است و غذای کمکی با بافت ایمن و بدون عسل/شیر گاو معرفی می‌شود.'
      : 'تنوع گروه‌های غذایی، پیشگیری از خفگی با برش ایمن و پایش روند رشد روی نمودار WHO.',
    hydrationTipFa: isUnder12
      ? 'جرعه‌های کوچک آب آشامیدنی ایمن کنار غذا؛ پرهیز کامل از آب‌میوه شیرین یا شیر گاو.'
      : 'آب آشامیدنی ساده در طول روز؛ پرهیز از نوشابه‌ها و نوشیدنی‌های قندی.',
    meals,
    verificationState: 'REQUIRE_REVIEW',
    citedSourceIds,
    citedSourcesFa,
    safetyDisclaimerFa:
      'وضعیت: REQUIRE_REVIEW — راهنمای آموزشی آفلاین بدون تغییر قوانین قطعی سن و آلرژی.',
  };
}
