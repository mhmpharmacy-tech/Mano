package com.example.domain.util

import android.net.Uri
import com.example.domain.model.PublicPharmacyOption
import com.example.ui.navigation.AskPharmacistRoute
import com.example.ui.navigation.DevicesRoute
import com.example.ui.navigation.FaqRoute
import com.example.ui.navigation.FeverDoseCalculatorRoute
import com.example.ui.navigation.GrowthRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.InfoRoute
import com.example.ui.navigation.MedicineDetailRoute
import com.example.ui.navigation.MedicinesRoute
import com.example.ui.navigation.MyChildRoute
import com.example.ui.navigation.NutritionRoute
import com.example.ui.navigation.SkinRoute
import com.example.ui.navigation.SupplementsHygieneRoute
import com.example.ui.navigation.SymptomsRoute
import com.example.ui.navigation.SyrupScannerRoute
import com.example.ui.navigation.TopicDetailRoute
import com.example.ui.navigation.TopicsRoute
import com.example.ui.navigation.TreatmentPlanRoute
import com.example.ui.navigation.VaccinesRoute
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ManoDomainLinks {
  val PRIMARY_DOMAINS = listOf("manokid.ir", "www.manokid.ir", "manokid.com", "www.manokid.com")
  private val PHARMACY_ID_REGEX = Regex("^[a-zA-Z0-9_-]{3,48}$")

  fun homeUrl(domain: String = "manokid.ir"): String = "https://$domain"
  fun topicsUrl(domain: String = "manokid.ir"): String = "https://$domain/topics"
  fun topicUrl(slug: String, domain: String = "manokid.ir"): String = "https://$domain/topics/$slug"
  fun medicinesUrl(tabKey: String? = null, domain: String = "manokid.ir"): String =
    if (tabKey.isNullOrBlank()) "https://$domain/medicines"
    else "https://$domain/medicines?tab=${Uri.encode(tabKey.trim())}"
  fun medicineUrl(slug: String, domain: String = "manokid.ir"): String = "https://$domain/medicines/$slug"
  fun vaccinesUrl(domain: String = "manokid.ir"): String = "https://$domain/vaccines"
  fun nutritionUrl(domain: String = "manokid.ir"): String = "https://$domain/nutrition"
  fun pharmacyInvitationUrl(pharmacyId: String, domain: String = "manokid.ir"): String =
    "https://$domain/pharmacy-invite/$pharmacyId"

  fun isValidPublicPharmacyId(pharmacyId: String): Boolean =
    PHARMACY_ID_REGEX.matches(pharmacyId.trim())

  fun buildMedicineShareText(nameFa: String, slug: String): String =
    "راهنمای ایمنی داروی کودک در مانو کید:\n$nameFa\n${medicineUrl(slug)}"

  fun buildMedicineShareText(medication: com.example.domain.model.MedicationMonograph): String =
    "راهنمای ایمنی داروی کودک در مانو کید:\n${medication.nameFa} (${medication.genericNameEn})\n${medication.dosageFormAlertFa}\n${medicineUrl(medication.slug)}"

  fun buildPharmacyIntroductionText(
    pharmacyId: String,
    displayNameFa: String = "داروخانه منتخب",
  ): String? {
    val cleanId = pharmacyId.trim()
    if (!isValidPublicPharmacyId(cleanId)) return null
    val label = displayNameFa.trim().ifBlank { "داروخانه منتخب" }
    return "دعوت‌نامه گفت‌وگو با داروساز در مانو کید ($label):\n${pharmacyInvitationUrl(cleanId)}\n\nتوجه: در موارد فوری به اورژانس ۱۱۵ مراجعه کنید."
  }

  fun buildAssetLinksJson(
    packageName: String,
    sha256Fingerprints: List<String>,
    includeReleasePlaceholder: Boolean = false,
  ): String {
    val prints = sha256Fingerprints
      .map { it.trim().uppercase() }
      .filter { it.isNotEmpty() }
      .distinct()
      .let { list ->
        if (list.isEmpty()) {
          listOf("REPLACE_WITH_PLAY_APP_SIGNING_SHA256_FINGERPRINT")
        } else if (includeReleasePlaceholder && list.none { it.contains("PLAY_APP_SIGNING") }) {
          list + "REPLACE_WITH_PLAY_APP_SIGNING_SHA256_FINGERPRINT"
        } else {
          list
        }
      }
    val fingerprintLines = prints.joinToString(",\n        ") { "\"$it\"" }
    return """
      [
        {
          "relation": ["delegate_permission/common.handle_all_urls"],
          "target": {
            "namespace": "android_app",
            "package_name": "$packageName",
            "sha256_cert_fingerprints": [
              $fingerprintLines
            ]
          }
        }
      ]
    """.trimIndent()
  }
}

