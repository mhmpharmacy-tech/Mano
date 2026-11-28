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
import com.example.MainActivity
import com.example.domain.model.TreatmentPlan
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.model.VaccineScheduleItem
import com.example.domain.model.VaccineScheduleSummary
import com.example.domain.util.HighFeverClinicalAlert
import com.example.domain.util.PersianNormalizer

private const val CHANNEL_HIGH_FEVER = "mano_high_fever_alerts"
private const val CHANNEL_VACCINES = "mano_vaccine_reminders"
private const val CHANNEL_TREATMENT = "mano_treatment_reminders"

object HighFeverNotificationHelper {
  fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

  private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (mgr.getNotificationChannel(CHANNEL_HIGH_FEVER) == null) {
        mgr.createNotificationChannel(
          NotificationChannel(
            CHANNEL_HIGH_FEVER,
            "هشدار تب بالا و اورژانسی کودک",
            NotificationManager.IMPORTANCE_HIGH,
          )
        )
      }
    }
  }

  fun postHighFeverAlertNotification(context: Context, alert: HighFeverClinicalAlert): Boolean {
    if (!hasNotificationPermission(context)) return false
    ensureChannel(context)
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_MANO_LAUNCH_TARGET, MainActivity.LAUNCH_TARGET_TIMELINE)
      }
    val pi =
      PendingIntent.getActivity(
        context,
        1001,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val builder =
      NotificationCompat.Builder(context, CHANNEL_HIGH_FEVER)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(alert.titleFa)
        .setContentText(alert.summaryFa)
        .setStyle(NotificationCompat.BigTextStyle().bigText(alert.summaryFa))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pi)

    return runCatching {
      NotificationManagerCompat.from(context).notify(1001, builder.build())
      true
    }.getOrDefault(false)
  }
}

object VaccineNotificationHelper {
  private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (mgr.getNotificationChannel(CHANNEL_VACCINES) == null) {
        mgr.createNotificationChannel(
          NotificationChannel(
            CHANNEL_VACCINES,
            "یادآور برنامه واکسیناسیون کودک",
            NotificationManager.IMPORTANCE_DEFAULT,
          )
        )
      }
    }
  }

  fun postVaccineReminderNotification(
    context: Context,
    dueItems: List<VaccineScheduleItem>,
    childAliasFa: String?,
    isPreview: Boolean = false,
  ): Boolean {
    if (!HighFeverNotificationHelper.hasNotificationPermission(context)) return false
    if (dueItems.isEmpty()) return false
    ensureChannel(context)
    val first = dueItems.first()
    val childPrefix = childAliasFa?.takeIf { it.isNotBlank() }?.let { "$it: " }.orEmpty()
    val title =
      if (isPreview) "${childPrefix}پیش‌نمایش یادآور واکسن"
      else "${childPrefix}یادآور واکسن ${first.definition.milestoneLabelFa}"
    val body =
      if (dueItems.size == 1) {
        "${first.definition.titleFa} — ${first.definition.preventsFa}"
      } else {
        "${PersianNormalizer.toPersianDigits(dueItems.size.toString())} نوبت واکسن نزدیک یا نیازمند پیگیری است؛ اولویت: ${first.definition.milestoneLabelFa}"
      }
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_MANO_LAUNCH_TARGET, MainActivity.LAUNCH_TARGET_VACCINES)
      }
    val pi =
      PendingIntent.getActivity(
        context,
        2001,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val builder =
      NotificationCompat.Builder(context, CHANNEL_VACCINES)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pi)

    return runCatching {
      NotificationManagerCompat.from(context).notify(2001, builder.build())
      true
    }.getOrDefault(false)
  }
}

object VaccineReminderScheduler {
  fun syncSchedule(
    context: Context,
    summary: VaccineScheduleSummary,
    settings: VaccineReminderSettings,
    alreadyNotifiedKeys: Set<String>,
    hasNotificationPermission: Boolean,
  ) {
    val prefs = context.getSharedPreferences("mano_vaccine_scheduler_cache", Context.MODE_PRIVATE)
    prefs.edit().apply {
      putBoolean("enabled", settings.enabled)
      putInt("due_count", summary.dueForReminder.size)
      putBoolean("permission_granted", hasNotificationPermission)
      putInt("notified_keys_count", alreadyNotifiedKeys.size)
      putLong("synced_at_ms", System.currentTimeMillis())
      apply()
    }
  }
}

object TreatmentPlanNotificationHelper {
  private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (mgr.getNotificationChannel(CHANNEL_TREATMENT) == null) {
        mgr.createNotificationChannel(
          NotificationChannel(
            CHANNEL_TREATMENT,
            "یادآور برنامه درمانی کودک",
            NotificationManager.IMPORTANCE_DEFAULT,
          )
        )
      }
    }
  }

  fun postReminderNotification(
    context: Context,
    plan: TreatmentPlan,
    childDisplayFa: String,
    isActivationPreview: Boolean = false,
  ): Boolean {
    if (!HighFeverNotificationHelper.hasNotificationPermission(context)) return false
    ensureChannel(context)
    val title =
      if (isActivationPreview) "یادآور برنامه درمانی فعال شد ($childDisplayFa)"
      else "یادآور نوبت داروی برنامه درمانی ($childDisplayFa)"
    val medsSummary =
      plan.medications.take(3).joinToString(" • ") { "${it.nameWithStrengthFa} (${it.doseAmountFa})" }
        .ifBlank { "برای مشاهده جزئیات برنامه درمانی ضربه بزنید." }
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_MANO_LAUNCH_TARGET, MainActivity.LAUNCH_TARGET_TREATMENT_PLAN)
      }
    val pi =
      PendingIntent.getActivity(
        context,
        3001,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val builder =
      NotificationCompat.Builder(context, CHANNEL_TREATMENT)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(title)
        .setContentText(medsSummary)
        .setStyle(NotificationCompat.BigTextStyle().bigText(medsSummary))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pi)

    return runCatching {
      NotificationManagerCompat.from(context).notify(3001, builder.build())
      true
    }.getOrDefault(false)
  }
}

object TreatmentPlanReminderScheduler {
  fun syncReminders(context: Context, plan: TreatmentPlan) {
    val prefs = context.getSharedPreferences("mano_treatment_scheduler_cache", Context.MODE_PRIVATE)
    prefs.edit().apply {
      putBoolean("enabled", plan.smartRemindersEnabled)
      putInt("med_count", plan.medications.size)
      putLong("synced_at_ms", System.currentTimeMillis())
      apply()
    }
  }
}
