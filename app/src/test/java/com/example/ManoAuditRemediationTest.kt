package com.example

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.ManoPreferencesRepository
import com.example.data.ManoThemeMode
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.LiquidFormulationId
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineTempPoint
import com.example.ui.components.ManoOrbitalHeroStage
import com.example.ui.components.PediatricIconKind
import com.example.ui.components.RadialTopicItem
import com.example.ui.components.TopicAccentRole
import com.example.ui.theme.ManoKidsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ManoAuditRemediationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @After
  fun cleanupPreferences() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ManoPreferencesRepository(context)
    runBlocking {
      repo.setThemeMode(ManoThemeMode.SYSTEM)
      repo.saveLocalChildProfile(null)
      repo.clearLocalFeverLogs()
      repo.clearLocalDoseLogs()
    }
  }

  @Test
  fun conservativeInfantFallback_blocksIbuprofenWhenExactAgeUnknown_andUnlocksAtSevenMonths() {
    val unspecificInfant =
      ChildProfileDemo(
        aliasFa = "نی‌نی",
        birthDateOrAgeFa = AgeBand.INFANT_3_12M.labelFa,
        ageBand = AgeBand.INFANT_3_12M,
        weightKgText = "7.5",
        consentGranted = true,
      )

    val conservativeMonths = LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(unspecificInfant)
    assertEquals(3, conservativeMonths)

    val conservativeIbuprofen =
      LiquidDoseVisualizerEngine.evaluate(
        formulation = LiquidFormulationId.IBUPROFEN_SYRUP_100_5,
        ageMonths = conservativeMonths,
        weightKg = 7.5f,
      )
    assertTrue("Ibuprofen must stay blocked when INFANT_3_12M lacks exact >=6m age", conservativeIbuprofen.isBlocked)

    val sevenMonthsTextProfile = unspecificInfant.copy(birthDateOrAgeFa = "۷ ماه")
    val parsedMonths = LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(sevenMonthsTextProfile)
    assertEquals(7, parsedMonths)

    val nowMs = 1_750_000_000_000L
    val sevenMonthsAgoMs = nowMs - (7.0 * 30.4375 * 86_400_000.0).toLong()
    val fromBirthDate =
      LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(
        profile = unspecificInfant,
        birthDateEpochMs = sevenMonthsAgoMs,
        nowMs = nowMs,
      )
    assertEquals(7, fromBirthDate)

    val safeIbuprofen =
      LiquidDoseVisualizerEngine.evaluate(
        formulation = LiquidFormulationId.IBUPROFEN_SYRUP_100_5,
        ageMonths = fromBirthDate,
        weightKg = 8.0f,
      )
    assertFalse("Ibuprofen should be unlocked once exact age >= 6 months", safeIbuprofen.isBlocked)
  }

  @Test
  fun preferencesRepository_persistsThemeChildProfileTimelineAndMilestones() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ManoPreferencesRepository(context)

    repo.setThemeMode(ManoThemeMode.DARK)
    assertEquals(ManoThemeMode.DARK, repo.themeMode.first())

    val profile =
      ChildProfileDemo(
        aliasFa = "آوا",
        birthDateOrAgeFa = "۱۸ ماه",
        ageBand = AgeBand.TODDLER_1_3Y,
        ageMonthsExact = 18,
        weightKgText = "11.0",
        consentGranted = true,
      )
    repo.saveLocalChildProfile(profile)
    assertEquals(profile, repo.localChildProfile.first())

    val fever =
      TimelineTempPoint(
        id = "f1",
        tempCelsius = 38.4f,
        siteFa = "زیربغلی",
        timeLabelFa = "۱۴:۳۰",
        timestampMs = 1_750_000_000_000L,
        caregiverFa = "مادر",
        noteFa = "اندازه‌گیری دقیق",
        isCloud = false,
      )
    val dose =
      TimelineDosePoint(
        id = "d1",
        medicationSlug = "acetaminophen-pediatric",
        medicationNameFa = "استامینوفن",
        dosageFormFa = "شربت ۱۲۰/۵",
        volumeMl = 5.0f,
        volumeMlText = "5.0 mL",
        timeLabelFa = "۱۴:۳۵",
        timestampMs = 1_750_000_300_000L,
        noteFa = "با سرنگ",
        isCloud = false,
      )
    repo.saveLocalFeverLogs(listOf(fever))
    repo.saveLocalDoseLogs(listOf(dose))
    assertEquals(listOf(fever), repo.localFeverLogs.first())
    assertEquals(listOf(dose), repo.localDoseLogs.first())

    repo.toggleMilestoneCheck("ms_walk_15m")
    assertTrue("ms_walk_15m" in repo.checkedMilestoneIds.first())
    repo.toggleMilestoneCheck("ms_walk_15m")
    assertFalse("ms_walk_15m" in repo.checkedMilestoneIds.first())

    repo.saveLocalChildProfile(null)
    repo.clearLocalFeverLogs()
    repo.clearLocalDoseLogs()
    assertNull(repo.localChildProfile.first())
    assertTrue(repo.localFeverLogs.first().isEmpty())
    assertTrue(repo.localDoseLogs.first().isEmpty())
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h780dp-xhdpi")
  fun orbitalHeroStage_narrowPhone_noSatelliteClipsOutsideStageBounds() {
    val innerTopics =
      listOf(
        RadialTopicItem("fever", "تب کودک", "تب‌سنجی", Icons.Rounded.DeviceThermostat, TopicAccentRole.CORAL, PediatricIconKind.FEVER),
        RadialTopicItem("medicines", "ایمنی دارو", "سرنگ mL", Icons.Rounded.MedicalServices, TopicAccentRole.TEAL, PediatricIconKind.MEDICINE),
        RadialTopicItem("vaccines", "تقویم واکسن", "یادآور", Icons.Rounded.Vaccines, TopicAccentRole.PLUM, PediatricIconKind.MEDICINE),
        RadialTopicItem("growth", "رشد", "نقاط عطف", Icons.Rounded.ChildCare, TopicAccentRole.SAGE, PediatricIconKind.GROWTH),
        RadialTopicItem("nutrition", "تغذیه", "غذای کودک", Icons.Rounded.Restaurant, TopicAccentRole.MANGO, PediatricIconKind.FORMULA),
        RadialTopicItem("urgent", "اورژانس", "علائم خطر", Icons.Rounded.HealthAndSafety, TopicAccentRole.STRAWBERRY, PediatricIconKind.URGENT),
      )
    val outerTopics =
      listOf(
        RadialTopicItem("cough", "سرفه", "تنفس", Icons.Rounded.Air, TopicAccentRole.SKY, PediatricIconKind.COUGH),
        RadialTopicItem("scanner", "اسکنر شربت", "دوربین هوشمند", Icons.Rounded.QrCodeScanner, TopicAccentRole.TEAL, PediatricIconKind.MEDICINE),
        RadialTopicItem("formula", "شیرخشک", "راهنما", Icons.Rounded.LocalDrink, TopicAccentRole.MANGO, PediatricIconKind.FORMULA),
        RadialTopicItem("diaper", "پوشک", "پوست", Icons.Rounded.Spa, TopicAccentRole.PLUM, PediatricIconKind.DIAPER),
        RadialTopicItem("studio3d", "سه‌بعدی", "سرنگ 3D", Icons.Rounded.ViewInAr, TopicAccentRole.AQUA, PediatricIconKind.DEVICE),
      )

    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = false) {
        ManoOrbitalHeroStage(
          topics = innerTopics,
          outerTopics = outerTopics,
          onSelectTopic = {},
          modifier = Modifier.padding(16.dp),
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("orbital_toggle_spin").performClick()
    composeTestRule.waitForIdle()

    val stageBounds = composeTestRule.onNodeWithTag("home_orbital_stage").fetchSemanticsNode().boundsInRoot
    val allIds = (innerTopics + outerTopics).map { it.id }
    allIds.forEach { id ->
      val itemNode = composeTestRule.onNodeWithTag("orbital_item_$id").assertIsDisplayed()
      val bounds = itemNode.fetchSemanticsNode().boundsInRoot
      assertTrue(
        "Satellite $id left=${bounds.left} clipped left of stage=${stageBounds.left}",
        bounds.left >= stageBounds.left - 1f,
      )
      assertTrue(
        "Satellite $id right=${bounds.right} clipped right of stage=${stageBounds.right}",
        bounds.right <= stageBounds.right + 1f,
      )
      assertTrue(
        "Satellite $id top=${bounds.top} clipped above stage=${stageBounds.top}",
        bounds.top >= stageBounds.top - 1f,
      )
      assertTrue(
        "Satellite $id bottom=${bounds.bottom} clipped below stage=${stageBounds.bottom}",
        bounds.bottom <= stageBounds.bottom + 1f,
      )
    }
  }
}
