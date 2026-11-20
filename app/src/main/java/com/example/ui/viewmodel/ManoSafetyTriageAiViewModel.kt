package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ManoSourcesAndClinicalSeed
import com.example.domain.util.PersianNormalizer
import com.example.domain.model.AgeBand
import com.example.domain.model.FaqItem
import com.example.domain.model.SymptomFlow
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "ManoSafetyTriageAiVM"
const val SAFETY_TRIAGE_GEMINI_MODEL = "gemini-3.8-flash"
private const val SAFETY_TRIAGE_TIMEOUT_MS = 60_000L

@Serializable
data class PediatricianSafetyChecklistPayload(
  val checklistTitleFa: String = "",
  val urgencySummaryFa: String = "",
  val immediateEmergencySignsFa: List<String> = emptyList(),
  val sameDayPediatricianVisitSignsFa: List<String> = emptyList(),
  val homeMonitoringChecklistFa: List<String> = emptyList(),
  val questionsForPediatricianFa: List<String> = emptyList(),
  val clinicalSourceCitationFa: String = "",
)

@Serializable
data class DocumentedQaPayload(
  val directAnswerFa: String = "",
  val actionableStepsFa: List<String> = emptyList(),
  val whenToVisitPediatricianFa: List<String> = emptyList(),
  val citedSourcesFa: List<String> = emptyList(),
  val safetyDisclaimerFa: String = "",
)

interface SafetyTriageAiEngine {
  suspend fun generateSafetyChecklistJson(prompt: String): String
  suspend fun generateDocumentedQaJson(prompt: String): String
}

class FirebaseSafetyTriageAiEngine : SafetyTriageAiEngine {
  private val checklistSchema =
    Schema.obj(
      properties =
        mapOf(
          "checklistTitleFa" to Schema.string(description = "عنوان فارسی چک‌لیست ایمنی و زمان مراجعه به متخصص کودکان"),
          "urgencySummaryFa" to Schema.string(description = "جمع‌بندی آموزشی و غیرتشخیصی وضعیت بر اساس علائم و سن کودک در ۲ جمله"),
          "immediateEmergencySignsFa" to
            Schema.array(
              items = Schema.string(),
              description = "علائم خطر فوری که نیازمند مراجعه بدون معطلی به اورژانس ۱۱۵ هستند (۳ تا ۵ مورد)",
            ),
          "sameDayPediatricianVisitSignsFa" to
            Schema.array(
              items = Schema.string(),
              description = "مواردی که والد باید همان روز یا ظرف ۲۴ ساعت کودک را نزد پزشک متخصص کودکان ببرد (۳ تا ۵ مورد)",
            ),
          "homeMonitoringChecklistFa" to
            Schema.array(
              items = Schema.string(),
              description = "چک‌لیست پایش ایمن در خانه تا زمان ویزیت پزشک بدون تجویز خودسرانه دارو (۳ تا ۵ مورد)",
            ),
          "questionsForPediatricianFa" to
            Schema.array(
              items = Schema.string(),
              description = "سوالات کلیدی که والد بهتر است در مطب از متخصص کودکان بپرسد (۳ مورد)",
            ),
          "clinicalSourceCitationFa" to
            Schema.string(description = "استناد علمی به راهنماهای AAP، WHO، NICE یا CDC"),
        ),
    )

  private val documentedQaSchema =
    Schema.obj(
      properties =
        mapOf(
          "directAnswerFa" to Schema.string(description = "پاسخ علمی، مستند، آرام و غیرتشخیصی به پرسش والد در ۲ تا ۴ جمله فارسی"),
          "actionableStepsFa" to
            Schema.array(
              items = Schema.string(),
              description = "گام‌های عملی و ایمن مراقبتی بر پایه منابع علمی (۳ تا ۴ مورد)",
            ),
          "whenToVisitPediatricianFa" to
            Schema.array(
              items = Schema.string(),
              description = "معیارهای هشدار و زمان مراجعه به پزشک متخصص کودکان (۳ مورد)",
            ),
          "citedSourcesFa" to
            Schema.array(
              items = Schema.string(),
              description = "فهرست منابع علمی مستند (مانند AAP، WHO، NICE، CDC) مرتبط با پاسخ",
            ),
          "safetyDisclaimerFa" to
            Schema.string(description = "یادآوری مرز ایمنی غیرتشخیصی و عدم جایگزینی با معاینه پزشک"),
        ),
    )

