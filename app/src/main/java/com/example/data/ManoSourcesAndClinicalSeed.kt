package com.example.data

import com.example.domain.model.AgeBand
import com.example.domain.model.ClinicalReviewMeta
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.DeviceGuide
import com.example.domain.model.DoseRule
import com.example.domain.model.MedicationMonograph
import com.example.domain.model.MilestoneItem
import com.example.domain.model.PharmacistCaseDemo
import com.example.domain.model.RedFlagOption
import com.example.domain.model.ReviewStatus
import com.example.domain.model.SourceReference
import com.example.domain.model.SymptomFlow

object ManoSourcesAndClinicalSeed {

  val srcAapFever =
    SourceReference(
      id = "aap_fever",
      title = "When to Call the Pediatrician: Fever",
      publisher = "AAP / HealthyChildren",
      url = "https://www.healthychildren.org/english/health-issues/conditions/fever/pages/when-to-call-the-pediatrician.aspx",
      publishedOrUpdated = "مرجع پایه جهت بازبینی بالینی (۲۰۲۵)",
    )

  val srcAapMedication =
    SourceReference(
      id = "aap_med_safety",
      title = "Ways to Prevent Home Medication Errors",
      publisher = "AAP / HealthyChildren",
      url = "https://www.healthychildren.org/English/news/Pages/ways-to-prevent-home-medication-errors.aspx",
      publishedOrUpdated = "مرجع پایه جهت بازبینی بالینی (۲۰۲۵)",
    )

  val srcNiceFever =
    SourceReference(
      id = "nice_ng143",
      title = "Fever in Under 5s: Assessment and Initial Management (NG143)",
      publisher = "NICE",
      url = "https://www.nice.org.uk/guidance/ng143/chapter/Recommendations",
      publishedOrUpdated = "نسخه مرجع بالینی جهت بازبینی",
    )

  val srcCdcMilestones =
    SourceReference(
      id = "cdc_milestones",
      title = "CDC Developmental Milestones (2 Months to 5 Years)",
      publisher = "CDC",
      url = "https://www.cdc.gov/act-early/milestones/",
      publishedOrUpdated = "مرجع پایه جهت بازبینی بالینی",
    )

  val srcWhoGrowth =
    SourceReference(
      id = "who_growth",
      title = "WHO Child Growth Standards",
      publisher = "WHO",
      url = "https://www.who.int/tools/child-growth-standards",
      publishedOrUpdated = "استاندارد مرجع سازمان جهانی بهداشت",
    )

  val allSources = listOf(srcAapFever, srcAapMedication, srcNiceFever, srcCdcMilestones, srcWhoGrowth)

  // Honest review metadata: NO fabricated clinician names or fake sign-off dates
  val pendingReviewMeta =
    ClinicalReviewMeta(
      status = ReviewStatus.NEEDS_CLINICAL_REVIEW,
      lastReviewedDate = "هنوز بازبینی نهایی انجام نشده است",
      nextReviewDate = "نیازمند تعیین تاریخ توسط مسئول علمی",
      reviewerIdentity = null,
      notes = "این متن آموزشی بر پایه منابع معتبر تدوین شده و پیش از انتشار عمومی نیازمند تأیید داروساز یا متخصص اطفال و تطبیق با دستورالعمل‌های ایران است.",
    )

