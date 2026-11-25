import type {
  VaccineDashboardSummary,
  VaccineFilter,
  VaccineRecord,
  VaccineReminderSettings,
  VaccineResolvedItem,
  VaccineScheduleEntry,
  VaccineStatus,
} from './models.ts';
import { toAsciiDigits, toPersianDigits } from './persianNormalizer.ts';

export const VACCINE_SCHEDULE_CATALOG: VaccineScheduleEntry[] = [
  {
    id: 'birth_bcg_hbv_opv',
    titleFa: 'واکسن‌های بدو تولد (ب‌ث‌ژ، هپاتیت B و فلج اطفال)',
    milestoneLabelFa: 'بدو تولد',
    recommendedAgeDays: 0,
    vaccinesIncludedFa: 'ب‌ث‌ژ (سل) + هپاتیت B نوبت ۰ + فلج اطفال خوراکی بدو تولد',
    administrationMethodFa: 'تزریقی و قطره خوراکی در بیمارستان یا مرکز بهداشت پیش از ترخیص',
    careNotesFa:
      'مراقب جای زخم واکسن ب‌ث‌ژ باشید؛ از دستکاری یا پماد زدن خودسرانه پرهیز کنید و تاریخ نوبت ۲ ماهگی را یادداشت نمایید.',
  },
  {
    id: '2m_pentavalent_opv_rota',
    titleFa: 'واکسیناسیون ۲ ماهگی (پنج‌گانه نوبت ۱)',
    milestoneLabelFa: '۲ ماهگی',
    recommendedAgeDays: 60,
    vaccinesIncludedFa:
      'واکسن پنج‌گانه (دیفتری، کزاز، سیاه‌سرفه، هپاتیت B، هموفیلوس آنفلوانزا) + فلج اطفال خوراکی ۱ (+ روتاویروس/پنوموکوک طبق برنامه)',
    administrationMethodFa: 'تزریق عضلانی ران و قطره خوراکی در مرکز بهداشت',
    careNotesFa:
      'پس از واکسن پنج‌گانه احتمال تب خفیف تا ۲۴–۴۸ ساعت و بی‌قراری طبیعی است؛ دوز استامینوفن فقط بر اساس وزن کودک و دستور پزشک/مرکز بهداشت تنظیم شود.',
  },
  {
    id: '4m_pentavalent_opv_ipv',
    titleFa: 'واکسیناسیون ۴ ماهگی (پنج‌گانه نوبت ۲ + IPV)',
    milestoneLabelFa: '۴ ماهگی',
    recommendedAgeDays: 120,
    vaccinesIncludedFa: 'واکسن پنج‌گانه نوبت ۲ + فلج اطفال خوراکی ۲ + فلج اطفال تزریقی (IPV)',
    administrationMethodFa: 'تزریق عضلانی و قطره خوراکی',
    careNotesFa:
      'نشانه‌های موضعی مثل تورم یا درد محل تزریق با کمپرس خنک تمیز کاهش می‌یابد؛ در تب بالای ۳۹ درجه یا بی‌حالی شدید با پزشک تماس بگیرید.',
  },
  {
    id: '6m_pentavalent_opv',
    titleFa: 'واکسیناسیون ۶ ماهگی (پنج‌گانه نوبت ۳)',
    milestoneLabelFa: '۶ ماهگی',
    recommendedAgeDays: 180,
    vaccinesIncludedFa: 'واکسن پنج‌گانه نوبت ۳ + فلج اطفال خوراکی نوبت ۳',
    administrationMethodFa: 'تزریق عضلانی و قطره خوراکی',
    careNotesFa:
      'همزمان با ویزیت ۶ ماهگی، پایش وزن، قد و شروع تغذیه تکمیلی و قطره آهن/مولتی‌ویتامین را با پزشک یا مرکز بهداشت مرور کنید.',
  },
  {
    id: '12m_mmr1',
    titleFa: 'واکسیناسیون ۱۲ ماهگی (MMR نوبت اول)',
    milestoneLabelFa: '۱۲ ماهگی (۱ سال)',
    recommendedAgeDays: 365,
    vaccinesIncludedFa: 'واکسن سه‌گانه سرخک، سرخجه و اوریون (MMR) نوبت اول',
    administrationMethodFa: 'تزریق زیرجلدی در مرکز بهداشت',
    careNotesFa:
      'گاهی ۵ تا ۱۲ روز پس از تزریق MMR تب خفیف یا دانه پوستی گذرا دیده می‌شود؛ در صورت حساسیت شدید غذایی یا بیماری حاد به کادر درمان اطلاع دهید.',
  },
  {
    id: '18m_mmr2_dtp_opv',
    titleFa: 'واکسیناسیون ۱۸ ماهگی (یادآور اول)',
    milestoneLabelFa: '۱۸ ماهگی',
    recommendedAgeDays: 545,
    vaccinesIncludedFa: 'یادآور سه‌گانه (دیفتری، کزاز، سیاه‌سرفه) + MMR نوبت دوم + فلج اطفال خوراکی یادآور',
    administrationMethodFa: 'تزریقی و قطره خوراکی',
    careNotesFa: 'پیش از مراجعه، کودک را تغذیه سبک بدهید و کارت واکسیناسیون را همراه داشته باشید.',
  },
  {
    id: '6y_dtp_opv',
    titleFa: 'واکسیناسیون ۶ سالگی (پیش از دبستان)',
    milestoneLabelFa: '۶ سالگی',
    recommendedAgeDays: 2190,
    vaccinesIncludedFa: 'یادآور سه‌گانه (ثلاث) + فلج اطفال خوراکی پیش از ورود به مدرسه',
    administrationMethodFa: 'تزریقی و قطره خوراکی در مرکز بهداشت',
    careNotesFa: 'گواهی تکمیل واکسیناسیون ۶ سالگی معمولاً برای ثبت‌نام کلاس اول دبستان ضروری است.',
  },
  {
    id: 'seasonal_influenza',
    titleFa: 'واکسن سالانه آنفلوانزا (۶ ماهگی به بالا)',
    milestoneLabelFa: 'سالانه / اختیاری',
    recommendedAgeDays: 210,
    vaccinesIncludedFa: 'واکسن فصلی آنفلوانزا (تزریق با صلاحدید پزشک اطفال، به‌ویژه در فصل پاییز)',
    administrationMethodFa: 'تزریق عضلانی (در نخستین سال زیر ۹ سال ممکن است ۲ دوز با فاصله ۱ ماه نیاز باشد)',
    careNotesFa: 'نوع و دوز مناسب سن را مستقیماً با پزشک کودک هماهنگ کنید.',
    isOptionalSeasonal: true,
  },
];

