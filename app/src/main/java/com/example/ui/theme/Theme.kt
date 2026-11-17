package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val LocalManoDarkTheme = staticCompositionLocalOf { false }

private val ManoLightColorScheme =
  lightColorScheme(
    primary = DeepTeal,
    onPrimary = PureWhite,
    primaryContainer = LightMint,
    onPrimaryContainer = DarkInk,
    secondary = SeaGreen,
    onSecondary = PureWhite,
    secondaryContainer = AquaSurface,
    onSecondaryContainer = DarkInk,
    tertiary = AccentGold,
    onTertiary = PureWhite,
    tertiaryContainer = GoldSurface,
    onTertiaryContainer = DarkInk,
    background = PureWhite,
    onBackground = DarkInk,
    surface = PureWhite,
    onSurface = DarkInk,
    surfaceVariant = AquaSurface,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = PureWhite,
    surfaceContainer = Color(0xFFF7FAFB),
    surfaceContainerHigh = Color(0xFFF0F6F7),
    surfaceTint = SeaGreen,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = DangerCrimson,
    onError = PureWhite,
    errorContainer = DangerSurface,
    onErrorContainer = DangerCrimson,
  )

private val ManoDarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkTextPrimary,
    secondary = TealGlow,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkGold,
    onTertiary = DarkInk,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceHigh,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceTint = DarkPrimary,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = DarkCoral,
    onError = DarkBackground,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
  )

private val ManoShapes =
  Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
  )

@Composable
fun ManoKidsTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) ManoDarkColorScheme else ManoLightColorScheme

  CompositionLocalProvider(
    LocalLayoutDirection provides LayoutDirection.Rtl,
    LocalManoDarkTheme provides darkTheme,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = ManoTypography,
      shapes = ManoShapes,
      content = content,
    )
  }
}

@Composable
fun manoCardSurface(): Color = if (LocalManoDarkTheme.current) DarkSurface else PureWhite

@Composable
fun manoElevatedSurface(): Color = if (LocalManoDarkTheme.current) DarkSurfaceHigh else Color(0xFFF7FBFC)

@Composable
fun manoBorderColor(): Color = if (LocalManoDarkTheme.current) DarkBorder else HairlineBorder

@Composable
fun manoSemanticAccent(lightAccent: Color, darkAccent: Color): Color =
  if (LocalManoDarkTheme.current) darkAccent else lightAccent

@Composable
fun manoTintedSurface(
  lightSurface: Color,
  darkAccent: Color,
  lightAlpha: Float = 1f,
  darkAlpha: Float = 0.16f,
): Color = if (LocalManoDarkTheme.current) darkAccent.copy(alpha = darkAlpha) else lightSurface.copy(alpha = lightAlpha)

@Composable
fun manoChartGridColor(): Color =
  if (LocalManoDarkTheme.current) DarkBorder else Color(0xFFD5E4E7)

@Composable
fun manoChartAxisHex(): Int =
  if (LocalManoDarkTheme.current) 0xFFBBD7D2.toInt() else 0xFF334E57.toInt()

@Composable
fun manoChartRailColor(): Color =
  if (LocalManoDarkTheme.current) DarkSurfaceVariant else Color(0xFFF1F7F8)

