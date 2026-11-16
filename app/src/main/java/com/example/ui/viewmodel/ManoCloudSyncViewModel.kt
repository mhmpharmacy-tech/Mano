package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FeverLogCloud
import com.example.data.ManoCloudRepository
import com.example.data.MedicationDoseLogCloud
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationRequestDemo
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.SavedSyrupScan
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

  private var hasAttemptedOfflineMigration = false

  fun clearCloudToast() {
    _cloudToastFa.value = null
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

  fun deleteChildProfile() {
    viewModelScope.launch {
      val result = repository.deleteChildProfile()
      _cloudToastFa.value =
        if (result.isSuccess) "پروفایل کودک از فضای ابری حذف شد."
        else "خطا در حذف پروفایل ابری."
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

  fun submitConsultation(
    category: ConsultationCategory,
    ageBand: AgeBand,
    questionSummaryFa: String,
    onSuccess: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val result = repository.createConsultation(category, ageBand, questionSummaryFa)
      if (result.isSuccess) {
        _cloudToastFa.value = "درخواست مشاوره غیرتشخیصی با موفقیت در حساب کاربری ثبت شد."
        onSuccess()
      } else {
        _cloudToastFa.value = "خطا در ثبت درخواست ابری: ${result.exceptionOrNull()?.localizedMessage ?: "نامشخص"}"
      }
    }
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
