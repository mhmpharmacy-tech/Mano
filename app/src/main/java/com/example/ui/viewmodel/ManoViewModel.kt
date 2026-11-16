package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GalacticOrbitSelection
import com.example.data.ManoContentRepository
import com.example.data.ManoPreferencesRepository
import com.example.data.ManoThemeMode
import com.example.domain.model.AgeBand
import com.example.domain.model.Article
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationRequestDemo
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.DeviceGuide
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.FaqItem
import com.example.domain.model.FollowUpNote
import com.example.domain.model.MedicationMonograph
import com.example.domain.model.MedicationSafetyCategory
import com.example.domain.model.MilestoneItem
import com.example.domain.model.NutritionPlanResult
import com.example.domain.model.SavedSyrupScan
import com.example.domain.model.SourceReference
import com.example.domain.model.SymptomFlow
import com.example.domain.model.TopicCategory
import com.example.domain.model.TriageEvaluationResult
import com.example.domain.model.VaccineRecord
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.util.ChildProfilePrivacy
import com.example.notifications.VaccineReminderScheduler
import com.example.notifications.VaccineNotificationHelper
import com.example.notifications.HighFeverNotificationHelper
import com.example.widget.FeverTimelineWidgetStore
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.PersianNormalizer
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineTempPoint
import com.example.domain.util.TriageEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SymptomWizardState(
  val step: Int = 0, // 0: choose symptom, 1: age & temp/duration, 2: red-flag check, 3: outcome
  val selectedSymptomId: String = "fever",
  val selectedAgeBand: AgeBand? = null,
  val temperatureText: String = "",
  val durationHoursText: String = "",
  val selectedRedFlagIds: Set<String> = emptySet(),
  val validationErrorFa: String? = null,
  val evaluation: TriageEvaluationResult? = null,
)

