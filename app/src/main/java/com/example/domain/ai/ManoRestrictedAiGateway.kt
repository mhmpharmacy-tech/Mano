package com.example.domain.ai

import com.example.domain.model.AgeBand
import com.example.domain.model.TriageOutcome
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.NutritionPromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Single restricted, auditable gateway mediating all Gemini interactions in MANO Kid.
 * Enforces:
 * - Emergency triage bypass (URGENT_EMERGENCY never calls Gemini)
 * - Model catalog allowlist validation
 * - Untrusted input sanitization & prompt-injection/prohibited-intent detection
 * - Sliding-window rate limit & session request/char budgets
 * - Bounded coroutine timeouts & bounded transient retries
 * - Cooperative CancellationException propagation
 * - Independent semantic validation rejecting malformed JSON, contradictions, and fabricated references
 * - Safe deterministic fallbacks + PII-free audit trail
 */
class ManoRestrictedAiGateway(
  private val transport: AiGatewayTransport,
  private val config: AiGatewayConfig = AiGatewayConfig(),
  private val auditRecorder: AiAuditTrailRecorder = AiAuditTrailRecorder(config.maxAuditEntries),
  nowMs: () -> Long = { System.currentTimeMillis() },
) {
  private val rateLimiter =
    AiRateLimiterAndBudget(
      rateLimitWindowMs = config.rateLimitWindowMs,
      maxRequestsPerWindow = config.maxRequestsPerWindow,
      maxSessionRequests = config.maxSessionRequests,
      maxSessionInputChars = config.maxSessionInputChars,
      maxSinglePromptChars = config.maxSinglePromptChars,
      nowMs = nowMs,
    )

  fun auditEntries(): List<AiAuditEntry> = auditRecorder.snapshot()

  fun clearAuditTrail() {
    auditRecorder.clear()
  }

  suspend fun generateSafetyChecklist(
    request: SafetyChecklistRequest,
  ): AiGatewayResponse<SafetyChecklistAiJson> {
    if (request.triageOutcome == TriageOutcome.URGENT_EMERGENCY) {
      val fallback = AiOfflineFallbacks.emergencyChecklistBypass(request)
      recordAudit(
        responsibility = AiResponsibility.EXPLAIN_DETERMINISTIC_OUTPUT,
        modelId = "deterministic-triage-engine",
        outcome = AiGatewayOutcome.EMERGENCY_BYPASS,
        verificationState = AiVerificationState.DETERMINISTIC_EMERGENCY_GUIDANCE,
        promptCharCount = 0,
        attemptsUsed = 0,
        reasonCode = "URGENT_EMERGENCY_BYPASS",
      )
      return AiGatewayResponse(
        data = fallback,
        outcome = AiGatewayOutcome.EMERGENCY_BYPASS,
        verificationState = AiVerificationState.DETERMINISTIC_EMERGENCY_GUIDANCE,
        usedFallback = true,
        modelId = "deterministic-triage-engine",
        rejectionReasonCode = "URGENT_EMERGENCY_BYPASS",
        userNoticeFa = "به دلیل وجود نشانه فوری، هوش مصنوعی دور زده شد و راهنمای قطعی اورژانس ۱۱۵ نمایش داده شد.",
      )
    }

    val modelId = request.modelId.trim()
    val rawUserStrings = request.coSelectedSymptomsFa.joinToString(" | ")
    val prompt = buildString {
      appendLine("Task: Explain deterministic clinical checklist items without changing triage severity.")
      appendLine("Allowed citedSourceIds: ${config.approvedSourceIds.joinToString(", ")}")
      appendLine("Deterministic Triage Outcome: ${request.triageOutcome?.name ?: "UNKNOWN"}")
      appendLine("Symptom Flow: ${request.primaryFlow.titleFa}")
      appendLine("AgeBand: ${request.ageBand?.labelFa ?: "UNKNOWN"}")
      appendLine("Temperature: ${request.temperatureCelsius ?: "UNKNOWN"}")
      appendLine("DurationHours: ${request.durationHours ?: "UNKNOWN"}")
      appendLine(AiPolicyAndGuardrails.wrapUntrustedBlock("co_selected_symptoms", rawUserStrings))
    }

    return executeGuarded(
      responsibility = AiResponsibility.EXPLAIN_DETERMINISTIC_OUTPUT,
      taskSchema = AiTaskSchema.SAFETY_CHECKLIST,
      modelId = modelId,
      requiresVision = false,
      rawUntrustedInputs = arrayOf(rawUserStrings),
      userPrompt = prompt,
      jpegImageBytes = null,
      validate = { rawJson ->
        AiSemanticValidator.validateSafetyChecklist(rawJson, config.approvedSourceIds)
      },
      fallbackProvider = { _ -> AiOfflineFallbacks.safetyChecklistFallback(request) },
    )
  }

  suspend fun answerDocumentedQuestion(
    request: DocumentedQaRequest,
  ): AiGatewayResponse<DocumentedQaAiJson> {
    val modelId = request.modelId.trim()
    val approvedSnippets =
      request.relatedFaqs.take(4).joinToString("\n") { faq ->
        "- [${faq.source.id}] Q: ${faq.questionFa} | Summary: ${faq.summaryFa}"
      }
    val prompt = buildString {
      appendLine("Task: Simplify only reviewed educational content listed below. Do NOT diagnose or prescribe.")
      appendLine("Allowed citedSourceIds: ${config.approvedSourceIds.joinToString(", ")}")
      appendLine("AgeBand: ${request.ageBand?.labelFa ?: "GENERAL"}")
      appendLine("Reviewed Reference Content:")
      appendLine(approvedSnippets.ifBlank { "- [aap_fever] Always consult a clinician for red flags." })
      appendLine(AiPolicyAndGuardrails.wrapUntrustedBlock("parent_question", request.questionFa))
    }

    return executeGuarded(
      responsibility = AiResponsibility.SIMPLIFY_EDUCATIONAL_CONTENT,
      taskSchema = AiTaskSchema.DOCUMENTED_QA,
      modelId = modelId,
      requiresVision = false,
      rawUntrustedInputs = arrayOf(request.questionFa),
      userPrompt = prompt,
      jpegImageBytes = null,
      validate = { rawJson ->
        AiSemanticValidator.validateDocumentedQa(rawJson, config.approvedSourceIds)
      },
      fallbackProvider = { _ -> AiOfflineFallbacks.documentedQaFallback(request) },
    )
  }

  suspend fun prepareVisitDraft(
    request: VisitDraftRequest,
  ): AiGatewayResponse<VisitDraftStructuredOutput> {
    val modelId = request.modelId.trim()
    val timelineSummary =
      "doses24h=${request.timelineSnapshot.doseCount24h}, peakTemp=${request.timelineSnapshot.peakTemp24h ?: "NONE"}"
    val prompt = buildString {
      appendLine("Task: Summarize parent-logged details into a clear visit draft for physician or pharmacist review.")
      appendLine("Do NOT add diagnosis, prescriptions, or invented references.")
      appendLine("AgeBand: ${request.ageBandLabelFa} | WeightKg: ${request.weightKgText.ifBlank { "UNKNOWN" }}")
      appendLine("Symptom: ${request.symptomTitleFa} | TempC: ${request.temperatureCelsius ?: "UNKNOWN"}")
      appendLine("Timeline: $timelineSummary")
      appendLine(AiPolicyAndGuardrails.wrapUntrustedBlock("allergies", request.allergiesFa))
      appendLine(AiPolicyAndGuardrails.wrapUntrustedBlock("current_medications", request.currentMedsFa))
      appendLine(AiPolicyAndGuardrails.wrapUntrustedBlock("symptom_summary", request.symptomSummaryFa))
      appendLine(AiPolicyAndGuardrails.wrapUntrustedBlock("parent_note", request.parentNoteFa))
    }

    return executeGuarded(
      responsibility = AiResponsibility.PREPARE_CLINICAL_DRAFT,
      taskSchema = AiTaskSchema.VISIT_DRAFT,
      modelId = modelId,
      requiresVision = false,
      rawUntrustedInputs = arrayOf(request.allergiesFa, request.currentMedsFa, request.symptomSummaryFa, request.parentNoteFa),
      userPrompt = prompt,
      jpegImageBytes = null,
      validate = { rawJson ->
        when (val res = AiSemanticValidator.validateVisitDraft(rawJson, config.approvedSourceIds)) {
          is SemanticValidationResult.Invalid -> res
          is SemanticValidationResult.Valid -> {
            val v = res.value
            val rendered = buildString {
              appendLine(v.summaryParagraphFa)
              if (v.redFlagsObservedFa.isNotEmpty()) {
                appendLine("\nعلائم گزارش‌شده:")
                v.redFlagsObservedFa.forEach { appendLine("• $it") }
              }
              if (v.questionsForClinicianFa.isNotEmpty()) {
                appendLine("\nپرسش‌ها برای پزشک یا داروساز:")
                v.questionsForClinicianFa.forEachIndexed { idx, q ->
                  appendLine("${idx + 1}. $q")
                }
              }
              val notice = v.clinicianReviewNoticeFa.ifBlank {
                "پیش‌نویس کمکی جهت مرور پزشک/داروساز؛ جایگزین تشخیص یا نسخه نیست."
              }
              appendLine("\n$notice")
            }.trim()
            SemanticValidationResult.Valid(
              VisitDraftStructuredOutput(
                titleFa = v.titleFa,
                draftTextFa = rendered,
              ),
            )
          }
        }
      },
      fallbackProvider = { _ -> AiOfflineFallbacks.visitDraftFallback(request) },
    )
  }

  suspend fun explainNutritionEducation(
    request: NutritionEducationRequest,
  ): AiGatewayResponse<NutritionPlanAiJson> {
    val modelId = request.modelId.trim()
    val underSix = NutritionPromptBuilder.isLikelyUnderSixMonths(request.ageBand, request.ageDetailFa)
    val underTwelve = underSix || request.ageBand == AgeBand.INFANT_3_12M
    val prompt = buildString {
      appendLine("Task: Prepare educational pediatric nutrition examples matching deterministic age safety locks.")
      appendLine("Allowed citedSourceIds: ${config.approvedSourceIds.joinToString(", ")}")
      appendLine("MANDATORY LOCKS: isUnderSixMonths=$underSix (milk-only; NO water/solids/honey/cow_milk), isUnderTwelveMonths=$underTwelve (NO honey/cow_milk).")
      appendLine("AgeBand: ${request.ageBand.labelFa} (${request.ageDetailFa.ifBlank { "standard" }})")
      appendLine("AppetiteStatus: ${request.appetiteStatus.labelFa}")
      appendLine("WeightKg: ${request.weightKg ?: "UNKNOWN"} | HeightCm: ${request.heightCm ?: "UNKNOWN"}")
      appendLine("LatestTemp: ${request.timelineSnapshot.latestTempCelsius?.let { FeverDoseTimelineEngine.formatTempC(it) } ?: "NONE"}")
      appendLine(AiPolicyAndGuardrails.wrapUntrustedBlock("allergies", request.allergiesFa))
      appendLine(AiPolicyAndGuardrails.wrapUntrustedBlock("available_ingredients", request.availableIngredientsFa))
    }

    return executeGuarded(
      responsibility = AiResponsibility.EXPLAIN_DETERMINISTIC_OUTPUT,
      taskSchema = AiTaskSchema.NUTRITION_EDUCATION,
      modelId = modelId,
      requiresVision = false,
      rawUntrustedInputs = arrayOf(request.ageDetailFa, request.allergiesFa, request.availableIngredientsFa),
      userPrompt = prompt,
      jpegImageBytes = null,
      validate = { rawJson ->
        AiSemanticValidator.validateNutritionPlan(
          rawText = rawJson,
          isUnderSixMonths = underSix,
          isUnderTwelveMonths = underTwelve,
          allergiesRawFa = request.allergiesFa,
          approvedSourceIds = config.approvedSourceIds,
        )
      },
      fallbackProvider = { _ -> AiOfflineFallbacks.nutritionFallback(request) },
    )
  }

  suspend fun extractMedicationLabel(
    request: SyrupLabelOcrRequest,
  ): AiGatewayResponse<SyrupLabelAiJson> {
    val modelId = request.modelId.trim()
    if (request.jpegBytes == null || request.jpegBytes.isEmpty()) {
      val fallback = AiOfflineFallbacks.syrupOcrFallback("تصویر برچسب ارسال نشد؛ مقادیر را به‌صورت دستی ثبت و بررسی کنید.")
      recordAudit(
        responsibility = AiResponsibility.EXTRACT_MEDICATION_LABEL,
        modelId = modelId,
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
        promptCharCount = 0,
        attemptsUsed = 0,
        reasonCode = "MISSING_IMAGE_BYTES",
      )
      return AiGatewayResponse(
        data = fallback,
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
        usedFallback = true,
        modelId = modelId,
        rejectionReasonCode = "MISSING_IMAGE_BYTES",
      )
    }

    val prompt = buildString {
      appendLine("Task: Extract candidate pediatric liquid medication bottle label fields ONLY.")
      appendLine("If any field is illegible or absent on the label, output \"UNKNOWN\" (or null for numbers) and confidenceLevel=\"REQUIRE_REVIEW\". NEVER guess or prescribe doses.")
      if (request.optionalUserTextHint.isNotBlank()) {
        appendLine(AiPolicyAndGuardrails.wrapUntrustedBlock("bottle_ocr_hint", request.optionalUserTextHint))
      }
    }

    return executeGuarded(
      responsibility = AiResponsibility.EXTRACT_MEDICATION_LABEL,
      taskSchema = AiTaskSchema.SYRUP_LABEL_OCR,
      modelId = modelId,
      requiresVision = true,
      rawUntrustedInputs = arrayOf(request.optionalUserTextHint),
      userPrompt = prompt,
      jpegImageBytes = request.jpegBytes,
      validate = { rawJson ->
        AiSemanticValidator.validateSyrupLabel(rawJson, config.approvedSourceIds)
      },
      fallbackProvider = { reasonFa -> AiOfflineFallbacks.syrupOcrFallback(reasonFa) },
    )
  }

  private suspend fun <T> executeGuarded(
    responsibility: AiResponsibility,
    taskSchema: AiTaskSchema,
    modelId: String,
    requiresVision: Boolean,
    rawUntrustedInputs: Array<String>,
    userPrompt: String,
    jpegImageBytes: ByteArray?,
    validate: (String) -> SemanticValidationResult<T>,
    fallbackProvider: (String?) -> T,
  ): AiGatewayResponse<T> {
    if (!config.modelCatalog.validateModelId(modelId, requiresVision)) {
      val reasonCode = "UNSUPPORTED_MODEL_$modelId"
      recordAudit(
        responsibility = responsibility,
        modelId = modelId,
        outcome = AiGatewayOutcome.REJECTED_UNSUPPORTED_MODEL,
        verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
        promptCharCount = userPrompt.length,
        attemptsUsed = 0,
        reasonCode = reasonCode,
      )
      val notice = "مدل هوش مصنوعی انتخاب‌شده ($modelId) در فهرست مجاز نیست؛ از نسخه پشتیبان تاییدشده استفاده شد."
      return AiGatewayResponse(
        data = fallbackProvider(notice),
        outcome = AiGatewayOutcome.REJECTED_UNSUPPORTED_MODEL,
        verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
        usedFallback = true,
        modelId = modelId,
        rejectionReasonCode = reasonCode,
        userNoticeFa = notice,
      )
    }

    val inputCheck = AiPolicyAndGuardrails.evaluateUserInputs(*rawUntrustedInputs)
    if (inputCheck is InputPolicyCheckResult.Blocked) {
      recordAudit(
        responsibility = responsibility,
        modelId = modelId,
        outcome = inputCheck.outcome,
        verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
        promptCharCount = userPrompt.length,
        attemptsUsed = 0,
        reasonCode = inputCheck.reasonCode,
      )
      return AiGatewayResponse(
        data = fallbackProvider(inputCheck.userMessageFa),
        outcome = inputCheck.outcome,
        verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
        usedFallback = true,
        modelId = modelId,
        rejectionReasonCode = inputCheck.reasonCode,
        userNoticeFa = inputCheck.userMessageFa,
      )
    }

    val budgetCheck = rateLimiter.tryAcquire(userPrompt.length)
    if (budgetCheck is BudgetDecision.Denied) {
      recordAudit(
        responsibility = responsibility,
        modelId = modelId,
        outcome = budgetCheck.outcome,
        verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
        promptCharCount = userPrompt.length,
        attemptsUsed = 0,
        reasonCode = budgetCheck.reasonCode,
      )
      return AiGatewayResponse(
        data = fallbackProvider(budgetCheck.messageFa),
        outcome = budgetCheck.outcome,
        verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
        usedFallback = true,
        modelId = modelId,
        rejectionReasonCode = budgetCheck.reasonCode,
        userNoticeFa = budgetCheck.messageFa,
      )
    }

    val transportRequest =
      AiTransportRequest(
        taskSchema = taskSchema,
        responsibility = responsibility,
        modelId = modelId,
        systemInstruction = AiPolicyAndGuardrails.systemContractPrompt(responsibility),
        userPrompt = userPrompt,
        jpegImageBytes = jpegImageBytes,
      )

    val maxAttempts = 1 + config.maxRetries.coerceAtLeast(0)
    var attemptsUsed = 0
    var lastOutcome = AiGatewayOutcome.FALLBACK_API_ERROR
    var lastReasonCode = "UNKNOWN_TRANSPORT_ERROR"
    var lastMessageFa = "خطا در ارتباط با سرویس کمکی؛ پاسخ پشتیبان تاییدشده نمایش داده شد."

    for (attempt in 0 until maxAttempts) {
      attemptsUsed = attempt + 1
      try {
        val rawJson = withTimeout(config.timeoutMs) {
          transport.generateStructuredJson(transportRequest)
        }
        when (val validated = validate(rawJson)) {
          is SemanticValidationResult.Valid -> {
            recordAudit(
              responsibility = responsibility,
              modelId = modelId,
              outcome = AiGatewayOutcome.ALLOWED_VERIFIED,
              verificationState = AiVerificationState.UNVERIFIED_AI_DRAFT,
              promptCharCount = userPrompt.length,
              attemptsUsed = attemptsUsed,
              reasonCode = "VERIFIED_STRUCTURED_OUTPUT",
            )
            return AiGatewayResponse(
              data = validated.value,
              outcome = AiGatewayOutcome.ALLOWED_VERIFIED,
              verificationState = AiVerificationState.UNVERIFIED_AI_DRAFT,
              usedFallback = false,
              modelId = modelId,
            )
          }
          is SemanticValidationResult.Invalid -> {
            // Semantic / policy validation failures are NOT retried to conserve budget and fail safely.
            recordAudit(
              responsibility = responsibility,
              modelId = modelId,
              outcome = validated.outcome,
              verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
              promptCharCount = userPrompt.length,
              attemptsUsed = attemptsUsed,
              reasonCode = validated.reasonCode,
            )
            return AiGatewayResponse(
              data = fallbackProvider(validated.detailFa),
              outcome = validated.outcome,
              verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
              usedFallback = true,
              modelId = modelId,
              rejectionReasonCode = validated.reasonCode,
              userNoticeFa = validated.detailFa,
            )
          }
        }
      } catch (e: TimeoutCancellationException) {
        lastOutcome = AiGatewayOutcome.FALLBACK_TIMEOUT
        lastReasonCode = "REQUEST_TIMEOUT"
        lastMessageFa = "زمان پاسخ سرویس کمکی به پایان رسید؛ راهنمای ایمن آفلاین فعال شد."
      } catch (e: CancellationException) {
        // Strictly propagate cooperative coroutine cancellation
        throw e
      } catch (e: AiTransientTransportException) {
        lastOutcome = AiGatewayOutcome.FALLBACK_API_ERROR
        lastReasonCode = "TRANSIENT_API_ERROR"
        lastMessageFa = "سرویس کمکی موقتاً در دسترس نیست؛ راهنمای ایمن آفلاین فعال شد."
      } catch (e: Exception) {
        lastOutcome = AiGatewayOutcome.FALLBACK_API_ERROR
        lastReasonCode = "UNEXPECTED_API_ERROR"
        lastMessageFa = "خطا در سرویس کمکی؛ راهنمای ایمن آفلاین فعال شد."
      }

      if (attempt < maxAttempts - 1) {
        val backoff = config.initialRetryBackoffMs * (1L shl attempt.coerceAtMost(4))
        if (backoff > 0L) delay(backoff)
      }
    }

    recordAudit(
      responsibility = responsibility,
      modelId = modelId,
      outcome = lastOutcome,
      verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
      promptCharCount = userPrompt.length,
      attemptsUsed = attemptsUsed,
      reasonCode = lastReasonCode,
    )
    return AiGatewayResponse(
      data = fallbackProvider(lastMessageFa),
      outcome = lastOutcome,
      verificationState = AiVerificationState.SAFE_OFFLINE_FALLBACK,
      usedFallback = true,
      modelId = modelId,
      rejectionReasonCode = lastReasonCode,
      userNoticeFa = lastMessageFa,
    )
  }

  private fun recordAudit(
    responsibility: AiResponsibility,
    modelId: String,
    outcome: AiGatewayOutcome,
    verificationState: AiVerificationState,
    promptCharCount: Int,
    attemptsUsed: Int,
    reasonCode: String,
  ) {
    auditRecorder.record(
      AiAuditEntry(
        timestampMs = System.currentTimeMillis(),
        responsibility = responsibility,
        modelId = modelId,
        outcome = outcome,
        verificationState = verificationState,
        promptCharCount = promptCharCount,
        attemptsUsed = attemptsUsed,
        reasonCode = reasonCode,
      ),
    )
  }
}
