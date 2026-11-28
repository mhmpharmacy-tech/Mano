package com.example.domain.util

import com.example.data.FeverLogCloud
import com.example.data.MedicationDoseLogCloud
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.MedicationMonograph
import com.example.ui.components.AlertSeverity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

object FeverDoseCalculatorEngine {
  fun formatPersianDecimal(value: Float): String {
    val rounded = (value * 10f).roundToInt() / 10f
    val raw = if (rounded % 1f == 0f) rounded.toInt().toString() else String.format(Locale.US, "%.1f", rounded)
    return PersianNormalizer.toPersianDigits(raw)
  }
}

enum class LiquidFormulationId(
  val id: String,
  val titleFa: String,
  val shortTagFa: String,
  val ingredientKey: String,
  val mgPerMl: Float,
  val mgAmount: Double,
  val perMlAmount: Double,
  val maxSyringeMl: Float,
  val minSafeAgeMonths: Int,
  val doseMgPerKg: Float,
  val minMgPerKg: Float,
  val maxMgPerKg: Float,
) {
  ACETAMINOPHEN_DROPS_100(
    id = "acetaminophen_drops_100",
    titleFa = "قطره استامینوفن ۱۰۰ میلی‌گرم / ۱ میلی‌لیتر",
    shortTagFa = "قطره ۱۰۰mg/1mL",
    ingredientKey = "ACETAMINOPHEN",
    mgPerMl = 100f,
    mgAmount = 100.0,
    perMlAmount = 1.0,
    maxSyringeMl = 3f,
    minSafeAgeMonths = 3,
    doseMgPerKg = 12.5f,
    minMgPerKg = 10f,
    maxMgPerKg = 15f,
  ),
  ACETAMINOPHEN_SYRUP_120_5(
    id = "acetaminophen_syrup_120_5",
    titleFa = "شربت استامینوفن ۱۲۰ میلی‌گرم / ۵ میلی‌لیتر",
    shortTagFa = "شربت ۱۲۰mg/5mL",
    ingredientKey = "ACETAMINOPHEN",
    mgPerMl = 24f,
    mgAmount = 120.0,
    perMlAmount = 5.0,
    maxSyringeMl = 10f,
    minSafeAgeMonths = 3,
    doseMgPerKg = 12.5f,
    minMgPerKg = 10f,
    maxMgPerKg = 15f,
  ),
  ACETAMINOPHEN_SYRUP_160_5(
    id = "acetaminophen_syrup_160_5",
    titleFa = "شربت استامینوفن ۱۶۰ میلی‌گرم / ۵ میلی‌لیتر",
    shortTagFa = "شربت ۱۶۰mg/5mL",
    ingredientKey = "ACETAMINOPHEN",
    mgPerMl = 32f,
    mgAmount = 160.0,
    perMlAmount = 5.0,
    maxSyringeMl = 10f,
    minSafeAgeMonths = 3,
    doseMgPerKg = 12.5f,
    minMgPerKg = 10f,
    maxMgPerKg = 15f,
  ),
  IBUPROFEN_SYRUP_100_5(
    id = "ibuprofen_syrup_100_5",
    titleFa = "سوسپانسیون ایبوپروفن ۱۰۰ میلی‌گرم / ۵ میلی‌لیتر",
    shortTagFa = "ایبوپروفن ۱۰۰mg/5mL",
    ingredientKey = "IBUPROFEN",
    mgPerMl = 20f,
    mgAmount = 100.0,
    perMlAmount = 5.0,
    maxSyringeMl = 10f,
    minSafeAgeMonths = 6,
    doseMgPerKg = 7.5f,
    minMgPerKg = 5f,
    maxMgPerKg = 10f,
  ),
  CETIRIZINE_SYRUP_5_5(
    id = "cetirizine_syrup_5_5",
    titleFa = "شربت ستیریزین ۵ میلی‌گرم / ۵ میلی‌لیتر",
    shortTagFa = "ستیریزین ۵mg/5mL",
    ingredientKey = "CETIRIZINE",
    mgPerMl = 1f,
    mgAmount = 5.0,
    perMlAmount = 5.0,
    maxSyringeMl = 10f,
    minSafeAgeMonths = 24,
    doseMgPerKg = 0.2f,
    minMgPerKg = 0.15f,
    maxMgPerKg = 0.25f,
  ),
  ;

  val shortLabelFa: String get() = shortTagFa
  val concentrationLabelFa: String get() = titleFa
  val maxSyringeScaleMl: Float get() = maxSyringeMl
  val intervalFa: String
    get() =
      when (this) {
        ACETAMINOPHEN_DROPS_100,
        ACETAMINOPHEN_SYRUP_120_5,
        ACETAMINOPHEN_SYRUP_160_5 -> "هر ۴ تا ۶ ساعت (حداکثر ۴ نوبت در ۲۴ ساعت)"
        IBUPROFEN_SYRUP_100_5 -> "هر ۶ تا ۸ ساعت همراه با شیر یا غذا (زیر ۶ ماه ممنوع)"
        CETIRIZINE_SYRUP_5_5 -> "روزی یک نوبت طبق دستور پزشک (زیر ۲ سال ممنوع)"
      }
}

