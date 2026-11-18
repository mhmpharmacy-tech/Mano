package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CoralCare
import com.example.ui.theme.CoralCareInk
import com.example.ui.theme.CoralSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkJade
import com.example.ui.theme.DarkOlive
import com.example.ui.theme.DarkOrange
import com.example.ui.theme.DarkPlum
import com.example.ui.theme.DarkSky
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.JadeCare
import com.example.ui.theme.JadeCareInk
import com.example.ui.theme.JadeSurface
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.OliveCare
import com.example.ui.theme.OliveCareInk
import com.example.ui.theme.OliveSurface
import com.example.ui.theme.OrangeCare
import com.example.ui.theme.OrangeCareInk
import com.example.ui.theme.OrangeSurface
import com.example.ui.theme.PlumCare
import com.example.ui.theme.PlumCareInk
import com.example.ui.theme.PlumSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.ShadowSlate
import com.example.ui.theme.SkyCare
import com.example.ui.theme.SkyCareInk
import com.example.ui.theme.SkySurface

enum class ManoSquadMember(
  val id: String,
  val englishName: String,
  val responsibilityFa: String,
  val summaryFa: String,
  val colorNameFa: String,
  @DrawableRes val characterResId: Int,
) {
  DRUG(
    id = "drug",
    englishName = "Mano Drug",
    responsibilityFa = "ایمنی دارویی",
    summaryFa = "غلظت دارو، سرنگ mL و تداخل‌ها",
    colorNameFa = "سبز یشمی",
    characterResId = R.drawable.ic_mano_drug,
  ),
  GROWTH(
    id = "growth",
    englishName = "Mano Growth",
    responsibilityFa = "رشد و تکامل",
    summaryFa = "نقاط عطف سنی، قد، وزن و خواب",
    colorNameFa = "آبی",
    characterResId = R.drawable.ic_mano_growth,
  ),
  SKIN(
    id = "skin",
    englishName = "Mano Skin",
    responsibilityFa = "مراقبت پوست",
    summaryFa = "سوختگی پوشک، خشکی و اگزما",
    colorNameFa = "مرجانی",
    characterResId = R.drawable.ic_mano_skin,
  ),
  NUTRITION(
    id = "nutrition",
    englishName = "Mano Nutrition",
    responsibilityFa = "تغذیه",
    summaryFa = "غذای کمکی، شیرخشک و اشتها",
    colorNameFa = "سبز زیتونی",
    characterResId = R.drawable.ic_mano_nutrition,
  ),
  CARE(
    id = "care",
    englishName = "Mano Care",
    responsibilityFa = "تب و علائم",
    summaryFa = "تب‌سنجی، سرفه و راهنمای مراقبت",
    colorNameFa = "نارنجی",
    characterResId = R.drawable.ic_mano_care,
  ),
  CONNECT(
    id = "connect",
    englishName = "Mano Connect",
    responsibilityFa = "ارتباط با داروساز",
    summaryFa = "پرسش علمی مستقیم از داروساز",
    colorNameFa = "بنفش",
    characterResId = R.drawable.ic_mano_connect,
  );

  fun accentColor(isDark: Boolean): Color =
    when (this) {
      DRUG -> if (isDark) DarkJade else JadeCare
      GROWTH -> if (isDark) DarkSky else SkyCare
      SKIN -> if (isDark) DarkCoral else CoralCare
      NUTRITION -> if (isDark) DarkOlive else OliveCare
      CARE -> if (isDark) DarkOrange else OrangeCare
      CONNECT -> if (isDark) DarkPlum else PlumCare
    }

  fun tintedSurfaceColor(isDark: Boolean): Color =
    when (this) {
      DRUG -> if (isDark) DarkJade.copy(alpha = 0.17f) else JadeSurface
      GROWTH -> if (isDark) DarkSky.copy(alpha = 0.17f) else SkySurface
      SKIN -> if (isDark) DarkCoral.copy(alpha = 0.17f) else CoralSurface
      NUTRITION -> if (isDark) DarkOlive.copy(alpha = 0.17f) else OliveSurface
      CARE -> if (isDark) DarkOrange.copy(alpha = 0.17f) else OrangeSurface
      CONNECT -> if (isDark) DarkPlum.copy(alpha = 0.17f) else PlumSurface
    }

  fun textAccentColor(isDark: Boolean): Color =
    when (this) {
      DRUG -> if (isDark) DarkJade else JadeCareInk
      GROWTH -> if (isDark) DarkSky else SkyCareInk
      SKIN -> if (isDark) DarkCoral else CoralCareInk
      NUTRITION -> if (isDark) DarkOlive else OliveCareInk
      CARE -> if (isDark) DarkOrange else OrangeCareInk
      CONNECT -> if (isDark) DarkPlum else PlumCareInk
    }
}

