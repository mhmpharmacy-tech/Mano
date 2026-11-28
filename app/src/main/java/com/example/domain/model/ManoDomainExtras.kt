package com.example.domain.model

import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.PersianNormalizer
import kotlinx.serialization.Serializable

typealias TreatmentPlan = PediatricTreatmentPlan

@Serializable
data class SymptomReferral(
  val id: String,
  val symptomFlowId: String,
  val ageBand: AgeBand,
  val summaryFa: String,
  val temperatureCelsius: Float? = null,
  val durationHours: Int? = null,
  val ageMonthsExact: Int? = null,
  val selectedRedFlagIds: Set<String> = emptySet(),
  val createdAtMs: Long = System.currentTimeMillis(),
) {
  val flowId: String get() = symptomFlowId
}

@Serializable
data class DoctorVisitDraft(
  val id: String,
  val titleFa: String,
  val symptomContextFa: String,
  val timelineSummaryFa: String,
  val draftTextFa: String,
  val parentNoteFa: String = "",
  val modelName: String = "gemini-3.8-flash",
  val createdAtMs: Long = System.currentTimeMillis(),
  val createdAtLabelFa: String = "",
  val isSyncedCloud: Boolean = false,
)

@Serializable
data class GrowthMeasurement(
  val id: String,
  val weightKg: Double,
  val heightCm: Double? = null,
  val recordedAtMs: Long = System.currentTimeMillis(),
) {
  fun summaryFa(): String {
    val w = FeverDoseTimelineEngine.formatDecimalFa(weightKg.toFloat())
    return if (heightCm != null && heightCm > 0.0) {
      val h = FeverDoseTimelineEngine.formatDecimalFa(heightCm.toFloat())
      "وزن $w کیلوگرم • قد $h سانتی‌متر"
    } else {
      "وزن $w کیلوگرم"
    }
  }
}

sealed interface PreviousCare {
  val recordedAtMs: Long

  data class Temperature(
    val celsius: Float,
    override val recordedAtMs: Long,
  ) : PreviousCare

  data class Growth(
    val measurement: GrowthMeasurement,
  ) : PreviousCare {
    override val recordedAtMs: Long get() = measurement.recordedAtMs
  }

  companion object {
    fun latest(
      uid: String?,
      localFeverLogs: List<com.example.domain.util.TimelineTempPoint>,
      cloudFeverLogs: List<com.example.data.FeverLogCloud>,
      growthHistory: List<GrowthMeasurement>,
    ): PreviousCare? {
      val latestCloudTemp = if (uid != null) {
        cloudFeverLogs.maxByOrNull { it.createdAt?.toDate()?.time ?: 0L }?.let { cloud ->
          Temperature(
            celsius = cloud.tempCelsius.toFloat(),
            recordedAtMs = cloud.createdAt?.toDate()?.time ?: 0L,
          )
        }
      } else null
      val latestLocalTemp = if (uid == null) {
        localFeverLogs.maxByOrNull { it.timestampMs }?.let {
          Temperature(celsius = it.tempCelsius, recordedAtMs = it.timestampMs)
        }
      } else null
      val temp = latestCloudTemp ?: latestLocalTemp
      val growth = growthHistory.maxByOrNull { it.recordedAtMs }?.let { Growth(it) }
      return when {
        temp != null && growth != null ->
          if (temp.recordedAtMs >= growth.measurement.recordedAtMs) temp else growth
        temp != null -> temp
        else -> growth
      }
    }
  }
}

enum class VaccineStatus(val labelFa: String) {
  OVERDUE("عقب‌افتاده"),
  DUE_SOON("نزدیک به موعد"),
  UPCOMING("پیش‌رو"),
  ADMINISTERED("تزریق‌شده"),
  DEFERRED("معوق / مشاوره"),
  NEEDS_BIRTHDATE("نیازمند تاریخ تولد"),
}

enum class VaccineFilter(val labelFa: String) {
  ALL("همه نوبت‌ها"),
  DUE_AND_OVERDUE("نزدیک و عقب‌افتاده"),
  UPCOMING("آینده"),
  ADMINISTERED("ثبت‌شده"),
}

