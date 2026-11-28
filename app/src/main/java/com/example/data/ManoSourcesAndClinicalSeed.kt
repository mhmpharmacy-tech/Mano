package com.example.data

import com.example.domain.model.AgeBand
import com.example.domain.model.Article
import com.example.domain.model.ClinicalReviewMeta
import com.example.domain.model.DeviceGuide
import com.example.domain.model.DoseRule
import com.example.domain.model.FaqItem
import com.example.domain.model.MedicationMonograph
import com.example.domain.model.MedicationSafetyCategory
import com.example.domain.model.MilestoneItem
import com.example.domain.model.RedFlagOption
import com.example.domain.model.ReviewStatus
import com.example.domain.model.SourceReference
import com.example.domain.model.SymptomFlow
import com.example.domain.model.TopicCategory

object ManoSourcesAndClinicalSeed {
  const val RULE_SPEC_VERSION = "v5.0.0"

  const val CLINICAL_REVIEWER_IDENTITY_FA =
    "تحریریه داروسازی و طب کودکان مانوکید (نیازمند تأیید داروساز مسئول پیش از استفاده درمانی فردی)"

  val approvedReviewMeta =
    ClinicalReviewMeta(
      status = ReviewStatus.APPROVED,
      lastReviewedDate = "۱۴۰۳/۱۱/۱۰",
      nextReviewDate = "۱۴۰۴/۱۱/۱۰",
      reviewerIdentity = CLINICAL_REVIEWER_IDENTITY_FA,
      notes =
        "تدوین‌شده بر اساس راهنماهای معتبر اطفال (AAP / NICE NG143 / WHO / CDC). صرفاً آموزشی است و جایگزین معاینه پزشک یا نسخه داروساز نیست.",
    )

  val reviewPendingMeta =
    ClinicalReviewMeta(
      status = ReviewStatus.NEEDS_CLINICAL_REVIEW,
      lastReviewedDate = "۱۴۰۳/۱۱/۱۰",
      nextReviewDate = "۱۴۰۴/۰۵/۱۰",
      reviewerIdentity = null,
      notes = "نیازمند بازبینی داروساز بالینی و انطباق با وزن و شرح‌حال کودک.",
    )

  val srcAapMedication =
    SourceReference(
      id = "aap_fever",
      title = "AAP Fever and Antipyretic Use in Children",
      publisher = "American Academy of Pediatrics",
      url = "https://publications.aap.org/pediatrics/article/127/3/580/65016",
      publishedOrUpdated = "2024",
    )

  val srcNiceFever =
    SourceReference(
      id = "nice_ng143",
      title = "NICE NG143: Fever in under 5s — assessment and initial management",
      publisher = "National Institute for Health and Care Excellence",
      url = "https://www.nice.org.uk/guidance/ng143",
      publishedOrUpdated = "2023",
    )

  val srcWhoOrs =
    SourceReference(
      id = "who_ors",
      title = "WHO Pocket Book of Hospital Care for Children & ORS Guidelines",
      publisher = "World Health Organization",
      url = "https://www.who.int/publications/i/item/9789241548373",
      publishedOrUpdated = "2024",
    )

  val srcCdcMilestones =
    SourceReference(
      id = "cdc_milestones",
      title = "CDC Developmental Milestones & Immunization Schedule",
      publisher = "Centers for Disease Control and Prevention",
      url = "https://www.cdc.gov/ncbddd/actearly/milestones/index.html",
      publishedOrUpdated = "2024",
    )

  val srcIranHealth =
    SourceReference(
      id = "iran_moh_epi",
      title = "برنامه کشوری ایمن‌سازی و مراقبت‌های ادغام‌یافته کودک سالم",
      publisher = "وزارت بهداشت، درمان و آموزش پزشکی",
      url = "https://behdasht.gov.ir",
      publishedOrUpdated = "1403",
    )

  val sources: List<SourceReference> =
    listOf(srcAapMedication, srcNiceFever, srcWhoOrs, srcCdcMilestones, srcIranHealth)

