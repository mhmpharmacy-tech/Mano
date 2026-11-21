package com.example.data

import com.example.domain.model.*

/** Original Persian summaries of specifically read sections; no textbook pages are bundled. */
object ManoTextbookEvidence {
  val revisedReview = ClinicalReviewMeta(
    status = ReviewStatus.NEEDS_CLINICAL_REVIEW,
    lastReviewedDate = "تطبیق منابع: ۲۰۲۶/۱۱/۲۰؛ تأیید انسانی این اصلاحات ثبت نشده",
    nextReviewDate = "پیش از انتشار این نسخه",
    reviewerIdentity = null,
    notes = "خلاصهٔ تألیفی از بخش‌های مشخص کتاب؛ تأیید نسخهٔ قبلی به این اصلاحات منتقل نمی‌شود.",
  )

  private fun book(id: String, title: String, publisher: String, locator: String) = SourceReference(
    id = id, title = "$title — $locator", publisher = publisher, url = "",
    publishedOrUpdated = "نسخهٔ بارگذاری‌شده؛ بررسی بخش مرتبط در ۲۰۲۶/۱۱/۲۰",
  )

  val nelsonGrowth = book("nelson8_growth", "Nelson Essentials of Pediatrics, 8th ed. (2019)", "Elsevier",
    "فصل‌های ۵ و ۶؛ چاپی ۱۱–۱۳؛ PDF ۲۷–۲۹")
  val nelsonDevelopment = book("nelson8_development", "Nelson Essentials of Pediatrics, 8th ed. (2019)", "Elsevier",
    "فصل‌های ۷ و ۸؛ چاپی ۱۴–۱۷؛ PDF ۳۰–۳۳")
  val nelsonFever = book("nelson8_fever", "Nelson Essentials of Pediatrics, 8th ed. (2019)", "Elsevier",
    "فصل ۹۶؛ چاپی ۳۶۸–۳۶۹؛ PDF ۳۹۰–۳۹۱")
  val nelsonDiarrhea = book("nelson8_gastroenteritis", "Nelson Essentials of Pediatrics, 8th ed. (2019)", "Elsevier",
    "فصل ۱۱۲؛ چاپی ۴۱۰–۴۱۲؛ PDF ۴۳۲–۴۳۴")
  val nelsonConstipation = book("nelson8_constipation", "Nelson Essentials of Pediatrics, 8th ed. (2019)", "Elsevier",
    "فصل ۱۲۶، Constipation and Encopresis؛ چاپی ۴۷۵–۴۷۶؛ PDF ۴۹۷–۴۹۸")
  // Uploaded filename says 2023; the title/copyright pages identify the 12th edition, ©2024.
  val appliedMedication = book("applied12_medication", "Applied Therapeutics: The Clinical Use of Drugs, 12th ed. (©2024)", "Wolters Kluwer",
    "فصل ۱۰۴، Administering Medication to Children؛ PDF ۸۰۷۳–۸۰۷۵")
  val appliedFever = book("applied12_fever", "Applied Therapeutics, 12th ed. (©2024)", "Wolters Kluwer",
    "فصل ۱۰۴، Fever؛ PDF ۸۰۸۰–۸۰۸۱")
  val appliedCold = book("applied12_cold", "Applied Therapeutics, 12th ed. (©2024)", "Wolters Kluwer",
    "فصل ۱۰۴، Case 104-4؛ PDF ۸۰۸۳–۸۰۸۴")
  val appliedSkin = book("applied12_diaper", "Applied Therapeutics, 12th ed. (©2024)", "Wolters Kluwer",
    "فصل ۱۰۴، Case 104-2؛ PDF ۸۰۷۸–۸۰۷۹")
  val appliedDiarrhea = book("applied12_gastroenteritis", "Applied Therapeutics, 12th ed. (©2024)", "Wolters Kluwer",
    "فصل ۱۰۴، Viral Gastroenteritis؛ PDF ۸۰۸۹–۸۰۹۰ و ۸۰۹۴")
  val allSources = listOf(nelsonGrowth, nelsonDevelopment, nelsonFever, nelsonDiarrhea,
    nelsonConstipation, appliedMedication, appliedFever, appliedCold, appliedSkin, appliedDiarrhea)

