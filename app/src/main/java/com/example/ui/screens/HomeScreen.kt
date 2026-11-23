package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.ManoHapticIntensity
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.PreviousCare
import com.example.domain.model.Article
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.ui.util.ManoHaptics
import com.example.ui.components.DashboardHealthSummaryState
import com.example.ui.components.GoldenOrbitalGeometry
import com.example.ui.components.Mano3DIconArtwork
import com.example.ui.components.ManoBrandCrest
import com.example.ui.components.ManoDashboardHealthSummaryCard
import com.example.ui.components.ManoEcosystemPillar
import com.example.ui.components.ManoIconMedallion
import com.example.ui.components.ManoKidWordmark
import com.example.ui.components.OrbitalMenuItem
import com.example.ui.components.PediatricIconKind
import com.example.ui.components.TopicAccentRole
import com.example.ui.components.manoFloatingGlass
import com.example.ui.components.resolveAccentColor
import com.example.ui.theme.CalmAquaSurface
import com.example.ui.theme.CleanWhite
import com.example.ui.theme.ConciergeDarkMid
import com.example.ui.theme.ConciergeDarkStart
import com.example.ui.theme.ConciergeLightMid
import com.example.ui.theme.DarkAqua
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HeroDarkMid
import com.example.ui.theme.HeroWarmCream
import com.example.ui.theme.MascotOrbDarkEdge
import com.example.ui.theme.MascotOrbDarkMid
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ParticleTealTint
import com.example.ui.theme.WarmIvory
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DeepScreen
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.DeepTealGlass
import com.example.ui.theme.KidLime
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.ShadowSlate
import com.example.ui.theme.SkyCare
import com.example.ui.theme.SkySurface
import com.example.ui.theme.SoftMintHalo
import com.example.ui.theme.SoftSkyHalo
import com.example.ui.theme.TealGlow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class SpecializedQuickLink(
  val tag: String,
  val labelFa: String,
  val icon: ImageVector,
  val iconKind: PediatricIconKind,
  val accentRole: TopicAccentRole,
  val onClick: () -> Unit,
)

private enum class ParticleDepthLayer {
  INNER_BREATH,
  ORBITAL_CORRIDOR,
  OUTER_BOKEH,
}

private data class AmbientOrbitalParticle(
  val baseAngleDeg: Float,
  val radiusFactor: Float,
  val coreRadiusDp: Float,
  val haloRadiusDp: Float,
  val baseAlphaLight: Float,
  val baseAlphaDark: Float,
  val phaseOffsetRad: Float,
  val floatAmplitudeDp: Float,
  val parallaxFactor: Float,
  val breathCoupling: Float,
  val isStarGlint: Boolean,
  val colorIndex: Int,
  val layer: ParticleDepthLayer,
)

