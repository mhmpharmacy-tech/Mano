"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OperationalCleanupService = void 0;
const TRIAGE_RETENTION_GRACE_MS = 60 * 60 * 1000; // keep expired passes 1h for audit correlation
const RATE_LIMIT_RETENTION_MS = 24 * 60 * 60 * 1000; // prune stale rate limit buckets after 24h
class OperationalCleanupService {
    store;
    constructor(store) {
        this.store = store;
    }
    async purgeExpiredOperationalDocs(batchLimit = 200) {
        const now = this.store.nowMs();
        const triageCutoff = now - TRIAGE_RETENTION_GRACE_MS;
        const rateLimitCutoff = now - RATE_LIMIT_RETENTION_MS;
        const [deletedTriagePasses, deletedRateLimits] = await Promise.all([
            this.store.deleteWhereLessThan("triagePasses", "expiresAt", triageCutoff, batchLimit),
            this.store.deleteWhereLessThan("rateLimits", "windowStartMs", rateLimitCutoff, batchLimit),
        ]);
        return { deletedTriagePasses, deletedRateLimits };
    }
}
exports.OperationalCleanupService = OperationalCleanupService;
//# sourceMappingURL=cleanup.js.map