package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationRequestDemo
import com.example.domain.model.GrowthMeasurement
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.NutritionPlanResult
import com.example.domain.model.SavedSyrupScan
import com.example.domain.model.VaccineRecord
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineTempPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.manoDataStore: DataStore<Preferences> by preferencesDataStore(name = "mano_ui_prefs")

data class GalacticOrbitSelection(
  val ringName: String,
  val topicId: String,
)

enum class ManoThemeMode(val id: String, val labelFa: String) {
  SYSTEM("system", "هماهنگ با سیستم"),
  LIGHT("light", "حالت روز"),
  DARK("dark", "حالت شب"),
}

enum class ManoHapticIntensity(val id: String, val labelFa: String, val summaryFa: String) {
  OFF("off", "خاموش", "بدون بازخورد لرزشی"),
  LIGHT("light", "ملایم", "ضربه بسیار نرم (مناسب شب و آرامش)"),
  MEDIUM("medium", "متعادل", "شدت استاندارد پیشنهادی برای تعامل روزمره"),
  STRONG("strong", "قوی", "لرزش واضح‌تر برای گوشی‌های با موتور لرزش ضعیف"),
}

enum class ManoWebGlQuality(val id: String, val labelFa: String, val summaryFa: String) {
  AUTO("auto", "خودکار", "تنظیم هوشمند بر اساس پردازنده و رم دستگاه"),
  BALANCED("balanced", "میان‌رده / روان", "بهینه برای گوشی‌های میان‌رده (مصرف باتری کمتر و نرخ فریم پایدار)"),
  HIGH("high", "کیفیت بالا", "وضوح کامل، سایه نرم و حداکثر ذرات معلق"),
}

/**
 * Stores UI preferences, optional growth height, doctor visit drafts, and saved Gemini nutrition plans.
 */
class ManoPreferencesRepository(private val context: Context) {

  private val json = Json { ignoreUnknownKeys = true }

  private object Keys {
    val BOOKMARKED_SLUGS = stringSetPreferencesKey("bookmarked_article_slugs")
    val SELECTED_AGE_BAND = stringPreferencesKey("selected_age_band")
    val SAVED_VISIT_DRAFTS_JSON = stringPreferencesKey("saved_doctor_visit_drafts_json")
    val SAVED_NUTRITION_PLANS_JSON = stringPreferencesKey("saved_nutrition_plans_json")
    val CHILD_HEIGHT_CM = stringPreferencesKey("child_height_cm")
    val CHECKED_MED_SAFETY_IDS = stringSetPreferencesKey("checked_med_safety_checklist_ids")
    val CHILD_BIRTH_DATE_EPOCH_MS = longPreferencesKey("child_birth_date_epoch_ms")
    val VACCINE_RECORDS_JSON = stringPreferencesKey("vaccine_records_json")
    val VACCINE_REMINDER_ENABLED = booleanPreferencesKey("vaccine_reminder_enabled")
    val VACCINE_REMINDER_LEAD_DAYS = intPreferencesKey("vaccine_reminder_lead_days")
    val NOTIFIED_VACCINE_REMINDER_KEYS = stringSetPreferencesKey("notified_vaccine_reminder_keys")
    val CHILD_ALIAS_FA = stringPreferencesKey("child_alias_fa")
    val GALACTIC_RING = stringPreferencesKey("galactic_orbit_active_ring")
    val GALACTIC_TOPIC_ID = stringPreferencesKey("galactic_orbit_active_topic_id")
    val SAVED_SYRUP_SCANS_JSON = stringPreferencesKey("saved_syrup_scans_json")
    val THEME_MODE = stringPreferencesKey("mano_theme_mode")
    val LOCAL_CHILD_PROFILE_JSON = stringPreferencesKey("local_child_profile_json")
    val OFFLINE_QUESTIONS = stringPreferencesKey("offline_question_drafts")
    const val GROWTH_PREFIX = "growth_measurements_"
    fun growth(uid: String?) = stringPreferencesKey(GROWTH_PREFIX + (uid?.let { "user_$it" } ?: "guest"))
    val LOCAL_FEVER_LOGS_JSON = stringPreferencesKey("local_fever_logs_json")
    val LOCAL_DOSE_LOGS_JSON = stringPreferencesKey("local_dose_logs_json")
    val CHECKED_MILESTONE_IDS = stringSetPreferencesKey("checked_milestone_ids")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("mano_onboarding_completed")
    val HAPTIC_INTENSITY = stringPreferencesKey("mano_haptic_intensity")
    val WEBGL_QUALITY = stringPreferencesKey("mano_webgl_quality")
  }

