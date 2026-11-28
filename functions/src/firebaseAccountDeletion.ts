import { applicationDefault } from "firebase-admin/app";
import { Auth, getAuth } from "firebase-admin/auth";
import { FieldPath, Firestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { AccountDeletionBackend, AccountDeletionJob, AccountDeletionPolicy, ErasurePage } from "./accountDeletion";
import { AppError, FIRESTORE_DATABASE_ID } from "./config";
import { getFirebaseAdminApp } from "./firestoreStore";
import { assertAccountUid, assertValidId } from "./auth";

const JOBS = "accountDeletionJobs";
export const ACCOUNT_DELETION_LOCKS = "accountDeletionLocks";

/** REST showMissing is essential: ordinary queries skip missing ancestor documents. */
export class FirestoreTreeEraser {
  private readonly deadline = Date.now() + 35_000;
  private requestsRemaining = 180;
  private readonly base: string;
  private readonly resourcePrefix: string;

  constructor(private readonly db: Firestore) {
    // Inventory and deletion must address the same initialized SDK connection.
    // This runtime identity is deliberately checked rather than falling back to
    // a potentially different environment-selected project. If the SDK stops
    // exposing its initialized identity, erasure fails closed for review.
    let projectId: unknown;
    try { projectId = (db as unknown as { readonly projectId: unknown }).projectId; }
    catch { throw new AppError("failed-precondition", "Deletion database identity is not initialized."); }
    const configuredProject = process.env.GCLOUD_PROJECT ?? process.env.GOOGLE_CLOUD_PROJECT;
    if (typeof projectId !== "string" || !/^[a-z][a-z0-9-]{4,61}[a-z0-9]$/.test(projectId) ||
        (configuredProject !== undefined && configuredProject !== projectId) || db.databaseId !== FIRESTORE_DATABASE_ID) {
      throw new AppError("failed-precondition", "Deletion inventory must match the configured Admin SDK project and database.");
    }
    this.resourcePrefix = `projects/${projectId}/databases/${FIRESTORE_DATABASE_ID}/documents/`;
    const emulator = process.env.FIRESTORE_EMULATOR_HOST;
    if (emulator && !/^(localhost|127\.0\.0\.1|\[::1\]):\d{1,5}$/.test(emulator)) {
      throw new AppError("failed-precondition", "Deletion requires a reviewed Firestore endpoint.");
    }
    this.base = `${emulator ? `http://${emulator}` : "https://firestore.googleapis.com"}/v1/${this.resourcePrefix}`;
  }

  private async request(path: string, init: RequestInit = {}): Promise<Record<string, unknown>> {
    if (--this.requestsRemaining < 0 || Date.now() >= this.deadline) {
      throw new AppError("resource-exhausted", "Deletion traversal will continue on the next worker run.");
    }
    const credential = getFirebaseAdminApp().options.credential ?? applicationDefault();
    const headers = new Headers(init.headers);
    if (!process.env.FIRESTORE_EMULATOR_HOST) {
      headers.set("Authorization", `Bearer ${(await credential.getAccessToken()).access_token}`);
    }
    headers.set("Content-Type", "application/json");
    const response = await fetch(`${this.base}${path}`, {
      ...init, headers, signal: AbortSignal.timeout(Math.max(1, Math.min(15_000, this.deadline - Date.now()))),
    });
    if (!response.ok) throw new Error("Deletion inventory request failed.");
    return await response.json() as Record<string, unknown>;
  }

  private encoded(path: string): string {
    return path.split("/").map(encodeURIComponent).join("/");
  }

  async erasePage(root: string, limit: number): Promise<ErasurePage> {
    const parts = root.split("/");
    if (parts.length === 2 && parts[0] === "users") assertAccountUid(parts[1]);
    else if (parts.length === 4 && parts[0] === "pharmacies" && parts[2] === "cases") {
      assertValidId(parts[1], "pharmacyId"); assertValidId(parts[3], "caseId");
    } else throw new AppError("failed-precondition", "Deletion subtree is outside the reviewed inventory.");
    if (!Number.isInteger(limit) || limit < 1 || limit > 50) {
      throw new AppError("failed-precondition", "Deletion page size is outside the reviewed limit.");
    }
    const leaves: string[] = [];
    let traversalComplete = true;
    const visit = async (path: string, depth: number): Promise<boolean> => {
      if (depth > 100) throw new AppError("failed-precondition", "Unreviewed nested inventory depth.");
      if (leaves.length >= limit) return false;
      let collectionToken: string | undefined;
      let hasChildren = false;
      do {
        const page = await this.request(`${this.encoded(path)}:listCollectionIds`, {
          method: "POST", body: JSON.stringify({ pageSize: 100, ...(collectionToken ? { pageToken: collectionToken } : {}) }),
        });
        const ids = page.collectionIds ?? [];
        if (!Array.isArray(ids) || ids.some(id => typeof id !== "string" || !id || id.includes("/"))) {
          throw new AppError("failed-precondition", "Unexpected cloud inventory response.");
        }
        for (const id of ids as string[]) {
          let documentToken: string | undefined;
          do {
            const query = new URLSearchParams({ pageSize: String(Math.min(limit, 100)), showMissing: "true" });
            if (documentToken) query.set("pageToken", documentToken);
            const docsPage = await this.request(`${this.encoded(path)}/${encodeURIComponent(id)}?${query}`);
            const docs = docsPage.documents ?? [];
            if (!Array.isArray(docs)) throw new AppError("failed-precondition", "Unexpected cloud document inventory.");
            for (const doc of docs as Array<{ name?: unknown }>) {
              if (typeof doc.name !== "string" || !doc.name.startsWith(`${this.resourcePrefix}${path}/${id}/`)) {
                throw new AppError("failed-precondition", "Cloud inventory escaped the owned subtree.");
              }
              const childPath = doc.name.slice(this.resourcePrefix.length);
              if (childPath.split("/").length !== path.split("/").length + 2) {
                throw new AppError("failed-precondition", "Unexpected descendant document path.");
              }
              hasChildren = true;
              if (!await visit(childPath, depth + 1)) return false;
              if (leaves.length >= limit) return false;
            }
            documentToken = typeof docsPage.nextPageToken === "string" ? docsPage.nextPageToken : undefined;
          } while (documentToken);
        }
        collectionToken = typeof page.nextPageToken === "string" ? page.nextPageToken : undefined;
      } while (collectionToken);
      // Delete ancestors on a later pass; doing so cannot hide orphan descendants.
      if (!hasChildren && (await this.db.doc(path).get()).exists) leaves.push(path);
      return !hasChildren;
    };
    try {
      traversalComplete = await visit(root, 0);
    } catch (err) {
      if (!(err instanceof AppError) || err.code !== "resource-exhausted" || leaves.length === 0) throw err;
      traversalComplete = false;
    }
    if (leaves.length > 0) {
      const batch = this.db.batch();
      for (const path of leaves) batch.delete(this.db.doc(path));
      await batch.commit();
    }
    return { deleted: leaves.length, done: traversalComplete };
  }
}

export class FirebaseAccountDeletionBackend implements AccountDeletionBackend {
  private readonly auth: Auth;
  constructor(private readonly db: Firestore, auth?: Auth) {
    this.auth = auth ?? getAuth(getFirebaseAdminApp());
  }
  nowMs(): number { return Date.now(); }

  async isStaffAccount(uid: string): Promise<boolean> {
    try {
      const user = await this.auth.getUser(uid);
      const claims = user.customClaims ?? {};
      if (claims.manoStaffRole || claims.manoStaffVerified || claims.manoSystemAdmin) return true;
    } catch (err) {
      if ((err as { code?: string }).code !== "auth/user-not-found") throw err;
    }
    // Live membership remains authoritative even when custom claims are stale.
    return !(await this.db.collectionGroup("staff").where("uid", "==", uid).limit(1).get()).empty;
  }

  async createOrGetJob(job: AccountDeletionJob): Promise<AccountDeletionJob> {
    return this.db.runTransaction(async tx => {
      const lockRef = this.db.collection(ACCOUNT_DELETION_LOCKS).doc(job.uid);
      const lock = await tx.get(lockRef);
      if (lock.exists) {
        const existingId: unknown = lock.get("jobId");
        if (typeof existingId !== "string" || !/^del_[0-9a-f]{40}$/.test(existingId)) {
          throw new AppError("failed-precondition", "Deletion lock needs operational review.");
        }
        const existing = await tx.get(this.db.collection(JOBS).doc(existingId));
        if (!existing.exists) throw new AppError("failed-precondition", "Deletion receipt needs operational review.");
        return existing.data() as AccountDeletionJob;
      }
      // The preflight Auth/directory check can race staff onboarding. Read the
      // live directory in this transaction; onboarding reads this same lock.
      const staff = await tx.get(this.db.collectionGroup("staff").where("uid", "==", job.uid).limit(1));
      if (!staff.empty) throw new AppError("failed-precondition", "Staff account deletion requires separate review.");
      tx.create(this.db.collection(JOBS).doc(job.jobId), job);
      tx.create(lockRef, { jobId: job.jobId, requestedAt: job.requestedAt });
      return job;
    });
  }

  async getJob(jobId: string): Promise<AccountDeletionJob | null> {
    const doc = await this.db.collection(JOBS).doc(jobId).get();
    return doc.exists ? doc.data() as AccountDeletionJob : null;
  }
  async listPendingJobs(limit: number): Promise<string[]> {
    const result = await this.db.collection(JOBS).where("nextAttemptAtMs", "<=", this.nowMs()).limit(limit).get();
    return result.docs.map(doc => doc.id);
  }
  async listRetentionDueJobs(limit: number): Promise<string[]> {
    const result = await this.db.collection(JOBS).where("maintenanceAtMs", "<=", this.nowMs()).limit(limit).get();
    return result.docs.filter(doc => doc.get("status") === "COMPLETED_WITH_RETENTION").map(doc => doc.id);
  }
  async eraseExpiredJobMetadata(job: AccountDeletionJob, owner: string): Promise<void> {
    await this.db.runTransaction(async tx => {
      const ref = this.db.collection(JOBS).doc(job.jobId);
      const lockRef = this.db.collection(ACCOUNT_DELETION_LOCKS).doc(job.uid);
      const [actual, lock] = await Promise.all([tx.get(ref), tx.get(lockRef)]);
      if (!actual.exists || actual.get("leaseOwner") !== owner || actual.get("leaseUntilMs") < this.nowMs() ||
          !job.clinicalPurged || Math.max(actual.get("retainedUntilMs"), actual.get("backupRetentionUntilMs"), actual.get("receiptExpiresAtMs")) > this.nowMs() ||
          (lock.exists && lock.get("jobId") !== job.jobId)) {
        throw new AppError("failed-precondition", "Deletion metadata retention needs review.");
      }
      tx.delete(ref);
      tx.delete(lockRef);
    });
  }
  async claimJob(jobId: string, owner: string, until: number): Promise<AccountDeletionJob | null> {
    return this.db.runTransaction(async tx => {
      const ref = this.db.collection(JOBS).doc(jobId);
      const doc = await tx.get(ref);
      if (!doc.exists) return null;
      const job = doc.data() as AccountDeletionJob;
      if (job.leaseUntilMs > this.nowMs()) return null;
      tx.update(ref, { leaseOwner: owner, leaseUntilMs: until });
      return { ...job, leaseOwner: owner, leaseUntilMs: until };
    });
  }
  async saveClaimedJob(job: AccountDeletionJob, owner: string): Promise<void> {
    await this.db.runTransaction(async tx => {
      const ref = this.db.collection(JOBS).doc(job.jobId);
      const actual = await tx.get(ref);
      if (!actual.exists || actual.get("leaseOwner") !== owner || actual.get("leaseUntilMs") < this.nowMs()) {
        throw new AppError("failed-precondition", "Deletion worker lease expired; retry required.");
      }
      tx.set(ref, job);
    });
  }
  async disableAndRevokeAuth(uid: string): Promise<void> {
    try {
      await this.auth.updateUser(uid, { disabled: true });
      await this.auth.revokeRefreshTokens(uid);
    } catch (err) {
      if ((err as { code?: string }).code !== "auth/user-not-found") throw err;
    }
  }
  async deleteAuth(uid: string): Promise<void> {
    try { await this.auth.deleteUser(uid); }
    catch (err) { if ((err as { code?: string }).code !== "auth/user-not-found") throw err; }
  }
  eraseUserDataPage(uid: string, limit: number): Promise<ErasurePage> {
    return new FirestoreTreeEraser(this.db).erasePage(`users/${uid}`, limit);
  }
  async eraseOperationalPage(collection: "triagePasses" | "rateLimits", uid: string, limit: number): Promise<ErasurePage> {
    const docs = await this.db.collection(collection).where("uid", "==", uid).limit(limit).get();
    if (!docs.empty) {
      const batch = this.db.batch();
      for (const doc of docs.docs) batch.delete(doc.ref);
      await batch.commit();
    }
    return { deleted: docs.size, done: docs.size < limit };
  }
  async eraseStoragePage(uid: string, policy: AccountDeletionPolicy, limit: number): Promise<ErasurePage> {
    if (policy.storageMode === "none") return { deleted: 0, done: true };
    if (!policy.storageBucket || policy.storageWritersStopped !== true) {
      throw new AppError("failed-precondition", "Storage inventory and write quiescence require review.");
    }
    const prefix = `users/${uid}/`;
    const bucket = getStorage(getFirebaseAdminApp()).bucket(policy.storageBucket);
    const [files] = await bucket.getFiles({ prefix, maxResults: limit, autoPaginate: false });
    for (const file of files) {
      if (!file.name.startsWith(prefix)) throw new AppError("failed-precondition", "Storage inventory escaped the owned prefix.");
      // The service waits out cached Auth tokens and re-sweeps before completion.
      // Approved deployment policy must additionally stop resumable/signed/admin
      // writers; a named Firestore lock alone does not protect Storage writes.
      await file.delete({ ignoreNotFound: true });
    }
    return { deleted: files.length, done: files.length < limit };
  }
  async markClinicalRetentionPage(job: AccountDeletionJob, limit: number): Promise<{ done: boolean; cursor: string | null }> {
    let query = this.db.collectionGroup("cases").where("parentUid", "==", job.uid).orderBy(FieldPath.documentId()).limit(limit);
    if (job.clinicalCursor) query = query.startAfter(this.db.doc(job.clinicalCursor));
    const cases = await query.get();
    if (!cases.empty) {
      const batch = this.db.batch();
      for (const doc of cases.docs) {
        if (!/^pharmacies\/[^/]+\/cases\/[^/]+$/.test(doc.ref.path)) {
          throw new AppError("failed-precondition", "Clinical inventory requires review.");
        }
        batch.update(doc.ref, { accountDeletion: { jobId: job.jobId, policyVersion: job.policyVersion, retainUntilMs: job.retainedUntilMs } });
      }
      await batch.commit();
    }
    return { done: cases.size < limit, cursor: cases.docs.at(-1)?.ref.path ?? job.clinicalCursor };
  }
  async purgeExpiredClinicalPage(job: AccountDeletionJob, limit: number): Promise<ErasurePage> {
    const cases = await this.db.collectionGroup("cases").where("parentUid", "==", job.uid).limit(limit).get();
    let deleted = 0;
    let complete = cases.size < limit;
    for (const doc of cases.docs) {
      if (!/^pharmacies\/[^/]+\/cases\/[^/]+$/.test(doc.ref.path) ||
          doc.get("accountDeletion.jobId") !== job.jobId ||
          doc.get("accountDeletion.retainUntilMs") > this.nowMs()) {
        throw new AppError("failed-precondition", "Retained clinical inventory requires review.");
      }
      const page = await new FirestoreTreeEraser(this.db).erasePage(doc.ref.path, Math.max(1, limit - deleted));
      deleted += page.deleted;
      complete = complete && page.done;
      if (deleted >= limit) return { deleted, done: false };
    }
    return { deleted, done: complete };
  }
}