data class VaccineDefinition(
  val id: String,
  val titleFa: String,
  val milestoneLabelFa: String,
  val dueAgeDays: Int,
  val isCoreNational: Boolean = true,
  val preventsFa: String = "",
  val careNotesFa: String = "",
)

@Serializable
data class VaccineRecord(
  val id: String = "",
  val vaccineId: String,
  val customDueDateEpochMs: Long? = null,
  val administeredDateEpochMs: Long? = null,
  val isDeferred: Boolean = false,
  val facilityOrLotFa: String = "",
  val noteFa: String = "",
  val updatedAtMs: Long = System.currentTimeMillis(),
  val isSyncedCloud: Boolean = false,
)

@Serializable
data class VaccineReminderSettings(
  val enabled: Boolean = true,
  val leadDaysBefore: Int = 3,
  val notifyOverdue: Boolean = true,
)

data class VaccineScheduleItem(
  val definition: VaccineDefinition,
  val status: VaccineStatus,
  val dueDateEpochMs: Long?,
  val record: VaccineRecord?,
  val daysRemaining: Int?,
) {
  val effectiveDueDateEpochMs: Long? get() = dueDateEpochMs
}

data class VaccineScheduleSummary(
  val items: List<VaccineScheduleItem>,
  val dueForReminder: List<VaccineScheduleItem>,
  val nextUpcoming: VaccineScheduleItem?,
  val administeredCoreCount: Int,
  val totalCoreCount: Int,
  val overdueCount: Int,
  val dueSoonCount: Int,
)

@Serializable
data class SavedSyrupScan(
  val id: String,
  val drugNameFa: String,
  val activeIngredientKey: String,
  val dosageForm: String,
  val mgAmount: Double,
  val perMlAmount: Double,
  val mgPerMl: Double,
  val matchedFormulationId: String,
  val confidenceLevel: String,
  val concentrationDisplayFa: String,
  val safetyNoteFa: String,
  val confirmedByParent: Boolean = false,
  val createdAtMs: Long = System.currentTimeMillis(),
  val updatedAtMs: Long = System.currentTimeMillis(),
  val isSyncedCloud: Boolean = false,
)

enum class NutritionAppetiteStatus(val id: String, val labelFa: String) {
  NORMAL("normal", "اشتهای معمولی"),
  LOW_APPETITE("low", "کم‌اشتهایی خفیف"),
  FEVER_RECOVERY("recovery", "نقاهت بعد از تب یا بیماری"),
  PICKY("picky", "بدغذایی"),
}

@Serializable
data class MealSuggestionItem(
  val mealTypeFa: String,
  val titleFa: String,
  val whySuitableFa: String = "",
  val textureAndPortionFa: String = "",
  val ingredientsFa: List<String> = emptyList(),
  val prepStepsFa: List<String> = emptyList(),
)

@Serializable
data class NutritionPlanResult(
  val id: String,
  val planTitleFa: String,
  val childContextSummaryFa: String = "",
  val overviewFa: String = "",
  val growthNutritionFocusFa: String = "",
  val meals: List<MealSuggestionItem> = emptyList(),
  val hydrationTipFa: String = "",
  val safetyAvoidListFa: List<String> = emptyList(),
  val doctorFollowUpHintFa: String = "",
  val createdAtMs: Long = System.currentTimeMillis(),
  val createdAtLabelFa: String = "",
)

@Serializable
enum class TreatmentDosageForm(val labelFa: String) {
  SYRUP_SUSPENSION("شربت / سوسپانسیون"),
  DROPS("قطره خوراکی"),
  NASAL_SPRAY("اسپری بینی / تنفسی"),
  OINTMENT_CREAM("پماد / کرم موضعی"),
  TABLET_SACHET("قرص / ساشه"),
}

