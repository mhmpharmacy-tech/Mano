package com.example.domain.util

import com.example.domain.model.NutritionPlanResult
import com.example.domain.model.VaccineDefinition
import com.example.domain.model.VaccineRecord
import com.example.domain.model.VaccineReminderSettings
import com.example.domain.model.VaccineScheduleItem
import com.example.domain.model.VaccineScheduleSummary
import com.example.domain.model.VaccineStatus
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

object VaccinationScheduleEngine {
  private const val DAY_MS = 24L * 3600L * 1000L

  val coreDefinitions: List<VaccineDefinition> = listOf(
    VaccineDefinition(
      id = "vac_birth_bcg_hbv_opv",
      titleFa = "واکسن بدو تولد (BCG، هپاتیت B، فلج اطفال خوراکی)",
      milestoneLabelFa = "بدو تولد",
      dueAgeDays = 0,
      preventsFa = "سل، هپاتیت B و فلج اطفال",
      careNotesFa = "معمولاً در بیمارستان یا زایشگاه تزریق می‌شود؛ تاریخ دقیق را ثبت کنید.",
    ),
    VaccineDefinition(
      id = "vac_2m_penta_opv",
      titleFa = "واکسن ۲ ماهگی (پنج‌گانه واکسن + فلج اطفال + روتاویروس)",
      milestoneLabelFa = "۲ ماهگی",
      dueAgeDays = 60,
      preventsFa = "دیفتری، کزاز، سیاه‌سرفه، هپاتیت B، هموفیلوس آنفلوآنزا و فلج اطفال",
      careNotesFa = "پس از واکسن ممکن است تب خفیف یا بی‌قراری ۲۴ تا ۴۸ ساعته رخ دهد.",
    ),
    VaccineDefinition(
      id = "vac_4m_penta_opv_ipv",
      titleFa = "واکسن ۴ ماهگی (نوبت دوم پنج‌گانه + فلج اطفال تزریقی و خوراکی)",
      milestoneLabelFa = "۴ ماهگی",
      dueAgeDays = 120,
      preventsFa = "تکمیل ایمنی پنج‌گانه و فلج اطفال",
      careNotesFa = "از کمپرس خنک تمیز روی محل تزریق در صورت تورم استفاده کنید.",
    ),
    VaccineDefinition(
      id = "vac_6m_penta_opv",
      titleFa = "واکسن ۶ ماهگی (نوبت سوم پنج‌گانه + فلج اطفال)",
      milestoneLabelFa = "۶ ماهگی",
      dueAgeDays = 180,
      preventsFa = "ایمنی کامل سه‌نوبته نوزادی",
      careNotesFa = "همزمان مراقبت تغذیه تکمیلی و قطره آهن طبق نظر مرکز بهداشت آغاز می‌شود.",
    ),
    VaccineDefinition(
      id = "vac_12m_mmr",
      titleFa = "واکسن ۱۲ ماهگی (MMR نوبت اول)",
      milestoneLabelFa = "۱۲ ماهگی",
      dueAgeDays = 365,
      preventsFa = "سرخک، سرخجه و اوریون",
      careNotesFa = "تب خفیف ۵ تا ۱۲ روز بعد از تزریق MMR طبیعی است؛ در تب بالا با پزشک مشورت کنید.",
    ),
    VaccineDefinition(
      id = "vac_18m_dtp_opv_mmr",
      titleFa = "واکسن ۱۸ ماهگی (سه‌گانه یادآور + فلج اطفال + MMR نوبت دوم)",
      milestoneLabelFa = "۱۸ ماهگی",
      dueAgeDays = 545,
      preventsFa = "یادآور دیفتری، کزاز، سیاه‌سرفه، سرخک و اوریون",
      careNotesFa = "سابقه تب یا واکنش نوبت‌های قبلی را پیش از تزریق به بهورز یا پزشک اطلاع دهید.",
    ),
    VaccineDefinition(
      id = "vac_6y_dtp_opv",
      titleFa = "واکسن ۶ سالگی (یادآور بدو ورود به مدرسه)",
      milestoneLabelFa = "۶ سالگی",
      dueAgeDays = 2190,
      preventsFa = "دیفتری، کزاز، سیاه‌سرفه و فلج اطفال",
      careNotesFa = "برای ثبت‌نام مدرسه کارت واکسن کامل الزامی است.",
    ),
  )

