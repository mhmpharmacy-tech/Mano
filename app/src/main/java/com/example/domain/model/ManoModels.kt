package com.example.domain.model

import androidx.annotation.DrawableRes
import com.example.R
import kotlinx.serialization.Serializable

@Serializable
enum class AgeBand(val id: String, val labelFa: String, val rangeFa: String) {
  NEWBORN("newborn", "نوزاد", "۰ تا ۳ ماه"),
  INFANT_3_12M("infant_3_12m", "۳ تا ۱۲ ماه", "۳ تا ۱۲ ماه"),
  TODDLER_1_3Y("toddler_1_3y", "۱ تا ۳ سال", "۱ تا ۳ سال"),
  PRESCHOOL_3_6Y("preschool_3_6y", "۳ تا ۶ سال", "۳ تا ۶ سال"),
  SCHOOL_6_PLUS("school_6_plus", "۶ سال به بالا", "۶ سال به بالا"),
}

enum class ReviewStatus(val labelFa: String) {
  DRAFT("پیش‌نویس"),
  NEEDS_CLINICAL_REVIEW("نیازمند تأیید علمی"),
  APPROVED("تأیید‌شده"),
  RETIRED("بایگانی‌شده"),
}

enum class TriageOutcome(val labelFa: String) {
  URGENT_EMERGENCY("اقدام اورژانسی فوری"),
  NEEDS_TIMELY_REVIEW("نیاز به بررسی پزشک"),
  GENERAL_EDUCATIONAL("راهنمای آموزشی عمومی"),
}

enum class MascotPose(val labelFa: String, @param:DrawableRes val drawableRes: Int) {
  WELCOME("خوشامد", R.drawable.mano_pose_welcome),
  EXPLAIN("راهنمایی", R.drawable.mano_pose_explain),
  LISTEN("شنیدن", R.drawable.mano_pose_listen),
  CAUTION("توجه و هشدار", R.drawable.mano_pose_caution),
  SAVED("ثبت شد", R.drawable.mano_pose_saved),
  WAIT("در انتظار بررسی", R.drawable.mano_pose_wait),
}

data class SourceReference(
  val id: String,
  val title: String,
  val publisher: String,
  val url: String,
  val publishedOrUpdated: String,
)

data class ClinicalReviewMeta(
  val status: ReviewStatus,
  val lastReviewedDate: String,
  val nextReviewDate: String,
  val reviewerIdentity: String?, // Null unless a real named reviewer exists
  val notes: String,
)

enum class TopicCategory(val id: String, val labelFa: String) {
  SYMPTOMS("symptoms", "علائم و موارد فوری"),
  MEDICINES("medicines", "داروها و مصرف ایمن"),
  NUTRITION("nutrition", "تغذیه و رشد"),
  SKIN("skin", "پوست و پوشک"),
  SKILLS_DEVICES("devices", "مهارت‌های والدین و وسایل"),
}

data class Article(
  val slug: String,
  val titleFa: String,
  val category: TopicCategory,
  val ageBands: List<AgeBand>,
  val shortAnswerFa: String,
  val ageRelevanceFa: String,
  val selfCarePointsFa: List<String>,
  val redFlagsFa: List<String>,
  val caregiverMistakesFa: List<String>,
  val nextStepCtaLabelFa: String,
  val nextStepRouteKey: String,
  val relatedSlugs: List<String>,
  val sources: List<SourceReference>,
  val review: ClinicalReviewMeta,
  val isHighRisk: Boolean = false,
)

data class FaqItem(
  val id: String,
  val questionFa: String,
  val category: TopicCategory,
  val ageBands: List<AgeBand>,
  val tags: List<String>,
  val summaryFa: String,
  val whatToDoFa: List<String>,
  val whenToSeeDoctorFa: List<String>,
  val source: SourceReference,
  val review: ClinicalReviewMeta,
  val isHighRisk: Boolean = false,
)

data class RedFlagOption(
  val id: String,
  val labelFa: String,
  val isImmediateEmergency: Boolean,
)

data class SymptomFlow(
  val id: String,
  val titleFa: String,
  val subtitleFa: String,
  val relatedArticleSlug: String,
  val asksTemperature: Boolean = false,
  val redFlags: List<RedFlagOption>,
  val educationalStepsFa: List<String>,
  val escalationCriteriaFa: List<String>,
  val review: ClinicalReviewMeta,
  val sources: List<SourceReference>,
)

data class TriageRule(
  val id: String,
  val descriptionFa: String,
  val outcome: TriageOutcome,
  val reviewStatus: ReviewStatus,
)

data class TriageEvaluationResult(
  val outcome: TriageOutcome,
  val titleFa: String,
  val summaryFa: String,
  val immediateActionsFa: List<String>,
  val escalationCriteriaFa: List<String>,
  val relatedArticleSlug: String,
  val canShowSelfCare: Boolean,
  val triggeredRule: TriageRule,
  val showPharmacistOption: Boolean,
)

data class DoseRule(
  val id: String,
  val medicationSlug: String,
  val isEnabled: Boolean = false, // Disabled until clinical & pharmacist sign-off
  val requiredInputsFa: List<String>,
  val blockingReasonsFa: List<String>,
  val reviewStatus: ReviewStatus = ReviewStatus.NEEDS_CLINICAL_REVIEW,
)

