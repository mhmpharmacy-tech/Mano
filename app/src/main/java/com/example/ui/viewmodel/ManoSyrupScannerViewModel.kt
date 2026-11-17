package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.GeminiSyrupScanPayload
import com.example.domain.model.SavedSyrupScan
import com.example.domain.model.SyrupDosageForm
import com.example.domain.model.SyrupIngredientKey
import com.example.domain.model.SyrupScanConfidence
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.LiquidFormulationId
import com.example.domain.util.ReconciledSyrupExtraction
import com.example.domain.util.SyrupConcentrationMatcher
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

private const val TAG = "ManoSyrupScannerVM"
const val SYRUP_SCANNER_GEMINI_MODEL = "gemini-3.8-flash"
private const val SYRUP_SCAN_TIMEOUT_MS = 60_000L

interface SyrupScannerAiEngine {
  suspend fun extractSyrupLabelJson(bitmap: Bitmap, childContextSummaryFa: String): String
}

class FirebaseSyrupScannerAiEngine : SyrupScannerAiEngine {
  private val labelSchema =
    Schema.obj(
      properties =
        mapOf(
          "detectedDrugNameFa" to Schema.string(description = "نام دارو به فارسی همراه با نام ژنریک کوتاه در صورت وجود"),
          "activeIngredientKey" to
            Schema.enumeration(
              values = listOf("ACETAMINOPHEN", "IBUPROFEN", "CETIRIZINE", "UNKNOWN"),
              description = "ماده موثره اصلی دارو؛ اگر قطعی نیست UNKNOWN بگذارید",
            ),
          "dosageForm" to
            Schema.enumeration(
              values = listOf("DROPS", "SYRUP", "SUSPENSION", "UNKNOWN"),
              description = "شکل دارویی مایع روی جعبه یا بطری",
            ),
          "mgAmount" to Schema.double(description = "میزان میلی‌گرم در کسر غلظت برچسب، مثلاً 120 یا 160 یا 100 یا 5"),
          "perMlAmount" to Schema.double(description = "مخرج میلی‌لیتر روی برچسب، معمولاً 5 برای شربت یا 1 برای قطره"),
          "mgPerMl" to Schema.double(description = "میلی‌گرم خالص در هر ۱ میلی‌لیتر (mgAmount / perMlAmount)"),
          "matchedFormulationId" to
            Schema.enumeration(
              values =
                listOf(
                  "acetaminophen_drops_100",
                  "acetaminophen_syrup_120_5",
                  "acetaminophen_syrup_160_5",
                  "ibuprofen_syrup_100_5",
                  "cetirizine_syrup_5_5",
                  "CUSTOM_UNVERIFIED",
                ),
              description = "شناسه دقیق فرمولاسیون استاندارد مانو در صورت تطابق قطعی، وگرنه CUSTOM_UNVERIFIED",
            ),
          "confidenceLevel" to
            Schema.enumeration(
              values = listOf("HIGH", "MEDIUM", "LOW"),
              description = "سطح خوانایی و اطمینان استخراج برچسب از تصویر",
            ),
          "concentrationDisplayFa" to Schema.string(description = "نمایش خوانای غلظت مثلاً 120 mg / 5 mL"),
          "safetyNoteFa" to Schema.string(description = "یادداشت ایمنی ۱ تا ۲ جمله‌ای فارسی فقط درباره تطبیق برچسب و عدم خلط قطره و شربت"),
          "isPediatricFeverOrAllergyLiquid" to Schema.boolean(description = "آیا تصویر واقعاً برچسب داروی مایع کودک (تب‌بر/آلرژی) است؟"),
        ),
    )

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = SYRUP_SCANNER_GEMINI_MODEL,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = labelSchema
        },
    )
  }

  override suspend fun extractSyrupLabelJson(bitmap: Bitmap, childContextSummaryFa: String): String {
    val prompt =
      """
      شما دستیار دقیق استخراج متن و غلظت برچسب داروهای مایع کودک هستید (غیرتشخیصی).
      تصویر برچسب یا جعبه دارو را به دقت بخوانید و فقط ساختار JSON تعریف‌شده را برگردانید.
      قوانین ایمنی:
      1) تفاوت بین قطره استامینوفن (100 mg/mL = acetaminophen_drops_100)، شربت 120 mg/5mL (acetaminophen_syrup_120_5) و سوسپانسیون 160 mg/5mL (acetaminophen_syrup_160_5) را با دقت حیاتی تفکیک کنید.
      2) اگر تصویر تار است، دارو نیست، یا غلظت به‌وضوح خوانده نمی‌شود، confidenceLevel را LOW و matchedFormulationId را CUSTOM_UNVERIFIED بگذارید و هرگز حدس خطرناک نزنید.
      3) اگر نام ماده موثره ایبوپروفن (100 mg/5mL) بود -> ibuprofen_syrup_100_5 و سیتیریزین (5 mg/5mL) -> cetirizine_syrup_5_5.
      4) از تصویر فقط اطلاعات دارو و غلظت را استخراج کنید؛ از اطلاعات سلامت کودک استفاده نکنید.
      """.trimIndent()

    val response =
      model.generateContent(
        content {
          image(bitmap)
          text(prompt)
        }
      )
    return response.text?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("سرویس هوش مصنوعی پاسخ خالی برای تصویر برگرداند.")
  }
}

