export type MedicationCheckCategoryId = 'VERIFY_MEDICINE' | 'DOSE_MEASURE' | 'TIMING_GIVING';

export interface MedicationSafetyCheckItem {
  id: string;
  category: MedicationCheckCategoryId;
  stepNumberFa: string;
  titleFa: string;
  preventsErrorFa: string;
  detailTipFa: string;
  isCritical: boolean;
}

export interface MedicationChecklistProgress {
  checkedCount: number;
  totalCount: number;
  criticalCheckedCount: number;
  criticalTotalCount: number;
  isAllCriticalReady: boolean;
  isAllCompleted: boolean;
  progressFraction: number;
  statusHeadlineFa: string;
  statusSublineFa: string;
}

export const MEDICATION_CHECK_CATEGORIES: { id: MedicationCheckCategoryId; labelFa: string }[] = [
  { id: 'VERIFY_MEDICINE', labelFa: '۱. بررسی بطری، تاریخ انقضا و مشخصات کودک' },
  { id: 'DOSE_MEASURE', labelFa: '۲. کنترل غلظت دارو و اندازه‌گیری دقیق mL' },
  { id: 'TIMING_GIVING', labelFa: '۳. ساعت آخرین نوبت، تداخل‌ها و ثبت ایمن' },
];

export const MEDICATION_SAFETY_CHECKLIST_ITEMS: MedicationSafetyCheckItem[] = [
  {
    id: 'check_right_child_weight',
    category: 'VERIFY_MEDICINE',
    stepNumberFa: 'گام ۱',
    titleFa: 'نام کودک، سن و وزن فعلی با برچسب دارو تطبیق داده شد',
    preventsErrorFa: 'خطای دادن داروی بزرگسال یا دوز کودک دیگر',
    detailTipFa:
      'دوز داروهای کودکان بر اساس وزن دقیق (کیلوگرم) و سن تعیین می‌شود؛ از اشتراک گذاشتن داروی خواهر و برادر بدون تایید پزشک خودداری کنید.',
    isCritical: true,
  },
  {
    id: 'check_expiry_and_opened_date',
    category: 'VERIFY_MEDICINE',
    stepNumberFa: 'گام ۲',
    titleFa: 'تاریخ انقضا، ظاهر دارو و تاریخ باز شدن درب بطری بررسی شد',
    preventsErrorFa: 'مصرف داروی منقضی، کدر یا سوسپانسیون فاسدشده',
    detailTipFa:
      'روی بطری تاریخ باز شدن را یادداشت کنید. بسیاری از سوسپانسیون‌های آنتی‌بیوتیک پس از حل شدن فقط ۷ تا ۱۴ روز در یخچال قابل استفاده‌اند.',
    isCritical: true,
  },
  {
    id: 'check_concentration_match',
    category: 'DOSE_MEASURE',
    stepNumberFa: 'گام ۳',
    titleFa: 'غلظت روی بطری (مثلاً 100mg/5mL یا قطره) دوباره خوانده شد',
    preventsErrorFa: 'اشتباه گرفتن غلظت قطره نوزادان و شربت کودکان',
    detailTipFa:
      'غلظت قطره خوراکی و شربت یکسان نیست؛ پیش از هر بار کشیدن دارو، عدد میلی‌گرم در میلی‌لیتر روی جعبه را چک کنید.',
    isCritical: true,
  },
  {
    id: 'check_shake_and_oral_syringe',
    category: 'DOSE_MEASURE',
    stepNumberFa: 'گام ۴',
    titleFa: 'بطری تکان داده شد و دارو فقط با سرنگ مدرج mL کشیده شد',
    preventsErrorFa: 'خطای قاشق آشپزخانه و ته‌نشینی ماده مؤثره',
    detailTipFa:
      'بطری سوسپانسیون را قبل از مصرف به‌خوبی تکان دهید. هرگز از قاشق چای‌خوری خانگی استفاده نکنید و خط سرنگ را هم‌سطح چشم بخوانید.',
    isCritical: true,
  },
  {
    id: 'check_dose_time_interval',
    category: 'TIMING_GIVING',
    stepNumberFa: 'گام ۵',
    titleFa: 'ساعت آخرین نوبت و حداقل فاصله ایمن کنترل شد',
    preventsErrorFa: 'تکرار زودهنگام دوز یا دوباره‌کاری بین مادر و پدر',
    detailTipFa:
      'فاصله ایمن استامینوفن معمولاً ۴ تا ۶ ساعت و ایبوپروفن ۶ تا ۸ ساعت است؛ پیش از مصرف، خط زمانی ۲۴ ساعته را با همسر خود هماهنگ کنید.',
    isCritical: true,
  },
  {
    id: 'check_no_hidden_overlap',
    category: 'TIMING_GIVING',
    stepNumberFa: 'گام ۶',
    titleFa: 'عدم تداخل یا ماده تکراری (مثل استامینوفن پنهان) بررسی شد',
    preventsErrorFa: 'مسمومیت ناشی از هم‌پوشانی شربت سرماخوردگی و تب‌بر',
    detailTipFa:
      'بسیاری از شربت‌های سرماخوردگی یا سرفه کودکان حاوی استامینوفن یا آنتی‌هیستامین هستند؛ ترکیبات داروهای هم‌زمان را مقایسه کنید.',
    isCritical: true,
  },
  {
    id: 'check_allergy_and_hydration',
    category: 'TIMING_GIVING',
    stepNumberFa: 'گام ۷',
    titleFa: 'حساسیت‌های دارویی کودک و دریافت مایعات کافی چک شد',
    preventsErrorFa: 'واکنش آلرژیک یا تحریک معده و کلیه هنگام کم‌آبی',
    detailTipFa:
      'پیش از دادن دارو، لیست حساسیت‌ها را چک کنید. در صورت کم‌آبی شدید، بی‌حالی یا استفراغ مداوم، پیش از دادن ایبوپروفن با پزشک مشورت کنید.',
    isCritical: false,
  },
  {
    id: 'check_record_and_childproof_cap',
    category: 'TIMING_GIVING',
    stepNumberFa: 'گام ۸',
    titleFa: 'ثبت فوری ساعت و حجم (mL) و بستن درب ایمنی دارو انجام می‌شود',
    preventsErrorFa: 'فراموشی نوبت بعدی و دسترسی تصادفی کودک به بطری',
    detailTipFa:
      'بلافاصله پس از خوراندن دارو، مقدار mL و ساعت دقیق را در دفترچه ثبت کنید و بطری را در قفسه مرتفع و قفل‌دار بگذارید.',
    isCritical: false,
  },
];