data class ClinicalDoseCalculationResult(
  val formulation: LiquidFormulationId,
  val isBlocked: Boolean,
  val isWeightBased: Boolean,
  val recommendedMl: Float,
  val minSafeMl: Float,
  val maxSafeMl: Float,
  val recommendedMg: Int,
  val alertSeverity: AlertSeverity,
  val alertTitleFa: String,
  val alertBodyFa: String,
)

object LiquidDoseVisualizerEngine {
  fun formatMl(value: Float): String = "${FeverDoseCalculatorEngine.formatPersianDecimal(value)} mL"

  fun parseAgeMonthsFromText(raw: String): Int? {
    val clean = PersianNormalizer.normalize(raw)
    if (clean.isEmpty()) return null
    val monthsMatch = Regex("(\\d+)\\s*ماه").find(clean)
    if (monthsMatch != null) return monthsMatch.groupValues[1].toIntOrNull()
    val yearsMatch = Regex("(\\d+)\\s*سال").find(clean)
    if (yearsMatch != null) return yearsMatch.groupValues[1].toIntOrNull()?.times(12)
    return clean.toIntOrNull()
  }

  fun initialAgeMonthsFromProfile(
    profile: ChildProfileDemo?,
    birthDateEpochMs: Long? = null,
  ): Int? {
    if (birthDateEpochMs != null && birthDateEpochMs > 0L) {
      val diffDays = ((System.currentTimeMillis() - birthDateEpochMs) / (24L * 3600L * 1000L)).coerceAtLeast(0L)
      return (diffDays / 30L).toInt()
    }
    if (profile?.ageMonthsExact != null) return profile.ageMonthsExact
    val fromText = profile?.birthDateOrAgeFa?.let { parseAgeMonthsFromText(it) }
    if (fromText != null) return fromText
    return when (profile?.ageBand) {
      AgeBand.NEWBORN -> 1
      AgeBand.INFANT_3_12M -> 6
      AgeBand.TODDLER_1_3Y -> 24
      AgeBand.PRESCHOOL_3_6Y -> 48
      AgeBand.SCHOOL_6_PLUS -> 72
      null -> null
    }
  }

  fun initialWeightKgFromProfile(profile: ChildProfileDemo?): Float? {
    val raw = profile?.weightKgText ?: return null
    return ClinicalNumberInput.decimal(raw)?.toFloat()?.takeIf { it in 2f..80f }
  }

