package com.example.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

val VazirmatnClinicalFontFamily = FontFamily(
  Font(R.font.vazirmatn, FontWeight.Normal),
  Font(R.font.vazirmatn_medium, FontWeight.Medium),
  Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
  Font(R.font.vazirmatn_bold, FontWeight.Bold),
  Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

object VazirmatnClinicalStyles {
  object Headings {
    val displayHero = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 28.sp,
      lineHeight = 40.sp,
    )
    val displayPage = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 25.sp,
      lineHeight = 36.sp,
    )
    val h1 = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 23.sp,
      lineHeight = 33.sp,
    )
    val h2 = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 22.sp,
      lineHeight = 32.sp,
    )
    val h3 = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      lineHeight = 29.sp,
    )
    val h4 = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      lineHeight = 27.sp,
    )
    val topBarTitle = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 19.sp,
      lineHeight = 27.sp,
    )
    val cardTitle = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 17.sp,
      lineHeight = 25.sp,
    )
    val compactTitle = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 15.sp,
      lineHeight = 22.sp,
    )
    val sectionTitle = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      lineHeight = 26.sp,
    )
    val alertTitle = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp,
      lineHeight = 24.sp,
    )
  }

  object Body {
    val readingLarge = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 28.sp,
    )
    val readingPrimary = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 26.sp,
    )
    val readingStandard = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 24.sp,
    )
    val supporting = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 13.sp,
      lineHeight = 22.sp,
    )
  }

  object Captions {
    val overlineKicker = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 12.sp,
      lineHeight = 19.sp,
    )
    val captionPrimary = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 12.5.sp,
      lineHeight = 21.5.sp,
    )
    val captionMedium = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 19.sp,
    )
    val captionMicro = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 17.sp,
    )
    val navMicro = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 11.sp,
      lineHeight = 16.sp,
    )
  }

  object Controls {
    val buttonPrimary = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp,
      lineHeight = 20.sp,
    )
    val chipLabel = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 12.5.sp,
      lineHeight = 18.sp,
    )
  }

  object ClinicalMetrics {
    val dosageHero = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 26.sp,
      lineHeight = 32.sp,
      fontFeatureSettings = "tnum",
    )
    val metricMedium = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      lineHeight = 24.sp,
      fontFeatureSettings = "tnum",
    )
    val latinMonograph = TextStyle(
      fontFamily = VazirmatnClinicalFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.2.sp,
    )
  }
}

@Immutable
data class ManoSpacing(
  val xxs: Dp = 4.dp,
  val xs: Dp = 6.dp,
  val sm: Dp = 8.dp,
  val md: Dp = 12.dp,
  val lg: Dp = 16.dp,
  val xl: Dp = 24.dp,
  val screenHorizontal: Dp = 16.dp,
  val screenBottomClearance: Dp = 88.dp,
  val sectionGap: Dp = 14.dp,
  val cardPadding: Dp = 16.dp,
  val cardPaddingCompact: Dp = 12.dp,
  val cardItemGap: Dp = 10.dp,
  val inlineGap: Dp = 8.dp,
  val textPairGap: Dp = 4.dp,
  val chipGap: Dp = 8.dp,
) {
  val screenContentPadding: PaddingValues
    get() = PaddingValues(
      start = screenHorizontal,
      top = 12.dp,
      end = screenHorizontal,
      bottom = screenBottomClearance,
    )
}

val DefaultManoSpacing = ManoSpacing()
val LocalManoSpacing = staticCompositionLocalOf { DefaultManoSpacing }
