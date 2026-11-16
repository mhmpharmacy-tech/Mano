package com.example.domain.util

import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.MilestoneItem
import com.example.domain.model.NutritionAppetiteStatus
import com.example.domain.model.NutritionPlanResult
import java.util.Locale

data class NutritionPromptBundle(
  val promptText: String,
  val childContextSummaryFa: String,
  val isUnderSixMonths: Boolean,
  val isUnderTwelveMonths: Boolean,
  val normalizedWeightKg: Double?,
  val normalizedHeightCm: Double?,
)

object NutritionPromptBuilder {

  fun parsePositiveDecimal(raw: String): Double? {
    val normalized =
      PersianNormalizer.normalize(raw)
        .replace("٫", ".")
        .replace(",", ".")
        .trim()
    if (normalized.isEmpty()) return null
    return normalized.toDoubleOrNull()
  }

  fun validateGrowthMeasurements(weightKgText: String, heightCmText: String): String? {
    if (weightKgText.isNotBlank()) {
      val weight = parsePositiveDecimal(weightKgText)
      if (weight == null || weight < 1.5 || weight > 90.0) {
        return "لطفاً وزن معتبر کودک را بین ۱.۵ تا ۹۰ کیلوگرم وارد کنید (مثلاً 11.5)."
      }
    }
    if (heightCmText.isNotBlank()) {
      val height = parsePositiveDecimal(heightCmText)
      if (height == null || height < 35.0 || height > 180.0) {
        return "لطفاً قد معتبر کودک را بین ۳۵ تا ۱۸۰ سانتی‌متر وارد کنید (مثلاً 82)."
      }
    }
    return null
  }

  /**
   * Detects infants likely under 6 months so prompt & UI strictly preserve exclusive milk feeding guidance.
   */
  fun isLikelyUnderSixMonths(ageBand: AgeBand, ageDetailFa: String): Boolean {
    if (ageBand == AgeBand.NEWBORN) return true
    if (ageBand != AgeBand.INFANT_3_12M) return false
    val norm = PersianNormalizer.normalize(ageDetailFa)
    val monthMatch = Regex("""(\d+)\s*ماه""").find(norm)
    val months = monthMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
    return months != null && months < 6
  }

