package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.ManoContentRepository
import com.example.data.ManoHapticIntensity
import com.example.data.ManoPreferencesRepository
import com.example.data.ManoWebGlQuality
import com.example.domain.model.AgeBand
import com.example.domain.util.AssetLinksCheckSeverity
import com.example.domain.util.AssetLinksVerifier
import com.example.domain.util.ManoDomainLinks
import com.example.ui.components.ManoOnboardingOverlay
import com.example.ui.screens.InfoScreen
import com.example.ui.theme.ManoKidsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = Application::class)
class ManoOnboardingAndReleasePolishTest {

  @get:Rule val composeTestRule = createComposeRule()

  @After
  fun cleanupPreferences() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ManoPreferencesRepository(context)
    runBlocking {
      repo.setOnboardingCompleted(false)
      repo.setHapticIntensity(ManoHapticIntensity.MEDIUM)
      repo.setWebGlQuality(ManoWebGlQuality.AUTO)
      repo.setLocalChildProfile(null)
    }
  }

  @Test
  fun onboardingThreeStepFlow_completesWithPersonalizedProfileAndBirthDate() {
    var capturedAlias: String? = null
    var capturedBirthMs: Long? = null
    var capturedBand: AgeBand? = null
    var capturedWeight: String? = null
    var capturedHeight: String? = null

    composeTestRule.setContent {
      ManoKidsTheme {
        ManoOnboardingOverlay(
          onComplete = { alias, birthMs, band, weightKg, heightCm ->
            capturedAlias = alias
            capturedBirthMs = birthMs
            capturedBand = band
            capturedWeight = weightKg
            capturedHeight = heightCm
          },
          onSkip = {},
        )
      }
    }

    // Step 1: Nickname & Age band
    composeTestRule.onNodeWithTag("onboarding_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_alias_input").performTextInput("آراد")
    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()

    // Step 2: Birth Date preset (9 months = 275 days ago) + Weight & Height
    composeTestRule.onNodeWithTag("onboarding_birthdate_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_preset_275").performClick()
    composeTestRule.onNodeWithTag("onboarding_weight_input").performTextInput("9.2")
    composeTestRule.onNodeWithTag("onboarding_height_input").performTextInput("73")
    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()

    // Step 3: Live Personalization Preview cards (Vaccines, mL Syringe, WHO) + Complete
    composeTestRule.onNodeWithTag("onboarding_preview_vaccine").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_preview_syringe").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_preview_who").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_finish_btn").performClick()

    assertEquals("آراد", capturedAlias)
    assertNotNull("Expected valid parsed birth date epoch", capturedBirthMs)
    assertEquals(AgeBand.INFANT_3_12M, capturedBand)
    assertEquals("9.2", capturedWeight)
    assertEquals("73", capturedHeight)
  }

  @Test
  fun onboardingValidation_blocksInvalidDateAndAllowsSkip() {
    var skipped = false
    composeTestRule.setContent {
      ManoKidsTheme {
        ManoOnboardingOverlay(
          onComplete = { _, _, _, _, _ -> },
          onSkip = { skipped = true },
        )
      }
    }

    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()
    composeTestRule.onNodeWithTag("onboarding_birthdate_input").performTextInput("invalid-date")
    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()
    composeTestRule.onNodeWithTag("onboarding_error_banner").assertIsDisplayed()

    composeTestRule.onNodeWithTag("onboarding_skip_btn").performClick()
    assertTrue("Skip button should invoke onSkip", skipped)
  }

  @Test
  fun preferencesRepository_persistsOnboardingHapticAndWebGlSettings() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ManoPreferencesRepository(context)

    assertFalse(repo.onboardingCompleted.first())
    assertEquals(ManoHapticIntensity.MEDIUM, repo.hapticIntensity.first())
    assertEquals(ManoWebGlQuality.AUTO, repo.webGlQuality.first())

    repo.setOnboardingCompleted(true)
    repo.setHapticIntensity(ManoHapticIntensity.STRONG)
    repo.setWebGlQuality(ManoWebGlQuality.BALANCED)

    assertTrue(repo.onboardingCompleted.first())
    assertEquals(ManoHapticIntensity.STRONG, repo.hapticIntensity.first())
    assertEquals(ManoWebGlQuality.BALANCED, repo.webGlQuality.first())
  }

  @Test
  fun assetLinksVerifier_validatesJsonMatchPlaceholderMismatchAndErrors() {
    val pkg = "com.aistudio.manokids.x8k4mz"
    val validFp = "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"

    val validJson =
      ManoDomainLinks.buildAssetLinksJson(
        packageName = pkg,
        sha256Fingerprints = listOf(validFp),
        includeReleasePlaceholder = false,
      )
    val verified =
      AssetLinksVerifier.parseAndValidateAssetLinksJson(
        domain = "manokid.ir",
        rawJson = validJson,
        expectedPackageName = pkg,
        expectedFingerprints = listOf(validFp),
      )
    assertEquals(AssetLinksCheckSeverity.VERIFIED, verified.severity)
    assertTrue(verified.packageMatched)
    assertTrue(verified.fingerprintMatched)

    val placeholderJson =
      ManoDomainLinks.buildAssetLinksJson(
        packageName = pkg,
        sha256Fingerprints = listOf(validFp),
        includeReleasePlaceholder = true,
      )
    val placeholderReport =
      AssetLinksVerifier.parseAndValidateAssetLinksJson(
        domain = "manokid.ir",
        rawJson = placeholderJson,
        expectedPackageName = pkg,
        expectedFingerprints = listOf(validFp),
      )
    assertEquals(AssetLinksCheckSeverity.WARNING, placeholderReport.severity)
    assertTrue(placeholderReport.hasPlaceholderFingerprint)

    val wrongPkgReport =
      AssetLinksVerifier.parseAndValidateAssetLinksJson(
        domain = "manokid.com",
        rawJson = validJson,
        expectedPackageName = "com.other.app",
        expectedFingerprints = listOf(validFp),
      )
    assertEquals(AssetLinksCheckSeverity.FAILED, wrongPkgReport.severity)
    assertFalse(wrongPkgReport.packageMatched)

    val invalidJsonReport =
      AssetLinksVerifier.parseAndValidateAssetLinksJson(
        domain = "manokid.ir",
        rawJson = "<html>404 Not Found</html>",
        expectedPackageName = pkg,
        expectedFingerprints = listOf(validFp),
      )
    assertEquals(AssetLinksCheckSeverity.FAILED, invalidJsonReport.severity)
    assertFalse(invalidJsonReport.isValidJson)
  }

  @Test
  fun infoScreen_releaseTabAndDomainVerifyButtonWorkCorrectly() {
    val sources = ManoContentRepository().sources
    var selectedHaptic: ManoHapticIntensity? = null
    var selectedQuality: ManoWebGlQuality? = null
    var relaunchedOnboarding = false
    var feedbackToast: String? = null

    composeTestRule.setContent {
      ManoKidsTheme {
        InfoScreen(
          initialSection = "release",
          sources = sources,
          hapticIntensity = ManoHapticIntensity.MEDIUM,
          webGlQuality = ManoWebGlQuality.AUTO,
          onSelectHapticIntensity = { selectedHaptic = it },
          onSelectWebGlQuality = { selectedQuality = it },
          onLaunchOnboarding = { relaunchedOnboarding = true },
          onShowFeedback = { feedbackToast = it },
        )
      }
    }

    composeTestRule.onNodeWithTag("info_haptic_tuning_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("haptic_level_strong").performClick()
    assertEquals(ManoHapticIntensity.STRONG, selectedHaptic)

    composeTestRule.onNodeWithTag("test_haptic_btn").performClick()
    assertNotNull(feedbackToast)

    composeTestRule.onNodeWithTag("info_list").performScrollToNode(hasTestTag("info_webgl_tuning_card"))
    composeTestRule.onNodeWithTag("webgl_quality_balanced").performClick()
    assertEquals(ManoWebGlQuality.BALANCED, selectedQuality)

    composeTestRule.onNodeWithTag("info_list").performScrollToNode(hasTestTag("info_release_checklist_card"))
    composeTestRule.onNodeWithTag("info_reopen_onboarding_btn").performClick()
    assertTrue(relaunchedOnboarding)

    // Switch to Domain tab and confirm live verification button exists
    composeTestRule.onNodeWithTag("info_list").performScrollToNode(hasTestTag("info_tab_domain"))
    composeTestRule.onNodeWithTag("info_tab_domain").performClick()
    composeTestRule.onNodeWithTag("info_list").performScrollToNode(hasTestTag("info_assetlinks_card"))
    composeTestRule.onNodeWithTag("verify_assetlinks_btn").assertIsDisplayed()
  }
}
