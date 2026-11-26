package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.ChildProfileDemo
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.LiquidFormulationId
import com.example.ui.theme.AquaCare
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.CoralCare
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoSurface
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SageCare
import com.example.ui.theme.ShadowSlate
import com.example.ui.theme.SkyCare
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoChartAxisHex
import com.example.ui.theme.manoElevatedSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveOralSyringeLabCard(
  modifier: Modifier = Modifier,
  childProfile: ChildProfileDemo? = null,
  childBirthDateEpochMs: Long? = null,
  onLogRecommendedDose: ((medicationNameFa: String, volumeMlText: String) -> Unit)? = null,
  onOpen3DWithDose: ((recommendedMl: Float) -> Unit)? = null,
) {
  var selectedFormulation by remember { mutableStateOf(LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5) }
  var ageText by remember(childProfile, childBirthDateEpochMs) {
    mutableStateOf(LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(childProfile, childBirthDateEpochMs)?.toString().orEmpty())
  }
  var weightText by remember(childProfile) {
    mutableStateOf(LiquidDoseVisualizerEngine.initialWeightKgFromProfile(childProfile)?.toString().orEmpty())
  }
  val ageMonths = com.example.domain.util.ClinicalNumberInput.durationHours(ageText)
  val weightKg = com.example.domain.util.ClinicalNumberInput.decimal(weightText)?.toFloat()
  var labelConfirmed by remember(selectedFormulation, ageText, weightText) { mutableStateOf(false) }

  val doseResult =
    remember(selectedFormulation, ageMonths, weightKg) {
      LiquidDoseVisualizerEngine.evaluate(
        formulation = selectedFormulation,
        ageMonths = ageMonths,
        weightKg = weightKg,
      )
    }

  val animatedMl by animateFloatAsState(
    targetValue = doseResult.recommendedMl,
    animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f),
    label = "syringeMlFill",
  )

  val isDark = LocalManoDarkTheme.current
  val cardSurface = manoCardSurface()
  val elevatedSurface = manoElevatedSurface()
  val borderColor = manoBorderColor()
  val chartAxisHex = manoChartAxisHex()
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val infoAccent = manoSemanticAccent(InfoBlue, DarkSky)
  val sageAccent = manoSemanticAccent(SageCare, DarkSage)
  val primaryAccent = MaterialTheme.colorScheme.primary
  val liquidAccent by animateColorAsState(
    targetValue =
      when {
        doseResult.isBlocked -> dangerAccent
        doseResult.alertSeverity == AlertSeverity.CAUTION -> cautionAccent
        else -> primaryAccent
      },
    label = "syringeLiquidTint",
  )

  val shape = RoundedCornerShape(24.dp)

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
        .testTag("interactive_syringe_lab_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ManoIconMedallion(
          icon = Icons.Rounded.Vaccines,
          accentColor = primaryAccent,
          iconKind = PediatricIconKind.MEDICINE,
          containerSize = 44.dp,
          iconSize = 24.dp,
          cornerRadius = 14.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "سرنگ مدرج تعاملی دوز مایع (سن و وزن)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "سن و وزن اندازه‌گیری‌شده را وارد کنید و غلظت را با بطری تطبیق دهید. مقدار آموزشی باید با دستور مصرف کودک بررسی شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (childProfile == null) {
        SeverityAlert(
          severity = AlertSeverity.CAUTION,
          title = "پروفایل کودک ثبت نشده است",
          body = "برای این کودک سن و وزن ثبت نشده است. هر دو مقدار را وارد کنید.",
          modifier = Modifier.testTag("syringe_missing_profile_caution"),
        )
      }

      // 1. Formulation & concentration selector
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("۱. انتخاب دارو و غلظت روی جعبه:", style = MaterialTheme.typography.labelLarge, color = primaryAccent)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth().testTag("syringe_formulation_row"),
        ) {
          items(LiquidFormulationId.entries, key = { it.id }) { form ->
            FilterChip(
              selected = selectedFormulation == form,
              onClick = { selectedFormulation = form },
              label = { Text(form.shortLabelFa, style = MaterialTheme.typography.labelMedium) },
              colors =
                FilterChipDefaults.filterChipColors(
                  containerColor = cardSurface,
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = primaryAccent,
                ),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("syringe_form_${form.id}"),
            )
          }
        }
        Surface(
          color = AquaCare.copy(alpha = 0.10f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "${selectedFormulation.titleFa} • غلظت مرجع: ${selectedFormulation.concentrationLabelFa}",
            style = MaterialTheme.typography.labelMedium,
            color = primaryAccent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
      }

      androidx.compose.material3.OutlinedTextField(
        value = ageText, onValueChange = { ageText = it.take(16) },
        label = { Text("سن به ماه کامل (۰ تا ۱۴۴)") }, singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("syringe_age_input"),
      )
      androidx.compose.material3.OutlinedTextField(
        value = weightText, onValueChange = { weightText = it.take(16) },
        label = { Text("وزن اندازه‌گیری‌شده به کیلوگرم (۳ تا ۴۰)") }, singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("syringe_weight_input"),
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Checkbox(checked = labelConfirmed, onCheckedChange = { labelConfirmed = it },
          modifier = Modifier.testTag("syringe_confirm_label"))
        Text("نام و غلظت انتخابی با برچسب بطری مطابقت دارد.")
      }
      if (doseResult.isBlocked || !labelConfirmed) {
        if (doseResult.isBlocked) SeverityAlert(severity = doseResult.alertSeverity, title = doseResult.alertTitleFa,
          body = doseResult.alertBodyFa, modifier = Modifier.testTag("syringe_dose_alert_banner"))
        return@Column
      }

      // 4. Interactive Graduated Syringe Visualizer & Direct Draggable Plunger
      Surface(
        color =
          if (doseResult.isBlocked) manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 0.55f, darkAlpha = 0.16f)
          else manoTintedSurface(InfoSurface, DarkSky, lightAlpha = 0.65f, darkAlpha = 0.16f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, liquidAccent.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = if (doseResult.isBlocked) "دوز خانگی قفل‌شده:" else "حجم آموزشی روی سرنگ:",
                style = MaterialTheme.typography.labelLarge,
              )
              if (!doseResult.isBlocked) {
                Text(
                  text =
                    if (doseResult.isWeightBased) {
                      "معادل ${doseResult.recommendedMg} میلی‌گرم • محدوده ${LiquidDoseVisualizerEngine.formatMl(doseResult.minSafeMl)} تا ${LiquidDoseVisualizerEngine.formatMl(doseResult.maxSafeMl)}"
                    } else {
                      "معادل ${doseResult.recommendedMg} میلی‌گرم • دوز رده سنی"
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.testTag("syringe_mg_range_label"),
                )
              }
            }
            Surface(
              color = cardSurface,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.2.dp, liquidAccent.copy(alpha = 0.45f)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                if (doseResult.isBlocked) {
                  Icon(Icons.Rounded.Lock, contentDescription = null, tint = dangerAccent, modifier = Modifier.size(16.dp))
                }
                Text(
                  text = LiquidDoseVisualizerEngine.formatMl(doseResult.recommendedMl),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = liquidAccent,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.testTag("syringe_selected_ml_label"),
                )
              }
            }
          }

          // Force LTR drawing orientation for standard mL scale ticks (0 -> maxSyringeScaleMl)
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val maxScale = selectedFormulation.maxSyringeScaleMl.coerceAtLeast(1.0f)
            val syringeColors = ManoTheme.colors.illustration.syringe
            Canvas(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(106.dp)
                  .testTag("syringe_visual_canvas")
            ) {
              val w = size.width
              val h = size.height
              val nozzleWidth = w * 0.08f
              val barrelLeft = nozzleWidth + 4f
              val barrelWidth = w * 0.64f
              val barrelTop = h * 0.14f
              val barrelHeight = h * 0.50f
              val fraction = if (doseResult.isBlocked) 0f else (animatedMl / maxScale).coerceIn(0.02f, 1.0f)
              val liquidWidth = barrelWidth * fraction

              // Syringe nozzle tip on left
              drawRoundRect(
                color = syringeColors.labNozzle,
                topLeft = Offset(4f, barrelTop + barrelHeight * 0.34f),
                size = Size(nozzleWidth, barrelHeight * 0.32f),
                cornerRadius = CornerRadius(6f, 6f),
              )

              // Barrel background (glass cylinder)
              drawRoundRect(
                color = if (isDark) DarkSurfaceVariant else PureWhite,
                topLeft = Offset(barrelLeft, barrelTop),
                size = Size(barrelWidth, barrelHeight),
                cornerRadius = CornerRadius(10f, 10f),
              )

              // Safe therapeutic window subtle highlight band (minSafeMl .. maxSafeMl)
              if (!doseResult.isBlocked && doseResult.isWeightBased) {
                val minX = barrelLeft + barrelWidth * (doseResult.minSafeMl / maxScale).coerceIn(0f, 1f)
                val maxX = barrelLeft + barrelWidth * (doseResult.maxSafeMl / maxScale).coerceIn(0f, 1f)
                if (maxX > minX) {
                  drawRect(
                    color = sageAccent.copy(alpha = 0.18f),
                    topLeft = Offset(minX, barrelTop + 2f),
                    size = Size(maxX - minX, barrelHeight - 4f),
                  )
                }
              }

              // Colored liquid fill up to calculated mL
              if (liquidWidth > 2f) {
                drawRoundRect(
                  brush = Brush.horizontalGradient(listOf(liquidAccent.copy(alpha = 0.80f), AquaCare.copy(alpha = 0.65f))),
                  topLeft = Offset(barrelLeft, barrelTop + 2f),
                  size = Size(liquidWidth, barrelHeight - 4f),
                  cornerRadius = CornerRadius(8f, 8f),
                )
              }

              // Plunger rubber stopper at liquidWidth boundary
              val stopperX = barrelLeft + liquidWidth
              drawRoundRect(
                color = if (doseResult.isBlocked) dangerAccent else syringeColors.labStopper,
                topLeft = Offset(stopperX - 4f, barrelTop + 3f),
                size = Size(10f, barrelHeight - 6f),
                cornerRadius = CornerRadius(4f, 4f),
              )

              // Plunger rod extending to the right + ergonomic thumb handle
              val rodEnd = (stopperX + w * 0.22f).coerceAtMost(w - 16f)
              drawRect(
                color = syringeColors.labRod,
                topLeft = Offset(stopperX + 6f, barrelTop + barrelHeight * 0.36f),
                size = Size((rodEnd - (stopperX + 6f)).coerceAtLeast(8f), barrelHeight * 0.28f),
              )
              drawRoundRect(
                color = syringeColors.labThumb,
                topLeft = Offset(rodEnd, barrelTop + barrelHeight * 0.08f),
                size = Size(12f, barrelHeight * 0.84f),
                cornerRadius = CornerRadius(5f, 5f),
              )

              // Barrel flange & glass border
              val barrelStrokeColor = syringeColors.labBarrelStroke
              drawRoundRect(
                color = barrelStrokeColor,
                topLeft = Offset(barrelLeft, barrelTop),
                size = Size(barrelWidth, barrelHeight),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 3f),
              )
              drawRoundRect(
                color = barrelStrokeColor,
                topLeft = Offset(barrelLeft + barrelWidth - 3f, barrelTop - 6f),
                size = Size(8f, barrelHeight + 12f),
                cornerRadius = CornerRadius(4f, 4f),
              )

              // Graduation ticks & numeric mL scale marks
              val totalMajorDivisions = 4
              val totalTicks = 20
              val tickColor = syringeColors.labTick
              for (step in 0..totalTicks) {
                val x = barrelLeft + barrelWidth * (step / totalTicks.toFloat())
                val isMajor = step % (totalTicks / totalMajorDivisions) == 0
                val tickLen = if (isMajor) barrelHeight * 0.38f else barrelHeight * 0.20f
                drawLine(
                  color = tickColor.copy(alpha = if (isMajor) 0.85f else 0.42f),
                  start = Offset(x, barrelTop + 2f),
                  end = Offset(x, barrelTop + tickLen),
                  strokeWidth = if (isMajor) 2.2f else 1.2f,
                  cap = StrokeCap.Round,
                )
              }

              // Numeric graduation labels directly below the syringe barrel
              val labelPaint =
                android.graphics.Paint().apply {
                  color = chartAxisHex
                  textSize = 24f
                  textAlign = android.graphics.Paint.Align.CENTER
                  isAntiAlias = true
                }
              for (div in 0..totalMajorDivisions) {
                val value = maxScale * (div / totalMajorDivisions.toFloat())
                val label =
                  if (maxScale <= 5f) {
                    String.format(Locale.US, "%.1f", value)
                  } else {
                    String.format(Locale.US, "%.0f", value)
                  }
                val x = barrelLeft + barrelWidth * (div / totalMajorDivisions.toFloat())
                drawContext.canvas.nativeCanvas.drawText(label, x, barrelTop + barrelHeight + 28f, labelPaint)
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.TouchApp,
              contentDescription = null,
              tint = primaryAccent,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text =
                if (doseResult.isWeightBased && !doseResult.isBlocked) {
                  "فاصله نوبت‌ها را با دستور داروساز تطبیق دهید. ${selectedFormulation.intervalFa}"
                } else {
                  selectedFormulation.intervalFa
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // 5. Clinical evaluation & safety banner
      val bannerBg =
        when (doseResult.alertSeverity) {
          AlertSeverity.DANGER -> manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 1f, darkAlpha = 0.16f)
          AlertSeverity.CAUTION -> manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.16f)
          AlertSeverity.SUCCESS -> manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 1f, darkAlpha = 0.16f)
          AlertSeverity.INFO -> manoTintedSurface(InfoSurface, DarkSky, lightAlpha = 1f, darkAlpha = 0.16f)
        }
      val bannerTint =
        when (doseResult.alertSeverity) {
          AlertSeverity.DANGER -> dangerAccent
          AlertSeverity.CAUTION -> cautionAccent
          AlertSeverity.SUCCESS -> successAccent
          AlertSeverity.INFO -> infoAccent
        }
      Surface(
        color = bannerBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, bannerTint.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("syringe_dose_alert_banner"),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(doseResult.alertTitleFa, style = MaterialTheme.typography.titleSmall, color = bannerTint, fontWeight = FontWeight.Bold)
          Text(doseResult.alertBodyFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
      }

      if (onLogRecommendedDose != null && !doseResult.isBlocked && doseResult.recommendedMl > 0f) {
        OutlinedButton(
          onClick = {
            onLogRecommendedDose(
              selectedFormulation.titleFa,
              LiquidDoseVisualizerEngine.formatMl(doseResult.recommendedMl),
            )
          },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryAccent),
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("syringe_send_to_logbook_btn"),
        ) {
          Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("ثبت این مقدار (${LiquidDoseVisualizerEngine.formatMl(doseResult.recommendedMl)}) در دفترچه مصرف")
        }
      }

      Surface(
        color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.16f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "هشدار قاشق خانگی و تطبیق برچسب: حجم «قاشق چای‌خوری» خانگی بین ۲ تا بیش از ۷ میلی‌لیتر متغیر است! فقط از سرنگ مدرج mL استفاده کنید و حتماً غلظت دقیق روی بطری (mg/mL) را پیش از مصرف با پزشک یا داروساز چک کنید.",
          style = MaterialTheme.typography.bodySmall,
          color = cautionAccent,
          modifier = Modifier.padding(12.dp),
        )
      }
    }
  }
}

