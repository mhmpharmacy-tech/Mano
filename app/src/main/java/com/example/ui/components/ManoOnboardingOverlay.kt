package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.LiquidFormulationId
import com.example.domain.util.PersianNormalizer
import com.example.domain.util.VaccinationScheduleEngine
import com.example.ui.theme.CalmAquaSurface
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.DarkAqua
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DeepScreen
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SageCare
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.SkyCare
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.TealGlow
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import java.util.Locale

private fun deriveAgeBandFromBirthDate(birthDateEpochMs: Long, nowMs: Long = System.currentTimeMillis()): AgeBand {
  val days = VaccinationScheduleEngine.diffDays(birthDateEpochMs, nowMs).coerceAtLeast(0)
  val months = days / 30.4375
  return when {
    months < 3.0 -> AgeBand.NEWBORN
    months < 12.0 -> AgeBand.INFANT_3_12M
    months < 36.0 -> AgeBand.TODDLER_1_3Y
    months < 72.0 -> AgeBand.PRESCHOOL_3_6Y
    else -> AgeBand.SCHOOL_6_PLUS
  }
}

private fun parsePositiveDecimal(raw: String): Float? {
  val normalized = PersianNormalizer.normalize(raw).replace("٫", ".").replace(",", ".").trim()
  if (normalized.isEmpty()) return null
  return normalized.toFloatOrNull()?.takeIf { it > 0f && it.isFinite() }
}

