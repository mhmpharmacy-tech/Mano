package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.domain.model.AgeBand
import com.example.domain.model.Article
import com.example.domain.model.MascotPose
import com.example.domain.model.TopicCategory
import com.example.domain.util.ManoDomainLinks
import com.example.domain.util.PersianNormalizer
import com.example.ui.components.AgeSelectorRow
import com.example.ui.components.AlertSeverity
import com.example.ui.components.ArticleCard
import com.example.ui.components.ClinicalNote
import com.example.ui.components.EmptyState
import com.example.ui.components.LtrText
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SourceBadge
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DarkGold
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.util.ManoLinkShareHelper
import com.example.ui.viewmodel.ManoImageStudioViewModel

@Composable
fun TopicsScreen(
  query: String,
  selectedCategory: TopicCategory?,
  selectedAgeBand: AgeBand?,
  articles: List<Article>,
  bookmarkedSlugs: Set<String>,
  onQueryChange: (String) -> Unit,
  onCategorySelect: (TopicCategory?) -> Unit,
  onAgeSelect: (AgeBand?) -> Unit,
  onToggleBookmark: (String) -> Unit,
  onOpenArticle: (String) -> Unit,
  onNavigateSkin: () -> Unit,
  onNavigateDevices: () -> Unit,
  onNavigate3DStudio: () -> Unit = {},
  onShowFeedback: (String) -> Unit = {},
  imageStudioViewModel: ManoImageStudioViewModel = viewModel(factory = ManoImageStudioViewModel.provideFactory()),
) {
  var showStudioTab by rememberSaveable { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = !showStudioTab,
        onClick = { showStudioTab = false },
        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) },
        label = { Text("مقالات آموزشی") },
        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("topics_tab_articles"),
      )
      FilterChip(
        selected = showStudioTab,
        onClick = { showStudioTab = true },
        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
        label = { Text("استودیو و گالری کارت") },
        modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("topics_tab_studio"),
      )
    }

    if (showStudioTab) {
      TopicsCardStudioPanel(
        studioViewModel = imageStudioViewModel,
        onOpen3DStudio = onNavigate3DStudio,
        onShowFeedback = onShowFeedback,
      )
      return
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item {
          SectionCard {
            Image(
              painter = painterResource(id = R.drawable.img_topics_library_banner),
              contentDescription = "کتابخانه سلامت کودک مانو کیدز",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxWidth().height(156.dp).clip(RoundedCornerShape(16.dp)),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              com.example.ui.components.ManoPoseAvatar(pose = MascotPose.EXPLAIN, size = 62.dp)
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("موضوعات سلامت و راهنماهای جامع", style = MaterialTheme.typography.headlineSmall)
                Text(
                  text = "مقالات آموزشی پایه با ذکر رده سنی، علائم هشدار و منابع معتبر علمی (در انتظار بررسی نهایی بالینی).",
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
          label = { Text("جست‌وجو در موضوعات (مثلاً تب، یبوست، شیرخشک)") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth().testTag("topics_search_input"),
        )
      }

      if (PersianNormalizer.isHighRiskQuery(query)) {
        item {
          SeverityAlert(
            severity = AlertSeverity.DANGER,
            title = "یادآوری ایمنی در جست‌وجوی علائم پرخطر",
            body = "اگر کودک زیر ۳ ماه تب ۳۸ درجه یا بیشتر دارد، یا تنفس سخت و بی‌حالی مشاهده می‌کنید، بلافاصله به اورژانس ۱۱۵ یا پزشک مراجعه نمایید.",
            showCautionMascot = true,
          )
        }
      }

      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = selectedCategory == null,
              onClick = { onCategorySelect(null) },
              label = { Text("همه دسته‌ها") },
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(onClick = onNavigateSkin, modifier = Modifier.weight(1f).minimumInteractiveComponentSize()) {
            Text("پوست و پوشک")
          }
          OutlinedButton(onClick = onNavigateDevices, modifier = Modifier.weight(1f).minimumInteractiveComponentSize()) {
            Text("وسایل مراقبتی")
          }
          OutlinedButton(onClick = onNavigate3DStudio, modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("topics_open_3d_btn")) {
            Text("نمایش سه‌بعدی")
          }
        }
      }

      if (articles.isEmpty()) {
        item {
          EmptyState(
            title = "مقاله‌ای با این فیلتر پیدا نشد",
            subtitle = "می‌توانید فیلتر سن یا عبارت جست‌وجو را پاک کنید تا همه راهنماها نمایش داده شوند.",
            pose = MascotPose.WAIT,
            actionLabel = "پاک کردن فیلترها",
            onAction = {
              onQueryChange("")
              onCategorySelect(null)
              onAgeSelect(null)
            },
          )
        }
      } else {
        items(articles, key = { it.slug }) { article ->
          ArticleCard(
            article = article,
            isBookmarked = article.slug in bookmarkedSlugs,
            onToggleBookmark = { onToggleBookmark(article.slug) },
            onOpen = { onOpenArticle(article.slug) },
          )
        }
      }
      }
    }
  }
}

