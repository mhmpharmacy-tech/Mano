package com.example.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.domain.model.VaccineDashboardSummary
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.model.VaccineStatus
import com.example.domain.util.VaccinationScheduleEngine
import java.util.Calendar
import java.util.concurrent.TimeUnit

object VaccineReminderScheduler {
  const val PERIODIC_WORK_NAME = "mano_vaccine_periodic_check"
  const val NEXT_TRIGGER_WORK_NAME = "mano_vaccine_next_trigger"
  private const val IMMEDIATE_UNNOTIFIED_DELAY_MS = 5_000L

  fun computeNextScheduleDelayMs(
    summary: VaccineDashboardSummary,
    settings: VaccineReminderSettings,
    alreadyNotifiedKeys: Set<String> = emptySet(),
    hasNotificationPermission: Boolean = true,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): Long? {
    if (!settings.enabled) return null

    if (
      hasNotificationPermission &&
        VaccinationScheduleEngine.findUnnotifiedReminders(
          summary = summary,
          settings = settings,
          alreadyNotifiedKeys = alreadyNotifiedKeys,
        ).isNotEmpty()
    ) {
      return IMMEDIATE_UNNOTIFIED_DELAY_MS
    }

    val leadDays = settings.leadDaysBefore.coerceIn(1, 30)
    val nextFutureTriggerMs =
      summary.items
        .asSequence()
        .filter {
          it.status != VaccineStatus.ADMINISTERED &&
            it.status != VaccineStatus.DEFERRED &&
            it.effectiveDueDateEpochMs != null
        }
        .filter { item ->
          val key = VaccinationScheduleEngine.reminderKeyFor(item)
          key == null || key !in alreadyNotifiedKeys
        }
        .mapNotNull { item ->
          val dueMs = item.effectiveDueDateEpochMs ?: return@mapNotNull null
          val triggerDay = VaccinationScheduleEngine.addDaysToDay(dueMs, -leadDays)
          Calendar.getInstance().apply {
            timeInMillis = triggerDay
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
          }.timeInMillis
        }
        .filter { triggerMs -> triggerMs > nowEpochMs }
        .minOrNull()

    return nextFutureTriggerMs?.let { triggerMs -> (triggerMs - nowEpochMs).coerceAtLeast(1_000L) }
  }

  fun syncSchedule(
    context: Context,
    summary: VaccineDashboardSummary,
    settings: VaccineReminderSettings,
    nowEpochMs: Long = System.currentTimeMillis(),
    alreadyNotifiedKeys: Set<String> = emptySet(),
    hasNotificationPermission: Boolean = true,
  ) {
    VaccineNotificationHelper.ensureChannel(context)
    runCatching {
      val workManager = WorkManager.getInstance(context.applicationContext)
      if (!settings.enabled) {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(NEXT_TRIGGER_WORK_NAME)
        return
      }

      val periodicRequest =
        PeriodicWorkRequestBuilder<VaccineReminderWorker>(12, TimeUnit.HOURS)
          .build()
      workManager.enqueueUniquePeriodicWork(
        PERIODIC_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        periodicRequest,
      )

      val delayMs =
        computeNextScheduleDelayMs(
          summary = summary,
          settings = settings,
          alreadyNotifiedKeys = alreadyNotifiedKeys,
          hasNotificationPermission = hasNotificationPermission,
          nowEpochMs = nowEpochMs,
        )

      if (delayMs != null) {
        val oneTimeRequest =
          OneTimeWorkRequestBuilder<VaccineReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
          NEXT_TRIGGER_WORK_NAME,
          ExistingWorkPolicy.REPLACE,
          oneTimeRequest,
        )
      } else {
        workManager.cancelUniqueWork(NEXT_TRIGGER_WORK_NAME)
      }
    }
  }
}