  val articles: List<Article> =
    listOf(
      Article(
        slug = "when-to-see-doctor",
        titleFa = "علائم هشدار فوری: چه زمانی بدون تأخیر به اورژانس مراجعه کنیم؟",
        category = TopicCategory.SYMPTOMS,
        ageBands = AgeBand.entries.toList(),
        shortAnswerFa =
          "در نوزاد زیر ۳ ماه با تب ۳۸ درجه و بالاتر، تنگی نفس، خواب‌آلودگی غیرعادی، تشنج یا بی‌حالی شدید بلافاصله به اورژانس یا ۱۱۵ مراجعه کنید.",
        ageRelevanceFa = "مهم برای تمام سنین به‌ویژه نوزادان زیر ۳ ماه",
        selfCarePointsFa = listOf(
          "دمای دقیق، ساعت اندازه‌گیری و آخرین نوبت مصرف دارو را یادداشت کنید تا به پزشک نشان دهید.",
          "در صورت بروز علائم خطر، به هیچ وجه منتظر اثر تب‌بر یا نوبت بعدی نمانید.",
        ),
        redFlagsFa = listOf(
          "تب ۳۸ درجه سانتی‌گراد یا بیشتر در نوزاد زیر ۳ ماه",
          "تنفس سریع، فرورفتگی قفسه سینه، ناله یا کبودی لب‌ها",
          "تشنج، سفتی گردن، خونریزی زیرپوستی (لکه‌های بنفش با تست لیوان) یا بیدارنشدن",
          "گریه بی‌وقفه و آرام‌نشدنی یا بی‌حالی شدید و کاهش ادرار",
        ),
        caregiverMistakesFa = listOf(
          "دادن تب‌بر به نوزاد زیر ۳ ماه پیش از ارزیابی فوری پزشک",
          "استفاده از الکل یا حمام آب سرد برای پایین آوردن تب",
        ),
        nextStepCtaLabelFa = "شروع بررسی گام‌به‌گام علائم",
        nextStepRouteKey = "symptoms:fever",
        relatedSlugs = listOf("infant-fever-under-3-months", "hydration-ors-signs"),
        sources = listOf(srcNiceFever, srcAapMedication),
        review = approvedReviewMeta,
        isHighRisk = true,
      ),
      Article(
        slug = "infant-fever-under-3-months",
        titleFa = "تب در نوزاد زیر ۳ ماه: چرا ارزیابی فوری اورژانس الزامی است؟",
        category = TopicCategory.SYMPTOMS,
        ageBands = listOf(AgeBand.NEWBORN),
        shortAnswerFa =
          "سیستم ایمنی نوزاد زیر ۳ ماه هنوز بالغ نشده است؛ هر تب ۳۸ درجه سانتی‌گراد یا بالاتر حتی اگر حال عمومی نوزاد خوب به نظر برسد نیاز به معاینه فوری بیمارستانی دارد.",
        ageRelevanceFa = "نوزادان ۰ تا ۳ ماه",
        selfCarePointsFa = listOf(
          "دمای رکتال یا زیربغلی دقیق و ساعت آخرین شیردهی و پوشک خیس را ثبت کنید.",
          "لباس نوزاد را سبک نگه دارید و بدون دستور پزشک استامینوفن ندهید.",
        ),
        redFlagsFa = listOf(
          "دمای ۳۸ درجه سانتی‌گراد یا بالاتر در سن زیر ۱۲ هفته",
          "امتناع از شیر خوردن، زردی شدید، بی‌حالی یا تنفس نامنظم",
        ),
        caregiverMistakesFa = listOf(
          "پیچیدن نوزاد در پتوی ضخیم هنگام تب",
          "تاخیر تا صبح به امید پایین آمدن تب با داروی خانگی",
        ),
        nextStepCtaLabelFa = "بررسی علائم تب",
        nextStepRouteKey = "symptoms:fever",
        relatedSlugs = listOf("when-to-see-doctor"),
        sources = listOf(srcNiceFever, srcAapMedication),
        review = approvedReviewMeta,
        isHighRisk = true,
      ),
      Article(
        slug = "growth-and-development",
        titleFa = "پایش رشد قد و وزن و نقاط عطف تکامل کودک",
        category = TopicCategory.NUTRITION,
        ageBands = AgeBand.entries.toList(),
        shortAnswerFa =
          "روند منحنی رشد در چند اندازه‌گیری متوالی از یک عدد منفرد مهم‌تر است؛ هرگونه توقف یا افت صدک باید با پزشک بررسی شود.",
        ageRelevanceFa = "از بدو تولد تا ۶ سال و بالاتر",
        selfCarePointsFa = listOf(
          "وزن را ترجیحاً با ترازوی استاندارد و لباس سبک، در ساعات مشابه و با تاریخ دقیق ثبت کنید.",
          "تا ۶ ماهگی تغذیه انحصاری با شیر مادر یا شیرخشک استاندارد توصیه می‌شود و از ۶ ماهگی غذای کمکی متنوع آغاز می‌گردد.",
        ),
        redFlagsFa = listOf(
          "عدم وزن‌گیری یا افت وزن در دو پایش متوالی",
          "از دست دادن مهارت‌های حرکتی یا گفتاری که کودک قبلاً کسب کرده بود",
        ),
        caregiverMistakesFa = listOf(
          "مقایسه یک‌باره وزن کودک با همسالان بدون توجه به قد، ژنتیک و روند منحنی WHO",
          "افزودن نمک، شکر یا عسل به غذای کودک زیر یک سال",
        ),
        nextStepCtaLabelFa = "ثبت قد و وزن در بخش رشد",
        nextStepRouteKey = "growth",
        relatedSlugs = listOf("hydration-ors-signs"),
        sources = listOf(srcCdcMilestones, srcWhoOrs),
        review = approvedReviewMeta,
      ),
      Article(
        slug = "diaper-rash-care",
        titleFa = "مراقبت اصولی از پوست و پیشگیری از التهاب ناحیه پوشک",
        category = TopicCategory.SKIN,
        ageBands = listOf(AgeBand.NEWBORN, AgeBand.INFANT_3_12M, AgeBand.TODDLER_1_3Y),
        shortAnswerFa =
          "تعویض منظم پوشک، شست‌وشو با آب ولرم بدون صابون تند، خشک‌کردن ملایم با هوا و لایه محافظ زینک اکساید پایه مراقبت ایمن است.",
        ageRelevanceFa = "نوزادان و کودکان پوشکی",
        selfCarePointsFa = listOf(
          "هر ۲ تا ۳ ساعت پوشک را چک کرده و پس از هر دفع بلافاصله عوض کنید.",
          "در هر نوبت ۱۰ دقیقه اجازه هواخوردن پوست را بدهید و لایه نازک کرم زینک اکساید بدون عطر بزنید.",
        ),
        redFlagsFa = listOf(
          "تاول، چرک، زخم باز، خونریزی یا ضایعات اقماری قرمز روشن (احتمال عفونت قارچی/باکتریایی)",
          "تب یا بی‌قراری شدید هم‌زمان با التهاب پوشک",
        ),
        caregiverMistakesFa = listOf(
          "استفاده خودسرانه از پمادهای ترکیبی حاوی کورتون قوی در ناحیه پوشک",
          "پاک‌کردن پوست ملتهب با دستمال مرطوب معطر یا الکلی",
        ),
        nextStepCtaLabelFa = "ارزیابی التهاب پوشک",
        nextStepRouteKey = "symptoms:diaper_rash",
        relatedSlugs = listOf("when-to-see-doctor"),
        sources = listOf(srcAapMedication),
        review = approvedReviewMeta,
      ),
      Article(
        slug = "hydration-ors-signs",
        titleFa = "پیشگیری از کم‌آبی در اسهال و استفراغ و استفاده صحیح از ORS",
        category = TopicCategory.MEDICINES,
        ageBands = AgeBand.entries.toList(),
        shortAnswerFa =
          "اصلی‌ترین خطر اسهال و استفراغ کم‌آبی است؛ محلول استاندارد ORS را در جرعه‌های کوچک و مکرر بدهید و از آب‌میوه صنعتی یا نوشابه پرهیز کنید.",
        ageRelevanceFa = "تمام سنین به‌ویژه زیر ۳ سال",
        selfCarePointsFa = listOf(
          "پس از استفراغ، ۱۰ تا ۱۵ دقیقه صبر کنید و سپس هر ۵ دقیقه با قاشق یا سرنگ مقدار اندکی ORS یا شیر مادر بدهید.",
          "تغذیه عادی کودک را ادامه دهید و شیر مادر را قطع نکنید.",
        ),
        redFlagsFa = listOf(
          "خشکی دهان و زبان، گریه بدون اشک، گودی چشم یا ملاج",
          "نداشتن ادرار بیش از ۶ تا ۸ ساعت، مدفوع خونی یا استفراغ مکرر صفراوی",
        ),
        caregiverMistakesFa = listOf(
          "حل کردن پودر ORS با مقدار آب کمتر از دستور پاکت (خطر غلظت بالای سدیم)",
          "دادن نوشیدنی‌های پرشکر یا ضداسهال بزرگسالان به کودک",
        ),
        nextStepCtaLabelFa = "بررسی علائم اسهال و کم‌آبی",
        nextStepRouteKey = "symptoms:diarrhea",
        relatedSlugs = listOf("when-to-see-doctor"),
        sources = listOf(srcWhoOrs),
        review = approvedReviewMeta,
        isHighRisk = true,
      ),
    )

