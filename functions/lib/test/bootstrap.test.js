"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const strict_1 = __importDefault(require("node:assert/strict"));
const node_test_1 = __importDefault(require("node:test"));
const auth_1 = require("../src/auth");
const config_1 = require("../src/config");
const bootstrap_1 = require("../src/scripts/bootstrap");
const onboardPharmacist_1 = require("../src/scripts/onboardPharmacist");
const inMemoryStore_1 = require("./inMemoryStore");
class EmailAwareMemoryAuthService {
    emailToUid = new Map();
    claims = new Map();
    seedUser(email, uid, initialClaims = {}) {
        this.emailToUid.set(email.trim().toLowerCase(), uid);
        this.claims.set(uid, { ...initialClaims });
    }
    async getUidByEmail(email) {
        return this.emailToUid.get(email.trim().toLowerCase()) ?? null;
    }
    async getCustomClaims(uid) {
        return { ...(this.claims.get(uid) ?? {}) };
    }
    async setCustomClaims(uid, claims) {
        this.claims.set(uid, { ...claims });
    }
    async revokeRefreshTokens(_uid) { }
}
(0, node_test_1.default)("Bootstrap + Pharmacist onboarding resolves email, activates staff directory, and grants verified claims", async () => {
    const store = new inMemoryStore_1.InMemoryBackendStore();
    const auth = new EmailAwareMemoryAuthService();
    auth.seedUser("mhmpharmacy@gmail.com", "uid_dr_mohammadi_34361", {
        existingFlag: true,
    });
    const boot = await (0, bootstrap_1.runOutOfBandBootstrap)(store, auth, {
        pharmacyId: "mano_central_pharmacy",
        pharmacyNameFa: "داروخانه مرکزی تاییدشده مانو",
        adminUid: "uid_system_admin",
    });
    strict_1.default.equal(boot.pharmacy.active, true);
    strict_1.default.equal(boot.adminGrantedUid, "uid_system_admin");
    strict_1.default.equal((await auth.getCustomClaims("uid_system_admin")).manoSystemAdmin, true);
    const onboarded = await (0, onboardPharmacist_1.runOutOfBandPharmacistOnboarding)(store, auth, {
        pharmacyId: "mano_central_pharmacy",
        pharmacyNameFa: "داروخانه مرکزی تاییدشده مانو",
        targetEmail: "mhmpharmacy@gmail.com",
        credentialReference: "دکتر محمدحسین محمدی — کد نظام د۳۴۳۶۱",
    });
    strict_1.default.equal(onboarded.staff.uid, "uid_dr_mohammadi_34361");
    strict_1.default.equal(onboarded.staff.status, "ACTIVE");
    strict_1.default.equal(onboarded.staff.credentialVerified, true);
    strict_1.default.equal(onboarded.claims.manoStaffVerified, true);
    strict_1.default.equal(onboarded.claims.manoStaffRole, "pharmacist");
    strict_1.default.equal(onboarded.claims.manoPharmacyId, "mano_central_pharmacy");
    strict_1.default.equal(onboarded.claims.existingFlag, true);
    const verified = await (0, auth_1.verifyLivePharmacistAccess)(store, { uid: "uid_dr_mohammadi_34361", token: onboarded.claims }, "mano_central_pharmacy");
    strict_1.default.equal(verified.staff.credentialReference.includes("د۳۴۳۶۱"), true);
});
(0, node_test_1.default)("Pharmacist onboarding fails cleanly if target user has not signed in yet", async () => {
    const store = new inMemoryStore_1.InMemoryBackendStore();
    const auth = new EmailAwareMemoryAuthService();
    await strict_1.default.rejects(() => (0, onboardPharmacist_1.runOutOfBandPharmacistOnboarding)(store, auth, {
        pharmacyId: "mano_central_pharmacy",
        pharmacyNameFa: "داروخانه مرکزی تاییدشده مانو",
        targetEmail: "unknown@example.com",
        credentialReference: "کد نظام د۳۴۳۶۱",
    }), (err) => err instanceof config_1.AppError && err.code === "not-found");
});
//# sourceMappingURL=bootstrap.test.js.map