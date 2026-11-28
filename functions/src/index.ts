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
import { AccountDeletionService, resolveAccountDeletionPolicy } from "./accountDeletion";
import { FirebaseAccountDeletionBackend } from "./firebaseAccountDeletion";
import { assertVerifiedCaregiver } from "./auth";

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

function createAccountDeletionService(): AccountDeletionService {
  return new AccountDeletionService(
    new FirebaseAccountDeletionBackend(getNamedFirestore()),
    resolveAccountDeletionPolicy(process.env),
  );
}

// Erasure has its own approved-policy gate. Disabling clinical AI must never
// prevent a user from checking or requesting a previously approved deletion.
export const getAccountDeletionPolicy = onCall(CALLABLE_OPTIONS, async request => {
  try { return createAccountDeletionService().getPolicy(extractCaller(request)); }
  catch (err) { return handleError(err); }
});

export const requestAccountDeletion = onCall(CALLABLE_OPTIONS, async request => {
  try { return await createAccountDeletionService().request(extractCaller(request), request.data); }
  catch (err) { return handleError(err); }
});

export const getAccountDeletionStatus = onCall(CALLABLE_OPTIONS, async request => {
  try { return await createAccountDeletionService().getStatus(extractCaller(request), request.data); }
  catch (err) { return handleError(err); }
});

export const processAccountDeletionJobs = onSchedule({
  region: FUNCTIONS_REGION, schedule: "every 1 minutes", timeoutSeconds: 540, maxInstances: 1,
}, async () => {
  if (!resolveAccountDeletionPolicy(process.env)) return;
  const service = createAccountDeletionService();
  await service.processPendingJobs(5);
  await service.purgeExpiredClinicalRetention(5);
});

export const listAcceptingPharmacies = onCall(
  CALLABLE_OPTIONS,
  async (request) => {
    try {
      requireClinicalRelease();
      const { caseService } = createServices();
      return await caseService.listAcceptingPharmacies(assertVerifiedCaregiver(extractCaller(request)));
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
        assertVerifiedCaregiver(extractCaller(request)),
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
        assertVerifiedCaregiver(extractCaller(request)),
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
        assertVerifiedCaregiver(extractCaller(request)),
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
        assertVerifiedCaregiver(extractCaller(request)),
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