@Composable
fun TopicDetailScreen(
  article: Article?,
  relatedArticles: List<Article>,
  isBookmarked: Boolean,
  onToggleBookmark: () -> Unit,
  onBack: () -> Unit,
  onNextStepCta: (String) -> Unit,
  onOpenArticle: (String) -> Unit,
  onShowFeedback: (String) -> Unit = {},
) {
  if (article == null) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
      EmptyState(
        title = "مقاله مورد نظر یافت نشد",
        subtitle = "ممکن است نشانی تغییر کرده باشد. به فهرست موضوعات برگردید.",
        actionLabel = "بازگشت",
        onAction = onBack,
      )
    }
    return
  }

  val context = LocalContext.current
  val canonicalUrl = ManoDomainLinks.topicUrl(article.slug)

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("بازگشت")
          }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SourceBadge(reviewStatus = article.review.status)
            IconButton(
              onClick = {
                val shareBody = ManoDomainLinks.buildTopicShareText(article)
                ManoLinkShareHelper.shareTextWithLink(context, article.titleFa, shareBody)
                  .onFailure {
                    ManoLinkShareHelper.copyToClipboard(context, article.titleFa, canonicalUrl)
                    onShowFeedback("لینک راهنما در حافظه کپی شد.")
                  }
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_share_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = "اشتراک‌گذاری لینک راهنما",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            IconButton(
              onClick = onToggleBookmark,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_bookmark_button"),
            ) {
              Icon(
                imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = "نشان‌گذاری مقاله",
                tint = if (isBookmarked) manoSemanticAccent(AccentGold, DarkGold) else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(article.titleFa, style = MaterialTheme.typography.headlineMedium)
          Surface(
            color = manoCardSurface(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, manoBorderColor()),
            modifier =
              Modifier
                .fillMaxWidth()
                .clickable {
                  ManoLinkShareHelper.copyToClipboard(context, article.titleFa, canonicalUrl)
                  onShowFeedback("لینک مستقیم مقاله کپی شد.")
                }
                .minimumInteractiveComponentSize()
                .testTag("detail_domain_link_pill"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Box(modifier = Modifier.weight(1f)) {
                LtrText(text = canonicalUrl.removePrefix("https://"), color = MaterialTheme.colorScheme.primary)
              }
              Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی لینک راهنما", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      item {
        SectionCard {
          Text("پاسخ کوتاه و روشن", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          Text(article.shortAnswerFa, style = MaterialTheme.typography.bodyLarge)
          Text(
            text = "محدوده سنی مرتبط: ${article.ageRelevanceFa}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item {
        SeverityAlert(
          severity = AlertSeverity.DANGER,
          title = "چه زمانی باید سریع به پزشک یا اورژانس ۱۱۵ مراجعه کنیم؟",
          body = article.redFlagsFa.joinToString("\n• ", prefix = "• "),
          showCautionMascot = article.isHighRisk,
        )
      }

      item {
        SectionCard {
          Text("نکات مراقبت حمایتی در منزل", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          article.selfCarePointsFa.forEach { point ->
            Text("• $point", style = MaterialTheme.typography.bodyMedium)
          }
        }
      }

      item {
        SeverityAlert(
          severity = AlertSeverity.CAUTION,
          title = "اشتباهات رایجی که باید از آن‌ها پرهیز کرد",
          body = article.caregiverMistakesFa.joinToString("\n• ", prefix = "• "),
        )
      }

      item {
        Button(
          onClick = { onNextStepCta(article.nextStepRouteKey) },
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("article_next_step_cta"),
        ) {
          Text(article.nextStepCtaLabelFa)
        }
      }

      item {
        ClinicalNote(
          title = "وضعیت بررسی علمی و منابع",
          items =
            listOf(
              "وضعیت فعلی: ${article.review.status.labelFa}",
              "آخرین بازبینی: ${article.review.lastReviewedDate}",
              "یادداشت تحریریه: ${article.review.notes}",
            ),
          sources = article.sources,
          reviewStatus = article.review.status,
        )
      }

      if (relatedArticles.isNotEmpty()) {
        item {
          Text("مقالات مرتبط", style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(8.dp))
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            relatedArticles.forEach { rel ->
              ArticleCard(
                article = rel,
                isBookmarked = false,
                onToggleBookmark = {},
                onOpen = { onOpenArticle(rel.slug) },
              )
            }
          }
        }
      }
    }
  }
}
