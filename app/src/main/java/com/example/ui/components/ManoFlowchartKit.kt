package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface

/**
 * Compact 1-row screen header with MANO visual badge, concise title/subtitle,
 * and an optional trailing action pill — keeps screens compact and above the fold.
 */
@Composable
fun ManoCompactScreenHeader(
  titleFa: String,
  subtitleFa: String,
  @DrawableRes illustrationResId: Int? = null,
  icon: ImageVector? = null,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
  trailingAction: (@Composable () -> Unit)? = null,
) {
  val isDark = LocalManoDarkTheme.current
  Surface(
    color = manoCardSurface(),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.30f else 0.20f)),
    shadowElevation = 1.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (illustrationResId != null) {
        ManoFramedIllustration(
          resId = illustrationResId,
          contentDescription = titleFa,
          width = 48.dp,
          aspectRatio = 1f,
          accentColor = accentColor,
        )
      } else if (icon != null) {
        ManoIconMedallion(
          icon = icon,
          accentColor = accentColor,
          containerSize = 44.dp,
          iconSize = 22.dp,
          cornerRadius = 14.dp,
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = titleFa,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = if (isDark) DarkTextPrimary else DarkInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitleFa,
          style = MaterialTheme.typography.labelSmall,
          color = if (isDark) DarkTextMuted else MutedSlate,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      trailingAction?.invoke()
    }
  }
}

/**
 * Horizontal icon-driven connected flowchart step bar.
 * Displays compact circular/squircle icon nodes joined by directional flow arrows.
 */
data class FlowchartStepSpec(
  val id: String,
  val labelFa: String,
  val icon: ImageVector,
  val badgeFa: String? = null,
  val testTag: String? = null,
)

@Composable
fun ManoFlowchartStepperBar(
  steps: List<FlowchartStepSpec>,
  activeIndex: Int,
  onSelectStep: ((Int) -> Unit)? = null,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  val mutedColor = if (isDark) DarkTextMuted else MutedSlate
  val inactiveBg = if (isDark) DarkSurfaceHigh else PureWhite
  val borderBase = if (isDark) DarkBorder else HairlineBorder

  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    steps.forEachIndexed { index, step ->
      val isActive = index == activeIndex
      val isCompleted = index < activeIndex
      val nodeAccent = if (isActive || isCompleted) accentColor else mutedColor
      val nodeBg =
        when {
          isActive -> accentColor.copy(alpha = if (isDark) 0.24f else 0.14f)
          isCompleted -> accentColor.copy(alpha = if (isDark) 0.14f else 0.08f)
          else -> inactiveBg
        }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier =
          Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .then(
              if (onSelectStep != null) {
                Modifier.clickable { onSelectStep(index) }.minimumInteractiveComponentSize()
              } else {
                Modifier
              }
            )
            .then(if (step.testTag != null) Modifier.testTag(step.testTag) else Modifier)
            .padding(vertical = 3.dp, horizontal = 2.dp),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(nodeBg)
              .border(
                width = if (isActive) 1.8.dp else 1.dp,
                color = if (isActive) accentColor else borderBase,
                shape = CircleShape,
              ),
        ) {
          Icon(
            imageVector = step.icon,
            contentDescription = step.labelFa,
            tint = nodeAccent,
            modifier = Modifier.size(18.dp),
          )
        }
        Text(
          text = step.labelFa,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
          color = if (isActive) accentColor else mutedColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
        if (!step.badgeFa.isNullOrBlank()) {
          Text(
            text = step.badgeFa,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            maxLines = 1,
          )
        }
      }

      if (index < steps.lastIndex) {
        FlowchartConnectorArrow(
          color = if (index < activeIndex) accentColor else borderBase,
          modifier = Modifier.width(16.dp).height(14.dp),
        )
      }
    }
  }
}

/**
 * Minimal horizontal flowchart connector arrow (respects RTL visual flow).
 */
@Composable
fun FlowchartConnectorArrow(
  color: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val cy = size.height / 2f
    val strokeW = 1.6.dp.toPx()
    // RTL reading flow points right-to-left (←)
    drawLine(
      color = color,
      start = Offset(size.width * 0.90f, cy),
      end = Offset(size.width * 0.16f, cy),
      strokeWidth = strokeW,
      cap = StrokeCap.Round,
    )
    val head =
      Path().apply {
        moveTo(size.width * 0.36f, cy - size.height * 0.28f)
        lineTo(size.width * 0.12f, cy)
        lineTo(size.width * 0.36f, cy + size.height * 0.28f)
      }
    drawPath(
      path = head,
      color = color,
      style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
  }
}

