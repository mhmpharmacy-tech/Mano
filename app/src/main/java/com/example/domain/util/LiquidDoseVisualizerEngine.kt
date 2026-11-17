package com.example.domain.util

import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.ui.components.AlertSeverity
import java.util.Locale
import kotlin.math.roundToInt

enum class LiquidFormulationId(
  val id: String,
  val titleFa: String,
  val shortLabelFa: String,
  val concentrationLabelFa: String,
  val mgPerMl: Double,
  val minHomeAgeMonths: Int,
  val maxSyringeScaleMl: Float,
  val stepMl: Float,
  val minMgPerKg: Double,
  val targetMgPerKg: Double,
  val maxMgPerKg: Double,
  val maxSingleDoseMg: Double,
  val intervalFa: String,
  val safetyTipFa: String,
) {
  ACETAMINOPHEN_DROPS_100(
    id = "acetaminophen_drops_100",
    titleFa = "قطره استامینوفن کودکان (۱۰۰ mg/mL)",
    shortLabelFa = "قطره استامینوفن ۱۰۰",
    concentrationLabelFa = "۱۰۰ میلی‌گرم در هر ۱ mL",
    mgPerMl = 100.0,
    minHomeAgeMonths = 3,
    maxSyringeScaleMl = 3.0f,
    stepMl = 0.1f,
    minMgPerKg = 10.0,
    targetMgPerKg = 12.5,
    maxMgPerKg = 15.0,
    maxSingleDoseMg = 500.0,
    intervalFa = "هر ۴ تا ۶ ساعت در صورت نیاز (حداکثر ۵ نوبت در ۲۴ ساعت)",
    safetyTipFa = "غلظت قطره ۴ برابر شربت ۱۲۰ است! هرگز قطره را با سرنگ بزرگ یا پیمانه شربت ندهید.",
  ),
  ACETAMINOPHEN_SYRUP_120_5(
    id = "acetaminophen_syrup_120_5",
    titleFa = "شربت استامینوفن (۱۲۰ mg / 5 mL)",
    shortLabelFa = "شربت استامینوفن ۱۲۰/۵",
    concentrationLabelFa = "۲۴ میلی‌گرم در هر ۱ mL",
    mgPerMl = 24.0,
    minHomeAgeMonths = 3,
    maxSyringeScaleMl = 20.0f,
    stepMl = 0.5f,
    minMgPerKg = 10.0,
    targetMgPerKg = 12.5,
    maxMgPerKg = 15.0,
    maxSingleDoseMg = 500.0,
    intervalFa = "هر ۴ تا ۶ ساعت در صورت نیاز (حداکثر ۵ نوبت در ۲۴ ساعت)",
    safetyTipFa = "هم‌زمان با شربت‌های ترکیبی سرماخوردگی مصرف نشود تا خطر مسمومیت کبدی ایجاد نشود.",
  ),
  ACETAMINOPHEN_SYRUP_160_5(
    id = "acetaminophen_syrup_160_5",
    titleFa = "سوسپانسیون استامینوفن (۱۶۰ mg / 5 mL)",
    shortLabelFa = "استامینوفن ۱۶۰/۵",
    concentrationLabelFa = "۳۲ میلی‌گرم در هر ۱ mL",
    mgPerMl = 32.0,
    minHomeAgeMonths = 3,
    maxSyringeScaleMl = 16.0f,
    stepMl = 0.5f,
    minMgPerKg = 10.0,
    targetMgPerKg = 12.5,
    maxMgPerKg = 15.0,
    maxSingleDoseMg = 500.0,
    intervalFa = "هر ۴ تا ۶ ساعت در صورت نیاز (حداکثر ۵ نوبت در ۲۴ ساعت)",
    safetyTipFa = "حتماً عدد روی جعبه را چک کنید که ۱۲۰/۵ است یا ۱۶۰/۵؛ حجم mL این دو متفاوت است.",
  ),
  IBUPROFEN_SYRUP_100_5(
    id = "ibuprofen_syrup_100_5",
    titleFa = "سوسپانسیون ایبوپروفن (۱۰۰ mg / 5 mL)",
    shortLabelFa = "ایبوپروفن ۱۰۰/۵",
    concentrationLabelFa = "۲۰ میلی‌گرم در هر ۱ mL",
    mgPerMl = 20.0,
    minHomeAgeMonths = 6,
    maxSyringeScaleMl = 20.0f,
    stepMl = 0.5f,
    minMgPerKg = 5.0,
    targetMgPerKg = 7.5,
    maxMgPerKg = 10.0,
    maxSingleDoseMg = 400.0,
    intervalFa = "هر ۶ تا ۸ ساعت همراه کمی شیر یا غذا (حداکثر ۳ تا ۴ نوبت در ۲۴ ساعت)",
    safetyTipFa = "شیشه را ۲۰ ثانیه کامل تکان دهید. در کودک زیر ۶ ماه یا دچار استفراغ/کم‌آبی مصرف نشود.",
  ),
  CETIRIZINE_SYRUP_5_5(
    id = "cetirizine_syrup_5_5",
    titleFa = "شربت سیتیریزین (۵ mg / 5 mL)",
    shortLabelFa = "سیتیریزین ۵/۵",
    concentrationLabelFa = "۱ میلی‌گرم در هر ۱ mL",
    mgPerMl = 1.0,
    minHomeAgeMonths = 24,
    maxSyringeScaleMl = 10.0f,
    stepMl = 0.5f,
    minMgPerKg = 0.0,
    targetMgPerKg = 0.0,
    maxMgPerKg = 0.0,
    maxSingleDoseMg = 5.0,
    intervalFa = "معمولاً یک نوبت در ۲۴ ساعت (بر اساس سن و نظر پزشک/داروساز)",
    safetyTipFa = "دوز سیتیریزین بر پایه رده سنی تنظیم می‌شود، نه صرفاً وزن؛ در کودکان زیر ۲ سال بدون تجویز پزشک ندهید.",
  ),
}

