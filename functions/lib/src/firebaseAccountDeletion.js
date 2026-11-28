"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FirebaseAccountDeletionBackend = exports.FirestoreTreeEraser = exports.ACCOUNT_DELETION_LOCKS = void 0;
const app_1 = require("firebase-admin/app");
const auth_1 = require("firebase-admin/auth");
const firestore_1 = require("firebase-admin/firestore");
const storage_1 = require("firebase-admin/storage");
const config_1 = require("./config");
const firestoreStore_1 = require("./firestoreStore");
const auth_2 = require("./auth");
const JOBS = "accountDeletionJobs";
exports.ACCOUNT_DELETION_LOCKS = "accountDeletionLocks";
/** REST showMissing is essential: ordinary queries skip missing ancestor documents. */
class FirestoreTreeEraser {
    db;
    deadline = Date.now() + 35_000;
    requestsRemaining = 180;
    base;
    resourcePrefix;
    constructor(db) {
        this.db = db;
        // Inventory and deletion must address the same initialized SDK connection.
        // This runtime identity is deliberately checked rather than falling back to
        // a potentially different environment-selected project. If the SDK stops
        // exposing its initialized identity, erasure fails closed for review.
        let projectId;
        try {
            projectId = db.projectId;
        }
        catch {
            throw new config_1.AppError("failed-precondition", "Deletion database identity is not initialized.");
        }
        const configuredProject = process.env.GCLOUD_PROJECT ?? process.env.GOOGLE_CLOUD_PROJECT;
        if (typeof projectId !== "string" || !/^[a-z][a-z0-9-]{4,61}[a-z0-9]$/.test(projectId) ||
            (configuredProject !== undefined && configuredProject !== projectId) || db.databaseId !== config_1.FIRESTORE_DATABASE_ID) {
            throw new config_1.AppError("failed-precondition", "Deletion inventory must match the configured Admin SDK project and database.");
        }
        this.resourcePrefix = `projects/${projectId}/databases/${config_1.FIRESTORE_DATABASE_ID}/documents/`;
        const emulator = process.env.FIRESTORE_EMULATOR_HOST;
        if (emulator && !/^(localhost|127\.0\.0\.1|\[::1\]):\d{1,5}$/.test(emulator)) {
            throw new config_1.AppError("failed-precondition", "Deletion requires a reviewed Firestore endpoint.");
        }
        this.base = `${emulator ? `http://${emulator}` : "https://firestore.googleapis.com"}/v1/${this.resourcePrefix}`;
    }
    async request(path, init = {}) {
        if (--this.requestsRemaining < 0 || Date.now() >= this.deadline) {
            throw new config_1.AppError("resource-exhausted", "Deletion traversal will continue on the next worker run.");
        }
        const credential = (0, firestoreStore_1.getFirebaseAdminApp)().options.credential ?? (0, app_1.applicationDefault)();
        const headers = new Headers(init.headers);
        if (!process.env.FIRESTORE_EMULATOR_HOST) {
            headers.set("Authorization", `Bearer ${(await credential.getAccessToken()).access_token}`);
        }
        headers.set("Content-Type", "application/json");
        const response = await fetch(`${this.base}${path}`, {
            ...init, headers, signal: AbortSignal.timeout(Math.max(1, Math.min(15_000, this.deadline - Date.now()))),
        });
        if (!response.ok)
            throw new Error("Deletion inventory request failed.");
        return await response.json();
    }
    encoded(path) {
        return path.split("/").map(encodeURIComponent).join("/");
    }
    async erasePage(root, limit) {
        const parts = root.split("/");
        if (parts.length === 2 && parts[0] === "users")
            (0, auth_2.assertAccountUid)(parts[1]);
        else if (parts.length === 4 && parts[0] === "pharmacies" && parts[2] === "cases") {
            (0, auth_2.assertValidId)(parts[1], "pharmacyId");
            (0, auth_2.assertValidId)(parts[3], "caseId");
        }
        else
            throw new config_1.AppError("failed-precondition", "Deletion subtree is outside the reviewed inventory.");
        if (!Number.isInteger(limit) || limit < 1 || limit > 50) {
            throw new config_1.AppError("failed-precondition", "Deletion page size is outside the reviewed limit.");
        }
        const leaves = [];
        let traversalComplete = true;
        const visit = async (path, depth) => {
            if (depth > 100)
                throw new config_1.AppError("failed-precondition", "Unreviewed nested inventory depth.");
            if (leaves.length >= limit)
                return false;
            let collectionToken;
            let hasChildren = false;
            do {
                const page = await this.request(`${this.encoded(path)}:listCollectionIds`, {
                    method: "POST", body: JSON.stringify({ pageSize: 100, ...(collectionToken ? { pageToken: collectionToken } : {}) }),
                });
                const ids = page.collectionIds ?? [];
                if (!Array.isArray(ids) || ids.some(id => typeof id !== "string" || !id || id.includes("/"))) {
                    throw new config_1.AppError("failed-precondition", "Unexpected cloud inventory response.");
                }
                for (const id of ids) {
                    let documentToken;
                    do {
                        const query = new URLSearchParams({ pageSize: String(Math.min(limit, 100)), showMissing: "true" });
                        if (documentToken)
                            query.set("pageToken", documentToken);
                        const docsPage = await this.request(`${this.encoded(path)}/${encodeURIComponent(id)}?${query}`);
                        const docs = docsPage.documents ?? [];
                        if (!Array.isArray(docs))
                            throw new config_1.AppError("failed-precondition", "Unexpected cloud document inventory.");
                        for (const doc of docs) {
                            if (typeof doc.name !== "string" || !doc.name.startsWith(`${this.resourcePrefix}${path}/${id}/`)) {
                                throw new config_1.AppError("failed-precondition", "Cloud inventory escaped the owned subtree.");
                            }
                            const childPath = doc.name.slice(this.resourcePrefix.length);
                            if (childPath.split("/").length !== path.split("/").length + 2) {
                                throw new config_1.AppError("failed-precondition", "Unexpected descendant document path.");
                            }
                            hasChildren = true;
                            if (!await visit(childPath, depth + 1))
                                return false;
                            if (leaves.length >= limit)
                                return false;
                        }
                        documentToken = typeof docsPage.nextPageToken === "string" ? docsPage.nextPageToken : undefined;
                    } while (documentToken);
                }
                collectionToken = typeof page.nextPageToken === "string" ? page.nextPageToken : undefined;
            } while (collectionToken);
            // Delete ancestors on a later pass; doing so cannot hide orphan descendants.
            if (!hasChildren && (await this.db.doc(path).get()).exists)
                leaves.push(path);
            return !hasChildren;
        };
        try {
            traversalComplete = await visit(root, 0);
        }
        catch (err) {
            if (!(err instanceof config_1.AppError) || err.code !== "resource-exhausted" || leaves.length === 0)
                throw err;
            traversalComplete = false;
        }
        if (leaves.length > 0) {
            const batch = this.db.batch();
            for (const path of leaves)
                batch.delete(this.db.doc(path));
            await batch.commit();
        }
        return { deleted: leaves.length, done: traversalComplete };
    }
}
exports.FirestoreTreeEraser = FirestoreTreeEraser;
class FirebaseAccountDeletionBackend {
    db;
    auth;
    constructor(db, auth) {
        this.db = db;
        this.auth = auth ?? (0, auth_1.getAuth)((0, firestoreStore_1.getFirebaseAdminApp)());
    }
    nowMs() { return Date.now(); }
    async isStaffAccount(uid) {
        try {
            const user = await this.auth.getUser(uid);
            const claims = user.customClaims ?? {};
            if (claims.manoStaffRole || claims.manoStaffVerified || claims.manoSystemAdmin)
                return true;
        }
        catch (err) {
            if (err.code !== "auth/user-not-found")
                throw err;
        }
        // Live membership remains authoritative even when custom claims are stale.
        return !(await this.db.collectionGroup("staff").where("uid", "==", uid).limit(1).get()).empty;
    }
    async createOrGetJob(job) {
        return this.db.runTransaction(async (tx) => {
            const lockRef = this.db.collection(exports.ACCOUNT_DELETION_LOCKS).doc(job.uid);
            const lock = await tx.get(lockRef);
            if (lock.exists) {
                const existingId = lock.get("jobId");
                if (typeof existingId !== "string" || !/^del_[0-9a-f]{40}$/.test(existingId)) {
                    throw new config_1.AppError("failed-precondition", "Deletion lock needs operational review.");
                }
                const existing = await tx.get(this.db.collection(JOBS).doc(existingId));
                if (!existing.exists)
                    throw new config_1.AppError("failed-precondition", "Deletion receipt needs operational review.");
                return existing.data();
            }
            // The preflight Auth/directory check can race staff onboarding. Read the
            // live directory in this transaction; onboarding reads this same lock.
            const staff = await tx.get(this.db.collectionGroup("staff").where("uid", "==", job.uid).limit(1));
            if (!staff.empty)
                throw new config_1.AppError("failed-precondition", "Staff account deletion requires separate review.");
            tx.create(this.db.collection(JOBS).doc(job.jobId), job);
            tx.create(lockRef, { jobId: job.jobId, requestedAt: job.requestedAt });
            return job;
        });
    }
    async getJob(jobId) {
        const doc = await this.db.collection(JOBS).doc(jobId).get();
        return doc.exists ? doc.data() : null;
    }
    async listPendingJobs(limit) {
        const result = await this.db.collection(JOBS).where("nextAttemptAtMs", "<=", this.nowMs()).limit(limit).get();
        return result.docs.map(doc => doc.id);
    }
    async listRetentionDueJobs(limit) {
        const result = await this.db.collection(JOBS).where("maintenanceAtMs", "<=", this.nowMs()).limit(limit).get();
        return result.docs.filter(doc => doc.get("status") === "COMPLETED_WITH_RETENTION").map(doc => doc.id);
    }
    async eraseExpiredJobMetadata(job, owner) {
        await this.db.runTransaction(async (tx) => {
            const ref = this.db.collection(JOBS).doc(job.jobId);
            const lockRef = this.db.collection(exports.ACCOUNT_DELETION_LOCKS).doc(job.uid);
            const [actual, lock] = await Promise.all([tx.get(ref), tx.get(lockRef)]);
            if (!actual.exists || actual.get("leaseOwner") !== owner || actual.get("leaseUntilMs") < this.nowMs() ||
                !job.clinicalPurged || Math.max(actual.get("retainedUntilMs"), actual.get("backupRetentionUntilMs"), actual.get("receiptExpiresAtMs")) > this.nowMs() ||
                (lock.exists && lock.get("jobId") !== job.jobId)) {
                throw new config_1.AppError("failed-precondition", "Deletion metadata retention needs review.");
            }
            tx.delete(ref);
            tx.delete(lockRef);
        });
    }
    async claimJob(jobId, owner, until) {
        return this.db.runTransaction(async (tx) => {
            const ref = this.db.collection(JOBS).doc(jobId);
            const doc = await tx.get(ref);
            if (!doc.exists)
                return null;
            const job = doc.data();
            if (job.leaseUntilMs > this.nowMs())
                return null;
            tx.update(ref, { leaseOwner: owner, leaseUntilMs: until });
            return { ...job, leaseOwner: owner, leaseUntilMs: until };
        });
    }
    async saveClaimedJob(job, owner) {
        await this.db.runTransaction(async (tx) => {
            const ref = this.db.collection(JOBS).doc(job.jobId);
            const actual = await tx.get(ref);
            if (!actual.exists || actual.get("leaseOwner") !== owner || actual.get("leaseUntilMs") < this.nowMs()) {
                throw new config_1.AppError("failed-precondition", "Deletion worker lease expired; retry required.");
            }
            tx.set(ref, job);
        });
    }
    async disableAndRevokeAuth(uid) {
        try {
            await this.auth.updateUser(uid, { disabled: true });
            await this.auth.revokeRefreshTokens(uid);
        }
        catch (err) {
            if (err.code !== "auth/user-not-found")
                throw err;
        }
    }
    async deleteAuth(uid) {
        try {
            await this.auth.deleteUser(uid);
        }
        catch (err) {
            if (err.code !== "auth/user-not-found")
                throw err;
        }
    }
    eraseUserDataPage(uid, limit) {
        return new FirestoreTreeEraser(this.db).erasePage(`users/${uid}`, limit);
    }
    async eraseOperationalPage(collection, uid, limit) {
        const docs = await this.db.collection(collection).where("uid", "==", uid).limit(limit).get();
        if (!docs.empty) {
            const batch = this.db.batch();
            for (const doc of docs.docs)
                batch.delete(doc.ref);
            await batch.commit();
        }
        return { deleted: docs.size, done: docs.size < limit };
    }
    async eraseStoragePage(uid, policy, limit) {
        if (policy.storageMode === "none")
            return { deleted: 0, done: true };
        if (!policy.storageBucket || policy.storageWritersStopped !== true) {
            throw new config_1.AppError("failed-precondition", "Storage inventory and write quiescence require review.");
        }
        const prefix = `users/${uid}/`;
        const bucket = (0, storage_1.getStorage)((0, firestoreStore_1.getFirebaseAdminApp)()).bucket(policy.storageBucket);
        const [files] = await bucket.getFiles({ prefix, maxResults: limit, autoPaginate: false });
        for (const file of files) {
            if (!file.name.startsWith(prefix))
                throw new config_1.AppError("failed-precondition", "Storage inventory escaped the owned prefix.");
            // The service waits out cached Auth tokens and re-sweeps before completion.
            // Approved deployment policy must additionally stop resumable/signed/admin
            // writers; a named Firestore lock alone does not protect Storage writes.
            await file.delete({ ignoreNotFound: true });
        }
        return { deleted: files.length, done: files.length < limit };
    }
    async markClinicalRetentionPage(job, limit) {
        let query = this.db.collectionGroup("cases").where("parentUid", "==", job.uid).orderBy(firestore_1.FieldPath.documentId()).limit(limit);
        if (job.clinicalCursor)
            query = query.startAfter(this.db.doc(job.clinicalCursor));
        const cases = await query.get();
        if (!cases.empty) {
            const batch = this.db.batch();
            for (const doc of cases.docs) {
                if (!/^pharmacies\/[^/]+\/cases\/[^/]+$/.test(doc.ref.path)) {
                    throw new config_1.AppError("failed-precondition", "Clinical inventory requires review.");
                }
                batch.update(doc.ref, { accountDeletion: { jobId: job.jobId, policyVersion: job.policyVersion, retainUntilMs: job.retainedUntilMs } });
            }
            await batch.commit();
        }
        return { done: cases.size < limit, cursor: cases.docs.at(-1)?.ref.path ?? job.clinicalCursor };
    }
    async purgeExpiredClinicalPage(job, limit) {
        const cases = await this.db.collectionGroup("cases").where("parentUid", "==", job.uid).limit(limit).get();
        let deleted = 0;
        let complete = cases.size < limit;
        for (const doc of cases.docs) {
            if (!/^pharmacies\/[^/]+\/cases\/[^/]+$/.test(doc.ref.path) ||
                doc.get("accountDeletion.jobId") !== job.jobId ||
                doc.get("accountDeletion.retainUntilMs") > this.nowMs()) {
                throw new config_1.AppError("failed-precondition", "Retained clinical inventory requires review.");
            }
            const page = await new FirestoreTreeEraser(this.db).erasePage(doc.ref.path, Math.max(1, limit - deleted));
            deleted += page.deleted;
            complete = complete && page.done;
            if (deleted >= limit)
                return { deleted, done: false };
        }
        return { deleted, done: complete };
    }
}
exports.FirebaseAccountDeletionBackend = FirebaseAccountDeletionBackend;
//# sourceMappingURL=firebaseAccountDeletion.js.map