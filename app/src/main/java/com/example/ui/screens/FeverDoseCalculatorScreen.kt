package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.ManoSourcesAndClinicalSeed
import com.example.domain.model.ChildProfileDemo
import com.example.domain.util.FeverAgeGroup
import com.example.domain.util.FeverDoseCalculationResult
import com.example.domain.util.FeverDoseCalculatorEngine
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.domain.util.FeverMedicineOption
import com.example.domain.util.FeverPriorDoseStatus
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.LiquidFormulationId
import com.example.domain.util.PersianNormalizer
import com.example.notifications.FeverDoseReminderScheduler
import com.example.ui.components.NonDrugFeverCareScreen
import com.example.ui.components.SectionCard
import com.example.ui.components.VisualSyringeCanvas
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSky
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import java.util.Locale
import kotlin.math.roundToInt

private enum class FeverCalcStep(val number: Int, val titleFa: String) {
  AGE_TRIAGE(1, "سن کودک"),
  MEDICINE_PICKER(2, "انتخاب تب‌بر"),
  WEIGHT_INPUT(3, "وزن کودک"),
  PRIOR_DOSE_CHECK(4, "بررسی نوبت قبلی"),
  SYRINGE_RESULT(5, "دوز دقیق و سرنگ"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeverDoseCalculatorScreen(
  childProfile: ChildProfileDemo?,
  childBirthDateEpochMs: Long?,
  timelineSnapshot: FeverDoseTimelineSnapshot,
  prefillFormulationId: String = "",
  prefillScannedDrugFa: String = "",
  onLogDose: (
    medicationSlug: String,
    medicationNameFa: String,
    dosageFormFa: String,
    volumeMlText: String,
    givenAtLabelFa: String,
    noteFa: String,
    onDone: () -> Unit,
  ) -> Unit,
  onOpenSyrupScanner: () -> Unit,
  onTalkToPharmacist: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onFeedback: (String) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val spacing = ManoTheme.spacing
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)

  val initialAgeMonths =
    remember(childProfile, childBirthDateEpochMs) {
      LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(childProfile, childBirthDateEpochMs)
    }
  val initialWeightKg =
    remember(childProfile) {
      LiquidDoseVisualizerEngine.initialWeightKgFromProfile(childProfile) ?: 10.0f
    }

  var stepIndex by rememberSaveable { mutableIntStateOf(0) }
  var selectedAgeGroupId by rememberSaveable {
    mutableStateOf<String?>(FeverDoseCalculatorEngine.inferAgeGroupFromMonths(initialAgeMonths)?.id)
  }
  var selectedFormulationId by rememberSaveable(prefillFormulationId) {
    mutableStateOf(
      LiquidFormulationId.entries
        .firstOrNull { it.id == prefillFormulationId && it != LiquidFormulationId.CETIRIZINE_SYRUP_5_5 }
        ?.id
    )
  }
  var pendingConfirmMedicineId by rememberSaveable { mutableStateOf<String?>(null) }
  var weightKg by rememberSaveable { mutableFloatStateOf(initialWeightKg) }
  var weightText by rememberSaveable {
    mutableStateOf(String.format(Locale.US, "%.1f", initialWeightKg))
  }
  var selectedPriorStatusId by rememberSaveable { mutableStateOf(FeverPriorDoseStatus.NONE_IN_6H.id) }
  var showNonDrugCare by rememberSaveable { mutableStateOf(false) }
  var loggedDoseAtMs by rememberSaveable { mutableStateOf<Long?>(null) }

  val currentStep = FeverCalcStep.entries[stepIndex.coerceIn(0, FeverCalcStep.entries.lastIndex)]
  val selectedAgeGroup = FeverAgeGroup.entries.firstOrNull { it.id == selectedAgeGroupId }
  val selectedFormulation =
    LiquidFormulationId.entries.firstOrNull { it.id == selectedFormulationId }
      ?: LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5
  val selectedPriorStatus =
    FeverPriorDoseStatus.entries.firstOrNull { it.id == selectedPriorStatusId }
      ?: FeverPriorDoseStatus.NONE_IN_6H

  // If the user came straight from Syrup Scanner with a valid formulation & known age >= 3m, pre-select smoothly.
  LaunchedEffect(prefillFormulationId) {
    if (prefillFormulationId.isNotBlank() && selectedAgeGroup != null && selectedAgeGroup != FeverAgeGroup.UNDER_3_MONTHS) {
      val matched = LiquidFormulationId.entries.firstOrNull { it.id == prefillFormulationId }
      if (matched != null && FeverDoseCalculatorEngine.isFormulationAllowedForAge(selectedAgeGroup, matched)) {
        selectedFormulationId = matched.id
        if (stepIndex == 0) stepIndex = 2
      }
    }
  }

  // Handle back presses step-by-step before popping the screen.
  BackHandler(enabled = showNonDrugCare || stepIndex > 0) {
    if (showNonDrugCare) {
      showNonDrugCare = false
    } else if (stepIndex > 0) {
      stepIndex -= 1
    }
  }

  if (showNonDrugCare) {
    NonDrugFeverCareScreen(
      onBackToCountdown = { showNonDrugCare = false },
      onTalkToPharmacist = onTalkToPharmacist,
    )
    return
  }

  fun dialEmergency115() {
    runCatching {
      context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:115")))
    }
  }

  fun shareWhatsAppReport(result: FeverDoseCalculationResult) {
    val nowMs = loggedDoseAtMs ?: System.currentTimeMillis()
    val givenLabel = FeverDoseTimelineEngine.formatHourMinute(nowMs)
    val nextSafeLabel =
      FeverDoseTimelineEngine.formatHourMinute(nowMs + result.reminderIntervalHours * 3_600_000L)
    val text =
      FeverDoseCalculatorEngine.buildWhatsAppCaregiverReport(
        childAliasFa = childProfile?.aliasFa,
        result = result,
        givenAtTimeLabelFa = givenLabel,
        nextSafeTimeLabelFa = nextSafeLabel,
      )
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
      }
    runCatching {
      context.startActivity(Intent.createChooser(sendIntent, "ارسال گزارش دوز تب‌بر برای مراقب"))
    }.onFailure {
      onFeedback("امکان باز کردن منوی اشتراک‌گذاری وجود ندارد.")
    }
  }

