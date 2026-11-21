package com.example

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineFeedItem
import com.example.domain.util.TimelineTempPoint
import com.example.ui.components.AlertSeverity
import com.example.ui.components.FeverDoseTimelineCard
import com.example.ui.components.HomeFeverTimelineWidget
import com.example.ui.components.HighFeverUrgentAlertDialog
import com.example.ui.components.InteractiveOralSyringeLabCard
import com.example.ui.screens.Pediatric3DStudioScreen
import com.example.ui.theme.ManoKidsTheme
import com.example.widget.FeverTimelineWidgetStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ManoTimelineAnd3DWalkthroughTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun newbornAt38C_hasImmediateTimelineAlert_evenBelowGeneral39Threshold() {
    val now = 1_750_000_000_000L
    val newborn = ChildProfileDemo(
      aliasFa = "شیرخوار",
      birthDateOrAgeFa = "۲ ماه",
      ageBand = AgeBand.NEWBORN,
      weightKgText = "4",
      weightDateFa = "امروز",
      allergiesFa = "",
      currentMedsFa = "",
      consentGranted = true,
      ageMonthsExact = 2,
    )
    val fever = TimelineTempPoint(
      id = "fever_38",
      tempCelsius = 38.0f,
      timestampMs = now - 60_000L,
      timeLabelFa = "۱۴:۰۰",
      siteFa = "زیربغلی",
      caregiverFa = "مادر",
      noteFa = "",
      isSyncedCloud = false,
    )
    val snapshot = FeverDoseTimelineEngine.buildSnapshot(
      emptyList(), listOf(fever), emptyList(), emptyList(), nowMs = now
    )
    val result = FeverDoseTimelineEngine.evaluateHighFeverAlert(snapshot, newborn)
    assertNotNull(result)
    assertTrue(result!!.titleFa.contains("زیر ۳ ماه"))
    assertTrue(result.bodyFa.contains("فوری"))
  }

  @Test
  fun doseTimeResolver_usesActualClockTimeAndRejectsMalformedEntries() {
    // The caregiver enters local wall time. Anchor the fixture in the same device zone.
    val cal = java.util.Calendar.getInstance().apply {
      clear()
      set(2026, java.util.Calendar.NOVEMBER, 21, 18, 0, 0)
    }
    val now = cal.timeInMillis
    val afternoon = FeverDoseTimelineEngine.resolveDoseTimeMs("۱۴:۰۰", now)
    assertNotNull(afternoon)
    assertEquals(4 * 3_600_000L, now - afternoon!!)

    val nextMorning = now + 12 * 3_600_000L // 06:00 following day
    val lastNight = FeverDoseTimelineEngine.resolveDoseTimeMs("23:00", nextMorning)
    assertNotNull(lastNight)
    assertEquals(7 * 3_600_000L, nextMorning - lastNight!!)
    assertNull(FeverDoseTimelineEngine.resolveDoseTimeMs("25:00", now))
    assertNull(FeverDoseTimelineEngine.resolveDoseTimeMs("دیروز", now))
  }

  @Test
  fun feverDoseTimelineEngine_detectsShortIntervalAndMergesChronologicalFeed() {
    val now = 1_750_000_000_000L
    val hourMs = 3_600_000L
    val recentTemps =
      listOf(
        TimelineTempPoint(
          id = "t1",
          tempCelsius = 38.6f,
          timestampMs = now - 2 * hourMs,
          timeLabelFa = "12:00",
          siteFa = "زیربغلی",
          caregiverFa = "مادر",
          noteFa = "بی‌قراری",
          isSyncedCloud = false,
        ),
        TimelineTempPoint(
          id = "t2",
          tempCelsius = 37.9f,
          timestampMs = now - hourMs / 2,
          timeLabelFa = "13:30",
          siteFa = "زیربغلی",
          caregiverFa = "پدر",
          noteFa = "",
          isSyncedCloud = false,
        ),
        // Outside 24h window -> should be excluded
        TimelineTempPoint(
          id = "t_old",
          tempCelsius = 40.1f,
          timestampMs = now - 26 * hourMs,
          timeLabelFa = "دیروز",
          siteFa = "زیربغلی",
          caregiverFa = "مادر",
          noteFa = "",
          isSyncedCloud = false,
        ),
      )

    val doses =
      listOf(
        TimelineDosePoint(
          id = "d1",
          medicationSlug = "acetaminophen-pediatric",
          medicationNameFa = "استامینوفن",
          dosageFormFa = "شربت ۱۲۰/۵",
          volumeMlText = "5.0 mL",
          timestampMs = now - hourMs, // 1h ago -> interleaved between t1 and t2, and < 4h min interval
          givenAtLabelFa = "13:00",
          noteFa = "مادر",
          isSyncedCloud = false,
        ),
        TimelineDosePoint(
          id = "d2",
          medicationSlug = "ibuprofen-pediatric",
          medicationNameFa = "ایبوپروفن",
          dosageFormFa = "سوسپانسیون",
          volumeMlText = "4.0 mL",
          timestampMs = now - 7 * hourMs, // 7h ago -> over 6h min interval!
          givenAtLabelFa = "07:00",
          noteFa = "پدر",
          isSyncedCloud = false,
        ),
      )

    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs = recentTemps,
        cloudDoseLogs = emptyList(),
        localDoseLogs = doses,
        nowMs = now,
      )

    assertEquals(2, snapshot.tempPoints.size)
    assertEquals(38.6f, snapshot.peakTemp24h ?: 0f, 0.01f)
    assertEquals(37.9f, snapshot.latestTempCelsius ?: 0f, 0.01f)
    assertEquals(AlertSeverity.DANGER, snapshot.acetaminophenStatus.severity)
    assertEquals(AlertSeverity.SUCCESS, snapshot.ibuprofenStatus.severity)

    val merged = FeverDoseTimelineEngine.mergedChronologicalEvents(snapshot)
    assertEquals(4, merged.size)
    assertTrue(merged[0] is TimelineFeedItem.TemperatureEvent) // t2 (-0.5h)
    assertTrue(merged[1] is TimelineFeedItem.DoseEvent)        // d1 (-1h)
    assertTrue(merged[2] is TimelineFeedItem.TemperatureEvent) // t1 (-2h)
    assertTrue(merged[3] is TimelineFeedItem.DoseEvent)        // d2 (-7h)

    val context = ApplicationProvider.getApplicationContext<Application>()
    FeverTimelineWidgetStore.saveAndUpdateWidgets(context, snapshot)
    val (tempSummary, doseSummary, intervalSummary) = FeverTimelineWidgetStore.readSummary(context)
    assertTrue(tempSummary.contains("37.9°C"))
    assertTrue(doseSummary.contains("5.0 mL"))
    assertTrue(intervalSummary.contains("استامینوفن بعدی"))

    val report =
      FeverDoseTimelineEngine.buildDoctorSummaryReportFa(
        snapshot,
        ChildProfileDemo(
          aliasFa = "کیان",
          birthDateOrAgeFa = "۲ سال",
          ageBand = AgeBand.TODDLER_1_3Y,
          weightKgText = "12",
          weightDateFa = "امروز",
          allergiesFa = "ندارد",
          currentMedsFa = "ندارد",
          consentGranted = true,
        ),
      )
    assertTrue(report.contains("کیان"))
    assertTrue(report.contains("38.6°C"))
    assertTrue(report.contains("مادر"))
    assertTrue(report.contains("پدر"))
  }

  @Test
  fun feverDoseTimelineCard_rendersUnifiedFeedAndAllowsCoParentTempSave() {
    val now = System.currentTimeMillis()
    var savedTemp: Float? = null
    var savedCaregiver = ""

    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "tf1",
              tempCelsius = 38.4f,
              timestampMs = now - 3_600_000L,
              timeLabelFa = "12:00",
              siteFa = "زیربغلی",
              caregiverFa = "مادر",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs =
          listOf(
            TimelineDosePoint(
              id = "df1",
              medicationSlug = "acetaminophen-pediatric",
              medicationNameFa = "استامینوفن",
              dosageFormFa = "شربت",
              volumeMlText = "5.0 mL",
              timestampMs = now - 1_800_000L,
              givenAtLabelFa = "12:30",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        nowMs = now,
      )

    composeTestRule.setContent {
      ManoKidsTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          FeverDoseTimelineCard(
            snapshot = snapshot,
            isCloudSynced = false,
            onSaveTempLog = { temp, _, _, caregiver, _, onDone ->
              savedTemp = temp
              savedCaregiver = caregiver
              onDone()
            },
            onDeleteTempLog = { _, _ -> },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fever_dose_timeline_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fever_timeline_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timeline_share_doctor_btn").performScrollTo().assertIsDisplayed()

    // Both temp and dose nodes appear together in unified vertical timeline feed
    composeTestRule.onNodeWithTag("timeline_unified_feed").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("timeline_feed_item_temp_tf1").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("timeline_feed_item_dose_df1").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("timeline_save_temp_btn").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("timeline_temp_input").performScrollTo().performTextInput("۳۷٫۶")
    composeTestRule.onNodeWithTag("caregiver_chip_پدر").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("timeline_save_temp_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals(37.6f, savedTemp)
    assertEquals("پدر", savedCaregiver)
  }

  @Test
  fun homeFeverTimelineWidget_displaysSparklineQuickSavesAndOpensTimeline() {
    val now = System.currentTimeMillis()
    var savedQuickTemp: Float? = null
    var openedFullTimeline = false

    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "ht1",
              tempCelsius = 38.2f,
              timestampMs = now - 2_400_000L,
              timeLabelFa = "11:40",
              siteFa = "زیربغلی",
              caregiverFa = "مادر",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs =
          listOf(
            TimelineDosePoint(
              id = "hd1",
              medicationSlug = "acetaminophen-pediatric",
              medicationNameFa = "استامینوفن",
              dosageFormFa = "شربت",
              volumeMlText = "4.5 mL",
              timestampMs = now - 1_200_000L,
              givenAtLabelFa = "12:00",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        nowMs = now,
      )

    composeTestRule.setContent {
      ManoKidsTheme {
        HomeFeverTimelineWidget(
          snapshot = snapshot,
          onQuickSaveTemp = { temp, _ -> savedQuickTemp = temp },
          onOpenFullTimeline = { openedFullTimeline = true },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_fever_timeline_widget").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_timeline_sparkline").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_widget_quick_log_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_widget_save_temp_btn").performClick()
    composeTestRule.waitForIdle()
    assertNotNull(savedQuickTemp)

    composeTestRule.onNodeWithTag("home_timeline_widget_open_btn").performClick()
    assertTrue(openedFullTimeline)
  }

  @Test
  fun syringeLabCard_triggers3DHandoffWithCalculatedDose() {
    var handoffMl: Float? = null
    composeTestRule.setContent {
      ManoKidsTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          InteractiveOralSyringeLabCard(
            childProfile =
              ChildProfileDemo(
                aliasFa = "سارا",
                birthDateOrAgeFa = "۳ سال",
                ageBand = AgeBand.TODDLER_1_3Y,
                weightKgText = "12",
                weightDateFa = "امروز",
                allergiesFa = "ندارد",
                currentMedsFa = "ندارد",
                consentGranted = true,
              ),
            onOpen3DWithDose = { ml -> handoffMl = ml },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("syringe_confirm_label").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("syringe_open_3d_dose_btn").assertDoesNotExist()
    composeTestRule.onNodeWithTag("syringe_selected_ml_label").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun pediatric3DStudio_prefillSyncsTargetBannerAndFourStepWalkthrough() {
    composeTestRule.setContent {
      ManoKidsTheme {
        Pediatric3DStudioScreen(
          initialScene = "workbench",
          prefillMl = 5.0f,
          prefillFever = true,
          latestTempCelsius = 38.7f,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("studio_3d_target_sync_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_3d_guided_steps_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_3d_shake_bottle_btn").assertIsDisplayed()

    composeTestRule.onNodeWithTag("guided_step_2").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("studio_3d_guided_instruction").assertIsDisplayed()
    composeTestRule.onNodeWithText("پیش از کشیدن شربت", substring = true).assertIsDisplayed()
  }

  @Test
  fun feverDoseTimelineEngine_triggersHighFeverAlertStrictlyAbove39Degrees() {
    val now = 1_750_000_000_000L
    val child =
      ChildProfileDemo(
        aliasFa = "آوا",
        birthDateOrAgeFa = "۲ سال",
        ageBand = AgeBand.TODDLER_1_3Y,
        weightKgText = "12",
        weightDateFa = "امروز",
        allergiesFa = "ندارد",
        currentMedsFa = "ندارد",
        consentGranted = true,
      )

    val boundarySnapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "tb39",
              tempCelsius = 39.0f,
              timestampMs = now - 60_000L,
              timeLabelFa = "14:00",
              siteFa = "زیربغلی",
              caregiverFa = "مادر",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs = emptyList(),
        nowMs = now,
      )
    assertNull(FeverDoseTimelineEngine.evaluateHighFeverAlert(boundarySnapshot, child))

    val highSnapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "th391",
              tempCelsius = 39.1f,
              timestampMs = now - 60_000L,
              timeLabelFa = "14:15",
              siteFa = "پیشانی",
              caregiverFa = "پدر",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs = emptyList(),
        nowMs = now,
      )
    val alert = FeverDoseTimelineEngine.evaluateHighFeverAlert(highSnapshot, child)
    assertNotNull(alert)
    assertEquals(39.1f, alert!!.triggerTempCelsius, 0.01f)
    assertTrue(alert.titleFa.contains("مراجعه به پزشک"))
    assertTrue(alert.bodyFa.contains("آوا"))
    assertTrue(alert.bodyFa.contains("پدر"))
    assertEquals(4, alert.checklistFa.size)
  }

  @Test
  fun feverDoseTimelineCard_showsUrgentDoctorBannerWhenTempExceeds39() {
    val now = System.currentTimeMillis()
    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "th_card",
              tempCelsius = 39.4f,
              timestampMs = now - 600_000L,
              timeLabelFa = "15:10",
              siteFa = "زیربغلی",
              caregiverFa = "مادر",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs = emptyList(),
        nowMs = now,
      )

    composeTestRule.setContent {
      ManoKidsTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          FeverDoseTimelineCard(
            snapshot = snapshot,
            isCloudSynced = false,
            onSaveTempLog = { _, _, _, _, _, onDone -> onDone() },
            onDeleteTempLog = { _, _ -> },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("timeline_high_fever_urgent_banner").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun homeFeverTimelineWidget_showsUrgentHighFeverAlertWhenTempExceeds39() {
    val now = System.currentTimeMillis()
    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "th_home",
              tempCelsius = 39.4f,
              timestampMs = now - 300_000L,
              timeLabelFa = "15:20",
              siteFa = "زیربغلی",
              caregiverFa = "مادر",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs = emptyList(),
        nowMs = now,
      )

    composeTestRule.setContent {
      ManoKidsTheme {
        HomeFeverTimelineWidget(
          snapshot = snapshot,
          onQuickSaveTemp = { _, _ -> },
          onOpenFullTimeline = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_widget_high_fever_alert").assertIsDisplayed()
  }

  @Test
  fun highFeverUrgentAlertDialog_displaysActionsAndInvokesCallbacks() {
    val now = System.currentTimeMillis()
    val snapshot =
      FeverDoseTimelineEngine.buildSnapshot(
        cloudFeverLogs = emptyList(),
        localFeverLogs =
          listOf(
            TimelineTempPoint(
              id = "th_dlg",
              tempCelsius = 39.6f,
              timestampMs = now - 60_000L,
              timeLabelFa = "15:30",
              siteFa = "زیربغلی",
              caregiverFa = "مادر",
              noteFa = "",
              isSyncedCloud = false,
            )
          ),
        cloudDoseLogs = emptyList(),
        localDoseLogs = emptyList(),
        nowMs = now,
      )
    val alert = FeverDoseTimelineEngine.evaluateHighFeverAlert(snapshot)!!
    var openedDoctorGuide = false
    var dismissedCount = 0

    composeTestRule.setContent {
      ManoKidsTheme {
        HighFeverUrgentAlertDialog(
          alert = alert,
          canRequestSystemNotifications = true,
          onRequestSystemNotifications = {},
          onOpenWhenToSeeDoctor = { openedDoctorGuide = true },
          onDismiss = { dismissedCount += 1 },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("high_fever_urgent_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("high_fever_enable_notif_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("high_fever_call_115_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("high_fever_doctor_guide_btn").performClick()
    composeTestRule.waitForIdle()
    assertTrue(openedDoctorGuide)
    assertEquals(1, dismissedCount)
  }

  @Test
  fun pediatric3DStudio_weighingMannequinGuide_rendersStepsPosturesAndAdvances() {
    composeTestRule.setContent {
      ManoKidsTheme {
        Pediatric3DStudioScreen(
          initialScene = "weighing",
          childProfile =
            ChildProfileDemo(
              aliasFa = "رادین",
              birthDateOrAgeFa = "۱۰ ماهه",
              ageBand = AgeBand.INFANT_3_12M,
              weightKgText = "9.5",
              weightDateFa = "امروز",
              allergiesFa = "ندارد",
              currentMedsFa = "ندارد",
              consentGranted = true,
            ),
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("scene_switch_weighing").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weighing_guided_steps_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weighing_posture_infant").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weighing_posture_toddler").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weighing_guided_instruction").assertIsDisplayed()
    composeTestRule.onNodeWithText("9.5 kg", substring = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("weighing_step_2").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("لباس ضخیم", substring = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("weighing_next_step_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("مرکز حلقه ترازو", substring = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("weighing_posture_toddler").performClick()
    composeTestRule.onNodeWithTag("weighing_next_step_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("LOCKED", substring = true).assertIsDisplayed()
  }

  @Test
  fun pediatric3DStudio_switchesFromWorkbenchToWeighingScene() {
    composeTestRule.setContent {
      ManoKidsTheme {
        Pediatric3DStudioScreen(initialScene = "workbench")
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("scene_switch_weighing").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("weighing_guided_steps_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weighing_step_1").assertIsDisplayed()
  }
}
