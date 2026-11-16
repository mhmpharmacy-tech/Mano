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

  fun evaluate(
    flow: SymptomFlow,
    ageBand: AgeBand,
    temperatureCelsius: Double?,
    durationHours: Int?,
    selectedRedFlagIds: Set<String>,
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

    // Rule 2: Fever >= 38.0 °C in an infant under three months needs urgent review
    // regardless of which symptom flow the parent opened. For a "fever" flow
    // without a measured number, treat possible fever cautiously as urgent.
    val measuredInfantFever =
      ageBand == AgeBand.NEWBORN && temperatureCelsius != null && temperatureCelsius >= 38.0
    val suspectedInfantFever =
      ageBand == AgeBand.NEWBORN && flow.id == "fever" && temperatureCelsius == null
    if (measuredInfantFever || suspectedInfantFever) {
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

    // Rule 3: Timely clinician review flags (non-immediate red flags or prolonged duration)
    val checkedReviewFlags =
      flow.redFlags.filter { it.id in selectedRedFlagIds && !it.isImmediateEmergency }
    val prolongedFever = flow.id == "fever" && (durationHours ?: 0) >= 72
    val prolongedCough = flow.id == "cough" && (durationHours ?: 0) >= 24 * 14
    val youngInfantHighTemp =
      ageBand == AgeBand.INFANT_3_12M && (temperatureCelsius ?: 0.0) >= 39.0

    if (checkedReviewFlags.isNotEmpty() || prolongedFever || prolongedCough || youngInfantHighTemp) {
      val reasonParts = mutableListOf<String>()
      if (checkedReviewFlags.isNotEmpty()) {
        reasonParts += checkedReviewFlags.joinToString("، ") { it.labelFa }
      }
      if (prolongedFever) reasonParts += "تداوم تب بیش از ۷۲ ساعت"
      if (prolongedCough) reasonParts += "تداوم سرفه بیش از ۲ هفته"
      if (youngInfantHighTemp) reasonParts += "تب ۳۹ درجه یا بالاتر در شیرخوار ۳ تا ۱۲ ماه"

      return TriageEvaluationResult(
        outcome = TriageOutcome.NEEDS_TIMELY_REVIEW,
        titleFa = "توصیه به معاینه پزشک در زمان نزدیک",
        summaryFa =
          "بر اساس اطلاعات واردشده (${reasonParts.joinToString("، ")})، لازم است کودک در اولین فرصت مناسب توسط پزشک معاینه شود. نکات زیر صرفاً برای مراقبت حمایتی تا زمان ویزیت است و جایگزین تشخیص پزشکی نیست.",
        immediateActionsFa = flow.educationalStepsFa,
        escalationCriteriaFa = flow.escalationCriteriaFa,
        relatedArticleSlug = flow.relatedArticleSlug,
        canShowSelfCare = true,
        triggeredRule =
          TriageRule(
            id = "rule_timely_clinician_review",
            descriptionFa = "وجود عامل خطر غیرفوری یا تداوم علامت نیازمند ویزیت پزشک است.",
            outcome = TriageOutcome.NEEDS_TIMELY_REVIEW,
            reviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
          ),
        showPharmacistOption = true,
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