private fun buildAmbientOrbitalParticles(): List<AmbientOrbitalParticle> {
  val goldenAngle = 137.50777f
  return buildList {
    // 6 inner sanctuary breathing motes drifting around MANO's aura
    for (i in 0 until 6) {
      add(
        AmbientOrbitalParticle(
          baseAngleDeg = (i * goldenAngle + 18f) % 360f,
          radiusFactor = 0.46f + (i % 3) * 0.055f,
          coreRadiusDp = 1.4f + (i % 2) * 0.6f,
          haloRadiusDp = 5.5f + (i % 3) * 1.8f,
          baseAlphaLight = 0.34f + (i % 2) * 0.12f,
          baseAlphaDark = 0.44f + (i % 2) * 0.14f,
          phaseOffsetRad = i * 1.05f,
          floatAmplitudeDp = 2.8f + (i % 3) * 0.9f,
          parallaxFactor = 0.16f,
          breathCoupling = 1.0f,
          isStarGlint = i % 3 == 0,
          colorIndex = i % 3,
          layer = ParticleDepthLayer.INNER_BREATH,
        )
      )
    }
    // 7 mid-corridor luminous dust & gentle starlight motes floating between MANO and the 6 care icons
    for (i in 0 until 7) {
      add(
        AmbientOrbitalParticle(
          baseAngleDeg = ((i + 6) * goldenAngle + 34f) % 360f,
          radiusFactor = 0.69f + (i % 4) * 0.07f,
          coreRadiusDp = 1.5f + (i % 3) * 0.45f,
          haloRadiusDp = 6.5f + (i % 3) * 2.2f,
          baseAlphaLight = 0.28f + (i % 2) * 0.11f,
          baseAlphaDark = 0.38f + (i % 2) * 0.13f,
          phaseOffsetRad = (i + 2) * 0.92f,
          floatAmplitudeDp = 3.4f + (i % 2) * 1.2f,
          parallaxFactor = 0.34f,
          breathCoupling = 0.55f,
          isStarGlint = i == 1 || i == 5,
          colorIndex = (i + 1) % 3,
          layer = ParticleDepthLayer.ORBITAL_CORRIDOR,
        )
      )
    }
    // 5 soft outer bokeh particles giving atmospheric depth around the orbital ring perimeter
    for (i in 0 until 5) {
      add(
        AmbientOrbitalParticle(
          baseAngleDeg = ((i + 13) * goldenAngle + 11f) % 360f,
          radiusFactor = 1.06f + (i % 3) * 0.045f,
          coreRadiusDp = 2.0f + (i % 2) * 0.8f,
          haloRadiusDp = 10.5f + (i % 3) * 2.8f,
          baseAlphaLight = 0.22f + (i % 2) * 0.08f,
          baseAlphaDark = 0.28f + (i % 2) * 0.10f,
          phaseOffsetRad = (i + 4) * 1.28f,
          floatAmplitudeDp = 4.2f + (i % 2) * 1.0f,
          parallaxFactor = -0.22f,
          breathCoupling = 0.30f,
          isStarGlint = false,
          colorIndex = (i + 2) % 3,
          layer = ParticleDepthLayer.OUTER_BOKEH,
        )
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
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
  onNavigateFeverCalculator: () -> Unit = {},
  timelineSnapshot: FeverDoseTimelineSnapshot =
    FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList()),
  onQuickSaveFever: (tempCelsius: Float, caregiverFa: String) -> Unit = { _, _ -> },
  onOpenFeverTimeline: () -> Unit = {},
  onNavigateNutrition: () -> Unit = {},
  checkedMedSafetyCount: Int = 0,
  totalMedSafetyCount: Int = 8,
  isMedSafetyCriticalReady: Boolean = false,
  onOpenMedicationChecklist: () -> Unit = {},
  vaccineBadgeLabelFa: String = "",
  vaccineAdministeredCount: Int = 0,
  vaccineTotalCoreCount: Int = 0,
  onNavigateVaccines: () -> Unit = {},
  milestonesCheckedCount: Int = 0,
  milestonesTotalCount: Int = 0,
  savedGalacticRingName: String? = null,
  savedGalacticTopicId: String? = null,
  onSaveGalacticSelection: (ringName: String, topicId: String) -> Unit = { _, _ -> },
  onNavigateSyrupScanner: () -> Unit = {},
  savedSyrupScansCount: Int = 0,
  onNavigateEcosystemPillar: (ManoEcosystemPillar) -> Unit = {},
  onCareCards: (() -> Unit)? = null,
  previousCare: PreviousCare? = null,
  onContinueCare: (PreviousCare) -> Unit = {},
  scrollToTopTrigger: Int = 0,
  hapticIntensity: ManoHapticIntensity = ManoHapticIntensity.MEDIUM,
  // Opt-in from ManoApp; legacy home remains available for regression tests.
  useCareHomeDesign: Boolean = false,
  childProfile: ChildProfileDemo? = null,
  onNavigateSupplementsHygiene: () -> Unit = {},
  onNavigateTreatmentPlan: () -> Unit = {},
  treatmentPlanMedCount: Int = 0,
) {
  if (useCareHomeDesign) {
    ManoCareHomeScreen(
      childProfile = childProfile,
      vaccineBadgeLabelFa = vaccineBadgeLabelFa,
      previousCare = previousCare,
      onContinueCare = onContinueCare,
      onOpenCareCards = onCareCards,
      onOpenChild = onNavigateMyChild,
      onCheckSymptoms = { onNavigateSymptoms("fever") },
      onAskPharmacist = onNavigateAskPharmacist,
      onMedicines = onNavigateMedicines,
      onSyrupScanner = onNavigateSyrupScanner,
      onSupplementsHygiene = onNavigateSupplementsHygiene,
      onTreatmentPlan = onNavigateTreatmentPlan,
      treatmentPlanMedCount = treatmentPlanMedCount,
      onVaccines = onNavigateVaccines,
      onGrowth = onNavigateGrowth,
      onNutrition = onNavigateNutrition,
      onFeverCalculator = onNavigateFeverCalculator,
      onOpenGlobalSearch = onOpenGlobalSearch,
      scrollToTopTrigger = scrollToTopTrigger,
    )
    return
  }
  val isDark = LocalManoDarkTheme.current
  val listState = rememberLazyListState()

  LaunchedEffect(scrollToTopTrigger) {
    if (scrollToTopTrigger > 0) {
      listState.scrollToItem(0)
    }
  }

  val orbitalPrimaryItems =
    remember(
      onNavigateSymptoms,
      onNavigateMedicines,
      onNavigateSyrupScanner,
      onNavigateGrowth,
      onNavigateSkin,
      onNavigateEcosystemPillar,
    ) {
      listOf(
        OrbitalMenuItem(
          id = "fever",
          testTag = "home_menu_fever",
          titleFa = "تب و علائم",
          contentDescriptionFa = "تب و علائم کودک، ارزیابی علائم و هشدارهای فوری",
          icon = Icons.Rounded.DeviceThermostat,
          accentRole = TopicAccentRole.CORAL,
          iconKind = PediatricIconKind.FEVER,
          onClick = { onNavigateSymptoms("fever") },
        ),
        OrbitalMenuItem(
          id = "medicines",
          testTag = "home_menu_medicines",
          titleFa = "ایمنی دارو",
          contentDescriptionFa = "ایمنی دارو و مصرف، محاسبه سرنگ و تداخل داروها",
          icon = Icons.Rounded.MedicalServices,
          accentRole = TopicAccentRole.TEAL,
          iconKind = PediatricIconKind.MEDICINE,
          onClick = onNavigateMedicines,
        ),
        OrbitalMenuItem(
          id = "scanner",
          testTag = "home_menu_scanner",
          titleFa = "اسکنر شربت",
          contentDescriptionFa = "اسکنر شربت و قطره برای تشخیص غلظت دارو با دوربین",
          icon = Icons.Rounded.QrCodeScanner,
          accentRole = TopicAccentRole.MANGO,
          iconKind = PediatricIconKind.SCANNER,
          onClick = onNavigateSyrupScanner,
        ),
        OrbitalMenuItem(
          id = "growth",
          testTag = "home_menu_growth",
          titleFa = "رشد کودک",
          contentDescriptionFa = "رشد و تغذیه کودک، نمودار WHO و نقاط عطف سنی",
          icon = Icons.Rounded.ChildCare,
          accentRole = TopicAccentRole.SKY,
          iconKind = PediatricIconKind.GROWTH,
          onClick = onNavigateGrowth,
        ),
        OrbitalMenuItem(
          id = "skin",
          testTag = "home_menu_skin",
          titleFa = "پوست و مراقبت",
          contentDescriptionFa = "پوست و مراقبت منزل، پوست نوزاد و پوشک",
          icon = Icons.Rounded.Spa,
          accentRole = TopicAccentRole.SAGE,
          iconKind = PediatricIconKind.DIAPER,
          onClick = onNavigateSkin,
        ),
        OrbitalMenuItem(
          id = "qa",
          testTag = "home_menu_qa",
          titleFa = "پرسش‌وپاسخ",
          contentDescriptionFa = "پرسش و پاسخ علمی و مستند با منابع معتبر",
          icon = Icons.AutoMirrored.Rounded.Help,
          accentRole = TopicAccentRole.PLUM,
          iconKind = PediatricIconKind.FAQ,
          onClick = { onNavigateEcosystemPillar(ManoEcosystemPillar.DOCUMENTED_QA) },
        ),
      )
    }

  val specializedQuickLinks =
    remember(
      onNavigateVaccines,
      onNavigateNutrition,
      onNavigateEcosystemPillar,
      onNavigateDevices,
    ) {
      listOf(
        SpecializedQuickLink("home_tool_safety", "هشدارهای دارویی", Icons.Rounded.Shield, PediatricIconKind.SAFETY_SHIELD, TopicAccentRole.CORAL) {
          onNavigateEcosystemPillar(ManoEcosystemPillar.MANO_SAFETY)
        },
        SpecializedQuickLink("home_tool_comparison", "مقایسه علمی محصولات", Icons.AutoMirrored.Rounded.CompareArrows, PediatricIconKind.COMPARE_LAB, TopicAccentRole.AQUA) {
          onNavigateEcosystemPillar(ManoEcosystemPillar.PRODUCT_COMPARISON)
        },
        SpecializedQuickLink("home_tool_vaccines", "تقویم واکسن", Icons.Rounded.Vaccines, PediatricIconKind.VACCINE, TopicAccentRole.SAGE, onNavigateVaccines),
        SpecializedQuickLink("home_tool_nutrition", "مشاور تغذیه", Icons.Rounded.Restaurant, PediatricIconKind.SUPPLEMENTS, TopicAccentRole.GOLD, onNavigateNutrition),
        SpecializedQuickLink("home_tool_devices", "تمرین سرنگ و دماسنج", Icons.Rounded.MedicalServices, PediatricIconKind.REMINDERS, TopicAccentRole.TEAL, onNavigateDevices),
        SpecializedQuickLink("home_tool_pharmacy", "پنل داروخانه", Icons.Rounded.LocalPharmacy, PediatricIconKind.PHARMACY, TopicAccentRole.PLUM) {
          onNavigateEcosystemPillar(ManoEcosystemPillar.MANO_PHARMACY)
        },
      )
    }

  val spacing = ManoTheme.spacing
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      state = listState,
      modifier =
        Modifier
          .fillMaxSize()
          .widthIn(max = 820.dp)
          .testTag("home_list"),
      contentPadding = PaddingValues(top = 0.dp, bottom = spacing.screenBottomClearance),
      verticalArrangement = Arrangement.spacedBy(0.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // 1. Unified Edge-to-Edge Hero: Official MANO Kid Brand Lockup + Golden-Ratio OrbitalMenu around Spring-Breathing MANO Robot
      item {
        val secondaryInk = if (isDark) DarkTextMuted else MutedSlate
        val sanctuaryAtmosphere =
          if (isDark) {
            Brush.verticalGradient(listOf(DeepScreen, HeroDarkMid, DeepScreen))
          } else {
            Brush.verticalGradient(listOf(CleanWhite, HeroWarmCream, WarmIvory))
          }

        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(sanctuaryAtmosphere)
              .testTag("home_hero_card"),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap),
            modifier =
              Modifier
                .padding(top = spacing.xs, bottom = spacing.xxs, start = spacing.screenHorizontal, end = spacing.screenHorizontal)
                .testTag("home_brand_crest"),
          ) {
            ManoBrandCrest(size = 42.dp, isDark = isDark)
            Column(verticalArrangement = Arrangement.spacedBy(spacing.textPairGap)) {
              ManoKidWordmark(isDark = isDark, fontSize = 20.sp)
              Text(
                text = "دستیار هوشمند سلامت کودک و خانواده",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = secondaryInk,
              )
            }
          }

          onCareCards?.let { open -> androidx.compose.material3.TextButton(onClick = open) { Text("ادامهٔ مراقبت داروخانه") } }
          if (previousCare != null) {
            ContinueCareRow(previousCare, onClick = { onContinueCare(previousCare) })
          }

          OrbitalMenu(
            items = orbitalPrimaryItems,
            modifier = Modifier.fillMaxWidth(),
            hapticIntensity = hapticIntensity,
          )
        }
      }

      // 2. Flagship Pharmacist Consultation Concierge Card docked flush directly under the orbit (0.dp separation)
      item {
        PharmacistConciergeDock(
          isDark = isDark,
          onNavigateAskPharmacist = onNavigateAskPharmacist,
          onNavigateFollowUp = { onNavigateEcosystemPillar(ManoEcosystemPillar.MANO_FOLLOW_UP) },
        )
      }

      // 2.5 One-Tap 2 AM Pediatric Fever Dose Calculator Entry Card
      item {
        FeverDoseQuickEntryCard(
          isDark = isDark,
          onClick = onNavigateFeverCalculator,
        )
      }

      // 3. Soft Pastel Turquoise Circular Health Summary Status Indicators
      item {
        val summaryState =
          remember(
            vaccineAdministeredCount,
            vaccineTotalCoreCount,
            vaccineBadgeLabelFa,
            milestonesCheckedCount,
            milestonesTotalCount,
            selectedAgeBand,
            checkedMedSafetyCount,
            totalMedSafetyCount,
            isMedSafetyCriticalReady,
            timelineSnapshot.latestTempCelsius,
          ) {
            DashboardHealthSummaryState(
              vaccineAdministeredCount = vaccineAdministeredCount,
              vaccineTotalCoreCount = vaccineTotalCoreCount,
              vaccineCaptionFa = vaccineBadgeLabelFa,
              milestonesCheckedCount = milestonesCheckedCount,
              milestonesTotalCount = milestonesTotalCount,
              ageBandLabelFa = selectedAgeBand?.labelFa ?: "همه رده‌های سنی",
              medSafetyCheckedCount = checkedMedSafetyCount,
              medSafetyTotalCount = totalMedSafetyCount,
              medSafetyAllCriticalReady = isMedSafetyCriticalReady,
              latestTempCelsius = timelineSnapshot.latestTempCelsius,
            )
          }
        ManoDashboardHealthSummaryCard(
          state = summaryState,
          onOpenVaccines = onNavigateVaccines,
          onOpenGrowth = onNavigateGrowth,
          onOpenMedicationSafety = onOpenMedicationChecklist,
          onOpenFeverTimeline = onOpenFeverTimeline,
        )
      }

      // 3. Compact Supplementary Care Tools neatly below the unified centerpiece
      item {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sectionGap)
              .testTag("home_specialized_strip"),
          verticalArrangement = Arrangement.spacedBy(spacing.cardItemGap),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
          ) {
            Box(
              modifier =
                Modifier
                  .width(3.5.dp)
                  .height(16.dp)
                  .clip(RoundedCornerShape(50))
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.78f))
            )
            Text(
              text = "ابزارهای تکمیلی مراقبت",
              style = ManoTheme.typeScale.sectionHeader,
              color = if (isDark) DarkTextPrimary else DarkInk,
            )
            HorizontalDivider(
              modifier = Modifier.weight(1f).padding(start = spacing.xs),
              thickness = 0.75.dp,
              color = (if (isDark) DarkBorder else HairlineBorder).copy(alpha = 0.70f),
            )
          }
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.chipGap),
            verticalArrangement = Arrangement.spacedBy(spacing.chipGap),
          ) {
            specializedQuickLinks.forEach { tool ->
              val accent = tool.accentRole.resolveAccentColor()
              val iconChipBg =
                lerp(
                  start = if (isDark) DarkSurfaceHigh else PureWhite,
                  stop = accent,
                  fraction = if (isDark) 0.16f else 0.10f,
                )
              Surface(
                onClick = tool.onClick,
                color = if (isDark) DarkSurface else PureWhite,
                shape = RoundedCornerShape(16.dp),
                border =
                  BorderStroke(
                    0.9.dp,
                    if (isDark) DarkBorder else HairlineBorder.copy(alpha = 0.85f),
                  ),
                shadowElevation = 1.dp,
                modifier = Modifier.minimumInteractiveComponentSize().testTag(tool.tag),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                      Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(iconChipBg)
                        .border(0.75.dp, accent.copy(alpha = if (isDark) 0.28f else 0.18f), RoundedCornerShape(9.dp)),
                  ) {
                    Icon(
                      imageVector = tool.icon,
                      contentDescription = null,
                      tint = accent,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                  Text(
                    text = tool.labelFa,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) DarkTextPrimary else DarkInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(spacing.xs)) }
    }
  }
}

