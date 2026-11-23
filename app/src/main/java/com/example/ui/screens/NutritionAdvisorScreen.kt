package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.ManoCircularProgressSize
import com.example.ui.components.ManoTurquoiseLoadingRing
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.MascotPose
import com.example.domain.model.MealSuggestionItem
import com.example.domain.model.MilestoneItem
import com.example.domain.model.NutritionAppetiteStatus
import com.example.domain.model.NutritionPlanResult
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.domain.util.NutritionPromptBuilder
import com.example.ui.components.AlertSeverity
import com.example.ui.components.FlowchartStepSpec
import com.example.ui.components.ManoBannerFrame
import com.example.ui.components.ManoCompactScreenHeader
import com.example.ui.components.ManoFlowchartStepperBar
import com.example.ui.components.ManoIconMedallion
import com.example.ui.components.ManoPoseAvatar
import com.example.ui.components.PediatricIconKind
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SymptomChip
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.DeepScreen
import com.example.ui.theme.GoldSurface
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoSurface
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.MangoCare
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.example.ui.viewmodel.NutritionUiState

@Composable
fun NutritionGrowthEntryCard(
  childProfile: ChildProfileDemo?,
  selectedAgeBand: AgeBand?,
  savedHeightCm: String,
  checkedMilestonesCount: Int,
  savedPlansCount: Int,
  onOpenNutritionAdvisor: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val effectiveBand = childProfile?.ageBand ?: selectedAgeBand
  val profileHeadline =
    if (childProfile != null) {
      val weightTxt = childProfile.weightKgText.takeIf { it.isNotBlank() }?.let { " • $it kg" }.orEmpty()
      val heightTxt = savedHeightCm.takeIf { it.isNotBlank() }?.let { " • $it cm" }.orEmpty()
      "پروفایل فعال: ${childProfile.aliasFa} (${childProfile.birthDateOrAgeFa}$weightTxt$heightTxt)"
    } else {
      val bandText = effectiveBand?.labelFa ?: "رده سنی عمومی"
      "پروفایل کودک هنوز ثبت نشده (بر اساس $bandText و داده‌های رشد فعلی)"
    }

  val mangoAccent = manoSemanticAccent(MangoCare, DarkGold)
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, mangoAccent.copy(alpha = 0.45f)),
    modifier = modifier.fillMaxWidth().testTag("growth_nutrition_entry_card"),
  ) {
    Column(
      modifier =
        Modifier
          .background(
            Brush.linearGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                  MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                ),
            )
          )
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ManoBannerFrame(
        resId = R.drawable.img_nutrition_banner,
        contentDescription = "مانو در کنار وعده غذای سالم و مقوی کودک",
        aspectRatio = 16f / 9f,
        cornerRadius = 16.dp,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        ManoIconMedallion(
          icon = Icons.Rounded.Restaurant,
          accentColor = mangoAccent,
          iconKind = PediatricIconKind.FORMULA,
          containerSize = 48.dp,
          iconSize = 26.dp,
          cornerRadius = 15.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "پیشنهاد غذای سالم و مقوی هوشمند",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = profileHeadline,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Text(
        text =
          buildString {
            append("متناسب با سن کودک، وزن، قد، حساسیت‌ها و $checkedMilestonesCount نقطه عطف رشد ثبت‌شده")
            if (savedPlansCount > 0) append(" ($savedPlansCount برنامه ذخیره‌شده)")
          },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )

      val isDark = LocalManoDarkTheme.current
      Button(
        onClick = onOpenNutritionAdvisor,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = mangoAccent,
            contentColor = if (isDark) DeepScreen else PureWhite,
          ),
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("growth_open_nutrition_btn"),
      ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("دریافت برنامه غذای سالم کودک")
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionAdvisorScreen(
  nutritionUiState: NutritionUiState,
  savedPlans: List<NutritionPlanResult>,
  childProfile: ChildProfileDemo?,
  selectedAgeBand: AgeBand?,
  savedHeightCm: String,
  checkedMilestones: List<MilestoneItem>,
  timelineSnapshot: FeverDoseTimelineSnapshot,
  onSaveHeightCm: (String) -> Unit,
  onGeneratePlan: (
    ageBand: AgeBand,
    ageDetailFa: String,
    weightKgText: String,
    heightCmText: String,
    allergiesFa: String,
    appetiteStatus: NutritionAppetiteStatus,
    availableIngredientsFa: String,
  ) -> Unit,
  onLoadSavedPlan: (NutritionPlanResult) -> Unit,
  onDeleteSavedPlan: (String) -> Unit,
  onOpenMyChildProfile: () -> Unit,
  onOpen3DWeighingGuide: () -> Unit,
  onFeedback: (String) -> Unit,
) {
  val context = LocalContext.current

  var ageBand by remember(childProfile, selectedAgeBand) {
    mutableStateOf(childProfile?.ageBand ?: selectedAgeBand ?: AgeBand.INFANT_3_12M)
  }
  var ageDetailFa by remember(childProfile) {
    mutableStateOf(childProfile?.birthDateOrAgeFa.orEmpty())
  }
  var weightKgText by remember(childProfile) {
    mutableStateOf(childProfile?.weightKgText.orEmpty())
  }
  var heightCmText by remember(savedHeightCm) {
    mutableStateOf(savedHeightCm)
  }
  var allergiesFa by remember(childProfile) {
    mutableStateOf(
      childProfile?.allergiesFa?.takeUnless { it == "ثبت نشده" || it == "ندارد" }.orEmpty()
    )
  }
  var appetiteStatus by remember {
    mutableStateOf(
      if ((timelineSnapshot.latestTempCelsius ?: 0f) >= 38.0f) {
        NutritionAppetiteStatus.FEVER_RECOVERY
      } else {
        NutritionAppetiteStatus.NORMAL
      }
    )
  }
  var availableIngredientsFa by remember { mutableStateOf("") }

  LaunchedEffect(savedHeightCm) {
    if (heightCmText.isBlank() && savedHeightCm.isNotBlank()) {
      heightCmText = savedHeightCm
    }
  }

  val isUnderSixMonths = remember(ageBand, ageDetailFa) {
    NutritionPromptBuilder.isLikelyUnderSixMonths(ageBand, ageDetailFa)
  }
  val isUnderTwelveMonths = ageBand == AgeBand.NEWBORN || ageBand == AgeBand.INFANT_3_12M

  val triggerGeneration: () -> Unit = {
    onSaveHeightCm(heightCmText)
    onGeneratePlan(
      ageBand,
      ageDetailFa,
      weightKgText,
      heightCmText,
      allergiesFa,
      appetiteStatus,
      availableIngredientsFa,
    )
  }

  val mangoAccent = manoSemanticAccent(MangoCare, DarkGold)
  val nutritionFlowSteps =
    remember {
      listOf(
        FlowchartStepSpec("child", "۱. سن و رشد", Icons.Rounded.MonitorWeight),
        FlowchartStepSpec("appetite", "۲. اشتها", Icons.Rounded.Restaurant),
        FlowchartStepSpec("plan", "۳. برنامه", Icons.Rounded.AutoAwesome),
      )
    }
  val activeStepIdx = if (nutritionUiState is NutritionUiState.Ready) 2 else 0

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).imePadding().testTag("nutrition_advisor_list"),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      item {
        ManoCompactScreenHeader(
          titleFa = "مشاور تغذیه کودک",
          subtitleFa = if (childProfile != null) "همگام با ${childProfile.aliasFa} • ${checkedMilestones.size} مهارت" else "برنامه غذایی سالم متناسب با سن و رشد",
          icon = Icons.Rounded.Restaurant,
          accentColor = mangoAccent,
          trailingAction = {
            TextButton(
              onClick = onOpenMyChildProfile,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("nutrition_open_profile_btn"),
            ) {
              Icon(Icons.Rounded.PersonOutline, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(3.dp))
              Text("پروفایل", style = MaterialTheme.typography.labelSmall)
            }
          },
        )
      }

      item {
        ManoFlowchartStepperBar(
          steps = nutritionFlowSteps,
          activeIndex = activeStepIdx,
          accentColor = mangoAccent,
        )
      }

      // Compact Context & Growth Inputs Card
      item {
        SectionCard(modifier = Modifier.testTag("nutrition_context_card")) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(AgeBand.entries) { band ->
              SymptomChip(
                label = band.labelFa,
                selected = ageBand == band,
                onClick = { ageBand = band },
                modifier = Modifier.testTag("nutrition_age_${band.id}"),
              )
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            OutlinedTextField(
              value = ageDetailFa,
              onValueChange = { ageDetailFa = it },
              label = { Text("سن دقیق") },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1.1f).testTag("nutrition_age_detail_input"),
            )
            OutlinedTextField(
              value = weightKgText,
              onValueChange = { weightKgText = it },
              label = { Text("وزن kg") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(0.75f).testTag("nutrition_weight_input"),
            )
            OutlinedTextField(
              value = heightCmText,
              onValueChange = { heightCmText = it },
              label = { Text("قد cm") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(0.75f).testTag("nutrition_height_input"),
            )
          }

          val chipContainer = manoCardSurface()
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(NutritionAppetiteStatus.entries) { st ->
              FilterChip(
                selected = appetiteStatus == st,
                onClick = { appetiteStatus = st },
                label = { Text(st.labelFa) },
                colors = FilterChipDefaults.filterChipColors(containerColor = chipContainer),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("nutrition_status_${st.id}"),
              )
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            OutlinedTextField(
              value = allergiesFa,
              onValueChange = { allergiesFa = it },
              label = { Text("حساسیت غذایی") },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f).testTag("nutrition_allergies_input"),
            )
            OutlinedTextField(
              value = availableIngredientsFa,
              onValueChange = { availableIngredientsFa = it },
              label = { Text("مواد در دسترس") },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f).testTag("nutrition_ingredients_input"),
            )
          }

          if (isUnderSixMonths) {
            SeverityAlert(
              severity = AlertSeverity.CAUTION,
              title = "ایمنی نوزاد زیر ۶ ماه",
              body = "فقط شیر مادر یا شیرخشک استاندارد؛ غذای کمکی، آب ساده و عسل ممنوع است.",
              modifier = Modifier.testTag("nutrition_under6m_safety_banner"),
            )
          } else if (isUnderTwelveMonths) {
            SeverityAlert(
              severity = AlertSeverity.INFO,
              title = "ایمنی زیر ۱ سال",
              body = "عسل تا ۱۲ ماهگی ممنوع است؛ غذا بدون نمک/شکر و با بافت نرم باشد.",
              modifier = Modifier.testTag("nutrition_under12m_safety_banner"),
            )
          }

          Button(
            onClick = triggerGeneration,
            enabled = nutritionUiState !is NutritionUiState.Loading,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("generate_nutrition_plan_btn"),
          ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("دریافت برنامه غذای سالم کودک")
          }
        }
      }

      // Loading State
      if (nutritionUiState is NutritionUiState.Loading) {
        item {
          SectionCard(modifier = Modifier.testTag("nutrition_loading_card")) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              ManoTurquoiseLoadingRing(
                size = ManoCircularProgressSize.SMALL,
                contentDescriptionFa = "در حال طراحی برنامه غذایی سالم",
              )
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "در حال طراحی برنامه غذایی سالم و متناسب با رشد...",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = nutritionUiState.childContextSummaryFa,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      // Error State (Transparent error propagation — never swallow or mock)
      if (nutritionUiState is NutritionUiState.Error) {
        item {
          SeverityAlert(
            severity = AlertSeverity.DANGER,
            title = "خطا در دریافت پیشنهاد تغذیه",
            body = nutritionUiState.messageFa,
            actionLabel = "تلاش مجدد",
            onAction = triggerGeneration,
            modifier = Modifier.testTag("nutrition_error_alert"),
          )
        }
      }

      // Ready Result Cards
      if (nutritionUiState is NutritionUiState.Ready) {
        val plan = nutritionUiState.plan
        item {
          SectionCard(modifier = Modifier.testTag("nutrition_result_summary_card")) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = plan.planTitleFa,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
              )
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                  onClick = {
                    val shareable = NutritionPromptBuilder.formatShareableNutritionTextFa(plan)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("برنامه غذای کودک مانو", shareable))
                    onFeedback("برنامه غذایی در حافظه کپی شد.")
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("nutrition_copy_btn"),
                ) {
                  Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی برنامه غذایی")
                }
                IconButton(
                  onClick = {
                    val shareable = NutritionPromptBuilder.formatShareableNutritionTextFa(plan)
                    val sendIntent =
                      Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, plan.planTitleFa)
                        putExtra(Intent.EXTRA_TEXT, shareable)
                      }
                    runCatching {
                      context.startActivity(Intent.createChooser(sendIntent, "اشتراک‌گذاری برنامه غذای کودک"))
                    }.onFailure {
                      onFeedback("امکان باز کردن صفحه اشتراک‌گذاری وجود ندارد.")
                    }
                  },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("nutrition_share_btn"),
                ) {
                  Icon(Icons.Rounded.Share, contentDescription = "اشتراک‌گذاری برنامه غذایی")
                }
              }
            }

            if (plan.childContextSummaryFa.isNotBlank()) {
              Text(
                text = plan.childContextSummaryFa,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
              )
            }

            if (plan.overviewFa.isNotBlank()) {
              Text(plan.overviewFa, style = MaterialTheme.typography.bodyMedium)
            }

            if (plan.growthNutritionFocusFa.isNotBlank()) {
              Surface(
                color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 0.65f, darkAlpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "تمرکز تغذیه‌ای رشد",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(plan.growthNutritionFocusFa, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }

        itemsIndexed(plan.meals, key = { idx, item -> "${plan.id}_meal_${idx}_${item.titleFa}" }) { idx, meal ->
          MealSuggestionCard(meal = meal, index = idx)
        }

        if (plan.hydrationTipFa.isNotBlank()) {
          item {
            val infoAccent = manoSemanticAccent(InfoBlue, DarkSky)
            Surface(
              color = manoTintedSurface(InfoSurface, DarkSky, lightAlpha = 0.72f, darkAlpha = 0.16f),
              shape = RoundedCornerShape(18.dp),
              border = BorderStroke(1.dp, infoAccent.copy(alpha = 0.30f)),
              modifier = Modifier.fillMaxWidth().testTag("nutrition_hydration_card"),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(Icons.Rounded.LocalDrink, contentDescription = null, tint = infoAccent)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text("نوشیدنی و آبرسانی سالم", style = MaterialTheme.typography.labelLarge, color = infoAccent, fontWeight = FontWeight.Bold)
                  Text(plan.hydrationTipFa, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }

        if (plan.safetyAvoidListFa.isNotEmpty()) {
          item {
            SeverityAlert(
              severity = AlertSeverity.CAUTION,
              title = "موارد ممنوع یا نیازمند احتیاط برای این کودک",
              body = plan.safetyAvoidListFa.joinToString("\n• ", prefix = "• "),
              modifier = Modifier.testTag("nutrition_safety_avoid_card"),
            )
          }
        }

        if (plan.doctorFollowUpHintFa.isNotBlank()) {
          item {
            SectionCard {
              Text("زمان مشورت با پزشک یا کارشناس تغذیه", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
              Text(plan.doctorFollowUpHintFa, style = MaterialTheme.typography.bodySmall)
              Text(
                text = "یادآوری: این پیشنهادهای هوشمند آموزشی است و جایگزین ارزیابی پزشک یا متخصص تغذیه کودک نیست.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Saved Plans History
      if (savedPlans.isNotEmpty()) {
        item {
          SectionCard(modifier = Modifier.testTag("nutrition_saved_plans_section")) {
            Text(
              text = "برنامه‌های غذایی ذخیره‌شده (${savedPlans.size})",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            val dividerColor = manoBorderColor()
            savedPlans.forEachIndexed { index, saved ->
              if (index > 0) HorizontalDivider(color = dividerColor)
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onLoadSavedPlan(saved) }
                    .minimumInteractiveComponentSize()
                    .padding(vertical = 6.dp)
                    .testTag("load_nutrition_plan_${saved.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(saved.planTitleFa, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                  Text(
                    text = "${saved.childContextSummaryFa} • ${saved.createdAtLabelFa}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                IconButton(
                  onClick = { onDeleteSavedPlan(saved.id) },
                  modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_nutrition_plan_${saved.id}"),
                ) {
                  Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف برنامه غذایی", tint = MaterialTheme.colorScheme.error)
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
private fun MealSuggestionCard(
  meal: MealSuggestionItem,
  index: Int,
) {
  var expandedPrep by remember(meal.titleFa) { mutableStateOf(true) }

  val mangoAccent = manoSemanticAccent(MangoCare, DarkGold)
  val successAccent = manoSemanticAccent(SuccessEmerald, DarkSage)

  SectionCard(modifier = Modifier.testTag("nutrition_meal_card_$index")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        color = manoTintedSurface(GoldSurface, DarkGold, lightAlpha = 1f, darkAlpha = 0.18f),
        shape = RoundedCornerShape(10.dp),
      ) {
        Text(
          text = meal.mealTypeFa.ifBlank { "وعده پیشنهادی" },
          style = MaterialTheme.typography.labelMedium,
          color = mangoAccent,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
      if (meal.textureAndPortionFa.isNotBlank()) {
        Surface(
          color = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.7f, darkAlpha = 0.16f),
          shape = RoundedCornerShape(10.dp),
        ) {
          Text(
            text = meal.textureAndPortionFa,
            style = MaterialTheme.typography.labelSmall,
            color = successAccent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }

    Text(meal.titleFa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

    if (meal.whySuitableFa.isNotBlank()) {
      Text(
        text = meal.whySuitableFa,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (meal.ingredientsFa.isNotEmpty()) {
      Text("مواد لازم:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
      meal.ingredientsFa.forEach { ing ->
        Text("• $ing", style = MaterialTheme.typography.bodyMedium)
      }
    }

    if (meal.prepStepsFa.isNotEmpty()) {
      TextButton(
        onClick = { expandedPrep = !expandedPrep },
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text(if (expandedPrep) "بستن مراحل تهیه" else "مشاهده مراحل تهیه")
      }
      AnimatedVisibility(visible = expandedPrep) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          meal.prepStepsFa.forEachIndexed { stepIdx, step ->
            Text("${stepIdx + 1}. $step", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}
