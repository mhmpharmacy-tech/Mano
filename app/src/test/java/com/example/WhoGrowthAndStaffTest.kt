package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.data.ConsultationPayloadMapper
import com.example.data.ManoArticlesSeed
import com.example.data.ManoFaqSeed
import com.example.data.ManoMedicationSeed
import com.example.data.ManoSourcesAndClinicalSeed
import com.example.domain.model.AgeBand
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.ReviewStatus
import com.example.domain.model.StaffAccessStatus
import com.example.domain.model.StaffConsultationCase
import com.example.domain.util.ChildSex
import com.example.domain.util.GrowthMetric
import com.example.domain.util.WhoGrowthStandardsEngine
import com.example.ui.components.ManoSquadSection
import com.example.ui.components.WhoGrowthChartCard
import com.example.ui.screens.AskPharmacistScreen
import com.example.ui.screens.SyrupScannerScreen
import com.example.ui.theme.ManoKidsTheme
import com.example.ui.viewmodel.AskPharmacistState
import com.example.ui.viewmodel.SyrupScannerUiState
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
@Config(sdk = [36])
class WhoGrowthAndStaffTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun whoGrowthEngine_calculatesAccuratePercentilesAndCurveOrdering() {
    // WHO reference boy 12 months median weight = ~9.65 kg
    val boy12mMedian =
      WhoGrowthStandardsEngine.evaluate(
        metric = GrowthMetric.WEIGHT_FOR_AGE,
        sex = ChildSex.BOY,
        ageMonths = 12.0,
        measuredValue = 9.65,
      )
    assertNotNull(boy12mMedian)
    assertTrue("Expected ~50th percentile, got ${boy12mMedian!!.percentile}", boy12mMedian.percentile in 47.0..53.0)
    assertFalse(boy12mMedian.needsPediatricReview)

    // Girl 24 months below P3 (~9.0 kg vs P3 ~9.2 kg) triggers pediatric follow-up recommendation
    val girl24mLow =
      WhoGrowthStandardsEngine.evaluate(
        metric = GrowthMetric.WEIGHT_FOR_AGE,
        sex = ChildSex.GIRL,
        ageMonths = 24.0,
        measuredValue = 8.5,
      )
    assertNotNull(girl24mLow)
    assertTrue(girl24mLow!!.percentile < 3.0)
    assertTrue(girl24mLow.needsPediatricReview)

