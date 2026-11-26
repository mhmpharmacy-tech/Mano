package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ManoDuoPalette
import com.example.ui.theme.ManoTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class DuoProductArt {
  HEADER_CARE,
  TAB_SUPPLEMENTS,
  TAB_HYGIENE,
  VITAMIN_D,
  IRON_DROPS,
  PROBIOTIC,
  ZINC_CREAM,
  GENTLE_WASH,
  ORAL_CARE,
}

private typealias DuoIconPalette = ManoDuoPalette

@Composable
private fun duoPalette(art: DuoProductArt): DuoIconPalette {
  val ill = ManoTheme.colors.illustration
  return when (art) {
    DuoProductArt.HEADER_CARE -> ill.duoHeaderCare
    DuoProductArt.TAB_SUPPLEMENTS -> ill.duoTabSupplements
    DuoProductArt.TAB_HYGIENE -> ill.duoTabHygiene
    DuoProductArt.VITAMIN_D -> ill.duoVitaminD
    DuoProductArt.IRON_DROPS -> ill.duoIronDrops
    DuoProductArt.PROBIOTIC -> ill.duoProbiotic
    DuoProductArt.ZINC_CREAM -> ill.duoZincCream
    DuoProductArt.GENTLE_WASH -> ill.duoGentleWash
    DuoProductArt.ORAL_CARE -> ill.duoOralCare
  }
}

/**
 * Custom duo-tone graphical icon badge harmonized with Care Home v3 SVG artwork.
 */
@Composable
fun SupplementsHygieneDuoIcon(
  art: DuoProductArt,
  modifier: Modifier = Modifier,
  size: Dp = 46.dp,
  contentDescription: String? = null,
) {
  val palette = duoPalette(art)
  val descModifier =
    if (contentDescription != null) {
      modifier.semantics { this.contentDescription = contentDescription }
    } else {
      modifier
    }

  Box(modifier = descModifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      val radius = w * 0.28f

      // Pastel rounded squircle tile backdrop + hairline border
      drawRoundRect(
        color = palette.backdrop,
        topLeft = Offset(w * 0.04f, h * 0.04f),
        size = Size(w * 0.92f, h * 0.92f),
        cornerRadius = CornerRadius(radius, radius),
      )
      drawRoundRect(
        color = palette.border,
        topLeft = Offset(w * 0.04f, h * 0.04f),
        size = Size(w * 0.92f, h * 0.92f),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = w * 0.025f),
      )

      when (art) {
        DuoProductArt.HEADER_CARE -> drawHeaderCareArt(w, h, palette)
        DuoProductArt.TAB_SUPPLEMENTS -> drawVitaminSunDropArt(w, h, palette)
        DuoProductArt.TAB_HYGIENE -> drawGentleBubblesShieldArt(w, h, palette)
        DuoProductArt.VITAMIN_D -> drawVitaminSunDropArt(w, h, palette)
        DuoProductArt.IRON_DROPS -> drawIronDropperArt(w, h, palette)
        DuoProductArt.PROBIOTIC -> drawProbioticBalanceArt(w, h, palette)
        DuoProductArt.ZINC_CREAM -> drawZincShieldTubeArt(w, h, palette)
        DuoProductArt.GENTLE_WASH -> drawGentleWashBottleArt(w, h, palette)
        DuoProductArt.ORAL_CARE -> drawOralCareSparkleArt(w, h, palette)
      }
    }
  }
}