@Composable
private fun FeverDoseQuickEntryCard(
  isDark: Boolean,
  onClick: () -> Unit,
) {
  val spacing = ManoTheme.spacing
  val borderColor =
    if (isDark) TealGlow.copy(alpha = 0.42f) else SeaGreen.copy(alpha = 0.34f)
  val cardBg =
    if (isDark) DarkSurfaceHigh else CalmAquaSurface.copy(alpha = 0.72f)
  Surface(
    onClick = onClick,
    color = cardBg,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.2.dp, borderColor),
    shadowElevation = 2.dp,
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xs)
        .minimumInteractiveComponentSize()
        .testTag("home_fever_dose_quick_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.cardPadding, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.24f else 0.14f)),
      ) {
        Icon(
          imageVector = Icons.Rounded.DeviceThermostat,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp),
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = "محاسبه سریع دوز تب‌بر کودک",
          style = ManoTheme.typeScale.cardTitle.copy(fontWeight = FontWeight.ExtraBold),
          color = if (isDark) DarkTextPrimary else DarkInk,
        )
        Text(
          text = "دوز دقیق با سرنگ، در کمتر از ۱۵ ثانیه",
          style = ManoTheme.typeScale.cardSubtitle,
          color = if (isDark) DarkTextMuted else MutedSlate,
        )
      }
      Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(12.dp),
      ) {
        Text(
          text = "شروع",
          style = ManoTheme.typeScale.actionLabel.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
      }
    }
  }
}

