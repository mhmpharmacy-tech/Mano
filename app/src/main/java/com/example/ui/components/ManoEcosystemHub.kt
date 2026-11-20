package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.MascotPose
import com.example.domain.model.ReviewStatus
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkJade
import com.example.ui.theme.DarkPlum
import com.example.ui.theme.DarkSage
import com.example.ui.theme.DarkSky
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.JadeCare
import com.example.ui.theme.JadeCareInk
import com.example.ui.theme.JadeSurface
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.MangoCare
import com.example.ui.theme.MangoSurface
import com.example.ui.theme.PlumCare
import com.example.ui.theme.PlumCareInk
import com.example.ui.theme.PlumSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SageCare
import com.example.ui.theme.SageSurface
import com.example.ui.theme.ShadowSlate
import com.example.ui.theme.SkyCare
import com.example.ui.theme.SkyCareInk
import com.example.ui.theme.SkySurface
import com.example.ui.theme.manoCardSurface

/**
 * Canonical 6 specialized environments of the MANO clinical ecosystem:
 * 1. دستیار پرسش و پاسخ مستند (Documented Q&A Assistant)
 * 2. MANO Safety — بررسی هشدارهای دارویی (Medication Safety & Overlap Checker)
 * 3. مقایسه علمی محصولات سلامت (Scientific Health Product Comparison)
 * 4. MANO Connect — ارتباط امن خانواده و داروساز (Secure Family-Pharmacist Intake)
 * 5. MANO Follow-up — پیگیری نتیجه مشاوره (Consultation Outcome Tracker)
 * 6. MANO Pharmacy — پنل اختصاصی داروخانه (Verified Pharmacist Clinical Desk)
 */
