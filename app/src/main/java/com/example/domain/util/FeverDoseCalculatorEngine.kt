package com.example.domain.util

import com.example.data.ManoSourcesAndClinicalSeed
import java.util.Locale
import kotlin.math.roundToInt

enum class FeverAgeGroup(
  val id: String,
  val labelFa: String,
  val subtitleFa: String,
  val representativeAgeMonths: Int,
) {
  UNDER_3_MONTHS(
    id = "under_3m",
    labelFa = "زیر ۳ ماه",
    subtitleFa = "نوزاد و شیرخوار بسیار کوچک",
    representativeAgeMonths = 2,
  ),
  THREE_TO_SIX_MONTHS(
    id = "3_to_6m",
    labelFa = "۳ تا ۶ ماه",
    subtitleFa = "فقط استامینوفن مجاز است",
    representativeAgeMonths = 4,
  ),
  OVER_6_MONTHS(
    id = "over_6m",
    labelFa = "بالای ۶ ماه",
    subtitleFa = "استامینوفن یا ایبوپروفن بر اساس وزن",
    representativeAgeMonths = 18,
  ),
}

enum class FeverPriorDoseStatus(
  val id: String,
  val labelFa: String,
) {
  NONE_IN_6H("none", "خیر، در ۶ ساعت گذشته تب‌بر نداده‌ام"),
  ACETAMINOPHEN_UNDER_4H("acet_under_4h", "بله، استامینوفن (کمتر از ۴ ساعت پیش)"),
  ACETAMINOPHEN_4_TO_6H("acet_4_to_6h", "بله، استامینوفن (۴ تا ۶ ساعت پیش)"),
  IBUPROFEN_UNDER_4H("ibu_under_4h", "بله، ایبوپروفن (کمتر از ۴ ساعت پیش)"),
  IBUPROFEN_4_TO_6H("ibu_4_to_6h", "بله، ایبوپروفن (۴ تا ۶ ساعت پیش)"),
}

data class FeverMedicineOption(
  val formulation: LiquidFormulationId,
  val titleFa: String,
  val localBrandsHintFa: String,
  val boxStrengthBadgeFa: String,
  val confirmationPromptFa: String,
  val isDrops: Boolean,
  val isIbuprofen: Boolean,
)

data class WeightSafetyNotice(
  val isOutlierWarning: Boolean = false,
  val outlierMessageFa: String? = null,
  val showDropsToSyrupSuggestion: Boolean = false,
  val dropsSuggestionMessageFa: String? = null,
)

data class PriorDoseConflictEvaluation(
  val isHardConflict: Boolean,
  val isCautionSpacing: Boolean,
  val titleFa: String,
  val bodyFa: String,
  val suggestedWaitHours: Int = 0,
)

data class FeverDoseCalculationResult(
  val ageGroup: FeverAgeGroup,
  val formulation: LiquidFormulationId,
  val weightKg: Float,
  val isBlocked: Boolean,
  val blockReasonFa: String? = null,
  val recommendedMl: Float = 0f,
  val minSafeMl: Float = 0f,
  val maxSafeMl: Float = 0f,
  val recommendedMg: Double = 0.0,
  val approxDropsCount: Int? = null,
  val isSafetyCeilingApplied: Boolean = false,
  val reminderIntervalHours: Int = 6,
  val maxDosesPer24h: Int = 5,
  val clinicalSourceTagFa: String = "Lexicomp Pediatric 2026 • UpToDate",
)

object FeverDoseCalculatorEngine {

  const val MIN_FEVER_WEIGHT_KG = 3.0f
  const val MAX_FEVER_WEIGHT_KG = 40.0f
  const val DROPS_TO_SYRUP_WEIGHT_THRESHOLD_KG = 12.0f

