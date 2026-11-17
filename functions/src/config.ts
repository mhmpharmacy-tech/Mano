export const FIRESTORE_DATABASE_ID =
  process.env.MANO_FIRESTORE_DATABASE_ID ||
  "ai-studio-android-c61bb361-5d09-41d6-8602-26bbc07b52d7";

export const FUNCTIONS_REGION = process.env.MANO_FUNCTIONS_REGION || "us-central1";

export const CONSENT_VERSION = "v1" as const;

export const TRIAGE_PASS_TTL_MS = 15 * 60 * 1000; // 15 minutes

export const VALID_AGE_BANDS = [
  "newborn",
  "infant_3_12m",
  "toddler_1_3y",
  "preschool_3_6y",
  "school_6_plus",
] as const;

export const VALID_CATEGORIES = [
  "SYMPTOMS",
  "OTC",
  "SKIN",
  "SCALP_HAIR",
  "SUPPLEMENT",
] as const;

export const VALID_SYMPTOM_FLOWS = [
  "fever",
  "cough",
  "diarrhea",
  "constipation",
  "rash",
  "otc_question",
  "supplement_question",
  "general",
] as const;

export const VALID_CASE_STATUSES = [
  "PENDING",
  "RECEIVED",
  "REVIEWED",
] as const;

export const RATE_LIMITS = {
  triagePass: { maxRequests: 12, windowMs: 10 * 60 * 1000 },
  caseSubmit: { maxRequests: 5, windowMs: 15 * 60 * 1000 },
  inboxRead: { maxRequests: 60, windowMs: 60 * 1000 },
  caseReview: { maxRequests: 40, windowMs: 5 * 60 * 1000 },
  adminAction: { maxRequests: 30, windowMs: 5 * 60 * 1000 },
} as const;

export const FIELD_LIMITS = {
  idRegex: /^[a-zA-Z0-9_-]{1,128}$/,
  questionMinLength: 8,
  questionMaxLength: 600,
  internalNoteMaxLength: 1000,
  parentReplyMinLength: 10,
  parentReplyMaxLength: 2000,
  credentialRefMinLength: 4,
  credentialRefMaxLength: 120,
} as const;

export type AppErrorCode =
  | "unauthenticated"
  | "permission-denied"
  | "invalid-argument"
  | "failed-precondition"
  | "not-found"
  | "already-exists"
  | "resource-exhausted";

export class AppError extends Error {
  readonly code: AppErrorCode;
  readonly details?: Record<string, unknown>;

  constructor(
    code: AppErrorCode,
    message: string,
    details?: Record<string, unknown>,
  ) {
    super(message);
    this.name = "AppError";
    this.code = code;
    this.details = details;
  }
}
