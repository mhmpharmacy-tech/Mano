import assert from "node:assert/strict";
import { createHash, randomBytes } from "node:crypto";
import test from "node:test";
import {
  AccountDeletionBackend, AccountDeletionJob, AccountDeletionPolicy, AccountDeletionService,
  DELETION_BATCH_SIZE, ErasurePage, resolveAccountDeletionPolicy, STORAGE_TOKEN_EXPIRY_HOLD_MS,
} from "../src/accountDeletion";
import { ConsultationCaseService } from "../src/cases";
import { updateCare } from "../src/care";
import { PharmacyAdminService } from "../src/admin";
import { runOutOfBandPharmacistOnboarding } from "../src/scripts/onboardPharmacist";
import { AppError } from "../src/config";
import { CallerContext, StoreTransaction } from "../src/types";
import { InMemoryAuthService, InMemoryBackendStore } from "./inMemoryStore";

const DAY = 86_400_000;
const POLICY: AccountDeletionPolicy = {
  version: "test-approved-v1", clinicalRetentionDays: 10, backupRetentionDays: 40,
  storageMode: "none", storageBucket: null, storageWritersStopped: false,
};
const ENV = {
  MANO_ACCOUNT_DELETION_ENABLED: "true", MANO_ACCOUNT_DELETION_POLICY_APPROVED: "true",
  MANO_ACCOUNT_DELETION_POLICY_VERSION: POLICY.version, MANO_ACCOUNT_DELETION_CLINICAL_RETENTION_DAYS: "10",
  MANO_ACCOUNT_DELETION_BACKUP_RETENTION_DAYS: "40", MANO_ACCOUNT_DELETION_STORAGE_MODE: "none",
};

