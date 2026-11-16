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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AquaCare
import com.example.ui.theme.CoralCare
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.GoldSurface
import com.example.ui.theme.MangoCare
import com.example.ui.theme.PlumCare
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SageCare
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.SkyCare
import com.example.ui.theme.StrawberryCare
import com.example.ui.theme.TealGlow

enum class PediatricIconKind {
  FEVER,
  COUGH,
  STOMACH,
  DIAPER,
  HYDRATION,
  MEDICINE,
  FORMULA,
  DEVICE,
  GROWTH,
  URGENT,
  FAQ,
  PHARMACY,
}

/**
 * Custom multi-color layered 3D-look pediatric icons drawn on Canvas
 * with sculpted shading, glossy specular highlights, and vibrant kid-friendly hues.
 */
@Composable
fun Mano3DIconArtwork(
  kind: PediatricIconKind,
  modifier: Modifier = Modifier,
  size: Dp = 28.dp,
) {
  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      when (kind) {
        PediatricIconKind.FEVER -> draw3DFeverThermometer()
        PediatricIconKind.COUGH -> draw3DBreathingCloud()
        PediatricIconKind.STOMACH -> draw3DTummyCare()
        PediatricIconKind.DIAPER -> draw3DSkinShield()
        PediatricIconKind.HYDRATION -> draw3DHydrationDrop()
        PediatricIconKind.MEDICINE -> draw3DMedicineCapsuleAndDrop()
        PediatricIconKind.FORMULA -> draw3DFeedingBottle()
        PediatricIconKind.DEVICE -> draw3DNebulizerMist()
        PediatricIconKind.GROWTH -> draw3DGrowthSproutAndStar()
        PediatricIconKind.URGENT -> draw3DUrgentHeartShield()
        PediatricIconKind.FAQ -> draw3DFaqBubbles()
        PediatricIconKind.PHARMACY -> draw3DPharmacyCross()
      }
    }
  }
}

private fun DrawScope.draw3DFeverThermometer() {
  val w = size.width
  val h = size.height
  rotate(degrees = 26f, pivot = center) {
    // Drop shadow behind thermometer body
    drawRoundRect(
      color = CoralCare.copy(alpha = 0.28f),
      topLeft = Offset(w * 0.40f, h * 0.14f),
      size = Size(w * 0.24f, h * 0.62f),
      cornerRadius = CornerRadius(w * 0.12f),
    )
    // Glossy pearl-sky tube
    drawRoundRect(
      brush = Brush.horizontalGradient(listOf(PureWhite, Color(0xFFD9F3FF))),
      topLeft = Offset(w * 0.37f, h * 0.10f),
      size = Size(w * 0.24f, h * 0.62f),
      cornerRadius = CornerRadius(w * 0.12f),
    )
    // Mercury stem
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(MangoCare, CoralCare, DangerCrimson)),
      topLeft = Offset(w * 0.445f, h * 0.26f),
      size = Size(w * 0.09f, h * 0.44f),
      cornerRadius = CornerRadius(w * 0.045f),
    )
    // 3D Bulb shadow & sphere
    drawCircle(
      color = CoralCare.copy(alpha = 0.35f),
      radius = w * 0.19f,
      center = Offset(w * 0.51f, h * 0.77f),
    )
    drawCircle(
      brush = Brush.radialGradient(listOf(Color(0xFFFF8FA3), CoralCare, DangerCrimson)),
      radius = w * 0.18f,
      center = Offset(w * 0.49f, h * 0.75f),
    )
    // Specular gloss dot on bulb
    drawCircle(
      color = PureWhite.copy(alpha = 0.85f),
      radius = w * 0.055f,
      center = Offset(w * 0.43f, h * 0.70f),
    )
    // Degree ticks
    for (i in 0..2) {
      val y = h * (0.24f + i * 0.12f)
      drawLine(
        color = SkyCare,
        start = Offset(w * 0.55f, y),
        end = Offset(w * 0.63f, y),
        strokeWidth = w * 0.055f,
        cap = StrokeCap.Round,
      )
    }
  }
  // Golden fever star accent
  drawCircle(
    color = MangoCare,
    radius = w * 0.075f,
    center = Offset(w * 0.22f, h * 0.24f),
  )
}