  fun evaluate(
    formulation: LiquidFormulationId,
    ageMonths: Int?,
    weightKg: Float?,
    customMgPerMl: Float? = null,
  ): ClinicalDoseCalculationResult {
    val effectiveMgPerMl = (customMgPerMl ?: formulation.mgPerMl).coerceAtLeast(0.1f)
    if (ageMonths == null) {
      return ClinicalDoseCalculationResult(
        formulation = formulation,
        isBlocked = true,
        isWeightBased = false,
        recommendedMl = 0f,
        minSafeMl = 0f,
        maxSafeMl = 0f,
        recommendedMg = 0,
        alertSeverity = AlertSeverity.CAUTION,
        alertTitleFa = "سن کودک برای بررسی ایمنی ضروری است",
        alertBodyFa = "پیش از نمایش حجم روی سرنگ، سن کودک را به ماه وارد کنید تا منع سنی (مثل زیر ۳ یا ۶ ماه) بررسی شود.",
      )
    }

    if (ageMonths < formulation.minSafeAgeMonths) {
      return ClinicalDoseCalculationResult(
        formulation = formulation,
        isBlocked = true,
        isWeightBased = false,
        recommendedMl = 0f,
        minSafeMl = 0f,
        maxSafeMl = 0f,
        recommendedMg = 0,
        alertSeverity = AlertSeverity.DANGER,
        alertTitleFa = "محدودیت سنی — نیازمند نظر پزشک",
        alertBodyFa = "برای کودک زیر ${PersianNormalizer.toPersianDigits(formulation.minSafeAgeMonths.toString())} ماه، محاسبه خانگی این فرآورده قفل است؛ حتماً با پزشک یا داروساز مشورت کنید.",
      )
    }

    if (weightKg == null || weightKg < 2.5f || weightKg > 75f) {
      return ClinicalDoseCalculationResult(
        formulation = formulation,
        isBlocked = true,
        isWeightBased = false,
        recommendedMl = 0f,
        minSafeMl = 0f,
        maxSafeMl = 0f,
        recommendedMg = 0,
        alertSeverity = AlertSeverity.CAUTION,
        alertTitleFa = "وزن معتبر کودک ثبت نشده است",
        alertBodyFa = "برای نمایش محدوده آموزشی بر حسب میلی‌لیتر، وزن دقیق کودک (کیلوگرم) را وارد کنید.",
      )
    }

    if (formulation == LiquidFormulationId.CETIRIZINE_SYRUP_5_5) {
      val targetMg = if (ageMonths < 72) 2.5f else 5.0f
      val targetMl = (targetMg / effectiveMgPerMl).coerceIn(0.5f, formulation.maxSyringeMl)
      return ClinicalDoseCalculationResult(
        formulation = formulation,
        isBlocked = false,
        isWeightBased = false,
        recommendedMl = targetMl,
        minSafeMl = targetMl,
        maxSafeMl = targetMl,
        recommendedMg = targetMg.roundToInt(),
        alertSeverity = AlertSeverity.INFO,
        alertTitleFa = "الگوی آموزشی ستیریزین بر اساس رده سنی",
        alertBodyFa = "این مقدار صرفاً نمایش آموزشی استاندارد بر اساس غلظت ۵mg/5mL است و جایگزین دستور پزشک یا داروساز نیست.",
      )
    }

    val rawRecMg = (weightKg * formulation.doseMgPerKg).coerceAtMost(
      if (formulation.ingredientKey == "ACETAMINOPHEN") 500f else 400f
    )
    val minMg = (weightKg * formulation.minMgPerKg).coerceAtMost(rawRecMg)
    val maxMg = (weightKg * formulation.maxMgPerKg).coerceAtMost(
      if (formulation.ingredientKey == "ACETAMINOPHEN") 500f else 400f
    )
    val recMl = ((rawRecMg / effectiveMgPerMl) * 10f).roundToInt() / 10f
    val minMl = ((minMg / effectiveMgPerMl) * 10f).roundToInt() / 10f
    val maxMl = ((maxMg / effectiveMgPerMl) * 10f).roundToInt() / 10f

    return ClinicalDoseCalculationResult(
      formulation = formulation,
      isBlocked = false,
      isWeightBased = true,
      recommendedMl = recMl.coerceIn(0.2f, formulation.maxSyringeMl),
      minSafeMl = minMl.coerceIn(0.2f, formulation.maxSyringeMl),
      maxSafeMl = maxMl.coerceIn(0.2f, formulation.maxSyringeMl),
      recommendedMg = rawRecMg.roundToInt(),
      alertSeverity = AlertSeverity.SUCCESS,
      alertTitleFa = "نمایش آموزشی سرنگ بر اساس وزن و غلظت انتخاب‌شده",
      alertBodyFa = "پیش از کشیدن دارو، غلظت روی جعبه دارو را با ${formulation.shortTagFa} مطابقت دهید و بیش از ۴ نوبت در ۲۴ ساعت مصرف نکنید.",
    )
  }
}