  fun reviseFlow(flow: SymptomFlow): SymptomFlow {
    val sources = when (flow.id) {
      "fever" -> flow.sources + listOf(nelsonFever, appliedFever)
      "runny_nose", "wet_cough" -> return flow.copy(sources = flow.sources + appliedCold)
      "cough" -> listOf(appliedCold)
      "constipation" -> listOf(nelsonConstipation)
      "diaper_rash" -> listOf(appliedSkin)
      "diarrhea_vomiting" -> listOf(nelsonDiarrhea, appliedDiarrhea)
      else -> return flow
    }
    val extra = when (flow.id) {
      "fever" -> listOf("هدف تب‌بر، کم کردن ناراحتی کودک است؛ پایین آمدن تب، بیماری جدی را رد نمی‌کند و تب‌بر پیشگیری قطعی از تشنج نیست.")
      "diarrhea_vomiting" -> listOf("ORS را دقیقاً با حجم آب نوشته‌شده روی همان بسته آماده کنید؛ پودر را غلیظ‌تر نکنید. به کودک با کاهش هوشیاری چیزی نخورانید.")
      "constipation" -> listOf("تعداد کم دفع به‌تنهایی یبوست نیست؛ سفتی مدفوع، درد، زمان شروع و روند رشد را گزارش کنید.")
      else -> emptyList()
    }
    return flow.copy(sources = sources, review = revisedReview,
      educationalStepsFa = flow.educationalStepsFa + extra,
      escalationCriteriaFa = if (flow.id == "fever") flow.escalationCriteriaFa + listOf(
        "در سن ۳ تا ۶ ماه، دمای ۳۹ یا بیشتر: تماس فوری برای ارزیابی سریع پزشک؛ منتظر پیام آنلاین نمانید.",
        "تب ۵ روز یا بیشتر، حتی با حال نسبتاً خوب، نیاز به ارزیابی پزشک دارد.") else flow.escalationCriteriaFa)
  }

  fun reviseArticle(article: Article): Article {
    val refs = when (article.slug) {
      "fever-in-children" -> listOf(nelsonFever, appliedFever)
      "liquid-medicine-safety" -> listOf(appliedMedication)
      "prolonged-cough" -> listOf(appliedCold)
      "constipation-in-children" -> listOf(nelsonConstipation)
      "diaper-rash-care" -> listOf(appliedSkin)
      else -> return article
    }
    return article.copy(sources = (article.sources + refs).distinctBy { it.id }, review = revisedReview)
  }

  private fun faq(id: String, question: String, category: TopicCategory, tags: List<String>,
    summary: String, steps: List<String>, warnings: List<String>, source: SourceReference) = FaqItem(
      id = id, questionFa = question, category = category, ageBands = AgeBand.entries, tags = tags,
      summaryFa = summary, whatToDoFa = steps, whenToSeeDoctorFa = warnings, source = source,
      review = revisedReview,
    )

