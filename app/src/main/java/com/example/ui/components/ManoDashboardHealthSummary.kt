package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.ui.theme.AquaBreeze
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.DeepTealGlass
import com.example.ui.theme.GlassCyanRim
import com.example.ui.theme.LightMint
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.MinimalTeal
import com.example.ui.theme.MinimalTealDark
import com.example.ui.theme.MintFrost
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftMintCard
import com.example.ui.theme.SoftMintHalo
import kotlin.math.roundToInt

/**
 * Snapshot of live pediatric health summary metrics rendered as circular progress rings
 * in the main dashboard view.
 */
@Immutable
data class DashboardHealthSummaryState(
  val vaccineAdministeredCount: Int = 0,
  val vaccineTotalCoreCount: Int = 0,
  val vaccineCaptionFa: String = "",
  val milestonesCheckedCount: Int = 0,
  val milestonesTotalCount: Int = 0,
  val ageBandLabelFa: String = "همه سنین",
  val medSafetyCheckedCount: Int = 0,
  val medSafetyTotalCount: Int = 8,
  val medSafetyAllCriticalReady: Boolean = false,
  val latestTempCelsius: Float? = null,
) {
  val vaccineProgress: Float
    get() =
      if (vaccineTotalCoreCount > 0) {
        (vaccineAdministeredCount.toFloat() / vaccineTotalCoreCount.toFloat()).coerceIn(0f, 1f)
      } else {
        0f
      }

  val milestoneProgress: Float
    get() =
      if (milestonesTotalCount > 0) {
        (milestonesCheckedCount.toFloat() / milestonesTotalCount.toFloat()).coerceIn(0f, 1f)
      } else {
        0f
      }

  val medSafetyProgress: Float
    get() =
      if (medSafetyTotalCount > 0) {
        (medSafetyCheckedCount.toFloat() / medSafetyTotalCount.toFloat()).coerceIn(0f, 1f)
      } else {
        0f
      }

  val overallReadinessPercent: Int
    get() = (((vaccineProgress + milestoneProgress + medSafetyProgress) / 3f) * 100f).roundToInt().coerceIn(0, 100)
}

/**
 * Main dashboard frosted-glass Health Summary Status component featuring three soft pastel
 * turquoise circular progress rings for Vaccination, Developmental Milestones, and Medication Safety.
 */
