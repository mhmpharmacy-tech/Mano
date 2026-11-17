package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AgeBand
import com.example.domain.model.Article
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.domain.util.ManoDomainLinks
import com.example.ui.components.AgeSelectorRow
import com.example.ui.components.AlertSeverity
import com.example.ui.components.ArticleCard
import com.example.ui.components.CoreNavigationCard
import com.example.ui.components.GalacticRing
import com.example.ui.components.HomeFeverTimelineWidget
import com.example.ui.components.LtrText
import com.example.ui.components.ManoOrbitalHeroStage
import com.example.ui.components.ManoCharacterFamily
import com.example.ui.components.PediatricIconKind
import com.example.ui.components.PharmacistCtaCard
import com.example.ui.components.RadialTopicItem
import com.example.ui.components.SectionCard
import com.example.ui.components.SeverityAlert
import com.example.ui.components.TopicAccentRole
import com.example.ui.util.ManoLinkShareHelper
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.CoralCare
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SageCare
import com.example.ui.theme.SkyCare
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoTintedSurface

@Composable
fun HomeScreen(
  selectedAgeBand: AgeBand?,
  bookmarkedSlugs: Set<String>,
  articles: List<Article>,
  onSelectAgeBand: (AgeBand?) -> Unit,
  onToggleBookmark: (String) -> Unit,
  onNavigateSymptoms: (String) -> Unit,
  onNavigateTopics: (String?, String) -> Unit,
  onNavigateTopicDetail: (String) -> Unit,
  onNavigateMedicines: () -> Unit,
  onNavigateGrowth: () -> Unit,
  onNavigateSkin: () -> Unit,
  onNavigateDevices: () -> Unit,
  onNavigateAskPharmacist: () -> Unit,
  onNavigateMyChild: () -> Unit,
  onNavigateInfo: (String) -> Unit,
  onOpenGlobalSearch: () -> Unit = {},
  hasPlayedHeroIntro: Boolean = false,
  onHeroIntroPlayed: () -> Unit = {},
  onNavigate3DStudio: (String) -> Unit = {},
  timelineSnapshot: FeverDoseTimelineSnapshot =
    FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList()),
  onQuickSaveFever: (tempCelsius: Float, caregiverFa: String) -> Unit = { _, _ -> },
  onOpenFeverTimeline: () -> Unit = {},
  onNavigateNutrition: () -> Unit = {},
  checkedMedSafetyCount: Int = 0,
  onOpenMedicationChecklist: () -> Unit = {},
  vaccineBadgeLabelFa: String = "",
  onNavigateVaccines: () -> Unit = {},
  savedGalacticRingName: String? = null,
  savedGalacticTopicId: String? = null,
  onSaveGalacticSelection: (ringName: String, topicId: String) -> Unit = { _, _ -> },
  onNavigateSyrupScanner: () -> Unit = {},
  savedSyrupScansCount: Int = 0,
) {
  val filteredByAge =
    remember(articles, selectedAgeBand) {
      if (selectedAgeBand == null) articles.take(3)
      else articles.filter { selectedAgeBand in it.ageBands }.take(3)
    }

  val galacticInnerTopics =
    remember {
      listOf(
        RadialTopicItem("fever", "تب کودک", "تب‌سنجی و علائم هشدار", Icons.Rounded.DeviceThermostat, TopicAccentRole.CORAL, PediatricIconKind.FEVER),
        RadialTopicItem("medicines", "ایمنی دارو و سرنگ", "اندازه‌گیری mL و غلظت", Icons.Rounded.MedicalServices, TopicAccentRole.TEAL, PediatricIconKind.MEDICINE),
        RadialTopicItem("vaccines", "تقویم واکسن", "نوبت‌های شمسی و یادآور", Icons.Rounded.Vaccines, TopicAccentRole.PLUM, PediatricIconKind.MEDICINE),
        RadialTopicItem("growth", "رشد و تکامل", "نقاط عطف سنی و خواب", Icons.Rounded.ChildCare, TopicAccentRole.SAGE, PediatricIconKind.GROWTH),
        RadialTopicItem("nutrition", "مشاور تغذیه", "برنامه غذایی متناسب با سن", Icons.Rounded.Restaurant, TopicAccentRole.MANGO, PediatricIconKind.FORMULA),
        RadialTopicItem("urgent", "علائم خطر فوری", "چه زمانی به اورژانس برویم؟", Icons.Rounded.HealthAndSafety, TopicAccentRole.STRAWBERRY, PediatricIconKind.URGENT),
      )
    }

  val galacticOuterTopics =
    remember {
      listOf(
        RadialTopicItem("cough", "سرفه و خس‌خس", "تنفس و سرفه طولانی", Icons.Rounded.Air, TopicAccentRole.SKY, PediatricIconKind.COUGH),
        RadialTopicItem("scanner", "اسکنر شربت و قطره", "خواندن غلظت با دوربین هوشمند", Icons.Rounded.QrCodeScanner, TopicAccentRole.TEAL, PediatricIconKind.MEDICINE),
        RadialTopicItem("formula", "شیرخشک و تغذیه", "راهنمای علمی بدون تبلیغ", Icons.Rounded.LocalDrink, TopicAccentRole.MANGO, PediatricIconKind.FORMULA),
        RadialTopicItem("diaper", "سوختگی پوشک", "مراقبت پوست و تعویض", Icons.Rounded.Spa, TopicAccentRole.PLUM, PediatricIconKind.DIAPER),
        RadialTopicItem("diarrhea", "اسهال و استفراغ", "پیشگیری از کم‌آبی", Icons.Rounded.WaterDrop, TopicAccentRole.AQUA, PediatricIconKind.HYDRATION),
        RadialTopicItem("constipation", "یبوست و دل‌درد", "دفع و نفخ کودک", Icons.Rounded.MonitorHeart, TopicAccentRole.GOLD, PediatricIconKind.STOMACH),
        RadialTopicItem("studio3d", "استودیو سه‌بعدی", "تمرین سرنگ، تب‌سنج و توزین", Icons.Rounded.ViewInAr, TopicAccentRole.AQUA, PediatricIconKind.DEVICE),
      )
    }

  val openConcernTile: (RadialTopicItem) -> Unit = { item ->
    when (item.id) {
      "fever" -> onNavigateSymptoms("fever")
      "cough" -> onNavigateSymptoms("cough")
      "constipation" -> onNavigateSymptoms("constipation")
      "diaper" -> onNavigateSkin()
      "diarrhea" -> onNavigateSymptoms("diarrhea_vomiting")
      "medicines" -> onNavigateMedicines()
      "scanner" -> onNavigateSyrupScanner()
      "vaccines" -> onNavigateVaccines()
      "nutrition" -> onNavigateNutrition()
      "studio3d" -> onNavigate3DStudio("workbench")
      "formula" -> onNavigateTopicDetail("formula-selection-guide")
      "devices" -> onNavigateDevices()
      "growth" -> onNavigateGrowth()
      "urgent" -> onNavigateTopicDetail("when-to-see-doctor")
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("home_list"),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      // WELCOMING DUAL-RING GALACTIC ORBITAL CENTERPIECE AROUND PROMINENT MANO MASCOT
      item {
        val restoredRing =
          savedGalacticRingName?.let { name -> GalacticRing.entries.firstOrNull { it.name == name } }
        ManoOrbitalHeroStage(
          topics = galacticInnerTopics,
          outerTopics = galacticOuterTopics,
          onSelectTopic = openConcernTile,
          hasPlayedIntro = hasPlayedHeroIntro,
          onIntroPlayed = onHeroIntroPlayed,
          savedRing = restoredRing,
          savedTopicId = savedGalacticTopicId,
          onPersistSelection = { ring, topicId ->
            onSaveGalacticSelection(ring.name, topicId)
          },
        )
      }

      // UNIFIED QUICK SEARCH ENTRY BAR
      item {
        Surface(
          onClick = onOpenGlobalSearch,
          color = manoCardSurface(),
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.dp, manoBorderColor()),
          modifier =
            Modifier
              .fillMaxWidth()
              .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = com.example.ui.theme.ShadowSlate.copy(alpha = 0.10f),
                spotColor = com.example.ui.theme.ShadowSlate.copy(alpha = 0.08f),
              )
              .minimumInteractiveComponentSize()
              .testTag("home_quick_search_bar"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "جست‌وجوی سریع دارو، تب نوزاد، سرفه، شیرخشک...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // URGENT EMERGENCY & RED-FLAG ACTION CARD (High-priority above-the-scroll safety)
      item {
        SeverityAlert(
          severity = AlertSeverity.DANGER,
          title = "هشدار فوری: چه زمانی باید فوراً به اورژانس ۱۱۵ مراجعه کنیم؟",
          body =
            "تب ۳۸.۰ درجه یا بیشتر در نوزاد زیر ۳ ماه، تنفس سخت یا تند، کبودی لب‌ها، تشنج، بی‌حالی شدید یا کم‌آبی جدی نیازمند مراجعه فوری بدون انتظار است.",
          showCautionMascot = true,
          actionLabel = "بررسی علائم هشدار و اقدام فوری",
          onAction = { onNavigateTopicDetail("when-to-see-doctor") },
          modifier = Modifier.testTag("home_urgent_red_flag_card"),
        )
      }

      // THREE STACKED FULL-WIDTH CORE NAVIGATION CARDS
      item {
        val isDark = LocalManoDarkTheme.current
        val cardSurface = if (isDark) DarkSurface else PureWhite
        val triageAccent = if (isDark) DarkCoral else CoralCare
        val medAccent = if (isDark) DarkSage else SageCare
        val growthAccent = if (isDark) DarkSky else SkyCare

        Column(
          modifier = Modifier.fillMaxWidth().testTag("core_navigation_stack"),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = stringResource(R.string.core_sections_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("core_sections_header"),
          )

          CoreNavigationCard(
            title = stringResource(R.string.nav_emergency_triage_title),
            subtitle = stringResource(R.string.nav_emergency_triage_subtitle),
            badgeText = stringResource(R.string.nav_emergency_triage_badge),
            icon = Icons.Rounded.HealthAndSafety,
            iconKind = PediatricIconKind.URGENT,
            accentColor = triageAccent,
            containerColor = cardSurface,
            iconContainerColor = triageAccent.copy(alpha = 0.10f),
            onClick = { onNavigateSymptoms("fever") },
            testTag = "core_nav_emergency_triage",
          )

          CoreNavigationCard(
            title = stringResource(R.string.nav_medication_safety_title),
            subtitle = stringResource(R.string.nav_medication_safety_subtitle),
            badgeText = stringResource(R.string.nav_medication_safety_badge),
            icon = Icons.Rounded.MedicalServices,
            iconKind = PediatricIconKind.MEDICINE,
            accentColor = medAccent,
            containerColor = cardSurface,
            iconContainerColor = medAccent.copy(alpha = 0.10f),
            onClick = onNavigateMedicines,
            testTag = "core_nav_medication_safety",
          )

          CoreNavigationCard(
            title = stringResource(R.string.nav_growth_tracker_title),
            subtitle = stringResource(R.string.nav_growth_tracker_subtitle),
            badgeText = stringResource(R.string.nav_growth_tracker_badge),
            icon = Icons.Rounded.ChildCare,
            iconKind = PediatricIconKind.GROWTH,
            accentColor = growthAccent,
            containerColor = cardSurface,
            iconContainerColor = growthAccent.copy(alpha = 0.10f),
            onClick = onNavigateGrowth,
            testTag = "core_nav_growth_tracker",
          )
        }
      }

      // LIVE 24H FEVER & DOSE TIMELINE DASHBOARD WIDGET
      item {
        HomeFeverTimelineWidget(
          snapshot = timelineSnapshot,
          onQuickSaveTemp = onQuickSaveFever,
          onOpenFullTimeline = onOpenFeverTimeline,
        )
      }

      // Specialist mascot shortcuts; urgent guidance and fever timeline stay above this section.
      item {
        ManoCharacterFamily(
          onMedicines = onNavigateMedicines,
          onGrowth = onNavigateGrowth,
          onSkin = onNavigateSkin,
          onNutrition = onNavigateNutrition,
          onCare = { onNavigateSymptoms("fever") },
          onConnect = onNavigateAskPharmacist,
        )
      }

      // SMART SYRUP LABEL CAMERA SCANNER QUICK-LAUNCH CARD
      item {
        SectionCard(modifier = Modifier.testTag("home_syrup_scanner_card")) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_syrup_scanner_banner),
              contentDescription = "اسکنر هوشمند برچسب شربت و قطره با دوربین",
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(78.dp).clip(RoundedCornerShape(14.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                  shape = RoundedCornerShape(6.dp),
                ) {
                  Text(
                    text = stringResource(R.string.syrup_scanner_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
                if (savedSyrupScansCount > 0) {
                  Text(
                    text = "$savedSyrupScansCount اسکن ذخیره‌شده",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Text(
                text = stringResource(R.string.syrup_scanner_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = stringResource(R.string.syrup_scanner_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Button(
            onClick = onNavigateSyrupScanner,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("home_syrup_scanner_btn"),
          ) {
            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("اسکن برچسب دارو و تنظیم سرنگ سه‌بعدی")
          }
        }
      }

      // راهنمای مناسب سن کودک
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "راهنمای مناسب سن کودک",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onBackground,
            )
            TextButton(onClick = onNavigateMyChild, modifier = Modifier.minimumInteractiveComponentSize()) {
              Text("فرزند من")
            }
          }
          AgeSelectorRow(
            selectedAgeBand = selectedAgeBand,
            onSelect = onSelectAgeBand,
            includeAllOption = true,
          )
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredByAge.forEach { article ->
              ArticleCard(
                article = article,
                isBookmarked = article.slug in bookmarkedSlugs,
                onToggleBookmark = { onToggleBookmark(article.slug) },
                onOpen = { onNavigateTopicDetail(article.slug) },
              )
            }
          }
          OutlinedButton(
            onClick = { onNavigateTopics(null, "") },
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("see_all_topics_button"),
          ) {
            Text("مشاهده همه موضوعات سلامت")
          }
        }
      }

      // داروها و مراقبت ایمن
      item {
        SectionCard {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
              painter = painterResource(id = R.drawable.mascot_medicine),
              contentDescription = "مانو در کنار بطری دارو و دفترچه ثبت مصرف",
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(78.dp).clip(RoundedCornerShape(14.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("داروها و مراقبت ایمن", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "همیشه شکل و غلظت دارو را بررسی کنید و برای داروهای مایع فقط از سرنگ مدرج (mL) استفاده نمایید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = onNavigateMedicines,
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("home_medicines_btn"),
            ) {
              Text("داروهای کودک")
            }
            OutlinedButton(
              onClick = { onNavigateTopicDetail("safe-liquid-medicine") },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) {
              Text("آموزش سرنگ mL")
            }
          }
          FilledTonalButton(
            onClick = onOpenMedicationChecklist,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("home_med_checklist_btn"),
          ) {
            Icon(Icons.Rounded.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
              text =
                if (checkedMedSafetyCount > 0) "چک‌لیست ایمنی پیش از دارو ($checkedMedSafetyCount/8 بررسی‌شده)"
                else "چک‌لیست تعاملی ایمنی مصرف دارو",
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      // رشد و تکامل
      item {
        SectionCard {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
              painter = painterResource(id = R.drawable.mascot_growth),
              contentDescription = "مانو در محور رشد، مراقبت و آموزش کودک",
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(78.dp).clip(RoundedCornerShape(14.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("رشد، تکامل و مهارت‌های مراقبت", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "چک‌لیست نقاط عطف رشد (بدون برچسب تشخیصی)، تغذیه، خواب و استفاده ایمن از وسایل خانگی.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = onNavigateGrowth,
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("home_growth_btn"),
            ) {
              Text("رشد و تکامل")
            }
            OutlinedButton(
              onClick = onNavigateDevices,
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("home_devices_btn"),
            ) {
              Text("وسایل مراقبتی")
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = { onNavigate3DStudio("workbench") },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("home_3d_workbench_btn"),
            ) {
              Text("آزمایشگاه سه‌بعدی ابزار (3D)")
            }
            OutlinedButton(
              onClick = { onNavigate3DStudio("mano") },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("home_3d_mano_btn"),
            ) {
              Text("مانو سه‌بعدی (3D)")
            }
          }
          OutlinedButton(
            onClick = { onNavigate3DStudio("weighing") },
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("home_3d_weighing_btn"),
          ) {
            Text("راهنمای سه‌بعدی وزن‌کشی با مانکن کودک (3D)")
          }
          OutlinedButton(
            onClick = onNavigateNutrition,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("home_nutrition_btn"),
          ) {
            Text("پیشنهاد غذای سالم و مقوی کودک (هوشمند)")
          }
          FilledTonalButton(
            onClick = onNavigateVaccines,
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("home_vaccines_btn"),
          ) {
            Icon(Icons.Rounded.Vaccines, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
              text =
                if (vaccineBadgeLabelFa.isNotBlank()) "برنامه واکسیناسیون کودک ($vaccineBadgeLabelFa)"
                else "برنامه واکسیناسیون و یادآوری نوبت واکسن",
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      // ارتباط با داروساز
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("ارتباط با داروساز", style = MaterialTheme.typography.titleLarge)
          PharmacistCtaCard(onClick = onNavigateAskPharmacist)
        }
      }

      // منابع علمی، حریم خصوصی و هویت پروژه
      item {
        SectionCard {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
              painter = painterResource(id = R.drawable.family_support),
              contentDescription = "خانواده در حال گفت‌وگو و یادداشت‌برداری همراه با مانو",
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("منابع علمی، شفافیت و حریم خصوصی", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "مانو جایگزین پزشک یا تشخیص‌دهنده خودکار نیست. محتوای بالینی این راهنما دارای برچسب شفافیت علمی است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outline)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = { onNavigateInfo("sources") },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) {
              Text("منابع علمی")
            }
            OutlinedButton(
              onClick = { onNavigateInfo("privacy") },
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
            ) {
              Icon(Icons.Rounded.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("حریم خصوصی")
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val context = LocalContext.current
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "دامنه‌های متصل به اپلیکیشن (App Links):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              TextButton(
                onClick = { onNavigateInfo("domain") },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("home_domain_settings_btn"),
              ) {
                Text("تنظیمات اتصال")
              }
            }
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              ManoDomainLinks.PRIMARY_DOMAINS.forEach { domain ->
                val url = ManoDomainLinks.homeUrl(domain)
                Surface(
                  color = manoTintedSurface(AquaSurface, DarkSky, lightAlpha = 0.7f, darkAlpha = 0.16f),
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, manoBorderColor()),
                  modifier =
                    Modifier
                      .clickable {
                        ManoLinkShareHelper.openExternalBrowserUrl(context, url)
                          .onFailure { ManoLinkShareHelper.copyToClipboard(context, domain, url) }
                      }
                      .minimumInteractiveComponentSize()
                      .testTag("home_domain_chip_$domain"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    LtrText(text = domain, color = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }
          }
        }
        Spacer(Modifier.height(12.dp))
      }
    }
  }
}
