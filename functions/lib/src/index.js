"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.cleanupExpiredConsultationState = exports.revokePharmacyStaff = exports.onboardPharmacyStaff = exports.upsertPharmacyRegistry = exports.reviewConsultationCase = exports.getPharmacyInbox = exports.listParentConsultations = exports.getParentConsultation = exports.submitConsultationCase = exports.issueConsultationTriagePass = exports.listAcceptingPharmacies = void 0;
const https_1 = require("firebase-functions/v2/https");
const scheduler_1 = require("firebase-functions/v2/scheduler");
const admin_1 = require("./admin");
const cases_1 = require("./cases");
const cleanup_1 = require("./cleanup");
const config_1 = require("./config");
const firestoreStore_1 = require("./firestoreStore");
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
exports.listAcceptingPharmacies = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.listAcceptingPharmacies(extractCaller(request));
    }
    catch (err) {
        return handleError(err);
    }
});
exports.issueConsultationTriagePass = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.issueConsultationTriagePass(extractCaller(request), request.data ?? {});
    }
    catch (err) {
        return handleError(err);
    }
});
exports.submitConsultationCase = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.submitConsultationCase(extractCaller(request), request.data ?? {});
    }
    catch (err) {
        return handleError(err);
    }
});
exports.getParentConsultation = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.getParentConsultation(extractCaller(request), request.data ?? {});
    }
    catch (err) {
        return handleError(err);
    }
});
exports.listParentConsultations = (0, https_1.onCall)(CALLABLE_OPTIONS, async (request) => {
    try {
        requireClinicalRelease();
        const { caseService } = createServices();
        return await caseService.listParentConsultations(extractCaller(request), request.data ?? {});
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
//# sourceMappingURL=index.js.map