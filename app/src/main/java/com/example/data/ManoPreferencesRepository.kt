package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationRequestDemo
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.GrowthMeasurement
import com.example.domain.model.NutritionPlanResult
import com.example.domain.model.PediatricTreatmentPlan
import com.example.domain.model.SavedSyrupScan
import com.example.domain.model.VaccineRecord
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineTempPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.manoDataStore: DataStore<Preferences> by preferencesDataStore(name = "mano_kid_prefs")

class ManoPreferencesRepository(private val context: Context) {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val keyThemeMode = stringPreferencesKey("theme_mode")
  private val keyHapticIntensity = stringPreferencesKey("haptic_intensity")
  private val keyWebGlQuality = stringPreferencesKey("webgl_quality")
  private val keyOnboardingDone = booleanPreferencesKey("onboarding_done")
  private val keySelectedAgeBand = stringPreferencesKey("selected_age_band")
  private val keyChildProfile = stringPreferencesKey("child_profile_demo")
  private val keyChildHeightCm = doublePreferencesKey("child_height_cm")
  private val keyChildBirthDateEpochMs = longPreferencesKey("child_birth_date_epoch_ms")
  private val keyBookmarks = stringSetPreferencesKey("bookmarked_slugs")
  private val keyCheckedMilestones = stringSetPreferencesKey("checked_milestones")
  private val keyCheckedMedSafety = stringSetPreferencesKey("checked_med_safety")
  private val keyNotifiedVaccines = stringSetPreferencesKey("notified_vaccine_keys")
  private val keyOfflineQuestions = stringPreferencesKey("offline_consultations")
  private val keyLocalFeverLogs = stringPreferencesKey("local_fever_logs")
  private val keyLocalDoseLogs = stringPreferencesKey("local_dose_logs")
  private val keyLocalVisitDrafts = stringPreferencesKey("local_visit_drafts")
  private val keySavedNutritionPlans = stringPreferencesKey("saved_nutrition_plans")
  private val keyLocalVaccineRecords = stringPreferencesKey("local_vaccine_records")
  private val keyVaccineReminderSettings = stringPreferencesKey("vaccine_reminder_settings")
  private val keySavedSyrupScans = stringPreferencesKey("saved_syrup_scans")
  private val keyTreatmentPlan = stringPreferencesKey("treatment_plan")
  private val keyGalacticSelection = stringPreferencesKey("galactic_selection")

  private fun scopedKey(prefix: String, uid: String): Preferences.Key<String> =
    stringPreferencesKey("${prefix}_${uid.trim().ifBlank { "guest" }}")

  val themeMode: Flow<ManoThemeMode> =
    context.manoDataStore.data.map { prefs ->
      val id = prefs[keyThemeMode]
      ManoThemeMode.entries.firstOrNull { it.id == id } ?: ManoThemeMode.SYSTEM
    }

  val hapticIntensity: Flow<ManoHapticIntensity> =
    context.manoDataStore.data.map { prefs ->
      val id = prefs[keyHapticIntensity]
      ManoHapticIntensity.entries.firstOrNull { it.id == id } ?: ManoHapticIntensity.MEDIUM
    }

  val webGlQuality: Flow<ManoWebGlQuality> =
    context.manoDataStore.data.map { prefs ->
      val id = prefs[keyWebGlQuality]
      ManoWebGlQuality.entries.firstOrNull { it.id == id } ?: ManoWebGlQuality.AUTO
    }

  val onboardingCompleted: Flow<Boolean> =
    context.manoDataStore.data.map { prefs -> prefs[keyOnboardingDone] ?: false }

  val selectedAgeBand: Flow<AgeBand?> =
    context.manoDataStore.data.map { prefs ->
      val id = prefs[keySelectedAgeBand]
      AgeBand.entries.firstOrNull { it.id == id }
    }

  val childProfile: Flow<ChildProfileDemo?> =
    context.manoDataStore.data.map { prefs ->
      prefs[keyChildProfile]?.let { raw ->
        runCatching { json.decodeFromString<ChildProfileDemo>(raw) }.getOrNull()
      }
    }