  fun buildContext(
    childProfile: ChildProfileDemo?,
    ageBand: AgeBand,
    ageDetailFa: String,
    weightKgText: String,
    heightCmText: String,
    allergiesFa: String,
    checkedMilestones: List<MilestoneItem>,
    timelineSnapshot: FeverDoseTimelineSnapshot,
    appetiteStatus: NutritionAppetiteStatus,
    availableIngredientsFa: String,
  ): NutritionPromptBundle {
    val alias = childProfile?.aliasFa?.trim()?.ifEmpty { "کودک" } ?: "کودک"
    val effectiveAgeDetail =
      ageDetailFa.trim().ifEmpty { childProfile?.birthDateOrAgeFa?.trim().orEmpty() }.ifEmpty { ageBand.labelFa }
    val normalizedWeight = parsePositiveDecimal(weightKgText)
    val normalizedHeight = parsePositiveDecimal(heightCmText)
    val effectiveAllergies =
      allergiesFa.trim().ifEmpty { childProfile?.allergiesFa?.trim().orEmpty() }.ifEmpty { "موردی ثبت نشده" }
    val currentMeds = childProfile?.currentMedsFa?.trim()?.ifEmpty { "ثبت نشده" } ?: "ثبت نشده"

    val underSixMonths = isLikelyUnderSixMonths(ageBand, effectiveAgeDetail)
    val underTwelveMonths = ageBand == AgeBand.NEWBORN || ageBand == AgeBand.INFANT_3_12M

    val weightSummary =
      normalizedWeight?.let { String.format(Locale.US, "%.1f kg", it) } ?: "ثبت نشده"
    val heightSummary =
      normalizedHeight?.let { String.format(Locale.US, "%.1f cm", it) } ?: "ثبت نشده"

    val recentFeverInfo =
      when {
        timelineSnapshot.latestTempCelsius != null && timelineSnapshot.latestTempCelsius >= 38.0f ->
          "بله — آخرین دمای ثبت‌شده ${FeverDoseTimelineEngine.formatTempC(timelineSnapshot.latestTempCelsius)} (اوج ۲۴ساعته: ${timelineSnapshot.peakTemp24h?.let { FeverDoseTimelineEngine.formatTempC(it) } ?: "نامشخص"})"
        timelineSnapshot.latestTempCelsius != null ->
          "بدون تب فعال (آخرین دما: ${FeverDoseTimelineEngine.formatTempC(timelineSnapshot.latestTempCelsius)})"
        else -> "رکورد دمای ۲۴ ساعت اخیر ثبت نشده است"
      }

    val milestonesBlock =
      if (checkedMilestones.isEmpty()) {
        "• هنوز نقطه عطف رشدی در چک‌لیست تیک نخورده است."
      } else {
        checkedMilestones.take(6).joinToString("\n") { m ->
          "• [${m.domainFa}] ${m.descriptionFa}"
        }
      }

    val childContextSummary =
      buildString {
        append("$alias • ${ageBand.labelFa}")
        if (effectiveAgeDetail != ageBand.labelFa) append(" ($effectiveAgeDetail)")
        if (normalizedWeight != null) append(" • وزن: $weightSummary")
        if (normalizedHeight != null) append(" • قد: $heightSummary")
      }

    val ageSafetyInstruction =
      when {
        underSixMonths ->
          """
          قاعده ایمنی اجباری سن (زیر ۶ ماه):
          - بر اساس توصیه WHO و AAP، نوزاد زیر ۶ ماه فقط باید با شیر مادر یا شیرخشک استاندارد تغذیه شود.
          - به هیچ عنوان دستور غذای جامد، پوره، لعاب، آب ساده، عرق گیاهی یا عسل پیشنهاد ندهید.
          - بخش meals را به ۳ تا ۴ راهنمای عملی متناسب با سن تبدیل کنید (مانند: الگوی شیردهی درخواستی، بررسی کفایت شیر و پوشک خیس، آمادگی‌های تغذیه کمکی برای پایان ۶ ماهگی، و تغذیه مادر شیرده یا آماده‌سازی استاندارد شیرخشک).
          """.trimIndent()
        underTwelveMonths ->
          """
          قاعده ایمنی اجباری سن (۶ تا ۱۲ ماه):
          - عسل تا پایان ۱۲ ماهگی مطلقاً ممنوع است (خطر بوتولیسم نوزادی).
          - شیر گاو پاستوریزه به عنوان نوشیدنی اصلی، نمک افزوده، شکر افزوده و غذاهای سفت/گرد خفگی‌آور (مغز درشت، انگور کامل، هویج خام) ممنوع است.
          - روی غذاهای کمکی غنی از آهن طبیعی، بافت پوره نرم یا له‌شده متناسب با تکامل جویدن و معرفی تک‌به‌تک مواد حساسیت‌زا تمرکز کنید.
          """.trimIndent()
        else ->
          """
          قاعده ایمنی سن (۱ سال به بالا):
          - از پیشنهاد قطعات سفت و خطر خفگی برای خردسالان (آجیل کامل، سوسیس حلقه‌ای، ذرت بو داده در سن زیر ۴ سال) پرهیز کنید و شکل ایمن سرو (پوره، رنده، خردشده نرم) را بنویسید.
          - قندهای افزوده، نوشیدنی‌های شیرین و غذاهای پرادویه/پرچرب سنگین را حذف یا محدود کنید.
          """.trimIndent()
      }

    val promptText =
      """
      شما مشاور آموزشی تغذیه کودک در اپلیکیشن فارسی «مانو کیدز» هستید و بر پایه راهنماهای علمی معتبر (WHO، AAP و UNICEF) به والدین پیشنهاد غذای سالم، کاربردی و بومی ایرانی ارائه می‌دهید.
      محدوده وظیفه:
      - تشخیص بیماری یا تجویز رژیم درمانی/مکمل دارویی صادر نکنید؛ لحن آرام، حمایتی و علمی داشته باشید.
      - هر پیشنهاد غذایی باید دقیقاً با سن کودک، داده‌های رشد (وزن، قد، نقاط عطف تکاملی تیک‌خورده)، حساسیت‌ها و وضعیت فعلی کودک هماهنگ باشد.
      - اگر در بخش «حساسیت‌های شناخته‌شده» ماده‌ای ثبت شده است، آن ماده و مشتقاتش را کاملاً حذف کنید و در safetyAvoidListFa هشدار دهید.

      $ageSafetyInstruction

      مشخصات پروفایل و داده‌های رشد کودک:
      - نام مستعار: $alias
      - گروه سنی: ${ageBand.labelFa} (${ageBand.rangeFa})
      - سن دقیق ثبت‌شده: $effectiveAgeDetail
      - وزن فعلی: $weightSummary ${childProfile?.weightDateFa?.takeIf { it.isNotBlank() }?.let { "(تاریخ: $it)" }.orEmpty()}
      - قد فعلی: $heightSummary
      - حساسیت‌های غذایی/دارویی شناخته‌شده: $effectiveAllergies
      - داروهای فعلی: $currentMeds
      - وضعیت فعلی اشتها و گوارش: ${appetiteStatus.labelFa} (${appetiteStatus.promptHintFa})
      - وضعیت دمای بدن ۲۴ ساعت اخیر: $recentFeverInfo
      - مواد اولیه در دسترس یا علاقه کودک (اختیاری): ${availableIngredientsFa.trim().ifEmpty { "محدودیتی مشخص نشده؛ غذاهای در دسترس خانواده ایرانی پیشنهاد شود" }}

      نقاط عطف رشد ثبت‌شده توسط والدین:
      $milestonesBlock

      خروجی باید دقیقاً منطبق بر JSON Schema خواسته‌شده و تماماً به زبان فارسی روان، شفاف و قابل اجرا در خانه باشد. تعداد ۳ تا ۴ وعده کاربردی و متنوع در meals ارائه دهید.
      """.trimIndent()

    return NutritionPromptBundle(
      promptText = promptText,
      childContextSummaryFa = childContextSummary,
      isUnderSixMonths = underSixMonths,
      isUnderTwelveMonths = underTwelveMonths,
      normalizedWeightKg = normalizedWeight,
      normalizedHeightCm = normalizedHeight,
    )
  }

