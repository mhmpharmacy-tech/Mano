package com.example.domain.util

/** Parse caregiver input without turning malformed/negative numbers into plausible measurements. */
object ClinicalNumberInput {
  fun decimal(raw: String): Double? {
    val value = PersianNormalizer.toAsciiDigits(raw).trim().replace(',', '.')
    if (!Regex("[0-9]+(?:\\.[0-9]+)?").matches(value)) return null
    return value.toDoubleOrNull()?.takeIf { it.isFinite() }
  }

  fun temperature(raw: String): Double? = decimal(raw)?.takeIf { it in 34.0..43.0 }

  fun durationHours(raw: String): Int? {
    val value = PersianNormalizer.toAsciiDigits(raw).trim()
    if (!Regex("[0-9]+").matches(value)) return null
    return value.toIntOrNull()?.takeIf { it in 0..8760 }
  }
}
