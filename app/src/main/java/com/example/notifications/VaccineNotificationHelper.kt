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
import com.example.domain.model.VaccineResolvedItem
import com.example.domain.util.VaccinationScheduleEngine

object VaccineNotificationHelper {
  const val CHANNEL_ID = "mano_vaccine_reminders"
  const val EXTRA_OPEN_VACCINES = "com.example.OPEN_VACCINES"
  private const val NOTIFICATION_ID = 4201

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
            context.getString(R.string.vaccines_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
          ).apply {
            description = context.getString(R.string.vaccines_reminder_channel_desc)
            enableVibration(true)
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun postVaccineReminderNotification(
    context: Context,
    dueItems: List<VaccineResolvedItem>,
    childAliasFa: String? = null,
    isPreview: Boolean = false,
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    ensureChannel(context)

    val (titleFa, bodyFa) =
      VaccinationScheduleEngine.buildReminderNotificationCopy(
        items = dueItems,
        childAliasFa = childAliasFa,
        isPreview = isPreview,
      )

    val tapIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_OPEN_VACCINES, true)
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