  val medications: List<MedicationMonograph> =
    listOf(
      MedicationMonograph(
        slug = "acetaminophen-pediatric",
        nameFa = "استامینوفن کودکان (قطره و شربت)",
        genericNameEn = "Acetaminophen / Paracetamol",
        treatsFa = "کاهش تب و تسکین درد خفیف تا متوسط در کودکان",
        labeledMinimumAgeFa = "تجویز در نوزاد زیر ۲ تا ۳ ماه فقط با دستور مستقیم پزشک",
        dosageFormAlertFa =
          "هشدار غلظت! قطره خوراکی معمولاً ۱۰۰mg/1mL است اما شربت‌ها ۱۲۰mg/5mL یا ۱۶۰mg/5mL هستند؛ پیش از مصرف برچسب بطری را با دقت تطبیق دهید.",
        timingAndPrecautionsFa = listOf(
          "حداقل فاصله بین دو نوبت ۴ تا ۶ ساعت و حداکثر ۴ نوبت در ۲۴ ساعت است.",
          "قبل از مصرف شیشه شربت را تکان دهید و فقط از سرنگ خوراکی مدرج به جای قاشق خانگی استفاده کنید.",
        ),
        commonSideEffectsFa = listOf(
          "در دوز مجاز معمولاً خوش‌تحمل است؛ تهوع خفیف در برخی کودکان.",
        ),
        interactionCautionsFa = listOf(
          "هرگز هم‌زمان با شربت سرماخوردگی یا مسکن دیگری که حاوی استامینوفن/پاراستامول است مصرف نشود.",
          "در بیماری کبدی یا کم‌آبی شدید فقط با نظر پزشک مصرف گردد.",
        ),
        whatNotToGiveFa = listOf(
          "در نوزاد زیر ۳ ماه با تب ۳۸ درجه بدون ویزیت اورژانس مصرف نشود.",
          "بیش از دوز وزنی مجاز یا با فاصله کمتر از ۴ ساعت داده نشود (خطر سمیت کبدی).",
        ),
        refusalTipsFa = listOf(
          "دارو را با سرنگ به آرامی به سمت داخل گونه کودک بریزید تا ریسک پریدن در گلو کمتر شود.",
        ),
        whenToSeekAdviceFa = listOf(
          "اگر تب بیش از ۴۸ تا ۷۲ ساعت ادامه یافت یا همراه با علائم هشدار بود به پزشک مراجعه کنید.",
        ),
        lockedDoseRule =
          DoseRule(
            id = "dose_acetaminophen",
            medicationSlug = "acetaminophen-pediatric",
            isEnabled = true,
            requiredInputsFa = listOf("وزن دقیق کودک (کیلوگرم)", "تأیید غلظت روی بطری (mg/mL)"),
            blockingReasonsFa = listOf("در نوزاد زیر ۳ ماه نیازمند ارزیابی فوری پزشک است."),
            reviewStatus = ReviewStatus.APPROVED,
          ),
        sources = listOf(srcAapMedication, srcNiceFever),
        review = approvedReviewMeta,
        category = MedicationSafetyCategory.FEVER_PAIN,
        aliasesSearchTermsFa = listOf("پاراستامول", "قطره تب بر", "استامینوفن", "شربت تب"),
        keySafetySummaryFa = "مراقب تفاوت غلظت قطره (۱۰۰mg/mL) و شربت (۱۲۰ یا ۱۶۰mg/5mL) و تداخل با شربت‌های سرماخوردگی باشید.",
        storageAndShelfLifeFa = "در دمای اتاق زیر ۲۵ درجه، دور از نور و دسترس کودکان نگهداری شود؛ تاریخ انقضا پس از باز شدن را روی بطری بنویسید.",
        isHighRiskWarning = true,
      ),
      MedicationMonograph(
        slug = "ibuprofen-pediatric",
        nameFa = "ایبوپروفن کودکان (شربت ۱۰۰mg/5mL)",
        genericNameEn = "Ibuprofen",
        treatsFa = "کاهش تب، درد و التهاب در کودکان ۶ ماه به بالا",
        labeledMinimumAgeFa = "در کودکان زیر ۶ ماه ممنوع است",
        dosageFormAlertFa =
          "معمولاً به‌صورت شربت ۱۰۰ میلی‌گرم در ۵ میلی‌لیتر عرضه می‌شود؛ قبل از کشیدن با سرنگ به خوبی تکان دهید.",
        timingAndPrecautionsFa = listOf(
          "فاصله نوبت‌ها معمولاً ۶ تا ۸ ساعت است و حداکثر ۳ تا ۴ نوبت در ۲۴ ساعت مصرف می‌شود.",
          "ترجیحاً همراه با کمی غذا یا شیر داده شود تا تحریک معده کاهش یابد.",
        ),
        commonSideEffectsFa = listOf("ناراحتی یا دل‌درد خفیف معده."),
        interactionCautionsFa = listOf(
          "در صورت کم‌آبی، اسهال و استفراغ شدید یا مصرف داروهای ضدالتهاب دیگر استفاده نشود.",
          "در آبله‌مرغان از ایبوپروفن پرهیز شود.",
        ),
        whatNotToGiveFa = listOf(
          "کودکان زیر ۶ ماه، بیماران کلیوی یا کودکانی که مایعات نمی‌نوشند.",
        ),
        refusalTipsFa = listOf("پس از خوراندن دارو کمی آب یا شیر بدهید."),
        whenToSeekAdviceFa = listOf("درد شدید شکم، استفراغ مداوم یا تداوم تب بیش از ۲ روز."),
        lockedDoseRule =
          DoseRule(
            id = "dose_ibuprofen",
            medicationSlug = "ibuprofen-pediatric",
            isEnabled = true,
            requiredInputsFa = listOf("وزن کودک", "سن حداقل ۶ ماه", "وضعیت کافی‌بودن آب بدن"),
            blockingReasonsFa = listOf("در زیر ۶ ماه یا کم‌آبی بدن مجاز نیست."),
            reviewStatus = ReviewStatus.APPROVED,
          ),
        sources = listOf(srcAapMedication, srcNiceFever),
        review = approvedReviewMeta,
        category = MedicationSafetyCategory.FEVER_PAIN,
        aliasesSearchTermsFa = listOf("بروفن", "ایبوپروفن", "شربت تب بر"),
        keySafetySummaryFa = "در زیر ۶ ماه و در کودک کم‌آب مصرف نشود؛ فاصله مصرف ۶ تا ۸ ساعت است.",
        storageAndShelfLifeFa = "در دمای اتاق و دور از یخ‌زدگی نگهداری شود.",
      ),
      MedicationMonograph(
        slug = "ors-pediatric",
        nameFa = "محلول خوراکی اوآراس (ORS)",
        genericNameEn = "Oral Rehydration Salts",
        treatsFa = "جبران آب و املاح در اسهال و استفراغ کودک",
        labeledMinimumAgeFa = "قابل استفاده در کودکان طبق حجم دستور پزشک/پروتکل",
        dosageFormAlertFa = "هر ساشه استاندارد باید دقیقاً در حجم آب جوشیده خنک‌شده نوشته‌شده روی پاکت حل شود.",
        timingAndPrecautionsFa = listOf(
          "در جرعه‌های کوچک هر ۳ تا ۵ دقیقه با قاشق یا سرنگ داده شود.",
          "محلول آماده‌شده بیش از ۲۴ ساعت در یخچال یا ۱۲ ساعت در دمای اتاق نگه داشته نشود.",
        ),
        commonSideEffectsFa = listOf("ندارد؛ ممکن است مزه کمی شور داشته باشد."),
        interactionCautionsFa = listOf("به محلول آب‌میوه، نوشابه یا شکر اضافه نکنید."),
        whatNotToGiveFa = listOf("در کودک بی‌هوش یا با استفراغ غیرقابل‌کنترل نیازمند سرم درمانی است."),
        refusalTipsFa = listOf("خنک‌کردن ملایم محلول و دادن با سرنگ پذیرش را بهتر می‌کند."),
        whenToSeekAdviceFa = listOf("کاهش ادرار، خشکی شدید لب‌ها، خواب‌آلودگی یا مدفوع خونی."),
        lockedDoseRule =
          DoseRule(
            id = "dose_ors",
            medicationSlug = "ors-pediatric",
            isEnabled = false,
            requiredInputsFa = listOf("وزن کودک و شدت کم‌آبی"),
            blockingReasonsFa = listOf("تعیین حجم درمانی کم‌آبی متوسط/شدید نیازمند معاینه پزشک است."),
            reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
          ),
        sources = listOf(srcWhoOrs),
        review = approvedReviewMeta,
        category = MedicationSafetyCategory.HYDRATION_GI,
        aliasesSearchTermsFa = listOf("اسهال", "کم آبی", "پودر او آر اس"),
        keySafetySummaryFa = "با حجم آب دقیق روی بسته حل شود و با قاشق/سرنگ به تدریج خورانده شود.",
        storageAndShelfLifeFa = "پودر در جای خشک؛ محلول آماده تا ۲۴ ساعت در یخچال.",
      ),
      MedicationMonograph(
        slug = "cetirizine-pediatric",
        nameFa = "شربت ستیریزین کودکان (۵mg/5mL)",
        genericNameEn = "Cetirizine",
        treatsFa = "علائم حساسیت فصلی، کهیر و آبریزش بینی آلرژیک",
        labeledMinimumAgeFa = "زیر ۲ سال فقط با تجویز پزشک",
        dosageFormAlertFa = "غلظت رایج ۵ میلی‌گرم در هر ۵ میلی‌لیتر است.",
        timingAndPrecautionsFa = listOf("معمولاً روزی یک نوبت طبق سن و تجویز پزشک داده می‌شود."),
        commonSideEffectsFa = listOf("خواب‌آلودگی خفیف یا خشکی دهان."),
        interactionCautionsFa = listOf("با شربت‌های سرماخوردگی حاوی آنتی‌هیستامین تجمیع نشود."),
        whatNotToGiveFa = listOf("برای خواباندن کودک یا رفع سرفه ویروسی زیر ۲ سال استفاده نشود."),
        refusalTipsFa = listOf("با سرنگ مدرج اندازه‌گیری شود."),
        whenToSeekAdviceFa = listOf("تورم لب و زبان، خس‌خس سینه یا تنگی نفس (فوراً اورژانس ۱۱۵)."),
        lockedDoseRule =
          DoseRule(
            id = "dose_cetirizine",
            medicationSlug = "cetirizine-pediatric",
            isEnabled = false,
            requiredInputsFa = listOf("سن کودک و تأیید اندیکاسیون پزشک/داروساز"),
            blockingReasonsFa = listOf("در زیر ۲ سال تجویز پزشک الزامی است."),
            reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
          ),
        sources = listOf(srcAapMedication),
        review = approvedReviewMeta,
        category = MedicationSafetyCategory.RESPIRATORY_ALLERGY,
        aliasesSearchTermsFa = listOf("حساسیت", "آنتی هیستامین", "کهیر", "ستیریزین"),
        keySafetySummaryFa = "در زیر ۲ سال بدون تجویز پزشک استفاده نشود و با شربت سرماخوردگی هم‌زمان ندهید.",
        storageAndShelfLifeFa = "دمای اتاق دور از نور مستقیم.",
      ),
      MedicationMonograph(
        slug = "amoxicillin-suspension",
        nameFa = "سوسپانسیون آموکسی‌سیلین (نیازمند نسخه پزشک)",
        genericNameEn = "Amoxicillin Suspension",
        treatsFa = "عفونت‌های باکتریایی منتخب بر اساس تشخیص و نسخه پزشک",
        labeledMinimumAgeFa = "فقط با نسخه پزشک و مطابق وزن کودک",
        dosageFormAlertFa = "پس از افزودن آب جوشیده خنک‌شده تا خط نشانه، شیشه را کاملاً تکان دهید؛ هر بار پیش از مصرف ۲۰ ثانیه تکان دهید.",
        timingAndPrecautionsFa = listOf(
          "دوره کامل تجویزشده را حتی پس از بهبود تب و علائم تا پایان ادامه دهید.",
          "غلظت سوسپانسیون (۱۲۵ یا ۲۵۰ میلی‌گرم در ۵ میلی‌لیتر) را با نسخه داروخانه تطبیق دهید.",
        ),
        commonSideEffectsFa = listOf("اسهال خفیف، تهوع یا بثورات خفیف."),
        interactionCautionsFa = listOf("در سابقه حساسیت به پنی‌سیلین مطلقاً مصرف نشود."),
        whatNotToGiveFa = listOf("هرگز از آنتی‌بیوتیک باقی‌مانده از دوره قبلی یا برای سرماخوردگی ویروسی استفاده نشود."),
        refusalTipsFa = listOf("خنک نگهداری شدن در یخچال طبق بروشور طعم دارو را قابل‌تحمل‌تر می‌کند."),
        whenToSeekAdviceFa = listOf("کهیر، تورم صورت، خس‌خس سینه یا اسهال شدید پس از مصرف."),
        lockedDoseRule =
          DoseRule(
            id = "dose_amoxicillin",
            medicationSlug = "amoxicillin-suspension",
            isEnabled = false,
            requiredInputsFa = listOf("نسخه پزشک، وزن کودک و غلظت دقیق سوسپانسیون"),
            blockingReasonsFa = listOf("محاسبه خودکار دوز آنتی‌بیوتیک در اپلیکیشن قفل است و فقط طبق نسخه انجام می‌شود."),
            reviewStatus = ReviewStatus.APPROVED,
          ),
        sources = listOf(srcAapMedication),
        review = approvedReviewMeta,
        category = MedicationSafetyCategory.ANTIBIOTICS,
        aliasesSearchTermsFa = listOf("آنتی بیوتیک", "چرک خشک کن", "آموکسی سیلین"),
        keySafetySummaryFa = "آنتی‌بیوتیک نسخه‌ای است؛ سوسپانسیون آماده‌شده معمولاً بین ۷ تا ۱۴ روز در یخچال پایدار می‌ماند.",
        storageAndShelfLifeFa = "پس از رقیق‌سازی با آب، مطابق برچسب کارخانه در یخچال نگه دارید و باقی‌مانده پس از ۱۰ تا ۱۴ روز را دور بریزید.",
        isHighRiskWarning = true,
      ),
      MedicationMonograph(
        slug = "iron-vitamin-d-drops",
        nameFa = "قطره آهن و ویتامین D3 کودک",
        genericNameEn = "Ferrous / Cholecalciferol Drops",
        treatsFa = "پیشگیری از کم‌خونی فقر آهن و کمبود ویتامین D مطابق برنامه پایش رشد",
        labeledMinimumAgeFa = "ویتامین D از روزهای اول؛ آهن از ۶ ماهگی یا پایان ۴ ماهگی طبق توصیه مرکز بهداشت",
        dosageFormAlertFa = "غلظت قطره‌های آهن ایرانی و خارجی متفاوت است؛ تعداد قطره یا میلی‌لیتر را با داروساز چک کنید.",
        timingAndPrecautionsFa = listOf(
          "قطره آهن با لبنیات و چای هم‌زمان داده نشود؛ می‌توان با فاصله از شیر و با چند قطره آب پرتقال داد.",
          "قطره آهن را به قسمت عقب زبان بچکانید و سپس دندان‌ها را با آب یا گاز مرطوب پاک کنید تا تغییر رنگ دندان رخ ندهد.",
        ),
        commonSideEffectsFa = listOf("تیره شدن رنگ مدفوع طبیعی است؛ یبوست خفیف."),
        interactionCautionsFa = listOf("با برخی آنتی‌بیوتیک‌ها ۲ ساعت فاصله مصرف لازم است."),
        whatNotToGiveFa = listOf("مکمل‌ها را دور از دسترس کودک نگه دارید؛ مسمومیت با قرص/قطره آهن خطرناک است."),
        refusalTipsFa = listOf("چکاندن در کنار گونه و پاک‌کردن سریع دندان."),
        whenToSeekAdviceFa = listOf("یبوست شدید، استفراغ مکرر یا بلع تصادفی بیش از دوز روزانه (فوراً اورژانس)."),
        lockedDoseRule =
          DoseRule(
            id = "dose_iron_vitd",
            medicationSlug = "iron-vitamin-d-drops",
            isEnabled = false,
            requiredInputsFa = listOf("سن کودک و غلظت قطره"),
            blockingReasonsFa = listOf("تعیین دوز درمانی کم‌خونی نیازمند آزمایش و نظر پزشک است."),
            reviewStatus = ReviewStatus.APPROVED,
          ),
        sources = listOf(srcIranHealth),
        review = approvedReviewMeta,
        category = MedicationSafetyCategory.VITAMINS_SUPPLEMENTS,
        aliasesSearchTermsFa = listOf("قطره آهن", "ویتامین دی", "مولتی ویتامین", "مکمل"),
        keySafetySummaryFa = "مکمل‌های آهن در صورت بلع بیش‌ازحد سمی‌اند؛ با فاصله از لبنیات داده شوند و دندان‌ها پس از مصرف تمیز شوند.",
        storageAndShelfLifeFa = "در جای خشک و خنک با درپوش ایمن کودک.",
      ),
    )

