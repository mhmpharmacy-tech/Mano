import { AppError, FIELD_LIMITS } from "./config";
import {
  BackendStore,
  CallerContext,
  PharmacyRegistryDoc,
  StaffDirectoryDoc,
  StoreTransaction,
} from "./types";

export function assertAuthenticated(
  caller: CallerContext | null | undefined,
): CallerContext {
  if (!caller || typeof caller.uid !== "string" || !caller.uid.trim()) {
    throw new AppError(
      "unauthenticated",
      "Authentication is required for this endpoint.",
    );
  }
  return caller;
}

export function assertValidId(value: unknown, fieldName: string): string {
  if (typeof value !== "string" || !FIELD_LIMITS.idRegex.test(value)) {
    throw new AppError(
      "invalid-argument",
      `${fieldName} must be 1..128 characters matching ^[a-zA-Z0-9_-]+$.`,
    );
  }
  return value;
}

/**
 * Verifies verified pharmacist custom claims AND rejects administrative
 * accounts attempting clinical inbox or case approval actions.
 */
export function assertPharmacistClaims(
  caller: CallerContext | null | undefined,
  pharmacyId: string,
): CallerContext {
  const authed = assertAuthenticated(caller);
  const token = authed.token || {};

  if (token.manoStaffRole === "admin") {
    throw new AppError(
      "permission-denied",
      "Administrative accounts cannot access or approve clinical case decisions.",
    );
  }

  if (token.manoStaffVerified !== true) {
    throw new AppError(
      "permission-denied",
      "Verified staff claim manoStaffVerified=true is required.",
    );
  }

  if (token.manoStaffRole !== "pharmacist") {
    throw new AppError(
      "permission-denied",
      "Only verified pharmacist staff accounts are permitted.",
    );
  }

  if (token.manoPharmacyId !== pharmacyId) {
    throw new AppError(
      "permission-denied",
      "Cross-tenant access denied: staff pharmacy claim does not match target pharmacyId.",
    );
  }

  return authed;
}

/**
 * Validates the live directory document at pharmacies/{pharmacyId}/staff/{uid}.
 * An old valid custom-claim token never overrides a revoked, suspended,
 * unverified, or missing directory record.
 */
export function assertVerifiedActiveStaffRecord(
  staffRecord: StaffDirectoryDoc | null,
  uid: string,
  pharmacyId: string,
): StaffDirectoryDoc {
  if (!staffRecord) {
    throw new AppError(
      "permission-denied",
      "Staff directory record not found; access revoked or not onboarded.",
    );
  }

  if (staffRecord.uid !== uid || staffRecord.pharmacyId !== pharmacyId) {
    throw new AppError(
      "permission-denied",
      "Staff directory identity or pharmacy tenant mismatch.",
    );
  }

  if (staffRecord.role !== "pharmacist") {
    throw new AppError(
      "permission-denied",
      "Staff directory role must be pharmacist.",
    );
  }

  if (staffRecord.status !== "ACTIVE") {
    throw new AppError(
      "permission-denied",
      "Staff membership is not ACTIVE.",
    );
  }

  if (staffRecord.credentialVerified !== true) {
    throw new AppError(
      "permission-denied",
      "Staff pharmacist credential is not verified.",
    );
  }

  if (staffRecord.revokedAt !== null) {
    throw new AppError(
      "permission-denied",
      "Staff access has been revoked.",
    );
  }

  return staffRecord;
}

export function assertActivePharmacy(
  pharmacy: PharmacyRegistryDoc | null,
  requireAcceptingCases = false,
): PharmacyRegistryDoc {
  if (!pharmacy || pharmacy.active !== true) {
    throw new AppError(
      "failed-precondition",
      "Target pharmacy registry record is missing or inactive.",
    );
  }
  if (requireAcceptingCases && pharmacy.acceptingCases !== true) {
    throw new AppError(
      "failed-precondition",
      "Target pharmacy is not currently accepting consultation cases.",
    );
  }
  return pharmacy;
}

export async function verifyLivePharmacistAccess(
  store: BackendStore,
  caller: CallerContext | null | undefined,
  pharmacyId: string,
): Promise<{
  caller: CallerContext;
  pharmacy: PharmacyRegistryDoc;
  staff: StaffDirectoryDoc;
}> {
  const authed = assertPharmacistClaims(caller, pharmacyId);

  const [pharmacy, staffRecord] = await Promise.all([
    store.get<PharmacyRegistryDoc>(`pharmacies/${pharmacyId}`),
    store.get<StaffDirectoryDoc>(`pharmacies/${pharmacyId}/staff/${authed.uid}`),
  ]);

  const validPharmacy = assertActivePharmacy(pharmacy, false);
  const validStaff = assertVerifiedActiveStaffRecord(
    staffRecord,
    authed.uid,
    pharmacyId,
  );

  return { caller: authed, pharmacy: validPharmacy, staff: validStaff };
}

export async function verifyPharmacistAccessInTransaction(
  tx: StoreTransaction,
  caller: CallerContext | null | undefined,
  pharmacyId: string,
): Promise<{
  caller: CallerContext;
  pharmacy: PharmacyRegistryDoc;
  staff: StaffDirectoryDoc;
}> {
  const authed = assertPharmacistClaims(caller, pharmacyId);

  const pharmacy = await tx.get<PharmacyRegistryDoc>(`pharmacies/${pharmacyId}`);
  const staffRecord = await tx.get<StaffDirectoryDoc>(
    `pharmacies/${pharmacyId}/staff/${authed.uid}`,
  );

  const validPharmacy = assertActivePharmacy(pharmacy, false);
  const validStaff = assertVerifiedActiveStaffRecord(
    staffRecord,
    authed.uid,
    pharmacyId,
  );

  return { caller: authed, pharmacy: validPharmacy, staff: validStaff };
}

export function assertSystemAdmin(
  caller: CallerContext | null | undefined,
): CallerContext {
  const authed = assertAuthenticated(caller);
  if (authed.token?.manoSystemAdmin !== true) {
    throw new AppError(
      "permission-denied",
      "Trusted administrative claim manoSystemAdmin=true is required.",
    );
  }
  return authed;
}