enum class ManoEcosystemPillar(
  val id: String,
  val englishBadge: String,
  val titleFa: String,
  val subtitleFa: String,
  val icon: ImageVector,
  val iconKind: PediatricIconKind,
  val mascotPose: MascotPose,
) {
  DOCUMENTED_QA(
    id = "documented_qa",
    englishBadge = "MANO Evidence Q&A",
    titleFa = "دستیار پرسش و پاسخ مستند",
    subtitleFa = "پاسخ علمی و غیرتشخیصی با استناد مستقیم به AAP، WHO و NICE",
    icon = Icons.AutoMirrored.Rounded.MenuBook,
    iconKind = PediatricIconKind.FAQ,
    mascotPose = MascotPose.EXPLAIN,
  ),
  MANO_SAFETY(
    id = "mano_safety",
    englishBadge = "MANO Safety",
    titleFa = "بررسی هشدارهای دارویی",
    subtitleFa = "تشخیص همپوشانی ماده مؤثره، تداخل‌ها و ایمنی غلظت شربت و قطره",
    icon = Icons.Rounded.Shield,
    iconKind = PediatricIconKind.SAFETY_SHIELD,
    mascotPose = MascotPose.CAUTION,
  ),
  PRODUCT_COMPARISON(
    id = "product_comparison",
    englishBadge = "MANO Compare",
    titleFa = "مقایسه علمی محصولات سلامت",
    subtitleFa = "مقایسه بی‌طرفانه و بدون تبلیغ تب‌برها، ORS، شیرخشک و ابزارهای دوزینگ",
    icon = Icons.AutoMirrored.Rounded.CompareArrows,
    iconKind = PediatricIconKind.COMPARE_LAB,
    mascotPose = MascotPose.EXPLAIN,
  ),
  MANO_CONNECT(
    id = "mano_connect",
    englishBadge = "MANO Connect",
    titleFa = "ارتباط امن خانواده و داروساز",
    subtitleFa = "ثبت پرسش غیرفوری دارویی و مراقبتی با غربالگری علائم خطر",
    icon = Icons.Rounded.SupportAgent,
    iconKind = PediatricIconKind.FAMILY,
    mascotPose = MascotPose.LISTEN,
  ),
  MANO_FOLLOW_UP(
    id = "mano_followup",
    englishBadge = "MANO Follow-up",
    titleFa = "پیگیری نتیجه مشاوره",
    subtitleFa = "رصد گام‌به‌گام وضعیت پرونده، پاسخ علمی داروساز و چک‌لیست اقدام والدین",
    icon = Icons.Rounded.HistoryEdu,
    iconKind = PediatricIconKind.FOLLOW_UP,
    mascotPose = MascotPose.SAVED,
  ),
  MANO_PHARMACY(
    id = "mano_pharmacy",
    englishBadge = "MANO Pharmacy",
    titleFa = "پنل اختصاصی داروخانه",
    subtitleFa = "میز کار تخصصی داروساز تاییدشده برای پذیرش، بررسی و ثبت پاسخ علمی",
    icon = Icons.Rounded.LocalPharmacy,
    iconKind = PediatricIconKind.PHARMACY,
    mascotPose = MascotPose.WELCOME,
  );

  fun accentColor(isDark: Boolean): Color =
    when (this) {
      DOCUMENTED_QA -> if (isDark) DarkSky else SkyCare
      MANO_SAFETY -> if (isDark) DarkJade else JadeCare
      PRODUCT_COMPARISON -> if (isDark) DarkGold else MangoCare
      MANO_CONNECT -> if (isDark) DarkPlum else PlumCare
      MANO_FOLLOW_UP -> if (isDark) DarkSage else SageCare
      MANO_PHARMACY -> if (isDark) DarkSky else SkyCare
    }

  fun textAccentColor(isDark: Boolean): Color =
    when (this) {
      DOCUMENTED_QA -> if (isDark) DarkSky else SkyCareInk
      MANO_SAFETY -> if (isDark) DarkJade else JadeCareInk
      PRODUCT_COMPARISON -> if (isDark) DarkGold else MangoCare
      MANO_CONNECT -> if (isDark) DarkPlum else PlumCareInk
      MANO_FOLLOW_UP -> if (isDark) DarkSage else JadeCareInk
      MANO_PHARMACY -> if (isDark) DarkSky else SkyCareInk
    }

  fun badgeSurfaceColor(isDark: Boolean): Color =
    when (this) {
      DOCUMENTED_QA -> if (isDark) DarkSky.copy(alpha = 0.16f) else SkySurface
      MANO_SAFETY -> if (isDark) DarkJade.copy(alpha = 0.16f) else JadeSurface
      PRODUCT_COMPARISON -> if (isDark) DarkGold.copy(alpha = 0.16f) else MangoSurface
      MANO_CONNECT -> if (isDark) DarkPlum.copy(alpha = 0.16f) else PlumSurface
      MANO_FOLLOW_UP -> if (isDark) DarkSage.copy(alpha = 0.16f) else SageSurface
      MANO_PHARMACY -> if (isDark) DarkSky.copy(alpha = 0.16f) else SkySurface
    }
}

/**
 * Reusable branded header banner eliminating duplicate pill + LtrText + SourceBadge
 * boilerplate across internal MANO screens while enforcing consistent character pose & iconography.
 */
@Composable
fun ManoBrandedModuleHeader(
  englishBadge: String,
  titleFa: String,
  subtitleFa: String,
  icon: ImageVector,
  iconKind: PediatricIconKind,
  mascotPose: MascotPose,
  accentColor: Color,
  modifier: Modifier = Modifier,
  reviewStatus: ReviewStatus = ReviewStatus.APPROVED,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    ManoPoseAvatar(pose = mascotPose, size = 58.dp)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = accentColor.copy(alpha = 0.12f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.30f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(14.dp),
            )
            LtrText(text = englishBadge, color = accentColor)
          }
        }
        SourceBadge(reviewStatus = reviewStatus)
      }
      Text(
        text = titleFa,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitleFa,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    ManoIconMedallion(
      icon = icon,
      accentColor = accentColor,
      iconKind = iconKind,
      containerSize = 44.dp,
      iconSize = 26.dp,
      cornerRadius = 14.dp,
    )
  }
}

