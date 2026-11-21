package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.ManoPreferencesRepository
import com.example.data.PendingOfflineConsultation
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.util.FeverAgeGroup
import com.example.domain.util.FeverDoseCalculatorEngine
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverPriorDoseStatus
import com.example.domain.util.LiquidFormulationId
import com.example.domain.util.TimelineDosePoint
import com.example.ui.screens.FeverDoseCalculatorScreen
import com.example.ui.theme.ManoKidsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class FeverDoseCalculatorAndOfflineSyncTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun engine_under3Months_isHardStopped() {
    val result =
      FeverDoseCalculatorEngine.calculate(
        ageGroup = FeverAgeGroup.UNDER_3_MONTHS,
        formulation = LiquidFormulationId.ACETAMINOPHEN_DROPS_100,
        weightKg = 5.0f,
      )
    assertTrue(result.isBlocked)
    assertEquals(0f, result.recommendedMl, 0.001f)
    assertNotNull(result.blockReasonFa)
  }

  @Test
  fun engine_3To6Months_locksIbuprofenAndAllowsAcetaminophen() {
    assertFalse(
      FeverDoseCalculatorEngine.isFormulationAllowedForAge(
        FeverAgeGroup.THREE_TO_SIX_MONTHS,
        LiquidFormulationId.IBUPROFEN_SYRUP_100_5,
      )
    )
    assertTrue(
      FeverDoseCalculatorEngine.isFormulationAllowedForAge(
        FeverAgeGroup.THREE_TO_SIX_MONTHS,
        LiquidFormulationId.ACETAMINOPHEN_DROPS_100,
      )
    )
    val blockedIbu =
      FeverDoseCalculatorEngine.calculate(
        ageGroup = FeverAgeGroup.THREE_TO_SIX_MONTHS,
        formulation = LiquidFormulationId.IBUPROFEN_SYRUP_100_5,
        weightKg = 7.0f,
      )
    assertTrue(blockedIbu.isBlocked)

    val safeDrops =
      FeverDoseCalculatorEngine.calculate(
        ageGroup = FeverAgeGroup.THREE_TO_SIX_MONTHS,
        formulation = LiquidFormulationId.ACETAMINOPHEN_DROPS_100,
        weightKg = 6.0f,
      )
    assertFalse(safeDrops.isBlocked)
    assertEquals(0.7f, safeDrops.recommendedMl, 0.05f)
    assertEquals(14, safeDrops.approxDropsCount)
  }

  @Test
  fun engine_over12KgWithDrops_triggersSyrupSuggestion_andSafetyCeilingCapsHeavyWeight() {
    val notices =
      FeverDoseCalculatorEngine.evaluateWeightNotices(
        ageGroup = FeverAgeGroup.OVER_6_MONTHS,
        weightKg = 14.0f,
        formulation = LiquidFormulationId.ACETAMINOPHEN_DROPS_100,
      )
    assertTrue(notices.showDropsToSyrupSuggestion)
    assertNotNull(notices.dropsSuggestionMessageFa)

    val outlier3To6 =
      FeverDoseCalculatorEngine.evaluateWeightNotices(
        ageGroup = FeverAgeGroup.THREE_TO_SIX_MONTHS,
        weightKg = 14.0f,
        formulation = LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5,
      )
    assertTrue(outlier3To6.isOutlierWarning)

    val heavyResult =
      FeverDoseCalculatorEngine.calculate(
        ageGroup = FeverAgeGroup.OVER_6_MONTHS,
        formulation = LiquidFormulationId.ACETAMINOPHEN_SYRUP_160_5,
        weightKg = 40.0f,
      )
    assertFalse(heavyResult.isBlocked)
    assertTrue(heavyResult.isSafetyCeilingApplied)
    assertTrue(heavyResult.recommendedMg <= 500.0)
    assertTrue(heavyResult.recommendedMg >= 480.0)
  }

  @Test
  fun engine_priorDoseUnder4Hours_triggersHardConflict_andBuildsWhatsAppReport() {
    val conflict =
      FeverDoseCalculatorEngine.evaluatePriorDoseConflict(
        selectedFormulation = LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5,
        priorStatus = FeverPriorDoseStatus.ACETAMINOPHEN_UNDER_4H,
      )
    assertTrue(conflict.isHardConflict)

    val result =
      FeverDoseCalculatorEngine.calculate(
        ageGroup = FeverAgeGroup.OVER_6_MONTHS,
        formulation = LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5,
        weightKg = 12.0f,
      )
    val report =
      FeverDoseCalculatorEngine.buildWhatsAppCaregiverReport(
        childAliasFa = "آوا",
        result = result,
        givenAtTimeLabelFa = "۰۲:۱۵",
        nextSafeTimeLabelFa = "۰۸:۱۵",
      )
    assertTrue(report.contains("آوا"))
    assertTrue(report.contains("۰۲:۱۵"))
    assertTrue(report.contains("۰۸:۱۵"))
    assertTrue(report.contains("مانو کید"))
  }

  @Test
  fun feverCalculatorUi_under3MonthsShowsHardStopEmergencyCard() {
    var pharmacistTapped = false
    val emptySnapshot = FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())

    composeTestRule.setContent {
      ManoKidsTheme {
        FeverDoseCalculatorScreen(
          childProfile = null,
          childBirthDateEpochMs = null,
          timelineSnapshot = emptySnapshot,
          onLogDose = { _, _, _, _, _, _, done -> done() },
          onOpenSyrupScanner = {},
          onTalkToPharmacist = { pharmacistTapped = true },
          onRequestNotificationPermission = {},
          onFeedback = {},
          onBack = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fever_age_under_3m").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("fever_under3m_hard_stop_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fever_under3m_call_115_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fever_under3m_pharmacist_btn").performClick()
    assertTrue(pharmacistTapped)
  }

  @Test
  fun feverCalculatorUi_fullWizardTo2DSyringeAndNonDrugCareCard() {
    var loggedSlug: String? = null
    val emptySnapshot = FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())

    composeTestRule.setContent {
      ManoKidsTheme {
        FeverDoseCalculatorScreen(
          childProfile = null,
          childBirthDateEpochMs = null,
          timelineSnapshot = emptySnapshot,
          onLogDose = { slug, _, _, _, _, _, done ->
            loggedSlug = slug
            done()
          },
          onOpenSyrupScanner = {},
          onTalkToPharmacist = {},
          onRequestNotificationPermission = {},
          onFeedback = {},
          onBack = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    // Step 1 -> Over 6 months
    composeTestRule.onNodeWithTag("fever_age_over_6m").performClick()
    composeTestRule.waitForIdle()

    // Step 2 -> Pick Acetaminophen Syrup 120/5 -> Confirm concentration modal
    composeTestRule
      .onNodeWithTag("fever_dose_calculator_list")
      .performScrollToNode(hasTestTag("fever_med_acetaminophen_syrup_120_5"))
    composeTestRule.onNodeWithTag("fever_med_acetaminophen_syrup_120_5").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fever_confirm_concentration_btn").performClick()
    composeTestRule.waitForIdle()

    // Step 3 -> Enter weight 12 kg -> continue
    composeTestRule.onNodeWithTag("fever_weight_manual_input").performScrollTo().performTextReplacement("12")
    composeTestRule.onNodeWithTag("fever_step3_continue_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Step 4 -> First test <4h conflict block opens Non-Drug Care Card
    composeTestRule.onNodeWithTag("fever_prior_acet_under_4h").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fever_prior_dose_conflict_block").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("fever_conflict_open_nondrug_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Non-Drug Care Card displays all 3 sections and bottom action bar
    composeTestRule.onNodeWithTag("non_drug_fever_care_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("non_drug_safe_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("non_drug_care_list").performScrollToNode(hasTestTag("non_drug_myths_section"))
    composeTestRule.onNodeWithTag("non_drug_myths_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("non_drug_care_list").performScrollToNode(hasTestTag("non_drug_red_flags_section"))
    composeTestRule.onNodeWithTag("non_drug_red_flags_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("non_drug_emergency_115_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("non_drug_back_countdown_btn").performClick()
    composeTestRule.waitForIdle()

    // Select None in 6h -> proceed to Step 5 result
    composeTestRule.onNodeWithTag("fever_prior_none").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("fever_step4_calculate_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Step 5 -> 2D syringe, pharmacist badge, and one-tap dose log
    composeTestRule.onNodeWithTag("visual_2d_syringe_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fever_result_ml_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fever_dose_calculator_list").performScrollToNode(hasTestTag("fever_pharmacist_trust_badge"))
    composeTestRule.onNodeWithTag("fever_pharmacist_trust_badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("د۳۴۳۶۱", substring = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("fever_dose_calculator_list").performScrollToNode(hasTestTag("fever_log_and_remind_btn"))
    composeTestRule.onNodeWithTag("fever_log_and_remind_btn").performClick()
    composeTestRule.waitForIdle()
    assertEquals("acetaminophen", loggedSlug)
    composeTestRule.onNodeWithTag("fever_logged_countdown_card").assertIsDisplayed()
  }

  @Test
  fun preferencesRepository_uidScopedOfflineProfileAndQueuesRemainStrictlyIsolated() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ManoPreferencesRepository(context)
    prefs.clearLocalChildHealthData()

    val profileA =
      ChildProfileDemo(
        aliasFa = "آوا",
        birthDateOrAgeFa = "۱۸ ماه",
        ageBand = AgeBand.TODDLER_1_3Y,
        weightKgText = "11",
        weightDateFa = "امروز",
        allergiesFa = "ندارد",
        currentMedsFa = "ندارد",
        consentGranted = true,
        ageMonthsExact = 18,
      )
    val pendingConsultA =
      PendingOfflineConsultation(
        id = "offline_q_1",
        categoryName = "OTC",
        ageBandId = AgeBand.TODDLER_1_3Y.id,
        questionSummaryFa = "سوال آفلاین تستی برای همگام‌سازی",
        createdAtLabelFa = "۰۲:۳۰",
        savedAtEpochMs = 1_700_000_000_000L,
      )
    val pendingDoseA =
      TimelineDosePoint(
        id = "offline_dose_1",
        medicationSlug = "acetaminophen-pediatric",
        medicationNameFa = "استامینوفن",
        dosageFormFa = "شربت ۱۲۰/۵",
        volumeMlText = "7.5 mL",
        givenAtLabelFa = "۰۲:۳۰",
        noteFa = "ثبت آفلاین",
        timestampMs = 1_700_000_000_000L,
        isSyncedCloud = false,
      )

    try {
      prefs.saveScopedChildProfile("uid_alpha", profileA, pendingCloudSync = true)
      prefs.addScopedPendingConsultation("uid_alpha", pendingConsultA)
      prefs.addScopedPendingDoseLog("uid_alpha", pendingDoseA)

      val savedA = prefs.scopedChildProfile("uid_alpha").first()
      assertNotNull(savedA)
      assertEquals("آوا", savedA?.profile?.aliasFa)
      assertTrue(savedA?.pendingCloudSync == true)
      assertEquals(1, prefs.scopedPendingConsultations("uid_alpha").first().size)
      assertEquals(1, prefs.scopedPendingDoseLogs("uid_alpha").first().size)

      // Strict UID isolation: uid_beta must NOT see uid_alpha's offline records
      assertNull(prefs.scopedChildProfile("uid_beta").first())
      assertTrue(prefs.scopedPendingConsultations("uid_beta").first().isEmpty())
      assertTrue(prefs.scopedPendingDoseLogs("uid_beta").first().isEmpty())

      // Removal and full wipe work cleanly
      prefs.removeScopedPendingConsultation("uid_alpha", "offline_q_1")
      prefs.removeScopedPendingDoseLog("uid_alpha", "offline_dose_1")
      assertTrue(prefs.scopedPendingConsultations("uid_alpha").first().isEmpty())
      assertTrue(prefs.scopedPendingDoseLogs("uid_alpha").first().isEmpty())
    } finally {
      prefs.clearLocalChildHealthData()
    }
    assertNull(prefs.scopedChildProfile("uid_alpha").first())
  }
}
