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
 * Deterministic WHO Child Growth Standards (2006, 0–60 months) LMS reference engine.
 * Computes smoothed L, M, S interpolation, reference percentile curves (P3, P15, P50, P85, P97),
 * Z-scores, and non-diagnostic educational percentile bands without external charting SDKs.
 */
object WhoGrowthStandardsEngine {

  private const val Z_P3 = -1.8807936
  private const val Z_P15 = -1.0364334
  private const val Z_P50 = 0.0
  private const val Z_P85 = 1.0364334
  private const val Z_P97 = 1.8807936

  // WHO Child Growth Standards: Weight-for-age (0–60 months), Boys
  private val weightBoysLms =
    listOf(
      WhoLmsPoint(0.0, 0.3487, 3.3464, 0.14602),
      WhoLmsPoint(1.0, 0.2297, 4.4709, 0.13395),
      WhoLmsPoint(2.0, 0.1970, 5.5675, 0.12385),
      WhoLmsPoint(3.0, 0.1738, 6.3762, 0.11727),
      WhoLmsPoint(4.0, 0.1553, 7.0023, 0.11316),
      WhoLmsPoint(5.0, 0.1395, 7.5105, 0.11080),
      WhoLmsPoint(6.0, 0.1257, 7.9340, 0.10958),
      WhoLmsPoint(9.0, 0.0920, 8.9022, 0.10798),
      WhoLmsPoint(12.0, 0.0644, 9.6479, 0.10665),
      WhoLmsPoint(15.0, 0.0398, 10.3046, 0.10631),
      WhoLmsPoint(18.0, 0.0170, 10.9403, 0.10695),
      WhoLmsPoint(24.0, -0.0238, 12.1515, 0.10957),
      WhoLmsPoint(30.0, -0.0593, 13.2665, 0.11264),
      WhoLmsPoint(36.0, -0.0906, 14.3429, 0.11572),
      WhoLmsPoint(42.0, -0.1182, 15.3712, 0.11940),
      WhoLmsPoint(48.0, -0.1427, 16.3489, 0.12352),
      WhoLmsPoint(54.0, -0.1645, 17.3264, 0.12789),
      WhoLmsPoint(60.0, -0.1839, 18.3366, 0.13227),
    )

  // WHO Child Growth Standards: Weight-for-age (0–60 months), Girls
  private val weightGirlsLms =
    listOf(
      WhoLmsPoint(0.0, 0.3809, 3.2322, 0.14171),
      WhoLmsPoint(1.0, 0.1714, 4.1873, 0.13724),
      WhoLmsPoint(2.0, 0.0962, 5.1282, 0.13000),
      WhoLmsPoint(3.0, 0.0402, 5.8458, 0.12619),
      WhoLmsPoint(4.0, -0.0050, 6.4237, 0.12402),
      WhoLmsPoint(5.0, -0.0430, 6.8985, 0.12274),
      WhoLmsPoint(6.0, -0.0756, 7.2970, 0.12204),
      WhoLmsPoint(9.0, -0.1500, 8.2254, 0.12116),
      WhoLmsPoint(12.0, -0.1945, 8.9481, 0.12029),
      WhoLmsPoint(15.0, -0.2194, 9.6050, 0.11971),
      WhoLmsPoint(18.0, -0.2330, 10.2322, 0.11986),
      WhoLmsPoint(24.0, -0.2421, 11.4775, 0.12131),
      WhoLmsPoint(30.0, -0.2392, 12.6901, 0.12377),
      WhoLmsPoint(36.0, -0.2323, 13.8503, 0.12687),
      WhoLmsPoint(42.0, -0.2236, 14.9693, 0.13022),
      WhoLmsPoint(48.0, -0.2140, 16.0697, 0.13370),
      WhoLmsPoint(54.0, -0.2039, 17.1520, 0.13719),
      WhoLmsPoint(60.0, -0.1934, 18.2193, 0.14065),
    )

