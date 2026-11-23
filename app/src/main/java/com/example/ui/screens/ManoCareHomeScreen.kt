package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.PreviousCare
import com.example.ui.components.ManoKidWordmark
import com.example.ui.theme.LocalManoDarkTheme

/**
 * Care Home v3 — UI-only, native Jetpack Compose.
 *
 * All child data is supplied by the existing application state. No synthetic
 * names, weights, medication reminders, vaccine dates or clinical statuses.
 * Clinical actions continue to use the established, fail-closed routes.
 * Canonical mascot image is reused from the existing branded drawable.
 */
private val CareForest = Color(0xFF105B48)
private val CareDeep = Color(0xFF103C31)
private val CareWarm = Color(0xFFFFFBF6)
private val CareSage = Color(0xFFE9F5EB)
private val CareCream = Color(0xFFF8F6EF)
private val CareBorder = Color(0xFFD7E7DD)
private val CareMuted = Color(0xFF52665E)
private val CareCoral = Color(0xFFCB634E)
private val CareBlue = Color(0xFF2F83B9)
private val CareGold = Color(0xFFAA7B34)

private data class CareVisuals(
  val background: Color,
  val surface: Color,
  val ink: Color,
  val muted: Color,
  val outline: Color,
  val accent: Color,
  val paleAccent: Color,
)

@Composable
private fun careVisuals(): CareVisuals =
  if (LocalManoDarkTheme.current) {
    CareVisuals(
      background = Color(0xFF101E1C),
      surface = Color(0xFF1E302C),
      ink = Color(0xFFF4F9F5),
      muted = Color(0xFFC6D9CE),
      outline = Color(0xFF385448),
      accent = Color(0xFF79D7B0),
      paleAccent = Color(0xFF233D33),
    )
  } else {
    CareVisuals(CareWarm, Color.White, CareDeep, CareMuted, CareBorder, CareForest, CareSage)
  }

@Composable
private fun CareCard(
  modifier: Modifier = Modifier,
  tinted: Boolean = false,
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val palette = careVisuals()
  val fill = if (tinted) palette.paleAccent else palette.surface
  if (onClick != null) {
    Surface(
      onClick = onClick,
      modifier = modifier,
      shape = RoundedCornerShape(22.dp),
      color = fill,
      tonalElevation = 0.dp,
      shadowElevation = 2.dp,
      border = BorderStroke(1.dp, palette.outline),
      content = content,
    )
  } else {
    Surface(
      modifier = modifier,
      shape = RoundedCornerShape(22.dp),
      color = fill,
      tonalElevation = 0.dp,
      shadowElevation = 2.dp,
      border = BorderStroke(1.dp, palette.outline),
      content = content,
    )
  }
}

@Composable
private fun CareSectionTitle(title: String, onMore: (() -> Unit)? = null, moreText: String = "") {
  val p = careVisuals()
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = p.ink)
    if (onMore != null) {
      Surface(
        onClick = onMore,
        color = Color.Transparent,
        modifier = Modifier.testTag("care_section_more"),
      ) {
        Text(
          moreText,
          modifier = Modifier.padding(8.dp),
          style = MaterialTheme.typography.labelLarge,
          color = p.accent,
        )
      }
    }
  }
}

