package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ChildProfileCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
  val aliasFa: String = "",
  val birthDateOrAgeFa: String = "",
  val ageBandId: String = "toddler_1_3y",
  val weightKgText: String = "",
  val weightDateFa: String = "",
  val allergiesFa: String = "",
  val currentMedsFa: String = "",
  val consentGranted: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MedicationDoseLogCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
  val medicationSlug: String = "",
  val medicationNameFa: String = "",
  val dosageFormFa: String = "",
  val volumeMlText: String = "",
  val givenAtLabelFa: String = "",
  val noteFa: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PharmacistConsultationCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
  val categoryName: String = "SYMPTOMS",
  val ageBandId: String = "toddler_1_3y",
  val questionSummaryFa: String = "",
  val hasUrgentFlag: Boolean = false,
  val consentAccepted: Boolean = false,
  val status: String = "PENDING",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ArticleBookmarkCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
  val articleSlug: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FeverLogCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
  val tempCelsius: Double = 37.0,
  val siteFa: String = "زیربغلی",
  val recordedAtLabelFa: String = "",
  val caregiverFa: String = "مادر",
  val noteFa: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DoctorVisitDraftCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
  val titleFa: String = "",
  val symptomContextFa: String = "",
  val timelineSummaryFa: String = "",
  val draftTextFa: String = "",
  val parentNoteFa: String = "",
  val modelName: String = "gemini-3.8-flash",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class VaccineRecordCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
  val vaccineId: String = "",
  val customDueDateEpochMs: Long = 0L,
  val administeredDateEpochMs: Long = 0L,
  val isDeferred: Boolean = false,
  val facilityOrLotFa: String = "",
  val noteFa: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SyrupScanCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
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
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)