export const VACCINE_SCIENTIFIC_NOTE_FA =
  'برنامه مرجع بر اساس جدول ایمن‌سازی کشوری و توصیه‌های CDC/WHO تنظیم شده است. تاریخ دقیق مراجعات بعدی همیشه باید با کارت واکسن کودک و دستور پزشک یا مرکز بهداشت محل زندگی تطبیق داده شود.';

export const VACCINE_LEAD_DAY_OPTIONS = [1, 3, 7];

export function startOfDayEpochMs(epochMs: number): number {
  const d = new Date(epochMs);
  d.setHours(0, 0, 0, 0);
  return d.getTime();
}

export function addDaysToDay(startDayMs: number, days: number): number {
  const d = new Date(startOfDayEpochMs(startDayMs));
  d.setDate(d.getDate() + days);
  return d.getTime();
}

export function diffDays(fromDayMs: number, toDayMs: number): number {
  const from = startOfDayEpochMs(fromDayMs);
  const to = startOfDayEpochMs(toDayMs);
  return Math.round((to - from) / 86_400_000);
}

export function gregorianToJalali(gy: number, gm: number, gd: number): [number, number, number] {
  const gdm = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
  const gy2 = gm > 2 ? gy + 1 : gy;
  let days =
    355666 +
    365 * gy +
    Math.floor((gy2 + 3) / 4) -
    Math.floor((gy2 + 99) / 100) +
    Math.floor((gy2 + 399) / 400) +
    gd +
    gdm[gm - 1];
  let jy = -1595 + 33 * Math.floor(days / 12053);
  days %= 12053;
  jy += 4 * Math.floor(days / 1461);
  days %= 1461;
  if (days > 365) {
    jy += Math.floor((days - 1) / 365);
    days = (days - 1) % 365;
  }
  const jm = days < 186 ? 1 + Math.floor(days / 31) : 7 + Math.floor((days - 186) / 30);
  const jd = 1 + (days < 186 ? days % 31 : (days - 186) % 30);
  return [jy, jm, jd];
}

