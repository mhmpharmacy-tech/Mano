"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.ConsultationCaseService = void 0;
exports.toParentConsultationView = toParentConsultationView;
exports.toPharmacyInboxCaseView = toPharmacyInboxCaseView;
const auth_1 = require("./auth");
const config_1 = require("./config");
const rateLimit_1 = require("./rateLimit");
const triage_1 = require("./triage");
function toParentConsultationView(doc) {
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
function toPharmacyInboxCaseView(doc) {
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
class ConsultationCaseService {
    store;
    constructor(store) {
        this.store = store;
    }
    async listAcceptingPharmacies(caller) {
        (0, auth_1.assertAuthenticated)(caller);
        const pharmacies = await this.store.queryCollection("pharmacies", [
            { field: "active", op: "==", value: true },
            { field: "acceptingCases", op: "==", value: true },
        ], "updatedAt", "desc", 20);
        return pharmacies
            .filter((p) => p.active === true && p.acceptingCases === true)
            .map((p) => ({
            pharmacyId: p.pharmacyId,
            displayNameFa: p.displayNameFa,
            acceptingCases: p.acceptingCases,
        }));
    }
    async issueConsultationTriagePass(caller, req) {
        const authed = (0, auth_1.assertAuthenticated)(caller);
        await (0, rateLimit_1.consumeRateLimit)(this.store, authed.uid, "triage_pass", config_1.RATE_LIMITS.triagePass.maxRequests, config_1.RATE_LIMITS.triagePass.windowMs);
        const decision = (0, triage_1.evaluateServerTriage)(req);
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
        const expiresAt = now + config_1.TRIAGE_PASS_TTL_MS;
        const passDoc = {
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
            tx.create(`triagePasses/${passId}`, passDoc);
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
    async submitConsultationCase(caller, req) {
        const authed = (0, auth_1.assertAuthenticated)(caller);
        const pharmacyId = (0, auth_1.assertValidId)(req.pharmacyId, "pharmacyId");
        const triagePassId = (0, auth_1.assertValidId)(req.triagePassId, "triagePassId");
        if (!config_1.VALID_CATEGORIES.includes(req.categoryName)) {
            throw new config_1.AppError("invalid-argument", "Invalid categoryName.");
        }
        const categoryName = req.categoryName;
        if (!config_1.VALID_AGE_BANDS.includes(req.ageBandId)) {
            throw new config_1.AppError("invalid-argument", "Invalid ageBandId.");
        }
        const ageBandId = req.ageBandId;
        const flowCandidate = req.symptomFlowId ?? "general";
        if (!config_1.VALID_SYMPTOM_FLOWS.includes(flowCandidate)) {
            throw new config_1.AppError("invalid-argument", "Invalid symptomFlowId.");
        }
        const symptomFlowId = flowCandidate;
        if (req.hasUrgentFlag !== false) {
            throw new config_1.AppError("failed-precondition", "Consultation intake rejects urgent requests; direct caregiver to emergency care.");
        }
        if (req.consentAccepted !== true || req.consentVersion !== config_1.CONSENT_VERSION) {
            throw new config_1.AppError("failed-precondition", `Explicit consentAccepted=true with consentVersion=${config_1.CONSENT_VERSION} is required.`);
        }
        if (typeof req.questionSummaryFa !== "string") {
            throw new config_1.AppError("invalid-argument", "questionSummaryFa is required.");
        }
        const questionSummaryFa = req.questionSummaryFa.trim();
        if (questionSummaryFa.length < config_1.FIELD_LIMITS.questionMinLength ||
            questionSummaryFa.length > config_1.FIELD_LIMITS.questionMaxLength) {
            throw new config_1.AppError("invalid-argument", `questionSummaryFa must be between ${config_1.FIELD_LIMITS.questionMinLength} and ${config_1.FIELD_LIMITS.questionMaxLength} characters.`);
        }
        // Re-evaluate submitted question text so unsafe keywords cannot bypass triage pass issuance
        const textTriageCheck = (0, triage_1.evaluateServerTriage)({
            ageBandId,
            symptomFlowId,
            questionSummaryFa,
            hasUrgentFlag: req.hasUrgentFlag,
        });
        if (textTriageCheck.outcome !== "NONURGENT") {
            throw new config_1.AppError("failed-precondition", "Submitted question contains urgent safety signals and cannot enter non-urgent pharmacist intake.", { ruleId: textTriageCheck.ruleId });
        }
        await (0, rateLimit_1.consumeRateLimit)(this.store, authed.uid, "case_submit", config_1.RATE_LIMITS.caseSubmit.maxRequests, config_1.RATE_LIMITS.caseSubmit.windowMs);
        const caseId = this.store.generateId("case");
        const createdCase = await this.store.runTransaction(async (tx) => {
            const now = this.store.nowMs();
            const pharmacy = await tx.get(`pharmacies/${pharmacyId}`);
            (0, auth_1.assertActivePharmacy)(pharmacy, true);
            const passRef = `triagePasses/${triagePassId}`;
            const passDoc = await tx.get(passRef);
            if (!passDoc) {
                throw new config_1.AppError("permission-denied", "A valid server-issued NONURGENT triage pass is required.");
            }
            if (passDoc.uid !== authed.uid) {
                throw new config_1.AppError("permission-denied", "Triage pass does not belong to the authenticated parent.");
            }
            if (passDoc.outcome !== "NONURGENT") {
                throw new config_1.AppError("failed-precondition", "Triage pass outcome is not NONURGENT.");
            }
            if (passDoc.consumed === true || passDoc.consumedByCaseId !== null) {
                throw new config_1.AppError("failed-precondition", "Triage pass has already been consumed.");
            }
            if (passDoc.expiresAt <= now) {
                throw new config_1.AppError("failed-precondition", "Triage pass has expired; please re-run triage evaluation.");
            }
            if (passDoc.ageBandId !== ageBandId) {
                throw new config_1.AppError("invalid-argument", "Triage pass ageBandId does not match submitted consultation.");
            }
            const newCase = {
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
                consentVersion: config_1.CONSENT_VERSION,
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
            const auditEvent = {
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
            tx.create(`pharmacies/${pharmacyId}/cases/${caseId}`, newCase);
            tx.create(`pharmacies/${pharmacyId}/cases/${caseId}/auditEvents/${auditEventId}`, auditEvent);
            return newCase;
        });
        return toParentConsultationView(createdCase);
    }
    async getParentConsultation(caller, req) {
        const authed = (0, auth_1.assertAuthenticated)(caller);
        const pharmacyId = (0, auth_1.assertValidId)(req.pharmacyId, "pharmacyId");
        const caseId = (0, auth_1.assertValidId)(req.caseId, "caseId");
        const caseDoc = await this.store.get(`pharmacies/${pharmacyId}/cases/${caseId}`);
        if (!caseDoc) {
            throw new config_1.AppError("not-found", "Consultation case was not found.");
        }
        if (caseDoc.parentUid !== authed.uid) {
            throw new config_1.AppError("permission-denied", "Parents may only read their own consultation cases.");
        }
        return toParentConsultationView(caseDoc);
    }
    async listParentConsultations(caller, req = {}) {
        const authed = (0, auth_1.assertAuthenticated)(caller);
        const limit = Math.min(Math.max(Number(req.limit ?? 25), 1), 50);
        const cases = await this.store.queryCollectionGroup("cases", [{ field: "parentUid", op: "==", value: authed.uid }], "createdAt", "desc", limit);
        return cases
            .filter((doc) => doc.parentUid === authed.uid)
            .map(toParentConsultationView);
    }
    async getPharmacyInbox(caller, req) {
        const pharmacyId = (0, auth_1.assertValidId)(req.pharmacyId, "pharmacyId");
        // Verifies custom claims AND reads live directory record on every request
        const { caller: authed } = await (0, auth_1.verifyLivePharmacistAccess)(this.store, caller, pharmacyId);
        await (0, rateLimit_1.consumeRateLimit)(this.store, authed.uid, "inbox_read", config_1.RATE_LIMITS.inboxRead.maxRequests, config_1.RATE_LIMITS.inboxRead.windowMs);
        const filters = [];
        if (req.statusFilter !== undefined && req.statusFilter !== null) {
            if (!config_1.VALID_CASE_STATUSES.includes(req.statusFilter)) {
                throw new config_1.AppError("invalid-argument", "Invalid statusFilter.");
            }
            filters.push({
                field: "status",
                op: "==",
                value: req.statusFilter,
            });
        }
        const limit = Math.min(Math.max(Number(req.limit ?? 30), 1), 100);
        const cases = await this.store.queryCollection(`pharmacies/${pharmacyId}/cases`, filters, "createdAt", "desc", limit);
        return cases.map(toPharmacyInboxCaseView);
    }
    async reviewConsultationCase(caller, req) {
        const pharmacyId = (0, auth_1.assertValidId)(req.pharmacyId, "pharmacyId");
        const caseId = (0, auth_1.assertValidId)(req.caseId, "caseId");
        if (!config_1.VALID_CASE_STATUSES.includes(req.nextStatus)) {
            throw new config_1.AppError("invalid-argument", "Invalid nextStatus.");
        }
        const nextStatus = req.nextStatus;
        if (nextStatus === "PENDING") {
            throw new config_1.AppError("failed-precondition", "Cannot transition a case back to PENDING.");
        }
        const trimmedInternalNote = typeof req.internalNote === "string" ? req.internalNote.trim() : "";
        if (trimmedInternalNote.length > config_1.FIELD_LIMITS.internalNoteMaxLength) {
            throw new config_1.AppError("invalid-argument", `internalNote cannot exceed ${config_1.FIELD_LIMITS.internalNoteMaxLength} characters.`);
        }
        const trimmedParentReply = typeof req.parentReply === "string" ? req.parentReply.trim() : "";
        if (trimmedParentReply.length > config_1.FIELD_LIMITS.parentReplyMaxLength) {
            throw new config_1.AppError("invalid-argument", `parentReply cannot exceed ${config_1.FIELD_LIMITS.parentReplyMaxLength} characters.`);
        }
        // Fast-fail claims check prior to transaction
        const authedPreliminary = await verifyPharmacistQuickClaims(caller, pharmacyId);
        await (0, rateLimit_1.consumeRateLimit)(this.store, authedPreliminary.uid, "case_review", config_1.RATE_LIMITS.caseReview.maxRequests, config_1.RATE_LIMITS.caseReview.windowMs);
        const updatedCase = await this.store.runTransaction(async (tx) => {
            // MANDATORY: Re-read active credential-verified staff directory inside transaction
            // before committing any state transition!
            const { caller: authed } = await (0, auth_1.verifyPharmacistAccessInTransaction)(tx, caller, pharmacyId);
            const casePath = `pharmacies/${pharmacyId}/cases/${caseId}`;
            const existingCase = await tx.get(casePath);
            if (!existingCase) {
                throw new config_1.AppError("not-found", "Consultation case was not found.");
            }
            if (existingCase.pharmacyId !== pharmacyId) {
                throw new config_1.AppError("permission-denied", "Tenant mismatch on consultation case.");
            }
            const fromStatus = existingCase.status;
            if (fromStatus === "REVIEWED") {
                throw new config_1.AppError("failed-precondition", "Consultation case is already REVIEWED and locked.");
            }
            const validTransition = (fromStatus === "PENDING" && nextStatus === "RECEIVED") ||
                (fromStatus === "RECEIVED" && nextStatus === "REVIEWED");
            if (!validTransition) {
                throw new config_1.AppError("failed-precondition", `Invalid state transition from ${fromStatus} to ${nextStatus}; sequential PENDING -> RECEIVED -> REVIEWED is required.`);
            }
            if (nextStatus === "RECEIVED" && trimmedParentReply.length > 0) {
                throw new config_1.AppError("invalid-argument", "Internal receipt acknowledgment (RECEIVED) cannot publish a parent-facing clinical reply without REVIEWED attestation.");
            }
            if (nextStatus === "REVIEWED") {
                if (req.pharmacistAttestation !== true) {
                    throw new config_1.AppError("failed-precondition", "No clinical response may be posted without explicit pharmacistAttestation=true.");
                }
                if (trimmedParentReply.length < config_1.FIELD_LIMITS.parentReplyMinLength) {
                    throw new config_1.AppError("invalid-argument", `parentReply must be at least ${config_1.FIELD_LIMITS.parentReplyMinLength} characters when marking REVIEWED.`);
                }
            }
            const now = this.store.nowMs();
            const updatedNotes = [...existingCase.internalNotes];
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
            let parentResponse = existingCase.parentResponse;
            if (nextStatus === "REVIEWED") {
                parentResponse = {
                    replyTextFa: trimmedParentReply,
                    respondedByRole: "pharmacist",
                    respondedAt: now,
                    attestationConfirmed: true,
                };
            }
            const receivedByUid = nextStatus === "RECEIVED" ? authed.uid : existingCase.receivedByUid;
            const receivedAt = nextStatus === "RECEIVED" ? now : existingCase.receivedAt;
            const reviewedByUid = nextStatus === "REVIEWED" ? authed.uid : existingCase.reviewedByUid;
            const reviewedAt = nextStatus === "REVIEWED" ? now : existingCase.reviewedAt;
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
            const action = nextStatus === "RECEIVED"
                ? "STATUS_PENDING_TO_RECEIVED"
                : "STATUS_RECEIVED_TO_REVIEWED";
            const auditEvent = {
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
            tx.create(`pharmacies/${pharmacyId}/cases/${caseId}/auditEvents/${auditEventId}`, auditEvent);
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
exports.ConsultationCaseService = ConsultationCaseService;
async function verifyPharmacistQuickClaims(caller, pharmacyId) {
    const { assertPharmacistClaims } = await Promise.resolve().then(() => __importStar(require("./auth")));
    return assertPharmacistClaims(caller, pharmacyId);
}
//# sourceMappingURL=cases.js.map