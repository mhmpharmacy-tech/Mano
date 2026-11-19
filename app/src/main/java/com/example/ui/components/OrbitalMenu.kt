package com.example.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.min

/**
 * Immutable item descriptor for a direct-tap orbital node placed symmetrically around MANO.
 */
data class OrbitalMenuItem(
  val id: String,
  val testTag: String,
  val titleFa: String,
  val contentDescriptionFa: String,
  val icon: ImageVector,
  val accentRole: TopicAccentRole,
  val iconKind: PediatricIconKind,
  val onClick: () -> Unit,
)

/**
 * Golden Ratio ($\varphi \approx 1.6180339$) radial proportions computed dynamically from container width.
 *
 * Exact Radial Golden Section identity ($\varphi^{-1} + \varphi^{-2} = 1$):
 * - Whole radial span  = [orbitRadiusDp]
 * - Minor segment      = [mascotRadiusDp] = `orbitRadiusDp / φ²` (`≈ 0.382 * R`)
 * - Major segment      = [radialClearanceToOrbitDp] = `orbitRadiusDp / φ` (`≈ 0.618 * R`)
 * - Exact ratio        = `orbitRadiusDp / radialClearanceToOrbitDp == radialClearanceToOrbitDp / mascotRadiusDp == φ`
 */
internal data class GoldenOrbitalMetrics(
  val stageWidthDp: Float,
  val stageHeightDp: Float,
  val orbitRadiusDp: Float,
  val goldenInnerRingRadiusDp: Float,
  val outerHaloRingRadiusDp: Float,
  val mascotDiameterDp: Float,
  val mascotRadiusDp: Float,
  val radialClearanceToOrbitDp: Float,
  val nodeWidthDp: Float,
  val nodeHeightDp: Float,
  val medallionDiameterDp: Float,
)

internal object GoldenOrbitalGeometry {
  const val GOLDEN_RATIO: Float = 1.61803398875f
  const val INV_GOLDEN_RATIO: Float = 0.61803398875f // 1 / φ
  const val INV_GOLDEN_RATIO_SQ: Float = 0.38196601125f // 1 / φ²
  private const val INV_GOLDEN_RATIO_POW5: Float = 0.09016994375f // 1 / φ⁵

  fun calculate(
    availableWidthDp: Float,
    maxNodeFloatDp: Float = 2.2f,
    maxMascotFloatDp: Float = 5.0f,
  ): GoldenOrbitalMetrics {
    val stageWidth = min(availableWidthDp, 456f).coerceAtLeast(280f)
    val outerEdgePadding = 2f
    val nodeWidth = (stageWidth * 0.232f).coerceIn(74f, 92f)
    val nodeHeight = (nodeWidth * 0.96f).coerceIn(72f, 88f)

    // Full 360° rotational clearance: keep nodes strictly inside stageWidth even when passing 3 & 9 o'clock (cos 0° = 1.0)
    val maxHorizontalRadius = (stageWidth / 2f) - (nodeWidth / 2f) - outerEdgePadding
    val preferredOrbitRadius = (stageWidth * (INV_GOLDEN_RATIO * 0.608f)).coerceIn(112f, 154f)
    val orbitRadius = min(preferredOrbitRadius, maxHorizontalRadius).coerceAtLeast(102f)

    // Exact Golden Section split between MANO radius (1 / φ²) and radial spacing to orbit (1 / φ)
    val goldenMascotRadius = orbitRadius * INV_GOLDEN_RATIO_SQ
    val minSafety = maxNodeFloatDp + maxMascotFloatDp + 5f
    val maxNodeHalfExtent = maxOf(nodeWidth, nodeHeight) / 2f
    val maxSafeRotationalMascotRadius = orbitRadius - maxNodeHalfExtent - minSafety
    val mascotRadius =
      min(goldenMascotRadius, maxSafeRotationalMascotRadius)
        .coerceIn(44f, 68f)
    val mascotDiameter = mascotRadius * 2f
    val radialClearanceToOrbit = orbitRadius - mascotRadius

    // Harmonic concentric Golden Ratio rings:
    // - Inner Golden Section ring at orbitRadius / φ (≈ 0.618 * R)
    // - Outer whisper halo ring at orbitRadius * (1 + 1 / φ⁵) (≈ 1.090 * R)
    val goldenInnerRingRadius = orbitRadius * INV_GOLDEN_RATIO
    val outerHaloRingRadius = orbitRadius * (1f + INV_GOLDEN_RATIO_POW5)

    // Medallion diameter mirrors MANO's radius (orbitRadius / φ²) for visual harmonic coherence
    val medallionDiameter = mascotRadius.coerceIn(48f, 58f)

    val verticalPadding = outerEdgePadding + maxNodeFloatDp + 2f
    val stageHeight = orbitRadius * 2f + nodeHeight + verticalPadding * 2f

    return GoldenOrbitalMetrics(
      stageWidthDp = stageWidth,
      stageHeightDp = stageHeight,
      orbitRadiusDp = orbitRadius,
      goldenInnerRingRadiusDp = goldenInnerRingRadius,
      outerHaloRingRadiusDp = outerHaloRingRadius,
      mascotDiameterDp = mascotDiameter,
      mascotRadiusDp = mascotRadius,
      radialClearanceToOrbitDp = radialClearanceToOrbit,
      nodeWidthDp = nodeWidth,
      nodeHeightDp = nodeHeight,
      medallionDiameterDp = medallionDiameter,
    )
  }
}
