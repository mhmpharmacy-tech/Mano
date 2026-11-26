package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.ui.ManoApp
import com.example.ui.components.SupplementsHygieneRecommendedCard
import com.example.ui.screens.SupplementsHygieneScreen
import com.example.ui.theme.ManoKidsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "fa-rIR-w411dp-h915dp-xhdpi")
class ManoSupplementsHygieneAndNoLabTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun supplementsHygieneCard_switchesCategoriesOpensBottomSheetAndRoutesPharmacist() {
    var askedCategory: String? = null
    var openedAllGuides = false

    composeTestRule.setContent {
      ManoKidsTheme {
        SupplementsHygieneRecommendedCard(
          onAskPharmacist = { askedCategory = it },
          onOpenAllGuides = { openedAllGuides = true },
        )
      }
    }

    // Default tab is Supplements ("مکمل‌ها")
    composeTestRule.onNodeWithTag("supplements_hygiene_recommended_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recommended_tab_supplements").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recommended_tab_hygiene").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recommended_product_rec_vit_d3").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recommended_product_rec_zinc_cream").assertDoesNotExist()

    // Switch to Hygiene tab ("بهداشت")
    composeTestRule.onNodeWithTag("recommended_tab_hygiene").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("recommended_product_rec_zinc_cream").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recommended_product_rec_vit_d3").assertDoesNotExist()

    // Tap product row -> opens safe guide bottom sheet with product-specific Ask Pharmacist CTA
    composeTestRule.onNodeWithTag("recommended_product_rec_zinc_cream").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("recommended_product_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recommended_sheet_ask_pharmacist_btn").performClick()
    assertEquals("skin", askedCategory)

    // Switch back to Supplements and tap main Ask Pharmacist CTA
    composeTestRule.onNodeWithTag("recommended_tab_supplements").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("recommended_card_ask_pharmacist_btn").performClick()
    assertEquals("supplement", askedCategory)

    // "همه راهنماها" link triggers full guides navigation
    composeTestRule.onNodeWithTag("recommended_card_view_all_btn").performClick()
    assertTrue(openedAllGuides)
  }

  @Test
  fun supplementsHygieneScreenSwitchesTabsFiltersAndRoutesToPharmacist() {
    var askedCategory: String? = null
    var openedSkin = false
    var openedNutrition = false

    composeTestRule.setContent {
      ManoKidsTheme {
        SupplementsHygieneScreen(
          onNavigateAskPharmacist = { askedCategory = it },
          onNavigateSkinCare = { openedSkin = true },
          onNavigateNutrition = { openedNutrition = true },
        )
      }
    }

    composeTestRule.onNodeWithTag("supplements_hygiene_hero_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tab_supplements").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tab_hygiene").assertIsDisplayed()

    // Supplements tab shows Vitamin A+D and Iron guides
    composeTestRule.onNodeWithTag("supplements_hygiene_list")
      .performScrollToNode(hasTestTag("guide_card_supp_vit_ad_d3"))
    composeTestRule.onNodeWithTag("guide_card_supp_vit_ad_d3").assertIsDisplayed()

    // Switch to Hygiene tab
    composeTestRule.onNodeWithTag("supplements_hygiene_list")
      .performScrollToNode(hasTestTag("tab_hygiene"))
    composeTestRule.onNodeWithTag("tab_hygiene").performClick()

    composeTestRule.onNodeWithTag("supplements_hygiene_list")
      .performScrollToNode(hasTestTag("guide_card_hyg_oral_teeth"))
    composeTestRule.onNodeWithTag("guide_card_hyg_oral_teeth").assertIsDisplayed()

    // Filter with search input
    composeTestRule.onNodeWithTag("supplements_hygiene_list")
      .performScrollToNode(hasTestTag("supplements_hygiene_search_input"))
    composeTestRule.onNodeWithTag("supplements_hygiene_search_input").performTextInput("پوشک")
    composeTestRule.onNodeWithTag("supplements_hygiene_list")
      .performScrollToNode(hasTestTag("guide_card_hyg_diaper"))
    composeTestRule.onNodeWithTag("guide_card_hyg_diaper").assertIsDisplayed()
    composeTestRule.onNodeWithTag("guide_card_hyg_oral_teeth").assertDoesNotExist()

    // Direct Pharmacist CTA routes medication/supplement questions to Ask Pharmacist
    composeTestRule.onNodeWithTag("supplements_hygiene_list")
      .performScrollToNode(hasTestTag("supplements_ask_pharmacist_btn"))
    composeTestRule.onNodeWithTag("supplements_ask_pharmacist_btn").performClick()
    assertEquals("otc", askedCategory)

    // Quick link to Skin & Nutrition works
    composeTestRule.onNodeWithTag("supplements_hygiene_list")
      .performScrollToNode(hasTestTag("supplements_open_skin_btn"))
    composeTestRule.onNodeWithTag("supplements_open_skin_btn").performClick()
    composeTestRule.onNodeWithTag("supplements_open_nutrition_btn").performClick()
    assertTrue(openedSkin)
    assertTrue(openedNutrition)
  }

  @Test
  fun manoAppCareHomeDisplaysSupplementsHygieneCardAndNavigatesToPharmacist() {
    composeTestRule.setContent {
      ManoKidsTheme {
        ManoApp()
      }
    }

    // Active Care Home shows MANO hero card & primary actions
    composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_menu_fever").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_ask_pharmacist").assertIsDisplayed()

    // Bottom navigation bar has Supplements & Hygiene + Pharmacist
    composeTestRule.onNodeWithTag("nav_supplements_hygiene").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_pharmacist").assertIsDisplayed()

    // Care Home displays the upgraded categorized "مکمل‌ها و بهداشت کودک" card with Ask Pharmacist CTA
    composeTestRule.onNodeWithTag("home_list")
      .performScrollToNode(hasTestTag("supplements_hygiene_recommended_card"))
    composeTestRule.onNodeWithTag("supplements_hygiene_recommended_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_list")
      .performScrollToNode(hasTestTag("recommended_card_ask_pharmacist_btn"))
    composeTestRule.onNodeWithTag("recommended_card_ask_pharmacist_btn").performClick()
    composeTestRule.waitUntil(5_000L) {
      composeTestRule.onAllNodes(hasTestTag("mano_connect_header_card")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("mano_connect_header_card").assertIsDisplayed()

    // Tap bottom nav Supplements & Hygiene -> opens SupplementsHygieneScreen
    composeTestRule.onNodeWithTag("nav_supplements_hygiene").performClick()
    composeTestRule.waitUntil(5_000L) {
      composeTestRule.onAllNodes(hasTestTag("supplements_hygiene_hero_card")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("supplements_hygiene_hero_card").assertIsDisplayed()

    // Tap bottom nav Pharmacist -> opens AskPharmacistScreen
    composeTestRule.onNodeWithTag("nav_pharmacist").performClick()
    composeTestRule.waitUntil(5_000L) {
      composeTestRule.onAllNodes(hasTestTag("mano_connect_header_card")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("mano_connect_header_card").assertIsDisplayed()
  }
}
