package com.example.domain.util

import com.example.domain.model.TopicCategory
import com.example.ui.navigation.AskPharmacistRoute
import com.example.ui.navigation.DevicesRoute
import com.example.ui.navigation.FaqRoute
import com.example.ui.navigation.GrowthRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.InfoRoute
import com.example.ui.navigation.MedicineDetailRoute
import com.example.ui.navigation.MedicinesRoute
import com.example.ui.navigation.MyChildRoute
import com.example.ui.navigation.NutritionRoute
import com.example.ui.navigation.Pediatric3DStudioRoute
import com.example.ui.navigation.SkinRoute
import com.example.ui.navigation.SymptomsRoute
import com.example.ui.navigation.TopicDetailRoute
import com.example.ui.navigation.TopicsRoute
import com.example.ui.navigation.VaccinesRoute
import java.net.URI
import java.net.URLDecoder

data class DeepLinkResolution(
  val route: Any,
  val medicineTabKey: String? = null,
  val topicCategoryId: String? = null,
  val topicQuery: String? = null,
  val faqQuery: String? = null,
  val symptomId: String? = null,
  val fallbackNoticeFa: String? = null,
  val resetTopicFilters: Boolean = false,
  val resetFaqFilters: Boolean = false,
)

object ManoDeepLinkParser {
  private val validInfoSections = setOf("about", "sources", "privacy", "domain")
  private val valid3dScenes = setOf("workbench", "weighing")
  private val validMedicineTabs = setOf("kb", "syringe", "overlap", "timeline", "checklist")

  fun resolve(
    rawUriString: String?,
    validArticleSlugs: Set<String>,
    validMedicationSlugs: Set<String>,
    validSymptomIds: Set<String>,
  ): DeepLinkResolution? {
    val trimmed = rawUriString?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val uri = runCatching { URI.create(trimmed) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "https" && scheme != "http") return null
    if (!ManoDomainLinks.isSupportedHost(uri.host)) return null

    val segments =
      (uri.rawPath ?: "")
        .split('/')
        .map { safeDecode(it).trim() }
        .filter { it.isNotEmpty() }
    val query = parseQueryParams(uri.rawQuery)

    val first = segments.firstOrNull()?.lowercase()
    val second = segments.getOrNull(1)

    return when (first) {
      null, "home", "index.html" -> DeepLinkResolution(route = HomeRoute)

      "topics", "articles", "guide", "guides" -> {
        if (!second.isNullOrBlank()) {
          val matchedSlug = validArticleSlugs.firstOrNull { it.equals(second, ignoreCase = true) }
          if (matchedSlug != null) {
            DeepLinkResolution(route = TopicDetailRoute(slug = matchedSlug))
          } else {
            DeepLinkResolution(
              route = TopicsRoute(),
              fallbackNoticeFa = "مقاله انتخاب‌شده در این نسخه پیدا نشد؛ فهرست راهنماها باز شد.",
              resetTopicFilters = true,
            )
          }
        } else {
          val rawCat = query["category"]?.trim()
          val validCat = rawCat?.let { id -> TopicCategory.entries.firstOrNull { it.id.equals(id, ignoreCase = true) }?.id }
          val q = query["q"]?.trim().orEmpty()
          DeepLinkResolution(
            route = TopicsRoute(categoryId = validCat, query = q),
            topicCategoryId = validCat,
            topicQuery = q.takeIf { it.isNotEmpty() },
            resetTopicFilters = true,
          )
        }
      }

      "medicines", "meds", "drugs" -> {
        if (!second.isNullOrBlank()) {
          val matchedMedSlug = validMedicationSlugs.firstOrNull { it.equals(second, ignoreCase = true) }
          if (matchedMedSlug != null) {
            DeepLinkResolution(route = MedicineDetailRoute(slug = matchedMedSlug))
          } else {
            DeepLinkResolution(
              route = MedicinesRoute,
              fallbackNoticeFa = "کارت داروی انتخاب‌شده یافت نشد؛ فهرست داروها باز شد.",
            )
          }
        } else {
          val tab = query["tab"]?.trim()?.lowercase()?.takeIf { it in validMedicineTabs }
          DeepLinkResolution(route = MedicinesRoute, medicineTabKey = tab)
        }
      }

      "vaccines", "vaccination", "vaccine" -> DeepLinkResolution(route = VaccinesRoute)

      "symptoms", "triage" -> {
        if (!second.isNullOrBlank()) {
          val matchedSymptomId = validSymptomIds.firstOrNull { it.equals(second, ignoreCase = true) }
          if (matchedSymptomId != null) {
            DeepLinkResolution(route = SymptomsRoute(matchedSymptomId), symptomId = matchedSymptomId)
          } else {
            DeepLinkResolution(
              route = SymptomsRoute(),
              fallbackNoticeFa = "مسیر علامت درخواستی یافت نشد؛ صفحه تریاژ باز شد.",
            )
          }
        } else {
          val rawId = query["id"]?.trim()
          val qSymptom = rawId?.let { id -> validSymptomIds.firstOrNull { it.equals(id, ignoreCase = true) } }
          DeepLinkResolution(
            route = SymptomsRoute(qSymptom ?: "fever"),
            symptomId = qSymptom,
          )
        }
      }

      "growth" -> DeepLinkResolution(route = GrowthRoute)

      "nutrition", "food" -> DeepLinkResolution(route = NutritionRoute)

      "3d", "studio" -> {
        val scene = query["scene"]?.trim()?.lowercase()?.takeIf { it in valid3dScenes } ?: "workbench"
        DeepLinkResolution(route = Pediatric3DStudioRoute(initialScene = scene))
      }

      "skin", "diaper" -> DeepLinkResolution(route = SkinRoute)

      "devices" -> DeepLinkResolution(route = DevicesRoute)

      "faq" -> {
        val q = query["q"]?.trim().orEmpty()
        DeepLinkResolution(
          route = FaqRoute(q),
          faqQuery = q.takeIf { it.isNotEmpty() },
          resetFaqFilters = true,
        )
      }

      "pharmacist", "ask-pharmacist", "consult" -> {
        val cat = query["category"]?.trim()?.takeIf { it.isNotEmpty() }
        DeepLinkResolution(route = AskPharmacistRoute(cat))
      }

      "child", "my-child", "profile" -> DeepLinkResolution(route = MyChildRoute)

      "info", "about", "privacy", "sources", "domain" -> {
        val section =
          when (first) {
            "privacy" -> "privacy"
            "sources" -> "sources"
            "domain" -> "domain"
            else -> query["section"]?.trim()?.lowercase()?.takeIf { it in validInfoSections } ?: "about"
          }
        DeepLinkResolution(route = InfoRoute(initialSection = section))
      }

      else ->
        DeepLinkResolution(
          route = HomeRoute,
          fallbackNoticeFa = "مسیر لینک در صفحه اصلی مانو کید باز شد.",
        )
    }
  }

  private fun parseQueryParams(rawQuery: String?): Map<String, String> {
    if (rawQuery.isNullOrBlank()) return emptyMap()
    val result = linkedMapOf<String, String>()
    rawQuery.split('&').forEach { pair ->
      if (pair.isBlank()) return@forEach
      val idx = pair.indexOf('=')
      if (idx <= 0) {
        val k = safeDecode(pair).trim()
        if (k.isNotEmpty()) result[k] = ""
      } else {
        val k = safeDecode(pair.substring(0, idx)).trim()
        val v = safeDecode(pair.substring(idx + 1)).trim()
        if (k.isNotEmpty()) result[k] = v
      }
    }
    return result
  }

  private fun safeDecode(raw: String): String =
    runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
}
