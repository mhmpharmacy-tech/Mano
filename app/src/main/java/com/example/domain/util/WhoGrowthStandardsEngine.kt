package com.example.domain.util

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class GrowthMetric(
  val id: String,
  val labelFa: String,
  val shortLabelFa: String,
  val unitFa: String,
  val minValid: Double,
  val maxValid: Double,
) {
  WEIGHT_FOR_AGE(
    id = "weight_for_age",
    labelFa = "وزن نسبت به سن",
    shortLabelFa = "وزن (kg)",
    unitFa = "kg",
    minValid = 1.0,
    maxValid = 40.0,
  ),
  HEIGHT_FOR_AGE(
    id = "height_for_age",
    labelFa = "قد نسبت به سن",
    shortLabelFa = "قد (cm)",
    unitFa = "cm",
    minValid = 35.0,
    maxValid = 140.0,
  ),
}

enum class ChildSex(val id: String, val labelFa: String) {
  BOY("boy", "پسر"),
  GIRL("girl", "دختر"),
}

data class WhoLmsPoint(
  val ageMonths: Double,
  val l: Double,
  val m: Double,
  val s: Double,
)

data class WhoPercentileCurves(
  val agesMonths: List<Double>,
  val p3: List<Double>,
  val p15: List<Double>,
  val p50: List<Double>,
  val p85: List<Double>,
  val p97: List<Double>,
)

data class WhoGrowthAssessment(
  val metric: GrowthMetric,
  val sex: ChildSex,
  val ageMonths: Double,
  val measuredValue: Double,
  val zScore: Double,
  val percentile: Double,
  val p3: Double,
  val p15: Double,
  val p50: Double,
  val p85: Double,
  val p97: Double,
  val bandLabelFa: String,
  val guidanceFa: String,
  val needsPediatricReview: Boolean,
)

/**
 * Educational interpolation of WHO monthly LMS tables (2006, 0–60 months).
 * Monthly interpolation is approximate, not the WHO Anthro daily algorithm. Computes linear L, M, S interpolation, reference percentile curves (P3, P15, P50, P85, P97),
 * Z-scores, and non-diagnostic educational percentile bands without external charting SDKs.
 */
object WhoGrowthStandardsEngine {

  private const val Z_P3 = -1.8807936
  private const val Z_P15 = -1.0364334
  private const val Z_P50 = 0.0
  private const val Z_P85 = 1.0364334
  private const val Z_P97 = 1.8807936

  private val weightBoysLms = WhoReferenceData.weightBoys
  private val weightGirlsLms = WhoReferenceData.weightGirls
  private val heightBoysLms = WhoReferenceData.lengthBoys.filter { it.ageMonths < 24 } + WhoReferenceData.heightBoys
  private val heightGirlsLms = WhoReferenceData.lengthGirls.filter { it.ageMonths < 24 } + WhoReferenceData.heightGirls

  fun lmsTableFor(metric: GrowthMetric, sex: ChildSex): List<WhoLmsPoint> =
    when (metric) {
      GrowthMetric.WEIGHT_FOR_AGE -> if (sex == ChildSex.BOY) weightBoysLms else weightGirlsLms
      GrowthMetric.HEIGHT_FOR_AGE -> if (sex == ChildSex.BOY) heightBoysLms else heightGirlsLms
    }

  fun interpolateLms(metric: GrowthMetric, sex: ChildSex, ageMonths: Double): WhoLmsPoint {
    require(ageMonths.isFinite() && ageMonths in 0.0..60.0) { "Age must be finite and within 0–60 months" }
    val clampedAge = ageMonths
    // Do not interpolate across the 0.7 cm change from length to standing height at 24 months.
    val table = if (metric == GrowthMetric.HEIGHT_FOR_AGE && ageMonths < 24.0) {
      if (sex == ChildSex.BOY) WhoReferenceData.lengthBoys else WhoReferenceData.lengthGirls
    } else lmsTableFor(metric, sex)
    val exact = table.firstOrNull { abs(it.ageMonths - clampedAge) < 1e-6 }
    if (exact != null) return exact

    val upperIndex = table.indexOfFirst { it.ageMonths > clampedAge }
    if (upperIndex <= 0) return table.first()
    val a = table[upperIndex - 1]
    val b = table[upperIndex]
    val ratio = (clampedAge - a.ageMonths) / (b.ageMonths - a.ageMonths)
    return WhoLmsPoint(
      ageMonths = clampedAge,
      l = a.l + (b.l - a.l) * ratio,
      m = a.m + (b.m - a.m) * ratio,
      s = a.s + (b.s - a.s) * ratio,
    )
  }

  fun valueForZ(lms: WhoLmsPoint, z: Double): Double {
    if (abs(lms.l) < 1e-5) {
      return lms.m * exp(lms.s * z)
    }
    val inner = (1.0 + lms.l * lms.s * z).coerceAtLeast(0.05)
    return lms.m * inner.pow(1.0 / lms.l)
  }

  fun zScoreForValue(lms: WhoLmsPoint, measuredValue: Double): Double {
    if (measuredValue <= 0.0) return -4.0
    return if (abs(lms.l) < 1e-5) {
      ln(measuredValue / lms.m) / lms.s
    } else {
      ((measuredValue / lms.m).pow(lms.l) - 1.0) / (lms.l * lms.s)
    }
  }

