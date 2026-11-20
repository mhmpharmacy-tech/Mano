package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FeverLogCloud
import com.example.data.ManoCloudRepository
import com.example.data.MedicationDoseLogCloud
import com.example.data.UrgentTriageBlockedException
import com.example.domain.model.AgeBand
import com.example.domain.model.SymptomReferral
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationRequestDemo
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.PublicPharmacyOption
import com.example.domain.model.SavedSyrupScan
import com.example.domain.model.StaffAccessStatus
import com.example.domain.model.StaffConsultationCase
import com.example.domain.model.VaccineRecord
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineTempPoint
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudDataState<out T> {
  data object Loading : CloudDataState<Nothing>
  data class Success<T>(val data: T) : CloudDataState<T>
  data class Error(val message: String) : CloudDataState<Nothing>
}

class ManoCloudSyncViewModel(
  private val repository: ManoCloudRepository,
  private val currentUserId: String,
) : ViewModel() {

  val childProfileState: StateFlow<CloudDataState<ChildProfileDemo?>> =
    repository
      .observePrimaryChildProfile(currentUserId)
      .map<ChildProfileDemo?, CloudDataState<ChildProfileDemo?>> { CloudDataState.Success(it) }
      .catch { error -> emit(CloudDataState.Error(unwrapError(error, "بارگذاری پروفایل کودک"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CloudDataState.Loading)

  val doseLogsState: StateFlow<CloudDataState<List<MedicationDoseLogCloud>>> =
    repository
      .observeDoseLogs(currentUserId)
      .map<List<MedicationDoseLogCloud>, CloudDataState<List<MedicationDoseLogCloud>>> { CloudDataState.Success(it) }
      .catch { error -> emit(CloudDataState.Error(unwrapError(error, "بارگذاری دفترچه مصرف دارو"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CloudDataState.Loading)

  val feverLogsState: StateFlow<CloudDataState<List<FeverLogCloud>>> =
    repository
      .observeFeverLogs(currentUserId)
      .map<List<FeverLogCloud>, CloudDataState<List<FeverLogCloud>>> { CloudDataState.Success(it) }
      .catch { error -> emit(CloudDataState.Error(unwrapError(error, "بارگذاری ثبت دمای بدن"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CloudDataState.Loading)

  val consultationsState: StateFlow<CloudDataState<List<ConsultationRequestDemo>>> =
    repository
      .observeConsultations(currentUserId)
      .map<List<ConsultationRequestDemo>, CloudDataState<List<ConsultationRequestDemo>>> { CloudDataState.Success(it) }
      .catch { error -> emit(CloudDataState.Error(unwrapError(error, "بارگذاری درخواست‌های مشاوره"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CloudDataState.Loading)

  val bookmarkedSlugsState: StateFlow<Set<String>> =
    repository
      .observeBookmarkedSlugs(currentUserId)
      .catch { error ->
        unwrapError(error, "بارگذاری نشانک‌ها")
        emit(emptySet())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

  val visitDraftsState: StateFlow<CloudDataState<List<DoctorVisitDraft>>> =
    repository
      .observeVisitDrafts(currentUserId)
      .map<List<DoctorVisitDraft>, CloudDataState<List<DoctorVisitDraft>>> { CloudDataState.Success(it) }
      .catch { error -> emit(CloudDataState.Error(unwrapError(error, "بارگذاری پیش‌نویس‌های ویزیت"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CloudDataState.Loading)

  val vaccineRecordsState: StateFlow<CloudDataState<List<VaccineRecord>>> =
    repository
      .observeVaccineRecords(currentUserId)
      .map<List<VaccineRecord>, CloudDataState<List<VaccineRecord>>> { CloudDataState.Success(it) }
      .catch { error -> emit(CloudDataState.Error(unwrapError(error, "بارگذاری پرونده واکسیناسیون"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CloudDataState.Loading)

  val syrupScansState: StateFlow<CloudDataState<List<SavedSyrupScan>>> =
    repository
      .observeSyrupScans(currentUserId)
      .map<List<SavedSyrupScan>, CloudDataState<List<SavedSyrupScan>>> { CloudDataState.Success(it) }
      .catch { error -> emit(CloudDataState.Error(unwrapError(error, "بارگذاری اسکن‌های دارو"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CloudDataState.Loading)

  private val _cloudToastFa = MutableStateFlow<String?>(null)
  val cloudToastFa: StateFlow<String?> = _cloudToastFa.asStateFlow()

  private val _acceptingPharmacies = MutableStateFlow<List<PublicPharmacyOption>>(emptyList())
  val acceptingPharmacies: StateFlow<List<PublicPharmacyOption>> = _acceptingPharmacies.asStateFlow()

  private val _selectedPharmacyId = MutableStateFlow<String?>(null)
  val selectedPharmacyId: StateFlow<String?> = _selectedPharmacyId.asStateFlow()

  private val _serverConsultations = MutableStateFlow<List<ConsultationRequestDemo>>(emptyList())
  val serverConsultations: StateFlow<List<ConsultationRequestDemo>> = _serverConsultations.asStateFlow()

  private val _isSubmittingConsultation = MutableStateFlow(false)
  val isSubmittingConsultation: StateFlow<Boolean> = _isSubmittingConsultation.asStateFlow()

  private val _serverConsultationErrorFa = MutableStateFlow<String?>(null)
  val serverConsultationErrorFa: StateFlow<String?> = _serverConsultationErrorFa.asStateFlow()

  private val _staffAccessStatus = MutableStateFlow(StaffAccessStatus())
  val staffAccessStatus: StateFlow<StaffAccessStatus> = _staffAccessStatus.asStateFlow()

  private val _staffInboxCases = MutableStateFlow<List<StaffConsultationCase>>(emptyList())
  val staffInboxCases: StateFlow<List<StaffConsultationCase>> = _staffInboxCases.asStateFlow()

  private val _isLoadingStaffInbox = MutableStateFlow(false)
  val isLoadingStaffInbox: StateFlow<Boolean> = _isLoadingStaffInbox.asStateFlow()

  private val _staffActionInFlightCaseId = MutableStateFlow<String?>(null)
  val staffActionInFlightCaseId: StateFlow<String?> = _staffActionInFlightCaseId.asStateFlow()

  private val _staffErrorFa = MutableStateFlow<String?>(null)
  val staffErrorFa: StateFlow<String?> = _staffErrorFa.asStateFlow()

  private var hasAttemptedOfflineMigration = false

  init {
    refreshServerConsultations(silent = true)
    refreshStaffStatusAndInbox(silent = true)
  }

  fun clearCloudToast() {
    _cloudToastFa.value = null
  }

  fun clearServerConsultationError() {
    _serverConsultationErrorFa.value = null
  }

  fun clearStaffError() {
    _staffErrorFa.value = null
  }

  fun selectPharmacy(pharmacyId: String) {
    _selectedPharmacyId.value = pharmacyId
  }

  fun refreshServerConsultations(silent: Boolean = false) {
    viewModelScope.launch {
      val pharmaciesRes = repository.fetchAcceptingPharmaciesFromServer()
      pharmaciesRes.getOrNull()?.let { list ->
        _acceptingPharmacies.value = list
        if (_selectedPharmacyId.value == null || list.none { it.pharmacyId == _selectedPharmacyId.value }) {
          _selectedPharmacyId.value = list.firstOrNull { it.acceptingCases }?.pharmacyId
        }
      }

      val casesRes = repository.fetchParentConsultationsFromServer()
      if (casesRes.isSuccess) {
        _serverConsultations.value = casesRes.getOrNull().orEmpty()
        if (!silent) {
          _cloudToastFa.value = "آخرین وضعیت مشاوره‌ها از سرور به‌روزرسانی شد."
        }
      } else if (!silent) {
        val msg = casesRes.exceptionOrNull()?.localizedMessage ?: "خطا در دریافت وضعیت از سرور."
        _cloudToastFa.value = msg
      }
    }
  }

  fun refreshStaffStatusAndInbox(
    forceTokenRefresh: Boolean = false,
    explicitPharmacyId: String? = null,
    statusFilter: ConsultationStatus? = null,
    silent: Boolean = false,
  ) {
    viewModelScope.launch {
      _isLoadingStaffInbox.value = true
      if (!silent) _staffErrorFa.value = null
      try {
        val status = repository.fetchPharmacistStaffClaims(forceRefresh = forceTokenRefresh)
        _staffAccessStatus.value = status
        val targetPharmacyId =
          explicitPharmacyId?.trim()?.takeIf { it.isNotEmpty() } ?: status.pharmacyId
        if (targetPharmacyId == null || !status.isVerifiedPharmacist) {
          _staffInboxCases.value = emptyList()
          return@launch
        }
        val inboxRes = repository.fetchPharmacyInbox(pharmacyId = targetPharmacyId, statusFilter = statusFilter)
        if (inboxRes.isSuccess) {
          _staffInboxCases.value = inboxRes.getOrNull().orEmpty()
          if (!silent) {
            _cloudToastFa.value = "صندوق پرونده‌های داروخانه به‌روزرسانی شد."
          }
        } else {
          val msg = inboxRes.exceptionOrNull()?.localizedMessage ?: "خطا در دریافت پرونده‌های داروخانه."
          if (!silent) {
            _staffErrorFa.value = msg
            _cloudToastFa.value = msg
          }
        }
      } finally {
        _isLoadingStaffInbox.value = false
      }
    }
  }

  fun acknowledgeStaffCaseReceived(
    pharmacyId: String,
    caseId: String,
    internalNote: String?,
    onSuccess: () -> Unit = {},
  ) {
    if (_staffActionInFlightCaseId.value != null) return
    _staffActionInFlightCaseId.value = caseId
    _staffErrorFa.value = null
    viewModelScope.launch {
      try {
        val res =
          repository.reviewStaffConsultationCase(
            pharmacyId = pharmacyId,
            caseId = caseId,
            nextStatus = ConsultationStatus.RECEIVED,
            internalNote = internalNote,
            parentReply = null,
            pharmacistAttestation = false,
          )
        if (res.isSuccess) {
          val updated = res.getOrThrow()
          _staffInboxCases.value =
            _staffInboxCases.value.map { if (it.caseId == updated.caseId) updated else it }
          _cloudToastFa.value = "پرونده توسط داروساز پذیرش شد (وضعیت: دریافت شده)."
          refreshServerConsultations(silent = true)
          onSuccess()
        } else {
          val msg = res.exceptionOrNull()?.localizedMessage ?: "خطا در پذیرش پرونده."
          _staffErrorFa.value = msg
          _cloudToastFa.value = msg
        }
      } finally {
        _staffActionInFlightCaseId.value = null
      }
    }
  }

  fun submitStaffCaseReview(
    pharmacyId: String,
    caseId: String,
    internalNote: String?,
    parentReply: String,
    pharmacistAttestation: Boolean,
    onSuccess: () -> Unit = {},
  ) {
    if (_staffActionInFlightCaseId.value != null) return
    _staffActionInFlightCaseId.value = caseId
    _staffErrorFa.value = null
    viewModelScope.launch {
      try {
        val res =
          repository.reviewStaffConsultationCase(
            pharmacyId = pharmacyId,
            caseId = caseId,
            nextStatus = ConsultationStatus.REVIEWED,
            internalNote = internalNote,
            parentReply = parentReply,
            pharmacistAttestation = pharmacistAttestation,
          )
        if (res.isSuccess) {
          val updated = res.getOrThrow()
          _staffInboxCases.value =
            _staffInboxCases.value.map { if (it.caseId == updated.caseId) updated else it }
          _cloudToastFa.value = "پاسخ علمی با تاییدیه داروساز ثبت و قفل شد."
          refreshServerConsultations(silent = true)
          onSuccess()
        } else {
          val msg = res.exceptionOrNull()?.localizedMessage ?: "خطا در ثبت پاسخ داروساز."
          _staffErrorFa.value = msg
          _cloudToastFa.value = msg
        }
      } finally {
        _staffActionInFlightCaseId.value = null
      }
    }
  }

  fun migrateOfflineDataIfNeeded(
    localProfile: ChildProfileDemo?,
    localFevers: List<TimelineTempPoint>,
    localDoses: List<TimelineDosePoint>,
    cloudHasProfile: Boolean,
    onMigratedSuccess: () -> Unit,
  ) {
    if (hasAttemptedOfflineMigration) return
    val hasWork = (!cloudHasProfile && localProfile != null) || localFevers.isNotEmpty() || localDoses.isNotEmpty()
    if (!hasWork) {
      hasAttemptedOfflineMigration = true
      return
    }
    hasAttemptedOfflineMigration = true
    viewModelScope.launch {
      val ok =
        repository.migrateOfflineRecords(
          localProfile = localProfile,
          localFevers = localFevers,
          localDoses = localDoses,
          cloudHasProfile = cloudHasProfile,
        )
      if (ok) {
        onMigratedSuccess()
        _cloudToastFa.value = "اطلاعات ثبت‌شده آفلاین با حساب گوگل همگام‌سازی شد."
      }
    }
  }

  fun saveChildProfile(profile: ChildProfileDemo) {
    viewModelScope.launch {
      val result = repository.saveChildProfile(profile)
      _cloudToastFa.value =
        if (result.isSuccess) "پروفایل کودک با موفقیت در فضای ابری ذخیره و همگام‌سازی شد."
        else "خطا در ذخیره پروفایل ابری: ${result.exceptionOrNull()?.localizedMessage ?: "لطفاً دوباره تلاش کنید."}"
    }
  }

  fun deleteChildProfile(onDeleted: () -> Unit = {}) {
    viewModelScope.launch {
      val result = repository.deleteChildProfile()
      if (result.isSuccess) {
        onDeleted()
        _cloudToastFa.value = "پروفایل کودک از فضای ابری و دستگاه حذف شد."
      } else {
        _cloudToastFa.value = "خطا در حذف پروفایل ابری؛ حذف محلی نیز انجام نشد."
      }
    }
  }

  fun addDoseLog(
    medicationSlug: String,
    medicationNameFa: String,
    dosageFormFa: String,
    volumeMlText: String,
    givenAtLabelFa: String,
    noteFa: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val result =
        repository.addDoseLog(
          medicationSlug = medicationSlug,
          medicationNameFa = medicationNameFa,
          dosageFormFa = dosageFormFa,
          volumeMlText = volumeMlText,
          givenAtLabelFa = givenAtLabelFa,
          noteFa = noteFa,
        )
      if (result.isSuccess) {
        _cloudToastFa.value = "نوبت مصرف دارو در دفترچه ابری ثبت شد."
        onDone()
      } else {
        _cloudToastFa.value = "خطا در ثبت نوبت مصرف دارو: ${result.exceptionOrNull()?.localizedMessage ?: "نامشخص"}"
      }
    }
  }

  fun deleteDoseLog(logId: String) {
    viewModelScope.launch {
      val result = repository.deleteDoseLog(logId)
      _cloudToastFa.value =
        if (result.isSuccess) "رکورد مصرف دارو حذف شد."
        else "خطا در حذف رکورد مصرف دارو."
    }
  }

  fun addFeverLog(
    tempCelsius: Double,
    siteFa: String,
    recordedAtLabelFa: String,
    caregiverFa: String,
    noteFa: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val result =
        repository.addFeverLog(
          tempCelsius = tempCelsius,
          siteFa = siteFa,
          recordedAtLabelFa = recordedAtLabelFa,
          caregiverFa = caregiverFa,
          noteFa = noteFa,
        )
      if (result.isSuccess) {
        _cloudToastFa.value = "دمای کودک در خط زمانی ابری ثبت شد."
        onDone()
      } else {
        _cloudToastFa.value = "خطا در ثبت دما: ${result.exceptionOrNull()?.localizedMessage ?: "نامشخص"}"
      }
    }
  }

  fun deleteFeverLog(logId: String) {
    viewModelScope.launch {
      val result = repository.deleteFeverLog(logId)
      _cloudToastFa.value =
        if (result.isSuccess) "رکورد دما حذف شد."
        else "خطا در حذف رکورد دما."
    }
  }

  fun submitTrustedConsultation(
    category: ConsultationCategory,
    ageBand: AgeBand,
    questionSummaryFa: String,
    hasUrgentFlag: Boolean,
    consentAccepted: Boolean,
    referral: SymptomReferral? = null,
    onSuccess: () -> Unit = {},
  ) {
    if (_isSubmittingConsultation.value) return
    _isSubmittingConsultation.value = true
    _serverConsultationErrorFa.value = null
    viewModelScope.launch {
      try {
        val result =
          repository.submitTrustedConsultationViaServer(
            preferredPharmacyId = _selectedPharmacyId.value,
            category = category,
            ageBand = ageBand,
            questionSummaryFa = questionSummaryFa,
            hasUrgentFlag = hasUrgentFlag,
            consentAccepted = consentAccepted,
            referral = referral,
          )
        if (result.isSuccess) {
          val created = result.getOrThrow()
          _serverConsultations.value =
            (listOf(created) + _serverConsultations.value).distinctBy { it.id }
          _cloudToastFa.value = "درخواست غیرفوری پس از تایید گذرنامه تریاژ در داروخانه ثبت شد."
          onSuccess()
        } else {
          val err = result.exceptionOrNull()
          val msg =
            if (err is UrgentTriageBlockedException) {
              "${err.titleFa}: ${err.summaryFa}"
            } else {
              err?.localizedMessage ?: "خطا در ثبت امن درخواست مشاوره."
            }
          _serverConsultationErrorFa.value = msg
          _cloudToastFa.value = msg
        }
      } finally {
        _isSubmittingConsultation.value = false
      }
    }
  }

  @Deprecated("Use submitTrustedConsultation for server-verified intake.")
  fun submitConsultation(
    category: ConsultationCategory,
    ageBand: AgeBand,
    questionSummaryFa: String,
    onSuccess: () -> Unit = {},
  ) {
    submitTrustedConsultation(
      category = category,
      ageBand = ageBand,
      questionSummaryFa = questionSummaryFa,
      hasUrgentFlag = false,
      consentAccepted = true,
      onSuccess = onSuccess,
    )
  }

  @Deprecated("Status advances exclusively via trusted pharmacist Cloud Function reviewConsultationCase.")
  fun advanceConsultationStatus(consultationId: String, currentStatus: ConsultationStatus) {
    refreshServerConsultations(silent = false)
  }

  fun setBookmark(articleSlug: String, bookmarked: Boolean) {
    viewModelScope.launch {
      repository.setBookmark(articleSlug, bookmarked)
    }
  }

  fun addVisitDraft(
    titleFa: String,
    symptomContextFa: String,
    timelineSummaryFa: String,
    draftTextFa: String,
    parentNoteFa: String,
    modelName: String,
    onSavedId: (String) -> Unit = {},
  ) {
    viewModelScope.launch {
      val result =
        repository.addVisitDraft(
          titleFa = titleFa,
          symptomContextFa = symptomContextFa,
          timelineSummaryFa = timelineSummaryFa,
          draftTextFa = draftTextFa,
          parentNoteFa = parentNoteFa,
          modelName = modelName,
        )
      if (result.isSuccess) {
        _cloudToastFa.value = "پیش‌نویس ویزیت پزشک در فضای ابری ذخیره شد."
        onSavedId(result.getOrThrow())
      } else {
        _cloudToastFa.value = "خطا در ذخیره ابری پیش‌نویس: ${result.exceptionOrNull()?.localizedMessage ?: "نامشخص"}"
      }
    }
  }

  fun updateVisitDraft(
    draftId: String,
    titleFa: String,
    draftTextFa: String,
    parentNoteFa: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val result = repository.updateVisitDraft(draftId, titleFa, draftTextFa, parentNoteFa)
      if (result.isSuccess) {
        _cloudToastFa.value = "تغییرات پیش‌نویس ویزیت در فضای ابری ذخیره شد."
        onDone()
      } else {
        _cloudToastFa.value = "خطا در به‌روزرسانی پیش‌نویس ابری."
      }
    }
  }

  fun deleteVisitDraft(draftId: String) {
    viewModelScope.launch {
      val result = repository.deleteVisitDraft(draftId)
      _cloudToastFa.value =
        if (result.isSuccess) "پیش‌نویس ویزیت حذف شد."
        else "خطا در حذف پیش‌نویس ویزیت."
    }
  }

  fun upsertVaccineRecord(
    record: VaccineRecord,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val result = repository.upsertVaccineRecord(record)
      if (result.isSuccess) {
        _cloudToastFa.value = "اطلاعات واکسن در حساب کاربری همگام‌سازی شد."
        onDone()
      } else {
        _cloudToastFa.value = "خطا در ثبت ابری واکسن: ${result.exceptionOrNull()?.localizedMessage ?: "نامشخص"}"
      }
    }
  }

  fun deleteVaccineRecord(
    vaccineId: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val result = repository.deleteVaccineRecord(vaccineId)
      if (result.isSuccess) {
        _cloudToastFa.value = "سابقه واکسن پاک شد."
        onDone()
      } else {
        _cloudToastFa.value = "خطا در حذف سابقه واکسن."
      }
    }
  }

  fun upsertSyrupScan(
    scan: SavedSyrupScan,
    onDone: (String) -> Unit = {},
  ) {
    viewModelScope.launch {
      val result = repository.upsertSyrupScan(scan)
      if (result.isSuccess) {
        _cloudToastFa.value = "اسکن بطری دارو در حساب کاربری همگام‌سازی شد."
        onDone(result.getOrThrow())
      } else {
        _cloudToastFa.value = "خطا در همگام‌سازی ابری اسکن دارو: ${result.exceptionOrNull()?.localizedMessage ?: "نامشخص"}"
      }
    }
  }

  fun deleteSyrupScan(
    scanId: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val result = repository.deleteSyrupScan(scanId)
      if (result.isSuccess) {
        _cloudToastFa.value = "اسکن ذخیره‌شده حذف شد."
        onDone()
      } else {
        _cloudToastFa.value = "خطا در حذف اسکن ابری."
      }
    }
  }

  private fun unwrapError(error: Throwable, contextLabel: String): String {
    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, "Error during $contextLabel", rootError)
    return rootError.localizedMessage ?: "خطا در $contextLabel"
  }

  private companion object {
    const val TAG = "ManoCloudSyncVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
