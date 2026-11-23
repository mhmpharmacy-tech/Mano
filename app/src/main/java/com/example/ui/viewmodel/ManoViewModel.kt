package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.json.JSONObject
import com.example.data.GalacticOrbitSelection
import com.example.data.ManoContentRepository
import com.example.data.ManoHapticIntensity
import com.example.data.ManoPreferencesRepository
import com.example.data.ManoThemeMode
import com.example.data.ManoWebGlQuality
import com.example.domain.model.AgeBand
import com.example.domain.model.SymptomReferral
import com.example.domain.model.Article
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationRequestDemo
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.DeviceGuide
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.FaqItem
import com.example.domain.model.GrowthMeasurement
import com.example.domain.model.MedicationMonograph
import com.example.domain.model.MedicationSafetyCategory
import com.example.domain.model.MilestoneItem
import com.example.domain.model.NutritionPlanResult
import com.example.domain.model.PediatricTreatmentPlan
import com.example.domain.model.SavedSyrupScan
import com.example.domain.model.SourceReference
import com.example.domain.model.SymptomFlow
import com.example.domain.model.TopicCategory
import com.example.domain.model.TreatmentMedicationItem
import com.example.domain.model.TreatmentPlanFormatter
import com.example.domain.model.TriageEvaluationResult
import com.example.domain.model.VaccineRecord
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.PersianNormalizer
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineTempPoint
import com.example.domain.util.TriageEngine
import com.example.domain.util.VaccinationScheduleEngine
import com.example.domain.util.ClinicalNumberInput
import com.example.widget.FeverTimelineWidgetStore
import com.example.ui.util.ManoSensitiveCacheCleaner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SymptomWizardState(
  val step: Int = 0, // 0: choose symptom, 1: age & temp/duration, 2: red-flag check, 3: outcome
  val selectedSymptomId: String = "fever",
  val selectedAgeBand: AgeBand? = null,
  val temperatureText: String = "",
  val ageMonthsText: String = "",
  val durationHoursText: String = "",
  val selectedRedFlagIds: Set<String> = emptySet(),
  val validationErrorFa: String? = null,
  val evaluation: TriageEvaluationResult? = null,
)

data class AskPharmacistState(
  val category: ConsultationCategory = ConsultationCategory.SYMPTOMS,
  val ageBand: AgeBand? = null,
  val questionText: String = "",
  val hasUrgentFlags: Boolean = false,
  val consentAccepted: Boolean = false,
  val validationErrorFa: String? = null,
  val referral: SymptomReferral? = null,
  val requests: List<ConsultationRequestDemo> = emptyList(),
)

data class ManoUiState(
  val globalSearchQuery: String = "",
  val faqSearchQuery: String = "",
  val faqCategory: TopicCategory? = null,
  val expandedFaqIds: Set<String> = emptySet(),
  val topicSearchQuery: String = "",
  val topicCategory: TopicCategory? = null,
  val medicineSearchQuery: String = "",
  val medicineCategory: MedicationSafetyCategory? = null,
  val symptomWizard: SymptomWizardState = SymptomWizardState(),
  val askPharmacist: AskPharmacistState = AskPharmacistState(),
  val exportedProfileJson: String? = null,
  val hasPlayedHeroIntro: Boolean = false,
  val toastMessageFa: String? = null,
)

class ManoViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = ManoContentRepository()
  private val prefs = ManoPreferencesRepository(application.applicationContext)

  val themeMode: StateFlow<ManoThemeMode> =
    prefs.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManoThemeMode.LIGHT)

  val localChildProfile: StateFlow<ChildProfileDemo?> =
    prefs.localChildProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val localFeverLogs: StateFlow<List<TimelineTempPoint>> =
    prefs.localFeverLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val localDoseLogs: StateFlow<List<TimelineDosePoint>> =
    prefs.localDoseLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val checkedMilestoneIds: StateFlow<Set<String>> =
    prefs.checkedMilestoneIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  val bookmarkedSlugs: StateFlow<Set<String>> =
    prefs.bookmarkedSlugs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  val selectedAgeBand: StateFlow<AgeBand?> =
    prefs.selectedAgeBand.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val localVisitDrafts: StateFlow<List<DoctorVisitDraft>> =
    prefs.savedVisitDrafts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val savedNutritionPlans: StateFlow<List<NutritionPlanResult>> =
    prefs.savedNutritionPlans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val childHeightCm: StateFlow<String> =
    prefs.childHeightCm.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  val checkedMedSafetyIds: StateFlow<Set<String>> =
    prefs.checkedMedSafetyIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  val childBirthDateEpochMs: StateFlow<Long?> =
    prefs.childBirthDateEpochMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val localVaccineRecords: StateFlow<List<VaccineRecord>> =
    prefs.vaccineRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val vaccineReminderSettings: StateFlow<VaccineReminderSettings> =
    prefs.vaccineReminderSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaccineReminderSettings())

  val notifiedVaccineReminderKeys: StateFlow<Set<String>> =
    prefs.notifiedVaccineReminderKeys.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  val savedGalacticSelection: StateFlow<GalacticOrbitSelection?> =
    prefs.galacticOrbitSelection.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val savedSyrupScans: StateFlow<List<SavedSyrupScan>> =
    prefs.savedSyrupScans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val onboardingCompleted: StateFlow<Boolean?> =
    prefs.onboardingCompleted.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val hapticIntensity: StateFlow<ManoHapticIntensity> =
    prefs.hapticIntensity.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManoHapticIntensity.MEDIUM)

  val webGlQuality: StateFlow<ManoWebGlQuality> =
    prefs.webGlQuality.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManoWebGlQuality.AUTO)

  val treatmentPlan: StateFlow<PediatricTreatmentPlan> =
    prefs.treatmentPlan
      .map { TreatmentPlanFormatter.normalizeForToday(it) }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TreatmentPlanFormatter.normalizeForToday(PediatricTreatmentPlan()),
      )

  private val _uiState = MutableStateFlow(ManoUiState())
  val uiState: StateFlow<ManoUiState> = _uiState.asStateFlow()

  // Static catalog accessors (delegated to lazy repository lists for instant cold start)
  val allArticles: List<Article> get() = repository.articles
  val allSymptomFlows: List<SymptomFlow> get() = repository.symptomFlows
  val allMedications: List<MedicationMonograph> get() = repository.medications
  val allDeviceGuides: List<DeviceGuide> get() = repository.deviceGuides
  val allSources: List<SourceReference> get() = repository.sources

  fun articleBySlug(slug: String): Article? = repository.articleBySlug(slug)

  fun medicationBySlug(slug: String): MedicationMonograph? = repository.medicationBySlug(slug)

  fun toggleBookmark(slug: String) {
    viewModelScope.launch {
      prefs.toggleBookmark(slug)
      showToast("وضعیت نشان‌گذاری مقاله به‌روزرسانی شد.")
    }
  }

  fun selectAgeBand(ageBand: AgeBand?) {
    viewModelScope.launch { prefs.setSelectedAgeBand(ageBand) }
  }

  fun toggleMedSafetyCheckItem(itemId: String) {
    viewModelScope.launch { prefs.toggleMedSafetyCheck(itemId) }
  }

  fun resetMedSafetyChecklist() {
    viewModelScope.launch {
      prefs.resetMedSafetyChecklist()
      showToast("چک‌لیست ایمنی مصرف دارو برای نوبت جدید بازنشانی شد.")
    }
  }

  fun setGlobalSearchQuery(query: String) {
    _uiState.update { it.copy(globalSearchQuery = query) }
  }

  fun setFaqSearchQuery(query: String) {
    _uiState.update { it.copy(faqSearchQuery = query) }
  }

  fun setFaqCategory(category: TopicCategory?) {
    _uiState.update { it.copy(faqCategory = category) }
  }

  fun toggleFaqExpanded(id: String) {
    _uiState.update { state ->
      val updated = state.expandedFaqIds.toMutableSet()
      if (!updated.add(id)) updated.remove(id)
      state.copy(expandedFaqIds = updated)
    }
  }

  fun setTopicSearchQuery(query: String) {
    _uiState.update { it.copy(topicSearchQuery = query) }
  }

  fun setTopicCategory(category: TopicCategory?) {
    _uiState.update { it.copy(topicCategory = category) }
  }

  fun setMedicineSearchQuery(query: String) {
    _uiState.update { it.copy(medicineSearchQuery = query) }
  }

  fun setMedicineCategory(category: MedicationSafetyCategory?) {
    _uiState.update { it.copy(medicineCategory = category) }
  }

  fun filteredArticles(ageBand: AgeBand?): List<Article> =
    repository.searchArticles(_uiState.value.topicSearchQuery, _uiState.value.topicCategory, ageBand)

  fun filteredFaqs(ageBand: AgeBand?): List<FaqItem> =
    repository.searchFaqs(_uiState.value.faqSearchQuery, _uiState.value.faqCategory, ageBand)

  fun filteredMedications(): List<MedicationMonograph> =
    repository.searchMedications(_uiState.value.medicineSearchQuery, _uiState.value.medicineCategory)

  fun milestonesForAge(ageBand: AgeBand?): List<MilestoneItem> =
    repository.milestonesForAge(ageBand)

  fun toggleMilestoneChecked(id: String) {
    viewModelScope.launch { prefs.toggleMilestoneCheck(id) }
  }

  fun setThemeMode(mode: ManoThemeMode) {
    viewModelScope.launch {
      prefs.setThemeMode(mode)
      showToast("حالت نمایش به «${mode.labelFa}» تغییر کرد.")
    }
  }

  fun toggleDayNightTheme(currentlyDark: Boolean) {
    val next = if (currentlyDark) ManoThemeMode.LIGHT else ManoThemeMode.DARK
    setThemeMode(next)
  }

  fun globalSearchResults(ageBand: AgeBand?): Triple<List<Article>, List<FaqItem>, List<MedicationMonograph>> {
    val q = _uiState.value.globalSearchQuery
    if (q.isBlank()) return Triple(emptyList(), emptyList(), emptyList())
    return Triple(
      repository.searchArticles(q, null, ageBand).take(4),
      repository.searchFaqs(q, null, ageBand).take(5),
      repository.searchMedications(q).take(3),
    )
  }

  // --- Symptom Wizard Actions ---
  fun startSymptomFlow(symptomId: String) {
    _uiState.update {
      it.copy(
        symptomWizard =
          SymptomWizardState(
            step = 1,
            selectedSymptomId = symptomId,
          )
      )
    }
  }

  fun selectWizardSymptom(symptomId: String) {
    _uiState.update {
      it.copy(
        symptomWizard =
          it.symptomWizard.copy(
            selectedSymptomId = symptomId,
            selectedRedFlagIds = emptySet(),
            validationErrorFa = null,
            evaluation = null,
          )
      )
    }
  }

  fun selectWizardAgeBand(ageBand: AgeBand) {
    _uiState.update {
      it.copy(symptomWizard = it.symptomWizard.copy(selectedAgeBand = ageBand, ageMonthsText = "", evaluation = null, validationErrorFa = null))
    }
  }

  fun updateWizardAgeMonths(text: String) {
    _uiState.update { it.copy(symptomWizard = it.symptomWizard.copy(ageMonthsText = text.take(10), evaluation = null, validationErrorFa = null)) }
  }

  fun updateWizardTemperature(text: String) {
    _uiState.update {
      it.copy(symptomWizard = it.symptomWizard.copy(temperatureText = text.take(20), evaluation = null, validationErrorFa = null))
    }
  }

  fun updateWizardDurationHours(text: String) {
    _uiState.update {
      it.copy(symptomWizard = it.symptomWizard.copy(durationHoursText = text.take(20), evaluation = null, validationErrorFa = null))
    }
  }

  fun toggleWizardRedFlag(flagId: String) {
    _uiState.update { state ->
      val updated = state.symptomWizard.selectedRedFlagIds.toMutableSet()
      if (!updated.add(flagId)) updated.remove(flagId)
      state.copy(symptomWizard = state.symptomWizard.copy(selectedRedFlagIds = updated, evaluation = null))
    }
  }

  fun wizardNextStep() {
    val wiz = _uiState.value.symptomWizard
    val flow = repository.symptomFlowById(wiz.selectedSymptomId)

    when (wiz.step) {
      0 -> {
        _uiState.update { it.copy(symptomWizard = wiz.copy(step = 1, validationErrorFa = null)) }
      }
      1 -> {
        val age = wiz.selectedAgeBand
        if (age == null) {
          _uiState.update {
            it.copy(symptomWizard = wiz.copy(validationErrorFa = "لطفاً ابتدا گروه سنی کودک را انتخاب کنید."))
          }
          return
        }
        val parsedTemp = parseTemperature(wiz.temperatureText)
        val newbornFever = age == AgeBand.NEWBORN &&
          ((parsedTemp != null && parsedTemp >= 38.0) || (flow.id == "fever" && parsedTemp == null))
        if (newbornFever || TriageEngine.isExtremeTemperature(parsedTemp)) {
          val result =
            TriageEngine.evaluate(
              flow = flow,
              ageBand = age,
              temperatureCelsius = parsedTemp,
              durationHours = parseIntOrNull(wiz.durationHoursText),
              selectedRedFlagIds = wiz.selectedRedFlagIds,
              ageMonthsExact = if (age == AgeBand.INFANT_3_12M) ClinicalNumberInput.durationHours(wiz.ageMonthsText) else null,
            )
          _uiState.update {
            it.copy(symptomWizard = wiz.copy(step = 3, evaluation = result, validationErrorFa = null))
          }
          return
        }
        if (wiz.durationHoursText.isNotBlank() && ClinicalNumberInput.durationHours(wiz.durationHoursText) == null) {
          _uiState.update { it.copy(symptomWizard = wiz.copy(validationErrorFa = "مدت علائم باید عدد صحیح نامنفی بر حسب ساعت باشد.")) }
          return
        }
        if (age == AgeBand.INFANT_3_12M && wiz.ageMonthsText.isNotBlank() &&
          ClinicalNumberInput.durationHours(wiz.ageMonthsText) !in 3..11) {
          _uiState.update { it.copy(symptomWizard = wiz.copy(validationErrorFa = "سن دقیق باید با گروه انتخابی (۳ تا کمتر از ۱۲ ماه) سازگار باشد.")) }
          return
        }
        if (wiz.temperatureText.isNotBlank() && parsedTemp == null) {
          _uiState.update {
            it.copy(symptomWizard = wiz.copy(validationErrorFa = "لطفاً دمای معتبر به سانتی‌گراد (مثلاً 38.0) وارد کنید."))
          }
          return
        }
        _uiState.update { it.copy(symptomWizard = wiz.copy(step = 2, validationErrorFa = null)) }
      }
      2 -> {
        val age = wiz.selectedAgeBand ?: return
        val result =
          TriageEngine.evaluate(
            flow = flow,
            ageBand = age,
            temperatureCelsius = parseTemperature(wiz.temperatureText),
            durationHours = parseIntOrNull(wiz.durationHoursText),
            selectedRedFlagIds = wiz.selectedRedFlagIds,
              ageMonthsExact = if (age == AgeBand.INFANT_3_12M) ClinicalNumberInput.durationHours(wiz.ageMonthsText) else null,
          )
        _uiState.update {
          it.copy(symptomWizard = wiz.copy(step = 3, evaluation = result, validationErrorFa = null))
        }
      }
    }
  }

  fun wizardPreviousStep() {
    val wiz = _uiState.value.symptomWizard
    if (wiz.step > 0) {
      _uiState.update {
        it.copy(
          symptomWizard =
            wiz.copy(
              step = wiz.step - 1,
              validationErrorFa = null,
              evaluation = if (wiz.step == 3) null else wiz.evaluation,
            )
        )
      }
    }
  }

  fun resetWizard() {
    _uiState.update { it.copy(symptomWizard = SymptomWizardState()) }
  }

  fun beginSymptomReferral(): String? {
    val wizard = _uiState.value.symptomWizard
    val age = wizard.selectedAgeBand ?: return null
    if (wizard.step != 3 || wizard.evaluation?.showPharmacistOption != true) return null
    val flow = repository.symptomFlowById(wizard.selectedSymptomId)
    val referral = SymptomReferral(
      id = java.util.UUID.randomUUID().toString(),
      flowId = when (flow.id) {
        "diaper_rash" -> "rash"
        "diarrhea_vomiting" -> "diarrhea"
        else -> flow.id
      },
      symptomFa = flow.titleFa,
      ageBand = age,
      ageMonthsExact = parseIntOrNull(wizard.ageMonthsText),
      temperatureCelsius = parseTemperature(wizard.temperatureText),
      durationHours = parseIntOrNull(wizard.durationHoursText),
      selectedRedFlagIds = wizard.selectedRedFlagIds.toSet(),
    )
    _uiState.update { it.copy(askPharmacist = AskPharmacistState(
      ageBand = age, referral = referral, requests = it.askPharmacist.requests,
    )) }
    return referral.id
  }

  fun clearSymptomReferral() {
    _uiState.update {
      if (it.askPharmacist.referral == null) it
      else it.copy(askPharmacist = AskPharmacistState(requests = it.askPharmacist.requests))
    }
  }

  // --- Ask Pharmacist Demo Actions ---
  fun setAskCategory(category: ConsultationCategory) {
    _uiState.update { it.copy(askPharmacist = it.askPharmacist.copy(category = category)) }
  }

  fun setAskAgeBand(ageBand: AgeBand) {
    _uiState.update { it.copy(askPharmacist = it.askPharmacist.copy(ageBand = ageBand)) }
  }

  fun setAskQuestion(text: String) {
    _uiState.update {
      it.copy(askPharmacist = it.askPharmacist.copy(questionText = text, validationErrorFa = null))
    }
  }

  fun setAskUrgentFlags(hasUrgent: Boolean) {
    _uiState.update { it.copy(askPharmacist = it.askPharmacist.copy(hasUrgentFlags = hasUrgent)) }
  }

  fun setAskConsent(accepted: Boolean) {
    _uiState.update {
      it.copy(askPharmacist = it.askPharmacist.copy(consentAccepted = accepted, validationErrorFa = null))
    }
  }

  fun validateAskPharmacistForm(): Triple<ConsultationCategory, AgeBand, String>? {
    val form = _uiState.value.askPharmacist
    if (form.hasUrgentFlags) {
      _uiState.update {
        it.copy(
          askPharmacist =
            form.copy(
              validationErrorFa =
                "در صورت وجود علائم هشدار فوری، درخواست مشاوره آنلاین کافی نیست؛ لطفاً فوراً با اورژانس ۱۱۵ یا پزشک تماس بگیرید."
            )
        )
      }
      return null
    }
    val age = form.ageBand
    if (age == null) {
      _uiState.update { it.copy(askPharmacist = form.copy(validationErrorFa = "گروه سنی کودک را انتخاب کنید.")) }
      return null
    }
    if (!form.consentAccepted) {
      _uiState.update {
        it.copy(askPharmacist = form.copy(validationErrorFa = "لطفاً ابتدا رضایت‌نامه ثبت درخواست را تأیید کنید."))
      }
      return null
    }
    if (form.questionText.trim().length < 8) {
      _uiState.update {
        it.copy(askPharmacist = form.copy(validationErrorFa = "لطفاً پرسش خود را حداقل در چند کلمه شفاف بنویسید."))
      }
      return null
    }
    val question = listOfNotNull(form.questionText.trim(), form.referral?.summaryFa).joinToString("\n")
    if (question.length > 600) {
      _uiState.update { it.copy(askPharmacist = form.copy(
        validationErrorFa = "متن پرسش و خلاصه غربالگری باید در مجموع حداکثر ۶۰۰ نویسه باشد.",
      )) }
      return null
    }
    _uiState.update { it.copy(askPharmacist = it.askPharmacist.copy(validationErrorFa = null)) }
    return Triple(form.category, age, question)
  }

  fun clearAskPharmacistQuestion() {
    _uiState.update {
      it.copy(askPharmacist = it.askPharmacist.copy(questionText = "", consentAccepted = false, validationErrorFa = null))
    }
  }

  fun consumeValidatedAskPharmacistQuestion(): Triple<ConsultationCategory, AgeBand, String>? {
    val prepared = validateAskPharmacistForm() ?: return null
    clearAskPharmacistQuestion()
    return prepared
  }

  val offlineQuestions = prefs.offlineQuestions

  fun growthHistory(uid: String?) = prefs.growthHistory(uid)

  suspend fun saveGrowthMeasurement(uid: String?, weight: Double?, height: Double?) {
    prefs.addGrowthMeasurement(uid, GrowthMeasurement(System.currentTimeMillis(), weight, height))
  }

  private var savingQuestion = false

  suspend fun saveOfflineQuestion(): Boolean {
    if (savingQuestion) return false
    val validated = validateAskPharmacistForm() ?: return false
    val form = _uiState.value.askPharmacist
    savingQuestion = true
    try {
      prefs.saveOfflineQuestion(ConsultationRequestDemo(
        id = "LOCAL-${java.util.UUID.randomUUID()}",
        category = validated.first,
        childAgeBand = validated.second,
        questionSummaryFa = validated.third,
        hasUrgentFlag = false,
        status = ConsultationStatus.PENDING,
        createdAtLabelFa = java.text.DateFormat.getDateTimeInstance().format(java.util.Date()),
      ))
      if (_uiState.value.askPharmacist == form) clearAskPharmacistQuestion()
      showToast("پرسش روی این دستگاه ذخیره شد. به داروخانه ارسال نشده است.")
      return true
    } catch (e: java.io.IOException) {
      showToast("ذخیره پرسش انجام نشد. دوباره تلاش کنید.")
      return false
    } finally {
      savingQuestion = false
    }
  }

  // --- Consent-Gated My Child Profile (DataStore & Cloud Parity) ---
  fun resolveValidatedChildProfile(
    aliasFa: String,
    birthDateOrAgeFa: String,
    ageBand: AgeBand,
    weightKgText: String,
    weightDateFa: String,
    allergiesFa: String,
    currentMedsFa: String,
    consentGranted: Boolean,
  ): Pair<ChildProfileDemo, Long?>? {
    if (!consentGranted) {
      showToast("ذخیره پروفایل کودک فقط پس از تأیید رضایت والد امکان‌پذیر است.")
      return null
    }
    if (aliasFa.isBlank()) {
      showToast("لطفاً یک نام مستعار یا نام کوچک وارد کنید.")
      return null
    }
    val cleanWeight =
      PersianNormalizer.normalize(weightKgText).replace("٫", ".").replace(",", ".").trim()
    if (cleanWeight.isNotEmpty()) {
      val parsedKg = cleanWeight.toDoubleOrNull()
      if (parsedKg == null || parsedKg !in 1.5..60.0) {
        showToast("وزن کودک باید عددی بین ۱.۵ تا ۶۰ کیلوگرم باشد.")
        return null
      }
    }
    val cleanedBirthOrAge = birthDateOrAgeFa.trim().ifEmpty { ageBand.labelFa }
    val parsedBirthEpochMs = VaccinationScheduleEngine.parseFlexibleDateToEpochMs(cleanedBirthOrAge)
    if (parsedBirthEpochMs != null && parsedBirthEpochMs > System.currentTimeMillis()) {
      showToast("تاریخ تولد نمی‌تواند در آینده باشد.")
      return null
    }
    val computedAgeMonths =
      parsedBirthEpochMs?.let { bMs ->
        val days = VaccinationScheduleEngine.diffDays(bMs, System.currentTimeMillis()).coerceAtLeast(0)
        (days / 30.4375).toInt().coerceIn(0, 144)
      } ?: LiquidDoseVisualizerEngine.parseAgeMonthsFromText(cleanedBirthOrAge)
    val profile =
      ChildProfileDemo(
        aliasFa = aliasFa.trim(),
        birthDateOrAgeFa = cleanedBirthOrAge,
        ageBand = ageBand,
        weightKgText = cleanWeight,
        weightDateFa = weightDateFa.trim(),
        allergiesFa = allergiesFa.trim().ifEmpty { "ثبت نشده" },
        currentMedsFa = currentMedsFa.trim().ifEmpty { "ثبت نشده" },
        consentGranted = true,
        ageMonthsExact = computedAgeMonths,
      )
    return profile to parsedBirthEpochMs
  }

  fun syncChildMetadataFromProfile(ageBand: AgeBand, parsedBirthEpochMs: Long?) {
    viewModelScope.launch {
      prefs.setSelectedAgeBand(ageBand)
      if (parsedBirthEpochMs != null && parsedBirthEpochMs > 0L) {
        prefs.setChildBirthDateEpochMs(parsedBirthEpochMs)
      }
    }
  }

  fun saveChildProfileDemo(
    aliasFa: String,
    birthDateOrAgeFa: String,
    ageBand: AgeBand,
    weightKgText: String,
    weightDateFa: String,
    allergiesFa: String,
    currentMedsFa: String,
    consentGranted: Boolean,
  ) {
    val resolved =
      resolveValidatedChildProfile(
        aliasFa = aliasFa,
        birthDateOrAgeFa = birthDateOrAgeFa,
        ageBand = ageBand,
        weightKgText = weightKgText,
        weightDateFa = weightDateFa,
        allergiesFa = allergiesFa,
        currentMedsFa = currentMedsFa,
        consentGranted = consentGranted,
      ) ?: return
    val (profile, parsedBirthEpochMs) = resolved
    viewModelScope.launch {
      prefs.setLocalChildProfile(profile)
      prefs.setSelectedAgeBand(ageBand)
      if (parsedBirthEpochMs != null && parsedBirthEpochMs > 0L) {
        prefs.setChildBirthDateEpochMs(parsedBirthEpochMs)
      }
      _uiState.update {
        it.copy(
          exportedProfileJson = null,
          toastMessageFa = "پروفایل کودک با موفقیت ذخیره و با تقویم واکسن و سرنگ mL همگام شد.",
        )
      }
    }
  }

  fun exportChildProfileSummary(overrideProfile: ChildProfileDemo? = null) {
    val profile = overrideProfile ?: localChildProfile.value ?: return
    val json =
      JSONObject()
        .put("source", "MANO_KIDS_LOCAL_SUMMARY")
        .put("alias", profile.aliasFa)
        .put("ageBand", profile.ageBand.labelFa)
        .put("birthOrAge", profile.birthDateOrAgeFa)
        .put("weightKg", profile.weightKgText)
        .put("weightDate", profile.weightDateFa)
        .put("allergies", profile.allergiesFa)
        .put("currentMedications", profile.currentMedsFa)
        .toString(2)
    _uiState.update { it.copy(exportedProfileJson = json) }
  }

  fun deleteChildProfileDemo() {
    viewModelScope.launch {
      prefs.setLocalChildProfile(null)
      prefs.setChildBirthDateEpochMs(null)
      prefs.setChildHeightCm("")
      _uiState.update {
        it.copy(
          exportedProfileJson = null,
          toastMessageFa = "پروفایل کودک از حافظه دستگاه پاک شد.",
        )
      }
    }
  }

  /** Clear unsaved health-related UI when account ownership changes; never transfer forms between users. */
  fun clearTransientHealthSession() {
    _uiState.update { state ->
      state.copy(
        symptomWizard = SymptomWizardState(),
        askPharmacist = AskPharmacistState(),
        exportedProfileJson = null,
      )
    }
  }

  /** Local-only wipe. Cloud documents and pharmacist audit records are never touched. */
  fun eraseLocalChildHealthData() {
    viewModelScope.launch {
      try {
        val context = getApplication<Application>().applicationContext
        prefs.clearLocalChildHealthData()
        FeverTimelineWidgetStore.clearStoredSummary(context)
        if (!ManoSensitiveCacheCleaner.clear(context)) {
          throw java.io.IOException("Some MANO sensitive cache files could not be deleted")
        }
        _uiState.update { state ->
          state.copy(
            exportedProfileJson = null,
            askPharmacist = AskPharmacistState(),
            symptomWizard = SymptomWizardState(),
            toastMessageFa = "داده‌های محلی، فایل‌های موقت و خلاصه ویجت پاک شدند؛ کش Firebase و پرونده ابری همچنان ممکن است وجود داشته باشند.",
          )
        }
      } catch (_: Exception) {
        showToast("حذف داده‌های محلی کامل نشد. دوباره تلاش کنید و در صورت تداوم خطا با پشتیبانی تماس بگیرید.")
      }
    }
  }

  fun markHeroIntroPlayed() {
    if (!_uiState.value.hasPlayedHeroIntro) {
      _uiState.update { it.copy(hasPlayedHeroIntro = true) }
    }
  }

  // --- Local Persistent 24h Fever & Medication Timeline ---
  fun addLocalFeverLog(
    tempCelsius: Float,
    siteFa: String,
    timeLabelFa: String,
    caregiverFa: String,
    noteFa: String,
  ) {
    val clamped = tempCelsius.coerceIn(34.0f, 43.0f)
    val now = System.currentTimeMillis()
    val point =
      TimelineTempPoint(
        id = "local_fever_$now",
        tempCelsius = clamped,
        timestampMs = now,
        timeLabelFa = timeLabelFa.trim().ifEmpty { FeverDoseTimelineEngine.formatHourMinute(now) },
        siteFa = siteFa.trim().ifEmpty { "زیربغلی" },
        caregiverFa = caregiverFa.trim().ifEmpty { "والد" },
        noteFa = noteFa.trim(),
        isSyncedCloud = false,
      )
    viewModelScope.launch {
      prefs.addLocalFeverLog(point)
      showToast("دمای ${FeverDoseTimelineEngine.formatTempC(clamped)} در حافظه دستگاه ثبت شد.")
    }
  }

  fun deleteLocalFeverLog(id: String) {
    viewModelScope.launch {
      prefs.deleteLocalFeverLog(id)
      showToast("رکورد دما حذف شد.")
    }
  }

  fun addLocalDoseLog(
    medicationSlug: String,
    medicationNameFa: String,
    dosageFormFa: String,
    volumeMlText: String,
    givenAtLabelFa: String,
    noteFa: String,
  ) {
    val now = System.currentTimeMillis()
    val point =
      TimelineDosePoint(
        id = "local_dose_$now",
        medicationSlug = medicationSlug,
        medicationNameFa = medicationNameFa,
        dosageFormFa = dosageFormFa,
        volumeMlText = volumeMlText,
        timestampMs = FeverDoseTimelineEngine.resolveDoseTimeMs(givenAtLabelFa, now) ?: now,
        givenAtLabelFa = givenAtLabelFa.trim().ifEmpty { FeverDoseTimelineEngine.formatHourMinute(now) },
        noteFa = noteFa.trim(),
        isSyncedCloud = false,
      )
    viewModelScope.launch {
      prefs.addLocalDoseLog(point)
      showToast("نوبت مصرف دارو در حافظه دستگاه ثبت شد.")
    }
  }

  fun deleteLocalDoseLog(id: String) {
    viewModelScope.launch {
      prefs.deleteLocalDoseLog(id)
      showToast("رکورد مصرف دارو حذف شد.")
    }
  }

  fun clearLocalTimelineLogsAfterCloudSync() {
    viewModelScope.launch {
      prefs.clearLocalFeverLogs()
      prefs.clearLocalDoseLogs()
    }
  }

  fun saveLocalVisitDraft(
    titleFa: String,
    symptomContextFa: String,
    timelineSummaryFa: String,
    draftTextFa: String,
    parentNoteFa: String,
    modelName: String,
    existingId: String? = null,
    onSavedId: (String) -> Unit = {},
  ) {
    val now = System.currentTimeMillis()
    val id = existingId?.takeIf { it.isNotBlank() } ?: "local_draft_$now"
    val draft =
      DoctorVisitDraft(
        id = id,
        titleFa = titleFa.trim().ifBlank { "پیش‌نویس ویزیت پزشک" }.take(120),
        symptomContextFa = symptomContextFa.trim().take(300),
        timelineSummaryFa = timelineSummaryFa.trim().ifBlank { "خلاصه خط زمانی ثبت‌شده" }.take(800),
        draftTextFa = draftTextFa.trim().take(6000),
        parentNoteFa = parentNoteFa.trim().take(500),
        modelName = modelName,
        createdAtMs = now,
        createdAtLabelFa = FeverDoseTimelineEngine.formatHourMinute(now),
        isSyncedCloud = false,
      )
    viewModelScope.launch {
      prefs.upsertVisitDraft(draft)
      onSavedId(id)
      showToast("پیش‌نویس ویزیت پزشک در حافظه دستگاه ذخیره شد.")
    }
  }

  fun deleteLocalVisitDraft(draftId: String) {
    viewModelScope.launch {
      prefs.deleteVisitDraft(draftId)
      showToast("پیش‌نویس ویزیت حذف شد.")
    }
  }

  fun setChildHeightCm(heightCm: String) {
    viewModelScope.launch { prefs.setChildHeightCm(heightCm) }
  }

  fun saveNutritionPlan(plan: NutritionPlanResult) {
    viewModelScope.launch {
      prefs.upsertNutritionPlan(plan)
      showToast("پیشنهاد غذای سالم ذخیره شد.")
    }
  }

  fun deleteNutritionPlan(planId: String) {
    viewModelScope.launch {
      prefs.deleteNutritionPlan(planId)
      showToast("برنامه غذایی ذخیره‌شده حذف شد.")
    }
  }

  fun checkedMilestonesList(): List<MilestoneItem> {
    val checked = checkedMilestoneIds.value
    return repository.milestones.filter { it.id in checked }
  }

  // --- Vaccination Schedule & Reminder Preferences ---
  fun setChildBirthDateEpochMs(epochMs: Long?) {
    viewModelScope.launch {
      prefs.setChildBirthDateEpochMs(epochMs)
      showToast(
        if (epochMs == null) "تاریخ تولد کودک پاک شد."
        else "تاریخ تولد ثبت شد و موعد واکسن‌ها به‌روزرسانی گردید."
      )
    }
  }

  fun saveLocalVaccineRecord(record: VaccineRecord, toastFa: String = "سابقه واکسن با موفقیت ذخیره شد.") {
    viewModelScope.launch {
      prefs.upsertVaccineRecord(record.copy(updatedAtMs = System.currentTimeMillis()))
      if (toastFa.isNotBlank()) showToast(toastFa)
    }
  }

  fun deleteLocalVaccineRecord(vaccineId: String) {
    viewModelScope.launch {
      prefs.deleteVaccineRecord(vaccineId)
      showToast("سابقه واکسن به حالت پیش‌فرض بازگشت.")
    }
  }

  fun setChildAliasFa(alias: String?) {
    viewModelScope.launch { prefs.setChildAliasFa(alias) }
  }

  fun mirrorCloudVaccineRecords(records: List<VaccineRecord>) {
    viewModelScope.launch { prefs.mirrorCloudVaccineRecords(records) }
  }

  fun updateVaccineReminderSettings(enabled: Boolean, leadDaysBefore: Int) {
    viewModelScope.launch {
      prefs.setVaccineReminderSettings(enabled, leadDaysBefore)
      showToast(
        if (enabled) "یادآوری واکسن برای $leadDaysBefore روز پیش از موعد تنظیم شد."
        else "یادآوری خودکار واکسیناسیون غیرفعال شد."
      )
    }
  }

  fun markVaccineReminderKeysNotified(keys: Set<String>) {
    if (keys.isEmpty()) return
    viewModelScope.launch { prefs.markVaccineReminderKeysNotified(keys) }
  }

  fun saveGalacticOrbitSelection(ringName: String, topicId: String) {
    viewModelScope.launch { prefs.setGalacticOrbitSelection(ringName, topicId) }
  }

  fun saveLocalSyrupScan(
    scan: SavedSyrupScan,
    toastFa: String = "برچسب شربت با تایید والد ذخیره شد.",
  ) {
    val now = System.currentTimeMillis()
    val stamped =
      scan.copy(
        createdAtMs = if (scan.createdAtMs > 0L) scan.createdAtMs else now,
        updatedAtMs = now,
      )
    viewModelScope.launch {
      prefs.upsertSyrupScan(stamped)
      if (toastFa.isNotBlank()) showToast(toastFa)
    }
  }

  fun deleteLocalSyrupScan(scanId: String) {
    viewModelScope.launch {
      prefs.deleteSyrupScan(scanId)
      showToast("اسکن شربت ذخیره‌شده حذف شد.")
    }
  }

  fun mirrorCloudSyrupScans(scans: List<SavedSyrupScan>) {
    viewModelScope.launch { prefs.mirrorCloudSyrupScans(scans) }
  }

  fun completeOnboarding(
    aliasFa: String,
    birthDateEpochMs: Long?,
    ageBand: AgeBand,
    weightKgText: String,
    heightCmText: String,
    onProfileReady: (ChildProfileDemo) -> Unit = {},
  ) {
    val cleanAlias = aliasFa.trim().ifBlank { "فرزند من" }.take(40)
    val cleanWeight = PersianNormalizer.normalize(weightKgText).replace("٫", ".").replace(",", ".").trim()
    val cleanHeight = PersianNormalizer.normalize(heightCmText).replace("٫", ".").replace(",", ".").trim()
    if (birthDateEpochMs != null && birthDateEpochMs > System.currentTimeMillis()) {
      showToast("تاریخ تولد نمی‌تواند در آینده باشد.")
      return
    }
    val ageMonthsExact = LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(null, birthDateEpochMs)
    val birthDateLabel =
      birthDateEpochMs?.let { VaccinationScheduleEngine.formatPersianDate(it) } ?: ageBand.labelFa
    val profile =
      ChildProfileDemo(
        aliasFa = cleanAlias,
        birthDateOrAgeFa = birthDateLabel,
        ageBand = ageBand,
        weightKgText = cleanWeight,
        weightDateFa = VaccinationScheduleEngine.formatPersianDate(System.currentTimeMillis()),
        allergiesFa = "ثبت نشده",
        currentMedsFa = "ثبت نشده",
        consentGranted = true,
        ageMonthsExact = ageMonthsExact,
      )
    viewModelScope.launch {
      prefs.setLocalChildProfile(profile)
      prefs.setSelectedAgeBand(ageBand)
      if (birthDateEpochMs != null && birthDateEpochMs > 0L) {
        prefs.setChildBirthDateEpochMs(birthDateEpochMs)
      }
      if (cleanHeight.isNotEmpty()) {
        prefs.setChildHeightCm(cleanHeight)
      }
      prefs.setOnboardingCompleted(true)
      onProfileReady(profile)
      showToast("پروفایل «$cleanAlias» ثبت شد؛ تقویم واکسن، سرنگ mL و نمودار رشد شخصی‌سازی شدند.")
    }
  }

  fun skipOnboarding() {
    viewModelScope.launch {
      prefs.setOnboardingCompleted(true)
    }
  }

  fun setHapticIntensity(intensity: ManoHapticIntensity) {
    viewModelScope.launch {
      prefs.setHapticIntensity(intensity)
      showToast("شدت بازخورد لرزشی روی «${intensity.labelFa}» تنظیم شد.")
    }
  }

  fun setWebGlQuality(quality: ManoWebGlQuality) {
    viewModelScope.launch {
      prefs.setWebGlQuality(quality)
      showToast("کیفیت موتور سه‌بعدی WebGL روی «${quality.labelFa}» تنظیم شد.")
    }
  }

  fun toggleTreatmentDoseSlot(medicationId: String, slotIndex: Int) {
    viewModelScope.launch {
      val today = TreatmentPlanFormatter.currentDayKey()
      val current = TreatmentPlanFormatter.normalizeForToday(treatmentPlan.value, today)
      var toggledOn = false
      val updatedMeds =
        current.medications.map { med ->
          if (med.id != medicationId) {
            med
          } else {
            val nextSlots = med.completedSlotTimesFa.toMutableMap()
            if (nextSlots.containsKey(slotIndex)) {
              nextSlots.remove(slotIndex)
              toggledOn = false
            } else {
              nextSlots[slotIndex] = TreatmentPlanFormatter.currentTimeFa()
              toggledOn = true
            }
            med.copy(completedSlotTimesFa = nextSlots)
          }
        }
      prefs.saveTreatmentPlan(current.copy(lastCheckoffDayKey = today, medications = updatedMeds))
      val slotFa = TreatmentPlanFormatter.toPersianDigits(slotIndex.toString())
      showToast(if (toggledOn) "نوبت $slotFa امروز ثبت شد." else "تیک نوبت $slotFa برداشته شد.")
    }
  }

  fun upsertTreatmentMedication(item: TreatmentMedicationItem) {
    viewModelScope.launch {
      val today = TreatmentPlanFormatter.currentDayKey()
      val current = TreatmentPlanFormatter.normalizeForToday(treatmentPlan.value, today)
      val exists = current.medications.any { it.id == item.id }
      val updatedMeds =
        if (exists) {
          current.medications.map { if (it.id == item.id) item else it }
        } else {
          current.medications + item
        }
      prefs.saveTreatmentPlan(current.copy(lastCheckoffDayKey = today, medications = updatedMeds))
      showToast(if (exists) "تغییرات دارو در برنامه درمانی ذخیره شد." else "داروی جدید به برنامه درمانی اضافه شد.")
    }
  }

  fun deleteTreatmentMedication(medicationId: String) {
    viewModelScope.launch {
      val today = TreatmentPlanFormatter.currentDayKey()
      val current = TreatmentPlanFormatter.normalizeForToday(treatmentPlan.value, today)
      val updatedMeds = current.medications.filterNot { it.id == medicationId }
      prefs.saveTreatmentPlan(current.copy(lastCheckoffDayKey = today, medications = updatedMeds))
      showToast("قلم دارویی از برنامه درمانی حذف شد.")
    }
  }

  fun updateTreatmentPlanHeader(
    pharmacyNameFa: String,
    pharmacistBadgeFa: String,
    prescriptionDateFa: String,
    storageInstructionFa: String,
  ) {
    viewModelScope.launch {
      val current = TreatmentPlanFormatter.normalizeForToday(treatmentPlan.value)
      prefs.saveTreatmentPlan(
        current.copy(
          pharmacyNameFa = pharmacyNameFa.trim().ifBlank { current.pharmacyNameFa },
          pharmacistBadgeFa = pharmacistBadgeFa.trim().ifBlank { current.pharmacistBadgeFa },
          prescriptionDateFa = prescriptionDateFa.trim().ifBlank { current.prescriptionDateFa },
          storageInstructionFa = storageInstructionFa.trim().ifBlank { current.storageInstructionFa },
        )
      )
      showToast("اطلاعات نسخه و داروخانه به‌روزرسانی شد.")
    }
  }

  fun setTreatmentSmartRemindersEnabled(enabled: Boolean) {
    viewModelScope.launch {
      val current = TreatmentPlanFormatter.normalizeForToday(treatmentPlan.value)
      prefs.saveTreatmentPlan(current.copy(smartRemindersEnabled = enabled))
      showToast(
        if (enabled) "یادآور هوشمند برای همه داروهای این نسخه فعال شد."
        else "یادآور هوشمند داروهای این نسخه غیرفعال شد."
      )
    }
  }

  fun resetTreatmentPlanToDefault() {
    viewModelScope.launch {
      prefs.saveTreatmentPlan(TreatmentPlanFormatter.normalizeForToday(PediatricTreatmentPlan()))
      showToast("نسخه نمونه استاندارد داروسازی بازیابی شد.")
    }
  }

  fun showToast(message: String) {
    _uiState.update { it.copy(toastMessageFa = message) }
  }

  fun clearToast() {
    _uiState.update { it.copy(toastMessageFa = null) }
  }

  private fun parseTemperature(raw: String): Double? = ClinicalNumberInput.temperature(raw)

  private fun parseIntOrNull(raw: String): Int? = ClinicalNumberInput.durationHours(raw)
}