data class MedicationOverlapResult(
  val severity: AlertSeverity,
  val titleFa: String,
  val summaryFa: String,
  val actionStepsFa: List<String>,
)

object MedicationOverlapEngine {
  fun evaluatePair(medA: MedicationMonograph, medB: MedicationMonograph): MedicationOverlapResult {
    val pairText = PersianNormalizer.normalize("${medA.nameFa} ${medA.genericNameEn} ${medB.nameFa} ${medB.genericNameEn}")
    val aNorm = PersianNormalizer.normalize("${medA.nameFa} ${medA.genericNameEn}")
    val bNorm = PersianNormalizer.normalize("${medB.nameFa} ${medB.genericNameEn}")

    val aIsAcet = aNorm.contains("acetaminophen") || aNorm.contains("استامینوفن") || aNorm.contains("سرماخوردگی")
    val bIsAcet = bNorm.contains("acetaminophen") || bNorm.contains("استامینوفن") || bNorm.contains("سرماخوردگی")
    val aIsNsaid = aNorm.contains("ibuprofen") || aNorm.contains("ایبوپروفن")
    val bIsNsaid = bNorm.contains("ibuprofen") || bNorm.contains("ایبوپروفن")

    return when {
      medA.slug == medB.slug -> MedicationOverlapResult(
        severity = AlertSeverity.CAUTION,
        titleFa = "یک فرآورده دو بار انتخاب شده است",
        summaryFa = "مصرف تکراری یک دارو قبل از پایان فاصله زمانی ایمن می‌تواند منجر به بیش‌دوزی شود.",
        actionStepsFa = listOf(
          "فاصله زمانی مجاز و سقف نوبت ۲۴ ساعته را با داروساز چک کنید.",
          "هر نوبت مصرف را بلافاصله در دفترچه مصرف ثبت کنید.",
        ),
      )
      aIsAcet && bIsAcet -> MedicationOverlapResult(
        severity = AlertSeverity.DANGER,
        titleFa = "هشدار ماده مؤثر تکراری: استامینوفن!",
        summaryFa = "هر دو فرآورده انتخابی ممکن است حاوی استامینوفن باشند؛ مصرف هم‌زمان خطر آسیب کبدی ناشی از بیش‌دوزی دارد.",
        actionStepsFa = listOf(
          "از دادن هم‌زمان این دو دارو خودداری کنید.",
          "ترکیبات روی جعبه شربت سرماخوردگی را از نظر وجود Paracetamol/Acetaminophen بررسی کنید.",
          "پیش از ادامه مصرف با داروساز مشورت کنید.",
        ),
      )
      aIsNsaid && bIsNsaid -> MedicationOverlapResult(
        severity = AlertSeverity.DANGER,
        titleFa = "هشدار هم‌پوشانی داروهای ضدالتهاب (NSAID)",
        summaryFa = "مصرف هم‌زمان دو داروی هم‌خانواده ایبوپروفن مجاز نیست و عوارض گوارشی و کلیوی ایجاد می‌کند.",
        actionStepsFa = listOf("فقط یک فرآورده ضدالتهاب طبق تجویز پزشک مصرف شود."),
      )
      (aIsAcet && bIsNsaid) || (aIsNsaid && bIsAcet) -> MedicationOverlapResult(
        severity = AlertSeverity.CAUTION,
        titleFa = "نیازمند زمان‌بندی دقیق و نظر پزشک",
        summaryFa = "تناوب استامینوفن و ایبوپروفن در خانه بدون برنامه مکتوب پزشک یا داروساز می‌تواند باعث خطای فاصله دوز شود.",
        actionStepsFa = listOf(
          "در کودکان زیر ۶ ماه مصرف ایبوپروفن ممنوع است.",
          "ساعت دقیق هر نوبت را در خط زمانی تب ثبت کنید و با داروساز هماهنگ باشید.",
        ),
      )
      pairText.contains("antibiotic") || medA.category.id == "antibiotics" || medB.category.id == "antibiotics" -> MedicationOverlapResult(
        severity = AlertSeverity.INFO,
        titleFa = "بررسی تکمیل دوره و فاصله مصرف با کارشناس",
        summaryFa = "آنتی‌بیوتیک باید سر ساعت و تا پایان دوره کامل با فاصله مناسب از مکمل‌ها (مثل آهن یا زینک در برخی موارد) مصرف شود.",
        actionStepsFa = listOf("فاصله زمانی با لبنیات یا مکمل‌ها را روی برچسب داروخانه بخوانید."),
      )
      else -> MedicationOverlapResult(
        severity = AlertSeverity.SUCCESS,
        titleFa = "هم‌پوشانی تکراری آشکار شناسایی نشد (تأیید نهایی با داروساز)",
        summaryFa = "در مقایسه اولیه دو نام، ماده مؤثر مشابهی دیده نشد؛ با این حال همیشه برچسب ترکیبات جانبی شربت‌ها را چک کنید.",
        actionStepsFa = listOf(
          "نام برند تجاری روی جعبه را در صورت ابهام در بخش «پرسش از داروساز» ارسال کنید.",
        ),
      )
    }
  }
}

