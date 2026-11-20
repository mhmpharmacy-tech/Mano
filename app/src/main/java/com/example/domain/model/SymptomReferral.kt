package com.example.domain.model

/** Measurements from one completed symptom screening, previewed before consent. */
data class SymptomReferral(
  val id: String,
  val flowId: String,
  val symptomFa: String,
  val ageBand: AgeBand,
  val ageMonthsExact: Int?,
  val temperatureCelsius: Double?,
  val durationHours: Int?,
  val selectedRedFlagIds: Set<String>,
) {
  val summaryFa: String get() = buildList {
    add("غربالگری: $symptomFa")
    add("گروه سنی: ${ageBand.rangeFa}")
    ageMonthsExact?.let { add("سن: $it ماه") }
    temperatureCelsius?.let { add("دما: $it درجه") }
    durationHours?.let { add("مدت: $it ساعت") }
  }.joinToString("، ")
}
