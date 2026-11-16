"use strict";

/* Pure validation; callable functions must enforce these rules on the server.
   No clinical advice, diagnosis, or numeric drug dose is generated here. */

const AGE_BANDS = new Set(["newborn", "infant_3_12m", "toddler_1_3y", "preschool_3_6y", "school_6_plus"]);
const CATEGORIES = new Set(["SYMPTOMS", "OTC", "SKIN", "HAIR", "SUPPLEMENT", "NUTRITION", "OTHER"]);
const DECISIONS = new Set(["EDUCATION", "REFER", "NEEDS_MORE_INFO"]);
const NEXT_STATUSES = Object.freeze({ PENDING: "RECEIVED", RECEIVED: "REVIEWED" });
const ID_PATTERN = /^[A-Za-z0-9_-]{3,96}$/;

function isId(value) {
  return typeof value === "string" && ID_PATTERN.test(value);
}

function isVerifiedPharmacist(auth, pharmacyId) {
  const claims = auth && auth.token;
  return Boolean(
    auth &&
      claims &&
      claims.manoStaffVerified === true &&
      claims.manoStaffRole === "pharmacist" &&
      isId(claims.manoPharmacyId) &&
      claims.manoPharmacyId === pharmacyId
  );
}

function validateIntake(payload) {
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) return null;
  const pharmacyId = payload.pharmacyId;
  const categoryName = payload.categoryName;
  const ageBandId = payload.ageBandId;
  const questionSummaryFa = typeof payload.questionSummaryFa === "string" ? payload.questionSummaryFa.trim() : "";
  if (!isId(pharmacyId)) return null;
  if (!AGE_BANDS.has(ageBandId) || !CATEGORIES.has(categoryName)) return null;
  if (questionSummaryFa.length < 12 || questionSummaryFa.length > 600) return null;
  if (payload.consentAccepted !== true || payload.privacyNoticeVersion !== "v1") return null;
  if (payload.hasUrgentFlag !== false) return null; // emergency / referral must not enter a delayed asynchronous queue
  return { pharmacyId, categoryName, ageBandId, questionSummaryFa };
}

function validateStaffReview(payload, oldStatus) {
  if (!payload || typeof payload !== "object" || !Object.prototype.hasOwnProperty.call(NEXT_STATUSES, oldStatus)) return null;
  const status = payload.status;
  if (status !== NEXT_STATUSES[oldStatus]) return null;
  if (oldStatus === "PENDING") return { status: "RECEIVED", decision: null, reviewNoteFa: null, parentReplyFa: null, sourceFa: null };
  const decision = payload.decision;
  const note = typeof payload.reviewNoteFa === "string" ? payload.reviewNoteFa.trim() : "";
  const source = typeof payload.sourceFa === "string" ? payload.sourceFa.trim() : "";
  const reply = typeof payload.parentReplyFa === "string" ? payload.parentReplyFa.trim() : "";
  if (!DECISIONS.has(decision) || note.length < 20 || note.length > 1800) return null;
  if (source.length < 5 || source.length > 240) return null;
  // Private staff note and explicit parent-facing message are different artifacts.
  if (reply.length < 20 || reply.length > 1200 || payload.parentReplyApproved !== true) return null;
  return { status: "REVIEWED", decision, reviewNoteFa: note, parentReplyFa: reply, sourceFa: source };
}

/**
 * Explicit response DTO: never serialize internal staff notes or audit metadata
 * into a parent-facing endpoint by spreading a Firestore document.
 */
function buildParentCaseView(caseId, source) {
  const reviewed = source.status === "REVIEWED";
  return {
    caseId,
    categoryName: source.categoryName,
    ageBandId: source.ageBandId,
    status: source.status,
    decision: reviewed ? (source.decision || null) : null,
    parentReplyFa: reviewed ? (source.parentReplyFa || null) : null,
    sourceFa: reviewed ? (source.sourceFa || null) : null,
    updatedAtMs: source.updatedAt && typeof source.updatedAt.toMillis === "function" ? source.updatedAt.toMillis() : null,
  };
}

module.exports = { AGE_BANDS, CATEGORIES, DECISIONS, NEXT_STATUSES, isId, isVerifiedPharmacist, validateIntake, validateStaffReview, buildParentCaseView };