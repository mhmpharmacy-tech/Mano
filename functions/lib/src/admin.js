"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PharmacyAdminService = void 0;
const auth_1 = require("./auth");
const config_1 = require("./config");
const rateLimit_1 = require("./rateLimit");
class PharmacyAdminService {
    store;
    authService;
    constructor(store, authService) {
        this.store = store;
        this.authService = authService;
    }
    async upsertPharmacyRegistry(caller, req) {
        const admin = (0, auth_1.assertSystemAdmin)(caller);
        const pharmacyId = (0, auth_1.assertValidId)(req.pharmacyId, "pharmacyId");
        if (typeof req.displayNameFa !== "string" ||
            req.displayNameFa.trim().length < 2 ||
            req.displayNameFa.trim().length > 120) {
            throw new config_1.AppError("invalid-argument", "displayNameFa must be between 2 and 120 characters.");
        }
        await (0, rateLimit_1.consumeRateLimit)(this.store, admin.uid, "admin_action", config_1.RATE_LIMITS.adminAction.maxRequests, config_1.RATE_LIMITS.adminAction.windowMs);
        const pharmacyPath = `pharmacies/${pharmacyId}`;
        return this.store.runTransaction(async (tx) => {
            const now = this.store.nowMs();
            const existing = await tx.get(pharmacyPath);
            const doc = {
                pharmacyId,
                displayNameFa: req.displayNameFa.trim(),
                active: req.active === true,
                acceptingCases: req.active === true && req.acceptingCases === true,
                createdAt: existing?.createdAt ?? now,
                updatedAt: now,
            };
            tx.set(pharmacyPath, doc);
            return doc;
        });
    }
    async onboardPharmacist(caller, req) {
        const admin = (0, auth_1.assertSystemAdmin)(caller);
        const pharmacyId = (0, auth_1.assertValidId)(req.pharmacyId, "pharmacyId");
        const targetUid = (0, auth_1.assertValidId)(req.targetUid, "targetUid");
        if (req.credentialVerified !== true) {
            throw new config_1.AppError("failed-precondition", "Pharmacist onboarding requires verified credentials (credentialVerified=true).");
        }
        const credRef = typeof req.credentialReference === "string"
            ? req.credentialReference.trim()
            : "";
        if (credRef.length < config_1.FIELD_LIMITS.credentialRefMinLength ||
            credRef.length > config_1.FIELD_LIMITS.credentialRefMaxLength) {
            throw new config_1.AppError("invalid-argument", "credentialReference must be 4..120 characters.");
        }
        await (0, rateLimit_1.consumeRateLimit)(this.store, admin.uid, "admin_action", config_1.RATE_LIMITS.adminAction.maxRequests, config_1.RATE_LIMITS.adminAction.windowMs);
        const staffPath = `pharmacies/${pharmacyId}/staff/${targetUid}`;
        const staffDoc = await this.store.runTransaction(async (tx) => {
            await (0, auth_1.assertAccountActiveInTransaction)(tx, targetUid);
            const pharmacy = await tx.get(`pharmacies/${pharmacyId}`);
            if (!pharmacy || pharmacy.active !== true) {
                throw new config_1.AppError("failed-precondition", "Cannot onboard staff into a missing or inactive pharmacy.");
            }
            const existing = await tx.get(staffPath);
            const now = this.store.nowMs();
            const doc = {
                uid: targetUid,
                pharmacyId,
                role: "pharmacist",
                status: "ACTIVE",
                credentialVerified: true,
                credentialReference: credRef,
                revokedAt: null,
                revocationReason: null,
                revokedByUid: null,
                onboardedByUid: admin.uid,
                createdAt: existing?.createdAt ?? now,
                updatedAt: now,
            };
            tx.set(staffPath, doc);
            return doc;
        });
        const existingClaims = await this.authService.getCustomClaims(targetUid);
        await this.authService.setCustomClaims(targetUid, {
            ...existingClaims,
            manoStaffVerified: true,
            manoStaffRole: "pharmacist",
            manoPharmacyId: pharmacyId,
        });
        return staffDoc;
    }
    async revokePharmacist(caller, req) {
        const admin = (0, auth_1.assertSystemAdmin)(caller);
        const pharmacyId = (0, auth_1.assertValidId)(req.pharmacyId, "pharmacyId");
        const targetUid = (0, auth_1.assertValidId)(req.targetUid, "targetUid");
        const nextStatus = req.status === "SUSPENDED" ? "SUSPENDED" : "REVOKED";
        const reason = typeof req.reason === "string" ? req.reason.trim() : "";
        if (reason.length < 3 || reason.length > 300) {
            throw new config_1.AppError("invalid-argument", "Revocation reason must be 3..300 characters.");
        }
        await (0, rateLimit_1.consumeRateLimit)(this.store, admin.uid, "admin_action", config_1.RATE_LIMITS.adminAction.maxRequests, config_1.RATE_LIMITS.adminAction.windowMs);
        const staffPath = `pharmacies/${pharmacyId}/staff/${targetUid}`;
        const updatedDoc = await this.store.runTransaction(async (tx) => {
            const existing = await tx.get(staffPath);
            if (!existing) {
                throw new config_1.AppError("not-found", "Target pharmacist directory record was not found.");
            }
            const now = this.store.nowMs();
            const revokedDoc = {
                ...existing,
                status: nextStatus,
                revokedAt: now,
                revocationReason: reason,
                revokedByUid: admin.uid,
                updatedAt: now,
            };
            tx.set(staffPath, revokedDoc);
            return revokedDoc;
        });
        const existingClaims = await this.authService.getCustomClaims(targetUid);
        await this.authService.setCustomClaims(targetUid, {
            ...existingClaims,
            manoStaffVerified: false,
            manoStaffRole: null,
            manoPharmacyId: null,
        });
        await this.authService.revokeRefreshTokens(targetUid);
        return updatedDoc;
    }
}
exports.PharmacyAdminService = PharmacyAdminService;
//# sourceMappingURL=admin.js.map