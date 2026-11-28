package com.example.domain.ai

import com.example.data.ManoSourcesAndClinicalSeed
import com.example.domain.model.AgeBand
import com.example.domain.model.FaqItem
import com.example.domain.model.MilestoneItem
import com.example.domain.model.NutritionAppetiteStatus
import com.example.domain.model.SymptomFlow
import com.example.domain.model.TriageOutcome
import com.example.domain.util.FeverDoseTimelineSnapshot
import kotlinx.serialization.Serializable

/**
 * Explicitly enumerated responsibilities permitted for Gemini inside MANO Kid.
 * Gemini is strictly an assistant, never a clinical authority.
 */
enum class AiResponsibility(val code: String) {
  SIMPLIFY_EDUCATIONAL_CONTENT("SIMPLIFY_EDUCATIONAL_CONTENT"),
  SUMMARIZE_USER_INFO("SUMMARIZE_USER_INFO"),
  EXTRACT_MEDICATION_LABEL("EXTRACT_MEDICATION_LABEL"),
  PREPARE_CLINICAL_DRAFT("PREPARE_CLINICAL_DRAFT"),
  EXPLAIN_DETERMINISTIC_OUTPUT("EXPLAIN_DETERMINISTIC_OUTPUT"),
}

/**
 * Actions permanently prohibited for the AI layer.
 */
enum class AiProhibitedAction(val code: String) {
  INDEPENDENT_DIAGNOSIS("INDEPENDENT_DIAGNOSIS"),
  INDEPENDENT_PRESCRIBING_OR_DOSING("INDEPENDENT_PRESCRIBING_OR_DOSING"),
  OVERRIDE_URGENT_ESCALATION("OVERRIDE_URGENT_ESCALATION"),
  FABRICATE_REFERENCES("FABRICATE_REFERENCES"),
  AUTO_MODIFY_TREATMENT_PLAN("AUTO_MODIFY_TREATMENT_PLAN"),
}

enum class AiVerificationState(val labelFa: String) {
  UNVERIFIED_AI_DRAFT("پیش‌نویس کمکی هوش مصنوعی — نیازمند تأیید والد/داروساز"),
  DETERMINISTIC_EMERGENCY_GUIDANCE("هشدار قطعی موتور بالینی — بدون دخالت هوش مصنوعی"),
  SAFE_OFFLINE_FALLBACK("پاسخ ایمن پشتیبان (مبتنی بر راهنماهای تاییدشده بدون هوش مصنوعی)"),
}

enum class AiGatewayOutcome(val code: String) {
  ALLOWED_VERIFIED("ALLOWED_VERIFIED"),
  EMERGENCY_BYPASS("EMERGENCY_BYPASS"),
  REJECTED_PROMPT_INJECTION("REJECTED_PROMPT_INJECTION"),
  REJECTED_PROHIBITED_INTENT("REJECTED_PROHIBITED_INTENT"),
  REJECTED_UNSUPPORTED_MODEL("REJECTED_UNSUPPORTED_MODEL"),
  REJECTED_RATE_LIMIT("REJECTED_RATE_LIMIT"),
  REJECTED_BUDGET("REJECTED_BUDGET"),
  REJECTED_MALFORMED_JSON("REJECTED_MALFORMED_JSON"),
  REJECTED_SEMANTIC_VALIDATION("REJECTED_SEMANTIC_VALIDATION"),
  FALLBACK_API_ERROR("FALLBACK_API_ERROR"),
  FALLBACK_TIMEOUT("FALLBACK_TIMEOUT"),
}

/**
 * Configurable catalog of supported Gemini model identifiers.
 * Deprecated or unapproved model IDs are rejected before any request leaves the gateway.
 */
