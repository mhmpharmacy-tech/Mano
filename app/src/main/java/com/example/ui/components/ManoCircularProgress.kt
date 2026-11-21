package com.example.ui.components

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AquaBreeze
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.GlassCyanRim
import com.example.ui.theme.LightMint
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.MinimalTeal
import com.example.ui.theme.MinimalTealDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.SoftMintHalo
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TealGlow
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Standardized size presets for MANO Kid's minimal turquoise circular progress indicators.
 */
enum class ManoCircularProgressSize(
  val diameter: Dp,
  val strokeWidth: Dp,
  val showCenterDotOnIndeterminate: Boolean,
) {
  /** Inline controls, filled buttons, and compact status badges. */
  COMPACT(diameter = 18.dp, strokeWidth = 2.2.dp, showCenterDotOnIndeterminate = false),
  /** Small inline cards and compact sync rows. */
  SMALL(diameter = 28.dp, strokeWidth = 2.8.dp, showCenterDotOnIndeterminate = false),
  /** Standard data-loading indicator or compact health status ring. */
  MEDIUM(diameter = 44.dp, strokeWidth = 3.6.dp, showCenterDotOnIndeterminate = true),
  /** Health summary ring with legible centered value or percentage. */
  LARGE(diameter = 64.dp, strokeWidth = 5.0.dp, showCenterDotOnIndeterminate = true),
  /** Primary dashboard/overview health summary gauge. */
  HERO(diameter = 84.dp, strokeWidth = 6.2.dp, showCenterDotOnIndeterminate = true),
}

/**
 * Semantic tone variants anchored in MANO Kid's clinical turquoise color system.
 */
enum class ManoHealthRingTone {
  /** Default MANO turquoise palette (DeepTeal -> SeaGreen -> TealGlow). */
  TURQUOISE,
  /** Airy soft pastel turquoise palette tailored for dashboard frosted-glass summary rings. */
  PASTEL_TURQUOISE,
  /** Positive clinical completion tint (blends emerald into turquoise). */
  EMERALD,
  /** Caution / attention health state tint. */
  CAUTION,
  /** High-contrast tint for indicators placed inside filled primary buttons. */
  ON_PRIMARY,
}

@Immutable
private data class TurquoiseRingPalette(
  val trackColor: Color,
  val innerHairline: Color,
  val arcStart: Color,
  val arcMid: Color,
  val arcEnd: Color,
  val tipGlow: Color,
  val inkColor: Color,
)