@Composable
private fun CareIcon(
  icon: ImageVector,
  color: Color,
  surfaceColor: Color,
) {
  Box(
    modifier = Modifier.size(38.dp).background(surfaceColor, RoundedCornerShape(12.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
  }
}

@Composable
private fun CarePrimaryAction(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  lightSurface: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val p = careVisuals()
  val dark = LocalManoDarkTheme.current
  val bg = if (dark) p.surface else lightSurface
  Surface(
    onClick = onClick,
    modifier = modifier.testTag(testTag),
    color = bg,
    shape = RoundedCornerShape(21.dp),
    border = BorderStroke(1.dp, if (dark) p.outline else accent.copy(alpha = 0.2f)),
    shadowElevation = 3.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.Start,
    ) {
      CareIcon(icon, if (dark) p.accent else accent, if (dark) p.paleAccent else Color.White.copy(alpha = 0.75f))
      Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = p.ink,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
      )
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = p.muted,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun CareCompactTool(
  text: String,
  icon: ImageVector,
  accent: Color,
  tint: Color,
  tag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val p = careVisuals()
  val dark = LocalManoDarkTheme.current
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = if (dark) p.surface else tint,
    border = BorderStroke(1.dp, if (dark) p.outline else accent.copy(alpha = 0.15f)),
    modifier = modifier.testTag(tag),
    shadowElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 12.dp, horizontal = 5.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Icon(icon, contentDescription = null, tint = if (dark) p.accent else accent, modifier = Modifier.size(24.dp))
      Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = p.ink,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ManoCareHomeScreen(
  childProfile: ChildProfileDemo?,
  ageBandLabelFa: String?,
  vaccineBadgeLabelFa: String,
  previousCare: PreviousCare?,
  onContinueCare: (PreviousCare) -> Unit,
  onOpenCareCards: (() -> Unit)?,
  onOpenChild: () -> Unit,
  onCheckSymptoms: () -> Unit,
  onAskPharmacist: () -> Unit,
  onMedicines: () -> Unit,
  onVaccines: () -> Unit,
  onGrowth: () -> Unit,
  onNutrition: () -> Unit,
  onFeverCalculator: () -> Unit,
  onOpenGlobalSearch: () -> Unit,
  scrollToTopTrigger: Int = 0,
) {
  val p = careVisuals()
  val isDark = LocalManoDarkTheme.current
  val listState = rememberLazyListState()

  LaunchedEffect(scrollToTopTrigger) {
    if (scrollToTopTrigger > 0) listState.scrollToItem(0)
  }

  LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize().background(p.background).testTag("home_list"),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
    verticalArrangement = Arrangement.spacedBy(15.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      Column(modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          modifier = Modifier.fillMaxWidth().testTag("home_brand_crest"),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ManoKidWordmark(isDark = isDark, fontSize = 22.sp)
        }
        Text(
          "دستیار سلامت کودک و خانواده",
          color = p.muted,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }
    item {
      Column(modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CareCard(onClick = onOpenChild, modifier = Modifier.fillMaxWidth().testTag("care_child_profile")) {
          Row(
            modifier = Modifier.padding(15.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CareIcon(Icons.Rounded.ChildCare, p.accent, p.paleAccent)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                childProfile?.aliasFa?.takeIf { it.isNotBlank() } ?: "پرونده کودک",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = p.ink,
              )
              Text(
                childProfile?.birthDateOrAgeFa?.takeIf { it.isNotBlank() } ?: ageBandLabelFa ?: "برای شخصی‌سازی، اطلاعات کودک را وارد کنید",
                style = MaterialTheme.typography.bodySmall,
                color = p.muted,
                maxLines = 2,
              )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = p.muted, modifier = Modifier.size(20.dp))
          }
        }
        CareCard(modifier = Modifier.fillMaxWidth().testTag("home_hero_card"), tinted = true) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
              Text("من مانو هستم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = p.ink)
              Text(
                "کنار خانواده و داروساز، برای پیگیری مراقبت کودک؛ نه جایگزین ارزیابی پزشکی.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.muted,
              )
            }
            Image(
              painter = painterResource(R.drawable.mano_hero),
              contentDescription = "شخصیت اصلی مانو با چشم‌های گرد",
              modifier = Modifier.size(98.dp),
              contentScale = ContentScale.Fit,
            )
          }
        }
      }
    }
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        CareSectionTitle("چه کمکی نیاز دارید؟")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          CarePrimaryAction(
            title = "بررسی علائم",
            subtitle = "پرسش‌های ایمنی، علائم هشدار و راهنمای مراجعه",
            icon = Icons.Rounded.DeviceThermostat,
            accent = CareCoral,
            lightSurface = Color(0xFFF9EBE6),
            testTag = "home_menu_fever",
            onClick = onCheckSymptoms,
            modifier = Modifier.weight(1f),
          )
          CarePrimaryAction(
            title = "پرسش دارویی",
            subtitle = "پرسش درباره دارو و مسیر ارتباط با داروساز",
            icon = Icons.Rounded.LocalPharmacy,
            accent = CareForest,
            lightSurface = Color(0xFFE8F4F1),
            testTag = "home_ask_pharmacist",
            onClick = onAskPharmacist,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        CareSectionTitle("مراقبت امروز", onMore = onOpenChild, moreText = "پرونده کودک")
        CareCard(modifier = Modifier.fillMaxWidth().testTag("care_today_card")) {
          Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)) {
            CareTodayRow(
              icon = Icons.Rounded.Medication,
              color = CareCoral,
              title = "دارو و نحوه مصرف",
              detail = "اطلاعات داروها و ثبت مصرف را در بخش داروها ببینید.",
              onClick = onMedicines,
              testTag = "home_menu_medicines",
            )
            androidx.compose.material3.HorizontalDivider(color = p.outline)
            CareTodayRow(
              icon = Icons.Rounded.CalendarMonth,
              color = CareBlue,
              title = "واکسن و پیگیری",
              detail = vaccineBadgeLabelFa.ifBlank { "برای مشاهده نوبت‌ها، سابقه واکسیناسیون را تکمیل کنید." },
              onClick = onVaccines,
              testTag = "home_tool_vaccines",
            )
            if (previousCare != null) {
              androidx.compose.material3.HorizontalDivider(color = p.outline)
              CareTodayRow(
                icon = Icons.Rounded.HealthAndSafety,
                color = CareForest,
                title = "ادامه مراقبت ثبت‌شده",
                detail = "به آخرین مراقبت مستند بازگردید.",
                onClick = { onContinueCare(previousCare) },
                testTag = "home_followup_chip",
              )
            }
            if (onOpenCareCards != null) {
              androidx.compose.material3.HorizontalDivider(color = p.outline)
              CareTodayRow(
                icon = Icons.Rounded.Info,
                color = CareGold,
                title = "کارت مراقبت داروخانه",
                detail = "مراقبت ثبت‌شده توسط داروخانه را مشاهده کنید.",
                onClick = onOpenCareCards,
                testTag = "home_care_card_link",
              )
            }
          }
        }
      }
    }
    item {
      Column(modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CareSectionTitle("ابزارهای کودک", onMore = onOpenGlobalSearch, moreText = "جست‌وجوی ابزارها")
        Row(modifier = Modifier.fillMaxWidth().testTag("home_specialized_strip"), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
          CareCompactTool(
            text = "رشد کودک",
            icon = Icons.Rounded.AutoGraph,
            accent = CareForest,
            tint = Color(0xFFE9F4E9),
            tag = "home_menu_growth",
            modifier = Modifier.weight(1f),
            onClick = onGrowth,
          )
          CareCompactTool(
            text = "واکسن‌ها",
            icon = Icons.Rounded.Vaccines,
            accent = CareBlue,
            tint = Color(0xFFE9F3FC),
            tag = "care_quick_vaccines",
            modifier = Modifier.weight(1f),
            onClick = onVaccines,
          )
          CareCompactTool(
            text = "بانک دارویی",
            icon = Icons.Rounded.Medication,
            accent = CareCoral,
            tint = Color(0xFFFBEFEA),
            tag = "care_quick_drugs",
            modifier = Modifier.weight(1f),
            onClick = onMedicines,
          )
        }
      }
    }
    item {
      CareCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp).testTag("home_safety_notice"),
        tinted = true,
        onClick = onCheckSymptoms,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(15.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(23.dp), tint = p.accent)
          Column(modifier = Modifier.weight(1f)) {
            Text("راهنمای ایمنی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = p.ink)
            Text(
              "در علائم شدید یا خطرناک، ارزیابی پزشکی فوری را به تأخیر نیندازید.",
              style = MaterialTheme.typography.bodySmall,
              color = p.muted,
            )
          }
        }
      }
    }
    item { Spacer(modifier = Modifier.height(8.dp)) }
  }
}

@Composable
private fun CareTodayRow(
  icon: ImageVector,
  color: Color,
  title: String,
  detail: String,
  testTag: String,
  onClick: () -> Unit,
) {
  val p = careVisuals()
  Surface(
    onClick = onClick,
    color = Color.Transparent,
    modifier = Modifier.fillMaxWidth().testTag(testTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      CareIcon(icon, color, p.paleAccent)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = p.ink)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
      Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = p.muted, modifier = Modifier.size(19.dp))
    }
  }
}
