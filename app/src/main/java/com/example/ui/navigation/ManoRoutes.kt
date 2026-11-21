package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute

@Serializable data class TopicsRoute(val categoryId: String? = null, val query: String = "")

@Serializable data class TopicDetailRoute(val slug: String)

@Serializable data class SymptomsRoute(val symptomId: String = "fever")

@Serializable data object MedicinesRoute

@Serializable data class MedicineDetailRoute(val slug: String)

@Serializable data object GrowthRoute

@Serializable data object NutritionRoute

@Serializable data object VaccinesRoute

@Serializable data object SkinRoute

@Serializable data object DevicesRoute

@Serializable
data class Pediatric3DStudioRoute(
  val initialScene: String = "workbench",
  val prefillMl: Float = 0f,
  val prefillFever: Boolean = false,
  val prefillFormulationId: String = "",
  val prefillScannedDrugFa: String = "",
)

@Serializable
data class FeverDoseCalculatorRoute(
  val prefillFormulationId: String = "",
  val prefillScannedDrugFa: String = "",
)

@Serializable data object SyrupScannerRoute

@Serializable data class FaqRoute(val query: String = "")

@Serializable
data class AskPharmacistRoute(
  val categoryId: String? = null,
  val initialMode: String = "parent",
  val referralId: String? = null,
  val invitedPharmacyId: String? = null,
)

@Serializable data object MyChildRoute

@Serializable data class InfoRoute(val initialSection: String = "about")

@Serializable data object CareCardsRoute
