package com.example.domain.util

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

enum class AssetLinksCheckSeverity {
  VERIFIED,
  WARNING,
  FAILED,
}

data class AssetLinksDomainReport(
  val domain: String,
  val url: String,
  val httpStatusCode: Int?,
  val contentType: String?,
  val redirectedTo: String? = null,
  val isValidJson: Boolean = false,
  val packageMatched: Boolean = false,
  val fingerprintMatched: Boolean = false,
  val hasPlaceholderFingerprint: Boolean = false,
  val foundFingerprints: List<String> = emptyList(),
  val severity: AssetLinksCheckSeverity,
  val headlineFa: String,
  val detailsFa: String,
)

/**
 * Verifies real HTTPS `/.well-known/assetlinks.json` hosting for Android App Links on
 * manokid.ir and manokid.com, enforcing strict zero-redirect, JSON content-type, package match,
 * and SHA-256 certificate fingerprint validation.
 */
object AssetLinksVerifier {

  private const val CONNECT_TIMEOUT_MS = 7_500
  private const val READ_TIMEOUT_MS = 7_500

  suspend fun verifyAllPrimaryDomains(
    expectedPackageName: String,
    expectedFingerprints: List<String>,
  ): List<AssetLinksDomainReport> =
    withContext(Dispatchers.IO) {
      ManoDomainLinks.PRIMARY_DOMAINS.map { domain ->
        verifyDomainBlocking(
          domain = domain,
          expectedPackageName = expectedPackageName,
          expectedFingerprints = expectedFingerprints,
        )
      }
    }