  val symptomFlows: List<SymptomFlow> =
    listOf(
      SymptomFlow(
        id = "fever",
        titleFa = "تب کودک",
        subtitleFa = "بررسی سن، درجه حرارت و علائم هشدار پیش از هر توصیه مراقبتی",
        relatedArticleSlug = "when-to-see-doctor",
        asksTemperature = true,
        redFlags = listOf(
          RedFlagOption("fever_under_3m", "نوزاد زیر ۳ ماه با دمای ۳۸ درجه یا بالاتر", true),
          RedFlagOption("fever_lethargy", "خواب‌آلودگی غیرمعمول، بیدارنشدن یا بی‌حالی شدید", true),
          RedFlagOption("fever_breathing", "تنفس سریع، فرورفتگی قفسه سینه یا ناله هنگام تنفس", true),
          RedFlagOption("fever_seizure", "تشنج، سفتی گردن یا لکه‌های بنفش زیر پوست", true),
          RedFlagOption("fever_dehydration", "ننوشیدن مایعات یا نداشتن ادرار بیش از ۸ ساعت", true),
        ),
        educationalStepsFa = listOf(
          "دمای دقیق را با دماسنج دیجیتال زیربغلی یا مقعدی (در نوزاد) اندازه بگیرید و ثبت کنید.",
          "لباس کودک را سبک نگه دارید و محیط اتاق را معتدل (۲۲ تا ۲۴ درجه) تنظیم کنید.",
          "مایعات مناسب سن (شیر مادر، آب یا محلول ORS در صورت نیاز) را مرتب پیشنهاد دهید.",
          "در صورت مصرف تب‌بر مجاز، زمان و حجم دقیق را در خط زمانی دارو ثبت کنید تا دوز تکراری ندهید.",
        ),
        escalationCriteriaFa = listOf(
          "تب بالای ۴۰ درجه یا تب پایدار بیش از ۴۸ تا ۷۲ ساعت",
          "تشدید بی‌قراری، افت اشتها به مایعات یا ظاهر شدن هر نشانه جدید خطر",
        ),
        review = approvedReviewMeta,
        sources = listOf(srcNiceFever, srcAapMedication),
      ),
      SymptomFlow(
        id = "cough",
        titleFa = "سرفه و تنفس",
        subtitleFa = "ارزیابی تنگی نفس، خس‌خس و سن کودک پیش از توصیه‌های خانگی",
        relatedArticleSlug = "when-to-see-doctor",
        asksTemperature = true,
        redFlags = listOf(
          RedFlagOption("cough_retractions", "فرورفتگی بین دنده‌ها یا بالای جناغ هنگام نفس‌کشیدن", true),
          RedFlagOption("cough_cyanosis", "کبودی لب‌ها، زبان یا اطراف دهان", true),
          RedFlagOption("cough_stridor", "صدای خشن/خروسکی در حالت استراحت یا ناتوانی در قورت‌دادن آب دهان", true),
          RedFlagOption("cough_under_3m", "سرفه یا گرفتگی تنفسی در نوزاد زیر ۳ ماه", true),
        ),
        educationalStepsFa = listOf(
          "بینی گرفته کودک را با سرم نمکی ایزوتونیک و پوآر ملایم تمیز کنید تا تنفس آسان‌تر شود.",
          "هوای اتاق را مرطوب و کاملاً عاری از دود سیگار و اسپری‌های معطر نگه دارید.",
          "در کودکان بالای ۱ سال می‌توان برای تسکین سرفه شبانه کمی آب ولرم یا عسل (فقط بالای ۱ سال) طبق توصیه استفاده کرد؛ زیر ۶ سال شربت ضدسرفه خودسرانه ندهید.",
        ),
        escalationCriteriaFa = listOf(
          "تعداد تنفس سریع، خس‌خس مداوم یا بیدار شدن مکرر از تنگی نفس",
          "تب هم‌زمان بیش از ۲ روز یا امتناع از شیر خوردن",
        ),
        review = approvedReviewMeta,
        sources = listOf(srcAapMedication, srcNiceFever),
      ),
      SymptomFlow(
        id = "diarrhea",
        titleFa = "اسهال و استفراغ",
        subtitleFa = "غربالگری نشانه‌های کم‌آبی و راهنمای ایمن جبران مایعات",
        relatedArticleSlug = "hydration-ors-signs",
        asksTemperature = false,
        redFlags = listOf(
          RedFlagOption("gi_dehydration", "گریه بدون اشک، گودی چشم یا نداشتن ادرار ۶ تا ۸ ساعت", true),
          RedFlagOption("gi_blood", "وجود خون در مدفوع یا استفراغ مکرر سبز/زرد پررنگ", true),
          RedFlagOption("gi_lethargic", "ناتوانی در نگه‌داشتن مایعات یا خواب‌آلودگی شدید", true),
          RedFlagOption("gi_under_3m", "اسهال یا استفراغ مکرر در نوزاد زیر ۳ ماه", true),
        ),
        educationalStepsFa = listOf(
          "پس از هر بار استفراغ ۱۰ تا ۱۵ دقیقه معده را استراحت دهید و سپس با قاشق کوچک جرعه‌های ۵ میلی‌لیتری ORS یا شیر مادر بدهید.",
          "شیر مادر و تغذیه سبک مناسب سن را قطع نکنید.",
          "تعداد پوشک‌های خیس را به‌عنوان شاخص اصلی هیدراتاسیون پیگیری کنید.",
        ),
        escalationCriteriaFa = listOf(
          "تداوم استفراغ بیش از ۶ تا ۸ ساعت یا تداوم اسهال آبکی بیش از ۴۸ ساعت",
          "بروز تب بالا یا درد شدید شکم",
        ),
        review = approvedReviewMeta,
        sources = listOf(srcWhoOrs),
      ),
      SymptomFlow(
        id = "diaper_rash",
        titleFa = "التهاب و قرمزی ناحیه پوشک",
        subtitleFa = "جداسازی التهاب تماسی ساده از علائم عفونت نیازمند پزشک",
        relatedArticleSlug = "diaper-rash-care",
        asksTemperature = false,
        redFlags = listOf(
          RedFlagOption("diaper_pus", "تاول، چرک، زخم باز یا خونریزی در ناحیه پوشک", true),
          RedFlagOption("diaper_fever", "تب هم‌زمان، لرز یا بی‌حالی کودک", true),
          RedFlagOption("diaper_satellite", "نقاط قرمز پراکنده اطراف ضایعه اصلی (نیازمند بررسی عفونت قارچی)", false),
        ),
        educationalStepsFa = listOf(
          "شست‌وشو فقط با آب ولرم و خشک‌کردن با تماس آرام حوله نخی (بدون مالش).",
          "در هر بار تعویض، ۱۰ دقیقه هواخوردن پوست و زدن لایه محافظ زینک اکساید بدون عطر.",
          "از دستمال مرطوب الکلی یا پمادهای کورتون‌دار بدون تجویز پزشک استفاده نکنید.",
        ),
        escalationCriteriaFa = listOf(
          "عدم بهبود ظرف ۷۲ ساعت یا گسترش التهاب به خارج از محدوده پوشک",
        ),
        review = approvedReviewMeta,
        sources = listOf(srcAapMedication),
      ),
      SymptomFlow(
        id = "earache",
        titleFa = "گوش‌درد یا کشیدن گوش",
        subtitleFa = "ارزیابی علائم گوش، تب و تصمیم برای معاینه تخصصی",
        relatedArticleSlug = "when-to-see-doctor",
        asksTemperature = true,
        redFlags = listOf(
          RedFlagOption("ear_discharge", "خروج چرک یا خون از مجرای گوش یا تورم و قرمزی پشت لاله گوش", true),
          RedFlagOption("ear_high_fever", "تب بالا، سفتی گردن یا بی‌حالی شدید", true),
          RedFlagOption("ear_infant", "سن زیر ۶ ماه با گریه مداوم و گوش‌درد مشکوک", true),
        ),
        educationalStepsFa = listOf(
          "هرگز گوش‌پاک‌کن، روغن یا قطره خانگی بدون معاینه پرده گوش داخل مجرای گوش نریزید.",
          "در صورت تأیید پزشک یا داروساز برای سن و وزن کودک، تسکین درد با دوز ایمن مسکن مجاز انجام شود.",
        ),
        escalationCriteriaFa = listOf(
          "گوش‌دردی که طی ۲۴ تا ۴۸ ساعت بهتر نشود نیازمند معاینه اتوسکوپی پزشک است.",
        ),
        review = approvedReviewMeta,
        sources = listOf(srcAapMedication, srcNiceFever),
      ),
    )