  val bookmarkedSlugs: Flow<Set<String>> =
    context.manoDataStore.data.map { prefs -> prefs[Keys.BOOKMARKED_SLUGS] ?: emptySet() }

  val selectedAgeBand: Flow<AgeBand?> =
    context.manoDataStore.data.map { prefs ->
      val id = prefs[Keys.SELECTED_AGE_BAND]
      AgeBand.entries.firstOrNull { it.id == id }
    }

  val childHeightCm: Flow<String> =
    context.manoDataStore.data.map { prefs -> prefs[Keys.CHILD_HEIGHT_CM].orEmpty() }

  val checkedMedSafetyIds: Flow<Set<String>> =
    context.manoDataStore.data.map { prefs -> prefs[Keys.CHECKED_MED_SAFETY_IDS] ?: emptySet() }

  val savedVisitDrafts: Flow<List<DoctorVisitDraft>> =
    context.manoDataStore.data.map { prefs ->
      val raw = prefs[Keys.SAVED_VISIT_DRAFTS_JSON].orEmpty()
      if (raw.isBlank()) {
        emptyList()
      } else {
        runCatching {
          json.decodeFromString<List<DoctorVisitDraft>>(raw)
            .sortedByDescending { it.createdAtMs }
        }.getOrDefault(emptyList())
      }
    }

  val savedNutritionPlans: Flow<List<NutritionPlanResult>> =
    context.manoDataStore.data.map { prefs ->
      val raw = prefs[Keys.SAVED_NUTRITION_PLANS_JSON].orEmpty()
      if (raw.isBlank()) {
        emptyList()
      } else {
        runCatching {
          json.decodeFromString<List<NutritionPlanResult>>(raw)
            .sortedByDescending { it.createdAtMs }
        }.getOrDefault(emptyList())
      }
    }

  val childBirthDateEpochMs: Flow<Long?> =
    context.manoDataStore.data.map { prefs -> prefs[Keys.CHILD_BIRTH_DATE_EPOCH_MS] }

  val vaccineRecords: Flow<List<VaccineRecord>> =
    context.manoDataStore.data.map { prefs ->
      val raw = prefs[Keys.VACCINE_RECORDS_JSON].orEmpty()
      if (raw.isBlank()) {
        emptyList()
      } else {
        runCatching { json.decodeFromString<List<VaccineRecord>>(raw) }.getOrDefault(emptyList())
      }
    }

  val vaccineReminderSettings: Flow<VaccineReminderSettings> =
    context.manoDataStore.data.map { prefs ->
      VaccineReminderSettings(
        enabled = prefs[Keys.VACCINE_REMINDER_ENABLED] ?: true,
        leadDaysBefore = (prefs[Keys.VACCINE_REMINDER_LEAD_DAYS] ?: 3).coerceIn(1, 30),
      )
    }

  val notifiedVaccineReminderKeys: Flow<Set<String>> =
    context.manoDataStore.data.map { prefs -> prefs[Keys.NOTIFIED_VACCINE_REMINDER_KEYS] ?: emptySet() }

  val childAliasFa: Flow<String?> =
    context.manoDataStore.data.map { prefs -> prefs[Keys.CHILD_ALIAS_FA] }

  val galacticOrbitSelection: Flow<GalacticOrbitSelection?> =
    context.manoDataStore.data.map { prefs ->
      val topicId = prefs[Keys.GALACTIC_TOPIC_ID]?.takeIf { it.isNotBlank() } ?: return@map null
      val ringName = prefs[Keys.GALACTIC_RING]?.takeIf { it.isNotBlank() } ?: "INNER"
      GalacticOrbitSelection(ringName = ringName, topicId = topicId)
    }

