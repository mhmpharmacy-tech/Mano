package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

val IbmPlexArabicFamily = FontFamily(
  Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal),
  Font(R.font.ibm_plex_sans_arabic, FontWeight.Medium),
  Font(R.font.ibm_plex_sans_arabic, FontWeight.SemiBold),
  Font(R.font.ibm_plex_sans_arabic, FontWeight.Bold),
  Font(R.font.ibm_plex_sans_arabic, FontWeight.ExtraBold),
)

val DisplayLalezarFamily = FontFamily(
  Font(R.font.lalezar, FontWeight.Normal),
  Font(R.font.lalezar, FontWeight.Bold),
)

val VazirmatnFontFamily = FontFamily(
  Font(R.font.vazirmatn, FontWeight.Normal),
  Font(R.font.vazirmatn, FontWeight.Medium),
  Font(R.font.vazirmatn, FontWeight.SemiBold),
  Font(R.font.vazirmatn, FontWeight.Bold),
  Font(R.font.vazirmatn, FontWeight.ExtraBold),
)

private val ClinicalLineHeightStyle =
  LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
  )

// International-grade Persian + Latin typography system:
// - IBM Plex Sans Arabic drives display & section headlines with crisp geometric clarity
// - Vazirmatn drives clinical reading copy with zero artificial letter-spacing (preserving cursive ligatures)
//   and generous 1.69x–1.72x line heights for effortless reading in glare or dim night light
val ManoTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 46.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    displayMedium =
      TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    displaySmall =
      TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    headlineLarge =
      TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 35.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 31.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.5.sp,
        lineHeight = 29.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    titleLarge =
      TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.5.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    titleMedium =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.5.sp,
        lineHeight = 25.5.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    titleSmall =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.5.sp,
        lineHeight = 22.5.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 27.5.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    bodySmall =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    labelLarge =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    labelMedium =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 18.5.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    labelSmall =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 16.5.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
  )
