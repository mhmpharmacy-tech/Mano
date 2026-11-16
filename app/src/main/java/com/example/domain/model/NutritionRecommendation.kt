package com.example.domain.model

import kotlinx.serialization.Serializable

enum class NutritionAppetiteStatus(
  val id: String,
  val labelFa: String,
  val promptHintFa: String,
) {
  NORMAL(
    id = "normal",
    labelFa = "اشتها و گوارش عادی",
    promptHintFa = "اشتهای طبیعی؛ وعده‌های متعادل و متنوع برای رشد سالم",
  ),
  LOW_APPETITE(
    id = "low_appetite",
    labelFa = "کم‌اشتهایی یا بدغذایی",
    promptHintFa = "حجم کم اما پرانرژی و مغذی با ظاهر جذاب و تقسیم به وعده‌های کوچک‌تر",
  ),
  FEVER_RECOVERY(
    id = "fever_recovery",
    labelFa = "دوران تب یا نقاهت",
    promptHintFa = "غذاهای سبک، زودهضم، گرم و ملایم همراه با تأکید بر مایعات",
  ),
  CONSTIPATION(
    id = "constipation",
    labelFa = "تمایل به یبوست",
    promptHintFa = "فیبر ملایم متناسب با سن، میوه‌ها/سبزیجات نرم ملین و مایعات کافی",
  ),
  DIARRHEA_MILD(
    id = "diarrhea_mild",
    labelFa = "معده حساس یا اسهال خفیف",
    promptHintFa = "غذاهای ساده، کم‌چرب، بدون قند زیاد و قابل تحمل برای معده + جبران مایعات",
  ),
}

@Serializable
data class MealSuggestionItem(
  val mealTypeFa: String = "",
  val titleFa: String = "",
  val whySuitableFa: String = "",
  val textureAndPortionFa: String = "",
  val ingredientsFa: List<String> = emptyList(),
  val prepStepsFa: List<String> = emptyList(),
)

@Serializable
data class NutritionPlanResult(
  val id: String = "",
  val planTitleFa: String = "",
  val childContextSummaryFa: String = "",
  val overviewFa: String = "",
  val growthNutritionFocusFa: String = "",
  val hydrationTipFa: String = "",
  val meals: List<MealSuggestionItem> = emptyList(),
  val safetyAvoidListFa: List<String> = emptyList(),
  val doctorFollowUpHintFa: String = "",
  val appetiteStatusLabelFa: String = "",
  val modelName: String = "gemini-3.8-flash",
  val createdAtMs: Long = 0L,
  val createdAtLabelFa: String = "",
)

@Serializable
data class GeminiNutritionResponsePayload(
  val planTitleFa: String = "",
  val overviewFa: String = "",
  val growthNutritionFocusFa: String = "",
  val hydrationTipFa: String = "",
  val meals: List<MealSuggestionItem> = emptyList(),
  val safetyAvoidListFa: List<String> = emptyList(),
  val doctorFollowUpHintFa: String = "",
)
