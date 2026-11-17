package com.example

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.ui.screens.MyChildScreen
import com.example.ui.theme.ManoKidsTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManoPrivacyScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun localHealthDataDeletionRequiresAnExplicitSecondConfirmation() {
    var eraseRequested = false
    composeTestRule.setContent {
      ManoKidsTheme {
        MyChildScreen(
          profile = null,
          exportedJson = null,
          onSaveProfile = { _, _, _, _, _, _, _, _ -> },
          onExportSummary = {},
          onDeleteProfile = {},
          onDeleteAllLocalChildData = { eraseRequested = true },
        )
      }
    }
    composeTestRule.onNodeWithTag("child_profile_list")
      .performScrollToNode(hasTestTag("delete_all_local_child_data_btn"))
    composeTestRule.onNodeWithTag("delete_all_local_child_data_btn").performClick()
    composeTestRule.waitForIdle()
    assertFalse("Opening dialog must never erase records", eraseRequested)

    composeTestRule.onNodeWithText("انصراف").performClick()
    composeTestRule.waitForIdle()
    assertFalse("Cancel must preserve the records", eraseRequested)

    composeTestRule.onNodeWithTag("delete_all_local_child_data_btn").performClick()
    composeTestRule.onNodeWithTag("delete_all_local_confirm_btn").performClick()
    composeTestRule.waitForIdle()
    assertTrue("Only the explicit affirmative dialog action may request erasure", eraseRequested)
  }
}