class FakeDeletionBackend implements AccountDeletionBackend {
  now = 1_800_000_000_000;
  jobs = new Map<string, AccountDeletionJob>();
  locks = new Map<string, string>();
  staff = new Set<string>();
  documents = new Set<string>();
  clinical = new Map<string, { uid: string; retained: boolean }>();
  disabled = new Set<string>();
  deletedAuth = new Set<string>();
  objects = new Set<string>();
  events: string[] = [];
  failUserPage = false;
  nowMs() { return this.now; }
  async isStaffAccount(uid: string) { return this.staff.has(uid); }
  async createOrGetJob(job: AccountDeletionJob) {
    const existing = this.locks.get(job.uid);
    if (existing) return structuredClone(this.jobs.get(existing)!);
    this.jobs.set(job.jobId, structuredClone(job)); this.locks.set(job.uid, job.jobId);
    return structuredClone(job);
  }
  async getJob(id: string) { return structuredClone(this.jobs.get(id) ?? null); }
  async listPendingJobs(limit: number) {
    return [...this.jobs.values()].filter(j => j.nextAttemptAtMs <= this.now).sort((a, b) => a.nextAttemptAtMs - b.nextAttemptAtMs).slice(0, limit).map(j => j.jobId);
  }
  async claimJob(id: string, owner: string, until: number) {
    const job = this.jobs.get(id);
    if (!job || job.leaseUntilMs > this.now) return null;
    job.leaseOwner = owner; job.leaseUntilMs = until;
    return structuredClone(job);
  }
  async saveClaimedJob(job: AccountDeletionJob, owner: string) {
    assert.equal(this.jobs.get(job.jobId)?.leaseOwner, owner);
    this.jobs.set(job.jobId, structuredClone(job));
  }
  async disableAndRevokeAuth(uid: string) { this.events.push(`disable:${uid}`); this.disabled.add(uid); }
  async deleteAuth(uid: string) {
    assert.ok(this.disabled.has(uid));
    assert.equal([...this.documents].some(path => path.startsWith(`users/${uid}/`) || path.endsWith(`:${uid}`)), false);
    assert.equal([...this.objects].some(path => path.startsWith(`users/${uid}/`)), false);
    this.events.push(`deleteAuth:${uid}`); this.deletedAuth.add(uid);
  }
  private erase(prefix: string, limit: number): ErasurePage {
    const selected = [...this.documents].filter(path => path.startsWith(prefix)).slice(0, limit);
    for (const path of selected) this.documents.delete(path);
    return { deleted: selected.length, done: ![...this.documents].some(path => path.startsWith(prefix)) };
  }
  async eraseUserDataPage(uid: string, limit: number) {
    assert.ok(this.disabled.has(uid));
    this.events.push(`userPage:${uid}`);
    if (this.failUserPage) { this.failUserPage = false; throw new Error("injected provider failure, never exposed"); }
    return this.erase(`users/${uid}/`, limit);
  }
  async eraseOperationalPage(collection: "triagePasses" | "rateLimits", uid: string, limit: number) {
    return this.erase(`${collection}:${uid}`, limit);
  }
  async eraseStoragePage(uid: string, policy: AccountDeletionPolicy, limit: number) {
    this.events.push(`storagePage:${uid}`);
    if (policy.storageMode === "none") return { deleted: 0, done: true };
    const page = [...this.objects].filter(path => path.startsWith(`users/${uid}/`)).slice(0, limit);
    for (const path of page) this.objects.delete(path);
    return { deleted: page.length, done: ![...this.objects].some(path => path.startsWith(`users/${uid}/`)) };
  }
  async markClinicalRetentionPage(job: AccountDeletionJob, limit: number) {
    const entries = [...this.clinical].filter(([, c]) => c.uid === job.uid && !c.retained).slice(0, limit);
    for (const [id, c] of entries) this.clinical.set(id, { ...c, retained: true });
    return { done: ![...this.clinical.values()].some(c => c.uid === job.uid && !c.retained), cursor: entries.at(-1)?.[0] ?? job.clinicalCursor };
  }
  async purgeExpiredClinicalPage(job: AccountDeletionJob, limit: number) {
    assert.ok(this.now >= job.retainedUntilMs);
    const owned = [...this.clinical].filter(([, c]) => c.uid === job.uid).slice(0, limit);
    for (const [id, c] of owned) { assert.ok(c.retained); this.clinical.delete(id); }
    return { deleted: owned.length, done: ![...this.clinical.values()].some(c => c.uid === job.uid) };
  }
  async listRetentionDueJobs(limit: number) {
    return [...this.jobs.values()].filter(j => j.status === "COMPLETED_WITH_RETENTION" && j.maintenanceAtMs <= this.now).slice(0, limit).map(j => j.jobId);
  }
  async eraseExpiredJobMetadata(job: AccountDeletionJob, owner: string) {
    assert.equal(this.jobs.get(job.jobId)?.leaseOwner, owner);
    assert.ok(job.clinicalPurged && this.now >= Math.max(job.retainedUntilMs, job.backupRetentionUntilMs, job.receiptExpiresAtMs));
    this.jobs.delete(job.jobId); this.locks.delete(job.uid);
  }
}

function setup(policy: AccountDeletionPolicy | null = POLICY) {
  const backend = new FakeDeletionBackend();
  const caller = (uid = "parent_a"): CallerContext => ({ uid, token: { auth_time: Math.floor(backend.now / 1000) } });
  const token = randomBytes(32).toString("base64url");
  const input = { confirmation: "DELETE_MY_ACCOUNT", policyVersion: POLICY.version, receiptToken: token };
  return { backend, caller, token, input, service: new AccountDeletionService(backend, policy) };
}
const rejectsCode = (code: string) => (err: unknown) => err instanceof AppError && err.code === code;

