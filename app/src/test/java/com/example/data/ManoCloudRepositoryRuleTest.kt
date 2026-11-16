package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.AgeBand
import com.example.domain.model.ChildProfileDemo
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationStatus
import com.example.domain.model.SavedSyrupScan
import com.example.domain.model.VaccineRecord
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ManoCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun doseLog_ownerCreateObserveAndDelete_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ManoCloudRepository(firestore, auth)

    val uniqueNote = "یادداشت-${UUID.randomUUID().toString().take(8)}"
    val createResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addDoseLog(
          medicationSlug = "acetaminophen-pediatric",
          medicationNameFa = "استامینوفن کودکان",
          dosageFormFa = "شربت با سرنگ مدرج",
          volumeMlText = "4.5 mL",
          givenAtLabelFa = "۱۵:۱۰",
          noteFa = uniqueNote,
        )
      }
    assertTrue("Expected dose log creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
    val logId = createResult.getOrThrow()
    assertTrue(logId.isNotBlank())

    val logs =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeDoseLogs(uid).first { list -> list.any { it.id == logId } }
      }
    val created = logs.first { it.id == logId }
    assertEquals("4.5 mL", created.volumeMlText)
    assertEquals(uniqueNote, created.noteFa)

    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteDoseLog(logId) }
    assertTrue(deleteResult.isSuccess)
  }

  @Test
  fun childProfile_saveAndObserve_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ManoCloudRepository(firestore, auth)

    val saveResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveChildProfile(
          ChildProfileDemo(
            aliasFa = "کودک ۱",
            birthDateOrAgeFa = "۲ سال",
            ageBand = AgeBand.TODDLER_1_3Y,
            weightKgText = "12.5",
            weightDateFa = "امروز",
            allergiesFa = "ندارد",
            currentMedsFa = "ویتامین D3",
            consentGranted = true,
          )
        )
      }
    assertTrue("Expected profile save to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

    val profile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePrimaryChildProfile(uid).first { it != null }
      }
    assertNotNull(profile)
    assertEquals("کودک ۱", profile?.aliasFa)
  }

  @Test
  fun consultation_createAndAdvanceStatus_enforcesProgression() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ManoCloudRepository(firestore, auth)

    val createRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createConsultation(
          category = ConsultationCategory.OTC,
          ageBand = AgeBand.TODDLER_1_3Y,
          questionSummaryFa = "آیا می‌توانم قطره نرمال سالین همراه تب‌بر بدهم؟",
        )
      }
    assertTrue("Expected consultation create to succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)
    val consId = createRes.getOrThrow()

    val adv1 = withTimeout(DEFAULT_TIMEOUT_MS) { repo.advanceConsultationStatus(consId, ConsultationStatus.PENDING) }
    assertTrue(adv1.isSuccess)
    val adv2 = withTimeout(DEFAULT_TIMEOUT_MS) { repo.advanceConsultationStatus(consId, ConsultationStatus.RECEIVED) }
    assertTrue(adv2.isSuccess)

    val list =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeConsultations(uid).first { items -> items.any { it.id == consId && it.status == ConsultationStatus.REVIEWED } }
      }
    assertEquals(ConsultationStatus.REVIEWED, list.first { it.id == consId }.status)
  }

  @Test
  fun bookmarks_setAndRemove_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ManoCloudRepository(firestore, auth)

    val addRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.setBookmark("fever-care", true) }
    assertTrue(addRes.isSuccess)

    val slugs =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeBookmarkedSlugs(uid).first { "fever-care" in it }
      }
    assertTrue("fever-care" in slugs)

    val removeRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.setBookmark("fever-care", false) }
    assertTrue(removeRes.isSuccess)
  }

  @Test
  fun crossUserRead_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = ManoCloudRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.addDoseLog(
        medicationSlug = "ibuprofen-pediatric",
        medicationNameFa = "ایبوپروفن",
        dosageFormFa = "سوسپانسیون",
        volumeMlText = "3 mL",
        givenAtLabelFa = "۱۸:۰۰",
        noteFa = "",
      ).getOrThrow()
    }

    // Switch to Bob and attempt to observe Alice's doseLogs
    signInTestUser(BOB_EMAIL)
    val bobRepo = ManoCloudRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeDoseLogs(aliceUid).first() }
      fail("Expected cross-user read to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }
  }

  @Test
  fun unauthenticatedObserve_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = ManoCloudRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeDoseLogs("alice_unauth_check").first() }
      fail("Expected unauthenticated read to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }
  }

  @Test
  fun visitDraft_ownerCreateObserveUpdateAndDelete_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ManoCloudRepository(firestore, auth)

    val createRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addVisitDraft(
          titleFa = "پیش‌نویس ویزیت تب",
          symptomContextFa = "تب ۳۹.۲ درجه • بی‌قراری",
          timelineSummaryFa = "۲ ثبت دما • ۱ نوبت استامینوفن",
          draftTextFa = "خلاصه آماده برای پزشک اطفال",
          parentNoteFa = "سوال درباره فاصله دوز بعدی",
          modelName = "gemini-3.8-flash",
        )
      }
    assertTrue("Expected visit draft create to succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)
    val draftId = createRes.getOrThrow()

    val drafts =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeVisitDrafts(uid).first { list -> list.any { it.id == draftId } }
      }
    assertEquals("پیش‌نویس ویزیت تب", drafts.first { it.id == draftId }.titleFa)

    val updateRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.updateVisitDraft(
          draftId = draftId,
          titleFa = "پیش‌نویس ویزیت (ویرایش‌شده)",
          draftTextFa = "متن نهایی ویرایش‌شده برای پزشک",
          parentNoteFa = "سوال نهایی",
        )
      }
    assertTrue("Expected visit draft update to succeed: ${updateRes.exceptionOrNull()}", updateRes.isSuccess)

    val updated =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeVisitDrafts(uid).first { list ->
          list.any { it.id == draftId && it.titleFa == "پیش‌نویس ویزیت (ویرایش‌شده)" }
        }
      }
    assertEquals("متن نهایی ویرایش‌شده برای پزشک", updated.first { it.id == draftId }.draftTextFa)

    // Cross-user Bob cannot observe Alice's visitDrafts
    signInTestUser(BOB_EMAIL)
    val bobRepo = ManoCloudRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeVisitDrafts(uid).first() }
      fail("Expected cross-user visitDrafts read to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    // Owner deletes draft cleanly
    signInTestUser(ALICE_EMAIL)
    val delRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteVisitDraft(draftId) }
    assertTrue(delRes.isSuccess)
  }

  @Test
  fun vaccineRecord_ownerCreateObserveUpdateAndDelete_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ManoCloudRepository(firestore, auth)

    val vaccineId = "2m_pentavalent_opv_rota"
    val createRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.upsertVaccineRecord(
          VaccineRecord(
            id = vaccineId,
            vaccineId = vaccineId,
            customDueDateEpochMs = 1750000000000L,
            administeredDateEpochMs = null,
            isDeferred = false,
            facilityOrLotFa = "مرکز بهداشت شماره ۱",
            noteFa = "برنامه واکسن ۲ ماهگی",
          )
        )
      }
    assertTrue("Expected vaccine record create to succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)

    val records =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeVaccineRecords(uid).first { list -> list.any { it.vaccineId == vaccineId } }
      }
    assertEquals("مرکز بهداشت شماره ۱", records.first { it.vaccineId == vaccineId }.facilityOrLotFa)

    val updateRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.upsertVaccineRecord(
          VaccineRecord(
            id = vaccineId,
            vaccineId = vaccineId,
            customDueDateEpochMs = 1750000000000L,
            administeredDateEpochMs = 1750086400000L,
            isDeferred = false,
            facilityOrLotFa = "مرکز بهداشت شماره ۱ - سری ۴۰۲",
            noteFa = "تزریق انجام شد",
          )
        )
      }
    assertTrue("Expected vaccine record update to succeed: ${updateRes.exceptionOrNull()}", updateRes.isSuccess)

    val updated =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeVaccineRecords(uid).first { list ->
          list.any { it.vaccineId == vaccineId && it.administeredDateEpochMs == 1750086400000L }
        }
      }
    assertEquals("تزریق انجام شد", updated.first { it.vaccineId == vaccineId }.noteFa)

    // Cross-user Bob cannot observe Alice's vaccineRecords
    signInTestUser(BOB_EMAIL)
    val bobRepo = ManoCloudRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeVaccineRecords(uid).first() }
      fail("Expected cross-user vaccineRecords read to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    signInTestUser(ALICE_EMAIL)
    val delRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteVaccineRecord(vaccineId) }
    assertTrue(delRes.isSuccess)
  }

  @Test
  fun syrupScan_ownerCreateObserveUpdateDeleteAndCrossUser_enforcesRules() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = ManoCloudRepository(firestore, auth)
    val scanId = "scan_${UUID.randomUUID().toString().take(8)}"

    val unconfirmed =
      SavedSyrupScan(
        id = scanId,
        drugNameFa = "شربت استامینوفن",
        activeIngredientKey = "ACETAMINOPHEN",
        dosageForm = "SYRUP",
        mgAmount = 120.0,
        perMlAmount = 5.0,
        mgPerMl = 24.0,
        matchedFormulationId = "acetaminophen_syrup_120_5",
        confidenceLevel = "HIGH",
        concentrationDisplayFa = "120 mg / 5 mL",
        safetyNoteFa = "نیاز به تایید",
        confirmedByParent = false,
      )
    var repoThrewUnconfirmed = false
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { repo.upsertSyrupScan(unconfirmed) }
    } catch (_: IllegalArgumentException) {
      repoThrewUnconfirmed = true
    }
    assertTrue("Expected repository to reject confirmedByParent=false", repoThrewUnconfirmed)

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(uid).collection("syrupScans").document("raw_unconfirmed").set(
          mapOf(
            "userId" to uid,
            "drugNameFa" to "شربت تست",
            "activeIngredientKey" to "ACETAMINOPHEN",
            "dosageForm" to "SYRUP",
            "mgAmount" to 120.0,
            "perMlAmount" to 5.0,
            "mgPerMl" to 24.0,
            "matchedFormulationId" to "acetaminophen_syrup_120_5",
            "confidenceLevel" to "HIGH",
            "concentrationDisplayFa" to "120 mg / 5 mL",
            "safetyNoteFa" to "تست",
            "confirmedByParent" to false,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
          )
        ).await()
      }
      fail("Expected raw Firestore rule write with confirmedByParent=false to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    val validScan = unconfirmed.copy(confirmedByParent = true)
    val createRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.upsertSyrupScan(validScan) }
    assertTrue("Expected confirmed syrup scan create to succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)

    val list =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeSyrupScans(uid).first { items -> items.any { it.id == scanId } }
      }
    assertEquals(24.0, list.first { it.id == scanId }.mgPerMl, 0.01)

    val updatedScan = validScan.copy(mgAmount = 160.0, mgPerMl = 32.0, matchedFormulationId = "acetaminophen_syrup_160_5")
    val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.upsertSyrupScan(updatedScan) }
    assertTrue("Expected syrup scan update to succeed: ${updateRes.exceptionOrNull()}", updateRes.isSuccess)

    val updatedList =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeSyrupScans(uid).first { items -> items.any { it.id == scanId && it.mgPerMl == 32.0 } }
      }
    assertEquals("acetaminophen_syrup_160_5", updatedList.first { it.id == scanId }.matchedFormulationId)

    // Cross-user Bob cannot read Alice's syrupScans
    signInTestUser(BOB_EMAIL)
    val bobRepo = ManoCloudRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeSyrupScans(uid).first() }
      fail("Expected cross-user syrupScans read to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    signInTestUser(ALICE_EMAIL)
    val delRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteSyrupScan(scanId) }
    assertTrue(delRes.isSuccess)
  }

  private companion object {
    const val ALICE_EMAIL = "alice.mano@test.com"
    const val BOB_EMAIL = "bob.mano@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
