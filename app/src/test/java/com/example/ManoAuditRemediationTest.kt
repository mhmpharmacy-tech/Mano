package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.ManoPreferencesRepository
import com.example.data.ManoThemeMode
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.LiquidFormulationId
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineTempPoint
import com.example.ui.components.ManoOrbitalHeroStage
import com.example.ui.components.ManoSquadSection
import com.example.ui.components.PediatricIconKind
import com.example.ui.components.RadialTopicItem
import com.example.ui.components.TopicAccentRole
import com.example.ui.theme.ManoKidsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManoAuditRemediationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @After
  fun cleanupPreferences() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ManoPreferencesRepository(context)
    runBlocking {
      repo.setThemeMode(ManoThemeMode.SYSTEM)
      repo.setLocalChildProfile(null)
      repo.clearLocalFeverLogs()
      repo.clearLocalDoseLogs()
    }
  }

  @Test
  fun localHealthErasure_clearsChildDataAndWidget_butRetainsDisplayPreferences() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ManoPreferencesRepository(context)
    prefs.setThemeMode(ManoThemeMode.DARK)
    val profile = ChildProfileDemo(
      aliasFa = "کودک تست",
      birthDateOrAgeFa = "۸ ماهه",
      ageBand = AgeBand.INFANT_3_12M,
      weightKgText = "8",
      weightDateFa = "امروز",
      allergiesFa = "",
      currentMedsFa = "",
      consentGranted = true,
      ageMonthsExact = 8,
    )
    prefs.setLocalChildProfile(profile)
    prefs.setChildBirthDateEpochMs(1_750_000_000_000L)
    prefs.setChildHeightCm("70")
    val log = TimelineDosePoint(
      id = "private_dose",
      medicationSlug = "acetaminophen",
      medicationNameFa = "استامینوفن",
      dosageFormFa = "شربت",
      volumeMlText = "2 mL",
      timestampMs = 1_750_000_000_000L,
      givenAtLabelFa = "۱۴:۰۰",
      noteFa = "",
      isSyncedCloud = false,
    )
    prefs.addLocalDoseLog(log)
    val temp = TimelineTempPoint(
      id = "private_fever",
      tempCelsius = 38.1f,
      timestampMs = 1_750_000_000_000L,
      timeLabelFa = "۱۴:۰۰",
      siteFa = "زیربغلی",
      caregiverFa = "مادر",
      noteFa = "",
      isSyncedCloud = false,
    )
    prefs.addLocalFeverLog(temp)
    val snapshot = com.example.domain.util.FeverDoseTimelineEngine.buildSnapshot(
      emptyList(), listOf(temp), emptyList(), listOf(log), nowMs = 1_750_000_000_000L,
    )
    com.example.widget.FeverTimelineWidgetStore.saveAndUpdateWidgets(context, snapshot)
    assertTrue(com.example.widget.FeverTimelineWidgetStore.readSummary(context).second.contains("استامینوفن"))
    prefs.clearLocalChildHealthData()
    com.example.widget.FeverTimelineWidgetStore.clearStoredSummary(context)
    assertNull(prefs.localChildProfile.first())
    assertNull(prefs.childBirthDateEpochMs.first())
    assertEquals("", prefs.childHeightCm.first())
    assertTrue(prefs.localDoseLogs.first().isEmpty())
    assertTrue(prefs.localFeverLogs.first().isEmpty())
    assertTrue(prefs.savedVisitDrafts.first().isEmpty())
    assertTrue(prefs.savedNutritionPlans.first().isEmpty())
    assertTrue(prefs.savedSyrupScans.first().isEmpty())
    assertEquals(ManoThemeMode.DARK, prefs.themeMode.first())
    assertTrue(com.example.widget.FeverTimelineWidgetStore.readSummary(context).second.contains("ثبت نشده"))
  }

  @Test
  fun childDeletion_uiRequiresConfirmationAndSeparatesCloudFromLocalErasure() {
    val profile = ChildProfileDemo(
      aliasFa = "کودک",
      birthDateOrAgeFa = "۱۸ ماهه",
      ageBand = AgeBand.TODDLER_1_3Y,
      weightKgText = "10",
      weightDateFa = "امروز",
      allergiesFa = "",
      currentMedsFa = "",
      consentGranted = true,
      ageMonthsExact = 18,
    )
    var profileDeleted = 0
    var localErased = 0
    composeTestRule.setContent {
      ManoKidsTheme {
        com.example.ui.screens.MyChildScreen(
          profile = profile,
          exportedJson = null,
          onSaveProfile = { _, _, _, _, _, _, _, _ -> },
          onExportSummary = {},
          onDeleteProfile = { profileDeleted++ },
          onEraseLocalData = { localErased++ },
        )
      }
    }
    composeTestRule.onNodeWithTag("my_child_list").performScrollToNode(hasTestTag("delete_child_btn"))
    composeTestRule.onNodeWithTag("delete_child_btn").performClick()
    assertEquals(0, profileDeleted)
    composeTestRule.onNodeWithTag("confirm_profile_delete_btn").performClick()
    assertEquals(1, profileDeleted)
    assertEquals(0, localErased)
    composeTestRule.onNodeWithTag("my_child_list").performScrollToNode(hasTestTag("erase_local_health_data_btn"))
    composeTestRule.onNodeWithTag("erase_local_health_data_btn").performClick()
    assertEquals(0, localErased)
    composeTestRule.onNodeWithTag("confirm_local_health_erase_btn").performClick()
    assertEquals(1, localErased)
    assertEquals(1, profileDeleted)
  }

  @Test
  fun conservativeInfantFallback_blocksIbuprofenWhenExactAgeUnknown_andUnlocksAtSevenMonths() {
    val unspecificInfant =
      ChildProfileDemo(
        aliasFa = "نی‌نی",
        birthDateOrAgeFa = AgeBand.INFANT_3_12M.labelFa,
        ageBand = AgeBand.INFANT_3_12M,
        weightKgText = "7.5",
        weightDateFa = "امروز",
        allergiesFa = "ندارد",
        currentMedsFa = "ندارد",
        consentGranted = true,
      )

    val conservativeMonths = LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(unspecificInfant)
    assertNull(conservativeMonths)

    val conservativeIbuprofen =
      LiquidDoseVisualizerEngine.evaluate(
        formulation = LiquidFormulationId.IBUPROFEN_SYRUP_100_5,
        ageMonths = conservativeMonths,
        weightKg = 7.5f,
      )
    assertTrue("Ibuprofen must stay blocked when INFANT_3_12M lacks exact >=6m age", conservativeIbuprofen.isBlocked)

    val sevenMonthsTextProfile = unspecificInfant.copy(birthDateOrAgeFa = "۷ ماه")
    val parsedMonths = LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(sevenMonthsTextProfile)
    assertEquals(7, parsedMonths)

    val nowMs = 1_750_000_000_000L
    val sevenMonthsAgoMs = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = nowMs; add(java.util.Calendar.MONTH, -7) }.timeInMillis
    val fromBirthDate =
      LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(
        profile = unspecificInfant,
        birthDateEpochMs = sevenMonthsAgoMs,
        nowMs = nowMs,
      )
    assertEquals(7, fromBirthDate)

    val safeIbuprofen =
      LiquidDoseVisualizerEngine.evaluate(
        formulation = LiquidFormulationId.IBUPROFEN_SYRUP_100_5,
        ageMonths = fromBirthDate,
        weightKg = 8.0f,
      )
    assertFalse("Ibuprofen should be unlocked once exact age >= 6 months", safeIbuprofen.isBlocked)
  }

  @Test
  fun preferencesRepository_persistsThemeChildProfileTimelineAndMilestones() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ManoPreferencesRepository(context)

    repo.setThemeMode(ManoThemeMode.DARK)
    assertEquals(ManoThemeMode.DARK, repo.themeMode.first())

    val profile =
      ChildProfileDemo(
        aliasFa = "آوا",
        birthDateOrAgeFa = "۱۸ ماه",
        ageBand = AgeBand.TODDLER_1_3Y,
        weightKgText = "11.0",
        weightDateFa = "امروز",
        allergiesFa = "ندارد",
        currentMedsFa = "ندارد",
        consentGranted = true,
        ageMonthsExact = 18,
      )
    repo.setLocalChildProfile(profile)
    assertEquals(profile, repo.localChildProfile.first())

    val fever =
      TimelineTempPoint(
        id = "f1",
        tempCelsius = 38.4f,
        timestampMs = 1_750_000_000_000L,
        timeLabelFa = "۱۴:۳۰",
        siteFa = "زیربغلی",
        caregiverFa = "مادر",
        noteFa = "اندازه‌گیری دقیق",
        isSyncedCloud = false,
      )
    val dose =
      TimelineDosePoint(
        id = "d1",
        medicationSlug = "acetaminophen-pediatric",
        medicationNameFa = "استامینوفن",
        dosageFormFa = "شربت ۱۲۰/۵",
        volumeMlText = "5.0 mL",
        timestampMs = 1_750_000_300_000L,
        givenAtLabelFa = "۱۴:۳۵",
        noteFa = "با سرنگ",
        isSyncedCloud = false,
      )
    repo.addLocalFeverLog(fever)
    repo.addLocalDoseLog(dose)
    assertEquals(listOf(fever), repo.localFeverLogs.first())
    assertEquals(listOf(dose), repo.localDoseLogs.first())

    repo.toggleMilestoneCheck("ms_walk_15m")
    assertTrue("ms_walk_15m" in repo.checkedMilestoneIds.first())
    repo.toggleMilestoneCheck("ms_walk_15m")
    assertFalse("ms_walk_15m" in repo.checkedMilestoneIds.first())

    repo.setLocalChildProfile(null)
    repo.clearLocalFeverLogs()
    repo.clearLocalDoseLogs()
    assertNull(repo.localChildProfile.first())
    assertTrue(repo.localFeverLogs.first().isEmpty())
    assertTrue(repo.localDoseLogs.first().isEmpty())
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h780dp-xhdpi")
  fun orbitalHeroStage_narrowPhone_noSatelliteClipsOutsideStageBounds() {
    val innerTopics =
      listOf(
        RadialTopicItem("fever", "تب کودک", "تب‌سنجی", Icons.Rounded.DeviceThermostat, TopicAccentRole.CORAL, PediatricIconKind.FEVER),
        RadialTopicItem("medicines", "ایمنی دارو", "سرنگ mL", Icons.Rounded.MedicalServices, TopicAccentRole.TEAL, PediatricIconKind.MEDICINE),
        RadialTopicItem("vaccines", "تقویم واکسن", "یادآور", Icons.Rounded.Vaccines, TopicAccentRole.PLUM, PediatricIconKind.MEDICINE),
        RadialTopicItem("growth", "رشد", "نقاط عطف", Icons.Rounded.ChildCare, TopicAccentRole.SAGE, PediatricIconKind.GROWTH),
        RadialTopicItem("nutrition", "تغذیه", "غذای کودک", Icons.Rounded.Restaurant, TopicAccentRole.MANGO, PediatricIconKind.FORMULA),
        RadialTopicItem("urgent", "اورژانس", "علائم خطر", Icons.Rounded.HealthAndSafety, TopicAccentRole.STRAWBERRY, PediatricIconKind.URGENT),
      )
    val outerTopics =
      listOf(
        RadialTopicItem("cough", "سرفه", "تنفس", Icons.Rounded.Air, TopicAccentRole.SKY, PediatricIconKind.COUGH),
        RadialTopicItem("scanner", "اسکنر شربت", "دوربین هوشمند", Icons.Rounded.QrCodeScanner, TopicAccentRole.TEAL, PediatricIconKind.MEDICINE),
        RadialTopicItem("formula", "شیرخشک", "راهنما", Icons.Rounded.LocalDrink, TopicAccentRole.MANGO, PediatricIconKind.FORMULA),
        RadialTopicItem("diaper", "پوشک", "پوست", Icons.Rounded.Spa, TopicAccentRole.PLUM, PediatricIconKind.DIAPER),
        RadialTopicItem("studio3d", "سه‌بعدی", "سرنگ 3D", Icons.Rounded.ViewInAr, TopicAccentRole.AQUA, PediatricIconKind.DEVICE),
      )

    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = false) {
        ManoOrbitalHeroStage(
          topics = innerTopics,
          outerTopics = outerTopics,
          onSelectTopic = {},
          modifier = Modifier.padding(16.dp),
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("orbital_toggle_spin").performClick()
    composeTestRule.waitForIdle()

    val stageBounds = composeTestRule.onNodeWithTag("home_orbital_stage").fetchSemanticsNode().boundsInRoot
    val allIds = (innerTopics + outerTopics).map { it.id }
    allIds.forEach { id ->
      val itemNode = composeTestRule.onNodeWithTag("orbital_item_$id").assertIsDisplayed()
      val bounds = itemNode.fetchSemanticsNode().boundsInRoot
      assertTrue(
        "Satellite $id left=${bounds.left} clipped left of stage=${stageBounds.left}",
        bounds.left >= stageBounds.left - 1f,
      )
      assertTrue(
        "Satellite $id right=${bounds.right} clipped right of stage=${stageBounds.right}",
        bounds.right <= stageBounds.right + 1f,
      )
      assertTrue(
        "Satellite $id top=${bounds.top} clipped above stage=${stageBounds.top}",
        bounds.top >= stageBounds.top - 1f,
      )
      assertTrue(
        "Satellite $id bottom=${bounds.bottom} clipped below stage=${stageBounds.bottom}",
        bounds.bottom <= stageBounds.bottom + 1f,
      )
    }
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h780dp-xhdpi", fontScale = 2.0f)
  fun orbitalAndSquad_maxFontScale_narrowPhone_renderCleanlyWithoutOverflowCrash() {
    val innerTopics =
      listOf(
        RadialTopicItem("fever", "تب کودک", "تب‌سنجی و علائم هشدار", Icons.Rounded.DeviceThermostat, TopicAccentRole.CORAL, PediatricIconKind.FEVER),
        RadialTopicItem("medicines", "ایمنی دارو", "سرنگ mL", Icons.Rounded.MedicalServices, TopicAccentRole.TEAL, PediatricIconKind.MEDICINE),
        RadialTopicItem("growth", "رشد و تکامل", "نقاط عطف", Icons.Rounded.ChildCare, TopicAccentRole.SAGE, PediatricIconKind.GROWTH),
        RadialTopicItem("urgent", "علائم خطر", "اورژانس ۱۱۵", Icons.Rounded.HealthAndSafety, TopicAccentRole.STRAWBERRY, PediatricIconKind.URGENT),
      )

    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = true) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
          ManoOrbitalHeroStage(
            topics = innerTopics,
            outerTopics = emptyList(),
            onSelectTopic = {},
          )
          ManoSquadSection(onSelectMember = {})
        }
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("mano_mascot_touch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("orbital_focus_cta").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_squad_drug").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_squad_connect").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun safetyTriageAiViewModel_generatesPediatricianChecklistAndDocumentedQa_andRendersProductComparison() {
    val fakeEngine =
      object : com.example.ui.viewmodel.SafetyTriageAiEngine {
        override suspend fun generateSafetyChecklistJson(prompt: String): String =
          """
          {
            "checklistTitleFa": "چک‌لیست مراجعه به متخصص کودکان برای تب و بی‌قراری",
            "urgencySummaryFa": "در کودک نوپا با تب و بی‌قراری، پایش علائم تنفسی و آب بدن در اولویت است.",
            "immediateEmergencySignsFa": ["تب در نوزاد زیر ۳ ماه", "تنگی نفس یا کبودی لب‌ها"],
            "sameDayPediatricianVisitSignsFa": ["تداوم تب بیش از ۷۲ ساعت", "کاهش واضح دفعات ادرار"],
            "homeMonitoringChecklistFa": ["ثبت دمای بدن هر ۴ ساعت", "استفاده از سرنگ مدرج mL"],
            "questionsForPediatricianFa": ["چه علائمی نیاز به مراجعه مجدد فوری دارد؟"],
            "clinicalSourceCitationFa": "AAP / WHO Pediatric Fever Guidelines"
          }
          """.trimIndent()

        override suspend fun generateDocumentedQaJson(prompt: String): String =
          """
          {
            "directAnswerFa": "تفاوت اصلی قطره و شربت استامینوفن در غلظت میلی‌گرم بر میلی‌لیتر است.",
            "actionableStepsFa": ["غلظت روی جعبه را بررسی کنید", "فقط از سرنگ مدرج mL استفاده کنید"],
            "whenToVisitPediatricianFa": ["در صورت شک به دوز اشتباه یا بی‌حالی کودک"],
            "citedSourcesFa": ["AAP — Safe Liquid Medication Dosing"],
            "safetyDisclaimerFa": "این پاسخ آموزشی و غیرتشخیصی است."
          }
          """.trimIndent()
      }

    val vm = com.example.ui.viewmodel.ManoSafetyTriageAiViewModel(fakeEngine)
    val feverFlow = com.example.data.ManoSourcesAndClinicalSeed.symptomFlows.first { it.id == "fever" }

    vm.generatePediatricianSafetyChecklist(
      primaryFlow = feverFlow,
      coSelectedSymptomsFa = listOf("بی‌قراری یا گریه مداوم"),
      wizard = com.example.ui.viewmodel.SymptomWizardState(selectedSymptomId = "fever", temperatureText = "38.6"),
      childProfile = null,
    )
    composeTestRule.waitForIdle()

    val checklistReady = vm.checklistState.value as com.example.ui.viewmodel.SafetyChecklistUiState.Ready
    assertEquals("چک‌لیست مراجعه به متخصص کودکان برای تب و بی‌قراری", checklistReady.payload.checklistTitleFa)
    assertEquals(2, checklistReady.payload.immediateEmergencySignsFa.size)

    vm.toggleChecklistItem("er_0")
    val afterToggle = vm.checklistState.value as com.example.ui.viewmodel.SafetyChecklistUiState.Ready
    assertTrue("er_0" in afterToggle.checkedItems)

    vm.askDocumentedQuestion(
      questionFa = "تفاوت قطره و شربت استامینوفن چیست؟",
      ageBand = AgeBand.TODDLER_1_3Y,
      relatedFaqs = com.example.data.ManoFaqSeed.faqs.take(2),
    )
    composeTestRule.waitForIdle()

    val qaReady = vm.qaState.value as com.example.ui.viewmodel.DocumentedQaUiState.Ready
    assertTrue(qaReady.payload.directAnswerFa.contains("غلظت"))
    assertFalse("Generated source must not masquerade as reviewed", qaReady.payload.citedSourcesFa.contains("AAP — Safe Liquid Medication Dosing"))

    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = false) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
          com.example.ui.components.ManoSpecializedEcosystemSection(onSelectPillar = {})
          com.example.ui.components.ScientificHealthProductComparisonSection()
        }
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mano_specialized_ecosystem_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_pillar_documented_qa").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_pillar_mano_safety").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_pillar_product_comparison").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_pillar_mano_connect").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_pillar_mano_followup").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_pillar_mano_pharmacy").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("scientific_product_comparison_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("comparison_cat_chip_antipyretic_forms").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("comparison_option_card_antipyretic_forms_0").performScrollTo().assertIsDisplayed()
  }
  @Test
  fun ecosystemPillars_haveDistinctBrandedIconKinds() {
    val pillars = com.example.ui.components.ManoEcosystemPillar.entries
    assertEquals(6, pillars.size)
    assertEquals("Every pillar should have a visually distinct branded symbol", 6, pillars.map { it.iconKind }.distinct().size)
    assertEquals(
      PediatricIconKind.FAMILY,
      com.example.ui.components.ManoEcosystemPillar.MANO_CONNECT.iconKind,
    )
  }

  @Test
  fun growthChart_withoutChildMeasurements_doesNotSuggestSyntheticPercentile() {
    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = false) {
        com.example.ui.components.WhoGrowthChartCard(
          childProfile = null,
          savedHeightCm = "",
          selectedAgeBand = null,
          onOpen3DWeighingGuide = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithTag("who_percentile_result").assertDoesNotExist()
  }


  @Test
  fun allSixEcosystemCards_dispatchTheCorrectClickCallback() {
    val clicked = mutableListOf<com.example.ui.components.ManoEcosystemPillar>()
    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = false) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
          com.example.ui.components.ManoSpecializedEcosystemSection(
            onSelectPillar = { clicked.add(it) },
          )
        }
      }
    }

    com.example.ui.components.ManoEcosystemPillar.entries.forEach { pillar ->
      composeTestRule
        .onNodeWithTag("mano_pillar_${pillar.id}")
        .performScrollTo()
        .performClick()
    }
    assertEquals(com.example.ui.components.ManoEcosystemPillar.entries.toList(), clicked)
  }


  @Test
  fun review20261120_scienceAndFormsRemediation_verified() {
    val constipationFlow = com.example.data.ManoSourcesAndClinicalSeed.symptomFlows.first { it.id == "constipation" }
    assertTrue(
      "Constipation flow must cite pediatric constipation source instead of fever source",
      constipationFlow.sources.any { it.id == "aap_constipation" },
    )
    assertFalse(constipationFlow.sources.any { it.id == "aap_fever" })

    val meconiumTriage =
      com.example.domain.util.TriageEngine.evaluate(
        flow = constipationFlow,
        ageBand = AgeBand.NEWBORN,
        selectedRedFlagIds = setOf("infant_no_meconium"),
        temperatureCelsius = null,
        durationHours = null,
      )
    assertEquals(com.example.domain.model.TriageOutcome.URGENT_EMERGENCY, meconiumTriage.outcome)
    assertFalse(meconiumTriage.canShowSelfCare)

    assertTrue(
      com.example.data.ManoSourcesAndClinicalSeed.demoPharmacistCases.all {
        it.reviewStatus == com.example.domain.model.ReviewStatus.APPROVED
      }
    )

    val context = ApplicationProvider.getApplicationContext<Context>()
    val vm = com.example.ui.viewmodel.ManoViewModel(context.applicationContext as android.app.Application)

    assertNull(
      "Out-of-range weight must be rejected",
      vm.resolveValidatedChildProfile("آوا", "۱۴۰۳/۰۵/۱۲", AgeBand.TODDLER_1_3Y, "95", "", "", "", true),
    )
    val valid =
      vm.resolveValidatedChildProfile(
        aliasFa = "آوا",
        birthDateOrAgeFa = "۱۴۰۳/۰۵/۱۲",
        ageBand = AgeBand.TODDLER_1_3Y,
        weightKgText = "۱۱٫۵",
        weightDateFa = "امروز",
        allergiesFa = "",
        currentMedsFa = "",
        consentGranted = true,
      )
    assertTrue("Valid Jalali birth date and Persian decimal weight must resolve", valid != null)
    assertEquals("11.5", valid!!.first.weightKgText)
    assertTrue("Birth date epoch must sync for vaccine schedule", valid.second != null && valid.second!! > 0L)
    assertTrue("Exact age in months must be derived from birth date", valid.first.ageMonthsExact != null)
  }
}
