import type {
  AlertSeverity,
  ChildProfile,
  TimelineDosePoint,
  TimelineTempPoint,
} from './models.ts';
import { initialAgeMonthsFromProfile } from './liquidDoseVisualizerEngine.ts';
import { toAsciiDigits } from './persianNormalizer.ts';

const HOUR_MS = 3_600_000;
const WINDOW_24H_MS = 24 * HOUR_MS;
export const HIGH_FEVER_ALERT_THRESHOLD_C = 39.0;

export interface AntipyreticIntervalStatus {
  medFamilyLabelFa: string;
  medicationKey: 'acetaminophen' | 'ibuprofen';
  minIntervalHours: number;
  maxDoses24h: number;
  dosesLast24h: number;
  minutesSinceLastDose: number | null;
  minutesRemainingSafeInterval: number;
  lastDoseSummaryFa: string | null;
  severity: AlertSeverity;
  statusMessageFa: string;
}

export interface FeverDoseTimelineSnapshot {
  windowStartMs: number;
  windowEndMs: number;
  tempPoints: TimelineTempPoint[];
  dosePoints: TimelineDosePoint[];
  latestTempCelsius: number | null;
  peakTemp24h: number | null;
  acetaminophenStatus: AntipyreticIntervalStatus;
  ibuprofenStatus: AntipyreticIntervalStatus;
}

export type TimelineFeedItem =
  | { kind: 'temp'; key: string; timestampMs: number; temp: TimelineTempPoint }
  | { kind: 'dose'; key: string; timestampMs: number; dose: TimelineDosePoint };

export interface HighFeverClinicalAlert {
  triggerTempCelsius: number;
  caregiverFa: string;
  timeLabelFa: string;
  isHighFeverOver39: boolean;
  titleFa: string;
  bodyFa: string;
  checklistFa: string[];
}

export function resolveDoseTimeMs(labelFa: string, recordedAtMs = Date.now()): number | null {
  const ascii = toAsciiDigits(labelFa).trim();
  const match = ascii.match(/^([01]?\d|2[0-3]):([0-5]\d)$/);
  if (!match) return null;
  const hour = Number.parseInt(match[1], 10);
  const minute = Number.parseInt(match[2], 10);
  const d = new Date(recordedAtMs);
  d.setHours(hour, minute, 0, 0);
  if (d.getTime() > recordedAtMs + 60_000) {
    d.setDate(d.getDate() - 1);
  }
  const ms = d.getTime();
  return ms <= recordedAtMs + 60_000 && recordedAtMs - ms < WINDOW_24H_MS ? ms : null;
}

export function isAcetaminophenFamily(dose: TimelineDosePoint): boolean {
  const combined = `${dose.medicationSlug} ${dose.medicationNameFa} ${dose.dosageFormFa}`.toLowerCase();
  return (
    combined.includes('acetaminophen') ||
    combined.includes('paracetamol') ||
    combined.includes('استامینوفن') ||
    combined.includes('otc-cough-cold')
  );
}

export function isIbuprofenFamily(dose: TimelineDosePoint): boolean {
  const combined = `${dose.medicationSlug} ${dose.medicationNameFa} ${dose.dosageFormFa}`.toLowerCase();
  return (
    combined.includes('ibuprofen') ||
    combined.includes('بروفن') ||
    combined.includes('ایبوپروفن')
  );
}