    // Height curves P3 < P15 < P50 < P85 < P97 at every sampled point
    val curves = WhoGrowthStandardsEngine.buildCurves(GrowthMetric.HEIGHT_FOR_AGE, ChildSex.GIRL)
    assertTrue(curves.agesMonths.size > 15)
    for (i in curves.agesMonths.indices) {
      assertTrue(curves.p3[i] < curves.p15[i])
      assertTrue(curves.p15[i] < curves.p50[i])
      assertTrue(curves.p50[i] < curves.p85[i])
      assertTrue(curves.p85[i] < curves.p97[i])
    }
  }

  @Test
  fun clinicalSeeds_doNotClaimUnverifiedHumanApproval() {
    assertTrue(ManoArticlesSeed.starterArticles.all { it.review.status == ReviewStatus.NEEDS_CLINICAL_REVIEW })
    assertTrue(ManoFaqSeed.faqs.all { it.review.status == ReviewStatus.NEEDS_CLINICAL_REVIEW })
    assertTrue(ManoMedicationSeed.monographs.all { it.review.status == ReviewStatus.NEEDS_CLINICAL_REVIEW })
    assertTrue(ManoSourcesAndClinicalSeed.symptomFlows.all { it.review.status == ReviewStatus.NEEDS_CLINICAL_REVIEW })
    assertTrue(ManoMedicationSeed.monographs.all { it.review.reviewerIdentity == null })
  }

  @Test
  fun staffPayloadMapper_enforcesAttestationAndParsesInbox() {
    val rawInbox =
      listOf(
        mapOf(
          "caseId" to "case_staff_1",
          "pharmacyId" to "pharm_central",
          "categoryName" to "OTC",
          "ageBandId" to "toddler_1_3y",
          "symptomFlowId" to "otc_question",
          "questionSummaryFa" to "آیا قطره سدیم کلراید برای گرفتگی بینی مناسب است؟",
          "status" to "RECEIVED",
          "createdAt" to 1_750_000_000_000L,
          "internalNotes" to
            listOf(
              mapOf(
                "noteId" to "n1",
                "authorUid" to "uid_pharm",
                "noteText" to "بررسی غلظت محلول شستشو",
                "fromStatus" to "PENDING",
                "toStatus" to "RECEIVED",
                "createdAt" to 1_750_000_100_000L,
              )
            ),
        )
      )

    val parsed = ConsultationPayloadMapper.parsePharmacyInboxList(rawInbox)
    assertEquals(1, parsed.size)
    assertEquals("case_staff_1", parsed[0].caseId)
    assertEquals(ConsultationStatus.RECEIVED, parsed[0].status)
    assertEquals(1, parsed[0].internalNotes.size)

    // Reject REVIEWED without attestation
    val missingAttestation =
      ConsultationPayloadMapper.buildReviewCasePayload(
        pharmacyId = "pharm_central",
        caseId = "case_staff_1",
        nextStatus = ConsultationStatus.REVIEWED,
        internalNote = "نکته داخلی",
        parentReply = "پاسخ علمی کامل غیرتشخیصی برای والد کودک",
        pharmacistAttestation = false,
      )
    assertTrue(missingAttestation.isFailure)

    // Reject parentReply during RECEIVED
    val replyTooEarly =
      ConsultationPayloadMapper.buildReviewCasePayload(
        pharmacyId = "pharm_central",
        caseId = "case_staff_1",
        nextStatus = ConsultationStatus.RECEIVED,
        internalNote = "تحویل شد",
        parentReply = "پاسخ زودهنگام",
        pharmacistAttestation = false,
      )
    assertTrue(replyTooEarly.isFailure)

    // Valid REVIEWED payload
    val validReview =
      ConsultationPayloadMapper.buildReviewCasePayload(
        pharmacyId = "pharm_central",
        caseId = "case_staff_1",
        nextStatus = ConsultationStatus.REVIEWED,
        internalNote = "ارجاع غیردارویی",
        parentReply = "محلول نرمال سالین ۰.۹٪ و تمیز کردن ملایم بینی با نظر داروساز مناسب است.",
        pharmacistAttestation = true,
      )
    assertTrue(validReview.isSuccess)
    assertEquals("REVIEWED", validReview.getOrNull()?.get("nextStatus"))
    assertEquals(true, validReview.getOrNull()?.get("pharmacistAttestation"))
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h780dp-xhdpi")
  fun whoChartAndSquadCards_renderCleanlyUnderLargeFontScale130() {
    composeTestRule.setContent {
      val baseDensity = LocalDensity.current
      CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides Density(density = baseDensity.density, fontScale = 1.3f),
      ) {
        ManoKidsTheme(darkTheme = false) {
          Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            ManoSquadSection(onSelectMember = {})
            WhoGrowthChartCard(
              childProfile = null,
              savedHeightCm = "86",
              selectedAgeBand = AgeBand.TODDLER_1_3Y,
              onOpen3DWeighingGuide = {},
            )
          }
        }
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("mano_squad_drug").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("mano_squad_connect").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("who_growth_chart_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("who_growth_canvas").assertDoesNotExist()
    composeTestRule.onNodeWithTag("who_percentile_result").assertDoesNotExist()

    // Interactive switch to Height-for-Age + Girl + Age preset 24m
    composeTestRule.onNodeWithTag("who_metric_height").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("who_sex_girl").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("who_age_preset_24").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("who_value_input").performScrollTo().performTextReplacement("86.0")
    composeTestRule.onNodeWithTag("who_length_method_confirm").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("who_percentile_result").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun askPharmacistStaffMode_switchesToStaffPanelAndRequiresAttestation() {
    val sampleCases =
      listOf(
        StaffConsultationCase(
          caseId = "c_pending",
          pharmacyId = "pharm_1",
          category = ConsultationCategory.OTC,
          childAgeBand = AgeBand.INFANT_3_12M,
          symptomFlowId = "otc_question",
          questionSummaryFa = "پرسش نمونه درباره نحوه خواندن سرنگ x2",
          status = ConsultationStatus.PENDING,
          createdAtLabelFa = "ساعت ۱۰:۰۰",
        ),
        StaffConsultationCase(
          caseId = "c_received",
          pharmacyId = "pharm_1",
          category = ConsultationCategory.SKIN,
          childAgeBand = AgeBand.NEWBORN,
          symptomFlowId = "rash",
          questionSummaryFa = "قرمزی خفیف ناحیه پوشک بدون تاول و تب",
          status = ConsultationStatus.RECEIVED,
          createdAtLabelFa = "ساعت ۱۰:۲۰",
        ),
      )

    val submittedReplies = mutableListOf<String>()

    composeTestRule.setContent {
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ManoKidsTheme(darkTheme = false) {
          AskPharmacistScreen(
            state = AskPharmacistState(),
            onSelectCategory = {},
            onSelectAgeBand = {},
            onQuestionChange = {},
            onUrgentFlagChange = {},
            onConsentChange = {},
            onSubmit = {},
            onAdvanceStatus = { _, _ -> },
            staffAccess = StaffAccessStatus(isVerifiedPharmacist = true, role = "pharmacist", pharmacyId = "pharm_1"),
            staffInboxCases = sampleCases,
            onSubmitStaffCaseReview = { _, caseId, _, reply, attestation ->
              if (attestation) submittedReplies += "$caseId:$reply"
            },
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    // Switch to Pharmacist Staff Tab
    composeTestRule.onNodeWithTag("ask_mode_staff").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ask_pharmacist_list").performScrollToNode(hasTestTag("pharmacist_staff_panel"))
    composeTestRule.onNodeWithTag("pharmacist_staff_panel").assertIsDisplayed()

    // PENDING case card shows accept button
    composeTestRule.onNodeWithTag("staff_receive_btn_c_pending").performScrollTo().assertIsDisplayed().assertIsEnabled()

    // RECEIVED case card submit stays disabled until reply >= 10 chars AND attestation checked
    composeTestRule.onNodeWithTag("staff_review_submit_btn_c_received").performScrollTo().assertIsNotEnabled()
    composeTestRule.onNodeWithTag("staff_parent_reply_c_received").performScrollTo()
      .performTextInput("پاسخ علمی غیرتشخیصی: تعویض سریع پوشک و زینک اکساید ساده بدون عطر")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("staff_review_submit_btn_c_received").assertIsNotEnabled()

    composeTestRule.onNodeWithTag("staff_attest_checkbox_c_received").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("staff_review_submit_btn_c_received").performScrollTo().assertIsEnabled().performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, submittedReplies.size)
    assertTrue(submittedReplies[0].startsWith("c_received:"))
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h780dp-xhdpi")
  fun maxSystemFontScale200_scannerAndGrowthRemainUsableOn360dp() {
    composeTestRule.setContent {
      val baseDensity = LocalDensity.current
      CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f),
      ) {
        ManoKidsTheme(darkTheme = true) {
          SyrupScannerScreen(
            scannerUiState = SyrupScannerUiState.Idle,
            savedScans = emptyList(),
            childProfile = null,
            childBirthDateEpochMs = null,
            isCloudSignedIn = false,
            onAnalyzeBitmap = {},
            onStartManualFallback = {},
            onEditDrugName = {},
            onEditIngredient = {},
            onEditDosageForm = {},
            onAdjustStrength = { _, _ -> },
            onSelectCalibratedPreset = {},
            onSetParentVerified = {},
            onRefreshChildContext = {},
            onConfirmSaveAndOpen3D = { _, _, _ -> },
            onConfirmSaveOnly = {},
            onLoadSavedScan = {},
            onDeleteSavedScan = {},
            onResetScanner = {},
            onOpenMyChildProfile = {},
            onOpenMedicinesSyringe = {},
            onOpenAskPharmacist = {},
            onFeedback = {},
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("syrup_scanner_hero_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("scanner_camera_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("scanner_gallery_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("scanner_manual_fallback_btn").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun syrupImageHelper_prunesStaleScannerCacheAndKeepsFreshFile() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val scanDir = java.io.File(context.cacheDir, "scanned_syrups").apply { mkdirs() }
    val staleFile = java.io.File(scanDir, "syrup_scan_old.jpg").apply {
      writeBytes(byteArrayOf(1, 2, 3))
      setLastModified(System.currentTimeMillis() - (2L * 60L * 60L * 1000L))
    }
    val recentFile = java.io.File(scanDir, "syrup_scan_recent.jpg").apply {
      writeBytes(byteArrayOf(4, 5, 6))
      setLastModified(System.currentTimeMillis() - 60_000L)
    }

    val newUri = com.example.ui.util.SyrupImageHelper.createTempScannerImageUri(context)
    assertTrue(newUri.toString().contains("fileprovider"))
    assertFalse("Stale cached scan photo should be deleted", staleFile.exists())
    assertTrue("Recent cached scan photo should be retained", recentFile.exists())
  }
}