enum class MedicationSafetyCategory(val id: String, val labelFa: String) {
  FEVER_PAIN("fever_pain", "تب و درد"),
  HYDRATION_GI("hydration_gi", "گوارش و کم‌آبی"),
  RESPIRATORY_ALLERGY("respiratory_allergy", "تنفس و حساسیت"),
  ANTIBIOTICS("antibiotics", "آنتی‌بیوتیک‌ها"),
  VITAMINS_SUPPLEMENTS("vitamins_supplements", "ویتامین و مکمل"),
  TOPICAL_SKIN("topical_skin", "پوست و موضعی"),
}

data class MedicationMonograph(
  val slug: String,
  val nameFa: String,
  val genericNameEn: String,
  val treatsFa: String,
  val labeledMinimumAgeFa: String,
  val dosageFormAlertFa: String,
  val timingAndPrecautionsFa: List<String>,
  val commonSideEffectsFa: List<String>,
  val interactionCautionsFa: List<String>,
  val whatNotToGiveFa: List<String>,
  val refusalTipsFa: List<String>,
  val whenToSeekAdviceFa: List<String>,
  val lockedDoseRule: DoseRule,
  val sources: List<SourceReference>,
  val review: ClinicalReviewMeta,
  val category: MedicationSafetyCategory = MedicationSafetyCategory.FEVER_PAIN,
  val aliasesSearchTermsFa: List<String> = emptyList(),
  val keySafetySummaryFa: String = "",
  val storageAndShelfLifeFa: String = "",
  val isHighRiskWarning: Boolean = false,
)

data class MilestoneItem(
  val id: String,
  val ageBand: AgeBand,
  val domainFa: String, // حرکتی، گفتاری، اجتماعی، شناختی
  val descriptionFa: String,
  val watchOutFa: String,
  val source: SourceReference,
)

data class DeviceGuide(
  val id: String,
  val titleFa: String,
  val subtitleFa: String,
  val distinctionNoteFa: String,
  val stepsFa: List<String>,
  val cautionNotesFa: List<String>,
  val review: ClinicalReviewMeta,
  val sources: List<SourceReference>,
)

@Serializable
enum class ConsultationCategory(val labelFa: String) {
  SYMPTOMS("علائم کودک"),
  OTC("داروی بدون نسخه (OTC)"),
  SKIN("مراقبت پوست"),
  SCALP_HAIR("پوست و موی سر"),
  SUPPLEMENT("مکمل و ویتامین"),
}

@Serializable
enum class ConsultationStatus(val labelFa: String) {
  PENDING("در انتظار دریافت"),
  RECEIVED("دریافت شده"),
  REVIEWED("بررسی شده"),
}

data class ConsentRecord(
  val accepted: Boolean,
  val purposeSummaryFa: String,
  val timestampLabelFa: String,
)

@Serializable
data class FollowUpNote(
  val scheduleFa: String,
  val guidanceFa: String,
)

@Serializable
data class ConsultationRequestDemo(
  val id: String,
  val category: ConsultationCategory,
  val childAgeBand: AgeBand,
  val questionSummaryFa: String,
  val hasUrgentFlag: Boolean,
  val status: ConsultationStatus,
  val createdAtLabelFa: String,
  val followUp: FollowUpNote? = null,
)

data class PublicPharmacyOption(
  val pharmacyId: String,
  val displayNameFa: String,
  val acceptingCases: Boolean,
)

data class PharmacistCaseDemo(
  val id: String,
  val aliasFa: String,
  val category: ConsultationCategory,
  val ageLabelFa: String,
  val riskFlagFa: String?,
  val status: ConsultationStatus,
  val createdDateFa: String,
  val parentNoteFa: String,
  val suggestedReplyFa: String,
  val followUpScheduleFa: String,
  val reviewStatus: ReviewStatus,
  val referenceTitle: String,
)

@Serializable
data class ChildProfileDemo(
  val aliasFa: String,
  val birthDateOrAgeFa: String,
  val ageBand: AgeBand,
  val weightKgText: String,
  val weightDateFa: String,
  val allergiesFa: String,
  val currentMedsFa: String,
  val consentGranted: Boolean,
  val ageMonthsExact: Int? = null,
)

data class StaffInternalNote(
  val noteId: String,
  val authorUid: String,
  val noteText: String,
  val fromStatus: ConsultationStatus,
  val toStatus: ConsultationStatus,
  val createdAtMs: Long,
)

data class StaffConsultationCase(
  val caseId: String,
  val pharmacyId: String,
  val category: ConsultationCategory,
  val childAgeBand: AgeBand,
  val symptomFlowId: String,
  val questionSummaryFa: String,
  val status: ConsultationStatus,
  val createdAtLabelFa: String,
  val internalNotes: List<StaffInternalNote> = emptyList(),
  val parentReplyFa: String? = null,
  val receivedByUid: String? = null,
  val reviewedByUid: String? = null,
)

data class StaffAccessStatus(
  val isVerifiedPharmacist: Boolean = false,
  val role: String? = null,
  val pharmacyId: String? = null,
)

