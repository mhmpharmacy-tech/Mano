package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.GeminiNutritionResponsePayload
import com.example.domain.model.MilestoneItem
import com.example.domain.model.NutritionAppetiteStatus
import com.example.domain.model.NutritionPlanResult
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.domain.util.NutritionPromptBuilder
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.Schema
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

private const val TAG = "ManoNutritionVM"
const val NUTRITION_GEMINI_MODEL = "gemini-3.8-flash"
private const val NUTRITION_TIMEOUT_MS = 60_000L

interface NutritionAiEngine {
  suspend fun generateNutritionPlanJson(prompt: String): String
}

class FirebaseNutritionAiEngine : NutritionAiEngine {
  private val nutritionSchema =
    Schema.obj(
      properties =
        mapOf(
          "planTitleFa" to Schema.string(description = "عنوان فارسی جذاب و دقیق متناسب با سن و وزن کودک"),
          "overviewFa" to Schema.string(description = "جمع‌بندی آموزشی وضعیت تغذیه و رشد کودک در ۲ تا ۳ جمله فارسی"),
          "growthNutritionFocusFa" to Schema.string(description = "تمرکز کلیدی ریزمغذی‌ها، انرژی و بافت غذا متناسب با رشد کودک"),
          "hydrationTipFa" to Schema.string(description = "توصیه مایعات یا شیر متناسب با سن کودک"),
          "meals" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "mealTypeFa" to Schema.string(description = "نوع وعده مثل صبحانه، میان‌وعده یا راهنمای تغذیه تکمیلی"),
                      "titleFa" to Schema.string(description = "نام غذا یا راهنمای تغذیه به فارسی"),
                      "whySuitableFa" to Schema.string(description = "دلیل تناسب با سن، وزن یا نقاط عطف رشد"),
                      "textureAndPortionFa" to Schema.string(description = "بافت ایمن (پوره/نرم/تکه‌های ایمن) و مقدار تقریبی خانگی"),
                      "ingredientsFa" to Schema.array(items = Schema.string(), description = "مواد لازم با مقدار تقریبی"),
                      "prepStepsFa" to Schema.array(items = Schema.string(), description = "مراحل کوتاه آماده‌سازی ایمن"),
                    ),
                ),
              description = "۳ تا ۴ پیشنهاد غذایی یا تغذیه‌ای متناسب با سن و رشد کودک",
            ),
          "safetyAvoidListFa" to
            Schema.array(
              items = Schema.string(),
              description = "موارد ممنوع یا نیازمند پرهیز با توجه به سن و حساسیت‌های کودک",
            ),
          "doctorFollowUpHintFa" to Schema.string(description = "توصیه غیرتشخیصی درباره زمان مشورت با پزشک یا کارشناس تغذیه"),
        ),
    )

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = NUTRITION_GEMINI_MODEL,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = nutritionSchema
        },
    )
  }

  override suspend fun generateNutritionPlanJson(prompt: String): String {
    val response = model.generateContent(prompt)
    return response.text?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("سرویس هوشمند پاسخ خالی برگرداند.")
  }
}

sealed interface NutritionUiState {
  data object Idle : NutritionUiState

  data class Loading(val childContextSummaryFa: String) : NutritionUiState

  data class Ready(val plan: NutritionPlanResult) : NutritionUiState

  data class Error(val messageFa: String) : NutritionUiState
}

