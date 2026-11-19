package com.example.domain.util

import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.SymptomFlow
import com.example.ui.viewmodel.SymptomWizardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PreparedVisitPromptBundle(
  val defaultTitleFa: String,
  val symptomContextFa: String,
  val timelineSummaryFa: String,
  val parentNoteFa: String,
  val promptText: String,
)

object DoctorVisitPromptBuilder {

  fun buildContext(
    snapshot: FeverDoseTimelineSnapshot,
    childProfile: ChildProfileDemo?,
    selectedSymptomFlow: SymptomFlow?,
    symptomWizard: SymptomWizardState,
    parentNoteFa: String,
    nowMs: Long = System.currentTimeMillis(),
  ): PreparedVisitPromptBundle {
    val cleanNote = parentNoteFa.trim().take(500)
    val childName = childProfile?.aliasFa?.takeIf { it.isNotBlank() } ?: "کودک"
    val ageLabel =
      childProfile?.birthDateOrAgeFa?.takeIf { it.isNotBlank() }
        ?: symptomWizard.selectedAgeBand?.labelFa
        ?: "سن ثبت‌نشده"
    val weightLabel =
      childProfile?.weightKgText?.takeIf { it.isNotBlank() }?.let { "$it کیلوگرم" } ?: "ثبت‌نشده"

    val symptomTitle = selectedSymptomFlow?.titleFa ?: "پایش تب و علائم"
    val durationLabel =
      symptomWizard.durationHoursText.trim().takeIf { it.isNotEmpty() }?.let { "مدت حدود $it ساعت" }
    val wizardTempLabel =
      symptomWizard.temperatureText.trim().takeIf { it.isNotEmpty() }?.let { "دمای واردشده در هدایتگر: $it°C" }

    val matchedRedFlags =
      selectedSymptomFlow?.redFlags
        ?.filter { it.id in symptomWizard.selectedRedFlagIds }
        ?.map { it.labelFa }
        .orEmpty()

    val symptomContextParts = buildList {
      add("علامت کانونی: $symptomTitle")
      add("سن: $ageLabel")
      if (durationLabel != null) add(durationLabel)
      if (wizardTempLabel != null) add(wizardTempLabel)
      if (matchedRedFlags.isNotEmpty()) {
        add("موارد هشدار انتخاب‌شده: ${matchedRedFlags.joinToString("، ")}")
      }
      symptomWizard.evaluation?.let { eval ->
        add("نتیجه غربالگری: ${eval.outcome.labelFa} (${eval.titleFa})")
      }
    }
    val symptomContextFa = symptomContextParts.joinToString(" • ").take(300)

    val highFeverAlert = FeverDoseTimelineEngine.evaluateHighFeverAlert(snapshot, childProfile)
    val latestStr = snapshot.latestTempCelsius?.let { FeverDoseTimelineEngine.formatTempC(it) } ?: "ثبت‌نشده"
    val peakStr = snapshot.peakTemp24h?.let { FeverDoseTimelineEngine.formatTempC(it) } ?: "ثبت‌نشده"

    val timelineSummaryFa =
      buildString {
        append("${snapshot.tempPoints.size} ثبت دما (آخرین: $latestStr • اوج ۲۴س: $peakStr)")
        append(" • ${snapshot.dosePoints.size} نوبت دارو")
        if (highFeverAlert != null) {
          append(" • ⚠️ هشدار تب بالای ۳۹ درجه")
        }
      }.take(800)

    val timeTag = SimpleDateFormat("HH:mm", Locale.US).format(Date(nowMs))
    val defaultTitleFa =
      if (highFeverAlert != null) {
        "پیش‌نویس فوری ویزیت ($childName — تب ${peakStr}) • $timeTag"
      } else {
        "پیش‌نویس ویزیت ($childName — $symptomTitle) • $timeTag"
      }.take(120)

    val tempTimelineLines =
      if (snapshot.tempPoints.isEmpty()) {
        "• هیچ دمایی در ۲۴ ساعت گذشته در خط زمانی ثبت نشده است."
      } else {
        snapshot.tempPoints.sortedBy { it.timestampMs }.joinToString("\n") { pt ->
          val note = pt.noteFa.takeIf { it.isNotBlank() }?.let { " | توضیح: $it" } ?: ""
          "• ساعت ${pt.timeLabelFa}: ${FeverDoseTimelineEngine.formatTempC(pt.tempCelsius)} (محل: ${pt.siteFa}، ثبت‌کننده: ${pt.caregiverFa}$note)"
        }
      }

    val doseTimelineLines =
      if (snapshot.dosePoints.isEmpty()) {
        "• هیچ نوبت دارویی در ۲۴ ساعت گذشته ثبت نشده است."
      } else {
        snapshot.dosePoints.sortedBy { it.timestampMs }.joinToString("\n") { dose ->
          val note = dose.noteFa.takeIf { it.isNotBlank() }?.let { " | توضیح: $it" } ?: ""
          "• ساعت ${dose.givenAtLabelFa}: ${dose.medicationNameFa} (${dose.dosageFormFa}) — مقدار داده‌شده: ${dose.volumeMlText}$note"
        }
      }

    val promptText =
      """
      شما دستیار دقیق و بی‌طرف «تنظیم خلاصه پیش از ویزیت پزشک اطفال» برای والدین در اپلیکیشن مانو کید هستید.
      بر اساس داده‌های واقعی ثبت‌شده در خط زمانی ۲۴ ساعته و وضعیت علائم کودک زیر، یک پیش‌نویس متنی منظم، محترمانه، واضح و آماده ارائه به پزشک به زبان فارسی بنویسید.

      قوانین ایمنی و کیفیت خروجی:
      1. هیچ تشخیص قطعی پزشکی ندهید و هیچ دوز دارویی جدیدی تجویز یا محاسبه نکنید.
      2. فقط از داده‌های واقعی زیر استفاده کنید و اگر بخشی ثبت نشده بود، صادقانه بنویسید «ثبت نشده».
      3. متن را در ۵ بخش کوتاه و خوانا با تیترهای مشخص تنظیم کنید تا پزشک در ۳۰ ثانیه بتواند مرور کند:
         - «خلاصه مشخصات و علت مراجعه»
         - «روند ۲۴ ساعته تب و دماهای ثبت‌شده»
         - «داروهای مصرف‌شده در ۲۴ ساعت گذشته»
         - «علائم بررسی‌شده و موارد نیازمند توجه»
         - «پرسش‌های والد برای پزشک در مطب»
      4. اگر دمای بالای ۳۹.۰ درجه سانتی‌گراد یا علامت هشدار وجود دارد، در ابتدای خلاصه با برجستگی یادآوری کنید.
      5. لحن کوتاه، بالینی و قابل چاپ/ارسال باشد (حداکثر حدود ۳۵۰ کلمه).

      --- مشخصات کودک ---
      - نام/شناسه: $childName
      - سن / گروه سنی: $ageLabel
      - وزن ثبت‌شده: $weightLabel
      - حساسیت‌های شناخته‌شده: ${childProfile?.allergiesFa?.ifBlank { "ثبت‌نشده" } ?: "ثبت‌نشده"}
      - داروهای مصرفی هم‌زمان: ${childProfile?.currentMedsFa?.ifBlank { "ثبت‌نشده" } ?: "ثبت‌نشده"}

      --- وضعیت خط زمانی ۲۴ ساعته تب و دارو ---
      - آخرین دمای ثبت‌شده: $latestStr
      - بالاترین دمای ۲۴ ساعت: $peakStr
      - وضعیت هشدار تب > ۳۹°C: ${if (highFeverAlert != null) "بله (${FeverDoseTimelineEngine.formatTempC(highFeverAlert.triggerTempCelsius)} در ساعت ${highFeverAlert.timeLabelFa})" else "خیر"}
      - وضعیت استامینوفن: ${snapshot.acetaminophenStatus.dosesLast24h} نوبت در ۲۴س (${snapshot.acetaminophenStatus.statusMessageFa})
      - وضعیت ایبوپروفن: ${snapshot.ibuprofenStatus.dosesLast24h} نوبت در ۲۴س (${snapshot.ibuprofenStatus.statusMessageFa})
      - فهرست زمانی دماها:
      $tempTimelineLines
      - فهرست زمانی داروها:
      $doseTimelineLines

      --- داده‌های هدایتگر علائم کودک ---
      - علامت انتخاب‌شده: $symptomTitle
      - مدت زمان علامت: ${durationLabel ?: "ثبت‌نشده"}
      - دمای واردشده در فرم علائم: ${wizardTempLabel ?: "ثبت‌نشده"}
      - علائم هشدار انتخاب‌شده توسط والد: ${if (matchedRedFlags.isEmpty()) "موردی علامت زده نشده" else matchedRedFlags.joinToString("، ")}
      - نتیجه هدایتگر علائم: ${symptomWizard.evaluation?.let { "${it.outcome.labelFa}: ${it.summaryFa}" } ?: "تکمیل نشده"}

      --- یادداشت یا پرسش اضافه والد ---
      ${cleanNote.ifBlank { "مورد اضافه‌ای نوشته نشده است." }}
      """.trimIndent()

    return PreparedVisitPromptBundle(
      defaultTitleFa = defaultTitleFa,
      symptomContextFa = symptomContextFa,
      timelineSummaryFa = timelineSummaryFa,
      parentNoteFa = cleanNote,
      promptText = promptText,
    )
  }
}
