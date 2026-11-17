"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.runOutOfBandBootstrap = runOutOfBandBootstrap;
const config_1 = require("../config");
const firestoreStore_1 = require("../firestoreStore");
async function runOutOfBandBootstrap(store, authService, options) {
    if (!config_1.FIELD_LIMITS.idRegex.test(options.pharmacyId)) {
        throw new config_1.AppError("invalid-argument", "Invalid pharmacyId for bootstrap.");
    }
    const pharmacyPath = `pharmacies/${options.pharmacyId}`;
    const pharmacy = await store.runTransaction(async (tx) => {
        const now = store.nowMs();
        const existing = await tx.get(pharmacyPath);
        const doc = {
            pharmacyId: options.pharmacyId,
            displayNameFa: options.pharmacyNameFa.trim(),
            active: true,
            acceptingCases: true,
            createdAt: existing?.createdAt ?? now,
            updatedAt: now,
        };
        tx.set(pharmacyPath, doc);
        return doc;
    });
    let adminGrantedUid = null;
    if (options.adminUid && options.adminUid.trim().length > 0) {
        const uid = options.adminUid.trim();
        const existingClaims = await authService.getCustomClaims(uid);
        await authService.setCustomClaims(uid, {
            ...existingClaims,
            manoSystemAdmin: true,
        });
        adminGrantedUid = uid;
    }
    return { pharmacy, adminGrantedUid };
}
async function main() {
    const pharmacyId = process.env.MANO_BOOTSTRAP_PHARMACY_ID || "mano_central_pharmacy";
    const pharmacyNameFa = process.env.MANO_BOOTSTRAP_PHARMACY_NAME_FA || "داروخانه مرکزی تاییدشده مانو";
    const adminUid = process.env.MANO_BOOTSTRAP_ADMIN_UID;
    const store = new firestoreStore_1.FirestoreBackendStore((0, firestoreStore_1.getNamedFirestore)());
    const authService = new firestoreStore_1.FirebaseAdminAuthService();
    const res = await runOutOfBandBootstrap(store, authService, {
        pharmacyId,
        pharmacyNameFa,
        adminUid,
    });
    console.log("MANO out-of-band bootstrap completed:", JSON.stringify(res));
}
if (require.main === module) {
    main().catch((err) => {
        console.error("Bootstrap failed:", err);
        process.exitCode = 1;
    });
}
//# sourceMappingURL=bootstrap.js.map