  private fun verifyDomainBlocking(
    domain: String,
    expectedPackageName: String,
    expectedFingerprints: List<String>,
  ): AssetLinksDomainReport {
    val urlStr = "https://$domain/.well-known/assetlinks.json"
    var connection: HttpURLConnection? = null
    return try {
      connection =
        (URL(urlStr).openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          instanceFollowRedirects = false
          connectTimeout = CONNECT_TIMEOUT_MS
          readTimeout = READ_TIMEOUT_MS
          setRequestProperty("Accept", "application/json")
          setRequestProperty("User-Agent", "ManoKidAppLinksVerifier/1.0")
        }

      val statusCode = connection.responseCode
      val contentType = connection.contentType
      val locationHeader = connection.getHeaderField("Location")

      if (statusCode in 300..399) {
        return AssetLinksDomainReport(
          domain = domain,
          url = urlStr,
          httpStatusCode = statusCode,
          contentType = contentType,
          redirectedTo = locationHeader,
          severity = AssetLinksCheckSeverity.FAILED,
          headlineFa = "خطای ریدایرکت (HTTP $statusCode)",
          detailsFa =
            "اندروید برای تایید App Links هیچ ریدایرکتی را نمی‌پذیرد. مسیر /.well-known/assetlinks.json باید مستقیم با HTTP 200 پاسخ دهد" +
              (if (!locationHeader.isNullOrBlank()) " (ریدایرکت فعلی به: $locationHeader)." else "."),
        )
      }

      if (statusCode != HttpURLConnection.HTTP_OK) {
        return AssetLinksDomainReport(
          domain = domain,
          url = urlStr,
          httpStatusCode = statusCode,
          contentType = contentType,
          severity = AssetLinksCheckSeverity.FAILED,
          headlineFa = "فایل روی سرور یافت نشد یا خطا داد (HTTP $statusCode)",
          detailsFa =
            "پاسخ سرور برای $urlStr برابر HTTP $statusCode بود. فایل assetlinks.json را بدون ریدایرکت روی هاست بارگذاری کنید.",
        )
      }

      val body =
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
          reader.readText()
        }

      parseAndValidateAssetLinksJson(
        domain = domain,
        url = urlStr,
        rawJson = body,
        expectedPackageName = expectedPackageName,
        expectedFingerprints = expectedFingerprints,
        httpStatusCode = statusCode,
        contentType = contentType,
      )
    } catch (e: Exception) {
      AssetLinksDomainReport(
        domain = domain,
        url = urlStr,
        httpStatusCode = null,
        contentType = null,
        severity = AssetLinksCheckSeverity.FAILED,
        headlineFa = "اتصال به سرور برقرار نشد",
        detailsFa =
          "بررسی زنده $domain انجام نشد (${e.javaClass.simpleName}: ${e.localizedMessage ?: "بدون دسترسی شبکه یا خطای SSL"}). روی گوشی متصل به اینترنت دوباره امتحان کنید.",
      )
    } finally {
      connection?.disconnect()
    }
  }

  /**
   * Pure JSON parser & validator for `assetlinks.json` (unit-testable offline).
   */
  fun parseAndValidateAssetLinksJson(
    domain: String,
    url: String = "https://$domain/.well-known/assetlinks.json",
    rawJson: String,
    expectedPackageName: String,
    expectedFingerprints: List<String>,
    httpStatusCode: Int? = 200,
    contentType: String? = "application/json",
  ): AssetLinksDomainReport {
    val trimmed = rawJson.trim()
    val array =
      runCatching { JSONArray(trimmed) }.getOrNull()
        ?: return AssetLinksDomainReport(
          domain = domain,
          url = url,
          httpStatusCode = httpStatusCode,
          contentType = contentType,
          isValidJson = false,
          severity = AssetLinksCheckSeverity.FAILED,
          headlineFa = "ساختار JSON نامعتبر است",
          detailsFa = "پاسخ سرور یک آرایه JSON معتبر مطابق استاندارد Digital Asset Links نیست.",
        )

    val normalizedExpectedFps =
      expectedFingerprints.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()
    val foundFingerprints = linkedSetOf<String>()
    var packageMatched = false
    var hasHandleAllUrlsRelation = false
    var hasPlaceholder = false

    for (i in 0 until array.length()) {
      val item = array.optJSONObject(i) ?: continue
      val target = item.optJSONObject("target") ?: continue
      val namespace = target.optString("namespace")
      val pkg = target.optString("package_name")
      if (namespace == "android_app" && pkg == expectedPackageName) {
        packageMatched = true
        val relations = item.optJSONArray("relation")
        if (relations != null) {
          for (r in 0 until relations.length()) {
            if (relations.optString(r) == "delegate_permission/common.handle_all_urls") {
              hasHandleAllUrlsRelation = true
            }
          }
        }
        val fps = target.optJSONArray("sha256_cert_fingerprints")
        if (fps != null) {
          for (f in 0 until fps.length()) {
            val fp = fps.optString(f).trim()
            if (fp.isNotEmpty()) {
              if (fp.contains("REPLACE_WITH", ignoreCase = true) || fp.contains("PLACEHOLDER", ignoreCase = true)) {
                hasPlaceholder = true
              } else {
                foundFingerprints.add(fp.uppercase())
              }
            }
          }
        }
      }
    }

    val fingerprintMatched =
      normalizedExpectedFps.isNotEmpty() && foundFingerprints.any { it in normalizedExpectedFps }
    val contentTypeIsJson = contentType.isNullOrBlank() || contentType.contains("json", ignoreCase = true)

    return when {
      !packageMatched ->
        AssetLinksDomainReport(
          domain = domain,
          url = url,
          httpStatusCode = httpStatusCode,
          contentType = contentType,
          isValidJson = true,
          packageMatched = false,
          fingerprintMatched = false,
          hasPlaceholderFingerprint = hasPlaceholder,
          foundFingerprints = foundFingerprints.toList(),
          severity = AssetLinksCheckSeverity.FAILED,
          headlineFa = "شناسه بسته اپلیکیشن در فایل یافت نشد",
          detailsFa = "فایل JSON معتبر است اما بسته «$expectedPackageName» در آن ثبت نشده است.",
        )

      !hasHandleAllUrlsRelation ->
        AssetLinksDomainReport(
          domain = domain,
          url = url,
          httpStatusCode = httpStatusCode,
          contentType = contentType,
          isValidJson = true,
          packageMatched = true,
          fingerprintMatched = fingerprintMatched,
          hasPlaceholderFingerprint = hasPlaceholder,
          foundFingerprints = foundFingerprints.toList(),
          severity = AssetLinksCheckSeverity.FAILED,
          headlineFa = "دسترسی handle_all_urls ثبت نشده است",
          detailsFa = "مقدار delegate_permission/common.handle_all_urls در آرایه relation یافت نشد.",
        )

      hasPlaceholder || !fingerprintMatched ->
        AssetLinksDomainReport(
          domain = domain,
          url = url,
          httpStatusCode = httpStatusCode,
          contentType = contentType,
          isValidJson = true,
          packageMatched = true,
          fingerprintMatched = fingerprintMatched,
          hasPlaceholderFingerprint = hasPlaceholder,
          foundFingerprints = foundFingerprints.toList(),
          severity = AssetLinksCheckSeverity.WARNING,
          headlineFa =
            if (hasPlaceholder) "مقدار موقت SHA-256 هنوز جایگزین نشده"
            else "اثر انگشت امضای این بیلد با فایل سرور منطبق نیست",
          detailsFa =
            if (hasPlaceholder) {
              "بسته اپلیکیشن درست است، اما هنوز متن جایگزین (PLACEHOLDER) داخل فایل سرور وجود دارد. اثر انگشت کلید انتشار Play App Signing را جایگزین کنید."
            } else {
              "بسته اپلیکیشن در سرور تایید شد؛ اما SHA-256 امضای این نسخه در فهرست سرور دیده نشد (اگر این نسخه Debug است و سرور کلید Release دارد، این هشدار طبیعی است)."
            },
        )

      !contentTypeIsJson ->
        AssetLinksDomainReport(
          domain = domain,
          url = url,
          httpStatusCode = httpStatusCode,
          contentType = contentType,
          isValidJson = true,
          packageMatched = true,
          fingerprintMatched = true,
          hasPlaceholderFingerprint = false,
          foundFingerprints = foundFingerprints.toList(),
          severity = AssetLinksCheckSeverity.WARNING,
          headlineFa = "هدر Content-Type باید application/json باشد",
          detailsFa = "بسته و اثر انگشت منطبق هستند اما سرور هدر «$contentType» ارسال کرده است.",
        )

      else ->
        AssetLinksDomainReport(
          domain = domain,
          url = url,
          httpStatusCode = httpStatusCode,
          contentType = contentType,
          isValidJson = true,
          packageMatched = true,
          fingerprintMatched = true,
          hasPlaceholderFingerprint = false,
          foundFingerprints = foundFingerprints.toList(),
          severity = AssetLinksCheckSeverity.VERIFIED,
          headlineFa = "تأیید کامل App Links روی سرور واقعی",
          detailsFa = "HTTP 200 بدون ریدایرکت، JSON معتبر، شناسه بسته و SHA-256 امضای فعلی هر دو تایید شدند.",
        )
    }
  }
}