data class ModelCatalog(
  val defaultTextModel: String = DEFAULT_TEXT_MODEL,
  val defaultVisionModel: String = DEFAULT_VISION_MODEL,
  val supportedTextModels: Set<String> = DEFAULT_SUPPORTED_TEXT_MODELS,
  val supportedVisionModels: Set<String> = DEFAULT_SUPPORTED_VISION_MODELS,
) {
  fun isSupportedTextModel(modelId: String): Boolean =
    modelId.trim() in supportedTextModels

  fun isSupportedVisionModel(modelId: String): Boolean =
    modelId.trim() in supportedVisionModels

  fun validateModelId(modelId: String, requiresVision: Boolean): Boolean =
    if (requiresVision) isSupportedVisionModel(modelId) else isSupportedTextModel(modelId)

  companion object {
    const val DEFAULT_TEXT_MODEL = "gemini-3.8-flash"
    const val DEFAULT_LITE_MODEL = "gemini-3.5-flash-lite"
    const val DEFAULT_VISION_MODEL = "gemini-3.8-flash"

    val DEFAULT_SUPPORTED_TEXT_MODELS: Set<String> = setOf(
      "gemini-3.8-flash",
      "gemini-3.5-flash-lite",
      "gemini-4-argon",
    )

    val DEFAULT_SUPPORTED_VISION_MODELS: Set<String> = setOf(
      "gemini-3.8-flash",
      "gemini-3.5-flash-lite",
      "gemini-4-argon",
    )

    val APPROVED_SOURCE_IDS: Set<String> =
      ManoSourcesAndClinicalSeed.sources.map { it.id }.toSet()
  }
}

data class AiGatewayConfig(
  val modelCatalog: ModelCatalog = ModelCatalog(),
  val timeoutMs: Long = 45_000L,
  val maxRetries: Int = 1,
  val initialRetryBackoffMs: Long = 250L,
  val rateLimitWindowMs: Long = 60_000L,
  val maxRequestsPerWindow: Int = 12,
  val maxSessionRequests: Int = 40,
  val maxSessionInputChars: Int = 120_000,
  val maxSinglePromptChars: Int = 12_000,
  val maxAuditEntries: Int = 200,
  val approvedSourceIds: Set<String> = ModelCatalog.APPROVED_SOURCE_IDS,
)

enum class AiTaskSchema {
  SAFETY_CHECKLIST,
  DOCUMENTED_QA,
  VISIT_DRAFT,
  NUTRITION_EDUCATION,
  SYRUP_LABEL_OCR,
}

data class AiTransportRequest(
  val taskSchema: AiTaskSchema,
  val responsibility: AiResponsibility,
  val modelId: String,
  val systemInstruction: String,
  val userPrompt: String,
  val jpegImageBytes: ByteArray? = null,
) {
  val hasImage: Boolean get() = jpegImageBytes != null && jpegImageBytes.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AiTransportRequest) return false
    return taskSchema == other.taskSchema &&
      responsibility == other.responsibility &&
      modelId == other.modelId &&
      systemInstruction == other.systemInstruction &&
      userPrompt == other.userPrompt &&
      jpegImageBytes.contentEquals(other.jpegImageBytes)
  }

  override fun hashCode(): Int {
    var result = taskSchema.hashCode()
    result = 31 * result + responsibility.hashCode()
    result = 31 * result + modelId.hashCode()
    result = 31 * result + systemInstruction.hashCode()
    result = 31 * result + userPrompt.hashCode()
    result = 31 * result + (jpegImageBytes?.contentHashCode() ?: 0)
    return result
  }
}

/**
 * Retryable infrastructure failure from the underlying transport (network/5xx/rate-limit).
 */
class AiTransientTransportException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)

interface AiGatewayTransport {
  suspend fun generateStructuredJson(request: AiTransportRequest): String
}

data class AiAuditEntry(
  val timestampMs: Long,
  val responsibility: AiResponsibility,
  val modelId: String,
  val outcome: AiGatewayOutcome,
  val verificationState: AiVerificationState,
  val promptCharCount: Int,
  val attemptsUsed: Int,
  val reasonCode: String,
)

data class AiGatewayResponse<T>(
  val data: T,
  val outcome: AiGatewayOutcome,
  val verificationState: AiVerificationState,
  val usedFallback: Boolean,
  val modelId: String,
  val rejectionReasonCode: String? = null,
  val userNoticeFa: String? = null,
)

// ==================== Structured Wire Schemas (kotlinx.serialization) ====================

@Serializable
data class SafetyChecklistAiJson(
  val checklistTitleFa: String,
  val urgencySummaryFa: String,
  val immediateEmergencySignsFa: List<String> = emptyList(),
  val sameDayPediatricianVisitSignsFa: List<String> = emptyList(),
  val homeMonitoringChecklistFa: List<String> = emptyList(),
  val questionsForPediatricianFa: List<String> = emptyList(),
  val citedSourceIds: List<String> = emptyList(),
)