/**
 * 3-step first-launch MANO Kid onboarding experience that personalizes the
 * Vaccination Calendar, Oral mL Syringe Visualizer, and WHO Growth Chart from minute one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManoOnboardingOverlay(
  existingProfile: ChildProfileDemo? = null,
  existingBirthDateEpochMs: Long? = null,
  existingHeightCm: String = "",
  onComplete: (
    aliasFa: String,
    birthDateEpochMs: Long?,
    ageBand: AgeBand,
    weightKgText: String,
    heightCmText: String,
  ) -> Unit,
  onSkip: () -> Unit,
) {
  val isDark = LocalManoDarkTheme.current
  val cardSurface = manoCardSurface()
  val borderColor = manoBorderColor()
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)

  var step by remember { mutableIntStateOf(0) } // 0, 1, 2
  var aliasFa by remember(existingProfile) { mutableStateOf(existingProfile?.aliasFa.orEmpty()) }
  var selectedAgeBand by remember(existingProfile) { mutableStateOf(existingProfile?.ageBand ?: AgeBand.INFANT_3_12M) }
  var birthDateInput by remember(existingBirthDateEpochMs) {
    mutableStateOf(
      existingBirthDateEpochMs?.let { VaccinationScheduleEngine.formatPersianDate(it) }.orEmpty()
    )
  }
  var weightKgInput by remember(existingProfile) { mutableStateOf(existingProfile?.weightKgText.orEmpty()) }
  var heightCmInput by remember(existingHeightCm) { mutableStateOf(existingHeightCm) }
  var consentAccepted by remember { mutableStateOf(true) }
  var validationErrorFa by remember { mutableStateOf<String?>(null) }

  val parsedBirthDateMs =
    remember(birthDateInput) {
      if (birthDateInput.isBlank()) null
      else VaccinationScheduleEngine.parseFlexibleDateToEpochMs(birthDateInput)
    }

  val effectiveAgeMonths =
    remember(parsedBirthDateMs, selectedAgeBand) {
      if (parsedBirthDateMs != null) {
        val days = VaccinationScheduleEngine.diffDays(parsedBirthDateMs, System.currentTimeMillis()).coerceAtLeast(0)
        (days / 30.4375).toInt().coerceIn(0, 144)
      } else {
        when (selectedAgeBand) {
          AgeBand.NEWBORN -> 2
          AgeBand.INFANT_3_12M -> 8
          AgeBand.TODDLER_1_3Y -> 24
          AgeBand.PRESCHOOL_3_6Y -> 48
          AgeBand.SCHOOL_6_PLUS -> 84
        }
      }
    }

  val parsedWeightKg = remember(weightKgInput) { parsePositiveDecimal(weightKgInput) }
  val parsedHeightCm = remember(heightCmInput) { parsePositiveDecimal(heightCmInput) }

  val progress by animateFloatAsState(
    targetValue = (step + 1) / 3f,
    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
    label = "onboardingProgress",
  )

  Dialog(
    onDismissRequest = onSkip,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
      ),
  ) {
    BackHandler {
      if (step > 0) {
        validationErrorFa = null
        step--
      } else {
        onSkip()
      }
    }

    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                if (isDark) {
                  listOf(DeepScreen, Color(0xFF0E2334), DeepScreen)
                } else {
                  listOf(CleanWhite, CalmAquaSurface, Color(0xFFF3FAFC))
                },
            ),
          )
          .statusBarsPadding()
          .navigationBarsPadding()
          .imePadding()
          .testTag("onboarding_overlay"),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = 620.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top Bar: Brand + Step Progress + Skip
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ManoBrandCrest(size = 34.dp, isDark = isDark)
              ManoKidWordmark(isDark = isDark, fontSize = 17.sp)
            }
            TextButton(
              onClick = onSkip,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_skip_btn"),
            ) {
              Text(
                text = "فعلاً رد کن",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "مرحله ${VaccinationScheduleEngine.toPersianDigits((step + 1).toString())} از ۳",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.testTag("onboarding_step_label"),
            )
            Text(
              text =
                when (step) {
                  0 -> "معرفی و نام مستعار"
                  1 -> "تاریخ تولد و وزن کودک"
                  else -> "پیش‌نمایش شخصی‌سازی و شروع"
                },
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(99.dp)),
            color = if (isDark) TealGlow else SeaGreen,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          )
        }

        // Scrollable Step Body
        AnimatedContent(
          targetState = step,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "onboardingStepContent",
          modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { currentStep ->
          Column(
            modifier =
              Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            when (currentStep) {
              0 -> {
                // Step 1: Welcome with MANO + Nickname + Age Band
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier
                      .size(124.dp)
                      .clip(CircleShape)
                      .background(
                        Brush.radialGradient(
                          listOf(
                            TealGlow.copy(alpha = if (isDark) 0.34f else 0.24f),
                            SkyCare.copy(alpha = 0.12f),
                            Color.Transparent,
                          ),
                        ),
                      )
                      .border(2.5.dp, if (isDark) TealGlow else SeaGreen, CircleShape),
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.img_mano_orbital_mascot),
                    contentDescription = "شخصیت مهربان مانو کید",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(4.dp).clip(CircleShape),
                  )
                }

                Text(
                  text = "سلام! من مانو هستم، همراه علمی سلامت فرزند شما",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "با وارد کردن نام مستعار، تاریخ تولد و وزن کودک، تقویم واکسن، سرنگ mL و نمودار رشد WHO از همین دقیقه اول شخصی‌سازی می‌شوند.",
                  style = MaterialTheme.typography.bodyMedium,
                  textAlign = TextAlign.Center,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                  color = cardSurface,
                  shape = RoundedCornerShape(20.dp),
                  border = BorderStroke(1.dp, borderColor),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Text(
                      text = "۱. نام مستعار یا کوچک کودک",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedTextField(
                      value = aliasFa,
                      onValueChange = {
                        aliasFa = it
                        validationErrorFa = null
                      },
                      label = { Text("مثلاً: آراد، باران یا کوچولوی من") },
                      leadingIcon = { Icon(Icons.Rounded.ChildCare, contentDescription = null) },
                      singleLine = true,
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier.fillMaxWidth().testTag("onboarding_alias_input"),
                    )
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      listOf("آراد", "باران", "ماهان", "نیلا").forEach { sample ->
                        SuggestionChip(
                          onClick = {
                            aliasFa = sample
                            validationErrorFa = null
                          },
                          label = { Text(sample) },
                          modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                      }
                    }

                    Text(
                      text = "گروه سنی فعلی کودک",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      AgeBand.entries.forEach { band ->
                        FilterChip(
                          selected = selectedAgeBand == band,
                          onClick = { selectedAgeBand = band },
                          label = { Text("${band.labelFa} (${band.rangeFa})") },
                          modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_age_${band.id}"),
                        )
                      }
                    }
                  }
                }
              }

              1 -> {
                // Step 2: Birth Date (Jalali) + Weight (kg) + Optional Height (cm)
                Surface(
                  color = cardSurface,
                  shape = RoundedCornerShape(20.dp),
                  border = BorderStroke(1.dp, borderColor),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Text(
                        text = "تاریخ تولد کودک (شمسی یا میلادی)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                    Text(
                      text = "برای محاسبه دقیق موعد واکسن‌ها و سن ماهانه در نمودار رشد WHO.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                      value = birthDateInput,
                      onValueChange = { value ->
                        birthDateInput = value
                        validationErrorFa = null
                        VaccinationScheduleEngine.parseFlexibleDateToEpochMs(value)?.let { epoch ->
                          selectedAgeBand = deriveAgeBandFromBirthDate(epoch)
                        }
                      },
                      label = { Text("مثلاً 1403/06/15 (اختیاری)") },
                      leadingIcon = { Icon(Icons.Rounded.Vaccines, contentDescription = null) },
                      singleLine = true,
                      shape = RoundedCornerShape(14.dp),
                      modifier = Modifier.fillMaxWidth().testTag("onboarding_birthdate_input"),
                    )

                    Text(
                      text = "انتخاب سریع سن تقریبی (تنظیم خودکار تاریخ تولد):",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val nowMs = remember { System.currentTimeMillis() }
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      listOf(
                        "۲ ماهه" to 60,
                        "۹ ماهه" to 275,
                        "۱۸ ماهه" to 548,
                        "۳ ساله" to 1095,
                      ).forEach { (presetLabel, daysAgo) ->
                        SuggestionChip(
                          onClick = {
                            val epoch = VaccinationScheduleEngine.addDaysToDay(nowMs, -daysAgo)
                            birthDateInput = VaccinationScheduleEngine.formatPersianDate(epoch)
                            selectedAgeBand = deriveAgeBandFromBirthDate(epoch, nowMs)
                            validationErrorFa = null
                          },
                          label = { Text(presetLabel) },
                          modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_preset_$daysAgo"),
                        )
                      }
                    }

                    if (parsedBirthDateMs != null) {
                      Surface(
                        color = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.85f, darkAlpha = 0.22f),
                        shape = RoundedCornerShape(12.dp),
                      ) {
                        Row(
                          modifier = Modifier.fillMaxWidth().padding(10.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = successAccent, modifier = Modifier.size(18.dp))
                          Text(
                            text = "تاریخ معتبر: ${VaccinationScheduleEngine.formatPersianDate(parsedBirthDateMs)} • گروه سنی: ${selectedAgeBand.labelFa}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                          )
                        }
                      }
                    }
                  }
                }

                Surface(
                  color = cardSurface,
                  shape = RoundedCornerShape(20.dp),
                  border = BorderStroke(1.dp, borderColor),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Icon(Icons.Rounded.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Text(
                        text = "وزن و قد فعلی (مبنای سرنگ mL و نمودار WHO)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      OutlinedTextField(
                        value = weightKgInput,
                        onValueChange = {
                          weightKgInput = it
                          validationErrorFa = null
                        },
                        label = { Text("وزن (کیلوگرم)") },
                        placeholder = { Text("مثلاً 9.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("onboarding_weight_input"),
                      )
                      OutlinedTextField(
                        value = heightCmInput,
                        onValueChange = {
                          heightCmInput = it
                          validationErrorFa = null
                        },
                        label = { Text("قد cm (اختیاری)") },
                        placeholder = { Text("مثلاً 74") },
                        leadingIcon = { Icon(Icons.Rounded.Straighten, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("onboarding_height_input"),
                      )
                    }
                  }
                }
              }

              else -> {
                // Step 3: Live Personalization Preview + Parent Consent + Launch CTA
                val displayAlias = aliasFa.trim().ifBlank { "فرزند شما" }
                val previewVaccineSummary =
                  remember(parsedBirthDateMs) {
                    VaccinationScheduleEngine.resolveSchedule(birthDateEpochMs = parsedBirthDateMs)
                  }
                val previewSyringeEval =
                  remember(effectiveAgeMonths, parsedWeightKg) {
                    val form =
                      if (effectiveAgeMonths < 12) LiquidFormulationId.ACETAMINOPHEN_DROPS_100
                      else LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5
                    LiquidDoseVisualizerEngine.evaluate(
                      formulation = form,
                      ageMonths = effectiveAgeMonths,
                      weightKg = (parsedWeightKg ?: 10f).coerceIn(3f, 40f),
                    )
                  }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = if (isDark) TealGlow else SeaGreen)
                  Text(
                    text = "همه‌چیز برای «$displayAlias» آماده است!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                  )
                }

                OnboardingPreviewCard(
                  icon = Icons.Rounded.Vaccines,
                  accent = manoSemanticAccent(SageCare, DarkSage),
                  title = "تقویم واکسیناسیون شخصی‌سازی‌شده",
                  detail =
                    if (parsedBirthDateMs != null && previewVaccineSummary.nextUpcoming != null) {
                      val next = previewVaccineSummary.nextUpcoming
                      val dateFa = next.effectiveDueDateEpochMs?.let { VaccinationScheduleEngine.formatPersianDate(it) } ?: "—"
                      "نوبت بعدی: ${next.definition.milestoneLabelFa} • تاریخ تقریبی: $dateFa (${VaccinationScheduleEngine.formatRelativeDueLabelFa(next)})"
                    } else {
                      "برنامه کامل واکسیناسیون کشوری آماده است؛ هر زمان می‌توانید تاریخ تولد را دقیق‌تر ثبت کنید."
                    },
                  testTag = "onboarding_preview_vaccine",
                )

                OnboardingPreviewCard(
                  icon = Icons.Rounded.MedicalServices,
                  accent = if (isDark) DarkAqua else SeaGreen,
                  title = "سرنگ مدرج mL و ایمنی دارو",
                  detail =
                    when {
                      previewSyringeEval.isBlocked ->
                        "قفل ایمنی نوزاد فعال شد: در کودکان زیر ۳ ماه دوز خانگی نمایش داده نمی‌شود و نیاز به نظر پزشک است."
                      parsedWeightKg != null ->
                        "بر اساس وزن ${String.format(Locale.US, "%.1f", parsedWeightKg)} kg، خط‌کش سرنگ برای ${previewSyringeEval.formulation.shortLabelFa} (محدوده راهنمای ${String.format(Locale.US, "%.1f", previewSyringeEval.recommendedMl)} mL) آماده شد."
                      else ->
                        "سرنگ سه‌بعدی mL بر اساس گروه سنی «${selectedAgeBand.labelFa}» تنظیم شد؛ پس از ثبت وزن، محاسبه دقیق‌تر می‌شود."
                    },
                  testTag = "onboarding_preview_syringe",
                )

                OnboardingPreviewCard(
                  icon = Icons.Rounded.MonitorWeight,
                  accent = SkyCare,
                  title = "نمودار رشد استاندارد WHO",
                  detail =
                    if (parsedWeightKg != null) {
                      val hText = parsedHeightCm?.let { " و قد ${String.format(Locale.US, "%.0f", it)} cm" }.orEmpty()
                      "وزن ${String.format(Locale.US, "%.1f", parsedWeightKg)} kg$hText برای مقایسه با صدک‌های سازمان بهداشت جهانی ثبت می‌شود."
                    } else {
                      "پایش نقاط عطف حرکتی/گفتاری و نمودار صدک WHO برای «${selectedAgeBand.labelFa}» آماده است."
                    },
                  testTag = "onboarding_preview_who",
                )

                Surface(
                  color = cardSurface,
                  shape = RoundedCornerShape(16.dp),
                  border = BorderStroke(1.dp, borderColor),
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(16.dp))
                      .clickable {
                        consentAccepted = !consentAccepted
                        validationErrorFa = null
                      },
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Checkbox(
                      checked = consentAccepted,
                      onCheckedChange = {
                        consentAccepted = it
                        validationErrorFa = null
                      },
                      modifier = Modifier.testTag("onboarding_consent_checkbox"),
                    )
                    Text(
                      text = "اطلاعات کودک فقط برای شخصی‌سازی ابزارهای آموزشی روی دستگاه ذخیره می‌شود و مانو کید جایگزین معاینه پزشک نیست.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }

            if (validationErrorFa != null) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("onboarding_error_banner"),
              ) {
                Text(
                  text = validationErrorFa.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }
          }
        }

        // Bottom Navigation Buttons
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (step > 0) {
            OutlinedButton(
              onClick = {
                validationErrorFa = null
                step--
              },
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_back_btn"),
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("قبلی")
            }
          }

          Button(
            onClick = {
              when (step) {
                0 -> {
                  if (aliasFa.isBlank()) {
                    aliasFa = "کوچولوی من"
                  }
                  validationErrorFa = null
                  step = 1
                }
                1 -> {
                  if (birthDateInput.isNotBlank() && parsedBirthDateMs == null) {
                    validationErrorFa = "فرمت تاریخ تولد معتبر نیست (مثلاً 1403/05/15 وارد کنید یا از دکمه‌های سریع استفاده کنید)."
                    return@Button
                  }
                  if (weightKgInput.isNotBlank()) {
                    val w = parsedWeightKg
                    if (w == null || w < 1.5f || w > 60f) {
                      validationErrorFa = "لطفاً وزن معتبر کودک را بین ۱.۵ تا ۶۰ کیلوگرم وارد کنید."
                      return@Button
                    }
                  }
                  if (heightCmInput.isNotBlank()) {
                    val h = parsedHeightCm
                    if (h == null || h < 35f || h > 175f) {
                      validationErrorFa = "لطفاً قد معتبر کودک را بین ۳۵ تا ۱۷۵ سانتی‌متر وارد کنید."
                      return@Button
                    }
                  }
                  validationErrorFa = null
                  step = 2
                }
                else -> {
                  if (!consentAccepted) {
                    validationErrorFa = "برای ذخیره پروفایل کودک، لطفاً تیک تأیید والد را فعال کنید."
                    return@Button
                  }
                  onComplete(
                    aliasFa.trim().ifBlank { "کوچولوی من" },
                    parsedBirthDateMs,
                    selectedAgeBand,
                    weightKgInput.trim(),
                    heightCmInput.trim(),
                  )
                }
              }
            },
            shape = RoundedCornerShape(16.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (isDark) TealGlow else SeaGreen,
                contentColor = if (isDark) DeepScreen else PureWhite,
              ),
            modifier =
              Modifier
                .weight(1f)
                .height(52.dp)
                .minimumInteractiveComponentSize()
                .testTag(if (step < 2) "onboarding_next_btn" else "onboarding_finish_btn"),
          ) {
            Text(
              text =
                when (step) {
                  0 -> "ادامه (تولد و وزن)"
                  1 -> "مشاهده پیش‌نمایش شخصی‌سازی"
                  else -> "ذخیره و شروع با مانو کید"
                },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            if (step < 2) {
              Spacer(Modifier.width(6.dp))
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OnboardingPreviewCard(
  icon: ImageVector,
  accent: Color,
  title: String,
  detail: String,
  testTag: String,
) {
  Surface(
    color = manoCardSurface(),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
    modifier = Modifier.fillMaxWidth().testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
          text = detail,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
