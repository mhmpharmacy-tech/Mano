"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.runOutOfBandPharmacistOnboarding = runOutOfBandPharmacistOnboarding;
const config_1 = require("../config");
const auth_1 = require("../auth");
const firestoreStore_1 = require("../firestoreStore");
/**
 * Out-of-band operator utility using Firebase Admin SDK to onboard a verified
 * pharmacist (default: Dr. Mohammad Hossein Mohammadi, license د۳۴۳۶۱) into
 * the live Firestore staff directory and grant `manoStaffVerified=true`.
 */
async function runOutOfBandPharmacistOnboarding(store, authService, options) {
    if (!config_1.FIELD_LIMITS.idRegex.test(options.pharmacyId)) {
        throw new config_1.AppError("invalid-argument", "Invalid pharmacyId.");
    }
    const credRef = options.credentialReference.trim();
    if (credRef.length < config_1.FIELD_LIMITS.credentialRefMinLength ||
        credRef.length > config_1.FIELD_LIMITS.credentialRefMaxLength) {
        throw new config_1.AppError("invalid-argument", "credentialReference must be 4..120 chars.");
    }
    let uid = options.targetUid?.trim() ?? "";
    if (!uid) {
        const email = options.targetEmail?.trim();
        if (!email) {
            throw new config_1.AppError("invalid-argument", "Either targetUid or targetEmail must be provided.");
        }
        if (!authService.getUidByEmail) {
            throw new config_1.AppError("failed-precondition", "Auth service does not support getUidByEmail.");
        }
        const resolvedUid = await authService.getUidByEmail(email);
        if (!resolvedUid) {
            throw new config_1.AppError("not-found", `No Firebase Auth user found for ${email}. Sign in with Google inside the app first.`);
        }
        uid = resolvedUid;
    }
    if (!config_1.FIELD_LIMITS.idRegex.test(uid)) {
        throw new config_1.AppError("invalid-argument", "Resolved UID has invalid format.");
    }
    const pharmacyPath = `pharmacies/${options.pharmacyId}`;
    const staffPath = `pharmacies/${options.pharmacyId}/staff/${uid}`;
    const operatorId = (options.operatorId || "cli_admin_operator").trim();
    const { pharmacy, staff } = await store.runTransaction(async (tx) => {
        await (0, auth_1.assertAccountActiveInTransaction)(tx, uid);
        const now = store.nowMs();
        const existingPharmacy = await tx.get(pharmacyPath);
        const existingStaff = await tx.get(staffPath);
        const nextPharmacy = {
            pharmacyId: options.pharmacyId,
            displayNameFa: existingPharmacy?.displayNameFa || options.pharmacyNameFa.trim(),
            active: true,
            acceptingCases: existingPharmacy?.acceptingCases ?? true,
            createdAt: existingPharmacy?.createdAt ?? now,
            updatedAt: now,
        };
        tx.set(pharmacyPath, nextPharmacy);
        const nextStaff = {
            uid,
            pharmacyId: options.pharmacyId,
            role: "pharmacist",
            status: "ACTIVE",
            credentialVerified: true,
            credentialReference: credRef,
            revokedAt: null,
            revocationReason: null,
            revokedByUid: null,
            onboardedByUid: operatorId,
            createdAt: existingStaff?.createdAt ?? now,
            updatedAt: now,
        };
        tx.set(staffPath, nextStaff);
        return { pharmacy: nextPharmacy, staff: nextStaff };
    });
    const existingClaims = await authService.getCustomClaims(uid);
    const claims = {
        ...existingClaims,
        manoStaffVerified: true,
        manoStaffRole: "pharmacist",
        manoPharmacyId: options.pharmacyId,
    };
    await authService.setCustomClaims(uid, claims);
    return { pharmacy, staff, claims };
}
async function main() {
    const pharmacyId = process.env.MANO_PHARMACY_ID || "mano_central_pharmacy";
    const pharmacyNameFa = process.env.MANO_PHARMACY_NAME_FA || "داروخانه مرکزی تاییدشده مانو";
    const targetUid = process.env.MANO_PHARMACIST_UID;
    const targetEmail = process.env.MANO_PHARMACIST_EMAIL || "mhmpharmacy@gmail.com";
    const credentialReference = process.env.MANO_PHARMACIST_LICENSE_REF || "دکتر محمدحسین محمدی — کد نظام د۳۴۳۶۱";
    const store = new firestoreStore_1.FirestoreBackendStore((0, firestoreStore_1.getNamedFirestore)());
    const authService = new firestoreStore_1.FirebaseAdminAuthService();
    const result = await runOutOfBandPharmacistOnboarding(store, authService, {
        pharmacyId,
        pharmacyNameFa,
        targetUid,
        targetEmail,
        credentialReference,
    });
    console.log("MANO pharmacist onboarding completed:", JSON.stringify(result));
}
if (require.main === module) {
    main().catch((err) => {
        console.error("Pharmacist onboarding failed:", err);
        process.exitCode = 1;
    });
}
//# sourceMappingURL=onboardPharmacist.js.map