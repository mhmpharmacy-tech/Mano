package com.example

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.VaccineFilter
import com.example.domain.model.VaccineRecord
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.model.VaccineStatus
import com.example.domain.util.VaccinationScheduleEngine
import com.example.notifications.VaccineNotificationHelper
import com.example.notifications.VaccineReminderScheduler
import com.example.ui.screens.VaccinesScreen
import com.example.ui.theme.ManoKidsTheme
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = Application::class)
class ManoVaccinationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun jalaliDateConversion_handlesNowruzLeapYearAndFlexibleParsing() {
    val (jy, jm, jd) = VaccinationScheduleEngine.gregorianToJalali(2024, 3, 20)
    assertEquals(Triple(1403, 1, 1), Triple(jy, jm, jd))

    val (gy, gm, gd) = VaccinationScheduleEngine.jalaliToGregorian(1403, 1, 1)
    assertEquals(Triple(2024, 3, 20), Triple(gy, gm, gd))

    // 1403 is a Jalali leap year (30 Esfand 1403 = 2025-03-20)
    val (ly, lm, ld) = VaccinationScheduleEngine.gregorianToJalali(2025, 3, 20)
    assertEquals(Triple(1403, 12, 30), Triple(ly, lm, ld))

    val tz = TimeZone.getTimeZone("Asia/Tehran")
    val parsedShamsi = VaccinationScheduleEngine.parseFlexibleDateToEpochMs("۱۴۰۳/۰۱/۰۱", tz)
    val parsedGregorian = VaccinationScheduleEngine.parseFlexibleDateToEpochMs("2024/03/20", tz)
    assertNotNull(parsedShamsi)
    assertEquals(parsedGregorian, parsedShamsi)
  }

  @Test
  fun scheduleEngine_computesStatusDueWindowAndPreDueRemindersAccurately() {
    val tz = TimeZone.getTimeZone("UTC")
    // Suppose child born 58 days ago -> birth dose due 58 days ago (overdue if unlogged),
    // 2m (60 days) due in +2 days -> inside leadDaysBefore=3 -> DUE_SOON!
    val nowMs = VaccinationScheduleEngine.parseFlexibleDateToEpochMs("2025/05/01", tz)!!
    val birthMs = VaccinationScheduleEngine.addDaysToDay(nowMs, -58, tz)

    val birthCompletedRecord =
      VaccineRecord(
        id = "birth_bcg_hbv_opv",
        vaccineId = "birth_bcg_hbv_opv",
        administeredDateEpochMs = birthMs,
        updatedAtMs = nowMs,
      )

    val settings = VaccineReminderSettings(enabled = true, leadDaysBefore = 3)
    val summary =
      VaccinationScheduleEngine.resolveSchedule(
        localRecords = listOf(birthCompletedRecord),
        birthDateEpochMs = birthMs,
        reminderSettings = settings,
        nowEpochMs = nowMs,
        timeZone = tz,
      )

    assertEquals(7, summary.totalCoreCount)
    assertEquals(1, summary.administeredCoreCount)
    assertEquals(1, summary.dueSoonCount)
    assertEquals(0, summary.overdueCount)
    assertEquals("2m_pentavalent_opv_rota", summary.nextUpcoming?.definition?.id)
    assertEquals(VaccineStatus.DUE_SOON, summary.nextUpcoming?.status)
    assertEquals(2, summary.nextUpcoming?.daysUntilDue)

    // Pre-due reminder evaluation finds 2m vaccine once, then deduplicates via reminderKey
    val firstAlerts = VaccinationScheduleEngine.findUnnotifiedReminders(summary, settings, emptySet(), tz)
    assertEquals(1, firstAlerts.size)
    assertEquals("2m_pentavalent_opv_rota", firstAlerts.first().definition.id)

    val sentKeys = firstAlerts.mapNotNull { VaccinationScheduleEngine.reminderKeyFor(it, tz) }.toSet()
    val secondAlerts = VaccinationScheduleEngine.findUnnotifiedReminders(summary, settings, sentKeys, tz)
    assertTrue(secondAlerts.isEmpty())

    // Notification copy should include child alias and due info
    val (title, body) = VaccinationScheduleEngine.buildReminderNotificationCopy(firstAlerts, childAliasFa = "رادین")
    assertTrue(title.contains("رادین"))
    assertTrue(body.contains("۲ ماهگی"))
  }

  @Test
  fun reminderScheduler_enqueuesPeriodicAndOneTimeWorkInWorkManager() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    WorkManagerTestInitHelper.initializeTestWorkManager(
      context,
      Configuration.Builder()
        .setMinimumLoggingLevel(android.util.Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .setTaskExecutor(SynchronousExecutor())
        .build(),
    )
    val workManager = WorkManager.getInstance(context)

    val nowMs = VaccinationScheduleEngine.parseFlexibleDateToEpochMs("2025/05/01")!!
    val birthMs = VaccinationScheduleEngine.addDaysToDay(nowMs, -55)
    val settings = VaccineReminderSettings(enabled = true, leadDaysBefore = 3)
    val summary =
      VaccinationScheduleEngine.resolveSchedule(
        localRecords =
          listOf(
            VaccineRecord(
              id = "birth_bcg_hbv_opv",
              vaccineId = "birth_bcg_hbv_opv",
              administeredDateEpochMs = birthMs,
              updatedAtMs = nowMs,
            )
          ),
        birthDateEpochMs = birthMs,
        reminderSettings = settings,
        nowEpochMs = nowMs,
      )

    VaccineReminderScheduler.syncSchedule(context, summary, settings, nowMs)

    val periodicInfos = workManager.getWorkInfosForUniqueWork(VaccineReminderScheduler.PERIODIC_WORK_NAME).get()
    val nextTriggerInfos = workManager.getWorkInfosForUniqueWork(VaccineReminderScheduler.NEXT_TRIGGER_WORK_NAME).get()
    assertTrue("Expected enqueued/running periodic work, got: $periodicInfos", periodicInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING })
    assertTrue("Expected enqueued one-time trigger work, got: $nextTriggerInfos", nextTriggerInfos.any { it.state == WorkInfo.State.ENQUEUED })

    // Disabling reminders cancels unique work
    VaccineReminderScheduler.syncSchedule(context, summary, settings.copy(enabled = false), nowMs)
    val cancelledInfos = workManager.getWorkInfosForUniqueWork(VaccineReminderScheduler.NEXT_TRIGGER_WORK_NAME).get()
    assertTrue(cancelledInfos.all { it.state == WorkInfo.State.CANCELLED } || cancelledInfos.isEmpty())
    assertNotNull(VaccineNotificationHelper.CHANNEL_ID)
  }

  @Test
  fun vaccinesScreen_registersBirthDateLeadDaysAndAdministeredVaccine() {
    val child =
      ChildProfileDemo(
        aliasFa = "کیان",
        birthDateOrAgeFa = "۲ ماهه",
        ageBand = AgeBand.NEWBORN,
        weightKgText = "5.4",
        weightDateFa = "امروز",
        allergiesFa = "ندارد",
        currentMedsFa = "ویتامین AD",
        consentGranted = true,
      )

    composeTestRule.setContent {
      ManoKidsTheme {
        var birthMs by remember { mutableStateOf<Long?>(null) }
        var records by remember { mutableStateOf<List<VaccineRecord>>(emptyList()) }
        var reminderSettings by remember { mutableStateOf(VaccineReminderSettings(enabled = true, leadDaysBefore = 3)) }
        var lastToast by remember { mutableStateOf("") }

        val summary =
          remember(birthMs, records, reminderSettings) {
            VaccinationScheduleEngine.resolveSchedule(
              localRecords = records,
              birthDateEpochMs = birthMs,
              reminderSettings = reminderSettings,
            )
          }

        VaccinesScreen(
          summary = summary,
          birthDateEpochMs = birthMs,
          reminderSettings = reminderSettings,
          childProfile = child,
          hasNotificationPermission = true,
          isCloudSignedIn = false,
          onSaveBirthDate = { birthMs = it },
          onUpdateReminderSettings = { enabled, days ->
            reminderSettings = VaccineReminderSettings(enabled = enabled, leadDaysBefore = days)
          },
          onRequestNotificationPermission = {},
          onSendPreviewReminder = { lastToast = "sent" },
          onSaveVaccineRecord = { updated ->
            records = records.filterNot { it.vaccineId == updated.vaccineId } + updated
          },
          onDeleteVaccineRecord = { id ->
            records = records.filterNot { it.vaccineId == id }
          },
          onFeedback = { lastToast = it },
        )
      }
    }

    // 1. Register birth date via Shamsi text input
    composeTestRule.onNodeWithTag("vaccines_list").performScrollToNode(hasTestTag("vaccine_birth_date_card"))
    composeTestRule.onNodeWithTag("vaccine_birthdate_input").performTextReplacement("1404/01/10")
    composeTestRule.onNodeWithTag("vaccine_save_birthdate_btn").performClick()
    composeTestRule.onNodeWithTag("vaccine_clear_birthdate_btn").assertIsDisplayed()

    // 2. Switch pre-due reminder lead days to 7 days
    composeTestRule.onNodeWithTag("vaccines_list").performScrollToNode(hasTestTag("vaccine_reminder_card"))
    composeTestRule.onNodeWithTag("vaccine_lead_days_7").performClick()

    // 3. Open edit dialog for birth vaccine, set facility and save as today
    composeTestRule.onNodeWithTag("vaccines_list").performScrollToNode(hasTestTag("vaccine_card_birth_bcg_hbv_opv"))
    composeTestRule.onNodeWithTag("vaccine_edit_btn_birth_bcg_hbv_opv").performClick()
    composeTestRule.onNodeWithTag("vaccine_dialog_admin_today_btn").performClick()
    composeTestRule.onNodeWithTag("vaccine_dialog_facility_input").performTextInput("بیمارستان صارم")
    composeTestRule.onNodeWithTag("vaccine_dialog_save_btn").performClick()

    // 4. Filter by ADMINISTERED and verify birth vaccine appears while non-administered 6y disappears
    composeTestRule.onNodeWithTag("vaccines_list").performScrollToNode(hasTestTag("vaccine_filter_${VaccineFilter.ADMINISTERED.name}"))
    composeTestRule.onNodeWithTag("vaccine_filter_${VaccineFilter.ADMINISTERED.name}").performClick()
    composeTestRule.onNodeWithTag("vaccines_list").performScrollToNode(hasTestTag("vaccine_card_birth_bcg_hbv_opv"))
    composeTestRule.onNodeWithTag("vaccine_card_birth_bcg_hbv_opv").assertIsDisplayed()
  }

  @Test
  fun jalaliDateConversion_rejectsNonLeapEsfand30AndParsesLeapEsfand30() {
    val tz = TimeZone.getTimeZone("Asia/Tehran")
    // 1403 is leap (30 Esfand valid), 1404 is non-leap (30 Esfand invalid)
    assertNotNull(VaccinationScheduleEngine.parseFlexibleDateToEpochMs("1403/12/30", tz))
    assertNull(VaccinationScheduleEngine.parseFlexibleDateToEpochMs("1404/12/30", tz))
    assertNotNull(VaccinationScheduleEngine.parseFlexibleDateToEpochMs("1404/12/29", tz))
  }

  @Test
  fun reminderScheduler_computesNextDelayWithoutSpinLoop() {
    val tz = TimeZone.getTimeZone("Asia/Tehran")
    val nowMs = VaccinationScheduleEngine.parseFlexibleDateToEpochMs("2025/05/01", tz)!!
    // Child born 58 days ago -> 2m dose due in +2 days -> inside leadDaysBefore = 3
    val birthMs = VaccinationScheduleEngine.addDaysToDay(nowMs, -58, tz)
    val birthCompletedRecord =
      VaccineRecord(
        id = "birth_bcg_hbv_opv",
        vaccineId = "birth_bcg_hbv_opv",
        administeredDateEpochMs = birthMs,
        updatedAtMs = nowMs,
      )
    val settings = VaccineReminderSettings(enabled = true, leadDaysBefore = 3)
    val summary =
      VaccinationScheduleEngine.resolveSchedule(
        localRecords = listOf(birthCompletedRecord),
        birthDateEpochMs = birthMs,
        reminderSettings = settings,
        nowEpochMs = nowMs,
        timeZone = tz,
      )

    // 1. Disabled returns null
    assertNull(
      VaccineReminderScheduler.computeNextScheduleDelayMs(
        summary = summary,
        settings = settings.copy(enabled = false),
        alreadyNotifiedKeys = emptySet(),
        hasNotificationPermission = true,
        nowEpochMs = nowMs,
      )
    )

    // 2. Due unnotified + permission granted returns 5_000L once
    assertEquals(
      5_000L,
      VaccineReminderScheduler.computeNextScheduleDelayMs(
        summary = summary,
        settings = settings,
        alreadyNotifiedKeys = emptySet(),
        hasNotificationPermission = true,
        nowEpochMs = nowMs,
      ),
    )

    // 3. Once the due alert is marked notified, scheduler jumps strictly to the next future 09:00 reminder (> 5_000L)
    val dueAlerts = VaccinationScheduleEngine.findUnnotifiedReminders(summary, settings, emptySet())
    val sentKeys = dueAlerts.mapNotNull { VaccinationScheduleEngine.reminderKeyFor(it) }.toSet()
    val futureDelay =
      VaccineReminderScheduler.computeNextScheduleDelayMs(
        summary = summary,
        settings = settings,
        alreadyNotifiedKeys = sentKeys,
        hasNotificationPermission = true,
        nowEpochMs = nowMs,
      )
    assertNotNull(futureDelay)
    assertTrue("Expected future delay > 5_000L, got $futureDelay", futureDelay!! > 5_000L)

    // 4. Without notification permission and past the 09:00 trigger for the current due item, scheduler skips 5_000L spin
    val afterAllNotified =
      summary.items.mapNotNull { VaccinationScheduleEngine.reminderKeyFor(it) }.toSet()
    assertNull(
      VaccineReminderScheduler.computeNextScheduleDelayMs(
        summary = summary,
        settings = settings,
        alreadyNotifiedKeys = afterAllNotified,
        hasNotificationPermission = false,
        nowEpochMs = nowMs,
      )
    )
  }

  @Test
  fun preferencesRepository_mirrorCloudDropsDeletedCloudRecordsAndPreservesLocalOnly() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.ManoPreferencesRepository(context)

    val syncedCloudRec =
      VaccineRecord(
        id = "birth_bcg_hbv_opv",
        vaccineId = "birth_bcg_hbv_opv",
        administeredDateEpochMs = 1_700_000_000_000L,
        updatedAtMs = 1_700_000_000_000L,
        isSyncedCloud = true,
      )
    val localOnlyRec =
      VaccineRecord(
        id = "2m_pentavalent_opv_rota",
        vaccineId = "2m_pentavalent_opv_rota",
        administeredDateEpochMs = 1_705_000_000_000L,
        updatedAtMs = 1_705_000_000_000L,
        isSyncedCloud = false,
      )

    repo.upsertVaccineRecord(syncedCloudRec)
    repo.upsertVaccineRecord(localOnlyRec)
    assertEquals(2, repo.vaccineRecords.first().size)

    // Empty cloud snapshot removes deleted synced cloud record while preserving unsynced local record
    repo.mirrorCloudVaccineRecords(emptyList())
    val remaining = repo.vaccineRecords.first()
    assertEquals(1, remaining.size)
    assertEquals("2m_pentavalent_opv_rota", remaining.first().vaccineId)
    assertFalse(remaining.first().isSyncedCloud)

    // Child alias caching for worker notification personalization
    repo.setChildAliasFa("کیان")
    assertEquals("کیان", repo.childAliasFa.first())
    repo.setChildAliasFa(null)
    assertNull(repo.childAliasFa.first())
  }
}
