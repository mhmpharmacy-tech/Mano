package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleanHands
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalManoDarkTheme
import com.example.ui.theme.ManoTheme

enum class ChildCareProductGroup(
  val id: String,
  val labelFa: String,
  val countLabelFa: String,
  val duoArt: DuoProductArt,
  val defaultPharmacistCategory: String,
) {
  SUPPLEMENTS(
    id = "supplements",
    labelFa = "مکمل‌ها",
    countLabelFa = "۳ مورد",
    duoArt = DuoProductArt.TAB_SUPPLEMENTS,
    defaultPharmacistCategory = "supplement",
  ),
  HYGIENE(
    id = "hygiene",
    labelFa = "بهداشت",
    countLabelFa = "۳ مورد",
    duoArt = DuoProductArt.TAB_HYGIENE,
    defaultPharmacistCategory = "skin",
  ),
}

data class RecommendedChildCareProduct(
  val id: String,
  val group: ChildCareProductGroup,
  val titleFa: String,
  val subtitleFa: String,
  val categoryTagFa: String,
  val ageHintFa: String,
  val pharmacistCategoryId: String,
  val icon: ImageVector,
  val duoArt: DuoProductArt,
  val accentRole: TopicAccentRole,
  val selectionTipsFa: List<String>,
)

val RECOMMENDED_SUPPLEMENTS_HYGIENE_PRODUCTS: List<RecommendedChildCareProduct> =
  listOf(
    RecommendedChildCareProduct(
      id = "rec_vit_d3",
      group = ChildCareProductGroup.SUPPLEMENTS,
      titleFa = "قطره ویتامین A+D یا D3 کودک",
      subtitleFa = "حمایت از رشد استخوان و ایمنی • بدون افزودنی رنگی",
      categoryTagFa = "مکمل پایه",
      ageHintFa = "از ۱۵ روزگی",
      pharmacistCategoryId = "supplement",
      icon = Icons.Rounded.WbSunny,
      duoArt = DuoProductArt.VITAMIN_D,
      accentRole = TopicAccentRole.GOLD,
      selectionTipsFa = listOf(
        "فرمولاسیون استاندارد با قطره‌چکان مدرج انتخاب کنید تا مقدار روزانه دقیق اندازه‌گیری شود.",
        "در صورت مصرف شیرخشک غنی‌شده یا مولتی‌ویتامین هم‌زمان، مجموع ویتامین A و D دریافتی را با داروساز تطبیق دهید.",
        "بطری را در جای خشک، خنک و دور از نور مستقیم آفتاب نگهداری کنید.",
      ),
    ),
    RecommendedChildCareProduct(
      id = "rec_iron_drops",
      group = ChildCareProductGroup.SUPPLEMENTS,
      titleFa = "قطره آهن ملایم (لیپوزومال / سوکروزومیال)",
      subtitleFa = "کمک به پیشگیری از کم‌خونی با کمترین لک دندانی",
      categoryTagFa = "مکمل آهن",
      ageHintFa = "۶ تا ۲۴ ماه",
      pharmacistCategoryId = "supplement",
      icon = Icons.Rounded.WaterDrop,
      duoArt = DuoProductArt.IRON_DROPS,
      accentRole = TopicAccentRole.CORAL,
      selectionTipsFa = listOf(
        "غلظت آهن المنتال در هر میلی‌لیتر روی برچسب هر فرآورده متفاوت است؛ پیش از شروع با داروساز بررسی کنید.",
        "قطره را در عقب زبان بچکانید، بلافاصله کمی آب بدهید و با فاصله ۱ تا ۲ ساعته از لبنیات مصرف شود.",
      ),
    ),
    RecommendedChildCareProduct(
      id = "rec_probiotic",
      group = ChildCareProductGroup.SUPPLEMENTS,
      titleFa = "قطره پروبیوتیک شیرخوار و کودک",
      subtitleFa = "کمک به تعادل فلور روده و آرامش گوارش",
      categoryTagFa = "گوارش کودک",
      ageHintFa = "نوزادی تا ۶ سال",
      pharmacistCategoryId = "supplement",
      icon = Icons.Rounded.ChildCare,
      duoArt = DuoProductArt.PROBIOTIC,
      accentRole = TopicAccentRole.SKY,
      selectionTipsFa = listOf(
        "به شرایط نگهداری (یخچالی یا دمای اتاق زیر ۲۵ درجه) و مهلت مصرف پس از باز شدن بطری دقت کنید.",
        "قطره را داخل شیر یا غذای داغ نریزید تا باکتری‌های مفید زنده بمانند.",
      ),
    ),
    RecommendedChildCareProduct(
      id = "rec_zinc_cream",
      group = ChildCareProductGroup.HYGIENE,
      titleFa = "کرم محافظ پای کودک (زینک اکساید بدون عطر)",
      subtitleFa = "لایه محافظت‌کننده پوست در برابر رطوبت و التهاب پوشک",
      categoryTagFa = "بهداشت پوست",
      ageHintFa = "بدو تولد+",
      pharmacistCategoryId = "skin",
      icon = Icons.Rounded.Shield,
      duoArt = DuoProductArt.ZINC_CREAM,
      accentRole = TopicAccentRole.SAGE,
      selectionTipsFa = listOf(
        "محصول فاقد اسانس، الکل و پارابن را روی پوست کاملاً خشک به‌صورت لایه نازک استفاده کنید.",
        "در صورت قرمزی شدید یا دانه‌های حاشیه‌دار بیش از ۳ روز، برای بررسی قارچ با داروساز یا پزشک مشورت کنید.",
      ),
    ),
    RecommendedChildCareProduct(
      id = "rec_gentle_wash",
      group = ChildCareProductGroup.HYGIENE,
      titleFa = "شامپو و شوینده ملایم سر و بدن کودک",
      subtitleFa = "فرمول بدون سولفات و بدون اشک • سازگار با پوست حساس",
      categoryTagFa = "حمام و مو",
      ageHintFa = "نوزادی تا ۶ سال",
      pharmacistCategoryId = "scalp_hair",
      icon = Icons.Rounded.Spa,
      duoArt = DuoProductArt.GENTLE_WASH,
      accentRole = TopicAccentRole.TEAL,
      selectionTipsFa = listOf(
        "شوینده‌های سندت (Syndet) با pH متعادل پوست کودک از خشکی و خارش جلوگیری می‌کنند.",
        "بلافاصله پس از حمام کوتاه با آب ولرم، از مرطوب‌کننده ساده و بدون عطر استفاده نمایید.",
      ),
    ),
    RecommendedChildCareProduct(
      id = "rec_oral_care",
      group = ChildCareProductGroup.HYGIENE,
      titleFa = "مسواک انگشتی و خمیردندان فلورایددار کودک",
      subtitleFa = "مراقبت از اولین دندان شیری و پیشگیری از پوسیدگی",
      categoryTagFa = "دهان و دندان",
      ageHintFa = "از اولین دندان",
      pharmacistCategoryId = "otc",
      icon = Icons.Rounded.CleanHands,
      duoArt = DuoProductArt.ORAL_CARE,
      accentRole = TopicAccentRole.PLUM,
      selectionTipsFa = listOf(
        "تا ۳ سالگی مقدار خمیردندان به‌اندازه «یک دانه برنج» و از ۳ تا ۶ سالگی به‌اندازه «یک نخود کوچک» کافی است.",
        "شیشه‌شیر شبانه هنگام خواب مهم‌ترین عامل پوسیدگی دندان‌های شیری است.",
      ),
    ),
  )

