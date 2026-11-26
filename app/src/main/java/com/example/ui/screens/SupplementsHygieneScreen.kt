package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleanHands
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.util.PersianNormalizer
import com.example.ui.components.AlertSeverity
import com.example.ui.components.ClinicalBulletList
import com.example.ui.components.FlowchartDownConnector
import com.example.ui.components.ManoCompactScreenHeader
import com.example.ui.components.ManoFlowDetailBottomSheet
import com.example.ui.components.ManoFlowchartIconTile
import com.example.ui.components.PediatricIconKind
import com.example.ui.components.SeverityAlert
import com.example.ui.components.SupplementsHygieneRecommendedCard
import com.example.ui.components.TopicAccentRole
import com.example.ui.components.resolveAccentColor
import com.example.ui.theme.ManoTheme
import com.example.ui.theme.manoBorderColor
import com.example.ui.theme.manoCardSurface

private enum class CareGuideTab(val id: String, val labelFa: String) {
  SUPPLEMENTS("supplements", "مکمل‌های کودک"),
  HYGIENE("hygiene", "بهداشت و مراقبت"),
}

private enum class CareAgeFilter(val id: String, val labelFa: String) {
  ALL("all", "همه"),
  UNDER_6M("0_6m", "۰-۶ ماه"),
  MONTHS_6_24("6_24m", "۶-۲۴ ماه"),
  YEARS_2_6("2_6y", "۲-۶ سال"),
}

private data class SupplementOrHygieneGuide(
  val id: String,
  val tab: CareGuideTab,
  val shortTitleFa: String,
  val titleFa: String,
  val subtitleFa: String,
  val ageLabelFa: String,
  val ageFilters: Set<CareAgeFilter>,
  val icon: ImageVector,
  val iconKind: PediatricIconKind,
  val accentRole: TopicAccentRole,
  val howToUseTitleFa: String,
  val howToUsePointsFa: List<String>,
  val cautionPointsFa: List<String>,
  val sourceLabelFa: String,
)

