package com.example.ui.screens

import com.example.domain.model.GrowthMeasurement
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.VaccinationScheduleEngine
import java.text.NumberFormat
import java.util.Locale

internal fun measurementTimeFa(epochMs: Long): String {
  val day = VaccinationScheduleEngine.formatPersianDate(epochMs)
  val time = VaccinationScheduleEngine.toPersianDigits(FeverDoseTimelineEngine.formatHourMinute(epochMs))
  return "$day، ساعت $time"
}

internal fun measurementNumberFa(value: Double): String =
  NumberFormat.getNumberInstance(Locale.forLanguageTag("fa-IR")).format(value)

internal fun GrowthMeasurement.summaryFa(): String = listOfNotNull(
  weightKg?.let { "وزن: ${measurementNumberFa(it)} کیلوگرم" },
  heightCm?.let { "قد: ${measurementNumberFa(it)} سانتی‌متر" },
).joinToString("، ")