  val faqs = listOf(
    faq("evidence_growth_trend", "آیا یک صدک طبیعی یعنی رشد کودک حتماً خوب است؟", TopicCategory.NUTRITION,
      listOf("رشد", "قد", "وزن", "صدک"),
      "یک اندازه‌گیری برای قضاوت کافی نیست؛ روند چند نوبت و کیفیت اندازه‌گیری اهمیت دارند. وزن نسبت به سن، معیار تشخیص چاقی نیست.",
      listOf("سن دقیق، جنس مورد استفاده در نمودار، تاریخ و روش اندازه‌گیری را ثبت کنید.", "عدد غیرمنتظره را با ابزار مناسب دوباره اندازه بگیرید و سوابق را همراه ببرید."),
      listOf("افت پیوستهٔ منحنی، توقف وزن‌گیری یا نگرانی درباره تغذیه را با پزشک مطرح کنید؛ برای نتیجهٔ سبز برنامه منتظر نمانید."), nelsonGrowth),
    faq("evidence_milestone_loss", "اگر کودک مهارتی را که داشته از دست بدهد چه کنم؟", TopicCategory.NUTRITION,
      listOf("تکامل", "مهارت", "پسرفت", "رشد"),
      "چک‌لیست مانو ابزار گفت‌وگو است؛ تشخیص یا آزمون استاندارد غربالگری نیست. از دست رفتن مهارت یا نگرانی والد نیاز به ارزیابی دارد.",
      listOf("مهارت قبلی، زمان تغییر و نمونه‌های رفتاری را یادداشت کنید.", "برای بررسی تکامل و در صورت لزوم شنوایی یا بینایی وقت بگیرید."),
      listOf("برای از دست رفتن مهارت، ارزیابی را عقب نیندازید؛ همراهی با تشنج، اختلال هوشیاری یا ضعف ناگهانی نیازمند اقدام فوری است."), nelsonDevelopment),
    faq("evidence_fever_response", "اگر تب با دارو پایین آمد، بیماری جدی رد می‌شود؟", TopicCategory.SYMPTOMS,
      listOf("تب", "تب بر", "تشنج", "استامینوفن"),
      "خیر؛ پاسخ به تب‌بر معیار رد عفونت جدی نیست. تصمیم مراجعه به سن، حال عمومی و نشانه‌های هشدار وابسته است.",
      listOf("زمان، عدد و محل اندازه‌گیری دما را ثبت کنید.", "تنفس، هوشیاری، شیر خوردن و ادرار را پایش کنید."),
      listOf("تب یا احتمال تب زیر ۳ ماه: ارزیابی فوری حضوری؛ برای ۳ تا ۶ ماه با دمای ۳۹ یا بیشتر، تماس فوری با پزشک.", "تنگی نفس، بیدار نشدن یا تشنج: اورژانس ۱۱۵ در ایران."), ManoSourcesAndClinicalSeed.srcNiceFever),
    faq("evidence_liquid_strength", "آیا حجم دو شربت با غلظت متفاوت را می‌توان یکسان داد؟", TopicCategory.MEDICINES,
      listOf("غلظت", "سرنگ", "mg", "mL", "قطره"),
      "خیر؛ mg مقدار مادهٔ مؤثره و mL حجم مایع است. نام مشابه، به معنی غلظت یا حجم مصرف یکسان نیست.",
      listOf("نام، مادهٔ مؤثره، غلظت روی بطری و نسخه را با داروساز تطبیق دهید.", "از وسیلهٔ مدرج با واحد mL استفاده کنید و روش اندازه‌گیری را به داروساز نشان دهید.", "محصولات ترکیبی را برای تکرار همان مادهٔ مؤثره بررسی کنید."),
      listOf("اگر احتمال مقدار اضافی یا اشتباه در غلظت هست، منتظر بروز علامت نمانید و فوراً کمک پزشکی بگیرید."), appliedMedication),
    faq("evidence_milk_mixing", "می‌توان داروی کودک را در یک شیشهٔ کامل شیر مخلوط کرد؟", TopicCategory.MEDICINES,
      listOf("شیر", "دارو", "مخلوط", "سرنگ"),
      "اگر کودک همهٔ شیر را نخورد، مقدار دریافت‌شده نامعلوم می‌شود؛ سازگاری دارو با غذا یا شیر نیز برای هر دارو متفاوت است.",
      listOf("پیش از مخلوط کردن، دستور همان فرآورده را از داروساز بپرسید.", "اگر مخلوط کردن مجاز است، مقدار کمی از غذای مناسب که کودک کامل می‌خورد انتخاب کنید؛ دارو را به کل وعده اضافه نکنید.", "در استفراغ یا بیرون ریختن دارو، خودسرانه یک نوبت کامل دیگر تکرار نکنید."),
      listOf("وقتی مقدار دریافت‌شده معلوم نیست، برای تصمیم درباره تکرار دوز با داروساز یا پزشک تماس بگیرید."), appliedMedication),
    faq("evidence_cold_combinations", "چرا شربت ترکیبی سرماخوردگی برای کودک خردسال مناسب نیست؟", TopicCategory.MEDICINES,
      listOf("سرفه", "سرماخوردگی", "آنتی هیستامین", "ترکیبی"),
      "در کودکان خردسال، فایدهٔ بسیاری از این ترکیبات ثابت نشده و خطای مقدار مصرف یا تکرار ترکیبات می‌تواند عارضهٔ جدی ایجاد کند؛ محدودیت سن باید از برچسب همان فرآورده بررسی شود.",
      listOf("از داروساز بخواهید مواد مؤثرهٔ همهٔ فرآورده‌ها را کنار هم بررسی کند.", "برای گرفتگی بینی، درباره سالین و پاک کردن ملایم بینی راهنمایی بگیرید."),
      listOf("تنفس سخت، کبودی، خواب‌آلودگی شدید یا شروع سرفه بعد از پریدن غذا در گلو نیازمند ارزیابی فوری است."), appliedCold),
    faq("evidence_ors", "در اسهال کودک، مهم‌ترین کار چیست؟", TopicCategory.SYMPTOMS,
      listOf("اسهال", "ORS", "کم آبی", "استفراغ"),
      "ارزیابی کم‌آبی و جبران مایعات اولویت دارد؛ داروی ضداسهال جای ORS را نمی‌گیرد.",
      listOf("ORS استاندارد را طبق حجم آب نوشته‌شده روی همان بسته آماده کنید.", "در کودک هوشیار، جرعه‌های کم و مکرر پیشنهاد دهید و شیردهی را ادامه دهید.", "دفعات ادرار، استفراغ و مدفوع را یادداشت کنید؛ ضداسهال یا ضدتهوع را خودسرانه ندهید."),
      listOf("کاهش هوشیاری، ادرار بسیار کم، ناتوانی در نوشیدن، استفراغ سبز یا خونی نیازمند ارزیابی فوری است.", "شیرخوار کوچک، بیماری زمینه‌ای یا خون در مدفوع نیاز به تماس زودهنگام با پزشک دارد."), appliedDiarrhea),
    faq("evidence_diaper_care", "برای سوختگی پوشک از کجا شروع کنم؟", TopicCategory.SKIN,
      listOf("پوشک", "زینک", "پوست", "کرم"),
      "کم کردن تماس پوست با رطوبت و مدفوع و استفاده از لایهٔ محافظ، پایهٔ مراقبت است؛ همهٔ راش‌ها قارچی نیستند.",
      listOf("پوشک خیس را زود عوض کنید، با آب ولرم تمیز و آرام خشک کنید.", "در هر تعویض از محافظ سادهٔ زینک اکساید یا پترولاتوم استفاده کنید.", "کرم ترکیبی یا کورتون قوی را خودسرانه زیر پوشک نزنید."),
      listOf("عدم بهبود طی ۳ روز، درگیری چین‌ها یا ضایعات اطراف راش نیاز به بررسی دارد؛ تب، تاول یا حال عمومی بد را فوری پیگیری کنید."), appliedSkin),
  )
}
