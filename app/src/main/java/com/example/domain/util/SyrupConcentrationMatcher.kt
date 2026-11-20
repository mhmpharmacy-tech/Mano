package com.example.domain.util

import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.GeminiSyrupScanPayload
import com.example.domain.model.SyrupDosageForm
import com.example.domain.model.SyrupIngredientKey
import com.example.domain.model.SyrupScanConfidence
import com.example.ui.components.AlertSeverity
import java.util.Locale
import kotlin.math.abs

data class ReconciledSyrupExtraction(
  val drugNameFa: String,
  val ingredientKey: SyrupIngredientKey,
  val dosageForm: SyrupDosageForm,
  val mgAmount: Double,
  val perMlAmount: Double,
  val mgPerMl: Double,
  val matchedFormulation: LiquidFormulationId?,
  val matchedFormulationIdRaw: String,
  val confidence: SyrupScanConfidence,
  val concentrationDisplayFa: String,
  val safetyNoteFa: String,
  val mixUpWarningFa: String?,
  val warningSeverity: AlertSeverity,
  val canAutoFillSyringe: Boolean,
  val dosePreview: LiquidDoseVisualizationResult?,
)

object SyrupConcentrationMatcher {

  const val CUSTOM_UNVERIFIED_ID = "CUSTOM_UNVERIFIED"

  fun reconcileFromAi(
    payload: GeminiSyrupScanPayload,
    childProfile: ChildProfileDemo? = null,
    birthDateEpochMs: Long? = null,
  ): ReconciledSyrupExtraction {
    val ingredient =
      SyrupIngredientKey.entries.firstOrNull { it.name == payload.activeIngredientKey.uppercase(Locale.US) }
        ?: SyrupIngredientKey.UNKNOWN
    val form =
      SyrupDosageForm.entries.firstOrNull { it.name == payload.dosageForm.uppercase(Locale.US) }
        ?: SyrupDosageForm.UNKNOWN
    val confidence =
      SyrupScanConfidence.entries.firstOrNull { it.name == payload.confidenceLevel.uppercase(Locale.US) }
        ?: SyrupScanConfidence.LOW

    // Missing OCR fields must never turn into a plausible concentration via a preset or a default.
    val effectiveMg = payload.mgAmount.takeIf { it.isFinite() && it in 0.000001..2000.0 } ?: 0.0
    val effectivePerMl = payload.perMlAmount.takeIf { it.isFinite() && it in 0.000001..100.0 } ?: 0.0
    val measuredRatio = if (effectiveMg > 0.0 && effectivePerMl > 0.0) effectiveMg / effectivePerMl else 0.0
    val separatelyReadRatio = payload.mgPerMl.takeIf { it.isFinite() && it > 0.0 }
    val inconsistentOcr = separatelyReadRatio != null &&
      (measuredRatio <= 0.0 ||
        kotlin.math.abs(separatelyReadRatio - measuredRatio) >
          maxOf(0.02, measuredRatio * 0.005))
    val presetHint = LiquidFormulationId.entries.firstOrNull { it.id == payload.matchedFormulationId }

    return reconcileEditable(
      drugNameFa = payload.detectedDrugNameFa.trim().ifBlank { "داروی مایع اسکن‌شده" },
      ingredientKey = ingredient,
      dosageForm = form,
      mgAmount = effectiveMg,
      perMlAmount = effectivePerMl,
      confidence = if (inconsistentOcr) SyrupScanConfidence.LOW else confidence,
      aiSafetyNoteFa = if (inconsistentOcr) {
        "دو عدد غلظت خوانده‌شده از برچسب با هم تناقض دارند؛ ورود دستی و بررسی داروساز لازم است."
      } else payload.safetyNoteFa,
      presetOverride = presetHint,
      isPediatricFeverOrAllergyLiquid = payload.isPediatricFeverOrAllergyLiquid || presetHint != null,
      childProfile = childProfile,
      birthDateEpochMs = birthDateEpochMs,
    )
  }

