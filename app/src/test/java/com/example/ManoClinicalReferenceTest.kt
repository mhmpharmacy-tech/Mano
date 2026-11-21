package com.example

import com.example.data.ManoContentRepository
import com.example.data.ManoTextbookEvidence
import com.example.domain.model.*
import com.example.domain.util.*
import org.junit.Assert.*
import org.junit.Test

class ManoClinicalReferenceTest {
  @Test fun publishedWhoTables_matchAllSevenRoundedReferenceColumns() {
    val tables = mapOf("weightBoys" to WhoReferenceData.weightBoys, "weightGirls" to WhoReferenceData.weightGirls,
      "lengthBoys" to WhoReferenceData.lengthBoys, "lengthGirls" to WhoReferenceData.lengthGirls,
      "heightBoys" to WhoReferenceData.heightBoys, "heightGirls" to WhoReferenceData.heightGirls)
    val rows = requireNotNull(javaClass.getResourceAsStream("/who-published-zscores.csv")).bufferedReader().use { it.readLines().drop(1) }
    assertEquals(246, rows.distinct().size)
    for (row in rows) {
      val cells = row.split(',')
      val month = cells[1].toDouble()
      val point = tables.getValue(cells[0]).single { it.ageMonths == month }
      for (z in -3..3) {
        assertEquals("${cells[0]} month=$month z=$z", cells[8 + z].toDouble(),
          WhoGrowthStandardsEngine.valueForZ(point, z.toDouble()), 0.051)
      }
    }
    val height = GrowthMetric.HEIGHT_FOR_AGE
    for (sex in ChildSex.entries) {
      val length = WhoGrowthStandardsEngine.interpolateLms(height, sex, 23.999)
      val standing = WhoGrowthStandardsEngine.interpolateLms(height, sex, 24.0)
      assertEquals(0.7, length.m - standing.m, 0.01)
    }
  }

  @Test fun checksPersianInputsWithoutReinterpretingInvalidPatientData() {
    assertEquals(38.5, ClinicalNumberInput.temperature("۳۸٫۵")!!, 0.0001)
    for (raw in listOf("-38", "38..1", "NaN", "1e2", "∞")) assertNull(ClinicalNumberInput.temperature(raw))
    assertNull(ClinicalNumberInput.durationHours("-1"))
    assertNull(LiquidDoseVisualizerEngine.initialAgeMonthsFromProfile(null))
    assertNull(LiquidDoseVisualizerEngine.initialWeightKgFromProfile(null))
    assertNull(LiquidDoseVisualizerEngine.parseAgeMonthsFromText("-7 ماه"))
    val syrup = LiquidFormulationId.CETIRIZINE_SYRUP_5_5
    assertEquals(2.5, LiquidDoseVisualizerEngine.evaluate(syrup, 36, 13f).recommendedMg, 0.001)
    assertTrue(LiquidDoseVisualizerEngine.evaluate(syrup, null, 13f).isBlocked)
  }

  @Test fun repositoryProvidesPageReferencesAndNewClinicalReviewStatus() {
    val repository = ManoContentRepository()
    assertEquals(8, ManoTextbookEvidence.faqs.size)
    for (faq in ManoTextbookEvidence.faqs) {
      assertTrue(repository.faqs.contains(faq))
      assertEquals(ReviewStatus.NEEDS_CLINICAL_REVIEW, faq.review.status)
      assertTrue(faq.source.title.contains("PDF") || faq.source.url.startsWith("https://www.nice.org.uk/"))
    }
    assertEquals(8, repository.symptomFlows.size)
    for (flow in repository.symptomFlows) {
      assertEquals(ReviewStatus.NEEDS_CLINICAL_REVIEW, flow.review.status)
      assertTrue(flow.sources.any { it.title.contains("PDF") })
    }
  }

  @Test fun eyeNoseAndWetCoughUseTheirOwnReferralRules() {
    val repo = ManoContentRepository()
    for (id in listOf("runny_nose", "wet_cough", "eye")) {
      val flow = repo.symptomFlowById(id)
      assertEquals(id, flow.id)
      assertFalse(TriageEngine.evaluate(flow, AgeBand.NEWBORN, 37.0, 1, emptySet()).showPharmacistOption)
      assertTrue(TriageEngine.evaluate(flow, AgeBand.SCHOOL_6_PLUS, 37.0, 1, emptySet()).showPharmacistOption)
      for (flag in flow.redFlags) {
        val result = TriageEngine.evaluate(flow, AgeBand.SCHOOL_6_PLUS, null, 1, setOf(flag.id))
        assertFalse(result.showPharmacistOption)
        assertEquals(flag.isImmediateEmergency, result.outcome == TriageOutcome.URGENT_EMERGENCY)
      }
    }
    for ((id, hours) in listOf("runny_nose" to 240, "wet_cough" to 336)) {
      val flow = repo.symptomFlowById(id)
      assertTrue(TriageEngine.evaluate(flow, AgeBand.SCHOOL_6_PLUS, null, hours - 1, emptySet()).showPharmacistOption)
      assertFalse(TriageEngine.evaluate(flow, AgeBand.SCHOOL_6_PLUS, null, hours, emptySet()).showPharmacistOption)
    }
  }

  @Test fun crossSymptomFeverAndUnknownFlagsNeverReturnRoutineCare() {
    val flow = ManoContentRepository().symptomFlowById("cough")
    fun assess(age: AgeBand, temp: Double, exact: Int?, flags: Set<String> = emptySet()) =
      TriageEngine.evaluate(flow, age, temp, 1, flags, exact)
    assertEquals(TriageOutcome.URGENT_EMERGENCY, assess(AgeBand.NEWBORN, 38.0, null).outcome)
    for (age in listOf(null, 3, 6)) {
      val result = assess(AgeBand.INFANT_3_12M, 39.0, age)
      assertEquals(TriageOutcome.NEEDS_TIMELY_REVIEW, result.outcome)
      assertFalse(result.showPharmacistOption)
    }
    assertFalse(assess(AgeBand.SCHOOL_6_PLUS, 37.0, null, setOf("unknown")).canShowSelfCare)
  }

  @Test fun extremeTemperaturesOverrideDurationAndBlockRoutineCareInEveryFlow() {
    for (flow in ManoContentRepository().symptomFlows) {
      fun evaluate(temp: Double, duration: Int = 1) =
        TriageEngine.evaluate(flow, AgeBand.PRESCHOOL_3_6Y, temp, duration, emptySet())
      for (temp in listOf(34.0, 34.5, 34.99, 40.5, 43.0)) {
        val result = evaluate(temp, 336)
        assertEquals("rule_extreme_temperature_emergency", result.triggeredRule.id)
        assertFalse(result.canShowSelfCare)
        assertFalse(result.showPharmacistOption)
      }
      for (temp in listOf(35.0, 37.0, 40.49)) {
        assertEquals(TriageOutcome.GENERAL_EDUCATIONAL, evaluate(temp).outcome)
      }
    }
  }
}
