package com.example.domain.util

import com.example.data.FeverLogCloud
import com.example.data.MedicationDoseLogCloud
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.ui.components.AlertSeverity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

@Serializable
data class TimelineTempPoint(
  val id: String,
  val tempCelsius: Float,
  val timestampMs: Long,
  val timeLabelFa: String,
  val siteFa: String,
  val caregiverFa: String,
  val noteFa: String,
  val isSyncedCloud: Boolean,
)

@Serializable
data class TimelineDosePoint(
  val id: String,
  val medicationSlug: String,
  val medicationNameFa: String,
  val dosageFormFa: String,
  val volumeMlText: String,
  val timestampMs: Long,
  val givenAtLabelFa: String,
  val noteFa: String,
  val isSyncedCloud: Boolean,
)

data class AntipyreticIntervalStatus(
  val medFamilyLabelFa: String,
  val medicationKey: String,
  val minIntervalHours: Int,
  val maxDoses24h: Int,
  val dosesLast24h: Int,
  val minutesSinceLastDose: Long?,
  val minutesRemainingSafeInterval: Long,
  val lastDoseSummaryFa: String?,
  val severity: AlertSeverity,
  val statusMessageFa: String,
)

data class FeverDoseTimelineSnapshot(
  val windowStartMs: Long,
  val windowEndMs: Long,
  val tempPoints: List<TimelineTempPoint>,
  val dosePoints: List<TimelineDosePoint>,
  val latestTempCelsius: Float?,
  val peakTemp24h: Float?,
  val acetaminophenStatus: AntipyreticIntervalStatus,
  val ibuprofenStatus: AntipyreticIntervalStatus,
)

sealed interface TimelineFeedItem {
  val key: String
  val timestampMs: Long

  data class TemperatureEvent(val temp: TimelineTempPoint) : TimelineFeedItem {
    override val key: String = "temp_${temp.id}"
    override val timestampMs: Long = temp.timestampMs
  }

  data class DoseEvent(val dose: TimelineDosePoint) : TimelineFeedItem {
    override val key: String = "dose_${dose.id}"
    override val timestampMs: Long = dose.timestampMs
  }
}

data class HighFeverClinicalAlert(
  val triggerTempCelsius: Float,
  val caregiverFa: String,
  val timeLabelFa: String,
  val isHighFeverOver39: Boolean,
  val titleFa: String,
  val bodyFa: String,
  val checklistFa: List<String>,
)

object FeverDoseTimelineEngine {
  private const val HOUR_MS = 3_600_000L
  private const val WINDOW_24H_MS = 24L * HOUR_MS
  const val HIGH_FEVER_ALERT_THRESHOLD_C = 39.0f

