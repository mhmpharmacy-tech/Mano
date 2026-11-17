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
import com.example.R
import com.example.domain.util.HighFeverClinicalAlert
import com.example.widget.FeverTimelineWidgetStore

object HighFeverNotificationHelper {
  const val CHANNEL_ID = "mano_high_fever_alerts"
  private const val NOTIFICATION_ID = 3901

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
            "هشدارهای فوری تب کودک",
            NotificationManager.IMPORTANCE_HIGH,
          ).apply {
            description = "اعلان فوری هنگام ثبت دمای بالای ۳۹.۰ درجه سانتی‌گراد در خط زمانی"
            enableVibration(true)
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun postHighFeverAlertNotification(
    context: Context,
    alert: HighFeverClinicalAlert,
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    ensureChannel(context)

    val tapIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(FeverTimelineWidgetStore.EXTRA_OPEN_TIMELINE, true)
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
        .setContentTitle(alert.titleFa)
        .setContentText(alert.bodyFa)
        .setStyle(NotificationCompat.BigTextStyle().bigText(alert.bodyFa))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
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

    return try {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
      true
    } catch (_: SecurityException) {
      false
    } catch (_: Exception) {
      false
    }
  }
}
