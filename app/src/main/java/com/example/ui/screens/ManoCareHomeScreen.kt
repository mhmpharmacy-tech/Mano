package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.PreviousCare
import com.example.ui.components.ManoKidWordmark
import com.example.ui.components.SupplementsHygieneRecommendedCard
import com.example.ui.theme.DeepForest
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.SoftSage
import com.example.ui.theme.WarmSand

/**
 * Care Home v4 — Project Care Loop | Warm Clinical Futurism.
 *
 * Uses Warm Ivory (#F8F7F2), Deep Forest (#174F46), Soft Sage (#E2EDE6),
 * Orbit Cyan (#A7E1DF), Soft Lavender (#E6E3F2) and synchronized Dark Mode tokens,
 * pairing the floating transparent spherical Orbit MANO robot with three calm
 * family intentions and unconditional access to the MANO Care Card experience.
 */
private data class CareVisuals(
  val background: Color,
  val surface: Color,
  val ink: Color,
  val muted: Color,
  val outline: Color,
  val accent: Color,
  val forest: Color,
  val paleAccent: Color,
)

@Composable
private fun careVisuals(): CareVisuals {
  val colors = ManoTheme.colors
  return CareVisuals(
    background = colors.background,
    surface = colors.card,
    ink = colors.ink,
    muted = colors.muted,
    outline = colors.border,
    accent = colors.cyan.accent,
    forest = colors.sage.accent,
    paleAccent = colors.elevated,
  )
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
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = p.accent,
        )
      }
    }
  }
}

@Composable
private fun CareBrandIconFrame(
  @DrawableRes resId: Int,
  accentColor: Color,
  surfaceColor: Color,
  size: Dp = 44.dp,
  cornerRadius: Dp = 13.dp,
) {
  val isDark = LocalManoDarkTheme.current
  val shape = RoundedCornerShape(cornerRadius)
  Box(
    modifier =
      Modifier
        .size(size)
        .clip(shape)
        .background(surfaceColor)
        .border(1.dp, accentColor.copy(alpha = if (isDark) 0.38f else 0.22f), shape),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = resId),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize().clip(shape),
    )
  }
}

@Composable
private fun CareIntentionCard(
  title: String,
  subtitle: String,
  badgeFa: String,
  @DrawableRes iconResId: Int,
  accent: Color,
  lightSurface: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val p = careVisuals()
  val dark = LocalManoDarkTheme.current
  val bg = if (dark) p.surface else lightSurface
  val frameTint = if (dark) accent.copy(alpha = 0.40f) else accent
  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().testTag(testTag),
    color = bg,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, if (dark) p.outline else accent.copy(alpha = 0.22f)),
    shadowElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(15.dp),
      horizontalArrangement = Arrangement.spacedBy(13.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CareBrandIconFrame(
        resId = iconResId,
        accentColor = frameTint,
        surfaceColor = if (dark) p.paleAccent else PureWhite,
        size = 48.dp,
        cornerRadius = 15.dp,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
          color = accent.copy(alpha = if (dark) 0.18f else 0.12f),
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = badgeFa,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (dark) accent else DeepForest,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
          )
        }
        Text(
          title,
          style = MaterialTheme.typography.titleMedium,
          color = p.ink,
          fontWeight = FontWeight.Bold,
        )
        Text(
          subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = p.muted,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = p.muted, modifier = Modifier.size(20.dp))
    }
  }
}