test("account deletion remains disabled without explicit versioned approval, finite retention and reviewed storage inventory", () => {
  assert.equal(resolveAccountDeletionPolicy({}), null);
  assert.equal(resolveAccountDeletionPolicy({ ...ENV, MANO_ACCOUNT_DELETION_POLICY_APPROVED: "false" }), null);
  assert.equal(resolveAccountDeletionPolicy({ ...ENV, MANO_ACCOUNT_DELETION_CLINICAL_RETENTION_DAYS: "0" }), null);
  assert.equal(resolveAccountDeletionPolicy({ ...ENV, MANO_ACCOUNT_DELETION_BACKUP_RETENTION_DAYS: "Infinity" }), null);
  assert.equal(resolveAccountDeletionPolicy({ ...ENV, MANO_ACCOUNT_DELETION_STORAGE_MODE: "" }), null);
  assert.equal(resolveAccountDeletionPolicy({ ...ENV, MANO_ACCOUNT_DELETION_STORAGE_MODE: "users-prefix-v1" }), null);
  assert.equal(resolveAccountDeletionPolicy({ ...ENV, MANO_ACCOUNT_DELETION_STORAGE_BUCKET: "configured-bucket" }), null);
  assert.deepEqual(resolveAccountDeletionPolicy(ENV), POLICY);
  assert.equal(resolveAccountDeletionPolicy({ ...ENV, MANO_ACCOUNT_DELETION_STORAGE_MODE: "users-prefix-v1", MANO_ACCOUNT_DELETION_STORAGE_BUCKET: "reviewed-bucket" }), null);
  assert.equal(resolveAccountDeletionPolicy({ ...ENV, MANO_ACCOUNT_DELETION_STORAGE_MODE: "users-prefix-v1", MANO_ACCOUNT_DELETION_STORAGE_BUCKET: "reviewed-bucket", MANO_ACCOUNT_DELETION_STORAGE_WRITERS_STOPPED: "true" })?.storageWritersStopped, true);
});

test("missing/recent/future authentication, body UID injection, invalid token and stale policy fail before mutation", async () => {
  const { backend, caller, input, service } = setup();
  for (const who of [null, { uid: "parent_a", token: {} }, { uid: "parent_a", token: { auth_time: (backend.now - 301000) / 1000 } }, { uid: "parent_a", token: { auth_time: (backend.now + 60000) / 1000 } }]) {
    await assert.rejects(service.request(who, input), rejectsCode("unauthenticated"));
  }
  await assert.rejects(service.request(caller(), { ...input, uid: "parent_b" }), rejectsCode("invalid-argument"));
  await assert.rejects(service.request(caller(), { ...input, confirmation: "yes" }), rejectsCode("failed-precondition"));
  await assert.rejects(service.request(caller(), { ...input, policyVersion: "older" }), rejectsCode("failed-precondition"));
  await assert.rejects(service.request(caller(), { ...input, receiptToken: "short" }), rejectsCode("invalid-argument"));
  await assert.rejects(new AccountDeletionService(backend, null).request(caller(), input), rejectsCode("failed-precondition"));
  assert.equal(backend.jobs.size, 0); assert.equal(backend.locks.size, 0); assert.equal(backend.disabled.size, 0);
});

test("staff accounts cannot enter caregiver erasure or lose the clinical staff directory", async () => {
  const { backend, caller, input, service } = setup(); backend.staff.add("parent_a");
  await assert.rejects(service.request(caller(), input), rejectsCode("failed-precondition"));
  assert.equal(backend.jobs.size, 0); assert.equal(backend.locks.size, 0);
});

test("a staff role appearing after request pauses before disabling or finally deleting Auth", async () => {
  for (const stage of ["DISABLE_AUTH", "DELETE_AUTH"] as const) {
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
    assert.equal(status.stage, stage); assert.equal(status.status, "PARTIAL_FAILURE");
    assert.equal(status.failureCode, "INVENTORY_REVIEW_REQUIRED");
    assert.equal(backend.deletedAuth.size, 0);
    if (stage === "DISABLE_AUTH") assert.equal(backend.disabled.size, 0);
  }
});

test("staff onboarding cannot race past an account deletion lock through callable or operator helper", async () => {
  const store = new InMemoryBackendStore();
  const auth = new InMemoryAuthService();
  store.seedDoc("pharmacies/pharm_a", { pharmacyId: "pharm_a", displayNameFa: "TEST", active: true, acceptingCases: true });
  store.seedDoc("accountDeletionLocks/parent_a", { jobId: "locked" });
  const admin: CallerContext = { uid: "admin_a", token: { manoSystemAdmin: true } };
  const service = new PharmacyAdminService(store, auth);
  await assert.rejects(service.onboardPharmacist(admin, {
    pharmacyId: "pharm_a", targetUid: "parent_a", credentialVerified: true, credentialReference: "TEST-CREDENTIAL",
  }), rejectsCode("permission-denied"));
  await assert.rejects(runOutOfBandPharmacistOnboarding(store, auth, {
    pharmacyId: "pharm_a", pharmacyNameFa: "TEST", targetUid: "parent_a", credentialReference: "TEST-CREDENTIAL",
  }), rejectsCode("permission-denied"));
  assert.equal(store.peekDoc("pharmacies/pharm_a/staff/parent_a"), null);
  assert.deepEqual(await auth.getCustomClaims("parent_a"), {});
});

