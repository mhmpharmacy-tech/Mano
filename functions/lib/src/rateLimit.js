"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.consumeRateLimit = consumeRateLimit;
const config_1 = require("./config");
const auth_1 = require("./auth");
async function consumeRateLimit(store, uid, action, maxRequests, windowMs) {
    const bucketKey = `${action}_${uid}`.replace(/[^a-zA-Z0-9_-]/g, "_");
    const path = `rateLimits/${bucketKey}`;
    await store.runTransaction(async (tx) => {
        await (0, auth_1.assertAccountActiveInTransaction)(tx, uid);
        const now = store.nowMs();
        const existing = await tx.get(path);
        if (!existing || now - existing.windowStartMs >= windowMs) {
            tx.set(path, {
                bucketKey,
                uid,
                action,
                windowStartMs: now,
                count: 1,
                updatedAt: now,
            });
            return;
        }
        if (existing.count >= maxRequests) {
            throw new config_1.AppError("resource-exhausted", `Rate limit exceeded for ${action}. Please retry shortly.`, {
                retryAfterMs: Math.max(1000, windowMs - (now - existing.windowStartMs)),
            });
        }
        tx.update(path, {
            count: existing.count + 1,
            updatedAt: now,
        });
    });
}
//# sourceMappingURL=rateLimit.js.map