export function formatHourMinute(timestampMs: number): string {
  const d = new Date(timestampMs);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${hh}:${mm}`;
}

export function formatDurationFa(totalMinutes: number): string {
  const clean = Math.max(0, Math.round(totalMinutes));
  const h = Math.floor(clean / 60);
  const m = clean % 60;
  if (h === 0) return `${m} دقیقه`;
  if (m === 0) return `${h} ساعت`;
  return `${h} ساعت و ${m} دقیقه`;
}

export function formatRelativeTimeFa(timestampMs: number, nowMs = Date.now()): string {
  const diffMin = Math.max(0, Math.floor((nowMs - timestampMs) / 60_000));
  return diffMin < 1 ? 'هم‌اکنون' : `${formatDurationFa(diffMin)} پیش`;
}

export function formatTempC(temp: number): string {
  return `${(Math.round(temp * 10) / 10).toFixed(1)}°C`;
}

function evaluateAntipyreticInterval(
  medFamilyLabelFa: string,
  medicationKey: 'acetaminophen' | 'ibuprofen',
  matchingDoses: TimelineDosePoint[],
  minIntervalHours: number,
  maxDoses24h: number,
  nowMs: number,
): AntipyreticIntervalStatus {
  const count = matchingDoses.length;
  const latest =
    matchingDoses.length > 0
      ? matchingDoses.reduce((acc, cur) => (cur.timestampMs > acc.timestampMs ? cur : acc))
      : null;

  if (!latest) {
    return {
      medFamilyLabelFa,
      medicationKey,
      minIntervalHours,
      maxDoses24h,
      dosesLast24h: 0,
      minutesSinceLastDose: null,
      minutesRemainingSafeInterval: 0,
      lastDoseSummaryFa: null,
      severity: 'info',
      statusMessageFa: `در ۲۴ ساعت گذشته نوبتی ثبت نشده است (حداقل فاصله ایمن معمول: هر ${minIntervalHours} ساعت).`,
    };
  }

  const elapsedMin = Math.max(0, Math.floor((nowMs - latest.timestampMs) / 60_000));
  const minReqMin = minIntervalHours * 60;
  const remainingMin = Math.max(0, minReqMin - elapsedMin);
  const summary = `${latest.volumeMlText} در ساعت ${latest.givenAtLabelFa} (${formatDurationFa(elapsedMin)} پیش)`;

  let severity: AlertSeverity = 'success';
  let msg = `حداقل فاصله ${minIntervalHours} ساعته سپری شده (${count} از ${maxDoses24h} نوبت در ۲۴ ساعت اخیر).`;

  if (count >= maxDoses24h) {
    severity = 'danger';
    msg = `هشدار سقف ۲۴ ساعته: ${count} نوبت در ۲۴ ساعت اخیر ثبت شده (سقف رایج ${maxDoses24h} نوبت). پیش از هر دوز جدید با پزشک یا داروساز مشورت کنید.`;
  } else if (remainingMin > 0) {
    severity = 'danger';
    msg = `حداقل فاصله ${minIntervalHours} ساعته هنوز کامل نشده! ${formatDurationFa(remainingMin)} تا زمان ایمن بعدی باقی مانده تا از دوز تکراری جلوگیری شود.`;
  }

  return {
    medFamilyLabelFa,
    medicationKey,
    minIntervalHours,
    maxDoses24h,
    dosesLast24h: count,
    minutesSinceLastDose: elapsedMin,
    minutesRemainingSafeInterval: remainingMin,
    lastDoseSummaryFa: summary,
    severity,
    statusMessageFa: msg,
  };
}

export function buildTimelineSnapshot(
  feverLogs: TimelineTempPoint[],
  doseLogs: TimelineDosePoint[],
  nowMs = Date.now(),
): FeverDoseTimelineSnapshot {
  const windowStart = nowMs - WINDOW_24H_MS;

  const allTemps = feverLogs
    .filter((t) => t.timestampMs >= windowStart && t.timestampMs <= nowMs + 60_000)
    .sort((a, b) => a.timestampMs - b.timestampMs);

  const allDoses = doseLogs
    .filter((d) => d.timestampMs >= windowStart && d.timestampMs <= nowMs + 60_000)
    .sort((a, b) => a.timestampMs - b.timestampMs);

  const latestTempCelsius = allTemps.length > 0 ? allTemps[allTemps.length - 1].tempCelsius : null;
  const peakTemp24h =
    allTemps.length > 0 ? Math.max(...allTemps.map((t) => t.tempCelsius)) : null;

  return {
    windowStartMs: windowStart,
    windowEndMs: nowMs,
    tempPoints: allTemps,
    dosePoints: allDoses,
    latestTempCelsius,
    peakTemp24h,
    acetaminophenStatus: evaluateAntipyreticInterval(
      'خانواده استامینوفن',
      'acetaminophen',
      allDoses.filter(isAcetaminophenFamily),
      4,
      5,
      nowMs,
    ),
    ibuprofenStatus: evaluateAntipyreticInterval(
      'خانواده ایبوپروفن',
      'ibuprofen',
      allDoses.filter(isIbuprofenFamily),
      6,
      4,
      nowMs,
    ),
  };
}

export function mergedChronologicalEvents(snapshot: FeverDoseTimelineSnapshot): TimelineFeedItem[] {
  const temps: TimelineFeedItem[] = snapshot.tempPoints.map((temp) => ({
    kind: 'temp',
    key: `temp_${temp.id}`,
    timestampMs: temp.timestampMs,
    temp,
  }));
  const doses: TimelineFeedItem[] = snapshot.dosePoints.map((dose) => ({
    kind: 'dose',
    key: `dose_${dose.id}`,
    timestampMs: dose.timestampMs,
    dose,
  }));
  return [...temps, ...doses].sort((a, b) => b.timestampMs - a.timestampMs);
}

export function evaluateHighFeverAlert(
  snapshot: FeverDoseTimelineSnapshot,
  childProfile: ChildProfile | null | undefined,
): HighFeverClinicalAlert | null {
  const ageMonths =
    childProfile?.ageMonthsExact ?? initialAgeMonthsFromProfile(childProfile);
  const underThreeMonths =
    (ageMonths !== null && ageMonths !== undefined && ageMonths < 3) ||
    ((ageMonths === null || ageMonths === undefined) && childProfile?.ageBand === 'NEWBORN');

  const matching = snapshot.tempPoints.filter(
    (t) =>
      t.tempCelsius > HIGH_FEVER_ALERT_THRESHOLD_C ||
      (underThreeMonths && t.tempCelsius >= 38.0),
  );
  if (matching.length === 0) return null;
  const latestHigh = matching[matching.length - 1];
  const tempStr = formatTempC(latestHigh.tempCelsius);
  const childLabel = childProfile?.aliasFa?.trim() ? `برای ${childProfile.aliasFa.trim()} ` : '';
  const newbornUrgency = underThreeMonths && latestHigh.tempCelsius >= 38.0;

  return {
    triggerTempCelsius: latestHigh.tempCelsius,
    caregiverFa: latestHigh.caregiverFa,
    timeLabelFa: latestHigh.timeLabelFa,
    isHighFeverOver39: latestHigh.tempCelsius > HIGH_FEVER_ALERT_THRESHOLD_C,
    titleFa: newbornUrgency
      ? `هشدار فوری: تب ۳۸ درجه یا بیشتر در شیرخوار زیر ۳ ماه (${tempStr})`
      : `هشدار فوری تب بالای ۳۹ درجه (${tempStr}) — مراجعه به پزشک`,
    bodyFa: newbornUrgency
      ? `دمای ثبت‌شده ${childLabel}به ${tempStr} رسیده است. تب ۳۸ درجه یا بیشتر در شیرخوار زیر سه ماه نیازمند ارزیابی فوری حضوری توسط پزشک یا اورژانس است. داروی خودسرانه ندهید.`
      : `دمای ثبت‌شده ${childLabel}در ساعت ${latestHigh.timeLabelFa} (توسط ${latestHigh.caregiverFa}) به ${tempStr} رسیده و از آستانه ۳۹.۰°C فراتر رفته است. لطفاً وضعیت کودک را فوری بررسی کرده و برای ویزیت به پزشک یا مرکز درمانی مراجعه کنید.`,
    checklistFa: [
      'در صورت بی‌حالی شدید، خواب‌آلودگی غیرعادی، تنفس تند یا تشنج فوراً با اورژانس ۱۱۵ تماس بگیرید.',
      'در نوزاد زیر ۳ ماه حتی تب ۳۸.۰°C نیازمند ارجاع فوری و اورژانسی است.',
      'از پاشویه با آب یخ یا الکل و دادن خودسرانه آسپیرین اکیداً خودداری کنید.',
      'گزارش ۲۴ ساعته خط زمانی و نوبت داروهای داده‌شده را همراه داشته باشید.',
    ],
  };
}

export function buildDoctorTimelineReportFa(
  snapshot: FeverDoseTimelineSnapshot,
  childProfile: ChildProfile | null | undefined,
): string {
  const lines: string[] = ['گزارش ۲۴ ساعته تب و دارو — مانو کید'];
  if (childProfile) {
    const bandLabel = childProfile.ageBand;
    lines.push(
      `کودک: ${childProfile.aliasFa} (${bandLabel} • وزن: ${childProfile.weightKgText.trim() || 'ثبت‌نشده'} kg)`,
    );
  }
  if (snapshot.peakTemp24h !== null) {
    lines.push(`بالاترین دمای ۲۴ ساعت: ${formatTempC(snapshot.peakTemp24h)}`);
  }
  lines.push(`ثبت‌های دما (${snapshot.tempPoints.length}):`);
  if (snapshot.tempPoints.length === 0) {
    lines.push('• موردی ثبت نشده است');
  } else {
    [...snapshot.tempPoints]
      .reverse()
      .slice(0, 8)
      .forEach((t) => {
        lines.push(`• ${t.timeLabelFa}: ${formatTempC(t.tempCelsius)} (${t.siteFa}) — توسط ${t.caregiverFa}`);
      });
  }
  lines.push(`نوبت‌های دارو (${snapshot.dosePoints.length}):`);
  if (snapshot.dosePoints.length === 0) {
    lines.push('• موردی ثبت نشده است');
  } else {
    [...snapshot.dosePoints]
      .reverse()
      .slice(0, 8)
      .forEach((d) => {
        const notePart = d.noteFa.trim() ? ` (${d.noteFa})` : '';
        lines.push(`• ${d.givenAtLabelFa}: ${d.medicationNameFa} — ${d.volumeMlText}${notePart}`);
      });
  }
  return lines.join('\n');
}
