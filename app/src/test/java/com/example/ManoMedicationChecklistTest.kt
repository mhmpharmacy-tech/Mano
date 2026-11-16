package com.example

import android.app.Application
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import com.example.data.ManoContentRepository
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.MedicationSafetyChecklistCatalog
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.ui.screens.MedicinesScreen
import com.example.ui.theme.ManoKidsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = Application::class)
class ManoMedicationChecklistTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun catalogEvaluation_tracksCriticalAndFullCompletionAccurately() {
    val items = MedicationSafetyChecklistCatalog.items
    assertEquals(8, items.size)
    assertTrue(items.any { it.id == "check_expiry_and_opened_date" })
    assertTrue(items.any { it.id == "check_dose_time_interval" })
    assertTrue(items.any { it.id == "check_concentration_match" })

    val emptyProgress = MedicationSafetyChecklistCatalog.evaluateProgress(emptySet())
    assertEquals(0, emptyProgress.checkedCount)
    assertEquals(8, emptyProgress.totalCount)
    assertEquals(6, emptyProgress.criticalTotalCount)
    assertFalse(emptyProgress.isAllCriticalReady)
    assertFalse(emptyProgress.isAllCompleted)
    assertEquals(0f, emptyProgress.progressFraction, 0.001f)

    // Unknown IDs should be ignored cleanly
    val invalidOnly = MedicationSafetyChecklistCatalog.evaluateProgress(setOf("non_existent_id"))
    assertEquals(0, invalidOnly.checkedCount)

    // Checking all 6 critical items unlocks critical readiness even before optional 2
    val criticalIds = items.filter { it.isCritical }.map { it.id }.toSet()
    val criticalReady = MedicationSafetyChecklistCatalog.evaluateProgress(criticalIds)
    assertEquals(6, criticalReady.checkedCount)
    assertTrue(criticalReady.isAllCriticalReady)
    assertFalse(criticalReady.isAllCompleted)
    assertTrue(MedicationSafetyChecklistCatalog.remainingCriticalTitles(criticalIds).isEmpty())

    // Checking all 8 completes 100%
    val fullIds = items.map { it.id }.toSet()
    val fullProgress = MedicationSafetyChecklistCatalog.evaluateProgress(fullIds)
    assertEquals(8, fullProgress.checkedCount)
    assertTrue(fullProgress.isAllCriticalReady)
    assertTrue(fullProgress.isAllCompleted)
    assertEquals(1f, fullProgress.progressFraction, 0.001f)
  }

  @Test
  fun medicinesScreen_checklistChecksProgressResetsAndLinksWithDoseLogbook() {
    val catalog = ManoContentRepository().medications
    val child =
      ChildProfileDemo(
        aliasFa = "آوا",
        birthDateOrAgeFa = "۲ سال",
        ageBand = AgeBand.TODDLER_1_3Y,
        weightKgText = "12.5",
        weightDateFa = "امروز",
        allergiesFa = "آسپرین",
        currentMedsFa = "",
        consentGranted = true,
      )
    val snapshot = FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())

    composeTestRule.setContent {
      ManoKidsTheme {
        var checkedIds by remember { mutableStateOf(emptySet<String>()) }
        MedicinesScreen(
          query = "",
          medications = catalog,
          allMedicationsCatalog = catalog,
          onQueryChange = {},
          onOpenMedication = {},
          onNavigateDevices = {},
          onNavigateAskPharmacist = {},
          childProfile = child,
          timelineSnapshot = snapshot,
          checkedMedSafetyIds = checkedIds,
          onToggleMedSafetyCheck = { id ->
            checkedIds = if (id in checkedIds) checkedIds - id else checkedIds + id
          },
          onResetMedSafetyChecklist = { checkedIds = emptySet() },
        )
      }
    }

    composeTestRule.waitForIdle()

    // Open Checklist via hero shortcut CTA
    composeTestRule.onNodeWithTag("medicines_open_checklist_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("medicines_list").performScrollToIndex(2)
    composeTestRule.onNodeWithTag("med_safety_count_badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("0/8").assertIsDisplayed()
    composeTestRule.onNodeWithTag("med_safety_live_context_card").assertIsDisplayed()

    // Toggle expiry and dose interval items and inspect rationale tip
    composeTestRule.onNodeWithTag("medicines_list").performScrollToIndex(5)
    composeTestRule.onNodeWithTag("med_check_tip_toggle_check_expiry_and_opened_date").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("med_check_tip_body_check_expiry_and_opened_date").assertIsDisplayed()
    composeTestRule.onNodeWithText("روی بطری تاریخ باز شدن را یادداشت کنید.", substring = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("med_check_box_check_expiry_and_opened_date").performClick()
    composeTestRule.onNodeWithTag("medicines_list").performScrollToIndex(10)
    composeTestRule.onNodeWithTag("med_check_box_check_dose_time_interval").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("medicines_list").performScrollToIndex(2)
    composeTestRule.onNodeWithText("2/8").assertIsDisplayed()

    // Navigate to Dose Logbook from Checklist bottom action bar and verify readiness gate banner
    composeTestRule.onNodeWithTag("medicines_list").performScrollToIndex(14)
    composeTestRule.onNodeWithTag("med_checklist_go_logbook_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("dose_log_checklist_gate_banner"))
    composeTestRule.onNodeWithText("چک‌لیست ایمنی پیش از دارو: 2/8").assertIsDisplayed()

    // Return to checklist and reset checks for next dose
    composeTestRule.onNodeWithTag("dose_log_open_checklist_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("medicines_list").performScrollToIndex(14)
    composeTestRule.onNodeWithTag("med_checklist_reset_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("medicines_list").performScrollToIndex(2)
    composeTestRule.onNodeWithText("0/8").assertIsDisplayed()
  }

  @Test
  fun medicationSafetyChecklistPanel_rendersInDarkModeWithoutCrash() {
    val snapshot = FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column(
          modifier = androidx.compose.ui.Modifier.verticalScroll(
            androidx.compose.foundation.rememberScrollState()
          )
        ) {
          com.example.ui.components.MedicationSafetyChecklistPanel(
            checkedIds = setOf("check_expiry_and_opened_date"),
            childProfile = null,
            timelineSnapshot = snapshot,
            onToggleItem = {},
            onResetChecklist = {},
            onOpenDoseLogbook = {},
            onOpenSyringeLab = {},
            onOpenOverlapChecker = {},
          )
        }
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("med_safety_checklist_panel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("med_safety_checklist_header").assertIsDisplayed()
    composeTestRule.onNodeWithTag("med_safety_live_context_card").assertIsDisplayed()
  }
}
