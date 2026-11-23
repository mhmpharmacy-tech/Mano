package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
class ManoColdStartTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun coldStart_landsDirectlyOnCompactFlowchartHomeWithoutBlockingDialogOrTimeout() {
    composeTestRule.waitForIdle()

    // No blocking full-screen onboarding dialog on cold start
    assertTrue(
      "Onboarding overlay should not block cold-start HomeScreen",
      composeTestRule.onAllNodesWithTag("onboarding_overlay").fetchSemanticsNodes().isEmpty(),
    )

    // Core HomeScreen flowchart + MANO Orbital hub render immediately
    composeTestRule.onNodeWithTag("home_brand_crest").assertIsDisplayed()
    composeTestRule.onNodeWithTag("orbital_menu_stage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_mascot_touch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_menu_fever").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_menu_supplements_hygiene").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_menu_pharmacist").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_pharmacist_hero_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_specialized_strip").assertIsDisplayed()

    // Mascot tap bounce + greeting bubble works without jank
    composeTestRule.onNodeWithTag("mano_mascot_touch").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mano_mascot_greeting_bubble").assertIsDisplayed()
  }

  @Test
  fun coldStart_navigatesToSupplementsHygieneAndCanOpenOnboardingOnDemand() {
    composeTestRule.waitForIdle()

    // Direct tap on Orbital hub icon enters compact Supplements & Hygiene flowchart screen
    composeTestRule.onNodeWithTag("home_menu_supplements_hygiene").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("supplements_hygiene_screen").assertIsDisplayed()

    // Navigate back to Home cleanly
    composeTestRule.onNodeWithTag("topbar_back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("orbital_menu_stage").assertIsDisplayed()

    // Onboarding remains accessible on demand from the navigation drawer
    composeTestRule.onNodeWithTag("topbar_menu").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("drawer_onboarding_item").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_overlay").assertIsDisplayed()
  }
}
