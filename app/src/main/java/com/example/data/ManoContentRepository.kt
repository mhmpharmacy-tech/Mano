package com.example.data

import com.example.domain.model.AgeBand
import com.example.domain.model.Article
import com.example.domain.model.DeviceGuide
import com.example.domain.model.FaqItem
import com.example.domain.model.MedicationMonograph
import com.example.domain.model.MedicationSafetyCategory
import com.example.domain.model.MilestoneItem
import com.example.domain.model.PharmacistCaseDemo
import com.example.domain.model.SourceReference
import com.example.domain.model.SymptomFlow
import com.example.domain.model.TopicCategory
import com.example.domain.util.PersianNormalizer

class ManoContentRepository {

  val articles: List<Article> by lazy { ManoArticlesSeed.starterArticles.map(ManoTextbookEvidence::reviseArticle) }
  val faqs: List<FaqItem> by lazy { ManoFaqSeed.faqs + ManoTextbookEvidence.faqs }
  val symptomFlows: List<SymptomFlow> by lazy { ManoSourcesAndClinicalSeed.symptomFlows.map(ManoTextbookEvidence::reviseFlow) }
  val medications: List<MedicationMonograph> by lazy { ManoSourcesAndClinicalSeed.medicationMonographs }
  val milestones: List<MilestoneItem> by lazy { ManoSourcesAndClinicalSeed.milestones }
  val deviceGuides: List<DeviceGuide> by lazy { ManoSourcesAndClinicalSeed.deviceGuides }
  val sources: List<SourceReference> by lazy { ManoSourcesAndClinicalSeed.allSources + ManoTextbookEvidence.allSources }
  val demoPharmacistCases: List<PharmacistCaseDemo> by lazy { ManoSourcesAndClinicalSeed.demoPharmacistCases }

  fun articleBySlug(slug: String): Article? = articles.firstOrNull { it.slug == slug }

  fun medicationBySlug(slug: String): MedicationMonograph? = medications.firstOrNull { it.slug == slug }

  fun symptomFlowById(id: String): SymptomFlow =
    symptomFlows.firstOrNull { it.id == id } ?: symptomFlows.first()

  fun searchArticles(
    query: String,
    category: TopicCategory? = null,
    ageBand: AgeBand? = null,
  ): List<Article> {
    return articles
      .asSequence()
      .filter { category == null || it.category == category }
      .filter { ageBand == null || ageBand in it.ageBands }
      .map { article ->
        val score =
          if (query.isBlank()) {
            1
          } else {
            PersianNormalizer.scoreMatch(
              query,
              article.titleFa,
              article.shortAnswerFa,
              article.ageRelevanceFa,
              article.selfCarePointsFa.joinToString(" "),
              article.redFlagsFa.joinToString(" "),
            )
          }
        article to score
      }
      .filter { it.second > 0 }
      .sortedByDescending { it.second }
      .map { it.first }
      .toList()
  }

  fun searchFaqs(
    query: String,
    category: TopicCategory? = null,
    ageBand: AgeBand? = null,
  ): List<FaqItem> {
    return faqs
      .asSequence()
      .filter { category == null || it.category == category }
      .filter { ageBand == null || ageBand in it.ageBands }
      .map { faq ->
        val score =
          if (query.isBlank()) {
            1
          } else {
            PersianNormalizer.scoreMatch(
              query,
              faq.questionFa,
              faq.tags.joinToString(" "),
              faq.summaryFa,
              faq.whatToDoFa.joinToString(" "),
              faq.whenToSeeDoctorFa.joinToString(" "),
            )
          }
        faq to score
      }
      .filter { it.second > 0 }
      .sortedByDescending { it.second }
      .map { it.first }
      .toList()
  }

  fun searchMedications(
    query: String,
    category: MedicationSafetyCategory? = null,
  ): List<MedicationMonograph> {
    val byCategory =
      if (category == null) medications else medications.filter { it.category == category }
    if (query.isBlank()) return byCategory
    return byCategory
      .map { med ->
        val score =
          PersianNormalizer.scoreMatch(
            query,
            med.nameFa,
            med.aliasesSearchTermsFa.joinToString(" "),
            med.genericNameEn,
            med.keySafetySummaryFa,
            med.treatsFa,
            med.dosageFormAlertFa,
            med.whatNotToGiveFa.joinToString(" "),
            med.timingAndPrecautionsFa.joinToString(" "),
            med.interactionCautionsFa.joinToString(" "),
          )
        med to score
      }
      .filter { it.second > 0 }
      .sortedByDescending { it.second }
      .map { it.first }
  }

  fun milestonesForAge(ageBand: AgeBand?): List<MilestoneItem> {
    return if (ageBand == null) milestones else milestones.filter { it.ageBand == ageBand }
  }
}
