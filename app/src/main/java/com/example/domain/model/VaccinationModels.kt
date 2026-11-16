package com.example.domain.model

import kotlinx.serialization.Serializable

enum class VaccineStatus(val labelFa: String) {
  DUE_SOON("نزدیک موعد"),
  OVERDUE("نیازمند پیگیری"),
  UPCOMING("پیش‌رو"),
  ADMINISTERED("تزریق‌شده"),
  DEFERRED("تعویق با نظر پزشک"),
}

enum class VaccineFilter(val labelFa: String) {
  ALL("همه نوبت‌ها"),
  ACTIONABLE("پیش‌رو و نزدیک موعد"),
  OVERDUE("عقب‌افتاده"),
  ADMINISTERED("تزریق‌شده"),
}

data class VaccineScheduleEntry(
  val id: String,
  val titleFa: String,
  val milestoneLabelFa: String,
  val recommendedAgeDays: Int,
  val vaccinesIncludedFa: String,
  val administrationMethodFa: String,
  val careNotesFa: String,
  val isOptionalSeasonal: Boolean = false,
)

@Serializable
data class VaccineRecord(
  val id: String,
  val vaccineId: String,
  val customDueDateEpochMs: Long? = null,
  val administeredDateEpochMs: Long? = null,
  val isDeferred: Boolean = false,
  val facilityOrLotFa: String = "",
  val noteFa: String = "",
  val updatedAtMs: Long = 0L,
  val isSyncedCloud: Boolean = false,
)

@Serializable
data class VaccineReminderSettings(
  val enabled: Boolean = true,
  val leadDaysBefore: Int = 3,
)

data class VaccineResolvedItem(
  val definition: VaccineScheduleEntry,
  val record: VaccineRecord?,
  val effectiveDueDateEpochMs: Long?,
  val administeredDateEpochMs: Long?,
  val daysUntilDue: Int?,
  val status: VaccineStatus,
  val dueDateSourceFa: String,
)

data class VaccineDashboardSummary(
  val items: List<VaccineResolvedItem>,
  val totalCoreCount: Int,
  val administeredCoreCount: Int,
  val dueSoonCount: Int,
  val overdueCount: Int,
  val nextUpcoming: VaccineResolvedItem?,
  val dueForReminder: List<VaccineResolvedItem>,
)
