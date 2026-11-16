package com.example.domain.util

import com.example.domain.model.VaccineDashboardSummary
import com.example.domain.model.VaccineFilter
import com.example.domain.model.VaccineRecord
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.model.VaccineResolvedItem
import com.example.domain.model.VaccineScheduleEntry
import com.example.domain.model.VaccineStatus
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object VaccinationScheduleCatalog {

  val scheduleEntries: List<VaccineScheduleEntry> =
    listOf(
      VaccineScheduleEntry(
        id = "birth_bcg_hbv_opv",
        titleFa = "واکسن‌های بدو تولد (ب‌ث‌ژ، هپاتیت B و فلج اطفال)",
        milestoneLabelFa = "بدو تولد",
        recommendedAgeDays = 0,
        vaccinesIncludedFa = "ب‌ث‌ژ (سل) + هپاتیت B نوبت ۰ + فلج اطفال خوراکی بدو تولد",
        administrationMethodFa = "تزریقی و قطره خوراکی در بیمارستان یا مرکز بهداشت پیش از ترخیص",
        careNotesFa = "مراقب جای زخم واکسن ب‌ث‌ژ باشید؛ از دستکاری یا پماد زدن خودسرانه پرهیز کنید و تاریخ نوبت ۲ ماهگی را یادداشت نمایید.",
      ),
      VaccineScheduleEntry(
        id = "2m_pentavalent_opv_rota",
        titleFa = "واکسیناسیون ۲ ماهگی (پنج‌گانه نوبت ۱)",
        milestoneLabelFa = "۲ ماهگی",
        recommendedAgeDays = 60,
        vaccinesIncludedFa = "واکسن پنج‌گانه (دیفتری، کزاز، سیاه‌سرفه، هپاتیت B، هموفیلوس آنفلوانزا) + فلج اطفال خوراکی ۱ (+ روتاویروس/پنوموکوک طبق برنامه)",
        administrationMethodFa = "تزریق عضلانی ران و قطره خوراکی در مرکز بهداشت",
        careNotesFa = "پس از واکسن پنج‌گانه احتمال تب خفیف تا ۲۴–۴۸ ساعت و بی‌قراری طبیعی است؛ دوز استامینوفن فقط بر اساس وزن کودک و دستور پزشک/مرکز بهداشت تنظیم شود.",
      ),
      VaccineScheduleEntry(
        id = "4m_pentavalent_opv_ipv",
        titleFa = "واکسیناسیون ۴ ماهگی (پنج‌گانه نوبت ۲ + IPV)",
        milestoneLabelFa = "۴ ماهگی",
        recommendedAgeDays = 120,
        vaccinesIncludedFa = "واکسن پنج‌گانه نوبت ۲ + فلج اطفال خوراکی ۲ + فلج اطفال تزریقی (IPV)",
        administrationMethodFa = "تزریق عضلانی و قطره خوراکی",
        careNotesFa = "نشانه‌های موضعی مثل تورم یا درد محل تزریق با کمپرس خنک تمیز کاهش می‌یابد؛ در تب بالای ۳۹ درجه یا بی‌حالی شدید با پزشک تماس بگیرید.",
      ),
      VaccineScheduleEntry(
        id = "6m_pentavalent_opv",
        titleFa = "واکسیناسیون ۶ ماهگی (پنج‌گانه نوبت ۳)",
        milestoneLabelFa = "۶ ماهگی",
        recommendedAgeDays = 180,
        vaccinesIncludedFa = "واکسن پنج‌گانه نوبت ۳ + فلج اطفال خوراکی نوبت ۳",
        administrationMethodFa = "تزریق عضلانی و قطره خوراکی",
        careNotesFa = "همزمان با ویزیت ۶ ماهگی، پایش وزن، قد و شروع تغذیه تکمیلی و قطره آهن/مولتی‌ویتامین را با پزشک یا مرکز بهداشت مرور کنید.",
      ),
      VaccineScheduleEntry(
        id = "12m_mmr1",
        titleFa = "واکسیناسیون ۱۲ ماهگی (MMR نوبت اول)",
        milestoneLabelFa = "۱۲ ماهگی (۱ سال)",
        recommendedAgeDays = 365,
        vaccinesIncludedFa = "واکسن سه‌گانه سرخک، سرخجه و اوریون (MMR) نوبت اول",
        administrationMethodFa = "تزریق زیرجلدی در مرکز بهداشت",
        careNotesFa = "گاهی ۵ تا ۱۲ روز پس از تزریق MMR تب خفیف یا دانه پوستی گذرا دیده می‌شود؛ در صورت حساسیت شدید غذایی یا بیماری حاد به کادر درمان اطلاع دهید.",
      ),
      VaccineScheduleEntry(
        id = "18m_mmr2_dtp_opv",
        titleFa = "واکسیناسیون ۱۸ ماهگی (یادآور اول)",
        milestoneLabelFa = "۱۸ ماهگی",
        recommendedAgeDays = 545,
        vaccinesIncludedFa = "یادآور سه‌گانه (دیفتری، کزاز، سیاه‌سرفه) + MMR نوبت دوم + فلج اطفال خوراکی یادآور",
        administrationMethodFa = "تزریقی و قطره خوراکی",
        careNotesFa = "پیش از مراجعه، کودک را تغذیه سبک بدهید و کارت واکسیناسیون را همراه داشته باشید.",
      ),
      VaccineScheduleEntry(
        id = "6y_dtp_opv",
        titleFa = "واکسیناسیون ۶ سالگی (پیش از دبستان)",
        milestoneLabelFa = "۶ سالگی",
        recommendedAgeDays = 2190,
        vaccinesIncludedFa = "یادآور سه‌گانه (ثلاث) + فلج اطفال خوراکی پیش از ورود به مدرسه",
        administrationMethodFa = "تزریقی و قطره خوراکی در مرکز بهداشت",
        careNotesFa = "گواهی تکمیل واکسیناسیون ۶ سالگی معمولاً برای ثبت‌نام کلاس اول دبستان ضروری است.",
      ),
      VaccineScheduleEntry(
        id = "seasonal_influenza",
        titleFa = "واکسن سالانه آنفلوانزا (۶ ماهگی به بالا)",
        milestoneLabelFa = "سالانه / اختیاری",
        recommendedAgeDays = 210,
        vaccinesIncludedFa = "واکسن فصلی آنفلوانزا (تزریق با صلاحدید پزشک اطفال، به‌ویژه در فصل پاییز)",
        administrationMethodFa = "تزریق عضلانی (در نخستین سال زیر ۹ سال ممکن است ۲ دوز با فاصله ۱ ماه نیاز باشد)",
        careNotesFa = "نوع و دوز مناسب سن را مستقیماً با پزشک کودک هماهنگ کنید.",
        isOptionalSeasonal = true,
      ),
    )

  const val SCIENTIFIC_REFERENCE_NOTE_FA: String =
    "برنامه مرجع بر اساس جدول ایمن‌سازی کشوری و توصیه‌های CDC/WHO تنظیم شده است. تاریخ دقیق مراجعات بعدی همیشه باید با کارت واکسن کودک و دستور پزشک یا مرکز بهداشت محل زندگی تطبیق داده شود."
}