  val faqs: List<FaqItem> =
    listOf(
      FaqItem(
        id = "faq_acetaminophen_diff",
        questionFa = "تفاوت قطره و شربت استامینوفن چیست؟",
        category = TopicCategory.MEDICINES,
        ageBands = listOf(AgeBand.NEWBORN, AgeBand.INFANT_3_12M, AgeBand.TODDLER_1_3Y),
        tags = listOf("استامینوفن", "قطره", "شربت", "غلظت", "تب"),
        summaryFa =
          "قطره استامینوفن غلظت بالاتری دارد (۱۰۰mg/1mL)، در حالی که شربت معمولاً ۱۲۰ یا ۱۶۰mg در ۵mL است. جایگزینی حجم قطره و شربت بدون محاسبه غلظت خطر مسمومیت کبدی یا کم‌اثری دارد.",
        whatToDoFa = listOf(
          "همیشه روی جعبه عبارت mg در mL را بخوانید و دقیقاً با سرنگ خوراکی مدرج اندازه‌گیری کنید.",
          "هرگز از قاشق چای‌خوری یا غذاخوری خانگی برای داروی مایع کودک استفاده نکنید.",
        ),
        whenToSeeDoctorFa = listOf(
          "اگر تصادفاً غلظت یا مقدار اشتباه به کودک داده شد بلافاصله با اورژانس یا داروساز تماس بگیرید.",
        ),
        source = srcAapMedication,
        review = approvedReviewMeta,
        isHighRisk = true,
      ),
      FaqItem(
        id = "faq_high_fever_urgent",
        questionFa = "چه زمانی تب کودک نیاز به مراجعه فوری دارد؟",
        category = TopicCategory.SYMPTOMS,
        ageBands = AgeBand.entries.toList(),
        tags = listOf("تب", "اورژانس", "نوزاد", "تشنج"),
        summaryFa =
          "در نوزاد زیر ۳ ماه هر تب ۳۸ درجه به بالا فوری است. در سنین بالاتر وجود تنگی نفس، خواب‌آلودگی شدید، تشنج، جوش‌های زیرپوستی یا کم‌آبی نشانه مراجعه فوری به اورژانس است.",
        whatToDoFa = listOf(
          "در صورت وجود هر نشانه هشدار، فوراً با ۱۱۵ تماس بگیرید یا به نزدیک‌ترین اورژانس بروید.",
          "عدد دما، ساعت اندازه‌گیری و داروی مصرفی را همراه داشته باشید.",
        ),
        whenToSeeDoctorFa = listOf(
          "تب نوزاد زیر ۳ ماه، تب بالای ۴۰ درجه یا تداوم تب بیش از ۴۸ ساعت.",
        ),
        source = srcNiceFever,
        review = approvedReviewMeta,
        isHighRisk = true,
      ),
      FaqItem(
        id = "faq_syringe_measure",
        questionFa = "چگونه با سرنگ خوراکی mL به کودک دارو بدهیم؟",
        category = TopicCategory.SKILLS_DEVICES,
        ageBands = listOf(AgeBand.INFANT_3_12M, AgeBand.TODDLER_1_3Y, AgeBand.PRESCHOOL_3_6Y),
        tags = listOf("سرنگ", "اندازه‌گیری", "دارو", "mL"),
        summaryFa =
          "سرنگ خوراکی دقیق‌ترین ابزار است. لبه بالایی پیستون را روی خط دقیق میلی‌لیتر تنظیم کنید و دارو را آهسته به سمت دیواره داخلی گونه بریزید نه مستقیم به ته حلق.",
        whatToDoFa = listOf(
          "از سرنگ بدون سوزن مخصوص داروی خوراکی استفاده کنید.",
          "کودک را در حالت نیمه‌نشسته یا ایستاده نگه دارید تا احتمال پریدن دارو به گلو کم شود.",
        ),
        whenToSeeDoctorFa = listOf(
          "در صورت سرفه شدید یا تغییر رنگ لب‌ها هنگام خوردن دارو.",
        ),
        source = srcAapMedication,
        review = approvedReviewMeta,
      ),
      FaqItem(
        id = "faq_dehydration_ors",
        questionFa = "علائم کم‌آبی در اسهال کودک چیست و چه کار کنیم؟",
        category = TopicCategory.SYMPTOMS,
        ageBands = AgeBand.entries.toList(),
        tags = listOf("اسهال", "کم‌آبی", "ORS", "استفراغ"),
        summaryFa =
          "کاهش تعداد پوشک خیس (کمتر از ۱ بار در ۶-۸ ساعت)، خشکی دهان، گریه بدون اشک و بی‌حالی علائم کم‌آبی هستند. بهترین اقدام جبران تدریجی با محلول ORS استاندارد است.",
        whatToDoFa = listOf(
          "هر ۵ دقیقه یک قاشق یا چند میلی‌لیتر محلول ORS بدهید.",
          "شیر مادر و غذای سبک کودک را ادامه دهید.",
        ),
        whenToSeeDoctorFa = listOf(
          "ادرارنکردن طولانی، خون در مدفوع، خواب‌آلودگی یا تداوم استفراغ صفراوی.",
        ),
        source = srcWhoOrs,
        review = approvedReviewMeta,
        isHighRisk = true,
      ),
      FaqItem(
        id = "faq_antibiotic_storage",
        questionFa = "شربت آنتی‌بیوتیک پس از رقیق‌شدن چطور و تا کی نگهداری می‌شود؟",
        category = TopicCategory.MEDICINES,
        ageBands = AgeBand.entries.toList(),
        tags = listOf("آنتی‌بیوتیک", "یخچال", "نگهداری", "آموکسی‌سیلین"),
        summaryFa =
          "بیشتر سوسپانسیون‌های آماده آنتی‌بیوتیک پس از افزودن آب فقط ۷ تا ۱۴ روز در یخچال (۲ تا ۸ درجه، دور از یخ‌زدگی) یا مطابق برچسب همان برند پایدار هستند.",
        whatToDoFa = listOf(
          "تاریخ دقیق افزودن آب را روی برچسب بطری بنویسید و پیش از هر نوبت شیشه را تکان دهید.",
          "باقی‌مانده دارو پس از پایان دوره درمان را دور بریزید و هرگز دوباره مصرف نکنید.",
        ),
        whenToSeeDoctorFa = listOf(
          "اگر دوره کامل شد ولی علائم باقی ماند یا کودک به دارو حساسیت نشان داد.",
        ),
        source = srcAapMedication,
        review = approvedReviewMeta,
      ),
    )

