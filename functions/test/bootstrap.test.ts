import assert from "node:assert/strict";
import test from "node:test";
import { verifyLivePharmacistAccess } from "../src/auth";
import { AppError } from "../src/config";
import { runOutOfBandBootstrap } from "../src/scripts/bootstrap";
import { runOutOfBandPharmacistOnboarding } from "../src/scripts/onboardPharmacist";
import { BackendAuthService, StaffClaims } from "../src/types";
import { InMemoryBackendStore } from "./inMemoryStore";

class EmailAwareMemoryAuthService implements BackendAuthService {
  private readonly emailToUid = new Map<string, string>();
  private readonly claims = new Map<string, StaffClaims>();

  seedUser(email: string, uid: string, initialClaims: StaffClaims = {}): void {
    this.emailToUid.set(email.trim().toLowerCase(), uid);
    this.claims.set(uid, { ...initialClaims });
  }

  async getUidByEmail(email: string): Promise<string | null> {
    return this.emailToUid.get(email.trim().toLowerCase()) ?? null;
  }

  async getCustomClaims(uid: string): Promise<StaffClaims> {
    return { ...(this.claims.get(uid) ?? {}) };
  }

  async setCustomClaims(uid: string, claims: StaffClaims): Promise<void> {
    this.claims.set(uid, { ...claims });
  }

  async revokeRefreshTokens(_uid: string): Promise<void> {}
}

test("Bootstrap + Pharmacist onboarding resolves email, activates staff directory, and grants verified claims", async () => {
  const store = new InMemoryBackendStore();
  const auth = new EmailAwareMemoryAuthService();
  auth.seedUser("mhmpharmacy@gmail.com", "uid_dr_mohammadi_34361", {
    existingFlag: true,
  });

  const boot = await runOutOfBandBootstrap(store, auth, {
    pharmacyId: "mano_central_pharmacy",
    pharmacyNameFa: "داروخانه مرکزی تاییدشده مانو",
    adminUid: "uid_system_admin",
  });
  assert.equal(boot.pharmacy.active, true);
  assert.equal(boot.adminGrantedUid, "uid_system_admin");
  assert.equal((await auth.getCustomClaims("uid_system_admin")).manoSystemAdmin, true);

  const onboarded = await runOutOfBandPharmacistOnboarding(store, auth, {
    pharmacyId: "mano_central_pharmacy",
    pharmacyNameFa: "داروخانه مرکزی تاییدشده مانو",
    targetEmail: "mhmpharmacy@gmail.com",
    credentialReference: "دکتر محمدحسین محمدی — کد نظام د۳۴۳۶۱",
  });

  assert.equal(onboarded.staff.uid, "uid_dr_mohammadi_34361");
  assert.equal(onboarded.staff.status, "ACTIVE");
  assert.equal(onboarded.staff.credentialVerified, true);
  assert.equal(onboarded.claims.manoStaffVerified, true);
  assert.equal(onboarded.claims.manoStaffRole, "pharmacist");
  assert.equal(onboarded.claims.manoPharmacyId, "mano_central_pharmacy");
  assert.equal(onboarded.claims.existingFlag, true);

  const verified = await verifyLivePharmacistAccess(
    store,
    { uid: "uid_dr_mohammadi_34361", token: onboarded.claims },
    "mano_central_pharmacy",
  );
  assert.equal(verified.staff.credentialReference.includes("د۳۴۳۶۱"), true);
});

test("Pharmacist onboarding fails cleanly if target user has not signed in yet", async () => {
  const store = new InMemoryBackendStore();
  const auth = new EmailAwareMemoryAuthService();

  await assert.rejects(
    () =>
      runOutOfBandPharmacistOnboarding(store, auth, {
        pharmacyId: "mano_central_pharmacy",
        pharmacyNameFa: "داروخانه مرکزی تاییدشده مانو",
        targetEmail: "unknown@example.com",
        credentialReference: "کد نظام د۳۴۳۶۱",
      }),
    (err: unknown) => err instanceof AppError && err.code === "not-found",
  );
});