  val symptomFlows =
    listOf(
      SymptomFlow(
        id = "fever",
        titleFa = "تب کودک",
        subtitleFa = "بررسی سن، عدد دماسنج و نشانه‌های هشدار پیش از هر توصیه",
        relatedArticleSlug = "fever-in-children",
        asksTemperature = true,
        redFlags =
          listOf(
            RedFlagOption("breathing", "تنفس سخت، تند یا فرورفتن قفسه سینه", true),
            RedFlagOption("lips", "کبودی یا خاکستری شدن لب‌ها و زبان", true),
            RedFlagOption("lethargy", "خواب‌آلودگی شدید، بی‌حالی غیرعادی یا بیدار نشدن", true),
            RedFlagOption("seizure", "تشنج یا لرزش غیرقابل کنترل بدن", true),
            RedFlagOption("dehydration", "کم‌آبی شدید (اشک نداشتن، پوشک خشک طولانی، دهان خیلی خشک)", true),
            RedFlagOption("rash_nonblanch", "دانه پوستی بنفش/قرمز که با فشار محو نمی‌شود", true),
            RedFlagOption("ear_pain_or_crying", "بی‌قراری مداوم یا درد گوش همراه تب", false),
          ),
        educationalStepsFa =
          listOf(
            "به جای عدد تب به تنهایی، حال عمومی کودک، هوشیاری و میزان مصرف مایعات را زیر نظر بگیرید.",
            "لباس کودک را سبک و متعادل نگه دارید؛ از پاشویه با الکل یا آب سرد اکیداً خودداری کنید.",
            "مایعات مناسب سن (شیر مادر/شیرخشک در شیرخوار، آب یا محلول‌های مناسب در کودکان بزرگ‌تر) را به دفعات پیشنهاد دهید.",
            "اگر برای راحتی کودک از تب‌بر تجویز‌شده استفاده می‌کنید، فقط با سرنگ مدرج خوراکی (mL) و پس از تطبیق غلظت دارو اقدام کنید.",
          ),
        escalationCriteriaFa =
          listOf(
            "تب ۳۸ درجه سانتی‌گراد یا بالاتر در نوزاد زیر ۳ ماه (نیازمند ارزیابی فوری اورژانس)",
            "تداوم تب بیش از ۷۲ ساعت یا بدتر شدن حال عمومی کودک",
            "کاهش واضح ادرار یا امتناع کامل از نوشیدن مایعات",
          ),
        review = pendingReviewMeta,
        sources = listOf(srcAapFever, srcNiceFever),
      ),
      SymptomFlow(
        id = "constipation",
        titleFa = "یبوست کودک",
        subtitleFa = "ارزیابی علائم انسداد یا درد شدید پیش از راهنمای تغذیه و مراقبت",
        relatedArticleSlug = "constipation-in-children",
        asksTemperature = false,
        redFlags =
          listOf(
            RedFlagOption("bilious_vomit", "استفراغ مکرر یا استفراغ زرد/سبز رنگ", true),
            RedFlagOption("blood_stool", "خون واضح در مدفوع یا مدفوع قیری‌رنگ", true),
            RedFlagOption("severe_distension", "تورم شدید و سفت شدن شکم همراه بی‌حالی", true),
            RedFlagOption("infant_no_meconium", "دفع نکردن مدفوع در نوزاد تازه متولدشده یا وزن‌گیری نامناسب", false),
            RedFlagOption("painful_fissure", "درد شدید هنگام دفع یا خودداری کودک از اجابت مزاج", false),
          ),
        educationalStepsFa =
          listOf(
            "در کودکان دارای تغذیه تکمیلی یا بزرگ‌تر، مصرف آب کافی در طول روز را افزایش دهید.",
            "میوه‌ها و سبزیجات پرفیبر مناسب سن (مانند گلابی، آلو، زردآلو، حریره جو دوسر) را به‌تدریج در برنامه غذایی بگنجانید.",
            "زمان‌بندی آرام و بدون تنش برای نشستن کودک روی توالت (پس از وعده غذایی) بدون سرزنش یا تنبیه ایجاد کنید.",
            "از استفاده خودسرانه شیاف یا مسهل‌ها بدون نظر پزشک یا داروساز خودداری کنید.",
          ),
        escalationCriteriaFa =
          listOf(
            "بروز استفراغ، تب، تورم شکم یا وجود خون در مدفوع",
            "تداوم یبوست بیش از یک تا دو هفته یا کاهش وزن و بی‌اشتهایی واضح",
          ),
        review = pendingReviewMeta,
        sources = listOf(srcAapFever),
      ),
      SymptomFlow(
        id = "cough",
        titleFa = "سرفه و خس‌خس",
        subtitleFa = "افتراق علائم تنفسی فوری از سرفه خفیف سرماخوردگی",
        relatedArticleSlug = "prolonged-cough",
        asksTemperature = true,
        redFlags =
          listOf(
            RedFlagOption("breathing_effort", "تنفس سریع، خرخر هنگام بازدم یا فرورفتن بین دنده‌ها", true),
            RedFlagOption("cyanosis", "تغییر رنگ لب‌ها یا صورت به آبی/خاکستری", true),
            RedFlagOption("stridor_rest", "صدای سوت بلند هنگام دم در حالت استراحت (خروسک شدید)", true),
            RedFlagOption("choking_history", "احتمال پریدن جسم خارجی یا خوراکی در گلو و شروع ناگهانی سرفه", true),
            RedFlagOption("prolonged_2w", "تداوم سرفه بیش از ۲ تا ۳ هفته یا سرفه همراه تب", false),
          ),
        educationalStepsFa =
          listOf(
            "در شیرخواران گرفتگی بینی با قطره کلرور سدیم و تمیز کردن ملایم بینی می‌تواند تنفس و شیر خوردن را راحت‌تر کند.",
            "هوای منزل را از دود سیگار، قلیان و عطرهای تند دور نگه دارید.",
            "از دادن داروهای ترکیبی ضدسرفه و سرماخوردگی بدون نسخه به کودکان خردسال پرهیز کنید؛ شواهد ایمنی آن‌ها در این سن محدود است.",
            "توصیه درباره عسل فقط برای کودکان بالای ۱ سال مطرح می‌شود و برای نوزادان زیر ۱ سال ممنوع است.",
          ),
        escalationCriteriaFa =
          listOf(
            "هرگونه دشواری در تنفس یا ناتوانی در شیر خوردن و صحبت کردن",
            "سرفه‌ای که بیش از ۲ هفته طول بکشد یا با خس‌خس مداوم همراه باشد",
          ),
        review = pendingReviewMeta,
        sources = listOf(srcNiceFever, srcAapMedication),
      ),
      SymptomFlow(
        id = "diaper_rash",
        titleFa = "سوختگی پای نوزاد",
        subtitleFa = "پایش نشانه‌های عفونت یا تاول پیش از مراقبت موضعی",
        relatedArticleSlug = "diaper-rash-care",
        asksTemperature = false,
        redFlags =
          listOf(
            RedFlagOption("fever_blisters", "تب همراه با تاول، ترشح چرکی یا زخم‌های باز", true),
            RedFlagOption("spreading_severe", "گسترش سریع قرمزی شدید به خارج از ناحیه پوشک و بی‌قراری", false),
            RedFlagOption("no_improve_3d", "بهبود نیافتن یا بدتر شدن پس از ۳ روز مراقبت اصولی", false),
          ),
        educationalStepsFa =
          listOf(
            "پوشک را در فواصل کوتاه‌تر تعویض کنید تا پوست در تماس طولانی با رطوبت نماند.",
            "پوست را با آب ولرم (و در صورت نیاز شوینده بسیار ملایم بدون عطر) تمیز کرده و با ضربات آرام خشک کنید؛ از دستمال مرطوب عطردار پرهیز کنید.",
            "روزانه چند نوبت کوتاه بدون پوشک اجازه دهید پوست در معرض هوا قرار بگیرد.",
            "در هر تعویض پوشک، لایه محافظ زینک اکساید بدون عطر بزنید و از کرم‌های کورتون‌دار بدون تجویز پزشک استفاده نکنید.",
          ),
        escalationCriteriaFa =
          listOf(
            "پیدایش تاول، چرک، خونریزی یا ضایعات نقطه‌ای قرمز روشن در چین‌های پوستی",
            "همراهی با تب یا تداوم بیش از ۳ روز بدون بهبود",
          ),
        review = pendingReviewMeta,
        sources = listOf(srcAapFever),
      ),
      SymptomFlow(
        id = "diarrhea_vomiting",
        titleFa = "استفراغ و اسهال",
        subtitleFa = "تمرکز بر پیشگیری از کم‌آبی و شناخت علائم خطر گوارشی",
        relatedArticleSlug = "when-to-see-doctor",
        asksTemperature = true,
        redFlags =
          listOf(
            RedFlagOption("severe_dehyd", "کم‌آبی شدید (چشم گودافتاده، خواب‌آلودگی، نبود اشک یا ادرار در ۶ تا ۸ ساعت)", true),
            RedFlagOption("green_vomit", "استفراغ سبز/زرد پررنگ یا حاوی خون", true),
            RedFlagOption("unable_fluids", "ناتوانی در نگه داشتن هیچ مایعی و استفراغ پی‌درپی", true),
            RedFlagOption("bloody_diarrhea", "اسهال خونی یا تب بالا", false),
          ),
        educationalStepsFa =
          listOf(
            "مایعات جبرانی (مانند محلول استاندارد ORS با تجویز یا راهنمایی داروساز/پزشک) را به صورت جرعه‌های کوچک و مکرر (مثلاً هر چند دقیقه با قاشق یا سرنگ) بدهید.",
            "شیردهی با شیر مادر را ادامه دهید و نوشیدنی‌های خیلی شیرین، نوشابه یا آب‌میوه صنعتی ندهید.",
            "بدون نظر پزشک از داروهای ضداسهال یا ضدتهوع بزرگسالان استفاده نکنید.",
          ),
        escalationCriteriaFa =
          listOf(
            "بروز نشانه‌های کم‌آبی یا استفراغ مکرر در نوزاد زیر ۶ ماه",
            "مدفوع خونی یا تداوم اسهال شدید",
          ),
        review = pendingReviewMeta,
        sources = listOf(srcNiceFever, srcAapMedication),
      ),
    )