private fun DrawScope.drawHeaderCareArt(w: Float, h: Float, p: DuoIconPalette) {
  // Soft leaf behind pharmacy bottle/drop
  val leaf =
    Path().apply {
      moveTo(w * 0.56f, h * 0.74f)
      cubicTo(w * 0.80f, h * 0.68f, w * 0.84f, h * 0.34f, w * 0.68f, h * 0.24f)
      cubicTo(w * 0.50f, h * 0.30f, w * 0.46f, h * 0.58f, w * 0.56f, h * 0.74f)
      close()
    }
  drawPath(leaf, color = p.secondary)

  // Care droplet / bottle silhouette
  val drop =
    Path().apply {
      moveTo(w * 0.38f, h * 0.22f)
      cubicTo(w * 0.56f, h * 0.42f, w * 0.56f, h * 0.74f, w * 0.38f, h * 0.74f)
      cubicTo(w * 0.20f, h * 0.74f, w * 0.20f, h * 0.42f, w * 0.38f, h * 0.22f)
      close()
    }
  drawPath(drop, color = p.surfaceFill)
  drawPath(drop, color = p.primary, style = Stroke(width = w * 0.045f, join = StrokeJoin.Round))

  // Cross inside droplet
  drawLine(
    color = p.primary,
    start = Offset(w * 0.38f, h * 0.46f),
    end = Offset(w * 0.38f, h * 0.62f),
    strokeWidth = w * 0.05f,
    cap = StrokeCap.Round,
  )
  drawLine(
    color = p.primary,
    start = Offset(w * 0.30f, h * 0.54f),
    end = Offset(w * 0.46f, h * 0.54f),
    strokeWidth = w * 0.05f,
    cap = StrokeCap.Round,
  )

  // Warm accent orb
  drawCircle(color = p.highlight, radius = w * 0.065f, center = Offset(w * 0.74f, h * 0.72f))
}

private fun DrawScope.drawVitaminSunDropArt(w: Float, h: Float, p: DuoIconPalette) {
  // Radiant vitamin sun behind dropper droplet
  val sunCenter = Offset(w * 0.62f, h * 0.36f)
  drawCircle(color = p.secondary, radius = w * 0.145f, center = sunCenter)
  for (i in 0 until 8) {
    val angle = (i * 45f) * (PI / 180f).toFloat()
    val r1 = w * 0.185f
    val r2 = w * 0.235f
    drawLine(
      color = p.primary,
      start = Offset(sunCenter.x + cos(angle) * r1, sunCenter.y + sin(angle) * r1),
      end = Offset(sunCenter.x + cos(angle) * r2, sunCenter.y + sin(angle) * r2),
      strokeWidth = w * 0.036f,
      cap = StrokeCap.Round,
    )
  }

  // Golden D3 drop foreground
  val drop =
    Path().apply {
      moveTo(w * 0.38f, h * 0.32f)
      cubicTo(w * 0.56f, h * 0.50f, w * 0.56f, h * 0.78f, w * 0.38f, h * 0.78f)
      cubicTo(w * 0.20f, h * 0.78f, w * 0.20f, h * 0.50f, w * 0.38f, h * 0.32f)
      close()
    }
  drawPath(drop, color = p.surfaceFill)
  drawPath(drop, color = p.primary, style = Stroke(width = w * 0.045f, join = StrokeJoin.Round))
  drawCircle(color = p.secondary, radius = w * 0.085f, center = Offset(w * 0.38f, h * 0.61f))
  drawCircle(color = p.highlight, radius = w * 0.032f, center = Offset(w * 0.34f, h * 0.56f))
}

private fun DrawScope.drawIronDropperArt(w: Float, h: Float, p: DuoIconPalette) {
  // Bottle body
  drawRoundRect(
    color = p.surfaceFill,
    topLeft = Offset(w * 0.25f, h * 0.42f),
    size = Size(w * 0.36f, h * 0.36f),
    cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
  )
  // Fill level
  drawRoundRect(
    color = p.secondary,
    topLeft = Offset(w * 0.27f, h * 0.56f),
    size = Size(w * 0.32f, h * 0.20f),
    cornerRadius = CornerRadius(w * 0.07f, w * 0.07f),
  )
  drawRoundRect(
    color = p.primary,
    topLeft = Offset(w * 0.25f, h * 0.42f),
    size = Size(w * 0.36f, h * 0.36f),
    cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
    style = Stroke(width = w * 0.042f),
  )
  // Dropper cap & bulb
  drawRoundRect(
    color = p.primary,
    topLeft = Offset(w * 0.34f, h * 0.32f),
    size = Size(w * 0.18f, h * 0.11f),
    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
  )
  drawRoundRect(
    color = p.secondary,
    topLeft = Offset(w * 0.37f, h * 0.20f),
    size = Size(w * 0.12f, h * 0.14f),
    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
  )
  // Gentle iron drop on the side
  val ironDrop =
    Path().apply {
      moveTo(w * 0.72f, h * 0.44f)
      cubicTo(w * 0.83f, h * 0.57f, w * 0.83f, h * 0.74f, w * 0.72f, h * 0.74f)
      cubicTo(w * 0.61f, h * 0.74f, w * 0.61f, h * 0.57f, w * 0.72f, h * 0.44f)
      close()
    }
  drawPath(ironDrop, color = p.primary)
  drawCircle(color = p.highlight, radius = w * 0.042f, center = Offset(w * 0.72f, h * 0.28f))
}

