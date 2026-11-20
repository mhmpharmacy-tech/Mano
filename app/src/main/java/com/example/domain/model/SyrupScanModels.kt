package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SyrupIngredientKey(val labelFa: String) {
  ACETAMINOPHEN("استامینوفن (Paracetamol)"),
  IBUPROFEN("ایبوپروفن (Ibuprofen)"),
  CETIRIZINE("سیتیریزین (Cetirizine)"),
  UNKNOWN("داروی اختصاصی / سایر"),
}

@Serializable
enum class SyrupDosageForm(val labelFa: String) {
  DROPS("قطره خوراکی (Drops)"),
  SYRUP("شربت (Syrup)"),
  SUSPENSION("سوسپانسیون (Suspension)"),
  UNKNOWN("سایر / نامشخص"),
}

@Serializable
enum class SyrupScanConfidence(val labelFa: String) {
  HIGH("اطمینان بالا"),
  MEDIUM("اطمینان متوسط"),
  LOW("نیاز به بررسی دقیق"),
}

@Serializable
data class GeminiSyrupScanPayload(
  val detectedDrugNameFa: String = "",
  val activeIngredientKey: String = "UNKNOWN",
  val dosageForm: String = "UNKNOWN",
  val mgAmount: Double = 0.0,
  val perMlAmount: Double = 0.0,
  val mgPerMl: Double = 0.0,
  val matchedFormulationId: String = "CUSTOM_UNVERIFIED",
  val confidenceLevel: String = "LOW",
  val concentrationDisplayFa: String = "",
  val safetyNoteFa: String = "",
  val isPediatricFeverOrAllergyLiquid: Boolean = false,
)

@Serializable
data class SavedSyrupScan(
  val id: String = "",
  val drugNameFa: String = "",
  val activeIngredientKey: String = "UNKNOWN",
  val dosageForm: String = "UNKNOWN",
  val mgAmount: Double = 120.0,
  val perMlAmount: Double = 5.0,
  val mgPerMl: Double = 24.0,
  val matchedFormulationId: String = "CUSTOM_UNVERIFIED",
  val confidenceLevel: String = "MEDIUM",
  val concentrationDisplayFa: String = "",
  val safetyNoteFa: String = "",
  val confirmedByParent: Boolean = false,
  val createdAtMs: Long = 0L,
  val updatedAtMs: Long = 0L,
  val isSyncedCloud: Boolean = false,
)