  val iranFeverMedicines: List<FeverMedicineOption> =
    listOf(
      FeverMedicineOption(
        formulation = LiquidFormulationId.ACETAMINOPHEN_DROPS_100,
        titleFa = "قطره استامینوفن کودکان",
        localBrandsHintFa = "پاراکید، استا، تایلوفن، تایلنول نوزاد",
        boxStrengthBadgeFa = "۱۰۰ میلی‌گرم در ۱ سی‌سی",
        confirmationPromptFa = "لطفاً عدد روی جعبه یا شیشه را چک کنید: آیا دقیقاً «۱۰۰ میلی‌گرم در ۱ سی‌سی (100 mg/mL)» نوشته شده است؟",
        isDrops = true,
        isIbuprofen = false,
      ),
      FeverMedicineOption(
        formulation = LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5,
        titleFa = "شربت استامینوفن استاندارد",
        localBrandsHintFa = "شربت استامینوفن کودکان، پاراکید، کوریزان ساده",
        boxStrengthBadgeFa = "۱۲۰ میلی‌گرم در ۵ سی‌سی",
        confirmationPromptFa = "لطفاً عدد روی جعبه یا شیشه را چک کنید: آیا دقیقاً «۱۲۰ میلی‌گرم در ۵ سی‌سی (120 mg/5mL)» نوشته شده است؟",
        isDrops = false,
        isIbuprofen = false,
      ),
      FeverMedicineOption(
        formulation = LiquidFormulationId.ACETAMINOPHEN_SYRUP_160_5,
        titleFa = "شربت / سوسپانسیون استامینوفن غلیظ",
        localBrandsHintFa = "پاراکید ۱۶۰، سوسپانسیون استامینوفن ۱۶۰",
        boxStrengthBadgeFa = "۱۶۰ میلی‌گرم در ۵ سی‌سی",
        confirmationPromptFa = "لطفاً عدد روی جعبه یا شیشه را چک کنید: آیا دقیقاً «۱۶۰ میلی‌گرم در ۵ سی‌سی (160 mg/5mL)» نوشته شده است؟",
        isDrops = false,
        isIbuprofen = false,
      ),
      FeverMedicineOption(
        formulation = LiquidFormulationId.IBUPROFEN_SYRUP_100_5,
        titleFa = "شربت ایبوپروفن (پروفن / بروفن کودکان)",
        localBrandsHintFa = "پروفن کودکان، ادویل، بروفن سوسپانسیون",
        boxStrengthBadgeFa = "۱۰۰ میلی‌گرم در ۵ سی‌سی",
        confirmationPromptFa = "لطفاً عدد روی جعبه یا شیشه را چک کنید: آیا دقیقاً «۱۰۰ میلی‌گرم در ۵ سی‌سی (100 mg/5mL)» نوشته شده و شیشه را خوب تکان داده‌اید؟",
        isDrops = false,
        isIbuprofen = true,
      ),
    )

  fun isFormulationAllowedForAge(
    ageGroup: FeverAgeGroup,
    formulation: LiquidFormulationId,
  ): Boolean =
    when (ageGroup) {
      FeverAgeGroup.UNDER_3_MONTHS -> false
      FeverAgeGroup.THREE_TO_SIX_MONTHS -> formulation != LiquidFormulationId.IBUPROFEN_SYRUP_100_5
      FeverAgeGroup.OVER_6_MONTHS -> true
    }

  fun inferAgeGroupFromMonths(ageMonths: Int?): FeverAgeGroup? =
    when {
      ageMonths == null -> null
      ageMonths < 3 -> FeverAgeGroup.UNDER_3_MONTHS
      ageMonths < 6 -> FeverAgeGroup.THREE_TO_SIX_MONTHS
      else -> FeverAgeGroup.OVER_6_MONTHS
    }

