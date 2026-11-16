package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DoctorVisitDraft(
  val id: String,
  val titleFa: String,
  val symptomContextFa: String,
  val timelineSummaryFa: String,
  val draftTextFa: String,
  val parentNoteFa: String = "",
  val modelName: String = "gemini-3.8-flash",
  val createdAtMs: Long = System.currentTimeMillis(),
  val createdAtLabelFa: String = "",
  val isSyncedCloud: Boolean = false,
)