@Composable
private fun ContinueCareRow(care: PreviousCare, onClick: () -> Unit) {
  val spacing = ManoTheme.spacing
  val date = remember(care.recordedAtMs) { measurementTimeFa(care.recordedAtMs) }
  val summary = remember(care) {
    when (care) {
      is PreviousCare.Temperature -> "آخرین دما: ${measurementNumberFa(care.celsius)} درجه"
      is PreviousCare.Growth -> care.measurement.summaryFa()
    }
  }
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.44f),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(0.85.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xs)
        .manoFloatingGlass(intensity = 0.55f, enableSheen = true),
  ) {
    Column(modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
      .testTag("home_continue_care").padding(horizontal = spacing.cardPadding, vertical = spacing.cardPaddingCompact),
      verticalArrangement = Arrangement.spacedBy(spacing.textPairGap)) {
      Text(
        text = "ادامهٔ مراقبت قبلی",
        style = ManoTheme.typeScale.cardTitle,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = summary,
        style = ManoTheme.typeScale.clinicalBodyCompact.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "زمان ثبت: $date",
        style = ManoTheme.typeScale.microCaption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = if (care is PreviousCare.Temperature) "ثبت دمای جدید" else "دیدن سابقهٔ رشد",
        style = ManoTheme.typeScale.actionLabel,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

/**
 * Golden Ratio ($\varphi = 1.6180339$) circular [OrbitalMenu] implemented directly in [HomeScreen]:
 * - Positions the 6 primary care medallions symmetrically (`60°` spacing) along the exact Golden Section orbit
 *   where `R_orbit / (R_orbit - R_mascot) == (R_orbit - R_mascot) / R_mascot == φ`.
 * - Drives MANO's central breathing, buoyant vertical levitation, and subtle organic sway using coupled
 *   Compose **Spring** animations for a calm, weightless living presence.
 */
@Composable
fun OrbitalMenu(
  items: List<OrbitalMenuItem>,
  modifier: Modifier = Modifier,
  onMascotTap: () -> Unit = {},
  hapticIntensity: ManoHapticIntensity = ManoHapticIntensity.MEDIUM,
) {
  if (items.isEmpty()) return

  val isDark = LocalManoDarkTheme.current
  val context = LocalContext.current
  val hostView = LocalView.current
  val scope = rememberCoroutineScope()
  val mascotGreetings =
    remember {
      listOf(
        "سلام! من مانو هستم، همراه سلامت کودک شما",
        "برای چرخاندن مدار، آیکون‌ها را آرام بکشید",
        "هر سؤالی داشتید از داروساز مانو بپرسید",
      )
    }
  var greetingTapCount by remember { mutableIntStateOf(0) }
  var showGreetingBubble by remember { mutableStateOf(false) }

  LaunchedEffect(greetingTapCount) {
    if (greetingTapCount > 0) {
      showGreetingBubble = true
      delay(2600L)
      showGreetingBubble = false
    }
  }

  // Smooth spring radial blossom when entering the Golden Ratio orbit
  val entranceProgress = remember { Animatable(0.25f) }
  LaunchedEffect(Unit) {
    entranceProgress.animateTo(
      targetValue = 1f,
      animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
    )
  }

  // Coupled organic Spring Animation cycle for MANO's natural breathing, buoyant floating & subtle sway
  var isInhalePhase by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay(900L)
    while (isActive) {
      isInhalePhase = !isInhalePhase
      delay(2400L)
    }
  }

  val mascotBreathScale by
    animateFloatAsState(
      targetValue = if (isInhalePhase) 1.048f else 0.986f,
      animationSpec = spring(dampingRatio = 0.60f, stiffness = 48f, visibilityThreshold = 0.002f),
      label = "manoSpringBreathScale",
    )
  val mascotFloatY by
    animateDpAsState(
      targetValue = if (isInhalePhase) (-4.8).dp else 3.4.dp,
      animationSpec = spring(dampingRatio = 0.56f, stiffness = 44f, visibilityThreshold = 0.1.dp),
      label = "manoSpringBuoyantFloatY",
    )
  val mascotGentleTiltDeg by
    animateFloatAsState(
      targetValue = if (isInhalePhase) 1.4f else -1.2f,
      animationSpec = spring(dampingRatio = 0.62f, stiffness = 46f, visibilityThreshold = 0.05f),
      label = "manoSpringGentleTilt",
    )
  val breathLuminescence = ((mascotBreathScale - 0.986f) / (1.048f - 0.986f)).coerceIn(0f, 1f)

  val mascotTapScale = remember { Animatable(1f) }
  // Interactive 360° circular drag rotation offset with smooth Spring 60° harmonic snap on release
  val userDragRotationDeg = remember { Animatable(0f) }
  val infiniteTransition = rememberInfiniteTransition(label = "orbitalRingPhase")
  val orbitalDriftPhaseRad by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 4800, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "orbitalDriftPhaseRad",
    )
  // Smooth continuous circular orbit revolution around MANO (360° every 44s)
  val ambientOrbitRotationDeg by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 44000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "ambientOrbitRotationDeg",
    )
  // Serene ambient particle buoyancy & shimmer cycle
  val particlePhaseRad by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 11000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "ambientParticlePhaseRad",
    )
  val totalRotationDeg = ambientOrbitRotationDeg + userDragRotationDeg.value

  val ambientParticles = remember { buildAmbientOrbitalParticles() }
  val resolvedAccents = items.map { it.accentRole.resolveAccentColor() }
  val density = LocalDensity.current

  BoxWithConstraints(
    modifier = modifier.fillMaxWidth().testTag("orbital_menu_stage"),
    contentAlignment = Alignment.Center,
  ) {
    val maxNodeFloatDp = 2.2f
    val maxMascotFloatDp = 5.0f
    val metrics =
      remember(maxWidth, maxNodeFloatDp, maxMascotFloatDp) {
        GoldenOrbitalGeometry.calculate(
          availableWidthDp = maxWidth.value,
          maxNodeFloatDp = maxNodeFloatDp,
          maxMascotFloatDp = maxMascotFloatDp,
        )
      }

    // Explicit LTR polar frame prevents RTL mirroring of absoluteOffset(x, y)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
      val centerPxX = with(density) { (metrics.stageWidthDp / 2f).dp.toPx() }
      val centerPxY = with(density) { (metrics.stageHeightDp / 2f).dp.toPx() }
      Box(
        modifier =
          Modifier
            .width(metrics.stageWidthDp.dp)
            .height(metrics.stageHeightDp.dp)
            .pointerInput(centerPxX, centerPxY) {
              detectDragGestures(
                onDragEnd = {
                  scope.launch {
                    val snapped = (userDragRotationDeg.value / 60f).roundToInt() * 60f
                    userDragRotationDeg.animateTo(
                      targetValue = snapped,
                      animationSpec = spring(dampingRatio = 0.60f, stiffness = 170f),
                    )
                  }
                },
                onDrag = { change, dragAmount ->
                  val curr = change.position
                  val prev = Offset(curr.x - dragAmount.x, curr.y - dragAmount.y)
                  val prevAngle = atan2(prev.y - centerPxY, prev.x - centerPxX)
                  val currAngle = atan2(curr.y - centerPxY, curr.x - centerPxX)
                  var deltaDeg = Math.toDegrees((currAngle - prevAngle).toDouble()).toFloat()
                  if (deltaDeg > 180f) deltaDeg -= 360f
                  if (deltaDeg < -180f) deltaDeg += 360f
                  if (deltaDeg != 0f) {
                    change.consume()
                    scope.launch {
                      userDragRotationDeg.snapTo(userDragRotationDeg.value + deltaDeg)
                    }
                  }
                },
              )
            },
      ) {
        val centerX = metrics.stageWidthDp / 2f
        val centerY = metrics.stageHeightDp / 2f
        val angleStepDeg = 360.0 / items.size

        // 1. Golden Ratio Concentric Constellation Canvas & Spring-Coupled Breathing Aura
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width / 2f
          val cy = size.height / 2f
          val entranceScale = 0.76f + 0.24f * entranceProgress.value
          val orbitRadiusPx = metrics.orbitRadiusDp.dp.toPx() * entranceScale
          val goldenInnerPx = metrics.goldenInnerRingRadiusDp.dp.toPx() * entranceScale
          val outerHaloPx = metrics.outerHaloRingRadiusDp.dp.toPx() * entranceScale
          val mascotRadiusPx = metrics.mascotRadiusDp.dp.toPx() * mascotBreathScale
          val mascotCenterY = cy + mascotFloatY.toPx()

          // Full-bleed atmospheric halo expanding gently with MANO's spring breath
          val auraRadius = orbitRadiusPx * (1.24f + 0.06f * breathLuminescence)
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  if (isDark) {
                    listOf(
                      TealGlow.copy(alpha = 0.22f + 0.09f * breathLuminescence),
                      SkyCare.copy(alpha = 0.11f + 0.04f * breathLuminescence),
                      Color.Transparent,
                    )
                  } else {
                    listOf(
                      SoftMintHalo.copy(alpha = 0.86f + 0.11f * breathLuminescence),
                      SoftSkyHalo.copy(alpha = 0.40f + 0.08f * breathLuminescence),
                      Color.Transparent,
                    )
                  },
                center = Offset(cx, mascotCenterY),
                radius = auraRadius,
              ),
            radius = auraRadius,
            center = Offset(cx, mascotCenterY),
          )

          // Multi-depth floating ambient particles synchronized with MANO's breathing & subtle drag parallax
          val particlePalette =
            if (isDark) {
              listOf(TealGlow, DarkAqua, SkyCare)
            } else {
              listOf(SeaGreen, SkyCare, ParticleTealTint)
            }
          ambientParticles.forEach { particle ->
            val twinkle = 0.68f + 0.32f * sin(particlePhaseRad.toDouble() * 2.0 + particle.phaseOffsetRad).toFloat()
            val breathExpandPx = (4.2f * particle.breathCoupling * breathLuminescence).dp.toPx()
            val radialPx = orbitRadiusPx * particle.radiusFactor + breathExpandPx
            val angleDeg = particle.baseAngleDeg + totalRotationDeg * particle.parallaxFactor
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val floatY = sin(particlePhaseRad.toDouble() + particle.phaseOffsetRad).toFloat() * particle.floatAmplitudeDp.dp.toPx()
            val floatX = cos(particlePhaseRad.toDouble() * 0.5 + particle.phaseOffsetRad).toFloat() * (particle.floatAmplitudeDp * 0.42f).dp.toPx()
            val anchorY = if (particle.layer == ParticleDepthLayer.INNER_BREATH) mascotCenterY else cy
            val px = cx + radialPx * cos(angleRad).toFloat() + floatX
            val py = anchorY + radialPx * sin(angleRad).toFloat() + floatY

            // Skip drawing if outside canvas bounds
            if (px in 4f..(size.width - 4f) && py in 4f..(size.height - 4f)) {
              val tint = particlePalette[particle.colorIndex % particlePalette.size]
              val baseAlpha = if (isDark) particle.baseAlphaDark else particle.baseAlphaLight
              val alpha = (baseAlpha * twinkle * (0.82f + 0.28f * breathLuminescence * particle.breathCoupling)).coerceIn(0.06f, 0.72f)
              val haloR = particle.haloRadiusDp.dp.toPx() * (0.90f + 0.18f * breathLuminescence)
              val coreR = particle.coreRadiusDp.dp.toPx() * (0.92f + 0.14f * twinkle)

              // Diffused atmospheric bokeh halo
              drawCircle(
                brush =
                  Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = alpha * 0.55f), Color.Transparent),
                    center = Offset(px, py),
                    radius = haloR,
                  ),
                radius = haloR,
                center = Offset(px, py),
              )

              // Crisp luminous core mote
              drawCircle(
                color = if (isDark) PureWhite.copy(alpha = alpha * 0.85f) else tint.copy(alpha = alpha),
                radius = coreR,
                center = Offset(px, py),
              )

              // Delicate 4-point starlight micro-glint on select breathing & corridor motes
              if (particle.isStarGlint) {
                val glintLen = coreR * (2.6f + 1.0f * breathLuminescence)
                val glintColor = if (isDark) PureWhite.copy(alpha = alpha * 0.70f) else tint.copy(alpha = alpha * 0.75f)
                drawLine(
                  color = glintColor,
                  start = Offset(px - glintLen, py),
                  end = Offset(px + glintLen, py),
                  strokeWidth = 0.85.dp.toPx(),
                  cap = StrokeCap.Round,
                )
                drawLine(
                  color = glintColor,
                  start = Offset(px, py - glintLen),
                  end = Offset(px, py + glintLen),
                  strokeWidth = 0.85.dp.toPx(),
                  cap = StrokeCap.Round,
                )
              }
            }
          }

          // Outer Golden Ratio whisper-dashed halo ring: R * (1 + 1/φ⁵)
          drawCircle(
            color = (if (isDark) DarkAqua else SeaGreen).copy(alpha = if (isDark) 0.18f else 0.14f),
            radius = outerHaloPx,
            center = Offset(cx, cy),
            style =
              Stroke(
                width = 0.9.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 13f), phase = orbitalDriftPhaseRad * 5f + totalRotationDeg * 0.4f),
              ),
          )

          // Primary luminous Golden Ratio orbital track (R_orbit)
          drawCircle(
            brush =
              Brush.sweepGradient(
                colors =
                  listOf(
                    SeaGreen.copy(alpha = if (isDark) 0.48f else 0.34f),
                    SkyCare.copy(alpha = if (isDark) 0.44f else 0.30f),
                    TealGlow.copy(alpha = if (isDark) 0.52f else 0.38f),
                    SeaGreen.copy(alpha = if (isDark) 0.48f else 0.34f),
                  ),
                center = Offset(cx, cy),
              ),
            radius = orbitRadiusPx,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx()),
          )

          // Inner Golden Section harmonic circle: R_orbit / φ (≈ 0.618 * R)
          drawCircle(
            color = (if (isDark) TealGlow else SeaGreen).copy(alpha = (if (isDark) 0.16f else 0.13f) + 0.06f * breathLuminescence),
            radius = goldenInnerPx,
            center = Offset(cx, cy),
            style = Stroke(width = 0.95.dp.toPx()),
          )

          // Spring-coupled breathing ring gliding seamlessly with MANO
          drawCircle(
            color = (if (isDark) TealGlow else SeaGreen).copy(alpha = (if (isDark) 0.24f else 0.18f) + 0.14f * breathLuminescence),
            radius = mascotRadiusPx + (6.5f + 2.8f * breathLuminescence).dp.toPx(),
            center = Offset(cx, mascotCenterY),
            style = Stroke(width = 1.15.dp.toPx()),
          )

          // Rotating radial Golden filaments, anchor glow pools & midpoint compass pearls
          items.indices.forEach { index ->
            val angleRad = Math.toRadians(-90.0 + index * angleStepDeg + totalRotationDeg)
            val accent = resolvedAccents[index]
            val endR = orbitRadiusPx - (metrics.medallionDiameterDp.dp.toPx() * 0.48f)
            if (endR > goldenInnerPx + 3.dp.toPx()) {
              val sx = cx + goldenInnerPx * cos(angleRad).toFloat()
              val sy = cy + goldenInnerPx * sin(angleRad).toFloat()
              val ex = cx + endR * cos(angleRad).toFloat()
              val ey = cy + endR * sin(angleRad).toFloat()
              drawLine(
                brush = Brush.linearGradient(
                  colors = listOf(accent.copy(alpha = 0.04f), accent.copy(alpha = if (isDark) 0.26f else 0.18f)),
                  start = Offset(sx, sy),
                  end = Offset(ex, ey),
                ),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = 1.0.dp.toPx(),
                cap = StrokeCap.Round,
              )
            }

            val anchorX = cx + orbitRadiusPx * cos(angleRad).toFloat()
            val anchorY = cy + orbitRadiusPx * sin(angleRad).toFloat()
            drawCircle(
              brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = if (isDark) 0.14f else 0.08f), Color.Transparent),
                center = Offset(anchorX, anchorY),
                radius = 28.dp.toPx(),
              ),
              radius = 28.dp.toPx(),
              center = Offset(anchorX, anchorY),
            )

            val midAngleRad = Math.toRadians(-90.0 + (index + 0.5) * angleStepDeg + totalRotationDeg)
            val dotX = cx + orbitRadiusPx * cos(midAngleRad).toFloat()
            val dotY = cy + orbitRadiusPx * sin(midAngleRad).toFloat()
            drawCircle(
              color = (if (isDark) TealGlow else SeaGreen).copy(alpha = if (isDark) 0.18f else 0.14f),
              radius = 5.dp.toPx(),
              center = Offset(dotX, dotY),
            )
            drawCircle(
              color = (if (isDark) DarkAqua else SeaGreen).copy(alpha = if (isDark) 0.65f else 0.44f),
              radius = 2.1.dp.toPx(),
              center = Offset(dotX, dotY),
            )
          }
        }

        // 2. Central MANO Mascot Hub (new 3D MANO portrait) with natural Spring Breathing, Buoyancy & Subtle Sway
        val mascotLeft = (centerX - metrics.mascotRadiusDp).dp
        val mascotTop = (centerY - metrics.mascotRadiusDp).dp + mascotFloatY
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .absoluteOffset(x = mascotLeft, y = mascotTop)
              .size(metrics.mascotDiameterDp.dp)
              .zIndex(2f)
              .scale(mascotBreathScale * mascotTapScale.value)
              .rotate(mascotGentleTiltDeg)
              .shadow(
                elevation = (12f + 5f * breathLuminescence).dp,
                shape = CircleShape,
                ambientColor = SeaGreen.copy(alpha = if (isDark) 0.42f else 0.28f),
                spotColor = SkyCare.copy(alpha = if (isDark) 0.36f else 0.24f),
              )
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  listOf(
                    PureWhite,
                    if (isDark) MascotOrbDarkMid else CalmAquaSurface.copy(alpha = 0.88f),
                    if (isDark) MascotOrbDarkEdge else SkySurface.copy(alpha = 0.84f),
                  )
                )
              )
              .border(
                width = 3.dp,
                brush = Brush.linearGradient(
                  listOf(
                    PureWhite.copy(alpha = if (isDark) 0.55f else 0.92f),
                    SeaGreen.copy(alpha = 0.84f),
                    TealGlow.copy(alpha = 0.88f),
                    SkyCare.copy(alpha = 0.78f),
                  )
                ),
                shape = CircleShape,
              )
              .clickable(onClickLabel = "احوالپرسی با مانو", role = Role.Button) {
                ManoHaptics.perform(
                  context = context,
                  view = hostView,
                  intensity = hapticIntensity,
                  isPrimaryInteraction = true,
                )
                greetingTapCount++
                onMascotTap()
                scope.launch {
                  mascotTapScale.animateTo(1.09f, spring(dampingRatio = 0.40f, stiffness = 360f))
                  mascotTapScale.animateTo(1.0f, spring(dampingRatio = 0.52f, stiffness = 260f))
                }
              }
              .minimumInteractiveComponentSize()
              .semantics { contentDescription = "شخصیت مهربان مانو کید در مرکز مدار، برای لبخند زدن ضربه بزنید" }
              .testTag("mano_mascot_touch"),
        ) {
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(3.dp)
                .clip(CircleShape)
                .border(1.2.dp, PureWhite.copy(alpha = if (isDark) 0.36f else 0.84f), CircleShape),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_mano_orbital_mascot),
              contentDescription = "مانو کید، ربات مهربان و هوشمند سلامت کودک و خانواده با حلقه درخشان فیروزه‌ای",
              contentScale = ContentScale.Crop,
              alignment = Alignment.Center,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }

        // 3. 6 Symmetric Direct-Tap Care Medallions rotating smoothly along the Golden Ratio Orbit
        val activeRadius = metrics.orbitRadiusDp * (0.82f + 0.18f * entranceProgress.value)
        val nodeAlpha = (0.72f + 0.28f * entranceProgress.value).coerceIn(0f, 1f)

        items.forEachIndexed { index, item ->
          val angleRad = Math.toRadians(-90.0 + index * angleStepDeg + totalRotationDeg)
          val floatOffsetY = (sin(orbitalDriftPhaseRad.toDouble() + index * (PI / 3.0)) * maxNodeFloatDp).toFloat()
          val nodeLeft = (centerX + activeRadius * cos(angleRad).toFloat() - metrics.nodeWidthDp / 2f).dp
          val nodeTop = (centerY + activeRadius * sin(angleRad).toFloat() + floatOffsetY - metrics.nodeHeightDp / 2f).dp

          OrbitalCareNode(
            item = item,
            accentColor = resolvedAccents[index],
            isDark = isDark,
            nodeWidth = metrics.nodeWidthDp.dp,
            nodeHeight = metrics.nodeHeightDp.dp,
            medallionSize = metrics.medallionDiameterDp.dp,
            modifier =
              Modifier
                .absoluteOffset(x = nodeLeft, y = nodeTop)
                .alpha(nodeAlpha)
                .zIndex(3f),
          )
        }
      }
    }

    AnimatedVisibility(
      visible = showGreetingBubble,
      enter = fadeIn(tween(220)),
      exit = fadeOut(tween(300)),
      modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp).zIndex(4f),
    ) {
      Surface(
        color = if (isDark) DarkSurfaceHigh.copy(alpha = 0.94f) else PureWhite.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TealGlow.copy(alpha = 0.55f)),
        shadowElevation = 6.dp,
        modifier = Modifier.testTag("mano_mascot_greeting_bubble"),
      ) {
        Text(
          text = mascotGreetings[(greetingTapCount - 1).coerceAtLeast(0) % mascotGreetings.size],
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = if (isDark) DarkTextPrimary else DarkInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
    }
  }
}

