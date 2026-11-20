package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.DeepScreen
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.DeepTealGlass
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.TealGlow

enum class PediatricIconKind {
  FEVER,
  COUGH,
  STOMACH,
  DIAPER,
  HYDRATION,
  MEDICINE,
  SCANNER,
  FORMULA,
  DEVICE,
  GROWTH,
  URGENT,
  FAQ,
  PHARMACY,
  SAFETY_SHIELD,
  COMPARE_LAB,
  FOLLOW_UP,
  VACCINE,
  SUPPLEMENTS,
  REMINDERS,
  FAMILY,
  DENTAL,
  HAIR,
}

@DrawableRes
private fun PediatricIconKind.brandSheetDrawableOrNull(): Int? =
  when (this) {
    PediatricIconKind.FEVER -> R.drawable.mano_symptoms_orbit
    PediatricIconKind.MEDICINE -> R.drawable.ic_brand_medicines
    PediatricIconKind.SCANNER -> R.drawable.ic_brand_scanner
    PediatricIconKind.GROWTH -> R.drawable.mano_growth_orbit
    PediatricIconKind.DIAPER -> R.drawable.ic_brand_skin
    PediatricIconKind.FAQ -> R.drawable.mano_questions_orbit
    PediatricIconKind.SAFETY_SHIELD -> R.drawable.ic_brand_safety
    PediatricIconKind.COMPARE_LAB -> R.drawable.ic_brand_labs
    PediatricIconKind.FOLLOW_UP -> R.drawable.ic_brand_followup
    PediatricIconKind.URGENT -> R.drawable.ic_brand_illness
    PediatricIconKind.FORMULA, PediatricIconKind.SUPPLEMENTS -> R.drawable.ic_brand_supplements
    PediatricIconKind.VACCINE -> R.drawable.ic_brand_vaccines
    PediatricIconKind.REMINDERS -> R.drawable.ic_brand_reminders
    PediatricIconKind.FAMILY -> R.drawable.ic_brand_family_health
    PediatricIconKind.DENTAL -> R.drawable.ic_brand_dental
    PediatricIconKind.HAIR -> R.drawable.ic_brand_hair
    else -> null
  }

/**
 * Brand-sheet 3D Metallic & Glass pediatric icons:
 * Uses crisp extracted/generated studio 3D Metallic & Glass drawables for all core platform
 * categories, with matching Deep Screen (#0B1220) + Teal Glow (#00D7C7) + Clean White (#EAF5F7)
 * procedural Metallic & Glass Canvas icons for specialized auxiliary topics.
 */
@Composable
fun Mano3DIconArtwork(
  kind: PediatricIconKind,
  modifier: Modifier = Modifier,
  size: Dp = 28.dp,
) {
  val brandDrawable = kind.brandSheetDrawableOrNull()
  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    if (brandDrawable != null) {
      Image(
        painter = painterResource(id = brandDrawable),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(size * 0.24f)),
      )
    } else {
      Canvas(modifier = Modifier.fillMaxSize()) {
        when (kind) {
          PediatricIconKind.COUGH -> drawMetallicGlassCloud()
          PediatricIconKind.STOMACH -> drawMetallicGlassTummy()
          PediatricIconKind.HYDRATION -> drawMetallicGlassHydration()
          PediatricIconKind.DEVICE -> drawMetallicGlassDevice()
          PediatricIconKind.PHARMACY -> drawMetallicGlassPharmacy()
          else -> drawMetallicGlassPharmacy()
        }
      }
    }
  }
}

private fun DrawScope.drawMetallicGlassCloud() {
  val w = size.width
  val h = size.height
  drawCircle(
    brush = Brush.verticalGradient(listOf(PureWhite, CleanWhite, MetallicSilver)),
    radius = w * 0.22f,
    center = Offset(w * 0.34f, h * 0.42f),
  )
  drawCircle(
    brush = Brush.verticalGradient(listOf(PureWhite, CleanWhite, MetallicSilver)),
    radius = w * 0.25f,
    center = Offset(w * 0.54f, h * 0.35f),
  )
  drawCircle(
    brush = Brush.verticalGradient(listOf(PureWhite, CleanWhite, MetallicSilver)),
    radius = w * 0.19f,
    center = Offset(w * 0.72f, h * 0.45f),
  )
  drawRoundRect(
    brush = Brush.horizontalGradient(listOf(PureWhite, CleanWhite, MetallicSilver)),
    topLeft = Offset(w * 0.15f, h * 0.38f),
    size = Size(w * 0.72f, h * 0.26f),
    cornerRadius = CornerRadius(w * 0.13f),
  )
  drawRoundRect(
    color = DeepTealGlass.copy(alpha = 0.55f),
    topLeft = Offset(w * 0.15f, h * 0.38f),
    size = Size(w * 0.72f, h * 0.26f),
    cornerRadius = CornerRadius(w * 0.13f),
    style = Stroke(width = w * 0.032f),
  )
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
  drawPath(breeze1, color = DeepTeal, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
  drawPath(breeze2, color = TealGlow, style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))
}

