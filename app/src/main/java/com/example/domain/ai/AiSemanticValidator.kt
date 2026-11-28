package com.example.domain.ai

import com.example.domain.util.PersianNormalizer
import kotlin.math.abs
import kotlin.math.max
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

sealed interface SemanticValidationResult<out T> {
  data class Valid<T>(val value: T) : SemanticValidationResult<T>
  data class Invalid(
    val outcome: AiGatewayOutcome,
    val reasonCode: String,
    val detailFa: String,
  ) : SemanticValidationResult<Nothing>
}

/**
 * Independent semantic validator treating every Gemini model response as untrusted.
 * Checks JSON structure, clinical authority boundaries, reference fabrication,
 * mathematical label consistency, and age/allergy pediatric safety locks.
 */
object AiSemanticValidator {
  private val strictJson = Json {
    ignoreUnknownKeys = false
    isLenient = false
    coerceInputValues = false
  }

  private val URL_REGEX = Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE)
  private val DOI_REGEX = Regex("(?:doi\\s*:|10\\.\\d{4,9}/[-._;()/:A-Za-z0-9]+)", RegexOption.IGNORE_CASE)
  private val PMID_REGEX = Regex("\\bPMID\\s*:?\\s*\\d+", RegexOption.IGNORE_CASE)

  private val FORBIDDEN_CLINICAL_CLAIMS = listOf(
    Regex("تشخیص\\s+قطعی"),
    Regex("این\\s+نسخه\\s+رسمی\\s+است"),
    Regex("نیازی\\s+به\\s+(مراجعه\\s+به\\s+)?(پزشک|اورژانس|۱۱۵)\\s+نیست"),
    Regex("اورژانس\\s+(لازم|ضروری)\\s+نیست"),
    Regex("با\\s+۱۱۵\\s+تماس\\s+نگیرید"),
    Regex("برنامه\\s+درمانی\\s+به‌طور\\s+خودکار\\s+تغییر"),
    Regex("do\\s+not\\s+call\\s+(115|911|emergency)", RegexOption.IGNORE_CASE),
    Regex("confirmed\\s+diagnosis\\s*:", RegexOption.IGNORE_CASE),
    Regex("prescribed\\s+dose\\s*:", RegexOption.IGNORE_CASE),
  )

  fun <T> parseStrictJson(rawText: String, serializer: KSerializer<T>): SemanticValidationResult<T> {
    val trimmed = rawText.trim()
    if (trimmed.isEmpty() || trimmed.contains("```")) {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_MALFORMED_JSON,
        reasonCode = "MALFORMED_OR_FENCED_JSON",
        detailFa = "خروجی مدل ساختار JSON معتبر نداشت.",
      )
    }
    val element = runCatching { strictJson.parseToJsonElement(trimmed) }.getOrElse {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_MALFORMED_JSON,
        reasonCode = "JSON_SYNTAX_ERROR",
        detailFa = "خطا در خواندن ساختار JSON پاسخ هوش مصنوعی.",
      )
    }
    if (element !is JsonObject) {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_MALFORMED_JSON,
        reasonCode = "JSON_ROOT_NOT_OBJECT",
        detailFa = "ساختار JSON پاسخ باید یک شیء استاندارد باشد.",
      )
    }
    val decoded = runCatching { strictJson.decodeFromJsonElement(serializer, element) }.getOrElse {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_MALFORMED_JSON,
        reasonCode = "JSON_SCHEMA_MISMATCH",
        detailFa = "فیلدهای JSON با الگوی مجاز مطابقت نداشت.",
      )
    }
    return SemanticValidationResult.Valid(decoded)
  }

  private fun checkProhibitedClaimsAndHallucinatedRefs(
    allStrings: List<String>,
    citedSourceIds: List<String>,
    approvedSourceIds: Set<String>,
  ): SemanticValidationResult.Invalid? {
    for (text in allStrings) {
      val norm = PersianNormalizer.normalize(text)
      if (URL_REGEX.containsMatchIn(text) || DOI_REGEX.containsMatchIn(text) || PMID_REGEX.containsMatchIn(text)) {
        return SemanticValidationResult.Invalid(
          outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
          reasonCode = "HALLUCINATED_EXTERNAL_REFERENCE",
          detailFa = "پاسخ حاوی لینک یا شناسه مرجع تاییدنشده بود و مسدود شد.",
        )
      }
      for (pattern in FORBIDDEN_CLINICAL_CLAIMS) {
        if (pattern.containsMatchIn(text) || pattern.containsMatchIn(norm)) {
          return SemanticValidationResult.Invalid(
            outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
            reasonCode = "FORBIDDEN_CLINICAL_AUTHORITY_CLAIM",
            detailFa = "پاسخ شامل ادعای تشخیصی یا لغو هشدار اورژانس بود و مسدود شد.",
          )
        }
      }
    }

    val invalidIds = citedSourceIds.filter { it.isNotBlank() && it !in approvedSourceIds }
    if (invalidIds.isNotEmpty()) {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        reasonCode = "HALLUCINATED_SOURCE_ID",
        detailFa = "ارجاع علمی نامعتبر (${invalidIds.joinToString()}) در خروجی مدل رد شد.",
      )
    }
    return null
  }

  fun validateSafetyChecklist(
    rawText: String,
    approvedSourceIds: Set<String>,
  ): SemanticValidationResult<SafetyChecklistAiJson> {
    val parsed = parseStrictJson(rawText, SafetyChecklistAiJson.serializer())
    if (parsed is SemanticValidationResult.Invalid) return parsed
    val payload = (parsed as SemanticValidationResult.Valid).value

    if (payload.checklistTitleFa.isBlank() || payload.urgencySummaryFa.isBlank()) {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        reasonCode = "EMPTY_REQUIRED_CHECKLIST_FIELDS",
        detailFa = "عنوان یا خلاصه فوریت چک‌لیست خالی بود.",
      )
    }

    val texts =
      listOf(payload.checklistTitleFa, payload.urgencySummaryFa) +
        payload.immediateEmergencySignsFa +
        payload.sameDayPediatricianVisitSignsFa +
        payload.homeMonitoringChecklistFa +
        payload.questionsForPediatricianFa

    checkProhibitedClaimsAndHallucinatedRefs(texts, payload.citedSourceIds, approvedSourceIds)?.let {
      return it
    }
    return SemanticValidationResult.Valid(payload)
  }

  fun validateDocumentedQa(
    rawText: String,
    approvedSourceIds: Set<String>,
  ): SemanticValidationResult<DocumentedQaAiJson> {
    val parsed = parseStrictJson(rawText, DocumentedQaAiJson.serializer())
    if (parsed is SemanticValidationResult.Invalid) return parsed
    val payload = (parsed as SemanticValidationResult.Valid).value

    if (payload.directAnswerFa.isBlank() || payload.safetyDisclaimerFa.isBlank()) {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        reasonCode = "EMPTY_DOCUMENTED_QA_FIELDS",
        detailFa = "پاسخ مستقیم یا سلب مسئولیت ایمنی در خروجی وجود نداشت.",
      )
    }
    if (payload.citedSourceIds.isEmpty()) {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        reasonCode = "MISSING_VERIFIED_SOURCE_CITATIONS",
        detailFa = "پاسخ بدون استناد به منابع علمی تاییدشده رد شد.",
      )
    }

    val texts =
      listOf(payload.directAnswerFa, payload.safetyDisclaimerFa) +
        payload.actionableStepsFa +
        payload.whenToVisitPediatricianFa

    checkProhibitedClaimsAndHallucinatedRefs(texts, payload.citedSourceIds, approvedSourceIds)?.let {
      return it
    }
    return SemanticValidationResult.Valid(payload)
  }

  fun validateVisitDraft(
    rawText: String,
    approvedSourceIds: Set<String>,
  ): SemanticValidationResult<VisitDraftAiJson> {
    val parsed = parseStrictJson(rawText, VisitDraftAiJson.serializer())
    if (parsed is SemanticValidationResult.Invalid) return parsed
    val payload = (parsed as SemanticValidationResult.Valid).value

    if (payload.titleFa.isBlank() || payload.summaryParagraphFa.isBlank()) {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        reasonCode = "EMPTY_VISIT_DRAFT_FIELDS",
        detailFa = "عنوان یا خلاصه پیش‌نویس ویزیت ناقص بود.",
      )
    }

    val texts =
      listOf(payload.titleFa, payload.summaryParagraphFa, payload.clinicianReviewNoticeFa) +
        payload.redFlagsObservedFa +
        payload.questionsForClinicianFa

    checkProhibitedClaimsAndHallucinatedRefs(texts, emptyList(), approvedSourceIds)?.let {
      return it
    }
    return SemanticValidationResult.Valid(payload)
  }

  fun validateNutritionPlan(
    rawText: String,
    isUnderSixMonths: Boolean,
    isUnderTwelveMonths: Boolean,
    allergiesRawFa: String,
    approvedSourceIds: Set<String>,
  ): SemanticValidationResult<NutritionPlanAiJson> {
    val parsed = parseStrictJson(rawText, NutritionPlanAiJson.serializer())
    if (parsed is SemanticValidationResult.Invalid) return parsed
    val payload = (parsed as SemanticValidationResult.Valid).value

    if (payload.planTitleFa.isBlank() || payload.overviewFa.isBlank() || payload.meals.isEmpty()) {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        reasonCode = "EMPTY_NUTRITION_PLAN",
        detailFa = "ساختار برنامه تغذیه ناقص بود.",
      )
    }

    val texts =
      listOf(
        payload.planTitleFa,
        payload.overviewFa,
        payload.growthNutritionFocusFa,
        payload.hydrationTipFa,
        payload.doctorFollowUpHintFa,
      ) + payload.safetyAvoidListFa +
        payload.meals.flatMap { listOf(it.mealTypeFa, it.titleFa, it.whySuitableFa, it.textureAndPortionFa) + it.ingredientsFa + it.prepStepsFa }

    checkProhibitedClaimsAndHallucinatedRefs(texts, payload.citedSourceIds, approvedSourceIds)?.let {
      return it
    }

    val allergyTokens =
      allergiesRawFa
        .split('،', ',', '؛', '\n', '/', 'و')
        .map { PersianNormalizer.normalize(it) }
        .filter { it.length >= 2 && it !in setOf("ندارد", "ندارم", "هیچ", "none", "unknown", "نامشخص") }

    val solidOrWaterTokensUnder6m = listOf(
      "پوره", "سوپ", "فرنی", "حریره", "سرلاک", "برنج", "گوشت", "تخم مرغ",
      "ماست", "پنیر", "آب آشامیدنی", "آبمیوه", "آب میوه", "عسل", "شیر گاو",
      "solid food", "puree", "honey", "cow milk", "water",
    )
    val forbiddenUnder12m = listOf("عسل", "honey", "شیر گاو", "cow milk", "شیر پاستوریزه")

    for (meal in payload.meals) {
      val mealNorm = PersianNormalizer.normalize(
        "${meal.titleFa} ${meal.ingredientsFa.joinToString(" ")} ${meal.prepStepsFa.joinToString(" ")}"
      )
      if (isUnderSixMonths) {
        val allowsBreastOrFormulaOnly =
          mealNorm.contains("شیر مادر") || mealNorm.contains("شیر خشک") || mealNorm.contains("شیرخشک")
        val hasContradiction = solidOrWaterTokensUnder6m.any { token ->
          mealNorm.contains(PersianNormalizer.normalize(token))
        }
        if (hasContradiction || !allowsBreastOrFormulaOnly) {
          return SemanticValidationResult.Invalid(
            outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
            reasonCode = "NUTRITION_UNDER_6M_POLICY_CONTRADICTION",
            detailFa = "برنامه پیشنهادی مدل با قانون تغذیه انحصاری شیر برای زیر ۶ ماه در تناقض بود.",
          )
        }
      } else if (isUnderTwelveMonths) {
        if (forbiddenUnder12m.any { mealNorm.contains(PersianNormalizer.normalize(it)) }) {
          return SemanticValidationResult.Invalid(
            outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
            reasonCode = "NUTRITION_UNDER_12M_FORBIDDEN_FOOD",
            detailFa = "برنامه پیشنهادی شامل ماده ممنوع زیر ۱۲ ماه (عسل یا شیر گاو) بود و رد شد.",
          )
        }
      }

      if (allergyTokens.any { allergen -> mealNorm.contains(allergen) }) {
        return SemanticValidationResult.Invalid(
          outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
          reasonCode = "NUTRITION_ALLERGEN_CONTRADICTION",
          detailFa = "برنامه پیشنهادی شامل ماده آلرژی‌زا ثبت‌شده در پرونده کودک بود و مسدود شد.",
        )
      }
    }

    return SemanticValidationResult.Valid(payload)
  }

  fun validateSyrupLabel(
    rawText: String,
    approvedSourceIds: Set<String>,
  ): SemanticValidationResult<SyrupLabelAiJson> {
    val parsed = parseStrictJson(rawText, SyrupLabelAiJson.serializer())
    if (parsed is SemanticValidationResult.Invalid) return parsed
    val payload = (parsed as SemanticValidationResult.Valid).value

    val allowedIngredients = setOf("ACETAMINOPHEN", "IBUPROFEN", "CETIRIZINE", "UNKNOWN")
    val allowedForms = setOf("DROPS", "SYRUP", "SUSPENSION", "UNKNOWN")
    val allowedConfidence = setOf("HIGH", "MEDIUM", "LOW", "REQUIRE_REVIEW", "UNKNOWN")

    if (payload.activeIngredientKey !in allowedIngredients ||
      payload.dosageForm !in allowedForms ||
      payload.confidenceLevel !in allowedConfidence
    ) {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        reasonCode = "SYRUP_INVALID_ENUM_VALUE",
        detailFa = "مقادیر استاندارد ماده مؤثر یا شکل دارویی معتبر نبود.",
      )
    }

    val texts = listOf(payload.drugNameFa, payload.concentrationDisplayFa, payload.safetyNoteFa) + payload.missingFields
    checkProhibitedClaimsAndHallucinatedRefs(texts, emptyList(), approvedSourceIds)?.let {
      return it
    }

    val mg = payload.mgAmount
    val perMl = payload.perMlAmount
    val mgPerMl = payload.mgPerMl

    // Negative or impossible non-positive values (except null) are contradictory.
    if ((mg != null && (!mg.isFinite() || mg < 0.0 || mg > 2000.0)) ||
      (perMl != null && (!perMl.isFinite() || perMl <= 0.0 || perMl > 100.0)) ||
      (mgPerMl != null && (!mgPerMl.isFinite() || mgPerMl < 0.0 || mgPerMl > 1000.0))
    ) {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        reasonCode = "SYRUP_OUT_OF_RANGE_CONCENTRATION",
        detailFa = "مقادیر غلظت خارج از بازه منطقی بود و نیازمند بررسی انسانی است.",
      )
    }

    val hasCompleteNumericStrength = mg != null && mg > 0.0 && perMl != null && perMl > 0.0
    if (hasCompleteNumericStrength && mgPerMl != null && mgPerMl > 0.0) {
      val expectedRatio = mg!! / perMl!!
      val tolerance = max(0.5, expectedRatio * 0.05)
      if (abs(expectedRatio - mgPerMl) > tolerance) {
        return SemanticValidationResult.Invalid(
          outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
          reasonCode = "SYRUP_MATHEMATICAL_CONTRADICTION",
          detailFa = "نسبت میلی‌گرم بر میلی‌لیتر در خروجی مدل تناقض عددی داشت و رد شد.",
        )
      }
    }

    val hasUnknownFields =
      payload.drugNameFa.isBlank() ||
        payload.drugNameFa == "UNKNOWN" ||
        payload.activeIngredientKey == "UNKNOWN" ||
        payload.dosageForm == "UNKNOWN" ||
        !hasCompleteNumericStrength ||
        payload.missingFields.isNotEmpty()

    if (hasUnknownFields && payload.confidenceLevel == "HIGH") {
      return SemanticValidationResult.Invalid(
        outcome = AiGatewayOutcome.REJECTED_SEMANTIC_VALIDATION,
        reasonCode = "SYRUP_CONTRADICTORY_CONFIDENCE",
        detailFa = "مدل با وجود اطلاعات ناقص برچسب، اطمینان بالا گزارش کرده بود و رد شد.",
      )
    }

    // Normalize missing info strictly to UNKNOWN / REQUIRE_REVIEW so UI never infers guessed numbers
    val normalized =
      payload.copy(
        drugNameFa = payload.drugNameFa.ifBlank { "UNKNOWN" },
        confidenceLevel = if (hasUnknownFields && payload.confidenceLevel !in setOf("UNKNOWN", "REQUIRE_REVIEW")) {
          "REQUIRE_REVIEW"
        } else {
          payload.confidenceLevel
        },
        concentrationDisplayFa =
          if (hasCompleteNumericStrength) payload.concentrationDisplayFa.ifBlank { "UNKNOWN" }
          else "UNKNOWN",
        requiresHumanReview = true,
      )

    return SemanticValidationResult.Valid(normalized)
  }
}