export function evaluateChecklistProgress(checkedIds: Set<string>): MedicationChecklistProgress {
  const validIds = new Set(MEDICATION_SAFETY_CHECKLIST_ITEMS.map((i) => i.id));
  const validChecked = Array.from(checkedIds).filter((id) => validIds.has(id));
  const criticalItems = MEDICATION_SAFETY_CHECKLIST_ITEMS.filter((i) => i.isCritical);
  const criticalCheckedCount = criticalItems.filter((i) => checkedIds.has(i.id)).length;
  const totalCount = MEDICATION_SAFETY_CHECKLIST_ITEMS.length;
  const checkedCount = validChecked.length;
  const criticalTotalCount = criticalItems.length;
  const isAllCriticalReady = criticalCheckedCount === criticalTotalCount;
  const isAllCompleted = checkedCount === totalCount;
  const progressFraction = totalCount === 0 ? 0 : Math.min(1, Math.max(0, checkedCount / totalCount));

  let statusHeadlineFa: string;
  let statusSublineFa: string;
  if (isAllCompleted) {
    statusHeadlineFa = 'همه مراحل چک‌لیست ایمنی تکمیل شد';
    statusSublineFa =
      'تمام کنترل‌های حیاتی و مراقبتی انجام شده است؛ می‌توانید نوبت را با خیال راحت در خط زمانی ثبت کنید.';
  } else if (isAllCriticalReady) {
    statusHeadlineFa = `تمام مراحل حیاتی ایمنی تایید شد (${checkedCount} از ${totalCount})`;
    statusSublineFa =
      'پیش‌نیازهای کلیدی پیشگیری از خطای دوز و ساعت بررسی شده‌اند؛ موارد تکمیلی باقی‌مانده را هم مرور کنید.';
  } else if (checkedCount === 0) {
    statusHeadlineFa = 'پیش از دادن دارو، موارد زیر را تیک بزنید';
    statusSublineFa = `${criticalTotalCount} گام حیاتی برای جلوگیری از اشتباه در دوز، غلظت، تاریخ انقضا و تداخل زمانی آماده بررسی است.`;
  } else {
    const remaining = criticalTotalCount - criticalCheckedCount;
    statusHeadlineFa = `در حال بررسی ایمنی مصرف (${checkedCount} از ${totalCount})`;
    statusSublineFa = `${remaining} گام حیاتی دیگر (بررسی برچسب، غلظت، سرنگ یا ساعت) پیش از خوراندن دارو باقی مانده است.`;
  }

  return {
    checkedCount,
    totalCount,
    criticalCheckedCount,
    criticalTotalCount,
    isAllCriticalReady,
    isAllCompleted,
    progressFraction,
    statusHeadlineFa,
    statusSublineFa,
  };
}
