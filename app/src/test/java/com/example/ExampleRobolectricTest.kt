package com.example

import com.example.data.ManoPreferencesRepository
import com.example.domain.model.GrowthMeasurement
import com.example.domain.util.TimelineTempPoint
import kotlinx.coroutines.runBlocking
import android.content.Context
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @org.junit.After
  fun resetGalacticDataStore() {
    kotlinx.coroutines.runBlocking {
      com.example.data.ManoPreferencesRepository(ApplicationProvider.getApplicationContext())
        .setGalacticOrbitSelection("", "")
    }
  }

  @Test
  @Config(sdk = [36], qualifiers = "fa-rIR-w411dp-h915dp-xhdpi")
  fun continueCareUsesLatestActualMeasurementAndOpensItsAction() {
    val prefs = ManoPreferencesRepository(ApplicationProvider.getApplicationContext())
    val now = System.currentTimeMillis()
    fun awaitTag(tag: String) {
      composeTestRule.waitUntil(10_000L) {
        composeTestRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
      }
    }
    runBlocking { prefs.clearLocalChildHealthData() }
    composeTestRule.onNodeWithTag("home_continue_care").assertDoesNotExist()
    try {
      runBlocking { prefs.addGrowthMeasurement(null, GrowthMeasurement(now - 120_000L, 12.5, null)) }
      awaitTag("home_continue_care")
      composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_continue_care"))
      composeTestRule.onNodeWithTag("home_continue_care").assertIsDisplayed().performClick()
      awaitTag("growth_history_row")
      composeTestRule.onNodeWithTag("growth_history_row").assertIsDisplayed()
      runBlocking { prefs.addLocalFeverLog(TimelineTempPoint("recent", 37.2f, now, "", "", "", "", false)) }
      composeTestRule.onNodeWithTag("nav_home").performClick()
      awaitTag("home_continue_care")
      composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_continue_care"))
      composeTestRule.onNodeWithTag("home_continue_care").assertIsDisplayed().performClick()
      awaitTag("timeline_temp_input")
      composeTestRule.onNodeWithTag("timeline_temp_input").assertIsDisplayed()
      composeTestRule.onNodeWithTag("nav_home").performClick()
      runBlocking { prefs.clearLocalChildHealthData() }
      composeTestRule.waitUntil(10_000L) {
        composeTestRule.onAllNodes(hasTestTag("home_continue_care")).fetchSemanticsNodes().isEmpty()
      }
    } finally {
      runBlocking { prefs.clearLocalChildHealthData() }
    }
  }

  @Test
  @Config(sdk = [36], qualifiers = "fa-rIR-w411dp-h915dp-xhdpi")
  fun colloquialSymptomSearchRequiresExplicitSelection() {
    val ui = composeTestRule
    ui.onNodeWithTag("nav_symptoms").performClick()
    val search = ui.onNodeWithTag("symptom_search")
    for ((phrase, id) in listOf("دماغش راه افتاده" to "runny_nose", "چشمش قي ميكنه" to "eye", "گوششو می‌گیره" to "ear")) {
      search.performTextReplacement(phrase)
      ui.onNodeWithTag("wizard_next_btn").assertIsNotEnabled()
      ui.onNodeWithTag("symptom_choice_" + id).assertExists()
    }
    search.performTextReplacement("ناشناخته")
    ui.onNodeWithTag("symptom_choice_ear").assertDoesNotExist()
    ui.onNodeWithTag("wizard_next_btn").assertIsNotEnabled()
    search.performTextReplacement("گوششو می‌گیره")
    ui.onNodeWithTag("symptom_choice_ear").performClick()
    ui.onNodeWithTag("wizard_next_btn").performClick()
    ui.onNodeWithText("علامت انتخاب‌شده: گوش‌درد یا ترشح گوش").assertExists()
  }

  @Test
  fun appName_matchesManoKid() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("مانو کید", context.getString(R.string.app_name))
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
  fun homeScreen_careHomeShowsBrandedActionsAndNoSyntheticCareData() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_brand_crest").assertIsDisplayed()
    composeTestRule.onNodeWithTag("care_child_profile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_menu_fever").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_ask_pharmacist").assertIsDisplayed()
    composeTestRule.onNodeWithText("من مانو هستم").assertExists()
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("care_today_card"))
    composeTestRule.onNodeWithTag("care_today_card").assertIsDisplayed()
  }

  @Test
  fun homeScreen_bottomBarHomeButtonReturnsToHomeFromEverySectionAndDeepRoute() {
    composeTestRule.waitForIdle()

    // 1. Navigate via competitive-advantage Pharmacist Hero CTA -> AskPharmacistRoute, then return via bottom nav Home
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_ask_pharmacist"))
    composeTestRule.onNodeWithTag("home_ask_pharmacist").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_hero_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("nav_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()

    // 2. Navigate into Medicines tab -> nested MedicineDetailRoute, then return via nav_home without using Back
    composeTestRule.onNodeWithTag("nav_medicines").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("medicine_card_acetaminophen-pediatric"))
    composeTestRule.onNodeWithTag("medicine_card_acetaminophen-pediatric").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("medicine_detail_list").performScrollToNode(hasTestTag("medicine_detail_warnings"))
    composeTestRule.onNodeWithTag("medicine_detail_warnings").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()

    // 3. Navigate via Selective Menu card into Symptoms wizard, then return cleanly via nav_home
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_menu_fever"))
    composeTestRule.onNodeWithTag("home_menu_fever").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("wizard_next_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
  }

  @Test
  fun journeyA_feverForNewbornAt38C_showsUrgentWarningInUi() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_symptoms").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("wizard_next_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("wizard_age_newborn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("wizard_temp_input").performScrollTo().performTextInput("38.0")
    composeTestRule.onNodeWithTag("wizard_next_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("triage_outcome_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("ارزیابی فوری پزشکی برای تب نوزاد زیر ۳ ماه").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun nonurgentSymptomJourneyPreviewsScreeningInThePharmacistForm() {
    composeTestRule.onNodeWithTag("nav_symptoms").performClick()
    composeTestRule.onNodeWithTag("wizard_next_btn").performClick()
    composeTestRule.onNodeWithTag("wizard_age_preschool_3_6y").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("wizard_temp_input").performScrollTo().performTextInput("37.2")
    composeTestRule.onNodeWithTag("wizard_duration_input").performScrollTo().performTextInput("6")
    composeTestRule.onNodeWithTag("wizard_next_btn").performClick()
    composeTestRule.onNodeWithTag("wizard_next_btn").performClick()
    composeTestRule.onNodeWithTag("symptom_navigator_list")
      .performScrollToNode(hasTestTag("pharmacist_cta_button"))
    composeTestRule.onNodeWithTag("pharmacist_cta_button").performClick()
    val measurement = "دما: 37.2 درجه، مدت: 6 ساعت"
    composeTestRule.onNodeWithTag("ask_pharmacist_list").performScrollToNode(hasText(measurement, substring = true))
    composeTestRule.onNodeWithText(measurement, substring = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("ask_pharmacist_list").performScrollToNode(hasTestTag("ask_pharmacist_submit"))
    composeTestRule.onNodeWithText("ذخیره روی این دستگاه").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun parentActionsAreVisibleOnEntryAndGrowthHistoryContainsTheActualReading() {
    composeTestRule.onNodeWithTag("nav_symptoms").performClick()
    composeTestRule.onNodeWithTag("symptom_choice_fever").assertIsDisplayed()
    composeTestRule.onNodeWithTag("companion_symptom_chip_0").assertDoesNotExist()
    composeTestRule.onNodeWithTag("symptom_toggle_companions").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("companion_symptom_chip_0").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("nav_faq").performClick()
    composeTestRule.onNodeWithTag("faq_toggle_filters").performClick()
    composeTestRule.onNodeWithText("بستن فیلترها").assertIsDisplayed()
    composeTestRule.onNodeWithTag("faq_toggle_filters").performClick()
    composeTestRule.onNodeWithTag("faq_search_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("documented_qa_assistant_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("faq_toggle_assistant").performClick()
    composeTestRule.onNodeWithTag("documented_qa_assistant_card").assertExists()
    composeTestRule.onNodeWithTag("nav_growth").performClick()
    composeTestRule.onNodeWithTag("growth_new_weight").assertIsDisplayed().performTextInput("۱۲٫۳")
    composeTestRule.onNodeWithTag("growth_save_measurement").performScrollTo().performClick()
    composeTestRule.waitUntil(10_000L) {
      composeTestRule.onAllNodes(hasText("اندازه امروز ذخیره شد.")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("growth_list").performScrollToNode(hasTestTag("growth_history_row"))
    composeTestRule.onNodeWithText("وزن: ۱۲٫۳ کیلوگرم").assertIsDisplayed()
  }

  @Test
  fun journeyF_faqSearch_findsFormulaFaqWithSpaceOrCompact() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_faq").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("faq_search_input").performTextInput("شیر خشک")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("faq_list").performScrollToNode(hasTestTag("faq_card_faq_04"))
    composeTestRule.onNodeWithTag("faq_card_faq_04").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("faq_search_input").performTextReplacement("شیرخشک")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("faq_list").performScrollToNode(hasTestTag("faq_card_faq_04"))
    composeTestRule.onNodeWithTag("faq_card_faq_04").assertIsDisplayed()
  }

  @Test
  fun journeyG_medicationKnowledgeBase_searchAndDetailShowsWarnings() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_medicines").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("medicines_search_input"))
    composeTestRule.onNodeWithTag("medicines_search_input").performTextInput("آموکسی سیلین")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("medicine_card_antibiotic-suspensions-pediatric"))
    composeTestRule.onNodeWithTag("medicine_card_antibiotic-suspensions-pediatric").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("medicine_detail_list").performScrollToNode(hasTestTag("medicine_detail_warnings"))
    composeTestRule.onNodeWithTag("medicine_detail_warnings").assertIsDisplayed()
    composeTestRule.onNodeWithTag("medicine_detail_list").performScrollToNode(hasTestTag("medicine_detail_precautions"))
    composeTestRule.onNodeWithTag("medicine_detail_precautions").assertIsDisplayed()
    composeTestRule.onNodeWithTag("medicine_detail_list").performScrollToNode(hasTestTag("medicine_detail_storage"))
    composeTestRule.onNodeWithTag("medicine_detail_storage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("medicine_detail_list").performScrollToNode(hasTestTag("locked_dose_preview"))
    composeTestRule.onNodeWithTag("locked_dose_preview").assertIsDisplayed()
  }

  @Test
  fun homeQuickSearch_opensUnifiedSearchSheetAndFindsMedication() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("topbar_search").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("global_search_input").assertIsDisplayed().performTextInput("بروفن")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("ایبوپروفن خوراکی کودکان (سوسپانسیون)").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun medicinesTabs_overlapCheckerAndCloudLogbookGateRenderCleanly() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_medicines").performClick()
    composeTestRule.waitForIdle()

    // Overlap Checker tab
    composeTestRule.onNodeWithTag("med_tab_overlap").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("overlap_checker_card"))
    composeTestRule.onNodeWithTag("overlap_checker_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("overlap_first_acetaminophen-pediatric").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("overlap_second_otc-cough-cold-caution").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("overlap_result_alert"))
    composeTestRule.onNodeWithTag("overlap_result_alert").assertIsDisplayed()
    composeTestRule.onNodeWithText("خطر مسمومیت مضاعف با استامینوفن پنهان").assertIsDisplayed()

    // Dose Logbook tab (signed-out -> shows Google Sign-In gate)
    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("medicines_mode_tabs"))
    composeTestRule.onNodeWithTag("med_tab_logbook").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("google_signin_button"))
    composeTestRule.onNodeWithTag("google_signin_button").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
  fun devicesScreen_interactiveSyringeAndThermometerLabsRespondToInteraction() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_medicines").performClick()
    composeTestRule.waitForIdle()

    // Verify new Syringe Dose tab directly inside MedicinesScreen
    composeTestRule.onNodeWithTag("med_tab_syringe").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("interactive_syringe_lab_card").assertIsDisplayed()

    // Also navigate to DevicesScreen where both Syringe & Thermometer labs live
    composeTestRule.onNodeWithTag("med_tab_kb").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("آزمایشگاه سرنگ mL و دماسنج").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("devices_list").performScrollToNode(hasTestTag("interactive_syringe_lab_card"))
    composeTestRule.onNodeWithTag("interactive_syringe_lab_card").assertIsDisplayed()

    composeTestRule.onNodeWithTag("syringe_formulation_row").performScrollTo()
    composeTestRule.onNodeWithTag("syringe_form_ibuprofen_syrup_100_5").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("syringe_age_input").performScrollTo().performTextReplacement("۱۲")
    composeTestRule.onNodeWithTag("syringe_weight_input").performScrollTo().performTextReplacement("۱۶")
    composeTestRule.onNodeWithTag("syringe_confirm_label").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("syringe_selected_ml_label").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("syringe_selected_ml_label").assertTextEquals("6.0 mL")

    // Verify newborn (<3 months) locks home dose at 0.0 mL with emergency warning
    composeTestRule.onNodeWithTag("syringe_age_input").performScrollTo().performTextReplacement("۲")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("syringe_selected_ml_label").assertDoesNotExist()
    composeTestRule.onNodeWithTag("syringe_dose_alert_banner").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("devices_list").performScrollToNode(hasTestTag("interactive_thermometer_lab_card"))
    composeTestRule.onNodeWithTag("interactive_thermometer_lab_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("thermo_age_under_3m").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("thermometer_status_banner").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
  fun homeScreen_careHomeKeepsDoseCalculatorReachableWithoutFakeDose() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_specialized_strip"))
    composeTestRule.onNodeWithTag("home_specialized_strip").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_fever_dose_quick_card"))
    composeTestRule.onNodeWithTag("home_fever_dose_quick_card").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fever_dose_calculator_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()

  }
}
