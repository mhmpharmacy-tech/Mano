package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object CareCardsRoute

@Serializable
data object SupplementsHygieneRoute

@Serializable
data object MedicinesRoute

@Serializable
data object GrowthRoute

@Serializable
data object VaccinesRoute

@Serializable
data object NutritionRoute

@Serializable
data object SkinRoute

@Serializable
data object DevicesRoute

@Serializable
data object SyrupScannerRoute

@Serializable
data object MyChildRoute

@Serializable
data object TreatmentPlanRoute

@Serializable
data class TopicsRoute(
  val categoryId: String? = null,
  val query: String = "",
)

@Serializable
data class TopicDetailRoute(
  val slug: String,
)

@Serializable
data class SymptomsRoute(
  val symptomId: String = "",
)

@Serializable
data class MedicineDetailRoute(
  val slug: String,
)

@Serializable
data class FeverDoseCalculatorRoute(
  val prefillFormulationId: String? = null,
  val prefillScannedDrugFa: String? = null,
)

@Serializable
data class Pediatric3DStudioRoute(
  val prefillFormulationId: String? = null,
  val prefillScannedDrugFa: String? = null,
)

@Serializable
data class FaqRoute(
  val query: String = "",
)

@Serializable
data class AskPharmacistRoute(
  val categoryId: String? = null,
  val referralId: String? = null,
  val initialMode: String = "parent",
  val invitedPharmacyId: String? = null,
)

@Serializable
data class InfoRoute(
  val initialSection: String = "about",
)