  fun reconcileEditable(
    drugNameFa: String,
    ingredientKey: SyrupIngredientKey,
    dosageForm: SyrupDosageForm,
    mgAmount: Double,
    perMlAmount: Double,
    confidence: SyrupScanConfidence = SyrupScanConfidence.HIGH,
    aiSafetyNoteFa: String = "",
    presetOverride: LiquidFormulationId? = null,
    isPediatricFeverOrAllergyLiquid: Boolean = true,
    childProfile: ChildProfileDemo? = null,
    birthDateEpochMs: Long? = null,
  ): ReconciledSyrupExtraction {
    // Reject invalid data rather than clamping it into a believable (and potentially dangerous) dose.
    val validConcentration = mgAmount.isFinite() && perMlAmount.isFinite() &&
      mgAmount > 0.0 && mgAmount <= 2000.0 && perMlAmount > 0.0 && perMlAmount <= 100.0
    val cleanMg = if (validConcentration) mgAmount else 0.0
    val cleanPerMl = if (validConcentration) perMlAmount else 0.0
    val computedMgPerMl = if (validConcentration) cleanMg / cleanPerMl else 0.0

    val independentlyMatched = if (validConcentration) {
      matchCalibratedFormulation(ingredientKey, dosageForm, computedMgPerMl)
    } else null
    // OCR and persisted preset IDs are untrusted hints: they must agree with drug, form and strength.
    val matchedFormulation = independentlyMatched?.takeIf {
      presetOverride == null || presetOverride == it
    }

    val ageMonths = LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(childProfile, birthDateEpochMs)
    val weightKg = LiquidDoseVisualizerEngine.initialWeightKgFromProfile(childProfile)

    val dosePreview =
      if (matchedFormulation != null && isPediatricFeverOrAllergyLiquid) {
        LiquidDoseVisualizerEngine.evaluate(
          formulation = matchedFormulation,
          ageMonths = ageMonths,
          weightKg = weightKg,
        )
      } else {
        null
      }

    val mixUpWarningFa =
      when {
        matchedFormulation == LiquidFormulationId.ACETAMINOPHEN_DROPS_100 ||
          (ingredientKey == SyrupIngredientKey.ACETAMINOPHEN && computedMgPerMl >= 80.0) ->
          "هشدار تداخل غلظت قطره و شربت: این بطری غلظت بسیار بالا (~۱۰۰ mg/mL) دارد که حدود ۴ برابر شربت ۱۲۰/۵ است! فقط با قطره‌چکان یا سرنگ کوچک ۱ تا ۳ mL استفاده کنید."
        matchedFormulation == LiquidFormulationId.ACETAMINOPHEN_SYRUP_160_5 ->
          "توجه به غلظت دوگانه: این بطری ۱۶۰ mg / 5 mL است (غلیظ‌تر از شربت متداول ۱۲۰/۵)؛ حجم سرنگ دقیقاً بر پایه ۱۶۰/۵ تنظیم می‌شود."
        matchedFormulation == null || !isPediatricFeverOrAllergyLiquid ->
          "ماده مؤثره، شکل دارویی یا غلظت با فهرست کالیبره مانو تطابق قطعی ندارد؛ محاسبه و پرکردن خودکار سرنگ مسدود است. برچسب را بررسی و با داروساز مشورت کنید."
        dosageForm == SyrupDosageForm.SUSPENSION ->
          "سوسپانسیون خوراکی: پیش از کشیدن دارو با سرنگ، بطری را حداقل ۲۰ ثانیه کامل تکان دهید."
        else -> null
      }

    val severity =
      when {
        dosePreview?.alertSeverity == AlertSeverity.DANGER -> AlertSeverity.DANGER
        matchedFormulation == LiquidFormulationId.ACETAMINOPHEN_DROPS_100 ||
          (ingredientKey == SyrupIngredientKey.ACETAMINOPHEN && computedMgPerMl >= 80.0) -> AlertSeverity.DANGER
        matchedFormulation == null || !isPediatricFeverOrAllergyLiquid -> AlertSeverity.CAUTION
        matchedFormulation == LiquidFormulationId.ACETAMINOPHEN_SYRUP_160_5 -> AlertSeverity.CAUTION
        confidence == SyrupScanConfidence.LOW -> AlertSeverity.CAUTION
        else -> AlertSeverity.SUCCESS
      }

    val concentrationDisplay = formatConcentrationDisplay(cleanMg, cleanPerMl)
    val defaultSafety =
      aiSafetyNoteFa.trim().ifBlank {
        matchedFormulation?.safetyTipFa
          ?: "پیش از مصرف، عدد روی برچسب اصلی بطری را دوباره با چشم خود تطبیق دهید."
      }

    val canAutoFill =
      matchedFormulation != null &&
        validConcentration &&
        confidence != SyrupScanConfidence.LOW &&
        dosePreview != null &&
        !dosePreview.isBlocked &&
        dosePreview.recommendedMl > 0f

    return ReconciledSyrupExtraction(
      drugNameFa = drugNameFa.trim().ifBlank { matchedFormulation?.shortLabelFa ?: "داروی اسکن‌شده" }.take(140),
      ingredientKey = ingredientKey,
      dosageForm = dosageForm,
      mgAmount = cleanMg,
      perMlAmount = cleanPerMl,
      mgPerMl = computedMgPerMl,
      matchedFormulation = matchedFormulation,
      matchedFormulationIdRaw = matchedFormulation?.id ?: CUSTOM_UNVERIFIED_ID,
      confidence = confidence,
      concentrationDisplayFa = concentrationDisplay,
      safetyNoteFa = defaultSafety.take(400),
      mixUpWarningFa = mixUpWarningFa,
      warningSeverity = severity,
      canAutoFillSyringe = canAutoFill,
      dosePreview = dosePreview,
    )
  }