private fun DrawScope.draw3DBreathingCloud() {
  val w = size.width
  val h = size.height
  // Sky-lavender puffy 3D cloud
  drawCircle(
    brush = Brush.verticalGradient(listOf(PureWhite, Color(0xFFCDEAFF))),
    radius = w * 0.22f,
    center = Offset(w * 0.34f, h * 0.42f),
  )
  drawCircle(
    brush = Brush.verticalGradient(listOf(PureWhite, Color(0xFFC2E3FF))),
    radius = w * 0.25f,
    center = Offset(w * 0.54f, h * 0.35f),
  )
  drawCircle(
    brush = Brush.verticalGradient(listOf(PureWhite, Color(0xFFD1DCFF))),
    radius = w * 0.19f,
    center = Offset(w * 0.72f, h * 0.45f),
  )
  drawRoundRect(
    brush = Brush.horizontalGradient(listOf(Color(0xFFE8F7FF), Color(0xFFB5DEFF))),
    topLeft = Offset(w * 0.15f, h * 0.38f),
    size = Size(w * 0.72f, h * 0.26f),
    cornerRadius = CornerRadius(w * 0.13f),
  )
  // Breeze swirl ribbons in turquoise & mint
  val breeze1 =
    Path().apply {
      moveTo(w * 0.18f, h * 0.74f)
      cubicTo(w * 0.38f, h * 0.67f, w * 0.56f, h * 0.80f, w * 0.78f, h * 0.71f)
    }
  val breeze2 =
    Path().apply {
      moveTo(w * 0.26f, h * 0.86f)
      cubicTo(w * 0.44f, h * 0.81f, w * 0.60f, h * 0.90f, w * 0.72f, h * 0.83f)
    }
  drawPath(breeze1, color = SkyCare, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
  drawPath(breeze2, color = TealGlow, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))
}

private fun DrawScope.draw3DTummyCare() {
  val w = size.width
  val h = size.height
  // Rounded golden-peach cushion heart
  drawCircle(
    brush = Brush.radialGradient(listOf(Color(0xFFFFD885), MangoCare)),
    radius = w * 0.34f,
    center = Offset(w * 0.50f, h * 0.52f),
  )
  // Mint soothing wave band across center
  val wave =
    Path().apply {
      moveTo(w * 0.22f, h * 0.54f)
      cubicTo(w * 0.38f, h * 0.42f, w * 0.62f, h * 0.66f, w * 0.78f, h * 0.50f)
    }
  drawPath(wave, color = PureWhite, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round))
  drawPath(wave, color = SageCare, style = Stroke(width = w * 0.055f, cap = StrokeCap.Round))
  // Specular bubble & coral accent
  drawCircle(color = PureWhite.copy(alpha = 0.80f), radius = w * 0.08f, center = Offset(w * 0.36f, h * 0.32f))
  drawCircle(color = CoralCare, radius = w * 0.09f, center = Offset(w * 0.76f, h * 0.26f))
}

private fun DrawScope.draw3DSkinShield() {
  val w = size.width
  val h = size.height
  // Berry-lavender soft rounded droplet-shield
  val shield =
    Path().apply {
      moveTo(w * 0.50f, h * 0.12f)
      cubicTo(w * 0.84f, h * 0.22f, w * 0.86f, h * 0.62f, w * 0.50f, h * 0.88f)
      cubicTo(w * 0.14f, h * 0.62f, w * 0.16f, h * 0.22f, w * 0.50f, h * 0.12f)
      close()
    }
  drawPath(
    path = shield,
    brush = Brush.verticalGradient(listOf(Color(0xFFB394FF), PlumCare)),
  )
  // Glossy inner water drop
  val drop =
    Path().apply {
      moveTo(w * 0.50f, h * 0.30f)
      cubicTo(w * 0.66f, h * 0.48f, w * 0.65f, h * 0.68f, w * 0.50f, h * 0.70f)
      cubicTo(w * 0.35f, h * 0.68f, w * 0.34f, h * 0.48f, w * 0.50f, h * 0.30f)
      close()
    }
  drawPath(drop, brush = Brush.verticalGradient(listOf(PureWhite, Color(0xFF9DE6FF))))
  // Fresh mint leaf accent
  drawCircle(color = SageCare, radius = w * 0.11f, center = Offset(w * 0.74f, h * 0.28f))
  drawCircle(color = PureWhite.copy(alpha = 0.75f), radius = w * 0.04f, center = Offset(w * 0.71f, h * 0.25f))
}