  private val checklistModel by lazy {
    Firebase.ai.generativeModel(
      modelName = SAFETY_TRIAGE_GEMINI_MODEL,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = checklistSchema
        },
    )
  }

  private val documentedQaModel by lazy {
    Firebase.ai.generativeModel(
      modelName = SAFETY_TRIAGE_GEMINI_MODEL,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = documentedQaSchema
        },
    )
  }

  override suspend fun generateSafetyChecklistJson(prompt: String): String {
    val response = checklistModel.generateContent(prompt)
    return response.text?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("سرویس هوشمند پاسخ خالی برگرداند.")
  }

  override suspend fun generateDocumentedQaJson(prompt: String): String {
    val response = documentedQaModel.generateContent(prompt)
    return response.text?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("سرویس هوشمند پاسخ خالی برگرداند.")
  }
}

sealed interface SafetyChecklistUiState {
  data object Idle : SafetyChecklistUiState
  data class Loading(val contextSummaryFa: String) : SafetyChecklistUiState
  data class Ready(
    val contextSummaryFa: String,
    val payload: PediatricianSafetyChecklistPayload,
    val checkedItems: Set<String> = emptySet(),
  ) : SafetyChecklistUiState
  data class Error(val messageFa: String) : SafetyChecklistUiState
}

sealed interface DocumentedQaUiState {
  data object Idle : DocumentedQaUiState
  data class Loading(val questionFa: String) : DocumentedQaUiState
  data class Ready(
    val questionFa: String,
    val payload: DocumentedQaPayload,
  ) : DocumentedQaUiState
  data class Error(val messageFa: String) : DocumentedQaUiState
}

