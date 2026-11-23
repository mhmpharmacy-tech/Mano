package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
enum class TreatmentDosageForm(
  val labelFa: String,
  val defaultUnitHintFa: String,
) {
  SYRUP_SUSPENSION("شربت / سوسپانسیون", "سی‌سی (با سرنگ مدرج)"),
  DROPS("قطره", "قطره (با قطره‌چکان استاندارد)"),
  NASAL_SPRAY("اسپری بینی", "پاف در هر سوراخ بینی"),
  OINTMENT_CREAM("پماد / کرم", "لایه نازک موضعی"),
  TABLET_SACHET("قرص / ساشه", "عدد یا بسته"),
}

@Serializable
data class TreatmentMedicationItem(
  val id: String,
  val nameWithStrengthFa: String,
  val form: TreatmentDosageForm = TreatmentDosageForm.SYRUP_SUSPENSION,
  val doseAmountFa: String,
  val intervalHours: Int = 8,
  val scheduleLabelFa: String,
  val foodRelationFa: String,
  val counselingNoteFa: String,
  val isWarningHighlight: Boolean = true,
  val dailyDoseSlots: Int = 1,
  val completedSlotTimesFa: Map<Int, String> = emptyMap(),
)

@Serializable
data class PediatricTreatmentPlan(
  val pharmacyNameFa: String = "داروخانه شبانه‌روزی دکتر رضایی",
  val pharmacistBadgeFa: String = "تأیید دکتر داروساز (نظام: ۱۲۳۴۵)",
  val fallbackChildSummaryFa: String = "آراد عزیز • ۴ ساله (۱۶ کیلوگرم)",
  val prescriptionDateFa: String = "نسخه فعال • قابل ویرایش بر اساس دستور پزشک",
  val storageInstructionFa: String =
    "تمام داروهای این نسخه در دمای محیط (زیر ۲۵ درجه سانتی‌گراد)، دور از نور مستقیم و دسترس کودکان نگهداری شوند (نیازی به یخچال نیست؛ سوسپانسیون آماده‌شده حداکثر تا ۱۰ روز قابل مصرف است).",
  val smartRemindersEnabled: Boolean = false,
  val lastCheckoffDayKey: String = "",
  val medications: List<TreatmentMedicationItem> = defaultPediatricTreatmentMedications(),
) {
  val totalDailySlots: Int
    get() = medications.sumOf { it.dailyDoseSlots.coerceIn(1, 6) }

  val completedTodaySlots: Int
    get() = medications.sumOf { med ->
      val maxSlots = med.dailyDoseSlots.coerceIn(1, 6)
      med.completedSlotTimesFa.keys.count { it in 1..maxSlots }
    }
}

fun defaultPediatricTreatmentMedications(): List<TreatmentMedicationItem> =
  listOf(
    TreatmentMedicationItem(
      id = "rx_azithromycin_200",
      nameWithStrengthFa = "سوسپانسیون آزیترومایسین ۲۰۰ میلی‌گرم/۵ سی‌سی",
      form = TreatmentDosageForm.SYRUP_SUSPENSION,
      doseAmountFa = "روز اول ۴ سی‌سی، روز ۲ تا ۵ مقدار ۲ سی‌سی (با سرنگ)",
      intervalHours = 24,
      scheduleLabelFa = "هر ۲۴ ساعت (ساعت ۱۲ ظهر)",
      foodRelationFa = "همراه غذا یا پس از غذا",
      counselingNoteFa =
        "پودر را با آب جوشیده سردشده تا خط نشانه مخلوط کنید؛ پیش از هر مصرف خوب تکان دهید و دوره ۵ روزه را کامل نمایید (پایداری پس از آماده‌سازی: تا ۱۰ روز در دمای محیط).",
      isWarningHighlight = true,
      dailyDoseSlots = 1,
    ),
    TreatmentMedicationItem(
      id = "rx_acetaminophen_160",
      nameWithStrengthFa = "شربت استامینوفن کودکان ۱۶۰ میلی‌گرم/۵ سی‌سی",
      form = TreatmentDosageForm.SYRUP_SUSPENSION,
      doseAmountFa = "۷٫۵ سی‌سی (۲۴۰ میلی‌گرم با سرنگ مدرج)",
      intervalHours = 6,
      scheduleLabelFa = "هر ۶ ساعت در صورت تب",
      foodRelationFa = "فرقی ندارد (ناشتا یا با غذا)",
      counselingNoteFa =
        "برای وزن ۱۶ کیلوگرم شربت به‌جای ۵۰ قطره دقیق‌تر و ایمن‌تر است • حداکثر ۴ نوبت در شبانه‌روز و فاصله کمتر از ۴ تا ۶ ساعت نباشد.",
      isWarningHighlight = true,
      dailyDoseSlots = 4,
    ),
    TreatmentMedicationItem(
      id = "rx_saline_spray_09",
      nameWithStrengthFa = "اسپری بینی سدیم کلراید ۰٫۹٪ (سرم شست‌وشو)",
      form = TreatmentDosageForm.NASAL_SPRAY,
      doseAmountFa = "۱ پاف در هر سوراخ بینی",
      intervalHours = 8,
      scheduleLabelFa = "هر ۸ ساعت",
      foodRelationFa = "قبل از خواب و هنگام گرفتگی بینی",
      counselingNoteFa =
        "پس از هر بار استفاده، سری اسپری را با دستمال تمیز خشک کنید و به‌صورت اختصاصی برای یک کودک استفاده نمایید.",
      isWarningHighlight = false,
      dailyDoseSlots = 3,
    ),
  )

