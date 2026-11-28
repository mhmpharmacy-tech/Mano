"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.updateCareCard = exports.cleanupExpiredConsultationState = exports.revokePharmacyStaff = exports.onboardPharmacyStaff = exports.upsertPharmacyRegistry = exports.reviewConsultationCase = exports.getPharmacyInbox = exports.listParentConsultations = exports.getParentConsultation = exports.submitConsultationCase = exports.issueConsultationTriagePass = exports.listAcceptingPharmacies = exports.processAccountDeletionJobs = exports.getAccountDeletionStatus = exports.requestAccountDeletion = exports.getAccountDeletionPolicy = void 0;
const care_1 = require("./care");
const https_1 = require("firebase-functions/v2/https");
const scheduler_1 = require("firebase-functions/v2/scheduler");
const admin_1 = require("./admin");
const cases_1 = require("./cases");
const cleanup_1 = require("./cleanup");
const config_1 = require("./config");
const firestoreStore_1 = require("./firestoreStore");
const accountDeletion_1 = require("./accountDeletion");
const firebaseAccountDeletion_1 = require("./firebaseAccountDeletion");
const auth_1 = require("./auth");
// Clinical endpoints remain unavailable until independent release gates are satisfied.
const ENFORCE_APP_CHECK = true;
function requireClinicalRelease() {
    if (process.env.MANO_CLINICAL_SERVICE_ENABLED !== "true") {
        throw new https_1.HttpsError("failed-precondition", "Clinical service is not enabled.");
    }
}
function extractCaller(request) {
    if (!request.auth) {
        return null;
    }
    return {
        uid: request.auth.uid,
        token: (request.auth.token ?? {}),
    };
}
function handleError(err) {
    if (err instanceof https_1.HttpsError) {
        throw err;
    }
    if (err instanceof config_1.AppError) {
        throw new https_1.HttpsError(err.code, err.message, err.details);
    }
    throw new https_1.HttpsError("internal", "Unexpected server error.");
}
function createServices() {
    const store = new firestoreStore_1.FirestoreBackendStore((0, firestoreStore_1.getNamedFirestore)());
    const authService = new firestoreStore_1.FirebaseAdminAuthService();
    return {
        caseService: new cases_1.ConsultationCaseService(store),
        adminService: new admin_1.PharmacyAdminService(store, authService),
        cleanupService: new cleanup_1.OperationalCleanupService(store),
    };
}
const CALLABLE_OPTIONS = {
    region: config_1.FUNCTIONS_REGION,
    enforceAppCheck: ENFORCE_APP_CHECK,
    consumeAppCheckToken: false,
};
function createAccountDeletionService() {
    return new accountDeletion_1.AccountDeletionService(new firebaseAccountDeletion_1.FirebaseAccountDeletionBackend((0, firestoreStore_1.getNamedFirestore)()), (0, accountDeletion_1.resolveAccountDeletionPolicy)(process.env));
}
// Erasure has its own approved-policy gate. Disabling clinical AI must never
// prevent a user from checking or requesting a previously approved deletion.
exports.getAccountDeletionPolicy = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        return createAccountDeletionService().getPolicy(extractCaller(request));
    }
    catch (err) {
        return handleError(err);
    }
});
exports.requestAccountDeletion = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        return await createAccountDeletionService().request(extractCaller(request), request.data);
    }
    catch (err) {
        return handleError(err);
    }
});
exports.getAccountDeletionStatus = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        return await createAccountDeletionService().getStatus(extractCaller(request), request.data);
    }
    catch (err) {
        return handleError(err);
    }
});
exports.processAccountDeletionJobs = (0, scheduler_1.onSchedule)({
    region: config_1.FUNCTIONS_REGION, schedule: "every 1 minutes", timeoutSeconds: 540, maxInstances: 1,
}, async () => {
    if (!(0, accountDeletion_1.resolveAccountDeletionPolicy)(process.env))
        return;
    const service = createAccountDeletionService();
    await service.processPendingJobs(5);
    await service.purgeExpiredClinicalRetention(5);
});
exports.listAcceptingPharmacies = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.listAcceptingPharmacies((0, auth_1.assertVerifiedCaregiver)(extractCaller(request)));
    }
    catch (err) {
        return handleError(err);
    }
});
exports.issueConsultationTriagePass = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.issueConsultationTriagePass((0, auth_1.assertVerifiedCaregiver)(extractCaller(request)), request.data ?? {});
    }
    catch (err) {
        return handleError(err);
    }
});
exports.submitConsultationCase = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.submitConsultationCase((0, auth_1.assertVerifiedCaregiver)(extractCaller(request)), request.data ?? {});
    }
    catch (err) {
        return handleError(err);
    }
});
exports.getParentConsultation = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.getParentConsultation((0, auth_1.assertVerifiedCaregiver)(extractCaller(request)), request.data ?? {});
    }
    catch (err) {
        return handleError(err);
    }
});
exports.listParentConsultations = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.listParentConsultations((0, auth_1.assertVerifiedCaregiver)(extractCaller(request)), request.data ?? {});
    }
    catch (err) {
        return handleError(err);
    }
});
exports.getPharmacyInbox = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.getPharmacyInbox(extractCaller(request), request.data ?? {});
    }
    catch (err) {
        return handleError(err);
    }
});
exports.reviewConsultationCase = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.reviewConsultationCase(extractCaller(request), request.data ?? {});
    }
    catch (err) {
        return handleError(err);
    }
});
exports.upsertPharmacyRegistry = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        const { adminService } = createServices();
        return await adminService.upsertPharmacyRegistry(extractCaller(request), request.data);
    }
    catch (err) {
        return handleError(err);
    }
});
exports.onboardPharmacyStaff = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        const { adminService } = createServices();
        return await adminService.onboardPharmacist(extractCaller(request), request.data);
    }
    catch (err) {
        return handleError(err);
    }
});
exports.revokePharmacyStaff = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        const { adminService } = createServices();
        return await adminService.revokePharmacist(extractCaller(request), request.data);
    }
    catch (err) {
        return handleError(err);
    }
});
exports.cleanupExpiredConsultationState = (0, scheduler_1.onSchedule)({
    region: config_1.FUNCTIONS_REGION,
    schedule: "every 6 hours",
}, async () => {
    const { cleanupService } = createServices();
    await cleanupService.purgeExpiredOperationalDocs(250);
});
exports.updateCareCard = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        return await (0, care_1.updateCare)(new firestoreStore_1.FirestoreBackendStore((0, firestoreStore_1.getNamedFirestore)()), extractCaller(request), request.data ?? {});
    }
    catch (err) {
        return handleError(err);
    }
});
//# sourceMappingURL=index.js.map