object VaccinationScheduleEngine {

  val LEAD_DAY_OPTIONS = listOf(1, 3, 7)

  fun startOfDayEpochMs(
    epochMs: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Long {
    val cal = Calendar.getInstance(timeZone).apply {
      timeInMillis = epochMs
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  fun addDaysToDay(
    startDayMs: Long,
    days: Int,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Long {
    val cal = Calendar.getInstance(timeZone).apply {
      timeInMillis = startDayMs
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      add(Calendar.DAY_OF_YEAR, days)
    }
    return cal.timeInMillis
  }

  fun diffDays(
    fromDayMs: Long,
    toDayMs: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Int {
    val from = startOfDayEpochMs(fromDayMs, timeZone)
    val to = startOfDayEpochMs(toDayMs, timeZone)
    val offsetDiff = timeZone.getOffset(to) - timeZone.getOffset(from)
    return ((to - from + offsetDiff) / TimeUnit.DAYS.toMillis(1)).toInt()
  }

  fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days =
      355666 +
        (365 * gy) +
        ((gy2 + 3) / 4) -
        ((gy2 + 99) / 100) +
        ((gy2 + 399) / 400) +
        gd +
        gdm[gm - 1]
    var jy = -1595 + (33 * (days / 12053))
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
      jy += (days - 1) / 365
      days = (days - 1) % 365
    }
    val jm = if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
    val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
    return Triple(jy, jm, jd)
  }

  fun jalaliToGregorian(jyInput: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
    val jy = jyInput + 1595
    var days =
      -355668 +
        (365 * jy) +
        ((jy / 33) * 8) +
        (((jy % 33) + 3) / 4) +
        jd +
        (if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186)
    var gy = 400 * (days / 146097)
    days %= 146097
    if (days > 36524) {
      days--
      gy += 100 * (days / 36524)
      days %= 36524
      if (days >= 365) days++
    }
    gy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
      gy += (days - 1) / 365
      days = (days - 1) % 365
    }
    var gd = days + 1
    val isLeap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
    val salA = intArrayOf(0, 31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var gm = 0
    while (gm < 13 && gd > salA[gm]) {
      gd -= salA[gm]
      gm++
    }
    return Triple(gy, gm, gd)
  }

  fun toPersianDigits(input: String): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val sb = StringBuilder(input.length)
    for (ch in input) {
      if (ch in '0'..'9') sb.append(persianDigits[ch - '0'])
      else sb.append(ch)
    }
    return sb.toString()
  }

  fun toAsciiDigits(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
      when (ch) {
        in '۰'..'۹' -> sb.append(('0'.code + (ch - '۰')).toChar())
        in '٠'..'٩' -> sb.append(('0'.code + (ch - '٠')).toChar())
        else -> sb.append(ch)
      }
    }
    return sb.toString()
  }

  fun formatPersianDate(
    epochMs: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
    val gy = cal.get(Calendar.YEAR)
    val gm = cal.get(Calendar.MONTH) + 1
    val gd = cal.get(Calendar.DAY_OF_MONTH)
    val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
    val raw = "%04d/%02d/%02d".format(jy, jm, jd)
    return toPersianDigits(raw)
  }

  fun formatGregorianDateAscii(
    epochMs: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): String {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
    val gy = cal.get(Calendar.YEAR)
    val gm = cal.get(Calendar.MONTH) + 1
    val gd = cal.get(Calendar.DAY_OF_MONTH)
    return "%04d/%02d/%02d".format(gy, gm, gd)
  }

  /**
   * Parses user-entered date strings in either Shamsi/Jalali (e.g. "1404/02/15" or "۱۴۰۴-۲-۱۵")
   * or Gregorian (e.g. "2025/05/05") into local start-of-day epoch millis.
   */
  fun parseFlexibleDateToEpochMs(
    rawInput: String,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): Long? {
    val normalized = toAsciiDigits(rawInput.trim()).replace('-', '/').replace('.', '/')
    val parts = normalized.split('/').mapNotNull { it.trim().toIntOrNull() }
    if (parts.size != 3) return null
    val (year, month, day) = parts
    if (month !in 1..12 || day !in 1..31) return null

    val (gy, gm, gd) =
      when {
        year in 1350..1500 -> {
          val maxDay = if (month <= 6) 31 else 30
          if (day > maxDay) return null
          val converted = jalaliToGregorian(year, month, day)
          if (gregorianToJalali(converted.first, converted.second, converted.third) != Triple(year, month, day)) {
            return null
          }
          converted
        }
        year in 1950..2100 -> Triple(year, month, day)
        else -> return null
      }

    val cal = Calendar.getInstance(timeZone).apply {
      isLenient = false
      clear()
      set(gy, gm - 1, gd, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return runCatching { cal.timeInMillis }.getOrNull()
  }

  fun mergeRecords(
    cloudRecords: List<VaccineRecord>,
    localRecords: List<VaccineRecord>,
  ): Map<String, VaccineRecord> {
    val merged = linkedMapOf<String, VaccineRecord>()
    (localRecords + cloudRecords).forEach { record ->
      val existing = merged[record.vaccineId]
      if (existing == null || record.updatedAtMs >= existing.updatedAtMs || (record.isSyncedCloud && !existing.isSyncedCloud && record.updatedAtMs == existing.updatedAtMs)) {
        merged[record.vaccineId] = record
      }
    }
    return merged
  }

  fun resolveSchedule(
    catalog: List<VaccineScheduleEntry> = VaccinationScheduleCatalog.scheduleEntries,
    cloudRecords: List<VaccineRecord> = emptyList(),
    localRecords: List<VaccineRecord> = emptyList(),
    birthDateEpochMs: Long? = null,
    reminderSettings: VaccineReminderSettings = VaccineReminderSettings(),
    nowEpochMs: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): VaccineDashboardSummary {
    val recordMap = mergeRecords(cloudRecords, localRecords)
    val todayStart = startOfDayEpochMs(nowEpochMs, timeZone)
    val leadDays = reminderSettings.leadDaysBefore.coerceIn(1, 30)

    val items =
      catalog.map { entry ->
        val record = recordMap[entry.id]
        val computedDueMs =
          when {
            record?.customDueDateEpochMs != null -> startOfDayEpochMs(record.customDueDateEpochMs, timeZone)
            birthDateEpochMs != null -> addDaysToDay(startOfDayEpochMs(birthDateEpochMs, timeZone), entry.recommendedAgeDays, timeZone)
            else -> null
          }
        val dueSourceFa =
          when {
            record?.customDueDateEpochMs != null -> "تاریخ ثبت‌شده توسط والد"
            birthDateEpochMs != null -> "محاسبه‌شده بر اساس تاریخ تولد"
            else -> "در انتظار ثبت تاریخ تولد یا موعد"
          }
        val adminMs = record?.administeredDateEpochMs?.let { startOfDayEpochMs(it, timeZone) }
        val daysUntil = computedDueMs?.let { diffDays(todayStart, it, timeZone) }

        val status =
          when {
            adminMs != null -> VaccineStatus.ADMINISTERED
            record?.isDeferred == true -> VaccineStatus.DEFERRED
            daysUntil != null && daysUntil < 0 -> VaccineStatus.OVERDUE
            daysUntil != null && daysUntil <= leadDays -> VaccineStatus.DUE_SOON
            else -> VaccineStatus.UPCOMING
          }

        VaccineResolvedItem(
          definition = entry,
          record = record,
          effectiveDueDateEpochMs = computedDueMs,
          administeredDateEpochMs = adminMs,
          daysUntilDue = daysUntil,
          status = status,
          dueDateSourceFa = dueSourceFa,
        )
      }

    val coreItems = items.filterNot { it.definition.isOptionalSeasonal }
    val administeredCore = coreItems.count { it.status == VaccineStatus.ADMINISTERED }
    val dueSoonCount = items.count { it.status == VaccineStatus.DUE_SOON }
    val overdueCount = items.count { it.status == VaccineStatus.OVERDUE }
    val nextUpcoming =
      items
        .filter { it.status == VaccineStatus.DUE_SOON || it.status == VaccineStatus.UPCOMING || it.status == VaccineStatus.OVERDUE }
        .sortedWith(compareBy<VaccineResolvedItem> { it.effectiveDueDateEpochMs ?: Long.MAX_VALUE }.thenBy { it.definition.recommendedAgeDays })
        .firstOrNull()

    val dueForReminder =
      if (!reminderSettings.enabled) emptyList()
      else {
        items.filter { item ->
          item.effectiveDueDateEpochMs != null &&
            item.daysUntilDue != null &&
            item.daysUntilDue <= leadDays &&
            (item.status == VaccineStatus.DUE_SOON || item.status == VaccineStatus.OVERDUE)
        }
      }

    return VaccineDashboardSummary(
      items = items,
      totalCoreCount = coreItems.size,
      administeredCoreCount = administeredCore,
      dueSoonCount = dueSoonCount,
      overdueCount = overdueCount,
      nextUpcoming = nextUpcoming,
      dueForReminder = dueForReminder,
    )
  }

  fun filterItems(
    items: List<VaccineResolvedItem>,
    filter: VaccineFilter,
  ): List<VaccineResolvedItem> =
    when (filter) {
      VaccineFilter.ALL -> items
      VaccineFilter.ACTIONABLE -> items.filter { it.status == VaccineStatus.DUE_SOON || it.status == VaccineStatus.UPCOMING }
      VaccineFilter.OVERDUE -> items.filter { it.status == VaccineStatus.OVERDUE }
      VaccineFilter.ADMINISTERED -> items.filter { it.status == VaccineStatus.ADMINISTERED }
    }

  fun reminderKeyFor(item: VaccineResolvedItem, timeZone: TimeZone = TimeZone.getDefault()): String? {
    val dueMs = item.effectiveDueDateEpochMs ?: return null
    val dayBucket = startOfDayEpochMs(dueMs, timeZone)
    return "${item.definition.id}_$dayBucket"
  }

  fun findUnnotifiedReminders(
    summary: VaccineDashboardSummary,
    settings: VaccineReminderSettings,
    alreadyNotifiedKeys: Set<String>,
    timeZone: TimeZone = TimeZone.getDefault(),
  ): List<VaccineResolvedItem> {
    if (!settings.enabled) return emptyList()
    return summary.dueForReminder.filter { item ->
      val key = reminderKeyFor(item, timeZone) ?: return@filter false
      key !in alreadyNotifiedKeys
    }
  }

  fun formatRelativeDueLabelFa(item: VaccineResolvedItem): String {
    if (item.status == VaccineStatus.ADMINISTERED && item.administeredDateEpochMs != null) {
      return "تزریق‌شده در ${formatPersianDate(item.administeredDateEpochMs)}"
    }
    if (item.status == VaccineStatus.DEFERRED) {
      return "به تعویق افتاده با نظر پزشک"
    }
    val d = item.daysUntilDue ?: return "تاریخ موعد ثبت نشده"
    return when {
      d == 0 -> "موعد تزریق: امروز"
      d == 1 -> "فردا (۱ روز مانده)"
      d > 1 -> "${toPersianDigits(d.toString())} روز تا موعد"
      d == -1 -> "۱ روز از موعد گذشته"
      else -> "${toPersianDigits((-d).toString())} روز از موعد گذشته"
    }
  }

  fun buildReminderNotificationCopy(
    items: List<VaccineResolvedItem>,
    childAliasFa: String? = null,
    isPreview: Boolean = false,
  ): Pair<String, String> {
    val target = items.firstOrNull()
    val childLabel = childAliasFa?.takeIf { it.isNotBlank() } ?: "کودک"
    if (target == null) {
      return "یادآور برنامه واکسیناسیون مانو" to
        "اعلان‌های یادآوری پیش از موعد واکسیناسیون برای $childLabel فعال است."
    }
    val dueDateFa = target.effectiveDueDateEpochMs?.let { formatPersianDate(it) } ?: "موعد ثبت‌شده"
    val relativeFa = formatRelativeDueLabelFa(target)
    val prefix = if (isPreview) "نمونه اعلان یادآوری: " else "یادآوری واکسیناسیون "
    val title = "$prefix${target.definition.milestoneLabelFa} ($childLabel)"
    val extraCount = items.size - 1
    val extraText = if (extraCount > 0) " (+ ${toPersianDigits(extraCount.toString())} واکسن دیگر در نوبت)" else ""
    val body =
      "${target.definition.titleFa} • $relativeFa (تاریخ: $dueDateFa).$extraText همراه داشتن کارت واکسن فراموش نشود."
    return title to body
  }
}