private fun DrawScope.drawProbioticBalanceArt(w: Float, h: Float, p: DuoIconPalette) {
  // Two-tone friendly probiotic capsule
  drawRoundRect(
    color = p.surfaceFill,
    topLeft = Offset(w * 0.24f, h * 0.34f),
    size = Size(w * 0.52f, h * 0.30f),
    cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
  )
  drawRoundRect(
    color = p.secondary,
    topLeft = Offset(w * 0.49f, h * 0.34f),
    size = Size(w * 0.27f, h * 0.30f),
    cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
  )
  drawRoundRect(
    color = p.primary,
    topLeft = Offset(w * 0.24f, h * 0.34f),
    size = Size(w * 0.52f, h * 0.30f),
    cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
    style = Stroke(width = w * 0.042f),
  )
  drawLine(
    color = p.primary,
    start = Offset(w * 0.50f, h * 0.35f),
    end = Offset(w * 0.50f, h * 0.63f),
    strokeWidth = w * 0.036f,
  )
  // Friendly flora spheres
  drawCircle(color = p.highlight, radius = w * 0.07f, center = Offset(w * 0.31f, h * 0.24f))
  drawCircle(color = p.primary, radius = w * 0.045f, center = Offset(w * 0.68f, h * 0.76f))
  drawCircle(color = p.secondary, radius = w * 0.055f, center = Offset(w * 0.45f, h * 0.76f))
}

private fun DrawScope.drawZincShieldTubeArt(w: Float, h: Float, p: DuoIconPalette) {
  // Cream tube
  val tube =
    Path().apply {
      moveTo(w * 0.24f, h * 0.25f)
      lineTo(w * 0.54f, h * 0.25f)
      lineTo(w * 0.49f, h * 0.64f)
      lineTo(w * 0.29f, h * 0.64f)
      close()
    }
  drawPath(tube, color = p.surfaceFill)
  drawPath(tube, color = p.primary, style = Stroke(width = w * 0.04f, join = StrokeJoin.Round))
  // Cream stripe & cap
  drawLine(
    color = p.secondary,
    start = Offset(w * 0.33f, h * 0.38f),
    end = Offset(w * 0.45f, h * 0.38f),
    strokeWidth = w * 0.06f,
    cap = StrokeCap.Round,
  )
  drawRoundRect(
    color = p.primary,
    topLeft = Offset(w * 0.32f, h * 0.64f),
    size = Size(w * 0.14f, h * 0.11f),
    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
  )

  // Protective zinc shield badge
  val shield =
    Path().apply {
      moveTo(w * 0.67f, h * 0.36f)
      lineTo(w * 0.82f, h * 0.42f)
      lineTo(w * 0.80f, h * 0.60f)
      cubicTo(w * 0.78f, h * 0.70f, w * 0.67f, h * 0.76f, w * 0.67f, h * 0.76f)
      cubicTo(w * 0.67f, h * 0.76f, w * 0.56f, h * 0.70f, w * 0.54f, h * 0.60f)
      lineTo(w * 0.52f, h * 0.42f)
      close()
    }
  drawPath(shield, color = p.secondary)
  drawPath(shield, color = p.primary, style = Stroke(width = w * 0.036f, join = StrokeJoin.Round))
  drawCircle(color = p.highlight, radius = w * 0.045f, center = Offset(w * 0.67f, h * 0.26f))
}

