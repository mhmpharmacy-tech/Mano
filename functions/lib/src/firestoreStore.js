"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FirebaseAdminAuthService = exports.FirestoreBackendStore = void 0;
exports.getFirebaseAdminApp = getFirebaseAdminApp;
exports.getNamedFirestore = getNamedFirestore;
const app_1 = require("firebase-admin/app");
const auth_1 = require("firebase-admin/auth");
const firestore_1 = require("firebase-admin/firestore");
const node_crypto_1 = require("node:crypto");
const config_1 = require("./config");
let cachedApp = null;
function getFirebaseAdminApp() {
    if (!cachedApp) {
        cachedApp = (0, app_1.getApps)().length > 0 ? (0, app_1.getApps)()[0] : (0, app_1.initializeApp)();
    }
    return cachedApp;
}
function getNamedFirestore(databaseId = config_1.FIRESTORE_DATABASE_ID) {
    return (0, firestore_1.getFirestore)(getFirebaseAdminApp(), databaseId);
}
class FirestoreBackendStore {
    db;
    constructor(db) {
        this.db = db;
    }
    nowMs() {
        return Date.now();
    }
    generateId(prefix = "doc") {
        return `${prefix}_${(0, node_crypto_1.randomUUID)().replace(/-/g, "").slice(0, 20)}`;
    }
    async get(path) {
        const snap = await this.db.doc(path).get();
        return snap.exists ? snap.data() : null;
    }
    async queryCollection(collectionPath, filters, orderByField, direction, limitCount) {
        let query = this.db.collection(collectionPath);
        for (const f of filters) {
            query = query.where(f.field, f.op, f.value);
        }
        query = query.orderBy(orderByField, direction).limit(limitCount);
        const snap = await query.get();
        return snap.docs.map((d) => d.data());
    }
    async queryCollectionGroup(collectionId, filters, orderByField, direction, limitCount) {
        let query = this.db.collectionGroup(collectionId);
        for (const f of filters) {
            query = query.where(f.field, f.op, f.value);
        }
        query = query.orderBy(orderByField, direction).limit(limitCount);
        const snap = await query.get();
        return snap.docs.map((d) => d.data());
    }
    async deleteWhereLessThan(collectionPath, field, cutoffValue, limitCount) {
        const snap = await this.db
            .collection(collectionPath)
            .where(field, "<", cutoffValue)
            .limit(limitCount)
            .get();
        if (snap.empty)
            return 0;
        const batch = this.db.batch();
        for (const doc of snap.docs) {
            batch.delete(doc.ref);
        }
        await batch.commit();
        return snap.size;
    }
    async runTransaction(fn) {
        return this.db.runTransaction(async (adminTx) => {
            const wrappedTx = {
                get: async (path) => {
                    const snap = await adminTx.get(this.db.doc(path));
                    return snap.exists ? snap.data() : null;
                },
                set: (path, data) => {
                    adminTx.set(this.db.doc(path), data);
                },
                create: (path, data) => {
                    adminTx.create(this.db.doc(path), data);
                },
                update: (path, data) => {
                    adminTx.update(this.db.doc(path), data);
                },
                delete: (path) => {
                    adminTx.delete(this.db.doc(path));
                },
            };
            return fn(wrappedTx);
        });
    }
}
exports.FirestoreBackendStore = FirestoreBackendStore;
class FirebaseAdminAuthService {
    auth;
    constructor(auth) {
        this.auth = auth ?? (0, auth_1.getAuth)(getFirebaseAdminApp());
    }
    async getUidByEmail(email) {
        try {
            const user = await this.auth.getUserByEmail(email.trim().toLowerCase());
            return user.uid;
        }
        catch (err) {
            const code = err?.code;
            if (code === "auth/user-not-found") {
                return null;
            }
            throw err;
        }
    }
    async getCustomClaims(uid) {
        const user = await this.auth.getUser(uid);
        return user.customClaims ?? {};
    }
    async setCustomClaims(uid, claims) {
        await this.auth.setCustomUserClaims(uid, claims);
    }
    async revokeRefreshTokens(uid) {
        await this.auth.revokeRefreshTokens(uid);
    }
}
exports.FirebaseAdminAuthService = FirebaseAdminAuthService;
//# sourceMappingURL=firestoreStore.js.map