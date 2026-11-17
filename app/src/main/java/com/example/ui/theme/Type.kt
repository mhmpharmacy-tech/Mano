package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

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

// Scientific Persian-first typography hierarchy:
// - Lalezar reserved strictly for prominent display/hero banners
// - Vazirmatn tuned across headings & body with 1.65x–1.75x line heights for effortless reading in glare or dim night light
val ManoTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = DisplayLalezarFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 48.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    displayMedium =
      TextStyle(
        fontFamily = DisplayLalezarFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 44.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    displaySmall =
      TextStyle(
        fontFamily = DisplayLalezarFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 40.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    headlineLarge =
      TextStyle(
        fontFamily = DisplayLalezarFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 25.sp,
        lineHeight = 36.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 33.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 30.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    titleLarge =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 19.sp,
        lineHeight = 29.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    titleMedium =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.5.sp,
        lineHeight = 26.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    titleSmall =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.5.sp,
        lineHeight = 23.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 25.5.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    bodySmall =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    labelLarge =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    labelMedium =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 19.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
    labelSmall =
      TextStyle(
        fontFamily = VazirmatnFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 17.sp,
        lineHeightStyle = ClinicalLineHeightStyle,
      ),
  )