private fun DrawScope.drawGentleWashBottleArt(w: Float, h: Float, p: DuoIconPalette) {
  // Pump top
  drawLine(
    color = p.primary,
    start = Offset(w * 0.44f, h * 0.24f),
    end = Offset(w * 0.44f, h * 0.36f),
    strokeWidth = w * 0.055f,
    cap = StrokeCap.Round,
  )
  drawLine(
    color = p.primary,
    start = Offset(w * 0.35f, h * 0.24f),
    end = Offset(w * 0.57f, h * 0.24f),
    strokeWidth = w * 0.055f,
    cap = StrokeCap.Round,
  )
  // Rounded shampoo bottle
  drawRoundRect(
    color = p.surfaceFill,
    topLeft = Offset(w * 0.26f, h * 0.36f),
    size = Size(w * 0.36f, h * 0.42f),
    cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
  )
  drawRoundRect(
    color = p.secondary,
    topLeft = Offset(w * 0.30f, h * 0.48f),
    size = Size(w * 0.28f, h * 0.22f),
    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
  )
  drawRoundRect(
    color = p.primary,
    topLeft = Offset(w * 0.26f, h * 0.36f),
    size = Size(w * 0.36f, h * 0.42f),
    cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
    style = Stroke(width = w * 0.042f),
  )
  // Gentle soap bubbles
  drawCircle(color = p.highlight, radius = w * 0.075f, center = Offset(w * 0.72f, h * 0.38f))
  drawCircle(color = p.primary, radius = w * 0.075f, center = Offset(w * 0.72f, h * 0.38f), style = Stroke(w * 0.025f))
  drawCircle(color = p.secondary, radius = w * 0.055f, center = Offset(w * 0.77f, h * 0.56f))
}

private fun DrawScope.drawGentleBubblesShieldArt(w: Float, h: Float, p: DuoIconPalette) {
  drawCircle(color = p.surfaceFill, radius = w * 0.19f, center = Offset(w * 0.43f, h * 0.54f))
  drawCircle(color = p.secondary, radius = w * 0.13f, center = Offset(w * 0.43f, h * 0.54f))
  drawCircle(color = p.primary, radius = w * 0.19f, center = Offset(w * 0.43f, h * 0.54f), style = Stroke(w * 0.04f))
  drawCircle(color = p.highlight, radius = w * 0.10f, center = Offset(w * 0.67f, h * 0.36f))
  drawCircle(color = p.primary, radius = w * 0.10f, center = Offset(w * 0.67f, h * 0.36f), style = Stroke(w * 0.032f))
  drawCircle(color = p.secondary, radius = w * 0.06f, center = Offset(w * 0.69f, h * 0.64f))
}

private fun DrawScope.drawOralCareSparkleArt(w: Float, h: Float, p: DuoIconPalette) {
  // Cute rounded baby tooth
  val tooth =
    Path().apply {
      moveTo(w * 0.30f, h * 0.36f)
      cubicTo(w * 0.30f, h * 0.25f, w * 0.46f, h * 0.26f, w * 0.49f, h * 0.32f)
      cubicTo(w * 0.52f, h * 0.26f, w * 0.68f, h * 0.25f, w * 0.68f, h * 0.36f)
      cubicTo(w * 0.69f, h * 0.53f, w * 0.64f, h * 0.74f, w * 0.56f, h * 0.74f)
      cubicTo(w * 0.52f, h * 0.74f, w * 0.51f, h * 0.59f, w * 0.49f, h * 0.59f)
      cubicTo(w * 0.47f, h * 0.59f, w * 0.46f, h * 0.74f, w * 0.42f, h * 0.74f)
      cubicTo(w * 0.34f, h * 0.74f, w * 0.29f, h * 0.53f, w * 0.30f, h * 0.36f)
      close()
    }
  drawPath(tooth, color = p.surfaceFill)
  drawPath(tooth, color = p.primary, style = Stroke(width = w * 0.042f, join = StrokeJoin.Round))

  // Soft bristles brush strip across bottom-left
  drawRoundRect(
    color = p.secondary,
    topLeft = Offset(w * 0.20f, h * 0.60f),
    size = Size(w * 0.24f, h * 0.14f),
    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
  )
  // Mint sparkle dot
  drawCircle(color = p.highlight, radius = w * 0.065f, center = Offset(w * 0.72f, h * 0.26f))
}
