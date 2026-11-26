package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.model.AgeBand
import com.example.domain.model.SymptomReferral
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationRequestDemo
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.DoctorVisitDraft
import com.example.domain.model.FollowUpNote
import com.example.domain.model.SavedSyrupScan
import com.example.domain.model.PublicPharmacyOption
import com.example.domain.model.StaffAccessStatus
import com.example.domain.model.StaffConsultationCase
import com.example.domain.model.VaccineRecord
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.VaccinationScheduleEngine
import com.example.domain.util.TimelineDosePoint
import com.example.domain.util.TimelineTempPoint
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class ManoCloudRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
  private val functionsGateway: ConsultationBackendGateway? = null,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id)),
    auth = Firebase.auth,
    functionsGateway = defaultFunctionsGateway(),
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("کاربر باید پیش از دسترسی به فضای ابری با حساب گوگل وارد شده باشد.")

  private fun requireFunctionsGateway(): ConsultationBackendGateway =
    functionsGateway
      ?: runCatching { FirebaseFunctionsConsultationGateway(FirebaseFunctions.getInstance("us-central1")) }.getOrNull()
      ?: throw IllegalStateException("سرویس Cloud Functions در این نشست مقداردهی نشده است.")

  // --- CHILD PROFILE ---

  fun observePrimaryChildProfile(userId: String): Flow<ChildProfileDemo?> {
    val docRef = db.collection("users").document(userId).collection("profiles").document(PRIMARY_PROFILE_DOC_ID)
    return docRef
      .snapshots()
      .map { snapshot ->
        if (!snapshot.exists()) null
        else {
          snapshot.toObject(ChildProfileCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let { cloud ->
            ChildProfileDemo(
              aliasFa = cloud.aliasFa,
              birthDateOrAgeFa = cloud.birthDateOrAgeFa,
              ageBand = AgeBand.entries.firstOrNull { it.id == cloud.ageBandId } ?: AgeBand.TODDLER_1_3Y,
              weightKgText = cloud.weightKgText,
              weightDateFa = cloud.weightDateFa,
              allergiesFa = cloud.allergiesFa,
              currentMedsFa = cloud.currentMedsFa,
              consentGranted = cloud.consentGranted,
              ageMonthsExact = LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(
                profile = null,
                birthDateEpochMs = VaccinationScheduleEngine.parseFlexibleDateToEpochMs(cloud.birthDateOrAgeFa),
              ) ?: LiquidDoseVisualizerEngine.parseAgeMonthsFromText(cloud.birthDateOrAgeFa),
            )
          }
        }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(toLog, OperationType.GET, docRef.path)
        }
        throw error
      }
  }

  suspend fun saveChildProfile(profile: ChildProfileDemo): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("profiles").document(PRIMARY_PROFILE_DOC_ID)
    return try {
      withTimeout(6_000L) {
        val existingSnapshot = docRef.get().await()
        if (existingSnapshot.exists()) {
          val updates =
            mapOf(
              "aliasFa" to profile.aliasFa.take(60),
              "birthDateOrAgeFa" to profile.birthDateOrAgeFa.take(60),
              "ageBandId" to profile.ageBand.id,
              "weightKgText" to profile.weightKgText.take(20),
              "weightDateFa" to profile.weightDateFa.take(40),
              "allergiesFa" to profile.allergiesFa.take(300),
              "currentMedsFa" to profile.currentMedsFa.take(300),
              "consentGranted" to profile.consentGranted,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          docRef.update(updates).await()
        } else {
          val payload =
            mapOf(
              "userId" to uid,
              "aliasFa" to profile.aliasFa.take(60),
              "birthDateOrAgeFa" to profile.birthDateOrAgeFa.take(60),
              "ageBandId" to profile.ageBand.id,
              "weightKgText" to profile.weightKgText.take(20),
              "weightDateFa" to profile.weightDateFa.take(40),
              "allergiesFa" to profile.allergiesFa.take(300),
              "currentMedsFa" to profile.currentMedsFa.take(300),
              "consentGranted" to profile.consentGranted,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          docRef.set(payload).await()
        }
      }
      Result.success(Unit)
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
      Result.failure(e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateCareCard(payload: Map<String, Any>): Result<Unit> {
    requireUserId()
    return requireFunctionsGateway().updateCareCard(payload)
  }

  suspend fun deleteChildProfile(): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("profiles").document(PRIMARY_PROFILE_DOC_ID)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- MEDICATION DOSE LOGS ---

  fun observeDoseLogs(userId: String): Flow<List<MedicationDoseLogCloud>> = flow {
    val colRef = db.collection("users").document(userId).collection("doseLogs")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(MedicationDoseLogCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.LIST, colRef.path)
          }
          throw error
        }
    )
  }

  suspend fun addDoseLog(
    medicationSlug: String,
    medicationNameFa: String,
    dosageFormFa: String,
    volumeMlText: String,
    givenAtLabelFa: String,
    noteFa: String,
    administeredAtMs: Long? = null,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("doseLogs").document()
    val payload =
      mapOf(
        "userId" to uid,
        "medicationSlug" to medicationSlug.take(80),
        "medicationNameFa" to medicationNameFa.take(140),
        "dosageFormFa" to dosageFormFa.take(80),
        "volumeMlText" to volumeMlText.take(30),
        "givenAtLabelFa" to givenAtLabelFa.take(60),
        "noteFa" to noteFa.take(300),
        "administeredAtMs" to (administeredAtMs?.takeIf { it > 0 && it <= System.currentTimeMillis() + 60_000L }
          ?: FeverDoseTimelineEngine.resolveDoseTimeMs(givenAtLabelFa)
          ?: System.currentTimeMillis()),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      withTimeout(6_000L) {
        docRef.set(payload).await()
      }
      Result.success(docRef.id)
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
      Result.failure(IllegalStateException("اتصال به سرور در زمان مناسب برقرار نشد (حالت آفلاین)."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteDoseLog(logId: String): Result<Unit> {
    val uid = requireUserId()
    require(logId.isNotBlank())
    val docRef = db.collection("users").document(uid).collection("doseLogs").document(logId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- FEVER TEMPERATURE LOGS ---

  fun observeFeverLogs(userId: String): Flow<List<FeverLogCloud>> = flow {
    val colRef = db.collection("users").document(userId).collection("feverLogs")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(FeverLogCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.LIST, colRef.path)
          }
          throw error
        }
    )
  }

  suspend fun addFeverLog(
    tempCelsius: Double,
    siteFa: String,
    recordedAtLabelFa: String,
    caregiverFa: String,
    noteFa: String,
  ): Result<String> {
    val uid = requireUserId()
    val safeTemp = tempCelsius.coerceIn(34.0, 43.0)
    val docRef = db.collection("users").document(uid).collection("feverLogs").document()
    val payload =
      mapOf(
        "userId" to uid,
        "tempCelsius" to safeTemp,
        "siteFa" to siteFa.take(40).ifBlank { "زیربغلی" },
        "recordedAtLabelFa" to recordedAtLabelFa.take(60).ifBlank { "هم‌اکنون" },
        "caregiverFa" to caregiverFa.take(40).ifBlank { "والد" },
        "noteFa" to noteFa.take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteFeverLog(logId: String): Result<Unit> {
    val uid = requireUserId()
    require(logId.isNotBlank())
    val docRef = db.collection("users").document(uid).collection("feverLogs").document(logId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- PHARMACIST CONSULTATIONS ---

  fun observeConsultations(userId: String): Flow<List<ConsultationRequestDemo>> = flow {
    val colRef = db.collection("users").document(userId).collection("consultations")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(PharmacistConsultationCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            .map { cloud ->
              val status = ConsultationStatus.entries.firstOrNull { it.name == cloud.status } ?: ConsultationStatus.PENDING
              val followUp =
                when (status) {
                  ConsultationStatus.PENDING ->
                    FollowUpNote(
                      scheduleFa = "ثبت‌شده در فضای ابری",
                      guidanceFa = "در صف بررسی؛ ارسال واقعی به داروساز پس از اتصال مرکز دارویی فعال می‌شود.",
                    )
                  ConsultationStatus.RECEIVED ->
                    FollowUpNote(
                      scheduleFa = "دریافت شده در صف بررسی",
                      guidanceFa = "درخواست با موفقیت ثبت شد.",
                    )
                  ConsultationStatus.REVIEWED ->
                    FollowUpNote(
                      scheduleFa = "بررسی‌شده",
                      guidanceFa = "پاسخ نهایی تنها پس از بررسی داروساز تاییدشده نمایش داده می‌شود.",
                    )
                }
              ConsultationRequestDemo(
                id = cloud.id,
                category = ConsultationCategory.entries.firstOrNull { it.name == cloud.categoryName } ?: ConsultationCategory.SYMPTOMS,
                childAgeBand = AgeBand.entries.firstOrNull { it.id == cloud.ageBandId } ?: AgeBand.TODDLER_1_3Y,
                questionSummaryFa = cloud.questionSummaryFa,
                hasUrgentFlag = cloud.hasUrgentFlag,
                status = status,
                createdAtLabelFa = "همگام‌سازی‌شده در حساب گوگل",
                followUp = followUp,
              )
            }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.LIST, colRef.path)
          }
          throw error
        }
    )
  }

  suspend fun fetchAcceptingPharmaciesFromServer(): Result<List<PublicPharmacyOption>> {
    requireUserId()
    return requireFunctionsGateway().listAcceptingPharmacies()
  }

  suspend fun fetchParentConsultationsFromServer(limit: Int = 25): Result<List<ConsultationRequestDemo>> {
    requireUserId()
    return requireFunctionsGateway().listParentConsultations(limit)
  }

  suspend fun submitTrustedConsultationViaServer(
    preferredPharmacyId: String?,
    category: ConsultationCategory,
    ageBand: AgeBand,
    questionSummaryFa: String,
    hasUrgentFlag: Boolean,
    consentAccepted: Boolean,
    referral: SymptomReferral? = null,
  ): Result<ConsultationRequestDemo> {
    requireUserId()
    val gateway = requireFunctionsGateway()
    val resolvedPharmacyId =
      if (!preferredPharmacyId.isNullOrBlank()) {
        preferredPharmacyId.trim()
      } else {
        val listRes = gateway.listAcceptingPharmacies()
        val firstActive = listRes.getOrNull()?.firstOrNull { it.acceptingCases }?.pharmacyId
        firstActive
          ?: return Result.failure(
            IllegalStateException(
              "هیچ داروخانه فعال و آماده پذیرش از سرور دریافت نشد؛ پس از استقرار Functions و اجرای اسکریپت راه‌اندازی داروخانه دوباره تلاش کنید.",
            ),
          )
      }

    return gateway.issueTriagePassAndSubmitCase(
      pharmacyId = resolvedPharmacyId,
      category = category,
      ageBand = ageBand,
      questionSummaryFa = questionSummaryFa,
      hasUrgentFlag = hasUrgentFlag,
      consentAccepted = consentAccepted,
      referral = referral,
    )
  }

  suspend fun fetchPharmacistStaffClaims(forceRefresh: Boolean = false): StaffAccessStatus {
    val user = auth.currentUser ?: return StaffAccessStatus()
    return try {
      val tokenResult = user.getIdToken(forceRefresh).await()
      val claims = tokenResult.claims
      val verified = claims["manoStaffVerified"] as? Boolean ?: false
      val role = claims["manoStaffRole"] as? String
      val pharmacyId = (claims["manoPharmacyId"] as? String)?.takeIf { it.isNotBlank() }
      StaffAccessStatus(
        isVerifiedPharmacist = verified && role == "pharmacist" && pharmacyId != null,
        role = role,
        pharmacyId = pharmacyId,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      StaffAccessStatus()
    }
  }

  suspend fun fetchPharmacyInbox(
    pharmacyId: String,
    statusFilter: ConsultationStatus? = null,
    limit: Int = 30,
  ): Result<List<StaffConsultationCase>> {
    requireUserId()
    return requireFunctionsGateway().getPharmacyInbox(
      pharmacyId = pharmacyId.trim(),
      statusFilter = statusFilter,
      limit = limit,
    )
  }

  suspend fun reviewStaffConsultationCase(
    pharmacyId: String,
    caseId: String,
    nextStatus: ConsultationStatus,
    internalNote: String?,
    parentReply: String?,
    pharmacistAttestation: Boolean,
  ): Result<StaffConsultationCase> {
    requireUserId()
    return requireFunctionsGateway().reviewConsultationCase(
      pharmacyId = pharmacyId.trim(),
      caseId = caseId.trim(),
      nextStatus = nextStatus,
      internalNote = internalNote,
      parentReply = parentReply,
      pharmacistAttestation = pharmacistAttestation,
    )
  }

  @Deprecated("Legacy direct-Firestore helper retained for rule compatibility tests only; UI uses submitTrustedConsultationViaServer.")
  suspend fun createConsultation(
    category: ConsultationCategory,
    ageBand: AgeBand,
    questionSummaryFa: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("consultations").document()
    val payload =
      mapOf(
        "userId" to uid,
        "categoryName" to category.name,
        "ageBandId" to ageBand.id,
        "questionSummaryFa" to questionSummaryFa.take(600),
        "hasUrgentFlag" to false,
        "consentAccepted" to true,
        "status" to ConsultationStatus.PENDING.name,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  @Deprecated("Workflow transitions must execute server-side in reviewConsultationCase.")
  suspend fun advanceConsultationStatus(consultationId: String, currentStatus: ConsultationStatus): Result<Unit> {
    val uid = requireUserId()
    val nextStatus =
      when (currentStatus) {
        ConsultationStatus.PENDING -> ConsultationStatus.RECEIVED
        ConsultationStatus.RECEIVED -> ConsultationStatus.REVIEWED
        ConsultationStatus.REVIEWED -> return Result.success(Unit)
      }
    val docRef = db.collection("users").document(uid).collection("consultations").document(consultationId)
    return try {
      docRef
        .update(
          mapOf(
            "status" to nextStatus.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- ARTICLE BOOKMARKS ---

  fun observeBookmarkedSlugs(userId: String): Flow<Set<String>> = flow {
    val colRef = db.collection("users").document(userId).collection("bookmarks")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(ArticleBookmarkCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .map { it.articleSlug }
            .filter { it.isNotBlank() }
            .toSet()
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.LIST, colRef.path)
          }
          throw error
        }
    )
  }

  suspend fun setBookmark(articleSlug: String, bookmarked: Boolean): Result<Unit> {
    val uid = requireUserId()
    require(articleSlug.isNotBlank())
    val docRef = db.collection("users").document(uid).collection("bookmarks").document(articleSlug)
    return try {
      if (bookmarked) {
        val payload =
          mapOf(
            "userId" to uid,
            "articleSlug" to articleSlug.take(100),
            "createdAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      } else {
        docRef.delete().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (bookmarked) OperationType.CREATE else OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- DOCTOR VISIT DRAFTS ---

  fun observeVisitDrafts(userId: String): Flow<List<DoctorVisitDraft>> = flow {
    val colRef = db.collection("users").document(userId).collection("visitDrafts")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(DoctorVisitDraftCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            .map { cloud ->
              val createdMs = cloud.createdAt?.toDate()?.time ?: System.currentTimeMillis()
              DoctorVisitDraft(
                id = cloud.id,
                titleFa = cloud.titleFa,
                symptomContextFa = cloud.symptomContextFa,
                timelineSummaryFa = cloud.timelineSummaryFa,
                draftTextFa = cloud.draftTextFa,
                parentNoteFa = cloud.parentNoteFa,
                modelName = cloud.modelName,
                createdAtMs = createdMs,
                createdAtLabelFa = FeverDoseTimelineEngine.formatHourMinute(createdMs),
                isSyncedCloud = true,
              )
            }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.LIST, colRef.path)
          }
          throw error
        }
    )
  }

  suspend fun addVisitDraft(
    titleFa: String,
    symptomContextFa: String,
    timelineSummaryFa: String,
    draftTextFa: String,
    parentNoteFa: String,
    modelName: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("visitDrafts").document()
    val payload =
      mapOf(
        "userId" to uid,
        "titleFa" to titleFa.trim().ifBlank { "پیش‌نویس ویزیت پزشک" }.take(120),
        "symptomContextFa" to symptomContextFa.trim().take(300),
        "timelineSummaryFa" to timelineSummaryFa.trim().ifBlank { "خلاصه خط زمانی ثبت‌شده" }.take(800),
        "draftTextFa" to draftTextFa.trim().take(6000),
        "parentNoteFa" to parentNoteFa.trim().take(500),
        "modelName" to modelName.trim().ifBlank { "gemini-3.8-flash" }.take(60),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateVisitDraft(
    draftId: String,
    titleFa: String,
    draftTextFa: String,
    parentNoteFa: String,
  ): Result<Unit> {
    val uid = requireUserId()
    require(draftId.isNotBlank())
    val docRef = db.collection("users").document(uid).collection("visitDrafts").document(draftId)
    val updates =
      mapOf(
        "titleFa" to titleFa.trim().ifBlank { "پیش‌نویس ویزیت پزشک" }.take(120),
        "draftTextFa" to draftTextFa.trim().take(6000),
        "parentNoteFa" to parentNoteFa.trim().take(500),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteVisitDraft(draftId: String): Result<Unit> {
    val uid = requireUserId()
    require(draftId.isNotBlank())
    val docRef = db.collection("users").document(uid).collection("visitDrafts").document(draftId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- VACCINE RECORDS ---

  fun observeVaccineRecords(userId: String): Flow<List<VaccineRecord>> = flow {
    val colRef = db.collection("users").document(userId).collection("vaccineRecords")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(VaccineRecordCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .map { cloud ->
              VaccineRecord(
                id = cloud.id,
                vaccineId = cloud.vaccineId.ifBlank { cloud.id },
                customDueDateEpochMs = cloud.customDueDateEpochMs.takeIf { it > 0L },
                administeredDateEpochMs = cloud.administeredDateEpochMs.takeIf { it > 0L },
                isDeferred = cloud.isDeferred,
                facilityOrLotFa = cloud.facilityOrLotFa,
                noteFa = cloud.noteFa,
                updatedAtMs = cloud.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
                isSyncedCloud = true,
              )
            }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.LIST, colRef.path)
          }
          throw error
        }
    )
  }

  suspend fun upsertVaccineRecord(record: VaccineRecord): Result<Unit> {
    val uid = requireUserId()
    val safeVaccineId = record.vaccineId.trim()
    require(safeVaccineId.isNotBlank())
    val docRef = db.collection("users").document(uid).collection("vaccineRecords").document(safeVaccineId)
    val dueMs = record.customDueDateEpochMs?.coerceAtLeast(0L) ?: 0L
    val adminMs = record.administeredDateEpochMs?.coerceAtLeast(0L) ?: 0L
    val facility = record.facilityOrLotFa.trim().take(120)
    val note = record.noteFa.trim().take(300)
    return try {
      val existingSnapshot = docRef.get().await()
      if (existingSnapshot.exists()) {
        val updates =
          mapOf(
            "customDueDateEpochMs" to dueMs,
            "administeredDateEpochMs" to adminMs,
            "isDeferred" to record.isDeferred,
            "facilityOrLotFa" to facility,
            "noteFa" to note,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updates).await()
      } else {
        val payload =
          mapOf(
            "userId" to uid,
            "vaccineId" to safeVaccineId,
            "customDueDateEpochMs" to dueMs,
            "administeredDateEpochMs" to adminMs,
            "isDeferred" to record.isDeferred,
            "facilityOrLotFa" to facility,
            "noteFa" to note,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteVaccineRecord(vaccineId: String): Result<Unit> {
    val uid = requireUserId()
    require(vaccineId.isNotBlank())
    val docRef = db.collection("users").document(uid).collection("vaccineRecords").document(vaccineId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- SYRUP CONCENTRATION SCANS ---

  fun observeSyrupScans(userId: String): Flow<List<SavedSyrupScan>> = flow {
    val colRef = db.collection("users").document(userId).collection("syrupScans")
    emitAll(
      colRef
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(SyrupScanCloud::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            .map { cloud ->
              SavedSyrupScan(
                id = cloud.id,
                drugNameFa = cloud.drugNameFa,
                activeIngredientKey = cloud.activeIngredientKey,
                dosageForm = cloud.dosageForm,
                mgAmount = cloud.mgAmount,
                perMlAmount = cloud.perMlAmount,
                mgPerMl = cloud.mgPerMl,
                matchedFormulationId = cloud.matchedFormulationId,
                confidenceLevel = cloud.confidenceLevel,
                concentrationDisplayFa = cloud.concentrationDisplayFa,
                safetyNoteFa = cloud.safetyNoteFa,
                confirmedByParent = cloud.confirmedByParent,
                createdAtMs = cloud.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                updatedAtMs = cloud.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
                isSyncedCloud = true,
              )
            }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.LIST, colRef.path)
          }
          throw error
        }
    )
  }

  suspend fun upsertSyrupScan(scan: SavedSyrupScan): Result<String> {
    val uid = requireUserId()
    require(scan.confirmedByParent) { "اسکن شربت تنها پس از تأیید والد قابل ذخیره ابری است." }
    val safeMg = scan.mgAmount.coerceIn(1.0, 2000.0)
    val safePerMl = scan.perMlAmount.coerceIn(0.5, 100.0)
    val safeMgPerMl = (safeMg / safePerMl).coerceIn(0.01, 1000.0)
    val safeDrugName = scan.drugNameFa.trim().ifBlank { "داروی مایع کودک" }.take(140)
    val safeConcentrationDisplay = scan.concentrationDisplayFa.trim().ifBlank { "${safeMg.toInt()} mg / ${safePerMl.toInt()} mL" }.take(80)
    val safeSafetyNote = scan.safetyNoteFa.trim().take(400)
    val safeIngredient =
      scan.activeIngredientKey.takeIf { it in setOf("ACETAMINOPHEN", "IBUPROFEN", "CETIRIZINE", "UNKNOWN") } ?: "UNKNOWN"
    val safeForm =
      scan.dosageForm.takeIf { it in setOf("DROPS", "SYRUP", "SUSPENSION", "UNKNOWN") } ?: "UNKNOWN"
    val safeMatchedId =
      scan.matchedFormulationId.takeIf {
        it in setOf(
          "acetaminophen_drops_100",
          "acetaminophen_syrup_120_5",
          "acetaminophen_syrup_160_5",
          "ibuprofen_syrup_100_5",
          "cetirizine_syrup_5_5",
          "CUSTOM_UNVERIFIED",
        )
      } ?: "CUSTOM_UNVERIFIED"
    val safeConfidence =
      scan.confidenceLevel.takeIf { it in setOf("HIGH", "MEDIUM", "LOW") } ?: "MEDIUM"

    val colRef = db.collection("users").document(uid).collection("syrupScans")
    val safeId = scan.id.trim().takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$")) }
    val docRef = if (safeId != null) colRef.document(safeId) else colRef.document()

    return try {
      val existingSnapshot = if (safeId != null) docRef.get().await() else null
      if (existingSnapshot != null && existingSnapshot.exists()) {
        val updates =
          mapOf(
            "drugNameFa" to safeDrugName,
            "activeIngredientKey" to safeIngredient,
            "dosageForm" to safeForm,
            "mgAmount" to safeMg,
            "perMlAmount" to safePerMl,
            "mgPerMl" to safeMgPerMl,
            "matchedFormulationId" to safeMatchedId,
            "confidenceLevel" to safeConfidence,
            "concentrationDisplayFa" to safeConcentrationDisplay,
            "safetyNoteFa" to safeSafetyNote,
            "confirmedByParent" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updates).await()
      } else {
        val payload =
          mapOf(
            "userId" to uid,
            "drugNameFa" to safeDrugName,
            "activeIngredientKey" to safeIngredient,
            "dosageForm" to safeForm,
            "mgAmount" to safeMg,
            "perMlAmount" to safePerMl,
            "mgPerMl" to safeMgPerMl,
            "matchedFormulationId" to safeMatchedId,
            "confidenceLevel" to safeConfidence,
            "concentrationDisplayFa" to safeConcentrationDisplay,
            "safetyNoteFa" to safeSafetyNote,
            "confirmedByParent" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteSyrupScan(scanId: String): Result<Unit> {
    val uid = requireUserId()
    require(scanId.isNotBlank())
    val docRef = db.collection("users").document(uid).collection("syrupScans").document(scanId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun migrateOfflineRecords(
    localProfile: ChildProfileDemo?,
    localFevers: List<TimelineTempPoint>,
    localDoses: List<TimelineDosePoint>,
    cloudHasProfile: Boolean,
  ): Boolean {
    if (auth.currentUser == null) return false
    var allSucceeded = true
    if (!cloudHasProfile && localProfile != null && localProfile.consentGranted && localProfile.aliasFa.isNotBlank()) {
      if (saveChildProfile(localProfile).isFailure) {
        allSucceeded = false
      }
    }
    localFevers.forEach { fever ->
      if (
        addFeverLog(
          tempCelsius = fever.tempCelsius.toDouble(),
          siteFa = fever.siteFa,
          recordedAtLabelFa = fever.timeLabelFa,
          caregiverFa = fever.caregiverFa,
          noteFa = fever.noteFa,
        ).isFailure
      ) {
        allSucceeded = false
      }
    }
    localDoses.forEach { dose ->
      if (
        addDoseLog(
          medicationSlug = dose.medicationSlug.ifBlank { "custom-medication" },
          medicationNameFa = dose.medicationNameFa.ifBlank { "دارو" },
          dosageFormFa = dose.dosageFormFa.ifBlank { "شربت" },
          volumeMlText = dose.volumeMlText.ifBlank { "-" },
          givenAtLabelFa = dose.givenAtLabelFa.ifBlank { "هم‌اکنون" },
          noteFa = dose.noteFa,
          administeredAtMs = dose.timestampMs,
        ).isFailure
      ) {
        allSucceeded = false
      }
    }
    return allSucceeded
  }

  companion object {
    const val PRIMARY_PROFILE_DOC_ID = "primary"

    private fun defaultFunctionsGateway(): ConsultationBackendGateway? =
      try {
        FirebaseFunctionsConsultationGateway(FirebaseFunctions.getInstance("us-central1"))
      } catch (_: Exception) {
        null
      }
  }
}
