package com.example

import android.app.Application
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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.ManoContentRepository
import com.example.domain.model.VaccineFilter
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.util.VaccinationScheduleEngine
import com.example.ui.components.ManoEmptyStateDomain
import com.example.ui.components.ManoSectionEmptyStateCard
import com.example.ui.screens.MedicinesScreen
import com.example.ui.screens.SupplementsHygieneScreen
import com.example.ui.screens.VaccinesScreen
import com.example.ui.theme.ManoKidsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "fa-rIR-w411dp-h915dp-xhdpi", application = Application::class)
class ManoSectionEmptyStatesTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun emptyStateCards_renderDomainGraphicsNextStepsAndActionsInDarkAndLight() {
    var primaryClicked = false
    var secondaryClicked = false

    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = true) {
        ManoSectionEmptyStateCard(
          domain = ManoEmptyStateDomain.MEDICINES,
          titleFa = "دارویی یافت نشد",
          subtitleFa = "توضیح راهنما",
          nextStepsFa = listOf("گام اول پیشنهادی"),
          primaryActionLabelFa = "اقدام اصلی",
          onPrimaryAction = { primaryClicked = true },
          secondaryActionLabelFa = "اقدام دوم",
          onSecondaryAction = { secondaryClicked = true },
        )
      }
    }

    composeTestRule.onNodeWithTag("empty_state_medicines").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_state_medicines_next_steps").assertIsDisplayed()
    composeTestRule.onNodeWithText("پیشنهاد اقدام بعدی:").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_state_medicines_primary_btn").performClick()
    composeTestRule.onNodeWithTag("empty_state_medicines_secondary_btn").performClick()
    assertTrue(primaryClicked)
    assertTrue(secondaryClicked)
  }

  @Test
  fun medicinesScreen_emptyFilterShowsDedicatedMedicinesEmptyStateWithResetAndPharmacistCtas() {
    var resetCalled = false
    var pharmacistCalled = false
    val fullCatalog = ManoContentRepository().medications

    composeTestRule.setContent {
      ManoKidsTheme {
        MedicinesScreen(
          query = "ناموجود",
          medications = emptyList(),
          allMedicationsCatalog = fullCatalog,
          onQueryChange = { if (it.isEmpty()) resetCalled = true },
          onOpenMedication = {},
          onNavigateDevices = {},
          onNavigateAskPharmacist = { pharmacistCalled = true },
        )
      }
    }

    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("empty_state_medicines"))
    composeTestRule.onNodeWithTag("empty_state_medicines").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_state_medicines_next_steps").assertIsDisplayed()
    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("empty_state_medicines_primary_btn"))
    composeTestRule.onNodeWithTag("empty_state_medicines_primary_btn").performClick()
    composeTestRule.onNodeWithTag("medicines_list").performScrollToNode(hasTestTag("empty_state_medicines_secondary_btn"))
    composeTestRule.onNodeWithTag("empty_state_medicines_secondary_btn").performClick()
    assertTrue(resetCalled)
    assertTrue(pharmacistCalled)
  }

  @Test
  fun vaccinesScreen_emptyBirthDateAndAdministeredFilterShowDedicatedVaccinesEmptyState() {
    val birthMs = VaccinationScheduleEngine.parseFlexibleDateToEpochMs("2025/01/01")!!
    val summaryNoBirth =
      VaccinationScheduleEngine.resolveSchedule(
        localRecords = emptyList(),
        birthDateEpochMs = null,
        reminderSettings = VaccineReminderSettings(),
      )
    val summaryWithBirth =
      VaccinationScheduleEngine.resolveSchedule(
        localRecords = emptyList(),
        birthDateEpochMs = birthMs,
        reminderSettings = VaccineReminderSettings(),
      )

    composeTestRule.setContent {
      ManoKidsTheme {
        var activeBirth by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
        VaccinesScreen(
          summary = if (activeBirth == null) summaryNoBirth else summaryWithBirth,
          birthDateEpochMs = activeBirth,
          reminderSettings = VaccineReminderSettings(),
          childProfile = null,
          hasNotificationPermission = true,
          isCloudSignedIn = false,
          onSaveBirthDate = { activeBirth = birthMs },
          onUpdateReminderSettings = { _, _ -> },
          onRequestNotificationPermission = {},
          onSendPreviewReminder = {},
          onSaveVaccineRecord = {},
          onDeleteVaccineRecord = {},
          onFeedback = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("vaccines_list").performScrollToNode(hasTestTag("vaccine_empty_birthdate_card"))
    composeTestRule.onNodeWithTag("vaccine_empty_birthdate_card").assertIsDisplayed()

    // Register a birth date so the calendar activates, then filter by ADMINISTERED (0 logged)
    composeTestRule.onNodeWithTag("vaccines_list").performScrollToNode(hasTestTag("vaccine_birthdate_input"))
    composeTestRule.onNodeWithTag("vaccine_birthdate_input").performTextInput("1403/10/12")
    composeTestRule.onNodeWithTag("vaccine_save_birthdate_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("vaccines_list")
      .performScrollToNode(hasTestTag("vaccine_filter_${VaccineFilter.ADMINISTERED.name}"))
    composeTestRule.onNodeWithTag("vaccine_filter_${VaccineFilter.ADMINISTERED.name}").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("vaccines_list").performScrollToNode(hasTestTag("empty_state_vaccines"))
    composeTestRule.onNodeWithTag("empty_state_vaccines").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vaccines_list").performScrollToNode(hasTestTag("empty_state_vaccines_next_steps"))
    composeTestRule.onNodeWithTag("empty_state_vaccines_next_steps").assertIsDisplayed()

    composeTestRule.onNodeWithTag("vaccines_list").performScrollToNode(hasTestTag("empty_state_vaccines_primary_btn"))
    composeTestRule.onNodeWithTag("empty_state_vaccines_primary_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("empty_state_vaccines").assertDoesNotExist()
  }

  @Test
  fun supplementsScreen_noSearchMatchShowsDedicatedSupplementsEmptyStateAndResets() {
    var askedCategory: String? = null

    composeTestRule.setContent {
      ManoKidsTheme {
        SupplementsHygieneScreen(
          onNavigateAskPharmacist = { askedCategory = it },
          onNavigateSkinCare = {},
          onNavigateNutrition = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("supplements_hygiene_list")
      .performScrollToNode(hasTestTag("supplements_hygiene_search_input"))
    composeTestRule.onNodeWithTag("supplements_hygiene_search_input").performTextInput("عبارت_ناموجود_تست")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("supplements_hygiene_list").performScrollToNode(hasTestTag("empty_state_supplements"))
    composeTestRule.onNodeWithTag("empty_state_supplements").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_state_supplements_next_steps").assertIsDisplayed()

    composeTestRule.onNodeWithTag("supplements_hygiene_list")
      .performScrollToNode(hasTestTag("empty_state_supplements_secondary_btn"))
    composeTestRule.onNodeWithTag("empty_state_supplements_secondary_btn").performClick()
    assertTrue(askedCategory == "supplement")

    composeTestRule.onNodeWithTag("supplements_hygiene_list")
      .performScrollToNode(hasTestTag("empty_state_supplements_primary_btn"))
    composeTestRule.onNodeWithTag("empty_state_supplements_primary_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("empty_state_supplements").assertDoesNotExist()
  }
}