  val medicationMonographs = ManoMedicationSeed.monographs

  val milestones =
    listOf(
      MilestoneItem("m1", AgeBand.NEWBORN, "اجتماعی و ارتباطی", "لبخند پاسخ‌گو به چهره و صدای والدین حدود ۲ ماهگی و آرام شدن با بغل و صدا.", "عدم واکنش به صداهای بلند یا ناتوانی در تعقیب چهره تا پایان ۳ ماهگی.", srcCdcMilestones),
      MilestoneItem("m2", AgeBand.NEWBORN, "حرکتی", "بالا آوردن کوتاه سر هنگام قرارگیری روی شکم (Tummy Time با نظارت بیدار) و تکان دادن متقارن دست و پا.", "سفتی یا شلی شدید غیرعادی بدن یا حرکت بسیار نامتقارن اندام‌ها.", srcCdcMilestones),
      MilestoneItem("m3", AgeBand.INFANT_3_12M, "گفتاری و ارتباطی", "قان و قون، خندیدن، برگشتن به سمت صدا (۶ ماهگی) و ادای هجاهای تکراری مثل بابا/ماما و اشاره کردن (۹ تا ۱۲ ماهگی).", "واکنش نشان ندادن به نام خود یا کم شدن مهارت‌های ارتباطی قبلی.", srcCdcMilestones),
      MilestoneItem("m4", AgeBand.INFANT_3_12M, "حرکتی", "غلتیدن، نشستن بدون کمک (حدود ۶ تا ۹ ماهگی)، چهار دست‌وپا رفتن و گرفتن اشیا با شست و اشاره.", "عدم توانایی نشستن با کمک تا ۹ ماهگی یا ناتوانی در تحمل وزن روی پاها در حمایت.", srcCdcMilestones),
      MilestoneItem("m5", AgeBand.TODDLER_1_3Y, "گفتاری و شناختی", "گفتن چند کلمه معنادار در ۱۸ ماهگی، دنبال کردن دستورهای ساده، و ساخت جملات ۲ کلمه‌ای تا ۲ تا ۳ سالگی.", "کاهش چشمگیر دایره ارتباط، تماس چشمی بسیار محدود یا افت مهارت گفتاری.", srcCdcMilestones),
      MilestoneItem("m6", AgeBand.TODDLER_1_3Y, "حرکتی و استقلال", "راه رفتن مستقل، دویدن پایدار، بالا رفتن از پله با کمک و غذا خوردن با قاشق.", "راه نرفتن مستقل پس از ۱۸ ماهگی یا افت مکرر غیرعادی.", srcCdcMilestones),
      MilestoneItem("m7", AgeBand.PRESCHOOL_3_6Y, "اجتماعی و بازی", "بازی مشارکتی با همسالان، نوبت‌گیری ساده، بیان احساسات و پرسیدن «چرا».", "انزوای کامل، دشواری شدید در برقراری ارتباط یا رفتارهای تکراری آسیب‌زا.", srcCdcMilestones),
      MilestoneItem("m8", AgeBand.PRESCHOOL_3_6Y, "حرکتی ظریف و گفتار", "کشیدن اشکال ساده، پریدن روی یک پا و قابل فهم بودن بخش عمده گفتار برای اطرافیان.", "ناواضح بودن شدید گفتار برای اعضای خانواده پس از ۴ سالگی.", srcCdcMilestones),
      MilestoneItem("m9", AgeBand.SCHOOL_6_PLUS, "رشد و عادات مدرسه", "تنظیم خواب منظم (۹ تا ۱۱ ساعت)، تغذیه متعادل، ارتباط با همسالان و رشد مهارت‌های حرکتی.", "خستگی مداوم روزانه، اضطراب شدید مدرسه‌ای یا افت واضح شنوایی و بینایی (برچسب‌زنی تشخیصی مانند ADHD انجام نمی‌شود).", srcCdcMilestones),
    )

