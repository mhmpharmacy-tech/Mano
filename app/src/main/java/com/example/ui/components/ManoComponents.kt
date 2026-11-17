package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.annotation.DrawableRes
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AgeBand
import com.example.domain.model.Article
import com.example.domain.model.MascotPose
import com.example.domain.model.MedicationMonograph
import com.example.domain.model.MedicationSafetyCategory
import com.example.domain.model.ReviewStatus
import com.example.domain.model.SourceReference
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AquaCare
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.CoralCare
import com.example.ui.theme.CoralCareInk
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkPlum
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoSurface
import com.example.ui.theme.JadeCareInk
import com.example.ui.theme.LightMint
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.MangoCare
import com.example.ui.theme.PaleWarmSand
import com.example.ui.theme.PlumCare
import com.example.ui.theme.PlumCareInk
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SageCare
import com.example.ui.theme.ShadowSlate
import com.example.ui.theme.SkyCare
import com.example.ui.theme.SkyCareInk
import com.example.ui.theme.StrawberryCare
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.TealGlow
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface

enum class AlertSeverity {
  DANGER,
  CAUTION,
  INFO,
  SUCCESS,
}

enum class TopicAccentRole {
  TEAL,
  CORAL,
  SKY,
  SAGE,
  PLUM,
  GOLD,
  MANGO,
  AQUA,
  STRAWBERRY,
  DANGER,
}

@Composable
fun TopicAccentRole.resolveAccentColor(): Color {
  val isDark = LocalManoDarkTheme.current
  return when (this) {
    TopicAccentRole.TEAL -> MaterialTheme.colorScheme.primary
    TopicAccentRole.CORAL -> if (isDark) DarkCoral else CoralCareInk
    TopicAccentRole.SKY -> if (isDark) DarkSky else SkyCareInk
    TopicAccentRole.SAGE -> if (isDark) DarkSage else JadeCareInk
    TopicAccentRole.PLUM -> if (isDark) DarkPlum else PlumCareInk
    TopicAccentRole.GOLD -> if (isDark) DarkGold else CautionAmber
    TopicAccentRole.MANGO -> if (isDark) DarkGold else MangoCare
    TopicAccentRole.AQUA -> if (isDark) DarkSky else AquaCare
    TopicAccentRole.STRAWBERRY -> if (isDark) DarkCoral else StrawberryCare
    TopicAccentRole.DANGER -> MaterialTheme.colorScheme.error
  }
}

@Composable
fun ManoAtmosphericBackdrop(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val isDark = LocalManoDarkTheme.current
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .drawWithCache {
          val baseColor = if (isDark) DarkBackground else PureWhite
          val topHaloCenterColor =
            if (isDark) TealGlow.copy(alpha = 0.16f) else LightMint.copy(alpha = 0.34f)
          val topSecondaryHalo =
            if (isDark) DarkSky.copy(alpha = 0.10f) else AquaSurface.copy(alpha = 0.28f)

          onDrawBehind {
            drawRect(color = baseColor)
            // Very soft, airy pastel halo strictly at the top edge fading rapidly into pure white
            drawCircle(
              brush = Brush.radialGradient(listOf(topHaloCenterColor, Color.Transparent)),
              center = Offset(size.width * 0.50f, size.height * 0.02f),
              radius = size.width * 0.85f,
            )
            drawCircle(
              brush = Brush.radialGradient(listOf(topSecondaryHalo, Color.Transparent)),
              center = Offset(size.width * 0.15f, size.height * 0.06f),
              radius = size.width * 0.55f,
            )
          }
        },
    content = content,
  )
}

