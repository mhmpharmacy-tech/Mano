package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.DarkAqua
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.DeepForest
import com.example.ui.theme.GoldSurface
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.MangoCare
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SkyCare
import com.example.ui.theme.SkySurface
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.SoftSage
import com.example.ui.theme.WarmIvory
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoSemanticAccent

enum class ManoEmptyStateDomain(val tagKey: String, val badgeFa: String) {
  MEDICINES("medicines", "داروهای کودک"),
  VACCINES("vaccines", "برنامه واکسیناسیون"),
  SUPPLEMENTS("supplements", "مکمل‌ها و بهداشت"),
}

@Composable
fun ManoSectionEmptyStateCard(
  domain: ManoEmptyStateDomain,
  titleFa: String,
  subtitleFa: String,
  nextStepsFa: List<String>,
  primaryActionLabelFa: String? = null,
  onPrimaryAction: (() -> Unit)? = null,
  primaryActionIcon: ImageVector? = null,
  secondaryActionLabelFa: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
  secondaryActionIcon: ImageVector? = null,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  val spacing = ManoTheme.spacing
  val (accent, softBg, haloColor) =
    when (domain) {
      ManoEmptyStateDomain.MEDICINES ->
        Triple(
          if (isDark) OrbitCyan else DeepForest,
          if (isDark) DarkSurface else PureWhite,
          if (isDark) DarkSurfaceHigh else AquaSurface,
        )
      ManoEmptyStateDomain.VACCINES ->
        Triple(
          manoSemanticAccent(SkyCare, DarkAqua),
          if (isDark) DarkSurface else PureWhite,
          if (isDark) DarkSurfaceHigh else SkySurface,
        )
      ManoEmptyStateDomain.SUPPLEMENTS ->
        Triple(
          manoSemanticAccent(MangoCare, DarkGold),
          if (isDark) DarkSurface else PureWhite,
          if (isDark) DarkSurfaceHigh else GoldSurface,
        )
    }

  val stepSurfaceColor =
    when (domain) {
      ManoEmptyStateDomain.MEDICINES -> if (isDark) DarkSurfaceHigh else SoftSage.copy(alpha = 0.58f)
      ManoEmptyStateDomain.VACCINES -> if (isDark) DarkSurfaceHigh else SoftLavender.copy(alpha = 0.52f)
      ManoEmptyStateDomain.SUPPLEMENTS -> if (isDark) DarkSurfaceHigh else WarmIvory
    }

  Surface(
    color = softBg,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.2.dp, accent.copy(alpha = if (isDark) 0.34f else 0.26f)),
    modifier = modifier.fillMaxWidth().testTag("empty_state_${domain.tagKey}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
      Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
      ) {
        Text(
          text = domain.badgeFa,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = accent,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
      }

      SoftEmptyStateIllustration(
        domain = domain,
        accent = accent,
        haloColor = haloColor,
        isDark = isDark,
      )

      Text(
        text = titleFa,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )

      Text(
        text = subtitleFa,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      if (nextStepsFa.isNotEmpty()) {
        Surface(
          color = stepSurfaceColor,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, manoBorderColor()),
          modifier = Modifier.fillMaxWidth().testTag("empty_state_${domain.tagKey}_next_steps"),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "پیشنهاد اقدام بعدی:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
              )
            }
            nextStepsFa.forEach { step ->
              Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircleOutline,
                  contentDescription = null,
                  tint = accent.copy(alpha = 0.85f),
                  modifier = Modifier.padding(top = 2.dp).size(15.dp),
                )
                Text(
                  text = step,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      if (primaryActionLabelFa != null && onPrimaryAction != null) {
        Button(
          onClick = onPrimaryAction,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = accent,
              contentColor = if (isDark) DarkSurface else PureWhite,
            ),
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("empty_state_${domain.tagKey}_primary_btn"),
        ) {
          if (primaryActionIcon != null) {
            Icon(primaryActionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
          }
          Text(primaryActionLabelFa, fontWeight = FontWeight.Bold)
        }
      }

      if (secondaryActionLabelFa != null && onSecondaryAction != null) {
        OutlinedButton(
          onClick = onSecondaryAction,
          border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("empty_state_${domain.tagKey}_secondary_btn"),
        ) {
          if (secondaryActionIcon != null) {
            Icon(secondaryActionIcon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
          }
          Text(secondaryActionLabelFa, color = accent, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
private fun SoftEmptyStateIllustration(
  domain: ManoEmptyStateDomain,
  accent: Color,
  haloColor: Color,
  isDark: Boolean,
) {
  val description =
    when (domain) {
      ManoEmptyStateDomain.MEDICINES -> "تصویر ملایم وضعیت خالی بخش داروهای کودک"
      ManoEmptyStateDomain.VACCINES -> "تصویر ملایم وضعیت خالی بخش واکسیناسیون"
      ManoEmptyStateDomain.SUPPLEMENTS -> "تصویر ملایم وضعیت خالی بخش مکمل‌ها"
    }
  val cardSurface = if (isDark) DarkSurfaceHigh else PureWhite
  val secondaryTint =
    when (domain) {
      ManoEmptyStateDomain.MEDICINES -> OrbitCyan
      ManoEmptyStateDomain.VACCINES -> if (isDark) DarkSage else SoftSage
      ManoEmptyStateDomain.SUPPLEMENTS -> if (isDark) DarkSage else SoftSage
    }

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.size(108.dp).semantics { contentDescription = description },
  ) {
    Canvas(modifier = Modifier.size(108.dp)) {
      val w = size.width
      val h = size.height
      val cx = w / 2f
      val cy = h / 2f

      // Soft ambient cloud halo + Orbit ring
      drawCircle(
        color = haloColor,
        radius = w * 0.44f,
        center = Offset(cx, cy),
      )
      drawCircle(
        color = accent.copy(alpha = 0.16f),
        radius = w * 0.47f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.dp.toPx()),
      )
      // Ambient gentle floating bubbles
      drawCircle(color = OrbitCyan.copy(alpha = 0.48f), radius = w * 0.055f, center = Offset(w * 0.20f, h * 0.24f))
      drawCircle(color = secondaryTint.copy(alpha = 0.72f), radius = w * 0.04f, center = Offset(w * 0.81f, h * 0.72f))

      when (domain) {
        ManoEmptyStateDomain.MEDICINES -> {
          // Soft syrup bottle + tilted capsule in calm clinical tones
          val bottleW = w * 0.34f
          val bottleH = h * 0.40f
          val bx = cx - bottleW * 0.62f
          val by = cy - bottleH * 0.32f
          // Cap
          drawRoundRect(
            color = accent.copy(alpha = 0.86f),
            topLeft = Offset(bx + bottleW * 0.22f, by - h * 0.11f),
            size = Size(bottleW * 0.56f, h * 0.11f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
          )
          // Bottle body
          drawRoundRect(
            color = cardSurface,
            topLeft = Offset(bx, by),
            size = Size(bottleW, bottleH),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
          )
          drawRoundRect(
            color = accent,
            topLeft = Offset(bx, by),
            size = Size(bottleW, bottleH),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
            style = Stroke(width = 2.dp.toPx()),
          )
          // Bottle calm fill band
          drawRoundRect(
            color = OrbitCyan.copy(alpha = 0.45f),
            topLeft = Offset(bx + 3.dp.toPx(), by + bottleH * 0.42f),
            size = Size(bottleW - 6.dp.toPx(), bottleH * 0.50f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
          )
          // Cross mark on bottle
          drawLine(
            color = accent,
            start = Offset(bx + bottleW * 0.5f, by + bottleH * 0.20f),
            end = Offset(bx + bottleW * 0.5f, by + bottleH * 0.35f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawLine(
            color = accent,
            start = Offset(bx + bottleW * 0.36f, by + bottleH * 0.275f),
            end = Offset(bx + bottleW * 0.64f, by + bottleH * 0.275f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
          )
          // Gentle tilted capsule beside bottle
          rotate(degrees = -28f, pivot = Offset(cx + w * 0.18f, cy + h * 0.11f)) {
            val capX = cx + w * 0.08f
            val capY = cy + h * 0.01f
            val capW = w * 0.20f
            val capH = h * 0.26f
            drawRoundRect(
              color = cardSurface,
              topLeft = Offset(capX, capY),
              size = Size(capW, capH),
              cornerRadius = CornerRadius(capW / 2f, capW / 2f),
            )
            drawRoundRect(
              color = OrbitCyan.copy(alpha = 0.62f),
              topLeft = Offset(capX, capY + capH * 0.48f),
              size = Size(capW, capH * 0.52f),
              cornerRadius = CornerRadius(capW / 2f, capW / 2f),
            )
            drawRoundRect(
              color = accent,
              topLeft = Offset(capX, capY),
              size = Size(capW, capH),
              cornerRadius = CornerRadius(capW / 2f, capW / 2f),
              style = Stroke(width = 1.8.dp.toPx()),
            )
          }
        }

        ManoEmptyStateDomain.VACCINES -> {
          // Protective shield + calm calendar check
          val shieldPath = Path().apply {
            moveTo(cx, cy - h * 0.27f)
            lineTo(cx + w * 0.24f, cy - h * 0.18f)
            lineTo(cx + w * 0.22f, cy + h * 0.05f)
            quadraticTo(cx + w * 0.18f, cy + h * 0.24f, cx, cy + h * 0.30f)
            quadraticTo(cx - w * 0.18f, cy + h * 0.24f, cx - w * 0.22f, cy + h * 0.05f)
            lineTo(cx - w * 0.24f, cy - h * 0.18f)
            close()
          }
          drawPath(path = shieldPath, color = cardSurface)
          drawPath(
            path = shieldPath,
            color = accent.copy(alpha = 0.18f),
          )
          drawPath(
            path = shieldPath,
            color = accent,
            style = Stroke(width = 2.2.dp.toPx(), join = StrokeJoin.Round),
          )
          // Gentle checkmark inside shield
          val checkPath = Path().apply {
            moveTo(cx - w * 0.09f, cy + h * 0.01f)
            lineTo(cx - w * 0.01f, cy + h * 0.09f)
            lineTo(cx + w * 0.12f, cy - h * 0.06f)
          }
          drawPath(
            path = checkPath,
            color = accent,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
          )
        }

        ManoEmptyStateDomain.SUPPLEMENTS -> {
          // Sunny golden vitamin drop + soft botanical leaf accent
          val sunCenter = Offset(cx + w * 0.14f, cy - h * 0.14f)
          drawCircle(
            color = accent.copy(alpha = 0.28f),
            radius = w * 0.13f,
            center = sunCenter,
          )
          drawCircle(
            color = accent.copy(alpha = 0.78f),
            radius = w * 0.08f,
            center = sunCenter,
          )
          // Vitamin droplet path
          val dropPath = Path().apply {
            moveTo(cx - w * 0.04f, cy - h * 0.20f)
            cubicTo(
              cx + w * 0.15f, cy - h * 0.02f,
              cx + w * 0.17f, cy + h * 0.24f,
              cx - w * 0.04f, cy + h * 0.26f,
            )
            cubicTo(
              cx - w * 0.25f, cy + h * 0.24f,
              cx - w * 0.23f, cy - h * 0.02f,
              cx - w * 0.04f, cy - h * 0.20f,
            )
            close()
          }
          drawPath(path = dropPath, color = cardSurface)
          drawPath(
            path = dropPath,
            color = accent,
            style = Stroke(width = 2.2.dp.toPx(), join = StrokeJoin.Round),
          )
          drawCircle(
            color = OrbitCyan.copy(alpha = 0.65f),
            radius = w * 0.075f,
            center = Offset(cx - w * 0.04f, cy + h * 0.10f),
          )
          // Soft sheen inside droplet
          drawCircle(
            color = PureWhite.copy(alpha = 0.85f),
            radius = w * 0.024f,
            center = Offset(cx - w * 0.075f, cy + h * 0.065f),
          )
        }
      }
    }
  }
}