  /**
   * A recognized dose is usable only for the same ingredient, oral form and label strength.
   * Tight 0.5% numeric tolerance accommodates OCR decimal noise without approving 18 vs 20 mg/mL.
   */
  fun matchCalibratedFormulation(
    ingredientKey: SyrupIngredientKey,
    dosageForm: SyrupDosageForm,
    mgPerMl: Double,
  ): LiquidFormulationId? {
    if (!mgPerMl.isFinite() || mgPerMl <= 0.0 || dosageForm == SyrupDosageForm.UNKNOWN) return null
    return LiquidFormulationId.entries.firstOrNull { formulation ->
      val expectedIngredient = when (formulation) {
        LiquidFormulationId.ACETAMINOPHEN_DROPS_100,
        LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5,
        LiquidFormulationId.ACETAMINOPHEN_SYRUP_160_5 -> SyrupIngredientKey.ACETAMINOPHEN
        LiquidFormulationId.IBUPROFEN_SYRUP_100_5 -> SyrupIngredientKey.IBUPROFEN
        LiquidFormulationId.CETIRIZINE_SYRUP_5_5 -> SyrupIngredientKey.CETIRIZINE
      }
      val matchingForm = when (formulation) {
        LiquidFormulationId.ACETAMINOPHEN_DROPS_100 -> dosageForm == SyrupDosageForm.DROPS
        LiquidFormulationId.CETIRIZINE_SYRUP_5_5 -> dosageForm == SyrupDosageForm.SYRUP
        else -> dosageForm == SyrupDosageForm.SYRUP || dosageForm == SyrupDosageForm.SUSPENSION
      }
      ingredientKey == expectedIngredient && matchingForm &&
        abs(mgPerMl - formulation.mgPerMl) <= maxOf(0.02, formulation.mgPerMl * 0.005)
    }
  }

  fun defaultNumeratorMg(formulation: LiquidFormulationId): Double =
    when (formulation) {
      LiquidFormulationId.ACETAMINOPHEN_DROPS_100 -> 100.0
      LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5 -> 120.0
      LiquidFormulationId.ACETAMINOPHEN_SYRUP_160_5 -> 160.0
      LiquidFormulationId.IBUPROFEN_SYRUP_100_5 -> 100.0
      LiquidFormulationId.CETIRIZINE_SYRUP_5_5 -> 5.0
    }

  fun defaultDenominatorMl(formulation: LiquidFormulationId): Double =
    when (formulation) {
      LiquidFormulationId.ACETAMINOPHEN_DROPS_100 -> 1.0
      else -> 5.0
    }

  fun formatConcentrationDisplay(mgAmount: Double, perMlAmount: Double): String {
    val mgStr =
      if (mgAmount % 1.0 == 0.0) mgAmount.toInt().toString()
      else String.format(Locale.US, "%.1f", mgAmount)
    val mlStr =
      if (perMlAmount % 1.0 == 0.0) perMlAmount.toInt().toString()
      else String.format(Locale.US, "%.1f", perMlAmount)
    return "$mgStr mg / $mlStr mL"
  }
}