  val childHeightCm: Flow<Double?> =
    context.manoDataStore.data.map { prefs -> prefs[keyChildHeightCm] }

  val childBirthDateEpochMs: Flow<Long?> =
    context.manoDataStore.data.map { prefs -> prefs[keyChildBirthDateEpochMs] }

  val bookmarkedSlugs: Flow<Set<String>> =
    context.manoDataStore.data.map { prefs -> prefs[keyBookmarks] ?: emptySet() }

  val checkedMilestoneIds: Flow<Set<String>> =
    context.manoDataStore.data.map { prefs -> prefs[keyCheckedMilestones] ?: emptySet() }

  val checkedMedSafetyIds: Flow<Set<String>> =
    context.manoDataStore.data.map { prefs -> prefs[keyCheckedMedSafety] ?: emptySet() }

  val notifiedVaccineReminderKeys: Flow<Set<String>> =
    context.manoDataStore.data.map { prefs -> prefs[keyNotifiedVaccines] ?: emptySet() }

  val offlineQuestions: Flow<List<ConsultationRequestDemo>> =
    context.manoDataStore.data.map { prefs ->
      decodeList(prefs[keyOfflineQuestions])
    }

  val localFeverLogs: Flow<List<TimelineTempPoint>> =
    context.manoDataStore.data.map { prefs -> decodeList(prefs[keyLocalFeverLogs]) }

  val localDoseLogs: Flow<List<TimelineDosePoint>> =
    context.manoDataStore.data.map { prefs -> decodeList(prefs[keyLocalDoseLogs]) }

  val localVisitDrafts: Flow<List<DoctorVisitDraft>> =
    context.manoDataStore.data.map { prefs -> decodeList(prefs[keyLocalVisitDrafts]) }

  val savedNutritionPlans: Flow<List<NutritionPlanResult>> =
    context.manoDataStore.data.map { prefs -> decodeList(prefs[keySavedNutritionPlans]) }

  val localVaccineRecords: Flow<List<VaccineRecord>> =
    context.manoDataStore.data.map { prefs -> decodeList(prefs[keyLocalVaccineRecords]) }

  val vaccineReminderSettings: Flow<VaccineReminderSettings> =
    context.manoDataStore.data.map { prefs ->
      prefs[keyVaccineReminderSettings]?.let { raw ->
        runCatching { json.decodeFromString<VaccineReminderSettings>(raw) }.getOrNull()
      } ?: VaccineReminderSettings()
    }

  val savedSyrupScans: Flow<List<SavedSyrupScan>> =
    context.manoDataStore.data.map { prefs -> decodeList(prefs[keySavedSyrupScans]) }

  val treatmentPlan: Flow<PediatricTreatmentPlan> =
    context.manoDataStore.data.map { prefs ->
      prefs[keyTreatmentPlan]?.let { raw ->
        runCatching { json.decodeFromString<PediatricTreatmentPlan>(raw) }.getOrNull()
      } ?: PediatricTreatmentPlan()
    }

  val galacticSelection: Flow<GalacticSelection?> =
    context.manoDataStore.data.map { prefs ->
      prefs[keyGalacticSelection]?.let { raw ->
        runCatching { json.decodeFromString<GalacticSelection>(raw) }.getOrNull()
      }
    }

  fun growthHistory(uid: String?): Flow<List<GrowthMeasurement>> =
    context.manoDataStore.data.map { prefs ->
      decodeList(prefs[scopedKey("growth_history", uid ?: "local")])
    }

  fun scopedChildProfile(uid: String): Flow<ScopedPendingChildProfile?> =
    context.manoDataStore.data.map { prefs ->
      prefs[scopedKey("scoped_profile", uid)]?.let { raw ->
        runCatching { json.decodeFromString<ScopedPendingChildProfile>(raw) }.getOrNull()
      }
    }

  fun scopedPendingConsultations(uid: String): Flow<List<PendingOfflineConsultation>> =
    context.manoDataStore.data.map { prefs ->
      decodeList(prefs[scopedKey("scoped_consultations", uid)])
    }

