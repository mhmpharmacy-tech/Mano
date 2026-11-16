package com.example

import android.app.Application
import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.ManoSourcesAndClinicalSeed
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.util.DoctorVisitPromptBuilder
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineTempPoint
import com.example.ui.components.FeverDoseTimelineCard
import com.example.ui.components.VisitDraftStudioContent
import com.example.ui.theme.ManoKidsTheme
import com.example.ui.util.CachedVisitPdf
import com.example.ui.util.VisitDraftPdfHelper
import com.example.ui.util.VisitPdfDocumentData
import com.example.ui.viewmodel.ManoVisitDraftViewModel
import com.example.ui.viewmodel.SymptomWizardState
import com.example.ui.viewmodel.VISIT_DRAFT_GEMINI_MODEL
import com.example.ui.viewmodel.VisitDraftAiEngine
import com.example.ui.viewmodel.VisitDraftPdfGenerator
import com.example.ui.viewmodel.VisitDraftPdfState
import com.example.ui.viewmodel.VisitDraftUiState
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ManoVisitDraftTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun promptBuilder_includesTimelineTempOver39DosesSymptomsAndParentNote() {
    val now = 1_750_000_000_000L
    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "t_high",
              tempCelsius = 39.4f,
              timestampMs = now - 1_800_000L,
              timeLabelFa = "16:20",
              siteFa = "زیربغلی",
              caregiverFa = "مادر",
              noteFa = "لرز خفیف",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs =
          listOf(
            TimelineDosePoint(
              id = "d_acet",
              medicationSlug = "acetaminophen-pediatric",
              medicationNameFa = "استامینوفن کودکان",
              dosageFormFa = "شربت ۱۲۰/۵",
              volumeMlText = "5.0 mL",
              timestampMs = now - 900_000L,
              givenAtLabelFa = "16:35",
              noteFa = "پس از تب",
              isSyncedCloud = false,
            )
          ),
        nowMs = now,
      )
    val feverFlow = ManoSourcesAndClinicalSeed.symptomFlows.first { it.id == "fever" }
    val firstRedFlag = feverFlow.redFlags.first()
    val wizard =
      SymptomWizardState(
        step = 2,
        selectedSymptomId = "fever",
        selectedAgeBand = AgeBand.TODDLER_1_3Y,
        temperatureText = "39.4",
        durationHoursText = "10",
        selectedRedFlagIds = setOf(firstRedFlag.id),
      )
    val child =
      ChildProfileDemo(
        aliasFa = "پارسا",
        birthDateOrAgeFa = "۲ سال و ۳ ماه",
        ageBand = AgeBand.TODDLER_1_3Y,
        weightKgText = "13.0",
        weightDateFa = "امروز",
        allergiesFa = "پنی‌سیلین",
        currentMedsFa = "ندارد",
        consentGranted = true,
      )

    val bundle =
      DoctorVisitPromptBuilder.buildContext(
        snapshot = snapshot,
        childProfile = child,
        selectedSymptomFlow = feverFlow,
        symptomWizard = wizard,
        parentNoteFa = "آیا نیاز به آزمایش ادرار است؟",
        nowMs = now,
      )

    assertTrue(bundle.defaultTitleFa.contains("پارسا"))
    assertTrue(bundle.timelineSummaryFa.contains("39.4°C"))
    assertTrue(bundle.symptomContextFa.contains(firstRedFlag.labelFa))
    assertTrue(bundle.promptText.contains("پارسا"))
    assertTrue(bundle.promptText.contains("پنی‌سیلین"))
    assertTrue(bundle.promptText.contains("39.4°C"))
    assertTrue(bundle.promptText.contains("استامینوفن کودکان"))
    assertTrue(bundle.promptText.contains("5.0 mL"))
    assertTrue(bundle.promptText.contains("آیا نیاز به آزمایش ادرار است؟"))
    assertTrue(bundle.promptText.contains("هیچ تشخیص قطعی پزشکی ندهید"))
  }

  @Test
  fun visitDraftViewModel_rejectsEmptyContextGeneratesSavesEditsAndSurfacesFailures() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
      var failNext = false
      val vm =
        ManoVisitDraftViewModel(
          engine =
            object : VisitDraftAiEngine {
              override fun generateDraftStream(prompt: String): Flow<String> = flow {
                if (failNext) throw IllegalStateException("AppCheck HTTP 403")
                emit("خلاصه وضعیت کودک:\n")
                emit("• اوج تب ۳۹.۱ درجه ثبت شده است.")
              }
            }
        )

      val emptySnapshot =
        FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
      var persistCalled = false
      vm.generateAndSaveVisitDraft(
        snapshot = emptySnapshot,
        childProfile = null,
        selectedSymptomFlow = null,
        symptomWizard = SymptomWizardState(),
        parentNoteFa = "",
        isCloudSignedIn = false,
        onPersistNewDraft = { _, _, _, _, _, _, _ -> persistCalled = true },
      )
      assertTrue(vm.uiState.value is VisitDraftUiState.Error)
      assertFalse(persistCalled)

      // Valid timeline triggers streaming + auto-save
      val now = System.currentTimeMillis()
      val populatedSnapshot =
        FeverDoseTimelineEngine.buildSnapshot(
          cloudFeverLogs = emptyList(),
          localFeverLogs =
            listOf(
              TimelineTempPoint(
                id = "tf_vm",
                tempCelsius = 39.1f,
                timestampMs = now - 60_000L,
                timeLabelFa = "17:00",
                siteFa = "زیربغلی",
                caregiverFa = "مادر",
                noteFa = "",
                isSyncedCloud = false,
              )
            ),
          cloudDoseLogs = emptyList(),
          localDoseLogs = emptyList(),
          nowMs = now,
        )

      var savedModel = ""
      var savedText = ""
      vm.generateAndSaveVisitDraft(
        snapshot = populatedSnapshot,
        childProfile = null,
        selectedSymptomFlow = ManoSourcesAndClinicalSeed.symptomFlows.first(),
        symptomWizard = SymptomWizardState(),
        parentNoteFa = "کاهش اشتها",
        isCloudSignedIn = false,
        onPersistNewDraft = { _, _, _, draftTextFa, _, modelName, onSavedId ->
          savedModel = modelName
          savedText = draftTextFa
          persistCalled = true
          onSavedId("draft_saved_1")
        },
      )
      advanceUntilIdle()

      assertTrue(persistCalled)
      assertEquals(VISIT_DRAFT_GEMINI_MODEL, savedModel)
      assertTrue(savedText.contains("۳۹.۱"))
      val ready = vm.uiState.value as VisitDraftUiState.Ready
      assertTrue(ready.isSaved)
      assertEquals("draft_saved_1", ready.draftId)

      // Editing marks draft unsaved until re-saved
      vm.editActiveDraft(draftTextFa = "متن ویرایش‌شده توسط والد")
      val edited = vm.uiState.value as VisitDraftUiState.Ready
      assertFalse(edited.isSaved)
      assertEquals("متن ویرایش‌شده توسط والد", edited.draftTextFa)

      // Real failure surfaces transparently to UI state
      failNext = true
      vm.generateAndSaveVisitDraft(
        snapshot = populatedSnapshot,
        childProfile = null,
        selectedSymptomFlow = ManoSourcesAndClinicalSeed.symptomFlows.first(),
        symptomWizard = SymptomWizardState(),
        parentNoteFa = "",
        isCloudSignedIn = false,
        onPersistNewDraft = { _, _, _, _, _, _, _ -> },
      )
      advanceUntilIdle()
      val err = vm.uiState.value as VisitDraftUiState.Error
      assertTrue(err.messageFa.contains("AppCheck HTTP 403"))
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun visitDraftStudioContent_generatesSavesDisplaysHistoryAndDeletesDraft() {
    val now = System.currentTimeMillis()
    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "tf_ui",
              tempCelsius = 38.7f,
              timestampMs = now - 600_000L,
              timeLabelFa = "18:10",
              siteFa = "زیربغلی",
              caregiverFa = "مادر",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs = emptyList(),
        nowMs = now,
      )
    val feverFlow = ManoSourcesAndClinicalSeed.symptomFlows.first { it.id == "fever" }

    var deletedId: String? = null
    composeTestRule.setContent {
      ManoKidsTheme {
        var drafts by remember { mutableStateOf(emptyList<DoctorVisitDraft>()) }
        val vm = remember {
          ManoVisitDraftViewModel(
            engine =
              object : VisitDraftAiEngine {
                override fun generateDraftStream(prompt: String): Flow<String> =
                  flowOf("خلاصه ویزیت آماده برای پزشک اطفال\n• دمای ۳۸.۷ درجه ثبت شده است.")
              }
          )
        }
        val collectedState = vm.uiState.collectAsState().value
        VisitDraftStudioContent(
          draftUiState = collectedState,
          savedDrafts = drafts,
          snapshot = snapshot,
          childProfile = null,
          selectedSymptomFlow = feverFlow,
          symptomWizard = SymptomWizardState(),
          isCloudSignedIn = false,
          onGenerateAndSave = { note ->
            vm.generateAndSaveVisitDraft(
              snapshot = snapshot,
              childProfile = null,
              selectedSymptomFlow = feverFlow,
              symptomWizard = SymptomWizardState(),
              parentNoteFa = note,
              isCloudSignedIn = false,
              onPersistNewDraft = { titleFa, symptomCtx, timelineSum, draftText, noteFa, modelName, onSavedId ->
                val newDraft =
                  DoctorVisitDraft(
                    id = "d_ui_1",
                    titleFa = titleFa,
                    symptomContextFa = symptomCtx,
                    timelineSummaryFa = timelineSum,
                    draftTextFa = draftText,
                    parentNoteFa = noteFa,
                    modelName = modelName,
                    createdAtMs = now,
                    createdAtLabelFa = "18:15",
                    isSyncedCloud = false,
                  )
                drafts = listOf(newDraft) + drafts
                onSavedId(newDraft.id)
              },
            )
          },
          onEditDraft = { t, b -> vm.editActiveDraft(titleFa = t, draftTextFa = b) },
          onSaveEdits = {},
          onLoadDraft = { d -> vm.loadSavedDraft(d) },
          onDeleteDraft = { d ->
            deletedId = d.id
            drafts = drafts.filterNot { it.id == d.id }
          },
          onFeedback = {},
          modifier = Modifier.verticalScroll(rememberScrollState()),
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("visit_draft_studio_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("visit_draft_parent_note_input").performTextReplacement("سوال درباره تغذیه")
    composeTestRule.onNodeWithTag("generate_save_visit_draft_btn").performClick()

    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodesWithTag("visit_draft_editor_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("visit_draft_save_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("visit_draft_copy_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("visit_draft_share_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("saved_visit_draft_item_d_ui_1").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("delete_visit_draft_d_ui_1").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("d_ui_1", deletedId)
  }

  @Test
  fun feverDoseTimelineCard_displaysGeminiVisitDraftEntryButton() {
    var clickedDraft = false
    val now = System.currentTimeMillis()
    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), now)

    composeTestRule.setContent {
      ManoKidsTheme {
        androidx.compose.foundation.layout.Column(
          modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
          FeverDoseTimelineCard(
            snapshot = snapshot,
            savedVisitDraftsCount = 2,
            onSaveTempLog = { _, _, _, _, _, onDone -> onDone() },
            onDeleteTempLog = { _, _ -> },
            onOpenVisitDraftStudio = { clickedDraft = true },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("timeline_visit_draft_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("2 پیش‌نویس ذخیره‌شده", substring = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("open_visit_draft_studio_btn").performScrollTo().performClick()
    assertTrue(clickedDraft)
  }

  @Test
  fun pdfHelper_generatesCachedMultiPagePdfInCacheDirAndPrunesOldFiles() = runTest {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val cacheDir = VisitDraftPdfHelper.cacheDirectory(context)
    cacheDir.listFiles()?.forEach { it.delete() }

    val now = 1_750_000_000_000L
    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "t_pdf_1",
              tempCelsius = 39.3f,
              timestampMs = now - 1_200_000L,
              timeLabelFa = "15:10",
              siteFa = "زیربغلی",
              caregiverFa = "مادر",
              noteFa = "تب ناگهانی",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs =
          listOf(
            TimelineDosePoint(
              id = "d_pdf_1",
              medicationSlug = "acetaminophen-pediatric",
              medicationNameFa = "استامینوفن کودکان",
              dosageFormFa = "شربت ۱۲۰/۵",
              volumeMlText = "5.0 mL",
              timestampMs = now - 600_000L,
              givenAtLabelFa = "15:25",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        nowMs = now,
      )
    val child =
      ChildProfileDemo(
        aliasFa = "کیان",
        birthDateOrAgeFa = "۳ سال",
        ageBand = AgeBand.TODDLER_1_3Y,
        weightKgText = "14.5",
        weightDateFa = "امروز",
        allergiesFa = "ندارد",
        currentMedsFa = "ندارد",
        consentGranted = true,
      )

    val longGeminiBody =
      (1..45).joinToString("\n") { line ->
        "بند شماره $line: روند دمای کودک در ۲۴ ساعت گذشته با ثبت اوج ۳۹.۳ درجه و مصرف استامینوفن ۵ میلی‌لیتر ثبت شده است."
      }
    val docData =
      VisitDraftPdfHelper.buildDocumentData(
        titleFa = "پیش‌نویس فوری ویزیت (کیان)",
        draftTextFa = longGeminiBody,
        symptomContextFa = "علامت کانونی: تب • سن: ۳ سال",
        timelineSummaryFa = "۱ ثبت دما (اوج ۳۹.۳°C) • ⚠️ هشدار تب بالای ۳۹ درجه",
        parentNoteFa = "سوال درباره نیاز به آزمایش خون",
        childProfile = child,
        snapshot = snapshot,
        modelName = VISIT_DRAFT_GEMINI_MODEL,
        nowMs = now,
      )

    assertTrue(docData.hasHighFeverAlert)
    assertTrue(docData.childSummaryLineFa.contains("کیان"))
    assertEquals(1, docData.tempLogLinesFa.size)
    assertEquals(1, docData.doseLogLinesFa.size)

    val firstResult = VisitDraftPdfHelper.generatePdfInCache(context, docData, nowMs = now, maxCachedFiles = 2)
    assertTrue("PDF generation failed: ${firstResult.exceptionOrNull()?.stackTraceToString()}", firstResult.isSuccess)
    val cached1 = firstResult.getOrThrow()
    assertTrue(cached1.file.exists())
    assertTrue(cached1.sizeBytes > 0L)
    assertTrue(cached1.file.readText(Charsets.ISO_8859_1).startsWith("%PDF-1.4"))
    assertTrue(cached1.file.parentFile?.absolutePath == cacheDir.absolutePath)
    assertTrue(cached1.pageCount >= 2)

    val secondResult = VisitDraftPdfHelper.generatePdfInCache(context, docData, nowMs = now + 2_000L, maxCachedFiles = 2)
    val thirdResult = VisitDraftPdfHelper.generatePdfInCache(context, docData, nowMs = now + 4_000L, maxCachedFiles = 2)
    assertTrue(secondResult.isSuccess)
    assertTrue(thirdResult.isSuccess)

    val remainingFiles = cacheDir.listFiles { file -> file.extension == "pdf" }?.toList().orEmpty()
    assertTrue(remainingFiles.size <= 2)
  }

  @Test
  fun visitDraftViewModel_exportsPdfToCacheResetsOnEditAndSurfacesErrors() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    val context = ApplicationProvider.getApplicationContext<Application>()
    try {
      var failPdf = false
      var generateCalls = 0
      val fakeTempFile = File(context.cacheDir, "visit_draft_pdfs/fake_test_visit.pdf").apply {
        parentFile?.mkdirs()
        writeText("%PDF-1.4\n%fake\n")
      }
      val vm =
        ManoVisitDraftViewModel(
          engine =
            object : VisitDraftAiEngine {
              override fun generateDraftStream(prompt: String): Flow<String> =
                flowOf("متن آماده برای ویزیت پزشک")
            },
          pdfGenerator =
            object : VisitDraftPdfGenerator {
              override suspend fun generatePdfInCache(
                context: Context,
                data: VisitPdfDocumentData,
              ): Result<CachedVisitPdf> {
                generateCalls += 1
                if (failPdf) return Result.failure(IllegalStateException("Disk cache full"))
                return Result.success(
                  CachedVisitPdf(
                    file = fakeTempFile,
                    contentUri = null,
                    fileName = "fake_test_visit.pdf",
                    sizeBytes = 2048L,
                    pageCount = 1,
                    createdAtLabelFa = "2026/11/15 - 12:00",
                  )
                )
              }
            },
        )

      // Exporting before draft ready yields transparent error
      vm.exportActiveDraftToCachePdf(context, childProfile = null, snapshot = null)
      assertTrue(vm.pdfState.value is VisitDraftPdfState.Error)

      vm.loadSavedDraft(
        DoctorVisitDraft(
          id = "d_pdf_vm",
          titleFa = "پیش‌نویس تستی",
          symptomContextFa = "تب",
          timelineSummaryFa = "۱ ثبت دما",
          draftTextFa = "گزارش آماده پزشک",
          parentNoteFa = "",
          modelName = VISIT_DRAFT_GEMINI_MODEL,
          createdAtMs = 1_000L,
          createdAtLabelFa = "12:00",
          isSyncedCloud = false,
        )
      )
      assertTrue(vm.pdfState.value is VisitDraftPdfState.Idle)

      var readyCallbackFile: String? = null
      vm.exportActiveDraftToCachePdf(context, childProfile = null, snapshot = null) { cached ->
        readyCallbackFile = cached.fileName
      }
      advanceUntilIdle()
      assertEquals(1, generateCalls)
      assertEquals("fake_test_visit.pdf", readyCallbackFile)
      assertTrue(vm.pdfState.value is VisitDraftPdfState.Ready)

      // Calling without forceRegenerate reuses cached file without regenerating
      vm.exportActiveDraftToCachePdf(context, childProfile = null, snapshot = null)
      advanceUntilIdle()
      assertEquals(1, generateCalls)

      // Editing draft invalidates stale cached PDF state
      vm.editActiveDraft(draftTextFa = "متن اصلاح‌شده")
      assertTrue(vm.pdfState.value is VisitDraftPdfState.Idle)

      // Real failure surfaces transparently
      failPdf = true
      vm.exportActiveDraftToCachePdf(context, childProfile = null, snapshot = null, forceRegenerate = true)
      advanceUntilIdle()
      val err = vm.pdfState.value as VisitDraftPdfState.Error
      assertTrue(err.messageFa.contains("Disk cache full"))
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun visitDraftStudioContent_displaysPdfActionsAndShowsCachedBanner() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val now = System.currentTimeMillis()
    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), now)
    val savedDraft =
      DoctorVisitDraft(
        id = "d_ui_pdf",
        titleFa = "پیش‌نویس ویزیت رها",
        symptomContextFa = "تب",
        timelineSummaryFa = "۳۸.۸ درجه",
        draftTextFa = "خلاصه آماده برای چاپ",
        parentNoteFa = "",
        modelName = VISIT_DRAFT_GEMINI_MODEL,
        createdAtMs = now,
        createdAtLabelFa = "14:00",
        isSyncedCloud = false,
      )
    val fakeCachedFile = File(context.cacheDir, "visit_draft_pdfs/mano_ui_test.pdf").apply {
      parentFile?.mkdirs()
      writeText("%PDF-1.4")
    }
    var printTriggered = false
    var shareTriggered = false

    composeTestRule.setContent {
      ManoKidsTheme {
        val vm = remember {
          ManoVisitDraftViewModel(
            engine =
              object : VisitDraftAiEngine {
                override fun generateDraftStream(prompt: String): Flow<String> = flowOf("x")
              },
            pdfGenerator =
              object : VisitDraftPdfGenerator {
                override suspend fun generatePdfInCache(
                  context: Context,
                  data: VisitPdfDocumentData,
                ): Result<CachedVisitPdf> =
                  Result.success(
                    CachedVisitPdf(
                      file = fakeCachedFile,
                      contentUri = null,
                      fileName = "mano_ui_test.pdf",
                      sizeBytes = 4096L,
                      pageCount = 1,
                      createdAtLabelFa = "14:05",
                    )
                  )
              },
          ).apply { loadSavedDraft(savedDraft) }
        }
        val uiState = vm.uiState.collectAsState().value
        val pdfState = vm.pdfState.collectAsState().value
        VisitDraftStudioContent(
          draftUiState = uiState,
          pdfState = pdfState,
          savedDrafts = listOf(savedDraft),
          snapshot = snapshot,
          childProfile = null,
          selectedSymptomFlow = null,
          symptomWizard = SymptomWizardState(),
          isCloudSignedIn = false,
          onGenerateAndSave = {},
          onEditDraft = { t, b -> vm.editActiveDraft(titleFa = t, draftTextFa = b) },
          onSaveEdits = {},
          onSavePdfToCache = { vm.exportActiveDraftToCachePdf(context, null, snapshot, forceRegenerate = true) },
          onPrintPdf = { printTriggered = true },
          onSharePdf = { shareTriggered = true },
          onLoadDraft = { vm.loadSavedDraft(it) },
          onDeleteDraft = {},
          onFeedback = {},
          modifier = Modifier.verticalScroll(rememberScrollState()),
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("visit_draft_pdf_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("visit_draft_save_pdf_btn").performScrollTo().performClick()

    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodesWithTag("visit_draft_pdf_cached_banner").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("visit_draft_pdf_cached_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("mano_ui_test.pdf", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("visit_draft_open_pdf_btn").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("visit_draft_print_pdf_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("visit_draft_share_pdf_btn").performScrollTo().performClick()
    assertTrue(printTriggered)
    assertTrue(shareTriggered)
  }

  @Test
  fun pdfHelper_sanitizesMarkdownTokensIntoCleanReadableText() {
    val raw =
      """
      ## خلاصه ویزیت
      **نکته مهم:** تب `۳۹.۱` درجه ثبت شد.
      * مورد اول *تاکید*
      - مورد دوم
      """.trimIndent()

    val cleaned = VisitDraftPdfHelper.sanitizeMarkdownForPdf(raw)
    assertFalse(cleaned.contains("##"))
    assertFalse(cleaned.contains("**"))
    assertFalse(cleaned.contains("`"))
    assertTrue(cleaned.contains("• مورد اول تاکید"))
    assertTrue(cleaned.contains("• مورد دوم"))

    val docData =
      VisitDraftPdfHelper.buildDocumentData(
        titleFa = "گزارش تستی",
        draftTextFa = raw,
        symptomContextFa = "تب",
        timelineSummaryFa = "۱ ثبت",
        parentNoteFa = "**سوال:** آیا دارو نیاز است؟",
        childProfile = null,
        snapshot = null,
        modelName = VISIT_DRAFT_GEMINI_MODEL,
      )
    assertFalse(docData.draftBodyFa.contains("##"))
    assertFalse(docData.draftBodyFa.contains("**"))
    assertFalse(docData.parentNoteFa.contains("**"))
  }

  @Test
  fun visitDraftViewModel_rapidSecondGenerationCancelsFirstInFlightRequest() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
      val vm =
        ManoVisitDraftViewModel(
          engine =
            object : VisitDraftAiEngine {
              override fun generateDraftStream(prompt: String): Flow<String> = flow {
                if (prompt.contains("اول")) {
                  kotlinx.coroutines.delay(5_000L)
                  emit("پاسخ قدیمی اول")
                } else {
                  kotlinx.coroutines.delay(50L)
                  emit("پاسخ نهایی دوم")
                }
              }
            }
        )
      val snapshot =
        FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
      val persistedTexts = mutableListOf<String>()

      vm.generateAndSaveVisitDraft(
        snapshot = snapshot,
        childProfile = null,
        selectedSymptomFlow = null,
        symptomWizard = SymptomWizardState(),
        parentNoteFa = "اول",
        isCloudSignedIn = false,
        onPersistNewDraft = { _, _, _, body, _, _, onSaved ->
          persistedTexts.add(body)
          onSaved("d_1")
        },
      )
      vm.generateAndSaveVisitDraft(
        snapshot = snapshot,
        childProfile = null,
        selectedSymptomFlow = null,
        symptomWizard = SymptomWizardState(),
        parentNoteFa = "دوم",
        isCloudSignedIn = false,
        onPersistNewDraft = { _, _, _, body, _, _, onSaved ->
          persistedTexts.add(body)
          onSaved("d_2")
        },
      )
      advanceUntilIdle()

      assertEquals(listOf("پاسخ نهایی دوم"), persistedTexts)
      val ready = vm.uiState.value as VisitDraftUiState.Ready
      assertEquals("پاسخ نهایی دوم", ready.draftTextFa)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun feverDoseTimelineCard_rendersInDarkModeWithoutCrash() {
    val snapshot = FeverDoseTimelineEngine.buildSnapshot(emptyList(), emptyList(), emptyList(), emptyList())
    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column(
          modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
          FeverDoseTimelineCard(
            snapshot = snapshot,
            savedVisitDraftsCount = 1,
            onSaveTempLog = { _, _, _, _, _, onDone -> onDone() },
            onDeleteTempLog = { _, _ -> },
            onOpenVisitDraftStudio = {},
          )
        }
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("timeline_visit_draft_banner").performScrollTo().assertIsDisplayed()
  }
}
