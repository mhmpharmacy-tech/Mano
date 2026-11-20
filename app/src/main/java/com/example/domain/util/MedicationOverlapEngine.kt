package com.example.domain.util

import com.example.domain.model.MedicationMonograph
import com.example.ui.components.AlertSeverity

data class MedicationOverlapResult(
  val severity: AlertSeverity,
  val titleFa: String,
  val summaryFa: String,
  val actionStepsFa: List<String>,
)

object MedicationOverlapEngine {

  fun evaluatePair(
    first: MedicationMonograph,
    second: MedicationMonograph,
  ): MedicationOverlapResult {
    if (first.slug == second.slug) {
      return MedicationOverlapResult(
        severity = AlertSeverity.DANGER,
        titleFa = "هشدار مصرف تکراری یک دارو",
        summaryFa = "هر دو انتخاب مربوط به «${first.nameFa}» است. دادن دو نوبت تکراری یا دو برند متفاوت از یک ماده مؤثره (مثلاً قطره و شربت استامینوفن) خطر مسمومیت دارد.",
        actionStepsFa =
          listOf(
            "زمان آخرین نوبت را در دفترچه دارو بررسی کنید.",
            "هرگز دو فرم مختلف یک دارو را هم‌زمان ندهید.",
          ),
      )
    }

    val slugs = setOf(first.slug, second.slug)

    if ("acetaminophen-pediatric" in slugs && "otc-cough-cold-caution" in slugs) {
      return MedicationOverlapResult(
        severity = AlertSeverity.DANGER,
        titleFa = "خطر مسمومیت مضاعف با استامینوفن پنهان",
        summaryFa = "بسیاری از شربت‌های ترکیبی سرفه و سرماخوردگی کودکان حاوی استامینوفن (پاراستامول) هستند. مصرف هم‌زمان با قطره یا شربت استامینوفن می‌تواند به کبد آسیب جدی بزند.",
        actionStepsFa =
          listOf(
            "فهرست اجزای روی جعبه شربت سرماخوردگی را با داروساز چک کنید.",
            "در کودکان خردسال بدون نسخه، شربت ترکیبی سرماخوردگی ندهید.",
          ),
      )
    }

    if ("cetirizine-antihistamine-pediatric" in slugs && "otc-cough-cold-caution" in slugs) {
      return MedicationOverlapResult(
        severity = AlertSeverity.DANGER,
        titleFa = "تداخل مضاعف آنتی‌هیستامین و خطر خواب‌آلودگی شدید",
        summaryFa = "اکثر شربت‌های سرماخوردگی دارای آنتی‌هیستامین (مانند دیفن‌هیدرامین یا کلرفنیرامین) هستند؛ مصرف هم‌زمان با سیتیریزین یا آنتی‌هیستامین دیگر باعث تداخل، خواب‌آلودگی غیرعادی یا تپش قلب می‌شود.",
        actionStepsFa =
          listOf(
            "دو داروی ضدحساسیت/ضدآبریزش را بدون نظر پزشک با هم مصرف نکنید.",
            "برای گرفتگی بینی شیرخوار از قطره نرمال سالین ساده استفاده کنید.",
          ),
      )
    }

    if ("acetaminophen-pediatric" in slugs && "ibuprofen-pediatric" in slugs) {
      return MedicationOverlapResult(
        severity = AlertSeverity.CAUTION,
        titleFa = "احتیاط در مصرف هم‌زمان یا چرخشی دو تب‌بر",
        summaryFa = "مصرف هم‌زمان یا تناوبی استامینوفن و ایبوپروفن بدون برنامه ساعتی مکتوب پزشک، یکی از شایع‌ترین دلایل خطای دوز و فاصله زمانی در منزل است.",
        actionStepsFa =
          listOf(
            "تنها در صورت توصیه صریح پزشک و با ثبت دقیق ساعت و حجم mL هر نوبت در دفترچه دارو مصرف شود.",
            "اگر کودک زیر ۶ ماه است یا استفراغ/کم‌آبی دارد، ایبوپروفن ندهید.",
          ),
      )
    }

    if ("ibuprofen-pediatric" in slugs && "otc-cough-cold-caution" in slugs) {
      return MedicationOverlapResult(
        severity = AlertSeverity.CAUTION,
        titleFa = "احتمال هم‌پوشانی اجزای ضددرد/تب در شربت‌های ترکیبی",
        summaryFa = "برخی فرآورده‌های ترکیبی سرماخوردگی حاوی ایبوپروفن یا ضدالتهاب‌های مشابه هستند. همچنین در کودک دچار کاهش اشتها و کم‌آبی، مصرف NSAID نیازمند احتیاط است.",
        actionStepsFa =
          listOf(
            "ترکیبات روی جعبه شربت را پیش از مصرف با داروساز تطبیق دهید.",
            "به میزان مصرف آب و ادرار کودک توجه کنید.",
          ),
      )
    }

    if ("antibiotic-suspensions-pediatric" in slugs && "vitamin-d3-iron-drops" in slugs) {
      return MedicationOverlapResult(
        severity = AlertSeverity.CAUTION,
        titleFa = "نیاز به فاصله زمانی بین آنتی‌بیوتیک و قطره آهن/مکمل",
        summaryFa = "برخی آنتی‌بیوتیک‌ها در حضور آهن یا املاح معدنی جذب کمتری دارند و مصرف هم‌زمان هر دو می‌تواند تحریک معده کودک را بیشتر کند.",
        actionStepsFa =
          listOf(
            "بین نوبت آنتی‌بیوتیک و قطره آهن دست‌کم ۲ ساعت فاصله بگذارید (یا درباره ادامه موقت مکمل در دوره بیماری از پزشک بپرسید).",
            "دوره آنتی‌بیوتیک را دقیقاً با سرنگ mL و سر ساعت کامل کنید.",
          ),
      )
    }

    if ("antibiotic-suspensions-pediatric" in slugs && "ors-oral-rehydration" in slugs) {
      return MedicationOverlapResult(
        severity = AlertSeverity.INFO,
        titleFa = "همراهی ایمن ORS در اسهال ناشی از آنتی‌بیوتیک",
        summaryFa = "مصرف محلول استاندارد ORS تداخل دارویی منفی با آنتی‌بیوتیک ندارد و به جبران آب و املاح در اسهال خفیف ناشی از آنتی‌بیوتیک کمک می‌کند.",
        actionStepsFa =
          listOf(
            "ORS را جرعه‌جرعه در طول روز بدهید و مستقیماً با آنتی‌بیوتیک داخل یک قاشق مخلوط نکنید.",
            "در صورت اسهال شدید یا خونی، بلافاصله به پزشک مراجعه کنید.",
          ),
      )
    }

    if ("ors-oral-rehydration" in slugs && "ibuprofen-pediatric" in slugs) {
      return MedicationOverlapResult(
        severity = AlertSeverity.CAUTION,
        titleFa = "احتیاط مهم: نیاز کودک به ORS نشانه‌ خطر برای ایبوپروفن است",
        summaryFa = "اگر کودک به دلیل اسهال یا استفراغ دچار کم‌آبی است و به ORS نیاز دارد، مصرف ایبوپروفن می‌تواند به کلیه فشار وارد کند و خطر تحریک معده را افزایش دهد.",
        actionStepsFa =
          listOf(
            "پیش از دادن ایبوپروفن به کودک دچار استفراغ یا کم‌آبی حتماً با پزشک مشورت کنید.",
            "اولویت اول جبران تدریجی مایعات با ORS و بررسی علائم کم‌آبی است.",
          ),
      )
    }

    if ("topical-zinc-vs-steroid-cream" in slugs) {
      return MedicationOverlapResult(
        severity = AlertSeverity.INFO,
        titleFa = "تداخل سیستمیک پایین برای زینک اکساید؛ احتیاط برای کورتون زیر پوشک",
        summaryFa = "پماد ساده زینک اکساید جذب خونی ندارد و با داروهای خوراکی تداخل ایجاد نمی‌کند، اما کرم‌های کورتون‌دار زیر پوشک بسته جذب بالا دارند و بدون نسخه نباید به کار روند.",
        actionStepsFa =
          listOf(
            "فقط از لایه محافظ زینک اکساید بدون عطر استفاده کنید و از کرم‌های کورتون‌دار قوی بپرهیزید.",
          ),
      )
    }

    return MedicationOverlapResult(
      severity = AlertSeverity.CAUTION,
      titleFa = "این زوج دارویی بررسی جامع نشده است",
      summaryFa = "نبود قاعده اختصاصی در فهرست محدود مانو به معنی نبود تداخل نیست. ترکیب «${first.nameFa}» و «${second.nameFa}» نیازمند بررسی علمی مستقل توسط داروساز با توجه به سن، وزن، بیماری‌ها و داروهای هم‌زمان است.",
      actionStepsFa =
        listOf(
          "داروها را داخل یک سرنگ یا قاشق با هم مخلوط نکنید؛ هر دارو را با سرنگ mL تمیز و جداگانه بدهید.",
          "در صورت مشاهده هرگونه حساسیت پوستی، استفراغ یا خواب‌آلودگی غیرمعمول با پزشک تماس بگیرید.",
        ),
    )
  }
}
