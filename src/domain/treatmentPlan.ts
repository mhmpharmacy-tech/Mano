import type { ChildProfile } from './models.ts';
import { toPersianDigits } from './persianNormalizer.ts';

export type TreatmentDosageFormId =
  | 'SYRUP_SUSPENSION'
  | 'DROPS'
  | 'NASAL_SPRAY'
  | 'OINTMENT_CREAM'
  | 'TABLET_SACHET';

export const TREATMENT_DOSAGE_FORMS: {
  id: TreatmentDosageFormId;
  labelFa: string;
  defaultUnitHintFa: string;
}[] = [
  { id: 'SYRUP_SUSPENSION', labelFa: 'شربت / سوسپانسیون', defaultUnitHintFa: 'سی‌سی (با سرنگ مدرج)' },
  { id: 'DROPS', labelFa: 'قطره', defaultUnitHintFa: 'قطره (با قطره‌چکان استاندارد)' },
  { id: 'NASAL_SPRAY', labelFa: 'اسپری بینی', defaultUnitHintFa: 'پاف در هر سوراخ بینی' },
  { id: 'OINTMENT_CREAM', labelFa: 'پماد / کرم', defaultUnitHintFa: 'لایه نازک موضعی' },
  { id: 'TABLET_SACHET', labelFa: 'قرص / ساشه', defaultUnitHintFa: 'عدد یا بسته' },
];

export interface TreatmentMedicationItem {
  id: string;
  nameWithStrengthFa: string;
  form: TreatmentDosageFormId;
  doseAmountFa: string;
  intervalHours: number;
  scheduleLabelFa: string;
  foodRelationFa: string;
  counselingNoteFa: string;
  isWarningHighlight: boolean;
  dailyDoseSlots: number;
  completedSlotTimesFa: Record<number, string>;
}

export interface PediatricTreatmentPlan {
  pharmacyNameFa: string;
  pharmacistBadgeFa: string;
  fallbackChildSummaryFa: string;
  prescriptionDateFa: string;
  storageInstructionFa: string;
  smartRemindersEnabled: boolean;
  lastCheckoffDayKey: string;
  medications: TreatmentMedicationItem[];
}

export function defaultPediatricTreatmentMedications(): TreatmentMedicationItem[] {
  return [
    {
      id: 'rx_azithromycin_200',
      nameWithStrengthFa: 'سوسپانسیون آزیترومایسین ۲۰۰ میلی‌گرم/۵ سی‌سی',
      form: 'SYRUP_SUSPENSION',
      doseAmountFa: 'روز اول ۴ سی‌سی، روز ۲ تا ۵ مقدار ۲ سی‌سی (با سرنگ)',
      intervalHours: 24,
      scheduleLabelFa: 'هر ۲۴ ساعت (ساعت ۱۲ ظهر)',
      foodRelationFa: 'همراه غذا یا پس از غذا',
      counselingNoteFa:
        'پودر را با آب جوشیده سردشده تا خط نشانه مخلوط کنید؛ پیش از هر مصرف خوب تکان دهید و دوره ۵ روزه را کامل نمایید (پایداری پس از آماده‌سازی: تا ۱۰ روز در دمای محیط).',
      isWarningHighlight: true,
      dailyDoseSlots: 1,
      completedSlotTimesFa: {},
    },
    {
      id: 'rx_acetaminophen_160',
      nameWithStrengthFa: 'شربت استامینوفن کودکان ۱۶۰ میلی‌گرم/۵ سی‌سی',
      form: 'SYRUP_SUSPENSION',
      doseAmountFa: '۷٫۵ سی‌سی (۲۴۰ میلی‌گرم با سرنگ مدرج)',
      intervalHours: 6,
      scheduleLabelFa: 'هر ۶ ساعت در صورت تب',
      foodRelationFa: 'فرقی ندارد (ناشتا یا با غذا)',
      counselingNoteFa:
        'برای وزن ۱۶ کیلوگرم شربت به‌جای ۵۰ قطره دقیق‌تر و ایمن‌تر است • حداکثر ۴ نوبت در شبانه‌روز و فاصله کمتر از ۴ تا ۶ ساعت نباشد.',
      isWarningHighlight: true,
      dailyDoseSlots: 4,
      completedSlotTimesFa: {},
    },
    {
      id: 'rx_saline_spray_09',
      nameWithStrengthFa: 'اسپری بینی سدیم کلراید ۰٫۹٪ (سرم شست‌وشو)',
      form: 'NASAL_SPRAY',
      doseAmountFa: '۱ پاف در هر سوراخ بینی',
      intervalHours: 8,
      scheduleLabelFa: 'هر ۸ ساعت',
      foodRelationFa: 'قبل از خواب و هنگام گرفتگی بینی',
      counselingNoteFa:
        'پس از هر بار استفاده، سری اسپری را با دستمال تمیز خشک کنید و به‌صورت اختصاصی برای یک کودک استفاده نمایید.',
      isWarningHighlight: false,
      dailyDoseSlots: 3,
      completedSlotTimesFa: {},
    },
  ];
}

