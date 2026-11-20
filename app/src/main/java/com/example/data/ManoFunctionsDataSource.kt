package com.example.data

import com.example.domain.model.AgeBand
import com.example.domain.model.SymptomReferral
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationRequestDemo
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.FollowUpNote
import com.example.domain.model.PublicPharmacyOption
import com.example.domain.model.StaffConsultationCase
import com.example.domain.model.StaffInternalNote
import com.example.domain.util.FeverDoseTimelineEngine
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class UrgentTriageBlockedException(
  val titleFa: String,
  val summaryFa: String,
  val emergencyActionsFa: List<String> = emptyList(),
) : IllegalStateException(summaryFa)

interface ConsultationBackendGateway {
  suspend fun listAcceptingPharmacies(): Result<List<PublicPharmacyOption>>

  suspend fun issueTriagePassAndSubmitCase(
    pharmacyId: String,
    category: ConsultationCategory,
    ageBand: AgeBand,
    questionSummaryFa: String,
    hasUrgentFlag: Boolean,
    consentAccepted: Boolean,
    referral: SymptomReferral? = null,
  ): Result<ConsultationRequestDemo>

  suspend fun listParentConsultations(limit: Int = 25): Result<List<ConsultationRequestDemo>>

  suspend fun getPharmacyInbox(
    pharmacyId: String,
    statusFilter: ConsultationStatus? = null,
    limit: Int = 30,
  ): Result<List<StaffConsultationCase>>

  suspend fun reviewConsultationCase(
    pharmacyId: String,
    caseId: String,
    nextStatus: ConsultationStatus,
    internalNote: String? = null,
    parentReply: String? = null,
    pharmacistAttestation: Boolean = false,
  ): Result<StaffConsultationCase>
}

