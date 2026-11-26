package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val LocalManoDarkTheme = staticCompositionLocalOf { false }

@Immutable
data class ManoColorFamily(
  val accent: Color,
  val accentInk: Color,
  val container: Color,
  val border: Color,
)

@Immutable
data class ManoMascotColors(
  val champagneHighlight: Color,
  val champagneCore: Color,
  val champagneDeep: Color,
  val seafoamLight: Color,
  val seafoamCore: Color,
  val seafoamDeep: Color,
  val leafLight: Color,
  val leafMid: Color,
  val leafDark: Color,
  val skyMintGlow: Color,
  val roseBlush: Color,
  val cautionEye: Color,
  val cautionBadge: Color,
  val skyHalo: Color,
  val shadowTint: Color,
  val porcelainLight: Color,
  val porcelainMid: Color,
  val porcelainWarm: Color,
  val porcelainFootMid: Color,
  val porcelainShade: Color,
  val porcelainEdge: Color,
  val porcelainEarShade: Color,
  val porcelainFootEdge: Color,
  val visorLight: Color,
  val visorMid: Color,
  val visorDeep: Color,
  val eyePupilDark: Color,
)

@Immutable
data class ManoSyringeColors(
  val liquidStart: Color,
  val liquidEnd: Color,
  val barrelStroke: Color,
  val barrelFill: Color,
  val plunger: Color,
  val tick: Color,
  val labNozzle: Color,
  val labStopper: Color,
  val labRod: Color,
  val labThumb: Color,
  val labBarrelStroke: Color,
  val labTick: Color,
  val thermoMarker: Color,
)

@Immutable
data class ManoDuoPalette(
  val backdrop: Color,
  val border: Color,
  val primary: Color,
  val secondary: Color,
  val highlight: Color,
  val surfaceFill: Color,
)

@Immutable
data class ManoIllustrationColors(
  val mascot: ManoMascotColors,
  val syringe: ManoSyringeColors,
  val orbHaloGradient: List<Color>,
  val duoHeaderCare: ManoDuoPalette,
  val duoTabSupplements: ManoDuoPalette,
  val duoTabHygiene: ManoDuoPalette,
  val duoVitaminD: ManoDuoPalette,
  val duoIronDrops: ManoDuoPalette,
  val duoProbiotic: ManoDuoPalette,
  val duoZincCream: ManoDuoPalette,
  val duoGentleWash: ManoDuoPalette,
  val duoOralCare: ManoDuoPalette,
)

@Immutable
data class ManoSemanticColors(
  val isDark: Boolean,
  val background: Color,
  val card: Color,
  val elevated: Color,
  val track: Color,
  val border: Color,
  val divider: Color,
  val ink: Color,
  val muted: Color,
  val onPrimary: Color,
  val heroStart: Color,
  val heroEnd: Color,
  val heroBannerTint: Color,
  val streamLoadingTint: Color,
  val cream: ManoColorFamily,
  val sage: ManoColorFamily,
  val cyan: ManoColorFamily,
  val sky: ManoColorFamily,
  val coral: ManoColorFamily,
  val gold: ManoColorFamily,
  val lavender: ManoColorFamily,
  val illustration: ManoIllustrationColors,
)

private val SharedMascotColors =
  ManoMascotColors(
    champagneHighlight = MascotChampagneHighlight,
    champagneCore = MascotChampagneCore,
    champagneDeep = MascotChampagneDeep,
    seafoamLight = MascotSeafoamLight,
    seafoamCore = MascotSeafoamCore,
    seafoamDeep = MascotSeafoamDeep,
    leafLight = MascotLeafLight,
    leafMid = MascotLeafMid,
    leafDark = MascotLeafDark,
    skyMintGlow = MascotSkyMintGlow,
    roseBlush = MascotRoseBlush,
    cautionEye = MascotCautionEye,
    cautionBadge = MascotCautionBadge,
    skyHalo = MascotSkyHalo,
    shadowTint = MascotShadowTint,
    porcelainLight = PureWhite,
    porcelainMid = MascotPorcelainMid,
    porcelainWarm = MascotPorcelainWarm,
    porcelainFootMid = MascotPorcelainFootMid,
    porcelainShade = MascotPorcelainShade,
    porcelainEdge = MascotPorcelainEdge,
    porcelainEarShade = MascotPorcelainEarShade,
    porcelainFootEdge = MascotPorcelainFootEdge,
    visorLight = MascotVisorLight,
    visorMid = MascotVisorMid,
    visorDeep = MascotVisorDeep,
    eyePupilDark = MascotEyePupilDark,
  )

