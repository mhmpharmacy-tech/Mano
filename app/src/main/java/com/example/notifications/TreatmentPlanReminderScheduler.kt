package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.ManoPreferencesRepository
import com.example.domain.model.PediatricTreatmentPlan
import com.example.domain.model.TreatmentPlanFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

object TreatmentPlanNotificationHelper {
  const val CHANNEL_ID = "mano_treatment_plan_reminders"
  const val EXTRA_OPEN_TREATMENT_PLAN = "com.example.OPEN_TREATMENT_PLAN"
  private const val NOTIFICATION_ID = 4301

  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel =
          NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.treatment_plan_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
          ).apply {
            description = context.getString(R.string.treatment_plan_reminder_channel_desc)
            enableVibration(true)
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun postReminderNotification(
    context: Context,
    plan: PediatricTreatmentPlan,
    childDisplayFa: String? = null,
    isActivationPreview: Boolean = false,
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    ensureChannel(context)

    val medCountFa = TreatmentPlanFormatter.toPersianDigits(plan.medications.size.toString())
    val titleFa =
      if (isActivationPreview) {
        "یادآور هوشمند برنامه درمانی فعال شد"
      } else {
        "یادآوری نوبت داروهای کودک"
      }
    val childPrefix = childDisplayFa?.takeIf { it.isNotBlank() }?.let { "$it • " }.orEmpty()
    val firstMed = plan.medications.firstOrNull()
    val bodyFa =
      if (isActivationPreview) {
        "${childPrefix}برای $medCountFa قلم دارو یادآور منظم تنظیم شد. برای بررسی دوز سرنگ و تیک زدن نوبت‌ها وارد کارت برنامه درمانی شوید."
      } else if (firstMed != null) {
        "${childPrefix}نوبت بررسی برنامه درمانی ($medCountFa قلم دارو)؛ از جمله ${firstMed.nameWithStrengthFa} (${firstMed.doseAmountFa})."
      } else {
        "${childPrefix}نوبت بررسی برنامه درمانی کودک فرا رسیده است."
      }

    val tapIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_OPEN_TREATMENT_PLAN, true)
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        tapIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(titleFa)
        .setContentText(bodyFa)
        .setStyle(NotificationCompat.BigTextStyle().bigText(bodyFa))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }

    return runCatching {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
      true
    }.getOrDefault(false)
  }
}

class TreatmentPlanReminderWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val prefs = ManoPreferencesRepository(applicationContext)
    val plan = TreatmentPlanFormatter.normalizeForToday(prefs.treatmentPlan.first())
    if (!plan.smartRemindersEnabled || plan.medications.isEmpty()) {
      return Result.success()
    }
    if (plan.completedTodaySlots >= plan.totalDailySlots && plan.totalDailySlots > 0) {
      return Result.success()
    }
    val profile = prefs.localChildProfile.first()
    val childDisplay = TreatmentPlanFormatter.resolveChildDisplaySummary(plan, profile)
    TreatmentPlanNotificationHelper.postReminderNotification(
      context = applicationContext,
      plan = plan,
      childDisplayFa = childDisplay,
      isActivationPreview = false,
    )
    return Result.success()
  }
}

object TreatmentPlanReminderScheduler {
  const val PERIODIC_WORK_NAME = "mano_treatment_plan_periodic_reminder"

  fun resolveRepeatIntervalHours(plan: PediatricTreatmentPlan): Long {
    val minHours = plan.medications.map { it.intervalHours }.filter { it > 0 }.minOrNull() ?: 6
    return minHours.coerceIn(2, 24).toLong()
  }

  fun syncReminders(
    context: Context,
    plan: PediatricTreatmentPlan,
  ) {
    TreatmentPlanNotificationHelper.ensureChannel(context)
    runCatching {
      val workManager = WorkManager.getInstance(context.applicationContext)
      if (!plan.smartRemindersEnabled || plan.medications.isEmpty()) {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        return
      }
      val intervalHours = resolveRepeatIntervalHours(plan)
      val request =
        PeriodicWorkRequestBuilder<TreatmentPlanReminderWorker>(intervalHours, TimeUnit.HOURS)
          .build()
      workManager.enqueueUniquePeriodicWork(
        PERIODIC_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request,
      )
    }
  }
}
