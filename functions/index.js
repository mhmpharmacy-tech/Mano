"use strict";

const crypto = require("node:crypto");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { getFirestore } = require("firebase-admin/firestore");
const { isId, isVerifiedPharmacist, validateIntake, validateStaffReview, buildParentCaseView, isValidTriagePass, isActivePharmacistDirectoryRecord } = require("./consultation-policy");

if (!admin.apps.length) admin.initializeApp();
let db = null;
const serverTimestamp = admin.firestore.FieldValue.serverTimestamp;
const config = {
  region: "europe-west1",
  enforceAppCheck: true,
  maxInstances: 5,
  memory: "256MiB",
};

function requireEnabled() {
  // This gate is deliberately off by default even after Cloud Functions deployment.
  // Release must separately set this flag and complete the checklist in README.
  if (process.env.MANO_CLINICAL_SERVICE_ENABLED !== "true") {
    throw new HttpsError("failed-precondition", "Clinical consultation intake is not enabled.");
  }
  const databaseId = process.env.MANO_FIRESTORE_DATABASE_ID;
  if (!databaseId || !/^[A-Za-z0-9_-]{3,128}$/.test(databaseId)) {
    throw new HttpsError("failed-precondition", "Clinical Firestore database ID is not configured.");
  }
  if (!db) db = getFirestore(admin.app(), databaseId);
}

function requireUser(req) {
  if (!req.auth || typeof req.auth.uid !== "string" || !req.auth.uid) {
    throw new HttpsError("unauthenticated", "Sign-in required.");
  }
  return req.auth.uid;
}

function requireStaff(req, pharmacyId) {
  if (!isVerifiedPharmacist(req.auth, pharmacyId)) {
    throw new HttpsError("permission-denied", "Verified pharmacist role for this pharmacy is required.");
  }
  return req.auth.uid;
}

function staffRef(pharmacyId, uid) {
  return db.collection("pharmacies").doc(pharmacyId).collection("staff").doc(uid);
}

async function requireActiveStaffDirectory(req, pharmacyId) {
  const uid = requireStaff(req, pharmacyId);
  const staff = await staffRef(pharmacyId, uid).get();
  if (!staff.exists || !isActivePharmacistDirectoryRecord(staff.data(), uid, pharmacyId)) {
    throw new HttpsError("permission-denied", "Active, credential-verified pharmacist membership required.");
  }
  return uid;
}

async function requireActivePharmacy(pharmacyId, intake = false) {
  const snap = await db.collection("pharmacies").doc(pharmacyId).get();
  if (!snap.exists || snap.get("active") !== true) {
    throw new HttpsError("failed-precondition", "Pharmacy is unavailable.");
  }
  if (intake && snap.get("acceptingCases") !== true) {
    throw new HttpsError("failed-precondition", "Pharmacy is not accepting consultation requests.");
  }
}

function requireIds(data) {
  const pharmacyId = data && data.pharmacyId;
  const caseId = data && data.caseId;
  if (!isId(pharmacyId) || !isId(caseId)) {
    throw new HttpsError("invalid-argument", "Invalid pharmacy or case ID.");
  }
  return { pharmacyId, caseId };
}

function caseRef(pharmacyId, caseId) {
  return db.collection("pharmacies").doc(pharmacyId).collection("cases").doc(caseId);
}

