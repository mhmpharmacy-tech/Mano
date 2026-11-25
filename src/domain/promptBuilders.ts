import { AGE_BANDS, type ChildProfile } from './models.ts';
import type { FeverDoseTimelineSnapshot } from './feverDoseTimelineEngine.ts';
import { formatTempC } from './feverDoseTimelineEngine.ts';

export function buildChildContextSummaryFa(profile: ChildProfile | null | undefined): string {
  if (!profile) return 'مشخصات کودک ثبت نشده است.';
  const bandLabel = AGE_BANDS.find((b) => b.id === profile.ageBand)?.labelFa ?? profile.ageBand;
  const ageExact =
    profile.ageMonthsExact !== null && profile.ageMonthsExact !== undefined
      ? `${profile.ageMonthsExact} ماهه`
      : profile.birthDateOrAgeFa || bandLabel;
  const sexLabel = profile.sex === 'boy' ? 'پسر' : profile.sex === 'girl' ? 'دختر' : 'نامشخص';
  const weightText = profile.weightKgText.trim() ? `${profile.weightKgText.trim()} kg` : 'ثبت نشده';
  const allergyText = profile.allergiesFa.trim() || 'ثبت نشده';
  return `نام: ${profile.aliasFa} | سن: ${ageExact} | جنسیت: ${sexLabel} | وزن: ${weightText} | حساسیت: ${allergyText}`;
}

export function buildDoctorVisitPrompt(params: {
  profile: ChildProfile | null | undefined;
  timelineSnapshot: FeverDoseTimelineSnapshot;
  chiefComplaintFa: string;
  selectedSymptomsFa: string[];
  parentNotesFa: string;
}): string {
  const { profile, timelineSnapshot, chiefComplaintFa, selectedSymptomsFa, parentNotesFa } = params;
  const lines: string[] = [
    'اطلاعات ثبت‌شده توسط والد در اپلیکیشن مانو کید برای خلاصه ویزیت پزشک اطفال:',
    `• پرونده کودک: ${buildChildContextSummaryFa(profile)}`,
    `• علت اصلی مراجعه / شکایت فعلی: ${chiefComplaintFa.trim() || 'ذکر نشده'}`,
  ];

  if (selectedSymptomsFa.length > 0) {
    lines.push(`• علائم انتخاب‌شده: ${selectedSymptomsFa.join('، ')}`);
  }

  if (timelineSnapshot.tempPoints.length > 0) {
    lines.push('• دمای ثبت‌شده در ۲۴ ساعت اخیر:');
    timelineSnapshot.tempPoints.slice(-8).forEach((t) => {
      lines.push(`  - ساعت ${t.timeLabelFa}: ${formatTempC(t.tempCelsius)} (${t.siteFa}) — ثبت توسط ${t.caregiverFa}`);
    });
  } else {
    lines.push('• دمایی در ۲۴ ساعت اخیر ثبت نشده است.');
  }

  if (timelineSnapshot.dosePoints.length > 0) {
    lines.push('• داروهای مصرف‌شده در ۲۴ ساعت اخیر:');
    timelineSnapshot.dosePoints.slice(-8).forEach((d) => {
      lines.push(`  - ساعت ${d.givenAtLabelFa}: ${d.medicationNameFa} به مقدار ${d.volumeMlText}${d.noteFa ? ` (${d.noteFa})` : ''}`);
    });
  } else {
    lines.push('• دارویی در ۲۴ ساعت اخیر ثبت نشده است.');
  }

  if (parentNotesFa.trim()) {
    lines.push(`• یادداشت و سوالات والد: ${parentNotesFa.trim()}`);
  }

  lines.push(
    'لطفاً فقط داده‌های بالا را به صورت ساختارمند در ۵ بخش مشخص‌شده تنظیم کن تا مادر/پدر بتواند متن را در مطب به پزشک نشان دهد.',
  );

  return lines.join('\n');
}
