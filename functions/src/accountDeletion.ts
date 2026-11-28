import { createHash, randomBytes, timingSafeEqual } from "node:crypto";
import { assertAuthenticated, assertAccountUid } from "./auth";
import { AppError } from "./config";
import { CallerContext } from "./types";

const DAY_MS = 86_400_000;
const RECEIPT_DAYS = 30;
const REAUTH_MAX_AGE_MS = 5 * 60_000;
const LEASE_MS = 90_000;
export const STORAGE_TOKEN_EXPIRY_HOLD_MS = 65 * 60_000;
export const DELETION_BATCH_SIZE = 50;

export interface AccountDeletionPolicy {
  version: string;
  clinicalRetentionDays: number;
  backupRetentionDays: number;
  storageMode: "none" | "users-prefix-v1";
  storageBucket: string | null;
  storageWritersStopped: boolean;
}

/** These are deployment attestations, not a substitute for the recorded policy review. */
export function resolveAccountDeletionPolicy(env: NodeJS.ProcessEnv): AccountDeletionPolicy | null {
  if (env.MANO_ACCOUNT_DELETION_ENABLED !== "true" ||
      env.MANO_ACCOUNT_DELETION_POLICY_APPROVED !== "true") return null;
  const version = env.MANO_ACCOUNT_DELETION_POLICY_VERSION ?? "";
  const clinicalDaysText = env.MANO_ACCOUNT_DELETION_CLINICAL_RETENTION_DAYS ?? "";
  const backupDaysText = env.MANO_ACCOUNT_DELETION_BACKUP_RETENTION_DAYS ?? "";
  if (!/^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/.test(version) ||
      !/^\d{1,5}$/.test(clinicalDaysText) || !/^\d{1,5}$/.test(backupDaysText)) return null;
  const clinicalRetentionDays = Number(clinicalDaysText);
  const backupRetentionDays = Number(backupDaysText);
  if (clinicalRetentionDays < 1 || clinicalRetentionDays > 36500 || backupRetentionDays > 36500) return null;
  const storageMode = env.MANO_ACCOUNT_DELETION_STORAGE_MODE;
  if (storageMode !== "none" && storageMode !== "users-prefix-v1") return null;
  const storageBucket = env.MANO_ACCOUNT_DELETION_STORAGE_BUCKET ?? null;
  if (storageMode === "none" && storageBucket) return null;
  if (storageMode === "users-prefix-v1" &&
      (!storageBucket || !/^[a-z0-9][a-z0-9._-]{1,220}[a-z0-9]$/.test(storageBucket))) return null;
  const storageWritersStopped = env.MANO_ACCOUNT_DELETION_STORAGE_WRITERS_STOPPED === "true";
  // SDK/server writers, previously issued signed URLs and resumable uploads are
  // not disabled merely by revoking Firebase Auth tokens. Deployment must first
  // stop these writers or implement a verified deletion lock on every write path.
  if (storageMode === "users-prefix-v1" && !storageWritersStopped) return null;
  return { version, clinicalRetentionDays, backupRetentionDays, storageMode, storageBucket, storageWritersStopped };
}

export type DeletionStage = "DISABLE_AUTH" | "USER_DATA" | "TRIAGE" | "RATE_LIMITS" |
  "WAIT_STORAGE" | "STORAGE" | "CLINICAL_MARK" | "DELETE_AUTH" | "DONE";
export type DeletionStatus = "PENDING" | "IN_PROGRESS" | "PARTIAL_FAILURE" | "COMPLETED_WITH_RETENTION";
export type DeletionFailureCode = "ERASURE_RETRY_REQUIRED" | "INVENTORY_REVIEW_REQUIRED" | null;

export interface AccountDeletionReceipt {
  jobId: string;
  status: DeletionStatus;
  stage: DeletionStage;
  policyVersion: string;
  requestedAt: number;
  completedAt: number | null;
  retainedUntilMs: number;
  backupRetentionUntilMs: number;
  receiptExpiresAtMs: number;
  deletedDocuments: number;
  retainedScopes: readonly string[];
  failureCode: DeletionFailureCode;
}

