package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleanHands
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AquaSurface
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.DarkTextMuted
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.MinimalGold
import com.example.ui.theme.MinimalGoldDark
import com.example.ui.theme.MinimalMint
import com.example.ui.theme.MinimalMintDark
import com.example.ui.theme.MinimalRose
import com.example.ui.theme.MinimalRoseDark
import com.example.ui.theme.MinimalSky
import com.example.ui.theme.MinimalSkyDark
import com.example.ui.theme.MinimalTeal
import com.example.ui.theme.MinimalTealDark
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftMintCard
import com.example.ui.theme.TealGlow

data class RecommendedChildCareProduct(
  val id: String,
  val titleFa: String,
  val subtitleFa: String,
  val categoryTagFa: String,
  val ageHintFa: String,
  val pharmacistCategoryId: String,
  val icon: ImageVector,
  val accentRole: TopicAccentRole,
  val selectionTipsFa: List<String>,
)

val RECOMMENDED_SUPPLEMENTS_HYGIENE_PRODUCTS: List<RecommendedChildCareProduct> =
  listOf(
    RecommendedChildCareProduct(
      id = "rec_vit_d3",
      titleFa = "قطره ویتامین A+D یا D3 کودک",
      subtitleFa = "حمایت از رشد استخوان و ایمنی • بدون افزودنی رنگی",
      categoryTagFa = "مکمل پایه",
      ageHintFa = "از ۱۵ روزگی",
      pharmacistCategoryId = "supplement",
      icon = Icons.Rounded.WbSunny,
      accentRole = TopicAccentRole.GOLD,
      selectionTipsFa = listOf(
        "فرمولاسیون استاندارد با قطره‌چکان مدرج انتخاب کنید تا دوز روزانه (معمولاً ۴۰۰ واحد ویتامین D) دقیق باشد.",
        "در صورت مصرف شیرخشک غنی‌شده یا مولتی‌ویتامین هم‌زمان، میزان کل ویتامین A و D را با داروساز تطبیق دهید.",
      ),
    ),
    RecommendedChildCareProduct(
      id = "rec_iron_drops",
      titleFa = "قطره آهن ملایم (لیپوزومال / سوکروزومیال)",
      subtitleFa = "پیشگیری از کم‌خونی با کمترین لک دندانی و عارضه گوارشی",
      categoryTagFa = "مکمل آهن",
      ageHintFa = "۶ تا ۲۴ ماه",
      pharmacistCategoryId = "supplement",
      icon = Icons.Rounded.WaterDrop,
      accentRole = TopicAccentRole.CORAL,
      selectionTipsFa = listOf(
        "مقدار آهن المنتال در هر میلی‌لیتر روی برچسب هر برند متفاوت است؛ پیش از شروع از داروساز استعلام بگیرید.",
        "قطره را در عقب زبان بچکانید و بلافاصله چند قاشق آب بدهید و با فاصله از لبنیات مصرف شود.",
      ),
    ),
    RecommendedChildCareProduct(
      id = "rec_probiotic",
      titleFa = "قطره پروبیوتیک شیرخوار و کودک",
      subtitleFa = "کمک به تعادل فلور روده، آرامش کولیک و تقویت گوارش",
      categoryTagFa = "گوارش کودک",
      ageHintFa = "نوزادی تا ۶ سال",
      pharmacistCategoryId = "supplement",
      icon = Icons.Rounded.ChildCare,
      accentRole = TopicAccentRole.SKY,
      selectionTipsFa = listOf(
        "به شرایط نگهداری (یخچالی یا دمای اتاق زیر ۲۵ درجه) و مهلت مصرف پس از باز شدن بطری دقت کنید.",
        "قطره را داخل شیر یا غذای داغ نریزید تا باکتری‌های مفید زنده بمانند.",
      ),
    ),
    RecommendedChildCareProduct(
      id = "rec_zinc_cream",
      titleFa = "کرم محافظ پای کودک (زینک اکساید بدون عطر)",
      subtitleFa = "ایجاد لایه محافظت‌کننده و پیشگیری از التهاب و سوختگی پوشک",
      categoryTagFa = "بهداشت پوست",
      ageHintFa = "بدو تولد+",
      pharmacistCategoryId = "skin",
      icon = Icons.Rounded.Shield,
      accentRole = TopicAccentRole.SAGE,
      selectionTipsFa = listOf(
        "محصول فاقد اسانس، الکل و پارابن با درصد مناسب اکسید روی انتخاب کنید.",
        "در صورت قرمزی شدید یا دانه‌های حاشیه‌دار بیش از ۳ روز، برای بررسی قارچ با داروساز یا پزشک مشورت کنید.",
      ),
    ),
    RecommendedChildCareProduct(
      id = "rec_gentle_wash",
      titleFa = "شامپو و شوینده ملایم سر و بدن کودک",
      subtitleFa = "فرمول بدون سولفات و بدون اشک • سازگار با پوست خشک و حساس",
      categoryTagFa = "حمام و مو",
      ageHintFa = "نوزادی تا ۶ سال",
      pharmacistCategoryId = "scalp_hair",
      icon = Icons.Rounded.Spa,
      accentRole = TopicAccentRole.TEAL,
      selectionTipsFa = listOf(
        "شوینده‌های سندت (Syndet) با pH متعادل پوست کودک از خشکی و خارش جلوگیری می‌کنند.",
        "بلافاصله پس از حمام کوتاه با آب ولرم، از مرطوب‌کننده ساده و بدون عطر استفاده نمایید.",
      ),
    ),
    RecommendedChildCareProduct(
      id = "rec_oral_care",
      titleFa = "مسواک انگشتی و خمیردندان فلورایددار کودک",
      subtitleFa = "شروع مراقبت از اولین دندان شیری و پیشگیری از پوسیدگی",
      categoryTagFa = "دهان و دندان",
      ageHintFa = "از اولین دندان",
      pharmacistCategoryId = "supplement",
      icon = Icons.Rounded.CleanHands,
      accentRole = TopicAccentRole.PLUM,
      selectionTipsFa = listOf(
        "تا ۳ سالگی مقدار خمیردندان به‌اندازه «یک دانه برنج» و از ۳ تا ۶ سالگی به‌اندازه «یک نخود کوچک» کافی است.",
        "شیشه‌شیر شبانه هنگام خواب مهم‌ترین عامل پوسیدگی دندان‌های شیری است.",
      ),
    ),
  )