@Composable
private fun CareCompactTool(
  text: String,
  @DrawableRes iconResId: Int,
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
    border = BorderStroke(1.dp, if (dark) p.outline else accent.copy(alpha = 0.20f)),
    modifier = modifier.testTag(tag),
    shadowElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 11.dp, horizontal = 5.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      CareBrandIconFrame(
        resId = iconResId,
        accentColor = if (dark) p.accent else accent,
        surfaceColor = if (dark) p.paleAccent else PureWhite,
        size = 42.dp,
        cornerRadius = 13.dp,
      )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManoCareHomeScreen(
  childProfile: ChildProfileDemo?,
  vaccineBadgeLabelFa: String,
  previousCare: PreviousCare?,
  onContinueCare: (PreviousCare) -> Unit,
  onOpenCareCards: (() -> Unit)?,
  onOpenChild: () -> Unit,
  onCheckSymptoms: () -> Unit,
  onAskPharmacist: () -> Unit,
  onAskPharmacistCategory: (String) -> Unit = { onAskPharmacist() },
  onOpenSupplementsHygiene: () -> Unit = {},
  onMedicines: () -> Unit,
  onSyrupScanner: () -> Unit,
  onVaccines: () -> Unit,
  onGrowth: () -> Unit,
  onNutrition: () -> Unit,
  onFeverCalculator: () -> Unit,
  onOpenGlobalSearch: () -> Unit,
  scrollToTopTrigger: Int = 0,
) {
  val p = careVisuals()
  val colors = ManoTheme.colors
  val isDark = LocalManoDarkTheme.current
  val listState = rememberLazyListState()

  LaunchedEffect(scrollToTopTrigger) {
    if (scrollToTopTrigger > 0) listState.scrollToItem(0)
  }

  val infiniteFloat = rememberInfiniteTransition(label = "orbitHeroFloat")
  val heroFloatDp by infiniteFloat.animateFloat(
    initialValue = -3.5f,
    targetValue = 3.5f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "heroFloatOffset",
  )

  LazyColumn(
    state = listState,
    modifier = Modifier.fillMaxSize().background(p.background).testTag("home_list"),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
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
          "حلقه مراقبت کودک و داروساز • Warm Clinical Futurism",
          color = p.muted,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }

    item {
      Column(modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Flagship Transparent Orbit Hero
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("home_hero_card"),
          shape = RoundedCornerShape(26.dp),
          color = Color.Transparent,
          shadowElevation = 3.dp,
          border = BorderStroke(1.dp, if (isDark) p.outline else OrbitCyan.copy(alpha = 0.55f)),
        ) {
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(
                  Brush.linearGradient(
                    colors = listOf(colors.heroStart, colors.heroEnd),
                  )
                )
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
              Surface(
                color = if (isDark) p.paleAccent else SoftSage,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (isDark) p.outline else WarmSand),
              ) {
                Text(
                  text = "همراه دیجیتال داروخانه و خانواده",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isDark) OrbitCyan else DeepForest,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                )
              }
              Text(
                text = "سلام، من مانو هستم.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = p.ink,
              )
              Text(
                text = "کنار شما از لحظه دریافت دارو تا پیگیری آرام در خانه؛ جایگزین معاینه پزشکی نیست.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.muted,
              )
            }
            Box(
              modifier = Modifier.size(108.dp),
              contentAlignment = Alignment.Center,
            ) {
              Box(
                modifier =
                  Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                      Brush.radialGradient(
                        colors = listOf(
                          OrbitCyan.copy(alpha = if (isDark) 0.28f else 0.55f),
                          (if (isDark) colors.elevated else SoftSage).copy(alpha = 0.20f),
                          Color.Transparent,
                        )
                      )
                    )
              )
              Image(
                painter = painterResource(R.drawable.img_mano_orbit_hero),
                contentDescription = "ربات کروی سفید و فیروزه‌ای مانو کید",
                contentScale = ContentScale.Fit,
                modifier =
                  Modifier
                    .size(102.dp)
                    .offset(y = heroFloatDp.dp),
              )
            }
          }
        }

        // Child Profile / Zero-State Prompt
        CareCard(
          onClick = onOpenChild,
          tinted = childProfile == null,
          modifier = Modifier.fillMaxWidth().testTag("care_child_profile"),
        ) {
          Row(
            modifier = Modifier.padding(15.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CareBrandIconFrame(
              resId = R.drawable.ic_brand_child_health,
              accentColor = p.accent,
              surfaceColor = if (isDark) p.paleAccent else PureWhite,
              size = 44.dp,
              cornerRadius = 13.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              if (childProfile == null) {
                Text(
                  "افزودن اطلاعات کودک",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = p.ink,
                  modifier = Modifier.testTag("home_add_child_prompt"),
                )
                Text(
                  "سن و وزن کودک را ثبت کنید تا دوز داروها، واکسن‌ها و غربالگری دقیق‌تر شود.",
                  style = MaterialTheme.typography.bodySmall,
                  color = p.muted,
                  maxLines = 2,
                )
              } else {
                Text(
                  childProfile.aliasFa.ifBlank { "پرونده کودک" },
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = p.ink,
                )
                val details = listOfNotNull(
                  childProfile.birthDateOrAgeFa.takeIf { it.isNotBlank() },
                  childProfile.weightKgText.takeIf { it.isNotBlank() }?.let { "وزن $it کیلوگرم" },
                ).joinToString(" • ")
                Text(
                  details.ifBlank { "نمایش پرونده و ویرایش مشخصات" },
                  style = MaterialTheme.typography.bodySmall,
                  color = p.muted,
                  maxLines = 2,
                )
              }
            }
            if (childProfile == null) {
              Surface(
                color = if (isDark) OrbitCyan else DeepForest,
                shape = RoundedCornerShape(50),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF0F1B1C) else PureWhite,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    "ثبت",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF0F1B1C) else PureWhite,
                  )
                }
              }
            } else {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = p.muted, modifier = Modifier.size(20.dp))
            }
          }
        }
      }
    }

    // 3 Primary Family Intentions
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        CareSectionTitle("چه کمکی نیاز دارید؟")
        CareIntentionCard(
          title = "کودکم حال خوبی ندارد",
          subtitle = "بررسی گام‌به‌گام علائم، شناسایی علائم هشدار و راهنمای زمان مراجعه",
          badgeFa = "اولویت ایمنی • غربالگری علائم",
          iconResId = R.drawable.ic_brand_fever,
          accent = colors.coral.accent,
          lightSurface = colors.coral.container,
          testTag = "home_menu_fever",
          onClick = onCheckSymptoms,
        )
        CareIntentionCard(
          title = "درباره دارو سؤال دارم",
          subtitle = "ارسال پرسش غیرفوری به داروساز یا مشاهده تداخل‌ها و غلظت داروها",
          badgeFa = "ارتباط با داروساز تاییدشده",
          iconResId = R.drawable.ic_brand_safety,
          accent = colors.cyan.accent,
          lightSurface = if (isDark) p.surface else PureWhite,
          testTag = "home_ask_pharmacist",
          onClick = onAskPharmacist,
        )
        if (previousCare != null) {
          CareIntentionCard(
            title = "ادامه مراقبت قبلی",
            subtitle = when (previousCare) {
              is PreviousCare.Temperature -> "بازگشت به ثبت دمای اخیر (${measurementNumberFa(previousCare.celsius)} درجه) و دفترچه تب"
              is PreviousCare.Growth -> "بازگشت به آخرین پایش رشد: ${previousCare.measurement.summaryFa()}"
            },
            badgeFa = "پیگیری فعال در خانه",
            iconResId = R.drawable.ic_brand_followup,
            accent = colors.sage.accent,
            lightSurface = if (isDark) p.surface else SoftSage,
            testTag = "home_continue_care",
            onClick = { onContinueCare(previousCare) },
          )
        }
        if (onOpenCareCards != null) {
          CareIntentionCard(
            title = "کارت مراقبت داروخانه (MANO Care Card)",
            subtitle = "مشاهده کارت ساختاریافته داروخانه، دستور مصرف، هشدارها و ثبت پیگیری خانواده",
            badgeFa = "حلقه مراقبت مانو • دسترسی آزاد",
            iconResId = R.drawable.ic_brand_illness,
            accent = if (isDark) OrbitCyan else DeepForest,
            lightSurface = if (isDark) p.surface else SoftLavender.copy(alpha = 0.72f),
            testTag = "home_care_card_link",
            onClick = onOpenCareCards,
          )
        }
      }
    }

    // Today's Care & Tools
    item {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        CareSectionTitle("مراقبت امروز", onMore = onOpenChild, moreText = "پرونده کودک")
        CareCard(modifier = Modifier.fillMaxWidth().testTag("care_today_card")) {
          Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)) {
            CareTodayRow(
              iconResId = R.drawable.ic_brand_medicines,
              color = colors.coral.accent,
              title = "دارو و دفترچه مصرف",
              detail = "مرور داروها، هشدار تداخل و ثبت ساعت مصرف در شبانه‌روز.",
              onClick = onMedicines,
              testTag = "home_menu_medicines",
            )
            androidx.compose.material3.HorizontalDivider(color = p.outline)
            CareTodayRow(
              iconResId = R.drawable.ic_brand_reminders,
              color = colors.sky.accent,
              title = "واکسن و پیگیری",
              detail = vaccineBadgeLabelFa.ifBlank { "برای مشاهده نوبت‌ها، سابقه واکسیناسیون را تکمیل کنید." },
              onClick = onVaccines,
              testTag = "home_tool_vaccines",
            )
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
            iconResId = R.drawable.ic_brand_family_health,
            accent = colors.sage.accent,
            tint = colors.sage.container,
            tag = "home_menu_growth",
            modifier = Modifier.weight(1f),
            onClick = onGrowth,
          )
          CareCompactTool(
            text = "واکسن‌ها",
            iconResId = R.drawable.ic_brand_vaccines,
            accent = colors.sky.accent,
            tint = colors.sky.container,
            tag = "care_quick_vaccines",
            modifier = Modifier.weight(1f),
            onClick = onVaccines,
          )
          CareCompactTool(
            text = "اسکنر شربت",
            iconResId = R.drawable.ic_brand_scanner,
            accent = colors.lavender.accent,
            tint = colors.lavender.container,
            tag = "home_menu_scanner",
            modifier = Modifier.weight(1f),
            onClick = onSyrupScanner,
          )
          CareCompactTool(
            text = "سرنگ و دوز",
            iconResId = R.drawable.ic_brand_labs,
            accent = colors.gold.accent,
            tint = colors.gold.container,
            tag = "home_fever_dose_quick_card",
            modifier = Modifier.weight(1f),
            onClick = onFeverCalculator,
          )
        }
      }
    }

    item {
      SupplementsHygieneRecommendedCard(
        onAskPharmacist = onAskPharmacistCategory,
        onOpenAllGuides = onOpenSupplementsHygiene,
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp),
      )
    }

    item {
      CareCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = 680.dp).testTag("home_safety_notice"),
        tinted = true,
        onClick = onCheckSymptoms,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(15.dp),
          horizontalArrangement = Arrangement.spacedBy(11.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.VerifiedUser,
            contentDescription = null,
            tint = colors.coral.accent,
            modifier = Modifier.size(26.dp),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text("هشدار ایمنی کودک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = p.ink)
            Text(
              "در نوزاد زیر ۳ ماه با تب ۳۸ درجه یا علائم شدید (تنگی نفس، بی‌حالی، تشنج)، فوراً به اورژانس مراجعه کنید.",
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
  @DrawableRes iconResId: Int,
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
      horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
      CareBrandIconFrame(
        resId = iconResId,
        accentColor = color,
        surfaceColor = p.paleAccent,
        size = 42.dp,
        cornerRadius = 12.dp,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = p.ink)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
      }
      Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = p.muted, modifier = Modifier.size(19.dp))
    }
  }
}