  fun scopedPendingDoseLogs(uid: String): Flow<List<TimelineDosePoint>> =
    context.manoDataStore.data.map { prefs ->
      decodeList(prefs[scopedKey("scoped_doses", uid)])
    }

  suspend fun setThemeMode(mode: ManoThemeMode) {
    context.manoDataStore.edit { it[keyThemeMode] = mode.id }
  }

  suspend fun setHapticIntensity(intensity: ManoHapticIntensity) {
    context.manoDataStore.edit { it[keyHapticIntensity] = intensity.id }
  }

  suspend fun setWebGlQuality(quality: ManoWebGlQuality) {
    context.manoDataStore.edit { it[keyWebGlQuality] = quality.id }
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.manoDataStore.edit { it[keyOnboardingDone] = completed }
  }

  suspend fun setSelectedAgeBand(band: AgeBand?) {
    context.manoDataStore.edit {
      if (band == null) it.remove(keySelectedAgeBand) else it[keySelectedAgeBand] = band.id
    }
  }

  suspend fun saveChildProfile(profile: ChildProfileDemo?) {
    context.manoDataStore.edit {
      if (profile == null) it.remove(keyChildProfile)
      else it[keyChildProfile] = json.encodeToString(ChildProfileDemo.serializer(), profile)
    }
  }

  suspend fun setChildHeightCm(heightCm: Double?) {
    context.manoDataStore.edit {
      if (heightCm == null) it.remove(keyChildHeightCm) else it[keyChildHeightCm] = heightCm
    }
  }

  suspend fun setChildBirthDateEpochMs(epochMs: Long?) {
    context.manoDataStore.edit {
      if (epochMs == null) it.remove(keyChildBirthDateEpochMs) else it[keyChildBirthDateEpochMs] = epochMs
    }
  }

  suspend fun toggleBookmark(slug: String) {
    context.manoDataStore.edit { prefs ->
      val current = prefs[keyBookmarks] ?: emptySet()
      prefs[keyBookmarks] = if (slug in current) current - slug else current + slug
    }
  }

  suspend fun toggleMilestone(id: String) {
    context.manoDataStore.edit { prefs ->
      val current = prefs[keyCheckedMilestones] ?: emptySet()
      prefs[keyCheckedMilestones] = if (id in current) current - id else current + id
    }
  }

  suspend fun toggleMedSafetyCheck(id: String) {
    context.manoDataStore.edit { prefs ->
      val current = prefs[keyCheckedMedSafety] ?: emptySet()
      prefs[keyCheckedMedSafety] = if (id in current) current - id else current + id
    }
  }

  suspend fun resetMedSafetyChecklist() {
    context.manoDataStore.edit { it[keyCheckedMedSafety] = emptySet() }
  }

  suspend fun markVaccineReminderKeysNotified(keys: Set<String>) {
    if (keys.isEmpty()) return
    context.manoDataStore.edit { prefs ->
      prefs[keyNotifiedVaccines] = (prefs[keyNotifiedVaccines] ?: emptySet()) + keys
    }
  }

  suspend fun saveOfflineQuestion(item: ConsultationRequestDemo) {
    context.manoDataStore.edit { prefs ->
      val current: List<ConsultationRequestDemo> = decodeList(prefs[keyOfflineQuestions])
      val updated = listOf(item) + current.filterNot { it.id == item.id }
      prefs[keyOfflineQuestions] = json.encodeToString(updated.take(40))
    }
  }

  suspend fun saveLocalFeverLog(item: TimelineTempPoint) {
    context.manoDataStore.edit { prefs ->
      val current: List<TimelineTempPoint> = decodeList(prefs[keyLocalFeverLogs])
      val updated = (listOf(item) + current.filterNot { it.id == item.id }).take(60)
      prefs[keyLocalFeverLogs] = json.encodeToString(updated)
    }
  }

  suspend fun deleteLocalFeverLog(id: String) {
    context.manoDataStore.edit { prefs ->
      val current: List<TimelineTempPoint> = decodeList(prefs[keyLocalFeverLogs])
      prefs[keyLocalFeverLogs] = json.encodeToString(current.filterNot { it.id == id })
    }
  }

