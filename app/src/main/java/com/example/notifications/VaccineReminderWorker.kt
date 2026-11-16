package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.ManoPreferencesRepository
import com.example.domain.util.VaccinationScheduleEngine
import kotlinx.coroutines.flow.first

class VaccineReminderWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val prefs = ManoPreferencesRepository(applicationContext)
    val settings = prefs.vaccineReminderSettings.first()
    if (!settings.enabled) return Result.success()

    val birthDateMs = prefs.childBirthDateEpochMs.first()
    val childAliasFa = prefs.childAliasFa.first()
    val localRecords = prefs.vaccineRecords.first()
    val alreadyNotifiedKeys = prefs.notifiedVaccineReminderKeys.first()

    val summary =
      VaccinationScheduleEngine.resolveSchedule(
        localRecords = localRecords,
        birthDateEpochMs = birthDateMs,
        reminderSettings = settings,
        nowEpochMs = System.currentTimeMillis(),
      )

    val pendingAlerts =
      VaccinationScheduleEngine.findUnnotifiedReminders(
        summary = summary,
        settings = settings,
        alreadyNotifiedKeys = alreadyNotifiedKeys,
      )

    if (pendingAlerts.isNotEmpty()) {
      val posted =
        VaccineNotificationHelper.postVaccineReminderNotification(
          context = applicationContext,
          dueItems = pendingAlerts,
          childAliasFa = childAliasFa,
          isPreview = false,
        )
      if (posted) {
        val newKeys = pendingAlerts.mapNotNull { VaccinationScheduleEngine.reminderKeyFor(it) }.toSet()
        if (newKeys.isNotEmpty()) {
          prefs.markVaccineReminderKeysNotified(newKeys)
        }
      }
    }

    return Result.success()
  }
}