@Composable
fun ManoSquadSection(
  onSelectMember: (ManoSquadMember) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  val members = ManoSquadMember.entries

  Column(
    modifier = modifier.fillMaxWidth().testTag("mano_squad_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Rich header with 6-hue spectrum indicator strip
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            text = stringResource(R.string.mano_squad_section_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        ) {
          Text(
            text = stringResource(R.string.mano_squad_section_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      Text(
        text = stringResource(R.string.mano_squad_section_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // Subtle 6-color spectrum bar showcasing the squad color system
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        members.forEach { member ->
          Box(
            modifier =
              Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(member.accentColor(isDark))
          )
        }
      }
    }

    // Responsive 2-column grid of interactive character cards
    members.chunked(2).forEach { rowMembers ->
      Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        rowMembers.forEach { member ->
          ManoSquadCharacterCard(
            member = member,
            onClick = { onSelectMember(member) },
            modifier = Modifier.weight(1f).fillMaxHeight(),
          )
        }
        if (rowMembers.size == 1) {
          Spacer(Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
fun ManoSquadCharacterCard(
  member: ManoSquadMember,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  val accent = member.accentColor(isDark)
  val textAccent = member.textAccentColor(isDark)
  val tintSurface = member.tintedSurfaceColor(isDark)
  val cardShape = RoundedCornerShape(24.dp)
  val cardContainer = com.example.ui.theme.manoCardSurface()
  val borderColor = accent.copy(alpha = if (isDark) 0.40f else 0.28f)

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (isPressed) 0.965f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "squadCardScale_${member.id}",
  )

  Card(
    onClick = onClick,
    interactionSource = interactionSource,
    shape = cardShape,
    colors = CardDefaults.cardColors(containerColor = cardContainer),
    border = BorderStroke(1.2.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier =
      modifier
        .scale(pressScale)
        .shadow(
          elevation = if (isDark) 10.dp else 9.dp,
          shape = cardShape,
          ambientColor = accent.copy(alpha = if (isDark) 0.30f else 0.18f),
          spotColor = if (isDark) accent.copy(alpha = 0.26f) else ShadowSlate.copy(alpha = 0.14f),
        )
        .minimumInteractiveComponentSize()
        .semantics {
          role = Role.Button
          contentDescription = "${member.responsibilityFa}، ${member.englishName}، ${member.summaryFa}"
        }
        .testTag("mano_squad_${member.id}"),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .drawBehind {
            // Soft radial ambient glow behind character showcase
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      accent.copy(alpha = if (isDark) 0.20f else 0.12f),
                      Color.Transparent,
                    )
                ),
              center = Offset(size.width * 0.5f, size.height * 0.27f),
              radius = size.width * 0.56f,
            )
          }
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top row: Color-coded role pill + directional action dot
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = tintSurface,
            shape = RoundedCornerShape(50),
            border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f)),
            modifier = Modifier.weight(1f, fill = false),
          ) {
            Text(
              text = member.responsibilityFa,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = textAccent,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }

          Spacer(Modifier.width(6.dp))

          Surface(
            color = if (isDark) DarkSurfaceHigh else PureWhite.copy(alpha = 0.92f),
            shape = CircleShape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
            modifier = Modifier.size(28.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = textAccent,
                modifier = Modifier.size(15.dp),
              )
            }
          }
        }

        Spacer(Modifier.height(10.dp))

        // Crisp vector character inside luminous soft-shaded glass medallion
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(88.dp)
              .shadow(
                elevation = if (isDark) 10.dp else 8.dp,
                shape = CircleShape,
                ambientColor = accent.copy(alpha = if (isDark) 0.42f else 0.24f),
                spotColor = accent.copy(alpha = if (isDark) 0.40f else 0.22f),
              )
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  colors =
                    listOf(
                      PureWhite.copy(alpha = if (isDark) 0.16f else 0.98f),
                      tintSurface.copy(alpha = if (isDark) 0.92f else 0.96f),
                    )
                )
              )
              .border(
                width = 1.6.dp,
                brush =
                  Brush.linearGradient(
                    colors =
                      listOf(
                        if (isDark) accent.copy(alpha = 0.72f) else PureWhite.copy(alpha = 0.96f),
                        accent.copy(alpha = if (isDark) 0.30f else 0.38f),
                      )
                  ),
                shape = CircleShape,
              )
              .drawBehind {
                drawArc(
                  color = PureWhite.copy(alpha = if (isDark) 0.28f else 0.85f),
                  startAngle = 205f,
                  sweepAngle = 110f,
                  useCenter = false,
                  topLeft = Offset(size.width * 0.12f, size.height * 0.08f),
                  size = Size(size.width * 0.76f, size.height * 0.44f),
                  style = Stroke(width = 2.6f, cap = StrokeCap.Round),
                )
              },
        ) {
          ManoV1SquadCharacterCanvas(
            member = member,
            isDark = isDark,
            modifier =
              Modifier
                .fillMaxSize()
                .padding(3.dp)
                .clip(CircleShape)
                .semantics {
                  contentDescription = "کاراکتر ${member.englishName} برای بخش ${member.responsibilityFa}"
                },
          )
        }

        Spacer(Modifier.height(10.dp))

        // Bottom typography block: crisp LTR character name + Persian responsibility caption
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
              text = member.englishName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = textAccent,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = member.summaryFa,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
