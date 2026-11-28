import { OnlineBoundaryError } from './contracts.ts';

export const cardFields = {
  child: ['نام یا لقب کودک', 80], medicine: ['نام دارو', 120],
  strength: ['قدرت یا غلظت', 100], instructions: ['دستور مصرف ثبت‌شده', 1500],
  followUp: ['برنامهٔ پیگیری', 120],
} as const;

const optionalFields = {
  dosageForm: ['شکل دارویی', 80], route: ['راه مصرف', 80], doseAmount: ['مقدار مصرف', 120],
  frequency: ['فاصلهٔ مصرف', 120], duration: ['مدت مصرف', 120], precautions: ['احتیاط‌ها', 1000],
  pharmacyNameFa: ['داروخانه', 140],
} as const;

const eventLabels = {
  send: 'صدور کارت', revise: 'بازبینی کارت', open: 'مشاهدهٔ خانواده', dose: 'گزارش مصرف خانواده',
  question: 'پرسش خانواده', reply: 'پاسخ داروساز', checkin: 'پیگیری خانواده', close: 'پایان پیگیری',
} as const;

export interface CaseCareCard {
  fields: { label: string; value: string }[];
  events: { label: string; text: string; at: number }[];
  closed: boolean;
}

function invalid(): never { throw new OnlineBoundaryError('INVALID_RESPONSE'); }
function object(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return invalid();
  return value as Record<string, unknown>;
}
function validTime(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0 && value <= 8_640_000_000_000_000;
}

export function parseCaseCareCard(input: unknown): CaseCareCard {
  const card = object(input);
  const fields = [];
  for (const [key, [label, max]] of Object.entries({ ...cardFields, ...optionalFields })) {
    const value = card[key];
    if (value === undefined && !Object.hasOwn(cardFields, key)) continue;
    if (typeof value !== 'string' || !value.trim() || value.length > max) return invalid();
    fields.push({ label, value });
  }
  if (card.closedAt !== null && !validTime(card.closedAt)) return invalid();
  if (!Array.isArray(card.events) || card.events.length > 202) return invalid();
  const events = card.events.map((raw: unknown) => {
    const event = object(raw);
    if (typeof event.action !== 'string' || !Object.hasOwn(eventLabels, event.action) ||
        typeof event.text !== 'string' || event.text.length > 1000 || !validTime(event.at)) return invalid();
    return { label: eventLabels[event.action as keyof typeof eventLabels], text: event.text, at: event.at };
  });
  return { fields, events, closed: card.closedAt !== null };
}