@Composable
fun ManoIconMedallion(
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
  iconKind: PediatricIconKind? = null,
  containerSize: Dp = 48.dp,
  iconSize: Dp = 24.dp,
  cornerRadius: Dp = 15.dp,
) {
  val isDark = LocalManoDarkTheme.current
  val shape = RoundedCornerShape(cornerRadius)
  val surfaceHighlight = if (isDark) accentColor.copy(alpha = 0.34f) else PureWhite.copy(alpha = 0.94f)
  val borderBrush =
    Brush.linearGradient(
      colors =
        listOf(
          if (isDark) accentColor.copy(alpha = 0.52f) else PureWhite.copy(alpha = 0.96f),
          accentColor.copy(alpha = 0.42f),
        )
    )

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .size(containerSize)
        .shadow(
          elevation = if (isDark) 12.dp else 10.dp,
          shape = shape,
          ambientColor = accentColor.copy(alpha = 0.48f),
          spotColor = accentColor.copy(alpha = 0.44f),
        )
        .clip(shape)
        .background(
          Brush.linearGradient(
            colors =
              listOf(
                surfaceHighlight,
                accentColor.copy(alpha = if (isDark) 0.34f else 0.28f),
                accentColor.copy(alpha = if (isDark) 0.18f else 0.14f),
              )
          )
        )
        .border(1.3.dp, borderBrush, shape)
        .drawBehind {
          // Soft inner radial spotlight for stronger icon separation
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    PureWhite.copy(alpha = if (isDark) 0.22f else 0.78f),
                    Color.Transparent,
                  )
              ),
            center = center,
            radius = size.minDimension * 0.44f,
          )
          // Soft specular gloss arc at top-left for 3D tactile glass polish
          drawArc(
            color = PureWhite.copy(alpha = if (isDark) 0.28f else 0.90f),
            startAngle = 200f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(size.width * 0.12f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.74f, size.height * 0.40f),
            style = Stroke(width = 2.8f, cap = StrokeCap.Round),
          )
        },
  ) {
    if (iconKind != null) {
      Mano3DIconArtwork(kind = iconKind, size = iconSize)
    } else {
      // Subtle ambient depth shadow-glyph
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor.copy(alpha = 0.34f),
        modifier = Modifier.size(iconSize).offset(x = 1.2.dp, y = 1.8.dp),
      )
      // Crisp primary foreground icon
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(iconSize),
      )
    }
  }
}

@Composable
fun LtrText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.onSurface,
) {
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Text(
      text = text,
      modifier = modifier,
      style = MaterialTheme.typography.bodySmall,
      color = color,
    )
  }
}

@Composable
fun ManoPoseAvatar(
  pose: MascotPose,
  size: Dp = 64.dp,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  val shape = RoundedCornerShape(18.dp)
  val accentTint =
    when (pose) {
      MascotPose.CAUTION -> manoSemanticAccent(CautionAmber, DarkGold)
      MascotPose.SAVED -> manoSemanticAccent(SuccessEmerald, DarkSage)
      else -> MaterialTheme.colorScheme.primary
    }

  Surface(
    color = if (isDark) Color(0xFFFAFBFB) else PureWhite,
    shape = shape,
    border = BorderStroke(1.4.dp, accentTint.copy(alpha = if (isDark) 0.48f else 0.28f)),
    shadowElevation = 3.dp,
    modifier = modifier.size(size),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.radialGradient(
              colors =
                listOf(
                  PureWhite,
                  accentTint.copy(alpha = 0.08f),
                )
            )
          )
          .padding(3.dp),
    ) {
      Image(
        painter = painterResource(id = pose.drawableRes),
        contentDescription = "شخصیت مانو در حالت ${pose.labelFa}",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
      )
    }
  }
}

/**
 * Framed showcase for wide illustrations/photos (16:9, 3:2, 4:3) matching the
 * image's natural aspect ratio so characters and labels stay cleanly inside bounds.
 */
@Composable
fun ManoBannerFrame(
  @DrawableRes resId: Int,
  contentDescription: String,
  aspectRatio: Float = 16f / 9f,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 18.dp,
) {
  val shape = RoundedCornerShape(cornerRadius)
  val borderColor = manoBorderColor()
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = shape,
    border = BorderStroke(1.2.dp, borderColor),
    shadowElevation = 2.dp,
    modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio),
  ) {
    Image(
      painter = painterResource(id = resId),
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      alignment = Alignment.Center,
      modifier = Modifier.fillMaxSize().clip(shape),
    )
  }
}

