package com.example.domain.model

enum class MedicationCheckCategory(val id: String, val labelFa: String) {
  VERIFY_MEDICINE("verify_medicine", "۱. بررسی بطری، تاریخ انقضا و مشخصات کودک"),
  DOSE_MEASURE("dose_measure", "۲. کنترل غلظت دارو و اندازه‌گیری دقیق mL"),
  TIMING_GIVING("timing_giving", "۳. ساعت آخرین نوبت، تداخل‌ها و ثبت ایمن"),
}

data class MedicationSafetyCheckItem(
  val id: String,
  val category: MedicationCheckCategory,
  val stepNumberFa: String,
  val titleFa: String,
  val preventsErrorFa: String,
  val detailTipFa: String,
  val isCritical: Boolean = true,
)

data class MedicationChecklistProgress(
  val checkedCount: Int,
  val totalCount: Int,
  val criticalCheckedCount: Int,
  val criticalTotalCount: Int,
  val isAllCriticalReady: Boolean,
  val isAllCompleted: Boolean,
  val progressFraction: Float,
  val statusHeadlineFa: String,
  val statusSublineFa: String,
)

object MedicationSafetyChecklistCatalog {
  val items: List<MedicationSafetyCheckItem> =
    listOf(
      MedicationSafetyCheckItem(
        id = "check_right_child_weight",
        category = MedicationCheckCategory.VERIFY_MEDICINE,
        stepNumberFa = "گام ۱",
        titleFa = "نام کودک، سن و وزن فعلی با برچسب دارو تطبیق داده شد",
        preventsErrorFa = "خطای دادن داروی بزرگسال یا دوز کودک دیگر",
        detailTipFa =
          "دوز داروهای کودکان بر اساس وزن دقیق (کیلوگرم) و سن تعیین می‌شود؛ از اشتراک گذاشتن داروی خواهر و برادر بدون تایید پزشک خودداری کنید.",
        isCritical = true,
      ),
      MedicationSafetyCheckItem(
        id = "check_expiry_and_opened_date",
        category = MedicationCheckCategory.VERIFY_MEDICINE,
        stepNumberFa = "گام ۲",
        titleFa = "تاریخ انقضا، ظاهر دارو و تاریخ باز شدن درب بطری بررسی شد",
        preventsErrorFa = "مصرف داروی منقضی، کدر یا سوسپانسیون فاسدشده",
        detailTipFa =
          "روی بطری تاریخ باز شدن را یادداشت کنید. بسیاری از سوسپانسیون‌های آنتی‌بیوتیک پس از حل شدن فقط ۷ تا ۱۴ روز در یخچال قابل استفاده‌اند.",
        isCritical = true,
      ),
      MedicationSafetyCheckItem(
        id = "check_concentration_match",
        category = MedicationCheckCategory.DOSE_MEASURE,
        stepNumberFa = "گام ۳",
        titleFa = "غلظت روی بطری (مثلاً 100mg/5mL یا قطره) دوباره خوانده شد",
        preventsErrorFa = "اشتباه گرفتن غلظت قطره نوزادان و شربت کودکان",
        detailTipFa =
          "غلظت قطره خوراکی و شربت یکسان نیست؛ پیش از هر بار کشیدن دارو، عدد میلی‌گرم در میلی‌لیتر روی جعبه را چک کنید.",
        isCritical = true,
      ),
      MedicationSafetyCheckItem(
        id = "check_shake_and_oral_syringe",
        category = MedicationCheckCategory.DOSE_MEASURE,
        stepNumberFa = "گام ۴",
        titleFa = "بطری تکان داده شد و دارو فقط با سرنگ مدرج mL کشیده شد",
        preventsErrorFa = "خطای قاشق آشپزخانه و ته‌نشینی ماده مؤثره",
        detailTipFa =
          "بطری سوسپانسیون را قبل از مصرف به‌خوبی تکان دهید. هرگز از قاشق چای‌خوری خانگی استفاده نکنید و خط سرنگ را هم‌سطح چشم بخوانید.",
        isCritical = true,
      ),
      MedicationSafetyCheckItem(
        id = "check_dose_time_interval",
        category = MedicationCheckCategory.TIMING_GIVING,
        stepNumberFa = "گام ۵",
        titleFa = "ساعت آخرین نوبت و حداقل فاصله ایمن کنترل شد",
        preventsErrorFa = "تکرار زودهنگام دوز یا دوباره‌کاری بین مادر و پدر",
        detailTipFa =
          "فاصله ایمن استامینوفن معمولاً ۴ تا ۶ ساعت و ایبوپروفن ۶ تا ۸ ساعت است؛ پیش از مصرف، خط زمانی ۲۴ ساعته را با همسر خود هماهنگ کنید.",
        isCritical = true,
      ),
      MedicationSafetyCheckItem(
        id = "check_no_hidden_overlap",
        category = MedicationCheckCategory.TIMING_GIVING,
        stepNumberFa = "گام ۶",
        titleFa = "عدم تداخل یا ماده تکراری (مثل استامینوفن پنهان) بررسی شد",
        preventsErrorFa = "مسمومیت ناشی از هم‌پوشانی شربت سرماخوردگی و تب‌بر",
        detailTipFa =
          "بسیاری از شربت‌های سرماخوردگی یا سرفه کودکان حاوی استامینوفن یا آنتی‌هیستامین هستند؛ ترکیبات داروهای هم‌زمان را مقایسه کنید.",
        isCritical = true,
      ),
      MedicationSafetyCheckItem(
        id = "check_allergy_and_hydration",
        category = MedicationCheckCategory.TIMING_GIVING,
        stepNumberFa = "گام ۷",
        titleFa = "حساسیت‌های دارویی کودک و دریافت مایعات کافی چک شد",
        preventsErrorFa = "واکنش آلرژیک یا تحریک معده و کلیه هنگام کم‌آبی",
        detailTipFa =
          "پیش از دادن دارو، لیست حساسیت‌ها را چک کنید. در صورت کم‌آبی شدید، بی‌حالی یا استفراغ مداوم، پیش از دادن ایبوپروفن با پزشک مشورت کنید.",
        isCritical = false,
      ),
      MedicationSafetyCheckItem(
        id = "check_record_and_childproof_cap",
        category = MedicationCheckCategory.TIMING_GIVING,
        stepNumberFa = "گام ۸",
        titleFa = "ثبت فوری ساعت و حجم (mL) و بستن درب ایمنی دارو انجام می‌شود",
        preventsErrorFa = "فراموشی نوبت بعدی و دسترسی تصادفی کودک به بطری",
        detailTipFa =
          "بلافاصله پس از خوراندن دارو، مقدار mL و ساعت دقیق را در دفترچه ثبت کنید و بطری را در قفسه مرتفع و قفل‌دار بگذارید.",
        isCritical = false,
      ),
    )