private fun DrawScope.drawMetallicGlassTummy() {
  val w = size.width
  val h = size.height
  drawCircle(
    brush = Brush.radialGradient(listOf(CleanWhite, MetallicSilver, DeepTealGlass)),
    radius = w * 0.36f,
    center = Offset(w * 0.50f, h * 0.52f),
  )
  drawCircle(
    color = TealGlow.copy(alpha = 0.65f),
    radius = w * 0.36f,
    center = Offset(w * 0.50f, h * 0.52f),
    style = Stroke(width = w * 0.035f),
  )
  val wave =
    Path().apply {
      moveTo(w * 0.24f, h * 0.54f)
      cubicTo(w * 0.38f, h * 0.42f, w * 0.62f, h * 0.66f, w * 0.76f, h * 0.50f)
    }
  drawPath(wave, color = PureWhite, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round))
  drawPath(wave, color = TealGlow, style = Stroke(width = w * 0.055f, cap = StrokeCap.Round))
  drawCircle(color = PureWhite.copy(alpha = 0.86f), radius = w * 0.075f, center = Offset(w * 0.36f, h * 0.33f))
}

private fun DrawScope.drawMetallicGlassHydration() {
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
    brush = Brush.verticalGradient(listOf(TealGlow, SeaGreen, DeepTealGlass)),
  )
  drawArc(
    color = PureWhite.copy(alpha = 0.88f),
    startAngle = 150f,
    sweepAngle = 70f,
    useCenter = false,
    topLeft = Offset(w * 0.28f, h * 0.44f),
    size = Size(w * 0.26f, h * 0.30f),
    style = Stroke(width = w * 0.075f, cap = StrokeCap.Round),
  )
  drawCircle(color = DeepScreen, radius = w * 0.13f, center = Offset(w * 0.74f, h * 0.28f))
  drawLine(
    color = TealGlow,
    start = Offset(w * 0.74f, h * 0.22f),
    end = Offset(w * 0.74f, h * 0.34f),
    strokeWidth = w * 0.045f,
    cap = StrokeCap.Round,
  )
  drawLine(
    color = TealGlow,
    start = Offset(w * 0.68f, h * 0.28f),
    end = Offset(w * 0.80f, h * 0.28f),
    strokeWidth = w * 0.045f,
    cap = StrokeCap.Round,
  )
}

private fun DrawScope.drawMetallicGlassDevice() {
  val w = size.width
  val h = size.height
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(DeepTealGlass, DeepScreen)),
    topLeft = Offset(w * 0.20f, h * 0.50f),
    size = Size(w * 0.60f, h * 0.34f),
    cornerRadius = CornerRadius(w * 0.14f),
  )
  drawRoundRect(
    color = TealGlow.copy(alpha = 0.75f),
    topLeft = Offset(w * 0.20f, h * 0.50f),
    size = Size(w * 0.60f, h * 0.34f),
    cornerRadius = CornerRadius(w * 0.14f),
    style = Stroke(width = w * 0.032f),
  )
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(PureWhite, CleanWhite, MetallicSilver)),
    topLeft = Offset(w * 0.34f, h * 0.32f),
    size = Size(w * 0.32f, h * 0.22f),
    cornerRadius = CornerRadius(w * 0.10f),
  )
  drawCircle(color = TealGlow, radius = w * 0.085f, center = Offset(w * 0.38f, h * 0.20f))
  drawCircle(color = SeaGreen, radius = w * 0.07f, center = Offset(w * 0.56f, h * 0.16f))
  drawCircle(color = CleanWhite, radius = w * 0.055f, center = Offset(w * 0.68f, h * 0.25f))
}

private fun DrawScope.drawMetallicGlassPharmacy() {
  val w = size.width
  val h = size.height
  // Glossy Obsidian-Teal Glass Square with Luminous Teal Glow Medical Cross
  drawRoundRect(
    brush = Brush.linearGradient(listOf(DeepTealGlass, DeepScreen)),
    topLeft = Offset(w * 0.10f, h * 0.10f),
    size = Size(w * 0.80f, h * 0.80f),
    cornerRadius = CornerRadius(w * 0.24f),
  )
  drawRoundRect(
    brush = Brush.linearGradient(listOf(PureWhite.copy(alpha = 0.78f), TealGlow.copy(alpha = 0.80f))),
    topLeft = Offset(w * 0.10f, h * 0.10f),
    size = Size(w * 0.80f, h * 0.80f),
    cornerRadius = CornerRadius(w * 0.24f),
    style = Stroke(width = w * 0.045f),
  )
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(PureWhite, TealGlow)),
    topLeft = Offset(w * 0.41f, h * 0.24f),
    size = Size(w * 0.18f, h * 0.52f),
    cornerRadius = CornerRadius(w * 0.07f),
  )
  drawRoundRect(
    brush = Brush.horizontalGradient(listOf(PureWhite, TealGlow)),
    topLeft = Offset(w * 0.24f, h * 0.41f),
    size = Size(w * 0.52f, h * 0.18f),
    cornerRadius = CornerRadius(w * 0.07f),
  )
}
