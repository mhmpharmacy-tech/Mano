package com.example.domain.util

import com.example.domain.model.AgeBand
import com.example.domain.model.ReviewStatus
import com.example.domain.model.SymptomFlow
import com.example.domain.model.TriageEvaluationResult
import com.example.domain.model.TriageOutcome
import com.example.domain.model.TriageRule

/**
 * Deterministic, Gemini-independent pediatric symptom & red-flag triage engine.
 * Urgent red flags or newborn fever always escalate directly to emergency 115 care.
 */
object TriageEngine {
  fun evaluate(
    flow: SymptomFlow,
    ageBand: AgeBand,
    selectedRedFlagIds: Set<String>,
    temperatureCelsius: Float? = null,
    durationHours: Int? = null,
    ageMonthsExact: Int? = null,
  ): TriageEvaluationResult {
    val selectedFlags = flow.redFlags.filter { it.id in selectedRedFlagIds }
    val hasImmediateFlag = selectedFlags.any { it.isImmediateEmergency }
    val isUnderThreeMonths =
      ageBand == AgeBand.NEWBORN || (ageMonthsExact != null && ageMonthsExact < 3)
    val newbornFeverEmergency = isUnderThreeMonths && temperatureCelsius != null && temperatureCelsius >= 38.0f
    val veryHighFever = temperatureCelsius != null && temperatureCelsius >= 40.0f

    if (hasImmediateFlag || newbornFeverEmergency || veryHighFever) {
      val triggeredDescription =
        when {
          newbornFeverEmergency ->
            "تب ۳۸ درجه سانتی‌گراد یا بالاتر در نوزاد زیر ۳ ماه نیازمند ارزیابی فوری اورژانس است."
          veryHighFever ->
            "تب ۴۰ درجه سانتی‌گراد یا بالاتر نیازمند بررسی فوری پزشکی است."
          else ->
            selectedFlags.firstOrNull { it.isImmediateEmergency }?.labelFa
              ?: "وجود علامت هشدار فوری در غربالگری علائم."
        }
      return TriageEvaluationResult(
        outcome = TriageOutcome.URGENT_EMERGENCY,
        titleFa = "ارجاع فوری به اورژانس (تماس با ۱۱۵)",
        summaryFa =
          "$triggeredDescription در این وضعیت، نمایش مراقبت خانگی متوقف می‌شود و کودک باید بدون تأخیر توسط پزشک اورژانس معاینه شود.",
        immediateActionsFa =
          listOf(
            "هم‌اکنون با اورژانس ۱۱۵ تماس بگیرید یا کودک را به نزدیک‌ترین مرکز اورژانس اطفال برسانید.",
            "عدد دما، ساعت اندازه‌گیری و هر داروی مصرف‌شده را برای ارائه به تیم اورژانس همراه داشته باشید.",
            "اگر کودک نوزاد زیر ۳ ماه است، پیش از معاینه پزشک خودسرانه تب‌بر ندهید.",
          ),
        escalationCriteriaFa = flow.escalationCriteriaFa,
        relatedArticleSlug = "when-to-see-doctor",
        canShowSelfCare = false,
        triggeredRule =
          TriageRule(
            id = "rule_${flow.id}_urgent_emergency",
            descriptionFa = triggeredDescription,
            outcome = TriageOutcome.URGENT_EMERGENCY,
            reviewStatus = ReviewStatus.APPROVED,
          ),
        showPharmacistOption = false,
      )
    }

    val hasNonImmediateFlag = selectedFlags.isNotEmpty()
    val prolongedDuration = durationHours != null && durationHours >= 48
    val highFever = temperatureCelsius != null && temperatureCelsius >= 39.0f

    if (hasNonImmediateFlag || prolongedDuration || highFever || isUnderThreeMonths) {
      val reasonFa =
        when {
          isUnderThreeMonths -> "سن زیر ۳ ماه نیازمند احتیاط و ارزیابی پزشک حتی در علائم خفیف است."
          highFever -> "دمای ۳۹ درجه یا بالاتر نیازمند پیگیری پزشکی در همان روز است."
          prolongedDuration -> "تداوم علائم به مدت ۴۸ ساعت یا بیشتر نیازمند ویزیت پزشک است."
          else -> selectedFlags.firstOrNull()?.labelFa ?: "علائم انتخاب‌شده نیازمند بررسی پزشک است."
        }
      return TriageEvaluationResult(
        outcome = TriageOutcome.NEEDS_TIMELY_REVIEW,
        titleFa = "نیاز به معاینه پزشک در همان روز",
        summaryFa =
          "$reasonFa ضمن حفظ آبرسانی و پایش علائم، زمان ویزیت پزشک کودک را هماهنگ کنید.",
        immediateActionsFa =
          listOf(
            "کودک را از نظر هوشیاری، تنفس راحت و تعداد پوشک خیس زیر نظر بگیرید.",
            "پیش‌نویس خلاصه ویزیت را آماده کنید و برای ارزیابی دقیق به پزشک یا داروساز مراجعه کنید.",
          ),
        escalationCriteriaFa = flow.escalationCriteriaFa,
        relatedArticleSlug = flow.relatedArticleSlug,
        canShowSelfCare = !isUnderThreeMonths,
        triggeredRule =
          TriageRule(
            id = "rule_${flow.id}_timely_review",
            descriptionFa = reasonFa,
            outcome = TriageOutcome.NEEDS_TIMELY_REVIEW,
            reviewStatus = ReviewStatus.APPROVED,
          ),
        showPharmacistOption = true,
      )
    }

    return TriageEvaluationResult(
      outcome = TriageOutcome.GENERAL_EDUCATIONAL,
      titleFa = "مراقبت حمایتی خانگی با پایش منظم علائم",
      summaryFa =
        "در پاسخ‌های فعلی نشانه اورژانسی علامت‌گذاری نشده است؛ مراقبت‌های حمایتی زیر بر اساس راهنماهای معتبر تدوین شده‌اند و در صورت تغییر حال کودک باید فوراً بازنگری شوند.",
      immediateActionsFa = flow.educationalStepsFa,
      escalationCriteriaFa = flow.escalationCriteriaFa,
      relatedArticleSlug = flow.relatedArticleSlug,
      canShowSelfCare = true,
      triggeredRule =
        TriageRule(
          id = "rule_${flow.id}_educational",
          descriptionFa = "عدم علامت‌گذاری نشانه هشدار فوری در پایش اولیه",
          outcome = TriageOutcome.GENERAL_EDUCATIONAL,
          reviewStatus = ReviewStatus.APPROVED,
        ),
      showPharmacistOption = true,
    )
  }
}
