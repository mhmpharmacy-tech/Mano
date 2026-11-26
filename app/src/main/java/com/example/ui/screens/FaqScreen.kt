package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.ui.components.ManoCircularProgressSize
import com.example.ui.components.ManoHealthRingTone
import com.example.ui.components.ManoTurquoiseLoadingRing
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AgeBand
import com.example.domain.model.FaqItem
import com.example.domain.model.MascotPose
import com.example.domain.model.ReviewStatus
import com.example.domain.model.TopicCategory
import com.example.domain.util.PersianNormalizer
import com.example.ui.components.AgeSelectorRow
import com.example.ui.components.AlertSeverity
import com.example.ui.components.EmptyState
import com.example.ui.components.LtrText
import com.example.ui.components.ManoFramedIllustration
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SourceBadge
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.SageCare
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface
import com.example.ui.viewmodel.DocumentedQaUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FaqScreen(
  query: String,
  selectedCategory: TopicCategory?,
  selectedAgeBand: AgeBand?,
  faqs: List<FaqItem>,
  expandedIds: Set<String>,
  onQueryChange: (String) -> Unit,
  onCategorySelect: (TopicCategory?) -> Unit,
  onAgeSelect: (AgeBand?) -> Unit,
  onToggleExpand: (String) -> Unit,
  onNavigateSymptoms: () -> Unit,
  documentedQaState: DocumentedQaUiState = DocumentedQaUiState.Idle,
  onAskDocumentedQuestion: (String) -> Unit = {},
) {
  var assistantQuestionInput by rememberSaveable { mutableStateOf("") }
  var showFilters by rememberSaveable { mutableStateOf(false) }
  var showAssistant by rememberSaveable { mutableStateOf(false) }
  val sampleQuestions =
    remember {
      listOf(
        "تفاوت قطره و شربت استامینوفن چیست؟",
        "چه زمانی تب کودک نیاز به مراجعه فوری دارد؟",
        "چگونه با سرنگ خوراکی mL به کودک دارو بدهیم؟",
        "علائم کم‌آبی در اسهال کودک چیست؟",
      )
    }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("faq_list"),
      contentPadding = ManoTheme.spacing.screenContentPadding,
      verticalArrangement = Arrangement.spacedBy(ManoTheme.spacing.sectionGap),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ManoFramedIllustration(R.drawable.mano_orbit_attentive, "مانو کید، همراه پرسش‌های والدین", width = 48.dp, aspectRatio = 1f)
            Text("پرسش‌های والدین", style = MaterialTheme.typography.titleLarge)
          }
          OutlinedTextField(
            value = query,
            onValueChange = { showAssistant = false; onQueryChange(it) },
            label = { Text("پرسش یا موضوع را جست‌وجو کنید") },
            placeholder = { Text("مثلاً تب، شیرخشک یا یبوست") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
              if (query.isNotEmpty()) {
                IconButton(
                  onClick = { onQueryChange("") },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(Icons.Rounded.Close, contentDescription = "پاک کردن جست‌وجو")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().testTag("faq_search_input"),
          )
        }
      }

      if (PersianNormalizer.isHighRiskQuery(query)) {
        item {
          SeverityAlert(
            severity = AlertSeverity.DANGER,
            title = "یادآوری فوری ایمنی بالینی",
            body =
              "این جست‌وجو شامل موضوع پرخطر است. در تب نوزاد زیر ۳ ماه (≥ ۳۸ درجه)، تنگی نفس، تشنج یا بی‌حالی شدید بلافاصله به اورژانس ۱۱۵ مراجعه کنید.",
            showCautionMascot = true,
            actionLabel = "بررسی گام‌به‌گام علائم خطر",
            onAction = onNavigateSymptoms,
          )
        }
      }

      item {
        androidx.compose.material3.TextButton(onClick = { showAssistant = !showAssistant },
          modifier = Modifier.testTag("faq_toggle_assistant")) {
          Text(if (showAssistant) "بستن دستیار آموزشی" else "پرسیدن از دستیار هوش مصنوعی")
        }
      }
      // دستیار آموزشی؛ پاسخ هوش مصنوعی (MANO Documented Q&A Assistant)
      if (showAssistant) item {
        val sageAccent = manoSemanticAccent(SageCare, DarkSage)
        val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
        SectionCard(modifier = Modifier.testTag("documented_qa_assistant_card")) {
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
                LtrText(text = "MANO Evidence Assistant", color = sageAccent)
                Text(
                  text = "دستیار آموزشی؛ پاسخ هوش مصنوعی",
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
            text = "پرسش خود را بنویسید. پاسخ تولیدشده با هوش مصنوعی تأیید علمی مستقل ندارد؛ پاسخ‌های کتاب‌محور و نشانی منابع در فهرست پرسش‌ها آمده‌اند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            sampleQuestions.forEachIndexed { idx, sample ->
              SuggestionChip(
                onClick = {
                  assistantQuestionInput = sample
                  onAskDocumentedQuestion(sample)
                },
                label = { Text(sample, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("documented_qa_sample_$idx"),
              )
            }
          }

          OutlinedTextField(
            value = assistantQuestionInput,
            onValueChange = { assistantQuestionInput = it },
            label = { Text("سوال شما از دستیار مستند (مثلاً نحوه نگهداری شربت آنتی‌بیوتیک)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("documented_qa_input"),
          )

          Button(
            onClick = { onAskDocumentedQuestion(assistantQuestionInput.ifBlank { query }) },
            enabled = documentedQaState !is DocumentedQaUiState.Loading && assistantQuestionInput.ifBlank { query }.trim().length >= 4,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("documented_qa_submit_btn"),
          ) {
            if (documentedQaState is DocumentedQaUiState.Loading) {
              ManoTurquoiseLoadingRing(
                size = ManoCircularProgressSize.COMPACT,
                tone = ManoHealthRingTone.ON_PRIMARY,
                contentDescriptionFa = "در حال تولید پاسخ آموزشی",
              )
              Spacer(Modifier.width(8.dp))
              Text("در حال تولید پاسخ آموزشی...")
            } else {
              Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("دریافت پاسخ آموزشی")
            }
          }

          when (documentedQaState) {
            DocumentedQaUiState.Idle -> Unit
            is DocumentedQaUiState.Loading -> {
              Text(
                text = "پرسش در حال بررسی: «${documentedQaState.questionFa}»",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            is DocumentedQaUiState.Error -> {
              SeverityAlert(
                severity = AlertSeverity.CAUTION,
                title = "عدم دریافت پاسخ دستیار مستند",
                body = documentedQaState.messageFa,
                modifier = Modifier.testTag("documented_qa_error"),
              )
            }
            is DocumentedQaUiState.Ready -> {
              val payload = documentedQaState.payload
              HorizontalDivider(color = manoBorderColor())
              Text(
                text = "پاسخ تولیدشده برای: «${documentedQaState.questionFa}»",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
              SeverityAlert(
                severity = AlertSeverity.CAUTION, title = "پاسخ هوش مصنوعی؛ تأییدنشده",
                body = "این پاسخ ممکن است خطا داشته باشد. برای تصمیم دارویی با داروساز گفت‌وگو کنید و در علائم خطر منتظر پیام آنلاین نمانید.",
              )
              Text(
                text = payload.directAnswerFa,
                style = MaterialTheme.typography.bodyMedium,
              )

              if (payload.actionableStepsFa.isNotEmpty()) {
                Text(
                  text = "پیشنهادهای تولیدشده؛ نیازمند بررسی:",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = sageAccent,
                )
                payload.actionableStepsFa.forEach { step ->
                  Text("• $step", style = MaterialTheme.typography.bodySmall)
                }
              }

              if (payload.whenToVisitPediatricianFa.isNotEmpty()) {
                Surface(
                  color = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 0.85f, darkAlpha = 0.16f),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = "چه زمانی به پزشک متخصص کودکان مراجعه کنیم:",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      color = dangerAccent,
                    )
                    payload.whenToVisitPediatricianFa.forEach { warn ->
                      Text("• $warn", style = MaterialTheme.typography.bodySmall)
                    }
                  }
                }
              }

              Surface(
                color = manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.65f, darkAlpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = sageAccent, modifier = Modifier.size(16.dp))
                    Text("منابع زمینهٔ پاسخ؛ تطبیق ادعاها تأیید نشده:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = sageAccent)
                  }
                  payload.citedSourcesFa.forEach { src ->
                    Text("• $src", style = MaterialTheme.typography.labelSmall)
                  }
                  Text(
                    text = payload.safetyDisclaimerFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }


      item {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()) {
          Text("${measurementNumberFa(faqs.size.toDouble())} پرسش", style = MaterialTheme.typography.labelLarge)
          androidx.compose.material3.TextButton(onClick = { showFilters = !showFilters },
            modifier = Modifier.testTag("faq_toggle_filters")) {
            val selected = listOfNotNull(selectedCategory?.labelFa, selectedAgeBand?.labelFa).joinToString("، ")
            Text(when {
              showFilters -> "بستن فیلترها"
              selected.isNotEmpty() -> "فیلترها: $selected"
              else -> "فیلتر سن و موضوع"
            })
          }
        }
      }
      if (showFilters) item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = selectedCategory == null,
              onClick = { onCategorySelect(null) },
              label = { Text("همه") },
              modifier = Modifier.minimumInteractiveComponentSize(),
            )
          }
          items(TopicCategory.entries) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { onCategorySelect(cat) },
              label = { Text(cat.labelFa) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            )
          }
        }
      }

      if (showFilters) item {
        AgeSelectorRow(selectedAgeBand = selectedAgeBand, onSelect = onAgeSelect)
      }

      if (faqs.isEmpty()) {
        item {
          EmptyState(
            title = "پرسش منطبقی پیدا نشد",
            subtitle = "فیلتر سن یا دسته را تغییر دهید، یا پرسش خود را در بالای صفحه بنویسید.",
            pose = MascotPose.WAIT,
            actionLabel = "پاک کردن فیلترهای پرسش",
            onAction = {
              onQueryChange("")
              onCategorySelect(null)
              onAgeSelect(null)
            },
          )
        }
      } else {
        items(faqs, key = { it.id }) { faq ->
          val expanded = faq.id in expandedIds
          Card(
            onClick = { onToggleExpand(faq.id) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("faq_card_${faq.id}"),
          ) {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = faq.questionFa,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                  imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                  contentDescription = if (expanded) "بستن پاسخ" else "باز کردن پاسخ",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  SourceBadge(reviewStatus = faq.review.status)
                  Text("خلاصه:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                  Text(faq.summaryFa, style = MaterialTheme.typography.bodyMedium)

                  Text("چه کار کنم:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                  faq.whatToDoFa.forEach { step ->
                    Text("• $step", style = MaterialTheme.typography.bodySmall)
                  }

                  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
                  Surface(color = manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 1f, darkAlpha = 0.16f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text("چه زمانی به پزشک مراجعه کنم:", style = MaterialTheme.typography.labelLarge, color = dangerAccent)
                      faq.whenToSeeDoctorFa.forEach { warn ->
                        Text("• $warn", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                      }
                    }
                  }

                  Text(
                    text = "منبع: ${faq.source.publisher} — ${faq.source.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (faq.source.url.isNotBlank()) LtrText(text = faq.source.url, color = manoSemanticAccent(InfoBlue, DarkSky))
                }
              }
            }
          }
        }
      }
    }
  }
}
