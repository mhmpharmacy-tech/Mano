import { AGE_BANDS, type AgeBandId, type ChildProfile } from './models.ts';
import { normalizePersian } from './persianNormalizer.ts';

export interface ClinicalAgeResolution {
  status: 'known' | 'unknown' | 'conflict' | 'invalid';
  ageMonths: number | null;
  source: 'birth-date' | 'exact-age' | 'none';
  messageFa: string | null;
}

export function ageBandFromMonths(ageMonths: number | null | undefined): AgeBandId | null {
  if (ageMonths === null || ageMonths === undefined || !Number.isFinite(ageMonths) || ageMonths < 0 || ageMonths > 144) return null;
  if (ageMonths < 3) return 'NEWBORN';
  if (ageMonths < 12) return 'INFANT_3_12M';
  if (ageMonths < 36) return 'TODDLER_1_3Y';
  if (ageMonths < 72) return 'PRESCHOOL_3_6Y';
  return 'SCHOOL_6_12Y';
}

export function parseAgeMonthsFromText(raw: string): number | null {
  const clean = normalizePersian(raw);
  if (!clean || AGE_BANDS.some((band) => normalizePersian(band.labelFa) === clean)) return null;
  if (/\d+\s*(?:تا|الی|-|–|to)\s*\d+/i.test(clean)) return null;
  const match = clean.match(/^(?:(\d{1,2})\s*سال)?(?:\s*(?:و\s*)?(\d{1,3})\s*ماه)?$/);
  if (!match || (!match[1] && !match[2])) return null;
  const total = Number(match[1] || 0) * 12 + Number(match[2] || 0);
  return ageBandFromMonths(total) === null ? null : total;
}

/** Resolve legacy sources together; disagreement must be corrected in the profile. */
export function resolveClinicalAge(
  profile: Pick<ChildProfile, 'ageMonthsExact' | 'birthDateEpochMs' | 'birthDateOrAgeFa'> | null | undefined,
  externalBirthDateEpochMs?: number | null,
  nowMs = Date.now(),
): ClinicalAgeResolution {
  const failed = (status: 'conflict' | 'invalid', messageFa: string): ClinicalAgeResolution => ({
    status, ageMonths: null, source: 'none', messageFa,
  });
  const births = [profile?.birthDateEpochMs, externalBirthDateEpochMs].filter((value): value is number => value !== null && value !== undefined);
  if (births.some((birth) => !Number.isFinite(birth) || birth <= 0 || birth > nowMs)) {
    return failed('invalid', 'تاریخ تولد پرونده معتبر نیست؛ پیش از استفاده از ابزارها، آن را در پرونده کودک اصلاح یا حذف کنید.');
  }
  const localDay = (epochMs: number) => {
    const date = new Date(epochMs);
    return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
  };
  if (births.length > 1 && localDay(births[0]) !== localDay(births[1])) {
    return failed('conflict', 'دو تاریخ تولد متفاوت در پرونده و واکسیناسیون ثبت شده است؛ تاریخ درست را در پرونده کودک مشخص کنید.');
  }
  const numericAge = profile?.ageMonthsExact;
  if (numericAge !== null && numericAge !== undefined && ageBandFromMonths(numericAge) === null) {
    return failed('invalid', 'سن ثبت‌شده باید عددی از ۰ تا ۱۴۴ ماه باشد؛ سن پرونده کودک را اصلاح کنید.');
  }
  const textAge = parseAgeMonthsFromText(profile?.birthDateOrAgeFa ?? '');
  if (numericAge !== null && numericAge !== undefined && textAge !== null && Math.floor(numericAge) !== textAge) {
    return failed('conflict', 'سن عددی و متن سن پرونده با هم سازگار نیستند؛ سن واقعی را در پرونده کودک ثبت کنید.');
  }
  const explicitAge = numericAge ?? textAge;
  if (births.length > 0) {
    const birth = new Date(births[0]);
    const now = new Date(nowMs);
    let months = (now.getFullYear() - birth.getFullYear()) * 12 + now.getMonth() - birth.getMonth();
    if (now.getDate() < birth.getDate()) months -= 1;
    if (ageBandFromMonths(months) === null) return failed('invalid', 'تاریخ تولد خارج از محدوده سنی این ابزار است؛ پرونده کودک را بررسی کنید.');
    if (explicitAge !== null && explicitAge !== undefined && Math.floor(explicitAge) !== months) {
      return failed('conflict', 'سن عددی با تاریخ تولد ثبت‌شده سازگار نیست؛ تاریخ تولد را اصلاح کنید یا برای استفاده از سن عددی، حذف آن را در پرونده کودک تأیید کنید.');
    }
    return { status: 'known', ageMonths: months, source: 'birth-date', messageFa: null };
  }
  if (explicitAge !== null && explicitAge !== undefined) return { status: 'known', ageMonths: explicitAge, source: 'exact-age', messageFa: null };
  return { status: 'unknown', ageMonths: null, source: 'none', messageFa: 'سن واقعی کودک هنوز ثبت نشده است.' };
}

export function shouldShowFeverAlert(temperatureCelsius: number, age: ClinicalAgeResolution): boolean {
  if (!Number.isFinite(temperatureCelsius)) return false;
  return temperatureCelsius > 39 || (temperatureCelsius >= 38 && (age.status !== 'known' || age.ageMonths! < 3));
}