@Serializable
data class TreatmentMedicationItem(
  val id: String,
  val nameWithStrengthFa: String,
  val form: TreatmentDosageForm = TreatmentDosageForm.SYRUP_SUSPENSION,
  val doseAmountFa: String,
  val scheduleLabelFa: String,
  val foodRelationFa: String = "مطابق دستور داروخانه",
  val counselingNoteFa: String = "",
  val isWarningHighlight: Boolean = false,
  val dailyDoseSlots: Int = 2,
  val completedSlotTimesFa: Map<Int, String> = emptyMap(),
)

@Serializable
data class PediatricTreatmentPlan(
  val id: String = "default_plan",
  val pharmacyNameFa: String = "داروخانه همکار مانو کید",
  val pharmacistBadgeFa: String = "برنامه مراقبت خانگی کودک",
  val prescriptionDateFa: String = "ثبت نشده",
  val childDisplayFa: String = "",
  val storageInstructionFa: String = "داروها را دور از دسترس کودک، در دمای مناسب و بر اساس برچسب هر فرآورده نگهداری کنید.",
  val smartRemindersEnabled: Boolean = false,
  val medications: List<TreatmentMedicationItem> = emptyList(),
) {
  val totalDailySlots: Int
    get() = medications.sumOf { it.dailyDoseSlots.coerceIn(1, 6) }

  val completedTodaySlots: Int
    get() = medications.sumOf { med ->
      med.completedSlotTimesFa.keys.count { it in 0 until med.dailyDoseSlots.coerceIn(1, 6) }
    }
}

object TreatmentPlanFormatter {
  fun toPersianDigits(input: String): String = PersianNormalizer.toPersianDigits(input)

  fun resolveChildDisplaySummary(plan: PediatricTreatmentPlan, profile: ChildProfileDemo?): String {
    val fromProfile = profile?.let {
      listOfNotNull(
        it.aliasFa.takeIf { s -> s.isNotBlank() },
        it.weightKgText.takeIf { w -> w.isNotBlank() }?.let { w -> "${toPersianDigits(w)} کیلوگرم" },
      ).joinToString(" • ")
    }.orEmpty()
    return fromProfile.ifBlank { plan.childDisplayFa.ifBlank { "پرونده کودک ثبت نشده" } }
  }

  fun resolveAllergySummary(profile: ChildProfileDemo?): String {
    val raw = profile?.allergiesFa?.trim().orEmpty()
    return if (raw.isBlank()) "حساسیت شناخته‌شده ثبت نشده (در صورت سابقه حساسیت حتماً در پرونده کودک ثبت کنید)"
    else raw
  }

  fun buildCaregiverShareText(plan: PediatricTreatmentPlan, profile: ChildProfileDemo?): String =
    buildString {
      appendLine("برنامه مراقبت و پیگیری دارویی — مانو کید")
      appendLine("کودک: ${resolveChildDisplaySummary(plan, profile)}")
      appendLine("داروخانه: ${plan.pharmacyNameFa}")
      if (plan.medications.isEmpty()) {
        appendLine("هنوز دارویی در برنامه ثبت نشده است.")
      } else {
        plan.medications.forEachIndexed { idx, med ->
          appendLine("${toPersianDigits((idx + 1).toString())}. ${med.nameWithStrengthFa} — ${med.doseAmountFa} (${med.scheduleLabelFa})")
        }
      }
      appendLine("این یادداشت جهت هماهنگی مراقبین است و جایگزین برچسب رسمی داروخانه و نظر پزشک نیست.")
    }
}

enum class MedicationCheckCategory(val id: String, val titleFa: String, val subtitleFa: String) {
  CONCENTRATION("concentration", "برچسب و غلظت صحیح", "پیشگیری از اشتباه بین قطره و شربت"),
  MEASURE_TOOL("measure", "وسیله اندازه‌گیری استاندارد", "استفاده از سرنگ مدرج به‌جای قاشق خانگی"),
  OVERLAP_STORAGE("overlap", "عدم تداخل و نگهداری ایمن", "بررسی ترکیب مشابه و دور از دسترس کودک"),
}

