package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ReviewStatus
import com.example.domain.util.ChildSex
import com.example.domain.util.GrowthMetric
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.PersianNormalizer
import com.example.domain.util.WhoGrowthStandardsEngine
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkJade
import com.example.ui.theme.DarkSky
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.JadeCareInk
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyCareInk
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.DarkSage
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhoGrowthChartCard(
  childProfile: ChildProfileDemo?,
  savedHeightCm: String,
  selectedAgeBand: AgeBand?,
  onOpen3DWeighingGuide: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val initialAgeMonths =
    remember(childProfile, selectedAgeBand) {
      when {
        childProfile != null -> LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(childProfile).coerceIn(0, 60)
        selectedAgeBand == AgeBand.NEWBORN -> 1
        selectedAgeBand == AgeBand.INFANT_3_12M -> 6
        selectedAgeBand == AgeBand.TODDLER_1_3Y -> 18
        selectedAgeBand == AgeBand.PRESCHOOL_3_6Y -> 42
        else -> 12
      }
    }

  val initialWeightStr =
    remember(childProfile) {
      val parsed = PersianNormalizer.toAsciiDigits(childProfile?.weightKgText.orEmpty()).toDoubleOrNull()
      if (parsed != null && parsed in 1.5..35.0) String.format(Locale.US, "%.1f", parsed) else "9.6"
    }
  val initialHeightStr =
    remember(savedHeightCm) {
      val parsed = PersianNormalizer.toAsciiDigits(savedHeightCm).toDoubleOrNull()
      if (parsed != null && parsed in 40.0..130.0) String.format(Locale.US, "%.1f", parsed) else "75.7"
    }

  var metric by rememberSaveable { mutableStateOf(GrowthMetric.WEIGHT_FOR_AGE) }
  var sex by rememberSaveable { mutableStateOf(ChildSex.BOY) }
  var ageInput by rememberSaveable { mutableStateOf(initialAgeMonths.toString()) }
  var weightInput by rememberSaveable { mutableStateOf(initialWeightStr) }
  var heightInput by rememberSaveable { mutableStateOf(initialHeightStr) }

  val activeValueInput = if (metric == GrowthMetric.WEIGHT_FOR_AGE) weightInput else heightInput
  val parsedAge = PersianNormalizer.toAsciiDigits(ageInput).toDoubleOrNull()
  val parsedValue = PersianNormalizer.toAsciiDigits(activeValueInput).toDoubleOrNull()

  val curves =
    remember(metric, sex) {
      WhoGrowthStandardsEngine.buildCurves(
        metric = metric,
        sex = sex,
        minMonth = 0.0,
        maxMonth = 60.0,
        stepMonths = 2.0,
      )
    }

  val assessment =
    remember(metric, sex, parsedAge, parsedValue) {
      if (parsedAge != null && parsedValue != null) {
        WhoGrowthStandardsEngine.evaluate(metric, sex, parsedAge, parsedValue)
      } else {
        null
      }
    }

  val isDark = LocalManoDarkTheme.current
  val chartCardBg = if (isDark) DarkSurface else PureWhite
  val skyAccent = if (isDark) DarkSky else SkyCareInk
  val sageAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val amberAccent = if (isDark) DarkGold else CautionAmber
  val dangerAccent = if (isDark) DarkCoral else DangerCrimson
  val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.32f else 0.45f)
  val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant

  Surface(
    color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 0.55f, darkAlpha = 0.16f),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.2.dp, skyAccent.copy(alpha = if (isDark) 0.36f else 0.28f)),
    modifier = modifier.fillMaxWidth().testTag("growth_chart_placeholder"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("who_growth_chart_card"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header + Reviewed Badge
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ShowChart,
            contentDescription = null,
            tint = skyAccent,
          )
          Column {
            Text(
              text = "منحنی استاندارد صدک رشد WHO (۰ تا ۶۰ ماه)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "جدول رسمی سازمان جهانی بهداشت • P3 تا P97",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        SourceBadge(reviewStatus = ReviewStatus.APPROVED)
      }

      // Metric & Sex Pickers (FlowRow safe for large fontScale)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GrowthMetric.entries.forEach { item ->
          SymptomChip(
            label = item.labelFa,
            selected = metric == item,
            onClick = { metric = item },
            modifier = Modifier.testTag(
              if (item == GrowthMetric.WEIGHT_FOR_AGE) "who_metric_weight" else "who_metric_height"
            ),
          )
        }
        ChildSex.entries.forEach { s ->
          SymptomChip(
            label = s.labelFa,
            selected = sex == s,
            onClick = { sex = s },
            modifier = Modifier.testTag(if (s == ChildSex.BOY) "who_sex_boy" else "who_sex_girl"),
          )
        }
      }

      // Quick age presets + editable inputs
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf(3, 6, 12, 18, 24, 36, 48).forEach { m ->
          SymptomChip(
            label = "$m ماه",
            selected = parsedAge?.toInt() == m,
            onClick = { ageInput = m.toString() },
            modifier = Modifier.testTag("who_age_preset_$m"),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = ageInput,
          onValueChange = { ageInput = PersianNormalizer.toAsciiDigits(it).filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
          label = { Text("سن (ماه، ۰ تا ۶۰)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f).testTag("who_age_input"),
        )
        OutlinedTextField(
          value = activeValueInput,
          onValueChange = { raw ->
            val sanitized = PersianNormalizer.toAsciiDigits(raw).filter { ch -> ch.isDigit() || ch == '.' }.take(6)
            if (metric == GrowthMetric.WEIGHT_FOR_AGE) weightInput = sanitized else heightInput = sanitized
          },
          label = {
            Text(
              if (metric == GrowthMetric.WEIGHT_FOR_AGE) "وزن (kg)" else "قد (cm)"
            )
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f).testTag("who_value_input"),
        )
      }

      // Canvas WHO Percentile Curve
      Surface(
        color = chartCardBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, manoBorderColor()),
        modifier = Modifier.fillMaxWidth(),
      ) {
        val textMeasurer = rememberTextMeasurer()
        val axisTextStyle =
          remember(axisTextColor) {
            TextStyle(
              color = axisTextColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
            )
          }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .height(225.dp)
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .testTag("who_growth_canvas"),
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val leftPad = 34.dp.toPx()
              val rightPad = 12.dp.toPx()
              val topPad = 10.dp.toPx()
              val bottomPad = 24.dp.toPx()
              val plotW = (size.width - leftPad - rightPad).coerceAtLeast(40f)
              val plotH = (size.height - topPad - bottomPad).coerceAtLeast(40f)

              val minY = floor(curves.p3.min()).toFloat()
              val rawMaxY = ceil(curves.p97.max()).toFloat()
              val maxY = (rawMaxY + (if (metric == GrowthMetric.WEIGHT_FOR_AGE) 1f else 4f)).coerceAtLeast(minY + 4f)
              val yRange = (maxY - minY).coerceAtLeast(1f)

              fun xForMonth(month: Double): Float =
                leftPad + ((month.toFloat().coerceIn(0f, 60f)) / 60f) * plotW

              fun yForValue(v: Double): Float =
                topPad + (1f - ((v.toFloat() - minY) / yRange).coerceIn(0f, 1f)) * plotH

              // Horizontal grid + Y labels
              val gridSteps = 4
              for (step in 0..gridSteps) {
                val frac = step / gridSteps.toFloat()
                val yVal = minY + frac * yRange
                val py = topPad + (1f - frac) * plotH
                drawLine(
                  color = gridLineColor,
                  start = Offset(leftPad, py),
                  end = Offset(leftPad + plotW, py),
                  strokeWidth = 1.dp.toPx(),
                )
                val label = String.format(Locale.US, "%.0f", yVal)
                val measured = textMeasurer.measure(label, axisTextStyle)
                drawText(
                  textLayoutResult = measured,
                  topLeft = Offset(2f, py - measured.size.height / 2f),
                )
              }

              // Vertical age ticks (0, 12, 24, 36, 48, 60m)
              listOf(0, 12, 24, 36, 48, 60).forEach { m ->
                val px = xForMonth(m.toDouble())
                drawLine(
                  color = gridLineColor.copy(alpha = 0.6f),
                  start = Offset(px, topPad),
                  end = Offset(px, topPad + plotH),
                  strokeWidth = 0.8.dp.toPx(),
                )
                val label = "${m}m"
                val measured = textMeasurer.measure(label, axisTextStyle)
                drawText(
                  textLayoutResult = measured,
                  topLeft = Offset(
                    (px - measured.size.width / 2f).coerceIn(leftPad, leftPad + plotW - measured.size.width),
                    topPad + plotH + 4.dp.toPx(),
                  ),
                )
              }

              // Shaded P15..P85 standard zone
              val bandPath = Path()
              curves.agesMonths.forEachIndexed { idx, month ->
                val x = xForMonth(month)
                val y = yForValue(curves.p85[idx])
                if (idx == 0) bandPath.moveTo(x, y) else bandPath.lineTo(x, y)
              }
              for (idx in curves.agesMonths.lastIndex downTo 0) {
                bandPath.lineTo(xForMonth(curves.agesMonths[idx]), yForValue(curves.p15[idx]))
              }
              bandPath.close()
              drawPath(bandPath, color = sageAccent.copy(alpha = if (isDark) 0.16f else 0.12f))

              fun buildCurvePath(values: List<Double>): Path =
                Path().apply {
                  values.forEachIndexed { idx, v ->
                    val x = xForMonth(curves.agesMonths[idx])
                    val y = yForValue(v)
                    if (idx == 0) moveTo(x, y) else lineTo(x, y)
                  }
                }

              val dashed = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
              drawPath(
                path = buildCurvePath(curves.p3),
                color = amberAccent.copy(alpha = 0.82f),
                style = Stroke(width = 1.6.dp.toPx(), pathEffect = dashed, cap = StrokeCap.Round),
              )
              drawPath(
                path = buildCurvePath(curves.p97),
                color = amberAccent.copy(alpha = 0.82f),
                style = Stroke(width = 1.6.dp.toPx(), pathEffect = dashed, cap = StrokeCap.Round),
              )
              drawPath(
                path = buildCurvePath(curves.p15),
                color = sageAccent.copy(alpha = 0.72f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
              )
              drawPath(
                path = buildCurvePath(curves.p85),
                color = sageAccent.copy(alpha = 0.72f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
              )
              drawPath(
                path = buildCurvePath(curves.p50),
                color = skyAccent,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
              )

              // Child Marker Point
              if (assessment != null) {
                val cx = xForMonth(assessment.ageMonths)
                val cy = yForValue(assessment.measuredValue)
                val pointColor = if (assessment.needsPediatricReview) dangerAccent else skyAccent

                drawLine(
                  color = pointColor.copy(alpha = 0.45f),
                  start = Offset(cx, topPad + plotH),
                  end = Offset(cx, cy),
                  strokeWidth = 1.dp.toPx(),
                  pathEffect = dashed,
                )
                drawCircle(
                  color = pointColor.copy(alpha = 0.24f),
                  radius = 10.dp.toPx(),
                  center = Offset(cx, cy),
                )
                drawCircle(
                  color = PureWhite,
                  radius = 6.dp.toPx(),
                  center = Offset(cx, cy),
                )
                drawCircle(
                  color = pointColor,
                  radius = 4.2.dp.toPx(),
                  center = Offset(cx, cy),
                )
              }
            }
          }
        }
      }

      // Color Legend (Wrapping cleanly on small widths / large fonts)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        WhoLegendChip("P50 میانه استاندارد", skyAccent)
        WhoLegendChip("P15–P85 محدوده رایج", sageAccent)
        WhoLegendChip("P3 و P97 مرز پیگیری", amberAccent)
      }

      // Live Assessment Summary
      if (assessment != null) {
        val resultSurface =
          if (assessment.needsPediatricReview) manoTintedSurface(AquaSurface, DarkCoral, lightAlpha = 0.25f, darkAlpha = 0.16f)
          else manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.75f, darkAlpha = 0.16f)
        val resultAccent = if (assessment.needsPediatricReview) dangerAccent else sageAccent

        Surface(
          color = resultSurface,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.2.dp, resultAccent.copy(alpha = 0.40f)),
          modifier = Modifier.fillMaxWidth().testTag("who_percentile_result"),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "صدک تخمینی: ${String.format(Locale.US, "%.1f", assessment.percentile)} (${assessment.bandLabelFa})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = resultAccent,
                modifier = Modifier.weight(1f),
              )
            }
            Text(
              text =
                "میانه استاندارد این سن (P50): ${String.format(Locale.US, "%.1f", assessment.p50)} ${metric.unitFa} • Z-score: ${String.format(Locale.US, "%.2f", assessment.zScore)}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = assessment.guidanceFa,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        Text(
          text = "برای مشاهده صدک واقعی WHO، سن (۰ تا ۶۰ ماه) و وزن یا قد معتبر را وارد کنید.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      LtrText(text = "WHO Child Growth Standards (2006) — who.int/tools/child-growth-standards")

      OutlinedButton(
        onClick = onOpen3DWeighingGuide,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("growth_open_3d_weighing_btn"),
      ) {
        Icon(Icons.Rounded.MonitorWeight, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("راهنمای سه‌بعدی وزن‌کشی صحیح با مانکن کودک")
      }
    }
  }
}

@Composable
private fun WhoLegendChip(label: String, color: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Box(
      modifier = Modifier.size(10.dp).clip(CircleShape).background(color)
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