test("request is idempotent, bound to authenticated UID and receipt has no identity, credential or medical metadata", async () => {
  const { backend, caller, token, input, service } = setup();
  const first = await service.request(caller(), input);
  const retry = await service.request(caller(), input);
  assert.deepEqual(first, retry); assert.equal(backend.jobs.size, 1);
  assert.equal(first.jobId, `del_${createHash("sha256").update(token).digest("hex").slice(0, 40)}`);
  assert.equal(backend.jobs.get(first.jobId)?.uid, caller().uid);
  assert.equal(JSON.stringify(first).includes(caller().uid), false);
  assert.equal(JSON.stringify(first).includes(token), false);
  assert.equal(Object.hasOwn(first, "receiptHash"), false);
  assert.equal(Object.hasOwn(first, "policy"), false);
  await assert.rejects(service.request(caller(), { ...input, receiptToken: randomBytes(32).toString("base64url") }), rejectsCode("failed-precondition"));
});

test("opaque receipt recovers a lost response after Auth deletion; wrong owner/token and expired receipts are denied", async () => {
  const { backend, caller, token, input, service } = setup();
  const expectedId = `del_${createHash("sha256").update(token).digest("hex").slice(0, 40)}`;
  await service.request(caller(), input); // Simulate losing the response, not the pre-saved receipt.
  for (let i = 0; i < 10; i++) await service.processPendingJobs();
  assert.ok(backend.deletedAuth.has("parent_a"));
  assert.equal((await service.getStatus(null, { jobId: expectedId, receiptToken: token })).status, "COMPLETED_WITH_RETENTION");
  await assert.rejects(service.getStatus(caller("parent_b"), { jobId: expectedId }), rejectsCode("permission-denied"));
  await assert.rejects(service.getStatus(null, { jobId: expectedId, receiptToken: randomBytes(32).toString("base64url") }), rejectsCode("permission-denied"));
  backend.now += 31 * DAY;
  await assert.rejects(service.getStatus(null, { jobId: expectedId, receiptToken: token }), rejectsCode("permission-denied"));
});

test("more than 500 owned records are erased in bounded pages; other users and retained clinical records survive; Auth is deleted last", async () => {
  const { backend, caller, input, service } = setup();
  for (let i = 0; i < 551; i++) backend.documents.add(`users/parent_a/profiles/missing/nested/${i}`);
  backend.documents.add("users/parent_b/profiles/primary");
  backend.documents.add("triagePasses:parent_a"); backend.documents.add("rateLimits:parent_a");
  backend.documents.add("triagePasses:parent_b");
  backend.clinical.set("case_a", { uid: "parent_a", retained: false });
  backend.clinical.set("case_b", { uid: "parent_b", retained: false });
  const receipt = await service.request(caller(), input);
  await service.processJob(receipt.jobId);
  assert.deepEqual(backend.events, ["disable:parent_a"]);
  await service.processJob(receipt.jobId);
  assert.equal([...backend.documents].filter(p => p.startsWith("users/parent_a/")).length, 551 - DELETION_BATCH_SIZE);
  assert.equal(backend.deletedAuth.size, 0);
  for (let i = 0; i < 30; i++) await service.processPendingJobs();
  const status = await service.getStatus(null, { jobId: receipt.jobId, receiptToken: input.receiptToken });
  assert.equal(status.status, "COMPLETED_WITH_RETENTION"); assert.equal(status.deletedDocuments, 553);
  assert.equal(backend.events.at(-1), "deleteAuth:parent_a");
  assert.deepEqual([...backend.documents].sort(), ["triagePasses:parent_b", "users/parent_b/profiles/primary"]);
  assert.equal(backend.clinical.size, 2); assert.equal(backend.clinical.get("case_a")?.retained, true);
  assert.ok(status.retainedScopes.includes("managed-backups-declared-policy-not-verified-by-this-job"));
});

