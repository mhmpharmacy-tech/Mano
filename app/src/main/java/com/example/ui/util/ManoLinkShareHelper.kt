package com.example.ui.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import java.security.MessageDigest

object ManoLinkShareHelper {
  fun shareTextWithLink(
    context: Context,
    subjectFa: String,
    bodyText: String,
  ): Result<Unit> =
    runCatching {
      val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_SUBJECT, subjectFa)
          putExtra(Intent.EXTRA_TEXT, bodyText)
        }
      val chooser =
        Intent.createChooser(sendIntent, subjectFa).apply {
          if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        }
      context.startActivity(chooser)
    }

  fun copyToClipboard(context: Context, label: String, text: String): Boolean {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    return true
  }

  /**
   * Opens a URL in an external web browser without looping back into Mano Kids
   * when App Links verification is enabled on the device.
   */
  fun openExternalBrowserUrl(context: Context, url: String): Result<Unit> =
    runCatching {
      val uri = url.toUri()
      val pm = context.packageManager
      val browserProbe = Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
      val resolvedBrowsers =
        pm.queryIntentActivities(browserProbe, PackageManager.MATCH_DEFAULT_ONLY)
          .mapNotNull { it.activityInfo }
          .filter { it.packageName != context.packageName }

      val viewIntent =
        Intent(Intent.ACTION_VIEW, uri).apply {
          addCategory(Intent.CATEGORY_BROWSABLE)
          if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          val preferredBrowser = resolvedBrowsers.firstOrNull()
          if (preferredBrowser != null) {
            setClassName(preferredBrowser.packageName, preferredBrowser.name)
          }
        }
      context.startActivity(viewIntent)
    }

  @Suppress("DEPRECATION")
  fun readAppSigningSha256Fingerprints(context: Context): List<String> =
    runCatching {
      val pm = context.packageManager
      val pkg = context.packageName
      val signatures =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
          val signingInfo = info.signingInfo
          if (signingInfo == null) {
            emptyArray()
          } else if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
          } else {
            signingInfo.signingCertificateHistory
          }
        } else {
          val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
          info.signatures ?: emptyArray()
        }

      signatures.mapNotNull { sig ->
        runCatching {
          val digest = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
          digest.joinToString(":") { b -> "%02X".format(b) }
        }.getOrNull()
      }.distinct()
    }.getOrDefault(emptyList())
}
