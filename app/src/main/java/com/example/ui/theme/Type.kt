package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

// Canonical Vazirmatn multi-weight family delegated from VazirmatnClinicalStyles.kt
val VazirmatnFontFamily: FontFamily = VazirmatnClinicalFontFamily

// Retained auxiliary families for backward compatibility where explicitly referenced
val IbmPlexArabicFamily = FontFamily(
  Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal),
  Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
  Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
  Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
  Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.ExtraBold),
)

val DisplayLalezarFamily = FontFamily(
  Font(R.font.lalezar, FontWeight.Normal),
  Font(R.font.lalezar, FontWeight.Bold),
)

/**
 * Semantic hierarchical Type Scale backed by [VazirmatnClinicalStyles]
 * (Headings, Body, Captions, Controls, and ClinicalMetrics).
 */
@Immutable
data class ManoTypeScale(
  val heroDisplay: TextStyle,
  val screenTitle: TextStyle,
  val sectionHeader: TextStyle,
  val cardTitle: TextStyle,
  val cardSubtitle: TextStyle,
  val clinicalBody: TextStyle,
  val clinicalBodyCompact: TextStyle,
  val clinicalAlertTitle: TextStyle,
  val clinicalNumericHero: TextStyle,
  val clinicalNumericMedium: TextStyle,
  val sectionKicker: TextStyle,
  val actionLabel: TextStyle,
  val chipLabel: TextStyle,
  val microCaption: TextStyle,
  val latinTechnical: TextStyle,
)

val DefaultManoTypeScale =
  ManoTypeScale(
    heroDisplay = VazirmatnClinicalStyles.Headings.displayPage,
    screenTitle = VazirmatnClinicalStyles.Headings.h2,
    sectionHeader = VazirmatnClinicalStyles.Headings.sectionTitle,
    cardTitle = VazirmatnClinicalStyles.Headings.cardTitle,
    cardSubtitle = VazirmatnClinicalStyles.Body.supporting,
    clinicalBody = VazirmatnClinicalStyles.Body.readingPrimary,
    clinicalBodyCompact = VazirmatnClinicalStyles.Body.readingStandard,
    clinicalAlertTitle = VazirmatnClinicalStyles.Headings.alertTitle,
    clinicalNumericHero = VazirmatnClinicalStyles.ClinicalMetrics.dosageHero,
    clinicalNumericMedium = VazirmatnClinicalStyles.ClinicalMetrics.metricMedium,
    sectionKicker = VazirmatnClinicalStyles.Captions.overlineKicker,
    actionLabel = VazirmatnClinicalStyles.Controls.buttonPrimary,
    chipLabel = VazirmatnClinicalStyles.Controls.chipLabel,
    microCaption = VazirmatnClinicalStyles.Captions.captionMicro,
    latinTechnical = VazirmatnClinicalStyles.ClinicalMetrics.latinMonograph,
  )

val LocalManoTypeScale = staticCompositionLocalOf { DefaultManoTypeScale }

object ManoTheme {
  val typeScale: ManoTypeScale
    @Composable
    @ReadOnlyComposable
    get() = LocalManoTypeScale.current

  val spacing: ManoSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalManoSpacing.current
}

// Convenient top-level aliases backed by VazirmatnClinicalStyles
val ManoClinicalNumericLarge: TextStyle = VazirmatnClinicalStyles.ClinicalMetrics.dosageHero
val ManoClinicalNumericMedium: TextStyle = VazirmatnClinicalStyles.ClinicalMetrics.metricMedium
val ManoEditorialKicker: TextStyle = VazirmatnClinicalStyles.Captions.overlineKicker
val ManoLatinTechnicalStyle: TextStyle = VazirmatnClinicalStyles.ClinicalMetrics.latinMonograph

// Material 3 Typography wired directly to VazirmatnClinicalStyles as single source of truth
val ManoTypography =
  Typography(
    // Display & Headline tiers -> Headings
    displayLarge = VazirmatnClinicalStyles.Headings.displayHero,
    displayMedium = VazirmatnClinicalStyles.Headings.displayPage,
    displaySmall = VazirmatnClinicalStyles.Headings.h1,
    headlineLarge = VazirmatnClinicalStyles.Headings.h2,
    headlineMedium = VazirmatnClinicalStyles.Headings.h3,
    headlineSmall = VazirmatnClinicalStyles.Headings.h4,

    // Title tier -> Headings
    titleLarge = VazirmatnClinicalStyles.Headings.topBarTitle,
    titleMedium = VazirmatnClinicalStyles.Headings.cardTitle,
    titleSmall = VazirmatnClinicalStyles.Headings.compactTitle,

    // Body tier -> Body + Primary Caption
    bodyLarge = VazirmatnClinicalStyles.Body.readingLarge,
    bodyMedium = VazirmatnClinicalStyles.Body.readingStandard,
    bodySmall = VazirmatnClinicalStyles.Captions.captionPrimary,

    // Label tier -> Controls + Captions
    labelLarge = VazirmatnClinicalStyles.Controls.buttonPrimary,
    labelMedium = VazirmatnClinicalStyles.Captions.captionMedium,
    labelSmall = VazirmatnClinicalStyles.Captions.navMicro,
  )