  fun evaluateWeightNotices(
    ageGroup: FeverAgeGroup,
    weightKg: Float,
    formulation: LiquidFormulationId,
  ): WeightSafetyNotice {
    val outlierMessage =
      when (ageGroup) {
        FeverAgeGroup.THREE_TO_SIX_MONTHS ->
          when {
            weightKg < 4.5f -> "وزن واردشده برای کودک ۳ تا ۶ ماهه کمتر از بازه معمول است؛ لطفاً عدد وزن را مجدداً بررسی کنید."
            weightKg > 11.5f -> "وزن واردشده برای رده سنی ۳ تا ۶ ماه زیاد به نظر می‌رسد؛ لطفاً مطمئن شوید خطای تایپی یا انتخاب رده سنی رخ نداده است."
            else -> null
          }
        FeverAgeGroup.OVER_6_MONTHS ->
          when {
            weightKg < 5.5f -> "وزن واردشده برای کودک بالای ۶ ماه بسیار پایین است؛ لطفاً عدد وزن را دوباره بررسی کنید."
            weightKg > 38.0f -> "برای وزن‌های بالا سقف ایمنی دوز تک‌نوبت اعمال می‌شود؛ لطفاً وزن دقیق کودک را تأیید کنید."
            else -> null
          }
        FeverAgeGroup.UNDER_3_MONTHS -> null
      }

    val suggestSyrup =
      formulation == LiquidFormulationId.ACETAMINOPHEN_DROPS_100 &&
        weightKg > DROPS_TO_SYRUP_WEIGHT_THRESHOLD_KG
    val dropsMessage =
      if (suggestSyrup) {
        "برای کودکان با وزن بالای ۱۲ کیلوگرم، حجم قطره زیاد می‌شود و اندازه‌گیری آن دشوارتر است. پیشنهاد می‌شود از شربت استامینوفن ۱۲۰/۵ یا ۱۶۰/۵ استفاده کنید."
      } else {
        null
      }

    return WeightSafetyNotice(
      isOutlierWarning = outlierMessage != null,
      outlierMessageFa = outlierMessage,
      showDropsToSyrupSuggestion = suggestSyrup,
      dropsSuggestionMessageFa = dropsMessage,
    )
  }