  // WHO Child Growth Standards: Length/Height-for-age (0–60 months), Boys
  private val heightBoysLms =
    listOf(
      WhoLmsPoint(0.0, 1.0, 49.8842, 0.03795),
      WhoLmsPoint(1.0, 1.0, 54.7244, 0.03557),
      WhoLmsPoint(2.0, 1.0, 58.4249, 0.03424),
      WhoLmsPoint(3.0, 1.0, 61.4292, 0.03328),
      WhoLmsPoint(4.0, 1.0, 63.8860, 0.03257),
      WhoLmsPoint(5.0, 1.0, 65.9026, 0.03204),
      WhoLmsPoint(6.0, 1.0, 67.6236, 0.03165),
      WhoLmsPoint(9.0, 1.0, 71.9687, 0.03117),
      WhoLmsPoint(12.0, 1.0, 75.7488, 0.03137),
      WhoLmsPoint(15.0, 1.0, 79.1462, 0.03201),
      WhoLmsPoint(18.0, 1.0, 82.2587, 0.03283),
      WhoLmsPoint(24.0, 1.0, 87.1161, 0.03465),
      WhoLmsPoint(30.0, 1.0, 91.9308, 0.03651),
      WhoLmsPoint(36.0, 1.0, 96.0835, 0.03824),
      WhoLmsPoint(42.0, 1.0, 99.8950, 0.03970),
      WhoLmsPoint(48.0, 1.0, 103.3273, 0.04088),
      WhoLmsPoint(54.0, 1.0, 106.6634, 0.04182),
      WhoLmsPoint(60.0, 1.0, 109.9921, 0.04257),
    )

  // WHO Child Growth Standards: Length/Height-for-age (0–60 months), Girls
  private val heightGirlsLms =
    listOf(
      WhoLmsPoint(0.0, 1.0, 49.1477, 0.03790),
      WhoLmsPoint(1.0, 1.0, 53.6872, 0.03640),
      WhoLmsPoint(2.0, 1.0, 57.0673, 0.03568),
      WhoLmsPoint(3.0, 1.0, 59.8029, 0.03520),
      WhoLmsPoint(4.0, 1.0, 62.0899, 0.03486),
      WhoLmsPoint(5.0, 1.0, 64.0301, 0.03463),
      WhoLmsPoint(6.0, 1.0, 65.7311, 0.03448),
      WhoLmsPoint(9.0, 1.0, 70.1435, 0.03449),
      WhoLmsPoint(12.0, 1.0, 74.0150, 0.03497),
      WhoLmsPoint(15.0, 1.0, 77.5236, 0.03574),
      WhoLmsPoint(18.0, 1.0, 80.7079, 0.03661),
      WhoLmsPoint(24.0, 1.0, 85.7153, 0.03846),
      WhoLmsPoint(30.0, 1.0, 90.6976, 0.04016),
      WhoLmsPoint(36.0, 1.0, 95.0515, 0.04156),
      WhoLmsPoint(42.0, 1.0, 99.0442, 0.04269),
      WhoLmsPoint(48.0, 1.0, 102.7312, 0.04358),
      WhoLmsPoint(54.0, 1.0, 106.1731, 0.04427),
      WhoLmsPoint(60.0, 1.0, 109.4239, 0.04481),
    )

  fun lmsTableFor(metric: GrowthMetric, sex: ChildSex): List<WhoLmsPoint> =
    when (metric) {
      GrowthMetric.WEIGHT_FOR_AGE -> if (sex == ChildSex.BOY) weightBoysLms else weightGirlsLms
      GrowthMetric.HEIGHT_FOR_AGE -> if (sex == ChildSex.BOY) heightBoysLms else heightGirlsLms
    }

  fun interpolateLms(metric: GrowthMetric, sex: ChildSex, ageMonths: Double): WhoLmsPoint {
    val clampedAge = ageMonths.coerceIn(0.0, 60.0)
    val table = lmsTableFor(metric, sex)
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
    if (ageMonths.isNaN() || ageMonths < 0.0 || ageMonths > 60.0) return null
    if (measuredValue.isNaN() || measuredValue < metric.minValid || measuredValue > metric.maxValid) return null

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
            "نقطه ثبت‌شده در محدوده شایع استاندارد رشد سازمان جهانی بهداشت (WHO) برای این سن و جنس قرار دارد.",
            false,
          )
        percentile <= 97.0 ->
          Triple(
            "بازه صدک ۸۵ تا ۹۷ (P85–P97)",
            "در محدوده بالای طبیعی استاندارد WHO قرار دارد؛ هماهنگی روند قد و وزن در مراجعات دوره‌ای بررسی می‌شود.",
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