  fun formatShareableNutritionTextFa(plan: NutritionPlanResult): String =
    buildString {
      appendLine("پیشنهاد غذای سالم و مقوی کودک — مانو کیدز")
      appendLine(plan.planTitleFa)
      if (plan.childContextSummaryFa.isNotBlank()) {
        appendLine("مشخصات کودک: ${plan.childContextSummaryFa}")
      }
      if (plan.appetiteStatusLabelFa.isNotBlank()) {
        appendLine("وضعیت انتخاب‌شده: ${plan.appetiteStatusLabelFa}")
      }
      appendLine()
      appendLine("ارزیابی تغذیه‌ای: ${plan.overviewFa}")
      appendLine("تمرکز رشد: ${plan.growthNutritionFocusFa}")
      appendLine()
      plan.meals.forEachIndexed { idx, meal ->
        appendLine("${idx + 1}. [${meal.mealTypeFa}] ${meal.titleFa}")
        appendLine("   چرا مناسب است: ${meal.whySuitableFa}")
        appendLine("   بافت و حجم پیشنهادی: ${meal.textureAndPortionFa}")
        if (meal.ingredientsFa.isNotEmpty()) {
          appendLine("   مواد لازم: ${meal.ingredientsFa.joinToString("، ")}")
        }
        if (meal.prepStepsFa.isNotEmpty()) {
          appendLine("   طرز تهیه کوتاه: ${meal.prepStepsFa.joinToString(" -> ")}")
        }
        appendLine()
      }
      if (plan.hydrationTipFa.isNotBlank()) {
        appendLine("توصیه مایعات: ${plan.hydrationTipFa}")
      }
      if (plan.safetyAvoidListFa.isNotEmpty()) {
        appendLine("موارد احتیاط و پرهیز:")
        plan.safetyAvoidListFa.forEach { appendLine("• $it") }
      }
      if (plan.doctorFollowUpHintFa.isNotBlank()) {
        appendLine("پیگیری با پزشک: ${plan.doctorFollowUpHintFa}")
      }
      appendLine("تذکر: این پیشنهاد جنبه آموزشی دارد و جایگزین پزشک یا متخصص تغذیه کودک نیست.")
    }.trim()
}
