package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ManoPreferencesRepository
import com.example.data.ManoThemeMode
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.NutritionPlanResult
import com.example.domain.model.SavedSyrupScan
import com.example.domain.model.VaccineRecord
import com.example.domain.util.ChildProfilePrivacy
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineTempPoint
import com.example.widget.FeverTimelineWidgetStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManoChildPrivacyTest {
  private val context get() = ApplicationProvider.getApplicationContext<Context>()
  private val prefs get() = ManoPreferencesRepository(context)
  private val bookmarkId = "privacy-regression-only-article"

  private fun sampleProfile(alias: String = "کودک آزمایشی") =
    ChildProfileDemo(
      aliasFa = alias,
      birthDateOrAgeFa = "۲ سال و ۳ ماه",
      ageBand = AgeBand.TODDLER_1_3Y,
      weightKgText = "11.4",
      weightDateFa = "امروز",
      allergiesFa = "آلرژی آزمایشی",
      currentMedsFa = "داروی آزمایشی",
      consentGranted = true,
    )

  @After
  fun cleanup() = runBlocking {
    prefs.clearAllLocalChildHealthData()
    prefs.setThemeMode(ManoThemeMode.SYSTEM)
    prefs.setGalacticOrbitSelection("", "")
    if (bookmarkId in prefs.bookmarkedSlugs.first()) prefs.toggleBookmark(bookmarkId)
    FeverTimelineWidgetStore.clearAndUpdateWidgets(context)
  }

  @Test
  fun exportedProfileUsesCorrectJsonEscapingNotStringInterpolation() {
    val unsafe = "کودک \"یک\"\\\nخط جدید"
    val profile = sampleProfile(unsafe).copy(
      allergiesFa = "نامشخص \"؟\"",
      currentMedsFa = "دارو\\ی فعلی",
    )
    val output = ChildProfilePrivacy.exportProfileSummaryJson(profile)
    val parsed = Json.parseToJsonElement(output).jsonObject
    assertEquals(unsafe, parsed["alias"]?.jsonPrimitive?.content)
    assertEquals(profile.allergiesFa, parsed["allergies"]?.jsonPrimitive?.content)
    assertEquals(profile.currentMedsFa, parsed["currentMedications"]?.jsonPrimitive?.content)
    assertEquals("MANO_KIDS_LOCAL_SUMMARY", parsed["source"]?.jsonPrimitive?.content)
    assertTrue(output.startsWith("{"))
  }

  @Test
  fun widgetCacheIsClearedAfterLocalDataPurge() {
    val preferences = context.getSharedPreferences("mano_fever_timeline_widget", Context.MODE_PRIVATE)
    preferences.edit()
      .putString("latest_temp_text", "تب ساختگی خصوصی")
      .putString("latest_dose_text", "داروی ساختگی خصوصی")
      .putString("safe_interval_text", "زمان خصوصی")
      .commit()
    assertEquals("تب ساختگی خصوصی", FeverTimelineWidgetStore.readSummary(context).first)
    FeverTimelineWidgetStore.clearAndUpdateWidgets(context)
    val displayed = FeverTimelineWidgetStore.readSummary(context)
    assertEquals("دما: ثبت نشده", displayed.first)
    assertEquals("دارو: نوبتی ثبت نشده", displayed.second)
    assertFalse(displayed.third.contains("زمان خصوصی"))
  }

  @Test
  fun localPurgeDeletesAllChildRelatedPreferencesButKeepsThemeAndBookmarks() = runBlocking {
    val p = prefs
    p.clearAllLocalChildHealthData()
    p.setThemeMode(ManoThemeMode.DARK)
    p.setGalacticOrbitSelection("INNER", "topic-one")
    if (bookmarkId !in p.bookmarkedSlugs.first()) p.toggleBookmark(bookmarkId)

    val profile = sampleProfile()
    p.setLocalChildProfile(profile)
    p.setChildHeightCm("85")
    p.setSelectedAgeBand(AgeBand.TODDLER_1_3Y)
    p.setChildBirthDateEpochMs(1_750_000_000_000L)
    p.setChildAliasFa("کودک نمونه")
    p.setVaccineReminderSettings(true, 7)
    p.markVaccineReminderKeysNotified(setOf("vaccine-sample_170000000"))
    p.toggleMilestoneCheck("ms_privacy_sample")
    p.upsertVaccineRecord(VaccineRecord(id="v1", vaccineId="bcg", noteFa="اطلاعات خصوصی"))
    p.upsertSyrupScan(SavedSyrupScan(id="s1", drugNameFa="داروی خصوصی"))
    p.upsertVisitDraft(
      DoctorVisitDraft(
        id="draft1", titleFa="شرح حال",
        symptomContextFa="اطلاعات خصوصی", timelineSummaryFa="اطلاعات خصوصی",
        draftTextFa="اطلاعات خصوصی"
      )
    )
    p.upsertNutritionPlan(
      NutritionPlanResult(id="meal1", planTitleFa="تغذیه خصوصی", createdAtMs=1L)
    )
    p.addLocalFeverLog(
      TimelineTempPoint(
        id="fever1", tempCelsius=38.1f, siteFa="زیربغلی",
        timeLabelFa="۱۴:۳۰", timestampMs=1_750_000_000_000L,
        caregiverFa="مادر", noteFa="اطلاعات خصوصی", isSyncedCloud=false,
      )
    )
    p.addLocalDoseLog(
      TimelineDosePoint(
        id="dose1", medicationSlug="test-drug", medicationNameFa="داروی آزمایشی",
        dosageFormFa="شربت", volumeMlText="مقدار آزمایشی",
        givenAtLabelFa="۱۴:۳۰", timestampMs=1_750_000_000_000L,
        noteFa="اطلاعات خصوصی", isSyncedCloud=false,
      )
    )
    assertEquals(profile, p.localChildProfile.first())
    assertEquals(1, p.savedVisitDrafts.first().size)
    assertEquals(1, p.savedSyrupScans.first().size)
    assertEquals(1, p.vaccineRecords.first().size)

    p.clearAllLocalChildHealthData()

    assertNull(p.localChildProfile.first())
    assertNull(p.childAliasFa.first())
    assertNull(p.childBirthDateEpochMs.first())
    assertNull(p.selectedAgeBand.first())
    assertEquals("", p.childHeightCm.first())
    assertTrue(p.localFeverLogs.first().isEmpty())
    assertTrue(p.localDoseLogs.first().isEmpty())
    assertTrue(p.savedVisitDrafts.first().isEmpty())
    assertTrue(p.savedNutritionPlans.first().isEmpty())
    assertTrue(p.vaccineRecords.first().isEmpty())
    assertTrue(p.savedSyrupScans.first().isEmpty())
    assertTrue(p.notifiedVaccineReminderKeys.first().isEmpty())
    assertTrue(p.checkedMilestoneIds.first().isEmpty())
    assertTrue(p.checkedMedSafetyIds.first().isEmpty())
    assertFalse(p.vaccineReminderSettings.first().enabled)
    assertEquals(ManoThemeMode.DARK, p.themeMode.first())
    assertTrue(bookmarkId in p.bookmarkedSlugs.first())
    assertEquals("topic-one", p.galacticOrbitSelection.first()?.topicId)
  }
}
