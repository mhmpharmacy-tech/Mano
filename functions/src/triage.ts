import { AppError, VALID_AGE_BANDS, VALID_SYMPTOM_FLOWS } from "./config";
import { AgeBandId, SymptomFlowId } from "./types";

const DIACRITICS_REGEX = /[\u064B-\u065F\u0670\u0640]/g;
const ZWNJ_REGEX = /[\u200C\u200D\u200E\u200F]/g;
const MULTI_SPACE_REGEX = /\s+/g;

export function normalizePersian(input: string): string {
  if (!input || !input.trim()) return "";
  let out = "";
  for (const ch of input) {
    const code = ch.charCodeAt(0);
    if (ch === "ي" || ch === "ى" || ch === "ئ") {
      out += "ی";
    } else if (ch === "ك") {
      out += "ک";
    } else if (ch === "ة") {
      out += "ه";
    } else if (ch === "أ" || ch === "إ" || ch === "آ") {
      out += "ا";
    } else if (ch === "ؤ") {
      out += "و";
    } else if (code >= 0x06f0 && code <= 0x06f9) {
      out += String.fromCharCode(48 + (code - 0x06f0));
    } else if (code >= 0x0660 && code <= 0x0669) {
      out += String.fromCharCode(48 + (code - 0x0660));
    } else {
      out += ch.toLowerCase();
    }
  }
  const noDiacritics = out.replace(DIACRITICS_REGEX, "");
  const zwnjAsSpace = noDiacritics.replace(ZWNJ_REGEX, " ");
  return zwnjAsSpace.replace(MULTI_SPACE_REGEX, " ").trim();
}

export function compactPersian(input: string): string {
  return normalizePersian(input).replace(/ /g, "");
}

const CRITICAL_RED_FLAG_IDS = new Set<string>([
  "breathing_difficulty",
  "cyanosis",
  "seizure",
  "unresponsive",
  "lethargy_severe",
  "severe_dehydration",
  "petechiae_rash",
  "non_blanching_rash",
  "neck_stiffness",
  "persistent_vomiting",
  "blood_in_stool",
  "poisoning",
  "anaphylaxis",
  // IDs emitted by the current Android symptom wizard.
  "breathing", "lips", "lethargy", "dehydration", "rash_nonblanch",
  "bilious_vomit", "blood_stool", "severe_distension", "breathing_effort",
  "stridor_rest", "choking_history", "fever_blisters", "severe_dehyd",
  "green_vomit", "unable_fluids",
]);

const REVIEW_RED_FLAG_IDS = new Set([
  "ear_pain_or_crying", "infant_no_meconium", "painful_fissure", "prolonged_2w",
  "spreading_severe", "no_improve_3d", "bloody_diarrhea",
]);

const HIGH_RISK_COMPACT_KEYWORDS = [
  "تشنج",
  "کبودی",
  "تنگینفس",
  "سختنفس",
  "خسخسشدید",
  "بیهوش",
  "کما",
  "کمابی",
  "پوشکخشک",
  "مسمومیت",
  "خونریزی",
  "سفتیگردن",
  "لکهبنفش",
  "پتشی",
  "بیحالیشدید",
  "تبنوزاد",
];

export interface TriageEvaluationInput {
  ageBandId: string;
  symptomFlowId?: string;
  temperatureCelsius?: number | null;
  durationHours?: number | null;
  selectedRedFlagIds?: string[];
  questionSummaryFa?: string;
  hasUrgentFlag?: boolean;
}

export interface TriageEvaluationDecision {
  outcome: "URGENT_EMERGENCY" | "NEEDS_TIMELY_REVIEW" | "NONURGENT";
  ageBandId: AgeBandId;
  symptomFlowId: SymptomFlowId;
  ruleId: string;
  titleFa: string;
  summaryFa: string;
  emergencyActionsFa: string[];
}

