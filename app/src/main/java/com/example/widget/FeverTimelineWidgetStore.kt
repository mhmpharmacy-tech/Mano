package com.example.widget

import android.content.Context
import com.example.domain.util.FeverDoseTimelineSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object FeverTimelineWidgetStore {
  private const val PREFS_NAME = "mano_fever_timeline_widget_store"
  private const val KEY_LATEST_TEMP = "latest_temp_celsius"
  private const val KEY_LATEST_TIME = "latest_temp_time"
  private const val KEY_LATEST_DOSE = "latest_dose_summary"
  private const val KEY_UPDATED_MS = "updated_at_ms"

  private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

  fun saveAndUpdateWidgets(context: Context, snapshot: FeverDoseTimelineSnapshot) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val latestDose = snapshot.dosePoints.lastOrNull()
    val doseSummary =
      latestDose?.let { "${it.medicationNameFa} • ${it.volumeMlText} (${it.givenAtLabelFa})" } ?: ""
    prefs.edit().apply {
      putFloat(KEY_LATEST_TEMP, snapshot.latestTempCelsius ?: -1f)
      putString(KEY_LATEST_TIME, snapshot.latestTempTimeFa)
      putString(KEY_LATEST_DOSE, doseSummary)
      putString("snapshot_json", runCatching { json.encodeToString(snapshot) }.getOrNull())
      putLong(KEY_UPDATED_MS, System.currentTimeMillis())
      apply()
    }
  }

  fun clear(context: Context): Boolean =
    runCatching {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }.getOrDefault(false)
}