data class ManoDeepLinkResolution(
  val route: Any,
  val topicQuery: String? = null,
  val topicCategoryId: String? = null,
  val resetTopicFilters: Boolean = false,
  val faqQuery: String? = null,
  val resetFaqFilters: Boolean = false,
  val symptomId: String? = null,
  val medicineTabKey: String? = null,
  val fallbackNoticeFa: String? = null,
)

object ManoDeepLinkParser {
  fun resolve(
    rawUriString: String,
    validArticleSlugs: Set<String>,
    validMedicationSlugs: Set<String>,
    validSymptomIds: Set<String>,
  ): ManoDeepLinkResolution? {
    val trimmed = rawUriString.trim()
    if (trimmed.isEmpty()) return null
    val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
    val host = uri.host?.lowercase().orEmpty()
    if (host.isNotEmpty() && host !in ManoDomainLinks.PRIMARY_DOMAINS && host != "mano-kid.app") {
      return null
    }
    val segments = uri.pathSegments.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
    val first = segments.firstOrNull()?.lowercase().orEmpty()
    val second = segments.getOrNull(1).orEmpty()

    return when (first) {
      "", "home" -> ManoDeepLinkResolution(route = HomeRoute)
      "topics", "articles" -> {
        if (second.isNotBlank()) {
          if (second in validArticleSlugs) {
            ManoDeepLinkResolution(route = TopicDetailRoute(second))
          } else {
            ManoDeepLinkResolution(
              route = TopicsRoute(),
              resetTopicFilters = true,
              fallbackNoticeFa = "مطلب درخواستی یافت نشد؛ فهرست آموزش‌ها نمایش داده شد.",
            )
          }
        } else {
          val q = uri.getQueryParameter("q").orEmpty()
          val cat = uri.getQueryParameter("cat")
          ManoDeepLinkResolution(
            route = TopicsRoute(categoryId = cat, query = q),
            topicQuery = q,
            topicCategoryId = cat,
            resetTopicFilters = q.isBlank() && cat == null,
          )
        }
      }
      "medicines", "meds" -> {
        if (second.isNotBlank()) {
          if (second in validMedicationSlugs) {
            ManoDeepLinkResolution(route = MedicineDetailRoute(second))
          } else {
            ManoDeepLinkResolution(
              route = MedicinesRoute,
              fallbackNoticeFa = "داروی درخواستی در فهرست موجود نبود؛ بانک دارویی باز شد.",
            )
          }
        } else {
          val tab = uri.getQueryParameter("tab")
          ManoDeepLinkResolution(route = MedicinesRoute, medicineTabKey = tab)
        }
      }
      "symptoms" -> {
        val targetId = second.ifBlank { uri.getQueryParameter("id").orEmpty() }
        if (targetId.isNotBlank() && targetId in validSymptomIds) {
          ManoDeepLinkResolution(route = SymptomsRoute(targetId), symptomId = targetId)
        } else {
          ManoDeepLinkResolution(route = SymptomsRoute())
        }
      }
      "vaccines" -> ManoDeepLinkResolution(route = VaccinesRoute)
      "growth" -> ManoDeepLinkResolution(route = GrowthRoute)
      "nutrition" -> ManoDeepLinkResolution(route = NutritionRoute)
      "skin" -> ManoDeepLinkResolution(route = SkinRoute)
      "devices" -> ManoDeepLinkResolution(route = DevicesRoute)
      "scanner", "syrup-scanner" -> ManoDeepLinkResolution(route = SyrupScannerRoute)
      "fever-dose", "syringe" -> ManoDeepLinkResolution(route = FeverDoseCalculatorRoute())
      "treatment-plan" -> ManoDeepLinkResolution(route = TreatmentPlanRoute)
      "supplements", "hygiene" -> ManoDeepLinkResolution(route = SupplementsHygieneRoute)
      "care-cards", "care-card" -> ManoDeepLinkResolution(route = HomeRoute)
      "faq" -> {
        val q = uri.getQueryParameter("q").orEmpty()
        ManoDeepLinkResolution(
          route = FaqRoute(q),
          faqQuery = q,
          resetFaqFilters = q.isBlank(),
        )
      }
      "pharmacist", "ask-pharmacist" -> {
        val cat = uri.getQueryParameter("category")
        ManoDeepLinkResolution(route = AskPharmacistRoute(categoryId = cat))
      }
      "pharmacy-invite", "invite" -> {
        val pid = second.ifBlank { uri.getQueryParameter("pharmacyId").orEmpty() }
        if (ManoDomainLinks.isValidPublicPharmacyId(pid)) {
          ManoDeepLinkResolution(route = AskPharmacistRoute(invitedPharmacyId = pid))
        } else {
          ManoDeepLinkResolution(
            route = AskPharmacistRoute(),
            fallbackNoticeFa = "شناسه دعوت‌نامه داروخانه معتبر نیست؛ داروخانه‌های فعال نمایش داده شدند.",
          )
        }
      }
      "my-child", "child" -> ManoDeepLinkResolution(route = MyChildRoute)
      "info", "about", "sources" -> ManoDeepLinkResolution(route = InfoRoute(first))
      else -> ManoDeepLinkResolution(
        route = HomeRoute,
        fallbackNoticeFa = "مسیر پیوند شناخته‌شده نبود؛ صفحه اصلی باز شد.",
      )
    }
  }
}

