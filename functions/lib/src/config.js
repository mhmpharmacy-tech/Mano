"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AppError = exports.FIELD_LIMITS = exports.RATE_LIMITS = exports.VALID_CASE_STATUSES = exports.VALID_SYMPTOM_FLOWS = exports.VALID_CATEGORIES = exports.VALID_AGE_BANDS = exports.TRIAGE_PASS_TTL_MS = exports.CONSENT_VERSION = exports.FUNCTIONS_REGION = exports.FIRESTORE_DATABASE_ID = void 0;
exports.FIRESTORE_DATABASE_ID = process.env.MANO_FIRESTORE_DATABASE_ID ||
    "ai-studio-android-c61bb361-5d09-41d6-8602-26bbc07b52d7";
exports.FUNCTIONS_REGION = process.env.MANO_FUNCTIONS_REGION || "us-central1";
exports.CONSENT_VERSION = "v1";
exports.TRIAGE_PASS_TTL_MS = 15 * 60 * 1000; // 15 minutes
exports.VALID_AGE_BANDS = [
    "newborn",
    "infant_3_12m",
    "toddler_1_3y",
    "preschool_3_6y",
    "school_6_plus",
];
exports.VALID_CATEGORIES = [
    "SYMPTOMS",
    "OTC",
    "SKIN",
    "SCALP_HAIR",
    "SUPPLEMENT",
];
exports.VALID_SYMPTOM_FLOWS = [
    "fever",
    "cough",
    "runny_nose",
    "wet_cough",
    "eye",
    "ear",
    "diarrhea",
    "constipation",
    "rash",
    "otc_question",
    "supplement_question",
    "general",
];
exports.VALID_CASE_STATUSES = [
    "PENDING",
    "RECEIVED",
    "REVIEWED",
];
exports.RATE_LIMITS = {
    triagePass: { maxRequests: 12, windowMs: 10 * 60 * 1000 },
    caseSubmit: { maxRequests: 5, windowMs: 15 * 60 * 1000 },
    inboxRead: { maxRequests: 60, windowMs: 60 * 1000 },
    caseReview: { maxRequests: 40, windowMs: 5 * 60 * 1000 },
    adminAction: { maxRequests: 30, windowMs: 5 * 60 * 1000 },
};
exports.FIELD_LIMITS = {
    idRegex: /^[a-zA-Z0-9_-]{1,128}$/,
    questionMinLength: 8,
    questionMaxLength: 600,
    internalNoteMaxLength: 1000,
    parentReplyMinLength: 10,
    parentReplyMaxLength: 2000,
    credentialRefMinLength: 4,
    credentialRefMaxLength: 120,
};
class AppError extends Error {
    code;
    details;
    constructor(code, message, details) {
        super(message);
        this.name = "AppError";
        this.code = code;
        this.details = details;
    }
}
exports.AppError = AppError;
//# sourceMappingURL=config.js.map