class ManoSafetyTriageAiViewModel(
  private val engine: SafetyTriageAiEngine = FirebaseSafetyTriageAiEngine(),
) : ViewModel() {

  private val json = Json { ignoreUnknownKeys = true }

  private val _checklistState = MutableStateFlow<SafetyChecklistUiState>(SafetyChecklistUiState.Idle)
  val checklistState: StateFlow<SafetyChecklistUiState> = _checklistState.asStateFlow()

  private val _qaState = MutableStateFlow<DocumentedQaUiState>(DocumentedQaUiState.Idle)
  val qaState: StateFlow<DocumentedQaUiState> = _qaState.asStateFlow()

  private var checklistJob: Job? = null
  private var qaJob: Job? = null

  fun toggleChecklistItem(itemKey: String) {
    val current = _checklistState.value as? SafetyChecklistUiState.Ready ?: return
    val updated =
      if (itemKey in current.checkedItems) current.checkedItems - itemKey
      else current.checkedItems + itemKey
    _checklistState.value = current.copy(checkedItems = updated)
  }

  fun resetChecklist() {
    checklistJob?.cancel()
    checklistJob = null
    _checklistState.value = SafetyChecklistUiState.Idle
  }

  fun resetDocumentedQa() {
    qaJob?.cancel()
    qaJob = null
    _qaState.value = DocumentedQaUiState.Idle
  }

  fun generatePediatricianSafetyChecklist(
    primaryFlow: SymptomFlow,
    coSelectedSymptomsFa: List<String>,
    wizard: SymptomWizardState,
  ) {
    val ageBand = wizard.selectedAgeBand
    if (ageBand == null || wizard.evaluation?.showPharmacistOption != true || wizard.step != 3) {
      resetChecklist()
      _checklistState.value = SafetyChecklistUiState.Error("ابتدا غربالگری سن و علائم هشدار را کامل کنید.")
      return
    }
    val allSymptoms =
      (listOf(primaryFlow.titleFa) + coSelectedSymptomsFa)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val tempInfo = wizard.temperatureText.trim().ifBlank { "ثبت نشده" }
    val durationInfo = wizard.durationHoursText.trim().let { if (it.isBlank()) "ثبت نشده" else "$it ساعت" }
    val selectedFlags =
      primaryFlow.redFlags
        .filter { it.id in wizard.selectedRedFlagIds }
        .map { it.labelFa }

    val contextSummaryFa =
      "علائم: ${allSymptoms.joinToString("، ")} • رده سنی: ${ageBand.labelFa} (${ageBand.rangeFa})" +
        (if (tempInfo != "ثبت نشده") " • دما: $tempInfo°C" else "")

    val prompt = buildString {
      appendLine("شما دستیار ایمنی بالینی و غربالگری غیرتشخیصی «مانو کید (MANO Safety)» هستید.")
      appendLine("بر اساس اطلاعات زیر، یک چک‌لیست ایمنی ساختاریافته و مستند به زبان فارسی برای والدین تولید کنید که دقیقاً مشخص کند چه زمانی باید به پزشک متخصص کودکان یا اورژانس مراجعه کنند.")
      appendLine("محدودیت‌های ایمنی الزامی:")
      appendLine("۱. هرگز تشخیص قطعی بیماری یا تجویز آنتی‌بیوتیک/دارو صادر نکنید و دوز عددی دارو محاسبه ننمایید.")
      appendLine("۲. اگر کودک زیر ۳ ماه است و تب ۳۸.۰ درجه یا بیشتر دارد، مراجعه فوری به اورژانس ۱۱۵ را در اولویت اول قرار دهید.")
      appendLine("۳. به مراجع معتبر AAP (آکادمی اطفال آمریکا)، WHO، NICE و CDC استناد کنید.")
      appendLine()
      appendLine("اطلاعات ورودی کودک:")
      appendLine("- علائم انتخاب‌شده توسط والد: ${allSymptoms.joinToString("، ")}")
      appendLine("- رده سنی کودک: ${ageBand.labelFa} (${ageBand.rangeFa})")
      appendLine("- دمای ثبت‌شده: $tempInfo")
      appendLine("- مدت علائم: $durationInfo")
      appendLine("- علائم هشدار تیک‌خورده: ${if (selectedFlags.isEmpty()) "هیچ موردی تیک نخورده" else selectedFlags.joinToString("؛ ")}")
    }

    checklistJob?.cancel()
    checklistJob = viewModelScope.launch {
      _checklistState.value = SafetyChecklistUiState.Loading(contextSummaryFa)
      try {
        val rawJson =
          withTimeout(SAFETY_TRIAGE_TIMEOUT_MS) {
            engine.generateSafetyChecklistJson(prompt)
          }
        val parsed = json.decodeFromString<PediatricianSafetyChecklistPayload>(rawJson)
        if (parsed.immediateEmergencySignsFa.isEmpty() && parsed.sameDayPediatricianVisitSignsFa.isEmpty()) {
          _checklistState.value =
            SafetyChecklistUiState.Error("پاسخ چک‌لیست ایمنی ناقص بود. لطفاً دوباره تلاش کنید.")
          return@launch
        }
        _checklistState.value =
          SafetyChecklistUiState.Ready(
            contextSummaryFa = contextSummaryFa,
            payload =
              parsed.copy(
                checklistTitleFa =
                  parsed.checklistTitleFa.ifBlank {
                    "چک‌لیست هوشمند مراجعه به متخصص کودکان (${allSymptoms.firstOrNull() ?: primaryFlow.titleFa})"
                  },
                clinicalSourceCitationFa =
                  "چک‌لیست تولیدشده با هوش مصنوعی؛ استناد و تأیید بالینی آن بررسی نشده است.",
              ),
          )
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Safety checklist timed out", e)
        _checklistState.value =
          SafetyChecklistUiState.Error("زمان تولید چک‌لیست هوشمند به پایان رسید (بیش از ۶۰ ثانیه). لطفاً دوباره تلاش کنید.")
      } catch (e: QuotaExceededException) {
        Log.e(TAG, "Safety checklist quota exceeded", e)
        _checklistState.value =
          SafetyChecklistUiState.Error("سهمیه درخواست سرویس هوش مصنوعی تکمیل شده است. لطفاً کمی بعد دوباره امتحان کنید.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Safety checklist generation failed", e)
        val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "خطای ارتباط با سرویس هوش مصنوعی"
        _checklistState.value =
          SafetyChecklistUiState.Error("تولید چک‌لیست ایمنی انجام نشد: $detail")
      }
    }
  }

  fun askDocumentedQuestion(
    questionFa: String,
    ageBand: AgeBand?,
    relatedFaqs: List<FaqItem>,
  ) {
    val trimmed = questionFa.trim()
    if (trimmed.length < 4) {
      _qaState.value = DocumentedQaUiState.Error("لطفاً پرسش خود را کامل‌تر بنویسید (حداقل ۴ حرف).")
      return
    }

    val matchedFaqs = relatedFaqs.filter {
      PersianNormalizer.scoreMatch(trimmed, it.questionFa, it.tags.joinToString(" ")) >= 40
    }.take(4)
    val groundingContext =
      matchedFaqs.joinToString("\n") { faq ->
        "- پرسش مرجع: ${faq.questionFa} | خلاصه علمی: ${faq.summaryFa} | منبع: ${faq.source.publisher} (${faq.source.title}) ${faq.source.url}"
      }

    val prompt = buildString {
      appendLine("شما «دستیار پرسش و پاسخ مستند مانو کید (MANO Documented Q&A)» هستید.")
      appendLine("به پرسش والدین درباره سلامت و مراقبت کودک به زبان فارسی روان، علمی، مستند و کاملاً غیرتشخیصی پاسخ دهید.")
      appendLine("قواعد ایمنی بالینی:")
      appendLine("۱. فقط از خلاصه‌های مرجع ارائه‌شده استفاده کنید؛ اگر پاسخ در آن‌ها نیست، کافی نبودن اطلاعات را بیان کنید. منبع، صفحه یا تأیید مسئول علمی نسازید.")
      appendLine("۲. از تجویز دارو یا محاسبه دوز عددی خودداری کنید و بر استفاده از سرنگ مدرج mL و مشورت با پزشک/داروساز تأکید نمایید.")
      appendLine("۳. علائم هشدار نیازمند مراجعه به متخصص کودکان یا اورژانس ۱۱۵ را به روشنی بیان کنید.")
      appendLine()
      appendLine("رده سنی انتخابی: ${ageBand?.let { "${it.labelFa} (${it.rangeFa})" } ?: "عمومی کودکان"}")
      appendLine("پرسش والد: $trimmed")
      if (groundingContext.isNotBlank()) {
        appendLine("دانشنامه مستند مانو برای استناد:")
        appendLine(groundingContext)
      }
    }

    qaJob?.cancel()
    qaJob = viewModelScope.launch {
      _qaState.value = DocumentedQaUiState.Loading(trimmed)
      try {
        val rawJson =
          withTimeout(SAFETY_TRIAGE_TIMEOUT_MS) {
            engine.generateDocumentedQaJson(prompt)
          }
        val parsed = json.decodeFromString<DocumentedQaPayload>(rawJson)
        if (parsed.directAnswerFa.isBlank()) {
          _qaState.value = DocumentedQaUiState.Error("پاسخ مستند معتبری دریافت نشد. لطفاً دوباره تلاش کنید.")
          return@launch
        }
        _qaState.value =
          DocumentedQaUiState.Ready(
            questionFa = trimmed,
            payload =
              parsed.copy(
                // These are supplied context references, not verified citations for generated claims.
                citedSourcesFa = matchedFaqs.map { "${it.source.publisher} — ${it.source.title} ${it.source.url}" }.distinct(),
                safetyDisclaimerFa = "پاسخ تولیدشده با هوش مصنوعی است و تطبیق ادعاها با منابع یا تأیید داروساز برای این پاسخ ثبت نشده است. در علائم خطر منتظر پاسخ آنلاین نمانید.",
              ),
          )
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Documented QA timed out", e)
        _qaState.value =
          DocumentedQaUiState.Error("زمان دریافت پاسخ مستند به پایان رسید (بیش از ۶۰ ثانیه). لطفاً دوباره تلاش کنید.")
      } catch (e: QuotaExceededException) {
        Log.e(TAG, "Documented QA quota exceeded", e)
        _qaState.value =
          DocumentedQaUiState.Error("سهمیه درخواست سرویس هوش مصنوعی تکمیل شده است. لطفاً کمی بعد دوباره امتحان کنید.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Documented QA generation failed", e)
        val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "خطای ارتباط با سرویس هوش مصنوعی"
        _qaState.value =
          DocumentedQaUiState.Error("دریافت پاسخ مستند انجام نشد: $detail")
      }
    }
  }

  companion object {
    fun provideFactory(
      engine: SafetyTriageAiEngine = FirebaseSafetyTriageAiEngine(),
    ): ViewModelProvider.Factory =
      viewModelFactory {
        initializer { ManoSafetyTriageAiViewModel(engine) }
      }
  }
}