object PharmacyInvitationPolicy {
  fun resolveAcceptingPharmacyId(
    preferredPharmacyId: String?,
    acceptingPharmacies: List<PublicPharmacyOption>,
  ): String? {
    val cleanId = preferredPharmacyId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return acceptingPharmacies
      .firstOrNull { it.pharmacyId == cleanId && it.acceptingCases }
      ?.pharmacyId
  }

  fun resolveAcceptingPharmacyId(
    acceptingPharmacies: List<PublicPharmacyOption>,
    invitedPharmacyId: String?,
    selectedPharmacyId: String?,
  ): String? {
    val activeIds = acceptingPharmacies.filter { it.acceptingCases }.map { it.pharmacyId }
    val invited = invitedPharmacyId?.trim()?.takeIf { it.isNotEmpty() }
    if (invited != null && invited in activeIds) return invited
    val selected = selectedPharmacyId?.trim()?.takeIf { it.isNotEmpty() }
    if (selected != null && selected in activeIds) return selected
    return activeIds.firstOrNull()
  }
}

enum class AssetLinksCheckSeverity {
  VERIFIED,
  WARNING,
  FAILED,
}

data class AssetLinksDomainReport(
  val domain: String,
  val url: String,
  val severity: AssetLinksCheckSeverity,
  val headlineFa: String,
  val detailsFa: String,
)

object AssetLinksVerifier {
  suspend fun verifyAllPrimaryDomains(
    expectedPackageName: String,
    expectedFingerprints: List<String>,
  ): List<AssetLinksDomainReport> = withContext(Dispatchers.IO) {
    ManoDomainLinks.PRIMARY_DOMAINS.map { domain ->
      val url = "https://$domain/.well-known/assetlinks.json"
      runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
          connectTimeout = 4_000
          readTimeout = 4_000
          requestMethod = "GET"
        }
        val code = conn.responseCode
        val body = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
        conn.disconnect()
        if (code !in 200..299) {
          AssetLinksDomainReport(
            domain = domain,
            url = url,
            severity = AssetLinksCheckSeverity.WARNING,
            headlineFa = "پاسخ HTTP $code از سرور دامنه",
            detailsFa = "فایل assetlinks.json هنوز روی $domain منتشر نشده یا در دسترس نیست.",
          )
        } else {
          val pkgOk = body.contains(expectedPackageName)
          val fpOk = expectedFingerprints.any { fp ->
            fp.isNotBlank() && body.contains(fp, ignoreCase = true)
          }
          when {
            pkgOk && fpOk -> AssetLinksDomainReport(
              domain = domain,
              url = url,
              severity = AssetLinksCheckSeverity.VERIFIED,
              headlineFa = "تأییدشده و آماده App Links",
              detailsFa = "بسته $expectedPackageName و امضای SHA-256 روی این دامنه ثبت شده است.",
            )
            pkgOk -> AssetLinksDomainReport(
              domain = domain,
              url = url,
              severity = AssetLinksCheckSeverity.WARNING,
              headlineFa = "شناسه بسته موجود است؛ اثر انگشت امضا تطبیق نشد",
              detailsFa = "اثر انگشت امضای فعال دستگاه را به assetlinks.json اضافه کنید.",
            )
            else -> AssetLinksDomainReport(
              domain = domain,
              url = url,
              severity = AssetLinksCheckSeverity.FAILED,
              headlineFa = "شناسه برنامه در فایل دامنه یافت نشد",
              detailsFa = "فایل assetlinks.json موجود با شناسه $expectedPackageName هماهنگ نیست.",
            )
          }
        }
      }.getOrElse { err ->
        AssetLinksDomainReport(
          domain = domain,
          url = url,
          severity = AssetLinksCheckSeverity.WARNING,
          headlineFa = "عدم دسترسی شبکه به دامنه",
          detailsFa = err.localizedMessage ?: "اتصال به $domain برقرار نشد.",
        )
      }
    }
  }
}