data class LiquidDoseVisualizationResult(
  val formulation: LiquidFormulationId,
  val ageMonths: Int,
  val weightKg: Float,
  val isBlocked: Boolean,
  val recommendedMl: Float,
  val minSafeMl: Float,
  val maxSafeMl: Float,
  val recommendedMg: Int,
  val isWeightBased: Boolean,
  val alertSeverity: AlertSeverity,
  val alertTitleFa: String,
  val alertBodyFa: String,
)

object LiquidDoseVisualizerEngine {

  const val MIN_WEIGHT_KG = 3.0f
  const val MAX_WEIGHT_KG = 40.0f
  const val MIN_AGE_MONTHS = 0
  const val MAX_AGE_MONTHS = 144

  /**
   * The public-facing dose recommendation engine is disabled pending clinical validation.
   * Scanner auto-fill and 3D dose prefill must never bypass this gate.
   */
  fun evaluate(
    formulation: LiquidFormulationId,
    ageMonths: Int,
    weightKg: Float,
  ): LiquidDoseVisualizationResult {
    val safeAge = ageMonths.coerceIn(MIN_AGE_MONTHS, MAX_AGE_MONTHS)
    val safeWeight = ((weightKg * 2f).roundToInt() / 2f).coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG)

    val (severity, title, body) = when {
      safeAge < 3 -> Triple(
        AlertSeverity.DANGER,
        "دوز دارو غیرفعال — هشدار ویژه نوزاد",
        "تب ۳۸ درجه سانتی‌گراد یا بالاتر در کودک زیر ۳ ماه نیازمند ارزیابی فوری پزشکی است. مانو دوز خانگی پیشنهاد نمی‌کند.",
      )
      formulation == LiquidFormulationId.IBUPROFEN_SYRUP_100_5 && safeAge < 6 -> Triple(
        AlertSeverity.DANGER,
        "ایبوپروفن زیر ۶ ماه نیازمند نظر پزشک است",
        "محاسبه و پرکردن خودکار سرنگ غیرفعال است. برای مصرف و مقدار دارو با پزشک مشورت کنید.",
      )
      else -> Triple(
        AlertSeverity.CAUTION,
        "محاسبه دوز هنوز تأیید بالینی نشده است",
        "محاسبه و پیشنهاد مقدار دارو غیرفعال است. سن و وزن برای تجویز کافی نیستند؛ غلظت، سابقه و موارد منع مصرف باید توسط داروساز یا پزشک بررسی شوند.",
      )
    }

