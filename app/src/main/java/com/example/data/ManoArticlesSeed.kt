package com.example.data

import com.example.domain.model.AgeBand
import com.example.domain.model.Article
import com.example.domain.model.TopicCategory

object ManoArticlesSeed {

  private val review = ManoSourcesAndClinicalSeed.pendingReviewMeta

  val starterArticles: List<Article> =
    listOf(
      Article(
        slug = "fever-in-children",
        titleFa = "تب کودک: از اندازه‌گیری درست تا علائم هشدار فوری",
        category = TopicCategory.SYMPTOMS,
        ageBands = AgeBand.entries,
        shortAnswerFa =
          "تب به تنهایی یک بیماری نیست، بلکه واکنش دفاعی بدن است. مهم‌ترین معیار تصمیم‌گیری، سن کودک، حال عمومی و وجود علائم هشدار است؛ به ویژه در نوزادان زیر ۳ ماه هر تب ۳۸ درجه سانتی‌گراد یا بیشتر نیازمند ارزیابی فوری پزشکی است.",
        ageRelevanceFa = "شیرخواران زیر ۳ ماه در اولویت ارزیابی فوری قرار دارند؛ در کودکان بزرگ‌تر تمرکز بر راحتی، مایعات و پایش حال عمومی است.",
        selfCarePointsFa =
          listOf(
            "از دماسنج دیجیتال استاندارد استفاده کنید و عدد دقیق و ساعت اندازه‌گیری را یادداشت نمایید.",
            "لباس سبک بپوشانید و دمای اتاق را متعادل نگه دارید.",
            "تغذیه با شیر مادر/شیرخشک یا نوشیدن مایعات را به دفعات کوچک ادامه دهید تا از کم‌آبی پیشگیری شود.",
            "در صورت تجویز تب‌بر توسط پزشک، فقط با سرنگ مدرج بر حسب mL و پس از چک کردن غلظت دارو مصرف کنید.",
          ),
        redFlagsFa =
          listOf(
            "سن کمتر از ۳ ماه با دمای ۳۸.۰ درجه سانتی‌گراد یا بالاتر (اورژانس فوری).",
            "تنفس سخت، تند یا خس‌خس، یا کبودی لب‌ها و زبان.",
            "خواب‌آلودگی غیرعادی، تشنج، بی‌حالی شدید یا پاسخ ندادن معمول به والدین.",
            "نشانه‌های کم‌آبی (نبود اشک، خشکی شدید دهان، کاهش واضح پوشک خیس).",
            "دانه پوستی بنفش یا قرمز که با فشار لیوان شیشه‌ای محو نمی‌شود.",
          ),
        caregiverMistakesFa =
          listOf(
            "پاشویه با الکل یا آب یخ که می‌تواند باعث لرز و افت ناگهانی دما شود.",
            "دادن هم‌زمان چند داروی تب‌بر یا اشتباه گرفتن غلظت قطره با شربت.",
            "پوشاندن لباس زیاد به کودک برای «عرق کردن».",
          ),
        nextStepCtaLabelFa = "شروع ارزیابی مرحله‌به‌مرحله تب",
        nextStepRouteKey = "symptoms:fever",
        relatedSlugs = listOf("safe-liquid-medicine", "when-to-see-doctor"),
        sources = listOf(ManoSourcesAndClinicalSeed.srcAapFever, ManoSourcesAndClinicalSeed.srcNiceFever),
        review = review,
        isHighRisk = true,
      ),
      Article(
        slug = "prolonged-cough",
        titleFa = "سرفه‌ای که طول کشیده: چه مراقبتی ایمن است و چه زمانی ویزیت لازم است؟",
        category = TopicCategory.SYMPTOMS,
        ageBands = listOf(AgeBand.INFANT_3_12M, AgeBand.TODDLER_1_3Y, AgeBand.PRESCHOOL_3_6Y, AgeBand.SCHOOL_6_PLUS),
        shortAnswerFa =
          "سرفه پس از عفونت‌های ویروسی خفیف ممکن است تا حدود دو هفته باقی بماند، اما سرفه طولانی‌تر، خس‌خس یا هر گونه دشواری تنفسی نیازمند معاینه پزشک است و نباید با داروهای ترکیبی بدون نسخه سرکوب شود.",
        ageRelevanceFa = "در شیرخواران زیر ۱ سال مصرف عسل ممنوع است و داروهای ترکیبی ضدسرفه بدون نسخه در کودکان خردسال توصیه نمی‌شود.",
        selfCarePointsFa =
          listOf(
            "استفاده از قطره کلرور سدیم برای باز نگه داشتن راه بینی پیش از شیر خوردن یا خواب.",
            "دور نگه داشتن کامل کودک از دود سیگار، اسپری‌های تند و آلودگی هوای داخل خانه.",
            "تأمین مایعات گرم/ملایم متناسب با سن کودک.",
          ),
        redFlagsFa =
          listOf(
            "نفس‌نفس زدن، فرورفتن پوست بین دنده‌ها یا زیر گلو، و کبودی لب‌ها.",
            "شروع ناگهانی سرفه شدید پس از احتمال بلع یا پریدن خوراکی در گلو.",
            "سرفه‌ای که بیش از ۲ تا ۳ هفته ادامه یابد یا همراه تب جدید شود.",
          ),
        caregiverMistakesFa =
          listOf(
            "خرید و مصرف خودسرانه شربت‌های ضدسرفه بزرگسالان یا ترکیبی برای کودکان خردسال.",
            "استفاده از بخور گرم نزدیک تخت کودک که خطر سوختگی دارد یا افزودن اسانس به دستگاه بخور.",
          ),
        nextStepCtaLabelFa = "بررسی علائم تنفسی و سرفه",
        nextStepRouteKey = "symptoms:cough",
        relatedSlugs = listOf("fever-in-children", "when-to-see-doctor"),
        sources = listOf(ManoSourcesAndClinicalSeed.srcNiceFever, ManoSourcesAndClinicalSeed.srcAapMedication),
        review = review,
        isHighRisk = true,
      ),
      Article(
        slug = "constipation-in-children",
        titleFa = "یبوست کودک: اصلاح روتین و تغذیه بدون خوددرمانی",
        category = TopicCategory.NUTRITION,
        ageBands = listOf(AgeBand.INFANT_3_12M, AgeBand.TODDLER_1_3Y, AgeBand.PRESCHOOL_3_6Y, AgeBand.SCHOOL_6_PLUS),
        shortAnswerFa =
          "یبوست در کودکان بیشتر به دلیل تغییر رژیم غذایی، کم‌نوشیدن آب یا نگه داشتن مدفوع هنگام بازی یا اضطراب توالت رخ می‌دهد. اصلاح ملایم تغذیه و عادت‌سازی آرام، پایه مراقبت است.",
        ageRelevanceFa = "در نوزادان منحصراً شیرخوار الگوی دفع متفاوت است و پیش از هر مداخله یا تغییر شیرخشک باید با پزشک مشورت شود.",
        selfCarePointsFa =
          listOf(
            "افزایش تدریجی آب آشامیدنی در کودکان بالای ۶ ماه.",
            "گنجاندن میوه‌های ملین ملایم مانند گلابی، آلو و سبزیجات پرفیبر در برنامه غذایی.",
            "ایجاد زمان کوتاه و بدون استرس برای نشستن روی توالت حدود ۱۵ تا ۲۰ دقیقه پس از غذا.",
          ),
        redFlagsFa =
          listOf(
            "استفراغ مکرر، به خصوص استفراغ زرد یا سبز.",
            "وجود خون در مدفوع، تب، بی‌حالی یا سفتی و تورم واضح شکم.",
            "تداوم یبوست بیش از یک تا دو هفته یا اختلال وزن‌گیری.",
          ),
        caregiverMistakesFa =
          listOf(
            "استفاده مکرر از شیاف گلیسیرین، تنقیه یا ملین‌های بدون نسخه بدون نظر پزشک/داروساز.",
            "تنش، سرزنش یا اصرار عصبی هنگام آموزش توالت.",
          ),
        nextStepCtaLabelFa = "مسیر بررسی یبوست کودک",
        nextStepRouteKey = "symptoms:constipation",
        relatedSlugs = listOf("formula-selection-guide", "when-to-see-doctor"),
        sources = listOf(ManoSourcesAndClinicalSeed.srcAapFever),
        review = review,
      ),
      Article(
        slug = "diaper-rash-care",
        titleFa = "سوختگی پای نوزاد: مراقبت ملایم پوست و تمایز از عفونت",
        category = TopicCategory.SKIN,
        ageBands = listOf(AgeBand.NEWBORN, AgeBand.INFANT_3_12M, AgeBand.TODDLER_1_3Y),
        shortAnswerFa =
          "بیشتر سوختگی‌های پوشک بر اثر تماس طولانی پوست با رطوبت و اصطکاک ایجاد می‌شوند و با تعویض زودبه‌زود، شست‌وشوی ملایم، هواخوری و کرم محافظ زینک اکساید ظرف ۲ تا ۳ روز بهتر می‌شوند.",
        ageRelevanceFa = "مناسب نوزادان و نوپایان در دوره استفاده از پوشک.",
        selfCarePointsFa =
          listOf(
            "تعویض منظم پوشک بلافاصله پس از خیس شدن یا دفع.",
            "شست‌وشو با آب ولرم و خشک کردن با ضربه آرام حوله نخی نرم (بدون کشیدن).",
            "استفاده از لایه ضخیم و یکنواخت پماد محافظ زینک اکساید بدون عطر.",
          ),
        redFlagsFa =
          listOf(
            "تاول، ترشح چرکی، زخم باز یا تب.",
            "دانه‌های قرمز نقطه‌ای روشن در چین‌های پوستی که پس از ۳ روز مراقبت خانگی بهبود نمی‌یابند.",
          ),
        caregiverMistakesFa =
          listOf(
            "استفاده خودسرانه از پمادهای کورتونی قوی (مانند کلوبتازول یا بتامتازون) در ناحیه پوشک.",
            "استفاده از پودر بچه یا دستمال مرطوب‌های حاوی الکل و اسانس.",
          ),
        nextStepCtaLabelFa = "مشاهده بخش پوست و پوشک",
        nextStepRouteKey = "skin",
        relatedSlugs = listOf("when-to-see-doctor"),
        sources = listOf(ManoSourcesAndClinicalSeed.srcAapFever),
        review = review,
      ),
      Article(
        slug = "safe-liquid-medicine",
        titleFa = "مصرف صحیح داروی مایع: چرا سرنگ mL و بررسی غلظت حیاتی است؟",
        category = TopicCategory.MEDICINES,
        ageBands = AgeBand.entries,
        shortAnswerFa =
          "قاشق‌های خانگی اندازه ثابتی ندارند و می‌توانند خطای دو تا سه برابری ایجاد کنند. برای هر داروی مایع کودک، غلظت درج‌شده روی جعبه را چک کرده و فقط از سرنگ مدرج خوراکی (mL) استفاده کنید.",
        ageRelevanceFa = "برای تمامی گروه‌های سنی از نوزاد تا کودک مدرسه‌ای.",
        selfCarePointsFa =
          listOf(
            "همیشه برچسب غلظت (مانند mg/mL) را در نور کافی بخوانید.",
            "مقدار را در سطح چشم با خط پلانجر سرنگ تطبیق دهید.",
            "دارو را با نام کامل و ساعت مصرف در دفترچه یا یادداشت والدین ثبت کنید تا خطای تکرار رخ ندهد.",
          ),
        redFlagsFa =
          listOf(
            "احتمال مصرف بیش از حد، اشتباه در غلظت یا خواب‌آلودگی غیرعادی پس از دارو (تماس فوری با اورژانس و مرکز اطلاعات دارویی).",
          ),
        caregiverMistakesFa =
          listOf(
            "استفاده از قاشق چای‌خوری یا غذاخوری به جای سرنگ مدرج.",
            "مخلوط کردن دارو داخل یک شیشه کامل شیر که اگر کودک تمامش را نخورد، دوز نامشخص می‌ماند.",
          ),
        nextStepCtaLabelFa = "ورود به بخش داروهای کودک",
        nextStepRouteKey = "medicines",
        relatedSlugs = listOf("fever-in-children", "when-to-see-doctor"),
        sources = listOf(ManoSourcesAndClinicalSeed.srcAapMedication),
        review = review,
        isHighRisk = true,
      ),
      Article(
        slug = "formula-selection-guide",
        titleFa = "انتخاب شیرخشک و تغذیه نوزاد بر اساس نیاز بالینی، نه تبلیغات",
        category = TopicCategory.NUTRITION,
        ageBands = listOf(AgeBand.NEWBORN, AgeBand.INFANT_3_12M),
        shortAnswerFa =
          "شیر مادر تغذیه مرجع برای شیرخوار است؛ در شرایطی که شیرخشک نیاز است، انتخاب نوع شیرخشک (استاندارد، ضدرفلاکس یا هایپوآلرژنیک) باید بر پایه نیاز واقعی شیرخوار و نظر پزشک باشد، نه رتبه‌بندی‌های تبلیغاتی.",
        ageRelevanceFa = "نوزادان ۰ تا ۱۲ ماه و انتقال به تغذیه کمکی از حدود ۶ ماهگی.",
        selfCarePointsFa =
          listOf(
            "پیمانه داخل همان قوطی را با آب جوشیده سردشده و دقیقاً مطابق نسبت درج‌شده روی قوطی آماده کنید.",
            "شیرخشک رقیق‌شده یا غلیظ‌شده می‌تواند به تعادل آب و نمک نوزاد آسیب بزند.",
            "تعویض مداوم برند شیرخشک بدون دلیل پزشکی ممکن است بی‌قراری گوارشی را بیشتر کند.",
          ),
        redFlagsFa =
          listOf(
            "استفراغ جهنده، خون در مدفوع، کهیر یا کاهش وزن‌گیری و کم‌ادرار شدن نوزاد.",
          ),
        caregiverMistakesFa =
          listOf(
            "گرم کردن شیرخشک در مایکروویو که نقاط داغ خطرناک ایجاد می‌کند.",
            "شروع زودهنگام شیر گاو پیش از ۱ سالگی به عنوان جایگزین شیر مادر یا شیرخشک.",
          ),
        nextStepCtaLabelFa = "مشاهده پرسش‌های پرتکرار تغذیه",
        nextStepRouteKey = "faq:شیرخشک",
        relatedSlugs = listOf("constipation-in-children", "growth-and-development"),
        sources = listOf(ManoSourcesAndClinicalSeed.srcWhoGrowth),
        review = review,
      ),
      Article(
        slug = "growth-and-development",
        titleFa = "رشد و تکامل کودک: پایش نقاط عطف بدون برچسب‌زنی",
        category = TopicCategory.NUTRITION,
        ageBands = AgeBand.entries,
        shortAnswerFa =
          "هر کودک مسیر رشد و مهارت‌آموزی خاص خود را دارد، اما پیگیری نقاط عطف حرکتی، گفتاری و اجتماعی به والدین کمک می‌کند تأخیرهای احتمالی را زودتر با پزشک در میان بگذارند.",
        ageRelevanceFa = "از بدو تولد تا سن مدرسه با محورهای حرکتی، گفتاری، شناختی و خواب.",
        selfCarePointsFa =
          listOf(
            "تداوم ویزیت‌های دوره‌ای رشد (وزن، قد، دور سر) در مراکز معتبر سلامت و ثبت تاریخ اندازه‌گیری.",
            "گفت‌وگو، کتاب‌خوانی کوتاه روزانه و بازی بدون صفحه نمایش برای تقویت زبان و تعامل.",
            "روتین خواب منظم و محیط ایمن متناسب با سن.",
          ),
        redFlagsFa =
          listOf(
            "از دست رفتن مهارتی که کودک قبلاً به دست آورده بود (پسرفت تکاملی) در هر سنی نیازمند ارزیابی است.",
          ),
        caregiverMistakesFa =
          listOf(
            "مقایسه اضطراب‌آور کودک با همسالان یا برچسب‌زنی زودهنگام مانند ADHD بدون ارزیابی تخصصی.",
          ),
        nextStepCtaLabelFa = "مشاهده چک‌لیست رشد و تکامل",
        nextStepRouteKey = "growth",
        relatedSlugs = listOf("formula-selection-guide"),
        sources = listOf(ManoSourcesAndClinicalSeed.srcCdcMilestones, ManoSourcesAndClinicalSeed.srcWhoGrowth),
        review = review,
      ),
      Article(
        slug = "when-to-see-doctor",
        titleFa = "چه زمانی باید سریع به پزشک یا اورژانس ۱۱۵ مراجعه کنیم؟",
        category = TopicCategory.SYMPTOMS,
        ageBands = AgeBand.entries,
        shortAnswerFa =
          "حس والدین درباره تغییر غیرعادی حال کودک ارزشمند است. وجود نشانه‌های تنفسی، تغییر هوشیاری، تشنج، کم‌آبی شدید یا تب در نوزاد زیر ۳ ماه به معنای مراجعه فوری و بدون انتظار به پزشک یا اورژانس است.",
        ageRelevanceFa = "قابل استفاده به عنوان راهنمای ایمنی سریع برای تمام سنین کودک.",
        selfCarePointsFa =
          listOf(
            "در وضعیت‌های غیرفوری اما نگران‌کننده، شرح حال دقیق (زمان شروع علائم، دمای ثبت‌شده، میزان مایعات و تعداد پوشک خیس) را آماده کنید.",
            "برای دسترسی سریع، شماره تماس اورژانس ۱۱۵ و نزدیک‌ترین درمانگاه شبانه‌روزی اطفال را ذخیره داشته باشید.",
          ),
        redFlagsFa =
          listOf(
            "دشواری تنفس، خرخر بازدمی، کبودی لب‌ها یا توقف تنفس.",
            "خواب‌آلودگی شدید، بیدار نشدن، بی‌قراری تسکین‌ناپذیر یا تشنج.",
            "تب ۳۸.۰ درجه سانتی‌گراد یا بالاتر در نوزاد زیر ۳ ماه.",
            "کم‌آبی شدید، استفراغ زرد/سبز، یا دانه پوستی بنفش که با فشار محو نمی‌شود.",
          ),
        caregiverMistakesFa =
          listOf(
            "صبر کردن تا صبح در حضور علائم هشدار تنفسی یا تب نوزاد زیر ۳ ماه.",
            "تکیه بر پاسخ‌های عمومی اینترنتی به جای معاینه حضوری هنگام افت حال عمومی.",
          ),
        nextStepCtaLabelFa = "شروع هدایتگر علائم",
        nextStepRouteKey = "symptoms:fever",
        relatedSlugs = listOf("fever-in-children", "safe-liquid-medicine"),
        sources = listOf(ManoSourcesAndClinicalSeed.srcAapFever, ManoSourcesAndClinicalSeed.srcNiceFever),
        review = review,
        isHighRisk = true,
      ),
    )
}