  val savedSyrupScans: Flow<List<SavedSyrupScan>> =
    context.manoDataStore.data.map { prefs ->
      val raw = prefs[Keys.SAVED_SYRUP_SCANS_JSON].orEmpty()
      if (raw.isBlank()) {
        emptyList()
      } else {
        runCatching {
          json.decodeFromString<List<SavedSyrupScan>>(raw)
            .sortedByDescending { it.updatedAtMs }
        }.getOrDefault(emptyList())
      }
    }

  val themeMode: Flow<ManoThemeMode> =
    context.manoDataStore.data.map { prefs ->
      val id = prefs[Keys.THEME_MODE]
      ManoThemeMode.entries.firstOrNull { it.id == id } ?: ManoThemeMode.LIGHT
    }

  val localChildProfile: Flow<ChildProfileDemo?> =
    context.manoDataStore.data.map { prefs ->
      val raw = prefs[Keys.LOCAL_CHILD_PROFILE_JSON].orEmpty()
      if (raw.isBlank()) null
      else runCatching { json.decodeFromString<ChildProfileDemo>(raw) }.getOrNull()
    }

  val localFeverLogs: Flow<List<TimelineTempPoint>> =
    context.manoDataStore.data.map { prefs ->
      val raw = prefs[Keys.LOCAL_FEVER_LOGS_JSON].orEmpty()
      if (raw.isBlank()) emptyList()
      else runCatching {
        json.decodeFromString<List<TimelineTempPoint>>(raw).sortedByDescending { it.timestampMs }
      }.getOrDefault(emptyList())
    }

  fun growthHistory(uid: String?): Flow<List<GrowthMeasurement>> =
    context.manoDataStore.data.map { prefs ->
      prefs[Keys.growth(uid)]?.let { raw ->
        runCatching { json.decodeFromString<List<GrowthMeasurement>>(raw) }.getOrNull()
      }.orEmpty()
    }

  suspend fun addGrowthMeasurement(uid: String?, measure: GrowthMeasurement) {
    require(measure.weightKg != null || measure.heightCm != null)
    require(measure.weightKg == null || measure.weightKg.isFinite() && measure.weightKg > 0)
    require(measure.heightCm == null || measure.heightCm.isFinite() && measure.heightCm > 0)
    val key = Keys.growth(uid)
    context.manoDataStore.edit { prefs ->
      // Do not overwrite an unreadable longitudinal record with a new one.
      val previous = prefs[key]?.takeIf { it.isNotBlank() }?.let { raw ->
        try {
          json.decodeFromString<List<GrowthMeasurement>>(raw)
        } catch (e: Exception) {
          throw java.io.IOException("Saved growth history is unreadable; original data was preserved", e)
        }
      }.orEmpty()
      prefs[key] = json.encodeToString(listOf(measure) + previous)
    }
  }

  val offlineQuestions: Flow<List<ConsultationRequestDemo>> = context.manoDataStore.data.map { prefs ->
    prefs[Keys.OFFLINE_QUESTIONS]?.let { raw ->
      runCatching { json.decodeFromString<List<ConsultationRequestDemo>>(raw) }.getOrNull()
    }.orEmpty()
  }

  suspend fun saveOfflineQuestion(draft: ConsultationRequestDemo) {
    context.manoDataStore.edit { prefs ->
      // Preserve unreadable drafts instead of silently replacing the original JSON.
      val previous = prefs[Keys.OFFLINE_QUESTIONS]?.takeIf { it.isNotBlank() }?.let { raw ->
        try {
          json.decodeFromString<List<ConsultationRequestDemo>>(raw)
        } catch (e: Exception) {
          throw java.io.IOException("Saved offline questions are unreadable; original data was preserved", e)
        }
      }.orEmpty()
      prefs[Keys.OFFLINE_QUESTIONS] = json.encodeToString(listOf(draft) + previous.filter { it.id != draft.id })
    }
  }

  val localDoseLogs: Flow<List<TimelineDosePoint>> =
    context.manoDataStore.data.map { prefs ->
      val raw = prefs[Keys.LOCAL_DOSE_LOGS_JSON].orEmpty()
      if (raw.isBlank()) emptyList()
      else runCatching {
        json.decodeFromString<List<TimelineDosePoint>>(raw).sortedByDescending { it.timestampMs }
      }.getOrDefault(emptyList())
    }