/**
 * Framed showcase for vertical or square studio MANO illustrations (`mascot_growth`,
 * `mascot_medicine`, `mascot_skin`) using ContentScale.Fit inside an aspect-matched porcelain frame.
 */
@Composable
fun ManoFramedIllustration(
  @DrawableRes resId: Int,
  contentDescription: String,
  width: Dp = 100.dp,
  aspectRatio: Float = 0.90f,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(18.dp)
  Surface(
    color = PureWhite,
    shape = shape,
    border = BorderStroke(1.3.dp, accentColor.copy(alpha = 0.28f)),
    shadowElevation = 3.dp,
    modifier = modifier.width(width).aspectRatio(aspectRatio),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.radialGradient(
              colors = listOf(PureWhite, accentColor.copy(alpha = 0.08f))
            )
          )
          .padding(5.dp),
    ) {
      Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
      )
    }
  }
}

@Composable
fun SourceBadge(
  reviewStatus: ReviewStatus,
  isDemo: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val bg =
    when {
      isDemo -> manoTintedSurface(PaleWarmSand, DarkGold)
      reviewStatus == ReviewStatus.APPROVED -> manoTintedSurface(SuccessSurface, DarkSage)
      else -> manoTintedSurface(CautionSurface, DarkGold)
    }
  val fg =
    when {
      isDemo -> manoSemanticAccent(CautionAmber, DarkGold)
      reviewStatus == ReviewStatus.APPROVED -> manoSemanticAccent(SuccessEmerald, DarkSage)
      else -> manoSemanticAccent(CautionAmber, DarkGold)
    }
  val label = if (isDemo) "نیازمند بررسی" else reviewStatus.labelFa

  Surface(
    color = bg,
    shape = RoundedCornerShape(50),
    border = BorderStroke(1.dp, fg.copy(alpha = 0.35f)),
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
      Icon(
        imageVector = if (reviewStatus == ReviewStatus.APPROVED && !isDemo) Icons.Rounded.VerifiedUser else Icons.Rounded.Schedule,
        contentDescription = null,
        tint = fg,
        modifier = Modifier.size(14.dp),
      )
      Spacer(Modifier.width(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        maxLines = 2,
      )
    }
  }
}

