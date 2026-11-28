package com.example.domain.ai

import com.example.data.ManoSourcesAndClinicalSeed
import com.example.domain.model.AgeBand
import com.example.domain.model.TriageOutcome
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.NutritionPromptBuilder

/**
 * Deterministic, safe offline fallbacks grounded exclusively in verified clinical seed rules
 * and parent-entered logs. Never invents diagnoses, doses, or external references.
 */
object AiOfflineFallbacks {
  fun emergencyChecklistBypass(req: SafetyChecklistRequest): SafetyChecklistAiJson =
    SafetyChecklistAiJson(
      checklistTitleFa = "هشدار فوری اورژانس — ${req.primaryFlow.titleFa}",
      urgencySummaryFa =
        "بر اساس موتور قطعی غربالگری، علائم ثبت‌شده نیازمند مراجعه فوری به اورژانس (تماس با ۱۱۵) است. مراقبت خانگی متوقف شده و هوش مصنوعی اجازه تغییر این هشدار را ندارد.",
      immediateEmergencySignsFa =
        req.primaryFlow.redFlags.filter { it.isImmediateEmergency }.map { it.labelFa }.ifEmpty {
          listOf("هر علامت تنگی نفس، بی‌حالی شدید، تشنج یا تب بالای ۳۸ درجه در نوزاد زیر ۳ ماه")
        },
      sameDayPediatricianVisitSignsFa = req.primaryFlow.escalationCriteriaFa,
      homeMonitoringChecklistFa =
        listOf(
          "هم‌اکنون با اورژانس ۱۱۵ تماس بگیرید یا کودک را به نزدیک‌ترین مرکز درمانی برسانید.",
          "عدد دما، زمان آخرین دارو و تغذیه کودک را یادداشت کرده و همراه داشته باشید.",
        ),
      questionsForPediatricianFa =
        listOf(
          "با توجه به علائم فوری کودک، چه بررسی‌های اورژانسی نیاز است؟",
        ),
      citedSourceIds = req.primaryFlow.sources.map { it.id }.ifEmpty { listOf("nice_ng143", "aap_fever") },
    )

  fun safetyChecklistFallback(req: SafetyChecklistRequest): SafetyChecklistAiJson {
    if (req.triageOutcome == TriageOutcome.URGENT_EMERGENCY) {
      return emergencyChecklistBypass(req)
    }
    val ageLabel = req.ageBand?.labelFa ?: "رده سنی نامشخص"
    val urgency =
      when (req.triageOutcome) {
        TriageOutcome.NEEDS_TIMELY_REVIEW ->
          "نتیجه موتور بالینی نشان‌دهنده نیاز به معاینه پزشک در همان روز برای «${req.primaryFlow.titleFa}» ($ageLabel) است."
        else ->
          "مراقبت حمایتی مطابق راهنمای تاییدشده برای «${req.primaryFlow.titleFa}» ($ageLabel) همراه با پایش منظم علائم هشدار."
      }
    return SafetyChecklistAiJson(
      checklistTitleFa = "چک‌لیست ایمنی تاییدشده — ${req.primaryFlow.titleFa}",
      urgencySummaryFa = urgency,
      immediateEmergencySignsFa = req.primaryFlow.redFlags.map { it.labelFa },
      sameDayPediatricianVisitSignsFa = req.primaryFlow.escalationCriteriaFa,
      homeMonitoringChecklistFa = req.primaryFlow.educationalStepsFa,
      questionsForPediatricianFa =
        listOf(
          "با توجه به سن ($ageLabel) و روند علائم، چه معاینه یا آزمایشی لازم است؟",
          "در صورت تداوم علائم بیش از ۲۴ تا ۴۸ ساعت چه اقدامی انجام دهیم؟",
        ),
      citedSourceIds = req.primaryFlow.sources.map { it.id }.ifEmpty { listOf("aap_fever", "nice_ng143") },
    )
  }

