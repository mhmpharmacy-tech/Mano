"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.InMemoryAuthService = exports.InMemoryBackendStore = void 0;
const config_1 = require("../src/config");
function cloneDoc(obj) {
    return structuredClone(obj);
}
function matchesFilters(doc, filters) {
    return filters.every((f) => {
        if (f.op === "==") {
            return doc[f.field] === f.value;
        }
        return false;
    });
}
function getCollectionId(path) {
    const parts = path.split("/");
    // doc path has even segments; collection name of doc is second-to-last segment
    return parts.length >= 2 ? parts[parts.length - 2] : "";
}
function getParentCollectionPath(path) {
    const idx = path.lastIndexOf("/");
    return idx > 0 ? path.slice(0, idx) : "";
}
class InMemoryBackendStore {
    docs = new Map();
    idCounter = 0;
    currentNowMs = 1_750_000_000_000;
    nowMs() {
        return this.currentNowMs;
    }
    setNowMs(ms) {
        this.currentNowMs = ms;
    }
    advanceMs(deltaMs) {
        this.currentNowMs += deltaMs;
    }
    generateId(prefix = "doc") {
        this.idCounter += 1;
        return `${prefix}_${this.idCounter}`;
    }
    seedDoc(path, data) {
        this.docs.set(path, cloneDoc(data));
    }
    peekDoc(path) {
        const val = this.docs.get(path);
        return val ? cloneDoc(val) : null;
    }
    listDocsAtPrefix(prefix) {
        const out = [];
        for (const [path, data] of this.docs.entries()) {
            if (path.startsWith(prefix)) {
                out.push(cloneDoc(data));
            }
        }
        return out;
    }
    async get(path) {
        const val = this.docs.get(path);
        return val ? cloneDoc(val) : null;
    }
    async queryCollection(collectionPath, filters, orderByField, direction, limitCount) {
        const results = [];
        for (const [path, data] of this.docs.entries()) {
            if (getParentCollectionPath(path) === collectionPath &&
                matchesFilters(data, filters)) {
                results.push(cloneDoc(data));
            }
        }
        results.sort((a, b) => {
            const av = Number(a[orderByField] ?? 0);
            const bv = Number(b[orderByField] ?? 0);
            return direction === "desc" ? bv - av : av - bv;
        });
        return results.slice(0, limitCount);
    }
    async queryCollectionGroup(collectionId, filters, orderByField, direction, limitCount) {
        const results = [];
        for (const [path, data] of this.docs.entries()) {
            if (getCollectionId(path) === collectionId &&
                matchesFilters(data, filters)) {
                results.push(cloneDoc(data));
            }
        }
        results.sort((a, b) => {
            const av = Number(a[orderByField] ?? 0);
            const bv = Number(b[orderByField] ?? 0);
            return direction === "desc" ? bv - av : av - bv;
        });
        return results.slice(0, limitCount);
    }
    async deleteWhereLessThan(collectionPath, field, cutoffValue, limitCount) {
        let deletedCount = 0;
        for (const [path, data] of Array.from(this.docs.entries())) {
            if (deletedCount >= limitCount)
                break;
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
    async runTransaction(fn) {
        // Stage writes on a snapshot copy and commit atomically only if fn resolves
        const staged = new Map();
        const deleted = new Set();
        const tx = {
            get: async (path) => {
                if (deleted.has(path))
                    return null;
                if (staged.has(path)) {
                    return cloneDoc(staged.get(path));
                }
                const live = this.docs.get(path);
                return live ? cloneDoc(live) : null;
            },
            set: (path, data) => {
                deleted.delete(path);
                staged.set(path, cloneDoc(data));
            },
            create: (path, data) => {
                if (this.docs.has(path) || staged.has(path)) {
                    throw new config_1.AppError("already-exists", `Document already exists: ${path}`);
                }
                deleted.delete(path);
                staged.set(path, cloneDoc(data));
            },
            update: (path, data) => {
                const current = staged.get(path) ?? this.docs.get(path);
                if (!current) {
                    throw new config_1.AppError("not-found", `Document missing on update: ${path}`);
                }
                deleted.delete(path);
                staged.set(path, { ...cloneDoc(current), ...cloneDoc(data) });
            },
            delete: (path) => {
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
exports.InMemoryBackendStore = InMemoryBackendStore;
class InMemoryAuthService {
    claims = new Map();
    revokedTokensForUids = [];
    async getCustomClaims(uid) {
        return { ...(this.claims.get(uid) ?? {}) };
    }
    async setCustomClaims(uid, claims) {
        this.claims.set(uid, { ...claims });
    }
    async revokeRefreshTokens(uid) {
        this.revokedTokensForUids.push(uid);
    }
}
exports.InMemoryAuthService = InMemoryAuthService;
//# sourceMappingURL=inMemoryStore.js.map