val LightManoSemanticColors =
  ManoSemanticColors(
    isDark = false,
    background = WarmIvory,
    card = PureWhite,
    elevated = SoftSage,
    track = WarmIvory,
    border = WarmSand,
    divider = WarmSand,
    ink = DarkInk,
    muted = MutedSlate,
    onPrimary = PureWhite,
    heroStart = WarmIvory,
    heroEnd = OrbitCyan.copy(alpha = 0.28f),
    heroBannerTint = AquaSurface,
    streamLoadingTint = SoftLavender,
    cream =
      ManoColorFamily(
        accent = AccentGold,
        accentInk = DarkInk,
        container = WarmIvory,
        border = WarmSand,
      ),
    sage =
      ManoColorFamily(
        accent = DeepForest,
        accentInk = DeepForest,
        container = SoftSage,
        border = KidLime.copy(alpha = 0.45f),
      ),
    cyan =
      ManoColorFamily(
        accent = DeepForest,
        accentInk = DeepForest,
        container = OrbitCyan.copy(alpha = 0.34f),
        border = OrbitCyan.copy(alpha = 0.72f),
      ),
    sky =
      ManoColorFamily(
        accent = SkyCare,
        accentInk = SkyCareInk,
        container = SkySurface,
        border = SkyCare.copy(alpha = 0.24f),
      ),
    coral =
      ManoColorFamily(
        accent = CoralCare,
        accentInk = CoralCareInk,
        container = CoralSurface,
        border = CoralCare.copy(alpha = 0.24f),
      ),
    gold =
      ManoColorFamily(
        accent = CautionAmber,
        accentInk = OrangeCareInk,
        container = CautionSurface,
        border = DuoSuppTabBorderLight,
      ),
    lavender =
      ManoColorFamily(
        accent = PlumCare,
        accentInk = PlumCareInk,
        container = SoftLavender,
        border = PlumCare.copy(alpha = 0.24f),
      ),
    illustration =
      ManoIllustrationColors(
        mascot = SharedMascotColors,
        syringe =
          ManoSyringeColors(
            liquidStart = KidLime,
            liquidEnd = DeepTeal,
            barrelStroke = SyringeBarrelStrokeLight,
            barrelFill = PureWhite,
            plunger = DeepTeal,
            tick = DarkInk.copy(alpha = 0.78f),
            labNozzle = LabSyringeNozzleTint,
            labStopper = LabSyringeStopperDark,
            labRod = LabSyringeRodTint,
            labThumb = LabSyringeThumbLight,
            labBarrelStroke = LabSyringeBarrelLight,
            labTick = LabSyringeTickLight,
            thermoMarker = LabThermoMarkerLight,
          ),
        orbHaloGradient = listOf(PureWhite, OrbHaloCream, OrbHaloSky),
        duoHeaderCare =
          ManoDuoPalette(
            backdrop = AquaSurface,
            border = HairlineBorder,
            primary = DeepTeal,
            secondary = KidLime,
            highlight = AccentGold,
            surfaceFill = PureWhite,
          ),
        duoTabSupplements =
          ManoDuoPalette(
            backdrop = CautionSurface,
            border = DuoSuppTabBorderLight,
            primary = CautionAmber,
            secondary = AccentGold,
            highlight = DuoSuppTabHighlightLight,
            surfaceFill = PureWhite,
          ),
        duoTabHygiene =
          ManoDuoPalette(
            backdrop = AquaSurface,
            border = HairlineBorder,
            primary = DeepTeal,
            secondary = SkyCare,
            highlight = LightMint,
            surfaceFill = PureWhite,
          ),
        duoVitaminD =
          ManoDuoPalette(
            backdrop = DuoVitDBackdropLight,
            border = DuoVitDBorderLight,
            primary = DuoVitDPrimaryLight,
            secondary = DuoVitDSecondaryLight,
            highlight = DuoVitDHighlightLight,
            surfaceFill = PureWhite,
          ),
        duoIronDrops =
          ManoDuoPalette(
            backdrop = DuoIronBackdropLight,
            border = DuoIronBorderLight,
            primary = DuoIronPrimaryLight,
            secondary = DuoIronSecondaryLight,
            highlight = DuoIronHighlightLight,
            surfaceFill = PureWhite,
          ),
        duoProbiotic =
          ManoDuoPalette(
            backdrop = DuoProbioticBackdropLight,
            border = DuoProbioticBorderLight,
            primary = DuoProbioticPrimaryLight,
            secondary = DuoProbioticSecondaryLight,
            highlight = DuoProbioticHighlightLight,
            surfaceFill = PureWhite,
          ),
        duoZincCream =
          ManoDuoPalette(
            backdrop = DuoZincBackdropLight,
            border = DuoZincBorderLight,
            primary = DuoZincPrimaryLight,
            secondary = DuoZincSecondaryLight,
            highlight = DuoZincHighlightLight,
            surfaceFill = PureWhite,
          ),
        duoGentleWash =
          ManoDuoPalette(
            backdrop = DuoWashBackdropLight,
            border = DuoWashBorderLight,
            primary = DuoWashPrimaryLight,
            secondary = DuoWashSecondaryLight,
            highlight = DuoWashHighlightLight,
            surfaceFill = PureWhite,
          ),
        duoOralCare =
          ManoDuoPalette(
            backdrop = DuoOralBackdropLight,
            border = DuoOralBorderLight,
            primary = DuoOralPrimaryLight,
            secondary = DuoOralSecondaryLight,
            highlight = DuoOralHighlightLight,
            surfaceFill = PureWhite,
          ),
      ),
  )