    return LiquidDoseVisualizationResult(
      formulation = formulation,
      ageMonths = safeAge,
      weightKg = safeWeight,
      isBlocked = true,
      recommendedMl = 0f,
      minSafeMl = 0f,
      maxSafeMl = 0f,
      recommendedMg = 0,
      isWeightBased = formulation != LiquidFormulationId.CETIRIZINE_SYRUP_5_5,
      alertSeverity = severity,
      alertTitleFa = title,
      alertBodyFa = body,
    )
  }

  /** No reverse estimation from a dragged syringe before clinical approval. */
  @Suppress("UNUSED_PARAMETER")
  fun estimateWeightFromDraggedMl(
    formulation: LiquidFormulationId,
    ageMonths: Int,
    draggedMl: Float,
  ): Float? = null

  fun initialAgeMonthsFromProfile(
    profile: ChildProfileDemo?,
    birthDateEpochMs: Long? = null,
    nowMs: Long = System.currentTimeMillis(),
  ): Int {
    profile?.ageMonthsExact?.let { return it.coerceIn(0, 144) }
    parseAgeMonthsFromText(profile?.birthDateOrAgeFa.orEmpty())?.let { return it.coerceIn(0, 144) }
    if (birthDateEpochMs != null && birthDateEpochMs > 0L && nowMs > birthDateEpochMs) {
      val monthsFromBirth = ((nowMs - birthDateEpochMs) / (30.4375 * 86_400_000.0)).toInt()
      return monthsFromBirth.coerceIn(0, 144)
    }
    if (profile == null) return 24
    return when (profile.ageBand) {
      AgeBand.NEWBORN -> 1
      AgeBand.INFANT_3_12M -> 3 // Conservative lower bound so <6m infants stay blocked for ibuprofen until exact age >=6m
      AgeBand.TODDLER_1_3Y -> 18
      AgeBand.PRESCHOOL_3_6Y -> 42
      AgeBand.SCHOOL_6_PLUS -> 78
    }
  }

  fun parseAgeMonthsFromText(raw: String): Int? {
    val clean = PersianNormalizer.normalize(raw).trim()
    if (clean.isEmpty()) return null
    // A band label ("۳ تا ۱۲ ماه", "1-3 years") is not an exact age.
    // Parsing its upper bound as the child's age can bypass infant safeguards.
    val rangePattern = Regex("""\b\d+\s*(?:تا|الی|[-–—])\s*\d+\b""")
    if (rangePattern.containsMatchIn(clean)) return null
    val yearsMatch = Regex("""(\d+)\s*سال""").find(clean)
    val monthsMatch = Regex("""(\d+)\s*ماه""").find(clean)
    val years = yearsMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
    val months = monthsMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (years != null || months != null) {
      return (((years ?: 0) * 12) + (months ?: 0)).coerceIn(0, 144)
    }
    val weeksMatch = Regex("""(\d+)\s*هفته""").find(clean)
    val weeks = weeksMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (weeks != null) {
      return (weeks / 4).coerceIn(0, 144)
    }
    return null
  }

  fun initialWeightKgFromProfile(profile: ChildProfileDemo?): Float {
    val raw = profile?.weightKgText?.trim().orEmpty()
    if (raw.isEmpty()) return 12.0f
    val normalized =
      raw
        .replace('۰', '0')
        .replace('۱', '1')
        .replace('۲', '2')
        .replace('۳', '3')
        .replace('۴', '4')
        .replace('۵', '5')
        .replace('۶', '6')
        .replace('۷', '7')
        .replace('۸', '8')
        .replace('۹', '9')
        .replace('٫', '.')
    val parsed = Regex("""\d+(\.\d+)?""").find(normalized)?.value?.toFloatOrNull()
    return if (parsed != null) ((parsed * 2f).roundToInt() / 2f).coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG) else 12.0f
  }

  fun formatAgeFa(ageMonths: Int): String {
    val m = ageMonths.coerceIn(MIN_AGE_MONTHS, MAX_AGE_MONTHS)
    return when {
      m == 0 -> "نوزاد تازه متولدشده"
      m < 12 -> "$m ماه"
      m % 12 == 0 -> "${m / 12} سال"
      else -> "${m / 12} سال و ${m % 12} ماه"
    }
  }

  fun formatMl(ml: Float): String = String.format(Locale.US, "%.1f mL", ml)

  fun formatKg(kg: Float): String = String.format(Locale.US, "%.1f kg", kg)

  private fun snapToStep(value: Float, step: Float): Float {
    if (step <= 0f) return value
    val steps = (value / step).roundToInt()
    return ((steps * step) * 10f).roundToInt() / 10f
  }
}