/**
 * Prominent Material 3 card replacing legacy lab modules:
 * Title: "مکمل‌ها و بهداشت کودک"
 * Content: Icon-driven list of recommended pediatric products + dedicated "پرسش از داروساز" CTA button.
 */
@Composable
fun SupplementsHygieneRecommendedCard(
  onAskPharmacist: (categoryId: String) -> Unit,
  onOpenAllGuides: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  var selectedProduct by remember { mutableStateOf<RecommendedChildCareProduct?>(null) }

  val cardBg = if (isDark) DarkSurface else PureWhite
  val headerBrush =
    if (isDark) {
      Brush.horizontalGradient(listOf(DarkSurfaceHigh, DarkSurface))
    } else {
      Brush.horizontalGradient(listOf(SoftMintCard, AquaSurface.copy(alpha = 0.65f)))
    }
  val borderColor = if (isDark) TealGlow.copy(alpha = 0.34f) else DeepTeal.copy(alpha = 0.24f)
  val titleColor = if (isDark) DarkTextPrimary else MaterialTheme.colorScheme.onSurface
  val mutedColor = if (isDark) DarkTextMuted else MutedSlate
  val rowDividerColor = if (isDark) DarkBorder.copy(alpha = 0.65f) else HairlineBorder.copy(alpha = 0.85f)
  val primaryAccent = if (isDark) MinimalTealDark else DeepTeal

  Surface(
    color = cardBg,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.2.dp, borderColor),
    shadowElevation = if (isDark) 2.dp else 4.dp,
    modifier = modifier.fillMaxWidth().testTag("supplements_hygiene_recommended_card"),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Header Banner
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(headerBrush)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(primaryAccent.copy(alpha = if (isDark) 0.18f else 0.14f))
              .border(1.dp, primaryAccent.copy(alpha = 0.38f), RoundedCornerShape(14.dp)),
        ) {
          Icon(
            imageVector = Icons.Rounded.Spa,
            contentDescription = null,
            tint = primaryAccent,
            modifier = Modifier.size(22.dp),
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Text(
              text = "مکمل‌ها و بهداشت کودک",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = titleColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Icon(
              imageVector = Icons.Rounded.Verified,
              contentDescription = "تأیید داروساز",
              tint = if (isDark) MinimalGoldDark else MinimalGold,
              modifier = Modifier.size(16.dp),
            )
          }
          Text(
            text = "محصولات پیشنهادی و استاندارد داروخانه‌ای • لمس برای راهنمای انتخاب",
            style = MaterialTheme.typography.labelSmall,
            color = mutedColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (onOpenAllGuides != null) {
          TextButton(
            onClick = onOpenAllGuides,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("recommended_card_view_all_btn"),
          ) {
            Text(
              text = "همه راهنماها",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = primaryAccent,
            )
          }
        }
      }

      HorizontalDivider(color = rowDividerColor)

      // Icon-Driven List of Recommended Products
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        RECOMMENDED_SUPPLEMENTS_HYGIENE_PRODUCTS.forEachIndexed { index, product ->
          val itemAccent =
            when (product.accentRole) {
              TopicAccentRole.GOLD -> if (isDark) MinimalGoldDark else MinimalGold
              TopicAccentRole.CORAL -> if (isDark) MinimalRoseDark else MinimalRose
              TopicAccentRole.SKY -> if (isDark) MinimalSkyDark else MinimalSky
              TopicAccentRole.SAGE -> if (isDark) MinimalMintDark else MinimalMint
              TopicAccentRole.TEAL -> if (isDark) MinimalTealDark else MinimalTeal
              else -> product.accentRole.resolveAccentColor()
            }

          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { selectedProduct = product }
                .minimumInteractiveComponentSize()
                .padding(horizontal = 6.dp, vertical = 8.dp)
                .testTag("recommended_product_${product.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(itemAccent.copy(alpha = if (isDark) 0.16f else 0.13f))
                  .border(1.dp, itemAccent.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
            ) {
              Icon(
                imageVector = product.icon,
                contentDescription = product.titleFa,
                tint = itemAccent,
                modifier = Modifier.size(20.dp),
              )
            }

            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = product.titleFa,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = titleColor,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false),
                )
              }
              Text(
                text = product.subtitleFa,
                style = MaterialTheme.typography.labelSmall,
                color = mutedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Surface(
              color = itemAccent.copy(alpha = if (isDark) 0.16f else 0.11f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = product.ageHintFa,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = itemAccent,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              )
            }

            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
              contentDescription = null,
              tint = mutedColor.copy(alpha = 0.7f),
              modifier = Modifier.size(18.dp),
            )
          }

          if (index < RECOMMENDED_SUPPLEMENTS_HYGIENE_PRODUCTS.lastIndex) {
            HorizontalDivider(
              color = rowDividerColor.copy(alpha = 0.55f),
              modifier = Modifier.padding(horizontal = 6.dp),
            )
          }
        }
      }

      // Dedicated Primary "پرسش از داروساز" Action Button
      Box(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp),
      ) {
        Button(
          onClick = { onAskPharmacist("supplement") },
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = primaryAccent,
              contentColor = if (isDark) DarkSurface else PureWhite,
            ),
          modifier =
            Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("recommended_card_ask_pharmacist_btn"),
        ) {
          Icon(
            imageVector = Icons.Rounded.LocalPharmacy,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = "پرسش از داروساز",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }
    }
  }

  // Product Selection Tips Bottom Sheet when tapping any recommended item
  selectedProduct?.let { product ->
    val sheetAccent = product.accentRole.resolveAccentColor()
    ManoFlowDetailBottomSheet(
      titleFa = product.titleFa,
      subtitleFa = "${product.categoryTagFa} • ${product.ageHintFa}",
      icon = product.icon,
      accentColor = sheetAccent,
      onDismiss = { selectedProduct = null },
    ) {
      Text(
        text = product.subtitleFa,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      ClinicalBulletList(
        items = product.selectionTipsFa,
        bulletColor = sheetAccent,
      )
      Button(
        onClick = {
          val category = product.pharmacistCategoryId
          selectedProduct = null
          onAskPharmacist(category)
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
      ) {
        Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("پرسش از داروساز درباره این محصول")
      }
    }
  }
}