data class AskPharmacistState(
  val category: ConsultationCategory = ConsultationCategory.SYMPTOMS,
  val ageBand: AgeBand = AgeBand.TODDLER_1_3Y,
  val questionText: String = "",
  val hasUrgentFlags: Boolean = false,
  val consentAccepted: Boolean = false,
  val validationErrorFa: String? = null,
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
    prefs.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManoThemeMode.SYSTEM)

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

  private val _uiState = MutableStateFlow(ManoUiState())
  val uiState: StateFlow<ManoUiState> = _uiState.asStateFlow()

  // Static catalog accessors
  val allArticles: List<Article> = repository.articles
  val allSymptomFlows: List<SymptomFlow> = repository.symptomFlows
  val allMedications: List<MedicationMonograph> = repository.medications
  val allDeviceGuides: List<DeviceGuide> = repository.deviceGuides
  val allSources: List<SourceReference> = repository.sources

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
            selectedAgeBand = selectedAgeBand.value,
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
      it.copy(symptomWizard = it.symptomWizard.copy(selectedAgeBand = ageBand, validationErrorFa = null))
    }
  }

  fun updateWizardTemperature(text: String) {
    _uiState.update {
      it.copy(symptomWizard = it.symptomWizard.copy(temperatureText = text, validationErrorFa = null))
    }
  }

  fun updateWizardDurationHours(text: String) {
    _uiState.update {
      it.copy(symptomWizard = it.symptomWizard.copy(durationHoursText = text, validationErrorFa = null))
    }
  }

  fun toggleWizardRedFlag(flagId: String) {
    _uiState.update { state ->
      val updated = state.symptomWizard.selectedRedFlagIds.toMutableSet()
      if (!updated.add(flagId)) updated.remove(flagId)
      state.copy(symptomWizard = state.symptomWizard.copy(selectedRedFlagIds = updated))
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
        if (flow.asksTemperature && wiz.temperatureText.isNotBlank() && parsedTemp == null) {
          _uiState.update {
            it.copy(symptomWizard = wiz.copy(validationErrorFa = "لطفاً دمای معتبر به سانتی‌گراد (مثلاً 38.0) وارد کنید."))
          }
          return
        }
        // Immediate clinical safety short-circuit: fever >= 38.0 in infant < 3m triggers urgent alert immediately
        if (age == AgeBand.NEWBORN && parsedTemp != null && parsedTemp >= 38.0) {
          val result =
            TriageEngine.evaluate(
              flow = flow,
              ageBand = age,
              temperatureCelsius = parsedTemp,
              durationHours = parseIntOrNull(wiz.durationHoursText),
              selectedRedFlagIds = wiz.selectedRedFlagIds,
            )
          _uiState.update {
            it.copy(symptomWizard = wiz.copy(step = 3, evaluation = result, validationErrorFa = null))
          }
          return
        }
        _uiState.update { it.copy(symptomWizard = wiz.copy(step = 2, validationErrorFa = null)) }
      }
      2 -> {
        val age = wiz.selectedAgeBand ?: AgeBand.TODDLER_1_3Y
        val result =
          TriageEngine.evaluate(
            flow = flow,
            ageBand = age,
            temperatureCelsius = parseTemperature(wiz.temperatureText),
            durationHours = parseIntOrNull(wiz.durationHoursText),
            selectedRedFlagIds = wiz.selectedRedFlagIds,
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

  fun consumeValidatedAskPharmacistQuestion(): Triple<ConsultationCategory, AgeBand, String>? {
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
    val prepared = Triple(form.category, form.ageBand, form.questionText.trim())
    _uiState.update {
      it.copy(askPharmacist = form.copy(questionText = "", validationErrorFa = null))
    }
    return prepared
  }

  fun submitAskPharmacistDemo() {
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
      return
    }
    if (!form.consentAccepted) {
      _uiState.update {
        it.copy(askPharmacist = form.copy(validationErrorFa = "لطفاً ابتدا رضایت‌نامه ثبت پرسش را تأیید کنید."))
      }
      return
    }
    if (form.questionText.trim().length < 8) {
      _uiState.update {
        it.copy(askPharmacist = form.copy(validationErrorFa = "لطفاً پرسش خود را حداقل در چند کلمه شفاف بنویسید."))
      }
      return
    }

    val newReq =
      ConsultationRequestDemo(
        id = "LOCAL-${form.requests.size + 101}",
        category = form.category,
        childAgeBand = form.ageBand,
        questionSummaryFa = form.questionText.trim(),
        hasUrgentFlag = false,
        status = ConsultationStatus.PENDING,
        createdAtLabelFa = "هم‌اکنون (ذخیره‌شده روی دستگاه)",
        followUp =
          FollowUpNote(
            scheduleFa = "ثبت‌شده برای پیگیری",
            guidanceFa = "برای همگام‌سازی ابری و پیگیری مداوم، با حساب گوگل وارد شوید.",
          ),
      )

    _uiState.update {
      it.copy(
        askPharmacist =
          form.copy(
            questionText = "",
            validationErrorFa = null,
            requests = listOf(newReq) + form.requests,
          ),
        toastMessageFa = "پرسش پیگیری در حافظه دستگاه ثبت شد.",
      )
    }
  }

  // --- Consent-Gated My Child Profile (DataStore) ---
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
    if (!consentGranted) {
      showToast("ذخیره پروفایل کودک فقط پس از تأیید رضایت والد امکان‌پذیر است.")
      return
    }
    if (aliasFa.isBlank()) {
      showToast("لطفاً یک نام مستعار یا نام کوچک وارد کنید.")
      return
    }
    val cleanedBirthOrAge = birthDateOrAgeFa.trim().ifEmpty { ageBand.labelFa }
    val profile =
      ChildProfileDemo(
        aliasFa = aliasFa.trim(),
        birthDateOrAgeFa = cleanedBirthOrAge,
        ageBand = ageBand,
        weightKgText = weightKgText.trim(),
        weightDateFa = weightDateFa.trim(),
        allergiesFa = allergiesFa.trim().ifEmpty { "ثبت نشده" },
        currentMedsFa = currentMedsFa.trim().ifEmpty { "ثبت نشده" },
        consentGranted = true,
        ageMonthsExact = LiquidDoseVisualizerEngine.parseAgeMonthsFromText(cleanedBirthOrAge),
      )
    viewModelScope.launch {
      prefs.setLocalChildProfile(profile)
      _uiState.update {
        it.copy(
          exportedProfileJson = null,
          toastMessageFa = "پروفایل کودک با موفقیت در حافظه دستگاه ذخیره شد.",
        )
      }
    }
  }

  fun exportChildProfileSummary(overrideProfile: ChildProfileDemo? = null) {
    val profile = overrideProfile ?: localChildProfile.value ?: return
    _uiState.update {
      it.copy(exportedProfileJson = ChildProfilePrivacy.exportProfileSummaryJson(profile))
    }
  }

  /**
   * Local-only data deletion; never claim the authenticated user's Firestore
   * records or remote backups have been deleted. UI must confirm this separately.
   */
  fun deleteAllLocalChildHealthData() {
    viewModelScope.launch {
      try {
        prefs.clearAllLocalChildHealthData()
        val context = getApplication<Application>().applicationContext
        val remindersCancelled = VaccineReminderScheduler.cancelAllForLocalDataRemoval(context)
        val vaccineNotificationDismissed = VaccineNotificationHelper.dismissLocalReminder(context)
        val feverNotificationDismissed = HighFeverNotificationHelper.dismissLocalAlert(context)
        val widgetUpdated = runCatching {
          FeverTimelineWidgetStore.clearAndUpdateWidgets(context)
          true
        }.getOrDefault(false)
        val ancillaryOk =
          remindersCancelled && vaccineNotificationDismissed &&
            feverNotificationDismissed && widgetUpdated
        _uiState.update {
          it.copy(
            exportedProfileJson = null,
            toastMessageFa =
              if (ancillaryOk) {
                "داده‌های سلامت ذخیره‌شده در برنامه روی این دستگاه پاک شدند. داده‌های ابری حذف نشده‌اند."
              } else {
                "داده‌های سلامت در حافظه برنامه پاک شدند؛ برای حذف اعلان‌ها یا ویجت ممکن است اقدام دستی لازم باشد. داده‌های ابری حذف نشده‌اند."
              },
          )
        }
      } catch (_: Exception) {
        showToast("حذف داده‌ها کامل نشد؛ لطفاً دوباره تلاش کنید.")
      }
    }
  }

  fun deleteChildProfileDemo() {
    viewModelScope.launch {
      prefs.setLocalChildProfile(null)
      _uiState.update {
        it.copy(
          exportedProfileJson = null,
          toastMessageFa = "پروفایل کودک از حافظه دستگاه پاک شد.",
        )
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
        timestampMs = now,
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

  fun showToast(message: String) {
    _uiState.update { it.copy(toastMessageFa = message) }
  }

  fun clearToast() {
    _uiState.update { it.copy(toastMessageFa = null) }
  }

  private fun parseTemperature(raw: String): Double? {
    val norm = PersianNormalizer.normalize(raw).replace("٫", ".").replace(",", ".").trim()
    val value = norm.toDoubleOrNull() ?: return null
    return if (value in 34.0..43.0) value else null
  }

  private fun parseIntOrNull(raw: String): Int? = PersianNormalizer.normalize(raw).trim().toIntOrNull()
}
