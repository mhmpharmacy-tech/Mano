package com.example.domain.model

import com.example.data.FeverLogCloud
import com.example.domain.util.TimelineTempPoint

/** A shortcut to a recorded measurement. Browsing a screen never creates one. */
sealed interface PreviousCare {
  val recordedAtMs: Long
  data class Temperature(override val recordedAtMs: Long, val celsius: Double) : PreviousCare
  data class Growth(val measurement: GrowthMeasurement) : PreviousCare {
    override val recordedAtMs get() = measurement.recordedAtMs
  }

  companion object {
    fun latest(
      uid: String?,
      localTemps: List<TimelineTempPoint>,
      cloudTemps: List<FeverLogCloud>,
      growth: List<GrowthMeasurement>,
    ): PreviousCare? {
      val temperatures = if (uid == null) {
        localTemps.asSequence().map { Temperature(it.timestampMs, it.tempCelsius.toDouble()) }
      } else {
        cloudTemps.asSequence().filter { it.userId == uid }.mapNotNull { row ->
          row.createdAt?.toDate()?.time?.let { Temperature(it, row.tempCelsius) }
        }
      }
      val temp = temperatures.filter { it.recordedAtMs > 0 && it.celsius in 34.0..43.0 }
        .maxByOrNull { it.recordedAtMs }
      val measure = growth.filter { it.recordedAtMs > 0 && (it.weightKg != null || it.heightCm != null) }
        .maxByOrNull { it.recordedAtMs } ?: return temp
      return if (temp != null && temp.recordedAtMs >= measure.recordedAtMs) temp else Growth(measure)
    }
  }
}
