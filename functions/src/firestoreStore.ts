import { App, getApps, initializeApp } from "firebase-admin/app";
import { Auth, getAuth } from "firebase-admin/auth";
import {
  DocumentData,
  Firestore,
  Query,
  getFirestore,
} from "firebase-admin/firestore";
import { randomUUID } from "node:crypto";
import { FIRESTORE_DATABASE_ID } from "./config";
import {
  BackendAuthService,
  BackendStore,
  QueryFilter,
  StaffClaims,
  StoreTransaction,
} from "./types";

let cachedApp: App | null = null;

export function getFirebaseAdminApp(): App {
  if (!cachedApp) {
    cachedApp = getApps().length > 0 ? getApps()[0] : initializeApp();
  }
  return cachedApp;
}

export function getNamedFirestore(
  databaseId: string = FIRESTORE_DATABASE_ID,
): Firestore {
  return getFirestore(getFirebaseAdminApp(), databaseId);
}

export class FirestoreBackendStore implements BackendStore {
  constructor(private readonly db: Firestore) {}

  nowMs(): number {
    return Date.now();
  }

  generateId(prefix = "doc"): string {
    return `${prefix}_${randomUUID().replace(/-/g, "").slice(0, 20)}`;
  }

  async get<T>(path: string): Promise<T | null> {
    const snap = await this.db.doc(path).get();
    return snap.exists ? (snap.data() as T) : null;
  }

  async queryCollection<T>(
    collectionPath: string,
    filters: QueryFilter[],
    orderByField: string,
    direction: "asc" | "desc",
    limitCount: number,
  ): Promise<T[]> {
    let query: Query = this.db.collection(collectionPath);
    for (const f of filters) {
      query = query.where(f.field, f.op, f.value);
    }
    query = query.orderBy(orderByField, direction).limit(limitCount);
    const snap = await query.get();
    return snap.docs.map((d) => d.data() as T);
  }

  async queryCollectionGroup<T>(
    collectionId: string,
    filters: QueryFilter[],
    orderByField: string,
    direction: "asc" | "desc",
    limitCount: number,
  ): Promise<T[]> {
    let query: Query = this.db.collectionGroup(collectionId);
    for (const f of filters) {
      query = query.where(f.field, f.op, f.value);
    }
    query = query.orderBy(orderByField, direction).limit(limitCount);
    const snap = await query.get();
    return snap.docs.map((d) => d.data() as T);
  }

  async deleteWhereLessThan(
    collectionPath: string,
    field: string,
    cutoffValue: number,
    limitCount: number,
  ): Promise<number> {
    const snap = await this.db
      .collection(collectionPath)
      .where(field, "<", cutoffValue)
      .limit(limitCount)
      .get();
    if (snap.empty) return 0;
    const batch = this.db.batch();
    for (const doc of snap.docs) {
      batch.delete(doc.ref);
    }
    await batch.commit();
    return snap.size;
  }

  async runTransaction<R>(fn: (tx: StoreTransaction) => Promise<R>): Promise<R> {
    return this.db.runTransaction(async (adminTx) => {
      const wrappedTx: StoreTransaction = {
        get: async <T>(path: string): Promise<T | null> => {
          const snap = await adminTx.get(this.db.doc(path));
          return snap.exists ? (snap.data() as T) : null;
        },
        set: <T extends object>(path: string, data: T): void => {
          adminTx.set(this.db.doc(path), data as DocumentData);
        },
        create: <T extends object>(path: string, data: T): void => {
          adminTx.create(this.db.doc(path), data as DocumentData);
        },
        update: (path: string, data: Record<string, unknown>): void => {
          adminTx.update(this.db.doc(path), data);
        },
        delete: (path: string): void => {
          adminTx.delete(this.db.doc(path));
        },
      };
      return fn(wrappedTx);
    });
  }
}

export class FirebaseAdminAuthService implements BackendAuthService {
  private readonly auth: Auth;

  constructor(auth?: Auth) {
    this.auth = auth ?? getAuth(getFirebaseAdminApp());
  }

  async getUidByEmail(email: string): Promise<string | null> {
    try {
      const user = await this.auth.getUserByEmail(email.trim().toLowerCase());
      return user.uid;
    } catch (err: unknown) {
      const code = (err as { code?: string })?.code;
      if (code === "auth/user-not-found") {
        return null;
      }
      throw err;
    }
  }

  async getCustomClaims(uid: string): Promise<StaffClaims> {
    const user = await this.auth.getUser(uid);
    return (user.customClaims as StaffClaims | undefined) ?? {};
  }

  async setCustomClaims(uid: string, claims: StaffClaims): Promise<void> {
    await this.auth.setCustomUserClaims(uid, claims);
  }

  async revokeRefreshTokens(uid: string): Promise<void> {
    await this.auth.revokeRefreshTokens(uid);
  }
}
