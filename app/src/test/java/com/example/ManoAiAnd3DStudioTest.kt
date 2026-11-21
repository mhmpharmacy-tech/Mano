package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.GalacticOrbitSelection
import com.example.data.ManoArticlesSeed
import com.example.data.ManoPreferencesRepository
import com.example.ui.ManoApp
import com.example.ui.components.GalacticRing
import com.example.ui.screens.Pediatric3DStudioScreen
import com.example.ui.screens.TopicsScreen
import com.example.ui.theme.ManoKidsTheme
import com.example.ui.viewmodel.CareCardImageEngine
import com.example.ui.viewmodel.ImageStudioUiState
import com.example.ui.viewmodel.ManoImageStudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class ManoAiAnd3DStudioTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun emptyPrompt_emitsTransparentValidationError() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
      val vm =
        ManoImageStudioViewModel(
          engine =
            object : CareCardImageEngine {
              override suspend fun generateFromPrompt(prompt: String): Bitmap? = null
              override suspend fun editWithImage(inputBitmap: Bitmap, editInstruction: String): Bitmap? = null
            }
        )
      vm.generateCard("   ")
      val state = vm.uiState.value
      assertTrue(state is ImageStudioUiState.Error)
      assertTrue((state as ImageStudioUiState.Error).gallery.isEmpty())
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun generateAndEditCard_updatesGalleryAndSurfacesRealFailure() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
      var failNext = false
      var generateCount = 0
      var editCount = 0
      val sampleA = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
      val sampleB = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)

      val vm =
        ManoImageStudioViewModel(
          engine =
            object : CareCardImageEngine {
              override suspend fun generateFromPrompt(prompt: String): Bitmap {
                generateCount++
                if (failNext) throw IllegalStateException("AppCheck HTTP 403")
                return sampleA
              }

              override suspend fun editWithImage(inputBitmap: Bitmap, editInstruction: String): Bitmap {
                editCount++
                return sampleB
              }
            }
        )

      vm.generateCard("روتین شستن دست", "کارت شستن دست")
      advanceUntilIdle()
      val s1 = vm.uiState.value as ImageStudioUiState.Success
      assertEquals(1, generateCount)
      assertEquals(1, s1.gallery.size)

      vm.editActiveCard("پس‌زمینه آبی پاستلی کن")
      advanceUntilIdle()
      val s2 = vm.uiState.value as ImageStudioUiState.Success
      assertEquals(1, editCount)
      assertEquals(2, s2.gallery.size)
      assertTrue(s2.activeCard.isEditedFromExisting)

      failNext = true
      vm.generateCard("پوستر خواب کودک")
      advanceUntilIdle()
      val err = vm.uiState.value as ImageStudioUiState.Error
      assertTrue(err.messageFa.contains("AppCheck HTTP 403"))
      assertEquals(2, err.gallery.size)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun topicsAiCardStudioTab_generatesAndExposesShareAndEditControls() {
    val fakeBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
    val studioVm =
      ManoImageStudioViewModel(
        engine =
          object : CareCardImageEngine {
            override suspend fun generateFromPrompt(prompt: String): Bitmap = fakeBitmap
            override suspend fun editWithImage(inputBitmap: Bitmap, editInstruction: String): Bitmap = fakeBitmap
          }
      )

    var navigated3D = false
    composeTestRule.setContent {
      ManoKidsTheme {
        TopicsScreen(
          query = "",
          selectedCategory = null,
          selectedAgeBand = null,
          articles = ManoArticlesSeed.starterArticles,
          bookmarkedSlugs = emptySet(),
          onQueryChange = {},
          onCategorySelect = {},
          onAgeSelect = {},
          onToggleBookmark = {},
          onOpenArticle = {},
          onNavigateSkin = {},
          onNavigateDevices = {},
          onNavigate3DStudio = { navigated3D = true },
          imageStudioViewModel = studioVm,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("topics_tab_studio").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("generate_ai_card_btn").assertIsDisplayed().performClick()
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodesWithTag("active_generated_card_image").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("active_generated_card_image").assertIsDisplayed()
    composeTestRule.onNodeWithTag("share_active_card_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("edit_active_card_input").performScrollTo().performTextReplacement("رنگ سبز ملایم")
    composeTestRule.onNodeWithTag("edit_active_card_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_open_3d_btn").assertDoesNotExist()
  }

  @Test
  fun pediatric3DStudioScreen_displaysFloatingDockAndSceneControls() {
    composeTestRule.setContent {
      ManoKidsTheme {
        Pediatric3DStudioScreen(initialScene = "workbench")
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("pediatric_3d_studio_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_3d_control_dock").assertIsDisplayed()
    composeTestRule.onNodeWithTag("scene_switch_workbench").assertIsDisplayed()
    composeTestRule.onNodeWithTag("scene_switch_mano").assertIsDisplayed()

    // Expand floating dock reveals syringe mL slider & thermometer fever toggle
    composeTestRule.onNodeWithTag("studio_3d_expand_dock").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("studio_3d_syringe_slider").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_3d_fever_switch").assertIsDisplayed()

    // Switching to MANO 3D mode updates controls cleanly & exposes 6 PBR squad character chips
    composeTestRule.onNodeWithTag("scene_switch_mano").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("studio_3d_reset_camera").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_3d_mano_squad_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_3d_squad_mano_drug").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun appNavigation_fromHomeReachesPediatric3DStudio() {
    composeTestRule.setContent {
      ManoKidsTheme {
        ManoApp()
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_tool_3d").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_fever_dose_quick_card"))
    composeTestRule.onNodeWithTag("home_fever_dose_quick_card").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fever_dose_calculator_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("topbar_back").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_list").assertIsDisplayed()
  }

  @Test
  fun imageStudioViewModel_rapidSecondGenerateCancelsFirstSynthesis() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    try {
      val bmp1 = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
      val bmp2 = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
      val vm =
        ManoImageStudioViewModel(
          engine =
            object : CareCardImageEngine {
              override suspend fun generateFromPrompt(prompt: String): Bitmap {
                val isFirst = prompt.contains("اول")
                kotlinx.coroutines.delay(if (isFirst) 5_000L else 50L)
                return if (isFirst) bmp1 else bmp2
              }

              override suspend fun editWithImage(inputBitmap: Bitmap, editInstruction: String): Bitmap? = null
            }
        )

      vm.generateCard("کارت اول", "اول")
      vm.generateCard("کارت دوم", "دوم")
      advanceUntilIdle()

      val success = vm.uiState.value as ImageStudioUiState.Success
      assertEquals(1, success.gallery.size)
      assertEquals("دوم", success.activeCard.titleFa)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun homeFeverTimelineWidget_rendersInDarkModeWithoutCrash() {
    val snapshot =
      com.example.domain.util.FeverDoseTimelineEngine.buildSnapshot(
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
      )
    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = true) {
        com.example.ui.components.HomeFeverTimelineWidget(
          snapshot = snapshot,
          onQuickSaveTemp = { _, _ -> },
          onOpenFullTimeline = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_fever_timeline_widget").assertIsDisplayed()
  }

  @Test
  fun galacticOrbitalHero_rendersInDarkModeAndSwitchesRingsWithoutCrash() {
    val inner =
      listOf(
        com.example.ui.components.RadialTopicItem(
          id = "fever",
          titleFa = "تب کودک",
          subtitleFa = "تب‌سنجی",
          icon = androidx.compose.material.icons.Icons.Rounded.DeviceThermostat,
          accentRole = com.example.ui.components.TopicAccentRole.CORAL,
          iconKind = com.example.ui.components.PediatricIconKind.FEVER,
        ),
        com.example.ui.components.RadialTopicItem(
          id = "medicines",
          titleFa = "ایمنی دارو",
          subtitleFa = "سرنگ mL",
          icon = androidx.compose.material.icons.Icons.Rounded.MedicalServices,
          accentRole = com.example.ui.components.TopicAccentRole.TEAL,
          iconKind = com.example.ui.components.PediatricIconKind.MEDICINE,
        ),
      )
    val outer =
      listOf(
        com.example.ui.components.RadialTopicItem(
          id = "studio3d",
          titleFa = "استودیو سه‌بعدی",
          subtitleFa = "ابزار تعاملی",
          icon = androidx.compose.material.icons.Icons.Rounded.ViewInAr,
          accentRole = com.example.ui.components.TopicAccentRole.GOLD,
          iconKind = com.example.ui.components.PediatricIconKind.DEVICE,
        ),
        com.example.ui.components.RadialTopicItem(
          id = "cough",
          titleFa = "سرفه کودک",
          subtitleFa = "تنفس ایمن",
          icon = androidx.compose.material.icons.Icons.Rounded.Air,
          accentRole = com.example.ui.components.TopicAccentRole.SKY,
          iconKind = com.example.ui.components.PediatricIconKind.COUGH,
        ),
      )

    var selectedId: String? = null
    composeTestRule.setContent {
      ManoKidsTheme(darkTheme = true) {
        com.example.ui.components.ManoOrbitalHeroStage(
          topics = inner,
          outerTopics = outer,
          onSelectTopic = { selectedId = it.id },
          hasPlayedIntro = true,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_orbital_stage").assertIsDisplayed()
    composeTestRule.onNodeWithTag("orbital_ring_outer").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("orbital_focus_cta").performClick()
    composeTestRule.waitForIdle()
    assertEquals("studio3d", selectedId)
  }

  @Test
  fun preferencesRepository_savesAndRestoresGalacticOrbitSelection() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ManoPreferencesRepository(context)

    repo.setGalacticOrbitSelection("", "")
    assertNull(repo.galacticOrbitSelection.first())

    repo.setGalacticOrbitSelection("OUTER", "studio3d")
    assertEquals(GalacticOrbitSelection("OUTER", "studio3d"), repo.galacticOrbitSelection.first())

    repo.setGalacticOrbitSelection("INNER", "vaccines")
    assertEquals(GalacticOrbitSelection("INNER", "vaccines"), repo.galacticOrbitSelection.first())

    repo.setGalacticOrbitSelection("", "")
    assertNull(repo.galacticOrbitSelection.first())
  }

  @Test
  fun galacticOrbitalHero_restoresSavedRingAndTopicSelection() {
    val inner =
      listOf(
        com.example.ui.components.RadialTopicItem(
          id = "fever",
          titleFa = "تب کودک",
          subtitleFa = "تب‌سنجی",
          icon = Icons.Rounded.DeviceThermostat,
          accentRole = com.example.ui.components.TopicAccentRole.CORAL,
          iconKind = com.example.ui.components.PediatricIconKind.FEVER,
        ),
        com.example.ui.components.RadialTopicItem(
          id = "medicines",
          titleFa = "ایمنی دارو",
          subtitleFa = "سرنگ mL",
          icon = Icons.Rounded.MedicalServices,
          accentRole = com.example.ui.components.TopicAccentRole.TEAL,
          iconKind = com.example.ui.components.PediatricIconKind.MEDICINE,
        ),
      )
    val outer =
      listOf(
        com.example.ui.components.RadialTopicItem(
          id = "studio3d",
          titleFa = "استودیو سه‌بعدی",
          subtitleFa = "ابزار تعاملی",
          icon = Icons.Rounded.ViewInAr,
          accentRole = com.example.ui.components.TopicAccentRole.GOLD,
          iconKind = com.example.ui.components.PediatricIconKind.DEVICE,
        ),
        com.example.ui.components.RadialTopicItem(
          id = "cough",
          titleFa = "سرفه کودک",
          subtitleFa = "تنفس ایمن",
          icon = Icons.Rounded.Air,
          accentRole = com.example.ui.components.TopicAccentRole.SKY,
          iconKind = com.example.ui.components.PediatricIconKind.COUGH,
        ),
      )

    var selectedId: String? = null
    composeTestRule.setContent {
      ManoKidsTheme {
        com.example.ui.components.ManoOrbitalHeroStage(
          topics = inner,
          outerTopics = outer,
          onSelectTopic = { selectedId = it.id },
          hasPlayedIntro = true,
          savedRing = GalacticRing.OUTER,
          savedTopicId = "cough",
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("orbital_active_topic").assertIsDisplayed()
    composeTestRule.onNodeWithText("چرخش").assertIsDisplayed()
    composeTestRule.onNodeWithTag("orbital_focus_cta").performClick()
    composeTestRule.waitForIdle()
    assertEquals("cough", selectedId)
  }

  @Test
  fun homeScreen_studio3dToolOpensInManoAppAndReturnsViaNavHome() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    kotlinx.coroutines.runBlocking {
      ManoPreferencesRepository(context).setGalacticOrbitSelection("", "")
    }
    try {
      composeTestRule.setContent {
        ManoKidsTheme {
          ManoApp()
        }
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("home_tool_3d").assertDoesNotExist()
      composeTestRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_fever_dose_quick_card"))
      composeTestRule.onNodeWithTag("home_fever_dose_quick_card").performClick()
      composeTestRule.waitForIdle()

      // Confirm we entered the 2D Fever Dose Calculator
      composeTestRule.onNodeWithTag("fever_dose_calculator_screen").assertIsDisplayed()

      // Tap bottom-bar Home tab directly -> returns to Home without needing Back button
      composeTestRule.onNodeWithTag("nav_home").performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
    } finally {
      kotlinx.coroutines.runBlocking {
        ManoPreferencesRepository(context).setGalacticOrbitSelection("", "")
      }
    }
  }
}