@Serializable
data class DocumentedQaAiJson(
  val directAnswerFa: String,
  val actionableStepsFa: List<String> = emptyList(),
  val whenToVisitPediatricianFa: List<String> = emptyList(),
  val citedSourceIds: List<String> = emptyList(),
  val safetyDisclaimerFa: String,
)

@Serializable
data class VisitDraftAiJson(
  val titleFa: String,
  val summaryParagraphFa: String,
  val redFlagsObservedFa: List<String> = emptyList(),
  val questionsForClinicianFa: List<String> = emptyList(),
  val clinicianReviewNoticeFa: String = "",
)

@Serializable
data class NutritionMealAiJson(
  val mealTypeFa: String,
  val titleFa: String,
  val whySuitableFa: String = "",
  val textureAndPortionFa: String = "",
  val ingredientsFa: List<String> = emptyList(),
  val prepStepsFa: List<String> = emptyList(),
)

@Serializable
data class NutritionPlanAiJson(
  val planTitleFa: String,
  val overviewFa: String,
  val growthNutritionFocusFa: String,
  val meals: List<NutritionMealAiJson> = emptyList(),
  val hydrationTipFa: String,
  val safetyAvoidListFa: List<String> = emptyList(),
  val doctorFollowUpHintFa: String,
  val citedSourceIds: List<String> = emptyList(),
)

@Serializable
data class SyrupLabelAiJson(
  val drugNameFa: String,
  val activeIngredientKey: String = "UNKNOWN",
  val dosageForm: String = "UNKNOWN",
  val mgAmount: Double? = null,
  val perMlAmount: Double? = null,
  val mgPerMl: Double? = null,
  val confidenceLevel: String = "REQUIRE_REVIEW",
  val concentrationDisplayFa: String = "UNKNOWN",
  val safetyNoteFa: String = "",
  val missingFields: List<String> = emptyList(),
  val requiresHumanReview: Boolean = true,
)

// ==================== Domain Gateway Request Contexts ====================

data class SafetyChecklistRequest(
  val primaryFlow: SymptomFlow,
  val coSelectedSymptomsFa: List<String>,
  val ageBand: AgeBand?,
  val ageMonthsExact: Int?,
  val temperatureCelsius: Float?,
  val durationHours: Int?,
  val selectedRedFlagIds: Set<String>,
  val triageOutcome: TriageOutcome?,
  val modelId: String = ModelCatalog.DEFAULT_TEXT_MODEL,
)

data class DocumentedQaRequest(
  val questionFa: String,
  val ageBand: AgeBand?,
  val relatedFaqs: List<FaqItem>,
  val modelId: String = ModelCatalog.DEFAULT_TEXT_MODEL,
)

data class VisitDraftRequest(
  val childAliasFa: String,
  val ageBandLabelFa: String,
  val weightKgText: String,
  val allergiesFa: String,
  val currentMedsFa: String,
  val symptomTitleFa: String,
  val symptomSummaryFa: String,
  val temperatureCelsius: Float?,
  val timelineSnapshot: FeverDoseTimelineSnapshot,
  val parentNoteFa: String,
  val modelId: String = ModelCatalog.DEFAULT_TEXT_MODEL,
)

data class NutritionEducationRequest(
  val ageBand: AgeBand,
  val ageDetailFa: String,
  val weightKg: Double?,
  val heightCm: Double?,
  val allergiesFa: String,
  val appetiteStatus: NutritionAppetiteStatus,
  val availableIngredientsFa: String,
  val checkedMilestones: List<MilestoneItem>,
  val timelineSnapshot: FeverDoseTimelineSnapshot,
  val modelId: String = ModelCatalog.DEFAULT_TEXT_MODEL,
)

data class SyrupLabelOcrRequest(
  val jpegBytes: ByteArray?,
  val optionalUserTextHint: String = "",
  val modelId: String = ModelCatalog.DEFAULT_VISION_MODEL,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SyrupLabelOcrRequest) return false
    return jpegBytes.contentEquals(other.jpegBytes) &&
      optionalUserTextHint == other.optionalUserTextHint &&
      modelId == other.modelId
  }

  override fun hashCode(): Int {
    var result = jpegBytes?.contentHashCode() ?: 0
    result = 31 * result + optionalUserTextHint.hashCode()
    result = 31 * result + modelId.hashCode()
    return result
  }
}

data class VisitDraftStructuredOutput(
  val titleFa: String,
  val draftTextFa: String,
)