val DarkManoSemanticColors =
  ManoSemanticColors(
    isDark = true,
    background = DarkBackground,
    card = DarkSurface,
    elevated = DarkSurfaceHigh,
    track = DarkBackground,
    border = DarkBorder,
    divider = DarkBorder,
    ink = DarkTextPrimary,
    muted = DarkTextMuted,
    onPrimary = DarkOnPrimary,
    heroStart = DarkSurface,
    heroEnd = DarkSurfaceHigh,
    heroBannerTint = DarkPrimary.copy(alpha = 0.14f),
    streamLoadingTint = DarkPlum.copy(alpha = 0.14f),
    cream =
      ManoColorFamily(
        accent = DarkGold,
        accentInk = DarkTextPrimary,
        container = DarkSurface,
        border = DarkBorder,
      ),
    sage =
      ManoColorFamily(
        accent = DarkSage,
        accentInk = DarkSage,
        container = DarkSurfaceHigh,
        border = DarkBorder,
      ),
    cyan =
      ManoColorFamily(
        accent = DarkPrimary,
        accentInk = DarkPrimary,
        container = DarkSurfaceHigh,
        border = DarkBorder,
      ),
    sky =
      ManoColorFamily(
        accent = DarkSky,
        accentInk = DarkSky,
        container = DuoProbioticBackdropDark,
        border = DuoProbioticBorderDark,
      ),
    coral =
      ManoColorFamily(
        accent = DarkCoral,
        accentInk = DarkCoral,
        container = DarkErrorContainer,
        border = DuoIronBorderDark,
      ),
    gold =
      ManoColorFamily(
        accent = DarkGold,
        accentInk = DarkGold,
        container = DuoSuppTabBackdropDark,
        border = DuoSuppTabBorderDark,
      ),
    lavender =
      ManoColorFamily(
        accent = DarkPlum,
        accentInk = DarkPlum,
        container = DuoOralBackdropDark,
        border = DuoOralBorderDark,
      ),
    illustration =
      ManoIllustrationColors(
        mascot = SharedMascotColors,
        syringe =
          ManoSyringeColors(
            liquidStart = TealGlow,
            liquidEnd = SeaGreen,
            barrelStroke = SyringeBarrelStrokeDark,
            barrelFill = DarkSurfaceHigh,
            plunger = DarkSage,
            tick = SyringeTickDark,
            labNozzle = LabSyringeNozzleTint,
            labStopper = LabSyringeStopperDark,
            labRod = LabSyringeRodTint,
            labThumb = LabSyringeThumbDark,
            labBarrelStroke = LabSyringeBarrelDark,
            labTick = LabSyringeTickDark,
            thermoMarker = LabSyringeTickDark,
          ),
        orbHaloGradient = listOf(PureWhite, OrbHaloCream, OrbHaloSky),
        duoHeaderCare =
          ManoDuoPalette(
            backdrop = DarkSurfaceHigh,
            border = DarkBorder,
            primary = DarkPrimary,
            secondary = DarkSage,
            highlight = DarkGold,
            surfaceFill = DarkSurface,
          ),
        duoTabSupplements =
          ManoDuoPalette(
            backdrop = DuoSuppTabBackdropDark,
            border = DuoSuppTabBorderDark,
            primary = DarkGold,
            secondary = DarkPrimary,
            highlight = DuoSuppTabHighlightDark,
            surfaceFill = DuoSuppTabSurfaceDark,
          ),
        duoTabHygiene =
          ManoDuoPalette(
            backdrop = DarkSurfaceHigh,
            border = DarkBorder,
            primary = DarkPrimary,
            secondary = DarkSage,
            highlight = SkySurface,
            surfaceFill = DarkSurface,
          ),
        duoVitaminD =
          ManoDuoPalette(
            backdrop = DuoVitDBackdropDark,
            border = DuoVitDBorderDark,
            primary = DuoVitDPrimaryDark,
            secondary = DuoVitDSecondaryDark,
            highlight = DuoVitDHighlightDark,
            surfaceFill = DuoVitDSurfaceDark,
          ),
        duoIronDrops =
          ManoDuoPalette(
            backdrop = DuoIronBackdropDark,
            border = DuoIronBorderDark,
            primary = DuoIronPrimaryDark,
            secondary = DuoIronSecondaryDark,
            highlight = DuoIronHighlightDark,
            surfaceFill = DuoIronSurfaceDark,
          ),
        duoProbiotic =
          ManoDuoPalette(
            backdrop = DuoProbioticBackdropDark,
            border = DuoProbioticBorderDark,
            primary = DuoProbioticPrimaryDark,
            secondary = DuoProbioticSecondaryDark,
            highlight = DuoProbioticHighlightDark,
            surfaceFill = DuoProbioticSurfaceDark,
          ),
        duoZincCream =
          ManoDuoPalette(
            backdrop = DuoZincBackdropDark,
            border = DuoZincBorderDark,
            primary = DuoZincPrimaryDark,
            secondary = DuoZincSecondaryDark,
            highlight = DuoZincHighlightDark,
            surfaceFill = DuoZincSurfaceDark,
          ),
        duoGentleWash =
          ManoDuoPalette(
            backdrop = DuoWashBackdropDark,
            border = DuoWashBorderDark,
            primary = DuoWashPrimaryDark,
            secondary = DuoWashSecondaryDark,
            highlight = DuoWashHighlightDark,
            surfaceFill = DuoWashSurfaceDark,
          ),
        duoOralCare =
          ManoDuoPalette(
            backdrop = DuoOralBackdropDark,
            border = DuoOralBorderDark,
            primary = DuoOralPrimaryDark,
            secondary = DuoOralSecondaryDark,
            highlight = DuoOralHighlightDark,
            surfaceFill = DuoOralSurfaceDark,
          ),
      ),
  )

