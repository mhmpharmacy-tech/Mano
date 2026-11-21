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
  fun appName_matchesManoKid() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("مانو کید", context.getString(R.string.app_name))
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
  fun homeScreen_displaysOrbitalMenuWithoutOverlapAndPharmacistHeroBelow() {
    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(600L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_brand_crest").assertIsDisplayed()
    composeTestRule.onNodeWithTag("orbital_menu_stage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_mascot_touch").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithText("دستیار هوشمند سلامت کودک و خانواده").assertIsDisplayed()

    val stageBounds = composeTestRule.onNodeWithTag("orbital_menu_stage").fetchSemanticsNode().boundsInRoot
    val mascotBounds = composeTestRule.onNodeWithTag("mano_mascot_touch").fetchSemanticsNode().boundsInRoot
    val orbitalTags =
      listOf(
        "home_menu_fever",
        "home_menu_medicines",
        "home_menu_scanner",
        "home_menu_growth",
        "home_menu_skin",
        "home_menu_qa",
      )
    val nodeBounds =
      orbitalTags.associateWith { tag ->
        composeTestRule.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
      }

    // Every orbital node must sit strictly inside stage bounds and never overlap central MANO
    nodeBounds.forEach { (tag, bounds) ->
      org.junit.Assert.assertTrue("$tag clipped left of stage", bounds.left >= stageBounds.left - 1f)
      org.junit.Assert.assertTrue("$tag clipped right of stage", bounds.right <= stageBounds.right + 1f)
      org.junit.Assert.assertTrue("$tag clipped top of stage", bounds.top >= stageBounds.top - 1f)
      org.junit.Assert.assertTrue("$tag clipped bottom of stage", bounds.bottom <= stageBounds.bottom + 1f)
      org.junit.Assert.assertFalse(
        "$tag overlapped central MANO mascot ($bounds vs $mascotBounds)",
        bounds.overlaps(mascotBounds),
      )
    }

    // No pair of orbital nodes may overlap each other
    for (i in orbitalTags.indices) {
      for (j in i + 1 until orbitalTags.size) {
        val tagA = orbitalTags[i]
        val tagB = orbitalTags[j]
        org.junit.Assert.assertFalse(
          "Orbital nodes $tagA and $tagB overlapped",
          nodeBounds.getValue(tagA).overlaps(nodeBounds.getValue(tagB)),
        )
      }
    }

    // Verify circular hexagonal symmetry around MANO
    org.junit.Assert.assertTrue(nodeBounds.getValue("home_menu_fever").bottom <= mascotBounds.top)
    org.junit.Assert.assertTrue(nodeBounds.getValue("home_menu_growth").top >= mascotBounds.bottom)
    org.junit.Assert.assertTrue(nodeBounds.getValue("home_menu_medicines").left >= mascotBounds.right)
    org.junit.Assert.assertTrue(nodeBounds.getValue("home_menu_qa").right <= mascotBounds.left)

    // Verify zero separation gap between MANO orbital stage and Pharmacist card directly beneath it
    val pharmacistBounds =
      composeTestRule.onNodeWithTag("home_pharmacist_hero_card").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    org.junit.Assert.assertTrue(
      "Expected pharmacist card docked flush beneath orbital stage with zero gap, got gap=${pharmacistBounds.top - stageBounds.bottom}",
      pharmacistBounds.top - stageBounds.bottom <= 2f,
    )
    composeTestRule.onNodeWithTag("home_pharmacist_cta").assertIsDisplayed()
  }

  @Test
  fun homeScreen_bottomBarHomeButtonReturnsToHomeFromEverySectionAndDeepRoute() {
    composeTestRule.waitForIdle()

    // 1. Navigate via competitive-advantage Pharmacist Hero CTA -> AskPharmacistRoute, then return via bottom nav Home
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_pharmacist_cta"))
    composeTestRule.onNodeWithTag("home_pharmacist_cta").performClick()
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
    composeTestRule.onNodeWithTag("nav_faq").performClick()
    composeTestRule.onNodeWithTag("faq_search_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("documented_qa_assistant_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("faq_toggle_assistant").performClick()
    composeTestRule.onNodeWithTag("documented_qa_assistant_card").assertExists()
    composeTestRule.onNodeWithTag("nav_growth").performClick()
    composeTestRule.onNodeWithTag("growth_new_weight").assertIsDisplayed().performTextInput("۱۲٫۳")
    composeTestRule.onNodeWithTag("growth_save_measurement").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("growth_list").performScrollToNode(hasText("وزن: 12.3 کیلوگرم"))
    composeTestRule.onNodeWithText("وزن: 12.3 کیلوگرم").assertIsDisplayed()
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
  fun homeScreen_specializedStripOpens3DStudioAndReturnsHomeViaBottomNav() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_specialized_strip"))
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_tool_safety"))
    composeTestRule.onNodeWithTag("home_tool_safety").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_tool_comparison"))
    composeTestRule.onNodeWithTag("home_tool_comparison").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_tool_pharmacy"))
    composeTestRule.onNodeWithTag("home_tool_pharmacy").assertIsDisplayed()

    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_tool_3d"))
    composeTestRule.onNodeWithTag("home_tool_3d").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("pediatric_3d_studio_screen").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
  }
}
