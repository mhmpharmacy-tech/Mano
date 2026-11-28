"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AccountDeletionService = exports.DELETION_BATCH_SIZE = exports.STORAGE_TOKEN_EXPIRY_HOLD_MS = void 0;
exports.resolveAccountDeletionPolicy = resolveAccountDeletionPolicy;
exports.toAccountDeletionReceipt = toAccountDeletionReceipt;
const node_crypto_1 = require("node:crypto");
const auth_1 = require("./auth");
const config_1 = require("./config");
const DAY_MS = 86_400_000;
const RECEIPT_DAYS = 30;
const REAUTH_MAX_AGE_MS = 5 * 60_000;
const LEASE_MS = 90_000;
exports.STORAGE_TOKEN_EXPIRY_HOLD_MS = 65 * 60_000;
exports.DELETION_BATCH_SIZE = 50;
/** These are deployment attestations, not a substitute for the recorded policy review. */
function resolveAccountDeletionPolicy(env) {
    if (env.MANO_ACCOUNT_DELETION_ENABLED !== "true" ||
        env.MANO_ACCOUNT_DELETION_POLICY_APPROVED !== "true")
        return null;
    const version = env.MANO_ACCOUNT_DELETION_POLICY_VERSION ?? "";
    const clinicalDaysText = env.MANO_ACCOUNT_DELETION_CLINICAL_RETENTION_DAYS ?? "";
    const backupDaysText = env.MANO_ACCOUNT_DELETION_BACKUP_RETENTION_DAYS ?? "";
    if (!/^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/.test(version) ||
        !/^\d{1,5}$/.test(clinicalDaysText) || !/^\d{1,5}$/.test(backupDaysText))
        return null;
    const clinicalRetentionDays = Number(clinicalDaysText);
    const backupRetentionDays = Number(backupDaysText);
    if (clinicalRetentionDays < 1 || clinicalRetentionDays > 36500 || backupRetentionDays > 36500)
        return null;
    const storageMode = env.MANO_ACCOUNT_DELETION_STORAGE_MODE;
    if (storageMode !== "none" && storageMode !== "users-prefix-v1")
        return null;
    const storageBucket = env.MANO_ACCOUNT_DELETION_STORAGE_BUCKET ?? null;
    if (storageMode === "none" && storageBucket)
        return null;
    if (storageMode === "users-prefix-v1" &&
        (!storageBucket || !/^[a-z0-9][a-z0-9._-]{1,220}[a-z0-9]$/.test(storageBucket)))
        return null;
    const storageWritersStopped = env.MANO_ACCOUNT_DELETION_STORAGE_WRITERS_STOPPED === "true";
    // SDK/server writers, previously issued signed URLs and resumable uploads are
    // not disabled merely by revoking Firebase Auth tokens. Deployment must first
    // stop these writers or implement a verified deletion lock on every write path.
    if (storageMode === "users-prefix-v1" && !storageWritersStopped)
        return null;
    return { version, clinicalRetentionDays, backupRetentionDays, storageMode, storageBucket, storageWritersStopped };
}
function toAccountDeletionReceipt(job) {
    return {
        jobId: job.jobId, status: job.status, stage: job.stage, policyVersion: job.policyVersion,
        requestedAt: job.requestedAt, completedAt: job.completedAt, retainedUntilMs: job.retainedUntilMs,
        backupRetentionUntilMs: job.backupRetentionUntilMs, receiptExpiresAtMs: job.receiptExpiresAtMs,
        deletedDocuments: job.deletedDocuments, retainedScopes: [...job.retainedScopes], failureCode: job.failureCode,
    };
}
function exactKeys(value, required, optional = []) {
    if (!value || typeof value !== "object" || Array.isArray(value) ||
        required.some(key => !Object.hasOwn(value, key)) ||
        Object.keys(value).some(key => !required.includes(key) && !optional.includes(key))) {
        throw new config_1.AppError("invalid-argument", "Invalid account deletion request fields.");
    }
}
function receiptHash(token) {
    if (typeof token !== "string" || !/^[A-Za-z0-9_-]{43}$/.test(token) ||
        Buffer.from(token, "base64url").toString("base64url") !== token) {
        throw new config_1.AppError("invalid-argument", "A 32-byte random receipt token is required.");
    }
    return (0, node_crypto_1.createHash)("sha256").update(token).digest("hex");
}
function hashMatches(a, b) {
    return /^[0-9a-f]{64}$/.test(a) && /^[0-9a-f]{64}$/.test(b) &&
        (0, node_crypto_1.timingSafeEqual)(Buffer.from(a, "hex"), Buffer.from(b, "hex"));
}
class AccountDeletionService {
    backend;
    policy;
    constructor(backend, policy) {
        this.backend = backend;
        this.policy = policy;
    }
    getPolicy(caller) {
        (0, auth_1.assertAuthenticated)(caller);
        return {
            available: this.policy !== null, policyVersion: this.policy?.version ?? null,
            clinicalRetentionDays: this.policy?.clinicalRetentionDays ?? null,
            backupRetentionDays: this.policy?.backupRetentionDays ?? null,
            receiptDays: RECEIPT_DAYS, staffSelfServiceSupported: false,
            storageTokenHoldMinutes: this.policy?.storageMode === "users-prefix-v1" ? 65 : 0,
        };
    }
    async request(caller, input) {
        const user = (0, auth_1.assertAuthenticated)(caller);
        const uid = (0, auth_1.assertAccountUid)(user.uid);
        if (!this.policy)
            throw new config_1.AppError("failed-precondition", "Account deletion is unavailable until the retention and storage inventory policy is approved.");
        exactKeys(input, ["confirmation", "policyVersion", "receiptToken"]);
        if (input.confirmation !== "DELETE_MY_ACCOUNT" || input.policyVersion !== this.policy.version) {
            throw new config_1.AppError("failed-precondition", "Explicit confirmation of the current deletion policy is required.");
        }
        const now = this.backend.nowMs();
        const authTime = typeof user.token.auth_time === "number" ? user.token.auth_time * 1000 : NaN;
        if (!Number.isFinite(authTime) || authTime > now + 30_000 || now - authTime > REAUTH_MAX_AGE_MS) {
            throw new config_1.AppError("unauthenticated", "Sign in again before requesting account deletion.");
        }
        const hash = receiptHash(input.receiptToken);
        if (await this.backend.isStaffAccount(uid)) {
            throw new config_1.AppError("failed-precondition", "Clinical staff account deletion requires a separately reviewed staff-retention workflow.");
        }
        const job = {
            // Derivable before request submission: a lost response cannot strand the
            // receipt after Auth deletion. The hash is not the bearer credential.
            jobId: `del_${hash.slice(0, 40)}`, uid, receiptHash: hash,
            status: "PENDING", stage: "DISABLE_AUTH", policyVersion: this.policy.version, policy: { ...this.policy },
            requestedAt: now, completedAt: null, retainedUntilMs: now + this.policy.clinicalRetentionDays * DAY_MS,
            backupRetentionUntilMs: now + this.policy.backupRetentionDays * DAY_MS,
            receiptExpiresAtMs: now + RECEIPT_DAYS * DAY_MS,
            deletedDocuments: 0, retainedScopes: ["clinical-case-and-audit-records", "managed-backups-declared-policy-not-verified-by-this-job", "minimal-deletion-receipt-and-security-tombstone", "device-local-data-and-user-exports-outside-server-erasure"],
            failureCode: null, leaseOwner: null, leaseUntilMs: 0, clinicalCursor: null, clinicalPurged: false, updatedAt: now,
            maintenanceAtMs: Number.MAX_SAFE_INTEGER,
            nextAttemptAtMs: now, retryCount: 0,
            authDisabledAtMs: null,
        };
        const actual = await this.backend.createOrGetJob(job);
        if (!hashMatches(actual.receiptHash, hash)) {
            throw new config_1.AppError("failed-precondition", "An account deletion job already exists. Use its saved receipt to check progress.");
        }
        return toAccountDeletionReceipt(actual);
    }
    async getStatus(caller, input) {
        exactKeys(input, ["jobId"], ["receiptToken"]);
        if (typeof input.jobId !== "string" || !/^del_[0-9a-f]{40}$/.test(input.jobId)) {
            throw new config_1.AppError("invalid-argument", "Invalid deletion receipt ID.");
        }
        // Hash validation occurs before lookup for bearer access, reducing ID probing.
        const hash = input.receiptToken !== undefined ? receiptHash(input.receiptToken) : null;
        const job = await this.backend.getJob(input.jobId);
        if (!job || job.receiptExpiresAtMs <= this.backend.nowMs() ||
            !(caller?.uid === job.uid || (hash !== null && hashMatches(job.receiptHash, hash)))) {
            throw new config_1.AppError("permission-denied", "Deletion receipt is unavailable, expired, or does not belong to this requester.");
        }
        return toAccountDeletionReceipt(job);
    }
    /** One bounded step, under a server-side lease; never callable with a client-supplied UID. */
    async processJob(jobId) {
        if (!this.policy)
            return false;
        const owner = (0, node_crypto_1.randomBytes)(16).toString("hex");
        const job = await this.backend.claimJob(jobId, owner, this.backend.nowMs() + LEASE_MS);
        if (!job || job.status === "COMPLETED_WITH_RETENTION")
            return false;
        try {
            job.status = "IN_PROGRESS";
            job.failureCode = null;
            job.nextAttemptAtMs = this.backend.nowMs();
            let page;
            switch (job.stage) {
                case "DISABLE_AUTH":
                    if (await this.backend.isStaffAccount(job.uid)) {
                        throw new config_1.AppError("failed-precondition", "Staff account deletion requires separate review.");
                    }
                    await this.backend.disableAndRevokeAuth(job.uid);
                    job.authDisabledAtMs = this.backend.nowMs();
                    job.stage = "USER_DATA";
                    break;
                case "USER_DATA":
                    page = await this.backend.eraseUserDataPage(job.uid, exports.DELETION_BATCH_SIZE);
                    job.deletedDocuments += page.deleted;
                    if (page.done)
                        job.stage = "TRIAGE";
                    break;
                case "TRIAGE":
                case "RATE_LIMITS": {
                    const collection = job.stage === "TRIAGE" ? "triagePasses" : "rateLimits";
                    page = await this.backend.eraseOperationalPage(collection, job.uid, exports.DELETION_BATCH_SIZE);
                    job.deletedDocuments += page.deleted;
                    if (page.done)
                        job.stage = job.stage === "TRIAGE" ? "RATE_LIMITS" : "WAIT_STORAGE";
                    break;
                }
                case "WAIT_STORAGE": {
                    const until = this.storageWriteQuiescenceAt(job);
                    if (this.backend.nowMs() < until)
                        job.nextAttemptAtMs = until;
                    else
                        job.stage = "STORAGE";
                    break;
                }
                case "STORAGE":
                    if (this.backend.nowMs() < this.storageWriteQuiescenceAt(job)) {
                        job.stage = "WAIT_STORAGE";
                        break;
                    }
                    page = await this.backend.eraseStoragePage(job.uid, job.policy, exports.DELETION_BATCH_SIZE);
                    if (page.done)
                        job.stage = "CLINICAL_MARK";
                    break;
                case "CLINICAL_MARK": {
                    const marked = await this.backend.markClinicalRetentionPage(job, exports.DELETION_BATCH_SIZE);
                    job.clinicalCursor = marked.cursor;
                    if (marked.done)
                        job.stage = "DELETE_AUTH";
                    break;
                }
                case "DELETE_AUTH":
                    if (await this.backend.isStaffAccount(job.uid)) {
                        throw new config_1.AppError("failed-precondition", "Staff account deletion requires separate review.");
                    }
                    if (job.policy.storageMode === "users-prefix-v1") {
                        if (this.backend.nowMs() < this.storageWriteQuiescenceAt(job)) {
                            job.stage = "WAIT_STORAGE";
                            break;
                        }
                        // Re-sweep after clinical inventory work before deleting Auth. A
                        // recreated object must never be hidden by an earlier empty page.
                        page = await this.backend.eraseStoragePage(job.uid, job.policy, exports.DELETION_BATCH_SIZE);
                        if (!page.done)
                            break;
                    }
                    await this.backend.deleteAuth(job.uid);
                    job.stage = "DONE";
                    job.status = "COMPLETED_WITH_RETENTION";
                    job.completedAt = this.backend.nowMs();
                    job.maintenanceAtMs = job.retainedUntilMs;
                    job.nextAttemptAtMs = Number.MAX_SAFE_INTEGER;
                    break;
                case "DONE": return false;
            }
            job.retryCount = 0;
        }
        catch (err) {
            job.status = "PARTIAL_FAILURE";
            job.retryCount++;
            job.nextAttemptAtMs = this.backend.nowMs() + Math.min(30 * 60_000, 30_000 * 2 ** Math.min(job.retryCount, 6));
            job.failureCode = err instanceof config_1.AppError && err.code === "failed-precondition"
                ? "INVENTORY_REVIEW_REQUIRED" : "ERASURE_RETRY_REQUIRED";
        }
        job.updatedAt = this.backend.nowMs();
        job.leaseOwner = null;
        job.leaseUntilMs = 0;
        await this.backend.saveClaimedJob(job, owner);
        return true;
    }
    storageWriteQuiescenceAt(job) {
        if (job.policy.storageMode === "none")
            return 0;
        if (job.policy.storageWritersStopped !== true || !Number.isFinite(job.authDisabledAtMs) ||
            job.authDisabledAtMs === null || job.authDisabledAtMs < job.requestedAt) {
            throw new config_1.AppError("failed-precondition", "Storage write quiescence and Auth expiry checkpoint require review.");
        }
        return job.authDisabledAtMs + exports.STORAGE_TOKEN_EXPIRY_HOLD_MS;
    }
    async processPendingJobs(limit = 10) {
        if (!this.policy)
            return 0;
        const ids = await this.backend.listPendingJobs(Math.min(Math.max(limit, 1), 20));
        let processed = 0;
        for (const id of ids)
            if (await this.processJob(id))
                processed++;
        return processed;
    }
    async purgeExpiredClinicalRetention(limit = 10) {
        if (!this.policy)
            return 0;
        let processed = 0;
        for (const id of await this.backend.listRetentionDueJobs(Math.min(Math.max(limit, 1), 20))) {
            const owner = (0, node_crypto_1.randomBytes)(16).toString("hex");
            const job = await this.backend.claimJob(id, owner, this.backend.nowMs() + LEASE_MS);
            if (!job || job.status !== "COMPLETED_WITH_RETENTION" || job.retainedUntilMs > this.backend.nowMs())
                continue;
            try {
                if (!job.clinicalPurged) {
                    const page = await this.backend.purgeExpiredClinicalPage(job, exports.DELETION_BATCH_SIZE);
                    job.deletedDocuments += page.deleted;
                    if (page.done) {
                        job.clinicalPurged = true;
                        job.retainedScopes = job.retainedScopes.filter(scope => scope !== "clinical-case-and-audit-records");
                        job.maintenanceAtMs = Math.max(job.retainedUntilMs, job.backupRetentionUntilMs, job.receiptExpiresAtMs);
                    }
                }
                if (job.clinicalPurged && job.maintenanceAtMs <= this.backend.nowMs()) {
                    await this.backend.eraseExpiredJobMetadata(job, owner);
                    processed++;
                    continue;
                }
                job.failureCode = null;
            }
            catch {
                job.failureCode = "ERASURE_RETRY_REQUIRED";
                job.maintenanceAtMs = this.backend.nowMs() + 5 * 60_000;
            }
            job.leaseOwner = null;
            job.leaseUntilMs = 0;
            job.updatedAt = this.backend.nowMs();
            await this.backend.saveClaimedJob(job, owner);
            processed++;
        }
        return processed;
    }
}
exports.AccountDeletionService = AccountDeletionService;
//# sourceMappingURL=accountDeletion.js.map