export function jalaliToGregorian(jyInput: number, jm: number, jd: number): [number, number, number] {
  const jy = jyInput + 1595;
  let days =
    -355668 +
    365 * jy +
    Math.floor(jy / 33) * 8 +
    Math.floor(((jy % 33) + 3) / 4) +
    jd +
    (jm < 7 ? (jm - 1) * 31 : (jm - 7) * 30 + 186);
  let gy = 400 * Math.floor(days / 146097);
  days %= 146097;
  if (days > 36524) {
    days -= 1;
    gy += 100 * Math.floor(days / 36524);
    days %= 36524;
    if (days >= 365) days += 1;
  }
  gy += 4 * Math.floor(days / 1461);
  days %= 1461;
  if (days > 365) {
    gy += Math.floor((days - 1) / 365);
    days = (days - 1) % 365;
  }
  let gd = days + 1;
  const isLeap = (gy % 4 === 0 && gy % 100 !== 0) || gy % 400 === 0;
  const salA = [0, 31, isLeap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  let gm = 0;
  while (gm < 13 && gd > salA[gm]) {
    gd -= salA[gm];
    gm += 1;
  }
  return [gy, gm, gd];
}

export function formatPersianDate(epochMs: number): string {
  const d = new Date(epochMs);
  const [jy, jm, jd] = gregorianToJalali(d.getFullYear(), d.getMonth() + 1, d.getDate());
  const raw = `${String(jy).padStart(4, '0')}/${String(jm).padStart(2, '0')}/${String(jd).padStart(2, '0')}`;
  return toPersianDigits(raw);
}

export function parseFlexibleDateToEpochMs(rawInput: string): number | null {
  const normalized = toAsciiDigits(rawInput.trim()).replace(/[-.]/g, '/');
  const parts = normalized.split('/').map((p) => Number.parseInt(p.trim(), 10));
  if (parts.length !== 3 || parts.some((n) => !Number.isFinite(n))) return null;
  const [year, month, day] = parts;
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;

  let gy: number;
  let gm: number;
  let gd: number;

  if (year >= 1350 && year <= 1500) {
    const maxDay = month <= 6 ? 31 : 30;
    if (day > maxDay) return null;
    [gy, gm, gd] = jalaliToGregorian(year, month, day);
    const [cy, cm, cd] = gregorianToJalali(gy, gm, gd);
    if (cy !== year || cm !== month || cd !== day) return null;
  } else if (year >= 1950 && year <= 2100) {
    [gy, gm, gd] = [year, month, day];
  } else {
    return null;
  }

  const d = new Date(gy, gm - 1, gd, 0, 0, 0, 0);
  if (d.getFullYear() !== gy || d.getMonth() !== gm - 1 || d.getDate() !== gd) {
    return null;
  }
  return d.getTime();
}

export function resolveVaccineSchedule(
  records: VaccineRecord[],
  birthDateEpochMs: number | null,
  reminderSettings: VaccineReminderSettings = { enabled: false, leadDaysBefore: 3 },
  nowEpochMs = Date.now(),
): VaccineDashboardSummary {
  const recordMap = new Map<string, VaccineRecord>();
  records.forEach((rec) => {
    const existing = recordMap.get(rec.vaccineId);
    if (!existing || rec.updatedAtMs >= existing.updatedAtMs) {
      recordMap.set(rec.vaccineId, rec);
    }
  });

  const todayStart = startOfDayEpochMs(nowEpochMs);
  const leadDays = Math.min(30, Math.max(1, reminderSettings.leadDaysBefore));

  const items: VaccineResolvedItem[] = VACCINE_SCHEDULE_CATALOG.map((entry) => {
    const record = recordMap.get(entry.id);
    let effectiveDueDateEpochMs: number | null = null;
    let dueDateSourceFa = 'در انتظار ثبت تاریخ تولد یا موعد';

    if (record?.customDueDateEpochMs) {
      effectiveDueDateEpochMs = startOfDayEpochMs(record.customDueDateEpochMs);
      dueDateSourceFa = 'تاریخ ثبت‌شده توسط والد';
    } else if (birthDateEpochMs) {
      effectiveDueDateEpochMs = addDaysToDay(startOfDayEpochMs(birthDateEpochMs), entry.recommendedAgeDays);
      dueDateSourceFa = 'محاسبه‌شده بر اساس تاریخ تولد';
    }

    const administeredDateEpochMs = record?.administeredDateEpochMs
      ? startOfDayEpochMs(record.administeredDateEpochMs)
      : null;

    const daysUntilDue =
      effectiveDueDateEpochMs !== null ? diffDays(todayStart, effectiveDueDateEpochMs) : null;

    let status: VaccineStatus = 'UPCOMING';
    if (administeredDateEpochMs !== null) {
      status = 'ADMINISTERED';
    } else if (record?.isDeferred) {
      status = 'DEFERRED';
    } else if (daysUntilDue !== null && daysUntilDue < 0) {
      status = 'OVERDUE';
    } else if (daysUntilDue !== null && daysUntilDue <= leadDays) {
      status = 'DUE_SOON';
    }

    return {
      definition: entry,
      record,
      effectiveDueDateEpochMs,
      administeredDateEpochMs,
      daysUntilDue,
      status,
      dueDateSourceFa,
    };
  });

  const coreItems = items.filter((i) => !i.definition.isOptionalSeasonal);
  const administeredCoreCount = coreItems.filter((i) => i.status === 'ADMINISTERED').length;
  const dueSoonCount = items.filter((i) => i.status === 'DUE_SOON').length;
  const overdueCount = items.filter((i) => i.status === 'OVERDUE').length;

  const nextUpcoming =
    items
      .filter((i) => i.status === 'DUE_SOON' || i.status === 'UPCOMING' || i.status === 'OVERDUE')
      .sort((a, b) => {
        const da = a.effectiveDueDateEpochMs ?? Number.MAX_SAFE_INTEGER;
        const db = b.effectiveDueDateEpochMs ?? Number.MAX_SAFE_INTEGER;
        if (da !== db) return da - db;
        return a.definition.recommendedAgeDays - b.definition.recommendedAgeDays;
      })[0] ?? null;

  const dueForReminder = reminderSettings.enabled
    ? items.filter(
        (i) =>
          i.effectiveDueDateEpochMs !== null &&
          i.daysUntilDue !== null &&
          i.daysUntilDue <= leadDays &&
          (i.status === 'DUE_SOON' || i.status === 'OVERDUE'),
      )
    : [];

  return {
    items,
    totalCoreCount: coreItems.length,
    administeredCoreCount,
    dueSoonCount,
    overdueCount,
    nextUpcoming,
    dueForReminder,
  };
}

export function filterVaccineItems(
  items: VaccineResolvedItem[],
  filter: VaccineFilter,
): VaccineResolvedItem[] {
  switch (filter) {
    case 'ALL':
      return items;
    case 'ACTIONABLE':
      return items.filter((i) => i.status === 'DUE_SOON' || i.status === 'UPCOMING');
    case 'OVERDUE':
      return items.filter((i) => i.status === 'OVERDUE');
    case 'ADMINISTERED':
      return items.filter((i) => i.status === 'ADMINISTERED');
  }
}

export function formatRelativeDueLabelFa(item: VaccineResolvedItem): string {
  if (item.status === 'ADMINISTERED' && item.administeredDateEpochMs !== null) {
    return `تزریق‌شده در ${formatPersianDate(item.administeredDateEpochMs)}`;
  }
  if (item.status === 'DEFERRED') {
    return 'به تعویق افتاده با نظر پزشک';
  }
  const d = item.daysUntilDue;
  if (d === null) return 'تاریخ موعد ثبت نشده';
  if (d === 0) return 'موعد تزریق: امروز';
  if (d === 1) return 'فردا (۱ روز مانده)';
  if (d > 1) return `${toPersianDigits(d)} روز تا موعد`;
  if (d === -1) return '۱ روز از موعد گذشته';
  return `${toPersianDigits(-d)} روز از موعد گذشته`;
}
