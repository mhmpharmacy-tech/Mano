package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.example.domain.model.PediatricTreatmentPlan
import com.example.domain.model.TreatmentPlanFormatter
import com.example.domain.model.defaultPediatricTreatmentMedications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class ManoTreatmentPlanTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun defaultTreatmentPlan_enforcesClinicalSafetyUpgradesAndDailyReset() {
    val meds = defaultPediatricTreatmentMedications()
    assertEquals(3, meds.size)

    // 1. Azithromycin 200/5: accurate 5-day 10 mg/kg day-1 (4 cc) + 5 mg/kg days 2-5 (2 cc)
    val azithro = meds.first { it.id == "rx_azithromycin_200" }
    assertTrue(azithro.doseAmountFa.contains("۴ سی‌سی"))
    assertTrue(azithro.doseAmountFa.contains("۲ سی‌سی"))
    assertTrue(azithro.counselingNoteFa.contains("۱۰ روز"))

    // 2. Acetaminophen: upgraded to 160 mg/5 mL pediatric syrup (7.5 cc = 240 mg), NOT 50 drops
    val acetaminophen = meds.first { it.id == "rx_acetaminophen_160" }
    assertTrue(acetaminophen.nameWithStrengthFa.contains("۱۶۰"))
    assertTrue(acetaminophen.doseAmountFa.contains("۷٫۵ سی‌سی"))
    assertFalse("Unsafe 50-drop regimen must not appear for 4yo/16kg", acetaminophen.doseAmountFa.contains("۵۰ قطره"))
    assertTrue(acetaminophen.counselingNoteFa.contains("حداکثر ۴ نوبت"))

    // 3. Saline Nasal Spray: age-appropriate 4yo counseling (no infant feeding label)
    val saline = meds.first { it.id == "rx_saline_spray_09" }
    assertFalse("Infant milk label must be removed for 4-year-old", saline.foodRelationFa.contains("شیر"))
    assertTrue(saline.foodRelationFa.contains("خواب"))

    // 4. Daily check-off reset across calendar day rollover
    val yesterdayMeds =
      meds.map { med ->
        if (med.id == "rx_azithromycin_200") med.copy(completedSlotTimesFa = mapOf(1 to "۱۲:۰۰")) else med
      }
    val yesterdayPlan =
      PediatricTreatmentPlan(
        medications = yesterdayMeds,
        lastCheckoffDayKey = "2020-01-01",
      )
    val normalizedToday = TreatmentPlanFormatter.normalizeForToday(yesterdayPlan)
    assertEquals(
      "Previous day dose check-offs should auto-reset on a new day",
      0,
      normalizedToday.completedTodaySlots,
    )

    // 5. Caregiver share text contains child, pharmacy, syringe-first warning, and emergency notice
    val shareText = TreatmentPlanFormatter.buildCaregiverShareText(normalizedToday, null)
    assertTrue(shareText.contains("سرنگ مدرج"))
    assertTrue(shareText.contains("۱۱۵"))
    assertTrue(shareText.contains(azithro.nameWithStrengthFa))
  }

  @Test
  fun treatmentPlan_accessibleFromHomeDrawerAndMyChildProfile() {
    composeTestRule.waitForIdle()

    // 1. Entry from Home Screen quick strip
    composeTestRule.onNodeWithTag("home_treatment_plan_card").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("treatment_plan_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("treatment_plan_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("rx_measure_safety_bar").assertIsDisplayed()

    // Return to Home
    composeTestRule.onNodeWithTag("topbar_back").performClick()
    composeTestRule.waitForIdle()

    // 2. Entry from Navigation Drawer
    composeTestRule.onNodeWithTag("topbar_menu").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("drawer_treatment_plan_item").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("treatment_plan_screen").assertIsDisplayed()

    // 3. Entry from My Child ("فرزند من") profile
    composeTestRule.onNodeWithTag("topbar_my_child").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("child_profile_treatment_plan_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("treatment_plan_screen").assertIsDisplayed()
  }

  @Test
  fun treatmentPlan_supportsDailyDoseCheckOffAddMedicationAndPharmacistJump() {
    composeTestRule.waitForIdle()

    // Open Treatment Plan from Home
    composeTestRule.onNodeWithTag("home_treatment_plan_card").performClick()
    composeTestRule.waitForIdle()

    // Initial progress & med count
    composeTestRule.onNodeWithTag("rx_med_count_badge").assertTextContains("۳ قلم دارو", substring = true)
    composeTestRule.onNodeWithTag("rx_daily_progress_label").assertTextContains("۰ از", substring = true)

    // Check off Azithromycin dose #1
    composeTestRule.onNodeWithTag("rx_dose_slot_rx_azithromycin_200_1").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
        composeTestRule.onNodeWithTag("rx_daily_progress_label").assertTextContains("۱ از", substring = true)
        true
      }.getOrDefault(false)
    }

    // Verify daily progress incremented
    composeTestRule.onNodeWithTag("rx_daily_progress_label")
      .performScrollTo()
      .assertTextContains("۱ از", substring = true)

    // Scroll past the Add Medication button (to storage box) so it sits cleanly above any active Snackbar
    composeTestRule.onNodeWithTag("rx_storage_box").performScrollTo()
    composeTestRule.onNodeWithTag("rx_add_medication_btn").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
        composeTestRule.onNodeWithTag("rx_med_name_input").assertIsDisplayed()
        true
      }.getOrDefault(false)
    }

    composeTestRule.onNodeWithTag("rx_med_name_input").performTextInput("شربت سیتریزین ۵ میلی‌گرم")
    composeTestRule.onNodeWithTag("rx_med_dose_input").performTextInput("۲٫۵ سی‌سی")
    composeTestRule.onNodeWithTag("rx_med_save_btn").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
        composeTestRule.onNodeWithTag("rx_med_count_badge").assertTextContains("۴ قلم دارو", substring = true)
        true
      }.getOrDefault(false)
    }

    // Verify badge updated to 4 medications
    composeTestRule.onNodeWithTag("rx_med_count_badge")
      .performScrollTo()
      .assertTextContains("۴ قلم دارو", substring = true)

    // Verify emergency allergy stop banner and Ask Pharmacist navigation
    composeTestRule.onNodeWithTag("rx_allergy_stop_banner").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("treatment_plan_screen").performTouchInput { swipeUp() }
    composeTestRule.onNodeWithTag("rx_ask_pharmacist_btn").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      runCatching {
        composeTestRule.onNodeWithTag("ask_pharmacist_list").assertIsDisplayed()
        true
      }.getOrDefault(false)
    }
    composeTestRule.onNodeWithTag("ask_pharmacist_list").assertIsDisplayed()
  }
}