class FirebaseFunctionsConsultationGateway(
  private val functions: FirebaseFunctions,
) : ConsultationBackendGateway {

  override suspend fun listAcceptingPharmacies(): Result<List<PublicPharmacyOption>> =
    try {
      val result = functions.getHttpsCallable("listAcceptingPharmacies").call().await()
      val items = (result.data as? List<*>) ?: emptyList<Any?>()
      Result.success(parseAcceptingPharmacies(items))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(wrapFunctionsException(e, "دریافت فهرست داروخانه‌های فعال"))
    }

  override suspend fun issueTriagePassAndSubmitCase(
    pharmacyId: String,
    category: ConsultationCategory,
    ageBand: AgeBand,
    questionSummaryFa: String,
    hasUrgentFlag: Boolean,
    consentAccepted: Boolean,
    referral: SymptomReferral?,
  ): Result<ConsultationRequestDemo> {
    return try {
      require(consentAccepted) { "برای ارسال پرسش، تأیید رضایت لازم است." }
      val passPayload = ConsultationPayloadMapper.triagePayload(category, ageBand, questionSummaryFa, hasUrgentFlag, referral)
      val passResult = functions.getHttpsCallable("issueConsultationTriagePass").call(passPayload).await()
      val passMap = (passResult.data as? Map<*, *>)
        ?: return Result.failure(IllegalStateException("پاسخ نامعتبر از سرویس تریاژ سرور دریافت شد."))

      val outcome = passMap["outcome"] as? String
      val eligible = passMap["eligibleForNonUrgentConsultation"] as? Boolean ?: false
      val passId = passMap["triagePassId"] as? String
      if (outcome != "NONURGENT" || !eligible || passId.isNullOrBlank()) {
        val title = (passMap["titleFa"] as? String).orEmpty().ifBlank { "نیاز به ارزیابی فوری پزشکی" }
        val summary = (passMap["summaryFa"] as? String).orEmpty()
          .ifBlank { "بر اساس غربالگری سرور، این وضعیت برای ثبت مشاوره غیرفوری مجاز نیست." }
        val actions = (passMap["emergencyActionsFa"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        return Result.failure(UrgentTriageBlockedException(title, summary, actions))
      }

      val submitPayload =
        hashMapOf<String, Any>(
          "pharmacyId" to pharmacyId,
          "triagePassId" to passId,
          "categoryName" to category.name,
          "consentAccepted" to consentAccepted,
          "consentVersion" to "v1",
        )
      submitPayload.putAll(passPayload)
      val submitResult = functions.getHttpsCallable("submitConsultationCase").call(submitPayload).await()
      val caseMap = (submitResult.data as? Map<*, *>)
        ?: return Result.failure(IllegalStateException("پاسخ نامعتبر از سرویس ثبت پرونده دریافت شد."))
      val parsed = parseParentConsultationView(caseMap)
        ?: return Result.failure(IllegalStateException("ساختار پرونده ثبت‌شده قابل خواندن نبود."))
      Result.success(parsed)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(wrapFunctionsException(e, "ثبت امن درخواست مشاوره"))
    }
  }

  override suspend fun listParentConsultations(limit: Int): Result<List<ConsultationRequestDemo>> =
    try {
      val payload = hashMapOf<String, Any>("limit" to limit)
      val result = functions.getHttpsCallable("listParentConsultations").call(payload).await()
      val list = (result.data as? List<*>) ?: emptyList<Any?>()
      Result.success(list.mapNotNull { parseParentConsultationView(it as? Map<*, *>) })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(wrapFunctionsException(e, "دریافت وضعیت مشاوره‌ها از سرور"))
    }

  override suspend fun getPharmacyInbox(
    pharmacyId: String,
    statusFilter: ConsultationStatus?,
    limit: Int,
  ): Result<List<StaffConsultationCase>> =
    try {
      val payload =
        hashMapOf<String, Any>(
          "pharmacyId" to pharmacyId,
          "limit" to limit,
        )
      if (statusFilter != null) {
        payload["statusFilter"] = statusFilter.name
      }
      val result = functions.getHttpsCallable("getPharmacyInbox").call(payload).await()
      val list = (result.data as? List<*>) ?: emptyList<Any?>()
      Result.success(ConsultationPayloadMapper.parsePharmacyInboxList(list))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(wrapFunctionsException(e, "دریافت صندوق پرونده‌های داروخانه"))
    }

  override suspend fun reviewConsultationCase(
    pharmacyId: String,
    caseId: String,
    nextStatus: ConsultationStatus,
    internalNote: String?,
    parentReply: String?,
    pharmacistAttestation: Boolean,
  ): Result<StaffConsultationCase> =
    try {
      val payload =
        ConsultationPayloadMapper.buildReviewCasePayload(
          pharmacyId = pharmacyId,
          caseId = caseId,
          nextStatus = nextStatus,
          internalNote = internalNote,
          parentReply = parentReply,
          pharmacistAttestation = pharmacistAttestation,
        ).getOrElse { validationErr ->
          return Result.failure(validationErr)
        }
      val result = functions.getHttpsCallable("reviewConsultationCase").call(payload).await()
      val map = result.data as? Map<*, *>
        ?: return Result.failure(IllegalStateException("پاسخ نامعتبر از سرویس ثبت نظر داروساز دریافت شد."))
      val parsed = ConsultationPayloadMapper.parseStaffConsultationCase(map)
        ?: return Result.failure(IllegalStateException("ساختار پرونده به‌روزرسانی‌شده قابل خواندن نبود."))
      Result.success(parsed)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(wrapFunctionsException(e, "ثبت بررسی و پاسخ داروساز"))
    }

  companion object {
    fun mapCategoryToSymptomFlow(category: ConsultationCategory): String =
      ConsultationPayloadMapper.mapCategoryToSymptomFlow(category)

    fun parseAcceptingPharmacies(rawList: List<*>): List<PublicPharmacyOption> =
      ConsultationPayloadMapper.parseAcceptingPharmacies(rawList)

    fun parseParentConsultationView(map: Map<*, *>?): ConsultationRequestDemo? =
      ConsultationPayloadMapper.parseParentConsultationView(map)

    private fun wrapFunctionsException(error: Exception, contextLabel: String): Exception {
      val fnEx = generateSequence(error as Throwable) { it.cause }
        .filterIsInstance<FirebaseFunctionsException>()
        .firstOrNull()
      if (fnEx != null) {
        val codeLabel =
          when (fnEx.code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "لطفاً دوباره با حساب گوگل وارد شوید."
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "دسترسی کاربری برای عملیات داروساز تایید نشد (نیازمند گواهی داروساز در سرور)."
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> "تعداد درخواست‌ها بیش از حد مجاز است؛ چند دقیقه بعد تلاش کنید."
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> fnEx.message ?: "پیش‌شرط بالینی یا رضایت‌نامه احراز نشد."
            FirebaseFunctionsException.Code.UNAVAILABLE, FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
              "ارتباط با سرویس Cloud Functions برقرار نشد."
            else -> fnEx.message ?: "خطا در $contextLabel"
          }
        return IllegalStateException(codeLabel, fnEx)
      }
      return error
    }
  }
}

object ConsultationPayloadMapper {
  fun triagePayload(
    category: ConsultationCategory,
    ageBand: AgeBand,
    question: String,
    urgent: Boolean,
    referral: SymptomReferral?,
  ): Map<String, Any> {
    require(referral == null || (referral.ageBand == ageBand && category == ConsultationCategory.SYMPTOMS))
    return buildMap {
      put("ageBandId", ageBand.id)
      put("symptomFlowId", referral?.flowId ?: mapCategoryToSymptomFlow(category))
      put("questionSummaryFa", question)
      put("hasUrgentFlag", urgent)
      if (referral != null) {
        referral.temperatureCelsius?.let { put("temperatureCelsius", it) }
        referral.durationHours?.let { put("durationHours", it) }
        referral.ageMonthsExact?.let { put("ageMonthsExact", it) }
        put("selectedRedFlagIds", referral.selectedRedFlagIds.toList())
      }
    }
  }

  const val MIN_PARENT_REPLY_LENGTH = 10

  fun mapCategoryToSymptomFlow(category: ConsultationCategory): String =
    when (category) {
      ConsultationCategory.SYMPTOMS -> "fever"
      ConsultationCategory.OTC -> "otc_question"
      ConsultationCategory.SKIN -> "rash"
      ConsultationCategory.SCALP_HAIR -> "general"
      ConsultationCategory.SUPPLEMENT -> "supplement_question"
    }

  fun parseAcceptingPharmacies(rawList: List<*>): List<PublicPharmacyOption> =
    rawList.mapNotNull { item ->
      val map = item as? Map<*, *> ?: return@mapNotNull null
      val id = (map["pharmacyId"] as? String)?.trim().orEmpty()
      val name = (map["displayNameFa"] as? String)?.trim().orEmpty()
      val accepting = map["acceptingCases"] as? Boolean ?: false
      if (id.isBlank() || name.isBlank()) null
      else PublicPharmacyOption(pharmacyId = id, displayNameFa = name, acceptingCases = accepting)
    }

  fun parseParentConsultationView(map: Map<*, *>?): ConsultationRequestDemo? {
    if (map == null) return null
    val caseId = (map["caseId"] as? String)?.trim().orEmpty()
    if (caseId.isBlank()) return null

    val categoryName = map["categoryName"] as? String
    val category = ConsultationCategory.entries.firstOrNull { it.name == categoryName } ?: ConsultationCategory.SYMPTOMS
    val ageBandId = map["ageBandId"] as? String
    val ageBand = AgeBand.entries.firstOrNull { it.id == ageBandId } ?: AgeBand.TODDLER_1_3Y
    val questionSummaryFa = (map["questionSummaryFa"] as? String).orEmpty()
    val statusName = map["status"] as? String
    val status = ConsultationStatus.entries.firstOrNull { it.name == statusName } ?: ConsultationStatus.PENDING

    val createdAtMs = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    val timeLabel = FeverDoseTimelineEngine.formatHourMinute(createdAtMs)

    val parentRespMap = map["parentResponse"] as? Map<*, *>
    val replyText = (parentRespMap?.get("replyTextFa") as? String)?.trim().orEmpty()

    val followUp =
      when {
        status == ConsultationStatus.REVIEWED && replyText.isNotBlank() ->
          FollowUpNote(
            scheduleFa = "پاسخ تأییدشده داروساز",
            guidanceFa = replyText,
          )
        status == ConsultationStatus.RECEIVED ->
          FollowUpNote(
            scheduleFa = "تحویل داروخانه شد",
            guidanceFa = "پرونده توسط داروساز شیفت دریافت شده و در دست بررسی است.",
          )
        status == ConsultationStatus.REVIEWED ->
          FollowUpNote(
            scheduleFa = "بررسی‌شده",
            guidanceFa = "بررسی داروساز ثبت شده است.",
          )
        else ->
          FollowUpNote(
            scheduleFa = "ثبت‌شده با گذرنامه غیرفوری",
            guidanceFa = "در صف پذیرش داروخانه قرار دارد.",
          )
      }

    return ConsultationRequestDemo(
      id = caseId,
      category = category,
      childAgeBand = ageBand,
      questionSummaryFa = questionSummaryFa,
      hasUrgentFlag = false,
      status = status,
      createdAtLabelFa = "ساعت $timeLabel",
      followUp = followUp,
    )
  }

  fun parsePharmacyInboxList(rawList: List<*>): List<StaffConsultationCase> =
    rawList.mapNotNull { parseStaffConsultationCase(it as? Map<*, *>) }

  fun parseStaffConsultationCase(map: Map<*, *>?): StaffConsultationCase? {
    if (map == null) return null
    val caseId = (map["caseId"] as? String)?.trim().orEmpty()
    val pharmacyId = (map["pharmacyId"] as? String)?.trim().orEmpty()
    if (caseId.isBlank() || pharmacyId.isBlank()) return null

    val categoryName = map["categoryName"] as? String
    val category = ConsultationCategory.entries.firstOrNull { it.name == categoryName } ?: ConsultationCategory.SYMPTOMS
    val ageBandId = map["ageBandId"] as? String
    val ageBand = AgeBand.entries.firstOrNull { it.id == ageBandId } ?: AgeBand.TODDLER_1_3Y
    val symptomFlowId = (map["symptomFlowId"] as? String).orEmpty().ifBlank { "general" }
    val questionSummaryFa = (map["questionSummaryFa"] as? String).orEmpty()
    val statusName = map["status"] as? String
    val status = ConsultationStatus.entries.firstOrNull { it.name == statusName } ?: ConsultationStatus.PENDING
    val createdAtMs = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    val timeLabel = FeverDoseTimelineEngine.formatHourMinute(createdAtMs)

    val rawNotes = (map["internalNotes"] as? List<*>) ?: emptyList<Any?>()
    val parsedNotes =
      rawNotes.mapNotNull { rawNote ->
        val noteMap = rawNote as? Map<*, *> ?: return@mapNotNull null
        val noteId = (noteMap["noteId"] as? String)?.trim().orEmpty()
        val noteText = (noteMap["noteText"] as? String)?.trim().orEmpty()
        if (noteId.isBlank() || noteText.isBlank()) return@mapNotNull null
        val fromStatus =
          ConsultationStatus.entries.firstOrNull { it.name == (noteMap["fromStatus"] as? String) } ?: ConsultationStatus.PENDING
        val toStatus =
          ConsultationStatus.entries.firstOrNull { it.name == (noteMap["toStatus"] as? String) } ?: ConsultationStatus.RECEIVED
        StaffInternalNote(
          noteId = noteId,
          authorUid = (noteMap["authorUid"] as? String).orEmpty(),
          noteText = noteText,
          fromStatus = fromStatus,
          toStatus = toStatus,
          createdAtMs = (noteMap["createdAt"] as? Number)?.toLong() ?: createdAtMs,
        )
      }

    val parentRespMap = map["parentResponse"] as? Map<*, *>
    val parentReply = (parentRespMap?.get("replyTextFa") as? String)?.trim()?.takeIf { it.isNotEmpty() }

    return StaffConsultationCase(
      caseId = caseId,
      pharmacyId = pharmacyId,
      category = category,
      childAgeBand = ageBand,
      symptomFlowId = symptomFlowId,
      questionSummaryFa = questionSummaryFa,
      status = status,
      createdAtLabelFa = "ساعت $timeLabel",
      internalNotes = parsedNotes,
      parentReplyFa = parentReply,
      receivedByUid = (map["receivedByUid"] as? String)?.takeIf { it.isNotBlank() },
      reviewedByUid = (map["reviewedByUid"] as? String)?.takeIf { it.isNotBlank() },
    )
  }

  fun buildReviewCasePayload(
    pharmacyId: String,
    caseId: String,
    nextStatus: ConsultationStatus,
    internalNote: String?,
    parentReply: String?,
    pharmacistAttestation: Boolean,
  ): Result<HashMap<String, Any>> {
    val cleanPharmacyId = pharmacyId.trim()
    val cleanCaseId = caseId.trim()
    if (cleanPharmacyId.isBlank() || cleanCaseId.isBlank()) {
      return Result.failure(IllegalArgumentException("شناسه داروخانه و پرونده الزامی است."))
    }
    if (nextStatus == ConsultationStatus.PENDING) {
      return Result.failure(IllegalArgumentException("بازگشت پرونده به وضعیت در انتظار مجاز نیست."))
    }

    val trimmedInternal = internalNote?.trim().orEmpty()
    val trimmedReply = parentReply?.trim().orEmpty()

    if (nextStatus == ConsultationStatus.RECEIVED && trimmedReply.isNotEmpty()) {
      return Result.failure(
        IllegalArgumentException("در مرحله ثبت دریافت (RECEIVED)، ارسال پاسخ نهایی به والد مجاز نیست."),
      )
    }

    if (nextStatus == ConsultationStatus.REVIEWED) {
      if (!pharmacistAttestation) {
        return Result.failure(
          IllegalStateException("ثبت پاسخ نهایی بدون تیک تاییدیه حرفه‌ای داروساز امکان‌پذیر نیست."),
        )
      }
      if (trimmedReply.length < MIN_PARENT_REPLY_LENGTH) {
        return Result.failure(
          IllegalArgumentException("پاسخ علمی برای والد باید حداقل $MIN_PARENT_REPLY_LENGTH کاراکتر باشد."),
        )
      }
    }

    val payload =
      hashMapOf<String, Any>(
        "pharmacyId" to cleanPharmacyId,
        "caseId" to cleanCaseId,
        "nextStatus" to nextStatus.name,
        "pharmacistAttestation" to pharmacistAttestation,
      )
    if (trimmedInternal.isNotEmpty()) {
      payload["internalNote"] = trimmedInternal
    }
    if (nextStatus == ConsultationStatus.REVIEWED) {
      payload["parentReply"] = trimmedReply
    }
    return Result.success(payload)
  }
}