  val checkedMilestoneIds: Flow<Set<String>> =
    context.manoDataStore.data.map { prefs -> prefs[Keys.CHECKED_MILESTONE_IDS] ?: emptySet() }

  val onboardingCompleted: Flow<Boolean> =
    context.manoDataStore.data.map { prefs -> prefs[Keys.ONBOARDING_COMPLETED] ?: false }

  val hapticIntensity: Flow<ManoHapticIntensity> =
    context.manoDataStore.data.map { prefs ->
      val id = prefs[Keys.HAPTIC_INTENSITY]
      ManoHapticIntensity.entries.firstOrNull { it.id == id } ?: ManoHapticIntensity.MEDIUM
    }

  val webGlQuality: Flow<ManoWebGlQuality> =
    context.manoDataStore.data.map { prefs ->
      val id = prefs[Keys.WEBGL_QUALITY]
      ManoWebGlQuality.entries.firstOrNull { it.id == id } ?: ManoWebGlQuality.AUTO
    }

  suspend fun toggleBookmark(slug: String) {
    context.manoDataStore.edit { prefs ->
      val current = prefs[Keys.BOOKMARKED_SLUGS]?.toMutableSet() ?: mutableSetOf()
      if (!current.add(slug)) {
        current.remove(slug)
      }
      prefs[Keys.BOOKMARKED_SLUGS] = current
    }
  }

  suspend fun setSelectedAgeBand(ageBand: AgeBand?) {
    context.manoDataStore.edit { prefs ->
      if (ageBand == null) {
        prefs.remove(Keys.SELECTED_AGE_BAND)
      } else {
        prefs[Keys.SELECTED_AGE_BAND] = ageBand.id
      }
    }
  }

  suspend fun setChildHeightCm(heightCmText: String) {
    context.manoDataStore.edit { prefs ->
      val cleaned = heightCmText.trim().take(12)
      if (cleaned.isEmpty()) {
        prefs.remove(Keys.CHILD_HEIGHT_CM)
      } else {
        prefs[Keys.CHILD_HEIGHT_CM] = cleaned
      }
    }
  }