private data class CardCarePalette(
  val surface: Color,
  val headerTintTop: Color,
  val headerTintBottom: Color,
  val segmentedTrack: Color,
  val segmentedActiveBg: Color,
  val border: Color,
  val divider: Color,
  val ink: Color,
  val muted: Color,
  val forest: Color,
  val onForest: Color,
  val sageChip: Color,
  val pillBg: Color,
)

@Composable
private fun rememberCardCarePalette(): CardCarePalette {
  val colors = ManoTheme.colors
  return CardCarePalette(
    surface = colors.card,
    headerTintTop = if (colors.isDark) colors.elevated else colors.cyan.container,
    headerTintBottom = if (colors.isDark) colors.card else colors.cream.container,
    segmentedTrack = colors.track,
    segmentedActiveBg = if (colors.isDark) colors.elevated else colors.card,
    border = colors.border,
    divider = colors.divider,
    ink = colors.ink,
    muted = colors.muted,
    forest = colors.cyan.accent,
    onForest = colors.onPrimary,
    sageChip = if (colors.isDark) colors.elevated else colors.cyan.container,
    pillBg = colors.track,
  )
}

@Composable
private fun productPillColors(group: ChildCareProductGroup, isDark: Boolean): Pair<Color, Color> {
  val colors = ManoTheme.colors
  return when (group) {
    ChildCareProductGroup.SUPPLEMENTS -> colors.gold.container to colors.gold.accent
    ChildCareProductGroup.HYGIENE -> colors.cyan.container to colors.cyan.accent
  }
}