private fun DrawScope.draw3DHydrationDrop() {
  val w = size.width
  val h = size.height
  val drop =
    Path().apply {
      moveTo(w * 0.50f, h * 0.10f)
      cubicTo(w * 0.84f, h * 0.44f, w * 0.82f, h * 0.88f, w * 0.50f, h * 0.88f)
      cubicTo(w * 0.18f, h * 0.88f, w * 0.16f, h * 0.44f, w * 0.50f, h * 0.10f)
      close()
    }
  drawPath(
    path = drop,
    brush = Brush.verticalGradient(listOf(Color(0xFF7BE8FF), SkyCare, AquaCare)),
  )
  // Glossy crescent highlight
  drawArc(
    color = PureWhite.copy(alpha = 0.88f),
    startAngle = 150f,
    sweepAngle = 70f,
    useCenter = false,
    topLeft = Offset(w * 0.28f, h * 0.44f),
    size = Size(w * 0.26f, h * 0.30f),
    style = Stroke(width = w * 0.075f, cap = StrokeCap.Round),
  )
  // Emerald health sparkle dot
  drawCircle(color = SageCare, radius = w * 0.12f, center = Offset(w * 0.74f, h * 0.28f))
  drawLine(
    color = PureWhite,
    start = Offset(w * 0.74f, h * 0.22f),
    end = Offset(w * 0.74f, h * 0.34f),
    strokeWidth = w * 0.045f,
    cap = StrokeCap.Round,
  )
  drawLine(
    color = PureWhite,
    start = Offset(w * 0.68f, h * 0.28f),
    end = Offset(w * 0.80f, h * 0.28f),
    strokeWidth = w * 0.045f,
    cap = StrokeCap.Round,
  )
}

private fun DrawScope.draw3DMedicineCapsuleAndDrop() {
  val w = size.width
  val h = size.height
  rotate(degrees = -34f, pivot = Offset(w * 0.46f, h * 0.48f)) {
    // Top coral half of 3D pill
    drawRoundRect(
      brush = Brush.horizontalGradient(listOf(Color(0xFFFF8899), CoralCare)),
      topLeft = Offset(w * 0.27f, h * 0.18f),
      size = Size(w * 0.38f, h * 0.36f),
      cornerRadius = CornerRadius(w * 0.19f),
    )
    // Bottom bright teal half
    drawRoundRect(
      brush = Brush.horizontalGradient(listOf(TealGlow, DeepTeal)),
      topLeft = Offset(w * 0.27f, h * 0.44f),
      size = Size(w * 0.38f, h * 0.38f),
      cornerRadius = CornerRadius(w * 0.19f),
    )
    // Crisp glossy highlight
    drawLine(
      color = PureWhite.copy(alpha = 0.82f),
      start = Offset(w * 0.36f, h * 0.28f),
      end = Offset(w * 0.36f, h * 0.66f),
      strokeWidth = w * 0.06f,
      cap = StrokeCap.Round,
    )
  }
  // Golden mL droplet badge on corner
  drawCircle(
    brush = Brush.radialGradient(listOf(GoldSurface, MangoCare)),
    radius = w * 0.16f,
    center = Offset(w * 0.76f, h * 0.72f),
  )
  drawCircle(color = PureWhite.copy(alpha = 0.85f), radius = w * 0.045f, center = Offset(w * 0.72f, h * 0.67f))
}

