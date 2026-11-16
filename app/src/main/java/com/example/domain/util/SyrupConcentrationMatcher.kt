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

    val safePerMl = payload.perMlAmount.takeIf { it > 0.0 && it <= 100.0 } ?: 5.0
    val safeMg = payload.mgAmount.takeIf { it > 0.0 && it <= 2000.0 }
      ?: (payload.mgPerMl.takeIf { it > 0.0 }?.let { it * safePerMl } ?: 0.0)

    val presetHint =
      LiquidFormulationId.entries.firstOrNull { it.id == payload.matchedFormulationId }

    val effectiveMg =
      when {
        safeMg > 0.0 -> safeMg
        presetHint != null -> defaultNumeratorMg(presetHint)
        else -> 120.0
      }
    val effectivePerMl =
      when {
        safeMg > 0.0 -> safePerMl
        presetHint != null -> defaultDenominatorMl(presetHint)
        else -> 5.0
      }

    return reconcileEditable(
      drugNameFa = payload.detectedDrugNameFa.trim().ifBlank { "داروی مایع اسکن‌شده" },
      ingredientKey = ingredient,
      dosageForm = form,
      mgAmount = effectiveMg,
      perMlAmount = effectivePerMl,
      confidence = confidence,
      aiSafetyNoteFa = payload.safetyNoteFa,
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
    val cleanMg = mgAmount.coerceIn(1.0, 2000.0)
    val cleanPerMl = perMlAmount.coerceIn(0.5, 100.0)
    val computedMgPerMl = ((cleanMg / cleanPerMl) * 100.0).toInt() / 100.0

    val matchedFormulation =
      presetOverride
        ?: matchCalibratedFormulation(
          ingredientKey = ingredientKey,
          dosageForm = dosageForm,
          mgPerMl = computedMgPerMl,
        )

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
          "دارو یا غلظت خارج از کاتالوگ کالیبره‌شده مانو است؛ محاسبه خودکار سرنگ (mg/kg) غیرفعال شده است. پیش از خوراندن با پزشک یا داروساز مشورت کنید."
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

  fun matchCalibratedFormulation(
    ingredientKey: SyrupIngredientKey,
    dosageForm: SyrupDosageForm,
    mgPerMl: Double,
  ): LiquidFormulationId? {
    return when (ingredientKey) {
      SyrupIngredientKey.ACETAMINOPHEN ->
        when {
          abs(mgPerMl - 100.0) <= 5.0 -> LiquidFormulationId.ACETAMINOPHEN_DROPS_100
          abs(mgPerMl - 24.0) <= 2.0 -> LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5
          abs(mgPerMl - 32.0) <= 2.0 -> LiquidFormulationId.ACETAMINOPHEN_SYRUP_160_5
          dosageForm == SyrupDosageForm.DROPS && mgPerMl >= 80.0 -> LiquidFormulationId.ACETAMINOPHEN_DROPS_100
          else -> null
        }
      SyrupIngredientKey.IBUPROFEN ->
        if (abs(mgPerMl - 20.0) <= 2.0) LiquidFormulationId.IBUPROFEN_SYRUP_100_5 else null
      SyrupIngredientKey.CETIRIZINE ->
        if (abs(mgPerMl - 1.0) <= 0.25) LiquidFormulationId.CETIRIZINE_SYRUP_5_5 else null
      SyrupIngredientKey.UNKNOWN -> null
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