sealed interface SyrupScannerUiState {
  data object Idle : SyrupScannerUiState

  data class Analyzing(val previewBitmap: Bitmap) : SyrupScannerUiState

  data class Review(
    val previewBitmap: Bitmap?,
    val extraction: ReconciledSyrupExtraction,
    val parentVerified: Boolean = false,
    val activeSavedId: String? = null,
  ) : SyrupScannerUiState

  data class Error(
    val messageFa: String,
    val previewBitmap: Bitmap? = null,
  ) : SyrupScannerUiState
}

class ManoSyrupScannerViewModel(
  private val engine: SyrupScannerAiEngine = FirebaseSyrupScannerAiEngine(),
) : ViewModel() {

  private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
  }

  private val _uiState = MutableStateFlow<SyrupScannerUiState>(SyrupScannerUiState.Idle)
  val uiState: StateFlow<SyrupScannerUiState> = _uiState.asStateFlow()

  private var scanJob: Job? = null

  fun reset() {
    scanJob?.cancel()
    scanJob = null
    _uiState.value = SyrupScannerUiState.Idle
  }

  fun analyzeBitmap(
    bitmap: Bitmap,
    childProfile: ChildProfileDemo?,
    birthDateEpochMs: Long? = null,
  ) {
    val ageM = LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(childProfile, birthDateEpochMs)
    val weightKg = LiquidDoseVisualizerEngine.initialWeightKgFromProfile(childProfile)
    val childCtx =
      if (childProfile != null) {
        "${childProfile.aliasFa} • ${LiquidDoseVisualizerEngine.formatAgeFa(ageM)} • ${LiquidDoseVisualizerEngine.formatKg(weightKg)}"
      } else {
        "سن تقریبی ${LiquidDoseVisualizerEngine.formatAgeFa(ageM)} • وزن تقریبی ${LiquidDoseVisualizerEngine.formatKg(weightKg)}"
      }

    scanJob?.cancel()
    scanJob =
      viewModelScope.launch {
        _uiState.value = SyrupScannerUiState.Analyzing(bitmap)
        try {
          val rawJson =
            withTimeout(SYRUP_SCAN_TIMEOUT_MS) {
              engine.extractSyrupLabelJson(bitmap, childCtx)
            }
          val payload = json.decodeFromString<GeminiSyrupScanPayload>(rawJson)
          val reconciled = SyrupConcentrationMatcher.reconcileFromAi(payload, childProfile, birthDateEpochMs)
          _uiState.value =
            SyrupScannerUiState.Review(
              previewBitmap = bitmap,
              extraction = reconciled,
              parentVerified = false,
              activeSavedId = null,
            )
        } catch (e: TimeoutCancellationException) {
          Log.e(TAG, "Gemini syrup scan timed out", e)
          _uiState.value =
            SyrupScannerUiState.Error(
              messageFa = "زمان پردازش تصویر برچسب به پایان رسید (بیش از ۶۰ ثانیه). لطفاً با نور بهتر دوباره امتحان کنید.",
              previewBitmap = bitmap,
            )
        } catch (e: QuotaExceededException) {
          Log.e(TAG, "Gemini AI quota exceeded", e)
          _uiState.value =
            SyrupScannerUiState.Error(
              messageFa = "سهمیه درخواست هوش مصنوعی موقتاً تکمیل شده است؛ می‌توانید غلظت بطری را به‌صورت دستی انتخاب کنید.",
              previewBitmap = bitmap,
            )
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Syrup label scan failed", e)
          val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "خطا در خواندن یا تحلیل تصویر برچسب"
          _uiState.value =
            SyrupScannerUiState.Error(
              messageFa = "تحلیل تصویر بطری انجام نشد: $detail",
              previewBitmap = bitmap,
            )
        }
      }
  }

  fun startManualFallback(
    childProfile: ChildProfileDemo?,
    previewBitmap: Bitmap? = null,
    birthDateEpochMs: Long? = null,
  ) {
    scanJob?.cancel()
    val extraction =
      SyrupConcentrationMatcher.reconcileEditable(
        drugNameFa = "شربت استامینوفن ۱۲۰/۵ (ورود دستی والد)",
        ingredientKey = SyrupIngredientKey.ACETAMINOPHEN,
        dosageForm = SyrupDosageForm.SYRUP,
        mgAmount = 120.0,
        perMlAmount = 5.0,
        confidence = SyrupScanConfidence.MEDIUM,
        presetOverride = LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5,
        childProfile = childProfile,
        birthDateEpochMs = birthDateEpochMs,
      )
    _uiState.value =
      SyrupScannerUiState.Review(
        previewBitmap = previewBitmap,
        extraction = extraction,
        parentVerified = false,
        activeSavedId = null,
      )
  }

  fun editExtraction(
    drugNameFa: String? = null,
    ingredientKey: SyrupIngredientKey? = null,
    dosageForm: SyrupDosageForm? = null,
    mgAmount: Double? = null,
    perMlAmount: Double? = null,
    childProfile: ChildProfileDemo? = null,
    birthDateEpochMs: Long? = null,
  ) {
    val current = _uiState.value as? SyrupScannerUiState.Review ?: return
    val prev = current.extraction
    val updated =
      SyrupConcentrationMatcher.reconcileEditable(
        drugNameFa = drugNameFa ?: prev.drugNameFa,
        ingredientKey = ingredientKey ?: prev.ingredientKey,
        dosageForm = dosageForm ?: prev.dosageForm,
        mgAmount = mgAmount ?: prev.mgAmount,
        perMlAmount = perMlAmount ?: prev.perMlAmount,
        confidence = prev.confidence,
        aiSafetyNoteFa = prev.safetyNoteFa,
        presetOverride = null,
        isPediatricFeverOrAllergyLiquid = true,
        childProfile = childProfile,
        birthDateEpochMs = birthDateEpochMs,
      )
    _uiState.value =
      current.copy(
        extraction = updated,
        parentVerified = false,
      )
  }

  fun selectCalibratedPreset(
    formulation: LiquidFormulationId,
    childProfile: ChildProfileDemo?,
    birthDateEpochMs: Long? = null,
  ) {
    val current = _uiState.value as? SyrupScannerUiState.Review
    val (ingredient, form) =
      when (formulation) {
        LiquidFormulationId.ACETAMINOPHEN_DROPS_100 -> SyrupIngredientKey.ACETAMINOPHEN to SyrupDosageForm.DROPS
        LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5 -> SyrupIngredientKey.ACETAMINOPHEN to SyrupDosageForm.SYRUP
        LiquidFormulationId.ACETAMINOPHEN_SYRUP_160_5 -> SyrupIngredientKey.ACETAMINOPHEN to SyrupDosageForm.SUSPENSION
        LiquidFormulationId.IBUPROFEN_SYRUP_100_5 -> SyrupIngredientKey.IBUPROFEN to SyrupDosageForm.SUSPENSION
        LiquidFormulationId.CETIRIZINE_SYRUP_5_5 -> SyrupIngredientKey.CETIRIZINE to SyrupDosageForm.SYRUP
      }
    val updated =
      SyrupConcentrationMatcher.reconcileEditable(
        drugNameFa = formulation.titleFa,
        ingredientKey = ingredient,
        dosageForm = form,
        mgAmount = SyrupConcentrationMatcher.defaultNumeratorMg(formulation),
        perMlAmount = SyrupConcentrationMatcher.defaultDenominatorMl(formulation),
        confidence = SyrupScanConfidence.HIGH,
        aiSafetyNoteFa = formulation.safetyTipFa,
        presetOverride = formulation,
        isPediatricFeverOrAllergyLiquid = true,
        childProfile = childProfile,
        birthDateEpochMs = birthDateEpochMs,
      )
    _uiState.value =
      SyrupScannerUiState.Review(
        previewBitmap = current?.previewBitmap,
        extraction = updated,
        parentVerified = false,
        activeSavedId = current?.activeSavedId,
      )
  }

  fun setParentVerified(verified: Boolean) {
    val current = _uiState.value as? SyrupScannerUiState.Review ?: return
    _uiState.value = current.copy(parentVerified = verified)
  }

  fun refreshChildContext(childProfile: ChildProfileDemo?, birthDateEpochMs: Long? = null) {
    val current = _uiState.value as? SyrupScannerUiState.Review ?: return
    val prev = current.extraction
    val refreshed =
      SyrupConcentrationMatcher.reconcileEditable(
        drugNameFa = prev.drugNameFa,
        ingredientKey = prev.ingredientKey,
        dosageForm = prev.dosageForm,
        mgAmount = prev.mgAmount,
        perMlAmount = prev.perMlAmount,
        confidence = prev.confidence,
        aiSafetyNoteFa = prev.safetyNoteFa,
        presetOverride = prev.matchedFormulation,
        isPediatricFeverOrAllergyLiquid = prev.matchedFormulation != null,
        childProfile = childProfile,
        birthDateEpochMs = birthDateEpochMs,
      )
    _uiState.value = current.copy(extraction = refreshed)
  }

  fun loadSavedScan(scan: SavedSyrupScan, childProfile: ChildProfileDemo?, birthDateEpochMs: Long? = null) {
    scanJob?.cancel()
    val ingredient =
      SyrupIngredientKey.entries.firstOrNull { it.name == scan.activeIngredientKey } ?: SyrupIngredientKey.UNKNOWN
    val form = SyrupDosageForm.entries.firstOrNull { it.name == scan.dosageForm } ?: SyrupDosageForm.UNKNOWN
    val conf = SyrupScanConfidence.entries.firstOrNull { it.name == scan.confidenceLevel } ?: SyrupScanConfidence.MEDIUM
    val preset = LiquidFormulationId.entries.firstOrNull { it.id == scan.matchedFormulationId }

    val reconciled =
      SyrupConcentrationMatcher.reconcileEditable(
        drugNameFa = scan.drugNameFa,
        ingredientKey = ingredient,
        dosageForm = form,
        mgAmount = scan.mgAmount,
        perMlAmount = scan.perMlAmount,
        confidence = conf,
        aiSafetyNoteFa = scan.safetyNoteFa,
        presetOverride = preset,
        isPediatricFeverOrAllergyLiquid = preset != null,
        childProfile = childProfile,
        birthDateEpochMs = birthDateEpochMs,
      )
    _uiState.value =
      SyrupScannerUiState.Review(
        previewBitmap = null,
        extraction = reconciled,
        parentVerified = true,
        activeSavedId = scan.id,
      )
  }

  fun buildConfirmedSavedScan(): SavedSyrupScan? {
    val current = _uiState.value as? SyrupScannerUiState.Review ?: return null
    if (!current.parentVerified) return null
    val ext = current.extraction
    val now = System.currentTimeMillis()
    val id = current.activeSavedId?.takeIf { it.isNotBlank() } ?: "scan_$now"
    return SavedSyrupScan(
      id = id,
      drugNameFa = ext.drugNameFa,
      activeIngredientKey = ext.ingredientKey.name,
      dosageForm = ext.dosageForm.name,
      mgAmount = ext.mgAmount,
      perMlAmount = ext.perMlAmount,
      mgPerMl = ext.mgPerMl,
      matchedFormulationId = ext.matchedFormulationIdRaw,
      confidenceLevel = ext.confidence.name,
      concentrationDisplayFa = ext.concentrationDisplayFa,
      safetyNoteFa = ext.safetyNoteFa,
      confirmedByParent = true,
      createdAtMs = now,
      updatedAtMs = now,
      isSyncedCloud = false,
    )
  }

  companion object {
    fun provideFactory(engine: SyrupScannerAiEngine = FirebaseSyrupScannerAiEngine()): ViewModelProvider.Factory =
      viewModelFactory {
        initializer { ManoSyrupScannerViewModel(engine) }
      }
  }
}