private fun DrawScope.draw3DFeedingBottle() {
  val w = size.width
  val h = size.height
  rotate(degrees = 18f, pivot = center) {
    // Nipple in sunny mango
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(Color(0xFFFFD878), MangoCare)),
      topLeft = Offset(w * 0.40f, h * 0.12f),
      size = Size(w * 0.20f, h * 0.16f),
      cornerRadius = CornerRadius(w * 0.10f),
    )
    // Collar ring in sky blue
    drawRoundRect(
      brush = Brush.horizontalGradient(listOf(Color(0xFF7AD0FF), SkyCare)),
      topLeft = Offset(w * 0.32f, h * 0.25f),
      size = Size(w * 0.36f, h * 0.12f),
      cornerRadius = CornerRadius(w * 0.06f),
    )
    // Warm milk bottle body
    drawRoundRect(
      brush = Brush.horizontalGradient(listOf(PureWhite, Color(0xFFFFF3D9))),
      topLeft = Offset(w * 0.31f, h * 0.36f),
      size = Size(w * 0.38f, h * 0.48f),
      cornerRadius = CornerRadius(w * 0.12f),
    )
    // Milk level fill
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(Color(0xFFFFE5AC), MangoCare.copy(alpha = 0.80f))),
      topLeft = Offset(w * 0.34f, h * 0.56f),
      size = Size(w * 0.32f, h * 0.25f),
      cornerRadius = CornerRadius(w * 0.09f),
    )
    // Measurement lines
    drawLine(color = AquaCare, start = Offset(w * 0.56f, h * 0.46f), end = Offset(w * 0.64f, h * 0.46f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
    drawLine(color = AquaCare, start = Offset(w * 0.56f, h * 0.56f), end = Offset(w * 0.64f, h * 0.56f), strokeWidth = w * 0.045f, cap = StrokeCap.Round)
  }
}

private fun DrawScope.draw3DNebulizerMist() {
  val w = size.width
  val h = size.height
  // Base body in vibrant aqua-teal
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(TealGlow, AquaCare)),
    topLeft = Offset(w * 0.22f, h * 0.50f),
    size = Size(w * 0.56f, h * 0.34f),
    cornerRadius = CornerRadius(w * 0.14f),
  )
  // Mask/nozzle dome in glossy white-sky
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(PureWhite, Color(0xFFCDEEFF))),
    topLeft = Offset(w * 0.34f, h * 0.32f),
    size = Size(w * 0.32f, h * 0.22f),
    cornerRadius = CornerRadius(w * 0.10f),
  )
  // Rising colorful mist bubbles
  drawCircle(color = SkyCare, radius = w * 0.09f, center = Offset(w * 0.38f, h * 0.20f))
  drawCircle(color = SageCare, radius = w * 0.075f, center = Offset(w * 0.56f, h * 0.16f))
  drawCircle(color = MangoCare, radius = w * 0.06f, center = Offset(w * 0.68f, h * 0.26f))
}

private fun DrawScope.draw3DGrowthSproutAndStar() {
  val w = size.width
  val h = size.height
  // Golden sunshine orb behind leaves
  drawCircle(
    brush = Brush.radialGradient(listOf(GoldSurface, AccentGold)),
    radius = w * 0.16f,
    center = Offset(w * 0.72f, h * 0.26f),
  )
  // Stem
  drawLine(
    color = SageCare,
    start = Offset(w * 0.48f, h * 0.84f),
    end = Offset(w * 0.48f, h * 0.42f),
    strokeWidth = w * 0.09f,
    cap = StrokeCap.Round,
  )
  // Left emerald leaf
  val leftLeaf =
    Path().apply {
      moveTo(w * 0.48f, h * 0.62f)
      cubicTo(w * 0.18f, h * 0.60f, w * 0.16f, h * 0.30f, w * 0.46f, h * 0.44f)
      close()
    }
  drawPath(leftLeaf, brush = Brush.linearGradient(listOf(Color(0xFF56E3B2), SageCare)))
  // Right bright lime-teal leaf
  val rightLeaf =
    Path().apply {
      moveTo(w * 0.48f, h * 0.54f)
      cubicTo(w * 0.76f, h * 0.52f, w * 0.78f, h * 0.24f, w * 0.48f, h * 0.38f)
      close()
    }
  drawPath(rightLeaf, brush = Brush.linearGradient(listOf(TealGlow, DeepTeal)))
  // Ground mound
  drawRoundRect(
    brush = Brush.horizontalGradient(listOf(MangoCare, CoralCare)),
    topLeft = Offset(w * 0.26f, h * 0.78f),
    size = Size(w * 0.44f, h * 0.10f),
    cornerRadius = CornerRadius(w * 0.05f),
  )
}

