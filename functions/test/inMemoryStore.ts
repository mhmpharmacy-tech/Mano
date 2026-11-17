import { AppError } from "../src/config";
import {
  BackendAuthService,
  BackendStore,
  QueryFilter,
  StaffClaims,
  StoreTransaction,
} from "../src/types";

function cloneDoc<T>(obj: T): T {
  return structuredClone(obj);
}

function matchesFilters(
  doc: Record<string, unknown>,
  filters: QueryFilter[],
): boolean {
  return filters.every((f) => {
    if (f.op === "==") {
      return doc[f.field] === f.value;
    }
    return false;
  });
}

function getCollectionId(path: string): string {
  const parts = path.split("/");
  // doc path has even segments; collection name of doc is second-to-last segment
  return parts.length >= 2 ? parts[parts.length - 2] : "";
}

function getParentCollectionPath(path: string): string {
  const idx = path.lastIndexOf("/");
  return idx > 0 ? path.slice(0, idx) : "";
}

export class InMemoryBackendStore implements BackendStore {
  private readonly docs = new Map<string, Record<string, unknown>>();
  private idCounter = 0;
  private currentNowMs = 1_750_000_000_000;

  nowMs(): number {
    return this.currentNowMs;
  }

  setNowMs(ms: number): void {
    this.currentNowMs = ms;
  }

  advanceMs(deltaMs: number): void {
    this.currentNowMs += deltaMs;
  }

  generateId(prefix = "doc"): string {
    this.idCounter += 1;
    return `${prefix}_${this.idCounter}`;
  }

  seedDoc<T extends Record<string, unknown>>(path: string, data: T): void {
    this.docs.set(path, cloneDoc(data));
  }

  peekDoc<T>(path: string): T | null {
    const val = this.docs.get(path);
    return val ? cloneDoc(val as unknown as T) : null;
  }

  listDocsAtPrefix<T>(prefix: string): T[] {
    const out: T[] = [];
    for (const [path, data] of this.docs.entries()) {
      if (path.startsWith(prefix)) {
        out.push(cloneDoc(data as unknown as T));
      }
    }
    return out;
  }

  async get<T>(path: string): Promise<T | null> {
    const val = this.docs.get(path);
    return val ? cloneDoc(val as unknown as T) : null;
  }

  async queryCollection<T>(
    collectionPath: string,
    filters: QueryFilter[],
    orderByField: string,
    direction: "asc" | "desc",
    limitCount: number,
  ): Promise<T[]> {
    const results: Record<string, unknown>[] = [];
    for (const [path, data] of this.docs.entries()) {
      if (
        getParentCollectionPath(path) === collectionPath &&
        matchesFilters(data, filters)
      ) {
        results.push(cloneDoc(data));
      }
    }
    results.sort((a, b) => {
      const av = Number(a[orderByField] ?? 0);
      const bv = Number(b[orderByField] ?? 0);
      return direction === "desc" ? bv - av : av - bv;
    });
    return results.slice(0, limitCount) as unknown as T[];
  }

  async queryCollectionGroup<T>(
    collectionId: string,
    filters: QueryFilter[],
    orderByField: string,
    direction: "asc" | "desc",
    limitCount: number,
  ): Promise<T[]> {
    const results: Record<string, unknown>[] = [];
    for (const [path, data] of this.docs.entries()) {
      if (
        getCollectionId(path) === collectionId &&
        matchesFilters(data, filters)
      ) {
        results.push(cloneDoc(data));
      }
    }
    results.sort((a, b) => {
      const av = Number(a[orderByField] ?? 0);
      const bv = Number(b[orderByField] ?? 0);
      return direction === "desc" ? bv - av : av - bv;
    });
    return results.slice(0, limitCount) as unknown as T[];
  }

  async deleteWhereLessThan(
    collectionPath: string,
    field: string,
    cutoffValue: number,
    limitCount: number,
  ): Promise<number> {
    let deletedCount = 0;
    for (const [path, data] of Array.from(this.docs.entries())) {
      if (deletedCount >= limitCount) break;
      if (getParentCollectionPath(path) === collectionPath) {
        const val = data[field];
        if (typeof val === "number" && val < cutoffValue) {
          this.docs.delete(path);
          deletedCount += 1;
        }
      }
    }
    return deletedCount;
  }

  async runTransaction<R>(fn: (tx: StoreTransaction) => Promise<R>): Promise<R> {
    // Stage writes on a snapshot copy and commit atomically only if fn resolves
    const staged = new Map<string, Record<string, unknown>>();
    const deleted = new Set<string>();

    const tx: StoreTransaction = {
      get: async <T>(path: string): Promise<T | null> => {
        if (deleted.has(path)) return null;
        if (staged.has(path)) {
          return cloneDoc(staged.get(path) as unknown as T);
        }
        const live = this.docs.get(path);
        return live ? cloneDoc(live as unknown as T) : null;
      },
      set: <T extends object>(path: string, data: T): void => {
        deleted.delete(path);
        staged.set(path, cloneDoc(data as unknown as Record<string, unknown>));
      },
      create: <T extends object>(path: string, data: T): void => {
        if (this.docs.has(path) || staged.has(path)) {
          throw new AppError("already-exists", `Document already exists: ${path}`);
        }
        deleted.delete(path);
        staged.set(path, cloneDoc(data as unknown as Record<string, unknown>));
      },
      update: (path: string, data: Record<string, unknown>): void => {
        const current = staged.get(path) ?? this.docs.get(path);
        if (!current) {
          throw new AppError("not-found", `Document missing on update: ${path}`);
        }
        deleted.delete(path);
        staged.set(path, { ...cloneDoc(current), ...cloneDoc(data) });
      },
      delete: (path: string): void => {
        staged.delete(path);
        deleted.add(path);
      },
    };

    const result = await fn(tx);
    for (const path of deleted) {
      this.docs.delete(path);
    }
    for (const [path, data] of staged.entries()) {
      this.docs.set(path, data);
    }
    return result;
  }
}

export class InMemoryAuthService implements BackendAuthService {
  private readonly claims = new Map<string, StaffClaims>();
  readonly revokedTokensForUids: string[] = [];

  async getCustomClaims(uid: string): Promise<StaffClaims> {
    return { ...(this.claims.get(uid) ?? {}) };
  }

  async setCustomClaims(uid: string, claims: StaffClaims): Promise<void> {
    this.claims.set(uid, { ...claims });
  }

  async revokeRefreshTokens(uid: string): Promise<void> {
    this.revokedTokensForUids.push(uid);
  }
}
