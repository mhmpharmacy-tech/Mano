import { assertSystemAdmin, assertValidId } from "./auth";
import { AppError, FIELD_LIMITS, RATE_LIMITS } from "./config";
import { consumeRateLimit } from "./rateLimit";
import {
  BackendAuthService,
  BackendStore,
  CallerContext,
  PharmacyRegistryDoc,
  StaffDirectoryDoc,
} from "./types";

export interface UpsertPharmacyRegistryRequest {
  pharmacyId: string;
  displayNameFa: string;
  active: boolean;
  acceptingCases: boolean;
}

export interface OnboardPharmacistRequest {
  pharmacyId: string;
  targetUid: string;
  credentialReference: string;
  credentialVerified: boolean;
}

export interface RevokePharmacistRequest {
  pharmacyId: string;
  targetUid: string;
  status?: "REVOKED" | "SUSPENDED";
  reason: string;
}

export class PharmacyAdminService {
  constructor(
    private readonly store: BackendStore,
    private readonly authService: BackendAuthService,
  ) {}

  async upsertPharmacyRegistry(
    caller: CallerContext | null | undefined,
    req: UpsertPharmacyRegistryRequest,
  ): Promise<PharmacyRegistryDoc> {
    const admin = assertSystemAdmin(caller);
    const pharmacyId = assertValidId(req.pharmacyId, "pharmacyId");

    if (
      typeof req.displayNameFa !== "string" ||
      req.displayNameFa.trim().length < 2 ||
      req.displayNameFa.trim().length > 120
    ) {
      throw new AppError(
        "invalid-argument",
        "displayNameFa must be between 2 and 120 characters.",
      );
    }

    await consumeRateLimit(
      this.store,
      admin.uid,
      "admin_action",
      RATE_LIMITS.adminAction.maxRequests,
      RATE_LIMITS.adminAction.windowMs,
    );

    const pharmacyPath = `pharmacies/${pharmacyId}`;
    return this.store.runTransaction(async (tx) => {
      const now = this.store.nowMs();
      const existing = await tx.get<PharmacyRegistryDoc>(pharmacyPath);
      const doc: PharmacyRegistryDoc = {
        pharmacyId,
        displayNameFa: req.displayNameFa.trim(),
        active: req.active === true,
        acceptingCases: req.active === true && req.acceptingCases === true,
        createdAt: existing?.createdAt ?? now,
        updatedAt: now,
      };
      tx.set<PharmacyRegistryDoc>(pharmacyPath, doc);
      return doc;
    });
  }

  async onboardPharmacist(
    caller: CallerContext | null | undefined,
    req: OnboardPharmacistRequest,
  ): Promise<StaffDirectoryDoc> {
    const admin = assertSystemAdmin(caller);
    const pharmacyId = assertValidId(req.pharmacyId, "pharmacyId");
    const targetUid = assertValidId(req.targetUid, "targetUid");

    if (req.credentialVerified !== true) {
      throw new AppError(
        "failed-precondition",
        "Pharmacist onboarding requires verified credentials (credentialVerified=true).",
      );
    }

    const credRef =
      typeof req.credentialReference === "string"
        ? req.credentialReference.trim()
        : "";
    if (
      credRef.length < FIELD_LIMITS.credentialRefMinLength ||
      credRef.length > FIELD_LIMITS.credentialRefMaxLength
    ) {
      throw new AppError(
        "invalid-argument",
        "credentialReference must be 4..120 characters.",
      );
    }

    await consumeRateLimit(
      this.store,
      admin.uid,
      "admin_action",
      RATE_LIMITS.adminAction.maxRequests,
      RATE_LIMITS.adminAction.windowMs,
    );

    const staffPath = `pharmacies/${pharmacyId}/staff/${targetUid}`;
    const staffDoc = await this.store.runTransaction(async (tx) => {
      const pharmacy = await tx.get<PharmacyRegistryDoc>(
        `pharmacies/${pharmacyId}`,
      );
      if (!pharmacy || pharmacy.active !== true) {
        throw new AppError(
          "failed-precondition",
          "Cannot onboard staff into a missing or inactive pharmacy.",
        );
      }

      const existing = await tx.get<StaffDirectoryDoc>(staffPath);
      const now = this.store.nowMs();
      const doc: StaffDirectoryDoc = {
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
      tx.set<StaffDirectoryDoc>(staffPath, doc);
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

  async revokePharmacist(
    caller: CallerContext | null | undefined,
    req: RevokePharmacistRequest,
  ): Promise<StaffDirectoryDoc> {
    const admin = assertSystemAdmin(caller);
    const pharmacyId = assertValidId(req.pharmacyId, "pharmacyId");
    const targetUid = assertValidId(req.targetUid, "targetUid");

    const nextStatus =
      req.status === "SUSPENDED" ? "SUSPENDED" : "REVOKED";
    const reason = typeof req.reason === "string" ? req.reason.trim() : "";
    if (reason.length < 3 || reason.length > 300) {
      throw new AppError(
        "invalid-argument",
        "Revocation reason must be 3..300 characters.",
      );
    }

    await consumeRateLimit(
      this.store,
      admin.uid,
      "admin_action",
      RATE_LIMITS.adminAction.maxRequests,
      RATE_LIMITS.adminAction.windowMs,
    );

    const staffPath = `pharmacies/${pharmacyId}/staff/${targetUid}`;
    const updatedDoc = await this.store.runTransaction(async (tx) => {
      const existing = await tx.get<StaffDirectoryDoc>(staffPath);
      if (!existing) {
        throw new AppError(
          "not-found",
          "Target pharmacist directory record was not found.",
        );
      }

      const now = this.store.nowMs();
      const revokedDoc: StaffDirectoryDoc = {
        ...existing,
        status: nextStatus,
        revokedAt: now,
        revocationReason: reason,
        revokedByUid: admin.uid,
        updatedAt: now,
      };
      tx.set<StaffDirectoryDoc>(staffPath, revokedDoc);
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
