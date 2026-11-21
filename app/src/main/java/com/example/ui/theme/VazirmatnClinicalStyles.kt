package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Dedicated hierarchical typography style sheet built on authentic multi-weight
 * Vazirmatn static fonts (Regular 400 -> ExtraBold 800) for MANO Kid's
 * Clinical-Minimal theme.
 *
 * Core typographic principles enforced across every style:
 * - Zero artificial letter-spacing (0.sp) on all Persian cursive styles so ligatures stay crisp
 * - Optical vertical centering (`includeFontPadding = false` + `LineHeightStyle.Alignment.Center`)
 * - Airy clinical line-heights (1.68x–1.77x on Body) for effortless low-fatigue reading
 * - Tabular numerals (`tnum`) on Captions, Controls, and ClinicalMetrics for jitter-free alignment
 */
val VazirmatnClinicalFontFamily = FontFamily(
  Font(R.font.vazirmatn, FontWeight.Normal),
  Font(R.font.vazirmatn_medium, FontWeight.Medium),
  Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
  Font(R.font.vazirmatn_bold, FontWeight.Bold),
  Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

private val ClinicalMinimalLineHeightStyle =
  LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
  )

@Suppress("DEPRECATION")
private val ClinicalMinimalPlatformStyle = PlatformTextStyle(includeFontPadding = false)

private fun clinicalTextStyle(
  weight: FontWeight,
  sizeSp: Float,
  lineHeightSp: Float,
  letterSpacingSp: Float = 0f,
  tabularNumerals: Boolean = false,
): TextStyle =
  TextStyle(
    fontFamily = VazirmatnClinicalFontFamily,
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = lineHeightSp.sp,
    letterSpacing = letterSpacingSp.sp,
    fontFeatureSettings = if (tabularNumerals) "tnum" else null,
    platformStyle = ClinicalMinimalPlatformStyle,
    lineHeightStyle = ClinicalMinimalLineHeightStyle,
  )

@Immutable
object VazirmatnClinicalStyles {

  /** Headings (تیترها): ExtraBold 800 -> Bold 700 -> SemiBold 600 */
  @Immutable
  object Headings {
    val displayHero: TextStyle = clinicalTextStyle(FontWeight.ExtraBold, sizeSp = 34f, lineHeightSp = 46f)
    val displayPage: TextStyle = clinicalTextStyle(FontWeight.ExtraBold, sizeSp = 28f, lineHeightSp = 38f)
    val h1: TextStyle = clinicalTextStyle(FontWeight.Bold, sizeSp = 24f, lineHeightSp = 34f)
    val h2: TextStyle = clinicalTextStyle(FontWeight.Bold, sizeSp = 22f, lineHeightSp = 32f)
    val h3: TextStyle = clinicalTextStyle(FontWeight.Bold, sizeSp = 19.5f, lineHeightSp = 29f)
    val h4: TextStyle = clinicalTextStyle(FontWeight.SemiBold, sizeSp = 18f, lineHeightSp = 27f)
    val topBarTitle: TextStyle = clinicalTextStyle(FontWeight.Bold, sizeSp = 17.5f, lineHeightSp = 26f)
    val sectionTitle: TextStyle = clinicalTextStyle(FontWeight.Bold, sizeSp = 17f, lineHeightSp = 25.5f)
    val cardTitle: TextStyle = clinicalTextStyle(FontWeight.SemiBold, sizeSp = 15.5f, lineHeightSp = 24f)
    val compactTitle: TextStyle = clinicalTextStyle(FontWeight.SemiBold, sizeSp = 14f, lineHeightSp = 21.5f)
    val alertTitle: TextStyle = clinicalTextStyle(FontWeight.ExtraBold, sizeSp = 15.5f, lineHeightSp = 23.5f)
  }

  /** Body (متون بدنه): Regular 400 -> Medium 500 with airy 1.68x–1.77x clinical line-height */
  @Immutable
  object Body {
    val readingLarge: TextStyle = clinicalTextStyle(FontWeight.Normal, sizeSp = 15.5f, lineHeightSp = 27f)
    val readingPrimary: TextStyle = clinicalTextStyle(FontWeight.Normal, sizeSp = 15f, lineHeightSp = 26.5f)
    val readingStandard: TextStyle = clinicalTextStyle(FontWeight.Normal, sizeSp = 14f, lineHeightSp = 24.5f)
    val emphasis: TextStyle = clinicalTextStyle(FontWeight.Medium, sizeSp = 14f, lineHeightSp = 24f)
    val supporting: TextStyle = clinicalTextStyle(FontWeight.Normal, sizeSp = 13.5f, lineHeightSp = 22.5f)
  }

  /** Captions (توضیحات، متادیتا، برچسب‌های بالاسری و پاورقی): Regular 400 -> Medium 500 -> SemiBold 600 */
  @Immutable
  object Captions {
    val captionPrimary: TextStyle =
      clinicalTextStyle(FontWeight.Normal, sizeSp = 12.5f, lineHeightSp = 21f, tabularNumerals = true)
    val captionMedium: TextStyle =
      clinicalTextStyle(FontWeight.Medium, sizeSp = 12f, lineHeightSp = 18f, tabularNumerals = true)
    val captionMicro: TextStyle =
      clinicalTextStyle(FontWeight.Normal, sizeSp = 11.5f, lineHeightSp = 18f, tabularNumerals = true)
    val overlineKicker: TextStyle =
      clinicalTextStyle(FontWeight.SemiBold, sizeSp = 11.5f, lineHeightSp = 17f, tabularNumerals = true)
    val navMicro: TextStyle =
      clinicalTextStyle(FontWeight.Medium, sizeSp = 11f, lineHeightSp = 16f, tabularNumerals = true)
  }

  /** Interactive Controls (دکمه‌ها و چیپ‌ها): SemiBold 600 -> Medium 500 */
  @Immutable
  object Controls {
    val buttonPrimary: TextStyle =
      clinicalTextStyle(FontWeight.SemiBold, sizeSp = 13.5f, lineHeightSp = 20f, tabularNumerals = true)
    val chipLabel: TextStyle =
      clinicalTextStyle(FontWeight.Medium, sizeSp = 12f, lineHeightSp = 18f, tabularNumerals = true)
  }

  /** Clinical Metrics & Latin Monographs (اعداد بالینی و نام‌های ژنریک لاتین) */
  @Immutable
  object ClinicalMetrics {
    val dosageHero: TextStyle =
      clinicalTextStyle(FontWeight.ExtraBold, sizeSp = 26f, lineHeightSp = 34f, tabularNumerals = true)
    val metricMedium: TextStyle =
      clinicalTextStyle(FontWeight.Bold, sizeSp = 18f, lineHeightSp = 25f, tabularNumerals = true)
    val latinMonograph: TextStyle =
      clinicalTextStyle(
        weight = FontWeight.Medium,
        sizeSp = 12.5f,
        lineHeightSp = 18f,
        letterSpacingSp = 0.25f,
        tabularNumerals = true,
      )
  }
}