/** Private server-only job: no child details or credentials are stored here. */
export interface AccountDeletionJob extends AccountDeletionReceipt {
  uid: string;
  receiptHash: string;
  policy: AccountDeletionPolicy;
  leaseOwner: string | null;
  leaseUntilMs: number;
  clinicalCursor: string | null;
  clinicalPurged: boolean;
  maintenanceAtMs: number;
  nextAttemptAtMs: number;
  retryCount: number;
  updatedAt: number;
  authDisabledAtMs: number | null;
}

export interface ErasurePage {
  deleted: number;
  done: boolean;
}

export interface AccountDeletionBackend {
  nowMs(): number;
  isStaffAccount(uid: string): Promise<boolean>;
  createOrGetJob(job: AccountDeletionJob): Promise<AccountDeletionJob>;
  getJob(jobId: string): Promise<AccountDeletionJob | null>;
  listPendingJobs(limit: number): Promise<string[]>;
  claimJob(jobId: string, leaseOwner: string, leaseUntilMs: number): Promise<AccountDeletionJob | null>;
  saveClaimedJob(job: AccountDeletionJob, leaseOwner: string): Promise<void>;
  disableAndRevokeAuth(uid: string): Promise<void>;
  deleteAuth(uid: string): Promise<void>;
  eraseUserDataPage(uid: string, limit: number): Promise<ErasurePage>;
  eraseOperationalPage(collection: "triagePasses" | "rateLimits", uid: string, limit: number): Promise<ErasurePage>;
  eraseStoragePage(uid: string, policy: AccountDeletionPolicy, limit: number): Promise<ErasurePage>;
  markClinicalRetentionPage(job: AccountDeletionJob, limit: number): Promise<{ done: boolean; cursor: string | null }>;
  purgeExpiredClinicalPage(job: AccountDeletionJob, limit: number): Promise<ErasurePage>;
  listRetentionDueJobs(limit: number): Promise<string[]>;
  eraseExpiredJobMetadata(job: AccountDeletionJob, leaseOwner: string): Promise<void>;
}

export interface RequestAccountDeletionInput {
  confirmation: string;
  policyVersion: string;
  receiptToken: string;
}

export interface GetAccountDeletionStatusInput {
  jobId: string;
  receiptToken?: string;
}

export function toAccountDeletionReceipt(job: AccountDeletionJob): AccountDeletionReceipt {
  return {
    jobId: job.jobId, status: job.status, stage: job.stage, policyVersion: job.policyVersion,
    requestedAt: job.requestedAt, completedAt: job.completedAt, retainedUntilMs: job.retainedUntilMs,
    backupRetentionUntilMs: job.backupRetentionUntilMs, receiptExpiresAtMs: job.receiptExpiresAtMs,
    deletedDocuments: job.deletedDocuments, retainedScopes: [...job.retainedScopes], failureCode: job.failureCode,
  };
}

function exactKeys(value: unknown, required: readonly string[], optional: readonly string[] = []): asserts value is Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value) ||
      required.some(key => !Object.hasOwn(value, key)) ||
      Object.keys(value).some(key => !required.includes(key) && !optional.includes(key))) {
    throw new AppError("invalid-argument", "Invalid account deletion request fields.");
  }
}

function receiptHash(token: unknown): string {
  if (typeof token !== "string" || !/^[A-Za-z0-9_-]{43}$/.test(token) ||
      Buffer.from(token, "base64url").toString("base64url") !== token) {
    throw new AppError("invalid-argument", "A 32-byte random receipt token is required.");
  }
  return createHash("sha256").update(token).digest("hex");
}

function hashMatches(a: string, b: string): boolean {
  return /^[0-9a-f]{64}$/.test(a) && /^[0-9a-f]{64}$/.test(b) &&
    timingSafeEqual(Buffer.from(a, "hex"), Buffer.from(b, "hex"));
}

export class AccountDeletionService {
  constructor(private readonly backend: AccountDeletionBackend, private readonly policy: AccountDeletionPolicy | null) {}

