package com.example.domain.util

import com.example.domain.model.Article
import com.example.domain.model.MedicationMonograph
import com.example.domain.model.VaccineDashboardSummary
import java.net.URLEncoder

object ManoDomainLinks {
  const val CANONICAL_HOST = "manokid.ir"
  const val SECONDARY_HOST = "manokid.com"
  const val CANONICAL_ORIGIN = "https://$CANONICAL_HOST"
  const val SECONDARY_ORIGIN = "https://$SECONDARY_HOST"
  const val ASSETLINKS_WELL_KNOWN_PATH = "/.well-known/assetlinks.json"
  const val RELEASE_SHA256_PLACEHOLDER = "REPLACE_WITH_PLAY_OR_RELEASE_SHA256_FINGERPRINT"

  val PRIMARY_DOMAINS: List<String> = listOf(CANONICAL_HOST, SECONDARY_HOST)

  val SUPPORTED_HOSTS: Set<String> =
    setOf(
      "manokid.ir",
      "www.manokid.ir",
      "manokid.com",
      "www.manokid.com",
    )

  fun isSupportedHost(rawHost: String?): Boolean {
    val normalized =
      rawHost
        ?.trim()
        ?.substringBefore(':')
        ?.lowercase()
        ?.trimEnd('.')
        ?: return false
    return normalized in SUPPORTED_HOSTS
  }

  fun homeUrl(host: String = CANONICAL_HOST): String = "https://${safeHost(host)}/"

  fun topicsUrl(
    categoryId: String? = null,
    query: String = "",
    host: String = CANONICAL_HOST,
  ): String {
    val params = buildList {
      if (!categoryId.isNullOrBlank()) add("category=${encodeQuery(categoryId.trim())}")
      if (query.isNotBlank()) add("q=${encodeQuery(query.trim())}")
    }
    val base = "https://${safeHost(host)}/topics"
    return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
  }

  fun topicUrl(slug: String, host: String = CANONICAL_HOST): String {
    val cleanSlug = slug.trim().trim('/')
    return if (cleanSlug.isBlank()) topicsUrl(host = host) else "https://${safeHost(host)}/topics/$cleanSlug"
  }

  fun medicinesUrl(tab: String? = null, host: String = CANONICAL_HOST): String {
    val cleanTab = tab?.trim()?.takeIf { it.isNotEmpty() }
    val base = "https://${safeHost(host)}/medicines"
    return if (cleanTab == null) base else "$base?tab=${encodeQuery(cleanTab)}"
  }

  fun medicineUrl(slug: String, host: String = CANONICAL_HOST): String {
    val cleanSlug = slug.trim().trim('/')
    return if (cleanSlug.isBlank()) medicinesUrl(host = host) else "https://${safeHost(host)}/medicines/$cleanSlug"
  }

  fun vaccinesUrl(host: String = CANONICAL_HOST): String = "https://${safeHost(host)}/vaccines"

  fun symptomsUrl(symptomId: String? = null, host: String = CANONICAL_HOST): String {
    val cleanId = symptomId?.trim()?.trim('/')
    return if (cleanId.isNullOrBlank()) {
      "https://${safeHost(host)}/symptoms"
    } else {
      "https://${safeHost(host)}/symptoms/$cleanId"
    }
  }

  fun growthUrl(host: String = CANONICAL_HOST): String = "https://${safeHost(host)}/growth"

  fun nutritionUrl(host: String = CANONICAL_HOST): String = "https://${safeHost(host)}/nutrition"

  fun studio3dUrl(scene: String? = null, host: String = CANONICAL_HOST): String {
    val cleanScene = scene?.trim()?.takeIf { it.isNotEmpty() }
    val base = "https://${safeHost(host)}/3d"
    return if (cleanScene == null) base else "$base?scene=${encodeQuery(cleanScene)}"
  }

  fun infoUrl(section: String = "domain", host: String = CANONICAL_HOST): String {
    val cleanSection = section.trim().ifEmpty { "about" }
    return "https://${safeHost(host)}/info?section=${encodeQuery(cleanSection)}"
  }

  fun buildTopicShareText(article: Article, host: String = CANONICAL_HOST): String =
    buildString {
      appendLine("راهنمای سلامت کودک مانو کیدز:")
      appendLine(article.titleFa)
      appendLine()
      appendLine(article.shortAnswerFa.trim())
      appendLine()
      appendLine("باز کردن مستقیم در مانو کیدز:")
      append(topicUrl(article.slug, host))
    }.trim()