@Composable
fun ManoDashboardHealthSummaryCard(
  state: DashboardHealthSummaryState,
  onOpenVaccines: () -> Unit,
  onOpenGrowth: () -> Unit,
  onOpenMedicationSafety: () -> Unit,
  onOpenFeverTimeline: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  val spacing = ManoTheme.spacing

  val cardBrush =
    if (isDark) {
      Brush.linearGradient(
        colors = listOf(
          DarkSurface,
          DeepTealGlass.copy(alpha = 0.42f),
          DarkSurfaceHigh,
        ),
      )
    } else {
      Brush.linearGradient(
        colors = listOf(
          PureWhite,
          SoftMintCard,
          AquaSurface.copy(alpha = 0.72f),
        ),
      )
    }

  val cardBorderColor =
    if (isDark) MinimalTealDark.copy(alpha = 0.26f) else MinimalTeal.copy(alpha = 0.22f)
  val titleInk = if (isDark) DarkTextPrimary else DarkInk
  val mutedInk = if (isDark) DarkTextMuted else MutedSlate
  val turquoiseAccent = if (isDark) MinimalTealDark else DeepTeal
  val pillSurface = if (isDark) DeepTealGlass.copy(alpha = 0.65f) else LightMint.copy(alpha = 0.85f)
  val pillBorder = if (isDark) GlassCyanRim.copy(alpha = 0.28f) else MinimalTeal.copy(alpha = 0.22f)

  Surface(
    color = Color.Transparent,
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, cardBorderColor),
    shadowElevation = if (isDark) 2.dp else 3.dp,
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm)
        .manoFloatingGlass(intensity = 0.48f, enableSheen = true)
        .testTag("dashboard_health_summary_card"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(cardBrush)
          .padding(spacing.md),
      verticalArrangement = Arrangement.spacedBy(spacing.cardItemGap),
    ) {
      // Header Row: Title + Live Status Pill (Latest Temp or Overall Readiness %)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
          Box(
            modifier =
              Modifier
                .width(3.5.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(50))
                .background(turquoiseAccent.copy(alpha = 0.85f)),
          )
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "خلاصه وضعیت سلامت کودک",
              style = ManoTheme.typeScale.sectionHeader,
              color = titleInk,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "پایش زنده مراقبت • ${state.ageBandLabelFa}",
              style = ManoTheme.typeScale.microCaption,
              color = mutedInk,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        if (state.latestTempCelsius != null) {
          Surface(
            onClick = onOpenFeverTimeline,
            color = pillSurface,
            shape = RoundedCornerShape(50),
            border = BorderStroke(0.9.dp, pillBorder),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("dashboard_health_temp_pill"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Thermostat,
                contentDescription = null,
                tint = turquoiseAccent,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = FeverDoseTimelineEngine.formatTempC(state.latestTempCelsius),
                style = ManoTheme.typeScale.chipLabel,
                fontWeight = FontWeight.Bold,
                color = turquoiseAccent,
              )
            }
          }
        } else {
          Surface(
            color = pillSurface,
            shape = RoundedCornerShape(50),
            border = BorderStroke(0.9.dp, pillBorder),
            modifier = Modifier.testTag("dashboard_health_readiness_pill"),
          ) {
            Text(
              text = "آمادگی ${toPersianDigits(state.overallReadinessPercent)}٪",
              style = ManoTheme.typeScale.chipLabel,
              fontWeight = FontWeight.Bold,
              color = turquoiseAccent,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }
      }

      // Three Soft Pastel Turquoise Circular Progress Ring Tiles
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.Top,
      ) {
        val vaccineTileBg =
          if (isDark) DarkSurfaceHigh.copy(alpha = 0.78f) else SoftMintHalo.copy(alpha = 0.56f)
        val growthTileBg =
          if (isDark) DarkSurfaceHigh.copy(alpha = 0.78f) else MintFrost.copy(alpha = 0.52f)
        val safetyTileBg =
          if (isDark) DarkSurfaceHigh.copy(alpha = 0.78f) else AquaBreeze.copy(alpha = 0.44f)

        DashboardHealthRingTile(
          labelFa = "واکسیناسیون",
          captionFa =
            if (state.vaccineTotalCoreCount > 0) {
              "${toPersianDigits(state.vaccineAdministeredCount)} از ${toPersianDigits(state.vaccineTotalCoreCount)} نوبت"
            } else {
              "تقویم واکسن"
            },
          progress = state.vaccineProgress,
          valueText = "${toPersianDigits((state.vaccineProgress * 100f).roundToInt())}٪",
          subLabel = "نوبت",
          icon = Icons.Rounded.Vaccines,
          tileBackground = vaccineTileBg,
          tone =
            if (state.vaccineProgress >= 1f && state.vaccineTotalCoreCount > 0) {
              ManoHealthRingTone.EMERALD
            } else {
              ManoHealthRingTone.PASTEL_TURQUOISE
            },
          testTag = "dashboard_health_ring_vaccines",
          onClick = onOpenVaccines,
          modifier = Modifier.weight(1f),
        )

        DashboardHealthRingTile(
          labelFa = "رشد و مهارت",
          captionFa =
            if (state.milestonesTotalCount > 0) {
              "${toPersianDigits(state.milestonesCheckedCount)} از ${toPersianDigits(state.milestonesTotalCount)} مهارت"
            } else {
              "پایش رشد"
            },
          progress = state.milestoneProgress,
          valueText =
            if (state.milestonesTotalCount > 0) {
              "${toPersianDigits(state.milestonesCheckedCount)}/${toPersianDigits(state.milestonesTotalCount)}"
            } else {
              "۰٪"
            },
          subLabel = "رشد",
          icon = Icons.Rounded.ChildCare,
          tileBackground = growthTileBg,
          tone =
            if (state.milestoneProgress >= 1f && state.milestonesTotalCount > 0) {
              ManoHealthRingTone.EMERALD
            } else {
              ManoHealthRingTone.PASTEL_TURQUOISE
            },
          testTag = "dashboard_health_ring_growth",
          onClick = onOpenGrowth,
          modifier = Modifier.weight(1f),
        )

        DashboardHealthRingTile(
          labelFa = "ایمنی دارو",
          captionFa =
            if (state.medSafetyAllCriticalReady) {
              "گام‌های حیاتی آماده"
            } else {
              "${toPersianDigits(state.medSafetyCheckedCount)} از ${toPersianDigits(state.medSafetyTotalCount)} گام"
            },
          progress = state.medSafetyProgress,
          valueText = "${toPersianDigits(state.medSafetyCheckedCount)}/${toPersianDigits(state.medSafetyTotalCount)}",
          subLabel = if (state.medSafetyAllCriticalReady) "آماده" else "ایمنی",
          icon = Icons.Rounded.Shield,
          tileBackground = safetyTileBg,
          tone =
            if (state.medSafetyAllCriticalReady) {
              ManoHealthRingTone.EMERALD
            } else {
              ManoHealthRingTone.PASTEL_TURQUOISE
            },
          testTag = "dashboard_health_ring_safety",
          onClick = onOpenMedicationSafety,
          modifier = Modifier.weight(1f),
        )
      }

      if (state.vaccineCaptionFa.isNotBlank()) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(if (isDark) DarkSurface.copy(alpha = 0.65f) else PureWhite.copy(alpha = 0.72f))
              .border(
                width = 0.8.dp,
                color = if (isDark) DarkBorder else MinimalTeal.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
              )
              .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Vaccines,
            contentDescription = null,
            tint = turquoiseAccent,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = "نوبت بعدی واکسن: ${state.vaccineCaptionFa}",
            style = ManoTheme.typeScale.microCaption,
            color = mutedInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun DashboardHealthRingTile(
  labelFa: String,
  captionFa: String,
  progress: Float,
  valueText: String,
  subLabel: String,
  icon: ImageVector,
  tileBackground: Color,
  tone: ManoHealthRingTone,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  val titleInk = if (isDark) DarkTextPrimary else DarkInk
  val mutedInk = if (isDark) DarkTextMuted else MutedSlate
  val tileBorder = if (isDark) MinimalTealDark.copy(alpha = 0.20f) else MinimalTeal.copy(alpha = 0.18f)
  val iconTint = if (isDark) GlassCyanRim else DeepTeal

  Surface(
    onClick = onClick,
    color = tileBackground,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(0.85.dp, tileBorder),
    modifier = modifier.minimumInteractiveComponentSize().testTag(testTag),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      ManoHealthProgressRing(
        progress = progress,
        size = ManoCircularProgressSize.LARGE,
        diameter = 60.dp,
        strokeWidth = 4.6.dp,
        tone = tone,
        valueText = valueText,
        subLabel = subLabel,
        contentDescriptionFa = "$labelFa: $captionFa",
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(13.dp),
        )
        Text(
          text = labelFa,
          style = ManoTheme.typeScale.chipLabel,
          fontWeight = FontWeight.SemiBold,
          color = titleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
      }
      Text(
        text = captionFa,
        style = ManoTheme.typeScale.microCaption,
        color = mutedInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
    }
  }
}

private fun toPersianDigits(value: Int): String {
  val digits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
  return value.toString().map { ch ->
    if (ch in '0'..'9') digits[ch - '0'] else ch
  }.joinToString("")
}