object TreatmentPlanFormatter {
  private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

  fun toPersianDigits(input: String): String =
    buildString(input.length) {
      input.forEach { ch ->
        if (ch in '0'..'9') append(PERSIAN_DIGITS[ch - '0']) else append(ch)
      }
    }

  fun currentDayKey(nowMs: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))

  fun currentTimeFa(nowMs: Long = System.currentTimeMillis()): String =
    toPersianDigits(SimpleDateFormat("HH:mm", Locale.US).format(Date(nowMs)))

  fun resolveChildDisplaySummary(plan: PediatricTreatmentPlan, profile: ChildProfileDemo?): String {
    if (profile == null) return plan.fallbackChildSummaryFa
    val alias = profile.aliasFa.trim().ifBlank { "کودک عزیز" }
    val agePart =
      profile.ageMonthsExact?.let { months ->
        if (months >= 24) "${toPersianDigits((months / 12).toString())} ساله"
        else "${toPersianDigits(months.toString())} ماهه"
      } ?: profile.ageBand.labelFa
    val weightPart =
      profile.weightKgText.trim().takeIf { it.isNotBlank() }?.let { " (${toPersianDigits(it)} کیلوگرم)" }.orEmpty()
    return "$alias • $agePart$weightPart"
  }

  fun resolveAllergySummary(profile: ChildProfileDemo?): String {
    val allergies = profile?.allergiesFa?.trim().orEmpty()
    return if (allergies.isNotBlank()) {
      "حساسیت ثبت‌شده: $allergies"
    } else {
      "حساسیت دارویی ثبت‌نشده — پیش از مصرف بررسی شود"
    }
  }

  fun normalizeForToday(plan: PediatricTreatmentPlan, todayKey: String = currentDayKey()): PediatricTreatmentPlan {
    if (plan.lastCheckoffDayKey == todayKey) return plan
    return plan.copy(
      lastCheckoffDayKey = todayKey,
      medications = plan.medications.map { it.copy(completedSlotTimesFa = emptyMap()) },
    )
  }

  fun buildCaregiverShareText(plan: PediatricTreatmentPlan, profile: ChildProfileDemo?): String =
    buildString {
      appendLine("برنامه درمانی اختصاصی کودک (مانو کید)")
      appendLine("${plan.pharmacyNameFa} | ${plan.pharmacistBadgeFa}")
      appendLine("کودک: ${resolveChildDisplaySummary(plan, profile)}")
      appendLine(resolveAllergySummary(profile))
      appendLine("────────────────────")
      appendLine("نکته ایمنی پیمانه: همیشه از سرنگ یا پیمانه مدرج استاندارد استفاده کنید و از قاشق خانگی بپرهیزید (۱ سی‌سی = ۲۰ قطره استاندارد).")
      appendLine()
      plan.medications.forEachIndexed { idx, med ->
        val numFa = toPersianDigits((idx + 1).toString())
        appendLine("$numFa) ${med.nameWithStrengthFa} (${med.form.labelFa})")
        appendLine("   • دوز: ${med.doseAmountFa}")
        appendLine("   • زمان‌بندی: ${med.scheduleLabelFa} | ${med.foodRelationFa}")
        if (med.counselingNoteFa.isNotBlank()) {
          appendLine("   • نکته داروساز: ${med.counselingNoteFa}")
        }
        val doneCount = med.completedSlotTimesFa.size
        appendLine(
          "   • نوبت‌های ثبت‌شده امروز: ${toPersianDigits(doneCount.toString())} از ${toPersianDigits(med.dailyDoseSlots.toString())}"
        )
        appendLine()
      }
      appendLine("شرایط نگهداری: ${plan.storageInstructionFa}")
      appendLine("هشدار فوری: در صورت بروز کهیر، تورم لب/پلک یا تنگی نفس، دارو را فوراً قطع کرده و با اورژانس ۱۱۵ یا پزشک تماس بگیرید.")
    }
}
