import {
  assertActivePharmacy,
  assertAuthenticated,
  assertValidId,
  verifyLivePharmacistAccess,
  verifyPharmacistAccessInTransaction,
} from "./auth";
import {
  AppError,
  CONSENT_VERSION,
  FIELD_LIMITS,
  RATE_LIMITS,
  TRIAGE_PASS_TTL_MS,
  VALID_AGE_BANDS,
  VALID_CASE_STATUSES,
  VALID_CATEGORIES,
  VALID_SYMPTOM_FLOWS,
} from "./config";
import { consumeRateLimit } from "./rateLimit";
import { evaluateServerTriage } from "./triage";
import {
  AgeBandId,
  AuditEventDoc,
  BackendStore,
  CallerContext,
  ConsultationCaseDoc,
  ConsultationCategory,
  ConsultationStatus,
  InternalStaffNote,
  ParentClinicalResponse,
  ParentConsultationView,
  PharmacyInboxCaseView,
  PharmacyRegistryDoc,
  PublicPharmacyOption,
  SymptomFlowId,
  TriagePassDoc,
} from "./types";

export interface IssueTriagePassRequest {
  ageBandId: string;
  symptomFlowId?: string;
  temperatureCelsius?: number | null;
  durationHours?: number | null;
  selectedRedFlagIds?: string[];
  questionSummaryFa?: string;
  hasUrgentFlag?: boolean;
}

export interface IssueTriagePassResponse {
  outcome: "URGENT_EMERGENCY" | "NONURGENT";
  eligibleForNonUrgentConsultation: boolean;
  triagePassId: string | null;
  expiresAt: number | null;
  ruleId: string;
  titleFa: string;
  summaryFa: string;
  emergencyActionsFa: string[];
}

export interface SubmitConsultationCaseRequest {
  pharmacyId: string;
  triagePassId: string;
  categoryName: string;
  ageBandId: string;
  symptomFlowId?: string;
  questionSummaryFa: string;
  hasUrgentFlag: boolean;
  consentAccepted: boolean;
  consentVersion: string;
}

export interface ListParentConsultationsRequest {
  limit?: number;
}

export interface GetParentConsultationRequest {
  pharmacyId: string;
  caseId: string;
}

export interface GetPharmacyInboxRequest {
  pharmacyId: string;
  statusFilter?: string;
  limit?: number;
}

export interface ReviewConsultationCaseRequest {
  pharmacyId: string;
  caseId: string;
  nextStatus: string;
  internalNote?: string | null;
  parentReply?: string | null;
  pharmacistAttestation?: boolean;
}

export function toParentConsultationView(
  doc: ConsultationCaseDoc,
): ParentConsultationView {
  return {
    caseId: doc.caseId,
    pharmacyId: doc.pharmacyId,
    categoryName: doc.categoryName,
    ageBandId: doc.ageBandId,
    symptomFlowId: doc.symptomFlowId,
    questionSummaryFa: doc.questionSummaryFa,
    triageOutcome: doc.triageOutcome,
    consentVersion: doc.consentVersion,
    status: doc.status,
    parentResponse: doc.parentResponse,
    receivedAt: doc.receivedAt,
    reviewedAt: doc.reviewedAt,
    createdAt: doc.createdAt,
    updatedAt: doc.updatedAt,
  };
}

export function toPharmacyInboxCaseView(
  doc: ConsultationCaseDoc,
): PharmacyInboxCaseView {
  return {
    caseId: doc.caseId,
    pharmacyId: doc.pharmacyId,
    categoryName: doc.categoryName,
    ageBandId: doc.ageBandId,
    symptomFlowId: doc.symptomFlowId,
    questionSummaryFa: doc.questionSummaryFa,
    hasUrgentFlag: doc.hasUrgentFlag,
    triageOutcome: doc.triageOutcome,
    consentVersion: doc.consentVersion,
    status: doc.status,
    internalNotes: doc.internalNotes,
    parentResponse: doc.parentResponse,
    receivedByUid: doc.receivedByUid,
    receivedAt: doc.receivedAt,
    reviewedByUid: doc.reviewedByUid,
    reviewedAt: doc.reviewedAt,
    createdAt: doc.createdAt,
    updatedAt: doc.updatedAt,
  };
}

export class ConsultationCaseService {
  constructor(private readonly store: BackendStore) {}

