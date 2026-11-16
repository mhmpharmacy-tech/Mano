package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot

object FeverTimelineWidgetStore {
  const val EXTRA_OPEN_TIMELINE = "com.example.EXTRA_OPEN_TIMELINE"
  private const val PREFS_NAME = "mano_fever_timeline_widget"
  private const val KEY_TEMP_TEXT = "latest_temp_text"
  private const val KEY_DOSE_TEXT = "latest_dose_text"
  private const val KEY_INTERVAL_TEXT = "safe_interval_text"

  fun saveAndUpdateWidgets(context: Context, snapshot: FeverDoseTimelineSnapshot) {
    val latestTemp = snapshot.tempPoints.lastOrNull()
    val latestDose = snapshot.dosePoints.lastOrNull()

    val tempText =
      if (latestTemp != null) {
        "${FeverDoseTimelineEngine.formatTempC(latestTemp.tempCelsius)} • ساعت ${latestTemp.timeLabelFa} (${latestTemp.caregiverFa})"
      } else {
        "دما: ثبت نشده"
      }

    val doseText =
      if (latestDose != null) {
        "${latestDose.medicationNameFa.substringBefore(" (")} (${latestDose.volumeMlText}) • ساعت ${latestDose.givenAtLabelFa}"
      } else {
        "دارو: نوبتی ثبت نشده"
      }

    val intervalText =
      when {
        snapshot.acetaminophenStatus.minutesRemainingSafeInterval > 0L ->
          "زمان باقی‌مانده تا فاصله حداقلی ثبت‌شده: ${FeverDoseTimelineEngine.formatDurationFa(snapshot.acetaminophenStatus.minutesRemainingSafeInterval)} (مجوز مصرف نیست)"
        snapshot.ibuprofenStatus.minutesRemainingSafeInterval > 0L ->
          "زمان باقی‌مانده تا فاصله حداقلی ثبت‌شده: ${FeverDoseTimelineEngine.formatDurationFa(snapshot.ibuprofenStatus.minutesRemainingSafeInterval)} (مجوز مصرف نیست)"
        snapshot.dosePoints.isNotEmpty() ->
          "فاصله زمانی ثبت‌شده سپری شده؛ برای مصرف بعدی با متخصص مشورت کنید"
        else ->
          "برای ثبت سریع دما یا نوبت دارو لمس کنید"
      }

    context
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_TEMP_TEXT, tempText)
      .putString(KEY_DOSE_TEXT, doseText)
      .putString(KEY_INTERVAL_TEXT, intervalText)
      .apply()

    val manager = AppWidgetManager.getInstance(context)
    val component = ComponentName(context, FeverTimelineWidgetProvider::class.java)
    val ids = manager.getAppWidgetIds(component)
    if (ids.isNotEmpty()) {
      ids.forEach { widgetId ->
        FeverTimelineWidgetProvider.updateAppWidget(context, manager, widgetId)
      }
    }
  }

  /** Clear the separate SharedPreferences widget cache and refresh launcher widgets. */
  fun clearAndUpdateWidgets(context: Context) {
    val appContext = context.applicationContext
    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    val manager = AppWidgetManager.getInstance(appContext)
    val component = ComponentName(appContext, FeverTimelineWidgetProvider::class.java)
    manager.getAppWidgetIds(component).forEach { widgetId ->
      FeverTimelineWidgetProvider.updateAppWidget(appContext, manager, widgetId)
    }
  }

  fun readSummary(context: Context): Triple<String, String, String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return Triple(
      prefs.getString(KEY_TEMP_TEXT, "دما: ثبت نشده") ?: "دما: ثبت نشده",
      prefs.getString(KEY_DOSE_TEXT, "دارو: نوبتی ثبت نشده") ?: "دارو: نوبتی ثبت نشده",
      prefs.getString(KEY_INTERVAL_TEXT, "برای ثبت سریع دما یا نوبت دارو لمس کنید")
        ?: "برای ثبت سریع دما یا نوبت دارو لمس کنید",
    )
  }
}

class FeverTimelineWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    appWidgetIds.forEach { widgetId ->
      updateAppWidget(context, appWidgetManager, widgetId)
    }
  }

  companion object {
    fun updateAppWidget(
      context: Context,
      appWidgetManager: AppWidgetManager,
      appWidgetId: Int,
    ) {
      val (tempText, doseText, intervalText) = FeverTimelineWidgetStore.readSummary(context)
      val views = RemoteViews(context.packageName, R.layout.widget_fever_timeline)
      views.setTextViewText(R.id.widget_temp_line, tempText)
      views.setTextViewText(R.id.widget_dose_line, doseText)
      views.setTextViewText(R.id.widget_interval_line, intervalText)

      val launchIntent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          putExtra(FeverTimelineWidgetStore.EXTRA_OPEN_TIMELINE, true)
        }
      val pendingIntent =
        PendingIntent.getActivity(
          context,
          appWidgetId,
          launchIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      views.setOnClickPendingIntent(R.id.widget_root_container, pendingIntent)
      views.setOnClickPendingIntent(R.id.widget_open_timeline_btn, pendingIntent)
      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }
}
