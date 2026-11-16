package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AgeBand
import com.example.domain.model.FaqItem
import com.example.domain.model.MascotPose
import com.example.domain.model.TopicCategory
import com.example.domain.util.PersianNormalizer
import com.example.ui.components.AgeSelectorRow
import com.example.ui.components.AlertSeverity
import com.example.ui.components.EmptyState
import com.example.ui.components.LtrText
import com.example.ui.components.ManoPoseAvatar
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SourceBadge
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkSky
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface

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
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("faq_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        SectionCard {
          Image(
            painter = painterResource(id = R.drawable.img_topics_library_banner),
            contentDescription = "مانو و کتابخانه پرسش‌های پرتکرار والدین",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(144.dp).clip(RoundedCornerShape(16.dp)),
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ManoPoseAvatar(pose = MascotPose.EXPLAIN, size = 56.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text("پرسش‌های پرتکرار والدین (FAQ)", style = MaterialTheme.typography.headlineMedium)
              Text(
                text = "جست‌وجوی فارسی با نرمال‌سازی نیم‌فاصله و حروف (مثلاً شیر خشک و شیرخشک هر دو نتیجه مشابه می‌دهند).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        OutlinedTextField(
          value = query,
          onValueChange = onQueryChange,
          label = { Text("جست‌وجو (تب نوزاد، استامینوفن، شیرخشک، یبوست، پوشک...)") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("faq_search_input"),
        )
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

      item {
        AgeSelectorRow(selectedAgeBand = selectedAgeBand, onSelect = onAgeSelect)
      }

      item {
        Text(
          text = "تعداد نتایج: ${faqs.size} پرسش",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (faqs.isEmpty()) {
        item {
          EmptyState(
            title = "پرسش منطبقی پیدا نشد",
            subtitle = "فیلتر سن یا دسته را تغییر دهید تا پاسخ‌های مرتبط نمایش داده شوند.",
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
            border = BorderStroke(1.dp, if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().testTag("faq_card_${faq.id}"),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 1f, darkAlpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                  Text(
                    text = faq.category.labelFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
                SourceBadge(reviewStatus = faq.review.status)
              }

              AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  HorizontalDivider(color = MaterialTheme.colorScheme.outline)
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
                  LtrText(text = faq.source.url, color = manoSemanticAccent(InfoBlue, DarkSky))
                }
              }
            }
          }
        }
      }
    }
  }
}