test("provider failure preserves a retryable checkpoint and never reports full deletion or deletes Auth prematurely", async () => {
  const { backend, caller, input, service } = setup();
  backend.documents.add("users/parent_a/profiles/primary");
  const receipt = await service.request(caller(), input);
  await service.processJob(receipt.jobId); backend.failUserPage = true;
  await service.processJob(receipt.jobId);
  const failed = await service.getStatus(caller(), { jobId: receipt.jobId });
  assert.equal(failed.status, "PARTIAL_FAILURE"); assert.equal(failed.stage, "USER_DATA");
  assert.equal(failed.failureCode, "ERASURE_RETRY_REQUIRED");
  assert.equal(JSON.stringify(failed).includes("injected"), false); assert.equal(backend.deletedAuth.size, 0);
  assert.equal(await service.processPendingJobs(), 0, "Transient failures must back off instead of monopolizing the queue.");
  backend.now += 60_001;
  for (let i = 0; i < 10; i++) await service.processPendingJobs();
  assert.ok(backend.deletedAuth.has("parent_a")); assert.equal(backend.documents.size, 0);
});

test("Storage jobs wait out cached tokens without blocking a worker, then re-sweep recreated objects before Auth deletion", async () => {
  const policy: AccountDeletionPolicy = { ...POLICY, storageMode: "users-prefix-v1", storageBucket: "test-bucket", storageWritersStopped: true };
  const { backend, caller, input, service } = setup(policy);
  backend.objects.add("users/parent_a/before-disable");
  backend.objects.add("users/parent_b/keep");
  const receipt = await service.request(caller(), input);
  for (let i = 0; i < 12; i++) await service.processPendingJobs();
  assert.equal(backend.jobs.get(receipt.jobId)?.stage, "WAIT_STORAGE");
  assert.equal(backend.jobs.get(receipt.jobId)?.status, "IN_PROGRESS");
  assert.equal(backend.events.some(event => event.startsWith("storagePage:")), false);
  assert.equal(backend.deletedAuth.size, 0);
  // An old ID token can still upload during the hold. This file is not omitted.
  backend.objects.add("users/parent_a/cached-token-upload");
  backend.now += STORAGE_TOKEN_EXPIRY_HOLD_MS - 1;
  assert.equal(await service.processPendingJobs(), 0);
  assert.equal(backend.deletedAuth.size, 0);
  backend.now++;
  for (let i = 0; i < 10 && backend.jobs.get(receipt.jobId)?.stage !== "DELETE_AUTH"; i++) await service.processPendingJobs();
  assert.equal(backend.jobs.get(receipt.jobId)?.stage, "DELETE_AUTH");
  assert.deepEqual([...backend.objects], ["users/parent_b/keep"]);
  // Even a recreated object after the first sweep is caught by the final sweep.
  for (let i = 0; i < 51; i++) backend.objects.add(`users/parent_a/resweep_${i}`);
  await service.processPendingJobs();
  assert.equal(backend.deletedAuth.size, 0, "A bounded incomplete final page must not delete Auth or complete the job");
  await service.processPendingJobs();
  assert.deepEqual([...backend.objects], ["users/parent_b/keep"]);
  assert.ok(backend.deletedAuth.has("parent_a"));
  assert.equal((await service.getStatus(null, { jobId: receipt.jobId, receiptToken: input.receiptToken })).status, "COMPLETED_WITH_RETENTION");
});

