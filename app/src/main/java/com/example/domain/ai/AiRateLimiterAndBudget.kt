package com.example.domain.ai

sealed interface BudgetDecision {
  data object Allowed : BudgetDecision
  data class Denied(
    val outcome: AiGatewayOutcome,
    val reasonCode: String,
    val messageFa: String,
  ) : BudgetDecision
}

/**
 * Enforces sliding-window request rate limits plus per-session request & input-character budgets.
 */
class AiRateLimiterAndBudget(
  private val rateLimitWindowMs: Long,
  private val maxRequestsPerWindow: Int,
  private val maxSessionRequests: Int,
  private val maxSessionInputChars: Int,
  private val maxSinglePromptChars: Int,
  private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
  private val lock = Any()
  private val windowTimestampsMs = ArrayDeque<Long>()
  private var sessionRequestsCount: Int = 0
  private var sessionInputCharsCount: Int = 0

  fun tryAcquire(promptCharCount: Int): BudgetDecision =
    synchronized(lock) {
      if (promptCharCount > maxSinglePromptChars) {
        return@synchronized BudgetDecision.Denied(
          outcome = AiGatewayOutcome.REJECTED_BUDGET,
          reasonCode = "SINGLE_PROMPT_BUDGET_EXCEEDED",
          messageFa = "حجم متن ورودی از سقف مجاز ایمن بیشتر است؛ لطفاً متن کوتاه‌تری ارسال کنید.",
        )
      }
      if (sessionRequestsCount >= maxSessionRequests) {
        return@synchronized BudgetDecision.Denied(
          outcome = AiGatewayOutcome.REJECTED_BUDGET,
          reasonCode = "SESSION_REQUEST_CAP_EXCEEDED",
          messageFa = "سقف تعداد درخواست‌های هوش مصنوعی در این نشست تکمیل شده است؛ از راهنمای پشتیبان استفاده شد.",
        )
      }
      if (sessionInputCharsCount + promptCharCount > maxSessionInputChars) {
        return@synchronized BudgetDecision.Denied(
          outcome = AiGatewayOutcome.REJECTED_BUDGET,
          reasonCode = "SESSION_CHAR_BUDGET_EXCEEDED",
          messageFa = "بودجه پردازش متنی این نشست به پایان رسیده است.",
        )
      }

      val now = nowMs()
      while (windowTimestampsMs.isNotEmpty() && now - windowTimestampsMs.first() >= rateLimitWindowMs) {
        windowTimestampsMs.removeFirst()
      }
      if (windowTimestampsMs.size >= maxRequestsPerWindow) {
        return@synchronized BudgetDecision.Denied(
          outcome = AiGatewayOutcome.REJECTED_RATE_LIMIT,
          reasonCode = "RATE_LIMIT_WINDOW_EXCEEDED",
          messageFa = "تعداد درخواست‌ها در این دقیقه به سقف مجاز رسیده است؛ چند لحظه بعد تلاش کنید.",
        )
      }

      windowTimestampsMs.addLast(now)
      sessionRequestsCount += 1
      sessionInputCharsCount += promptCharCount
      BudgetDecision.Allowed
    }

  fun snapshotCounts(): Pair<Int, Int> =
    synchronized(lock) { sessionRequestsCount to sessionInputCharsCount }
}