  async listAcceptingPharmacies(
    caller: CallerContext | null | undefined,
  ): Promise<PublicPharmacyOption[]> {
    assertAuthenticated(caller);
    const pharmacies = await this.store.queryCollection<PharmacyRegistryDoc>(
      "pharmacies",
      [
        { field: "active", op: "==", value: true },
        { field: "acceptingCases", op: "==", value: true },
      ],
      "updatedAt",
      "desc",
      20,
    );
    return pharmacies
      .filter((p) => p.active === true && p.acceptingCases === true)
      .map((p) => ({
        pharmacyId: p.pharmacyId,
        displayNameFa: p.displayNameFa,
        acceptingCases: p.acceptingCases,
      }));
  }

  async issueConsultationTriagePass(
    caller: CallerContext | null | undefined,
    req: IssueTriagePassRequest,
  ): Promise<IssueTriagePassResponse> {
    const authed = assertAuthenticated(caller);
    await consumeRateLimit(
      this.store,
      authed.uid,
      "triage_pass",
      RATE_LIMITS.triagePass.maxRequests,
      RATE_LIMITS.triagePass.windowMs,
    );

    const decision = evaluateServerTriage(req);
    if (decision.outcome !== "NONURGENT") {
      return {
        outcome: "URGENT_EMERGENCY",
        eligibleForNonUrgentConsultation: false,
        triagePassId: null,
        expiresAt: null,
        ruleId: decision.ruleId,
        titleFa: decision.titleFa,
        summaryFa: decision.summaryFa,
        emergencyActionsFa: decision.emergencyActionsFa,
      };
    }

    const now = this.store.nowMs();
    const passId = this.store.generateId("tp");
    const expiresAt = now + TRIAGE_PASS_TTL_MS;

    const passDoc: TriagePassDoc = {
      passId,
      uid: authed.uid,
      outcome: "NONURGENT",
      ageBandId: decision.ageBandId,
      symptomFlowId: decision.symptomFlowId,
      ruleId: decision.ruleId,
      consumed: false,
      consumedAt: null,
      consumedByCaseId: null,
      expiresAt,
      createdAt: now,
    };

    await this.store.runTransaction(async (tx) => {
      tx.create<TriagePassDoc>(`triagePasses/${passId}`, passDoc);
    });

    return {
      outcome: "NONURGENT",
      eligibleForNonUrgentConsultation: true,
      triagePassId: passId,
      expiresAt,
      ruleId: decision.ruleId,
      titleFa: decision.titleFa,
      summaryFa: decision.summaryFa,
      emergencyActionsFa: [],
    };
  }

