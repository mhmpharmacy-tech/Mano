package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.DeepScreen
import com.example.ui.theme.KidLime
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SparkYellow
import com.example.ui.theme.TealGlow
import com.example.ui.theme.VazirmatnFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Official MANO Kid sculptural brand crest matching the brand identity sheet:
 * - Glossy 3D metallic-teal "M" monogram (#00D7C7 -> #0092A4 -> #054B59) with chrome specular rim
 * - Tilted luminous orbital swoop ring wrapping behind and across the "M"
 * - Three joyful sunny-yellow (#FFD026) spark rays radiating from the top-right peak
 */
@Composable
fun ManoBrandCrest(
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  isDark: Boolean = false,
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height

    val deepShadow = Color(0xFF044454)
    val tealBody = Color(0xFF009AA8)
    val tealBright = TealGlow
    val chromeHighlight = Color(0xFFD8FFF9)

    // Soft ambient teal glow aura behind the 3D "M"
    drawCircle(
      brush =
        Brush.radialGradient(
          listOf(
            TealGlow.copy(alpha = if (isDark) 0.30f else 0.20f),
            Color.Transparent,
          )
        ),
      center = Offset(w * 0.48f, h * 0.56f),
      radius = w * 0.50f,
    )

    // Back half of the tilted orbital swoop ring (behind the M)
    rotate(degrees = -24f, pivot = Offset(w * 0.50f, h * 0.58f)) {
      drawArc(
        brush =
          Brush.linearGradient(
            colors = listOf(deepShadow.copy(alpha = 0.72f), TealGlow.copy(alpha = 0.85f)),
            start = Offset(w * 0.08f, h * 0.44f),
            end = Offset(w * 0.92f, h * 0.70f),
          ),
        startAngle = 195f,
        sweepAngle = 155f,
        useCenter = false,
        topLeft = Offset(w * 0.06f, h * 0.43f),
        size = Size(w * 0.88f, h * 0.30f),
        style = Stroke(width = w * 0.062f, cap = StrokeCap.Round),
      )
    }

    // 3D Sculpted Metallic-Teal "M" (deep bevel shadow layer)
    val mPath =
      Path().apply {
        moveTo(w * 0.24f, h * 0.78f)
        lineTo(w * 0.24f, h * 0.32f)
        cubicTo(w * 0.24f, h * 0.22f, w * 0.35f, h * 0.22f, w * 0.41f, h * 0.31f)
        lineTo(w * 0.50f, h * 0.46f)
        lineTo(w * 0.59f, h * 0.31f)
        cubicTo(w * 0.65f, h * 0.22f, w * 0.76f, h * 0.22f, w * 0.76f, h * 0.32f)
        lineTo(w * 0.76f, h * 0.78f)
      }
    drawPath(
      path = mPath,
      color = deepShadow.copy(alpha = 0.88f),
      style = Stroke(width = w * 0.175f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Primary metallic-glass body stroke of the "M"
    drawPath(
      path = mPath,
      brush =
        Brush.linearGradient(
          colors = listOf(tealBright, tealBody, deepShadow),
          start = Offset(w * 0.20f, h * 0.20f),
          end = Offset(w * 0.78f, h * 0.82f),
        ),
      style = Stroke(width = w * 0.145f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Glossy inner specular highlight crest running along the top-left ridge of "M"
    val mSpecular =
      Path().apply {
        moveTo(w * 0.225f, h * 0.73f)
        lineTo(w * 0.225f, h * 0.31f)
        cubicTo(w * 0.225f, h * 0.23f, w * 0.33f, h * 0.23f, w * 0.39f, h * 0.31f)
        lineTo(w * 0.49f, h * 0.45f)
        lineTo(w * 0.59f, h * 0.30f)
        cubicTo(w * 0.65f, h * 0.23f, w * 0.745f, h * 0.23f, w * 0.745f, h * 0.31f)
      }
    drawPath(
      path = mSpecular,
      brush =
        Brush.linearGradient(
          colors = listOf(chromeHighlight.copy(alpha = 0.88f), TealGlow.copy(alpha = 0.35f)),
          start = Offset(w * 0.22f, h * 0.22f),
          end = Offset(w * 0.75f, h * 0.60f),
        ),
      style = Stroke(width = w * 0.042f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Front luminous half of the tilted orbital swoop ring crossing across the "M"
    rotate(degrees = -24f, pivot = Offset(w * 0.50f, h * 0.58f)) {
      drawArc(
        brush =
          Brush.linearGradient(
            colors = listOf(tealBody, chromeHighlight, TealGlow),
            start = Offset(w * 0.06f, h * 0.58f),
            end = Offset(w * 0.94f, h * 0.52f),
          ),
        startAngle = 8f,
        sweepAngle = 166f,
        useCenter = false,
        topLeft = Offset(w * 0.06f, h * 0.43f),
        size = Size(w * 0.88f, h * 0.30f),
        style = Stroke(width = w * 0.070f, cap = StrokeCap.Round),
      )
      drawArc(
        color = PureWhite.copy(alpha = 0.82f),
        startAngle = 22f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(w * 0.07f, h * 0.435f),
        size = Size(w * 0.86f, h * 0.285f),
        style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
      )
    }

    // Three joyful sunny-yellow (#FFD026) spark rays at top-right shoulder of "M"
    val rayStroke = w * 0.048f
    drawLine(
      color = SparkYellow,
      start = Offset(w * 0.74f, h * 0.17f),
      end = Offset(w * 0.76f, h * 0.06f),
      strokeWidth = rayStroke,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = SparkYellow,
      start = Offset(w * 0.81f, h * 0.21f),
      end = Offset(w * 0.90f, h * 0.11f),
      strokeWidth = rayStroke,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = SparkYellow,
      start = Offset(w * 0.84f, h * 0.28f),
      end = Offset(w * 0.94f, h * 0.24f),
      strokeWidth = rayStroke,
      cap = StrokeCap.Round,
    )
  }
}

/**
 * Official brand-sheet "MANO [Kid]" wordmark lockup:
 * Bold geometric "MANO" in Deep Screen / Clean White paired with the signature
 * vibrant lime-green (#5FD000) rounded "Kid" pill and sunny yellow burst rays.
 */
@Composable
fun ManoKidWordmark(
  isDark: Boolean,
  modifier: Modifier = Modifier,
  fontSize: TextUnit = 18.sp,
) {
  val manoInk = if (isDark) CleanWhite else DeepScreen
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      @Suppress("DEPRECATION")
      val wordmarkPlatformStyle = PlatformTextStyle(includeFontPadding = false)
      Text(
        text = "MANO",
        style =
          TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontSize = fontSize,
            lineHeight = (fontSize.value + 3f).sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.0.sp,
            platformStyle = wordmarkPlatformStyle,
          ),
        color = manoInk,
      )
      Box(contentAlignment = Alignment.TopEnd) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(
                Brush.linearGradient(
                  listOf(Color(0xFF72E008), KidLime, Color(0xFF47B500))
                )
              )
              .padding(horizontal = 7.5.dp, vertical = 2.5.dp),
        ) {
          Text(
            text = "Kid",
            style =
              TextStyle(
                fontFamily = VazirmatnFontFamily,
                fontSize = (fontSize.value * 0.84f).sp,
                lineHeight = (fontSize.value * 0.96f).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.35.sp,
                platformStyle = wordmarkPlatformStyle,
              ),
            color = PureWhite,
          )
        }
        // Subtle 3-ray yellow burst above the top-right corner of the lime Kid pill
        Canvas(modifier = Modifier.size(12.dp).offset(x = 5.dp, y = (-5).dp)) {
          val w = size.width
          val h = size.height
          val sw = 1.8.dp.toPx()
          drawLine(SparkYellow, Offset(w * 0.28f, h * 0.72f), Offset(w * 0.24f, h * 0.16f), sw, StrokeCap.Round)
          drawLine(SparkYellow, Offset(w * 0.52f, h * 0.78f), Offset(w * 0.80f, h * 0.30f), sw, StrokeCap.Round)
          drawLine(SparkYellow, Offset(w * 0.62f, h * 0.94f), Offset(w * 0.98f, h * 0.78f), sw, StrokeCap.Round)
        }
      }
    }
  }
}

/**
 * Algorithmic parameterized Canvas iconography for MANO Kid's 5 core navigation pillars:
 * خانه (nav_home), علائم (nav_symptoms), داروها (nav_medicines), رشد (nav_growth), پرسش‌ها (nav_faq).
 *
 * Uses continuous `selectionProgress` (0f..1f) to morph dual-layer opacity, pulse nodes,
 * and harmonic geometric strokes smoothly without harsh corners.
 */
@Composable
fun ManoAlgorithmicNavIcon(
  tabTag: String,
  selectionProgress: Float,
  accentColor: Color,
  inactiveColor: Color,
  modifier: Modifier = Modifier,
) {
  val t = selectionProgress.coerceIn(0f, 1f)
  val tint = lerp(inactiveColor, accentColor, t)

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeW = w * (0.068f + 0.010f * t)

    if (t > 0.05f) {
      drawCircle(
        brush =
          Brush.radialGradient(
            listOf(
              accentColor.copy(alpha = 0.12f * t),
              Color.Transparent,
            )
          ),
        center = Offset(w * 0.5f, h * 0.5f),
        radius = w * 0.48f,
      )
    }

    when (tabTag) {
      "nav_home" -> drawHomeSanctuaryGlyph(w, h, strokeW, t, tint, accentColor)
      "nav_symptoms" -> drawSymptomPulseGlyph(w, h, strokeW, t, tint, accentColor)
      "nav_medicines" -> drawMedicineSyringeGlyph(w, h, strokeW, t, tint, accentColor)
      "nav_growth" -> drawGrowthBloomGlyph(w, h, strokeW, t, tint, accentColor)
      else -> drawDocumentedQaGlyph(w, h, strokeW, t, tint, accentColor)
    }
  }
}

private fun DrawScope.drawHomeSanctuaryGlyph(
  w: Float,
  h: Float,
  strokeW: Float,
  t: Float,
  tint: Color,
  accentColor: Color,
) {
  val bodyPath =
    Path().apply {
      moveTo(w * 0.20f, h * 0.45f)
      lineTo(w * 0.20f, h * 0.77f)
      quadraticTo(w * 0.20f, h * 0.85f, w * 0.28f, h * 0.85f)
      lineTo(w * 0.72f, h * 0.85f)
      quadraticTo(w * 0.80f, h * 0.85f, w * 0.80f, h * 0.77f)
      lineTo(w * 0.80f, h * 0.45f)
      lineTo(w * 0.50f, h * 0.20f)
      close()
    }
  if (t > 0.04f) {
    drawPath(path = bodyPath, color = accentColor.copy(alpha = 0.20f * t))
  }
  drawPath(
    path = bodyPath,
    color = tint,
    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
  )

  // Protective arched roof canopy
  val roofPath =
    Path().apply {
      moveTo(w * 0.12f, h * 0.48f)
      quadraticTo(w * 0.34f, h * 0.26f, w * 0.50f, h * 0.17f)
      quadraticTo(w * 0.66f, h * 0.26f, w * 0.88f, h * 0.48f)
    }
  drawPath(
    path = roofPath,
    color = tint,
    style = Stroke(width = strokeW * 1.08f, cap = StrokeCap.Round, join = StrokeJoin.Round),
  )

  // Caring inner heart in sanctuary center (blooms smoothly with selection)
  val heartScale = 0.85f + 0.18f * t
  val hx = w * 0.50f
  val hy = h * 0.67f
  val heartPath =
    Path().apply {
      moveTo(hx, hy)
      cubicTo(hx - w * 0.14f * heartScale, hy - h * 0.08f, hx - w * 0.15f * heartScale, hy - h * 0.20f, hx - w * 0.06f * heartScale, hy - h * 0.22f)
      cubicTo(hx - w * 0.02f, hy - h * 0.23f, hx, hy - h * 0.19f, hx, hy - h * 0.17f)
      cubicTo(hx, hy - h * 0.19f, hx + w * 0.02f, hy - h * 0.23f, hx + w * 0.06f * heartScale, hy - h * 0.22f)
      cubicTo(hx + w * 0.15f * heartScale, hy - h * 0.20f, hx + w * 0.14f * heartScale, hy - h * 0.08f, hx, hy)
      close()
    }
  drawPath(path = heartPath, color = tint.copy(alpha = 0.42f + 0.58f * t))
}

private fun DrawScope.drawSymptomPulseGlyph(
  w: Float,
  h: Float,
  strokeW: Float,
  t: Float,
  tint: Color,
  accentColor: Color,
) {
  // Algorithmic golden-curve clinical shield
  val shield =
    Path().apply {
      moveTo(w * 0.50f, h * 0.14f)
      cubicTo(w * 0.72f, h * 0.17f, w * 0.83f, h * 0.24f, w * 0.83f, h * 0.44f)
      cubicTo(w * 0.83f, h * 0.67f, w * 0.65f, h * 0.83f, w * 0.50f, h * 0.89f)
      cubicTo(w * 0.35f, h * 0.83f, w * 0.17f, h * 0.67f, w * 0.17f, h * 0.44f)
      cubicTo(w * 0.17f, h * 0.24f, w * 0.28f, h * 0.17f, w * 0.50f, h * 0.14f)
      close()
    }
  if (t > 0.04f) {
    drawPath(path = shield, color = accentColor.copy(alpha = 0.19f * t))
  }
  drawPath(
    path = shield,
    color = tint,
    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
  )

  // Dynamic calm ECG wave whose amplitude responds smoothly to selectionProgress
  val peak = 0.07f + 0.06f * t
  val pulsePath =
    Path().apply {
      moveTo(w * 0.26f, h * 0.52f)
      lineTo(w * 0.38f, h * 0.52f)
      lineTo(w * 0.45f, h * (0.52f - peak))
      lineTo(w * 0.53f, h * (0.52f + peak * 0.9f))
      lineTo(w * 0.60f, h * (0.52f - peak * 0.45f))
      lineTo(w * 0.66f, h * 0.52f)
      lineTo(w * 0.74f, h * 0.52f)
    }
  drawPath(
    path = pulsePath,
    color = tint,
    style = Stroke(width = strokeW * 0.92f, cap = StrokeCap.Round, join = StrokeJoin.Round),
  )
}

private fun DrawScope.drawMedicineSyringeGlyph(
  w: Float,
  h: Float,
  strokeW: Float,
  t: Float,
  tint: Color,
  accentColor: Color,
) {
  // Algorithmic tilted capsule / precision pediatric bottle + droplet
  rotate(degrees = -34f + 6f * t, pivot = Offset(w * 0.50f, h * 0.50f)) {
    val bodyRectTopLeft = Offset(w * 0.35f, h * 0.20f)
    val bodyRectSize = Size(w * 0.30f, h * 0.58f)
    if (t > 0.04f) {
      drawRoundRect(
        color = accentColor.copy(alpha = 0.20f * t),
        topLeft = bodyRectTopLeft,
        size = bodyRectSize,
        cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
      )
    }
    drawRoundRect(
      color = tint,
      topLeft = bodyRectTopLeft,
      size = bodyRectSize,
      cornerRadius = CornerRadius(w * 0.15f, w * 0.15f),
      style = Stroke(width = strokeW),
    )
    // Lower liquid fill segment
    drawRoundRect(
      color = tint.copy(alpha = 0.30f + 0.55f * t),
      topLeft = Offset(w * 0.375f, h * 0.50f),
      size = Size(w * 0.25f, h * 0.25f),
      cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
    )
    // Calibrated dosage scale ticks
    listOf(0.36f, 0.45f, 0.54f).forEach { ty ->
      drawLine(
        color = tint,
        start = Offset(w * 0.36f, h * ty),
        end = Offset(w * 0.45f, h * ty),
        strokeWidth = strokeW * 0.72f,
        cap = StrokeCap.Round,
      )
    }
  }
}

private fun DrawScope.drawGrowthBloomGlyph(
  w: Float,
  h: Float,
  strokeW: Float,
  t: Float,
  tint: Color,
  accentColor: Color,
) {
  // Grounded gentle hill baseline
  val ground =
    Path().apply {
      moveTo(w * 0.22f, h * 0.82f)
      quadraticTo(w * 0.50f, h * 0.74f, w * 0.78f, h * 0.82f)
    }
  drawPath(
    path = ground,
    color = tint.copy(alpha = 0.72f + 0.28f * t),
    style = Stroke(width = strokeW * 0.90f, cap = StrokeCap.Round),
  )

  // Rising stem
  drawLine(
    color = tint,
    start = Offset(w * 0.50f, h * 0.78f),
    end = Offset(w * 0.50f, h * (0.38f - 0.06f * t)),
    strokeWidth = strokeW,
    cap = StrokeCap.Round,
  )

  // Unfurling twin parametric leaves
  val spread = 0.04f * t
  val rightLeaf =
    Path().apply {
      moveTo(w * 0.50f, h * 0.56f)
      cubicTo(w * 0.52f, h * (0.34f - spread), w * 0.74f, h * (0.30f - spread), w * 0.82f, h * (0.36f - spread))
      cubicTo(w * 0.77f, h * 0.52f, w * 0.62f, h * 0.58f, w * 0.50f, h * 0.56f)
      close()
    }
  val leftLeaf =
    Path().apply {
      moveTo(w * 0.50f, h * 0.62f)
      cubicTo(w * 0.46f, h * (0.42f - spread), w * 0.27f, h * (0.39f - spread), w * 0.19f, h * (0.45f - spread))
      cubicTo(w * 0.24f, h * 0.58f, w * 0.39f, h * 0.63f, w * 0.50f, h * 0.62f)
      close()
    }
  if (t > 0.04f) {
    drawPath(rightLeaf, color = accentColor.copy(alpha = 0.22f * t))
    drawPath(leftLeaf, color = accentColor.copy(alpha = 0.18f * t))
  }
  drawPath(rightLeaf, color = tint, style = Stroke(width = strokeW * 0.95f, join = StrokeJoin.Round))
  drawPath(leftLeaf, color = tint, style = Stroke(width = strokeW * 0.95f, join = StrokeJoin.Round))

  // Milestone 4-point star at crown
  val starCx = w * 0.50f
  val starCy = h * 0.22f
  val starR = w * (0.09f + 0.04f * t)
  val starPath = Path()
  for (i in 0 until 8) {
    val angle = (i * PI / 4.0) - PI / 2.0
    val r = if (i % 2 == 0) starR else starR * 0.36f
    val px = starCx + (r * cos(angle)).toFloat()
    val py = starCy + (r * sin(angle)).toFloat()
    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
  }
  starPath.close()
  drawPath(starPath, color = tint)
}

private fun DrawScope.drawDocumentedQaGlyph(
  w: Float,
  h: Float,
  strokeW: Float,
  t: Float,
  tint: Color,
  accentColor: Color,
) {
  // Soft rounded conversation bubble with reassuring guide lines
  val bubblePath =
    Path().apply {
      moveTo(w * 0.24f, h * 0.20f)
      lineTo(w * 0.76f, h * 0.20f)
      quadraticTo(w * 0.86f, h * 0.20f, w * 0.86f, h * 0.30f)
      lineTo(w * 0.86f, h * 0.63f)
      quadraticTo(w * 0.86f, h * 0.73f, w * 0.76f, h * 0.73f)
      lineTo(w * 0.65f, h * 0.73f)
      lineTo(w * 0.52f, h * 0.85f)
      lineTo(w * 0.47f, h * 0.73f)
      lineTo(w * 0.24f, h * 0.73f)
      quadraticTo(w * 0.14f, h * 0.73f, w * 0.14f, h * 0.63f)
      lineTo(w * 0.14f, h * 0.30f)
      quadraticTo(w * 0.14f, h * 0.20f, w * 0.24f, h * 0.20f)
      close()
    }
  if (t > 0.04f) {
    drawPath(bubblePath, color = accentColor.copy(alpha = 0.20f * t))
  }
  drawPath(
    path = bubblePath,
    color = tint,
    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
  )

  // Serene curved question/answer guide bars + glowing verification node
  drawLine(
    color = tint,
    start = Offset(w * 0.30f, h * 0.39f),
    end = Offset(w * 0.58f, h * 0.39f),
    strokeWidth = strokeW * 0.90f,
    cap = StrokeCap.Round,
  )
  drawLine(
    color = tint.copy(alpha = 0.78f),
    start = Offset(w * 0.30f, h * 0.53f),
    end = Offset(w * 0.70f, h * 0.53f),
    strokeWidth = strokeW * 0.90f,
    cap = StrokeCap.Round,
  )
  drawCircle(
    color = tint,
    center = Offset(w * 0.70f, h * 0.39f),
    radius = w * (0.045f + 0.02f * t),
  )
}