  getPolicy(caller: CallerContext | null) {
    assertAuthenticated(caller);
    return {
      available: this.policy !== null, policyVersion: this.policy?.version ?? null,
      clinicalRetentionDays: this.policy?.clinicalRetentionDays ?? null,
      backupRetentionDays: this.policy?.backupRetentionDays ?? null,
      receiptDays: RECEIPT_DAYS, staffSelfServiceSupported: false,
      storageTokenHoldMinutes: this.policy?.storageMode === "users-prefix-v1" ? 65 : 0,
    };
  }

  async request(caller: CallerContext | null, input: unknown): Promise<AccountDeletionReceipt> {
    const user = assertAuthenticated(caller);
    const uid = assertAccountUid(user.uid);
    if (!this.policy) throw new AppError("failed-precondition", "Account deletion is unavailable until the retention and storage inventory policy is approved.");
    exactKeys(input, ["confirmation", "policyVersion", "receiptToken"]);
    if (input.confirmation !== "DELETE_MY_ACCOUNT" || input.policyVersion !== this.policy.version) {
      throw new AppError("failed-precondition", "Explicit confirmation of the current deletion policy is required.");
    }
    const now = this.backend.nowMs();
    const authTime = typeof user.token.auth_time === "number" ? user.token.auth_time * 1000 : NaN;
    if (!Number.isFinite(authTime) || authTime > now + 30_000 || now - authTime > REAUTH_MAX_AGE_MS) {
      throw new AppError("unauthenticated", "Sign in again before requesting account deletion.");
    }
    const hash = receiptHash(input.receiptToken);
    if (await this.backend.isStaffAccount(uid)) {
      throw new AppError("failed-precondition", "Clinical staff account deletion requires a separately reviewed staff-retention workflow.");
    }
    const job: AccountDeletionJob = {
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
      throw new AppError("failed-precondition", "An account deletion job already exists. Use its saved receipt to check progress.");
    }
    return toAccountDeletionReceipt(actual);
  }

  async getStatus(caller: CallerContext | null, input: unknown): Promise<AccountDeletionReceipt> {
    exactKeys(input, ["jobId"], ["receiptToken"]);
    if (typeof input.jobId !== "string" || !/^del_[0-9a-f]{40}$/.test(input.jobId)) {
      throw new AppError("invalid-argument", "Invalid deletion receipt ID.");
    }
    // Hash validation occurs before lookup for bearer access, reducing ID probing.
    const hash = input.receiptToken !== undefined ? receiptHash(input.receiptToken) : null;
    const job = await this.backend.getJob(input.jobId);
    if (!job || job.receiptExpiresAtMs <= this.backend.nowMs() ||
        !(caller?.uid === job.uid || (hash !== null && hashMatches(job.receiptHash, hash)))) {
      throw new AppError("permission-denied", "Deletion receipt is unavailable, expired, or does not belong to this requester.");
    }
    return toAccountDeletionReceipt(job);
  }

