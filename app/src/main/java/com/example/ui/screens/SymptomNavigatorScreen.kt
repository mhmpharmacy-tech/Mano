package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AgeBand
import com.example.domain.model.MascotPose
import com.example.domain.model.ReviewStatus
import com.example.domain.model.SymptomFlow
import com.example.domain.model.TriageOutcome
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.ui.components.AlertSeverity
import com.example.ui.components.ClinicalBulletList
import com.example.ui.components.ClinicalNote
import com.example.ui.components.LtrText
import com.example.ui.components.ManoFramedIllustration
import com.example.ui.components.ManoPoseAvatar
import com.example.ui.components.PharmacistCtaCard
import com.example.ui.components.ProgressTimeline
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SourceBadge
import com.example.ui.components.SymptomChip
import com.example.ui.components.VisitDraftEntryBannerCard
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.SageCare
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.example.ui.viewmodel.SafetyChecklistUiState
import com.example.ui.viewmodel.SymptomWizardState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomNavigatorScreen(
  wizard: SymptomWizardState,
  flows: List<SymptomFlow>,
  onSelectSymptom: (String) -> Unit,
  onSelectAgeBand: (AgeBand) -> Unit,
  onUpdateTemp: (String) -> Unit,
  onUpdateDuration: (String) -> Unit,
  onToggleRedFlag: (String) -> Unit,
  onNext: () -> Unit,
  onBack: () -> Unit,
  onReset: () -> Unit,
  onOpenArticle: (String) -> Unit,
  onNavigateAskPharmacist: () -> Unit,
  timelineSnapshot: FeverDoseTimelineSnapshot =
    FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList()),
  savedVisitDraftsCount: Int = 0,
  onOpenVisitDraftStudio: () -> Unit = {},
  safetyChecklistState: SafetyChecklistUiState = SafetyChecklistUiState.Idle,
  onGenerateSafetyChecklist: (primaryFlow: SymptomFlow, coSymptomsFa: List<String>) -> Unit = { _, _ -> },
  onToggleSafetyChecklistItem: (String) -> Unit = {},
  onUpdateAgeMonths: (String) -> Unit = {},
  onResetChecklist: () -> Unit = {},
) {
  val currentFlow = flows.firstOrNull { it.id == wizard.selectedSymptomId } ?: flows.first()

  val companionSymptomOptions =
    remember {
      listOf(
        "بی‌قراری یا گریه مداوم",
        "کاهش اشتها یا امتناع از شیر/مایعات",
        "سرفه یا گرفتگی بینی",
        "خواب‌آلودگی غیرمعمول",
        "اسهال یا استفراغ خفیف",
        "بثورات یا قرمزی پوست",
        "کشیدن گوش یا گوش‌درد",
      )
    }
  var selectedCompanionSymptoms by rememberSaveable { mutableStateOf(emptyList<String>()) }

  androidx.compose.runtime.LaunchedEffect(wizard, selectedCompanionSymptoms) {
    onResetChecklist()
  }

  BackHandler(enabled = wizard.step > 0) {
    onBack()
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp)) {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth().imePadding().testTag("symptom_navigator_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        item {
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("راهنمای علائم کودک", modifier = Modifier.weight(1f),
              style = MaterialTheme.typography.titleLarge)
            TextButton(
              onClick = { selectedCompanionSymptoms = emptyList(); onReset() },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("wizard_reset_button"),
            ) {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("شروع مجدد")
            }
          }
        }

        item {
          ProgressTimeline(
            steps = listOf("۱. علامت", "۲. سن و زمینه", "۳. علائم خطر", "۴. نتیجه"),
            currentStepIndex = wizard.step,
          )
        }

        when (wizard.step) {
          // Step 0: Choose symptom
          0 -> {
            item {
              SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  ManoFramedIllustration(R.drawable.mano_symptoms_orbit, "مانو با دماسنج", width = 48.dp, aspectRatio = 1f)
                  Text("کدام موضوع یا علامت بیشتر نگرانتان کرده است؟", style = MaterialTheme.typography.titleMedium)
                }
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  flows.forEach { flow ->
                    SymptomChip(
                      label = flow.titleFa,
                      selected = flow.id == wizard.selectedSymptomId,
                      onClick = { onSelectSymptom(flow.id) },
                      modifier = Modifier.testTag("symptom_choice_${flow.id}"),
                    )
                  }
                }
                SourceBadge(reviewStatus = currentFlow.review.status)
                Text(currentFlow.subtitleFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(color = manoBorderColor())

                Text(
                  text = "علائم همراه (اختیاری — برای دقت بیشتر چک‌لیست هوشمند ویزیت متخصص کودکان):",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  companionSymptomOptions.forEachIndexed { idx, symptomLabel ->
                    val isSelected = symptomLabel in selectedCompanionSymptoms
                    FilterChip(
                      selected = isSelected,
                      onClick = {
                        selectedCompanionSymptoms =
                          if (isSelected) selectedCompanionSymptoms - symptomLabel
                          else selectedCompanionSymptoms + symptomLabel
                      },
                      label = { Text(symptomLabel, style = MaterialTheme.typography.labelMedium) },
                      modifier = Modifier.minimumInteractiveComponentSize().testTag("companion_symptom_chip_$idx"),
                    )
                  }
                }
              }
            }
          }

          // Step 1: Age + optional temperature & duration
          1 -> {
            item {
              SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  ManoPoseAvatar(pose = MascotPose.EXPLAIN, size = 56.dp)
                  Column {
                    Text("گام ۲: سن کودک را مشخص کنید", style = MaterialTheme.typography.titleMedium)
                    Text("علامت انتخاب‌شده: ${currentFlow.titleFa}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                  }
                }
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  AgeBand.entries.forEach { band ->
                    SymptomChip(
                      label = "${band.labelFa} (${band.rangeFa})",
                      selected = wizard.selectedAgeBand == band,
                      onClick = { onSelectAgeBand(band) },
                      modifier = Modifier.testTag("wizard_age_${band.id}"),
                    )
                  }
                }

                if (wizard.selectedAgeBand == AgeBand.INFANT_3_12M) {
                  OutlinedTextField(
                    value = wizard.ageMonthsText, onValueChange = onUpdateAgeMonths,
                    label = { Text("سن دقیق به ماه (اگر می‌دانید)") },
                    supportingText = { Text("در نبود سن دقیق، برای تب بالا مسیر محتاطانه انتخاب می‌شود.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("wizard_exact_age_input"),
                  )
                }
                if (currentFlow.asksTemperature || wizard.temperatureText.isNotBlank() ||
                  wizard.selectedAgeBand in setOf(AgeBand.NEWBORN, AgeBand.INFANT_3_12M)) {
                  OutlinedTextField(
                    value = wizard.temperatureText,
                    onValueChange = onUpdateTemp,
                    label = { Text("دمای ثبت‌شده دماسنج به سانتی‌گراد (مثلاً 38.0)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("wizard_temp_input"),
                  )
                }

                OutlinedTextField(
                  value = wizard.durationHoursText,
                  onValueChange = onUpdateDuration,
                  label = { Text("مدت زمان تقریبی علامت بر حسب ساعت (اختیاری)") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("wizard_duration_input"),
                )
              }
            }
          }

          // Step 2: Red flag check BEFORE any self-care guidance
          2 -> {
            item {
              SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  ManoPoseAvatar(pose = MascotPose.CAUTION, size = 56.dp)
                  Column {
                    Text("گام ۳: بررسی علائم هشدار فوری", style = MaterialTheme.typography.titleMedium)
                    Text(
                      text = "اگر هر یک از موارد زیر وجود دارد، علامت بزنید. در نبود آن‌ها گزینه ادامه را بزنید.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                currentFlow.redFlags.forEach { flag ->
                  val checked = flag.id in wizard.selectedRedFlagIds
                  Surface(
                    color = if (checked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .clickable { onToggleRedFlag(flag.id) }
                        .minimumInteractiveComponentSize()
                        .testTag("red_flag_${flag.id}"),
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                      Checkbox(checked = checked, onCheckedChange = { onToggleRedFlag(flag.id) })
                      Spacer(Modifier.width(8.dp))
                      Text(flag.labelFa, style = MaterialTheme.typography.bodyMedium)
                    }
                  }
                }
              }
            }
          }

          // Step 3: Outcome
          3 -> {
            val result = wizard.evaluation
            if (result != null) {
              item {
                val severity =
                  when (result.outcome) {
                    TriageOutcome.URGENT_EMERGENCY -> AlertSeverity.DANGER
                    TriageOutcome.NEEDS_TIMELY_REVIEW -> AlertSeverity.CAUTION
                    TriageOutcome.GENERAL_EDUCATIONAL -> AlertSeverity.INFO
                  }
                SeverityAlert(
                  severity = severity,
                  title = result.titleFa,
                  body = result.summaryFa,
                  showCautionMascot = result.outcome == TriageOutcome.URGENT_EMERGENCY,
                  modifier = Modifier.testTag("triage_outcome_card"),
                )
              }

              item {
                SectionCard {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = if (result.canShowSelfCare) "گام‌های آموزشی و مراقبت حمایتی" else "اقدامات فوری پیشنهادی",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    SourceBadge(reviewStatus = result.triggeredRule.reviewStatus)
                  }
                  ClinicalBulletList(
                    items = result.immediateActionsFa,
                    bulletColor = MaterialTheme.colorScheme.primary,
                  )
                }
              }

              item {
                SeverityAlert(
                  severity = AlertSeverity.CAUTION,
                  title = "معیارهای مراجعه فوری یا پیگیری پزشک",
                  body = result.escalationCriteriaFa.joinToString("\n\n• ", prefix = "• "),
                )
              }

              if (result.showPharmacistOption) item {
                GeminiPediatricianSafetyChecklistCard(
                  currentFlow = currentFlow,
                  companionSymptoms = selectedCompanionSymptoms,
                  companionOptions = companionSymptomOptions,
                  onToggleCompanionSymptom = { item ->
                    selectedCompanionSymptoms =
                      if (item in selectedCompanionSymptoms) selectedCompanionSymptoms - item
                      else selectedCompanionSymptoms + item
                  },
                  checklistState = safetyChecklistState,
                  onGenerate = {
                    onGenerateSafetyChecklist(currentFlow, selectedCompanionSymptoms)
                  },
                  onToggleCheckItem = onToggleSafetyChecklistItem,
                )
              }

              item {
                OutlinedButton(
                  onClick = { onOpenArticle(result.relatedArticleSlug) },
                  modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("triage_related_article_btn"),
                ) {
                  Text("مطالعه مقاله کامل مرتبط")
                }
              }

              if (result.showPharmacistOption) {
                item {
                  PharmacistCtaCard(
                    title = "پرسش از داروساز درباره مراقبت حمایتی",
                    onClick = onNavigateAskPharmacist,
                  )
                }
              }

              item {
                VisitDraftEntryBannerCard(
                  snapshot = timelineSnapshot,
                  selectedSymptomFlow = currentFlow,
                  savedDraftsCount = savedVisitDraftsCount,
                  buttonTag = "symptom_open_visit_draft_btn",
                  onOpenStudio = onOpenVisitDraftStudio,
                )
              }

              item {
                ClinicalNote(
                  title = "شفافیت قاعده بالینی (غیرتشخیصی)",
                  items =
                    listOf(
                      "شناسه قاعده فعال: ${result.triggeredRule.id}",
                      "توضیح قاعده: ${result.triggeredRule.descriptionFa}",
                      "مانو تشخیص بیماری صادر نمی‌کند و ابزار آموزشی سرنگ mL جایگزین نسخه پزشک یا داروساز نیست.",
                    ),
                  sources = currentFlow.sources,
                  reviewStatus = currentFlow.review.status,
                )
              }
            }
          }
        }

        if (wizard.step < 3) {
          item {
            Text("برای دسترسی به چک‌لیست آموزشی، ابتدا سن کودک و علائم هشدار را در غربالگری ثبت کنید.",
              style = MaterialTheme.typography.bodySmall)
          }

          item {
            VisitDraftEntryBannerCard(
              snapshot = timelineSnapshot,
              selectedSymptomFlow = currentFlow,
              savedDraftsCount = savedVisitDraftsCount,
              buttonTag = "symptom_open_visit_draft_btn",
              onOpenStudio = onOpenVisitDraftStudio,
            )
          }
        }

        if (wizard.validationErrorFa != null) {
          item {
            SeverityAlert(
              severity = AlertSeverity.CAUTION,
              title = "اطلاعات ورودی ناقص است",
              body = wizard.validationErrorFa,
            )
          }
        }
      }

      if (wizard.step < 3) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            if (wizard.step > 0) {
              OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("wizard_back_btn"),
              ) {
                Text("مرحله قبل")
              }
            }
            Button(
              onClick = onNext,
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("wizard_next_btn"),
            ) {
              Text(if (wizard.step == 2) "مشاهده نتیجه راهنما" else "ادامه")
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiPediatricianSafetyChecklistCard(
  currentFlow: SymptomFlow,
  companionSymptoms: List<String>,
  companionOptions: List<String>,
  onToggleCompanionSymptom: (String) -> Unit,
  checklistState: SafetyChecklistUiState,
  onGenerate: () -> Unit,
  onToggleCheckItem: (String) -> Unit,
) {
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val sageAccent = manoSemanticAccent(SageCare, DarkSage)

  SectionCard(modifier = Modifier.testTag("symptom_gemini_safety_checklist_card")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier =
            Modifier
              .width(4.dp)
              .height(26.dp)
              .clip(RoundedCornerShape(50))
              .background(sageAccent)
        )
        Column {
          LtrText(text = "MANO Safety • Gemini Pediatrician Checklist", color = sageAccent)
          Text(
            text = "چک‌لیست هوشمند زمان مراجعه به پزشک متخصص کودکان",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = null,
        tint = sageAccent,
        modifier = Modifier.size(22.dp),
      )
    }

    Text(
      text = "با انتخاب علامت اصلی (${currentFlow.titleFa}) و علائم همراه، هوش مصنوعی مانو یک چک‌لیست آموزشی تأییدنشده از علائم هشدار فوری، زمان مراجعه هم‌روز به متخصص کودکان و سوالات کلیدی ویزیت تهیه می‌کند.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      companionOptions.take(5).forEachIndexed { index, option ->
        val selected = option in companionSymptoms
        FilterChip(
          selected = selected,
          onClick = { onToggleCompanionSymptom(option) },
          label = { Text(option, style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.minimumInteractiveComponentSize().testTag("safety_checklist_symptom_chip_$index"),
        )
      }
    }

    Button(
      onClick = onGenerate,
      enabled = checklistState !is SafetyChecklistUiState.Loading,
      modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("generate_safety_checklist_btn"),
    ) {
      if (checklistState is SafetyChecklistUiState.Loading) {
        CircularProgressIndicator(
          modifier = Modifier.size(18.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.onPrimary,
        )
        Spacer(Modifier.width(8.dp))
        Text("در حال تنظیم چک‌لیست ایمنی با Gemini...")
      } else {
        Icon(Icons.Rounded.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("تولید چک‌لیست ایمنی مراجعه به متخصص کودکان")
      }
    }

    when (checklistState) {
      SafetyChecklistUiState.Idle -> Unit
      is SafetyChecklistUiState.Loading -> {
        Text(
          text = checklistState.contextSummaryFa,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      is SafetyChecklistUiState.Error -> {
        SeverityAlert(
          severity = AlertSeverity.CAUTION,
          title = "خطا در تولید چک‌لیست ایمنی",
          body = checklistState.messageFa,
          actionLabel = "تلاش مجدد",
          onAction = onGenerate,
          modifier = Modifier.testTag("safety_checklist_error_alert"),
        )
      }
      is SafetyChecklistUiState.Ready -> {
        val payload = checklistState.payload
        HorizontalDivider(color = manoBorderColor())

        Text(
          text = payload.checklistTitleFa,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = payload.urgencySummaryFa,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )

        if (payload.immediateEmergencySignsFa.isNotEmpty()) {
          Surface(
            color = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 0.85f, darkAlpha = 0.16f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, dangerAccent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "۱. علائم خطر فوری (مراجعه بلافاصله به اورژانس ۱۱۵):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = dangerAccent,
              )
              payload.immediateEmergencySignsFa.forEachIndexed { idx, sign ->
                val itemKey = "er_$idx"
                val isChecked = itemKey in checklistState.checkedItems
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clickable { onToggleCheckItem(itemKey) }
                      .minimumInteractiveComponentSize()
                      .testTag("safety_checklist_er_item_$idx"),
                ) {
                  Checkbox(checked = isChecked, onCheckedChange = { onToggleCheckItem(itemKey) })
                  Spacer(Modifier.width(6.dp))
                  Text(sign, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }

        if (payload.sameDayPediatricianVisitSignsFa.isNotEmpty()) {
          Surface(
            color = manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.75f, darkAlpha = 0.16f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, cautionAccent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "۲. چه زمانی همان روز به پزشک متخصص کودکان مراجعه کنیم؟",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = cautionAccent,
              )
              payload.sameDayPediatricianVisitSignsFa.forEachIndexed { idx, sign ->
                val itemKey = "same_day_$idx"
                val isChecked = itemKey in checklistState.checkedItems
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clickable { onToggleCheckItem(itemKey) }
                      .minimumInteractiveComponentSize()
                      .testTag("safety_checklist_sameday_item_$idx"),
                ) {
                  Checkbox(checked = isChecked, onCheckedChange = { onToggleCheckItem(itemKey) })
                  Spacer(Modifier.width(6.dp))
                  Text(sign, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }

        if (payload.homeMonitoringChecklistFa.isNotEmpty()) {
          Surface(
            color = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.65f, darkAlpha = 0.14f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, sageAccent.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "۳. چک‌لیست پایش ایمن در خانه تا زمان ویزیت:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = sageAccent,
              )
              payload.homeMonitoringChecklistFa.forEachIndexed { idx, step ->
                val itemKey = "home_$idx"
                val isChecked = itemKey in checklistState.checkedItems
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clickable { onToggleCheckItem(itemKey) }
                      .minimumInteractiveComponentSize()
                      .testTag("safety_checklist_home_item_$idx"),
                ) {
                  Checkbox(checked = isChecked, onCheckedChange = { onToggleCheckItem(itemKey) })
                  Spacer(Modifier.width(6.dp))
                  Text(step, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }

        if (payload.questionsForPediatricianFa.isNotEmpty()) {
          Text(
            text = "سوالات پیشنهادی برای پرسیدن از متخصص کودکان در مطب:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          payload.questionsForPediatricianFa.forEach { q ->
            Text("• $q", style = MaterialTheme.typography.bodySmall)
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.VerifiedUser,
            contentDescription = null,
            tint = sageAccent,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = payload.clinicalSourceCitationFa,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
