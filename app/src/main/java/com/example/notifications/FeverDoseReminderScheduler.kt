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
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import java.util.concurrent.TimeUnit

object FeverDoseReminderScheduler {
  const val CHANNEL_ID = "mano_fever_dose_reminders"
  const val WORK_NAME = "mano_next_fever_dose_reminder"
  const val KEY_DRUG_NAME_FA = "drug_name_fa"
  const val KEY_VOLUME_ML_FA = "volume_ml_fa"
  const val KEY_CHILD_ALIAS_FA = "child_alias_fa"
  private const val NOTIFICATION_ID = 4406

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          "یادآور نوبت بعدی تب‌بر کودک",
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = "یادآوری ایمن زمان نوبت بعدی داروی تب‌بر کودک پس از گذشت فاصله استاندارد"
        }
      manager.createNotificationChannel(channel)
    }
  }

  fun scheduleNextDoseReminder(
    context: Context,
    drugNameFa: String,
    volumeMlFa: String,
    childAliasFa: String?,
    delayHours: Long = 6L,
  ) {
    ensureChannel(context)
    runCatching {
      val inputData =
        Data.Builder()
          .putString(KEY_DRUG_NAME_FA, drugNameFa.take(80))
          .putString(KEY_VOLUME_ML_FA, volumeMlFa.take(24))
          .putString(KEY_CHILD_ALIAS_FA, childAliasFa?.take(40).orEmpty())
          .build()

      val request =
        OneTimeWorkRequestBuilder<FeverDoseReminderWorker>()
          .setInitialDelay(delayHours.coerceIn(1L, 12L), TimeUnit.HOURS)
          .setInputData(inputData)
          .build()

      WorkManager.getInstance(context.applicationContext)
        .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
  }

  fun postReminderNotification(
    context: Context,
    drugNameFa: String,
    volumeMlFa: String,
    childAliasFa: String?,
  ): Boolean {
    ensureChannel(context)
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }
    val openIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("open_fever_timeline", true)
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val childLabel = childAliasFa?.takeIf { it.isNotBlank() } ?: "کودک"
    val title = "فاصله ایمن نوبت بعدی تب‌بر «$childLabel» کامل شد"
    val body =
      "۶ ساعت از نوبت قبلی ($drugNameFa — $volumeMlFa) گذشت. در صورتی که کودک همچنان تب یا بی‌قراری دارد، دمای بدن را چک کرده و در صورت نیاز نوبت بعدی را ثبت کنید."

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    return runCatching {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
      true
    }.getOrDefault(false)
  }
}

class FeverDoseReminderWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val drugNameFa = inputData.getString(FeverDoseReminderScheduler.KEY_DRUG_NAME_FA) ?: "داروی تب‌بر"
    val volumeMlFa = inputData.getString(FeverDoseReminderScheduler.KEY_VOLUME_ML_FA) ?: ""
    val childAliasFa = inputData.getString(FeverDoseReminderScheduler.KEY_CHILD_ALIAS_FA)
    FeverDoseReminderScheduler.postReminderNotification(
      context = applicationContext,
      drugNameFa = drugNameFa,
      volumeMlFa = volumeMlFa,
      childAliasFa = childAliasFa,
    )
    return Result.success()
  }
}