/**
 * Care Home v3 card: "مکمل‌ها و بهداشت کودک"
 * Categorized into Supplements (مکمل‌ها) & Hygiene (بهداشت), with custom duo-tone graphical icons,
 * clean rows, safe-guidance bottom sheet on tap, and dedicated "پرسش از داروساز" CTA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementsHygieneRecommendedCard(
  onAskPharmacist: (categoryId: String) -> Unit,
  onOpenAllGuides: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalManoDarkTheme.current
  val palette = rememberCardCarePalette()
  var activeGroupId by rememberSaveable { mutableStateOf(ChildCareProductGroup.SUPPLEMENTS.id) }
  var selectedProduct by remember { mutableStateOf<RecommendedChildCareProduct?>(null) }

  val activeGroup =
    ChildCareProductGroup.entries.firstOrNull { it.id == activeGroupId } ?: ChildCareProductGroup.SUPPLEMENTS
  val visibleProducts =
    RECOMMENDED_SUPPLEMENTS_HYGIENE_PRODUCTS.filter { it.group == activeGroup }

  Surface(
    color = palette.surface,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, palette.border),
    shadowElevation = 2.dp,
    modifier = modifier.fillMaxWidth().testTag("supplements_hygiene_recommended_card"),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // 1. Calm Care Home Header Banner with Duo-Tone Emblem
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(palette.headerTintTop, palette.headerTintBottom)))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(palette.surface)
              .border(1.dp, palette.forest.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_brand_supplements),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)),
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
              color = palette.ink,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
              imageVector = Icons.Rounded.Verified,
              contentDescription = "راهنمای علمی داروخانه‌ای",
              tint = ManoTheme.colors.gold.accent,
              modifier = Modifier.size(16.dp),
            )
          }
          Text(
            text = "دسته‌بندی ایمن روزمره • برای نکات انتخاب لمس کنید",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
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
              color = palette.forest,
              maxLines = 1,
            )
          }
        }
      }

      HorizontalDivider(color = palette.divider)

      // 2. Categorized Segmented Switcher: مکمل‌ها | بهداشت
      Surface(
        color = palette.segmentedTrack,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.divider),
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(start = 13.dp, end = 13.dp, top = 12.dp, bottom = 6.dp)
            .testTag("recommended_category_tabs"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ChildCareProductGroup.entries.forEach { group ->
            val isSelected = group == activeGroup
            Surface(
              onClick = { activeGroupId = group.id },
              color = if (isSelected) palette.segmentedActiveBg else Color.Transparent,
              shape = RoundedCornerShape(13.dp),
              border = if (isSelected) BorderStroke(1.dp, palette.border) else null,
              shadowElevation = if (isSelected) 1.dp else 0.dp,
              modifier =
                Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .semantics { selected = isSelected }
                  .testTag("recommended_tab_${group.id}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                val tabDrawable =
                  if (group == ChildCareProductGroup.SUPPLEMENTS) R.drawable.ic_brand_supplements
                  else R.drawable.ic_brand_skin
                Image(
                  painter = painterResource(id = tabDrawable),
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                  text = group.labelFa,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                  color = if (isSelected) palette.forest else palette.muted,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(5.dp))
                Surface(
                  color = if (isSelected) palette.sageChip else palette.pillBg,
                  shape = RoundedCornerShape(50),
                ) {
                  Text(
                    text = group.countLabelFa,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) palette.forest else palette.muted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }
      }

      // 3. Organized Rows for Active Category
      AnimatedContent(
        targetState = visibleProducts,
        transitionSpec = {
          fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(130))
        },
        label = "recommended_group_rows",
      ) { products ->
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
          products.forEachIndexed { index, product ->
            val (ageBg, ageInk) = productPillColors(product.group, isDark)
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { selectedProduct = product }
                  .minimumInteractiveComponentSize()
                  .padding(horizontal = 6.dp, vertical = 10.dp)
                  .testTag("recommended_product_${product.id}"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
              SupplementsHygieneDuoIcon(
                art = product.duoArt,
                size = 46.dp,
                contentDescription = product.titleFa,
              )

              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Text(
                  text = product.titleFa,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = palette.ink,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = product.subtitleFa,
                  style = MaterialTheme.typography.bodySmall,
                  color = palette.muted,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Surface(
                  color = ageBg,
                  shape = RoundedCornerShape(8.dp),
                ) {
                  Text(
                    text = product.ageHintFa,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ageInk,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    maxLines = 1,
                  )
                }
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = null,
                  tint = palette.muted.copy(alpha = 0.75f),
                  modifier = Modifier.size(17.dp),
                )
              }
            }

            if (index < products.lastIndex) {
              HorizontalDivider(
                color = palette.divider,
                modifier = Modifier.padding(horizontal = 6.dp),
              )
            }
          }
        }
      }

      // 4. Dedicated "پرسش از داروساز" CTA Button
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 14.dp),
      ) {
        Button(
          onClick = { onAskPharmacist(activeGroup.defaultPharmacistCategory) },
          shape = RoundedCornerShape(15.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = palette.forest,
              contentColor = palette.onForest,
            ),
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 50.dp)
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

  // 5. Safe Selection Tips Bottom Sheet on Product Tap
  selectedProduct?.let { product ->
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (ageBg, ageInk) = productPillColors(product.group, isDark)
    ModalBottomSheet(
      onDismissRequest = { selectedProduct = null },
      sheetState = sheetState,
      containerColor = palette.surface,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState())
            .testTag("recommended_product_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
          SupplementsHygieneDuoIcon(
            art = product.duoArt,
            size = 52.dp,
            contentDescription = product.titleFa,
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = product.titleFa,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = palette.ink,
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(color = palette.sageChip, shape = RoundedCornerShape(8.dp)) {
                Text(
                  text = product.categoryTagFa,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = palette.forest,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
              }
              Surface(color = ageBg, shape = RoundedCornerShape(8.dp)) {
                Text(
                  text = product.ageHintFa,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = ageInk,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
              }
            }
          }
          IconButton(
            onClick = { selectedProduct = null },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("recommended_sheet_close_btn"),
          ) {
            Icon(Icons.Rounded.Close, contentDescription = "بستن", tint = palette.muted)
          }
        }

        Text(
          text = product.subtitleFa,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = palette.ink,
        )

        Surface(
          color = palette.pillBg,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, palette.divider),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "نکات ایمن انتخاب و مصرف",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = palette.forest,
            )
            ClinicalBulletList(
              items = product.selectionTipsFa,
              bulletColor = palette.forest,
            )
          }
        }

        Surface(
          color = palette.sageChip.copy(alpha = 0.72f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, palette.border),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Info,
              contentDescription = null,
              tint = palette.forest,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "برند تجاری یا دوز اختصاصی در این کارت تعیین نمی‌شود؛ پیش از شروع یا ترکیب مکمل‌ها با داروساز مشورت کنید.",
              style = MaterialTheme.typography.labelSmall,
              color = palette.muted,
            )
          }
        }

        Button(
          onClick = {
            val category = product.pharmacistCategoryId
            selectedProduct = null
            onAskPharmacist(category)
          },
          shape = RoundedCornerShape(15.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = palette.forest,
              contentColor = palette.onForest,
            ),
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 50.dp)
              .minimumInteractiveComponentSize()
              .testTag("recommended_sheet_ask_pharmacist_btn"),
        ) {
          Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, modifier = Modifier.size(19.dp))
          Spacer(Modifier.width(8.dp))
          Text(
            text = "پرسش از داروساز درباره این محصول",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
          )
        }

        Spacer(Modifier.height(14.dp))
      }
    }
  }
}