// Authenticated parents can submit only nonurgent requests with explicit versioned consent.
// The staff inbox is private and separated by the pharmacy tenancy boundary.
exports.manoSubmitCase = onCall(config, async (req) => {
  requireEnabled();
  const uid = requireUser(req);
  const content = validateIntake(req.data);
  if (!content) throw new HttpsError("invalid-argument", "Case is invalid or requires immediate clinical escalation.");

  const pharmacyRef = db.collection("pharmacies").doc(content.pharmacyId);
  const triagePassRef = pharmacyRef.collection("triagePasses").doc(content.triagePassId);
  const ref = pharmacyRef.collection("cases").doc();
  const auditRef = ref.collection("audit").doc();

  // The pass must have been issued by a separate, trusted, clinically reviewed
  // triage service. No such issuer exists in this draft; intake fails closed.
  // Check and consume pass atomically, preventing replay and closing races.
  await db.runTransaction(async (tx) => {
    const [pharmacy, pass] = await Promise.all([
      tx.get(pharmacyRef),
      tx.get(triagePassRef),
    ]);
    if (!pharmacy.exists || pharmacy.get("active") !== true || pharmacy.get("acceptingCases") !== true) {
      throw new HttpsError("failed-precondition", "Pharmacy is not accepting consultation requests.");
    }
    if (!pass.exists || !isValidTriagePass(pass.data(), uid, content.pharmacyId)) {
      throw new HttpsError("failed-precondition", "A valid, unused, server-issued nonurgent triage pass is required.");
    }

    tx.create(ref, {
      schemaVersion: 1,
      ownerUid: uid,
      pharmacyId: content.pharmacyId,
      categoryName: content.categoryName,
      ageBandId: content.ageBandId,
      questionSummaryFa: content.questionSummaryFa,
      consentAccepted: true,
      privacyNoticeVersion: "v1",
      triagePassId: content.triagePassId,
      status: "PENDING",
      reviewedByUid: null,
      decision: null,
      reviewNoteFa: null,
      parentReplyFa: null,
      sourceFa: null,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    tx.update(triagePassRef, {
      consumedAt: serverTimestamp(),
      consumedByCaseId: ref.id,
    });
    tx.create(auditRef, {
      event: "CASE_CREATED",
      actorType: "parent",
      actorUid: uid,
      toStatus: "PENDING",
      triageIssuer: "mano-triage-v1",
      createdAt: serverTimestamp(),
    });
  });
  return { caseId: ref.id, status: "PENDING" };
});

// A parent can inspect only their own case and authorized pharmacist reply.
exports.manoGetMyCase = onCall(config, async (req) => {
  requireEnabled();
  const uid = requireUser(req);
  const { pharmacyId, caseId } = requireIds(req.data);
  const snap = await caseRef(pharmacyId, caseId).get();
  if (!snap.exists || snap.get("ownerUid") !== uid) throw new HttpsError("not-found", "Case not found.");
  const c = snap.data();
  return buildParentCaseView(caseId, c);
});

// Staff may see only cases under their staff-claim pharmacy. This is not a global search.
exports.manoListStaffCases = onCall(config, async (req) => {
  requireEnabled();
  requireUser(req);
  const pharmacyId = req.data && req.data.pharmacyId;
  if (!isId(pharmacyId)) throw new HttpsError("invalid-argument", "Invalid pharmacy ID.");
  await requireActiveStaffDirectory(req, pharmacyId);
  await requireActivePharmacy(pharmacyId);
  const snapshot = await db.collection("pharmacies").doc(pharmacyId)
    .collection("cases").orderBy("createdAt", "desc").limit(25).get();
  return { cases: snapshot.docs.map((doc) => {
    const c = doc.data();
    return {
      caseId: doc.id,
      categoryName: c.categoryName,
      ageBandId: c.ageBandId,
      questionSummaryFa: c.questionSummaryFa,
      status: c.status,
      reviewedByUid: c.reviewedByUid || null,
      updatedAtMs: c.updatedAt && typeof c.updatedAt.toMillis === "function" ? c.updatedAt.toMillis() : null,
    };
  }) };
});

// Only an actively verified pharmacist of this exact pharmacy may transition a case.
// Clinical approval is auditable and cannot be forged with direct Firestore client writes.
exports.manoReviewCase = onCall(config, async (req) => {
  requireEnabled();
  const { pharmacyId, caseId } = requireIds(req.data);
  const reviewerUid = requireStaff(req, pharmacyId);
  await requireActivePharmacy(pharmacyId);
  const ref = caseRef(pharmacyId, caseId);
  const staff = staffRef(pharmacyId, reviewerUid);
  const auditRef = ref.collection("audit").doc();

  const result = await db.runTransaction(async (tx) => {
    // Read staff membership inside the transaction to block revoked pharmacists
    // before their approval is committed, even if a stale ID token still has claims.
    const [snap, staffDoc] = await Promise.all([tx.get(ref), tx.get(staff)]);
    if (!staffDoc.exists || !isActivePharmacistDirectoryRecord(staffDoc.data(), reviewerUid, pharmacyId)) {
      throw new HttpsError("permission-denied", "Pharmacist membership is inactive or no longer verified.");
    }
    if (!snap.exists) throw new HttpsError("not-found", "Case not found.");
    const previousStatus = snap.get("status");
    const change = validateStaffReview(req.data, previousStatus);
    if (!change) throw new HttpsError("failed-precondition", "Invalid clinical review transition.");

    const changes = { status: change.status, reviewedByUid: reviewerUid, updatedAt: serverTimestamp() };
    if (change.status === "REVIEWED") {
      changes.decision = change.decision;
      changes.reviewNoteFa = change.reviewNoteFa;
      changes.parentReplyFa = change.parentReplyFa;
      changes.sourceFa = change.sourceFa;
    }
    tx.update(ref, changes);
    tx.create(auditRef, {
      event: change.status === "REVIEWED" ? "CLINICAL_REVIEW_COMPLETED" : "CASE_RECEIVED",
      actorType: "verified_staff",
      actorUid: reviewerUid,
      fromStatus: previousStatus,
      toStatus: change.status,
      decision: change.decision,
      // Hash rather than duplicate the clinical note in the immutable audit log.
      noteSha256: change.reviewNoteFa ? crypto.createHash("sha256").update(change.reviewNoteFa, "utf8").digest("hex") : null,
      replySha256: change.parentReplyFa ? crypto.createHash("sha256").update(change.parentReplyFa, "utf8").digest("hex") : null,
      createdAt: serverTimestamp(),
    });
    return { caseId, status: change.status };
  });
  return result;
});