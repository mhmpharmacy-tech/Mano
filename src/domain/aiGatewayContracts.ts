import type {
  DocumentedQaResponse,
  GeminiSyrupScanPayload,
  NutritionPlan,
  SafetyChecklistReport,
} from './models.ts';

export type AiResponsibility =
  | 'SIMPLIFY_EDUCATIONAL_CONTENT'
  | 'SUMMARIZE_USER_INFO'
  | 'EXTRACT_MEDICATION_LABEL'
  | 'PREPARE_CLINICIAN_DRAFT'
  | 'EXPLAIN_ENGINE_OUTPUT';

export type AiProhibitedAction =
  | 'INDEPENDENT_DIAGNOSIS'
  | 'INDEPENDENT_PRESCRIBING_OR_DOSE'
  | 'OVERRIDE_URGENT_ESCALATION'
  | 'FABRICATE_REFERENCES'
  | 'AUTO_MODIFY_TREATMENT_PLAN'
  | 'PROMPT_INJECTION'
  | 'UNGROUNDED_CLINICAL_CLAIM'
  | 'MALFORMED_OUTPUT'
  | 'CONTRADICTORY_OUTPUT'
  | 'UNTRUSTED_DOCUMENT';

export type AiVerificationState = 'ASSISTANT_DRAFT' | 'REQUIRE_REVIEW' | 'UNKNOWN';

export type AiGatewayOutcome =
  | 'ALLOWED'
  | 'FALLBACK_OFFLINE'
  | 'REJECTED_POLICY'
  | 'REJECTED_VALIDATION'
  | 'RATE_LIMITED'
  | 'BUDGET_EXCEEDED'
  | 'TIMEOUT'
  | 'CANCELLED'
  | 'UPSTREAM_ERROR';

export interface GatewayResponseMeta {
  auditId: string;
  responsibility: AiResponsibility;
  verificationState: AiVerificationState;
  outcome: AiGatewayOutcome;
  modelUsed: string;
  fallbackUsed: boolean;
  attempts: number;
  latencyMs: number;
  warningsFa: string[];
  policyViolations: AiProhibitedAction[];
  requiresClinicianReview: boolean;
  disclaimerFa: string;
}

export interface GatewayEnvelope<T> {
  data: T;
  meta: GatewayResponseMeta;
}

export interface VisitDraftStructuredPayload {
  titleFa: string;
  childAndReasonSummaryFa: string;
  temperatureTrend24hFa: string;
  medicationHistory24hFa: string;
  observedSymptomsAndFlagsFa: string;
  questionsForClinicianFa: string[];
  missingInformationFlagsFa: string[];
  verificationState: AiVerificationState;
  formattedDraftFa: string;
}

export type RestrictedSyrupScanPayload = GeminiSyrupScanPayload & {
  verificationState: AiVerificationState;
  missingFields: string[];
  requiresManualConfirmation: boolean;
};

export type RestrictedNutritionPayload = Pick<
  NutritionPlan,
  'planTitleFa' | 'overviewFa' | 'growthNutritionFocusFa' | 'hydrationTipFa' | 'meals'
> & {
  verificationState: AiVerificationState;
  citedSourceIds: string[];
  citedSourcesFa: string[];
  safetyDisclaimerFa: string;
};

export type RestrictedSafetyChecklistPayload = SafetyChecklistReport & {
  verificationState: AiVerificationState;
  preservedUrgencyLevel: 'URGENT_EMERGENCY' | 'NEEDS_TIMELY_REVIEW' | 'GENERAL_EDUCATIONAL';
  citedSourceIds: string[];
};

export type RestrictedDocumentedQaPayload = DocumentedQaResponse & {
  verificationState: AiVerificationState;
  citedSourceIds: string[];
  matchedFaqIds: string[];
};

export interface GatewayHealthStatus {
  ok: boolean;
  hasGeminiKey: boolean;
  model: string;
  allowedModels: string[];
  gatewayMode: 'RESTRICTED_AUDITABLE';
  allowedResponsibilities: AiResponsibility[];
  prohibitedResponsibilities: AiProhibitedAction[];
  budgetRemainingRequests: number;
  offlineEnginesIndependent: true;
}

export interface GatewayAuditLogEntry {
  auditId: string;
  timestampIso: string;
  endpoint: string;
  responsibility: AiResponsibility;
  model: string;
  outcome: AiGatewayOutcome;
  verificationState: AiVerificationState;
  fallbackUsed: boolean;
  attempts: number;
  latencyMs: number;
  clientKeyHash: string;
  policyViolations: AiProhibitedAction[];
  validationNotes: string[];
}
