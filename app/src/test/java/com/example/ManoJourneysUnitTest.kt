package com.example

import com.example.data.ManoContentRepository
import com.example.domain.model.AgeBand
import com.example.domain.model.TriageOutcome
import com.example.domain.util.LiquidDoseVisualizerEngine
import com.example.domain.util.LiquidFormulationId
import com.example.domain.util.MedicationOverlapEngine
import com.example.domain.util.PersianNormalizer
import com.example.domain.util.TriageEngine
import com.example.ui.components.AlertSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManoJourneysUnitTest {

  private val repo = ManoContentRepository()

  @Test
  fun journeyA_twoMonthOldWith38Fever_triggersUrgentBypassAndNoDoseOutput() {
    val feverFlow = repo.symptomFlowById("fever")
    val result =
      TriageEngine.evaluate(
        flow = feverFlow,
        ageBand = AgeBand.NEWBORN,
        temperatureCelsius = 38.0,
        durationHours = 2,
        selectedRedFlagIds = emptySet(),
      )
    assertEquals(TriageOutcome.URGENT_EMERGENCY, result.outcome)
    assertFalse(result.canShowSelfCare)
    assertFalse(result.showPharmacistOption)
    assertTrue(result.summaryFa.contains("38.0") || result.titleFa.contains("نوزاد"))
  }

  @Test
  fun journeyB_threeYearOldWithConstipation_returnsEducationalGuidanceAndPharmacistOption() {
    val constipationFlow = repo.symptomFlowById("constipation")
    val result =
      TriageEngine.evaluate(
        flow = constipationFlow,
        ageBand = AgeBand.TODDLER_1_3Y,
        temperatureCelsius = null,
        durationHours = 48,
        selectedRedFlagIds = emptySet(),
      )
    assertEquals(TriageOutcome.GENERAL_EDUCATIONAL, result.outcome)
    assertTrue(result.canShowSelfCare)
    assertTrue(result.showPharmacistOption)
    assertTrue(result.escalationCriteriaFa.isNotEmpty())
  }

  @Test
  fun journeyC_medicinesHaveMlGuidanceConcentrationWarningAndLockedDoseRule() {
    val acetaminophen = repo.medicationBySlug("acetaminophen-pediatric")
    assertNotNull(acetaminophen)
    assertFalse(acetaminophen!!.lockedDoseRule.isEnabled)
    assertTrue(acetaminophen.dosageFormAlertFa.contains("شکل و غلظت دارو را بررسی کنید"))
    assertTrue(acetaminophen.timingAndPrecautionsFa.any { it.contains("mL") })
  }

  @Test
  fun journeyD_milestonesFilterByAgeAndGrowthArticleExists() {
    val toddlerMilestones = repo.milestonesForAge(AgeBand.TODDLER_1_3Y)
    assertTrue(toddlerMilestones.isNotEmpty())
    assertTrue(toddlerMilestones.all { it.ageBand == AgeBand.TODDLER_1_3Y })
    assertNotNull(repo.articleBySlug("growth-and-development"))
  }

  @Test
  fun journeyE_diaperRashArticleAndFlowExist() {
    val diaperArticle = repo.articleBySlug("diaper-rash-care")
    assertNotNull(diaperArticle)
    assertTrue(diaperArticle!!.redFlagsFa.isNotEmpty())
    assertEquals("diaper_rash", repo.symptomFlowById("diaper_rash").id)
  }

  @Test
  fun journeyF_persianSearchNormalizesHalfSpaceArabicFormsAndDigits() {
    val withSpace = repo.searchFaqs("شیر خشک")
    val withoutSpace = repo.searchFaqs("شیرخشک")
    assertTrue(withSpace.isNotEmpty())
    assertEquals(withSpace.first().id, withoutSpace.first().id)

    val arabicLetters = repo.searchFaqs("استامينوفن") // Arabic ي
    assertTrue(arabicLetters.isNotEmpty())
    assertTrue(PersianNormalizer.isHighRiskQuery("تب نوزاد"))
    assertTrue(repo.faqs.size >= 25)
    assertTrue(repo.articles.size >= 8)
  }

  @Test
  fun medicationKnowledgeBase_searchByNameAliasAndCategoryReturnsWarnings() {
    assertTrue(repo.medications.size >= 10)
    assertTrue(repo.medications.all { !it.lockedDoseRule.isEnabled && it.whatNotToGiveFa.isNotEmpty() })

    val ibuprofenByAlias = repo.searchMedications("بروفن")
    assertEquals("ibuprofen-pediatric", ibuprofenByAlias.first().slug)

    val antibioticByHalfSpace = repo.searchMedications("آموکسی سیلین")
    assertEquals("antibiotic-suspensions-pediatric", antibioticByHalfSpace.first().slug)

    val ironDrops = repo.searchMedications("قطره آهن")
    assertEquals("vitamin-d3-iron-drops", ironDrops.first().slug)

    val topicalSteroidCaution = repo.searchMedications("بتامتازون")
    assertEquals("topical-zinc-vs-steroid-cream", topicalSteroidCaution.first().slug)

    val filteredByCat = repo.searchMedications("", com.example.domain.model.MedicationSafetyCategory.ANTIBIOTICS)
    assertEquals(1, filteredByCat.size)
    assertEquals("antibiotic-suspensions-pediatric", filteredByCat.first().slug)
  }

  @Test
  fun medicationOverlapEngine_detectsDangerCautionInfoPairs() {
    val acetaminophen = repo.medicationBySlug("acetaminophen-pediatric")!!
    val ibuprofen = repo.medicationBySlug("ibuprofen-pediatric")!!
    val otcCoughCold = repo.medicationBySlug("otc-cough-cold-caution")!!
    val antibiotic = repo.medicationBySlug("antibiotic-suspensions-pediatric")!!
    val ironDrops = repo.medicationBySlug("vitamin-d3-iron-drops")!!
    val ors = repo.medicationBySlug("ors-oral-rehydration")!!

    val duplicate = MedicationOverlapEngine.evaluatePair(acetaminophen, acetaminophen)
    assertEquals(AlertSeverity.DANGER, duplicate.severity)

    val hiddenAcetaminophen = MedicationOverlapEngine.evaluatePair(acetaminophen, otcCoughCold)
    assertEquals(AlertSeverity.DANGER, hiddenAcetaminophen.severity)
    assertTrue(hiddenAcetaminophen.titleFa.contains("استامینوفن پنهان"))

    val dualAntipyretic = MedicationOverlapEngine.evaluatePair(acetaminophen, ibuprofen)
    assertEquals(AlertSeverity.CAUTION, dualAntipyretic.severity)

    val antibioticWithIron = MedicationOverlapEngine.evaluatePair(antibiotic, ironDrops)
    assertEquals(AlertSeverity.CAUTION, antibioticWithIron.severity)
    assertTrue(antibioticWithIron.actionStepsFa.any { it.contains("۲ ساعت") })

    val antibioticWithOrs = MedicationOverlapEngine.evaluatePair(antibiotic, ors)
    assertEquals(AlertSeverity.INFO, antibioticWithOrs.severity)
  }

  @Test
  fun liquidDoseVisualizer_neverSuggestsPediatricDoseUntilReviewed() {
    val scenarios = listOf(
      Triple(LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5, 24, 12.0f),
      Triple(LiquidFormulationId.ACETAMINOPHEN_DROPS_100, 6, 8.0f),
      Triple(LiquidFormulationId.ACETAMINOPHEN_DROPS_100, 2, 5.0f),
      Triple(LiquidFormulationId.IBUPROFEN_SYRUP_100_5, 4, 7.0f),
      Triple(LiquidFormulationId.IBUPROFEN_SYRUP_100_5, 12, 10.0f),
      Triple(LiquidFormulationId.CETIRIZINE_SYRUP_5_5, 36, 14.0f),
    )
    scenarios.forEach { (formulation, ageMonths, weightKg) ->
      val result = LiquidDoseVisualizerEngine.evaluate(formulation, ageMonths, weightKg)
      assertTrue(result.isBlocked)
      assertEquals(0f, result.recommendedMl, 0.01f)
      assertEquals(0f, result.minSafeMl, 0.01f)
      assertEquals(0f, result.maxSafeMl, 0.01f)
      assertEquals(0, result.recommendedMg)
      assertTrue(result.alertTitleFa.isNotEmpty())
    }
    assertTrue(
      LiquidDoseVisualizerEngine.estimateWeightFromDraggedMl(
        LiquidFormulationId.ACETAMINOPHEN_SYRUP_120_5, 24, 5f,
      ) == null
    )
  }
}