export function evaluateServerTriage(
  rawInput: TriageEvaluationInput,
): TriageEvaluationDecision {
  if (
    !VALID_AGE_BANDS.includes(rawInput.ageBandId as AgeBandId)
  ) {
    throw new AppError("invalid-argument", "Invalid or unsupported pediatric ageBandId.");
  }
  const ageBandId = rawInput.ageBandId as AgeBandId;

  const flowCandidate = rawInput.symptomFlowId ?? "general";
  if (!VALID_SYMPTOM_FLOWS.includes(flowCandidate as SymptomFlowId)) {
    throw new AppError("invalid-argument", "Invalid symptomFlowId.");
  }
  const symptomFlowId = flowCandidate as SymptomFlowId;

  if (
    rawInput.temperatureCelsius !== undefined &&
    rawInput.temperatureCelsius !== null &&
    (typeof rawInput.temperatureCelsius !== "number" ||
      !Number.isFinite(rawInput.temperatureCelsius) ||
      rawInput.temperatureCelsius < 34.0 ||
      rawInput.temperatureCelsius > 43.0)
  ) {
    throw new AppError(
      "invalid-argument",
      "temperatureCelsius must be a physiological reading between 34.0 and 43.0 °C.",
    );
  }

  if (rawInput.selectedRedFlagIds !== undefined &&
      (!Array.isArray(rawInput.selectedRedFlagIds) || rawInput.selectedRedFlagIds.some(
        (id) => typeof id !== "string" || (!CRITICAL_RED_FLAG_IDS.has(id) && !REVIEW_RED_FLAG_IDS.has(id)),
      ))) {
    throw new AppError("invalid-argument", "Unknown or malformed red-flag IDs; repeat triage with the current form.");
  }
  if (rawInput.hasUrgentFlag !== undefined && typeof rawInput.hasUrgentFlag !== "boolean") {
    throw new AppError("invalid-argument", "hasUrgentFlag must be boolean.");
  }
  if (rawInput.questionSummaryFa !== undefined && typeof rawInput.questionSummaryFa !== "string") {
    throw new AppError("invalid-argument", "questionSummaryFa must be text.");
  }
  const redFlags = rawInput.selectedRedFlagIds ?? [];

  if (rawInput.hasUrgentFlag === true) {
    return {
      outcome: "URGENT_EMERGENCY",
      ageBandId,
      symptomFlowId,
      ruleId: "rule_client_flagged_urgent",
      titleFa: "نیاز به ارزیابی فوری اورژانس",
      summaryFa:
        "به دلیل گزارش نشانه فوری، ثبت مشاوره غیرفوری مجاز نیست و لازم است کودک بلافاصله توسط پزشک یا اورژانس ۱۱۵ ارزیابی شود.",
      emergencyActionsFa: [
        "با اورژانس ۱۱۵ تماس بگیرید یا کودک را به نزدیک‌ترین مرکز اورژانس برسانید.",
        "بدون معاینه پزشک داروی تب‌بر یا آرام‌بخش جدید ندهید.",
      ],
    };
  }

  // Rule 1: Critical red flag IDs
  const matchedCriticalIds = redFlags.filter((id) =>
    CRITICAL_RED_FLAG_IDS.has(id),
  );
  if (matchedCriticalIds.length > 0) {
    return {
      outcome: "URGENT_EMERGENCY",
      ageBandId,
      symptomFlowId,
      ruleId: "rule_critical_red_flag_bypass",
      titleFa: "وجود نشانه هشدار بحرانی",
      summaryFa:
        "بر اساس نشانه‌های هشدار انتخاب‌شده، امکان ارسال پرسش غیرفوری وجود ندارد و ارزیابی فوری پزشکی ضروری است.",
      emergencyActionsFa: [
        "بلافاصله به اورژانس مراجعه کنید یا با ۱۱۵ تماس بگیرید.",
        "تا زمان ویزیت، به کودک خواب‌آلود مایعات یا داروی خودسرانه ندهید.",
      ],
    };
  }

  // Rule 2: Newborn (< 3 months) fever >= 38.0 °C (or fever flow without confirmed normal temp)
  const tempC =
    rawInput.temperatureCelsius === undefined
      ? null
      : rawInput.temperatureCelsius;
  const compactQuestion = compactPersian(rawInput.questionSummaryFa ?? "");

  const newbornHasFeverRisk =
    ageBandId === "newborn" &&
    ((tempC !== null && tempC >= 38.0) ||
      (symptomFlowId === "fever" && (tempC === null || tempC >= 38.0)) ||
      (tempC === null && compactQuestion.includes("تب")));

  if (newbornHasFeverRisk) {
    return {
      outcome: "URGENT_EMERGENCY",
      ageBandId,
      symptomFlowId,
      ruleId: "rule_infant_under_3m_fever_38",
      titleFa: "ارزیابی فوری پزشکی برای تب نوزاد زیر ۳ ماه",
      summaryFa:
        "در نوزادان کوچک‌تر از ۳ ماه، تب ۳۸ درجه یا احتمال تب یک وضعیت اورژانسی است و در مسیر مشاوره غیرفوری ثبت نمی‌شود.",
      emergencyActionsFa: [
        "نوزاد را بدون تأخیر به اورژانس اطفال یا پزشک برسانید.",
        "پیش از معاینه پزشک داروی تب‌بر به نوزاد ندهید.",
      ],
    };
  }

  // Rule 3: Extreme hyperpyrexia (>= 40.5°C) or hypothermia (< 35.0°C) at any pediatric age
  if (tempC !== null && (tempC >= 40.5 || tempC < 35.0)) {
    return {
      outcome: "URGENT_EMERGENCY",
      ageBandId,
      symptomFlowId,
      ruleId: "rule_extreme_temperature_emergency",
      titleFa: "نیاز به ارزیابی فوری دمای بحرانی",
      summaryFa:
        "دمای ثبت‌شده در محدوده بحرانی است و نیازمند ارزیابی فوری حضوری توسط پزشک یا اورژانس است.",
      emergencyActionsFa: [
        "بلافاصله کودک را به مرکز درمانی برسانید یا با اورژانس ۱۱۵ تماس بگیرید.",
      ],
    };
  }

  // Rule 4: High-risk keywords detected inside free-text question
  if (
    compactQuestion.length >= 2 &&
    HIGH_RISK_COMPACT_KEYWORDS.some((kw) => compactQuestion.includes(kw))
  ) {
    return {
      outcome: "URGENT_EMERGENCY",
      ageBandId,
      symptomFlowId,
      ruleId: "rule_high_risk_text_signal",
      titleFa: "شناسایی نشانه پرخطر در شرح سؤال",
      summaryFa:
        "در متن سؤال نشانه‌ای ذکر شده که نیازمند معاینه مستقیم پزشک یا ارزیابی فوری است و برای مشاوره غیرفوری داروساز مجاز نیست.",
      emergencyActionsFa: [
        "در صورت تنگی نفس، تشنج، کبودی، خونریزی یا بی‌حالی شدید با ۱۱۵ تماس بگیرید.",
        "برای علائم فوری به جای ثبت پیام آنلاین، به پزشک یا اورژانس مراجعه کنید.",
      ],
    };
  }

  // Duration never changes a confirmed emergency into an input-validation error.
  const duration = rawInput.durationHours;
  if (duration !== undefined && duration !== null &&
      (!Number.isInteger(duration) || duration < 0 || duration > 8760)) {
    throw new AppError("invalid-argument", "durationHours must be a non-negative whole number up to 8760.");
  }
  const infantHighFever = ageBandId === "infant_3_12m" && tempC !== null && tempC >= 39;
  const prolonged = (symptomFlowId === "fever" && (duration ?? 0) >= 72) ||
    (symptomFlowId === "cough" && (duration ?? 0) >= 336);
  if (redFlags.some((id) => REVIEW_RED_FLAG_IDS.has(id)) || infantHighFever || prolonged) {
    return {
      outcome: "NEEDS_TIMELY_REVIEW",
      ageBandId, symptomFlowId,
      ruleId: infantHighFever ? "rule_infant_high_fever_unknown_exact_age" : "rule_timely_clinical_review",
      titleFa: "نیاز به ارزیابی پزشک؛ پرسش غیرفوری کافی نیست",
      summaryFa: infantHighFever
        ? "در شیرخوار ۳ تا ۶ ماهه دمای ۳۹ درجه نیازمند ارزیابی فوری پزشک است؛ چون سن دقیق این فرم مشخص نیست، مسیر احتیاطی انتخاب شد."
        : "علامت هشدار یا طول مدت علائم نیازمند ارزیابی به‌موقع پزشک است. گذرنامه مشاوره غیرفوری صادر نمی‌شود.",
      emergencyActionsFa: ["برای ارزیابی با پزشک تماس بگیرید؛ در صورت تنگی نفس، کبودی، تشنج یا کاهش هوشیاری با ۱۱۵ تماس بگیرید."],
    };
  }

  return {
    outcome: "NONURGENT",
    ageBandId,
    symptomFlowId,
    ruleId: "rule_nonurgent_pass_eligible",
    titleFa: "مجاز برای ثبت پرسش غیرفوری داروساز",
    summaryFa:
      "علامت اورژانسی فوری شناسایی نشد؛ گذرنامه یک‌بارمصرف NONURGENT برای ثبت پرسش صادر شد.",
    emergencyActionsFa: [],
  };
}
