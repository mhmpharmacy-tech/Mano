import {
  VALID_AGE_BANDS,
  VALID_CASE_STATUSES,
  VALID_CATEGORIES,
  VALID_SYMPTOM_FLOWS,
} from "./config";

export type AgeBandId = (typeof VALID_AGE_BANDS)[number];
export type ConsultationCategory = (typeof VALID_CATEGORIES)[number];
export type SymptomFlowId = (typeof VALID_SYMPTOM_FLOWS)[number];
export type ConsultationStatus = (typeof VALID_CASE_STATUSES)[number];
export type TriageOutcome = "URGENT_EMERGENCY" | "NONURGENT";
export type StaffStatus = "ACTIVE" | "SUSPENDED" | "REVOKED";
export type StaffRole = "pharmacist" | "admin";

export interface StaffClaims {
  manoStaffVerified?: boolean;
  manoStaffRole?: string | null;
  manoPharmacyId?: string | null;
  manoSystemAdmin?: boolean;
  [key: string]: unknown;
}

export interface CallerContext {
  uid: string;
  token: StaffClaims;
}

export interface PharmacyRegistryDoc {
  pharmacyId: string;
  displayNameFa: string;
  active: boolean;
  acceptingCases: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface StaffDirectoryDoc {
  uid: string;
  pharmacyId: string;
  role: "pharmacist";
  status: StaffStatus;
  credentialVerified: boolean;
  credentialReference: string;
  revokedAt: number | null;
  revocationReason: string | null;
  revokedByUid: string | null;
  onboardedByUid: string;
  createdAt: number;
  updatedAt: number;
}

export interface TriagePassDoc {
  passId: string;
  uid: string;
  outcome: "NONURGENT";
  ageBandId: AgeBandId;
  symptomFlowId: SymptomFlowId;
  ruleId: string;
  consumed: boolean;
  consumedAt: number | null;
  consumedByCaseId: string | null;
  expiresAt: number;
  createdAt: number;
}

export interface InternalStaffNote {
  noteId: string;
  authorUid: string;
  noteText: string;
  fromStatus: ConsultationStatus;
  toStatus: ConsultationStatus;
  createdAt: number;
}

export interface ParentClinicalResponse {
  replyTextFa: string;
  respondedByRole: "pharmacist";
  respondedAt: number;
  attestationConfirmed: true;
}

export interface ConsultationCaseDoc {
  careCard?: import("./care").CareCard;
  caseId: string;
  pharmacyId: string;
  parentUid: string;
  categoryName: ConsultationCategory;
  ageBandId: AgeBandId;
  symptomFlowId: SymptomFlowId;
  questionSummaryFa: string;
  hasUrgentFlag: false;
  triagePassId: string;
  triageOutcome: "NONURGENT";
  consentAccepted: true;
  consentVersion: "v1";
  status: ConsultationStatus;
  internalNotes: InternalStaffNote[];
  parentResponse: ParentClinicalResponse | null;
  receivedByUid: string | null;
  receivedAt: number | null;
  reviewedByUid: string | null;
  reviewedAt: number | null;
  createdAt: number;
  updatedAt: number;
}

export interface AuditEventDoc {
  eventId: string;
  caseId: string;
  pharmacyId: string;
  action:
    | "CASE_CREATED"
    | "STATUS_PENDING_TO_RECEIVED"
    | "STATUS_RECEIVED_TO_REVIEWED";
  fromStatus: ConsultationStatus | null;
  toStatus: ConsultationStatus;
  actorUid: string;
  actorRole: "parent" | "pharmacist";
  attestationConfirmed: boolean;
  triagePassId?: string;
  internalNoteRecorded: boolean;
  parentReplyPublished: boolean;
  createdAt: number;
}

export interface RateLimitDoc {
  bucketKey: string;
  uid: string;
  action: string;
  windowStartMs: number;
  count: number;
  updatedAt: number;
}

export interface ParentConsultationView {
  careCard?: import("./care").CareCard;
  caseId: string;
  pharmacyId: string;
  categoryName: ConsultationCategory;
  ageBandId: AgeBandId;
  symptomFlowId: SymptomFlowId;
  questionSummaryFa: string;
  triageOutcome: "NONURGENT";
  consentVersion: "v1";
  status: ConsultationStatus;
  parentResponse: ParentClinicalResponse | null;
  receivedAt: number | null;
  reviewedAt: number | null;
  createdAt: number;
  updatedAt: number;
}

export interface PharmacyInboxCaseView {
  careCard?: import("./care").CareCard;
  caseId: string;
  pharmacyId: string;
  categoryName: ConsultationCategory;
  ageBandId: AgeBandId;
  symptomFlowId: SymptomFlowId;
  questionSummaryFa: string;
  hasUrgentFlag: false;
  triageOutcome: "NONURGENT";
  consentVersion: "v1";
  status: ConsultationStatus;
  internalNotes: InternalStaffNote[];
  parentResponse: ParentClinicalResponse | null;
  receivedByUid: string | null;
  receivedAt: number | null;
  reviewedByUid: string | null;
  reviewedAt: number | null;
  createdAt: number;
  updatedAt: number;
}

export interface PublicPharmacyOption {
  pharmacyId: string;
  displayNameFa: string;
  acceptingCases: boolean;
}

export interface StoreTransaction {
  get<T>(path: string): Promise<T | null>;
  set<T extends object>(path: string, data: T): void;
  create<T extends object>(path: string, data: T): void;
  update(path: string, data: Record<string, unknown>): void;
  delete(path: string): void;
}

export interface QueryFilter {
  field: string;
  op: "==" | "<";
  value: unknown;
}

export interface BackendStore {
  nowMs(): number;
  generateId(prefix?: string): string;
  get<T>(path: string): Promise<T | null>;
  queryCollection<T>(
    collectionPath: string,
    filters: QueryFilter[],
    orderByField: string,
    direction: "asc" | "desc",
    limitCount: number,
  ): Promise<T[]>;
  queryCollectionGroup<T>(
    collectionId: string,
    filters: QueryFilter[],
    orderByField: string,
    direction: "asc" | "desc",
    limitCount: number,
  ): Promise<T[]>;
  deleteWhereLessThan(
    collectionPath: string,
    field: string,
    cutoffValue: number,
    limitCount: number,
  ): Promise<number>;
  runTransaction<R>(fn: (tx: StoreTransaction) => Promise<R>): Promise<R>;
}

export interface BackendAuthService {
  getUidByEmail?(email: string): Promise<string | null>;
  getCustomClaims(uid: string): Promise<StaffClaims>;
  setCustomClaims(uid: string, claims: StaffClaims): Promise<void>;
  revokeRefreshTokens(uid: string): Promise<void>;
}