private val CHILD_SUPPLEMENTS_AND_HYGIENE_GUIDES: List<SupplementOrHygieneGuide> =
  listOf(
    // --- SUPPLEMENTS ---
    SupplementOrHygieneGuide(
      id = "supp_vit_ad_d3",
      tab = CareGuideTab.SUPPLEMENTS,
      shortTitleFa = "ویتامین A+D / D3",
      titleFa = "قطره ویتامین A+D یا ویتامین D3",
      subtitleFa = "مکمل پایه رشد استخوانی و ایمنی نوزاد و کودک",
      ageLabelFa = "۱۵ روزگی+",
      ageFilters = setOf(CareAgeFilter.UNDER_6M, CareAgeFilter.MONTHS_6_24, CareAgeFilter.YEARS_2_6),
      icon = Icons.Rounded.WbSunny,
      iconKind = PediatricIconKind.SUPPLEMENTS,
      accentRole = TopicAccentRole.GOLD,
      howToUseTitleFa = "اصول مصرف ایمن و روزانه",
      howToUsePointsFa = listOf(
        "طبق برنامه کشوری مراقبت کودک، معمولاً از ۱۵ روزگی شروع می‌شود (نیاز روزانه رایج نوزادان ۴۰۰ واحد ویتامین D).",
        "بهتر است در زمان ثابت روز و همراه وعده شیر یا غذا داده شود تا جذب ویتامین‌های محلول در چربی بهتر باشد.",
        "پیش از انتخاب قطره A+D یا D3 ساده، برچسب غلظت هر میلی‌لیتر را با داروساز یا پزشک کودک تطبیق دهید.",
      ),
      cautionPointsFa = listOf(
        "اگر کودک شیرخشک غنی‌شده یا مولتی‌ویتامین مصرف می‌کند، برای جلوگیری از دریافت مضاعف ویتامین A و D حتماً با داروساز مشورت کنید.",
        "قطره را در جای خشک، خنک و دور از نور مستقیم آفتاب نگهداری کنید.",
      ),
      sourceLabelFa = "دستورالعمل کشوری سلامت کودک • AAP",
    ),
    SupplementOrHygieneGuide(
      id = "supp_iron",
      tab = CareGuideTab.SUPPLEMENTS,
      shortTitleFa = "قطره آهن کودک",
      titleFa = "قطره آهن کودک (پیشگیری از کم‌خونی)",
      subtitleFa = "روش صحیح مصرف و جلوگیری از تغییر رنگ دندان‌ها",
      ageLabelFa = "۶ تا ۲۴ ماه",
      ageFilters = setOf(CareAgeFilter.MONTHS_6_24, CareAgeFilter.YEARS_2_6),
      icon = Icons.Rounded.WaterDrop,
      iconKind = PediatricIconKind.SUPPLEMENTS,
      accentRole = TopicAccentRole.CORAL,
      howToUseTitleFa = "نحوه دادن قطره آهن بدون سیاه شدن دندان",
      howToUsePointsFa = listOf(
        "معمولاً هم‌زمان با شروع غذای کمکی (پایان ۶ ماهگی) آغاز می‌شود؛ در نوزادان نارس زمان شروع ممکن است زودتر و با نظر پزشک باشد.",
        "قطره را با قطره‌چکان در بخش عقب زبان بچکانید و بلافاصله چند قاشق آب یا کمی پوره میوه حاوی ویتامین C بدهید و دندان‌ها را با دستمال مرطوب یا مسواک نرم تمیز کنید.",
        "انواع ملایم‌تر (مانند فرم‌های لیپوزومال یا سوکروزومیال) معمولاً عارضه گوارشی و لک دندانی کمتری دارند؛ برای تطبیق میزان آهن المنتال از داروساز بپرسید.",
      ),
      cautionPointsFa = listOf(
        "از دادن قطره آهن هم‌زمان با شیر، ماست یا چای خودداری کنید؛ حداقل ۱ تا ۲ ساعت فاصله بگذارید تا جذب آهن کاهش نیابد.",
        "تیره شدن رنگ مدفوع هنگام مصرف قطره آهن طبیعی است؛ اما یبوست شدید یا بی‌قراری را با پزشک یا داروساز مطرح کنید.",
      ),
      sourceLabelFa = "WHO • AAP Pediatric Nutrition",
    ),
    SupplementOrHygieneGuide(
      id = "supp_zinc_multi",
      tab = CareGuideTab.SUPPLEMENTS,
      shortTitleFa = "زینک و مولتی‌ویتامین",
      titleFa = "مکمل زینک (روی) و مولتی‌ویتامین کودکان",
      subtitleFa = "چه زمانی لازم است و چگونه از تداخل مکمل‌ها جلوگیری کنیم؟",
      ageLabelFa = "۶ ماه تا ۶ سال",
      ageFilters = setOf(CareAgeFilter.MONTHS_6_24, CareAgeFilter.YEARS_2_6),
      icon = Icons.Rounded.HealthAndSafety,
      iconKind = PediatricIconKind.SUPPLEMENTS,
      accentRole = TopicAccentRole.TEAL,
      howToUseTitleFa = "راهنمای انتخاب آگاهانه",
      howToUsePointsFa = listOf(
        "برای کودکان با رشد طبیعی و تغذیه متنوع، مصرف خودسرانه شربت‌های مولتی‌ویتامین یا اشتهاآور ضروری نیست.",
        "در دوره‌های اسهال حاد یا کمبود تأییدشده، مکمل زینک تنها با مقدار مناسب سن و زیر نظر پزشک یا داروساز مصرف می‌شود.",
        "شربت‌های فاقد قند افزوده، بدون رنگ مصنوعی و با پیمانه مدرج استاندارد انتخاب بهتری هستند.",
      ),
      cautionPointsFa = listOf(
        "هم‌زمان چند مکمل مختلف (مثلاً مولتی‌ویتامین + قطره A+D + زینک پلاس) به کودک ندهید تا دچار دریافت بیش از حد ریزمغذی‌ها نشود.",
        "برای تنظیم ترکیب مکمل‌ها با توجه به وزن و آزمایش کودک، لیست کامل مکمل‌ها را برای داروساز بفرستید.",
      ),
      sourceLabelFa = "WHO Diarrhoea & Micronutrient Guide • AAP",
    ),
    SupplementOrHygieneGuide(
      id = "supp_probiotic",
      tab = CareGuideTab.SUPPLEMENTS,
      shortTitleFa = "قطره پروبیوتیک",
      titleFa = "قطره پروبیوتیک و مراقبت گوارش شیرخوار",
      subtitleFa = "کمک به تعادل فلور روده و نکات نگهداری حساس به دما",
      ageLabelFa = "نوزادی تا ۶ سال",
      ageFilters = setOf(CareAgeFilter.UNDER_6M, CareAgeFilter.MONTHS_6_24, CareAgeFilter.YEARS_2_6),
      icon = Icons.Rounded.ChildCare,
      iconKind = PediatricIconKind.SUPPLEMENTS,
      accentRole = TopicAccentRole.SKY,
      howToUseTitleFa = "نکات کلیدی مصرف و نگهداری",
      howToUsePointsFa = listOf(
        "پروبیوتیک‌ها باکتری‌های مفید زنده هستند؛ پیش از دادن، شیشه قطره را به‌آرامی تکان دهید تا سوسپانسیون یکنواخت شود.",
        "قطره را داخل شیر یا غذای داغ نریزید زیرا گرما باکتری‌های مفید را غیرفعال می‌کند.",
        "در صورت مصرف هم‌زمان آنتی‌بیوتیک، معمولاً حداقل ۲ ساعت بین آنتی‌بیوتیک و پروبیوتیک فاصله بگذارید.",
      ),
      cautionPointsFa = listOf(
        "بروشور هر برند را برای دمای نگهداری (یخچالی یا دمای اتاق زیر ۲۵ درجه) و مهلت مصرف پس از باز شدن در بطری بررسی کنید.",
        "در نوزادان نارس یا کودکان با نقص ایمنی، مصرف پروبیوتیک تنها با تأیید مستقیم پزشک مجاز است.",
      ),
      sourceLabelFa = "ESPGHAN • AAP Pediatric Probiotics",
    ),
    SupplementOrHygieneGuide(
      id = "supp_omega3",
      tab = CareGuideTab.SUPPLEMENTS,
      shortTitleFa = "امگا-۳ (DHA)",
      titleFa = "امگا-۳ (DHA) و اسیدهای چرب ضروری کودک",
      subtitleFa = "حمایت از رشد مغز و بینایی در کودکانی که ماهی کم مصرف می‌کنند",
      ageLabelFa = "۲ تا ۶ سال",
      ageFilters = setOf(CareAgeFilter.YEARS_2_6),
      icon = Icons.Rounded.Spa,
      iconKind = PediatricIconKind.SUPPLEMENTS,
      accentRole = TopicAccentRole.PLUM,
      howToUseTitleFa = "اصول انتخاب و مصرف ایمن",
      howToUsePointsFa = listOf(
        "بهترین منبع امگا-۳، مصرف هفتگی ماهی‌های کم‌جیوه در برنامه غذایی است؛ مکمل امگا-۳ برای کودکانی مطرح می‌شود که ماهی نمی‌خورند.",
        "مکمل‌های تصفیه‌شده مخصوص کودکان را انتخاب کنید که فاقد فلزات سنگین و بدون قند یا طعم‌دهنده تند باشند.",
        "مصرف همراه وعده غذایی باعث جذب بهتر و کاهش طعم ماهی پس از مصرف می‌شود.",
      ),
      cautionPointsFa = listOf(
        "در کودکان با حساسیت به ماهی یا غذاهای دریایی، پیش از مصرف حتماً با پزشک یا داروساز مشورت کنید.",
      ),
      sourceLabelFa = "AAP Nutrition Handbook",
    ),

    // --- CHILD HYGIENE ---
    SupplementOrHygieneGuide(
      id = "hyg_oral_teeth",
      tab = CareGuideTab.HYGIENE,
      shortTitleFa = "دهان و دندان",
      titleFa = "بهداشت دهان و اولین دندان‌های کودک",
      subtitleFa = "پیشگیری از پوسیدگی زودرس از بدو تولد تا ۶ سالگی",
      ageLabelFa = "بدو تولد تا ۶ سال",
      ageFilters = setOf(CareAgeFilter.UNDER_6M, CareAgeFilter.MONTHS_6_24, CareAgeFilter.YEARS_2_6),
      icon = Icons.Rounded.CleanHands,
      iconKind = PediatricIconKind.FAMILY,
      accentRole = TopicAccentRole.TEAL,
      howToUseTitleFa = "مراقبت سنی مرحله‌به‌مرحله",
      howToUsePointsFa = listOf(
        "پیش از رویش دندان: روزی یک‌بار لثه نوزاد را با گاز استریل مرطوب و تمیز به‌آرامی پاک کنید.",
        "با رویش اولین دندان تا ۳ سالگی: روزی ۲ بار با مسواک بسیار نرم مخصوص کودک و مقدار خمیردندان فلورایددار در حد «یک دانه برنج» مسواک بزنید.",
        "۳ تا ۶ سالگی: مقدار خمیردندان به اندازه «یک نخود کوچک» و با نظارت کامل والدین برای تف کردن کف آن.",
      ),
      cautionPointsFa = listOf(
        "کودک را هرگز با شیشه‌شیر یا آبمیوه در دهان نخوابانید؛ این کار مهم‌ترین علت پوسیدگی شدید دندان‌های شیری است.",
      ),
      sourceLabelFa = "AAPD • AAP Oral Health",
    ),
    SupplementOrHygieneGuide(
      id = "hyg_bath_skin",
      tab = CareGuideTab.HYGIENE,
      shortTitleFa = "حمام و بند ناف",
      titleFa = "اصول حمام نوزاد، پوست حساس و بند ناف",
      subtitleFa = "شست‌وشوی ملایم بدون آسیب به سد محافظتی پوست کودک",
      ageLabelFa = "نوزادی تا ۶ سال",
      ageFilters = setOf(CareAgeFilter.UNDER_6M, CareAgeFilter.MONTHS_6_24, CareAgeFilter.YEARS_2_6),
      icon = Icons.Rounded.WaterDrop,
      iconKind = PediatricIconKind.DIAPER,
      accentRole = TopicAccentRole.SKY,
      howToUseTitleFa = "راهنمای حمام ایمن در خانه",
      howToUsePointsFa = listOf(
        "تا پیش از افتادن کامل بند ناف، نوزاد را به‌صورت اسفنجی (Sponge Bath) تمیز کنید و بند ناف را خشک و باز نگه دارید.",
        "دمای آب حمام حدود ۳۷ درجه (ولرم، قابل آزمایش با پشت مچ دست) و مدت حمام ۵ تا ۱۰ دقیقه کافی است.",
        "از شامپو و شوینده ملایم بدون اسانس و سولفات (Syndet) استفاده کنید و ظرف ۳ دقیقه پس از حمام، کرم مرطوب‌کننده کودک بزنید.",
      ),
      cautionPointsFa = listOf(
        "کودک را حتی برای یک لحظه در وان یا حمام تنها نگذارید. روی بند ناف الکل، پودر یا کرم خودسرانه نزنید.",
      ),
      sourceLabelFa = "WHO Newborn Care • AAP",
    ),
    SupplementOrHygieneGuide(
      id = "hyg_diaper",
      tab = CareGuideTab.HYGIENE,
      shortTitleFa = "بهداشت پوشک",
      titleFa = "بهداشت ناحیه پوشک و پیشگیری از التهاب",
      subtitleFa = "روتین خشک و ایمن برای جلوگیری از سوختگی و عفونت",
      ageLabelFa = "بدو تولد تا ۲ سال",
      ageFilters = setOf(CareAgeFilter.UNDER_6M, CareAgeFilter.MONTHS_6_24),
      icon = Icons.Rounded.Spa,
      iconKind = PediatricIconKind.DIAPER,
      accentRole = TopicAccentRole.SAGE,
      howToUseTitleFa = "گام‌های طلایی تعویض پوشک",
      howToUsePointsFa = listOf(
        "شست‌وشو را همیشه از جلو به عقب با آب ولرم انجام دهید و با حوله نخی نرم (به‌صورت ضربه‌ای، نه کشیدنی) خشک کنید.",
        "روزانه چند نوبت کوتاه بدون پوشک به پوست کودک اجازه هواخوری بدهید.",
        "لایه نازکی از کرم محافظ زینک اکساید ساده بدون عطر روی پوست کاملاً خشک بزنید.",
      ),
      cautionPointsFa = listOf(
        "از پودر تالک (خطر استنشاق ریوی)، دستمال مرطوب عطردار/الکلی و کرم‌های کورتونی بدون نسخه در ناحیه پوشک پرهیز کنید.",
      ),
      sourceLabelFa = "AAP Skin & Diaper Care",
    ),
    SupplementOrHygieneGuide(
      id = "hyg_nose_eye_ear",
      tab = CareGuideTab.HYGIENE,
      shortTitleFa = "بینی، چشم و گوش",
      titleFa = "بهداشت بینی، چشم و گوش کودک",
      subtitleFa = "تمیز کردن اصولی با سرم شست‌وشو و پیشگیری از آسیب پرده گوش",
      ageLabelFa = "نوزادی تا ۶ سال",
      ageFilters = setOf(CareAgeFilter.UNDER_6M, CareAgeFilter.MONTHS_6_24, CareAgeFilter.YEARS_2_6),
      icon = Icons.Rounded.ChildCare,
      iconKind = PediatricIconKind.DEVICE,
      accentRole = TopicAccentRole.CORAL,
      howToUseTitleFa = "روش بی‌خطر پاکسازی",
      howToUsePointsFa = listOf(
        "گرفتگی بینی شیرخوار: چند قطره سرم کلرید سدیم ۰.۹٪ (نرمال سالین) پیش از شیر خوردن یا خواب و در صورت نیاز مکش بسیار ملایم با پوآر تمیز.",
        "پاکسازی چشم: با گاز استریل مرطوب‌شده با آب جوشیده سردشده، از گوشه داخلی به سمت گوشه خارجی چشم (هر چشم با گاز جداگانه).",
        "بهداشت گوش: فقط لاله بیرونی گوش را با دستمال نرم مرطوب پاک کنید؛ جرم گوش به‌طور طبیعی به بیرون هدایت می‌شود.",
      ),
      cautionPointsFa = listOf(
        "هرگز از گوش‌پاک‌کن (سواب پنبه‌ای) داخل مجرای گوش استفاده نکنید؛ این کار جرم را به عمق می‌راند و خطر پارگی پرده گوش دارد.",
      ),
      sourceLabelFa = "AAP • NHS Baby Care",
    ),
    SupplementOrHygieneGuide(
      id = "hyg_hands_nails_bottles",
      tab = CareGuideTab.HYGIENE,
      shortTitleFa = "دست، ناخن و شیشه",
      titleFa = "بهداشت دست، ناخن و وسایل کودک",
      subtitleFa = "پیشگیری از عفونت‌های گوارشی و تنفسی در منزل و مهد",
      ageLabelFa = "همه سنین",
      ageFilters = setOf(CareAgeFilter.UNDER_6M, CareAgeFilter.MONTHS_6_24, CareAgeFilter.YEARS_2_6),
      icon = Icons.Rounded.CleanHands,
      iconKind = PediatricIconKind.FAMILY,
      accentRole = TopicAccentRole.GOLD,
      howToUseTitleFa = "عادت‌های ایمن و ساده روزمره",
      howToUsePointsFa = listOf(
        "ناخن‌های نوزاد را هنگام خواب و با ناخن‌گیر یا سوهان نرم مخصوص کودک به شکل صاف و بدون لبه‌های تیز کوتاه کنید.",
        "شیشه‌شیر، سرشیشه و پستانک را پس از هر وعده با برس اختصاصی و آب گرم بشویید و کاملاً خشک کنید.",
        "شستن دست‌ها با آب و صابون به مدت ۲۰ ثانیه پیش از غذا، پس از بازی و پس از برگشت از بیرون را به‌شکل بازی به کودک آموزش دهید.",
      ),
      cautionPointsFa = listOf(
        "ژل‌های ضدعفونی‌کننده الکلی را کاملاً دور از دسترس کودک نگه دارید و در منزل آب و صابون ملایم را در اولویت بگذارید.",
      ),
      sourceLabelFa = "CDC Child Hygiene • WHO",
    ),
  )

