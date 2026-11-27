package com.example.data

import android.util.Log
import androidx.annotation.Keep
import com.example.domain.model.ChildProfileDemo
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable

enum class OperationType {
  GET,
  LIST,
  CREATE,
  UPDATE,
  WRITE,
  DELETE,
}

fun handleFirestoreError(
  error: Exception,
  operationType: OperationType,
  path: String,
) {
  Log.e("ManoFirestore", "Firestore $operationType failed at $path: ${error.localizedMessage}", error)
}

@Keep
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
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

@Keep
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
  val administeredAtMs: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FeverLogCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
  val tempCelsius: Double = 37.0,
  val siteFa: String = "زیربغلی",
  val recordedAtLabelFa: String = "",
  val caregiverFa: String = "والد",
  val noteFa: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

@Keep
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
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ArticleBookmarkCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
  val articleSlug: String = "",
  val createdAt: Timestamp? = null,
)

@Keep
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
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

@Keep
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
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SyrupScanCloud(
  @DocumentId val id: String = "",
  val userId: String = "",
  val drugNameFa: String = "",
  val activeIngredientKey: String = "UNKNOWN",
  val dosageForm: String = "UNKNOWN",
  val mgAmount: Double = 0.0,
  val perMlAmount: Double = 5.0,
  val mgPerMl: Double = 0.0,
  val matchedFormulationId: String = "CUSTOM_UNVERIFIED",
  val confidenceLevel: String = "MEDIUM",
  val concentrationDisplayFa: String = "",
  val safetyNoteFa: String = "",
  val confirmedByParent: Boolean = false,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
)

@Serializable
data class ScopedPendingChildProfile(
  val profile: ChildProfileDemo,
  val pendingCloudSync: Boolean = true,
  val savedAtEpochMs: Long = System.currentTimeMillis(),
)

@Serializable
data class PendingOfflineConsultation(
  val id: String,
  val categoryName: String,
  val ageBandId: String,
  val questionSummaryFa: String,
  val createdAtLabelFa: String,
  val savedAtEpochMs: Long = System.currentTimeMillis(),
  val pendingCloudSync: Boolean = true,
)

@Serializable
data class GalacticSelection(
  val ringName: String,
  val topicId: String,
)

enum class ManoThemeMode(val id: String, val labelFa: String) {
  SYSTEM("system", "هماهنگ با سیستم"),
  LIGHT("light", "حالت روشن"),
  DARK("dark", "حالت تیره"),
}

enum class ManoHapticIntensity(val id: String, val labelFa: String) {
  OFF("off", "خاموش"),
  SUBTLE("subtle", "ملایم"),
  STANDARD("standard", "معمولی"),
}

enum class ManoWebGlQuality(val id: String, val labelFa: String) {
  LOW("low", "سبک"),
  BALANCED("balanced", "متعادل"),
  HIGH("high", "کیفیت بالا"),
}