@Composable
private fun rememberTurquoiseRingPalette(
  tone: ManoHealthRingTone,
): TurquoiseRingPalette {
  val isDark = LocalManoDarkTheme.current
  val onPrimary = MaterialTheme.colorScheme.onPrimary
  return remember(isDark, tone, onPrimary) {
    when (tone) {
      ManoHealthRingTone.ON_PRIMARY ->
        TurquoiseRingPalette(
          trackColor = onPrimary.copy(alpha = 0.24f),
          innerHairline = onPrimary.copy(alpha = 0.12f),
          arcStart = onPrimary.copy(alpha = 0.72f),
          arcMid = onPrimary,
          arcEnd = onPrimary,
          tipGlow = onPrimary,
          inkColor = onPrimary,
        )

      ManoHealthRingTone.EMERALD ->
        if (isDark) {
          TurquoiseRingPalette(
            trackColor = DarkSage.copy(alpha = 0.16f),
            innerHairline = GlassCyanRim.copy(alpha = 0.14f),
            arcStart = SeaGreen,
            arcMid = DarkSage,
            arcEnd = GlassCyanRim,
            tipGlow = PureWhite.copy(alpha = 0.90f),
            inkColor = DarkSage,
          )
        } else {
          TurquoiseRingPalette(
            trackColor = SuccessEmerald.copy(alpha = 0.13f),
            innerHairline = LightMint.copy(alpha = 0.75f),
            arcStart = SuccessEmerald,
            arcMid = SeaGreen,
            arcEnd = TealGlow,
            tipGlow = PureWhite,
            inkColor = SuccessEmerald,
          )
        }

      ManoHealthRingTone.CAUTION ->
        if (isDark) {
          TurquoiseRingPalette(
            trackColor = DarkGold.copy(alpha = 0.16f),
            innerHairline = DarkGold.copy(alpha = 0.12f),
            arcStart = CautionAmber,
            arcMid = DarkGold,
            arcEnd = DarkGold,
            tipGlow = PureWhite.copy(alpha = 0.85f),
            inkColor = DarkGold,
          )
        } else {
          TurquoiseRingPalette(
            trackColor = CautionAmber.copy(alpha = 0.14f),
            innerHairline = CautionAmber.copy(alpha = 0.10f),
            arcStart = CautionAmber,
            arcMid = CautionAmber.copy(alpha = 0.85f),
            arcEnd = DarkGold,
            tipGlow = PureWhite,
            inkColor = CautionAmber,
          )
        }

      ManoHealthRingTone.PASTEL_TURQUOISE ->
        if (isDark) {
          TurquoiseRingPalette(
            trackColor = GlassCyanRim.copy(alpha = 0.14f),
            innerHairline = MinimalTealDark.copy(alpha = 0.16f),
            arcStart = MinimalTeal,
            arcMid = MinimalTealDark,
            arcEnd = GlassCyanRim,
            tipGlow = PureWhite.copy(alpha = 0.90f),
            inkColor = GlassCyanRim,
          )
        } else {
          TurquoiseRingPalette(
            trackColor = AquaBreeze.copy(alpha = 0.72f),
            innerHairline = SoftMintHalo.copy(alpha = 0.92f),
            arcStart = MinimalTeal,
            arcMid = SeaGreen,
            arcEnd = TealGlow,
            tipGlow = PureWhite,
            inkColor = DeepTeal,
          )
        }

      ManoHealthRingTone.TURQUOISE ->
        if (isDark) {
          TurquoiseRingPalette(
            trackColor = MinimalTealDark.copy(alpha = 0.15f),
            innerHairline = GlassCyanRim.copy(alpha = 0.14f),
            arcStart = SeaGreen,
            arcMid = MinimalTealDark,
            arcEnd = GlassCyanRim,
            tipGlow = PureWhite.copy(alpha = 0.92f),
            inkColor = MinimalTealDark,
          )
        } else {
          TurquoiseRingPalette(
            trackColor = MinimalTeal.copy(alpha = 0.14f),
            innerHairline = LightMint.copy(alpha = 0.85f),
            arcStart = DeepTeal,
            arcMid = SeaGreen,
            arcEnd = TealGlow,
            tipGlow = PureWhite,
            inkColor = DeepTeal,
          )
        }
    }
  }
}

private fun isReducedMotion(context: Context): Boolean {
  if (Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) return true
  return runCatching {
    Settings.Global.getFloat(
      context.contentResolver,
      Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    ) <= 0f
  }.getOrDefault(false)
}

/**
 * Minimal indeterminate circular progress indicator styled with MANO Kid's turquoise palette.
 * Designed for data loading states inside cards, panels, and action buttons.
 */