  fun parseFlexibleDateToEpochMs(raw: String): Long? {
    val ascii = PersianNormalizer.toAsciiDigits(raw).trim()
    if (ascii.isEmpty()) return null
    val normalized = ascii.replace('-', '/').replace('.', '/')
    val match = Regex("^(\\d{4})/(\\d{1,2})/(\\d{1,2})$").find(normalized) ?: return null
    val year = match.groupValues[1].toIntOrNull() ?: return null
    val month = match.groupValues[2].toIntOrNull() ?: return null
    val day = match.groupValues[3].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null

    return if (year in 1300..1500) {
      jalaliToEpochMs(year, month, day)
    } else if (year in 1990..2100) {
      GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        isLenient = false
        set(year, month - 1, day, 9, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }.runCatching { timeInMillis }.getOrNull()
    } else {
      null
    }
  }

  private fun jalaliToEpochMs(jy: Int, jm: Int, jd: Int): Long? {
    var jyAdj = jy - 979
    val jmAdj = jm - 1
    val jdAdj = jd - 1
    var jDayNo = 365 * jyAdj + (jyAdj / 33) * 8 + ((jyAdj % 33 + 3) / 4)
    val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
    for (i in 0 until jmAdj) jDayNo += jDaysInMonth[i]
    jDayNo += jdAdj

    var gDayNo = jDayNo + 79
    var gy = 1600 + 400 * (gDayNo / 146097)
    gDayNo %= 146097
    var leap = true
    if (gDayNo >= 36525) {
      gDayNo--
      gy += 100 * (gDayNo / 36524)
      gDayNo %= 36524
      if (gDayNo >= 365) gDayNo++ else leap = false
    }
    gy += 4 * (gDayNo / 1461)
    gDayNo %= 1461
    if (gDayNo >= 366) {
      leap = false
      gDayNo--
      gy += gDayNo / 365
      gDayNo %= 365
    }
    val gDaysInMonth = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var gm = 0
    while (gm < 12 && gDayNo >= gDaysInMonth[gm]) {
      gDayNo -= gDaysInMonth[gm]
      gm++
    }
    val gd = gDayNo + 1
    return GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
      set(gy, gm, gd, 9, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
  }

  fun resolveSchedule(
    cloudRecords: List<VaccineRecord> = emptyList(),
    localRecords: List<VaccineRecord> = emptyList(),
    birthDateEpochMs: Long?,
    reminderSettings: VaccineReminderSettings,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): VaccineScheduleSummary {
    val recordById = (localRecords + cloudRecords).associateBy { it.vaccineId }
    val items = coreDefinitions.map { def ->
      val rec = recordById[def.id]
      val dueMs = rec?.customDueDateEpochMs ?: birthDateEpochMs?.let { it + def.dueAgeDays * DAY_MS }
      val daysRem = dueMs?.let { ((it - nowEpochMs) / DAY_MS).toInt() }
      val status = when {
        rec?.administeredDateEpochMs != null -> VaccineStatus.ADMINISTERED
        rec?.isDeferred == true -> VaccineStatus.DEFERRED
        dueMs == null -> VaccineStatus.NEEDS_BIRTHDATE
        daysRem != null && daysRem < 0 -> VaccineStatus.OVERDUE
        daysRem != null && daysRem <= reminderSettings.leadDaysBefore.coerceAtLeast(1) -> VaccineStatus.DUE_SOON
        else -> VaccineStatus.UPCOMING
      }
      VaccineScheduleItem(
        definition = def,
        status = status,
        dueDateEpochMs = dueMs,
        record = rec,
        daysRemaining = daysRem,
      )
    }

    val coreItems = items.filter { it.definition.isCoreNational }
    val adminCore = coreItems.count { it.status == VaccineStatus.ADMINISTERED }
    val overdue = items.count { it.status == VaccineStatus.OVERDUE }
    val dueSoon = items.count { it.status == VaccineStatus.DUE_SOON }
    val nextUpcoming = items.firstOrNull {
      it.status in setOf(VaccineStatus.OVERDUE, VaccineStatus.DUE_SOON, VaccineStatus.UPCOMING)
    }
    val dueForReminder = items.filter {
      it.status == VaccineStatus.DUE_SOON || (reminderSettings.notifyOverdue && it.status == VaccineStatus.OVERDUE)
    }
    return VaccineScheduleSummary(
      items = items,
      dueForReminder = dueForReminder,
      nextUpcoming = nextUpcoming,
      administeredCoreCount = adminCore,
      totalCoreCount = coreItems.size,
      overdueCount = overdue,
      dueSoonCount = dueSoon,
    )
  }

  fun reminderKeyFor(item: VaccineScheduleItem): String? {
    val due = item.dueDateEpochMs ?: return null
    return "${item.definition.id}_${due}_${item.status.name}"
  }

  fun findUnnotifiedReminders(
    summary: VaccineScheduleSummary,
    settings: VaccineReminderSettings,
    alreadyNotifiedKeys: Set<String>,
  ): List<VaccineScheduleItem> {
    if (!settings.enabled) return emptyList()
    return summary.dueForReminder.filter { item ->
      val key = reminderKeyFor(item)
      key != null && key !in alreadyNotifiedKeys
    }
  }

  fun formatRelativeDueLabelFa(item: VaccineScheduleItem): String {
    val days = item.daysRemaining ?: return item.status.labelFa
    return when (item.status) {
      VaccineStatus.ADMINISTERED -> "تزریق و ثبت شده"
      VaccineStatus.DEFERRED -> "معوق / نیازمند هماهنگی"
      VaccineStatus.NEEDS_BIRTHDATE -> "تاریخ تولد ثبت نشده"
      VaccineStatus.OVERDUE -> "${PersianNormalizer.toPersianDigits((-days).toString())} روز گذشته از موعد"
      VaccineStatus.DUE_SOON ->
        if (days == 0) "موعد امروز"
        else "${PersianNormalizer.toPersianDigits(days.toString())} روز تا موعد"
      VaccineStatus.UPCOMING -> "${PersianNormalizer.toPersianDigits(days.toString())} روز آینده"
    }
  }
}

object NutritionPromptBuilder {
  fun formatShareableNutritionTextFa(plan: NutritionPlanResult): String =
    buildString {
      appendLine(plan.planTitleFa)
      if (plan.childContextSummaryFa.isNotBlank()) appendLine(plan.childContextSummaryFa)
      if (plan.overviewFa.isNotBlank()) appendLine("\n${plan.overviewFa}")
      if (plan.meals.isNotEmpty()) {
        appendLine("\nوعده‌های پیشنهادی:")
        plan.meals.forEachIndexed { idx, meal ->
          appendLine("${PersianNormalizer.toPersianDigits((idx + 1).toString())}. [${meal.mealTypeFa}] ${meal.titleFa}")
          if (meal.whySuitableFa.isNotBlank()) appendLine("   علت تناسب: ${meal.whySuitableFa}")
          if (meal.ingredientsFa.isNotEmpty()) appendLine("   مواد لازم: ${meal.ingredientsFa.joinToString("، ")}")
        }
      }
      if (plan.hydrationTipFa.isNotBlank()) appendLine("\nآبرسانی: ${plan.hydrationTipFa}")
      if (plan.safetyAvoidListFa.isNotEmpty()) {
        appendLine("\nموارد پرهیز: ${plan.safetyAvoidListFa.joinToString("، ")}")
      }
      appendLine("\nاین برنامه آموزشی است و جایگزین توصیه پزشک یا متخصص تغذیه نیست.")
    }
}
