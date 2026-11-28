"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const strict_1 = __importDefault(require("node:assert/strict"));
const node_crypto_1 = require("node:crypto");
const node_test_1 = __importDefault(require("node:test"));
const accountDeletion_1 = require("../src/accountDeletion");
const cases_1 = require("../src/cases");
const care_1 = require("../src/care");
const admin_1 = require("../src/admin");
const onboardPharmacist_1 = require("../src/scripts/onboardPharmacist");
const config_1 = require("../src/config");
const inMemoryStore_1 = require("./inMemoryStore");
const DAY = 86_400_000;
const POLICY = {
    version: "test-approved-v1", clinicalRetentionDays: 10, backupRetentionDays: 40,
    storageMode: "none", storageBucket: null, storageWritersStopped: false,
};
const ENV = {
    MANO_ACCOUNT_DELETION_ENABLED: "true", MANO_ACCOUNT_DELETION_POLICY_APPROVED: "true",
    MANO_ACCOUNT_DELETION_POLICY_VERSION: POLICY.version, MANO_ACCOUNT_DELETION_CLINICAL_RETENTION_DAYS: "10",
    MANO_ACCOUNT_DELETION_BACKUP_RETENTION_DAYS: "40", MANO_ACCOUNT_DELETION_STORAGE_MODE: "none",
};
class FakeDeletionBackend {
    now = 1_800_000_000_000;
    jobs = new Map();
    locks = new Map();
    staff = new Set();
    documents = new Set();
    clinical = new Map();
    disabled = new Set();
    deletedAuth = new Set();
    objects = new Set();
    events = [];
    failUserPage = false;
    nowMs() { return this.now; }
    async isStaffAccount(uid) { return this.staff.has(uid); }
    async createOrGetJob(job) {
        const existing = this.locks.get(job.uid);
        if (existing)
            return structuredClone(this.jobs.get(existing));
        this.jobs.set(job.jobId, structuredClone(job));
        this.locks.set(job.uid, job.jobId);
        return structuredClone(job);
    }
    async getJob(id) { return structuredClone(this.jobs.get(id) ?? null); }
    async listPendingJobs(limit) {
        return [...this.jobs.values()].filter(j => j.nextAttemptAtMs <= this.now).sort((a, b) => a.nextAttemptAtMs - b.nextAttemptAtMs).slice(0, limit).map(j => j.jobId);
    }
    async claimJob(id, owner, until) {
        const job = this.jobs.get(id);
        if (!job || job.leaseUntilMs > this.now)
            return null;
        job.leaseOwner = owner;
        job.leaseUntilMs = until;
        return structuredClone(job);
    }
    async saveClaimedJob(job, owner) {
        strict_1.default.equal(this.jobs.get(job.jobId)?.leaseOwner, owner);
        this.jobs.set(job.jobId, structuredClone(job));
    }
    async disableAndRevokeAuth(uid) { this.events.push(`disable:${uid}`); this.disabled.add(uid); }
    async deleteAuth(uid) {
        strict_1.default.ok(this.disabled.has(uid));
        strict_1.default.equal([...this.documents].some(path => path.startsWith(`users/${uid}/`) || path.endsWith(`:${uid}`)), false);
        strict_1.default.equal([...this.objects].some(path => path.startsWith(`users/${uid}/`)), false);
        this.events.push(`deleteAuth:${uid}`);
        this.deletedAuth.add(uid);
    }
    erase(prefix, limit) {
        const selected = [...this.documents].filter(path => path.startsWith(prefix)).slice(0, limit);
        for (const path of selected)
            this.documents.delete(path);
        return { deleted: selected.length, done: ![...this.documents].some(path => path.startsWith(prefix)) };
    }
    async eraseUserDataPage(uid, limit) {
        strict_1.default.ok(this.disabled.has(uid));
        this.events.push(`userPage:${uid}`);
        if (this.failUserPage) {
            this.failUserPage = false;
            throw new Error("injected provider failure, never exposed");
        }
        return this.erase(`users/${uid}/`, limit);
    }
    async eraseOperationalPage(collection, uid, limit) {
        return this.erase(`${collection}:${uid}`, limit);
    }
    async eraseStoragePage(uid, policy, limit) {
        this.events.push(`storagePage:${uid}`);
        if (policy.storageMode === "none")
            return { deleted: 0, done: true };
        const page = [...this.objects].filter(path => path.startsWith(`users/${uid}/`)).slice(0, limit);
        for (const path of page)
            this.objects.delete(path);
        return { deleted: page.length, done: ![...this.objects].some(path => path.startsWith(`users/${uid}/`)) };
    }
    async markClinicalRetentionPage(job, limit) {
        const entries = [...this.clinical].filter(([, c]) => c.uid === job.uid && !c.retained).slice(0, limit);
        for (const [id, c] of entries)
            this.clinical.set(id, { ...c, retained: true });
        return { done: ![...this.clinical.values()].some(c => c.uid === job.uid && !c.retained), cursor: entries.at(-1)?.[0] ?? job.clinicalCursor };
    }
    async purgeExpiredClinicalPage(job, limit) {
        strict_1.default.ok(this.now >= job.retainedUntilMs);
        const owned = [...this.clinical].filter(([, c]) => c.uid === job.uid).slice(0, limit);
        for (const [id, c] of owned) {
            strict_1.default.ok(c.retained);
            this.clinical.delete(id);
        }
        return { deleted: owned.length, done: ![...this.clinical.values()].some(c => c.uid === job.uid) };
    }
    async listRetentionDueJobs(limit) {
        return [...this.jobs.values()].filter(j => j.status === "COMPLETED_WITH_RETENTION" && j.maintenanceAtMs <= this.now).slice(0, limit).map(j => j.jobId);
    }
    async eraseExpiredJobMetadata(job, owner) {
        strict_1.default.equal(this.jobs.get(job.jobId)?.leaseOwner, owner);
        strict_1.default.ok(job.clinicalPurged && this.now >= Math.max(job.retainedUntilMs, job.backupRetentionUntilMs, job.receiptExpiresAtMs));
        this.jobs.delete(job.jobId);
        this.locks.delete(job.uid);
    }
}
function setup(policy = POLICY) {
    const backend = new FakeDeletionBackend();
    const caller = (uid = "parent_a") => ({ uid, token: { auth_time: Math.floor(backend.now / 1000) } });
    const token = (0, node_crypto_1.randomBytes)(32).toString("base64url");
    const input = { confirmation: "DELETE_MY_ACCOUNT", policyVersion: POLICY.version, receiptToken: token };
    return { backend, caller, token, input, service: new accountDeletion_1.AccountDeletionService(backend, policy) };
}
const rejectsCode = (code) => (err) => err instanceof config_1.AppError && err.code === code;
(0, node_test_1.default)("account deletion remains disabled without explicit versioned approval, finite retention and reviewed storage inventory", () => {
    strict_1.default.equal((0, accountDeletion_1.resolveAccountDeletionPolicy)({}), null);
    strict_1.default.equal((0, accountDeletion_1.resolveAccountDeletionPolicy)({ ...ENV, MANO_ACCOUNT_DELETION_POLICY_APPROVED: "false" }), null);
    strict_1.default.equal((0, accountDeletion_1.resolveAccountDeletionPolicy)({ ...ENV, MANO_ACCOUNT_DELETION_CLINICAL_RETENTION_DAYS: "0" }), null);
    strict_1.default.equal((0, accountDeletion_1.resolveAccountDeletionPolicy)({ ...ENV, MANO_ACCOUNT_DELETION_BACKUP_RETENTION_DAYS: "Infinity" }), null);
    strict_1.default.equal((0, accountDeletion_1.resolveAccountDeletionPolicy)({ ...ENV, MANO_ACCOUNT_DELETION_STORAGE_MODE: "" }), null);
    strict_1.default.equal((0, accountDeletion_1.resolveAccountDeletionPolicy)({ ...ENV, MANO_ACCOUNT_DELETION_STORAGE_MODE: "users-prefix-v1" }), null);
    strict_1.default.equal((0, accountDeletion_1.resolveAccountDeletionPolicy)({ ...ENV, MANO_ACCOUNT_DELETION_STORAGE_BUCKET: "configured-bucket" }), null);
    strict_1.default.deepEqual((0, accountDeletion_1.resolveAccountDeletionPolicy)(ENV), POLICY);
    strict_1.default.equal((0, accountDeletion_1.resolveAccountDeletionPolicy)({ ...ENV, MANO_ACCOUNT_DELETION_STORAGE_MODE: "users-prefix-v1", MANO_ACCOUNT_DELETION_STORAGE_BUCKET: "reviewed-bucket" }), null);
    strict_1.default.equal((0, accountDeletion_1.resolveAccountDeletionPolicy)({ ...ENV, MANO_ACCOUNT_DELETION_STORAGE_MODE: "users-prefix-v1", MANO_ACCOUNT_DELETION_STORAGE_BUCKET: "reviewed-bucket", MANO_ACCOUNT_DELETION_STORAGE_WRITERS_STOPPED: "true" })?.storageWritersStopped, true);
});
(0, node_test_1.default)("missing/recent/future authentication, body UID injection, invalid token and stale policy fail before mutation", async () => {
    const { backend, caller, input, service } = setup();
    for (const who of [null, { uid: "parent_a", token: {} }, { uid: "parent_a", token: { auth_time: (backend.now - 301000) / 1000 } }, { uid: "parent_a", token: { auth_time: (backend.now + 60000) / 1000 } }]) {
        await strict_1.default.rejects(service.request(who, input), rejectsCode("unauthenticated"));
    }
    await strict_1.default.rejects(service.request(caller(), { ...input, uid: "parent_b" }), rejectsCode("invalid-argument"));
    await strict_1.default.rejects(service.request(caller(), { ...input, confirmation: "yes" }), rejectsCode("failed-precondition"));
    await strict_1.default.rejects(service.request(caller(), { ...input, policyVersion: "older" }), rejectsCode("failed-precondition"));
    await strict_1.default.rejects(service.request(caller(), { ...input, receiptToken: "short" }), rejectsCode("invalid-argument"));
    await strict_1.default.rejects(new accountDeletion_1.AccountDeletionService(backend, null).request(caller(), input), rejectsCode("failed-precondition"));
    strict_1.default.equal(backend.jobs.size, 0);
    strict_1.default.equal(backend.locks.size, 0);
    strict_1.default.equal(backend.disabled.size, 0);
});
(0, node_test_1.default)("staff accounts cannot enter caregiver erasure or lose the clinical staff directory", async () => {
    const { backend, caller, input, service } = setup();
    backend.staff.add("parent_a");
    await strict_1.default.rejects(service.request(caller(), input), rejectsCode("failed-precondition"));
    strict_1.default.equal(backend.jobs.size, 0);
    strict_1.default.equal(backend.locks.size, 0);
});
(0, node_test_1.default)("a staff role appearing after request pauses before disabling or finally deleting Auth", async () => {
    for (const stage of ["DISABLE_AUTH", "DELETE_AUTH"]) {
        const { backend, caller, input, service } = setup();
        const receipt = await service.request(caller(), input);
        if (stage === "DELETE_AUTH") {
            for (let i = 0; i < 10 && backend.jobs.get(receipt.jobId)?.stage !== "DELETE_AUTH"; i++) {
                await service.processJob(receipt.jobId);
            }
        }
        backend.staff.add("parent_a");
        await service.processJob(receipt.jobId);
        const status = await service.getStatus(null, { jobId: receipt.jobId, receiptToken: input.receiptToken });
        strict_1.default.equal(status.stage, stage);
        strict_1.default.equal(status.status, "PARTIAL_FAILURE");
        strict_1.default.equal(status.failureCode, "INVENTORY_REVIEW_REQUIRED");
        strict_1.default.equal(backend.deletedAuth.size, 0);
        if (stage === "DISABLE_AUTH")
            strict_1.default.equal(backend.disabled.size, 0);
    }
});
(0, node_test_1.default)("staff onboarding cannot race past an account deletion lock through callable or operator helper", async () => {
    const store = new inMemoryStore_1.InMemoryBackendStore();
    const auth = new inMemoryStore_1.InMemoryAuthService();
    store.seedDoc("pharmacies/pharm_a", { pharmacyId: "pharm_a", displayNameFa: "TEST", active: true, acceptingCases: true });
    store.seedDoc("accountDeletionLocks/parent_a", { jobId: "locked" });
    const admin = { uid: "admin_a", token: { manoSystemAdmin: true } };
    const service = new admin_1.PharmacyAdminService(store, auth);
    await strict_1.default.rejects(service.onboardPharmacist(admin, {
        pharmacyId: "pharm_a", targetUid: "parent_a", credentialVerified: true, credentialReference: "TEST-CREDENTIAL",
    }), rejectsCode("permission-denied"));
    await strict_1.default.rejects((0, onboardPharmacist_1.runOutOfBandPharmacistOnboarding)(store, auth, {
        pharmacyId: "pharm_a", pharmacyNameFa: "TEST", targetUid: "parent_a", credentialReference: "TEST-CREDENTIAL",
    }), rejectsCode("permission-denied"));
    strict_1.default.equal(store.peekDoc("pharmacies/pharm_a/staff/parent_a"), null);
    strict_1.default.deepEqual(await auth.getCustomClaims("parent_a"), {});
});
(0, node_test_1.default)("request is idempotent, bound to authenticated UID and receipt has no identity, credential or medical metadata", async () => {
    const { backend, caller, token, input, service } = setup();
    const first = await service.request(caller(), input);
    const retry = await service.request(caller(), input);
    strict_1.default.deepEqual(first, retry);
    strict_1.default.equal(backend.jobs.size, 1);
    strict_1.default.equal(first.jobId, `del_${(0, node_crypto_1.createHash)("sha256").update(token).digest("hex").slice(0, 40)}`);
    strict_1.default.equal(backend.jobs.get(first.jobId)?.uid, caller().uid);
    strict_1.default.equal(JSON.stringify(first).includes(caller().uid), false);
    strict_1.default.equal(JSON.stringify(first).includes(token), false);
    strict_1.default.equal(Object.hasOwn(first, "receiptHash"), false);
    strict_1.default.equal(Object.hasOwn(first, "policy"), false);
    await strict_1.default.rejects(service.request(caller(), { ...input, receiptToken: (0, node_crypto_1.randomBytes)(32).toString("base64url") }), rejectsCode("failed-precondition"));
});
(0, node_test_1.default)("opaque receipt recovers a lost response after Auth deletion; wrong owner/token and expired receipts are denied", async () => {
    const { backend, caller, token, input, service } = setup();
    const expectedId = `del_${(0, node_crypto_1.createHash)("sha256").update(token).digest("hex").slice(0, 40)}`;
    await service.request(caller(), input); // Simulate losing the response, not the pre-saved receipt.
    for (let i = 0; i < 10; i++)
        await service.processPendingJobs();
    strict_1.default.ok(backend.deletedAuth.has("parent_a"));
    strict_1.default.equal((await service.getStatus(null, { jobId: expectedId, receiptToken: token })).status, "COMPLETED_WITH_RETENTION");
    await strict_1.default.rejects(service.getStatus(caller("parent_b"), { jobId: expectedId }), rejectsCode("permission-denied"));
    await strict_1.default.rejects(service.getStatus(null, { jobId: expectedId, receiptToken: (0, node_crypto_1.randomBytes)(32).toString("base64url") }), rejectsCode("permission-denied"));
    backend.now += 31 * DAY;
    await strict_1.default.rejects(service.getStatus(null, { jobId: expectedId, receiptToken: token }), rejectsCode("permission-denied"));
});
(0, node_test_1.default)("more than 500 owned records are erased in bounded pages; other users and retained clinical records survive; Auth is deleted last", async () => {
    const { backend, caller, input, service } = setup();
    for (let i = 0; i < 551; i++)
        backend.documents.add(`users/parent_a/profiles/missing/nested/${i}`);
    backend.documents.add("users/parent_b/profiles/primary");
    backend.documents.add("triagePasses:parent_a");
    backend.documents.add("rateLimits:parent_a");
    backend.documents.add("triagePasses:parent_b");
    backend.clinical.set("case_a", { uid: "parent_a", retained: false });
    backend.clinical.set("case_b", { uid: "parent_b", retained: false });
    const receipt = await service.request(caller(), input);
    await service.processJob(receipt.jobId);
    strict_1.default.deepEqual(backend.events, ["disable:parent_a"]);
    await service.processJob(receipt.jobId);
    strict_1.default.equal([...backend.documents].filter(p => p.startsWith("users/parent_a/")).length, 551 - accountDeletion_1.DELETION_BATCH_SIZE);
    strict_1.default.equal(backend.deletedAuth.size, 0);
    for (let i = 0; i < 30; i++)
        await service.processPendingJobs();
    const status = await service.getStatus(null, { jobId: receipt.jobId, receiptToken: input.receiptToken });
    strict_1.default.equal(status.status, "COMPLETED_WITH_RETENTION");
    strict_1.default.equal(status.deletedDocuments, 553);
    strict_1.default.equal(backend.events.at(-1), "deleteAuth:parent_a");
    strict_1.default.deepEqual([...backend.documents].sort(), ["triagePasses:parent_b", "users/parent_b/profiles/primary"]);
    strict_1.default.equal(backend.clinical.size, 2);
    strict_1.default.equal(backend.clinical.get("case_a")?.retained, true);
    strict_1.default.ok(status.retainedScopes.includes("managed-backups-declared-policy-not-verified-by-this-job"));
});
(0, node_test_1.default)("provider failure preserves a retryable checkpoint and never reports full deletion or deletes Auth prematurely", async () => {
    const { backend, caller, input, service } = setup();
    backend.documents.add("users/parent_a/profiles/primary");
    const receipt = await service.request(caller(), input);
    await service.processJob(receipt.jobId);
    backend.failUserPage = true;
    await service.processJob(receipt.jobId);
    const failed = await service.getStatus(caller(), { jobId: receipt.jobId });
    strict_1.default.equal(failed.status, "PARTIAL_FAILURE");
    strict_1.default.equal(failed.stage, "USER_DATA");
    strict_1.default.equal(failed.failureCode, "ERASURE_RETRY_REQUIRED");
    strict_1.default.equal(JSON.stringify(failed).includes("injected"), false);
    strict_1.default.equal(backend.deletedAuth.size, 0);
    strict_1.default.equal(await service.processPendingJobs(), 0, "Transient failures must back off instead of monopolizing the queue.");
    backend.now += 60_001;
    for (let i = 0; i < 10; i++)
        await service.processPendingJobs();
    strict_1.default.ok(backend.deletedAuth.has("parent_a"));
    strict_1.default.equal(backend.documents.size, 0);
});
(0, node_test_1.default)("Storage jobs wait out cached tokens without blocking a worker, then re-sweep recreated objects before Auth deletion", async () => {
    const policy = { ...POLICY, storageMode: "users-prefix-v1", storageBucket: "test-bucket", storageWritersStopped: true };
    const { backend, caller, input, service } = setup(policy);
    backend.objects.add("users/parent_a/before-disable");
    backend.objects.add("users/parent_b/keep");
    const receipt = await service.request(caller(), input);
    for (let i = 0; i < 12; i++)
        await service.processPendingJobs();
    strict_1.default.equal(backend.jobs.get(receipt.jobId)?.stage, "WAIT_STORAGE");
    strict_1.default.equal(backend.jobs.get(receipt.jobId)?.status, "IN_PROGRESS");
    strict_1.default.equal(backend.events.some(event => event.startsWith("storagePage:")), false);
    strict_1.default.equal(backend.deletedAuth.size, 0);
    // An old ID token can still upload during the hold. This file is not omitted.
    backend.objects.add("users/parent_a/cached-token-upload");
    backend.now += accountDeletion_1.STORAGE_TOKEN_EXPIRY_HOLD_MS - 1;
    strict_1.default.equal(await service.processPendingJobs(), 0);
    strict_1.default.equal(backend.deletedAuth.size, 0);
    backend.now++;
    for (let i = 0; i < 10 && backend.jobs.get(receipt.jobId)?.stage !== "DELETE_AUTH"; i++)
        await service.processPendingJobs();
    strict_1.default.equal(backend.jobs.get(receipt.jobId)?.stage, "DELETE_AUTH");
    strict_1.default.deepEqual([...backend.objects], ["users/parent_b/keep"]);
    // Even a recreated object after the first sweep is caught by the final sweep.
    for (let i = 0; i < 51; i++)
        backend.objects.add(`users/parent_a/resweep_${i}`);
    await service.processPendingJobs();
    strict_1.default.equal(backend.deletedAuth.size, 0, "A bounded incomplete final page must not delete Auth or complete the job");
    await service.processPendingJobs();
    strict_1.default.deepEqual([...backend.objects], ["users/parent_b/keep"]);
    strict_1.default.ok(backend.deletedAuth.has("parent_a"));
    strict_1.default.equal((await service.getStatus(null, { jobId: receipt.jobId, receiptToken: input.receiptToken })).status, "COMPLETED_WITH_RETENTION");
});
(0, node_test_1.default)("unattested Storage writers or a missing Auth-disable checkpoint fail closed before object erasure", async () => {
    for (const broken of ["writers", "checkpoint"]) {
        const policy = { ...POLICY, storageMode: "users-prefix-v1", storageBucket: "test-bucket", storageWritersStopped: true };
        const { backend, caller, input, service } = setup(policy);
        backend.objects.add("users/parent_a/keep-until-reviewed");
        const receipt = await service.request(caller(), input);
        await service.processJob(receipt.jobId);
        const job = backend.jobs.get(receipt.jobId);
        job.stage = "WAIT_STORAGE";
        if (broken === "writers")
            job.policy.storageWritersStopped = false;
        else
            job.authDisabledAtMs = null;
        backend.now += accountDeletion_1.STORAGE_TOKEN_EXPIRY_HOLD_MS + 1;
        await service.processJob(receipt.jobId);
        strict_1.default.equal(backend.events.some(event => event.startsWith("storagePage:")), false);
        strict_1.default.equal(backend.objects.size, 1);
        strict_1.default.equal(backend.deletedAuth.size, 0);
        strict_1.default.equal(backend.jobs.get(receipt.jobId)?.failureCode, "INVENTORY_REVIEW_REQUIRED");
    }
});
(0, node_test_1.default)("a live lease prevents concurrent worker execution and expired leases can resume", async () => {
    const { backend, caller, input, service } = setup();
    const receipt = await service.request(caller(), input);
    await backend.claimJob(receipt.jobId, "other-worker", backend.now + 1000);
    strict_1.default.equal(await service.processJob(receipt.jobId), false);
    strict_1.default.equal(backend.events.length, 0);
    backend.now += 1001;
    strict_1.default.equal(await service.processJob(receipt.jobId), true);
    strict_1.default.deepEqual(backend.events, ["disable:parent_a"]);
});
(0, node_test_1.default)("clinical retention is finite; metadata/tombstone survives until both receipt and declared backup windows expire", async () => {
    const { backend, caller, input, service } = setup();
    backend.clinical.set("case_a", { uid: "parent_a", retained: false });
    backend.clinical.set("case_b", { uid: "parent_b", retained: false });
    const receipt = await service.request(caller(), input);
    for (let i = 0; i < 10; i++)
        await service.processPendingJobs();
    strict_1.default.equal(await service.purgeExpiredClinicalRetention(), 0);
    backend.now += 11 * DAY;
    await service.purgeExpiredClinicalRetention();
    strict_1.default.equal(backend.clinical.has("case_a"), false);
    strict_1.default.equal(backend.clinical.has("case_b"), true);
    strict_1.default.ok(backend.jobs.has(receipt.jobId));
    strict_1.default.ok(backend.locks.has("parent_a"));
    backend.now += 30 * DAY;
    await service.purgeExpiredClinicalRetention();
    strict_1.default.equal(backend.jobs.has(receipt.jobId), false);
    strict_1.default.equal(backend.locks.has("parent_a"), false);
});
(0, node_test_1.default)("cached tokens cannot read or write consultations/care while a deletion lock exists", async () => {
    const store = new inMemoryStore_1.InMemoryBackendStore();
    store.seedDoc("accountDeletionLocks/parent_a", { jobId: "not-sensitive" });
    const caller = { uid: "parent_a", token: {} };
    const cases = new cases_1.ConsultationCaseService(store);
    await strict_1.default.rejects(cases.listAcceptingPharmacies(caller), rejectsCode("permission-denied"));
    await strict_1.default.rejects(cases.listParentConsultations(caller), rejectsCode("permission-denied"));
    await strict_1.default.rejects(cases.getParentConsultation(caller, { pharmacyId: "p", caseId: "c" }), rejectsCode("permission-denied"));
    await strict_1.default.rejects(cases.issueConsultationTriagePass(caller, { ageBandId: "toddler_1_3y" }), rejectsCode("permission-denied"));
    await strict_1.default.rejects((0, care_1.updateCare)(store, caller, {}), rejectsCode("permission-denied"));
    strict_1.default.equal(store.listDocsAtPrefix("triagePasses/").length, 0);
});
(0, node_test_1.default)("a lock created after intake authentication but before its transaction prevents a new triage pass", async () => {
    class RaceStore extends inMemoryStore_1.InMemoryBackendStore {
        count = 0;
        runTransaction(fn) {
            if (++this.count === 2)
                this.seedDoc("accountDeletionLocks/parent_a", { jobId: "race" });
            return super.runTransaction(fn);
        }
    }
    const store = new RaceStore();
    const cases = new cases_1.ConsultationCaseService(store);
    await strict_1.default.rejects(cases.issueConsultationTriagePass({ uid: "parent_a", token: {} }, {
        ageBandId: "toddler_1_3y", symptomFlowId: "constipation", temperatureCelsius: 36.7,
        questionSummaryFa: "پرسش غیر فوری درباره یبوست خفیف کودک سه ساله",
    }), rejectsCode("permission-denied"));
    strict_1.default.equal(store.listDocsAtPrefix("triagePasses/").length, 0);
});
//# sourceMappingURL=accountDeletion.test.js.map