@Composable
fun ManoTurquoiseLoadingRing(
  modifier: Modifier = Modifier,
  size: ManoCircularProgressSize = ManoCircularProgressSize.MEDIUM,
  tone: ManoHealthRingTone = ManoHealthRingTone.TURQUOISE,
  diameter: Dp = size.diameter,
  strokeWidth: Dp = size.strokeWidth,
  contentDescriptionFa: String = "در حال بارگذاری اطلاعات",
) {
  val context = LocalContext.current
  val reducedMotion = remember(context) { isReducedMotion(context) }
  val palette = rememberTurquoiseRingPalette(tone)

  val rotationDeg: Float
  val sweepDeg: Float
  val corePulse: Float

  if (reducedMotion) {
    rotationDeg = -60f
    sweepDeg = 215f
    corePulse = 0.85f
  } else {
    val transition = rememberInfiniteTransition(label = "manoTurquoiseLoadingRing")
    val animRotation by
      transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 1280, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
          ),
        label = "manoTurquoiseRingRotation",
      )
    val animSweep by
      transition.animateFloat(
        initialValue = 62f,
        targetValue = 235f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 960, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "manoTurquoiseRingSweep",
      )
    val animPulse by
      transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "manoTurquoiseRingPulse",
      )
    rotationDeg = animRotation
    sweepDeg = animSweep
    corePulse = animPulse
  }

  Canvas(
    modifier =
      modifier
        .size(diameter)
        .semantics {
          contentDescription = contentDescriptionFa
        },
  ) {
    val strokePx = strokeWidth.toPx()
    val inset = strokePx * 0.5f + 1.dp.toPx()
    val arcSize = Size(
      width = (this.size.width - inset * 2f).coerceAtLeast(1f),
      height = (this.size.height - inset * 2f).coerceAtLeast(1f),
    )
    val topLeft = Offset(inset, inset)
    val center = Offset(this.size.width * 0.5f, this.size.height * 0.5f)
    val radius = (arcSize.minDimension * 0.5f).coerceAtLeast(1f)

    // Soft translucent turquoise circular track
    drawCircle(
      color = palette.trackColor,
      radius = radius,
      center = center,
      style = Stroke(width = strokePx),
    )

    // Orbiting turquoise sweep arc with luminous clinical gradient
    rotate(degrees = rotationDeg, pivot = center) {
      drawArc(
        brush =
          Brush.sweepGradient(
            0.0f to palette.arcStart.copy(alpha = 0.12f),
            0.28f to palette.arcStart,
            0.68f to palette.arcMid,
            1.0f to palette.arcEnd,
            center = center,
          ),
        startAngle = -90f,
        sweepAngle = sweepDeg,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )

      // Delicate luminous leading node at the tip of the arc
      val tipAngleRad = ((-90f + sweepDeg) * PI / 180.0)
      val tipCenter =
        Offset(
          x = center.x + (radius * cos(tipAngleRad)).toFloat(),
          y = center.y + (radius * sin(tipAngleRad)).toFloat(),
        )
      drawCircle(
        color = palette.tipGlow.copy(alpha = 0.85f),
        radius = (strokePx * 0.30f).coerceAtLeast(1f),
        center = tipCenter,
      )
    }

    // Whisper-soft turquoise breathing dot inside medium/large loading rings
    if (size.showCenterDotOnIndeterminate) {
      val dotRadius = (radius * 0.16f * (0.85f + 0.15f * corePulse)).coerceAtLeast(2f)
      drawCircle(
        color = palette.arcMid.copy(alpha = 0.16f * corePulse),
        radius = dotRadius * 2.1f,
        center = center,
      )
      drawCircle(
        color = palette.arcMid.copy(alpha = 0.72f * corePulse),
        radius = dotRadius,
        center = center,
      )
    }
  }
}

/**
 * Minimal determinate circular progress ring for health summaries
 * (vaccination completion, safety checklist readiness, developmental milestones, growth tracking).
 */
