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
    safetyTipFa = "این قطره ۱۰۰ mg/mL و شربت ۱۲۰/۵ معادل ۲۴ mg/mL است. هرگز قطره را با سرنگ بزرگ یا پیمانه شربت ندهید.",
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
  val recommendedMg: Double,
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

  fun evaluate(
    formulation: LiquidFormulationId,
    ageMonths: Int?,
    weightKg: Float?,
  ): LiquidDoseVisualizationResult {
    fun blocked(message: String) = LiquidDoseVisualizationResult(
      formulation, ageMonths ?: -1, weightKg ?: Float.NaN, true, 0f, 0f, 0f, 0.0,
      formulation != LiquidFormulationId.CETIRIZINE_SYRUP_5_5, AlertSeverity.CAUTION,
      "محاسبه در دسترس نیست", message,
    )
    if (ageMonths == null || ageMonths !in MIN_AGE_MONTHS..MAX_AGE_MONTHS ||
        weightKg == null || !weightKg.isFinite() || weightKg !in MIN_WEIGHT_KG..MAX_WEIGHT_KG) {
      return blocked("سن واقعی به ماه و وزن اندازه‌گیری‌شده را وارد کنید. این ابزار برای خارج از محدودهٔ خود تخمین نمی‌زند.")
    }
    val safeAge = ageMonths
    val safeWeight = weightKg

    // 1. Strict Newborn / Infant clinical safety gates
    if (safeAge < formulation.minHomeAgeMonths) {
      val (severity, title, body) =
        when {
          safeAge < 3 ->
            Triple(
              AlertSeverity.DANGER,
              "هشدار اورژانسی نوزاد زیر ۳ ماه — دوز خانگی نمایش داده نمی‌شود",
              "در نوزاد زیر ۳ ماه، تب ۳۸.۰ درجه یا نیاز به دارو نیازمند معاینه فوری پزشک است. سرنگ روی ۰.۰ mL قفل شده است؛ خودسرانه دارو ندهید.",
            )
          formulation == LiquidFormulationId.IBUPROFEN_SYRUP_100_5 && safeAge < 6 ->
            Triple(
              AlertSeverity.DANGER,
              "منع مصرف بدون نسخه ایبوپروفن در شیرخوار زیر ۶ ماه",
              "سوسپانسیون ایبوپروفن برای کودکان زیر ۶ ماه بدون دستور مستقیم پزشک مجاز نیست و سرنگ روی ۰.۰ mL قفل شده است.",
            )
          else ->
            Triple(
              AlertSeverity.CAUTION,
              "نیازمند تجویز پزشک برای رده سنی زیر ${formulation.minHomeAgeMonths} ماه",
              "برای این رده سنی (${formatAgeFa(safeAge)})، مقدار دقیق ${formulation.shortLabelFa} باید مستقیماً توسط پزشک یا داروساز تعیین شود.",
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
        recommendedMg = 0.0,
        isWeightBased = formulation != LiquidFormulationId.CETIRIZINE_SYRUP_5_5,
        alertSeverity = severity,
        alertTitleFa = title,
        alertBodyFa = body,
      )
    }

    // 2. Age-banded non-mg/kg dosing (Cetirizine 5 mg / 5 mL)
    if (formulation == LiquidFormulationId.CETIRIZINE_SYRUP_5_5) {
      val ml = if (safeAge < 72) 2.5f else 5.0f
      val maxMl = if (safeAge < 72) 2.5f else 10.0f
      val mg = ml * formulation.mgPerMl
      return LiquidDoseVisualizationResult(
        formulation = formulation,
        ageMonths = safeAge,
        weightKg = safeWeight,
        isBlocked = false,
        recommendedMl = ml,
        minSafeMl = ml,
        maxSafeMl = maxMl,
        recommendedMg = mg,
        isWeightBased = false,
        alertSeverity = AlertSeverity.INFO,
        alertTitleFa = "دوز سنی استاندارد (${formatAgeFa(safeAge)})",
        alertBodyFa = "برای ${formatAgeFa(safeAge)}، دوز رایج ${formatMl(ml)} ($mg mg) یک‌بار در روز است. برای بیش از ۲.۵ یا ۵ mL تنها با نظر پزشک اقدام کنید.",
      )
    }

    // 3. Weight-based liquid dosing (Acetaminophen & Ibuprofen)
    val rawMinMg = (safeWeight * formulation.minMgPerKg).coerceAtMost(formulation.maxSingleDoseMg)
    val rawTargetMg = (safeWeight * formulation.targetMgPerKg).coerceAtMost(formulation.maxSingleDoseMg)
    val rawMaxMg = (safeWeight * formulation.maxMgPerKg).coerceAtMost(formulation.maxSingleDoseMg)

    val stepMg = formulation.stepMl.toDouble() * formulation.mgPerMl
    val lowerTick = kotlin.math.ceil(rawMinMg / stepMg - 0.000001).toInt()
    val upperTick = kotlin.math.floor(rawMaxMg / stepMg + 0.000001).toInt()
    val targetTick = (rawTargetMg / stepMg).roundToInt()
    if (lowerTick > upperTick || rawTargetMg / formulation.mgPerMl > formulation.maxSyringeScaleMl) {
      return blocked("حجم هدف با درجه‌بندی یا ظرفیت این سرنگ سازگار نیست. برای انتخاب ابزار مناسب با داروساز مشورت کنید.")
    }
    val minMl = lowerTick * formulation.stepMl
    val targetMl = targetTick.coerceIn(lowerTick, upperTick) * formulation.stepMl
    val maxMl = upperTick * formulation.stepMl

    val actualMg = (targetMl.toString().toDouble() * formulation.mgPerMl)
    val dropsHighWeightCaution =
      formulation == LiquidFormulationId.ACETAMINOPHEN_DROPS_100 && safeWeight >= 14.0f

    val severity = if (dropsHighWeightCaution) AlertSeverity.CAUTION else AlertSeverity.SUCCESS
    val title =
      if (dropsHighWeightCaution) {
        "پیشنهاد شکل دارویی مناسب‌تر برای وزن ${formatKg(safeWeight)}"
      } else {
        "مقدار تخمینی آموزشی بر پایه وزن (${formatKg(safeWeight)}) و سن (${formatAgeFa(safeAge)})"
      }
    val body =
      if (dropsHighWeightCaution) {
        "حجم قطره برای این وزن نسبتاً زیاد است (${formatMl(targetMl)}). معمولاً برای کودکان بالای ۱۲ تا ۱۴ کیلوگرم، شربت ۱۲۰/۵ یا ۱۶۰/۵ راحت‌تر و دقیق‌تر است. ${formulation.safetyTipFa}"
      } else {
        "بر پایه محدوده استاندارد ${formulation.minMgPerKg.toInt()} تا ${formulation.maxMgPerKg.toInt()} mg/kg، محدوده مجاز هر نوبت ${formatMl(minMl)} تا ${formatMl(maxMl)} است. پیش از کشیدن دارو، غلظت روی جعبه را تطبیق دهید."
      }

    return LiquidDoseVisualizationResult(
      formulation = formulation,
      ageMonths = safeAge,
      weightKg = safeWeight,
      isBlocked = false,
      recommendedMl = targetMl,
      minSafeMl = minMl,
      maxSafeMl = maxMl,
      recommendedMg = actualMg,
      isWeightBased = true,
      alertSeverity = severity,
      alertTitleFa = title,
      alertBodyFa = body,
    )
  }

  fun initialAgeMonthsFromProfile(
    profile: ChildProfileDemo?,
    birthDateEpochMs: Long? = null,
    nowMs: Long = System.currentTimeMillis(),
  ): Int? {
    if (birthDateEpochMs != null) {
      if (birthDateEpochMs <= 0 || birthDateEpochMs > nowMs) return null
      val zone = java.util.TimeZone.getTimeZone("UTC")
      val birth = java.util.Calendar.getInstance(zone).apply { timeInMillis = birthDateEpochMs }
      val now = java.util.Calendar.getInstance(zone).apply { timeInMillis = nowMs }
      val months = (now.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)) * 12 +
        now.get(java.util.Calendar.MONTH) - birth.get(java.util.Calendar.MONTH)
      birth.add(java.util.Calendar.MONTH, months)
      return (months - if (birth.get(java.util.Calendar.DAY_OF_MONTH) > now.get(java.util.Calendar.DAY_OF_MONTH)) 1 else 0)
        .takeIf { it in MIN_AGE_MONTHS..MAX_AGE_MONTHS }
    }
    return (profile?.ageMonthsExact ?: parseAgeMonthsFromText(profile?.birthDateOrAgeFa.orEmpty()))
      ?.takeIf { it in MIN_AGE_MONTHS..MAX_AGE_MONTHS }
  }

  fun parseAgeMonthsFromText(raw: String): Int? {
    val clean = PersianNormalizer.normalize(raw).trim()
    if (clean.isEmpty()) return null
    val isBandLabel = AgeBand.entries.any { PersianNormalizer.normalize(it.labelFa) == clean }
    val hasNumericRange = Regex("""\d+\s*(?:تا|الی|-|–|to)\s*\d+""", RegexOption.IGNORE_CASE).containsMatchIn(clean)
    if (isBandLabel || hasNumericRange) return null
    val parts = Regex("""(?:(\d{1,2})\s*سال)?(?:\s*(?:و\s*)?(\d{1,3})\s*ماه)?""").matchEntire(clean) ?: return null
    val years = parts.groupValues[1].toIntOrNull() ?: 0
    val months = parts.groupValues[2].toIntOrNull() ?: 0
    return (12 * years + months).takeIf { it in MIN_AGE_MONTHS..MAX_AGE_MONTHS }
  }

  fun initialWeightKgFromProfile(profile: ChildProfileDemo?): Float? =
    ClinicalNumberInput.decimal(profile?.weightKgText.orEmpty())?.toFloat()?.takeIf { it in MIN_WEIGHT_KG..MAX_WEIGHT_KG }

  fun formatAgeFa(ageMonths: Int?): String {
    if (ageMonths == null || ageMonths < 0) return "سن ثبت نشده"
    val m = ageMonths.coerceIn(MIN_AGE_MONTHS, MAX_AGE_MONTHS)
    return when {
      m == 0 -> "نوزاد تازه متولدشده"
      m < 12 -> "$m ماه"
      m % 12 == 0 -> "${m / 12} سال"
      else -> "${m / 12} سال و ${m % 12} ماه"
    }
  }

  fun formatMl(ml: Float): String = String.format(Locale.US, "%.1f mL", ml)

  fun formatKg(kg: Float?): String = if (kg == null) "وزن ثبت نشده" else String.format(Locale.US, "%.1f kg", kg)

  private fun snapToStep(value: Float, step: Float): Float {
    if (step <= 0f) return value
    val steps = (value / step).roundToInt()
    return ((steps * step) * 10f).roundToInt() / 10f
  }
}
