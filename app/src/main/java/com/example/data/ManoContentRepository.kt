package com.example.data

import com.example.domain.model.Article
import com.example.domain.model.DeviceGuide
import com.example.domain.model.FaqItem
import com.example.domain.model.MedicationMonograph
import com.example.domain.model.MilestoneItem
import com.example.domain.model.SourceReference
import com.example.domain.model.SymptomFlow

class ManoContentRepository {
  val sources: List<SourceReference> = ManoSourcesAndClinicalSeed.sources
  val articles: List<Article> = ManoSourcesAndClinicalSeed.articles
  val medications: List<MedicationMonograph> = ManoSourcesAndClinicalSeed.medications
  val symptomFlows: List<SymptomFlow> = ManoSourcesAndClinicalSeed.symptomFlows
  val faqs: List<FaqItem> = ManoSourcesAndClinicalSeed.faqs
  val milestones: List<MilestoneItem> = ManoSourcesAndClinicalSeed.milestones
  val deviceGuides: List<DeviceGuide> = ManoSourcesAndClinicalSeed.deviceGuides
}