  /** One bounded step, under a server-side lease; never callable with a client-supplied UID. */
  async processJob(jobId: string): Promise<boolean> {
    if (!this.policy) return false;
    const owner = randomBytes(16).toString("hex");
    const job = await this.backend.claimJob(jobId, owner, this.backend.nowMs() + LEASE_MS);
    if (!job || job.status === "COMPLETED_WITH_RETENTION") return false;
    try {
      job.status = "IN_PROGRESS";
      job.failureCode = null;
      job.nextAttemptAtMs = this.backend.nowMs();
      let page: ErasurePage;
      switch (job.stage) {
        case "DISABLE_AUTH":
          if (await this.backend.isStaffAccount(job.uid)) {
            throw new AppError("failed-precondition", "Staff account deletion requires separate review.");
          }
          await this.backend.disableAndRevokeAuth(job.uid);
          job.authDisabledAtMs = this.backend.nowMs();
          job.stage = "USER_DATA";
          break;
        case "USER_DATA":
          page = await this.backend.eraseUserDataPage(job.uid, DELETION_BATCH_SIZE);
          job.deletedDocuments += page.deleted;
          if (page.done) job.stage = "TRIAGE";
          break;
        case "TRIAGE":
        case "RATE_LIMITS": {
          const collection = job.stage === "TRIAGE" ? "triagePasses" : "rateLimits";
          page = await this.backend.eraseOperationalPage(collection, job.uid, DELETION_BATCH_SIZE);
          job.deletedDocuments += page.deleted;
          if (page.done) job.stage = job.stage === "TRIAGE" ? "RATE_LIMITS" : "WAIT_STORAGE";
          break;
        }
        case "WAIT_STORAGE": {
          const until = this.storageWriteQuiescenceAt(job);
          if (this.backend.nowMs() < until) job.nextAttemptAtMs = until;
          else job.stage = "STORAGE";
          break;
        }
        case "STORAGE":
          if (this.backend.nowMs() < this.storageWriteQuiescenceAt(job)) {
            job.stage = "WAIT_STORAGE";
            break;
          }
          page = await this.backend.eraseStoragePage(job.uid, job.policy, DELETION_BATCH_SIZE);
          if (page.done) job.stage = "CLINICAL_MARK";
          break;
        case "CLINICAL_MARK": {
          const marked = await this.backend.markClinicalRetentionPage(job, DELETION_BATCH_SIZE);
          job.clinicalCursor = marked.cursor;
          if (marked.done) job.stage = "DELETE_AUTH";
          break;
        }
        case "DELETE_AUTH":
          if (await this.backend.isStaffAccount(job.uid)) {
            throw new AppError("failed-precondition", "Staff account deletion requires separate review.");
          }
          if (job.policy.storageMode === "users-prefix-v1") {
            if (this.backend.nowMs() < this.storageWriteQuiescenceAt(job)) {
              job.stage = "WAIT_STORAGE";
              break;
            }
            // Re-sweep after clinical inventory work before deleting Auth. A
            // recreated object must never be hidden by an earlier empty page.
            page = await this.backend.eraseStoragePage(job.uid, job.policy, DELETION_BATCH_SIZE);
            if (!page.done) break;
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
    } catch (err) {
      job.status = "PARTIAL_FAILURE";
      job.retryCount++;
      job.nextAttemptAtMs = this.backend.nowMs() + Math.min(30 * 60_000, 30_000 * 2 ** Math.min(job.retryCount, 6));
      job.failureCode = err instanceof AppError && err.code === "failed-precondition"
        ? "INVENTORY_REVIEW_REQUIRED" : "ERASURE_RETRY_REQUIRED";
    }
    job.updatedAt = this.backend.nowMs();
    job.leaseOwner = null;
    job.leaseUntilMs = 0;
    await this.backend.saveClaimedJob(job, owner);
    return true;
  }

  private storageWriteQuiescenceAt(job: AccountDeletionJob): number {
    if (job.policy.storageMode === "none") return 0;
    if (job.policy.storageWritersStopped !== true || !Number.isFinite(job.authDisabledAtMs) ||
        job.authDisabledAtMs === null || job.authDisabledAtMs < job.requestedAt) {
      throw new AppError("failed-precondition", "Storage write quiescence and Auth expiry checkpoint require review.");
    }
    return job.authDisabledAtMs + STORAGE_TOKEN_EXPIRY_HOLD_MS;
  }

  async processPendingJobs(limit = 10): Promise<number> {
    if (!this.policy) return 0;
    const ids = await this.backend.listPendingJobs(Math.min(Math.max(limit, 1), 20));
    let processed = 0;
    for (const id of ids) if (await this.processJob(id)) processed++;
    return processed;
  }

  async purgeExpiredClinicalRetention(limit = 10): Promise<number> {
    if (!this.policy) return 0;
    let processed = 0;
    for (const id of await this.backend.listRetentionDueJobs(Math.min(Math.max(limit, 1), 20))) {
      const owner = randomBytes(16).toString("hex");
      const job = await this.backend.claimJob(id, owner, this.backend.nowMs() + LEASE_MS);
      if (!job || job.status !== "COMPLETED_WITH_RETENTION" || job.retainedUntilMs > this.backend.nowMs()) continue;
      try {
        if (!job.clinicalPurged) {
          const page = await this.backend.purgeExpiredClinicalPage(job, DELETION_BATCH_SIZE);
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
      } catch {
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