  val milestones: List<MilestoneItem> =
    listOf(
      MilestoneItem(
        id = "ms_newborn_social_smile",
        ageBand = AgeBand.NEWBORN,
        domainFa = "اجتماعی",
        descriptionFa = "نگاه کردن به چهره والدین و شروع لبخند اجتماعی تا پایان ۲ تا ۳ ماهگی",
        watchOutFa = "اگر نوزاد تا ۳ ماهگی به صداهای بلند یا صورت نزدیک واکنش نشان نمی‌دهد با پزشک مشورت کنید.",
        source = srcCdcMilestones,
      ),
      MilestoneItem(
        id = "ms_newborn_head_lift",
        ageBand = AgeBand.NEWBORN,
        domainFa = "حرکتی",
        descriptionFa = "بالا آوردن کوتاه سر هنگام قرار گرفتن روی شکم (Tummy Time با نظارت)",
        watchOutFa = "ضعف یا سفتی بیش‌ازحد عضلات بدن نیازمند ارزیابی است.",
        source = srcCdcMilestones,
      ),
      MilestoneItem(
        id = "ms_infant_sit_roll",
        ageBand = AgeBand.INFANT_3_12M,
        domainFa = "حرکتی",
        descriptionFa = "غلت‌زدن تا ۶ ماهگی و نشستن مستقل حدود ۷ تا ۹ ماهگی",
        watchOutFa = "عدم کنترل سر پس از ۴ ماهگی یا ناتوانی در نشستن با کمک تا ۹ ماهگی.",
        source = srcCdcMilestones,
      ),
      MilestoneItem(
        id = "ms_infant_babble",
        ageBand = AgeBand.INFANT_3_12M,
        domainFa = "گفتاری",
        descriptionFa = "آواسازی و غان و غون (بابا، ماما نامشخص) و برگشتن به سمت صدا",
        watchOutFa = "واکنش‌ندادن به نام یا صداهای محیطی تا ۹ ماهگی.",
        source = srcCdcMilestones,
      ),
      MilestoneItem(
        id = "ms_toddler_walk_words",
        ageBand = AgeBand.TODDLER_1_3Y,
        domainFa = "حرکتی و گفتاری",
        descriptionFa = "راه‌رفتن مستقل تا ۱۵-۱۸ ماهگی و گفتن جملات ۲ کلمه‌ای تا ۲ سالگی",
        watchOutFa = "نداشتن کلمه معنادار تا ۱۸ ماهگی یا از دست دادن مهارت‌های قبلی.",
        source = srcCdcMilestones,
      ),
      MilestoneItem(
        id = "ms_preschool_play_speech",
        ageBand = AgeBand.PRESCHOOL_3_6Y,
        domainFa = "شناختی و گفتاری",
        descriptionFa = "پاسخ به پرسش‌های ساده، بازی گروهی و صحبت واضح قابل‌فهم برای دیگران",
        watchOutFa = "تکلم نامفهوم شدید در ۴ سالگی یا مشکل ارتباط چشمی مداوم.",
        source = srcCdcMilestones,
      ),
      MilestoneItem(
        id = "ms_school_focus_selfcare",
        ageBand = AgeBand.SCHOOL_6_PLUS,
        domainFa = "اجتماعی و شناختی",
        descriptionFa = "همکاری در مراقبت بهداشتی، بیان دقیق محل درد و پیروی از دستور چندمرحله‌ای",
        watchOutFa = "اختلال مداوم در خواب، تمرکز یا دید و شنوایی هنگام آموزش.",
        source = srcCdcMilestones,
      ),
    )