@Composable
fun SeverityAlert(
  severity: AlertSeverity,
  title: String,
  body: String,
  modifier: Modifier = Modifier,
  showCautionMascot: Boolean = false,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  val isDark = LocalManoDarkTheme.current
  val (bg, fg, icon) =
    when (severity) {
      AlertSeverity.DANGER ->
        Triple(
          manoTintedSurface(DangerSurface, DarkCoral),
          manoSemanticAccent(DangerCrimson, DarkCoral),
          Icons.Rounded.ErrorOutline,
        )
      AlertSeverity.CAUTION ->
        Triple(
          manoTintedSurface(CautionSurface, DarkGold),
          manoSemanticAccent(CautionAmber, DarkGold),
          Icons.Rounded.WarningAmber,
        )
      AlertSeverity.INFO ->
        Triple(
          manoTintedSurface(InfoSurface, DarkSky),
          manoSemanticAccent(InfoBlue, DarkSky),
          Icons.Rounded.Info,
        )
      AlertSeverity.SUCCESS ->
        Triple(
          manoTintedSurface(SuccessSurface, DarkSage),
          manoSemanticAccent(SuccessEmerald, DarkSage),
          Icons.Rounded.CheckCircle,
        )
    }
  val actionTextColor = if (isDark) DarkBackground else PureWhite

  Surface(
    color = bg,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.5.dp, fg.copy(alpha = 0.45f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (showCautionMascot && severity == AlertSeverity.DANGER) {
          // Strict clinical rule: never use cheerful celebratory pose on emergency warnings
          ManoPoseAvatar(pose = MascotPose.CAUTION, size = 54.dp)
        } else {
          Surface(
            color = fg.copy(alpha = 0.14f),
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
            }
          }
        }
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = fg,
          fontWeight = FontWeight.ExtraBold,
          modifier = Modifier.weight(1f),
        )
      }
      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (actionLabel != null && onAction != null) {
        Button(
          onClick = onAction,
          colors = ButtonDefaults.buttonColors(containerColor = fg, contentColor = actionTextColor),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("severity_alert_action"),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
fun AgeSelectorRow(
  selectedAgeBand: AgeBand?,
  onSelect: (AgeBand?) -> Unit,
  includeAllOption: Boolean = true,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (includeAllOption) {
      item {
        val selected = selectedAgeBand == null
        FilterChip(
          selected = selected,
          onClick = { onSelect(null) },
          label = { Text("همه سنین") },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("age_chip_all"),
        )
      }
    }
    items(AgeBand.entries) { band ->
      val selected = selectedAgeBand == band
      FilterChip(
        selected = selected,
        onClick = { onSelect(band) },
        label = { Text(band.labelFa) },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("age_chip_${band.id}"),
      )
    }
  }
}

@Composable
fun TopicCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  iconKind: PediatricIconKind? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "topic_card",
) {
  val isDark = LocalManoDarkTheme.current
  val shape = RoundedCornerShape(22.dp)
  val borderColor = if (isDark) accentColor.copy(alpha = 0.28f) else HairlineBorder
  val cardBg = if (isDark) DarkSurface else PureWhite
  Card(
    onClick = onClick,
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = BorderStroke(1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .shadow(
          elevation = 8.dp,
          shape = shape,
          ambientColor = if (isDark) accentColor.copy(alpha = 0.22f) else ShadowSlate.copy(alpha = 0.13f),
          spotColor = if (isDark) accentColor.copy(alpha = 0.20f) else ShadowSlate.copy(alpha = 0.11f),
        )
        .minimumInteractiveComponentSize()
        .testTag(testTag),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier =
            Modifier
              .width(3.5.dp)
              .height(34.dp)
              .clip(RoundedCornerShape(50))
              .background(accentColor)
        )
        ManoIconMedallion(
          icon = icon,
          accentColor = accentColor,
          iconKind = iconKind,
          containerSize = 50.dp,
          iconSize = 28.dp,
          cornerRadius = 16.dp,
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun SymptomChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedBg = manoTintedSurface(LightMint, MaterialTheme.colorScheme.primary, lightAlpha = 1f, darkAlpha = 0.20f)
  val bg by animateColorAsState(if (selected) selectedBg else manoCardSurface(), label = "chipBg")
  val border by animateColorAsState(
    if (selected) MaterialTheme.colorScheme.primary else manoBorderColor(),
    label = "chipBorder",
  )
  Surface(
    color = bg,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, border),
    modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).minimumInteractiveComponentSize(),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    )
  }
}