  /**
   * Resolve the caregiver's HH:mm as a real timestamp, today or the previous day.
   * An invalid label returns null: it must never silently masquerade as a verified dose time.
   */
  fun resolveDoseTimeMs(labelFa: String, recordedAtMs: Long = System.currentTimeMillis()): Long? {
    val ascii = PersianNormalizer.toAsciiDigits(labelFa).trim()
    val match = Regex("""([01]?\d|2[0-3]):([0-5]\d)""").matchEntire(ascii) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    val cal = java.util.Calendar.getInstance().apply {
      timeInMillis = recordedAtMs
      set(java.util.Calendar.HOUR_OF_DAY, hour)
      set(java.util.Calendar.MINUTE, minute)
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    if (cal.timeInMillis > recordedAtMs + 60_000L) cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    return cal.timeInMillis.takeIf { it <= recordedAtMs + 60_000L && recordedAtMs - it < WINDOW_24H_MS }
  }

  fun evaluateHighFeverAlert(
    snapshot: FeverDoseTimelineSnapshot,
    childProfile: ChildProfileDemo? = null,
  ): HighFeverClinicalAlert? {
    val ageMonths = childProfile?.ageMonthsExact
      ?: LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(childProfile)
    val underThreeMonths = (ageMonths != null && ageMonths < 3) ||
      (ageMonths == null && childProfile?.ageBand == AgeBand.NEWBORN)
    val latestHigh = snapshot.tempPoints.lastOrNull {
      it.tempCelsius > HIGH_FEVER_ALERT_THRESHOLD_C ||
        (underThreeMonths && it.tempCelsius >= 38.0f)
    } ?: return null
    val tempStr = formatTempC(latestHigh.tempCelsius)
    val childLabel =
      childProfile?.aliasFa?.takeIf { it.isNotBlank() }?.let { "برای $it " } ?: ""
    val newbornUrgency = underThreeMonths && latestHigh.tempCelsius >= 38.0f
    return HighFeverClinicalAlert(
      triggerTempCelsius = latestHigh.tempCelsius,
      caregiverFa = latestHigh.caregiverFa,
      timeLabelFa = latestHigh.timeLabelFa,
      isHighFeverOver39 = latestHigh.tempCelsius > HIGH_FEVER_ALERT_THRESHOLD_C,
      titleFa = if (newbornUrgency) "هشدار فوری: تب ۳۸ درجه یا بیشتر در شیرخوار زیر ۳ ماه ($tempStr)"
        else "هشدار فوری تب بالای ۳۹ درجه ($tempStr) — مراجعه به پزشک",
      bodyFa = if (newbornUrgency) {
        "دمای ثبت‌شده ${childLabel}به $tempStr رسیده است. تب ۳۸ درجه یا بیشتر در شیرخوار زیر سه ماه نیازمند ارزیابی فوری حضوری توسط پزشک یا اورژانس است. داروی خودسرانه ندهید."
      } else {
        "دمای ثبت‌شده ${childLabel}در ساعت ${latestHigh.timeLabelFa} (توسط ${latestHigh.caregiverFa}) به $tempStr رسیده و از آستانه ۳۹.۰°C فراتر رفته است. لطفاً وضعیت کودک را فوری بررسی کرده و برای ویزیت به پزشک یا مرکز درمانی مراجعه کنید."
      },
      checklistFa =
        listOf(
          "در صورت بی‌حالی شدید، خواب‌آلودگی غیرعادی، تنفس تند یا تشنج فوراً با اورژانس ۱۱۵ تماس بگیرید.",
          "در نوزاد زیر ۳ ماه حتی تب ۳۸.۰°C نیازمند ارجاع فوری و اورژانسی است.",
          "از پاشویه با آب یخ یا الکل و دادن خودسرانه آسپیرین اکیداً خودداری کنید.",
          "گزارش ۲۴ ساعته خط زمانی و نوبت داروهای داده‌شده را همراه داشته باشید.",
        ),
    )
  }

  fun mergedChronologicalEvents(snapshot: FeverDoseTimelineSnapshot): List<TimelineFeedItem> {
    val temps = snapshot.tempPoints.map { TimelineFeedItem.TemperatureEvent(it) }
    val doses = snapshot.dosePoints.map { TimelineFeedItem.DoseEvent(it) }
    return (temps + doses).sortedByDescending { it.timestampMs }
  }

  fun formatRelativeTimeFa(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val diffMin = ((nowMs - timestampMs) / 60_000L).coerceAtLeast(0L)
    return if (diffMin < 1L) "هم‌اکنون" else "${formatDurationFa(diffMin)} پیش"
  }

  fun buildSnapshot(
    cloudFeverLogs: List<FeverLogCloud>,
    localFeverLogs: List<TimelineTempPoint>,
    cloudDoseLogs: List<MedicationDoseLogCloud>,
    localDoseLogs: List<TimelineDosePoint>,
    nowMs: Long = System.currentTimeMillis(),
  ): FeverDoseTimelineSnapshot {
    val windowStart = nowMs - WINDOW_24H_MS

    val cloudTempPoints =
      cloudFeverLogs.map { item ->
        val ts = item.createdAt?.toDate()?.time ?: nowMs
        TimelineTempPoint(
          id = item.id,
          tempCelsius = item.tempCelsius.toFloat().coerceIn(34.0f, 43.0f),
          timestampMs = ts,
          timeLabelFa = item.recordedAtLabelFa.ifBlank { formatHourMinute(ts) },
          siteFa = item.siteFa.ifBlank { "زیربغلی" },
          caregiverFa = item.caregiverFa.ifBlank { "والد" },
          noteFa = item.noteFa,
          isSyncedCloud = true,
        )
      }

    val allTemps =
      (cloudTempPoints + localFeverLogs)
        .filter { it.timestampMs in windowStart..(nowMs + 60_000L) }
        .sortedBy { it.timestampMs }

    val cloudDosePoints =
      cloudDoseLogs.map { item ->
        val recorded = item.createdAt?.toDate()?.time ?: nowMs
        val ts = item.administeredAtMs?.takeIf { it > 0 && it <= nowMs + 60_000L }
          ?: resolveDoseTimeMs(item.givenAtLabelFa, recorded) ?: recorded
        TimelineDosePoint(
          id = item.id,
          medicationSlug = item.medicationSlug,
          medicationNameFa = item.medicationNameFa,
          dosageFormFa = item.dosageFormFa,
          volumeMlText = item.volumeMlText,
          timestampMs = ts,
          givenAtLabelFa = item.givenAtLabelFa.ifBlank { formatHourMinute(ts) },
          noteFa = item.noteFa,
          isSyncedCloud = true,
        )
      }

    val allDoses =
      (cloudDosePoints + localDoseLogs)
        .filter { it.timestampMs in windowStart..(nowMs + 60_000L) }
        .sortedBy { it.timestampMs }

    val latestTemp = allTemps.lastOrNull()?.tempCelsius
    val peakTemp = allTemps.maxOfOrNull { it.tempCelsius }

    val acetStatus =
      evaluateAntipyreticInterval(
        medFamilyLabelFa = "خانواده استامینوفن",
        medicationKey = "acetaminophen",
        matchingDoses = allDoses.filter { isAcetaminophenFamily(it) },
        minIntervalHours = 4,
        maxDoses24h = 5,
        nowMs = nowMs,
      )

    val ibuStatus =
      evaluateAntipyreticInterval(
        medFamilyLabelFa = "خانواده ایبوپروفن",
        medicationKey = "ibuprofen",
        matchingDoses = allDoses.filter { isIbuprofenFamily(it) },
        minIntervalHours = 6,
        maxDoses24h = 4,
        nowMs = nowMs,
      )

    return FeverDoseTimelineSnapshot(
      windowStartMs = windowStart,
      windowEndMs = nowMs,
      tempPoints = allTemps,
      dosePoints = allDoses,
      latestTempCelsius = latestTemp,
      peakTemp24h = peakTemp,
      acetaminophenStatus = acetStatus,
      ibuprofenStatus = ibuStatus,
    )
  }

  private fun evaluateAntipyreticInterval(
    medFamilyLabelFa: String,
    medicationKey: String,
    matchingDoses: List<TimelineDosePoint>,
    minIntervalHours: Int,
    maxDoses24h: Int,
    nowMs: Long,
  ): AntipyreticIntervalStatus {
    val count = matchingDoses.size
    val latest = matchingDoses.maxByOrNull { it.timestampMs }
    if (latest == null) {
      return AntipyreticIntervalStatus(
        medFamilyLabelFa = medFamilyLabelFa,
        medicationKey = medicationKey,
        minIntervalHours = minIntervalHours,
        maxDoses24h = maxDoses24h,
        dosesLast24h = 0,
        minutesSinceLastDose = null,
        minutesRemainingSafeInterval = 0L,
        lastDoseSummaryFa = null,
        severity = AlertSeverity.INFO,
        statusMessageFa = "در ۲۴ ساعت گذشته نوبتی ثبت نشده است (حداقل فاصله ایمن معمول: هر $minIntervalHours ساعت).",
      )
    }

    val elapsedMin = ((nowMs - latest.timestampMs) / 60_000L).coerceAtLeast(0L)
    val minReqMin = minIntervalHours * 60L
    val remainingMin = (minReqMin - elapsedMin).coerceAtLeast(0L)
    val summary = "${latest.volumeMlText} در ساعت ${latest.givenAtLabelFa} (${formatDurationFa(elapsedMin)} پیش)"

    val (severity, msg) =
      when {
        count >= maxDoses24h ->
          AlertSeverity.DANGER to
            "هشدار سقف ۲۴ ساعته: $count نوبت در ۲۴ ساعت اخیر ثبت شده (سقف رایج $maxDoses24h نوبت). پیش از هر دوز جدید با پزشک یا داروساز مشورت کنید."
        remainingMin > 0L ->
          AlertSeverity.DANGER to
            "حداقل فاصله $minIntervalHours ساعته هنوز کامل نشده! ${formatDurationFa(remainingMin)} تا زمان ایمن بعدی باقی مانده تا از دوز تکراری جلوگیری شود."
        else ->
          AlertSeverity.SUCCESS to
            "حداقل فاصله $minIntervalHours ساعته سپری شده (${count} از $maxDoses24h نوبت در ۲۴ ساعت اخیر)."
      }

    return AntipyreticIntervalStatus(
      medFamilyLabelFa = medFamilyLabelFa,
      medicationKey = medicationKey,
      minIntervalHours = minIntervalHours,
      maxDoses24h = maxDoses24h,
      dosesLast24h = count,
      minutesSinceLastDose = elapsedMin,
      minutesRemainingSafeInterval = remainingMin,
      lastDoseSummaryFa = summary,
      severity = severity,
      statusMessageFa = msg,
    )
  }

  fun isAcetaminophenFamily(dose: TimelineDosePoint): Boolean {
    val combined = "${dose.medicationSlug} ${dose.medicationNameFa} ${dose.dosageFormFa}".lowercase(Locale.US)
    return combined.contains("acetaminophen") ||
      combined.contains("paracetamol") ||
      combined.contains("استامینوفن") ||
      combined.contains("otc-cough-cold")
  }

  fun isIbuprofenFamily(dose: TimelineDosePoint): Boolean {
    val combined = "${dose.medicationSlug} ${dose.medicationNameFa} ${dose.dosageFormFa}".lowercase(Locale.US)
    return combined.contains("ibuprofen") ||
      combined.contains("بروفن") ||
      combined.contains("ایبوپروفن")
  }

  fun formatHourMinute(timestampMs: Long): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.US)
    return fmt.format(Date(timestampMs))
  }

  fun formatDurationFa(totalMinutes: Long): String {
    val h = totalMinutes / 60L
    val m = totalMinutes % 60L
    return when {
      h == 0L -> "$m دقیقه"
      m == 0L -> "$h ساعت"
      else -> "$h ساعت و $m دقیقه"
    }
  }

  fun formatTempC(temp: Float): String {
    val rounded = (temp * 10f).roundToInt() / 10f
    return String.format(Locale.US, "%.1f°C", rounded)
  }

  fun buildDoctorSummaryReportFa(
    snapshot: FeverDoseTimelineSnapshot,
    childProfile: ChildProfileDemo?,
  ): String {
    val sb = StringBuilder()
    sb.appendLine("گزارش ۲۴ ساعته تب و دارو — مانو کید")
    if (childProfile != null) {
      sb.appendLine(
        "کودک: ${childProfile.aliasFa} (${childProfile.ageBand.labelFa} • وزن: ${childProfile.weightKgText.ifBlank { "ثبت‌نشده" }} kg)"
      )
    }
    snapshot.peakTemp24h?.let {
      sb.appendLine("بالاترین دمای ۲۴ ساعت: ${formatTempC(it)}")
    }
    sb.appendLine("ثبت‌های دما (${snapshot.tempPoints.size}):")
    if (snapshot.tempPoints.isEmpty()) {
      sb.appendLine("• موردی ثبت نشده است")
    } else {
      snapshot.tempPoints.reversed().take(8).forEach { t ->
        sb.appendLine("• ${t.timeLabelFa}: ${formatTempC(t.tempCelsius)} (${t.siteFa}) — توسط ${t.caregiverFa}")
      }
    }
    sb.appendLine("نوبت‌های دارو (${snapshot.dosePoints.size}):")
    if (snapshot.dosePoints.isEmpty()) {
      sb.appendLine("• موردی ثبت نشده است")
    } else {
      snapshot.dosePoints.reversed().take(8).forEach { d ->
        val notePart = if (d.noteFa.isNotBlank()) " (${d.noteFa})" else ""
        sb.appendLine("• ${d.givenAtLabelFa}: ${d.medicationNameFa} — ${d.volumeMlText}$notePart")
      }
    }
    return sb.toString().trim()
  }
}