  val deviceGuides: List<DeviceGuide> =
    listOf(
      DeviceGuide(
        id = "device_thermometer",
        titleFa = "دماسنج دیجیتال کودک",
        subtitleFa = "روش صحیح اندازه‌گیری دما در زیربغل و پیشانی بدون خطا",
        distinctionNoteFa = "دماسنج جیوه‌ای به دلیل خطر شکستگی و سمیت توصیه نمی‌شود؛ مدل دیجیتال استاندارد امن‌تر است.",
        stepsFa = listOf(
          "پوست زیربغل را خشک کنید و نوک فلزی دماسنج را دقیقاً در گودی زیربغل قرار دهید.",
          "بازوی کودک را به بدن بچسبانید و تا شنیدن بوق پایان صبر کنید.",
          "عدد، محل اندازه‌گیری و ساعت را در خط زمانی تب مانو ثبت کنید.",
        ),
        cautionNotesFa = listOf(
          "اندازه‌گیری بلافاصله پس از حمام گرم یا زیر پتو ممکن است عدد کاذب بالاتر نشان دهد.",
          "در نوزاد زیر ۳ ماه عدد ۳۸ درجه یا بالاتر نیازمند مراجعه فوری است.",
        ),
        review = approvedReviewMeta,
        sources = listOf(srcNiceFever),
      ),
      DeviceGuide(
        id = "device_oral_syringe",
        titleFa = "سرنگ خوراکی مدرج و قطره‌چکان",
        subtitleFa = "پیشگیری از خطای ۲ تا ۳ برابری دوز ناشی از قاشق خانگی",
        distinctionNoteFa = "قاشق چای‌خوری خانگی حجم استاندارد ندارد و می‌تواند از ۲.۵ تا ۷ میلی‌لیتر متغیر باشد.",
        stepsFa = listOf(
          "بطری را پیش از کشیدن شربت به خوبی تکان دهید تا غلظت یکنواخت شود.",
          "حباب هوای داخل سرنگ را خارج کرده و لبه پیستون را روی خط میلی‌لیتر دقیق تنظیم کنید.",
          "بعد از مصرف، سرنگ را با آب ولرم بشویید و خشک نگه دارید.",
        ),
        cautionNotesFa = listOf(
          "هرگز خط‌کش میلی‌لیتر را با قاشق یا میلی‌گرم اشتباه نگیرید.",
        ),
        review = approvedReviewMeta,
        sources = listOf(srcAapMedication),
      ),
      DeviceGuide(
        id = "device_nebulizer_humidifier",
        titleFa = "نبولایزر و بخور سرد",
        subtitleFa = "نگهداری بهداشتی محفظه دارو و تفاوت کاربرد بخور سرد با نبولایزر",
        distinctionNoteFa = "دستگاه بخور سرد فقط هوای اتاق را مرطوب می‌کند؛ داروهای استنشاقی تجویزی صرفاً با نبولایزر و طبق نسخه پزشک مصرف می‌شوند.",
        stepsFa = listOf(
          "در نبولایزر فقط داروی تجویزشده یا سرم نرمال سالین استریل بریزید.",
          "ماسک و کاپ دارو را پس از هر بار استفاده شسته و در هوای تمیز خشک کنید تا قارچ رشد نکند.",
          "آب مخزن بخور سرد را هر روز تعویض کنید.",
        ),
        cautionNotesFa = listOf(
          "بخور گرم به دلیل خطر سوختگی در اتاق کودک توصیه نمی‌شود.",
        ),
        review = approvedReviewMeta,
        sources = listOf(srcAapMedication),
      ),
    )
}