  fun documentedQaFallback(req: DocumentedQaRequest): DocumentedQaAiJson {
    val firstFaq = req.relatedFaqs.firstOrNull()
    val summary =
      firstFaq?.summaryFa
        ?: "در حال حاضر اتصال به سرویس کمکی برقرار نیست؛ لطفاً در صورت وجود هر علامت هشدار فوری فوراً به پزشک یا اورژانس ۱۱۵ مراجعه کنید."
    val steps =
      firstFaq?.whatToDoFa?.ifEmpty { null }
        ?: listOf("ثبت دقیق زمان شروع علائم، دما و مایعات مصرفی کودک", "مشورت غیرفوری با داروساز یا معاینه پزشک اطفال")
    val seekDoctor =
      firstFaq?.whenToSeeDoctorFa?.ifEmpty { null }
        ?: ManoSourcesAndClinicalSeed.articles.firstOrNull()?.redFlagsFa
        ?: listOf("تنگی نفس، بی‌حالی شدید، تشنج یا تب بالای ۳۸ درجه در نوزاد زیر ۳ ماه")
    val sourceIds = listOfNotNull(firstFaq?.source?.id).ifEmpty { listOf("aap_fever", "nice_ng143") }
    return DocumentedQaAiJson(
      directAnswerFa = summary,
      actionableStepsFa = steps,
      whenToVisitPediatricianFa = seekDoctor,
      citedSourceIds = sourceIds,
      safetyDisclaimerFa = "این پاسخ از منابع علمی تاییدشده مانوکید استخراج شده و جایگزین معاینه حضوری پزشک یا نظر داروساز نیست.",
    )
  }

  fun visitDraftFallback(req: VisitDraftRequest): VisitDraftStructuredOutput {
    val title = "خلاصه آماده ویزیت — ${req.childAliasFa.ifBlank { "کودک" }} (${req.symptomTitleFa.ifBlank { "پایش خانگی" }})"
    val text = buildString {
      appendLine("خلاصه داده‌های ثبت‌شده توسط خانواده (پیش‌نویس نیازمند بررسی پزشک/داروساز):")
      appendLine("• مشخصات ثبت‌شده: ${req.childAliasFa.ifBlank { "ثبت‌نشده" }} | رده سنی: ${req.ageBandLabelFa} | وزن: ${req.weightKgText.ifBlank { "نامشخص" }} کیلوگرم")
      appendLine("• حساسیت شناخته‌شده: ${req.allergiesFa.ifBlank { "ثبت‌نشده" }}")
      appendLine("• داروهای فعلی: ${req.currentMedsFa.ifBlank { "ثبت‌نشده" }}")
      if (req.symptomSummaryFa.isNotBlank()) {
        appendLine("• شرح غربالگری علائم: ${req.symptomSummaryFa}")
      }
      if (req.temperatureCelsius != null) {
        appendLine("• آخرین دمای ثبت‌شده: ${FeverDoseTimelineEngine.formatTempC(req.temperatureCelsius)}")
      }
      appendLine("• خط زمانی ۲۴ ساعت اخیر: ${req.timelineSnapshot.doseCount24h} نوبت دارو ثبت‌شده")
      if (req.parentNoteFa.isNotBlank()) {
        appendLine("• یادداشت والد: ${req.parentNoteFa}")
      }
      appendLine()
      appendLine("پرسش‌های پیشنهادی برای پزشک یا داروساز:")
      appendLine("۱. با توجه به سن، وزن و علائم ثبت‌شده، آیا معاینه یا تغییر برنامه درمانی لازم است؟")
      appendLine("۲. در صورت بروز چه نشانه‌هایی باید فوراً به اورژانس مراجعه کنیم؟")
      appendLine()
      appendLine("توجه: این پیش‌نویس فقط جمع‌بندی داده‌های والد است و هیچ دوز یا تشخیصی صادر نمی‌کند.")
    }.trim()
    return VisitDraftStructuredOutput(titleFa = title, draftTextFa = text)
  }

