package com.example

import com.example.data.ConsultationPayloadMapper
import com.example.domain.model.AgeBand
import com.example.domain.model.ConsultationCategory
import com.example.domain.model.ConsultationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ManoFunctionsGatewayTest {

  @Test
  fun parseAcceptingPharmacies_extractsValidEntries() {
    val raw =
      listOf(
        mapOf("pharmacyId" to "pharm_a", "displayNameFa" to "داروخانه مرکزی الف", "acceptingCases" to true),
        mapOf("pharmacyId" to "", "displayNameFa" to "نامعتبر", "acceptingCases" to true),
        mapOf("pharmacyId" to "pharm_b", "displayNameFa" to "داروخانه ب", "acceptingCases" to false),
      )
    val parsed = ConsultationPayloadMapper.parseAcceptingPharmacies(raw)
    assertEquals(2, parsed.size)
    assertEquals("pharm_a", parsed[0].pharmacyId)
    assertEquals(true, parsed[0].acceptingCases)
    assertEquals(false, parsed[1].acceptingCases)
  }

  @Test
  fun parseParentConsultationView_reviewedShowsPharmacistReplyAndStripsInternalMetadata() {
    val reviewedMap =
      mapOf(
        "caseId" to "case_101",
        "pharmacyId" to "pharm_a",
        "categoryName" to "OTC",
        "ageBandId" to "infant_3_12m",
        "questionSummaryFa" to "آیا سرم شستشوی بینی پیش از شیرخوردن مناسب است؟",
        "status" to "REVIEWED",
        "createdAt" to 1_750_000_000_000L,
        "parentResponse" to
          mapOf(
            "replyTextFa" to "قطره سدیم کلراید ۰.۹٪ برای تمیز کردن ملایم بینی مناسب است.",
            "respondedByRole" to "pharmacist",
            "attestationConfirmed" to true,
          ),
      )

    val parsed = ConsultationPayloadMapper.parseParentConsultationView(reviewedMap)
    assertNotNull(parsed)
    assertEquals("case_101", parsed?.id)
    assertEquals(ConsultationCategory.OTC, parsed?.category)
    assertEquals(AgeBand.INFANT_3_12M, parsed?.childAgeBand)
    assertEquals(ConsultationStatus.REVIEWED, parsed?.status)
    assertEquals("پاسخ تأییدشده داروساز", parsed?.followUp?.scheduleFa)
    assertEquals(
      "قطره سدیم کلراید ۰.۹٪ برای تمیز کردن ملایم بینی مناسب است.",
      parsed?.followUp?.guidanceFa,
    )
  }

  @Test
  fun parseParentConsultationView_rejectsMissingCaseId() {
    assertNull(ConsultationPayloadMapper.parseParentConsultationView(mapOf("status" to "PENDING")))
    assertNull(ConsultationPayloadMapper.parseParentConsultationView(null))
  }

  @Test
  fun mapCategoryToSymptomFlow_mapsAllCategoriesDeterministically() {
    assertEquals("fever", ConsultationPayloadMapper.mapCategoryToSymptomFlow(ConsultationCategory.SYMPTOMS))
    assertEquals("otc_question", ConsultationPayloadMapper.mapCategoryToSymptomFlow(ConsultationCategory.OTC))
    assertEquals("rash", ConsultationPayloadMapper.mapCategoryToSymptomFlow(ConsultationCategory.SKIN))
    assertEquals("general", ConsultationPayloadMapper.mapCategoryToSymptomFlow(ConsultationCategory.SCALP_HAIR))
    assertEquals("supplement_question", ConsultationPayloadMapper.mapCategoryToSymptomFlow(ConsultationCategory.SUPPLEMENT))
  }
}
