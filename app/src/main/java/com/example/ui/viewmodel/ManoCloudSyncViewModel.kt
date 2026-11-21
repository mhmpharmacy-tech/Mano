package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FeverLogCloud
import com.example.data.ManoCloudRepository
import com.example.data.ManoPreferencesRepository
import com.example.data.MedicationDoseLogCloud
import com.example.data.PendingOfflineConsultation
import com.example.data.ScopedPendingChildProfile
import com.example.data.UrgentTriageBlockedException
import com.example.domain.model.AgeBand
import com.example.domain.model.SymptomReferral
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationRequestDemo
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.FollowUpNote
import com.example.domain.model.PublicPharmacyOption
import com.example.domain.util.ManoDomainLinks
import com.example.domain.util.PharmacyInvitationPolicy
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
  private val prefs: ManoPreferencesRepository? = null,
) : ViewModel() {

  private val _inMemoryScopedProfile = MutableStateFlow<ScopedPendingChildProfile?>(null)

  val scopedLocalProfileRecord: StateFlow<ScopedPendingChildProfile?> =
    prefs?.scopedChildProfile(currentUserId)
      ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)
      ?: _inMemoryScopedProfile.asStateFlow()

  private val _inMemoryPendingConsultations = MutableStateFlow<List<PendingOfflineConsultation>>(emptyList())

  val scopedPendingConsultationsRaw: StateFlow<List<PendingOfflineConsultation>> =
    prefs?.scopedPendingConsultations(currentUserId)
      ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
      ?: _inMemoryPendingConsultations.asStateFlow()

  val scopedPendingConsultations: StateFlow<List<ConsultationRequestDemo>> =
    scopedPendingConsultationsRaw
      .map { list ->
        list.map { item ->
          ConsultationRequestDemo(
            id = item.id,
            category = ConsultationCategory.entries.firstOrNull { it.name == item.categoryName } ?: ConsultationCategory.SYMPTOMS,
            childAgeBand = AgeBand.entries.firstOrNull { it.id == item.ageBandId } ?: AgeBand.TODDLER_1_3Y,
            questionSummaryFa = item.questionSummaryFa,
            hasUrgentFlag = false,
            status = ConsultationStatus.PENDING,
            createdAtLabelFa = "${item.createdAtLabelFa} • در صف ارسال آفلاین",
            followUp =
              FollowUpNote(
                scheduleFa = "ذخیره‌شده روی گوشی (آفلاین)",
                guidanceFa = "این پرسش به‌طور ایمن روی گوشی ذخیره شده است و به محض اتصال پایدار به اینترنت برای داروساز ارسال می‌شود.",
              ),
          )
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  private val _inMemoryPendingDoses = MutableStateFlow<List<TimelineDosePoint>>(emptyList())

  val scopedPendingDoseLogs: StateFlow<List<TimelineDosePoint>> =
    prefs?.scopedPendingDoseLogs(currentUserId)
      ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
      ?: _inMemoryPendingDoses.asStateFlow()

  private val _isSyncingOfflineQueue = MutableStateFlow(false)
  val isSyncingOfflineQueue: StateFlow<Boolean> = _isSyncingOfflineQueue.asStateFlow()

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

  private val _pendingInvitationPharmacyId = MutableStateFlow<String?>(null)
  val pendingInvitationPharmacyId: StateFlow<String?> = _pendingInvitationPharmacyId.asStateFlow()

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
    syncPendingOfflineQueue(silent = true)
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
    // Only choices returned by the verified server registry may be selected.
    val verifiedId = PharmacyInvitationPolicy.resolveAcceptingPharmacyId(
      pharmacyId, _acceptingPharmacies.value,
    ) ?: return
    _pendingInvitationPharmacyId.value = null
    _selectedPharmacyId.value = verifiedId
  }

  fun requestPharmacyFromInvitation(pharmacyId: String) {
    val id = pharmacyId.trim()
    if (!ManoDomainLinks.isValidPublicPharmacyId(id)) return
    _pendingInvitationPharmacyId.value = id
    _selectedPharmacyId.value = null
    // Delay actual selection until the live verified registry includes the ID.
    applyVerifiedPharmacyInvitation()
  }

  fun clearPharmacyInvitation() {
    _pendingInvitationPharmacyId.value = null
  }

  private fun applyVerifiedPharmacyInvitation() {
    val invitedId = _pendingInvitationPharmacyId.value ?: return
    val acceptedId = PharmacyInvitationPolicy.resolveAcceptingPharmacyId(
      invitedId, _acceptingPharmacies.value,
    )
    if (acceptedId != null) {
      _selectedPharmacyId.value = acceptedId
      _pendingInvitationPharmacyId.value = null
    } else {
      // No fallback to a different pharmacy: parents must explicitly choose it.
      _selectedPharmacyId.value = null
    }
  }

  fun refreshServerConsultations(silent: Boolean = false) {
    viewModelScope.launch {
      val pharmaciesRes = repository.fetchAcceptingPharmaciesFromServer()
      pharmaciesRes.getOrNull()?.let { list ->
        _acceptingPharmacies.value = list
        if (_pendingInvitationPharmacyId.value != null) {
          applyVerifiedPharmacyInvitation()
        } else if (_selectedPharmacyId.value != null &&
          list.none { it.pharmacyId == _selectedPharmacyId.value && it.acceptingCases }) {
          // Ordinary entry never auto-assigns a random pharmacy. Parents must
          // explicitly select it, even if a malformed invitation was opened.
          _selectedPharmacyId.value = null
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
      // 1. Offline-first immediate save scoped strictly to currentUserId so the user is NEVER blocked by connectivity.
      val localRecord =
        ScopedPendingChildProfile(
          profile = profile,
          pendingCloudSync = true,
          savedAtEpochMs = System.currentTimeMillis(),
        )
      _inMemoryScopedProfile.value = localRecord
      prefs?.saveScopedChildProfile(currentUserId, profile, pendingCloudSync = true)

      // 2. Attempt cloud sync right away; keep local copy if offline/unavailable.
      val result = repository.saveChildProfile(profile)
      if (result.isSuccess) {
        _inMemoryScopedProfile.value = localRecord.copy(pendingCloudSync = false)
        prefs?.saveScopedChildProfile(currentUserId, profile, pendingCloudSync = false)
        _cloudToastFa.value = "پروفایل کودک با موفقیت ذخیره و با فضای ابری همگام‌سازی شد."
      } else {
        _cloudToastFa.value =
          "پروفایل کودک روی گوشی ذخیره شد و پس از برقراری اینترنت، به‌صورت خودکار با ابر همگام می‌شود."
      }
    }
  }

  fun deleteChildProfile(onDeleted: () -> Unit = {}) {
    viewModelScope.launch {
      val result = repository.deleteChildProfile()
      if (result.isSuccess) {
        _inMemoryScopedProfile.value = null
        prefs?.clearScopedChildProfile(currentUserId)
        onDeleted()
        _cloudToastFa.value = "پروفایل کودک از فضای ابری و دستگاه حذف شد."
      } else if (_inMemoryScopedProfile.value?.pendingCloudSync == true || scopedLocalProfileRecord.value?.pendingCloudSync == true) {
        // Profile existed only locally while offline: allow clean local deletion.
        _inMemoryScopedProfile.value = null
        prefs?.clearScopedChildProfile(currentUserId)
        onDeleted()
        _cloudToastFa.value = "پروفایل آفلاین از حافظه این حساب حذف شد."
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
      val now = System.currentTimeMillis()
      val result =
        repository.addDoseLog(
          medicationSlug = medicationSlug,
          medicationNameFa = medicationNameFa,
          dosageFormFa = dosageFormFa,
          volumeMlText = volumeMlText,
          givenAtLabelFa = givenAtLabelFa,
          noteFa = noteFa,
          administeredAtMs = now,
        )
      if (result.isSuccess) {
        _cloudToastFa.value = "نوبت مصرف دارو در دفترچه ابری ثبت شد."
        onDone()
      } else {
        val offlineDose =
          TimelineDosePoint(
            id = "pending_dose_${currentUserId.take(6)}_$now",
            medicationSlug = medicationSlug,
            medicationNameFa = medicationNameFa,
            dosageFormFa = dosageFormFa,
            volumeMlText = volumeMlText,
            timestampMs = com.example.domain.util.FeverDoseTimelineEngine.resolveDoseTimeMs(givenAtLabelFa, now) ?: now,
            givenAtLabelFa = givenAtLabelFa.trim().ifEmpty { com.example.domain.util.FeverDoseTimelineEngine.formatHourMinute(now) },
            noteFa = noteFa.trim(),
            isSyncedCloud = false,
          )
        _inMemoryPendingDoses.value = listOf(offlineDose) + _inMemoryPendingDoses.value
        prefs?.addScopedPendingDoseLog(currentUserId, offlineDose)
        _cloudToastFa.value = "نوبت مصرف دارو روی گوشی ثبت شد و پس از اتصال به اینترنت با ابر همگام می‌شود."
        onDone()
      }
    }
  }

  fun syncPendingOfflineQueue(silent: Boolean = false) {
    if (_isSyncingOfflineQueue.value) return
    viewModelScope.launch {
      _isSyncingOfflineQueue.value = true
      var syncedCount = 0
      var remainingCount = 0
      try {
        val pendingProfile = scopedLocalProfileRecord.value ?: _inMemoryScopedProfile.value
        if (pendingProfile != null && pendingProfile.pendingCloudSync) {
          val res = repository.saveChildProfile(pendingProfile.profile)
          if (res.isSuccess) {
            syncedCount++
            _inMemoryScopedProfile.value = pendingProfile.copy(pendingCloudSync = false)
            prefs?.saveScopedChildProfile(currentUserId, pendingProfile.profile, pendingCloudSync = false)
          } else {
            remainingCount++
          }
        }

        val pendingDoses = scopedPendingDoseLogs.value.ifEmpty { _inMemoryPendingDoses.value }
        pendingDoses.forEach { dose ->
          val res =
            repository.addDoseLog(
              medicationSlug = dose.medicationSlug.ifBlank { "custom-medication" },
              medicationNameFa = dose.medicationNameFa.ifBlank { "دارو" },
              dosageFormFa = dose.dosageFormFa.ifBlank { "شربت" },
              volumeMlText = dose.volumeMlText.ifBlank { "-" },
              givenAtLabelFa = dose.givenAtLabelFa.ifBlank { "هم‌اکنون" },
              noteFa = dose.noteFa,
              administeredAtMs = dose.timestampMs,
            )
          if (res.isSuccess) {
            syncedCount++
            _inMemoryPendingDoses.value = _inMemoryPendingDoses.value.filterNot { it.id == dose.id }
            prefs?.removeScopedPendingDoseLog(currentUserId, dose.id)
          } else {
            remainingCount++
          }
        }

        val pendingConsults = scopedPendingConsultationsRaw.value.ifEmpty { _inMemoryPendingConsultations.value }
        if (pendingConsults.isNotEmpty()) {
          if (_acceptingPharmacies.value.isEmpty()) {
            repository.fetchAcceptingPharmaciesFromServer().getOrNull()?.let { list ->
              _acceptingPharmacies.value = list
            }
          }
          val targetPharmacyId =
            _selectedPharmacyId.value ?: _acceptingPharmacies.value.firstOrNull { it.acceptingCases }?.pharmacyId
          if (targetPharmacyId != null) {
            pendingConsults.forEach { item ->
              val cat = ConsultationCategory.entries.firstOrNull { it.name == item.categoryName } ?: ConsultationCategory.SYMPTOMS
              val band = AgeBand.entries.firstOrNull { it.id == item.ageBandId } ?: AgeBand.TODDLER_1_3Y
              val res =
                repository.submitTrustedConsultationViaServer(
                  preferredPharmacyId = targetPharmacyId,
                  category = cat,
                  ageBand = band,
                  questionSummaryFa = item.questionSummaryFa,
                  hasUrgentFlag = false,
                  consentAccepted = true,
                  referral = null,
                )
              if (res.isSuccess) {
                syncedCount++
                val created = res.getOrThrow()
                _serverConsultations.value = (listOf(created) + _serverConsultations.value).distinctBy { it.id }
                _inMemoryPendingConsultations.value = _inMemoryPendingConsultations.value.filterNot { it.id == item.id }
                prefs?.removeScopedPendingConsultation(currentUserId, item.id)
              } else {
                remainingCount++
              }
            }
          } else {
            remainingCount += pendingConsults.size
          }
        }

        if (!silent) {
          _cloudToastFa.value =
            when {
              syncedCount > 0 && remainingCount == 0 ->
                "همگام‌سازی کامل شد ($syncedCount مورد با ابر همگام گردید)."
              syncedCount > 0 && remainingCount > 0 ->
                "$syncedCount مورد همگام شد؛ $remainingCount مورد همچنان در انتظار اتصال پایدار است."
              remainingCount > 0 ->
                "اتصال به سرور برقرار نیست؛ اطلاعات شما همچنان ایمن روی گوشی نگهداری می‌شوند."
              else ->
                "همه اطلاعات شما با ابر همگام است."
            }
        }
      } finally {
        _isSyncingOfflineQueue.value = false
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
    if (hasUrgentFlag) {
      val urgentMsg =
        "در صورت وجود علائم هشدار فوری، مشاوره آنلاین جایگزین معاینه فوری نیست؛ لطفاً بلافاصله با اورژانس ۱۱۵ تماس بگیرید."
      _serverConsultationErrorFa.value = urgentMsg
      _cloudToastFa.value = urgentMsg
      return
    }
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
          _cloudToastFa.value = "پرسش به داروخانه ارسال شد. وضعیت آن را در بخش پیگیری ببینید."
          onSuccess()
        } else {
          val err = result.exceptionOrNull()
          if (err is UrgentTriageBlockedException) {
            val msg = "${err.titleFa}: ${err.summaryFa}"
            _serverConsultationErrorFa.value = msg
            _cloudToastFa.value = msg
          } else {
            // Offline or unreachable server: queue non-urgent consultation locally scoped to currentUserId!
            val now = System.currentTimeMillis()
            val pendingItem =
              PendingOfflineConsultation(
                id = "OFFLINE-CASE-${java.util.UUID.randomUUID()}",
                categoryName = category.name,
                ageBandId = ageBand.id,
                questionSummaryFa = questionSummaryFa.take(600),
                createdAtLabelFa = com.example.domain.util.FeverDoseTimelineEngine.formatHourMinute(now),
                savedAtEpochMs = now,
                pendingCloudSync = true,
              )
            _inMemoryPendingConsultations.value = listOf(pendingItem) + _inMemoryPendingConsultations.value
            prefs?.addScopedPendingConsultation(currentUserId, pendingItem)
            _serverConsultationErrorFa.value = null
            _cloudToastFa.value =
              "اتصال به سرور برقرار نبود؛ پرسش شما ایمن روی گوشی ذخیره شد و پس از اتصال به اینترنت با ابر همگام می‌شود."
            onSuccess()
          }
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
