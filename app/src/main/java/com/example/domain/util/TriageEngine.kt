package com.example.domain.util

import com.example.domain.model.AgeBand
import com.example.domain.model.ReviewStatus
import com.example.domain.model.SymptomFlow
import com.example.domain.model.TriageEvaluationResult
import com.example.domain.model.TriageOutcome
import com.example.domain.model.TriageRule

/**
 * Deterministic, reviewable clinical-safety triage evaluator.
 * Never outputs a medical diagnosis or numeric drug dose.
 */
object TriageEngine {

  fun isExtremeTemperature(celsius: Double?): Boolean =
    celsius != null && celsius in 34.0..43.0 && (celsius < 35.0 || celsius >= 40.5)

  fun evaluate(
    flow: SymptomFlow,
    ageBand: AgeBand,
    temperatureCelsius: Double?,
    durationHours: Int?,
    selectedRedFlagIds: Set<String>,
    ageMonthsExact: Int? = null,
  ): TriageEvaluationResult {
    val checkedImmediateFlags =
      flow.redFlags.filter { it.id in selectedRedFlagIds && it.isImmediateEmergency }

    // Rule 1: Critical red flags always bypass self-care immediately
    if (checkedImmediateFlags.isNotEmpty()) {
      return TriageEvaluationResult(
        outcome = TriageOutcome.URGENT_EMERGENCY,
        titleFa = "نیاز به ارزیابی فوری اورژانس",
        summaryFa =
          "به دلیل وجود نشانه هشدار (${checkedImmediateFlags.joinToString("، ") { it.labelFa }})، توصیه‌های مراقبت خانگی نمایش داده نمی‌شود و لازم است کودک بدون تأخیر توسط تیم درمانی یا اورژانس ۱۱۵ ارزیابی شود.",
        immediateActionsFa =
          listOf(
            "در صورت تنگی نفس، کبودی لب‌ها، تشنج یا کاهش هوشیاری، بلافاصله با اورژانس ۱۱۵ تماس بگیرید یا کودک را به نزدیک‌ترین مرکز اورژانس برسانید.",
            "تا زمان ارزیابی پزشک، از دادن خودسرانه دارو یا خوراندن مایعات در کودک خواب‌آلود خودداری کنید.",
          ),
        escalationCriteriaFa = flow.escalationCriteriaFa,
        relatedArticleSlug = flow.relatedArticleSlug,
        canShowSelfCare = false,
        triggeredRule =
          TriageRule(
            id = "rule_critical_red_flag_bypass",
            descriptionFa = "وجود علامت هشدار بحرانی، مسیر مراقبت خانگی را متوقف می‌کند.",
            outcome = TriageOutcome.URGENT_EMERGENCY,
            reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
          ),
        showPharmacistOption = false,
      )
    }

    // Rule 2: Fever >= 38.0 °C in infant younger than 3 months (NEWBORN band)
    if (ageBand == AgeBand.NEWBORN &&
      ((temperatureCelsius != null && temperatureCelsius >= 38.0) ||
        (flow.id == "fever" && temperatureCelsius == null))) {
      val tempText =
        if (temperatureCelsius != null) {
          "دمای ثبت‌شده ${temperatureCelsius} درجه سانتی‌گراد"
        } else {
          "احتمال تب در نوزاد زیر ۳ ماه"
        }
      return TriageEvaluationResult(
        outcome = TriageOutcome.URGENT_EMERGENCY,
        titleFa = "ارزیابی فوری پزشکی برای تب نوزاد زیر ۳ ماه",
        summaryFa =
          "در نوزادان کوچک‌تر از ۳ ماه، $tempText یک وضعیت نیازمند ارزیابی فوری حضوری توسط پزشک یا اورژانس است. مانو در این شرایط هیچ دوز دارویی یا اطمینان‌بخشی خانگی ارائه نمی‌کند.",
        immediateActionsFa =
          listOf(
            "نوزاد را بلافاصله به اورژانس اطفال یا پزشک برسانید (و در صورت بی‌حالی یا مشکل تنفسی با اورژانس ۱۱۵ تماس بگیرید).",
            "پیش از معاینه پزشک، خودسرانه تب‌بر به نوزاد ندهید و پوشش نوزاد را بیش از حد ضخیم نکنید.",
            "زمان شروع تب، عدد دقیق دماسنج و میزان شیر خوردن و پوشک خیس نوزاد را برای پزشک یادداشت کنید.",
          ),
        escalationCriteriaFa = flow.escalationCriteriaFa,
        relatedArticleSlug = flow.relatedArticleSlug,
        canShowSelfCare = false,
        triggeredRule =
          TriageRule(
            id = "rule_infant_under_3m_fever_38",
            descriptionFa = "تب ۳۸ درجه یا بیشتر در شیرخوار زیر ۳ ماه نیازمند ارزیابی فوری پزشکی است.",
            outcome = TriageOutcome.URGENT_EMERGENCY,
            reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
          ),
        showPharmacistOption = false,
      )
    }

    if (isExtremeTemperature(temperatureCelsius)) {
      return TriageEvaluationResult(
        outcome = TriageOutcome.URGENT_EMERGENCY,
        titleFa = "دمای ثبت‌شده نیاز به ارزیابی فوری دارد",
        summaryFa = "دمای ثبت‌شده $temperatureCelsius درجه است. برای ارزیابی فوری حضوری اقدام کنید.",
        immediateActionsFa = listOf("کودک را به مرکز درمانی برسانید یا با اورژانس ۱۱۵ تماس بگیرید."),
        escalationCriteriaFa = flow.escalationCriteriaFa,
        relatedArticleSlug = flow.relatedArticleSlug,
        canShowSelfCare = false,
        triggeredRule = TriageRule("rule_extreme_temperature_emergency", "دمای ثبت‌شده در محدوده هشدار فوری است.",
          TriageOutcome.URGENT_EMERGENCY, ReviewStatus.NEEDS_CLINICAL_REVIEW),
        showPharmacistOption = false,
      )
    }

    val invalidInput =
      (temperatureCelsius != null && (!temperatureCelsius.isFinite() || temperatureCelsius !in 34.0..43.0)) ||
      (durationHours != null && durationHours !in 0..8760) ||
      selectedRedFlagIds.any { id -> flow.redFlags.none { it.id == id } } ||
      (ageMonthsExact != null && ageBand == AgeBand.INFANT_3_12M && ageMonthsExact !in 3..11)
    if (invalidInput) return TriageEvaluationResult(
      outcome = TriageOutcome.NEEDS_TIMELY_REVIEW,
      titleFa = "اطلاعات نیاز به اصلاح دارد",
      summaryFa = "ورودی نامعتبر یا علامت ناشناخته دریافت شد. اطلاعات را بررسی کنید؛ این نتیجه به معنی نبود خطر نیست.",
      immediateActionsFa = listOf("در تنفس سخت، تشنج یا اختلال هوشیاری، منتظر اصلاح فرم نمانید و فوراً کمک پزشکی بگیرید."),
      escalationCriteriaFa = flow.escalationCriteriaFa,
      relatedArticleSlug = flow.relatedArticleSlug, canShowSelfCare = false,
      triggeredRule = TriageRule("rule_invalid_input", "ورودی ناقص یا نامعتبر؛ بدون نتیجهٔ اطمینان‌بخش",
        TriageOutcome.NEEDS_TIMELY_REVIEW, ReviewStatus.NEEDS_CLINICAL_REVIEW),
      showPharmacistOption = false,
    )

    // Rule 3: Timely clinician review flags (non-immediate red flags or prolonged duration)
    val checkedReviewFlags =
      flow.redFlags.filter { it.id in selectedRedFlagIds && !it.isImmediateEmergency }
    val prolongedFever = flow.id == "fever" && (durationHours ?: 0) >= 72
    val prolongedCough = flow.id == "cough" && (durationHours ?: 0) >= 24 * 14
    val youngInfantHighTemp =
      ageBand == AgeBand.INFANT_3_12M && (ageMonthsExact == null || ageMonthsExact <= 6) &&
        (temperatureCelsius ?: 0.0) >= 39.0

    if (checkedReviewFlags.isNotEmpty() || prolongedFever || prolongedCough || youngInfantHighTemp) {
      val reasonParts = mutableListOf<String>()
      if (checkedReviewFlags.isNotEmpty()) {
        reasonParts += checkedReviewFlags.joinToString("، ") { it.labelFa }
      }
      if (prolongedFever) reasonParts += "تداوم تب بیش از ۷۲ ساعت"
      if (prolongedCough) reasonParts += "تداوم سرفه بیش از ۲ هفته"
      if (youngInfantHighTemp) reasonParts += if (ageMonthsExact == null)
        "دمای ۳۹ یا بیشتر و سن دقیق شیرخوار نامشخص است؛ احتمال سن ۳ تا ۶ ماه"
        else "دمای ۳۹ یا بیشتر در شیرخوار ۳ تا ۶ ماه"
      if (flow.id == "fever" && (durationHours ?: 0) >= 120) reasonParts += "تب ۵ روز یا بیشتر؛ نیاز به ارزیابی علل تب طولانی"

      return TriageEvaluationResult(
        outcome = TriageOutcome.NEEDS_TIMELY_REVIEW,
        titleFa = if (youngInfantHighTemp) "تماس فوری با پزشک برای ارزیابی سریع" else "نیاز به ارزیابی پزشک",
        summaryFa =
          "بر اساس اطلاعات واردشده (${reasonParts.joinToString("، ")})، لازم است کودک در اولین فرصت مناسب توسط پزشک معاینه شود. نکات زیر صرفاً برای مراقبت حمایتی تا زمان ویزیت است و جایگزین تشخیص پزشکی نیست.",
        immediateActionsFa = if (youngInfantHighTemp) listOf("برای ارزیابی سریع با پزشک یا مرکز درمانی تماس بگیرید؛ منتظر پاسخ مشاورهٔ آنلاین نمانید.") else flow.educationalStepsFa,
        escalationCriteriaFa = flow.escalationCriteriaFa,
        relatedArticleSlug = flow.relatedArticleSlug,
        canShowSelfCare = !youngInfantHighTemp,
        triggeredRule =
          TriageRule(
            id = "rule_timely_clinician_review",
            descriptionFa = "وجود عامل خطر غیرفوری یا تداوم علامت نیازمند ویزیت پزشک است.",
            outcome = TriageOutcome.NEEDS_TIMELY_REVIEW,
            reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
          ),
        showPharmacistOption = false,
      )
    }

    // Rule 4: General educational support (no red flags checked, non-infant-fever)
    return TriageEvaluationResult(
      outcome = TriageOutcome.GENERAL_EDUCATIONAL,
      titleFa = "راهنمای آموزشی مراقبت حمایتی",
      summaryFa =
        "در حال حاضر علامت هشدار فوری ثبت نشده است. این راهنما برای آگاهی والدین و پایش وضعیت کودک ارائه می‌شود و تشخیص پزشکی محسوب نمی‌شود؛ در صورت تغییر حال عمومی با پزشک مشورت کنید.",
      immediateActionsFa = flow.educationalStepsFa,
      escalationCriteriaFa = flow.escalationCriteriaFa,
      relatedArticleSlug = flow.relatedArticleSlug,
      canShowSelfCare = true,
      triggeredRule =
        TriageRule(
          id = "rule_general_educational_support",
          descriptionFa = "نبود علامت هشدار در ارزیابی اولیه؛ نمایش راهنمای آموزشی و معیارهای پیگیری.",
          outcome = TriageOutcome.GENERAL_EDUCATIONAL,
          reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
        ),
      showPharmacistOption = true,
    )
  }
}
