package com.example

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.ManoPreferencesRepository
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.GeminiSyrupScanPayload
import com.example.domain.model.SyrupDosageForm
import com.example.domain.model.SyrupIngredientKey
import com.example.domain.util.LiquidFormulationId
import com.example.domain.util.SyrupConcentrationMatcher
import com.example.ui.components.AlertSeverity
import com.example.ui.viewmodel.ManoSyrupScannerViewModel
import com.example.ui.viewmodel.SyrupScannerAiEngine
import com.example.ui.viewmodel.SyrupScannerUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class ManoSyrupScannerTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @After
  fun cleanupDataStore() {
    runBlocking {
      val repo = ManoPreferencesRepository(ApplicationProvider.getApplicationContext())
      repo.setGalacticOrbitSelection("", "")
      val existingScans = repo.savedSyrupScans.first()
      existingScans.forEach { repo.deleteSyrupScan(it.id) }
    }
  }

  @Test
  fun matcher_acetaminophenDrops100_triggersMixUpWarningAndSafeDropVolume() {
    val payload =
      GeminiSyrupScanPayload(
        detectedDrugNameFa = "قطره استامینوفن کودکان ۱۰۰ میلی‌گرم",
        activeIngredientKey = "ACETAMINOPHEN",
        dosageForm = "DROPS",
        mgAmount = 100.0,
        perMlAmount = 1.0,
        mgPerMl = 100.0,
        matchedFormulationId = "acetaminophen_drops_100",
        confidenceLevel = "HIGH",
        isPediatricFeverOrAllergyLiquid = true,
      )
    val child =
      ChildProfileDemo(
        aliasFa = "آرتین",
        birthDateOrAgeFa = "۸ ماه",
        ageBand = AgeBand.INFANT_3_12M,
        weightKgText = "8.0",
        weightDateFa = "امروز",
        allergiesFa = "ندارد",
        currentMedsFa = "ندارد",
        consentGranted = true,
      )

    val result = SyrupConcentrationMatcher.reconcileFromAi(payload, child)

    assertEquals(LiquidFormulationId.ACETAMINOPHEN_DROPS_100, result.matchedFormulation)
    assertEquals(AlertSeverity.DANGER, result.warningSeverity)
    assertNotNull(result.mixUpWarningFa)
    assertFalse(result.canAutoFillSyringe)
    assertEquals(0.0f, result.dosePreview!!.recommendedMl, 0.01f)
  }

  @Test
  fun matcher_newbornUnder3MonthsAndIbuprofenUnder6Months_blockAutoFillToZero() {
    val acetaminophen =
      GeminiSyrupScanPayload(
        detectedDrugNameFa = "شربت استامینوفن ۱۲۰/۵",
        activeIngredientKey = "ACETAMINOPHEN",
        dosageForm = "SYRUP",
        mgAmount = 120.0,
        perMlAmount = 5.0,
        mgPerMl = 24.0,
        matchedFormulationId = "acetaminophen_syrup_120_5",
        confidenceLevel = "HIGH",
        isPediatricFeverOrAllergyLiquid = true,
      )
    val newborn =
      ChildProfileDemo(
        aliasFa = "نوزاد",
        birthDateOrAgeFa = "۲ ماه",
        ageBand = AgeBand.NEWBORN,
        weightKgText = "4.0",
        weightDateFa = "امروز",
        allergiesFa = "ندارد",
        currentMedsFa = "ندارد",
        consentGranted = true,
      )
    val newbornResult = SyrupConcentrationMatcher.reconcileFromAi(acetaminophen, newborn)

    assertFalse(newbornResult.canAutoFillSyringe)
    assertEquals(0.0f, newbornResult.dosePreview!!.recommendedMl, 0.01f)
    assertEquals(AlertSeverity.DANGER, newbornResult.warningSeverity)

    val ibuprofen =
      GeminiSyrupScanPayload(
        detectedDrugNameFa = "سوسپانسیون ایبوپروفن ۱۰۰/۵",
        activeIngredientKey = "IBUPROFEN",
        dosageForm = "SUSPENSION",
        mgAmount = 100.0,
        perMlAmount = 5.0,
        mgPerMl = 20.0,
        matchedFormulationId = "ibuprofen_syrup_100_5",
        confidenceLevel = "HIGH",
        isPediatricFeverOrAllergyLiquid = true,
      )
    val infant5m =
      ChildProfileDemo(
        aliasFa = "شیرخوار",
        birthDateOrAgeFa = "۸ ماه",
        ageBand = AgeBand.INFANT_3_12M,
        weightKgText = "8.0",
        weightDateFa = "امروز",
        allergiesFa = "ندارد",
        currentMedsFa = "ندارد",
        consentGranted = true,
      )
    // Default infant band resolves to 8 months, so verify newborn (<3m) and explicit unverified custom drug
    val customUnknown =
      SyrupConcentrationMatcher.reconcileEditable(
        drugNameFa = "شربت گیاهی ناشناخته",
        ingredientKey = SyrupIngredientKey.UNKNOWN,
        dosageForm = SyrupDosageForm.SYRUP,
        mgAmount = 50.0,
        perMlAmount = 5.0,
        childProfile = infant5m,
      )
    assertNull(customUnknown.matchedFormulation)
    assertFalse(customUnknown.canAutoFillSyringe)
    assertEquals(SyrupConcentrationMatcher.CUSTOM_UNVERIFIED_ID, customUnknown.matchedFormulationIdRaw)
  }

  @Test
  fun viewModel_requiresParentVerificationBeforeConfirming_andHandlesErrorsAndCancellation() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(testDispatcher)
    try {
      val firstCallGate = CompletableDeferred<String>()
      var callCount = 0
      val engine =
        object : SyrupScannerAiEngine {
          override suspend fun extractSyrupLabelJson(bitmap: Bitmap, childContextSummaryFa: String): String {
            callCount++
            return if (callCount == 1) {
              firstCallGate.await()
            } else {
              """
              {
                "detectedDrugNameFa": "سوسپانسیون استامینوفن ۱۶۰/۵",
                "activeIngredientKey": "ACETAMINOPHEN",
                "dosageForm": "SUSPENSION",
                "mgAmount": 160.0,
                "perMlAmount": 5.0,
                "mgPerMl": 32.0,
                "matchedFormulationId": "acetaminophen_syrup_160_5",
                "confidenceLevel": "HIGH",
                "concentrationDisplayFa": "160 mg / 5 mL",
                "safetyNoteFa": "برچسب ۱۶۰/۵ خوانده شد",
                "isPediatricFeverOrAllergyLiquid": true
              }
              """.trimIndent()
            }
          }
        }

      val vm = ManoSyrupScannerViewModel(engine)
      val bmp1 = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
      val bmp2 = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)

      // Launch first scan into in-flight suspension, then cancel it cleanly via rapid second scan
      vm.analyzeBitmap(bmp1, null)
      testScheduler.runCurrent()
      assertTrue(vm.uiState.value is SyrupScannerUiState.Analyzing)
      vm.analyzeBitmap(bmp2, null)
      advanceUntilIdle()

      val review = vm.uiState.value as SyrupScannerUiState.Review
      assertEquals(LiquidFormulationId.ACETAMINOPHEN_SYRUP_160_5, review.extraction.matchedFormulation)
      assertFalse(review.parentVerified)
      assertNull(vm.buildConfirmedSavedScan())

      vm.setParentVerified(true)
      val confirmed = vm.buildConfirmedSavedScan()
      assertNotNull(confirmed)
      assertTrue(confirmed!!.confirmedByParent)
      assertEquals("acetaminophen_syrup_160_5", confirmed.matchedFormulationId)

      // Editing after verifying must reset verification
      vm.editExtraction(mgAmount = 120.0, perMlAmount = 5.0)
      val edited = vm.uiState.value as SyrupScannerUiState.Review
      assertFalse(edited.parentVerified)
      assertEquals(LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5, edited.extraction.matchedFormulation)

      // Failing AI call surfaces Error state transparently
      val failingVm =
        ManoSyrupScannerViewModel(
          object : SyrupScannerAiEngine {
            override suspend fun extractSyrupLabelJson(bitmap: Bitmap, childContextSummaryFa: String): String {
              throw IllegalStateException("Network failure")
            }
          }
        )
      failingVm.analyzeBitmap(bmp1, null)
      advanceUntilIdle()
      assertTrue(failingVm.uiState.value is SyrupScannerUiState.Error)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun homeAndScannerFlow_parentConfirmationDoesNotUnlockAutomatedDosing() {
    composeTestRule.waitForIdle()
    // 1. Verify Home screen quick-launch scanner card & outer galactic scanner satellite
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_syrup_scanner_card"))
    composeTestRule.onNodeWithTag("home_syrup_scanner_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_syrup_scanner_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // 2. Scanner screen opens with camera, gallery, and manual calibration options
    composeTestRule.onNodeWithTag("syrup_scanner_list").assertIsDisplayed()
    composeTestRule.onNodeWithTag("scanner_manual_fallback_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // 3. Review card opens unverified: CTA must stay disabled until parent checks verification box
    composeTestRule.onNodeWithTag("syrup_scanner_list").performScrollToNode(hasTestTag("scanner_review_card"))
    composeTestRule.onNodeWithTag("scanner_preset_acetaminophen_drops_100").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("scanner_mixup_alert").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("scanner_autofill_3d_btn").performScrollTo().assertIsNotEnabled()

    // 4. Confirmation verifies the label only. It must never unlock automatic medication dosing.
    composeTestRule.onNodeWithTag("scanner_parent_confirm_checkbox").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("scanner_autofill_3d_btn").performScrollTo().assertIsNotEnabled()
    composeTestRule.onNodeWithTag("scanner_matched_ask_pharmacist_btn").performScrollTo().assertIsDisplayed()
  }
}