@Composable
fun ArticleCard(
  article: Article,
  isBookmarked: Boolean,
  onToggleBookmark: () -> Unit,
  onOpen: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(22.dp)
  val isDark = LocalManoDarkTheme.current
  val glowTint = MaterialTheme.colorScheme.primary
  val cardBg = if (isDark) DarkSurface else PureWhite
  Card(
    onClick = onOpen,
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = BorderStroke(1.dp, if (isDark) glowTint.copy(alpha = 0.24f) else HairlineBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 8.dp,
          shape = shape,
          ambientColor = if (isDark) glowTint.copy(alpha = 0.18f) else ShadowSlate.copy(alpha = 0.12f),
          spotColor = if (isDark) glowTint.copy(alpha = 0.16f) else ShadowSlate.copy(alpha = 0.10f),
        )
        .testTag("article_card_${article.slug}"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = manoTintedSurface(AquaSurface, MaterialTheme.colorScheme.primary, darkAlpha = 0.16f),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = article.category.labelFa,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          SourceBadge(reviewStatus = article.review.status)
        }
        IconButton(
          onClick = onToggleBookmark,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("bookmark_${article.slug}"),
        ) {
          Icon(
            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            contentDescription = if (isBookmarked) "حذف نشان مقاله" else "نشان‌گذاری مقاله",
            tint = if (isBookmarked) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Text(
        text = article.titleFa,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = article.shortAnswerFa,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = article.sources.firstOrNull()?.publisher ?: "منبع مرجع",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("مطالعه کامل", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
          Spacer(Modifier.width(4.dp))
          Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

@Composable
fun MedicineCard(
  medication: MedicationMonograph,
  onOpen: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(22.dp)
  val isDark = LocalManoDarkTheme.current
  val categoryColor =
    when (medication.category) {
      MedicationSafetyCategory.FEVER_PAIN -> MaterialTheme.colorScheme.primary
      MedicationSafetyCategory.HYDRATION_GI -> manoSemanticAccent(AquaCare, DarkSky)
      MedicationSafetyCategory.RESPIRATORY_ALLERGY -> manoSemanticAccent(SkyCare, DarkSky)
      MedicationSafetyCategory.ANTIBIOTICS -> manoSemanticAccent(PlumCare, DarkPlum)
      MedicationSafetyCategory.VITAMINS_SUPPLEMENTS -> manoSemanticAccent(MangoCare, DarkGold)
      MedicationSafetyCategory.TOPICAL_SKIN -> manoSemanticAccent(SageCare, DarkSage)
    }
  val accentTint = if (medication.isHighRiskWarning) manoSemanticAccent(DangerCrimson, DarkCoral) else categoryColor
  val cardBg = if (isDark) DarkSurface else PureWhite
  val warningBg =
    if (medication.isHighRiskWarning) manoTintedSurface(DangerSurface, DarkCoral)
    else manoTintedSurface(CautionSurface, DarkGold)
  val warningColor =
    if (medication.isHighRiskWarning) manoSemanticAccent(DangerCrimson, DarkCoral)
    else manoSemanticAccent(CautionAmber, DarkGold)
  val safetyPreview = medication.keySafetySummaryFa.ifBlank { medication.dosageFormAlertFa }

  Card(
    onClick = onOpen,
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (medication.isHighRiskWarning) DangerCrimson.copy(alpha = 0.25f)
          else if (isDark) accentTint.copy(alpha = 0.24f)
          else HairlineBorder,
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 8.dp,
          shape = shape,
          ambientColor = if (isDark) accentTint.copy(alpha = 0.18f) else ShadowSlate.copy(alpha = 0.12f),
          spotColor = if (isDark) accentTint.copy(alpha = 0.16f) else ShadowSlate.copy(alpha = 0.10f),
        )
        .testTag("medicine_card_${medication.slug}"),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier =
              Modifier
                .width(3.5.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(50))
                .background(accentTint)
          )
          ManoIconMedallion(
            icon = Icons.Rounded.MedicalServices,
            accentColor = accentTint,
            iconKind =
              if (medication.isHighRiskWarning) PediatricIconKind.URGENT
              else PediatricIconKind.MEDICINE,
            containerSize = 44.dp,
            iconSize = 26.dp,
            cornerRadius = 14.dp,
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = medication.nameFa,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            LtrText(text = medication.genericNameEn, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
          color = accentTint.copy(alpha = 0.10f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(0.8.dp, accentTint.copy(alpha = 0.28f)),
        ) {
          Text(
            text = medication.category.labelFa,
            style = MaterialTheme.typography.labelSmall,
            color = accentTint,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
          )
        }
      }

      Text(text = medication.treatsFa, style = MaterialTheme.typography.bodyMedium)

      Surface(
        color = warningBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.8.dp, warningColor.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = warningColor,
            modifier = Modifier.size(18.dp).padding(top = 1.dp),
          )
          Text(
            text = safetyPreview,
            style = MaterialTheme.typography.bodySmall,
            color = warningColor,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        SourceBadge(reviewStatus = medication.review.status)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "نکات و هشدارها",
            style = MaterialTheme.typography.labelLarge,
            color = accentTint,
          )
          Spacer(Modifier.width(4.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = accentTint,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
fun ClinicalBulletList(
  items: List<String>,
  bulletColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
  textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    items.forEach { point ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
      ) {
        Box(
          modifier =
            Modifier
              .padding(top = 9.dp)
              .size(6.dp)
              .clip(CircleShape)
              .background(bulletColor)
        )
        Text(
          text = point,
          style = textStyle,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
fun ClinicalNote(
  title: String,
  items: List<String>,
  sources: List<SourceReference> = emptyList(),
  reviewStatus: ReviewStatus = ReviewStatus.APPROVED,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = manoTintedSurface(AquaSurface, MaterialTheme.colorScheme.primary, lightAlpha = 0.6f, darkAlpha = 0.14f)),
    border = BorderStroke(1.dp, manoBorderColor()),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        SourceBadge(reviewStatus = reviewStatus)
      }
      ClinicalBulletList(
        items = items,
        bulletColor = MaterialTheme.colorScheme.primary,
        textStyle = MaterialTheme.typography.bodyMedium,
      )
      if (sources.isNotEmpty()) {
        HorizontalDivider(color = manoBorderColor())
        sources.forEach { src ->
          Text(
            text = "منبع: ${src.publisher} — ${src.title}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          LtrText(text = src.url, color = manoSemanticAccent(InfoBlue, DarkSky))
        }
      }
    }
  }
}

@Composable
fun PharmacistCtaCard(
  title: String = "ارتباط با داروساز",
  subtitle: String = "اگر درباره غلظت دارو، نحوه اندازه‌گیری با سرنگ mL یا مراقبت حمایتی پرسشی دارید، پرسش خود را ثبت کنید.",
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(24.dp)
  val isDark = LocalManoDarkTheme.current
  val connectAccent = if (isDark) DarkPlum else PlumCare
  val connectTextAccent = if (isDark) DarkPlum else PlumCareInk
  val cardBg = if (isDark) DarkSurface else PureWhite
  Card(
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = BorderStroke(1.2.dp, connectAccent.copy(alpha = if (isDark) 0.36f else 0.24f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 9.dp,
          shape = shape,
          ambientColor = connectAccent.copy(alpha = if (isDark) 0.28f else 0.14f),
          spotColor = connectAccent.copy(alpha = if (isDark) 0.24f else 0.12f),
        ),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .drawBehind {
            drawCircle(
              brush = Brush.radialGradient(listOf(connectAccent.copy(alpha = if (isDark) 0.16f else 0.10f), Color.Transparent)),
              center = Offset(size.width * 0.16f, size.height * 0.18f),
              radius = size.width * 0.45f,
            )
          }
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(64.dp)
              .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = connectAccent.copy(alpha = if (isDark) 0.40f else 0.22f),
                spotColor = connectAccent.copy(alpha = if (isDark) 0.36f else 0.20f),
              )
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  colors =
                    listOf(
                      PureWhite.copy(alpha = if (isDark) 0.15f else 0.98f),
                      connectAccent.copy(alpha = if (isDark) 0.24f else 0.14f),
                    )
                )
              )
              .border(
                width = 1.4.dp,
                brush =
                  Brush.linearGradient(
                    colors =
                      listOf(
                        if (isDark) connectAccent.copy(alpha = 0.68f) else PureWhite.copy(alpha = 0.95f),
                        connectAccent.copy(alpha = 0.38f),
                      )
                  ),
                shape = CircleShape,
              )
              .drawBehind {
                drawArc(
                  color = PureWhite.copy(alpha = if (isDark) 0.26f else 0.85f),
                  startAngle = 205f,
                  sweepAngle = 110f,
                  useCenter = false,
                  topLeft = Offset(size.width * 0.12f, size.height * 0.08f),
                  size = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.44f),
                  style = Stroke(width = 2.4f, cap = StrokeCap.Round),
                )
              },
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_mano_connect),
            contentDescription = "شخصیت Mano Connect برای ارتباط با داروساز",
            modifier = Modifier.size(56.dp),
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = connectTextAccent)
          SourceBadge(reviewStatus = ReviewStatus.APPROVED)
        }
      }
      Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
      Button(
        onClick = onClick,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = connectAccent,
            contentColor = if (isDark) DarkBackground else PureWhite,
          ),
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("pharmacist_cta_button"),
      ) {
        Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("پرسش از داروساز")
      }
    }
  }
}

