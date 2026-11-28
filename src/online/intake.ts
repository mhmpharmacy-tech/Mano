import { SYMPTOM_FLOWS } from '../data/symptomFlows.ts';
import { parseClinicalDecimal } from '../domain/clinicalNumberInput.ts';
import type { RedFlagItem } from '../domain/models.ts';
import type { TriageInput } from './contracts.ts';

export const ONLINE_FLOWS = [
  ...SYMPTOM_FLOWS.map(flow => ({ id: flow.id, titleFa: flow.titleFa, backendId: flow.id === 'diaper_rash' ? 'rash' : flow.id === 'diarrhea_vomiting' ? 'diarrhea' : flow.id, redFlags: flow.redFlags, asksTemperature: flow.asksTemperature === true, asksDuration: true })),
  { id: 'otc_question', backendId: 'otc_question', titleFa: 'پرسش دربارهٔ دارو', redFlags: SYMPTOM_FLOWS[0].redFlags, asksTemperature: false, asksDuration: false },
  { id: 'supplement_question', backendId: 'supplement_question', titleFa: 'پرسش دربارهٔ مکمل', redFlags: SYMPTOM_FLOWS[0].redFlags, asksTemperature: false, asksDuration: false },
];
export interface OnlineIntakeDraft {
  ageMonths: string;
  flowId: string;
  temperature: string;
  duration: string;
  question: string;
  selectedFlags: string[];
  flagsReviewed: boolean;
}
export class OnlineIntakeError extends Error {}
export function selectedOnlineFlow(id: string) { return ONLINE_FLOWS.find(flow => flow.id === id); }
export function onlineRedFlags(id: string): readonly RedFlagItem[] { return selectedOnlineFlow(id)?.redFlags ?? []; }

/** Entered information only. Eligibility is decided by the server, never here. */
export function buildOnlineTriageInput(draft: OnlineIntakeDraft): TriageInput {
  const flow = selectedOnlineFlow(draft.flowId);
  if (!flow) throw new OnlineIntakeError('موضوع پرسش را انتخاب کنید.');
  const months = parseClinicalDecimal(draft.ageMonths);
  if (months === null || months < 0 || months > 144) throw new OnlineIntakeError('سن واقعی کودک را از ۰ تا ۱۴۴ ماه وارد کنید؛ سن از پروندهٔ محلی حدس زده نمی‌شود.');
  const temperature = draft.temperature.trim() ? parseClinicalDecimal(draft.temperature) : null;
  if ((flow.asksTemperature && temperature === null) || (draft.temperature.trim() && (temperature === null || temperature < 34 || temperature > 43))) throw new OnlineIntakeError('دمای اندازه‌گیری‌شده را از ۳۴ تا ۴۳ درجه وارد کنید.');
  const duration = draft.duration.trim() ? parseClinicalDecimal(draft.duration) : null;
  if ((flow.asksDuration && duration === null) || (draft.duration.trim() && (duration === null || !Number.isInteger(duration) || duration < 0 || duration > 8760))) throw new OnlineIntakeError('مدت واقعی علائم را به ساعت کامل، از ۰ تا ۸۷۶۰ وارد کنید.');
  if (!draft.flagsReviewed) throw new OnlineIntakeError('نشانه‌های هشدار را مرور و تأیید کنید.');
  if (!Array.isArray(draft.selectedFlags) || draft.selectedFlags.length > flow.redFlags.length || new Set(draft.selectedFlags).size !== draft.selectedFlags.length || draft.selectedFlags.some(id => !flow.redFlags.some(flag => flag.id === id))) throw new OnlineIntakeError('انتخاب نشانه‌ها معتبر نیست؛ دوباره مرور کنید.');
  const questionSummaryFa = draft.question.trim();
  if (questionSummaryFa.length < 8 || questionSummaryFa.length > 600) throw new OnlineIntakeError('شرح پرسش باید بین ۸ تا ۶۰۰ نویسه باشد. نام، کد ملی و نشانی کودک را وارد نکنید.');
  const ageBandId = months < 3 ? 'newborn' : months < 12 ? 'infant_3_12m' : months < 36 ? 'toddler_1_3y' : months < 72 ? 'preschool_3_6y' : 'school_6_plus';
  return {
    ageBandId,
    // The existing server contract expects completed months only for infants.
    ...(ageBandId === 'infant_3_12m' ? { ageMonthsExact: Math.floor(months) } : {}),
    symptomFlowId: flow.backendId,
    temperatureCelsius: temperature,
    durationHours: duration,
    selectedRedFlagIds: [...draft.selectedFlags],
    questionSummaryFa,
    hasUrgentFlag: draft.selectedFlags.some(id => flow.redFlags.some(flag => flag.id === id && flag.isImmediateEmergency)),
  };
}