  fun evaluatePriorDoseConflict(
    selectedFormulation: LiquidFormulationId,
    priorStatus: FeverPriorDoseStatus,
  ): PriorDoseConflictEvaluation {
    val selectedIsIbuprofen = selectedFormulation == LiquidFormulationId.IBUPROFEN_SYRUP_100_5
    return when (priorStatus) {
      FeverPriorDoseStatus.NONE_IN_6H ->
        PriorDoseConflictEvaluation(
          isHardConflict = false,
          isCautionSpacing = false,
          titleFa = "تداخل زمانی مشاهده نشد",
          bodyFa = "در ۶ ساعت گذشته تب‌بر مصرف نشده است؛ می‌توانید دوز دقیق این نوبت را مشاهده کنید.",
        )

      FeverPriorDoseStatus.ACETAMINOPHEN_UNDER_4H ->
        if (!selectedIsIbuprofen) {
          PriorDoseConflictEvaluation(
            isHardConflict = true,
            isCautionSpacing = false,
            titleFa = "هنوز زمان نوبت بعدی استامینوفن نرسیده است",
            bodyFa = "از آخرین نوبت استامینوفن کمتر از ۴ ساعت گذشته است. دادن زودهنگام دوز تکراری خطر آسیب کبدی دارد. تا رسیدن به فاصله حداقل ۴ تا ۶ ساعت، از روش‌های ایمن غیردارویی استفاده کنید.",
            suggestedWaitHours = 4,
          )
        } else {
          PriorDoseConflictEvaluation(
            isHardConflict = false,
            isCautionSpacing = true,
            titleFa = "احتیاط در مصرف متناوب استامینوفن و ایبوپروفن",
            bodyFa = "به‌طور معمول مصرف یکی از دو تب‌بر کافی است. تناوب بین استامینوفن و ایبوپروفن تنها در صورت بی‌قراری شدید و با ثبت دقیق ساعت انجام شود تا خطای دوز رخ ندهد.",
          )
        }

      FeverPriorDoseStatus.ACETAMINOPHEN_4_TO_6H ->
        if (!selectedIsIbuprofen) {
          PriorDoseConflictEvaluation(
            isHardConflict = false,
            isCautionSpacing = true,
            titleFa = "حداقل فاصله ۴ ساعت رعایت شده است",
            bodyFa = "اگر کودک همچنان بی‌قرار است و حداقل ۴ ساعت از نوبت قبلی گذشته، می‌توان نوبت جدید استامینوفن را داد (حداکثر ۵ نوبت در ۲۴ ساعت).",
          )
        } else {
          PriorDoseConflictEvaluation(
            isHardConflict = false,
            isCautionSpacing = true,
            titleFa = "توجه به تغییر نوع تب‌بر",
            bodyFa = "اگر پیش‌تر استامینوفن داده‌اید و قصد دادن ایبوپروفن دارید، ساعت دقیق هر دو را ثبت کنید تا از تداخل جلوگیری شود.",
          )
        }

      FeverPriorDoseStatus.IBUPROFEN_UNDER_4H ->
        if (selectedIsIbuprofen) {
          PriorDoseConflictEvaluation(
            isHardConflict = true,
            isCautionSpacing = false,
            titleFa = "هنوز زمان نوبت بعدی ایبوپروفن نرسیده است",
            bodyFa = "حداقل فاصله ایمن بین دو نوبت ایبوپروفن ۶ تا ۸ ساعت است. دادن زودتر از موعد می‌تواند به معده و کلیه کودک آسیب بزند. تا نوبت بعدی از مراقبت‌های ایمن غیردارویی کمک بگیرید.",
            suggestedWaitHours = 6,
          )
        } else {
          PriorDoseConflictEvaluation(
            isHardConflict = false,
            isCautionSpacing = true,
            titleFa = "احتیاط در ترکیب با نوبت اخیر ایبوپروفن",
            bodyFa = "اگر کمتر از ۴ ساعت پیش ایبوپروفن داده‌اید و کودک صرفاً تب خفیف دارد ولی سرحال است، نیازی به داروی دوم نیست. در صورت بی‌قراری زیاد با ثبت دقیق ساعت اقدام کنید.",
          )
        }

      FeverPriorDoseStatus.IBUPROFEN_4_TO_6H ->
        if (selectedIsIbuprofen) {
          PriorDoseConflictEvaluation(
            isHardConflict = true,
            isCautionSpacing = false,
            titleFa = "تا تکمیل ۶ ساعت از ایبوپروفن صبر کنید",
            bodyFa = "فاصله ایمن بین دو نوبت ایبوپروفن حداقل ۶ ساعت است. تا پایان ساعت ششم، از روش‌های ایمن غیردارویی برای آرام کردن کودک استفاده کنید.",
            suggestedWaitHours = 2,
          )
        } else {
          PriorDoseConflictEvaluation(
            isHardConflict = false,
            isCautionSpacing = true,
            titleFa = "توجه به ثبت ساعت دو داروی متفاوت",
            bodyFa = "برای جلوگیری از اشتباه نیمه‌شب، حتماً دوز جدید را با دکمه ثبت در تایم‌لاین ذخیره کنید.",
          )
        }
    }
  }