test("unattested Storage writers or a missing Auth-disable checkpoint fail closed before object erasure", async () => {
  for (const broken of ["writers", "checkpoint"] as const) {
    const policy: AccountDeletionPolicy = { ...POLICY, storageMode: "users-prefix-v1", storageBucket: "test-bucket", storageWritersStopped: true };
    const { backend, caller, input, service } = setup(policy);
    backend.objects.add("users/parent_a/keep-until-reviewed");
    const receipt = await service.request(caller(), input);
    await service.processJob(receipt.jobId);
    const job = backend.jobs.get(receipt.jobId)!;
    job.stage = "WAIT_STORAGE";
    if (broken === "writers") job.policy.storageWritersStopped = false;
    else job.authDisabledAtMs = null;
    backend.now += STORAGE_TOKEN_EXPIRY_HOLD_MS + 1;
    await service.processJob(receipt.jobId);
    assert.equal(backend.events.some(event => event.startsWith("storagePage:")), false);
    assert.equal(backend.objects.size, 1); assert.equal(backend.deletedAuth.size, 0);
    assert.equal(backend.jobs.get(receipt.jobId)?.failureCode, "INVENTORY_REVIEW_REQUIRED");
  }
});

test("a live lease prevents concurrent worker execution and expired leases can resume", async () => {
  const { backend, caller, input, service } = setup();
  const receipt = await service.request(caller(), input);
  await backend.claimJob(receipt.jobId, "other-worker", backend.now + 1000);
  assert.equal(await service.processJob(receipt.jobId), false); assert.equal(backend.events.length, 0);
  backend.now += 1001;
  assert.equal(await service.processJob(receipt.jobId), true); assert.deepEqual(backend.events, ["disable:parent_a"]);
});

test("clinical retention is finite; metadata/tombstone survives until both receipt and declared backup windows expire", async () => {
  const { backend, caller, input, service } = setup();
  backend.clinical.set("case_a", { uid: "parent_a", retained: false });
  backend.clinical.set("case_b", { uid: "parent_b", retained: false });
  const receipt = await service.request(caller(), input);
  for (let i = 0; i < 10; i++) await service.processPendingJobs();
  assert.equal(await service.purgeExpiredClinicalRetention(), 0);
  backend.now += 11 * DAY;
  await service.purgeExpiredClinicalRetention();
  assert.equal(backend.clinical.has("case_a"), false); assert.equal(backend.clinical.has("case_b"), true);
  assert.ok(backend.jobs.has(receipt.jobId)); assert.ok(backend.locks.has("parent_a"));
  backend.now += 30 * DAY;
  await service.purgeExpiredClinicalRetention();
  assert.equal(backend.jobs.has(receipt.jobId), false); assert.equal(backend.locks.has("parent_a"), false);
});

test("cached tokens cannot read or write consultations/care while a deletion lock exists", async () => {
  const store = new InMemoryBackendStore();
  store.seedDoc("accountDeletionLocks/parent_a", { jobId: "not-sensitive" });
  const caller: CallerContext = { uid: "parent_a", token: {} };
  const cases = new ConsultationCaseService(store);
  await assert.rejects(cases.listAcceptingPharmacies(caller), rejectsCode("permission-denied"));
  await assert.rejects(cases.listParentConsultations(caller), rejectsCode("permission-denied"));
  await assert.rejects(cases.getParentConsultation(caller, { pharmacyId: "p", caseId: "c" }), rejectsCode("permission-denied"));
  await assert.rejects(cases.issueConsultationTriagePass(caller, { ageBandId: "toddler_1_3y" }), rejectsCode("permission-denied"));
  await assert.rejects(updateCare(store, caller, {}), rejectsCode("permission-denied"));
  assert.equal(store.listDocsAtPrefix("triagePasses/").length, 0);
});

test("a lock created after intake authentication but before its transaction prevents a new triage pass", async () => {
  class RaceStore extends InMemoryBackendStore {
    count = 0;
    override runTransaction<R>(fn: (tx: StoreTransaction) => Promise<R>): Promise<R> {
      if (++this.count === 2) this.seedDoc("accountDeletionLocks/parent_a", { jobId: "race" });
      return super.runTransaction(fn);
    }
  }
  const store = new RaceStore(); const cases = new ConsultationCaseService(store);
  await assert.rejects(cases.issueConsultationTriagePass({ uid: "parent_a", token: {} }, {
    ageBandId: "toddler_1_3y", symptomFlowId: "constipation", temperatureCelsius: 36.7,
    questionSummaryFa: "پرسش غیر فوری درباره یبوست خفیف کودک سه ساله",
  }), rejectsCode("permission-denied"));
  assert.equal(store.listDocsAtPrefix("triagePasses/").length, 0);
});