  async submitConsultationCase(
    caller: CallerContext | null | undefined,
    req: SubmitConsultationCaseRequest,
  ): Promise<ParentConsultationView> {
    const authed = assertAuthenticated(caller);

    const pharmacyId = assertValidId(req.pharmacyId, "pharmacyId");
    const triagePassId = assertValidId(req.triagePassId, "triagePassId");

    if (!VALID_CATEGORIES.includes(req.categoryName as ConsultationCategory)) {
      throw new AppError("invalid-argument", "Invalid categoryName.");
    }
    const categoryName = req.categoryName as ConsultationCategory;

    if (!VALID_AGE_BANDS.includes(req.ageBandId as AgeBandId)) {
      throw new AppError("invalid-argument", "Invalid ageBandId.");
    }
    const ageBandId = req.ageBandId as AgeBandId;

    const flowCandidate = req.symptomFlowId ?? "general";
    if (!VALID_SYMPTOM_FLOWS.includes(flowCandidate as SymptomFlowId)) {
      throw new AppError("invalid-argument", "Invalid symptomFlowId.");
    }
    const symptomFlowId = flowCandidate as SymptomFlowId;

    if (req.hasUrgentFlag !== false) {
      throw new AppError(
        "failed-precondition",
        "Consultation intake rejects urgent requests; direct caregiver to emergency care.",
      );
    }

    if (req.consentAccepted !== true || req.consentVersion !== CONSENT_VERSION) {
      throw new AppError(
        "failed-precondition",
        `Explicit consentAccepted=true with consentVersion=${CONSENT_VERSION} is required.`,
      );
    }

    if (typeof req.questionSummaryFa !== "string") {
      throw new AppError("invalid-argument", "questionSummaryFa is required.");
    }
    const questionSummaryFa = req.questionSummaryFa.trim();
    if (
      questionSummaryFa.length < FIELD_LIMITS.questionMinLength ||
      questionSummaryFa.length > FIELD_LIMITS.questionMaxLength
    ) {
      throw new AppError(
        "invalid-argument",
        `questionSummaryFa must be between ${FIELD_LIMITS.questionMinLength} and ${FIELD_LIMITS.questionMaxLength} characters.`,
      );
    }

    // Re-evaluate submitted question text so unsafe keywords cannot bypass triage pass issuance
    const textTriageCheck = evaluateServerTriage({
      ageBandId,
      symptomFlowId,
      questionSummaryFa,
      hasUrgentFlag: req.hasUrgentFlag,
    });
    if (textTriageCheck.outcome !== "NONURGENT") {
      throw new AppError(
        "failed-precondition",
        "Submitted question contains urgent safety signals and cannot enter non-urgent pharmacist intake.",
        { ruleId: textTriageCheck.ruleId },
      );
    }

    await consumeRateLimit(
      this.store,
      authed.uid,
      "case_submit",
      RATE_LIMITS.caseSubmit.maxRequests,
      RATE_LIMITS.caseSubmit.windowMs,
    );

    const caseId = this.store.generateId("case");
    const createdCase = await this.store.runTransaction(async (tx) => {
      const now = this.store.nowMs();
      const pharmacy = await tx.get<PharmacyRegistryDoc>(
        `pharmacies/${pharmacyId}`,
      );
      assertActivePharmacy(pharmacy, true);

      const passRef = `triagePasses/${triagePassId}`;
      const passDoc = await tx.get<TriagePassDoc>(passRef);
      if (!passDoc) {
        throw new AppError(
          "permission-denied",
          "A valid server-issued NONURGENT triage pass is required.",
        );
      }
      if (passDoc.uid !== authed.uid) {
        throw new AppError(
          "permission-denied",
          "Triage pass does not belong to the authenticated parent.",
        );
      }
      if (passDoc.outcome !== "NONURGENT") {
        throw new AppError(
          "failed-precondition",
          "Triage pass outcome is not NONURGENT.",
        );
      }
      if (passDoc.consumed === true || passDoc.consumedByCaseId !== null) {
        throw new AppError(
          "failed-precondition",
          "Triage pass has already been consumed.",
        );
      }
      if (passDoc.expiresAt <= now) {
        throw new AppError(
          "failed-precondition",
          "Triage pass has expired; please re-run triage evaluation.",
        );
      }
      if (passDoc.ageBandId !== ageBandId) {
        throw new AppError(
          "invalid-argument",
          "Triage pass ageBandId does not match submitted consultation.",
        );
      }

      const newCase: ConsultationCaseDoc = {
        caseId,
        pharmacyId,
        parentUid: authed.uid,
        categoryName,
        ageBandId,
        symptomFlowId,
        questionSummaryFa,
        hasUrgentFlag: false,
        triagePassId,
        triageOutcome: "NONURGENT",
        consentAccepted: true,
        consentVersion: CONSENT_VERSION,
        status: "PENDING",
        internalNotes: [],
        parentResponse: null,
        receivedByUid: null,
        receivedAt: null,
        reviewedByUid: null,
        reviewedAt: null,
        createdAt: now,
        updatedAt: now,
      };

      const auditEventId = this.store.generateId("evt");
      const auditEvent: AuditEventDoc = {
        eventId: auditEventId,
        caseId,
        pharmacyId,
        action: "CASE_CREATED",
        fromStatus: null,
        toStatus: "PENDING",
        actorUid: authed.uid,
        actorRole: "parent",
        attestationConfirmed: false,
        triagePassId,
        internalNoteRecorded: false,
        parentReplyPublished: false,
        createdAt: now,
      };

      tx.update(passRef, {
        consumed: true,
        consumedAt: now,
        consumedByCaseId: caseId,
      });

      tx.create<ConsultationCaseDoc>(
        `pharmacies/${pharmacyId}/cases/${caseId}`,
        newCase,
      );
      tx.create<AuditEventDoc>(
        `pharmacies/${pharmacyId}/cases/${caseId}/auditEvents/${auditEventId}`,
        auditEvent,
      );

      return newCase;
    });

    return toParentConsultationView(createdCase);
  }

  async getParentConsultation(
    caller: CallerContext | null | undefined,
    req: GetParentConsultationRequest,
  ): Promise<ParentConsultationView> {
    const authed = assertAuthenticated(caller);
    const pharmacyId = assertValidId(req.pharmacyId, "pharmacyId");
    const caseId = assertValidId(req.caseId, "caseId");

    const caseDoc = await this.store.get<ConsultationCaseDoc>(
      `pharmacies/${pharmacyId}/cases/${caseId}`,
    );
    if (!caseDoc) {
      throw new AppError("not-found", "Consultation case was not found.");
    }
    if (caseDoc.parentUid !== authed.uid) {
      throw new AppError(
        "permission-denied",
        "Parents may only read their own consultation cases.",
      );
    }

    return toParentConsultationView(caseDoc);
  }

