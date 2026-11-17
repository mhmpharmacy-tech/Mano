import { AppError, FIELD_LIMITS } from "../config";
import { FirebaseAdminAuthService, FirestoreBackendStore, getNamedFirestore } from "../firestoreStore";
import { BackendAuthService, BackendStore, PharmacyRegistryDoc } from "../types";

export interface BootstrapOptions {
  pharmacyId: string;
  pharmacyNameFa: string;
  adminUid?: string;
}

export async function runOutOfBandBootstrap(
  store: BackendStore,
  authService: BackendAuthService,
  options: BootstrapOptions,
): Promise<{ pharmacy: PharmacyRegistryDoc; adminGrantedUid: string | null }> {
  if (!FIELD_LIMITS.idRegex.test(options.pharmacyId)) {
    throw new AppError("invalid-argument", "Invalid pharmacyId for bootstrap.");
  }
  const pharmacyPath = `pharmacies/${options.pharmacyId}`;
  const pharmacy = await store.runTransaction(async (tx) => {
    const now = store.nowMs();
    const existing = await tx.get<PharmacyRegistryDoc>(pharmacyPath);
    const doc: PharmacyRegistryDoc = {
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

  let adminGrantedUid: string | null = null;
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

  const store = new FirestoreBackendStore(getNamedFirestore());
  const authService = new FirebaseAdminAuthService();
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
