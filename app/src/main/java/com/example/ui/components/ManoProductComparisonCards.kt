package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ManoSourcesAndClinicalSeed
import com.example.domain.model.ReviewStatus
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionSurface
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.DarkCoral
import com.example.ui.theme.DarkGold
import com.example.ui.theme.DarkSage
import com.example.ui.theme.SageCare
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.SuccessSurface
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface
import com.example.ui.theme.manoSemanticAccent
import com.example.ui.theme.manoTintedSurface

data class ScientificProductOption(
  val nameFa: String,
  val technicalSpecFa: String,
  val clinicalVerdictFa: String,
  val prosFa: List<String>,
  val cautionsFa: List<String>,
  val isGoldStandard: Boolean,
)

data class ScientificComparisonCategory(
  val id: String,
  val chipLabelFa: String,
  val titleFa: String,
  val summaryFa: String,
  val sourceCitationFa: String,
  val options: List<ScientificProductOption>,
)

object ScientificProductComparisonCatalog {
  val categories: List<ScientificComparisonCategory> =
    listOf(
      ScientificComparisonCategory(
        id = "dosing_tools",
        chipLabelFa = "ابزار اندازه‌گیری دوز",
        titleFa = "مقایسه علمی ابزارهای اندازه‌گیری داروی مایع کودک",
        summaryFa = "خطای حجمی قاشق‌های خانگی یکی از شایع‌ترین علل مسمومیت یا کم‌اثر شدن دارو در کودکان است.",
        sourceCitationFa = "AAP / FDA / ISMP — Safe Pediatric Liquid Medication Dosing",
        options =
          listOf(
            ScientificProductOption(
              nameFa = "سرنگ خوراکی مدرج به میلی‌لیتر (mL)",
              technicalSpecFa = "دقت ۰.۱ تا ۰.۲ میلی‌لیتر • استاندارد طلایی بیمارستانی و خانگی",
              clinicalVerdictFa = "انتخاب اول و ایمن‌ترین ابزار برای تمام شربت‌ها و سوسپانسیون‌ها",
              prosFa =
                listOf(
                  "خوانش دقیق خط زیرین منیسک مایع بر حسب mL",
                  "هدایت آرام مایع به سمت داخل گونه کودک (کاهش خطر پریدن در گلو)",
                ),
              cautionsFa =
                listOf(
                  "درمانگر یا والد باید کلاهک سرنگ را پیش از مصرف کاملاً جدا کند تا خطر بلع کلاهک وجود نداشته باشد.",
                ),
              isGoldStandard = true,
            ),
            ScientificProductOption(
              nameFa = "قطره‌چکان مدرج اختصاصی داخل جعبه",
              technicalSpecFa = "مخصوص قطره‌های غلیظ نوزادی (مثلاً استامینوفن ۱۰۰mg/mL یا قطره آهن)",
              clinicalVerdictFa = "مناسب فقط برای همان بطری دارو که قطره‌چکان داخل جعبه‌اش قرار دارد",
              prosFa =
                listOf(
                  "طراحی‌شده متناسب با غلظت و کشش سطحی همان قطره دارویی",
                ),
              cautionsFa =
                listOf(
                  "هرگز قطره‌چکان یک دارو را برای داروی دیگر یا به جای سرنگ شربت استفاده نکنید.",
                ),
              isGoldStandard = false,
            ),
            ScientificProductOption(
              nameFa = "قاشق چای‌خوری، مرباخوری یا غذاخوری خانگی",
              technicalSpecFa = "حجم غیراستاندارد (از ۲ تا ۹ میلی‌لیتر متغیر)",
              clinicalVerdictFa = "غیرمجاز برای داروی کودک — خطر خطای ۲ تا ۳ برابری دوز",
              prosFa = emptyList(),
              cautionsFa =
                listOf(
                  "به دلیل تفاوت اندازه قاشق‌های خانگی، احتمال اوردوز یا دوز ناکافی بسیار بالاست.",
                  "ریزش دارو از لبه قاشق هنگام حرکت کودک دقت مصرف را از بین می‌برد.",
                ),
              isGoldStandard = false,
            ),
          ),
      ),
      ScientificComparisonCategory(
        id = "antipyretic_forms",
        chipLabelFa = "غلظت تب‌برها",
        titleFa = "مقایسه علمی اشکال دارویی استامینوفن و ایبوپروفن کودکان",
        summaryFa = "جابه‌جایی حجم قطره و شربت بدون توجه به غلظت (mg/mL) خطرناک‌ترین خطای دارویی خانگی است.",
        sourceCitationFa = "AAP / FDA — Pediatric Antipyretic Concentration Safety",
        options =
          listOf(
            ScientificProductOption(
              nameFa = "شربت / سوسپانسیون استاندارد استامینوفن (120mg/5mL یا 160mg/5mL)",
              technicalSpecFa = "۲۴ تا ۳۲ میلی‌گرم در هر ۱ میلی‌لیتر (سنجش با سرنگ خوراکی mL)",
              clinicalVerdictFa = "فرم استاندارد کودکان با قابلیت تنظیم دقیق بر اساس وزن واقعی",
              prosFa =
                listOf(
                  "حجم ایمن‌تر و حاشیه خطای کمتر نسبت به قطره‌های فوق‌غلیظ",
                  "تطبیق آسان با سرنگ خوراکی ۵ میلی‌لیتری",
                ),
              cautionsFa =
                listOf(
                  "پیش از کشیدن دارو در سرنگ، تفاوت ۱۲۰mg/5mL و ۱۶۰mg/5mL روی جعبه را بررسی کنید.",
                ),
              isGoldStandard = true,
            ),
            ScientificProductOption(
              nameFa = "قطره خوراکی غلیظ استامینوفن (100mg/1mL)",
              technicalSpecFa = "۱۰۰ میلی‌گرم در هر ۱ میلی‌لیتر (حدود ۴ برابر غلیظ‌تر از شربت!)",
              clinicalVerdictFa = "صرفاً برای شیرخواران کوچک با حجم بسیار کم و با قطره‌چکان مدرج خودش",
              prosFa =
                listOf(
                  "نیاز به حجم مایع کمتر برای شیرخواران",
                ),
              cautionsFa =
                listOf(
                  "هرگز حجم شربت (مثلاً ۵ میلی‌لیتر) را با قطره ۱۰۰mg/mL به کودک ندهید؛ موجب مسمومیت شدید کبدی می‌شود.",
                ),
              isGoldStandard = false,
            ),
            ScientificProductOption(
              nameFa = "سوسپانسیون ایبوپروفن کودکان (100mg/5mL)",
              technicalSpecFa = "۲۰ میلی‌گرم در هر ۱ میلی‌لیتر • فاصله دوز ۶ تا ۸ ساعت",
              clinicalVerdictFa = "فقط برای کودکان بالای ۶ ماه و بدون کم‌آبی یا استفراغ مکرر",
              prosFa =
                listOf(
                  "اثر ضدالتهابی و کنترل تب طولانی‌تر در کودکان بالای ۶ ماه",
                ),
              cautionsFa =
                listOf(
                  "در نوزادان زیر ۶ ماه، کودک دچار کم‌آبی، اسهال/استفراغ شدید یا مشکلات کلیوی ممنوع است.",
                ),
              isGoldStandard = false,
            ),
          ),
      ),
      ScientificComparisonCategory(
        id = "hydration_ors",
        chipLabelFa = "آبرسانی و ORS",
        titleFa = "مقایسه علمی محلول‌های جبران آب و الکترولیت در اسهال و استفراغ",
        summaryFa = "تعادل سدیم و گلوکز در روده کودک تنها با اسمولاریته استاندارد قابل بازیابی ایمن است.",
        sourceCitationFa = "WHO / UNICEF — Low-Osmolarity Oral Rehydration Salts Guideline",
        options =
          listOf(
            ScientificProductOption(
              nameFa = "محلول استاندارد ORS کم‌اسمولاریته (WHO Formula)",
              technicalSpecFa = "اسمولاریته حدود ۲۴۵ mOsm/L • نسبت دقیق سدیم و گلوکز",
              clinicalVerdictFa = "استاندارد طلایی پیشگیری و درمان کم‌آبی خفیف تا متوسط کودکان",
              prosFa =
                listOf(
                  "جذب فعال آب و الکترولیت از دیواره روده حتی در حضور اسهال",
                  "کاهش نیاز به سرم‌تراپی وریدی در صورت مصرف جرعه‌جرعه و منظم",
                ),
              cautionsFa =
                listOf(
                  "پودر ORS باید دقیقاً در حجم آب جوشیده سردشده درج‌شده روی بسته حل شود (نه غلیظ‌تر و نه رقیق‌تر).",
                ),
              isGoldStandard = true,
            ),
            ScientificProductOption(
              nameFa = "آبمیوه‌های صنعتی، نوشابه یا محلول‌های خانگی پرشکر",
              technicalSpecFa = "اسمولاریته بسیار بالا (>600 mOsm/L) و سدیم ناکافی",
              clinicalVerdictFa = "نامناسب و تشدیدکننده اسهال اسموتیک در کودک",
              prosFa = emptyList(),
              cautionsFa =
                listOf(
                  "قند بالا آب را به داخل روده می‌کشد و اسهال و کم‌آبی کودک را تشدید می‌کند.",
                ),
              isGoldStandard = false,
            ),
          ),
      ),
      ScientificComparisonCategory(
        id = "skin_barrier",
        chipLabelFa = "محافظ پوست و پوشک",
        titleFa = "مقایسه علمی فرآورده‌های محافظ سد پوستی و تعویض پوشک",
        summaryFa = "پوست ناحیه پوشک شیرخوار نازک و نفوذپذیر است و نیاز به سد محافظ بی‌اثر و فاقد عطر دارد.",
        sourceCitationFa = "AAD / NICE — Neonatal & Infant Diaper Dermatitis Care",
        options =
          listOf(
            ScientificProductOption(
              nameFa = "خمیر/پماد زینک اکساید (Zinc Oxide) یا وازلین خالص دارویی",
              technicalSpecFa = "فاقد عطر، الکل و نگهدارنده‌های حساسیت‌زا • سد فیزیکی رطوبت",
              clinicalVerdictFa = "استاندارد علمی محافظت و ترمیم سوختگی ساده پوشک",
              prosFa =
                listOf(
                  "ایجاد لایه محافظ در برابر آنزیم‌های تحریک‌کننده ادرار و مدفوع",
                  "عدم جذب سیستمیک و ایمنی بالا در مصرف روزانه",
                ),
              cautionsFa =
                listOf(
                  "در ضایعات قارچی شدید (قرمزی چین‌ها با دانه‌های اقماری) یا عفونت باکتریایی، معاینه پزشک لازم است.",
                ),
              isGoldStandard = true,
            ),
            ScientificProductOption(
              nameFa = "پمادهای کورتونی قوی یا ترکیبی خودسرانه (مثل بتامتازون/کلوتریمازول)",
              technicalSpecFa = "جذب پوستی چندبرابری در ناحیه بسته پوشک",
              clinicalVerdictFa = "مصرف خودسرانه بدون نسخه پزشک اکیداً ممنوع است",
              prosFa = emptyList(),
              cautionsFa =
                listOf(
                  "نازک شدن پوست، جذب سیستمیک کورتون و تشدید عفونت‌های قارچی در محیط مرطوب پوشک.",
                ),
              isGoldStandard = false,
            ),
          ),
      ),
      ScientificComparisonCategory(
        id = "respiratory_devices",
        chipLabelFa = "وسایل تنفسی و بخور",
        titleFa = "مقایسه علمی وسایل مرطوب‌کننده هوا و داروهای استنشاقی کودک",
        summaryFa = "انتخاب وسیله مناسب تنفسی از سوختگی با بخار داغ و رسوب ناکافی دارو جلوگیری می‌کند.",
        sourceCitationFa = "AAP / GINA — Pediatric Aerosol & Humidifier Safety",
        options =
          listOf(
            ScientificProductOption(
              nameFa = "آسان‌نفس (Spacer) ماسک‌دار یا نبولایزر استاندارد با سرم نمکی",
              technicalSpecFa = "ذرات میکرونی هدفمند یا مرطوب‌سازی ایمن مجاری تنفسی",
              clinicalVerdictFa = "استاندارد علمی برای رساندن اسپری تجویزی یا نرم کردن ترشحات",
              prosFa =
                listOf(
                  "رسیدن دارو به ریه به جای رسوب در دهان و گلو (با اسپیسر متناسب با صورت کودک)",
                  "ایمنی کامل دمایی بدون خطر سوختگی",
                ),
              cautionsFa =
                listOf(
                  "مخزن نبولایزر و اسپیسر باید طبق دستورالعمل شست‌وشو و خشک شود تا آلودگی قارچی ایجاد نشود.",
                ),
              isGoldStandard = true,
            ),
            ScientificProductOption(
              nameFa = "دستگاه بخور داغ در اتاق کودک",
              technicalSpecFa = "تولید بخار با دمای جوش",
              clinicalVerdictFa = "توصیه نمی‌شود — خطر جدی سوختگی کودک با واژگونی دستگاه",
              prosFa = emptyList(),
              cautionsFa =
                listOf(
                  "آکادمی اطفال آمریکا (AAP) به دلیل موارد متعدد سوختگی شدید کودکان، بخور داغ را منع می‌کند.",
                ),
              isGoldStandard = false,
            ),
          ),
      ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScientificHealthProductComparisonSection(
  modifier: Modifier = Modifier,
) {
  val categories = remember { ScientificProductComparisonCatalog.categories }
  var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
  val activeCategory = categories[selectedIndex.coerceIn(categories.indices)]

  val sageAccent = manoSemanticAccent(SuccessEmerald, DarkSage)
  val cautionAccent = manoSemanticAccent(CautionAmber, DarkGold)
  val dangerAccent = manoSemanticAccent(DangerCrimson, DarkCoral)
  val cardSurface = manoCardSurface()

  Column(
    modifier = modifier.fillMaxWidth().testTag("scientific_product_comparison_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    SectionCard {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier =
              Modifier
                .width(4.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(50))
                .background(sageAccent)
          )
          Column {
            LtrText(text = "MANO Safety • Evidence Comparison", color = sageAccent)
            Text(
              text = "مقایسه علمی محصولات سلامت کودک (بدون تبلیغ تجاری)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
        SourceBadge(reviewStatus = ReviewStatus.APPROVED)
      }

      Text(
        text = "مقایسه بی‌طرفانه و مستند ابزارهای دارویی، اشکال تب‌بر، محلول‌های آبرسانی و وسایل مراقبتی بر پایه استانداردهای AAP، WHO و FDA (بدون معرفی برند تجاری).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        categories.forEachIndexed { idx, cat ->
          FilterChip(
            selected = selectedIndex == idx,
            onClick = { selectedIndex = idx },
            leadingIcon = {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text(cat.chipLabelFa, style = MaterialTheme.typography.labelMedium) },
            colors = FilterChipDefaults.filterChipColors(containerColor = cardSurface),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("comparison_cat_chip_${cat.id}"),
          )
        }
      }

      HorizontalDivider(color = manoBorderColor())

      Text(
        text = activeCategory.titleFa,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = activeCategory.summaryFa,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = sageAccent, modifier = Modifier.size(16.dp))
        Text(
          text = "مرجع علمی: ${activeCategory.sourceCitationFa} • ${ManoSourcesAndClinicalSeed.CLINICAL_REVIEWER_IDENTITY_FA}",
          style = MaterialTheme.typography.labelSmall,
          color = sageAccent,
        )
      }
    }

    activeCategory.options.forEachIndexed { idx, option ->
      val borderAccent = if (option.isGoldStandard) sageAccent else if (option.prosFa.isEmpty()) dangerAccent else cautionAccent
      val headerBg =
        when {
          option.isGoldStandard -> manoTintedSurface(SuccessSurface, DarkSage, lightAlpha = 0.72f, darkAlpha = 0.16f)
          option.prosFa.isEmpty() -> manoTintedSurface(DangerSurface, DarkCoral, lightAlpha = 0.78f, darkAlpha = 0.16f)
          else -> manoTintedSurface(CautionSurface, DarkGold, lightAlpha = 0.72f, darkAlpha = 0.16f)
        }

      SectionCard(modifier = Modifier.testTag("comparison_option_card_${activeCategory.id}_$idx")) {
        Surface(
          color = headerBg,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, borderAccent.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector =
                  when {
                    option.isGoldStandard -> Icons.Rounded.CheckCircle
                    option.prosFa.isEmpty() -> Icons.Rounded.WarningAmber
                    else -> Icons.Rounded.Shield
                  },
                contentDescription = null,
                tint = borderAccent,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = option.nameFa,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Text(
              text = option.technicalSpecFa,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "نتیجه علمی: ${option.clinicalVerdictFa}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = borderAccent,
            )
          }
        }

        if (option.prosFa.isNotEmpty()) {
          Text(
            text = "مزایای بالینی و ایمنی:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = sageAccent,
          )
          option.prosFa.forEach { pro ->
            Text("✓ $pro", style = MaterialTheme.typography.bodySmall)
          }
        }

        if (option.cautionsFa.isNotEmpty()) {
          Text(
            text = "هشدارها و محدودیت‌های ایمنی:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (option.prosFa.isEmpty()) dangerAccent else cautionAccent,
          )
          option.cautionsFa.forEach { warn ->
            Text("• $warn", style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}