class ManoNutritionViewModel(
  private val engine: NutritionAiEngine = FirebaseNutritionAiEngine(),
) : ViewModel() {

  private val json = Json { ignoreUnknownKeys = true }

  private val _uiState = MutableStateFlow<NutritionUiState>(NutritionUiState.Idle)
  val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

  private var generateJob: Job? = null

  fun resetToIdle() {
    generateJob?.cancel()
    generateJob = null
    _uiState.value = NutritionUiState.Idle
  }

  fun loadSavedPlan(plan: NutritionPlanResult) {
    generateJob?.cancel()
    generateJob = null
    _uiState.value = NutritionUiState.Ready(plan)
  }

  fun generateNutritionPlan(
    childProfile: ChildProfileDemo?,
    ageBand: AgeBand,
    ageDetailFa: String,
    weightKgText: String,
    heightCmText: String,
    allergiesFa: String,
    checkedMilestones: List<MilestoneItem>,
    timelineSnapshot: FeverDoseTimelineSnapshot,
    appetiteStatus: NutritionAppetiteStatus,
    availableIngredientsFa: String,
    onSavePlan: (NutritionPlanResult) -> Unit,
  ) {
    val validationError = NutritionPromptBuilder.validateGrowthMeasurements(weightKgText, heightCmText)
    if (validationError != null) {
      _uiState.value = NutritionUiState.Error(validationError)
      return
    }

    val bundle =
      NutritionPromptBuilder.buildContext(
        childProfile = childProfile,
        ageBand = ageBand,
        ageDetailFa = ageDetailFa,
        weightKgText = weightKgText,
        heightCmText = heightCmText,
        allergiesFa = allergiesFa,
        checkedMilestones = checkedMilestones,
        timelineSnapshot = timelineSnapshot,
        appetiteStatus = appetiteStatus,
        availableIngredientsFa = availableIngredientsFa,
      )

    generateJob?.cancel()
    generateJob = viewModelScope.launch {
      _uiState.value = NutritionUiState.Loading(bundle.childContextSummaryFa)
      try {
        val rawJson =
          withTimeout(NUTRITION_TIMEOUT_MS) {
            engine.generateNutritionPlanJson(bundle.promptText)
          }
        val parsed = json.decodeFromString<GeminiNutritionResponsePayload>(rawJson)
        if (parsed.meals.isEmpty() && parsed.overviewFa.isBlank()) {
          _uiState.value =
            NutritionUiState.Error("سرویس هوشمند پیشنهاد غذایی کاملی تولید نکرد. لطفاً دوباره تلاش کنید.")
          return@launch
        }

        val now = System.currentTimeMillis()
        val result =
          NutritionPlanResult(
            id = "nutrition_$now",
            planTitleFa = parsed.planTitleFa.ifBlank { "برنامه غذای سالم و مقوی (${ageBand.labelFa})" },
            childContextSummaryFa = bundle.childContextSummaryFa,
            overviewFa = parsed.overviewFa,
            growthNutritionFocusFa = parsed.growthNutritionFocusFa,
            hydrationTipFa = parsed.hydrationTipFa,
            meals = parsed.meals,
            safetyAvoidListFa = parsed.safetyAvoidListFa,
            doctorFollowUpHintFa = parsed.doctorFollowUpHintFa,
            appetiteStatusLabelFa = appetiteStatus.labelFa,
            modelName = NUTRITION_GEMINI_MODEL,
            createdAtMs = now,
            createdAtLabelFa = FeverDoseTimelineEngine.formatHourMinute(now),
          )
        _uiState.value = NutritionUiState.Ready(result)
        onSavePlan(result)
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini nutrition request timed out", e)
        _uiState.value =
          NutritionUiState.Error("زمان پاسخ‌گویی دستیار هوشمند به پایان رسید (بیش از ۶۰ ثانیه). لطفاً دوباره امتحان کنید.")
      } catch (e: QuotaExceededException) {
        Log.e(TAG, "Firebase AI quota exceeded for nutrition", e)
        _uiState.value =
          NutritionUiState.Error("سهمیه درخواست سرویس هوش مصنوعی تکمیل شده است. لطفاً کمی بعد دوباره تلاش کنید.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Gemini nutrition generation failed", e)
        val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "خطای نامشخص در پردازش یا اتصال به سرویس"
        _uiState.value = NutritionUiState.Error("تولید پیشنهاد تغذیه انجام نشد: $detail")
      }
    }
  }

  companion object {
    fun provideFactory(engine: NutritionAiEngine = FirebaseNutritionAiEngine()): ViewModelProvider.Factory =
      viewModelFactory {
        initializer { ManoNutritionViewModel(engine) }
      }
  }
}