@Serializable
data class TimelineTempPoint(
  val id: String,
  val tempCelsius: Float,
  val siteFa: String,
  val timeLabelFa: String,
  val caregiverFa: String,
  val noteFa: String = "",
  val timestampMs: Long = System.currentTimeMillis(),
  val isSyncedCloud: Boolean = false,
)

@Serializable
data class TimelineDosePoint(
  val id: String,
  val medicationSlug: String,
  val medicationNameFa: String,
  val dosageFormFa: String,
  val volumeMlText: String,
  val timestampMs: Long = System.currentTimeMillis(),
  val givenAtLabelFa: String = "",
  val noteFa: String = "",
  val isSyncedCloud: Boolean = false,
)

data class FeverDoseTimelineSnapshot(
  val tempPoints: List<TimelineTempPoint>,
  val dosePoints: List<TimelineDosePoint>,
  val peakTemp24h: Float?,
  val latestTempCelsius: Float?,
  val doseCount24h: Int,
  val acetaminophenCount24h: Int,
  val ibuprofenCount24h: Int,
  val hasFeverAlert: Boolean,
) {
  val latestTempTimeFa: String get() = tempPoints.lastOrNull()?.timeLabelFa ?: ""
}

data class HighFeverClinicalAlert(
  val titleFa: String,
  val bodyFa: String,
  val tempCelsius: Float,
  val isNewbornEmergency: Boolean,
) {
  val summaryFa: String get() = bodyFa
}

object FeverDoseTimelineEngine {
  const val HIGH_FEVER_ALERT_THRESHOLD_C = 39.0f