  fun evaluateProgress(checkedIds: Set<String>): MedicationChecklistProgress {
    val validIds = items.map { it.id }.toSet()
    val validChecked = checkedIds.intersect(validIds)
    val criticalItems = items.filter { it.isCritical }
    val criticalChecked = criticalItems.count { it.id in validChecked }
    val totalCount = items.size
    val checkedCount = validChecked.size
    val criticalTotal = criticalItems.size
    val allCriticalReady = criticalChecked == criticalTotal
    val allCompleted = checkedCount == totalCount
    val progress = if (totalCount == 0) 0f else (checkedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)

    val (headline, subline) =
      when {
        allCompleted ->
          "همه مراحل چک‌لیست ایمنی تکمیل شد" to
            "تمام کنترل‌های حیاتی و مراقبتی انجام شده است؛ می‌توانید نوبت را با خیال راحت در خط زمانی ثبت کنید."
        allCriticalReady ->
          "تمام مراحل حیاتی ایمنی تایید شد ($checkedCount از $totalCount)" to
            "پیش‌نیازهای کلیدی پیشگیری از خطای دوز و ساعت بررسی شده‌اند؛ موارد تکمیلی باقی‌مانده را هم مرور کنید."
        checkedCount == 0 ->
          "پیش از دادن دارو، موارد زیر را تیک بزنید" to
            "$criticalTotal گام حیاتی برای جلوگیری از اشتباه در دوز، غلظت، تاریخ انقضا و تداخل زمانی آماده بررسی است."
        else -> {
          val remainingCritical = criticalTotal - criticalChecked
          "در حال بررسی ایمنی مصرف ($checkedCount از $totalCount)" to
            "$remainingCritical گام حیاتی دیگر (بررسی برچسب، غلظت، سرنگ یا ساعت) پیش از خوراندن دارو باقی مانده است."
        }
      }

    return MedicationChecklistProgress(
      checkedCount = checkedCount,
      totalCount = totalCount,
      criticalCheckedCount = criticalChecked,
      criticalTotalCount = criticalTotal,
      isAllCriticalReady = allCriticalReady,
      isAllCompleted = allCompleted,
      progressFraction = progress,
      statusHeadlineFa = headline,
      statusSublineFa = subline,
    )
  }

  fun remainingCriticalTitles(checkedIds: Set<String>): List<String> =
    items.filter { it.isCritical && it.id !in checkedIds }.map { it.titleFa }
}
