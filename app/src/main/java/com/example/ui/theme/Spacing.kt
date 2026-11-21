package com.example.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified 4dp/8dp Clinical-Minimal Spacing System for MANO Kid.
 *
 * Eliminates ad-hoc dense paddings and margins by standardizing every layout surface
 * onto a serene, predictable visual rhythm:
 * - Micro (4dp–8dp): text pairs, chips, inline icon-label rhythm
 * - Component (12dp–20dp): airy card internals, list rows, button clusters
 * - Macro (20dp–32dp): screen horizontal gutters, section breaks, scroll tail clearance
 */
@Immutable
data class ManoSpacing(
  // Modular 4dp/8dp base tokens
  val xxs: Dp = 4.dp,
  val xs: Dp = 8.dp,
  val sm: Dp = 12.dp,
  val md: Dp = 16.dp,
  val lg: Dp = 20.dp,
  val xl: Dp = 24.dp,
  val xxl: Dp = 32.dp,

  // Semantic layout & component tokens
  val screenHorizontal: Dp = 20.dp,
  val screenVertical: Dp = 16.dp,
  val screenBottomClearance: Dp = 28.dp,
  val sectionGap: Dp = 20.dp,
  val cardPadding: Dp = 20.dp,
  val cardPaddingCompact: Dp = 16.dp,
  val cardItemGap: Dp = 12.dp,
  val inlineGap: Dp = 12.dp,
  val chipGap: Dp = 8.dp,
  val textPairGap: Dp = 4.dp,
) {
  val screenContentPadding: PaddingValues
    get() =
      PaddingValues(
        start = screenHorizontal,
        top = screenVertical,
        end = screenHorizontal,
        bottom = screenBottomClearance,
      )
}

val DefaultManoSpacing = ManoSpacing()

val LocalManoSpacing = staticCompositionLocalOf { DefaultManoSpacing }
