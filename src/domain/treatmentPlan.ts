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
  intervalHours: number | null;
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
  /** Old bundled examples are retained for inspection, never as active instructions. */
  legacyUnreviewedMedications?: TreatmentMedicationItem[];
}

export function defaultPediatricTreatmentMedications(): TreatmentMedicationItem[] {
  return [];
}

export function createDefaultTreatmentPlan(): PediatricTreatmentPlan {
  return {
    pharmacyNameFa: '',
    pharmacistBadgeFa: '',
    fallbackChildSummaryFa: 'پروفایل کودک ثبت نشده است',
    prescriptionDateFa: 'پیش‌نویس محلی مراقب • بدون تأیید بالینی',
    storageInstructionFa: '',
    smartRemindersEnabled: false,
    lastCheckoffDayKey: currentDayKey(),
    medications: defaultPediatricTreatmentMedications(),
    legacyUnreviewedMedications: [],
  };
}

const LEGACY_DEMO_MEDICATION_IDS = new Set([
  'rx_azithromycin_200',
  'rx_acetaminophen_160',
  'rx_saline_spray_09',
]);

const LEGACY_DEMO_METADATA = {
  pharmacyNameFa: 'داروخانه شبانه‌روزی دکتر رضایی',
  pharmacistBadgeFa: 'تأیید دکتر داروساز (نظام: ۱۲۳۴۵)',
  fallbackChildSummaryFa: 'آراد عزیز • ۴ ساله (۱۶ کیلوگرم)',
  prescriptionDateFa: 'نسخه فعال • قابل ویرایش بر اساس دستور پزشک',
  storageInstructionFa:
    'تمام داروهای این نسخه در دمای محیط (زیر ۲۵ درجه سانتی‌گراد)، دور از نور مستقیم و دسترس کودکان نگهداری شوند (نیازی به یخچال نیست؛ سوسپانسیون آماده‌شده حداکثر تا ۱۰ روز قابل مصرف است).',
} as const;

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
  const legacyRows = plan.medications.filter((med) => LEGACY_DEMO_MEDICATION_IDS.has(med.id));
  const metadataUpdates: Partial<PediatricTreatmentPlan> = {};
  const defaults = createDefaultTreatmentPlan();
  for (const field of Object.keys(LEGACY_DEMO_METADATA) as (keyof typeof LEGACY_DEMO_METADATA)[]) {
    if (plan[field] === LEGACY_DEMO_METADATA[field]) metadataUpdates[field] = defaults[field];
  }
  const dayChanged = plan.lastCheckoffDayKey !== todayKey;
  if (!dayChanged && !legacyRows.length && !Object.keys(metadataUpdates).length && !plan.smartRemindersEnabled) {
    return plan;
  }
  const activeRows = legacyRows.length
    ? plan.medications.filter((med) => !LEGACY_DEMO_MEDICATION_IDS.has(med.id))
    : plan.medications;
  const normalized = {
    ...plan,
    ...metadataUpdates,
    // Automatic reminders have never been implemented; an old preference cannot enable them.
    smartRemindersEnabled: false,
    lastCheckoffDayKey: todayKey,
    medications: dayChanged
      ? activeRows.map((med) => ({ ...med, completedSlotTimesFa: {} }))
      : activeRows,
  };
  if (legacyRows.length) {
    normalized.legacyUnreviewedMedications = [
      ...(plan.legacyUnreviewedMedications ?? []),
      ...legacyRows,
    ];
  }
  return normalized;
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

export function treatmentMedicationEntryError(
  entry: Pick<TreatmentMedicationItem, 'nameWithStrengthFa' | 'doseAmountFa' | 'scheduleLabelFa' | 'dailyDoseSlots'>,
): string | null {
  if (!entry.nameWithStrengthFa.trim() || !entry.doseAmountFa.trim() || !entry.scheduleLabelFa.trim()) {
    return 'نام و غلظت دارو، مقدار هر نوبت و دستور زمان‌بندی را از روی نسخه یا برچسب دارو وارد کنید.';
  }
  if (!Number.isInteger(entry.dailyDoseSlots) || entry.dailyDoseSlots < 1 || entry.dailyDoseSlots > 6) {
    return 'تعداد نوبت قابل ثبت را خودتان به‌صورت عدد صحیح بین ۱ تا ۶ وارد کنید.';
  }
  return null;
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
    'یادداشت دارویی مراقب (مانو کید)',
    'پیش‌نویس محلی • بدون تأیید پزشک یا داروساز',
    `کودک: ${resolvePlanChildSummary(plan, profile)}`,
    profile?.allergiesFa.trim()
      ? `حساسیت ثبت‌شده: ${profile.allergiesFa.trim()}`
      : 'حساسیت دارویی ثبت‌نشده — پیش از مصرف بررسی شود',
    '────────────────────',
    'اطلاعات واردشده را پیش از مصرف با دستور مکتوب پزشک یا برچسب داروخانه تطبیق دهید.',
    '',
  ];

  plan.medications.forEach((med, idx) => {
    const formLabel =
      TREATMENT_DOSAGE_FORMS.find((f) => f.id === med.form)?.labelFa ?? med.form;
    const doneCount = Object.keys(med.completedSlotTimesFa || {}).length;
    lines.push(`${toPersianDigits(idx + 1)}) ${med.nameWithStrengthFa} (${formLabel})`);
    lines.push(`   • دوز: ${med.doseAmountFa}`);
    lines.push(`   • زمان‌بندی ثبت‌شده: ${med.scheduleLabelFa}`);
    if (med.foodRelationFa.trim()) lines.push(`   • دستور ثبت‌شده درباره غذا: ${med.foodRelationFa}`);
    if (med.counselingNoteFa.trim()) {
      lines.push(`   • یادداشت ثبت‌شده: ${med.counselingNoteFa}`);
    }
    lines.push(
      `   • نوبت‌های ثبت‌شده امروز: ${toPersianDigits(doneCount)} از ${toPersianDigits(med.dailyDoseSlots)}`,
    );
    lines.push('');
  });

  if (plan.storageInstructionFa.trim()) {
    lines.push(`شرایط نگهداری ثبت‌شده: ${plan.storageInstructionFa}`);
  }
  lines.push(
    'هشدار فوری: در صورت بروز کهیر، تورم لب/پلک یا تنگی نفس، دارو را فوراً قطع کرده و با اورژانس ۱۱۵ یا پزشک تماس بگیرید.',
  );
  return lines.join('\n');
}