@Composable
private fun OrbitalCareNode(
  item: OrbitalMenuItem,
  accentColor: Color,
  isDark: Boolean,
  nodeWidth: Dp,
  nodeHeight: Dp,
  medallionSize: Dp,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.92f else 1f,
      animationSpec = spring(dampingRatio = 0.50f, stiffness = 460f),
      label = "orbitalNodePressSpring",
    )
  val iconSize = (medallionSize.value * 0.44f).coerceIn(22f, 26f).dp
  val orbSurface =
    lerp(
      start = if (isDark) DarkSurfaceHigh else PureWhite,
      stop = accentColor,
      fraction = if (isDark) 0.15f else 0.07f,
    )
  val orbBorderColor = accentColor.copy(alpha = if (isDark) 0.34f else 0.22f)
  val innerHaloColor = accentColor.copy(alpha = if (isDark) 0.12f else 0.07f)
  val labelPillBg = if (isDark) DarkSurface.copy(alpha = 0.84f) else PureWhite.copy(alpha = 0.90f)
  val labelPillBorder = accentColor.copy(alpha = if (isDark) 0.24f else 0.16f)

  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
      modifier =
        modifier
          .width(nodeWidth)
          .height(nodeHeight)
          .scale(pressScale)
          .clip(RoundedCornerShape(20.dp))
          .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(bounded = true, color = accentColor),
            onClickLabel = item.titleFa,
            role = Role.Button,
            onClick = {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              item.onClick()
            },
          )
          .minimumInteractiveComponentSize()
          .semantics { contentDescription = item.contentDescriptionFa }
          .testTag(item.testTag),
    ) {
      Surface(
        color = orbSurface,
        shape = CircleShape,
        border = BorderStroke(1.dp, orbBorderColor),
        modifier =
          Modifier
            .size(medallionSize)
            .shadow(
              elevation = if (isDark) 5.dp else 4.dp,
              shape = CircleShape,
              ambientColor = accentColor.copy(alpha = if (isDark) 0.20f else 0.10f),
              spotColor = ShadowSlate.copy(alpha = if (isDark) 0.18f else 0.08f),
            ),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxSize(),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(medallionSize * 0.72f)
                .clip(CircleShape)
                .background(innerHaloColor),
          ) {
            Icon(
              imageVector = item.icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(iconSize),
            )
          }
        }
      }

      Text(
        text = item.titleFa,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = if (isDark) DarkTextPrimary else DarkInk,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier =
          Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(labelPillBg)
            .border(0.6.dp, labelPillBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PharmacistConciergeDock(
  isDark: Boolean,
  onNavigateAskPharmacist: () -> Unit,
  onNavigateFollowUp: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val spacing = ManoTheme.spacing
  val dockBg = if (isDark) DarkSurface.copy(alpha = 0.92f) else Color(0xFFF2FAF8)
  val dockBorder =
    if (isDark) DarkBorder.copy(alpha = 0.90f)
    else scheme.primary.copy(alpha = 0.16f)

  Surface(
    color = dockBg,
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(0.95.dp, dockBorder),
    shadowElevation = if (isDark) 2.5.dp else 2.dp,
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, top = 0.dp, bottom = spacing.xs)
        .manoFloatingGlass(intensity = 0.72f, enableSheen = true)
        .testTag("home_pharmacist_hero_card"),
  ) {
    Column(
      modifier = Modifier.padding(spacing.cardPadding),
      verticalArrangement = Arrangement.spacedBy(spacing.cardItemGap),
    ) {
      Surface(
        color = scheme.primary.copy(alpha = if (isDark) 0.16f else 0.10f),
        shape = RoundedCornerShape(50),
      ) {
        Text(
          text = "مشاوره تخصصی و مستند",
          style = ManoTheme.typeScale.sectionKicker,
          color = scheme.primary,
          modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs),
        )
      }
      Column(verticalArrangement = Arrangement.spacedBy(spacing.textPairGap)) {
        Text(
          text = "داروساز کودک",
          style = ManoTheme.typeScale.sectionHeader,
          color = scheme.onSurface,
        )
        Text(
          text = "دربارهٔ مصرف دارو بپرسید یا پاسخ پرسش قبلی را ببینید.",
          style = ManoTheme.typeScale.clinicalBodyCompact,
          color = scheme.onSurfaceVariant,
        )
      }
      androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.chipGap),
        verticalArrangement = Arrangement.spacedBy(spacing.chipGap),
      ) {
        Button(
          onClick = onNavigateAskPharmacist,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("home_pharmacist_cta"),
        ) {
          Text(
            text = "پرسش از داروساز",
            style = ManoTheme.typeScale.actionLabel,
          )
        }
        androidx.compose.material3.TextButton(
          onClick = onNavigateFollowUp,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("home_followup_chip"),
        ) {
          Text(
            text = "پیگیری پاسخ",
            style = ManoTheme.typeScale.actionLabel,
          )
        }
      }
    }
  }
}
