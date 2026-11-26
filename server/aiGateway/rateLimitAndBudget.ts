export interface RateLimitResult {
  allowed: boolean;
  remaining: number;
  retryAfterMs: number;
}

export class SlidingWindowRateLimiter {
  private readonly hits = new Map<string, number[]>();
  private readonly maxRequests: number;
  private readonly windowMs: number;
  private readonly now: () => number;

  constructor(params: { maxRequests: number; windowMs: number; now?: () => number }) {
    this.maxRequests = Math.max(1, params.maxRequests);
    this.windowMs = Math.max(1_000, params.windowMs);
    this.now = params.now ?? (() => Date.now());
  }

  checkAndConsume(clientKey: string): RateLimitResult {
    const currentMs = this.now();
    const cutoff = currentMs - this.windowMs;
    const prev = (this.hits.get(clientKey) ?? []).filter((ts) => ts > cutoff);

    if (prev.length >= this.maxRequests) {
      const oldest = prev[0] ?? currentMs;
      const retryAfterMs = Math.max(100, oldest + this.windowMs - currentMs);
      this.hits.set(clientKey, prev);
      return { allowed: false, remaining: 0, retryAfterMs };
    }

    prev.push(currentMs);
    this.hits.set(clientKey, prev);
    if (this.hits.size > 2_000) {
      for (const [key, timestamps] of this.hits.entries()) {
        if (timestamps.every((ts) => ts <= cutoff)) {
          this.hits.delete(key);
        }
      }
    }

    return {
      allowed: true,
      remaining: Math.max(0, this.maxRequests - prev.length),
      retryAfterMs: 0,
    };
  }
}

export interface BudgetStatus {
  allowed: boolean;
  remainingRequests: number;
  remainingChars: number;
  reason?: string;
}

export class GatewayBudgetTracker {
  private windowStartMs: number;
  private usedRequests = 0;
  private usedChars = 0;
  private readonly maxRequests: number;
  private readonly maxChars: number;
  private readonly windowMs: number;
  private readonly now: () => number;

  constructor(params: {
    maxRequests: number;
    maxChars: number;
    windowMs: number;
    now?: () => number;
  }) {
    this.maxRequests = Math.max(1, params.maxRequests);
    this.maxChars = Math.max(100, params.maxChars);
    this.windowMs = Math.max(1_000, params.windowMs);
    this.now = params.now ?? (() => Date.now());
    this.windowStartMs = this.now();
  }

  private rollWindowIfNeeded(): void {
    const currentMs = this.now();
    if (currentMs - this.windowStartMs >= this.windowMs) {
      this.windowStartMs = currentMs;
      this.usedRequests = 0;
      this.usedChars = 0;
    }
  }

  reserve(estimatedChars: number): BudgetStatus {
    this.rollWindowIfNeeded();
    const cleanChars = Math.max(1, Math.round(estimatedChars));

    if (this.usedRequests + 1 > this.maxRequests) {
      return {
        allowed: false,
        remainingRequests: 0,
        remainingChars: Math.max(0, this.maxChars - this.usedChars),
        reason: 'BUDGET_REQUEST_LIMIT_REACHED',
      };
    }

    if (this.usedChars + cleanChars > this.maxChars) {
      return {
        allowed: false,
        remainingRequests: Math.max(0, this.maxRequests - this.usedRequests),
        remainingChars: Math.max(0, this.maxChars - this.usedChars),
        reason: 'BUDGET_CHAR_LIMIT_REACHED',
      };
    }

    this.usedRequests += 1;
    this.usedChars += cleanChars;
    return {
      allowed: true,
      remainingRequests: Math.max(0, this.maxRequests - this.usedRequests),
      remainingChars: Math.max(0, this.maxChars - this.usedChars),
    };
  }

  recordActualOutputChars(chars: number): void {
    this.rollWindowIfNeeded();
    if (Number.isFinite(chars) && chars > 0) {
      this.usedChars += Math.round(chars);
    }
  }

  snapshot(): { remainingRequests: number; remainingChars: number } {
    this.rollWindowIfNeeded();
    return {
      remainingRequests: Math.max(0, this.maxRequests - this.usedRequests),
      remainingChars: Math.max(0, this.maxChars - this.usedChars),
    };
  }
}