val LocalManoColors = staticCompositionLocalOf { LightManoSemanticColors }

object ManoTheme {
  val colors: ManoSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalManoColors.current

  val typeScale: ManoTypeScale
    @Composable
    @ReadOnlyComposable
    get() = LocalManoTypeScale.current

  val spacing: ManoSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalManoSpacing.current
}

private val ManoLightColorScheme =
  lightColorScheme(
    primary = DeepTeal,
    onPrimary = PureWhite,
    primaryContainer = LightMint,
    onPrimaryContainer = DarkInk,
    secondary = AccentGold,
    onSecondary = DarkInk,
    secondaryContainer = GoldSurface,
    onSecondaryContainer = DarkInk,
    tertiary = KidLime,
    onTertiary = DarkInk,
    tertiaryContainer = SkySurface,
    onTertiaryContainer = DarkInk,
    background = WarmIvory,
    onBackground = DarkInk,
    surface = PureWhite,
    onSurface = DarkInk,
    surfaceVariant = CleanWhite,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = SoftMintCard,
    surfaceContainer = CleanWhite,
    surfaceContainerHigh = CleanWhite,
    surfaceTint = DeepTeal,
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
    secondary = DarkGold,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkSage,
    onTertiary = DarkOnPrimary,
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
  val semanticColors = if (darkTheme) DarkManoSemanticColors else LightManoSemanticColors

  CompositionLocalProvider(
    LocalLayoutDirection provides LayoutDirection.Rtl,
    LocalManoDarkTheme provides darkTheme,
    LocalManoColors provides semanticColors,
    LocalManoTypeScale provides DefaultManoTypeScale,
    LocalManoSpacing provides DefaultManoSpacing,
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
fun manoCardSurface(): Color = ManoTheme.colors.card

@Composable
fun manoElevatedSurface(): Color = if (LocalManoDarkTheme.current) DarkSurfaceHigh else CleanWhite

@Composable
fun manoBorderColor(): Color = ManoTheme.colors.border

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
  if (LocalManoDarkTheme.current) DarkBorder else ChartGridLight

@Composable
fun manoChartAxisHex(): Int =
  ManoTheme.colors.muted.toArgb()

@Composable
fun manoChartRailColor(): Color =
  if (LocalManoDarkTheme.current) DarkSurfaceVariant else CleanWhite