export function createDefaultTreatmentPlan(): PediatricTreatmentPlan {
  return {
    pharmacyNameFa: 'داروخانه شبانه‌روزی دکتر رضایی',
    pharmacistBadgeFa: 'تأیید دکتر داروساز (نظام: ۱۲۳۴۵)',
    fallbackChildSummaryFa: 'آراد عزیز • ۴ ساله (۱۶ کیلوگرم)',
    prescriptionDateFa: 'نسخه فعال • قابل ویرایش بر اساس دستور پزشک',
    storageInstructionFa:
      'تمام داروهای این نسخه در دمای محیط (زیر ۲۵ درجه سانتی‌گراد)، دور از نور مستقیم و دسترس کودکان نگهداری شوند (نیازی به یخچال نیست؛ سوسپانسیون آماده‌شده حداکثر تا ۱۰ روز قابل مصرف است).',
    smartRemindersEnabled: false,
    lastCheckoffDayKey: currentDayKey(),
    medications: defaultPediatricTreatmentMedications(),
  };
}

export function currentDayKey(nowMs = Date.now()): string {
  const d = new Date(nowMs);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function currentTimeFa(nowMs = Date.now()): string {
  const d = new Date(nowMs);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return toPersianDigits(`${hh}:${mm}`);
}

export function normalizePlanForToday(
  plan: PediatricTreatmentPlan,
  todayKey = currentDayKey(),
): PediatricTreatmentPlan {
  if (plan.lastCheckoffDayKey === todayKey) return plan;
  return {
    ...plan,
    lastCheckoffDayKey: todayKey,
    medications: plan.medications.map((m) => ({ ...m, completedSlotTimesFa: {} })),
  };
}

export function getTreatmentSlotCounts(plan: PediatricTreatmentPlan) {
  let totalDailySlots = 0;
  let completedTodaySlots = 0;
  for (const med of plan.medications) {
    const slots = Math.min(6, Math.max(1, med.dailyDoseSlots));
    totalDailySlots += slots;
    for (const k of Object.keys(med.completedSlotTimesFa || {})) {
      const slotNum = Number(k);
      if (slotNum >= 1 && slotNum <= slots) {
        completedTodaySlots += 1;
      }
    }
  }
  return { totalDailySlots, completedTodaySlots };
}

export function resolvePlanChildSummary(
  plan: PediatricTreatmentPlan,
  profile: ChildProfile | null | undefined,
): string {
  if (!profile) return plan.fallbackChildSummaryFa;
  const alias = profile.aliasFa.trim() || 'کودک عزیز';
  const agePart =
    profile.ageMonthsExact !== null && profile.ageMonthsExact !== undefined
      ? profile.ageMonthsExact >= 24
        ? `${toPersianDigits(Math.floor(profile.ageMonthsExact / 12))} ساله`
        : `${toPersianDigits(profile.ageMonthsExact)} ماهه`
      : profile.birthDateOrAgeFa || 'کودک';
  const weightPart = profile.weightKgText.trim()
    ? ` (${toPersianDigits(profile.weightKgText.trim())} کیلوگرم)`
    : '';
  return `${alias} • ${agePart}${weightPart}`;
}

export function buildTreatmentCaregiverShareText(
  plan: PediatricTreatmentPlan,
  profile: ChildProfile | null | undefined,
): string {
  const lines: string[] = [
    'برنامه درمانی اختصاصی کودک (مانو کید)',
    `${plan.pharmacyNameFa} | ${plan.pharmacistBadgeFa}`,
    `کودک: ${resolvePlanChildSummary(plan, profile)}`,
    profile?.allergiesFa.trim()
      ? `حساسیت ثبت‌شده: ${profile.allergiesFa.trim()}`
      : 'حساسیت دارویی ثبت‌نشده — پیش از مصرف بررسی شود',
    '────────────────────',
    'نکته ایمنی پیمانه: همیشه از سرنگ یا پیمانه مدرج استاندارد استفاده کنید و از قاشق خانگی بپرهیزید (۱ سی‌سی = ۲۰ قطره استاندارد).',
    '',
  ];

  plan.medications.forEach((med, idx) => {
    const formLabel =
      TREATMENT_DOSAGE_FORMS.find((f) => f.id === med.form)?.labelFa ?? med.form;
    const doneCount = Object.keys(med.completedSlotTimesFa || {}).length;
    lines.push(`${toPersianDigits(idx + 1)}) ${med.nameWithStrengthFa} (${formLabel})`);
    lines.push(`   • دوز: ${med.doseAmountFa}`);
    lines.push(`   • زمان‌بندی: ${med.scheduleLabelFa} | ${med.foodRelationFa}`);
    if (med.counselingNoteFa.trim()) {
      lines.push(`   • نکته داروساز: ${med.counselingNoteFa}`);
    }
    lines.push(
      `   • نوبت‌های ثبت‌شده امروز: ${toPersianDigits(doneCount)} از ${toPersianDigits(med.dailyDoseSlots)}`,
    );
    lines.push('');
  });

  lines.push(`شرایط نگهداری: ${plan.storageInstructionFa}`);
  lines.push(
    'هشدار فوری: در صورت بروز کهیر، تورم لب/پلک یا تنگی نفس، دارو را فوراً قطع کرده و با اورژانس ۱۱۵ یا پزشک تماس بگیرید.',
  );
  return lines.join('\n');
}