  fun buildMedicineShareText(medication: MedicationMonograph, host: String = CANONICAL_HOST): String =
    buildString {
      appendLine("کارت ایمنی داروی کودک مانو کیدز:")
      appendLine("${medication.nameFa} (${medication.genericNameEn})")
      appendLine("• کاربرد: ${medication.treatsFa}")
      appendLine("• هشدار غلظت و شکل دارو: ${medication.dosageFormAlertFa}")
      appendLine()
      appendLine("مشاهده کارت کامل در مانو کیدز:")
      append(medicineUrl(medication.slug, host))
    }.trim()

  fun buildVaccinesShareText(
    summary: VaccineDashboardSummary,
    childAliasFa: String? = null,
    host: String = CANONICAL_HOST,
  ): String =
    buildString {
      val childLabel = childAliasFa?.trim()?.takeIf { it.isNotEmpty() }?.let { "برای «$it»" } ?: "کودک"
      appendLine("برنامه واکسیناسیون $childLabel — مانو کیدز")
      appendLine(
        "پیشرفت واکسن‌های اصلی: ${VaccinationScheduleEngine.toPersianDigits(summary.administeredCoreCount.toString())} از ${VaccinationScheduleEngine.toPersianDigits(summary.totalCoreCount.toString())} نوبت"
      )
      summary.nextUpcoming?.let { next ->
        val dateText =
          next.effectiveDueDateEpochMs?.let { VaccinationScheduleEngine.formatPersianDate(it) }
            ?: "در انتظار ثبت تاریخ تولد"
        appendLine("• نوبت پیش‌رو: ${next.definition.titleFa}")
        appendLine("• تاریخ موعد: $dateText (${VaccinationScheduleEngine.formatRelativeDueLabelFa(next)})")
      }
      if (summary.overdueCount > 0) {
        appendLine("• نوبت‌های نیازمند پیگیری: ${VaccinationScheduleEngine.toPersianDigits(summary.overdueCount.toString())}")
      }
      appendLine()
      appendLine("یادآور غیرتشخیصی؛ برای تطبیق نهایی به خانه بهداشت یا پزشک کودک مراجعه کنید.")
      appendLine("باز کردن برنامه واکسیناسیون در مانو کیدز:")
      append(vaccinesUrl(host))
    }.trim()

  fun buildAssetLinksJson(
    packageName: String,
    sha256Fingerprints: List<String>,
    includeReleasePlaceholder: Boolean = true,
  ): String {
    val cleanFingerprints =
      buildList {
        sha256Fingerprints
          .map { it.trim().uppercase() }
          .filter { it.isNotBlank() }
          .distinct()
          .forEach { add(it) }
        if (isEmpty() || includeReleasePlaceholder) {
          if (RELEASE_SHA256_PLACEHOLDER !in this) {
            add(RELEASE_SHA256_PLACEHOLDER)
          }
        }
      }

    val fingerprintsJson = cleanFingerprints.joinToString(",\n          ") { "\"$it\"" }
    val dynamicPaths =
      listOf(
        "/topics/*",
        "/topics",
        "/medicines/*",
        "/medicines",
        "/vaccines*",
        "/symptoms*",
        "/growth*",
        "/nutrition*",
        "/3d*",
        "/skin*",
        "/devices*",
        "/faq*",
        "/pharmacist*",
        "/child*",
        "/info*",
        "/",
      )
    val dynamicComponentsJson =
      dynamicPaths.joinToString(",\n            ") { path -> "{ \"/\": \"$path\" }" }

    return """
[
  {
    "relation": [
      "delegate_permission/common.handle_all_urls"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "${packageName.trim()}",
      "sha256_cert_fingerprints": [
          $fingerprintsJson
      ]
    },
    "relation_extensions": {
      "delegate_permission/common.handle_all_urls": {
        "dynamic_app_link_components": [
            $dynamicComponentsJson
        ]
      }
    }
  }
]
    """.trimIndent()
  }

  private fun safeHost(host: String): String {
    val normalized = host.trim().lowercase()
    return if (normalized in SUPPORTED_HOSTS) normalized else CANONICAL_HOST
  }

  private fun encodeQuery(value: String): String = URLEncoder.encode(value, "UTF-8")
}