  suspend fun saveLocalDoseLog(item: TimelineDosePoint) {
    context.manoDataStore.edit { prefs ->
      val current: List<TimelineDosePoint> = decodeList(prefs[keyLocalDoseLogs])
      val updated = (listOf(item) + current.filterNot { it.id == item.id }).take(60)
      prefs[keyLocalDoseLogs] = json.encodeToString(updated)
    }
  }

  suspend fun deleteLocalDoseLog(id: String) {
    context.manoDataStore.edit { prefs ->
      val current: List<TimelineDosePoint> = decodeList(prefs[keyLocalDoseLogs])
      prefs[keyLocalDoseLogs] = json.encodeToString(current.filterNot { it.id == id })
    }
  }

  suspend fun saveLocalVisitDraft(draft: DoctorVisitDraft) {
    context.manoDataStore.edit { prefs ->
      val current: List<DoctorVisitDraft> = decodeList(prefs[keyLocalVisitDrafts])
      val updated = (listOf(draft) + current.filterNot { it.id == draft.id }).take(25)
      prefs[keyLocalVisitDrafts] = json.encodeToString(updated)
    }
  }

  suspend fun deleteLocalVisitDraft(id: String) {
    context.manoDataStore.edit { prefs ->
      val current: List<DoctorVisitDraft> = decodeList(prefs[keyLocalVisitDrafts])
      prefs[keyLocalVisitDrafts] = json.encodeToString(current.filterNot { it.id == id })
    }
  }

  suspend fun saveNutritionPlan(plan: NutritionPlanResult) {
    context.manoDataStore.edit { prefs ->
      val current: List<NutritionPlanResult> = decodeList(prefs[keySavedNutritionPlans])
      val updated = (listOf(plan) + current.filterNot { it.id == plan.id }).take(20)
      prefs[keySavedNutritionPlans] = json.encodeToString(updated)
    }
  }

  suspend fun deleteNutritionPlan(id: String) {
    context.manoDataStore.edit { prefs ->
      val current: List<NutritionPlanResult> = decodeList(prefs[keySavedNutritionPlans])
      prefs[keySavedNutritionPlans] = json.encodeToString(current.filterNot { it.id == id })
    }
  }

  suspend fun upsertLocalVaccineRecord(record: VaccineRecord) {
    context.manoDataStore.edit { prefs ->
      val current: List<VaccineRecord> = decodeList(prefs[keyLocalVaccineRecords])
      val updated = listOf(record) + current.filterNot { it.vaccineId == record.vaccineId }
      prefs[keyLocalVaccineRecords] = json.encodeToString(updated)
    }
  }

  suspend fun deleteLocalVaccineRecord(vaccineId: String) {
    context.manoDataStore.edit { prefs ->
      val current: List<VaccineRecord> = decodeList(prefs[keyLocalVaccineRecords])
      prefs[keyLocalVaccineRecords] = json.encodeToString(current.filterNot { it.vaccineId == vaccineId })
    }
  }

  suspend fun updateVaccineReminderSettings(settings: VaccineReminderSettings) {
    context.manoDataStore.edit {
      it[keyVaccineReminderSettings] = json.encodeToString(VaccineReminderSettings.serializer(), settings)
    }
  }

  suspend fun upsertSavedSyrupScan(scan: SavedSyrupScan) {
    context.manoDataStore.edit { prefs ->
      val current: List<SavedSyrupScan> = decodeList(prefs[keySavedSyrupScans])
      val updated = (listOf(scan) + current.filterNot { it.id == scan.id }).take(25)
      prefs[keySavedSyrupScans] = json.encodeToString(updated)
    }
  }

  suspend fun deleteSavedSyrupScan(id: String) {
    context.manoDataStore.edit { prefs ->
      val current: List<SavedSyrupScan> = decodeList(prefs[keySavedSyrupScans])
      prefs[keySavedSyrupScans] = json.encodeToString(current.filterNot { it.id == id })
    }
  }