@Composable
fun ManoHealthProgressRing(
  progress: Float,
  modifier: Modifier = Modifier,
  size: ManoCircularProgressSize = ManoCircularProgressSize.LARGE,
  tone: ManoHealthRingTone = ManoHealthRingTone.TURQUOISE,
  diameter: Dp = size.diameter,
  strokeWidth: Dp = size.strokeWidth,
  valueText: String? = null,
  subLabel: String? = null,
  contentDescriptionFa: String? = null,
  centerContent: (@Composable () -> Unit)? = null,
) {
  val clampedTarget = progress.coerceIn(0f, 1f)
  val animatedProgress by
    animateFloatAsState(
      targetValue = clampedTarget,
      animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
      label = "manoHealthRingProgress",
    )
  val palette = rememberTurquoiseRingPalette(tone)
  val percentInt = (clampedTarget * 100f).roundToInt()
  val resolvedValueText = valueText ?: "${toPersianDigitsCompact(percentInt)}٪"
  val accessibilityDesc =
    contentDescriptionFa ?: "پیشرفت سلامت: ${toPersianDigitsCompact(percentInt)} درصد"

  Box(
    modifier =
      modifier
        .size(diameter)
        .semantics(mergeDescendants = true) {
          contentDescription = accessibilityDesc
          progressBarRangeInfo = ProgressBarRangeInfo(current = clampedTarget, range = 0f..1f)
        },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(diameter)) {
      val strokePx = strokeWidth.toPx()
      val inset = strokePx * 0.5f + 1.5.dp.toPx()
      val arcSize = Size(
        width = (this.size.width - inset * 2f).coerceAtLeast(1f),
        height = (this.size.height - inset * 2f).coerceAtLeast(1f),
      )
      val topLeft = Offset(inset, inset)
      val center = Offset(this.size.width * 0.5f, this.size.height * 0.5f)
      val radius = (arcSize.minDimension * 0.5f).coerceAtLeast(1f)

      // Outer minimal turquoise track
      drawCircle(
        color = palette.trackColor,
        radius = radius,
        center = center,
        style = Stroke(width = strokePx),
      )

      // Hairline clinical inner glass ring for depth
      val innerRadius = (radius - strokePx * 0.85f).coerceAtLeast(1f)
      drawCircle(
        color = palette.innerHairline,
        radius = innerRadius,
        center = center,
        style = Stroke(width = 1.dp.toPx()),
      )

      val sweep = (animatedProgress.coerceIn(0f, 1f) * 360f)
      if (sweep > 0.5f) {
        // Rotate gradient start to 12 o'clock (-90°) so sweep starts cleanly from top
        rotate(degrees = -90f, pivot = center) {
          drawArc(
            brush =
              Brush.sweepGradient(
                0.0f to palette.arcStart,
                0.55f to palette.arcMid,
                1.0f to palette.arcEnd,
                center = center,
              ),
            startAngle = 0f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
          )
        }

        // Crisp luminous dot at the active progress tip
        if (animatedProgress in 0.03f..0.99f) {
          val tipRad = ((-90f + sweep) * PI / 180.0)
          val tipOffset =
            Offset(
              x = center.x + (radius * cos(tipRad)).toFloat(),
              y = center.y + (radius * sin(tipRad)).toFloat(),
            )
          drawCircle(
            color = palette.tipGlow,
            radius = (strokePx * 0.28f).coerceAtLeast(1.2f),
            center = tipOffset,
          )
        }
      }
    }

    if (centerContent != null) {
      centerContent()
    } else {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = resolvedValueText,
          style =
            when (size) {
              ManoCircularProgressSize.HERO -> ManoTheme.typeScale.clinicalNumericMedium
              ManoCircularProgressSize.LARGE -> ManoTheme.typeScale.actionLabel
              else -> ManoTheme.typeScale.chipLabel
            },
          fontWeight = FontWeight.Bold,
          color = palette.inkColor,
          maxLines = 1,
          textAlign = TextAlign.Center,
        )
        if (!subLabel.isNullOrBlank() && (size == ManoCircularProgressSize.LARGE || size == ManoCircularProgressSize.HERO)) {
          Text(
            text = subLabel,
            style = ManoTheme.typeScale.microCaption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

/**
 * Minimal frosted-glass summary strip pairing a turquoise [ManoHealthProgressRing]
 * with clear clinical title, status caption, and optional right-side badge.
 */
@Composable
fun ManoHealthStatusRingCard(
  title: String,
  subtitle: String,
  progress: Float,
  modifier: Modifier = Modifier,
  valueText: String? = null,
  subLabel: String? = null,
  badgeText: String? = null,
  ringSize: ManoCircularProgressSize = ManoCircularProgressSize.LARGE,
  tone: ManoHealthRingTone = ManoHealthRingTone.TURQUOISE,
  badgeTestTag: String? = null,
) {
  val palette = rememberTurquoiseRingPalette(tone)
  Surface(
    color = manoCardSurface(),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, manoBorderColor()),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(ManoTheme.spacing.inlineGap),
    ) {
      ManoHealthProgressRing(
        progress = progress,
        size = ringSize,
        tone = tone,
        valueText = valueText,
        subLabel = subLabel,
        contentDescriptionFa = "$title: $subtitle",
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(ManoTheme.spacing.textPairGap),
      ) {
        Text(
          text = title,
          style = ManoTheme.typeScale.cardTitle,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = ManoTheme.typeScale.cardSubtitle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (!badgeText.isNullOrBlank()) {
        Surface(
          color = palette.trackColor,
          shape = RoundedCornerShape(50),
          modifier = if (badgeTestTag != null) Modifier.testTag(badgeTestTag) else Modifier,
        ) {
          Text(
            text = badgeText,
            style = ManoTheme.typeScale.chipLabel,
            fontWeight = FontWeight.Bold,
            color = palette.inkColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }
    }
  }
}

/**
 * Minimal frosted-glass data-loading card built on [ManoTurquoiseLoadingRing].
 */
@Composable
fun ManoCircularLoadingCard(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  ringSize: ManoCircularProgressSize = ManoCircularProgressSize.SMALL,
) {
  SectionCard(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(ManoTheme.spacing.inlineGap),
    ) {
      ManoTurquoiseLoadingRing(
        size = ringSize,
        contentDescriptionFa = title,
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(ManoTheme.spacing.textPairGap),
      ) {
        Text(
          text = title,
          style = ManoTheme.typeScale.cardTitle,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = ManoTheme.typeScale.cardSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

private fun toPersianDigitsCompact(value: Int): String {
  val digits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
  return value.toString().map { ch ->
    if (ch in '0'..'9') digits[ch - '0'] else ch
  }.joinToString("")
}