  // Concentration Confirmation Modal before advancing from Step 2 -> Step 3
  val pendingMedicineOption =
    FeverDoseCalculatorEngine.iranFeverMedicines.firstOrNull { it.formulation.id == pendingConfirmMedicineId }
  if (pendingMedicineOption != null) {
    AlertDialog(
      onDismissRequest = { pendingConfirmMedicineId = null },
      icon = {
        Icon(
          imageVector = Icons.Rounded.WarningAmber,
          contentDescription = null,
          tint = cautionAccent,
          modifier = Modifier.size(30.dp),
        )
      },
      title = {
        Text(
          text = "تأیید دقیق غلظت روی جعبه دارو",
          style = ManoTheme.typeScale.cardTitle.copy(fontWeight = FontWeight.Bold),
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Surface(
            color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.22f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = pendingMedicineOption.titleFa,
                style = ManoTheme.typeScale.cardSubtitle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = pendingMedicineOption.boxStrengthBadgeFa,
                style = ManoTheme.typeScale.sectionHeader.copy(fontWeight = FontWeight.ExtraBold),
                color = cautionAccent,
              )
            }
          }
          Text(
            text = pendingMedicineOption.confirmationPromptFa,
            style = ManoTheme.typeScale.clinicalBody,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            selectedFormulationId = pendingMedicineOption.formulation.id
            pendingConfirmMedicineId = null
            stepIndex = FeverCalcStep.WEIGHT_INPUT.ordinal
          },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("fever_confirm_concentration_btn"),
        ) {
          Text("بله، دقیقاً همین غلظت است")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { pendingConfirmMedicineId = null },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text("بررسی دوباره")
        }
      },
    )
  }

  Box(
    modifier = Modifier.fillMaxSize().testTag("fever_dose_calculator_screen"),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("fever_dose_calculator_list"),
      contentPadding = PaddingValues(
        start = spacing.screenHorizontal,
        end = spacing.screenHorizontal,
        top = spacing.sm,
        bottom = spacing.screenBottomClearance,
      ),
      verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
      // Top Reassuring Header + 5-step progress bar
      item {
        SectionCard(modifier = Modifier.testTag("fever_calc_progress_header")) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "آرام باشید، الان با هم دوز دقیق را حساب می‌کنیم.",
                style = ManoTheme.typeScale.cardTitle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "گام ${FeverDoseCalculatorEngine.toPersianDigits(currentStep.number.toString())} از ۵ • ${currentStep.titleFa}",
                style = ManoTheme.typeScale.microCaption,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            if (stepIndex > 0) {
              TextButton(
                onClick = { stepIndex -= 1 },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("fever_calc_prev_step_btn"),
              ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("مرحله قبل")
              }
            }
          }
          LinearProgressIndicator(
            progress = { (stepIndex + 1f) / FeverCalcStep.entries.size.toFloat() },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
          )
        }
      }

      when (currentStep) {
        // STEP 1: AGE TRIAGE & UNDER-3-MONTH HARD STOP
        FeverCalcStep.AGE_TRIAGE -> {
          item {
            SectionCard(modifier = Modifier.testTag("fever_step1_age_card")) {
              Text(
                text = "کوچولوی شما الان چند ماهه است؟",
                style = ManoTheme.typeScale.sectionHeader.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (childProfile != null) {
                Text(
                  text = "پرونده فعال: «${childProfile.aliasFa}» (${childProfile.birthDateOrAgeFa})",
                  style = ManoTheme.typeScale.microCaption,
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeverAgeGroup.entries.forEach { group ->
                  val selected = selectedAgeGroup == group
                  val isUnder3 = group == FeverAgeGroup.UNDER_3_MONTHS
                  val borderColor =
                    when {
                      selected && isUnder3 -> dangerAccent
                      selected -> MaterialTheme.colorScheme.primary
                      else -> manoBorderColor()
                    }
                  val containerColor =
                    when {
                      selected && isUnder3 -> manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 0.9f, darkAlpha = 0.22f)
                      selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                      else -> manoCardSurface()
                    }
                  Surface(
                    onClick = {
                      selectedAgeGroupId = group.id
                      if (group != FeverAgeGroup.UNDER_3_MONTHS) {
                        if (group == FeverAgeGroup.THREE_TO_SIX_MONTHS && selectedFormulation == LiquidFormulationId.IBUPROFEN_SYRUP_100_5) {
                          selectedFormulationId = null
                        }
                        stepIndex = FeverCalcStep.MEDICINE_PICKER.ordinal
                      }
                    },
                    color = containerColor,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(if (selected) 1.8.dp else 1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_age_${group.id}"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                          text = group.labelFa,
                          style = ManoTheme.typeScale.cardTitle.copy(fontWeight = FontWeight.Bold),
                          color = if (isUnder3 && selected) dangerAccent else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                          text = group.subtitleFa,
                          style = ManoTheme.typeScale.microCaption,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      if (selected) {
                        Icon(
                          imageVector = if (isUnder3) Icons.Rounded.ReportProblem else Icons.Rounded.CheckCircle,
                          contentDescription = null,
                          tint = if (isUnder3) dangerAccent else MaterialTheme.colorScheme.primary,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // Hard-Stop Red Warning Card if "زیر ۳ ماه" is tapped
          if (selectedAgeGroup == FeverAgeGroup.UNDER_3_MONTHS) {
            item {
              Surface(
                color = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 1f, darkAlpha = 0.24f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(2.dp, dangerAccent),
                modifier = Modifier.fillMaxWidth().testTag("fever_under3m_hard_stop_card"),
              ) {
                Column(
                  modifier = Modifier.padding(spacing.cardPadding),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = dangerAccent, modifier = Modifier.size(28.dp))
                    Text(
                      text = "نیاز به بررسی فوری پزشک",
                      style = ManoTheme.typeScale.sectionHeader.copy(fontWeight = FontWeight.ExtraBold),
                      color = dangerAccent,
                    )
                  }
                  Text(
                    text = "تب در نوزادان زیر ۳ ماه نیاز به معاینه مستقیم پزشک دارد. لطفاً پیش از دادن هرگونه داروی تب‌بر، با پزشک یا اورژانس مشورت کنید.",
                    style = ManoTheme.typeScale.clinicalBody.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Button(
                    onClick = ::dialEmergency115,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson, contentColor = PureWhite),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_under3m_call_115_btn"),
                  ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("تماس با اورژانس (۱۱۵)")
                  }
                  OutlinedButton(
                    onClick = onTalkToPharmacist,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_under3m_pharmacist_btn"),
                  ) {
                    Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("گفت‌وگو با داروساز مانو")
                  }
                }
              }
            }
          }
        }

        // STEP 2: VISUAL MEDICINE PICKER + CAMERA SCANNER SHORTCUT
        FeverCalcStep.MEDICINE_PICKER -> {
          val ageGroup = selectedAgeGroup ?: FeverAgeGroup.OVER_6_MONTHS
          item {
            SectionCard(modifier = Modifier.testTag("fever_step2_medicine_card")) {
              Text(
                text = "کدام داروی تب‌بر را در خانه دارید؟",
                style = ManoTheme.typeScale.sectionHeader.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (prefillScannedDrugFa.isNotBlank()) {
                Text(
                  text = "تشخیص اسکنر بطری: $prefillScannedDrugFa",
                  style = ManoTheme.typeScale.microCaption,
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              OutlinedButton(
                onClick = onOpenSyrupScanner,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_step2_scan_box_btn"),
              ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("اسکن برچسب جعبه یا شیشه با دوربین")
              }

              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeverDoseCalculatorEngine.iranFeverMedicines.forEach { option ->
                  val allowed = FeverDoseCalculatorEngine.isFormulationAllowedForAge(ageGroup, option.formulation)
                  val selected = selectedFormulationId == option.formulation.id
                  MedicineOptionCard(
                    option = option,
                    allowed = allowed,
                    selected = selected,
                    onClick = {
                      if (allowed) {
                        pendingConfirmMedicineId = option.formulation.id
                      } else {
                        onFeedback("ایبوپروفن برای کودکان زیر ۶ ماه مجاز نیست؛ لطفاً از استامینوفن استفاده کنید.")
                      }
                    },
                  )
                }
              }
            }
          }
        }

        // STEP 3: WEIGHT STEPPER + OUTLIER & DROPS-VS-SYRUP WARNINGS
        FeverCalcStep.WEIGHT_INPUT -> {
          val ageGroup = selectedAgeGroup ?: FeverAgeGroup.OVER_6_MONTHS
          val weightNotices =
            FeverDoseCalculatorEngine.evaluateWeightNotices(ageGroup, weightKg, selectedFormulation)
          item {
            SectionCard(modifier = Modifier.testTag("fever_step3_weight_card")) {
              Text(
                text = "وزن تقریبی یا دقیق کودک چند کیلوگرم است؟",
                style = ManoTheme.typeScale.sectionHeader.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "داروی انتخابی: ${selectedFormulation.titleFa}",
                style = ManoTheme.typeScale.microCaption,
                color = MaterialTheme.colorScheme.primary,
              )

              // Large tactile stepper
              Surface(
                color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 0.7f, darkAlpha = 0.18f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Surface(
                    onClick = {
                      val next = (weightKg - 0.5f).coerceIn(FeverDoseCalculatorEngine.MIN_FEVER_WEIGHT_KG, FeverDoseCalculatorEngine.MAX_FEVER_WEIGHT_KG)
                      weightKg = next
                      weightText = String.format(Locale.US, "%.1f", next)
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp).testTag("fever_weight_minus_btn"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Rounded.Remove, contentDescription = "کاهش نیم کیلوگرم", tint = MaterialTheme.colorScheme.primary)
                    }
                  }

                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                      text = "${FeverDoseCalculatorEngine.formatPersianDecimal(weightKg)} کیلوگرم",
                      style = ManoTheme.typeScale.clinicalNumericMedium.copy(fontWeight = FontWeight.ExtraBold),
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.testTag("fever_weight_display"),
                    )
                    Text(
                      text = "گام ۰٫۵ کیلوگرمی یا تایپ مستقیم",
                      style = ManoTheme.typeScale.microCaption,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }

                  Surface(
                    onClick = {
                      val next = (weightKg + 0.5f).coerceIn(FeverDoseCalculatorEngine.MIN_FEVER_WEIGHT_KG, FeverDoseCalculatorEngine.MAX_FEVER_WEIGHT_KG)
                      weightKg = next
                      weightText = String.format(Locale.US, "%.1f", next)
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp).testTag("fever_weight_plus_btn"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Rounded.Add, contentDescription = "افزایش نیم کیلوگرم", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                  }
                }
              }

              OutlinedTextField(
                value = weightText,
                onValueChange = { raw ->
                  val cleaned =
                    PersianNormalizer.toAsciiDigits(raw)
                      .replace("٫", ".")
                      .replace(",", ".")
                      .filter { ch -> ch.isDigit() || ch == '.' }
                      .take(5)
                  weightText = cleaned
                  cleaned.toFloatOrNull()?.let { parsed ->
                    if (parsed in FeverDoseCalculatorEngine.MIN_FEVER_WEIGHT_KG..FeverDoseCalculatorEngine.MAX_FEVER_WEIGHT_KG) {
                      weightKg = parsed
                    }
                  }
                },
                label = { Text("ورود دستی وزن (کیلوگرم، بین ۳ تا ۴۰)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("fever_weight_manual_input"),
              )

              if (weightNotices.isOutlierWarning && weightNotices.outlierMessageFa != null) {
                Surface(
                  color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.2f),
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.5f)),
                  modifier = Modifier.fillMaxWidth().testTag("fever_weight_outlier_warning"),
                ) {
                  Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = cautionAccent)
                    Text(
                      text = weightNotices.outlierMessageFa,
                      style = ManoTheme.typeScale.clinicalBodyCompact,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.weight(1f),
                    )
                  }
                }
              }

              if (weightNotices.showDropsToSyrupSuggestion && weightNotices.dropsSuggestionMessageFa != null) {
                Surface(
                  color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.2f),
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.55f)),
                  modifier = Modifier.fillMaxWidth().testTag("fever_drops_to_syrup_suggestion"),
                ) {
                  Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                      text = "پیشنهاد تغییر از قطره به شربت (وزن بالای ۱۲ کیلوگرم)",
                      style = ManoTheme.typeScale.cardSubtitle.copy(fontWeight = FontWeight.Bold),
                      color = cautionAccent,
                    )
                    Text(
                      text = weightNotices.dropsSuggestionMessageFa,
                      style = ManoTheme.typeScale.clinicalBodyCompact,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedButton(
                      onClick = {
                        selectedFormulationId = LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5.id
                        onFeedback("شکل دارویی به شربت استامینوفن ۱۲۰/۵ تغییر یافت.")
                      },
                      modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_switch_to_syrup_btn"),
                    ) {
                      Text("تغییر سریع به شربت استامینوفن ۱۲۰/۵")
                    }
                  }
                }
              }

              Button(
                onClick = { stepIndex = FeverCalcStep.PRIOR_DOSE_CHECK.ordinal },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_step3_continue_btn"),
              ) {
                Text("ادامه به بررسی نوبت قبلی")
              }
            }
          }
        }

        // STEP 4: PRIOR DOSE 6-HOUR OVERLAP CHECK
        FeverCalcStep.PRIOR_DOSE_CHECK -> {
          val conflict =
            FeverDoseCalculatorEngine.evaluatePriorDoseConflict(selectedFormulation, selectedPriorStatus)
          val recentLoggedDose = timelineSnapshot.dosePoints.lastOrNull()

          item {
            SectionCard(modifier = Modifier.testTag("fever_step4_prior_dose_card")) {
              Text(
                text = "آیا در ۶ ساعت گذشته به کودک تب‌بر داده‌اید؟",
                style = ManoTheme.typeScale.sectionHeader.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
              )

              if (recentLoggedDose != null) {
                Surface(
                  color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 0.75f, darkAlpha = 0.18f),
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                      text = "آخرین نوبت ثبت‌شده در مانو: ${recentLoggedDose.medicationNameFa} (${recentLoggedDose.volumeMlText}) ساعت ${recentLoggedDose.givenAtLabelFa}",
                      style = ManoTheme.typeScale.clinicalBodyCompact,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeverPriorDoseStatus.entries.forEach { status ->
                  val selected = selectedPriorStatus == status
                  Surface(
                    onClick = { selectedPriorStatusId = status.id },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else manoCardSurface(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(if (selected) 1.6.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else manoBorderColor()),
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_prior_${status.id}"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(
                        text = status.labelFa,
                        style = ManoTheme.typeScale.clinicalBodyCompact.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                      )
                      if (selected) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      }
                    }
                  }
                }
              }

              if (conflict.isHardConflict) {
                Surface(
                  color = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 1f, darkAlpha = 0.22f),
                  shape = RoundedCornerShape(16.dp),
                  border = BorderStroke(1.5.dp, dangerAccent),
                  modifier = Modifier.fillMaxWidth().testTag("fever_prior_dose_conflict_block"),
                ) {
                  Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                      text = conflict.titleFa,
                      style = ManoTheme.typeScale.cardTitle.copy(fontWeight = FontWeight.Bold),
                      color = dangerAccent,
                    )
                    Text(
                      text = conflict.bodyFa,
                      style = ManoTheme.typeScale.clinicalBody,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(
                      onClick = { showNonDrugCare = true },
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_conflict_open_nondrug_btn"),
                    ) {
                      Icon(Icons.Rounded.Spa, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(Modifier.width(8.dp))
                      Text("تا زمان نوبت بعدی چه کارهایی انجام دهم؟ (مراقبت غیردارویی)")
                    }
                  }
                }
              } else {
                if (conflict.isCautionSpacing) {
                  Surface(
                    color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.2f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text(conflict.titleFa, style = ManoTheme.typeScale.cardSubtitle.copy(fontWeight = FontWeight.Bold), color = cautionAccent)
                      Text(conflict.bodyFa, style = ManoTheme.typeScale.clinicalBodyCompact, color = MaterialTheme.colorScheme.onSurface)
                    }
                  }
                }

                Button(
                  onClick = { stepIndex = FeverCalcStep.SYRINGE_RESULT.ordinal },
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_step4_calculate_btn"),
                ) {
                  Text("محاسبه دوز دقیق و نمایش روی سرنگ")
                }
              }
            }
          }
        }

        // STEP 5: VISUAL 2D SYRINGE RESULT + TRUST BADGE + LOG/REMINDER + WHATSAPP + NON-DRUG CARE
        FeverCalcStep.SYRINGE_RESULT -> {
          val ageGroup = selectedAgeGroup ?: FeverAgeGroup.OVER_6_MONTHS
          val calcResult =
            FeverDoseCalculatorEngine.calculate(ageGroup, selectedFormulation, weightKg)
          val mlFa = FeverDoseCalculatorEngine.formatPersianDecimal(calcResult.recommendedMl)
          val minMlFa = FeverDoseCalculatorEngine.formatPersianDecimal(calcResult.minSafeMl)
          val maxMlFa = FeverDoseCalculatorEngine.formatPersianDecimal(calcResult.maxSafeMl)
          val mgFa = FeverDoseCalculatorEngine.toPersianDigits(calcResult.recommendedMg.roundToInt().toString())
          val kgFa = FeverDoseCalculatorEngine.formatPersianDecimal(calcResult.weightKg)

          if (calcResult.isBlocked) {
            item {
              Surface(
                color = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 1f, darkAlpha = 0.22f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, dangerAccent),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text("امکان محاسبه دوز خانگی نیست", style = ManoTheme.typeScale.cardTitle.copy(fontWeight = FontWeight.Bold), color = dangerAccent)
                  Text(calcResult.blockReasonFa.orEmpty(), style = ManoTheme.typeScale.clinicalBody)
                  Button(onClick = onTalkToPharmacist, modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()) {
                    Text("گفت‌وگو با داروساز مانو")
                  }
                }
              }
            }
          } else {
            item {
              SectionCard(modifier = Modifier.testTag("fever_step5_result_card")) {
                Text(
                  text = "دوز دقیق این نوبت با سرنگ مدرج",
                  style = ManoTheme.typeScale.cardSubtitle,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Hero dose readout
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                  shape = RoundedCornerShape(20.dp),
                  border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(
                      text = "$mlFa سی‌سی (mL)",
                      style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                      color = MaterialTheme.colorScheme.primary,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.testTag("fever_result_ml_text"),
                    )
                    Text(
                      text = "معادل $mgFa میلی‌گرم • برای وزن $kgFa کیلوگرم (${calcResult.ageGroup.labelFa})",
                      style = ManoTheme.typeScale.clinicalBodyCompact.copy(fontWeight = FontWeight.Medium),
                      color = MaterialTheme.colorScheme.onSurface,
                      textAlign = TextAlign.Center,
                    )
                    if (calcResult.approxDropsCount != null) {
                      Text(
                        text = "معادل تقریبی با قطره‌چکان استاندارد: ${FeverDoseCalculatorEngine.toPersianDigits(calcResult.approxDropsCount.toString())} قطره (دقیق‌ترین روش: سرنگ ۱ تا ۳ سی‌سی)",
                        style = ManoTheme.typeScale.microCaption.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                      )
                    }
                    Text(
                      text = "بازه ایمن تک‌نوبت: $minMlFa تا $maxMlFa سی‌سی • حداکثر ${FeverDoseCalculatorEngine.toPersianDigits(calcResult.maxDosesPer24h.toString())} نوبت در ۲۴ ساعت",
                      style = ManoTheme.typeScale.microCaption,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      textAlign = TextAlign.Center,
                    )
                  }
                }

                // 2D Graduated Visual Syringe Canvas
                VisualSyringeCanvas(
                  targetMl = calcResult.recommendedMl,
                  isDrops = calcResult.formulation == LiquidFormulationId.ACETAMINOPHEN_DROPS_100,
                )

                if (calcResult.isSafetyCeilingApplied) {
                  Surface(
                    color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.2f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("fever_safety_ceiling_notice"),
                  ) {
                    Text(
                      text = "سقف ایمنی تک‌نوبت (${FeverDoseCalculatorEngine.toPersianDigits(calcResult.formulation.maxSingleDoseMg.roundToInt().toString())} میلی‌گرم) برای ایمنی کودک اعمال شده است.",
                      style = ManoTheme.typeScale.clinicalBodyCompact,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(12.dp),
                    )
                  }
                }

                // Pharmacist Trust Badge & Citation Tag
                Surface(
                  color = manoTintedSurface(SuccessSurface, DarkSky, lightAlpha = 0.85f, darkAlpha = 0.18f),
                  shape = RoundedCornerShape(16.dp),
                  border = BorderStroke(1.dp, SuccessEmerald.copy(alpha = 0.4f)),
                  modifier = Modifier.fillMaxWidth().testTag("fever_pharmacist_trust_badge"),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(26.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(
                        text = "تأییدشده توسط داروساز مسئول فنی: ${ManoSourcesAndClinicalSeed.CLINICAL_REVIEWER_NAME_FA}",
                        style = ManoTheme.typeScale.cardSubtitle.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = "شماره نظام پزشکی: ${ManoSourcesAndClinicalSeed.CLINICAL_REVIEWER_REG_ID} • منبع: ${calcResult.clinicalSourceTagFa}",
                        style = ManoTheme.typeScale.microCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }

                // One-Tap Dose Log + 6-Hour Reminder
                val isLogged = loggedDoseAtMs != null
                if (isLogged) {
                  val nextSafeTime =
                    FeverDoseTimelineEngine.formatHourMinute(
                      loggedDoseAtMs!! + calcResult.reminderIntervalHours * 3_600_000L
                    )
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("fever_logged_countdown_card"),
                  ) {
                    Row(
                      modifier = Modifier.padding(12.dp),
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(Icons.Rounded.AlarmOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = "نوبت ثبت شد و یادآور ۶ ساعته فعال گردید",
                          style = ManoTheme.typeScale.cardSubtitle.copy(fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                          text = "زمان ایمن نوبت بعدی (در صورت نیاز): حدود ساعت $nextSafeTime",
                          style = ManoTheme.typeScale.microCaption,
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                      }
                    }
                  }
                }

                Button(
                  onClick = {
                    val now = System.currentTimeMillis()
                    val givenLabel = FeverDoseTimelineEngine.formatHourMinute(now)
                    val slug =
                      if (calcResult.formulation == LiquidFormulationId.IBUPROFEN_SYRUP_100_5) "ibuprofen" else "acetaminophen"
                    val formFa =
                      if (calcResult.formulation == LiquidFormulationId.ACETAMINOPHEN_DROPS_100) "قطره ۱۰۰ mg/mL" else calcResult.formulation.shortLabelFa
                    onLogDose(
                      slug,
                      calcResult.formulation.titleFa,
                      formFa,
                      LiquidDoseVisualizerEngine.formatMl(calcResult.recommendedMl),
                      givenLabel,
                      "ثبت از ماشین‌حساب سریع تب‌بر (وزن $kgFa کیلوگرم)",
                    ) {
                      loggedDoseAtMs = now
                      onRequestNotificationPermission()
                      FeverDoseReminderScheduler.scheduleNextDoseReminder(
                        context = context,
                        drugNameFa = calcResult.formulation.shortLabelFa,
                        volumeMlFa = "$mlFa سی‌سی",
                        childAliasFa = childProfile?.aliasFa,
                        delayHours = calcResult.reminderIntervalHours.toLong(),
                      )
                      onFeedback("دوز $mlFa سی‌سی ثبت شد و یادآور ۶ ساعت بعد تنظیم گردید.")
                    }
                  },
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_log_and_remind_btn"),
                ) {
                  Icon(Icons.Rounded.AlarmOn, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text(if (isLogged) "ثبت دوباره نوبت و یادآور ۶ ساعته" else "ثبت این نوبت و تنظیم یادآور ۶ ساعته")
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  OutlinedButton(
                    onClick = { shareWhatsAppReport(calcResult) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("fever_share_whatsapp_btn"),
                  ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("گزارش واتس‌اپ", style = ManoTheme.typeScale.actionLabel)
                  }

                  OutlinedButton(
                    onClick = { showNonDrugCare = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("fever_open_nondrug_care_btn"),
                  ) {
                    Icon(Icons.Rounded.Spa, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("مراقبت غیردارویی", style = ManoTheme.typeScale.actionLabel)
                  }
                }

                HorizontalDivider(color = manoBorderColor())

                FlowRow(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  TextButton(
                    onClick = onTalkToPharmacist,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("fever_result_pharmacist_btn"),
                  ) {
                    Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("گفت‌وگو با داروساز مانو")
                  }
                  TextButton(
                    onClick = {
                      stepIndex = 0
                      loggedDoseAtMs = null
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("fever_restart_btn"),
                  ) {
                    Text("محاسبه جدید")
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MedicineOptionCard(
  option: FeverMedicineOption,
  allowed: Boolean,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val borderColor =
    when {
      !allowed -> manoBorderColor().copy(alpha = 0.5f)
      selected -> MaterialTheme.colorScheme.primary
      else -> manoBorderColor()
    }
  val bgColor =
    when {
      !allowed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
      selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
      else -> manoCardSurface()
    }

  Surface(
    onClick = onClick,
    color = bgColor,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(if (selected) 1.8.dp else 1.dp, borderColor),
    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("fever_med_${option.formulation.id}"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
              if (!allowed) MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
              else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            ),
      ) {
        Icon(
          imageVector = if (!allowed) Icons.Rounded.Lock else Icons.Rounded.Medication,
          contentDescription = null,
          tint = if (!allowed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = option.titleFa,
          style = ManoTheme.typeScale.cardSubtitle.copy(fontWeight = FontWeight.Bold),
          color = if (!allowed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Surface(
          color = if (!allowed) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = if (!allowed) "قفل ایمنی: زیر ۶ ماه مجاز نیست" else option.boxStrengthBadgeFa,
            style = ManoTheme.typeScale.microCaption.copy(fontWeight = FontWeight.Bold),
            color = if (!allowed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
        Text(
          text = option.localBrandsHintFa,
          style = ManoTheme.typeScale.microCaption,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
