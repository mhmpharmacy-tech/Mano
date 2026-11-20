package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.SymptomFlow
import com.example.domain.util.AntipyreticIntervalStatus
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.domain.util.TimelineFeedItem
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.CoralCare
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkPlum
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoSurface
import com.example.ui.theme.PlumCare
import com.example.ui.theme.SageCare
import com.example.ui.theme.ShadowSlate
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoChartAxisHex
import com.example.ui.theme.manoChartGridColor
import com.example.ui.theme.manoChartRailColor
import com.example.ui.theme.manoElevatedSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeverDoseTimelineCard(
  snapshot: FeverDoseTimelineSnapshot,
  childProfile: ChildProfileDemo? = null,
  isCloudSynced: Boolean = false,
  onSaveTempLog: (tempCelsius: Float, siteFa: String, timeLabelFa: String, caregiverFa: String, noteFa: String, onDone: () -> Unit) -> Unit,
  onDeleteTempLog: (id: String, isCloud: Boolean) -> Unit,
  onDeleteDoseLog: (id: String, isCloud: Boolean) -> Unit = { _, _ -> },
  onOpenUrgentDoctorGuide: () -> Unit = {},
  selectedSymptomFlow: SymptomFlow? = null,
  savedVisitDraftsCount: Int = 0,
  onOpenVisitDraftStudio: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val primaryAccent = MaterialTheme.colorScheme.primary
  val shape = RoundedCornerShape(24.dp)

  var tempInput by remember { mutableFloatStateOf(38.1f) }
  var selectedSite by remember { mutableStateOf("زیربغلی") }
  var selectedCaregiver by remember { mutableStateOf("مادر") }
  var timeLabelInput by remember { mutableStateOf("") }
  var noteInput by remember { mutableStateOf("") }
  var inspectedEventKey by remember { mutableStateOf<String?>(null) }

  val siteOptions = remember { listOf("زیربغلی", "پیشانی", "گوشی") }
  val caregiverOptions = remember { listOf("مادر", "پدر", "پرستار/سایر") }

  val combinedFeed =
    remember(snapshot.tempPoints, snapshot.dosePoints) {
      FeverDoseTimelineEngine.mergedChronologicalEvents(snapshot)
    }

  val inspectedItem =
    remember(combinedFeed, inspectedEventKey) {
      combinedFeed.firstOrNull { it.key == inspectedEventKey }
    }

  val highFeverAlert =
    remember(snapshot, childProfile) {
      FeverDoseTimelineEngine.evaluateHighFeverAlert(snapshot, childProfile)
    }

  val cardSurface = manoCardSurface()
  val elevatedSurface = manoElevatedSurface()
  val borderColor = manoBorderColor()
  val chartGridColor = manoChartGridColor()
  val chartAxisHex = manoChartAxisHex()
  val chartRailColor = manoChartRailColor()
  val coralAccent = manoSemanticAccent(CoralCare, DarkCoral)
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val infoAccent = manoSemanticAccent(InfoBlue, DarkSky)
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val sageAccent = manoSemanticAccent(SageCare, DarkSage)
  val plumAccent = manoSemanticAccent(PlumCare, DarkPlum)

  Card(
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = cardSurface),
    border = BorderStroke(1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 8.dp,
          shape = shape,
          ambientColor = ShadowSlate.copy(alpha = 0.12f),
          spotColor = ShadowSlate.copy(alpha = 0.10f),
        )
        .testTag("fever_dose_timeline_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      // Header + Share pediatric 24h report action
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ManoIconMedallion(
            icon = Icons.Rounded.Timeline,
            accentColor = coralAccent,
            iconKind = PediatricIconKind.FEVER,
            containerSize = 44.dp,
            iconSize = 24.dp,
            cornerRadius = 14.dp,
          )
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "ویجت خط زمانی ۲۴ ساعته تب و دارو",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "ثبت دمای بدن و زمان‌های تب با نمایش هم‌زمان نوبت داروهای کودک",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
          onClick = {
            val reportText = FeverDoseTimelineEngine.buildDoctorSummaryReportFa(snapshot, childProfile)
            val sendIntent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "گزارش ۲۴ ساعته تب و داروی کودک — مانو کید")
                putExtra(Intent.EXTRA_TEXT, reportText)
              }
            runCatching {
              context.startActivity(Intent.createChooser(sendIntent, "ارسال گزارش ۲۴ ساعته برای پزشک"))
            }
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("timeline_share_doctor_btn"),
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("گزارش پزشک", style = MaterialTheme.typography.labelMedium)
        }
      }

      // Persistent Smart Urgent Banner when > 39.0°C is logged in timeline
      if (highFeverAlert != null) {
        SeverityAlert(
          severity = AlertSeverity.DANGER,
          title = highFeverAlert.titleFa,
          body = highFeverAlert.bodyFa,
          showCautionMascot = true,
          actionLabel = "اقدامات فوری و مراجعه به پزشک",
          onAction = onOpenUrgentDoctorGuide,
          modifier = Modifier.testTag("timeline_high_fever_urgent_banner"),
        )
      }

      // Sync badge + compact 24h summary KPI strip
      Surface(
        color =
          if (isCloudSynced) manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.6f, darkAlpha = 0.16f)
          else manoTintedSurface(InfoSurface, DarkSky, lightAlpha = 0.7f, darkAlpha = 0.16f),
        shape = RoundedCornerShape(14.dp),
        border =
          BorderStroke(
            1.dp,
            if (isCloudSynced) successAccent.copy(alpha = 0.30f) else infoAccent.copy(alpha = 0.25f),
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = if (isCloudSynced) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = if (isCloudSynced) successAccent else infoAccent,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text =
              if (isCloudSynced) {
                "همگام‌سازی زنده بین مادر و پدر فعال است"
              } else {
                "حالت آفلاین نشست (با ورود گوگل پایین صفحه، نوبت‌ها روی گوشی هر دو والد همگام می‌شود)"
              },
            style = MaterialTheme.typography.labelMedium,
            color = if (isCloudSynced) successAccent else infoAccent,
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TimelineMetricChip(
          labelFa = "آخرین دما",
          valueText = snapshot.latestTempCelsius?.let { FeverDoseTimelineEngine.formatTempC(it) } ?: "—",
          accent =
            if ((snapshot.latestTempCelsius ?: 0f) >= 38.0f) dangerAccent else successAccent,
          modifier = Modifier.weight(1f),
        )
        TimelineMetricChip(
          labelFa = "اوج ۲۴ ساعت",
          valueText = snapshot.peakTemp24h?.let { FeverDoseTimelineEngine.formatTempC(it) } ?: "—",
          accent =
            if ((snapshot.peakTemp24h ?: 0f) >= 38.0f) coralAccent else primaryAccent,
          modifier = Modifier.weight(1f),
        )
        TimelineMetricChip(
          labelFa = "نوبت‌های دارو",
          valueText = "${snapshot.dosePoints.size}",
          accent = sageAccent,
          modifier = Modifier.weight(1f),
        )
      }

      // Dual-track interactive 24h Compose Canvas chart (Temperature curve top + Dose markers bottom)
      Surface(
        color = elevatedSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "منحنی ۲۴ ساعت گذشته (خط قرمز: مرز تب ۳۸.۰°C)",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Text("● استامینوفن", style = MaterialTheme.typography.labelSmall, color = primaryAccent)
              Text("● ایبوپروفن", style = MaterialTheme.typography.labelSmall, color = plumAccent)
            }
          }

          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Canvas(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(160.dp)
                  .pointerInput(snapshot, combinedFeed) {
                    detectTapGestures { tapOffset ->
                      if (combinedFeed.isEmpty()) return@detectTapGestures
                      val padLeft = 44f
                      val padRight = 14f
                      val chartWidth = (size.width - padLeft - padRight).coerceAtLeast(1f)
                      val spanMs = (snapshot.windowEndMs - snapshot.windowStartMs).coerceAtLeast(1L).toFloat()
                      val tappedFrac = ((tapOffset.x - padLeft) / chartWidth).coerceIn(0f, 1f)
                      val tappedTs = snapshot.windowStartMs + (tappedFrac * spanMs).toLong()
                      val isDoseRowTap = tapOffset.y >= size.height * 0.68f
                      val candidates =
                        if (isDoseRowTap && snapshot.dosePoints.isNotEmpty()) {
                          combinedFeed.filterIsInstance<TimelineFeedItem.DoseEvent>()
                        } else if (!isDoseRowTap && snapshot.tempPoints.isNotEmpty()) {
                          combinedFeed.filterIsInstance<TimelineFeedItem.TemperatureEvent>()
                        } else {
                          combinedFeed
                        }
                      val nearest = candidates.minByOrNull { abs(it.timestampMs - tappedTs) }
                      inspectedEventKey = nearest?.key
                    }
                  }
                  .testTag("fever_timeline_canvas")
            ) {
              val w = size.width
              val h = size.height
              val padLeft = 44f
              val padRight = 14f
              val chartWidth = (w - padLeft - padRight).coerceAtLeast(1f)
              val tempTop = 12f
              val tempBottom = h * 0.65f
              val doseTrackY = h * 0.80f
              val minT = 36.0f
              val maxT = 40.8f
              val spanMs = (snapshot.windowEndMs - snapshot.windowStartMs).coerceAtLeast(1L).toFloat()

              fun yForTemp(t: Float): Float {
                val norm = ((t - minT) / (maxT - minT)).coerceIn(0f, 1f)
                return tempBottom - norm * (tempBottom - tempTop)
              }

              fun xForTime(ts: Long): Float {
                val frac = ((ts - snapshot.windowStartMs).toFloat() / spanMs).coerceIn(0f, 1f)
                return padLeft + frac * chartWidth
              }

              val gridPaint =
                android.graphics.Paint().apply {
                  color = chartAxisHex
                  textSize = 21f
                  textAlign = android.graphics.Paint.Align.RIGHT
                  isAntiAlias = true
                }
              listOf(37.0f, 38.0f, 39.5f).forEach { refTemp ->
                val y = yForTemp(refTemp)
                val isFeverThreshold = refTemp == 38.0f
                drawLine(
                  color =
                    if (isFeverThreshold) coralAccent.copy(alpha = 0.72f)
                    else chartGridColor,
                  start = Offset(padLeft, y),
                  end = Offset(w - padRight, y),
                  strokeWidth = if (isFeverThreshold) 2.6f else 1.3f,
                  pathEffect =
                    if (isFeverThreshold) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    else null,
                )
                drawContext.canvas.nativeCanvas.drawText(
                  String.format(Locale.US, "%.1f°", refTemp),
                  padLeft - 6f,
                  y + 7f,
                  gridPaint,
                )
              }

              drawRoundRect(
                color = chartRailColor,
                topLeft = Offset(padLeft, doseTrackY - 12f),
                size = Size(chartWidth, 24f),
                cornerRadius = CornerRadius(12f, 12f),
              )

              val axisPaint =
                android.graphics.Paint().apply {
                  color = chartAxisHex
                  textSize = 20f
                  textAlign = android.graphics.Paint.Align.CENTER
                  isAntiAlias = true
                }
              val timeLabels = listOf(0f to "-۲۴س", 0.5f to "-۱۲س", 1.0f to "اکنون")
              timeLabels.forEach { (frac, lbl) ->
                val tx = padLeft + chartWidth * frac
                drawContext.canvas.nativeCanvas.drawText(lbl, tx, h - 2f, axisPaint)
              }

              val pts = snapshot.tempPoints
              if (pts.isNotEmpty()) {
                val linePath = Path()
                val fillPath = Path()
                pts.forEachIndexed { index, pt ->
                  val x = xForTime(pt.timestampMs)
                  val y = yForTemp(pt.tempCelsius)
                  if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, tempBottom)
                    fillPath.lineTo(x, y)
                  } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                  }
                }
                val lastX = xForTime(pts.last().timestampMs)
                fillPath.lineTo(lastX, tempBottom)
                fillPath.close()

                drawPath(
                  path = fillPath,
                  brush =
                    Brush.verticalGradient(
                      colors = listOf(coralAccent.copy(alpha = 0.24f), Color.Transparent),
                      startY = tempTop,
                      endY = tempBottom,
                    ),
                )
                drawPath(
                  path = linePath,
                  color = primaryAccent,
                  style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                pts.forEach { pt ->
                  val cx = xForTime(pt.timestampMs)
                  val cy = yForTemp(pt.tempCelsius)
                  val dotColor = if (pt.tempCelsius >= 38.0f) dangerAccent else successAccent
                  if (inspectedEventKey == "temp_${pt.id}") {
                    drawCircle(
                      color = dotColor.copy(alpha = 0.28f),
                      radius = 16f,
                      center = Offset(cx, cy),
                    )
                  }
                  drawCircle(color = dotColor, radius = 8.5f, center = Offset(cx, cy))
                  drawCircle(color = cardSurface, radius = 3.5f, center = Offset(cx, cy))
                }
              }

              snapshot.dosePoints.forEach { dose ->
                val dx = xForTime(dose.timestampMs)
                val markerColor =
                  when {
                    FeverDoseTimelineEngine.isIbuprofenFamily(dose) -> plumAccent
                    FeverDoseTimelineEngine.isAcetaminophenFamily(dose) -> primaryAccent
                    else -> cautionAccent
                  }
                drawLine(
                  color = markerColor.copy(alpha = 0.28f),
                  start = Offset(dx, tempTop),
                  end = Offset(dx, doseTrackY),
                  strokeWidth = 1.8f,
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f),
                )
                if (inspectedEventKey == "dose_${dose.id}") {
                  drawCircle(
                    color = markerColor.copy(alpha = 0.28f),
                    radius = 17f,
                    center = Offset(dx, doseTrackY),
                  )
                }
                drawCircle(color = markerColor, radius = 9.5f, center = Offset(dx, doseTrackY))
                drawCircle(color = cardSurface, radius = 3.5f, center = Offset(dx, doseTrackY))
              }
            }
          }

          AnimatedVisibility(visible = inspectedItem != null) {
            inspectedItem?.let { item ->
              val pillText =
                when (item) {
                  is TimelineFeedItem.TemperatureEvent ->
                    "دمای انتخاب‌شده: ${FeverDoseTimelineEngine.formatTempC(item.temp.tempCelsius)} (${item.temp.siteFa}) • ساعت ${item.temp.timeLabelFa} • ${item.temp.caregiverFa}"
                  is TimelineFeedItem.DoseEvent ->
                    "نوبت انتخاب‌شده: ${item.dose.medicationNameFa} (${item.dose.volumeMlText}) • ساعت ${item.dose.givenAtLabelFa}"
                }
              Surface(
                color = cardSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("timeline_inspected_event_pill"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                  Text(pillText, style = MaterialTheme.typography.labelSmall, color = primaryAccent)
                }
              }
            }
          }

          if (snapshot.tempPoints.isEmpty() && snapshot.dosePoints.isEmpty()) {
            Text(
              text = "هنوز دما یا نوبتی در ۲۴ ساعت اخیر ثبت نشده؛ با فرم زیر اولین ثبت را انجام دهید تا موج تب و نوبت داروها هم‌زمان روی نمودار ترسیم شود.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else if (inspectedItem == null) {
            Text(
              text = "نکته: برای دیدن جزئیات دقیق، روی نقاط نمودار یا محور دارو ضربه بزنید.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Live Antipyretic Safe Interval Countdown Cards (prevents duplicate co-parent doses)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AntipyreticStatusBanner(
          status = snapshot.acetaminophenStatus,
          modifier = Modifier.testTag("timeline_acetaminophen_status"),
        )
        AntipyreticStatusBanner(
          status = snapshot.ibuprofenStatus,
          modifier = Modifier.testTag("timeline_ibuprofen_status"),
        )
      }

      VisitDraftEntryBannerCard(
        snapshot = snapshot,
        selectedSymptomFlow = selectedSymptomFlow,
        savedDraftsCount = savedVisitDraftsCount,
        buttonTag = "open_visit_draft_studio_btn",
        onOpenStudio = onOpenVisitDraftStudio,
        modifier = Modifier.testTag("timeline_visit_draft_banner"),
      )

      HorizontalDivider(color = borderColor)

      // Quick Temperature Logger Form (works offline/local AND cloud-synced)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("timeline_temp_form")) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.DeviceThermostat, contentDescription = null, tint = coralAccent, modifier = Modifier.size(20.dp))
            Text("ثبت دمای جدید کودک", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }
          Surface(
            color =
              if (tempInput >= 38.0f) manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 1f, darkAlpha = 0.18f)
              else manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 1f, darkAlpha = 0.18f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = FeverDoseTimelineEngine.formatTempC(tempInput),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (tempInput >= 38.0f) dangerAccent else successAccent,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp).testTag("timeline_temp_value_label"),
            )
          }
        }

        Slider(
          value = tempInput,
          onValueChange = { raw ->
            tempInput = ((raw * 10f).roundToInt() / 10f).coerceIn(35.5f, 41.0f)
          },
          valueRange = 35.5f..41.0f,
          colors =
            SliderDefaults.colors(
              thumbColor = if (tempInput >= 38.0f) dangerAccent else primaryAccent,
              activeTrackColor = if (tempInput >= 38.0f) dangerAccent else primaryAccent,
            ),
          modifier = Modifier.fillMaxWidth().testTag("timeline_temp_slider"),
        )

        if (tempInput > FeverDoseTimelineEngine.HIGH_FEVER_ALERT_THRESHOLD_C) {
          Surface(
            color = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 0.72f, darkAlpha = 0.16f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, dangerAccent.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().testTag("timeline_temp_over39_preview"),
          ) {
            Text(
              text = "هشدار هوشمند: دمای انتخاب‌شده بالای ۳۹.۰°C است؛ با ثبت آن، اعلان فوری مراجعه به پزشک فعال می‌شود.",
              style = MaterialTheme.typography.labelSmall,
              color = dangerAccent,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }

        Text("محل اندازه‌گیری:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          siteOptions.forEach { site ->
            FilterChip(
              selected = selectedSite == site,
              onClick = { selectedSite = site },
              label = { Text(site) },
              colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("site_chip_$site"),
            )
          }
        }

        Text("ثبت‌کننده نوبت (برای هماهنگی مادر و پدر):", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          caregiverOptions.forEach { parent ->
            FilterChip(
              selected = selectedCaregiver == parent,
              onClick = { selectedCaregiver = parent },
              label = { Text(parent) },
              colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("caregiver_chip_$parent"),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = timeLabelInput,
            onValueChange = { timeLabelInput = it },
            label = { Text("ساعت (اختیاری)") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("timeline_time_input"),
          )
          OutlinedTextField(
            value = noteInput,
            onValueChange = { noteInput = it },
            label = { Text("وضعیت کودک") },
            singleLine = true,
            modifier = Modifier.weight(1.4f).testTag("timeline_note_input"),
          )
        }

        Button(
          onClick = {
            val formattedTime =
              timeLabelInput.trim().ifEmpty {
                FeverDoseTimelineEngine.formatHourMinute(System.currentTimeMillis())
              }
            onSaveTempLog(
              tempInput,
              selectedSite,
              formattedTime,
              selectedCaregiver,
              noteInput.trim(),
            ) {
              timeLabelInput = ""
              noteInput = ""
            }
          },
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("timeline_save_temp_btn"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("ثبت دما در خط زمانی")
        }
      }

      // Unified Chronological Vertical Timeline Feed (temperatures + medication doses interleaved)
      if (combinedFeed.isNotEmpty()) {
        HorizontalDivider(color = borderColor)
        Text(
          text = "خط زمانی یکپارچه دماها و نوبت‌های دارو (۲۴ ساعت اخیر)",
          style = MaterialTheme.typography.labelLarge,
          color = primaryAccent,
        )
        Column(
          verticalArrangement = Arrangement.spacedBy(0.dp),
          modifier = Modifier.fillMaxWidth().testTag("timeline_unified_feed"),
        ) {
          combinedFeed.take(10).forEachIndexed { index, event ->
            val isLast = index == combinedFeed.take(10).lastIndex
            UnifiedTimelineNodeRow(
              item = event,
              isLast = isLast,
              isSelected = inspectedEventKey == event.key,
              onDelete = {
                when (event) {
                  is TimelineFeedItem.TemperatureEvent -> onDeleteTempLog(event.temp.id, event.temp.isSyncedCloud)
                  is TimelineFeedItem.DoseEvent -> onDeleteDoseLog(event.dose.id, event.dose.isSyncedCloud)
                }
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun UnifiedTimelineNodeRow(
  item: TimelineFeedItem,
  isLast: Boolean,
  isSelected: Boolean,
  onDelete: () -> Unit,
) {
  val primaryAccent = MaterialTheme.colorScheme.primary
  val borderColor = manoBorderColor()
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val plumAccent = manoSemanticAccent(PlumCare, DarkPlum)
  val (nodeColor, cardBg, titleText, subtitleText, iconVec, deleteLabel) =
    when (item) {
      is TimelineFeedItem.TemperatureEvent -> {
        val t = item.temp
        val isFever = t.tempCelsius >= 38.0f
        val color = if (isFever) dangerAccent else successAccent
        val bg =
          if (isFever) manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 0.45f, darkAlpha = 0.14f)
          else manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.40f, darkAlpha = 0.14f)
        val notePart = if (t.noteFa.isNotBlank()) " • ${t.noteFa}" else ""
        TimelineRowVisuals(
          nodeColor = color,
          cardBg = bg,
          titleText = "${FeverDoseTimelineEngine.formatTempC(t.tempCelsius)} (${t.siteFa}) — ${t.caregiverFa}",
          subtitleText = "ساعت ${t.timeLabelFa} • ${FeverDoseTimelineEngine.formatRelativeTimeFa(t.timestampMs)}$notePart",
          icon = Icons.Rounded.DeviceThermostat,
          deleteLabel = "حذف ثبت دما",
        )
      }
      is TimelineFeedItem.DoseEvent -> {
        val d = item.dose
        val isIbu = FeverDoseTimelineEngine.isIbuprofenFamily(d)
        val color = if (isIbu) plumAccent else primaryAccent
        val bg = color.copy(alpha = 0.12f)
        val notePart = if (d.noteFa.isNotBlank()) " • ${d.noteFa}" else ""
        TimelineRowVisuals(
          nodeColor = color,
          cardBg = bg,
          titleText = "${d.medicationNameFa} • ${d.volumeMlText}",
          subtitleText = "ساعت ${d.givenAtLabelFa} • ${FeverDoseTimelineEngine.formatRelativeTimeFa(d.timestampMs)} • ${d.dosageFormFa}$notePart",
          icon = Icons.Rounded.Medication,
          deleteLabel = "حذف نوبت دارو",
        )
      }
    }

  Row(
    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag("timeline_feed_item_${item.key}"),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Vertical timeline spine with colored circle dot
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(22.dp).fillMaxHeight(),
    ) {
      Spacer(Modifier.height(10.dp))
      Box(
        modifier =
          Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(nodeColor)
      )
      if (!isLast) {
        Box(
          modifier =
            Modifier
              .width(2.dp)
              .weight(1f)
              .background(borderColor)
        )
      }
    }

    Surface(
      color = cardBg,
      shape = RoundedCornerShape(14.dp),
      border =
        BorderStroke(
          width = if (isSelected) 1.5.dp else 1.dp,
          color = if (isSelected) nodeColor else borderColor,
        ),
      modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 8.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(iconVec, contentDescription = null, tint = nodeColor, modifier = Modifier.size(18.dp))
          Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(titleText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitleText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(Icons.Rounded.DeleteOutline, contentDescription = deleteLabel, tint = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
}

private data class TimelineRowVisuals(
  val nodeColor: Color,
  val cardBg: Color,
  val titleText: String,
  val subtitleText: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
  val deleteLabel: String,
)

@Composable
private fun TimelineMetricChip(
  labelFa: String,
  valueText: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = accent.copy(alpha = 0.08f),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(labelFa, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(valueText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
    }
  }
}

@Composable
private fun AntipyreticStatusBanner(
  status: AntipyreticIntervalStatus,
  modifier: Modifier = Modifier,
) {
  val bgColor =
    when (status.severity) {
      AlertSeverity.DANGER -> manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 0.72f, darkAlpha = 0.16f)
      AlertSeverity.CAUTION -> manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.16f)
      AlertSeverity.SUCCESS -> manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.72f, darkAlpha = 0.16f)
      AlertSeverity.INFO -> manoTintedSurface(InfoSurface, DarkSky, lightAlpha = 0.72f, darkAlpha = 0.16f)
    }
  val accentColor =
    when (status.severity) {
      AlertSeverity.DANGER -> manoSemanticAccent(DangerCrimson, DarkCoral)
      AlertSeverity.CAUTION -> manoSemanticAccent(CautionAmber, DarkGold)
      AlertSeverity.SUCCESS -> manoSemanticAccent(SuccessEmerald, DarkSage)
      AlertSeverity.INFO -> manoSemanticAccent(InfoBlue, DarkSky)
    }
  Surface(
    color = bgColor,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.30f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Rounded.Medication, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
          Text(status.medFamilyLabelFa, style = MaterialTheme.typography.labelLarge, color = accentColor, fontWeight = FontWeight.Bold)
        }
        Text(
          text = "${status.dosesLast24h}/${status.maxDoses24h} نوبت",
          style = MaterialTheme.typography.labelMedium,
          color = accentColor,
          fontWeight = FontWeight.Bold,
        )
      }
      if (status.lastDoseSummaryFa != null) {
        Text(
          text = "آخرین مصرف: ${status.lastDoseSummaryFa}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = status.statusMessageFa,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