data class MedicationSafetyChecklistItem(
  val id: String,
  val category: MedicationCheckCategory,
  val titleFa: String,
  val detailFa: String,
  val isCritical: Boolean = false,
  val actionTargetKey: String? = null,
)

data class MedicationSafetyChecklistProgress(
  val checkedCount: Int,
  val totalCount: Int,
  val criticalCheckedCount: Int,
  val criticalTotalCount: Int,
  val isAllCriticalReady: Boolean,
)

object MedicationSafetyChecklistCatalog {
  val items: List<MedicationSafetyChecklistItem> = listOf(
    MedicationSafetyChecklistItem(
      id = "med_chk_concentration",
      category = MedicationCheckCategory.CONCENTRATION,
      titleFa = "تطبیق دقیق غلظت (mg/mL) روی جعبه و بطری",
      detailFa = "قطره استامینوفن بسیار غلیظ‌تر از شربت است؛ هرگز مقدار میلی‌لیتر قطره و شربت را یکی نگیرید.",
      isCritical = true,
      actionTargetKey = "syringe",
    ),
    MedicationSafetyChecklistItem(
      id = "med_chk_weight_age",
      category = MedicationCheckCategory.CONCENTRATION,
      titleFa = "بررسی وزن فعلی کودک و منع سنی فرآورده",
      detailFa = "در نوزاد زیر ۳ ماه یا کودک با بیماری زمینه‌ای، قبل از هر دارو نظر پزشک ضروری است.",
      isCritical = true,
    ),
    MedicationSafetyChecklistItem(
      id = "med_chk_syringe",
      category = MedicationCheckCategory.MEASURE_TOOL,
      titleFa = "اندازه‌گیری با سرنگ یا پیمانه‌ مدرج استاندارد",
      detailFa = "قاشق چای‌خوری خانگی حجم یکنواختی ندارد و می‌تواند باعث کم‌دوزی یا بیش‌دوزی شود.",
      isCritical = true,
      actionTargetKey = "syringe",
    ),
    MedicationSafetyChecklistItem(
      id = "med_chk_shake",
      category = MedicationCheckCategory.MEASURE_TOOL,
      titleFa = "تکان دادن سوسپانسیون پیش از هر بار کشیدن",
      detailFa = "در سوسپانسیون‌ها ماده مؤثر ته‌نشین می‌شود؛ پیش از کشیدن با سرنگ به آرامی تکان دهید.",
      isCritical = false,
    ),
    MedicationSafetyChecklistItem(
      id = "med_chk_overlap",
      category = MedicationCheckCategory.OVERLAP_STORAGE,
      titleFa = "بررسی تداخل و ماده مؤثر تکراری (استامینوفن/ایبوپروفن پنهان)",
      detailFa = "بسیاری از شربت‌های سرماخوردگی کودکان حاوی استامینوفن هستند؛ هم‌زمان با مسکن ندهید.",
      isCritical = true,
      actionTargetKey = "overlap",
    ),
    MedicationSafetyChecklistItem(
      id = "med_chk_logbook",
      category = MedicationCheckCategory.OVERLAP_STORAGE,
      titleFa = "ثبت ساعت هر نوبت در دفترچه مصرف خانواده",
      detailFa = "وقتی چند مراقب از کودک نگهداری می‌کنند، ثبت ساعت مصرف جلوی تکرار ناخواسته را می‌گیرد.",
      isCritical = false,
      actionTargetKey = "logbook",
    ),
  )

  fun evaluateProgress(checkedIds: Set<String>): MedicationSafetyChecklistProgress {
    val criticalItems = items.filter { it.isCritical }
    val criticalChecked = criticalItems.count { it.id in checkedIds }
    val totalChecked = items.count { it.id in checkedIds }
    return MedicationSafetyChecklistProgress(
      checkedCount = totalChecked,
      totalCount = items.size,
      criticalCheckedCount = criticalChecked,
      criticalTotalCount = criticalItems.size,
      isAllCriticalReady = criticalChecked == criticalItems.size && criticalItems.isNotEmpty(),
    )
  }
}
