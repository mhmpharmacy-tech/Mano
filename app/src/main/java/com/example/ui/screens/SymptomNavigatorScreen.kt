package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AgeBand
import com.example.domain.model.MascotPose
import com.example.domain.model.SymptomFlow
import com.example.domain.model.TriageOutcome
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.ui.components.AlertSeverity
import com.example.ui.components.ClinicalNote
import com.example.ui.components.ManoPoseAvatar
import com.example.ui.components.PharmacistCtaCard
import com.example.ui.components.ProgressTimeline
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SourceBadge
import com.example.ui.components.SymptomChip
import com.example.ui.components.VisitDraftEntryBannerCard
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
) {
  val currentFlow = flows.firstOrNull { it.id == wizard.selectedSymptomId } ?: flows.first()

  BackHandler(enabled = wizard.step > 0) {
    onBack()
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 840.dp)) {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("هدایتگر گام‌به‌گام علائم کودک", style = MaterialTheme.typography.headlineMedium)
              Text(
                text = "ابزار آموزشی و غربالگری علائم هشدار — تشخیص پزشکی یا تجویز دارو انجام نمی‌دهد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            TextButton(
              onClick = onReset,
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
                Image(
                  painter = painterResource(id = R.drawable.img_symptom_care_banner),
                  contentDescription = "مانو در اتاق آرام کودک همراه دماسنج و سرنگ خوراکی",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxWidth().height(156.dp).clip(RoundedCornerShape(16.dp)),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  ManoPoseAvatar(pose = MascotPose.LISTEN, size = 56.dp)
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
                Text(currentFlow.subtitleFa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                if (currentFlow.asksTemperature) {
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
                  result.immediateActionsFa.forEach { stepText ->
                    Text("• $stepText", style = MaterialTheme.typography.bodyMedium)
                  }
                }
              }

              item {
                SeverityAlert(
                  severity = AlertSeverity.CAUTION,
                  title = "معیارهای مراجعه فوری یا پیگیری پزشک",
                  body = result.escalationCriteriaFa.joinToString("\n• ", prefix = "• "),
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
                      "مانو تشخیص بیماری صادر نمی‌کند و دوز عددی دارو محاسبه نمی‌نماید.",
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
