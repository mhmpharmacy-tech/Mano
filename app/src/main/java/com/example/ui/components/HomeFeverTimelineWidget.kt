package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CoralCare
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkPlum
import com.example.ui.theme.DarkSage
import com.example.ui.theme.PlumCare
import com.example.ui.theme.ShadowSlate
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoChartRailColor
import com.example.ui.theme.manoElevatedSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeFeverTimelineWidget(
  snapshot: FeverDoseTimelineSnapshot,
  onQuickSaveTemp: (tempCelsius: Float, caregiverFa: String) -> Unit,
  onOpenFullTimeline: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val primaryAccent = MaterialTheme.colorScheme.primary
  val shape = RoundedCornerShape(22.dp)

  var showQuickEntry by remember { mutableStateOf(false) }
  var quickTemp by remember { mutableFloatStateOf(38.2f) }
  var quickCaregiver by remember { mutableStateOf("مادر") }

  val tempPresets = remember { listOf(37.2f, 38.0f, 38.6f, 39.2f) }
  val caregivers = remember { listOf("مادر", "پدر") }
  val latestDose = snapshot.dosePoints.lastOrNull()
  val highFeverAlert = remember(snapshot) { FeverDoseTimelineEngine.evaluateHighFeverAlert(snapshot) }

  val cardSurface = manoCardSurface()
  val elevatedSurface = manoElevatedSurface()
  val borderColor = manoBorderColor()
  val chartRailColor = manoChartRailColor()
  val coralAccent = manoSemanticAccent(CoralCare, DarkCoral)
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val plumAccent = manoSemanticAccent(PlumCare, DarkPlum)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)

  Card(
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = cardSurface),
    border = BorderStroke(1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 6.dp,
          shape = shape,
          ambientColor = ShadowSlate.copy(alpha = 0.10f),
          spotColor = ShadowSlate.copy(alpha = 0.08f),
        )
        .testTag("home_fever_timeline_widget"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          ManoIconMedallion(
            icon = Icons.Rounded.Timeline,
            accentColor = coralAccent,
            iconKind = PediatricIconKind.FEVER,
            containerSize = 40.dp,
            iconSize = 22.dp,
            cornerRadius = 12.dp,
          )
          Column {
            Text("ویجت خط زمانی تب و دارو (۲۴س)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val sub =
              buildString {
                append(
                  snapshot.latestTempCelsius?.let { "آخرین دما: ${FeverDoseTimelineEngine.formatTempC(it)}" }
                    ?: "هنوز دمایی ثبت نشده"
                )
                append(" • ")
                append(
                  latestDose?.let { "${it.medicationNameFa.substringBefore(" (")} (${it.volumeMlText})" }
                    ?: "بدون نوبت دارو"
                )
              }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        snapshot.latestTempCelsius?.let { temp ->
          Surface(
            color =
              if (temp >= 38.0f) manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 1f, darkAlpha = 0.18f)
              else manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 1f, darkAlpha = 0.18f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = FeverDoseTimelineEngine.formatTempC(temp),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = if (temp >= 38.0f) dangerAccent else successAccent,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }

      if (highFeverAlert != null) {
        SeverityAlert(
          severity = AlertSeverity.DANGER,
          title = "اعلان فوری تب بالای ۳۹ درجه (${FeverDoseTimelineEngine.formatTempC(highFeverAlert.triggerTempCelsius)})",
          body = "برای ارزیابی وضعیت کودک به پزشک یا اورژانس مراجعه کنید.",
          actionLabel = "مشاهده هشدار",
          onAction = onOpenFullTimeline,
          modifier = Modifier.testTag("home_widget_high_fever_alert"),
        )
      }

      // Compact dual-track sparkline (fever line + dose dots)
      Surface(
        color = elevatedSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
      ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          Canvas(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 10.dp, vertical = 8.dp)
              .testTag("home_timeline_sparkline")
          ) {
            val w = size.width
            val h = size.height
            val tempTop = 4f
            val tempBottom = h * 0.64f
            val doseY = h * 0.85f
            val spanMs = (snapshot.windowEndMs - snapshot.windowStartMs).coerceAtLeast(1L).toFloat()

            fun xFor(ts: Long): Float = (((ts - snapshot.windowStartMs).toFloat() / spanMs).coerceIn(0f, 1f)) * w
            fun yForTemp(t: Float): Float {
              val norm = ((t - 36.0f) / (40.8f - 36.0f)).coerceIn(0f, 1f)
              return tempBottom - norm * (tempBottom - tempTop)
            }

            // 38.0°C threshold line
            val y38 = yForTemp(38.0f)
            drawLine(
              color = coralAccent.copy(alpha = 0.65f),
              start = Offset(0f, y38),
              end = Offset(w, y38),
              strokeWidth = 2f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
            )

            // Bottom dose rail
            drawRoundRect(
              color = chartRailColor,
              topLeft = Offset(0f, doseY - 5f),
              size = Size(w, 10f),
              cornerRadius = CornerRadius(5f, 5f),
            )

            val pts = snapshot.tempPoints
            if (pts.isNotEmpty()) {
              val path = Path()
              pts.forEachIndexed { idx, pt ->
                val px = xFor(pt.timestampMs)
                val py = yForTemp(pt.tempCelsius)
                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
              }
              drawPath(
                path = path,
                color = primaryAccent,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
              )
              pts.forEach { pt ->
                val color = if (pt.tempCelsius >= 38.0f) dangerAccent else successAccent
                drawCircle(color = color, radius = 5.5f, center = Offset(xFor(pt.timestampMs), yForTemp(pt.tempCelsius)))
              }
            }

            snapshot.dosePoints.forEach { dose ->
              val dx = xFor(dose.timestampMs)
              val doseColor =
                when {
                  FeverDoseTimelineEngine.isIbuprofenFamily(dose) -> plumAccent
                  FeverDoseTimelineEngine.isAcetaminophenFamily(dose) -> primaryAccent
                  else -> cautionAccent
                }
              drawCircle(color = doseColor, radius = 6.5f, center = Offset(dx, doseY))
              drawCircle(color = cardSurface, radius = 2.5f, center = Offset(dx, doseY))
            }
          }
        }
      }

      AnimatedVisibility(visible = showQuickEntry) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("دمای سریع (سانتی‌گراد):", style = MaterialTheme.typography.labelMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            tempPresets.forEach { preset ->
              FilterChip(
                selected = quickTemp == preset,
                onClick = { quickTemp = preset },
                label = { Text(FeverDoseTimelineEngine.formatTempC(preset)) },
                colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
                modifier = Modifier.minimumInteractiveComponentSize(),
              )
            }
          }
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            caregivers.forEach { parent ->
              FilterChip(
                selected = quickCaregiver == parent,
                onClick = { quickCaregiver = parent },
                label = { Text(parent) },
                colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
                modifier = Modifier.minimumInteractiveComponentSize(),
              )
            }
          }
          Button(
            onClick = {
              onQuickSaveTemp(quickTemp, quickCaregiver)
              showQuickEntry = false
            },
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("home_widget_save_temp_btn"),
          ) {
            Text("ذخیره ${FeverDoseTimelineEngine.formatTempC(quickTemp)} در خط زمانی")
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
          onClick = { showQuickEntry = !showQuickEntry },
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("home_widget_quick_log_btn"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(if (showQuickEntry) "بستن" else "ثبت سریع تب")
        }
        Button(
          onClick = onOpenFullTimeline,
          modifier = Modifier.weight(1.2f).minimumInteractiveComponentSize().testTag("home_timeline_widget_open_btn"),
        ) {
          Text("خط زمانی کامل و دارو")
        }
      }
    }
  }
}
