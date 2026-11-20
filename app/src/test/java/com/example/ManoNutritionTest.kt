package com.example

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.data.ManoSourcesAndClinicalSeed
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.MilestoneItem
import com.example.domain.model.NutritionAppetiteStatus
import com.example.domain.model.NutritionPlanResult
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.NutritionPromptBuilder
import com.example.domain.util.TimelineTempPoint
import com.example.ui.screens.GrowthScreen
import com.example.ui.screens.NutritionAdvisorScreen
import com.example.ui.theme.ManoKidsTheme
import com.example.ui.viewmodel.ManoNutritionViewModel
import com.example.ui.viewmodel.NUTRITION_GEMINI_MODEL
import com.example.ui.viewmodel.NutritionAiEngine
import com.example.ui.viewmodel.NutritionUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ManoNutritionTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun promptBuilder_includesProfileAgeGrowthMilestonesAllergiesAndFeverContext() {
    val now = 1_750_000_000_000L
    val child =
      ChildProfileDemo(
        aliasFa = "لیانا",
        birthDateOrAgeFa = "۲ سال و ۳ ماه",
        ageBand = AgeBand.TODDLER_1_3Y,
        weightKgText = "12.8",
        weightDateFa = "امروز",
        allergiesFa = "تخم‌مرغ",
        currentMedsFa = "قطره آهن",
        consentGranted = true,
      )
    val milestone =
      MilestoneItem(
        id = "ms_walk",
        ageBand = AgeBand.TODDLER_1_3Y,
        domainFa = "حرکتی و تغذیه",
        descriptionFa = "با قاشق غذا می‌خورد و از لیوان آب می‌نوشد",
        watchOutFa = "جویدن تکه‌های نرم",
        source = ManoSourcesAndClinicalSeed.srcWhoGrowth,
      )
    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "tf_nut",
              tempCelsius = 38.6f,
              timestampMs = now - 900_000L,
              timeLabelFa = "12:15",
              siteFa = "زیربغلی",
              caregiverFa = "مادر",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs = emptyList(),
        nowMs = now,
      )

    val bundle =
      NutritionPromptBuilder.buildContext(
        childProfile = child,
        ageBand = child.ageBand,
        ageDetailFa = child.birthDateOrAgeFa,
        weightKgText = child.weightKgText,
        heightCmText = "88",
        allergiesFa = child.allergiesFa,
        checkedMilestones = listOf(milestone),
        timelineSnapshot = snapshot,
        appetiteStatus = NutritionAppetiteStatus.FEVER_RECOVERY,
        availableIngredientsFa = "برنج، مرغ، هویج، سیب",
      )

    assertFalse(bundle.isUnderSixMonths)
    assertFalse(bundle.isUnderTwelveMonths)
    assertEquals(12.8, bundle.normalizedWeightKg ?: 0.0, 0.01)
    assertEquals(88.0, bundle.normalizedHeightCm ?: 0.0, 0.01)
    assertTrue(bundle.childContextSummaryFa.contains("لیانا"))
    assertTrue(bundle.childContextSummaryFa.contains("12.8 kg"))
    assertTrue(bundle.childContextSummaryFa.contains("88.0 cm"))
    assertTrue(bundle.promptText.contains("تخم‌مرغ"))
    assertTrue(bundle.promptText.contains("قطره آهن"))
    assertTrue(bundle.promptText.contains("با قاشق غذا می‌خورد"))
    assertTrue(bundle.promptText.contains("38.6°C"))
    assertTrue(bundle.promptText.contains("برنج، مرغ، هویج، سیب"))
  }

  @Test
  fun promptBuilder_enforcesUnderSixMonthsExclusiveMilkRuleAndValidation() {
    val emptySnapshot =
      FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())

    val infant4m =
      NutritionPromptBuilder.buildContext(
        childProfile = null,
        ageBand = AgeBand.INFANT_3_12M,
        ageDetailFa = "۴ ماهه",
        weightKgText = "۶.۵",
        heightCmText = "۶۳",
        allergiesFa = "",
        checkedMilestones = emptyList(),
        timelineSnapshot = emptySnapshot,
        appetiteStatus = NutritionAppetiteStatus.NORMAL,
        availableIngredientsFa = "",
      )

    assertTrue(infant4m.isUnderSixMonths)
    assertTrue(infant4m.isUnderTwelveMonths)
    assertTrue("Ambiguous age in 3–12m band must be treated conservatively",
      NutritionPromptBuilder.isLikelyUnderSixMonths(AgeBand.INFANT_3_12M, AgeBand.INFANT_3_12M.labelFa))
    assertFalse(NutritionPromptBuilder.isLikelyUnderSixMonths(AgeBand.INFANT_3_12M, "۱۰ ماهه"))
    assertEquals(6.5, infant4m.normalizedWeightKg ?: 0.0, 0.01)
    assertEquals(63.0, infant4m.normalizedHeightCm ?: 0.0, 0.01)
    assertTrue(infant4m.promptText.contains("نوزاد زیر ۶ ماه فقط باید با شیر مادر یا شیرخشک استاندارد تغذیه شود"))

    assertNotNull(NutritionPromptBuilder.validateGrowthMeasurements("0.5", "60"))
    assertNotNull(NutritionPromptBuilder.validateGrowthMeasurements("10", "20"))
    assertNull(NutritionPromptBuilder.validateGrowthMeasurements("10.5", "78"))
  }

  @Test
  fun nutritionViewModel_validatesInputsParsesStructuredJsonPersistsAndSurfacesErrors() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
      var shouldFail = false
      var callCount = 0
      val vm =
        ManoNutritionViewModel(
          engine =
            object : NutritionAiEngine {
              override suspend fun generateNutritionPlanJson(prompt: String): String {
                callCount += 1
                if (shouldFail) throw IllegalStateException("AppCheck HTTP 403")
                return """
                  {
                    "planTitleFa": "برنامه تغذیه سالم ۱۰ ماهگی",
                    "overviewFa": "پیشنهاد تغذیه کمکی غنی از آهن متناسب با وزن ۹.۲ کیلوگرم.",
                    "growthNutritionFocusFa": "تمرکز بر آهن، روی و بافت له‌شده نرم",
                    "hydrationTipFa": "شیر مادر یا شیرخشک همراه با چند قاشق آب ساده پس از غذا",
                    "meals": [
                      {
                        "mealTypeFa": "ناهار",
                        "titleFa": "پوره ماهیچه، عدس قرمز و هویج",
                        "whySuitableFa": "منبع عالی آهن و پروتئین برای رشد وزنی مناسب",
                        "textureAndPortionFa": "پوره نرم با قاشق کوچک • حدود نصف پیاله",
                        "ingredientsFa": ["۳۰ گرم گوشت ماهیچه پخته", "۱ قاشق عدس قرمز", "نصف هویج کوچک"],
                        "prepStepsFa": ["مواد را کاملاً بپزید.", "با کمی آب گوشت پوره نرم تهیه کنید."]
                      }
                    ],
                    "safetyAvoidListFa": ["عسل تا پایان ۱۲ ماهگی مطلقاً ممنوع است", "نمک و شکر افزوده"],
                    "doctorFollowUpHintFa": "در ویزیت دوره‌ای قد و وزن روی نمودار رشد بررسی شود."
                  }
                """.trimIndent()
              }
            }
        )

      val snapshot =
        FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
      var savedPlan: NutritionPlanResult? = null

      // Invalid weight fails locally without calling Gemini
      vm.generateNutritionPlan(
        childProfile = null,
        ageBand = AgeBand.INFANT_3_12M,
        ageDetailFa = "۱۰ ماهه",
        weightKgText = "0.4",
        heightCmText = "72",
        allergiesFa = "",
        checkedMilestones = emptyList(),
        timelineSnapshot = snapshot,
        appetiteStatus = NutritionAppetiteStatus.NORMAL,
        availableIngredientsFa = "",
        onSavePlan = { savedPlan = it },
      )
      assertTrue(vm.uiState.value is NutritionUiState.Error)
      assertEquals(0, callCount)
      assertNull(savedPlan)

      // No AI solid-food program for unknown infant age.
      vm.generateNutritionPlan(
        childProfile = null, ageBand = AgeBand.INFANT_3_12M,
        ageDetailFa = AgeBand.INFANT_3_12M.labelFa, weightKgText = "8",
        heightCmText = "70", allergiesFa = "", checkedMilestones = emptyList(),
        timelineSnapshot = snapshot, appetiteStatus = NutritionAppetiteStatus.NORMAL,
        availableIngredientsFa = "", onSavePlan = { savedPlan = it },
      )
      assertTrue(vm.uiState.value is NutritionUiState.Error)
      assertEquals(0, callCount)

      // Known allergies require an independently reviewed plan, not generative advice.
      vm.generateNutritionPlan(
        childProfile = null, ageBand = AgeBand.INFANT_3_12M,
        ageDetailFa = "۱۰ ماهه", weightKgText = "9",
        heightCmText = "74", allergiesFa = "بادام‌زمینی", checkedMilestones = emptyList(),
        timelineSnapshot = snapshot, appetiteStatus = NutritionAppetiteStatus.NORMAL,
        availableIngredientsFa = "", onSavePlan = { savedPlan = it },
      )
      assertTrue(vm.uiState.value is NutritionUiState.Error)
      assertEquals(0, callCount)

      // Valid request parses JSON cleanly and triggers save callback
      vm.generateNutritionPlan(
        childProfile = null,
        ageBand = AgeBand.INFANT_3_12M,
        ageDetailFa = "۱۰ ماهه",
        weightKgText = "9.2",
        heightCmText = "73",
        allergiesFa = "",
        checkedMilestones = emptyList(),
        timelineSnapshot = snapshot,
        appetiteStatus = NutritionAppetiteStatus.NORMAL,
        availableIngredientsFa = "عدس، هویج",
        onSavePlan = { savedPlan = it },
      )
      advanceUntilIdle()

      assertEquals(1, callCount)
      val ready = vm.uiState.value as NutritionUiState.Ready
      assertEquals(NUTRITION_GEMINI_MODEL, ready.plan.modelName)
      assertEquals("برنامه تغذیه سالم ۱۰ ماهگی", ready.plan.planTitleFa)
      assertEquals(1, ready.plan.meals.size)
      assertEquals("پوره ماهیچه، عدس قرمز و هویج", ready.plan.meals.first().titleFa)
      assertNotNull(savedPlan)
      assertEquals(ready.plan.id, savedPlan?.id)

      // Real failure surfaces transparently
      shouldFail = true
      vm.generateNutritionPlan(
        childProfile = null,
        ageBand = AgeBand.TODDLER_1_3Y,
        ageDetailFa = "۲ ساله",
        weightKgText = "12",
        heightCmText = "85",
        allergiesFa = "",
        checkedMilestones = emptyList(),
        timelineSnapshot = snapshot,
        appetiteStatus = NutritionAppetiteStatus.NORMAL,
        availableIngredientsFa = "",
        onSavePlan = {},
      )
      advanceUntilIdle()
      val err = vm.uiState.value as NutritionUiState.Error
      assertTrue(err.messageFa.contains("AppCheck HTTP 403"))
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun nutritionAdvisorScreen_rendersProfileGrowthContextGeneratesMealsAndManagesSavedPlans() {
    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
    val child =
      ChildProfileDemo(
        aliasFa = "سامیار",
        birthDateOrAgeFa = "۹ ماهه",
        ageBand = AgeBand.INFANT_3_12M,
        weightKgText = "8.8",
        weightDateFa = "امروز",
        allergiesFa = "تخم‌مرغ",
        currentMedsFa = "ویتامین AD",
        consentGranted = true,
      )
    var deletedPlanId: String? = null

    composeTestRule.setContent {
      ManoKidsTheme {
        var savedPlans by remember { mutableStateOf(emptyList<NutritionPlanResult>()) }
        val vm = remember {
          ManoNutritionViewModel(
            engine =
              object : NutritionAiEngine {
                override suspend fun generateNutritionPlanJson(prompt: String): String =
                  """
                  {
                    "planTitleFa": "غذای مقوی ۹ ماهگی سامیار",
                    "overviewFa": "ترکیب نرم و غنی از آهن بدون تخم‌مرغ.",
                    "growthNutritionFocusFa": "افزایش انرژی و آهن قابل جذب",
                    "hydrationTipFa": "شیر مادر در فواصل وعده‌ها",
                    "meals": [
                      {
                        "mealTypeFa": "صبحانه",
                        "titleFa": "حریره بادام پوست‌گرفته و جودوسر",
                        "whySuitableFa": "مناسب سن ۹ ماهگی و فاقد تخم‌مرغ",
                        "textureAndPortionFa": "پوره یکدست • ۳ قاشق غذاخوری",
                        "ingredientsFa": ["۱ قاشق پودر بادام", "۱ قاشق جودوسر پرک"],
                        "prepStepsFa": ["مواد را با آب بجوشانید تا نرم شود."]
                      }
                    ],
                    "safetyAvoidListFa": ["عسل و سفیده یا زرده تخم‌مرغ (به دلیل حساسیت ثبت‌شده)"],
                    "doctorFollowUpHintFa": "وزن‌گیری ماهانه را با پزشک پیگیری کنید."
                  }
                  """.trimIndent()
              }
          )
        }
        val state = vm.uiState.collectAsState().value

        NutritionAdvisorScreen(
          nutritionUiState = state,
          savedPlans = savedPlans,
          childProfile = child,
          selectedAgeBand = child.ageBand,
          savedHeightCm = "71",
          checkedMilestones = emptyList(),
          timelineSnapshot = snapshot,
          onSaveHeightCm = {},
          onGeneratePlan = { band, detail, weight, height, allergies, status, ingredients ->
            vm.generateNutritionPlan(
              childProfile = child,
              ageBand = band,
              ageDetailFa = detail,
              weightKgText = weight,
              heightCmText = height,
              allergiesFa = allergies,
              checkedMilestones = emptyList(),
              timelineSnapshot = snapshot,
              appetiteStatus = status,
              availableIngredientsFa = ingredients,
              onSavePlan = { newPlan -> savedPlans = listOf(newPlan) + savedPlans },
            )
          },
          onLoadSavedPlan = { vm.loadSavedPlan(it) },
          onDeleteSavedPlan = { id ->
            deletedPlanId = id
            savedPlans = savedPlans.filterNot { it.id == id }
          },
          onOpenMyChildProfile = {},
          onOpen3DWeighingGuide = {},
          onFeedback = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nutrition_advisor_list").performScrollToNode(hasTestTag("nutrition_context_card"))
    composeTestRule.onNodeWithTag("nutrition_context_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nutrition_under12m_safety_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("nutrition_ingredients_input").performScrollTo().performTextReplacement("بادام، جودوسر")
    composeTestRule.onNodeWithTag("generate_nutrition_plan_btn").performScrollTo().performClick()

    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      runCatching {
        composeTestRule.onNodeWithTag("nutrition_advisor_list").performScrollToNode(hasTestTag("nutrition_result_summary_card"))
      }.isSuccess &&
        composeTestRule.onAllNodesWithTag("nutrition_result_summary_card").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("nutrition_result_summary_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nutrition_copy_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nutrition_share_btn").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nutrition_advisor_list").performScrollToNode(hasTestTag("nutrition_meal_card_0"))
    composeTestRule.onNodeWithTag("nutrition_meal_card_0").assertIsDisplayed()
    composeTestRule.onNodeWithText("حریره بادام پوست‌گرفته و جودوسر", substring = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("nutrition_advisor_list").performScrollToNode(hasTestTag("nutrition_safety_avoid_card"))
    composeTestRule.onNodeWithTag("nutrition_safety_avoid_card").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nutrition_advisor_list").performScrollToNode(hasTestTag("nutrition_saved_plans_section"))
    composeTestRule.onNodeWithTag("nutrition_saved_plans_section").assertIsDisplayed()
    composeTestRule.onNodeWithText("غذای مقوی ۹ ماهگی سامیار", substring = true).assertIsDisplayed()

    val deleteButtons = composeTestRule.onAllNodes(
      androidx.compose.ui.test.hasContentDescription("حذف برنامه غذایی")
    )
    deleteButtons[0].performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertNotNull(deletedPlanId)
  }

  @Test
  fun growthScreen_displaysNutritionEntryCardAndTriggersNavigation() {
    var openedNutrition = false
    composeTestRule.setContent {
      ManoKidsTheme {
        GrowthScreen(
          selectedAgeBand = AgeBand.TODDLER_1_3Y,
          milestones = emptyList(),
          checkedMilestoneIds = setOf("m1", "m2"),
          onSelectAgeBand = {},
          onToggleMilestone = {},
          onOpenGrowthArticle = {},
          childProfile =
            ChildProfileDemo(
              aliasFa = "باران",
              birthDateOrAgeFa = "۱۸ ماهه",
              ageBand = AgeBand.TODDLER_1_3Y,
              weightKgText = "11.2",
              weightDateFa = "امروز",
              allergiesFa = "ندارد",
              currentMedsFa = "ندارد",
              consentGranted = true,
            ),
          savedHeightCm = "81",
          savedNutritionPlansCount = 1,
          onOpenNutritionAdvisor = { openedNutrition = true },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("growth_list").performScrollToNode(hasTestTag("growth_nutrition_entry_card"))
    composeTestRule.onNodeWithTag("growth_nutrition_entry_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("باران", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("growth_open_nutrition_btn").performScrollTo().performClick()
    assertTrue(openedNutrition)
  }

  @Test
  fun nutritionViewModel_rapidSecondPlanCancelsFirstInFlightRequest() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
      val vm =
        ManoNutritionViewModel(
          engine =
            object : NutritionAiEngine {
              override suspend fun generateNutritionPlanJson(prompt: String): String {
                val isFirst = prompt.contains("سیب قدیمی")
                kotlinx.coroutines.delay(if (isFirst) 5_000L else 50L)
                val title = if (isFirst) "برنامه قدیمی اول" else "برنامه نهایی دوم"
                return """
                  {
                    "planTitleFa": "$title",
                    "overviewFa": "خلاصه آزمایشی",
                    "growthNutritionFocusFa": "آهن",
                    "hydrationTipFa": "آب",
                    "meals": [],
                    "safetyAvoidListFa": [],
                    "doctorFollowUpHintFa": "پزشک"
                  }
                """.trimIndent()
              }
            }
        )
      val snapshot =
        FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
      val savedTitles = mutableListOf<String>()

      vm.generateNutritionPlan(
        childProfile = null,
        ageBand = AgeBand.TODDLER_1_3Y,
        ageDetailFa = "۲ ساله",
        weightKgText = "12",
        heightCmText = "85",
        allergiesFa = "",
        checkedMilestones = emptyList(),
        timelineSnapshot = snapshot,
        appetiteStatus = NutritionAppetiteStatus.NORMAL,
        availableIngredientsFa = "سیب قدیمی",
        onSavePlan = { savedTitles.add(it.planTitleFa) },
      )
      vm.generateNutritionPlan(
        childProfile = null,
        ageBand = AgeBand.TODDLER_1_3Y,
        ageDetailFa = "۲ ساله",
        weightKgText = "12",
        heightCmText = "85",
        allergiesFa = "",
        checkedMilestones = emptyList(),
        timelineSnapshot = snapshot,
        appetiteStatus = NutritionAppetiteStatus.NORMAL,
        availableIngredientsFa = "هویج جدید",
        onSavePlan = { savedTitles.add(it.planTitleFa) },
      )
      advanceUntilIdle()

      assertEquals(listOf("برنامه نهایی دوم"), savedTitles)
      val ready = vm.uiState.value as NutritionUiState.Ready
      assertEquals("برنامه نهایی دوم", ready.plan.planTitleFa)
    } finally {
      Dispatchers.resetMain()
    }
  }
}