@Composable
fun ConsentBox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  title: String,
  description: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = manoTintedSurface(PaleWarmSand, DarkGold, lightAlpha = 0.6f, darkAlpha = 0.14f),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, manoSemanticAccent(AccentGold, DarkGold).copy(alpha = 0.5f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.Top,
      modifier =
        Modifier
          .clickable { onCheckedChange(!checked) }
          .padding(12.dp)
          .minimumInteractiveComponentSize()
          .testTag("consent_checkbox_row"),
    ) {
      Checkbox(checked = checked, onCheckedChange = onCheckedChange)
      Spacer(Modifier.width(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
fun ProgressTimeline(
  steps: List<String>,
  currentStepIndex: Int,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    steps.forEachIndexed { index, label ->
      val active = index <= currentStepIndex
      val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(color)
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun EmptyState(
  title: String,
  subtitle: String,
  pose: MascotPose = MascotPose.WAIT,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(24.dp).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ManoPoseAvatar(pose = pose, size = 84.dp)
      Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
      Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      if (actionLabel != null && onAction != null) {
        OutlinedButton(onClick = onAction, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(actionLabel)
        }
      }
    }
  }
}

data class RadialTopicItem(
  val id: String,
  val titleFa: String,
  val subtitleFa: String,
  val icon: ImageVector,
  val accentRole: TopicAccentRole = TopicAccentRole.TEAL,
  val iconKind: PediatricIconKind? = null,
)

/**
 * Optional creative navigation: rotatable semicircular/radial topic selector
 * with a conventional accessible clickable list/grid fallback and zero forced motion.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RadialTopicSelector(
  topics: List<RadialTopicItem>,
  onSelectTopic: (RadialTopicItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  var useRadialView by remember { mutableStateOf(false) }
  var wheelOffset by remember { mutableFloatStateOf(0f) }
  val animatedOffset by animateFloatAsState(
    targetValue = wheelOffset,
    animationSpec = spring(),
    label = "radialStep",
  )

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (useRadialView) "چرخ موضوعات (قابل چرخش با دکمه)" else "فهرست سریع موضوعات",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      TextButton(
        onClick = { useRadialView = !useRadialView },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("toggle_radial_view"),
      ) {
        Text(if (useRadialView) "نمایش فهرست استاندارد" else "نمای چرخ موضوعات")
      }
    }

    if (useRadialView) {
      val visibleCount = 4
      val startIndex = ((animatedOffset.toInt() % topics.size) + topics.size) % topics.size
      val displayed = (0 until visibleCount).map { idx -> topics[(startIndex + idx) % topics.size] }
      Surface(
        color = manoTintedSurface(AquaSurface, MaterialTheme.colorScheme.primary, lightAlpha = 0.72f, darkAlpha = 0.14f),
        shape = RoundedCornerShape(topStart = 96.dp, topEnd = 96.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        border = BorderStroke(1.dp, manoBorderColor()),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            OutlinedButton(
              onClick = { wheelOffset -= 1f },
              modifier = Modifier.minimumInteractiveComponentSize().semantics { contentDescription = "چرخش به موضوع قبلی" },
            ) {
              Text("قبلی")
            }
            OutlinedButton(
              onClick = { wheelOffset += 1f },
              modifier = Modifier.minimumInteractiveComponentSize().semantics { contentDescription = "چرخش به موضوع بعدی" },
            ) {
              Text("بعدی")
            }
          }
          FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            displayed.forEach { item ->
              TopicCard(
                title = item.titleFa,
                subtitle = item.subtitleFa,
                icon = item.icon,
                accentColor = item.accentRole.resolveAccentColor(),
                iconKind = item.iconKind,
                onClick = { onSelectTopic(item) },
                modifier = Modifier.weight(1f, fill = true).fillMaxWidth(0.47f),
                testTag = "radial_topic_${item.id}",
              )
            }
          }
        }
      }
    }

    // Always keep direct accessible grid tiles available when standard mode is active
    if (!useRadialView) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        topics.chunked(2).forEach { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            pair.forEach { item ->
              TopicCard(
                title = item.titleFa,
                subtitle = item.subtitleFa,
                icon = item.icon,
                accentColor = item.accentRole.resolveAccentColor(),
                iconKind = item.iconKind,
                onClick = { onSelectTopic(item) },
                modifier = Modifier.weight(1f),
                testTag = "concern_tile_${item.id}",
              )
            }
            if (pair.size == 1) {
              Spacer(Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
fun CoreNavigationCard(
  title: String,
  subtitle: String,
  badgeText: String,
  icon: ImageVector,
  accentColor: Color,
  containerColor: Color,
  iconContainerColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  iconKind: PediatricIconKind? = null,
) {
  val isDark = LocalManoDarkTheme.current
  val shape = RoundedCornerShape(24.dp)
  val borderColor = if (isDark) accentColor.copy(alpha = 0.30f) else HairlineBorder
  Card(
    onClick = onClick,
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 9.dp,
          shape = shape,
          ambientColor = if (isDark) accentColor.copy(alpha = 0.24f) else ShadowSlate.copy(alpha = 0.13f),
          spotColor = if (isDark) accentColor.copy(alpha = 0.20f) else ShadowSlate.copy(alpha = 0.11f),
        )
        .minimumInteractiveComponentSize()
        .testTag(testTag),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .drawBehind {
            drawCircle(
              brush = Brush.radialGradient(listOf(accentColor.copy(alpha = if (isDark) 0.15f else 0.08f), Color.Transparent)),
              center = Offset(size.width * 0.90f, size.height * 0.50f),
              radius = size.height * 1.15f,
            )
          }
          .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Thin refined colored accent strip beside the 3D icon medallion
      Box(
        modifier =
          Modifier
            .width(4.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(accentColor)
      )
      ManoIconMedallion(
        icon = icon,
        accentColor = accentColor,
        iconKind = iconKind,
        containerSize = 56.dp,
        iconSize = 32.dp,
        cornerRadius = 18.dp,
        modifier = Modifier.background(iconContainerColor, RoundedCornerShape(18.dp)).testTag("${testTag}_icon"),
      )

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Surface(
            color = accentColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.25f)),
          ) {
            Text(
              text = badgeText,
              style = MaterialTheme.typography.labelSmall,
              color = accentColor,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Surface(
        color = if (isDark) accentColor.copy(alpha = 0.18f) else PureWhite.copy(alpha = 0.85f),
        shape = CircleShape,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.24f)),
        modifier =
          Modifier
            .size(38.dp)
            .shadow(4.dp, CircleShape, ambientColor = accentColor.copy(alpha = 0.2f), spotColor = accentColor.copy(alpha = 0.2f))
            .testTag("${testTag}_arrow"),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
fun SectionCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  val isDark = LocalManoDarkTheme.current
  val shape = RoundedCornerShape(24.dp)
  val glowTint = MaterialTheme.colorScheme.primary
  val cardBg = if (isDark) DarkSurface else PureWhite
  Card(
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = BorderStroke(1.dp, if (isDark) glowTint.copy(alpha = 0.28f) else HairlineBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(
          elevation = 9.dp,
          shape = shape,
          ambientColor = if (isDark) glowTint.copy(alpha = 0.20f) else ShadowSlate.copy(alpha = 0.13f),
          spotColor = if (isDark) glowTint.copy(alpha = 0.18f) else ShadowSlate.copy(alpha = 0.11f),
        ),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .drawBehind {
            drawCircle(
              brush = Brush.radialGradient(listOf(glowTint.copy(alpha = if (isDark) 0.11f else 0.06f), Color.Transparent)),
              center = Offset(size.width * 0.85f, size.height * 0.12f),
              radius = size.width * 0.55f,
            )
          }
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      content = content,
    )
  }
}