@Composable
fun InteractiveThermometerLabCard(
  modifier: Modifier = Modifier,
) {
  var tempCelsius by remember { mutableFloatStateOf(37.2f) }
  var isUnderThreeMonths by remember { mutableStateOf(false) }

  val isDark = LocalManoDarkTheme.current
  val cardSurface = manoCardSurface()
  val borderColor = manoBorderColor()
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val infoAccent = manoSemanticAccent(InfoBlue, DarkSky)
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val coralAccent = manoSemanticAccent(CoralCare, DarkCoral)

  val shape = RoundedCornerShape(24.dp)
  val statusTint: Color
  val statusBg: Color
  val statusTitle: String
  val statusDetail: String

  when {
    isUnderThreeMonths && tempCelsius >= 38.0f -> {
      statusTint = dangerAccent
      statusBg = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 1f, darkAlpha = 0.16f)
      statusTitle = "هشدار اورژانسی در نوزاد زیر ۳ ماه (تب ≥ ۳۸.۰°C)"
      statusDetail = "در نوزاد زیر ۳ ماه، تب ۳۸.۰ درجه یا بالاتر نیازمند ارزیابی فوری پزشکی و اورژانس ۱۱۵ بدون تأخیر و بدون تب‌بر خانگی خودسرانه است."
    }
    tempCelsius >= 39.5f -> {
      statusTint = dangerAccent
      statusBg = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 1f, darkAlpha = 0.16f)
      statusTitle = "تب بالا (نیازمند ارزیابی پزشک)"
      statusDetail = "دمای بالا به‌ویژه همراه بی‌حالی، تنفس تند یا عدم دریافت مایعات نیازمند معاینه سریع پزشک است."
    }
    tempCelsius >= 38.0f -> {
      statusTint = cautionAccent
      statusBg = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.16f)
      statusTitle = "محدوده تب (≥ ۳۸.۰°C)"
      statusDetail = "رفتار کودک، نوشیدن مایعات و علائم هشدار را پایش کنید؛ برای سن بالای ۳ ماه از راهنمای علائم تب و توصیه پزشک استفاده کنید."
    }
    tempCelsius >= 37.5f -> {
      statusTint = infoAccent
      statusBg = manoTintedSurface(InfoSurface, DarkSky, lightAlpha = 1f, darkAlpha = 0.16f)
      statusTitle = "لب‌مرزی / گرمای خفیف"
      statusDetail = "پوشش سبک و دمای متعادل اتاق را بررسی کنید و دما را پس از ۳۰ دقیقه دوباره با دماسنج دیجیتال دقیق چک نمایید."
    }
    else -> {
      statusTint = successAccent
      statusBg = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 1f, darkAlpha = 0.16f)
      statusTitle = "محدوده دمای طبیعی بدن"
      statusDetail = "دمای ثبت‌شده در محدوده طبیعی است؛ در صورت بی‌حالی یا علائم تنفسی، به حال عمومی کودک توجه کنید."
    }
  }

  val animatedTemp by animateFloatAsState(
    targetValue = tempCelsius,
    animationSpec = spring(dampingRatio = 0.78f, stiffness = 260f),
    label = "thermometerTempMarker",
  )

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
        .testTag("interactive_thermometer_lab_card"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ManoIconMedallion(
          icon = Icons.Rounded.DeviceThermostat,
          accentColor = coralAccent,
          iconKind = PediatricIconKind.FEVER,
          containerSize = 44.dp,
          iconSize = 24.dp,
          cornerRadius = 14.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("مقیاس دیداری دماسنج دیجیتال و آستانه تب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            text = "تفاوت آستانه ۳۸.۰ درجه در نوزاد زیر ۳ ماه و کودکان بزرگ‌تر را ببینید (از دماسنج جیوه‌ای شیشه‌ای هرگز استفاده نکنید).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(
          selected = !isUnderThreeMonths,
          onClick = { isUnderThreeMonths = false },
          label = { Text("کودک ۳ ماه به بالا") },
          colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("thermo_age_over_3m"),
        )
        FilterChip(
          selected = isUnderThreeMonths,
          onClick = { isUnderThreeMonths = true },
          label = { Text("نوزاد زیر ۳ ماه") },
          colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("thermo_age_under_3m"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "دمای خوانده‌شده روی دماسنج:",
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = String.format(Locale.US, "%.1f °C", tempCelsius),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = statusTint,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.testTag("thermometer_temp_label"),
        )
      }

      val thermoMarker = ManoTheme.colors.illustration.syringe.thermoMarker
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(
          modifier = Modifier.fillMaxWidth().height(34.dp).testTag("thermometer_scale_canvas")
        ) {
          val w = size.width
          val barTop = size.height * 0.34f
          val barHeight = size.height * 0.32f
          val minT = 35.5f
          val maxT = 41.0f

          // Multi-zone gradient bar: green -> amber -> red
          drawRoundRect(
            brush =
              Brush.horizontalGradient(
                0.0f to SkyCare,
                0.32f to SageCare,
                0.45f to CautionAmber,
                0.75f to DangerCrimson,
                1.0f to DangerCrimson,
              ),
            topLeft = Offset(0f, barTop),
            size = Size(w, barHeight),
            cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f),
          )

          // 38.0°C clinical fever marker line
          val x38 = ((38.0f - minT) / (maxT - minT)) * w
          drawLine(
            color = thermoMarker,
            start = Offset(x38, barTop - 5f),
            end = Offset(x38, barTop + barHeight + 5f),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
          )

          // Current animated indicator circle
          val xCurr = (((animatedTemp - minT) / (maxT - minT)).coerceIn(0f, 1f)) * w
          drawCircle(
            color = cardSurface,
            radius = 12f,
            center = Offset(xCurr, barTop + barHeight / 2f),
          )
          drawCircle(
            color = statusTint,
            radius = 8.5f,
            center = Offset(xCurr, barTop + barHeight / 2f),
          )
        }
      }

      Slider(
        value = tempCelsius,
        onValueChange = { raw ->
          tempCelsius = ((raw * 10f).roundToInt() / 10f).coerceIn(35.5f, 41.0f)
        },
        valueRange = 35.5f..41.0f,
        colors = SliderDefaults.colors(thumbColor = statusTint, activeTrackColor = statusTint),
        modifier = Modifier.fillMaxWidth().testTag("thermometer_temp_slider"),
      )

      Surface(
        color = statusBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, statusTint.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("thermometer_status_banner"),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(statusTitle, style = MaterialTheme.typography.titleSmall, color = statusTint, fontWeight = FontWeight.Bold)
          Text(statusDetail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
      }
    }
  }
}