  suspend fun upsertVisitDraft(draft: DoctorVisitDraft) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.SAVED_VISIT_DRAFTS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<DoctorVisitDraft>>(it) }.getOrNull() }
          .orEmpty()
          .filterNot { it.id == draft.id }
      val updated = (listOf(draft) + current).sortedByDescending { it.createdAtMs }.take(25)
      prefs[Keys.SAVED_VISIT_DRAFTS_JSON] = json.encodeToString(updated)
    }
  }

  suspend fun deleteVisitDraft(draftId: String) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.SAVED_VISIT_DRAFTS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<DoctorVisitDraft>>(it) }.getOrNull() }
          .orEmpty()
      val updated = current.filterNot { it.id == draftId }
      prefs[Keys.SAVED_VISIT_DRAFTS_JSON] = json.encodeToString(updated)
    }
  }

  suspend fun upsertNutritionPlan(plan: NutritionPlanResult) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.SAVED_NUTRITION_PLANS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<NutritionPlanResult>>(it) }.getOrNull() }
          .orEmpty()
          .filterNot { it.id == plan.id }
      val updated = (listOf(plan) + current).sortedByDescending { it.createdAtMs }.take(15)
      prefs[Keys.SAVED_NUTRITION_PLANS_JSON] = json.encodeToString(updated)
    }
  }

  suspend fun deleteNutritionPlan(planId: String) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.SAVED_NUTRITION_PLANS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<NutritionPlanResult>>(it) }.getOrNull() }
          .orEmpty()
      val updated = current.filterNot { it.id == planId }
      prefs[Keys.SAVED_NUTRITION_PLANS_JSON] = json.encodeToString(updated)
    }
  }

  suspend fun toggleMedSafetyCheck(itemId: String) {
    context.manoDataStore.edit { prefs ->
      val current = prefs[Keys.CHECKED_MED_SAFETY_IDS]?.toMutableSet() ?: mutableSetOf()
      if (!current.add(itemId)) {
        current.remove(itemId)
      }
      prefs[Keys.CHECKED_MED_SAFETY_IDS] = current
    }
  }

  suspend fun resetMedSafetyChecklist() {
    context.manoDataStore.edit { prefs ->
      prefs.remove(Keys.CHECKED_MED_SAFETY_IDS)
    }
  }

  suspend fun setChildBirthDateEpochMs(epochMs: Long?) {
    context.manoDataStore.edit { prefs ->
      if (epochMs == null || epochMs <= 0L) {
        prefs.remove(Keys.CHILD_BIRTH_DATE_EPOCH_MS)
      } else {
        prefs[Keys.CHILD_BIRTH_DATE_EPOCH_MS] = epochMs
      }
      prefs.remove(Keys.NOTIFIED_VACCINE_REMINDER_KEYS)
    }
  }

  suspend fun upsertVaccineRecord(record: VaccineRecord) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.VACCINE_RECORDS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<VaccineRecord>>(it) }.getOrNull() }
          .orEmpty()
          .filterNot { it.vaccineId == record.vaccineId }
      val updated = listOf(record) + current
      prefs[Keys.VACCINE_RECORDS_JSON] = json.encodeToString(updated)
      val cleanedKeys =
        (prefs[Keys.NOTIFIED_VACCINE_REMINDER_KEYS] ?: emptySet())
          .filterNot { it.startsWith("${record.vaccineId}_") }
          .toSet()
      prefs[Keys.NOTIFIED_VACCINE_REMINDER_KEYS] = cleanedKeys
    }
  }

  suspend fun deleteVaccineRecord(vaccineId: String) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.VACCINE_RECORDS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<VaccineRecord>>(it) }.getOrNull() }
          .orEmpty()
      val updated = current.filterNot { it.vaccineId == vaccineId }
      prefs[Keys.VACCINE_RECORDS_JSON] = json.encodeToString(updated)
      val cleanedKeys =
        (prefs[Keys.NOTIFIED_VACCINE_REMINDER_KEYS] ?: emptySet())
          .filterNot { it.startsWith("${vaccineId}_") }
          .toSet()
      prefs[Keys.NOTIFIED_VACCINE_REMINDER_KEYS] = cleanedKeys
    }
  }

  suspend fun setChildAliasFa(aliasFa: String?) {
    context.manoDataStore.edit { prefs ->
      val cleaned = aliasFa?.trim().orEmpty()
      if (cleaned.isEmpty()) {
        prefs.remove(Keys.CHILD_ALIAS_FA)
      } else {
        prefs[Keys.CHILD_ALIAS_FA] = cleaned.take(40)
      }
    }
  }

  suspend fun mirrorCloudVaccineRecords(cloudRecords: List<VaccineRecord>) {
    context.manoDataStore.edit { prefs ->
      val local =
        prefs[Keys.VACCINE_RECORDS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<VaccineRecord>>(it) }.getOrNull() }
          .orEmpty()
      val cloudIds = cloudRecords.map { it.vaccineId }.toSet()
      val preservedLocal = local.filter { !it.isSyncedCloud || it.vaccineId in cloudIds }
      val merged = linkedMapOf<String, VaccineRecord>()
      (preservedLocal + cloudRecords).forEach { rec ->
        val existing = merged[rec.vaccineId]
        if (
          existing == null ||
            rec.updatedAtMs > existing.updatedAtMs ||
            (rec.updatedAtMs == existing.updatedAtMs && rec.isSyncedCloud)
        ) {
          merged[rec.vaccineId] = rec
        }
      }
      prefs[Keys.VACCINE_RECORDS_JSON] = json.encodeToString(merged.values.toList())
    }
  }

  suspend fun setVaccineReminderSettings(enabled: Boolean, leadDaysBefore: Int) {
    context.manoDataStore.edit { prefs ->
      prefs[Keys.VACCINE_REMINDER_ENABLED] = enabled
      prefs[Keys.VACCINE_REMINDER_LEAD_DAYS] = leadDaysBefore.coerceIn(1, 30)
      prefs.remove(Keys.NOTIFIED_VACCINE_REMINDER_KEYS)
    }
  }

  suspend fun markVaccineReminderKeysNotified(keys: Set<String>) {
    if (keys.isEmpty()) return
    context.manoDataStore.edit { prefs ->
      val current = prefs[Keys.NOTIFIED_VACCINE_REMINDER_KEYS]?.toMutableSet() ?: mutableSetOf()
      current.addAll(keys)
      prefs[Keys.NOTIFIED_VACCINE_REMINDER_KEYS] = current
    }
  }

  suspend fun setGalacticOrbitSelection(ringName: String, topicId: String) {
    context.manoDataStore.edit { prefs ->
      val cleanedTopic = topicId.trim()
      if (cleanedTopic.isEmpty()) {
        prefs.remove(Keys.GALACTIC_RING)
        prefs.remove(Keys.GALACTIC_TOPIC_ID)
      } else {
        prefs[Keys.GALACTIC_RING] = ringName.trim().ifEmpty { "INNER" }
        prefs[Keys.GALACTIC_TOPIC_ID] = cleanedTopic
      }
    }
  }

  suspend fun upsertSyrupScan(scan: SavedSyrupScan) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.SAVED_SYRUP_SCANS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<SavedSyrupScan>>(it) }.getOrNull() }
          .orEmpty()
          .filterNot { it.id == scan.id }
      val updated = (listOf(scan) + current).sortedByDescending { it.updatedAtMs }.take(25)
      prefs[Keys.SAVED_SYRUP_SCANS_JSON] = json.encodeToString(updated)
    }
  }

  suspend fun deleteSyrupScan(scanId: String) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.SAVED_SYRUP_SCANS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<SavedSyrupScan>>(it) }.getOrNull() }
          .orEmpty()
      val updated = current.filterNot { it.id == scanId }
      prefs[Keys.SAVED_SYRUP_SCANS_JSON] = json.encodeToString(updated)
    }
  }

  suspend fun mirrorCloudSyrupScans(cloudScans: List<SavedSyrupScan>) {
    context.manoDataStore.edit { prefs ->
      val local =
        prefs[Keys.SAVED_SYRUP_SCANS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<SavedSyrupScan>>(it) }.getOrNull() }
          .orEmpty()
      val cloudIds = cloudScans.map { it.id }.toSet()
      val preservedLocal = local.filter { !it.isSyncedCloud || it.id in cloudIds }
      val merged = linkedMapOf<String, SavedSyrupScan>()
      (preservedLocal + cloudScans).forEach { scan ->
        val existing = merged[scan.id]
        if (
          existing == null ||
            scan.updatedAtMs > existing.updatedAtMs ||
            (scan.updatedAtMs == existing.updatedAtMs && scan.isSyncedCloud)
        ) {
          merged[scan.id] = scan
        }
      }
      prefs[Keys.SAVED_SYRUP_SCANS_JSON] =
        json.encodeToString(merged.values.sortedByDescending { it.updatedAtMs }.take(25))
    }
  }

  /**
   * Atomic local erasure of child-health data for a caregiver-initiated device reset.
   * No cloud records or pharmacist audit records are changed by this operation.
   * App-wide visual preferences are deliberately retained.
   */
  suspend fun clearLocalChildHealthData() {
    context.manoDataStore.edit { prefs ->
      listOf(
        Keys.OFFLINE_QUESTIONS,
        Keys.LOCAL_CHILD_PROFILE_JSON,
        Keys.CHILD_ALIAS_FA,
        Keys.CHILD_HEIGHT_CM,
        Keys.CHILD_BIRTH_DATE_EPOCH_MS,
        Keys.SELECTED_AGE_BAND,
        Keys.LOCAL_FEVER_LOGS_JSON,
        Keys.LOCAL_DOSE_LOGS_JSON,
        Keys.SAVED_SYRUP_SCANS_JSON,
        Keys.SAVED_VISIT_DRAFTS_JSON,
        Keys.SAVED_NUTRITION_PLANS_JSON,
        Keys.VACCINE_RECORDS_JSON,
        Keys.NOTIFIED_VACCINE_REMINDER_KEYS,
        Keys.CHECKED_MILESTONE_IDS,
        Keys.CHECKED_MED_SAFETY_IDS,
        Keys.BOOKMARKED_SLUGS,
        Keys.GALACTIC_RING,
        Keys.GALACTIC_TOPIC_ID,
        Keys.ONBOARDING_COMPLETED,
        Keys.VACCINE_REMINDER_ENABLED,
        Keys.VACCINE_REMINDER_LEAD_DAYS,
      ).forEach { prefs.remove(it) }
      prefs.asMap().keys.filter { it.name.startsWith(Keys.GROWTH_PREFIX) }.forEach { prefs.remove(it) }
    }
  }

  suspend fun setThemeMode(mode: ManoThemeMode) {
    context.manoDataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = mode.id
    }
  }

  suspend fun setLocalChildProfile(profile: ChildProfileDemo?) {
    context.manoDataStore.edit { prefs ->
      if (profile == null) {
        prefs.remove(Keys.LOCAL_CHILD_PROFILE_JSON)
        prefs.remove(Keys.CHILD_ALIAS_FA)
      } else {
        prefs[Keys.LOCAL_CHILD_PROFILE_JSON] = json.encodeToString(profile)
        val cleanAlias = profile.aliasFa.trim().take(40)
        if (cleanAlias.isNotEmpty()) {
          prefs[Keys.CHILD_ALIAS_FA] = cleanAlias
        }
      }
    }
  }

  suspend fun addLocalFeverLog(point: TimelineTempPoint) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.LOCAL_FEVER_LOGS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<TimelineTempPoint>>(it) }.getOrNull() }
          .orEmpty()
          .filterNot { it.id == point.id }
      val updated = (listOf(point) + current).sortedByDescending { it.timestampMs }.take(40)
      prefs[Keys.LOCAL_FEVER_LOGS_JSON] = json.encodeToString(updated)
    }
  }

  suspend fun deleteLocalFeverLog(id: String) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.LOCAL_FEVER_LOGS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<TimelineTempPoint>>(it) }.getOrNull() }
          .orEmpty()
      val updated = current.filterNot { it.id == id }
      prefs[Keys.LOCAL_FEVER_LOGS_JSON] = json.encodeToString(updated)
    }
  }

  suspend fun clearLocalFeverLogs() {
    context.manoDataStore.edit { prefs ->
      prefs.remove(Keys.LOCAL_FEVER_LOGS_JSON)
    }
  }

  suspend fun addLocalDoseLog(point: TimelineDosePoint) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.LOCAL_DOSE_LOGS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<TimelineDosePoint>>(it) }.getOrNull() }
          .orEmpty()
          .filterNot { it.id == point.id }
      val updated = (listOf(point) + current).sortedByDescending { it.timestampMs }.take(40)
      prefs[Keys.LOCAL_DOSE_LOGS_JSON] = json.encodeToString(updated)
    }
  }

  suspend fun deleteLocalDoseLog(id: String) {
    context.manoDataStore.edit { prefs ->
      val current =
        prefs[Keys.LOCAL_DOSE_LOGS_JSON]
          ?.takeIf { it.isNotBlank() }
          ?.let { runCatching { json.decodeFromString<List<TimelineDosePoint>>(it) }.getOrNull() }
          .orEmpty()
      val updated = current.filterNot { it.id == id }
      prefs[Keys.LOCAL_DOSE_LOGS_JSON] = json.encodeToString(updated)
    }
  }

  suspend fun clearLocalDoseLogs() {
    context.manoDataStore.edit { prefs ->
      prefs.remove(Keys.LOCAL_DOSE_LOGS_JSON)
    }
  }

  suspend fun toggleMilestoneCheck(id: String) {
    context.manoDataStore.edit { prefs ->
      val current = prefs[Keys.CHECKED_MILESTONE_IDS]?.toMutableSet() ?: mutableSetOf()
      if (!current.add(id)) {
        current.remove(id)
      }
      prefs[Keys.CHECKED_MILESTONE_IDS] = current
    }
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.manoDataStore.edit { prefs ->
      prefs[Keys.ONBOARDING_COMPLETED] = completed
    }
  }

  suspend fun setHapticIntensity(intensity: ManoHapticIntensity) {
    context.manoDataStore.edit { prefs ->
      prefs[Keys.HAPTIC_INTENSITY] = intensity.id
    }
  }

  suspend fun setWebGlQuality(quality: ManoWebGlQuality) {
    context.manoDataStore.edit { prefs ->
      prefs[Keys.WEBGL_QUALITY] = quality.id
    }
  }
}