/**
 * Compact vertical flowchart arrow linking two visual stages cleanly.
 */
@Composable
fun FlowchartDownConnector(
  labelFa: String? = null,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Canvas(modifier = Modifier.size(width = 16.dp, height = 16.dp)) {
      val cx = size.width / 2f
      val sw = 1.6.dp.toPx()
      drawLine(accentColor.copy(alpha = 0.65f), Offset(cx, 1f), Offset(cx, size.height - 3f), sw, StrokeCap.Round)
      val head =
        Path().apply {
          moveTo(cx - 4.dp.toPx(), size.height - 6.dp.toPx())
          lineTo(cx, size.height - 1.5.dp.toPx())
          lineTo(cx + 4.dp.toPx(), size.height - 6.dp.toPx())
        }
      drawPath(head, accentColor.copy(alpha = 0.85f), style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    if (!labelFa.isNullOrBlank()) {
      Spacer(Modifier.width(5.dp))
      Text(
        text = labelFa,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = if (isDark) DarkTextMuted else MutedSlate,
      )
    }
  }
}

/**
 * Compact interactive flowchart node card for 2-column or 3-column icon boards.
 * Designed so 6 nodes fit comfortably inside a single mobile screen without scrolling.
 */
@Composable
fun ManoFlowchartIconTile(
  titleFa: String,
  subtitleFa: String? = null,
  badgeFa: String? = null,
  icon: ImageVector,
  accentColor: Color,
  selected: Boolean = false,
  stepNumberFa: String? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  minHeight: Dp = 84.dp,
) {
  val isDark = LocalManoDarkTheme.current
  val baseBg =
    if (selected) {
      lerp(if (isDark) DarkSurfaceHigh else PureWhite, accentColor, if (isDark) 0.24f else 0.14f)
    } else {
      lerp(if (isDark) DarkSurface else PureWhite, accentColor, if (isDark) 0.08f else 0.04f)
    }
  val borderColor =
    if (selected) accentColor else accentColor.copy(alpha = if (isDark) 0.26f else 0.18f)
  val titleInk = if (isDark) DarkTextPrimary else DarkInk
  val mutedInk = if (isDark) DarkTextMuted else MutedSlate

  Surface(
    onClick = onClick,
    color = baseBg,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(if (selected) 1.7.dp else 1.dp, borderColor),
    shadowElevation = if (selected) 2.dp else 1.dp,
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    Box(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
      if (!stepNumberFa.isNullOrBlank()) {
        Surface(
          color = accentColor.copy(alpha = if (isDark) 0.22f else 0.12f),
          shape = CircleShape,
          modifier = Modifier.align(Alignment.TopEnd).size(20.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = stepNumberFa,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = accentColor,
            )
          }
        }
      }
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.13f))
              .border(0.8.dp, accentColor.copy(alpha = if (isDark) 0.35f else 0.22f), RoundedCornerShape(12.dp)),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = titleFa,
            tint = accentColor,
            modifier = Modifier.size(20.dp),
          )
        }
        Text(
          text = titleFa,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = titleInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
        if (!subtitleFa.isNullOrBlank()) {
          Text(
            text = subtitleFa,
            style = MaterialTheme.typography.labelSmall,
            color = mutedInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
          )
        }
        if (!badgeFa.isNullOrBlank()) {
          Surface(
            color = accentColor.copy(alpha = if (isDark) 0.18f else 0.10f),
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = badgeFa,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = accentColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
        }
      }
    }
  }
}

/**
 * Reusable ModalBottomSheet for inspecting full flowchart node details without cluttering the main screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManoFlowDetailBottomSheet(
  titleFa: String,
  subtitleFa: String? = null,
  icon: ImageVector? = null,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  onDismiss: () -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (icon != null) {
          ManoIconMedallion(
            icon = icon,
            accentColor = accentColor,
            containerSize = 40.dp,
            iconSize = 20.dp,
            cornerRadius = 12.dp,
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = titleFa,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (!subtitleFa.isNullOrBlank()) {
            Text(
              text = subtitleFa,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(Icons.Rounded.Close, contentDescription = "بستن")
        }
      }
      content()
      Spacer(Modifier.height(16.dp))
    }
  }
}