  async listParentConsultations(
    caller: CallerContext | null | undefined,
    req: ListParentConsultationsRequest = {},
  ): Promise<ParentConsultationView[]> {
    const authed = assertAuthenticated(caller);
    const limit = Math.min(Math.max(Number(req.limit ?? 25), 1), 50);

    const cases = await this.store.queryCollectionGroup<ConsultationCaseDoc>(
      "cases",
      [{ field: "parentUid", op: "==", value: authed.uid }],
      "createdAt",
      "desc",
      limit,
    );

    return cases
      .filter((doc) => doc.parentUid === authed.uid)
      .map(toParentConsultationView);
  }

  async getPharmacyInbox(
    caller: CallerContext | null | undefined,
    req: GetPharmacyInboxRequest,
  ): Promise<PharmacyInboxCaseView[]> {
    const pharmacyId = assertValidId(req.pharmacyId, "pharmacyId");

    // Verifies custom claims AND reads live directory record on every request
    const { caller: authed } = await verifyLivePharmacistAccess(
      this.store,
      caller,
      pharmacyId,
    );

    await consumeRateLimit(
      this.store,
      authed.uid,
      "inbox_read",
      RATE_LIMITS.inboxRead.maxRequests,
      RATE_LIMITS.inboxRead.windowMs,
    );

    const filters = [];
    if (req.statusFilter !== undefined && req.statusFilter !== null) {
      if (
        !VALID_CASE_STATUSES.includes(req.statusFilter as ConsultationStatus)
      ) {
        throw new AppError("invalid-argument", "Invalid statusFilter.");
      }
      filters.push({
        field: "status",
        op: "==" as const,
        value: req.statusFilter,
      });
    }

    const limit = Math.min(Math.max(Number(req.limit ?? 30), 1), 100);
    const cases = await this.store.queryCollection<ConsultationCaseDoc>(
      `pharmacies/${pharmacyId}/cases`,
      filters,
      "createdAt",
      "desc",
      limit,
    );

    return cases.map(toPharmacyInboxCaseView);
  }

