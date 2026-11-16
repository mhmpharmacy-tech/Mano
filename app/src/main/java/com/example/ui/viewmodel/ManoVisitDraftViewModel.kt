package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.SymptomFlow
import com.example.domain.util.DoctorVisitPromptBuilder
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.ui.util.CachedVisitPdf
import com.example.ui.util.VisitDraftPdfHelper
import com.example.ui.util.VisitPdfDocumentData
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.QuotaExceededException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "ManoVisitDraftVM"
const val VISIT_DRAFT_GEMINI_MODEL = "gemini-3.8-flash"
private const val VISIT_DRAFT_TIMEOUT_MS = 60_000L

interface VisitDraftAiEngine {
  fun generateDraftStream(prompt: String): Flow<String>
}

interface VisitDraftPdfGenerator {
  suspend fun generatePdfInCache(context: Context, data: VisitPdfDocumentData): Result<CachedVisitPdf>
}

class AndroidVisitDraftPdfGenerator : VisitDraftPdfGenerator {
  override suspend fun generatePdfInCache(context: Context, data: VisitPdfDocumentData): Result<CachedVisitPdf> =
    VisitDraftPdfHelper.generatePdfInCache(context = context, data = data)
}

class FirebaseVisitDraftAiEngine : VisitDraftAiEngine {
  private val model by lazy {
    Firebase.ai.generativeModel(modelName = VISIT_DRAFT_GEMINI_MODEL)
  }

  override fun generateDraftStream(prompt: String): Flow<String> = flow {
    model.generateContentStream(prompt).collect { chunk ->
      val chunkText = chunk.text.orEmpty()
      if (chunkText.isNotEmpty()) {
        emit(chunkText)
      }
    }
  }
}

sealed interface VisitDraftUiState {
  data object Idle : VisitDraftUiState

  data class Streaming(
    val titleFa: String,
    val symptomContextFa: String,
    val timelineSummaryFa: String,
    val parentNoteFa: String,
    val partialTextFa: String,
  ) : VisitDraftUiState

  data class Ready(
    val draftId: String?,
    val titleFa: String,
    val symptomContextFa: String,
    val timelineSummaryFa: String,
    val parentNoteFa: String,
    val draftTextFa: String,
    val isSaved: Boolean,
    val isCloudSynced: Boolean,
  ) : VisitDraftUiState

  data class Error(
    val messageFa: String,
  ) : VisitDraftUiState
}

sealed interface VisitDraftPdfState {
  data object Idle : VisitDraftPdfState

  data object Generating : VisitDraftPdfState

  data class Ready(
    val cachedPdf: CachedVisitPdf,
  ) : VisitDraftPdfState

  data class Error(
    val messageFa: String,
  ) : VisitDraftPdfState
}