private data class DailyHygieneChecklistItem(
  val id: String,
  val labelFa: String,
)

private val DAILY_HYGIENE_CHECKLIST =
  listOf(
    DailyHygieneChecklistItem("chk_vit_d", "قطره ویتامین D / A+D امروز داده شد"),
    DailyHygieneChecklistItem("chk_iron", "مراقبت دندان پس از قطره آهن / شیر"),
    DailyHygieneChecklistItem("chk_brush", "مسواک یا پاکسازی دهان کودک"),
    DailyHygieneChecklistItem("chk_hands", "شست‌وشوی دست‌ها پیش از وعده غذایی"),
    DailyHygieneChecklistItem("chk_skin", "مرطوب‌کننده ملایم / هواخوری پوست پوشک"),
  )

/**
 * Compact, zero-scroll icon-flowchart screen for Pediatric Supplements & Child Hygiene
 * (مکمل‌ها و بهداشت کودک). Tapping any flowchart icon opens its guided step-by-step sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupplementsHygieneScreen(
  onNavigateAskPharmacist: (categoryId: String) -> Unit,
  onNavigateSkinCare: () -> Unit,
  onNavigateNutrition: () -> Unit,
) {
  val spacing = ManoTheme.spacing
  var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
  var selectedAgeFilterId by rememberSaveable { mutableStateOf(CareAgeFilter.ALL.id) }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var checkedRoutineIds by remember { mutableStateOf(emptySet<String>()) }
  var selectedGuide by remember { mutableStateOf<SupplementOrHygieneGuide?>(null) }
  var showChecklistSheet by remember { mutableStateOf(false) }

  val activeTab = CareGuideTab.entries[selectedTabIndex.coerceIn(0, CareGuideTab.entries.lastIndex)]
  val activeAgeFilter = CareAgeFilter.entries.firstOrNull { it.id == selectedAgeFilterId } ?: CareAgeFilter.ALL

  val visibleGuides =
    remember(activeTab, activeAgeFilter, searchQuery) {
      val normQuery = PersianNormalizer.normalize(searchQuery)
      CHILD_SUPPLEMENTS_AND_HYGIENE_GUIDES.filter { guide ->
        val tabMatch = guide.tab == activeTab
        val ageMatch = activeAgeFilter == CareAgeFilter.ALL || activeAgeFilter in guide.ageFilters
        val queryMatch =
          if (normQuery.isBlank()) {
            true
          } else {
            val haystack =
              PersianNormalizer.normalize(
                "${guide.titleFa} ${guide.shortTitleFa} ${guide.subtitleFa} ${guide.ageLabelFa} ${guide.howToUsePointsFa.joinToString(" ")} ${guide.cautionPointsFa.joinToString(" ")}"
              )
            haystack.contains(normQuery)
          }
        tabMatch && ageMatch && queryMatch
      }
    }

  Box(
    modifier = Modifier.fillMaxSize().testTag("supplements_hygiene_screen"),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 840.dp).testTag("supplements_hygiene_list"),
      contentPadding =
        PaddingValues(
          start = spacing.screenHorizontal,
          end = spacing.screenHorizontal,
          top = spacing.xs,
          bottom = spacing.md,
        ),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // 1. Compact Header with MANO + Direct Pharmacist Pill
      item {
        ManoCompactScreenHeader(
          titleFa = "مکمل‌ها و بهداشت کودک",
          subtitleFa = "فلوچارت تصویری مراقبت • پرسش دارویی با داروساز",
          illustrationResId = R.drawable.mano_orbit_calm,
          accentColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag("supplements_hygiene_hero_card"),
          trailingAction = {
            Surface(
              onClick = { onNavigateAskPharmacist("otc") },
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("supplements_ask_pharmacist_btn"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.LocalPharmacy,
                  contentDescription = "سوال از داروساز",
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "داروساز",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              }
            }
          },
        )
      }

      // 2. Compact 2-Tab Flowchart Switcher
      item {
        Row(
          modifier = Modifier.fillMaxWidth().testTag("supplements_hygiene_tabs"),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CareGuideTab.entries.forEachIndexed { index, tab ->
            val selected = index == selectedTabIndex
            val accent =
              if (tab == CareGuideTab.SUPPLEMENTS) TopicAccentRole.GOLD.resolveAccentColor()
              else TopicAccentRole.SAGE.resolveAccentColor()
            Surface(
              onClick = { selectedTabIndex = index },
              color = if (selected) accent.copy(alpha = 0.16f) else manoCardSurface(),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(if (selected) 1.6.dp else 1.dp, if (selected) accent else manoBorderColor()),
              modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("tab_${tab.id}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  imageVector = if (tab == CareGuideTab.SUPPLEMENTS) Icons.Rounded.WbSunny else Icons.Rounded.CleanHands,
                  contentDescription = null,
                  tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                  text = tab.labelFa,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      // 3. Compact Search + Age Filter Strip
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("جست‌وجو: آهن، ویتامین D، مسواک، پوشک...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(
                  onClick = { searchQuery = "" },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(Icons.Rounded.Close, contentDescription = "پاک کردن جست‌وجو", modifier = Modifier.size(18.dp))
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("supplements_hygiene_search_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            CareAgeFilter.entries.forEach { age ->
              val selected = activeAgeFilter == age
              Surface(
                onClick = { selectedAgeFilterId = age.id },
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else manoCardSurface(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else manoBorderColor()),
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("age_filter_${age.id}"),
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
                ) {
                  Text(
                    text = age.labelFa,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }

      // 4. Visual Flowchart Connector
      item {
        FlowchartDownConnector(
          labelFa = "یک آیکون را برای راهنمای مرحله‌به‌مرحله لمس کنید",
          accentColor = MaterialTheme.colorScheme.primary,
        )
      }

      // 5. Compact 2-Column Icon Flowchart Grid (All 5 guides + Daily Checklist fit above the fold!)
      item {
        val gridEntries: List<Pair<SupplementOrHygieneGuide?, Boolean>> =
          visibleGuides.map { it to false } + (null to true)
        val rows = gridEntries.chunked(2)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          rows.forEachIndexed { rowIdx, pair ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              pair.forEachIndexed { colIdx, (guide, isChecklistTile) ->
                if (isChecklistTile) {
                  ManoFlowchartIconTile(
                    titleFa = "چک‌لیست روزانه",
                    subtitleFa = "مکمل و بهداشت امروز",
                    badgeFa = "${checkedRoutineIds.size} از ${DAILY_HYGIENE_CHECKLIST.size}",
                    icon = Icons.Rounded.CheckCircle,
                    accentColor = TopicAccentRole.TEAL.resolveAccentColor(),
                    selected = checkedRoutineIds.size == DAILY_HYGIENE_CHECKLIST.size,
                    onClick = { showChecklistSheet = true },
                    modifier = Modifier.weight(1f).testTag("daily_hygiene_checklist_card"),
                  )
                } else if (guide != null) {
                  val stepNum = (rowIdx * 2 + colIdx + 1).toString()
                  ManoFlowchartIconTile(
                    titleFa = guide.shortTitleFa,
                    subtitleFa = guide.ageLabelFa,
                    icon = guide.icon,
                    accentColor = guide.accentRole.resolveAccentColor(),
                    stepNumberFa = stepNum,
                    onClick = { selectedGuide = guide },
                    modifier = Modifier.weight(1f).testTag("guide_card_${guide.id}"),
                  )
                }
              }
              if (pair.size == 1) {
                Spacer(Modifier.weight(1f))
              }
            }
          }
        }
      }

      // 5B. Icon-driven Recommended Products Card + Dedicated Ask Pharmacist CTA
      item {
        SupplementsHygieneRecommendedCard(
          onAskPharmacist = onNavigateAskPharmacist,
        )
      }

      // 6. Compact Bottom 2-Button Quick Strip (Skin & Nutrition)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onNavigateSkinCare,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("supplements_open_skin_btn"),
          ) {
            Icon(Icons.Rounded.Spa, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("پوست و پوشک", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          OutlinedButton(
            onClick = onNavigateNutrition,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("supplements_open_nutrition_btn"),
          ) {
            Icon(Icons.Rounded.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("تغذیه کودک", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }
  }

  // Detail Flowchart Bottom Sheet for any tapped guide icon
  selectedGuide?.let { guide ->
    val accent = guide.accentRole.resolveAccentColor()
    ManoFlowDetailBottomSheet(
      titleFa = guide.titleFa,
      subtitleFa = "${guide.subtitleFa} • ${guide.ageLabelFa}",
      icon = guide.icon,
      accentColor = accent,
      onDismiss = { selectedGuide = null },
    ) {
      Surface(
        color = manoCardSurface(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "۱. ${guide.howToUseTitleFa}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
          )
          ClinicalBulletList(items = guide.howToUsePointsFa, bulletColor = accent)
        }
      }

      FlowchartDownConnector(accentColor = accent)

      SeverityAlert(
        severity = AlertSeverity.CAUTION,
        title = "۲. نکات ایمنی و احتیاط",
        body = guide.cautionPointsFa.joinToString("\n• ", prefix = "• "),
      )

      Text(
        text = "منبع علمی: ${guide.sourceLabelFa}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Button(
        onClick = {
          val cat = if (guide.tab == CareGuideTab.SUPPLEMENTS) "supplement" else "skin"
          selectedGuide = null
          onNavigateAskPharmacist(cat)
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
      ) {
        Icon(Icons.Rounded.LocalPharmacy, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("سوال از داروساز درباره این مورد")
      }
    }
  }

  // Interactive Daily Routine Checklist Bottom Sheet
  if (showChecklistSheet) {
    ManoFlowDetailBottomSheet(
      titleFa = "چک‌لیست روزانه مکمل و بهداشت",
      subtitleFa = "${checkedRoutineIds.size} از ${DAILY_HYGIENE_CHECKLIST.size} مورد امروز انجام شده است",
      icon = Icons.Rounded.CheckCircle,
      accentColor = TopicAccentRole.TEAL.resolveAccentColor(),
      onDismiss = { showChecklistSheet = false },
    ) {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        DAILY_HYGIENE_CHECKLIST.forEach { item ->
          val checked = item.id in checkedRoutineIds
          FilterChip(
            selected = checked,
            onClick = {
              checkedRoutineIds =
                if (checked) checkedRoutineIds - item.id else checkedRoutineIds + item.id
            },
            leadingIcon =
              if (checked) {
                { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
              } else {
                null
              },
            label = { Text(item.labelFa) },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("routine_${item.id}"),
          )
        }
      }
      OutlinedButton(
        onClick = {
          showChecklistSheet = false
          onNavigateAskPharmacist("supplement")
        },
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
      ) {
        Text("پرسش از داروساز درباره برنامه روزانه مکمل")
      }
    }
  }
}
