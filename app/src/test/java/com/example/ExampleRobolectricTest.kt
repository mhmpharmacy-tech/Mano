package com.example

import android.content.Context
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
  fun appName_matchesManoKids() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("مانو کیدز", context.getString(R.string.app_name))
  }

  @Test
  fun homeScreen_displaysWelcomingCardCoreNavButtonsAndRtlDirection() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("مراقبت از کودک، با خیال آسوده‌تر").assertIsDisplayed()

    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("core_nav_emergency_triage"))
    composeTestRule.onNodeWithTag("core_nav_emergency_triage").assertIsDisplayed()
    composeTestRule.onNodeWithText("تریاژ و علائم فوری").assertIsDisplayed()

    // Verify RTL layout order: leading icon is placed to the right of the trailing arrow
    val iconBounds = composeTestRule.onNodeWithTag("core_nav_emergency_triage_icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    val arrowBounds = composeTestRule.onNodeWithTag("core_nav_emergency_triage_arrow", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    org.junit.Assert.assertTrue("Expected RTL ordering with icon on the right of arrow", iconBounds.left > arrowBounds.right)

    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("core_nav_medication_safety"))
    composeTestRule.onNodeWithTag("core_nav_medication_safety").assertIsDisplayed()
    composeTestRule.onNodeWithText("ایمنی داروهای کودک").assertIsDisplayed()

    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("core_nav_growth_tracker"))
    composeTestRule.onNodeWithTag("core_nav_growth_tracker").assertIsDisplayed()
    composeTestRule.onNodeWithText("پایش رشد و تکامل").assertIsDisplayed()
  }

  @Test
  fun homeScreen_orbitalMenuAroundManoRotatesAndNavigates() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_orbital_stage").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_mascot_touch").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("orbital_active_topic").performScrollTo().assertIsDisplayed()

    // Toggle continuous auto-rotation pause/play pill
    composeTestRule.onNodeWithTag("orbital_toggle_spin").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("توقف").assertIsDisplayed()
    composeTestRule.onNodeWithTag("orbital_toggle_spin").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("چرخش").assertIsDisplayed()

    // Clicking rotate next steps cleanly and opens target section
    composeTestRule.onNodeWithTag("orbital_rotate_next").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("orbital_focus_cta").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("داروهای کودک و ایمنی مصرف").assertIsDisplayed()
  }

  @Test
  fun homeScreen_coreNavigationCardsOpenTargetSections() {
    composeTestRule.waitForIdle()

    // 1. Emergency Triage
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("core_nav_emergency_triage"))
    composeTestRule.onNodeWithTag("core_nav_emergency_triage").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("wizard_next_btn").assertIsDisplayed()

    composeTestRule.onNodeWithTag("topbar_back").performClick()
    composeTestRule.waitForIdle()

    // 2. Medication Safety
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("core_nav_medication_safety"))
    composeTestRule.onNodeWithTag("core_nav_medication_safety").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("داروهای کودک و ایمنی مصرف").assertIsDisplayed()

    composeTestRule.onNodeWithTag("topbar_back").performClick()
    composeTestRule.waitForIdle()

    // 3. Growth Tracker
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("core_nav_growth_tracker"))
    composeTestRule.onNodeWithTag("core_nav_growth_tracker").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("رشد و تکامل کودک").assertIsDisplayed()
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
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_quick_search_bar"))
    composeTestRule.onNodeWithTag("home_quick_search_bar").performClick()
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
    composeTestRule.onNodeWithTag("syringe_age_chip_12").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("syringe_weight_chip_16.0").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("syringe_selected_ml_label").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("syringe_selected_ml_label").assertTextEquals("6.0 mL")

    // Verify newborn (<3 months) locks home dose at 0.0 mL with emergency warning
    composeTestRule.onNodeWithTag("syringe_age_chip_2").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("syringe_selected_ml_label").performScrollTo().assertTextEquals("0.0 mL")
    composeTestRule.onNodeWithTag("syringe_dose_alert_banner").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("devices_list").performScrollToNode(hasTestTag("interactive_thermometer_lab_card"))
    composeTestRule.onNodeWithTag("interactive_thermometer_lab_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("thermo_age_under_3m").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("thermometer_status_banner").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
  fun homeScreen_galacticDualRingsSwitchAndNavigateToOuterSections() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("orbital_ring_switcher").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("orbital_ring_inner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("orbital_ring_outer").assertIsDisplayed()
    composeTestRule.onNodeWithTag("orbital_item_vaccines").assertIsDisplayed()
    composeTestRule.onNodeWithTag("orbital_item_studio3d").assertIsDisplayed()

    // Pause continuous motion cleanly and switch to outer galactic ring
    composeTestRule.onNodeWithTag("orbital_toggle_spin").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("orbital_ring_outer").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNode(hasText("سرفه و خس‌خس", substring = true) and hasAnyAncestor(hasTestTag("orbital_active_topic")))
      .assertIsDisplayed()

    // Step backwards (wraps cleanly to 3D Studio) and open via focus CTA
    composeTestRule.onNodeWithTag("orbital_rotate_prev").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("orbital_focus_cta").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("pediatric_3d_studio_screen").assertIsDisplayed()
  }
}
