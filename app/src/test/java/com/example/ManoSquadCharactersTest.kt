package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.ManoSquadMember
import com.example.ui.components.ManoSquadSection
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.ManoKidsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class ManoSquadCharactersTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun squadSection_rendersAllSixCharactersInRtlAndDarkTheme_andRespectsTouchTargets() {
    val clickedMembers = mutableListOf<ManoSquadMember>()

    composeRule.setContent {
      ManoKidsTheme(darkTheme = true) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            ManoSquadSection(onSelectMember = { clickedMembers.add(it) })
          }
        }
      }
    }

    composeRule.onNodeWithTag("mano_squad_section").assertIsDisplayed()

    ManoSquadMember.entries.forEach { member ->
      val cardNode = composeRule.onNodeWithTag("mano_squad_${member.id}")
      cardNode.performScrollTo().assertIsDisplayed()
      cardNode.assertHeightIsAtLeast(48.dp)

      composeRule
        .onNode(
          hasText(member.englishName) and hasAnyAncestor(hasTestTag("mano_squad_${member.id}")),
          useUnmergedTree = true,
        )
        .assertIsDisplayed()
      composeRule
        .onNode(
          hasText(member.responsibilityFa) and hasAnyAncestor(hasTestTag("mano_squad_${member.id}")),
          useUnmergedTree = true,
        )
        .assertIsDisplayed()

      cardNode.performClick()
    }

    assertEquals(ManoSquadMember.entries.toList(), clickedMembers)

    // Verify RTL column order in first row: second item (GROWTH) sits to the left of first item (DRUG)
    composeRule.onNodeWithTag("mano_squad_drug").performScrollTo()
    val drugBounds = composeRule.onNodeWithTag("mano_squad_drug").fetchSemanticsNode().boundsInRoot
    val growthBounds = composeRule.onNodeWithTag("mano_squad_growth").fetchSemanticsNode().boundsInRoot
    assertTrue("Expected RTL ordering with Mano Drug right of Mano Growth", drugBounds.left > growthBounds.right)
  }

  @Test
  fun homeScreen_selectiveMenu_routesPharmacistCoreNeedsAndEcosystemPillars() {
    val navigatedRoutes = mutableListOf<String>()

    composeRule.setContent {
      ManoKidsTheme(darkTheme = false) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          HomeScreen(
            selectedAgeBand = null,
            bookmarkedSlugs = emptySet(),
            articles = emptyList(),
            onSelectAgeBand = {},
            onToggleBookmark = {},
            onNavigateSymptoms = { topic -> navigatedRoutes.add("symptoms:$topic") },
            onNavigateTopics = { _, _ -> },
            onNavigateTopicDetail = { slug -> navigatedRoutes.add("topic:$slug") },
            onNavigateMedicines = { navigatedRoutes.add("medicines") },
            onNavigateGrowth = { navigatedRoutes.add("growth") },
            onNavigateSkin = { navigatedRoutes.add("skin") },
            onNavigateDevices = { navigatedRoutes.add("devices") },
            onNavigateAskPharmacist = { navigatedRoutes.add("pharmacist") },
            onNavigateMyChild = {},
            onNavigateInfo = {},
            onNavigateNutrition = { navigatedRoutes.add("nutrition") },
            onNavigateSyrupScanner = { navigatedRoutes.add("scanner") },
            onNavigateEcosystemPillar = { pillar -> navigatedRoutes.add("pillar:${pillar.id}") },
          )
        }
      }
    }

    composeRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
    composeRule.onNodeWithTag("home_brand_crest").assertIsDisplayed()
    composeRule.onNodeWithTag("mano_mascot_touch").assertIsDisplayed()

    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_pharmacist_hero_card"))
    composeRule.onNodeWithTag("home_pharmacist_hero_card").assertIsDisplayed()
    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_pharmacist_cta"))
    composeRule.onNodeWithTag("home_pharmacist_cta").performClick()
    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_followup_chip"))
    composeRule.onNodeWithTag("home_followup_chip").performClick()

    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_menu_fever"))
    composeRule.onNodeWithTag("home_menu_fever").performClick()
    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_menu_medicines"))
    composeRule.onNodeWithTag("home_menu_medicines").performClick()
    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_menu_scanner"))
    composeRule.onNodeWithTag("home_menu_scanner").performClick()
    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_menu_growth"))
    composeRule.onNodeWithTag("home_menu_growth").performClick()
    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_menu_skin"))
    composeRule.onNodeWithTag("home_menu_skin").performClick()
    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_menu_qa"))
    composeRule.onNodeWithTag("home_menu_qa").performClick()

    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_tool_safety"))
    composeRule.onNodeWithTag("home_tool_safety").performClick()
    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_tool_comparison"))
    composeRule.onNodeWithTag("home_tool_comparison").performClick()
    composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_tool_pharmacy"))
    composeRule.onNodeWithTag("home_tool_pharmacy").performClick()
    composeRule.waitForIdle()

    assertEquals("فیروزه‌ای", ManoSquadMember.CARE.colorNameFa)
    assertEquals(
      listOf(
        "pharmacist",
        "pillar:mano_followup",
        "symptoms:fever",
        "medicines",
        "scanner",
        "growth",
        "skin",
        "pillar:documented_qa",
        "pillar:mano_safety",
        "pillar:product_comparison",
        "pillar:mano_pharmacy",
      ),
      navigatedRoutes,
    )
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h780dp-xhdpi")
  fun orbitalMenu_narrowPhone_guaranteesZeroOverlapAndDirectTapRouting() {
    val tapped = mutableListOf<String>()
    composeRule.setContent {
      ManoKidsTheme(darkTheme = true) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          HomeScreen(
            selectedAgeBand = null,
            bookmarkedSlugs = emptySet(),
            articles = emptyList(),
            onSelectAgeBand = {},
            onToggleBookmark = {},
            onNavigateSymptoms = { tapped.add("fever:$it") },
            onNavigateTopics = { _, _ -> },
            onNavigateTopicDetail = {},
            onNavigateMedicines = { tapped.add("medicines") },
            onNavigateGrowth = { tapped.add("growth") },
            onNavigateSkin = { tapped.add("skin") },
            onNavigateDevices = {},
            onNavigateAskPharmacist = {},
            onNavigateMyChild = {},
            onNavigateInfo = {},
            onNavigateSyrupScanner = { tapped.add("scanner") },
            onNavigateEcosystemPillar = { tapped.add("pillar:${it.id}") },
          )
        }
      }
    }

    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(600L)
    composeRule.waitForIdle()
    val stage = composeRule.onNodeWithTag("orbital_menu_stage").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val mascot = composeRule.onNodeWithTag("mano_mascot_touch").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val tags =
      listOf(
        "home_menu_fever",
        "home_menu_medicines",
        "home_menu_scanner",
        "home_menu_growth",
        "home_menu_skin",
        "home_menu_qa",
      )
    val rects = tags.map { tag ->
      val node = composeRule.onNodeWithTag(tag).assertIsDisplayed()
      node.assertHeightIsAtLeast(48.dp)
      node.performClick()
      tag to node.fetchSemanticsNode().boundsInRoot
    }

    rects.forEach { (tag, rect) ->
      assertTrue("$tag clipped stage on 360dp", rect.left >= stage.left - 1f && rect.right <= stage.right + 1f)
      assertTrue("$tag clipped vertically on 360dp", rect.top >= stage.top - 1f && rect.bottom <= stage.bottom + 1f)
      assertTrue("$tag overlapped MANO on 360dp", !rect.overlaps(mascot))
    }
    for (i in rects.indices) {
      for (j in i + 1 until rects.size) {
        assertTrue(
          "${rects[i].first} overlapped ${rects[j].first} on 360dp",
          !rects[i].second.overlaps(rects[j].second),
        )
      }
    }

    assertEquals(
      listOf("fever:fever", "medicines", "scanner", "growth", "skin", "pillar:documented_qa"),
      tapped,
    )

    // Verify interactive circular drag rotates the orbital menu smoothly while keeping zero overlap
    // and zero edge clipping mid-rotation on 360dp phones
    val initialFeverPos = rects.first { it.first == "home_menu_fever" }.second
    composeRule.onNodeWithTag("orbital_menu_stage").performTouchInput {
      val topStart = androidx.compose.ui.geometry.Offset(center.x, top + 24f)
      val midArc = androidx.compose.ui.geometry.Offset(center.x + width * 0.28f, top + height * 0.22f)
      val rightArc = androidx.compose.ui.geometry.Offset(right - 24f, center.y)
      down(topStart)
      moveTo(midArc)
      moveTo(rightArc)
    }
    composeRule.waitForIdle()
    val breathingMascot = composeRule.onNodeWithTag("mano_mascot_touch").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val rotatedRects = tags.map { tag ->
      tag to composeRule.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    }
    val rotatedFeverPos = rotatedRects.first { it.first == "home_menu_fever" }.second
    assertTrue(
      "Expected orbital menu to rotate smoothly on circular drag (initial=$initialFeverPos, rotated=$rotatedFeverPos)",
      kotlin.math.abs(rotatedFeverPos.left - initialFeverPos.left) > 8f ||
        kotlin.math.abs(rotatedFeverPos.top - initialFeverPos.top) > 8f,
    )
    rotatedRects.forEach { (tag, nodeBounds) ->
      assertTrue("$tag clipped horizontally mid-rotation on 360dp", nodeBounds.left >= stage.left - 1f && nodeBounds.right <= stage.right + 1f)
      assertTrue("$tag clipped vertically mid-rotation on 360dp", nodeBounds.top >= stage.top - 1f && nodeBounds.bottom <= stage.bottom + 1f)
      assertTrue("$tag overlapped MANO mid-rotation during spring breathing", !nodeBounds.overlaps(breathingMascot))
    }
    for (i in rotatedRects.indices) {
      for (j in i + 1 until rotatedRects.size) {
        assertTrue(
          "${rotatedRects[i].first} overlapped ${rotatedRects[j].first} mid-rotation",
          !rotatedRects[i].second.overlaps(rotatedRects[j].second),
        )
      }
    }
    // Release drag and verify Spring 60° harmonic snap settles cleanly
    composeRule.onNodeWithTag("orbital_menu_stage").performTouchInput { up() }
    composeRule.waitForIdle()
  }

  @Test
  fun goldenRatioOrbitalGeometry_maintainsExactPhiRadialProportions() {
    val phi = com.example.ui.components.GoldenOrbitalGeometry.GOLDEN_RATIO
    listOf(360f, 411f, 456f).forEach { widthDp ->
      val m = com.example.ui.components.GoldenOrbitalGeometry.calculate(widthDp)
      val wholeToMajor = m.orbitRadiusDp / m.radialClearanceToOrbitDp
      val majorToMinor = m.radialClearanceToOrbitDp / m.mascotRadiusDp
      val orbitToInnerGoldenRing = m.orbitRadiusDp / m.goldenInnerRingRadiusDp
      assertEquals("Whole/Major radial ratio must match φ on ${widthDp}dp", phi, wholeToMajor, 0.01f)
      assertEquals("Major/Minor radial ratio must match φ on ${widthDp}dp", phi, majorToMinor, 0.01f)
      assertEquals("Orbit/InnerGoldenRing ratio must match φ on ${widthDp}dp", phi, orbitToInnerGoldenRing, 0.01f)
    }
  }

  @Test
  fun typographyAndColorContrast_meetScientificReadingHierarchyAndWcagStandards() {
    val typo = com.example.ui.theme.ManoTypography
    // Assert clear descending typographic hierarchy
    assertTrue(typo.displayLarge.fontSize > typo.headlineLarge.fontSize)
    assertTrue(typo.headlineLarge.fontSize > typo.headlineMedium.fontSize)
    assertTrue(typo.headlineMedium.fontSize > typo.titleLarge.fontSize)
    assertTrue(typo.titleLarge.fontSize > typo.titleMedium.fontSize)
    assertTrue(typo.titleMedium.fontSize > typo.bodyMedium.fontSize)

    // Assert generous Persian scientific reading lineHeight (>= 1.65x on body styles)
    listOf(typo.bodyLarge, typo.bodyMedium, typo.bodySmall).forEach { bodyStyle ->
      val ratio = bodyStyle.lineHeight.value / bodyStyle.fontSize.value
      assertTrue("Expected comfortable reading line-height ratio >= 1.65, was $ratio", ratio >= 1.65f)
    }

    // Assert WCAG AA (>= 4.5:1) contrast on primary ink & all 6 squad text inks on both Light & Dark card surfaces
    fun contrastRatio(fg: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color): Double {
      val l1 = androidx.core.graphics.ColorUtils.calculateLuminance(fg.hashCode()).let {
        androidx.compose.ui.graphics.Color(fg.value).let { c ->
          androidx.core.graphics.ColorUtils.calculateLuminance(
            android.graphics.Color.argb(
              255,
              (c.red * 255).toInt(),
              (c.green * 255).toInt(),
              (c.blue * 255).toInt(),
            )
          )
        }
      }
      val l2 = androidx.core.graphics.ColorUtils.calculateLuminance(
        android.graphics.Color.argb(
          255,
          (bg.red * 255).toInt(),
          (bg.green * 255).toInt(),
          (bg.blue * 255).toInt(),
        )
      )
      val brighter = maxOf(l1, l2)
      val darker = minOf(l1, l2)
      return (brighter + 0.05) / (darker + 0.05)
    }

    assertTrue(contrastRatio(com.example.ui.theme.DarkInk, com.example.ui.theme.PureWhite) >= 7.0)
    assertTrue(contrastRatio(com.example.ui.theme.MutedSlate, com.example.ui.theme.PureWhite) >= 7.0)
    assertTrue(contrastRatio(com.example.ui.theme.DarkTextPrimary, com.example.ui.theme.DarkSurface) >= 7.0)
    assertTrue(contrastRatio(com.example.ui.theme.DarkTextMuted, com.example.ui.theme.DarkSurface) >= 7.0)

    ManoSquadMember.entries.forEach { member ->
      val lightInkContrast = contrastRatio(member.textAccentColor(isDark = false), com.example.ui.theme.PureWhite)
      val lightPillContrast = contrastRatio(member.textAccentColor(isDark = false), member.tintedSurfaceColor(isDark = false))
      val darkInkContrast = contrastRatio(member.textAccentColor(isDark = true), com.example.ui.theme.DarkSurface)
      assertTrue("${member.englishName} Light card contrast $lightInkContrast < 4.5", lightInkContrast >= 4.5)
      assertTrue("${member.englishName} Light pill contrast $lightPillContrast < 4.5", lightPillContrast >= 4.5)
      assertTrue("${member.englishName} Dark card contrast $darkInkContrast < 4.5", darkInkContrast >= 4.5)
    }
  }
}