  val deviceGuides =
    listOf(
      DeviceGuide(
        id = "nebulizer_vs_spacer_humidifier",
        titleFa = "تفاوت نبولایزر، اسپیسر (آسان‌نفس) و دستگاه بخور",
        subtitleFa = "این سه وسیله کاربرد متفاوتی دارند و جایگزین یکدیگر نیستند",
        distinctionNoteFa = "نبولایزر و اسپیسر وسایل دارورسانی تنفسی طبق تجویز پزشک هستند؛ دستگاه بخور صرفاً رطوبت‌ساز محیط است و دارو رسانی نمی‌کند.",
        stepsFa =
          listOf(
            "اسپیسر (آسان‌نفس): برای اسپری‌های تنفسی تجویزشده استفاده می‌شود؛ استفاده صحیح با ماسک اندازه صورت کودک سریع و کم‌خطا است.",
            "نبولایزر: داروی مایع تنفسی تجویزشده را به مه ریز تبدیل می‌کند؛ ماسک باید به آرامی روی دهان و بینی قرار گیرد تا دارو به چشم نپاشد.",
            "دستگاه بخور سرد: فقط برای تعدیل خشکی هوا؛ بخور گرم خطر سوختگی در کودکان دارد و افزودن اسانس یا گیاهان به مخزن بخور توصیه نمی‌شود.",
          ),
        cautionNotesFa =
          listOf(
            "مخزن نبولایزر و بخور را پس از هر بار استفاده کاملاً بشویید و خشک کنید تا قارچ و باکتری رشد نکند.",
            "هیچ دارویی را خودسرانه در نبولایزر نریزید و دستگاه بخور را درمان تنگی نفس تلقی نکنید.",
          ),
        review = pendingReviewMeta,
        sources = listOf(srcNiceFever),
      ),
      DeviceGuide(
        id = "thermometry",
        titleFa = "تب‌سنجی درست در سنین مختلف",
        subtitleFa = "انتخاب دماسنج دیجیتال مناسب و پرهیز از دماسنج جیوه‌ای",
        distinctionNoteFa = "لمس پیشانی با دست دقت کافی ندارد؛ دماسنج دیجیتال زیر بغل یا مقعدی (با آموزش ایمن در نوزادان) ملاک معتبرتری است.",
        stepsFa =
          listOf(
            "از دماسنج دیجیتال استاندارد استفاده کنید؛ دماسنج شیشه‌ای جیوه‌ای به دلیل خطر شکستن و مسمومیت توصیه نمی‌شود.",
            "در روش زیر بغل، نوک دماسنج را روی پوست خشک مرکز زیر بغل قرار دهید و بازو را تا صدای بوق نزدیک بدن نگه دارید.",
            "عدد و زمان ثبت تب را با ساعت بنویسید تا در صورت نیاز به پزشک ارائه دهید.",
          ),
        cautionNotesFa =
          listOf(
            "در نوزاد زیر ۳ ماه هر دمای ۳۸.۰ درجه سانتی‌گراد یا بالاتر نیازمند ارزیابی فوری است.",
          ),
        review = pendingReviewMeta,
        sources = listOf(srcAapFever, srcNiceFever),
      ),
      DeviceGuide(
        id = "oral_syringe_liquid",
        titleFa = "اندازه‌گیری داروی مایع با سرنگ مدرج خوراکی (mL)",
        subtitleFa = "پیشگیری از مهم‌ترین خطای دارویی والدین در منزل",
        distinctionNoteFa = "حجم قاشق‌های خانگی می‌تواند از ۲ تا ۱۰ mL متغیر باشد؛ تنها ابزار استاندارد، سرنگ خوراکی مدرج بر حسب میلی‌لیتر است.",
        stepsFa =
          listOf(
            "سرنگ خوراکی تمیز و خوانا با درجه‌بندی mL انتخاب کنید (سرنگ تزریقی بدون سوزن نباشد که درپوش کوچک آن خطر بلع داشته باشد).",
            "بطری سوسپانسیون را تکان دهید و سرنگ را در سطح چشم نگه دارید تا خط مایع دقیقاً روی عدد mL باشد.",
            "در شیرخوار، دارو را آهسته در بخش داخلی گونه بریزید و پس از مصرف، سرنگ را با آب گرم بشویید.",
          ),
        cautionNotesFa =
          listOf(
            "هرگز در تاریکی و بدون خواندن برچسب غلظت دارو، به کودک دارو ندهید.",
          ),
        review = pendingReviewMeta,
        sources = listOf(srcAapMedication),
      ),
      DeviceGuide(
        id = "nasal_wash",
        titleFa = "شست‌وشوی ایمن بینی کودک",
        subtitleFa = "کمک به تنفس راحت‌تر و تغذیه شیرخوار بدون داروی ضداحتقان",
        distinctionNoteFa = "فقط از محلول نرمال سالین استریل (کلرور سدیم ۰.۹٪) یا افشانه مناسب سن کودک استفاده کنید.",
        stepsFa =
          listOf(
            "در شیرخواران ۱ تا ۲ قطره نرمال سالین در هر سوراخ بینی چکانده و با پوآر ملایم و تمیز ترشحات سطحی را تخلیه کنید.",
            "پیش از شیر خوردن یا خواب، تمیز کردن ملایم بینی می‌تواند بی‌قراری ناشی از گرفتگی را کمتر کند.",
          ),
        cautionNotesFa =
          listOf(
            "آب شیر جوشانده‌نشده برای شست‌وشوی بینی استفاده نشود؛ مکش شدید می‌تواند مخاط بینی را متورم‌تر کند.",
          ),
        review = pendingReviewMeta,
        sources = listOf(srcNiceFever),
      ),
      DeviceGuide(
        id = "weight_height_measurement",
        titleFa = "اندازه‌گیری دقیق وزن و قد در منزل",
        subtitleFa = "داده‌برداری دقیق برای ارائه به پزشک بدون قضاوت خودسرانه نمودار",
        distinctionNoteFa = "تفسیر نمودار صدک رشد نیازمند منحنی استاندارد WHO/CDC و بررسی روند چندنوبته توسط پزشک یا مراقب سلامت است.",
        stepsFa =
          listOf(
            "کودک را با حداقل لباس و پوشک خشک، روی ترازوی کالیبره‌شده در سطح صاف وزن کنید.",
            "تا ۲ سالگی قد به صورت خوابیده (طول بدن) و بالای ۲ سالگی ایستاده بدون کفش و با تکیه پاشنه و پشت سر به دیوار اندازه گرفته می‌شود.",
          ),
        cautionNotesFa =
          listOf(
            "یک عدد وزن یا قد به تنهایی مبنای قضاوت رشد نیست؛ تاریخ و شرایط اندازه‌گیری را ثبت کنید.",
          ),
        review = pendingReviewMeta,
        sources = listOf(srcWhoGrowth),
      ),
    )

