import { parseClinicalDecimal } from './clinicalNumberInput.ts';

export const TIMELINE_TEMPERATURE_SITES = [
  'زیر بغل (دیجیتال)', 'پیشانی (غیرتماسی)', 'گوشی (تیمپانیک)', 'مقعدی (نوزاد)', 'روش اندازه‌گیری نامشخص',
] as const;
export const TIMELINE_DOSE_UNITS = ['mL', 'قطره', 'mg', 'عدد'] as const;
export type TimelineDoseUnit = typeof TIMELINE_DOSE_UNITS[number];

// datetime-local is interpreted in the caregiver's local timezone. Reject date
// rollover and future events rather than silently replacing the actual time.
export function parseTimelineEventTime(raw: string, nowMs = Date.now()): number | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(raw);
  if (!match || !Number.isFinite(nowMs)) return null;
  const [year, month, day, hour, minute] = match.slice(1).map(Number);
  const date = new Date(year, month - 1, day, hour, minute, 0, 0);
  const ms = date.getTime();
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day &&
    date.getHours() === hour && date.getMinutes() === minute && Number.isFinite(ms) && ms > 0 && ms <= nowMs
    ? ms : null;
}

export function formatTimelineDateTimeInput(ms: number): string {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function validateTimelineTemperature(raw: string, site: string, dateTime: string, nowMs = Date.now()) {
  const temperatureCelsius = parseClinicalDecimal(raw);
  const measuredAtMs = parseTimelineEventTime(dateTime, nowMs);
  if (temperatureCelsius === null || temperatureCelsius < 34 || temperatureCelsius > 43 ||
      !TIMELINE_TEMPERATURE_SITES.some((item) => item === site) || measuredAtMs === null) return null;
  return { temperatureCelsius, measuredAtMs };
}

export function validateTimelineDose(input: {
  medication: string;
  formulation: string;
  amount: string;
  unit: string;
  dateTime: string;
}, nowMs = Date.now()) {
  const amount = parseClinicalDecimal(input.amount);
  const administeredAtMs = parseTimelineEventTime(input.dateTime, nowMs);
  if ((input.medication !== 'acetaminophen' && input.medication !== 'ibuprofen') ||
      !input.formulation.trim() || amount === null || amount <= 0 ||
      !TIMELINE_DOSE_UNITS.some((unit) => unit === input.unit) || administeredAtMs === null ||
      (input.unit === 'قطره' && !Number.isInteger(amount))) return null;
  return { amount, administeredAtMs, amountText: `${amount} ${input.unit}` };
}
