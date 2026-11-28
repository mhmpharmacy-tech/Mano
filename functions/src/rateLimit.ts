import { AppError } from "./config";
import { BackendStore, RateLimitDoc } from "./types";
import { assertAccountActiveInTransaction } from "./auth";

export async function consumeRateLimit(
  store: BackendStore,
  uid: string,
  action: string,
  maxRequests: number,
  windowMs: number,
): Promise<void> {
  const bucketKey = `${action}_${uid}`.replace(/[^a-zA-Z0-9_-]/g, "_");
  const path = `rateLimits/${bucketKey}`;

  await store.runTransaction(async (tx) => {
    await assertAccountActiveInTransaction(tx, uid);
    const now = store.nowMs();
    const existing = await tx.get<RateLimitDoc>(path);

    if (!existing || now - existing.windowStartMs >= windowMs) {
      tx.set<RateLimitDoc>(path, {
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
      throw new AppError(
        "resource-exhausted",
        `Rate limit exceeded for ${action}. Please retry shortly.`,
        {
          retryAfterMs: Math.max(1000, windowMs - (now - existing.windowStartMs)),
        },
      );
    }

    tx.update(path, {
      count: existing.count + 1,
      updatedAt: now,
    });
  });
}