  fun calculate(
    ageGroup: FeverAgeGroup,
    formulation: LiquidFormulationId,
    weightKg: Float,
  ): FeverDoseCalculationResult {
    if (ageGroup == FeverAgeGroup.UNDER_3_MONTHS) {
      return FeverDoseCalculationResult(
        ageGroup = ageGroup,
        formulation = formulation,
        weightKg = weightKg,
        isBlocked = true,
        blockReasonFa = "تب در نوزادان زیر ۳ ماه نیاز به معاینه مستقیم پزشک دارد. پیش از دادن هرگونه داروی تب‌بر با پزشک یا اورژانس ۱۱۵ تماس بگیرید.",
      )
    }
    if (!isFormulationAllowedForAge(ageGroup, formulation)) {
      return FeverDoseCalculationResult(
        ageGroup = ageGroup,
        formulation = formulation,
        weightKg = weightKg,
        isBlocked = true,
        blockReasonFa = "مصرف ایبوپروفن در کودکان زیر ۶ ماه مجاز نیست. لطفاً از قطره یا شربت استامینوفن استفاده کنید.",
      )
    }
    val safeWeight = weightKg.coerceIn(MIN_FEVER_WEIGHT_KG, MAX_FEVER_WEIGHT_KG)
    val eval =
      LiquidDoseVisualizerEngine.evaluate(
        formulation = formulation,
        ageMonths = ageGroup.representativeAgeMonths,
        weightKg = safeWeight,
      )
    if (eval.isBlocked) {
      return FeverDoseCalculationResult(
        ageGroup = ageGroup,
        formulation = formulation,
        weightKg = safeWeight,
        isBlocked = true,
        blockReasonFa = eval.alertBodyFa,
      )
    }

    val rawUncappedTargetMg = safeWeight * formulation.targetMgPerKg
    val ceilingApplied = rawUncappedTargetMg >= formulation.maxSingleDoseMg - 0.1
    val approxDrops =
      if (formulation == LiquidFormulationId.ACETAMINOPHEN_DROPS_100) {
        (eval.recommendedMl * 20f).roundToInt()
      } else {
        null
      }
    val isIbuprofen = formulation == LiquidFormulationId.IBUPROFEN_SYRUP_100_5

    return FeverDoseCalculationResult(
      ageGroup = ageGroup,
      formulation = formulation,
      weightKg = safeWeight,
      isBlocked = false,
      recommendedMl = eval.recommendedMl,
      minSafeMl = eval.minSafeMl,
      maxSafeMl = eval.maxSafeMl,
      recommendedMg = eval.recommendedMg,
      approxDropsCount = approxDrops,
      isSafetyCeilingApplied = ceilingApplied,
      reminderIntervalHours = 6,
      maxDosesPer24h = if (isIbuprofen) 4 else 5,
      clinicalSourceTagFa = "Lexicomp Pediatric 2026 • UpToDate",
    )
  }

  fun formatPersianDecimal(value: Float): String {
    val formatted = String.format(Locale.US, "%.1f", value).removeSuffix(".0")
    return toPersianDigits(formatted.replace(".", "٫"))
  }

  fun toPersianDigits(input: String): String {
    val map = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val sb = StringBuilder(input.length)
    for (ch in input) {
      if (ch in '0'..'9') sb.append(map[ch - '0']) else sb.append(ch)
    }
    return sb.toString()
  }

  fun buildWhatsAppCaregiverReport(
    childAliasFa: String?,
    result: FeverDoseCalculationResult,
    givenAtTimeLabelFa: String,
    nextSafeTimeLabelFa: String,
  ): String {
    val childName = childAliasFa?.takeIf { it.isNotBlank() } ?: "کودک"
    val mlFa = formatPersianDecimal(result.recommendedMl)
    val kgFa = formatPersianDecimal(result.weightKg)
    val mgFa = toPersianDigits(result.recommendedMg.roundToInt().toString())
    val dropsLine =
      result.approxDropsCount?.let { "\n• معادل تقریبی با قطره‌چکان استاندارد: ${toPersianDigits(it.toString())} قطره" }.orEmpty()
    return buildString {
      appendLine("گزارش دوز تب‌بر «$childName» (اپلیکیشن مانو کید)")
      appendLine("• دارو: ${result.formulation.titleFa}")
      appendLine("• وزن کودک: $kgFa کیلوگرم (${result.ageGroup.labelFa})")
      appendLine("• مقدار داده‌شده با سرنگ مدرج: $mlFa سی‌سی ($mgFa میلی‌گرم)$dropsLine")
      appendLine("• ساعت مصرف این نوبت: $givenAtTimeLabelFa")
      appendLine("• زمان ایمن نوبت بعدی (در صورت نیاز): حدود ساعت $nextSafeTimeLabelFa")
      appendLine("• منبع دوزینگ: ${result.clinicalSourceTagFa}")
      append("• بازبینی علمی: ${ManoSourcesAndClinicalSeed.CLINICAL_REVIEWER_NAME_FA} (شماره نظام پزشکی: ${ManoSourcesAndClinicalSeed.CLINICAL_REVIEWER_REG_ID})")
    }
  }
}