  val demoPharmacistCases =
    listOf(
      PharmacistCaseDemo(
        id = "CASE-DEMO-101",
        aliasFa = "پرونده نمایشی ۱ — مادر نیما (۳ سال)",
        category = ConsultationCategory.SYMPTOMS,
        ageLabelFa = "۳ سال و ۲ ماه",
        riskFlagFa = "بررسی علائم گوارشی بدون پرچم قرمز",
        status = ConsultationStatus.REVIEWED,
        createdDateFa = "نمونه نمایشی — امروز",
        parentNoteFa = "دفع سفت و با فاصله ۳ روزه همراه با کاهش مصرف آب و سبزیجات.",
        suggestedReplyFa = "پاسخ آموزشی نمونه: افزایش تدریجی آب و فیبر غذایی، بررسی نبود علائم هشدار (استفراغ، خون یا تورم شکم) و پرهیز از ملین بدون نسخه.",
        followUpScheduleFa = "پیگیری ۴۸ ساعته در صورت ادامه یبوست",
        reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
        referenceTitle = "AAP / HealthyChildren — راهنمای مراقبت حمایتی",
      ),
      PharmacistCaseDemo(
        id = "CASE-DEMO-102",
        aliasFa = "پرونده نمایشی ۲ — پدر سارا (۱۴ ماهه)",
        category = ConsultationCategory.OTC,
        ageLabelFa = "۱۴ ماهه",
        riskFlagFa = "تطبیق غلظت سوسپانسیون با سرنگ mL",
        status = ConsultationStatus.RECEIVED,
        createdDateFa = "نمونه نمایشی — دیروز",
        parentNoteFa = "سوال درباره نحوه خواندن درجه‌بندی mL روی سرنگ خوراکی و تفاوت غلظت قطره و شربت.",
        suggestedReplyFa = "پاسخ آموزشی نمونه: پیش از هر بار مصرف غلظت mg/mL بطری بررسی شود و فقط با سرنگ مدرج اندازه‌گیری گردد.",
        followUpScheduleFa = "ارسال راهنمای تصویری اندازه‌گیری با سرنگ",
        reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
        referenceTitle = "AAP — پیشگیری از خطای دارویی در منزل",
      ),
      PharmacistCaseDemo(
        id = "CASE-DEMO-103",
        aliasFa = "پرونده نمایشی ۳ — مادر باران (۵ ماهه)",
        category = ConsultationCategory.SKIN,
        ageLabelFa = "۵ ماهه",
        riskFlagFa = "پایش قرمزی ناحیه پوشک (بدون تب)",
        status = ConsultationStatus.PENDING,
        createdDateFa = "نمونه نمایشی — ۲ روز پیش",
        parentNoteFa = "قرمزی ملایم ناحیه پوشک از دیروز بدون تاول و بدون تب.",
        suggestedReplyFa = "در انتظار بررسی: تعویض مکرر پوشک، شست‌وشو با آب ولرم و لایه محافظ زینک اکساید؛ در صورت تداوم بیش از ۳ روز ویزیت پزشک.",
        followUpScheduleFa = "بررسی مجدد ۷۲ ساعته",
        reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
        referenceTitle = "مراقبت پوست نوزاد — نیازمند تأیید علمی",
      ),
    )
}
