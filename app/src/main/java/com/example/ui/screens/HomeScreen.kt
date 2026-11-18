package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AgeBand
import com.example.domain.model.Article
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.ui.components.GalacticRing
import com.example.ui.components.ManoEcosystemPillar
import com.example.ui.components.ManoOrbitalHeroStage
import com.example.ui.components.PediatricIconKind
import com.example.ui.components.RadialTopicItem
import com.example.ui.components.TopicAccentRole

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
  onNavigateEcosystemPillar: (ManoEcosystemPillar) -> Unit = {},
) {
  val galacticInnerTopics =
    remember {
      listOf(
        RadialTopicItem(
          id = "fever",
          titleFa = "تب کودک",
          subtitleFa = "تب‌سنجی و علائم هشدار",
          icon = Icons.Rounded.DeviceThermostat,
          accentRole = TopicAccentRole.CORAL,
          iconKind = PediatricIconKind.FEVER,
        ),
        RadialTopicItem(
          id = "medicines",
          titleFa = "ایمنی دارو و سرنگ",
          subtitleFa = "اندازه‌گیری mL و غلظت",
          icon = Icons.Rounded.MedicalServices,
          accentRole = TopicAccentRole.TEAL,
          iconKind = PediatricIconKind.MEDICINE,
        ),
        RadialTopicItem(
          id = "mano_safety",
          titleFa = "بررسی تداخل دارو",
          subtitleFa = "MANO Safety • هشدار دارویی",
          icon = Icons.Rounded.Shield,
          accentRole = TopicAccentRole.CORAL,
          iconKind = PediatricIconKind.MEDICINE,
        ),
        RadialTopicItem(
          id = "mano_connect",
          titleFa = "ارتباط با داروساز",
          subtitleFa = "MANO Connect • مشاوره امن",
          icon = Icons.Rounded.SupportAgent,
          accentRole = TopicAccentRole.SAGE,
          iconKind = PediatricIconKind.PHARMACY,
        ),
        RadialTopicItem(
          id = "documented_qa",
          titleFa = "پرسش‌وپاسخ مستند",
          subtitleFa = "دستیار علمی با منبع معتبر",
          icon = Icons.AutoMirrored.Rounded.Help,
          accentRole = TopicAccentRole.PLUM,
          iconKind = PediatricIconKind.FAQ,
        ),
        RadialTopicItem(
          id = "product_comparison",
          titleFa = "مقایسه محصولات",
          subtitleFa = "مقایسه علمی محصولات سلامت",
          icon = Icons.Rounded.CompareArrows,
          accentRole = TopicAccentRole.MANGO,
          iconKind = PediatricIconKind.FORMULA,
        ),
        RadialTopicItem(
          id = "growth",
          titleFa = "رشد و تکامل",
          subtitleFa = "نقاط عطف سنی و خواب",
          icon = Icons.Rounded.ChildCare,
          accentRole = TopicAccentRole.SAGE,
          iconKind = PediatricIconKind.GROWTH,
        ),
        RadialTopicItem(
          id = "urgent",
          titleFa = "علائم خطر فوری",
          subtitleFa = "چه زمانی به اورژانس برویم؟",
          icon = Icons.Rounded.HealthAndSafety,
          accentRole = TopicAccentRole.STRAWBERRY,
          iconKind = PediatricIconKind.URGENT,
        ),
      )
    }

  val galacticOuterTopics =
    remember {
      listOf(
        RadialTopicItem(
          id = "cough",
          titleFa = "سرفه و خس‌خس",
          subtitleFa = "تنفس و سرفه طولانی",
          icon = Icons.Rounded.Air,
          accentRole = TopicAccentRole.SKY,
          iconKind = PediatricIconKind.COUGH,
        ),
        RadialTopicItem(
          id = "mano_followup",
          titleFa = "پیگیری مشاوره",
          subtitleFa = "MANO Follow-up • نتیجه",
          icon = Icons.Rounded.FactCheck,
          accentRole = TopicAccentRole.MANGO,
          iconKind = PediatricIconKind.PHARMACY,
        ),
        RadialTopicItem(
          id = "mano_pharmacy",
          titleFa = "پنل داروخانه",
          subtitleFa = "MANO Pharmacy • میز داروساز",
          icon = Icons.Rounded.LocalPharmacy,
          accentRole = TopicAccentRole.TEAL,
          iconKind = PediatricIconKind.PHARMACY,
        ),
        RadialTopicItem(
          id = "scanner",
          titleFa = "اسکنر شربت و قطره",
          subtitleFa = "خواندن غلظت با دوربین هوشمند",
          icon = Icons.Rounded.QrCodeScanner,
          accentRole = TopicAccentRole.TEAL,
          iconKind = PediatricIconKind.MEDICINE,
        ),
        RadialTopicItem(
          id = "vaccines",
          titleFa = "تقویم واکسن",
          subtitleFa = "نوبت‌های شمسی و یادآور",
          icon = Icons.Rounded.Vaccines,
          accentRole = TopicAccentRole.PLUM,
          iconKind = PediatricIconKind.MEDICINE,
        ),
        RadialTopicItem(
          id = "nutrition",
          titleFa = "مشاور تغذیه",
          subtitleFa = "برنامه غذایی و شیرخشک",
          icon = Icons.Rounded.Restaurant,
          accentRole = TopicAccentRole.MANGO,
          iconKind = PediatricIconKind.FORMULA,
        ),
        RadialTopicItem(
          id = "diaper",
          titleFa = "پوست و پوشک",
          subtitleFa = "مراقبت پوست و تعویض",
          icon = Icons.Rounded.Spa,
          accentRole = TopicAccentRole.PLUM,
          iconKind = PediatricIconKind.DIAPER,
        ),
        RadialTopicItem(
          id = "studio3d",
          titleFa = "استودیو سه‌بعدی",
          subtitleFa = "تمرین سرنگ، تب‌سنج و توزین",
          icon = Icons.Rounded.ViewInAr,
          accentRole = TopicAccentRole.AQUA,
          iconKind = PediatricIconKind.DEVICE,
        ),
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
      "mano_safety" -> onNavigateEcosystemPillar(ManoEcosystemPillar.MANO_SAFETY)
      "mano_connect" -> onNavigateEcosystemPillar(ManoEcosystemPillar.MANO_CONNECT)
      "documented_qa" -> onNavigateEcosystemPillar(ManoEcosystemPillar.DOCUMENTED_QA)
      "product_comparison" -> onNavigateEcosystemPillar(ManoEcosystemPillar.PRODUCT_COMPARISON)
      "mano_followup" -> onNavigateEcosystemPillar(ManoEcosystemPillar.MANO_FOLLOW_UP)
      "mano_pharmacy" -> onNavigateEcosystemPillar(ManoEcosystemPillar.MANO_PHARMACY)
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

  // Full-screen stage focused exclusively on MANO and the Dual-Ring Galactic Orbit
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    androidx.compose.foundation.lazy.LazyColumn(
      modifier =
        Modifier
          .fillMaxSize()
          .widthIn(max = 840.dp)
          .padding(horizontal = 14.dp, vertical = 6.dp)
          .testTag("home_list"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
    }
  }
}
