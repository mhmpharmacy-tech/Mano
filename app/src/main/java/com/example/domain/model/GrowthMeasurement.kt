package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GrowthMeasurement(
  val recordedAtMs: Long,
  val weightKg: Double?,
  val heightCm: Double?,
)