  fun zScoreToPercentile(z: Double): Double {
    val clamped = z.coerceIn(-4.0, 4.0)
    val x = abs(clamped)
    val t = 1.0 / (1.0 + 0.2316419 * x)
    val poly =
      ((((1.330274429 * t - 1.821255978) * t + 1.781477937) * t - 0.356563782) * t + 0.319381530) * t
    val pdf = 0.3989422804014327 * exp(-0.5 * x * x)
    val cdfPos = 1.0 - pdf * poly
    val cdf = if (clamped >= 0.0) cdfPos else 1.0 - cdfPos
    return (cdf * 100.0).coerceIn(0.1, 99.9)
  }

  fun buildCurves(
    metric: GrowthMetric,
    sex: ChildSex,
    minMonth: Double = 0.0,
    maxMonth: Double = 60.0,
    stepMonths: Double = 2.0,
  ): WhoPercentileCurves {
    require(stepMonths.isFinite() && stepMonths >= 0.05) { "Curve step must be finite and at least 0.05 months" }
    require(minMonth.isFinite() && maxMonth.isFinite() && minMonth <= maxMonth)
    val ages = mutableListOf<Double>()
    var cur = minMonth.coerceIn(0.0, 60.0)
    val end = maxMonth.coerceIn(cur, 60.0)
    while (cur <= end + 1e-6) {
      ages += cur.coerceAtMost(60.0)
      cur += stepMonths
    }
    if (ages.lastOrNull() != end) ages += end

    val p3 = mutableListOf<Double>()
    val p15 = mutableListOf<Double>()
    val p50 = mutableListOf<Double>()
    val p85 = mutableListOf<Double>()
    val p97 = mutableListOf<Double>()

    ages.forEach { month ->
      val lms = interpolateLms(metric, sex, month)
      p3 += valueForZ(lms, Z_P3)
      p15 += valueForZ(lms, Z_P15)
      p50 += valueForZ(lms, Z_P50)
      p85 += valueForZ(lms, Z_P85)
      p97 += valueForZ(lms, Z_P97)
    }

    return WhoPercentileCurves(
      agesMonths = ages,
      p3 = p3,
      p15 = p15,
      p50 = p50,
      p85 = p85,
      p97 = p97,
    )
  }

  fun evaluate(
    metric: GrowthMetric,
    sex: ChildSex,
    ageMonths: Double,
    measuredValue: Double,
  ): WhoGrowthAssessment? {
    if (!ageMonths.isFinite() || ageMonths < 0.0 || ageMonths > 60.0) return null
    if (!measuredValue.isFinite() || measuredValue < metric.minValid || measuredValue > metric.maxValid) return null

    val lms = interpolateLms(metric, sex, ageMonths)
    val z = zScoreForValue(lms, measuredValue)
    val percentile = zScoreToPercentile(z)
    val p3 = valueForZ(lms, Z_P3)
    val p15 = valueForZ(lms, Z_P15)
    val p50 = valueForZ(lms, Z_P50)
    val p85 = valueForZ(lms, Z_P85)
    val p97 = valueForZ(lms, Z_P97)

    val (bandFa, guidanceFa, needsReview) =
      when {
        percentile < 3.0 ->
          Triple(
            "زیر صدک ۳ (P3)",
            "این عدد پایین‌تر از صدک ۳ جدول WHO است. یک اندازه‌گیری به تنهایی تشخیص محسوب نمی‌شود؛ روند وزن‌گیری/قد در نوبت‌های قبلی را با پزشک اطفال یا مراقب سلامت بررسی کنید.",
            true,
          )
        percentile < 15.0 ->
          Triple(
            "بازه صدک ۳ تا ۱۵ (P3–P15)",
            "در محدوده میانی-پایین استاندارد WHO قرار دارد. حفظ پیوستگی منحنی فردی کودک در طول زمان مهم‌تر از عدد تکی است.",
            false,
          )
        percentile <= 85.0 ->
          Triple(
            "محدوده میانی استاندارد (P15–P85)",
            "نقطه بین خطوط صدک ۱۵ و ۸۵ است. این نتیجه سلامت رشد را تأیید نمی‌کند؛ افت روند یا نگرانی والد حتی در این بازه نیاز به بررسی دارد.",
            false,
          )
        percentile <= 97.0 ->
          Triple(
            "بازه صدک ۸۵ تا ۹۷ (P85–P97)",
            "این اندازه‌گیری بین خطوط صدک ۸۵ و ۹۷ است؛ به‌تنهایی نشانهٔ سلامت یا بیماری نیست. وزن نسبت به سن برای تشخیص چاقی کافی نیست.",
            false,
          )
        else ->
          Triple(
            "بالای صدک ۹۷ (P97)",
            "این مقدار بالاتر از صدک ۹۷ جدول مرجع WHO است. برای بررسی دقیق روند رشد و تغذیه با پزشک اطفال مشورت کنید.",
            true,
          )
      }

    return WhoGrowthAssessment(
      metric = metric,
      sex = sex,
      ageMonths = ageMonths,
      measuredValue = measuredValue,
      zScore = (z * 100.0).roundToInt() / 100.0,
      percentile = (percentile * 10.0).roundToInt() / 10.0,
      p3 = p3,
      p15 = p15,
      p50 = p50,
      p85 = p85,
      p97 = p97,
      bandLabelFa = bandFa,
      guidanceFa = guidanceFa,
      needsPediatricReview = needsReview,
    )
  }
}