class ManoVisitDraftViewModel(
  private val engine: VisitDraftAiEngine = FirebaseVisitDraftAiEngine(),
  private val pdfGenerator: VisitDraftPdfGenerator = AndroidVisitDraftPdfGenerator(),
) : ViewModel() {

  private val _uiState = MutableStateFlow<VisitDraftUiState>(VisitDraftUiState.Idle)
  val uiState: StateFlow<VisitDraftUiState> = _uiState.asStateFlow()

  private val _pdfState = MutableStateFlow<VisitDraftPdfState>(VisitDraftPdfState.Idle)
  val pdfState: StateFlow<VisitDraftPdfState> = _pdfState.asStateFlow()

  private var draftJob: Job? = null
  private var pdfJob: Job? = null

  fun resetToIdle() {
    draftJob?.cancel()
    draftJob = null
    pdfJob?.cancel()
    pdfJob = null
    _uiState.value = VisitDraftUiState.Idle
    _pdfState.value = VisitDraftPdfState.Idle
  }

  fun clearPdfState() {
    pdfJob?.cancel()
    pdfJob = null
    _pdfState.value = VisitDraftPdfState.Idle
  }

  fun loadSavedDraft(draft: DoctorVisitDraft) {
    draftJob?.cancel()
    draftJob = null
    pdfJob?.cancel()
    pdfJob = null
    _pdfState.value = VisitDraftPdfState.Idle
    _uiState.value =
      VisitDraftUiState.Ready(
        draftId = draft.id,
        titleFa = draft.titleFa,
        symptomContextFa = draft.symptomContextFa,
        timelineSummaryFa = draft.timelineSummaryFa,
        parentNoteFa = draft.parentNoteFa,
        draftTextFa = draft.draftTextFa,
        isSaved = true,
        isCloudSynced = draft.isSyncedCloud,
      )
  }

  fun editActiveDraft(titleFa: String? = null, draftTextFa: String? = null, parentNoteFa: String? = null) {
    val current = _uiState.value as? VisitDraftUiState.Ready ?: return
    pdfJob?.cancel()
    pdfJob = null
    _pdfState.value = VisitDraftPdfState.Idle
    _uiState.value =
      current.copy(
        titleFa = titleFa ?: current.titleFa,
        draftTextFa = draftTextFa ?: current.draftTextFa,
        parentNoteFa = parentNoteFa ?: current.parentNoteFa,
        isSaved = false,
      )
  }

  fun markActiveDraftSaved(draftId: String, isCloudSynced: Boolean) {
    val current = _uiState.value as? VisitDraftUiState.Ready ?: return
    _uiState.value = current.copy(draftId = draftId, isSaved = true, isCloudSynced = isCloudSynced)
  }

  fun exportActiveDraftToCachePdf(
    context: Context,
    childProfile: ChildProfileDemo?,
    snapshot: FeverDoseTimelineSnapshot?,
    forceRegenerate: Boolean = false,
    onReady: ((CachedVisitPdf) -> Unit)? = null,
  ) {
    val activeDraft = _uiState.value as? VisitDraftUiState.Ready
    if (activeDraft == null) {
      _pdfState.value = VisitDraftPdfState.Error("ابتدا پیش‌نویس ویزیت را تولید یا از فهرست انتخاب کنید.")
      return
    }
    if (activeDraft.draftTextFa.isBlank()) {
      _pdfState.value = VisitDraftPdfState.Error("متن پیش‌نویس ویزیت خالی است و قابل تبدیل به PDF نیست.")
      return
    }

    val existingCached = (_pdfState.value as? VisitDraftPdfState.Ready)?.cachedPdf
    if (!forceRegenerate && existingCached != null && existingCached.file.exists()) {
      onReady?.invoke(existingCached)
      return
    }

    val docData =
      VisitDraftPdfHelper.buildDocumentData(
        titleFa = activeDraft.titleFa,
        draftTextFa = activeDraft.draftTextFa,
        symptomContextFa = activeDraft.symptomContextFa,
        timelineSummaryFa = activeDraft.timelineSummaryFa,
        parentNoteFa = activeDraft.parentNoteFa,
        childProfile = childProfile,
        snapshot = snapshot,
        modelName = VISIT_DRAFT_GEMINI_MODEL,
      )

    pdfJob?.cancel()
    pdfJob = viewModelScope.launch {
      _pdfState.value = VisitDraftPdfState.Generating
      val result = pdfGenerator.generatePdfInCache(context = context, data = docData)
      result
        .onSuccess { cachedPdf ->
          _pdfState.value = VisitDraftPdfState.Ready(cachedPdf)
          onReady?.invoke(cachedPdf)
        }
        .onFailure { err ->
          if (err is CancellationException) throw err
          val detail = err.localizedMessage?.takeIf { it.isNotBlank() } ?: "خطا در ساخت فایل PDF"
          _pdfState.value = VisitDraftPdfState.Error("ذخیره فایل PDF انجام نشد: $detail")
        }
    }
  }

  fun generateAndSaveVisitDraft(
    snapshot: FeverDoseTimelineSnapshot,
    childProfile: ChildProfileDemo?,
    selectedSymptomFlow: SymptomFlow?,
    symptomWizard: SymptomWizardState,
    parentNoteFa: String,
    isCloudSignedIn: Boolean,
    onPersistNewDraft: (
      titleFa: String,
      symptomContextFa: String,
      timelineSummaryFa: String,
      draftTextFa: String,
      parentNoteFa: String,
      modelName: String,
      onSavedId: (String) -> Unit,
    ) -> Unit,
  ) {
    val hasTimelineData = snapshot.tempPoints.isNotEmpty() || snapshot.dosePoints.isNotEmpty()
    val hasSymptomData =
      symptomWizard.temperatureText.isNotBlank() ||
        symptomWizard.durationHoursText.isNotBlank() ||
        symptomWizard.selectedRedFlagIds.isNotEmpty() ||
        symptomWizard.evaluation != null ||
        parentNoteFa.isNotBlank()

    if (!hasTimelineData && !hasSymptomData) {
      _uiState.value =
        VisitDraftUiState.Error(
          "برای تولید پیش‌نویس ویزیت، دست‌کم یک دما یا نوبت دارو در خط زمانی ثبت کنید یا علائم کودک / یادداشت والد را وارد نمایید."
        )
      return
    }

    val bundle =
      DoctorVisitPromptBuilder.buildContext(
        snapshot = snapshot,
        childProfile = childProfile,
        selectedSymptomFlow = selectedSymptomFlow,
        symptomWizard = symptomWizard,
        parentNoteFa = parentNoteFa,
      )

    draftJob?.cancel()
    pdfJob?.cancel()
    pdfJob = null
    draftJob = viewModelScope.launch {
      _pdfState.value = VisitDraftPdfState.Idle
      _uiState.value =
        VisitDraftUiState.Streaming(
          titleFa = bundle.defaultTitleFa,
          symptomContextFa = bundle.symptomContextFa,
          timelineSummaryFa = bundle.timelineSummaryFa,
          parentNoteFa = bundle.parentNoteFa,
          partialTextFa = "",
        )

      val accumulated = StringBuilder()
      try {
        withTimeout(VISIT_DRAFT_TIMEOUT_MS) {
          engine.generateDraftStream(bundle.promptText).collect { chunk ->
            accumulated.append(chunk)
            _uiState.value =
              VisitDraftUiState.Streaming(
                titleFa = bundle.defaultTitleFa,
                symptomContextFa = bundle.symptomContextFa,
                timelineSummaryFa = bundle.timelineSummaryFa,
                parentNoteFa = bundle.parentNoteFa,
                partialTextFa = accumulated.toString(),
              )
          }
        }

        val finalDraft = accumulated.toString().trim()
        if (finalDraft.isBlank()) {
          _uiState.value =
            VisitDraftUiState.Error("دستیار هوشمند پاسخ متنی معتبری برنگرداند. لطفاً دوباره تلاش کنید.")
        } else {
          _uiState.value =
            VisitDraftUiState.Ready(
              draftId = null,
              titleFa = bundle.defaultTitleFa,
              symptomContextFa = bundle.symptomContextFa,
              timelineSummaryFa = bundle.timelineSummaryFa,
              parentNoteFa = bundle.parentNoteFa,
              draftTextFa = finalDraft,
              isSaved = false,
              isCloudSynced = isCloudSignedIn,
            )
          onPersistNewDraft(
            bundle.defaultTitleFa,
            bundle.symptomContextFa,
            bundle.timelineSummaryFa,
            finalDraft,
            bundle.parentNoteFa,
            VISIT_DRAFT_GEMINI_MODEL,
          ) { savedId ->
            markActiveDraftSaved(savedId, isCloudSignedIn)
          }
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini visit draft timed out after 60s", e)
        _uiState.value =
          VisitDraftUiState.Error("زمان تولید پیش‌نویس ویزیت به پایان رسید (بیش از ۶۰ ثانیه). لطفاً دوباره امتحان کنید.")
      } catch (e: QuotaExceededException) {
        Log.e(TAG, "Firebase AI quota exceeded for visit draft", e)
        _uiState.value =
          VisitDraftUiState.Error("سهمیه درخواست سرویس هوش مصنوعی تکمیل شده است. لطفاً کمی بعد دوباره تلاش کنید.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Gemini visit draft generation failed", e)
        val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "خطای نامشخص در ارتباط با سرویس"
        _uiState.value =
          VisitDraftUiState.Error("تولید پیش‌نویس ویزیت انجام نشد: $detail")
      }
    }
  }

  companion object {
    fun provideFactory(
      engine: VisitDraftAiEngine = FirebaseVisitDraftAiEngine(),
      pdfGenerator: VisitDraftPdfGenerator = AndroidVisitDraftPdfGenerator(),
    ): ViewModelProvider.Factory =
      viewModelFactory {
        initializer { ManoVisitDraftViewModel(engine, pdfGenerator) }
      }
  }
}