/**
 * Interactive 6-module specialized ecosystem hub on the Home Screen providing direct 1-tap access
 * to Documented Q&A, MANO Safety, Scientific Product Comparison, MANO Connect, MANO Follow-up, and MANO Pharmacy.
 */
@Composable
fun ManoSpecializedEcosystemSection(
  onSelectPillar: (ManoEcosystemPillar) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  val pillars = ManoEcosystemPillar.entries

  Column(
    modifier = modifier.fillMaxWidth().testTag("mano_specialized_ecosystem_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = "شش محیط تخصصی سلامت و داروسازی مانو",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
      ) {
        LtrText(
          text = "MANO 6-Pillar Suite",
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }

    Text(
      text = "دسترسی مستقیم به دستیار پرسش و پاسخ مستند، بررسی هشدارهای دارویی (MANO Safety)، مقایسه علمی محصولات سلامت، ارتباط امن با داروساز (MANO Connect)، پیگیری مشاوره (MANO Follow-up) و پنل اختصاصی داروخانه (MANO Pharmacy).",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    pillars.chunked(2).forEach { rowPillars ->
      Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        rowPillars.forEach { pillar ->
          ManoEcosystemPillarCard(
            pillar = pillar,
            isDark = isDark,
            onClick = { onSelectPillar(pillar) },
            modifier = Modifier.weight(1f).fillMaxHeight(),
          )
        }
        if (rowPillars.size == 1) {
          Spacer(Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun ManoEcosystemPillarCard(
  pillar: ManoEcosystemPillar,
  isDark: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = pillar.accentColor(isDark)
  val textAccent = pillar.textAccentColor(isDark)
  val badgeSurface = pillar.badgeSurfaceColor(isDark)
  val shape = RoundedCornerShape(22.dp)
  val cardBg = manoCardSurface()

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.965f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "ecosystemPillarScale_${pillar.id}",
  )

  Card(
    onClick = onClick,
    interactionSource = interactionSource,
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = BorderStroke(1.2.dp, accent.copy(alpha = if (isDark) 0.38f else 0.28f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .scale(scale)
        .shadow(
          elevation = 8.dp,
          shape = shape,
          ambientColor = accent.copy(alpha = if (isDark) 0.26f else 0.15f),
          spotColor = ShadowSlate.copy(alpha = 0.12f),
        )
        .minimumInteractiveComponentSize()
        .semantics {
          role = Role.Button
          contentDescription = "${pillar.titleFa}، ${pillar.englishBadge}، ${pillar.subtitleFa}"
        }
        .testTag("mano_pillar_${pillar.id}"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .drawBehind {
            drawCircle(
              brush =
                Brush.radialGradient(
                  listOf(
                    accent.copy(alpha = if (isDark) 0.18f else 0.12f),
                    Color.Transparent,
                  )
                ),
              center = Offset(size.width * 0.20f, size.height * 0.22f),
              radius = size.width * 0.65f,
            )
          }
          .padding(13.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ManoIconMedallion(
            icon = pillar.icon,
            accentColor = accent,
            iconKind = pillar.iconKind,
            containerSize = 46.dp,
            iconSize = 26.dp,
            cornerRadius = 15.dp,
          )
          ManoPoseAvatar(pose = pillar.mascotPose, size = 42.dp)
        }

        Surface(
          color = badgeSurface,
          shape = RoundedCornerShape(50),
          border = BorderStroke(0.8.dp, accent.copy(alpha = 0.32f)),
        ) {
          LtrText(
            text = pillar.englishBadge,
            color = textAccent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }

        Text(
          text = pillar.titleFa,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text = pillar.subtitleFa,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "ورود به بخش",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = textAccent,
        )
        Surface(
          color = if (isDark) DarkSurfaceHigh else PureWhite,
          shape = CircleShape,
          border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
          modifier = Modifier.size(26.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
              contentDescription = null,
              tint = textAccent,
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
    }
  }
}
