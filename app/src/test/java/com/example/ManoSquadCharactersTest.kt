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
  fun homeScreen_orbitalOnlyStage_routesAllSixSpecializedPillarsAndCoreSections() {
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
            onNavigateEcosystemPillar = { pillar -> navigatedRoutes.add("pillar:${pillar.id}") },
          )
        }
      }
    }

    composeRule.onNodeWithTag("home_hero_card").assertIsDisplayed()
    composeRule.onNodeWithTag("mano_mascot_touch").assertIsDisplayed()
    composeRule.onNodeWithTag("orbital_toggle_spin").performClick()
    composeRule.waitForIdle()

    // Step through inner orbital ring: fever (0) -> medicines (1) -> mano_safety (2) -> mano_connect (3) -> documented_qa (4) -> product_comparison (5)
    composeRule.onNodeWithTag("orbital_focus_cta").performClick()
    val expectedInner =
      listOf(
        "medicines",
        "pillar:mano_safety",
        "pillar:mano_connect",
        "pillar:documented_qa",
        "pillar:product_comparison",
      )
    expectedInner.forEach { _ ->
      composeRule.onNodeWithTag("orbital_rotate_next").performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("orbital_focus_cta").performClick()
    }

    // Switch to outer orbital ring and verify mano_followup & mano_pharmacy pillars
    composeRule.onNodeWithTag("orbital_ring_outer").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("orbital_rotate_next").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("orbital_focus_cta").performClick()
    composeRule.onNodeWithTag("orbital_rotate_next").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("orbital_focus_cta").performClick()

    assertEquals(
      listOf(
        "symptoms:fever",
        "medicines",
        "pillar:mano_safety",
        "pillar:mano_connect",
        "pillar:documented_qa",
        "pillar:product_comparison",
        "pillar:mano_followup",
        "pillar:mano_pharmacy",
      ),
      navigatedRoutes,
    )
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