  suspend fun saveTreatmentPlan(plan: PediatricTreatmentPlan) {
    context.manoDataStore.edit {
      it[keyTreatmentPlan] = json.encodeToString(PediatricTreatmentPlan.serializer(), plan)
    }
  }

  suspend fun saveGalacticSelection(selection: GalacticSelection?) {
    context.manoDataStore.edit {
      if (selection == null) it.remove(keyGalacticSelection)
      else it[keyGalacticSelection] = json.encodeToString(GalacticSelection.serializer(), selection)
    }
  }

  suspend fun addGrowthMeasurement(uid: String?, item: GrowthMeasurement) {
    val key = scopedKey("growth_history", uid ?: "local")
    context.manoDataStore.edit { prefs ->
      val current: List<GrowthMeasurement> = decodeList(prefs[key])
      val updated = (listOf(item) + current.filterNot { it.id == item.id }).take(60)
      prefs[key] = json.encodeToString(updated)
    }
  }

  suspend fun saveScopedChildProfile(uid: String, profile: ChildProfileDemo, pendingCloudSync: Boolean) {
    val key = scopedKey("scoped_profile", uid)
    val wrapped = ScopedPendingChildProfile(profile = profile, pendingCloudSync = pendingCloudSync)
    context.manoDataStore.edit {
      it[key] = json.encodeToString(ScopedPendingChildProfile.serializer(), wrapped)
    }
  }

  suspend fun clearScopedChildProfile(uid: String) {
    val key = scopedKey("scoped_profile", uid)
    context.manoDataStore.edit { it.remove(key) }
  }

  suspend fun addScopedPendingConsultation(uid: String, item: PendingOfflineConsultation) {
    val key = scopedKey("scoped_consultations", uid)
    context.manoDataStore.edit { prefs ->
      val current: List<PendingOfflineConsultation> = decodeList(prefs[key])
      val updated = (listOf(item) + current.filterNot { it.id == item.id }).take(40)
      prefs[key] = json.encodeToString(updated)
    }
  }

  suspend fun removeScopedPendingConsultation(uid: String, id: String) {
    val key = scopedKey("scoped_consultations", uid)
    context.manoDataStore.edit { prefs ->
      val current: List<PendingOfflineConsultation> = decodeList(prefs[key])
      prefs[key] = json.encodeToString(current.filterNot { it.id == id })
    }
  }

  suspend fun addScopedPendingDoseLog(uid: String, item: TimelineDosePoint) {
    val key = scopedKey("scoped_doses", uid)
    context.manoDataStore.edit { prefs ->
      val current: List<TimelineDosePoint> = decodeList(prefs[key])
      val updated = (listOf(item) + current.filterNot { it.id == item.id }).take(60)
      prefs[key] = json.encodeToString(updated)
    }
  }

  suspend fun removeScopedPendingDoseLog(uid: String, id: String) {
    val key = scopedKey("scoped_doses", uid)
    context.manoDataStore.edit { prefs ->
      val current: List<TimelineDosePoint> = decodeList(prefs[key])
      prefs[key] = json.encodeToString(current.filterNot { it.id == id })
    }
  }

  suspend fun eraseAllLocalHealthData() {
    context.manoDataStore.edit { prefs ->
      prefs.remove(keyChildProfile)
      prefs.remove(keyChildHeightCm)
      prefs.remove(keyChildBirthDateEpochMs)
      prefs.remove(keyOfflineQuestions)
      prefs.remove(keyLocalFeverLogs)
      prefs.remove(keyLocalDoseLogs)
      prefs.remove(keyLocalVisitDrafts)
      prefs.remove(keySavedNutritionPlans)
      prefs.remove(keyLocalVaccineRecords)
      prefs.remove(keySavedSyrupScans)
      prefs.remove(keyTreatmentPlan)
      prefs.remove(keyCheckedMilestones)
      prefs.remove(keyCheckedMedSafety)
      prefs.remove(keyNotifiedVaccines)
    }
  }

  private inline fun <reified T> decodeList(raw: String?): List<T> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { json.decodeFromString<List<T>>(raw) }.getOrDefault(emptyList())
  }
}
