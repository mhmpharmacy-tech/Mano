package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.util.FeverDoseCalculatorEngine
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.DarkAqua
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.TealGlow
import com.example.ui.theme.manoBorderColor
import kotlin.math.ceil

/**
 * High-contrast, clinical-minimal 2D graduated oral syringe visualization built in Jetpack Compose Canvas.
 * Replaces the unused 3D WebView with an instant, legible, zero-lag syringe guide for 2 AM dosing.
 */
@Composable
fun VisualSyringeCanvas(
  targetMl: Float,
  isDrops: Boolean,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  val maxScaleMl =
    when {
      isDrops -> maxOf(1.5f, ceil(targetMl * 1.25f * 2f) / 2f).coerceAtMost(3.0f)
      targetMl <= 5.0f -> 5.0f
      targetMl <= 10.0f -> 10.0f
      else -> maxOf(15.0f, ceil(targetMl / 5f) * 5f).coerceAtMost(20.0f)
    }

  val targetFraction = (targetMl / maxScaleMl).coerceIn(0f, 1f)
  val animatedFraction by
    animateFloatAsState(
      targetValue = targetFraction,
      animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
      label = "syringeFillFraction",
    )

  val syringePalette = ManoTheme.colors.illustration.syringe
  val liquidStart = syringePalette.liquidStart
  val liquidEnd = syringePalette.liquidEnd
  val barrelStroke = syringePalette.barrelStroke
  val barrelFill = syringePalette.barrelFill
  val plungerColor = syringePalette.plunger
  val tickColor = syringePalette.tick
  val markerColor = MaterialTheme.colorScheme.primary
  val mlLabelFa = FeverDoseCalculatorEngine.formatPersianDecimal(targetMl)
  val scaleMaxFa = FeverDoseCalculatorEngine.formatPersianDecimal(maxScaleMl)

  Surface(
    color = if (isDark) DarkSurfaceHigh.copy(alpha = 0.78f) else AquaSurface.copy(alpha = 0.75f),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, manoBorderColor()),
    modifier =
      modifier
        .fillMaxWidth()
        .semantics {
          contentDescription = "تصویر سرنگ مدرج پرشده تا خط $mlLabelFa سی‌سی از سرنگ $scaleMaxFa سی‌سی"
        }
        .testTag("visual_2d_syringe_canvas"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (isDrops) "راهنمای قطره‌چکان / سرنگ مدرج (${scaleMaxFa} سی‌سی)" else "راهنمای سرنگ مدرج (${scaleMaxFa} سی‌سی)",
          style = ManoTheme.typeScale.cardSubtitle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = "خط هدف: $mlLabelFa سی‌سی",
            style = ManoTheme.typeScale.chipLabel.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      // Draw syringe explicitly LTR so 0 mL starts near the nozzle on the left and fills rightward toward plunger.
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxWidth().height(112.dp)) {
          Canvas(modifier = Modifier.fillMaxWidth().height(112.dp)) {
            val w = size.width
            val h = size.height
            val centerY = h * 0.54f

            val nozzleWidth = w * 0.08f
            val nozzleHeight = h * 0.14f
            val barrelLeft = w * 0.10f
            val barrelRight = w * 0.78f
            val barrelWidth = barrelRight - barrelLeft
            val barrelHeight = h * 0.44f
            val barrelTop = centerY - barrelHeight / 2f

            // 1. Tip nozzle
            drawRoundRect(
              color = barrelFill,
              topLeft = Offset(barrelLeft - nozzleWidth, centerY - nozzleHeight / 2f),
              size = Size(nozzleWidth + 4.dp.toPx(), nozzleHeight),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
            drawRoundRect(
              color = barrelStroke,
              topLeft = Offset(barrelLeft - nozzleWidth, centerY - nozzleHeight / 2f),
              size = Size(nozzleWidth + 4.dp.toPx(), nozzleHeight),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
              style = Stroke(width = 1.6.dp.toPx()),
            )

            // 2. Barrel background
            drawRoundRect(
              color = barrelFill,
              topLeft = Offset(barrelLeft, barrelTop),
              size = Size(barrelWidth, barrelHeight),
              cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )

            // 3. Liquid fill up to animatedFraction
            val liquidWidth = (barrelWidth * animatedFraction).coerceAtLeast(0f)
            if (liquidWidth > 2f) {
              drawRoundRect(
                brush = Brush.horizontalGradient(listOf(liquidStart, liquidEnd), startX = barrelLeft, endX = barrelLeft + liquidWidth),
                topLeft = Offset(barrelLeft + 2.dp.toPx(), barrelTop + 2.dp.toPx()),
                size = Size((liquidWidth - 2.dp.toPx()).coerceAtLeast(2f), barrelHeight - 4.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
              )
            }

            // 4. Black rubber plunger stopper at exact target edge
            val stopperWidth = 9.dp.toPx()
            val stopperX = (barrelLeft + liquidWidth).coerceIn(barrelLeft, barrelRight - stopperWidth)
            drawRoundRect(
              color = plungerColor,
              topLeft = Offset(stopperX, barrelTop + 3.dp.toPx()),
              size = Size(stopperWidth, barrelHeight - 6.dp.toPx()),
              cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            )

            // 5. Plunger rod & thumb rest extending right
            val rodEnd = (stopperX + stopperWidth + w * 0.16f).coerceAtMost(w - 10.dp.toPx())
            val rodHeight = barrelHeight * 0.28f
            drawRect(
              color = barrelStroke.copy(alpha = 0.55f),
              topLeft = Offset(stopperX + stopperWidth, centerY - rodHeight / 2f),
              size = Size((rodEnd - (stopperX + stopperWidth)).coerceAtLeast(10f), rodHeight),
            )
            // Thumb flange
            drawRoundRect(
              color = plungerColor,
              topLeft = Offset(rodEnd, centerY - barrelHeight * 0.42f),
              size = Size(8.dp.toPx(), barrelHeight * 0.84f),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )

            // 6. Barrel finger flange at barrelRight
            drawRoundRect(
              color = barrelStroke,
              topLeft = Offset(barrelRight - 2.dp.toPx(), barrelTop - 8.dp.toPx()),
              size = Size(5.dp.toPx(), barrelHeight + 16.dp.toPx()),
              cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            )

            // 7. Barrel outline & glossy highlight
            drawRoundRect(
              color = barrelStroke,
              topLeft = Offset(barrelLeft, barrelTop),
              size = Size(barrelWidth, barrelHeight),
              cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
              style = Stroke(width = 1.8.dp.toPx()),
            )
            drawLine(
              color = PureWhite.copy(alpha = if (isDark) 0.18f else 0.55f),
              start = Offset(barrelLeft + 8.dp.toPx(), barrelTop + 6.dp.toPx()),
              end = Offset(barrelRight - 8.dp.toPx(), barrelTop + 6.dp.toPx()),
              strokeWidth = 2.5.dp.toPx(),
              cap = StrokeCap.Round,
            )

            // 8. Graduation ticks along top edge of barrel
            val minorStep = if (isDrops) 0.1f else 0.5f
            val majorStep = if (isDrops) 0.5f else if (maxScaleMl <= 5f) 1.0f else if (maxScaleMl <= 10f) 2.0f else 5.0f
            val totalTicks = (maxScaleMl / minorStep).toInt()
            for (i in 0..totalTicks) {
              val mlVal = i * minorStep
              val frac = (mlVal / maxScaleMl).coerceIn(0f, 1f)
              val x = barrelLeft + barrelWidth * frac
              val isMajor = kotlin.math.abs((mlVal / majorStep) - kotlin.math.round(mlVal / majorStep)) < 0.04f
              val tickLen = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
              drawLine(
                color = tickColor,
                start = Offset(x, barrelTop),
                end = Offset(x, barrelTop + tickLen),
                strokeWidth = if (isMajor) 1.6.dp.toPx() else 1.0.dp.toPx(),
                cap = StrokeCap.Round,
              )
            }

            // 9. Target pointer triangle above stopper
            val arrowTipY = barrelTop - 4.dp.toPx()
            val arrowX = stopperX
            val pointerPath =
              Path().apply {
                moveTo(arrowX, arrowTipY)
                lineTo(arrowX - 6.dp.toPx(), arrowTipY - 10.dp.toPx())
                lineTo(arrowX + 6.dp.toPx(), arrowTipY - 10.dp.toPx())
                close()
              }
            drawPath(path = pointerPath, color = markerColor)
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "۰ سی‌سی (نوک سرنگ)",
          style = ManoTheme.typeScale.microCaption,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "لبه سیاه پیستون دقیقاً روی خط $mlLabelFa سی‌سی قرار گیرد",
          style = ManoTheme.typeScale.microCaption.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
