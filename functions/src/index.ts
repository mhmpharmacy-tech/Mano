import { updateCare } from "./care";
import { CallableRequest, HttpsError, onCall } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { PharmacyAdminService } from "./admin";
import {
  ConsultationCaseService,
  GetParentConsultationRequest,
  GetPharmacyInboxRequest,
  IssueTriagePassRequest,
  ListParentConsultationsRequest,
  ReviewConsultationCaseRequest,
  SubmitConsultationCaseRequest,
} from "./cases";
import { OperationalCleanupService } from "./cleanup";
import { AppError, FUNCTIONS_REGION } from "./config";
import {
  FirebaseAdminAuthService,
  FirestoreBackendStore,
  getNamedFirestore,
} from "./firestoreStore";
import { CallerContext, StaffClaims } from "./types";

// Clinical endpoints remain unavailable until independent release gates are satisfied.
const ENFORCE_APP_CHECK = true;
function requireClinicalRelease(): void {
  if (process.env.MANO_CLINICAL_SERVICE_ENABLED !== "true") {
    throw new HttpsError("failed-precondition", "Clinical service is not enabled.");
  }
}

function extractCaller(request: CallableRequest<unknown>): CallerContext | null {
  if (!request.auth) {
    return null;
  }
  return {
    uid: request.auth.uid,
    token: (request.auth.token ?? {}) as unknown as StaffClaims,
  };
}

function handleError(err: unknown): never {
  if (err instanceof HttpsError) {
    throw err;
  }
  if (err instanceof AppError) {
    throw new HttpsError(err.code, err.message, err.details);
  }
  throw new HttpsError("internal", "Unexpected server error.");
}

function createServices() {
  const store = new FirestoreBackendStore(getNamedFirestore());
  const authService = new FirebaseAdminAuthService();
  return {
    caseService: new ConsultationCaseService(store),
    adminService: new PharmacyAdminService(store, authService),
    cleanupService: new OperationalCleanupService(store),
  };
}

const CALLABLE_OPTIONS = {
  region: FUNCTIONS_REGION,
  enforceAppCheck: ENFORCE_APP_CHECK,
  consumeAppCheckToken: false,
} as const;

export const listAcceptingPharmacies = onCall(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      requireClinicalRelease();
      const { caseService } = createServices();
      return await caseService.listAcceptingPharmacies(extractCaller(request));
    } catch (err) {
      return handleError(err);
    }
  },
);

export const issueConsultationTriagePass = onCall<IssueTriagePassRequest>(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      requireClinicalRelease();
      const { caseService } = createServices();
      return await caseService.issueConsultationTriagePass(
        extractCaller(request),
        request.data ?? ({} as IssueTriagePassRequest),
      );
    } catch (err) {
      return handleError(err);
    }
  },
);

export const submitConsultationCase = onCall<SubmitConsultationCaseRequest>(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      requireClinicalRelease();
      const { caseService } = createServices();
      return await caseService.submitConsultationCase(
        extractCaller(request),
        request.data ?? ({} as SubmitConsultationCaseRequest),
      );
    } catch (err) {
      return handleError(err);
    }
  },
);

export const getParentConsultation = onCall<GetParentConsultationRequest>(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      requireClinicalRelease();
      const { caseService } = createServices();
      return await caseService.getParentConsultation(
        extractCaller(request),
        request.data ?? ({} as GetParentConsultationRequest),
      );
    } catch (err) {
      return handleError(err);
    }
  },
);

export const listParentConsultations = onCall<ListParentConsultationsRequest>(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      requireClinicalRelease();
      const { caseService } = createServices();
      return await caseService.listParentConsultations(
        extractCaller(request),
        request.data ?? {},
      );
    } catch (err) {
      return handleError(err);
    }
  },
);

export const getPharmacyInbox = onCall<GetPharmacyInboxRequest>(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      requireClinicalRelease();
      const { caseService } = createServices();
      return await caseService.getPharmacyInbox(
        extractCaller(request),
        request.data ?? ({} as GetPharmacyInboxRequest),
      );
    } catch (err) {
      return handleError(err);
    }
  },
);

export const reviewConsultationCase = onCall<ReviewConsultationCaseRequest>(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      requireClinicalRelease();
      const { caseService } = createServices();
      return await caseService.reviewConsultationCase(
        extractCaller(request),
        request.data ?? ({} as ReviewConsultationCaseRequest),
      );
    } catch (err) {
      return handleError(err);
    }
  },
);

export const upsertPharmacyRegistry = onCall(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      const { adminService } = createServices();
      return await adminService.upsertPharmacyRegistry(
        extractCaller(request),
        request.data,
      );
    } catch (err) {
      return handleError(err);
    }
  },
);

export const onboardPharmacyStaff = onCall(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      const { adminService } = createServices();
      return await adminService.onboardPharmacist(
        extractCaller(request),
        request.data,
      );
    } catch (err) {
      return handleError(err);
    }
  },
);

export const revokePharmacyStaff = onCall(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      const { adminService } = createServices();
      return await adminService.revokePharmacist(
        extractCaller(request),
        request.data,
      );
    } catch (err) {
      return handleError(err);
    }
  },
);

export const cleanupExpiredConsultationState = onSchedule(
  {
    region: FUNCTIONS_REGION,
    schedule: "every 6 hours",
  },
  async () => {
    const { cleanupService } = createServices();
    await cleanupService.purgeExpiredOperationalDocs(250);
  },
);

export const updateCareCard = onCall(CALLABLE_OPTIONS, async request => {
  try {
    requireClinicalRelease();
    return await updateCare(new FirestoreBackendStore(getNamedFirestore()), extractCaller(request), request.data ?? {});
  } catch (err) {
    return handleError(err);
  }
});