  async reviewConsultationCase(
    caller: CallerContext | null | undefined,
    req: ReviewConsultationCaseRequest,
  ): Promise<PharmacyInboxCaseView> {
    const pharmacyId = assertValidId(req.pharmacyId, "pharmacyId");
    const caseId = assertValidId(req.caseId, "caseId");

    if (!VALID_CASE_STATUSES.includes(req.nextStatus as ConsultationStatus)) {
      throw new AppError("invalid-argument", "Invalid nextStatus.");
    }
    const nextStatus = req.nextStatus as ConsultationStatus;
    if (nextStatus === "PENDING") {
      throw new AppError(
        "failed-precondition",
        "Cannot transition a case back to PENDING.",
      );
    }

    const trimmedInternalNote =
      typeof req.internalNote === "string" ? req.internalNote.trim() : "";
    if (trimmedInternalNote.length > FIELD_LIMITS.internalNoteMaxLength) {
      throw new AppError(
        "invalid-argument",
        `internalNote cannot exceed ${FIELD_LIMITS.internalNoteMaxLength} characters.`,
      );
    }

    const trimmedParentReply =
      typeof req.parentReply === "string" ? req.parentReply.trim() : "";
    if (trimmedParentReply.length > FIELD_LIMITS.parentReplyMaxLength) {
      throw new AppError(
        "invalid-argument",
        `parentReply cannot exceed ${FIELD_LIMITS.parentReplyMaxLength} characters.`,
      );
    }

    // Fast-fail claims check prior to transaction
    const authedPreliminary = await verifyPharmacistQuickClaims(
      caller,
      pharmacyId,
    );

    await consumeRateLimit(
      this.store,
      authedPreliminary.uid,
      "case_review",
      RATE_LIMITS.caseReview.maxRequests,
      RATE_LIMITS.caseReview.windowMs,
    );

    const updatedCase = await this.store.runTransaction(async (tx) => {
      // MANDATORY: Re-read active credential-verified staff directory inside transaction
      // before committing any state transition!
      const { caller: authed } = await verifyPharmacistAccessInTransaction(
        tx,
        caller,
        pharmacyId,
      );

      const casePath = `pharmacies/${pharmacyId}/cases/${caseId}`;
      const existingCase = await tx.get<ConsultationCaseDoc>(casePath);
      if (!existingCase) {
        throw new AppError("not-found", "Consultation case was not found.");
      }

      if (existingCase.pharmacyId !== pharmacyId) {
        throw new AppError(
          "permission-denied",
          "Tenant mismatch on consultation case.",
        );
      }

      const fromStatus = existingCase.status;
      if (fromStatus === "REVIEWED") {
        throw new AppError(
          "failed-precondition",
          "Consultation case is already REVIEWED and locked.",
        );
      }

      const validTransition =
        (fromStatus === "PENDING" && nextStatus === "RECEIVED") ||
        (fromStatus === "RECEIVED" && nextStatus === "REVIEWED");
      if (!validTransition) {
        throw new AppError(
          "failed-precondition",
          `Invalid state transition from ${fromStatus} to ${nextStatus}; sequential PENDING -> RECEIVED -> REVIEWED is required.`,
        );
      }

      if (nextStatus === "RECEIVED" && trimmedParentReply.length > 0) {
        throw new AppError(
          "invalid-argument",
          "Internal receipt acknowledgment (RECEIVED) cannot publish a parent-facing clinical reply without REVIEWED attestation.",
        );
      }

      if (nextStatus === "REVIEWED") {
        if (req.pharmacistAttestation !== true) {
          throw new AppError(
            "failed-precondition",
            "No clinical response may be posted without explicit pharmacistAttestation=true.",
          );
        }
        if (trimmedParentReply.length < FIELD_LIMITS.parentReplyMinLength) {
          throw new AppError(
            "invalid-argument",
            `parentReply must be at least ${FIELD_LIMITS.parentReplyMinLength} characters when marking REVIEWED.`,
          );
        }
      }

      const now = this.store.nowMs();
      const updatedNotes: InternalStaffNote[] = [...existingCase.internalNotes];
      if (trimmedInternalNote.length > 0) {
        updatedNotes.push({
          noteId: this.store.generateId("note"),
          authorUid: authed.uid,
          noteText: trimmedInternalNote,
          fromStatus,
          toStatus: nextStatus,
          createdAt: now,
        });
      }

      let parentResponse: ParentClinicalResponse | null =
        existingCase.parentResponse;
      if (nextStatus === "REVIEWED") {
        parentResponse = {
          replyTextFa: trimmedParentReply,
          respondedByRole: "pharmacist",
          respondedAt: now,
          attestationConfirmed: true,
        };
      }

      const receivedByUid =
        nextStatus === "RECEIVED" ? authed.uid : existingCase.receivedByUid;
      const receivedAt =
        nextStatus === "RECEIVED" ? now : existingCase.receivedAt;
      const reviewedByUid =
        nextStatus === "REVIEWED" ? authed.uid : existingCase.reviewedByUid;
      const reviewedAt =
        nextStatus === "REVIEWED" ? now : existingCase.reviewedAt;

      tx.update(casePath, {
        status: nextStatus,
        internalNotes: updatedNotes,
        parentResponse,
        receivedByUid,
        receivedAt,
        reviewedByUid,
        reviewedAt,
        updatedAt: now,
      });

      const auditEventId = this.store.generateId("evt");
      const action =
        nextStatus === "RECEIVED"
          ? "STATUS_PENDING_TO_RECEIVED"
          : "STATUS_RECEIVED_TO_REVIEWED";

      const auditEvent: AuditEventDoc = {
        eventId: auditEventId,
        caseId,
        pharmacyId,
        action,
        fromStatus,
        toStatus: nextStatus,
        actorUid: authed.uid,
        actorRole: "pharmacist",
        attestationConfirmed: req.pharmacistAttestation === true,
        internalNoteRecorded: trimmedInternalNote.length > 0,
        parentReplyPublished: nextStatus === "REVIEWED",
        createdAt: now,
      };

      tx.create<AuditEventDoc>(
        `pharmacies/${pharmacyId}/cases/${caseId}/auditEvents/${auditEventId}`,
        auditEvent,
      );

      return {
        ...existingCase,
        status: nextStatus,
        internalNotes: updatedNotes,
        parentResponse,
        receivedByUid,
        receivedAt,
        reviewedByUid,
        reviewedAt,
        updatedAt: now,
      };
    });

    return toPharmacyInboxCaseView(updatedCase);
  }
}

async function verifyPharmacistQuickClaims(
  caller: CallerContext | null | undefined,
  pharmacyId: string,
): Promise<CallerContext> {
  const { assertPharmacistClaims } = await import("./auth");
  return assertPharmacistClaims(caller, pharmacyId);
}
