"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.assertAuthenticated = assertAuthenticated;
exports.assertValidId = assertValidId;
exports.assertPharmacistClaims = assertPharmacistClaims;
exports.assertVerifiedActiveStaffRecord = assertVerifiedActiveStaffRecord;
exports.assertActivePharmacy = assertActivePharmacy;
exports.verifyLivePharmacistAccess = verifyLivePharmacistAccess;
exports.verifyPharmacistAccessInTransaction = verifyPharmacistAccessInTransaction;
exports.assertSystemAdmin = assertSystemAdmin;
const config_1 = require("./config");
function assertAuthenticated(caller) {
    if (!caller || typeof caller.uid !== "string" || !caller.uid.trim()) {
        throw new config_1.AppError("unauthenticated", "Authentication is required for this endpoint.");
    }
    return caller;
}
function assertValidId(value, fieldName) {
    if (typeof value !== "string" || !config_1.FIELD_LIMITS.idRegex.test(value)) {
        throw new config_1.AppError("invalid-argument", `${fieldName} must be 1..128 characters matching ^[a-zA-Z0-9_-]+$.`);
    }
    return value;
}
/**
 * Verifies verified pharmacist custom claims AND rejects administrative
 * accounts attempting clinical inbox or case approval actions.
 */
function assertPharmacistClaims(caller, pharmacyId) {
    const authed = assertAuthenticated(caller);
    const token = authed.token || {};
    if (token.manoStaffRole === "admin") {
        throw new config_1.AppError("permission-denied", "Administrative accounts cannot access or approve clinical case decisions.");
    }
    if (token.manoStaffVerified !== true) {
        throw new config_1.AppError("permission-denied", "Verified staff claim manoStaffVerified=true is required.");
    }
    if (token.manoStaffRole !== "pharmacist") {
        throw new config_1.AppError("permission-denied", "Only verified pharmacist staff accounts are permitted.");
    }
    if (token.manoPharmacyId !== pharmacyId) {
        throw new config_1.AppError("permission-denied", "Cross-tenant access denied: staff pharmacy claim does not match target pharmacyId.");
    }
    return authed;
}
/**
 * Validates the live directory document at pharmacies/{pharmacyId}/staff/{uid}.
 * An old valid custom-claim token never overrides a revoked, suspended,
 * unverified, or missing directory record.
 */
function assertVerifiedActiveStaffRecord(staffRecord, uid, pharmacyId) {
    if (!staffRecord) {
        throw new config_1.AppError("permission-denied", "Staff directory record not found; access revoked or not onboarded.");
    }
    if (staffRecord.uid !== uid || staffRecord.pharmacyId !== pharmacyId) {
        throw new config_1.AppError("permission-denied", "Staff directory identity or pharmacy tenant mismatch.");
    }
    if (staffRecord.role !== "pharmacist") {
        throw new config_1.AppError("permission-denied", "Staff directory role must be pharmacist.");
    }
    if (staffRecord.status !== "ACTIVE") {
        throw new config_1.AppError("permission-denied", "Staff membership is not ACTIVE.");
    }
    if (staffRecord.credentialVerified !== true) {
        throw new config_1.AppError("permission-denied", "Staff pharmacist credential is not verified.");
    }
    if (staffRecord.revokedAt !== null) {
        throw new config_1.AppError("permission-denied", "Staff access has been revoked.");
    }
    return staffRecord;
}
function assertActivePharmacy(pharmacy, requireAcceptingCases = false) {
    if (!pharmacy || pharmacy.active !== true) {
        throw new config_1.AppError("failed-precondition", "Target pharmacy registry record is missing or inactive.");
    }
    if (requireAcceptingCases && pharmacy.acceptingCases !== true) {
        throw new config_1.AppError("failed-precondition", "Target pharmacy is not currently accepting consultation cases.");
    }
    return pharmacy;
}
async function verifyLivePharmacistAccess(store, caller, pharmacyId) {
    const authed = assertPharmacistClaims(caller, pharmacyId);
    const [pharmacy, staffRecord] = await Promise.all([
        store.get(`pharmacies/${pharmacyId}`),
        store.get(`pharmacies/${pharmacyId}/staff/${authed.uid}`),
    ]);
    const validPharmacy = assertActivePharmacy(pharmacy, false);
    const validStaff = assertVerifiedActiveStaffRecord(staffRecord, authed.uid, pharmacyId);
    return { caller: authed, pharmacy: validPharmacy, staff: validStaff };
}
async function verifyPharmacistAccessInTransaction(tx, caller, pharmacyId) {
    const authed = assertPharmacistClaims(caller, pharmacyId);
    const pharmacy = await tx.get(`pharmacies/${pharmacyId}`);
    const staffRecord = await tx.get(`pharmacies/${pharmacyId}/staff/${authed.uid}`);
    const validPharmacy = assertActivePharmacy(pharmacy, false);
    const validStaff = assertVerifiedActiveStaffRecord(staffRecord, authed.uid, pharmacyId);
    return { caller: authed, pharmacy: validPharmacy, staff: validStaff };
}
function assertSystemAdmin(caller) {
    const authed = assertAuthenticated(caller);
    if (authed.token?.manoSystemAdmin !== true) {
        throw new config_1.AppError("permission-denied", "Trusted administrative claim manoSystemAdmin=true is required.");
    }
    return authed;
}
//# sourceMappingURL=auth.js.map