private fun DrawScope.draw3DUrgentHeartShield() {
  val w = size.width
  val h = size.height
  // Soft warm glow ring
  drawCircle(
    color = CoralCare.copy(alpha = 0.22f),
    radius = w * 0.44f,
    center = center,
  )
  // 3D Heart silhouette in vibrant coral-crimson gradient
  val heart =
    Path().apply {
      moveTo(w * 0.50f, h * 0.84f)
      cubicTo(w * 0.14f, h * 0.60f, w * 0.12f, h * 0.24f, w * 0.34f, h * 0.20f)
      cubicTo(w * 0.44f, h * 0.18f, w * 0.50f, h * 0.28f, w * 0.50f, h * 0.32f)
      cubicTo(w * 0.50f, h * 0.28f, w * 0.56f, h * 0.18f, w * 0.66f, h * 0.20f)
      cubicTo(w * 0.88f, h * 0.24f, w * 0.86f, h * 0.60f, w * 0.50f, h * 0.84f)
      close()
    }
  drawPath(
    path = heart,
    brush = Brush.verticalGradient(listOf(Color(0xFFFF7D8F), CoralCare, DangerCrimson)),
  )
  // Crisp white medical cross inside heart
  drawLine(
    color = PureWhite,
    start = Offset(w * 0.50f, h * 0.38f),
    end = Offset(w * 0.50f, h * 0.62f),
    strokeWidth = w * 0.10f,
    cap = StrokeCap.Round,
  )
  drawLine(
    color = PureWhite,
    start = Offset(w * 0.38f, h * 0.50f),
    end = Offset(w * 0.62f, h * 0.50f),
    strokeWidth = w * 0.10f,
    cap = StrokeCap.Round,
  )
  // Golden alert badge sparkle
  drawCircle(color = MangoCare, radius = w * 0.08f, center = Offset(w * 0.78f, h * 0.22f))
}

private fun DrawScope.draw3DFaqBubbles() {
  val w = size.width
  val h = size.height
  // Back sky-blue bubble
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(Color(0xFF7FD5FF), SkyCare)),
    topLeft = Offset(w * 0.34f, h * 0.18f),
    size = Size(w * 0.52f, h * 0.42f),
    cornerRadius = CornerRadius(w * 0.16f),
  )
  // Front berry-purple bubble
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(Color(0xFFB596FF), PlumCare)),
    topLeft = Offset(w * 0.14f, h * 0.36f),
    size = Size(w * 0.56f, h * 0.44f),
    cornerRadius = CornerRadius(w * 0.18f),
  )
  // Three colorful dots inside bubble
  drawCircle(color = PureWhite, radius = w * 0.055f, center = Offset(w * 0.28f, h * 0.58f))
  drawCircle(color = GoldSurface, radius = w * 0.055f, center = Offset(w * 0.42f, h * 0.58f))
  drawCircle(color = Color(0xFF9DF7DE), radius = w * 0.055f, center = Offset(w * 0.56f, h * 0.58f))
}

private fun DrawScope.draw3DPharmacyCross() {
  val w = size.width
  val h = size.height
  // Rounded emerald cross
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(TealGlow, SageCare)),
    topLeft = Offset(w * 0.36f, h * 0.16f),
    size = Size(w * 0.28f, h * 0.68f),
    cornerRadius = CornerRadius(w * 0.10f),
  )
  drawRoundRect(
    brush = Brush.horizontalGradient(listOf(TealGlow, DeepTeal)),
    topLeft = Offset(w * 0.16f, h * 0.36f),
    size = Size(w * 0.68f, h * 0.28f),
    cornerRadius = CornerRadius(w * 0.10f),
  )
  // Warm coral heart center
  drawCircle(color = PureWhite, radius = w * 0.12f, center = center)
  drawCircle(color = StrawberryCare, radius = w * 0.075f, center = center)
}