  fun formatHourMinute(timestampMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.US)
    return PersianNormalizer.toPersianDigits(sdf.format(Date(timestampMs)))
  }

  fun formatTempC(temp: Float): String =
    "${FeverDoseCalculatorEngine.formatPersianDecimal(temp)} °C"

  fun formatDecimalFa(value: Float): String =
    FeverDoseCalculatorEngine.formatPersianDecimal(value)

  fun resolveDoseTimeMs(labelFa: String, fallbackMs: Long = System.currentTimeMillis()): Long? {
    val ascii = PersianNormalizer.toAsciiDigits(labelFa).trim()
    val match = Regex("(\\d{1,2}):(\\d{2})").find(ascii) ?: return null
    val hour = match.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return null
    val min = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return null
    val cal = java.util.Calendar.getInstance().apply {
      timeInMillis = fallbackMs
      set(java.util.Calendar.HOUR_OF_DAY, hour)
      set(java.util.Calendar.MINUTE, min)
      set(java.util.Calendar.SECOND, 0)
    }
    return cal.timeInMillis
  }

  fun buildSnapshot(
    cloudFeverLogs: List<FeverLogCloud> = emptyList(),
    localFeverLogs: List<TimelineTempPoint> = emptyList(),
    cloudDoseLogs: List<MedicationDoseLogCloud> = emptyList(),
    localDoseLogs: List<TimelineDosePoint> = emptyList(),
  ): FeverDoseTimelineSnapshot {
    val mergedTemps = (
      cloudFeverLogs.map { c ->
        TimelineTempPoint(
          id = c.id,
          tempCelsius = c.tempCelsius.toFloat(),
          siteFa = c.siteFa,
          timeLabelFa = c.recordedAtLabelFa.ifBlank { formatHourMinute(c.createdAt?.toDate()?.time ?: System.currentTimeMillis()) },
          caregiverFa = c.caregiverFa,
          noteFa = c.noteFa,
          timestampMs = c.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
          isSyncedCloud = true,
        )
      } + localFeverLogs
      ).distinctBy { it.id }.sortedBy { it.timestampMs }

    val mergedDoses = (
      cloudDoseLogs.map { c ->
        val ts = c.administeredAtMs.takeIf { it > 0L } ?: c.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        TimelineDosePoint(
          id = c.id,
          medicationSlug = c.medicationSlug,
          medicationNameFa = c.medicationNameFa,
          dosageFormFa = c.dosageFormFa,
          volumeMlText = c.volumeMlText,
          timestampMs = ts,
          givenAtLabelFa = c.givenAtLabelFa.ifBlank { formatHourMinute(ts) },
          noteFa = c.noteFa,
          isSyncedCloud = true,
        )
      } + localDoseLogs
      ).distinctBy { it.id }.sortedBy { it.timestampMs }

    val now = System.currentTimeMillis()
    val cutoff24h = now - 24L * 3600L * 1000L
    val temps24 = mergedTemps.filter { it.timestampMs >= cutoff24h }
    val doses24 = mergedDoses.filter { it.timestampMs >= cutoff24h }
    val peak24 = temps24.maxOfOrNull { it.tempCelsius } ?: mergedTemps.maxOfOrNull { it.tempCelsius }
    val latest = mergedTemps.lastOrNull()?.tempCelsius
    val acetCount = doses24.count {
      val n = PersianNormalizer.normalize("${it.medicationSlug} ${it.medicationNameFa}")
      n.contains("acetaminophen") || n.contains("استامینوفن")
    }
    val ibuCount = doses24.count {
      val n = PersianNormalizer.normalize("${it.medicationSlug} ${it.medicationNameFa}")
      n.contains("ibuprofen") || n.contains("ایبوپروفن")
    }

    return FeverDoseTimelineSnapshot(
      tempPoints = mergedTemps,
      dosePoints = mergedDoses,
      peakTemp24h = peak24,
      latestTempCelsius = latest,
      doseCount24h = doses24.size,
      acetaminophenCount24h = acetCount,
      ibuprofenCount24h = ibuCount,
      hasFeverAlert = (latest ?: 0f) >= HIGH_FEVER_ALERT_THRESHOLD_C,
    )
  }

  fun evaluateHighFeverAlert(
    snapshot: FeverDoseTimelineSnapshot,
    childProfile: ChildProfileDemo?,
  ): HighFeverClinicalAlert? {
    val latest = snapshot.tempPoints.lastOrNull() ?: return null
    val ageMonths = childProfile?.ageMonthsExact
      ?: LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(childProfile)
    val isUnder3Months = (ageMonths != null && ageMonths < 3) ||
      (ageMonths == null && childProfile?.ageBand == AgeBand.NEWBORN)
    return when {
      isUnder3Months && latest.tempCelsius >= 38.0f -> HighFeverClinicalAlert(
        titleFa = "هشدار فوری تب در نوزاد زیر ۳ ماه",
        bodyFa = "دمای ${formatTempC(latest.tempCelsius)} برای نوزاد زیر ۳ ماه نیازمند ارزیابی فوری اورژانس پزشکی است. از دادن داروی خودسرانه خودداری کنید.",
        tempCelsius = latest.tempCelsius,
        isNewbornEmergency = true,
      )
      latest.tempCelsius > HIGH_FEVER_ALERT_THRESHOLD_C -> HighFeverClinicalAlert(
        titleFa = "هشدار تب بالا (${formatTempC(latest.tempCelsius)})",
        bodyFa = "ثبت دمای بالای ۳۹ درجه نیازمند پایش علائم خطر (بی‌حالی، تنگی نفس، تشنج، کاهش ادرار) و تماس با پزشک یا اورژانس در صورت تداوم است.",
        tempCelsius = latest.tempCelsius,
        isNewbornEmergency = false,
      )
      else -> null
    }
  }
}