  fun nutritionFallback(req: NutritionEducationRequest): NutritionPlanAiJson {
    val underSix = NutritionPromptBuilder.isLikelyUnderSixMonths(req.ageBand, req.ageDetailFa)
    val underTwelve = underSix || req.ageBand == AgeBand.INFANT_3_12M
    val avoidList = buildList {
      add("عسل در کودکان زیر ۱۲ ماه (خطر بوتولیسم)")
      add("شیر گاو پاستوریزه به‌عنوان نوشیدنی اصلی تا پایان ۱۲ ماهگی")
      if (underSix) add("هرگونه غذای کمکی جامد، پوره، آبمیوه یا آب اضافه پیش از پایان ۶ ماهگی")
      if (req.allergiesFa.isNotBlank() && req.allergiesFa != "ندارد") {
        add("مواد حساسیت‌زای ثبت‌شده در پرونده: ${req.allergiesFa}")
      }
    }
    val meals =
      if (underSix) {
        listOf(
          NutritionMealAiJson(
            mealTypeFa = "تغذیه انحصاری شیر",
            titleFa = "شیر مادر یا شیرخشک استاندارد طبق نیاز نوزاد",
            whySuitableFa = "در زیر ۶ ماهگی تمام نیاز آب و انرژی از شیر مادر یا شیرخشک استاندارد تأمین می‌شود.",
            textureAndPortionFa = "درخواست‌محور (هر ۲ تا ۳ ساعت یا طبق علائم گرسنگی)",
            ingredientsFa = listOf("شیر مادر یا شیرخشک استاندارد"),
            prepStepsFa = listOf("رعایت بهداشت وسایل شیردهی و پیگیری تعداد پوشک خیس"),
          ),
        )
      } else {
        listOf(
          NutritionMealAiJson(
            mealTypeFa = "وعده اصلی ملایم",
            titleFa = "سوپ برنج و سبزیجات له‌شده بدون نمک و ادویه",
            whySuitableFa = "هضم آسان و تأمین مایعات و انرژی متناسب با اشتهای کودک",
            textureAndPortionFa = "بافت نرم و متناسب با سن کودک",
            ingredientsFa = listOf("برنج نرم‌پخته", "هویج و کدو پخته"),
            prepStepsFa = listOf("مواد را کاملاً بپزید و با کمی آبِ پخت نرم کنید بدون افزودن نمک یا شکر"),
          ),
          NutritionMealAiJson(
            mealTypeFa = "میان‌وعده آموزشی",
            titleFa = "پوره میوه فصل مناسب سن (مثل سیب یا موز رسیده)",
            whySuitableFa = "تأمین ویتامین و فیبر بدون قند افزوده",
            textureAndPortionFa = "تکه‌های بسیار نرم یا پوره ایمن بدون خطر خفگی",
            ingredientsFa = listOf("سیب پخته یا موز کاملاً رسیده"),
            prepStepsFa = listOf("هسته و پوست سفت را جدا کرده و به بافت ایمن برسانید"),
          ),
        )
      }

    return NutritionPlanAiJson(
      planTitleFa = "راهنمای تغذیه ایمن پشتیبان — ${req.ageBand.labelFa}",
      overviewFa =
        if (underSix) "در سن زیر ۶ ماه تغذیه انحصاری با شیر مادر یا شیرخشک استاندارد اولویت قطعی است و غذای جامد مجاز نیست."
        else "نمونه برنامه آموزشی سبک و کم‌نمک برای همراهی با رشد کودک؛ پیش از تغییرات تغذیه درمانی با پزشک مشورت کنید.",
      growthNutritionFocusFa = "وضعیت اشتها: ${req.appetiteStatus.labelFa}${if (underTwelve) " • رعایت منع عسل و شیر گاو زیر یک سال" else ""}",
      meals = meals,
      hydrationTipFa =
        if (underSix) "آب و مایعات مورد نیاز صرفاً از طریق شیر مادر یا شیرخشک تأمین می‌شود."
        else "پیشنهاد منظم آب آشامیدنی تمیز، شیر و در صورت اسهال/استفراغ استفاده از ORS طبق نظر پزشک.",
      safetyAvoidListFa = avoidList,
      doctorFollowUpHintFa = "در صورت افت وزن، حساسیت غذایی جدید یا کاهش پوشک خیس با پزشک کودک مشورت کنید.",
      citedSourceIds = listOf("who_ors", "cdc_milestones"),
    )
  }

  fun syrupOcrFallback(reasonNoteFa: String? = null): SyrupLabelAiJson =
    SyrupLabelAiJson(
      drugNameFa = "UNKNOWN",
      activeIngredientKey = "UNKNOWN",
      dosageForm = "UNKNOWN",
      mgAmount = null,
      perMlAmount = null,
      mgPerMl = null,
      confidenceLevel = "REQUIRE_REVIEW",
      concentrationDisplayFa = "UNKNOWN",
      safetyNoteFa =
        reasonNoteFa
          ?: "خواندن خودکار برچسب دارو کامل نشد یا نیاز به بازبینی دارد. غلظت (mg در mL) را مستقیماً از روی جعبه وارد کنید و با داروساز چک کنید.",
      missingFields = listOf("drugNameFa", "activeIngredientKey", "mgAmount", "perMlAmount"),
      requiresHumanReview = true,
    )
}
