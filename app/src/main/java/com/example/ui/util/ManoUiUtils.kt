package com.example.ui.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.content.FileProvider
import com.example.data.ManoHapticIntensity
import com.example.domain.model.ChildProfileDemo
import com.example.domain.util.FeverDoseTimelineSnapshot
import com.example.domain.util.PersianNormalizer
import com.example.widget.FeverTimelineWidgetStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ManoHaptics {
  fun perform(
    context: Context,
    view: View?,
    intensity: ManoHapticIntensity,
    isPrimaryInteraction: Boolean = true,
  ) {
    if (intensity == ManoHapticIntensity.OFF) return
    val targetView = view ?: return
    val constant =
      when (intensity) {
        ManoHapticIntensity.OFF -> return
        ManoHapticIntensity.SUBTLE -> HapticFeedbackConstants.CLOCK_TICK
        ManoHapticIntensity.MEDIUM ->
          if (isPrimaryInteraction) HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.CLOCK_TICK
        ManoHapticIntensity.STANDARD ->
          if (isPrimaryInteraction) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.KEYBOARD_TAP
      }
    runCatching { targetView.performHapticFeedback(constant) }
  }
}

object ManoLinkShareHelper {
  fun readAppSigningSha256Fingerprints(context: Context): List<String> =
    runCatching {
      val pm = context.packageManager
      val pkgName = context.packageName
      val signatures =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
          val signingInfo = info.signingInfo
          if (signingInfo != null) {
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
            else signingInfo.signingCertificateHistory
          } else {
            emptyArray()
          }
        } else {
          @Suppress("DEPRECATION")
          pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES).signatures ?: emptyArray()
        }
      val digest = MessageDigest.getInstance("SHA-256")
      signatures.mapNotNull { sig ->
        val bytes = digest.digest(sig.toByteArray())
        bytes.joinToString(":") { b -> "%02X".format(b) }
      }.distinct()
    }.getOrDefault(emptyList())

  fun openExternalBrowserUrl(context: Context, url: String): Result<Unit> =
    runCatching {
      val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
    }

  fun copyToClipboard(
    context: Context,
    label: String,
    text: String,
    sensitive: Boolean = false,
  ): Boolean =
    runCatching {
      val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
      val clip = ClipData.newPlainText(label, text)
      if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras =
          PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
          }
      }
      cm.setPrimaryClip(clip)
      true
    }.getOrDefault(false)

  fun shareTextWithLink(context: Context, title: String, body: String): Result<Unit> =
    runCatching {
      val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_SUBJECT, title)
          putExtra(Intent.EXTRA_TEXT, body)
        }
      val chooser =
        Intent.createChooser(sendIntent, title).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(chooser)
    }
}

object ManoSensitiveCacheCleaner {
  fun clear(context: Context): Boolean {
    var allClean = true
    runCatching {
      val cacheDir = context.cacheDir
      cacheDir.listFiles()?.forEach { file ->
        if (file.name.endsWith(".pdf", ignoreCase = true) || file.name.startsWith("mano_")) {
          if (!file.deleteRecursively()) allClean = false
        }
      }
    }.onFailure { allClean = false }
    runCatching {
      if (!FeverTimelineWidgetStore.clear(context)) allClean = false
    }.onFailure { allClean = false }
    return allClean
  }
}

object PharmacyQrCard {
  fun createQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
    if (content.isBlank()) return null
    return runCatching {
      val hints =
        mapOf(
          EncodeHintType.MARGIN to 1,
          EncodeHintType.CHARACTER_SET to "UTF-8",
        )
      val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
      val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
      for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
          bmp.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
      }
      bmp
    }.getOrNull()
  }

  fun printStarterCard(
    context: Context,
    invitationUrl: String,
    pharmacyDisplayNameFa: String,
  ): Result<Unit> =
    runCatching {
      val doc = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
      val page = doc.startPage(pageInfo)
      val canvas = page.canvas
      val titlePaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = AndroidColor.rgb(16, 76, 58)
          textSize = 20f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          textAlign = Paint.Align.CENTER
        }
      val bodyPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = AndroidColor.DKGRAY
          textSize = 13f
          textAlign = Paint.Align.CENTER
        }
      canvas.drawText("MANO Kid • کارت معرفی داروخانه", 297.5f, 90f, titlePaint)
      canvas.drawText(pharmacyDisplayNameFa, 297.5f, 128f, bodyPaint)
      createQrBitmap(invitationUrl, 300)?.let { qr ->
        canvas.drawBitmap(qr, 147.5f, 170f, null)
      }
      canvas.drawText(invitationUrl, 297.5f, 510f, bodyPaint)
      canvas.drawText("این کارت صرفاً معرفی ارتباط با داروخانه است و جایگزین اورژانس ۱۱۵ نیست.", 297.5f, 560f, bodyPaint)
      doc.finishPage(page)

      val file = File(context.cacheDir, "mano_pharmacy_starter_card.pdf")
      FileOutputStream(file).use { doc.writeTo(it) }
      doc.close()

      val cached =
        CachedVisitPdf(
          file = file,
          fileName = file.name,
          pageCount = 1,
          sizeBytes = file.length(),
        )
      VisitDraftPdfHelper.shareCachedPdf(context, cached, "کارت معرفی $pharmacyDisplayNameFa").getOrThrow()
    }
}

data class CachedVisitPdf(
  val file: File,
  val fileName: String,
  val pageCount: Int,
  val sizeBytes: Long,
) {
  fun formattedSizeLabelFa(): String {
    val kb = (sizeBytes / 1024L).coerceAtLeast(1L)
    return "${PersianNormalizer.toPersianDigits(kb.toString())} کیلوبایت"
  }
}

object VisitDraftPdfHelper {
  fun generateCachedPdf(
    context: Context,
    titleFa: String,
    symptomContextFa: String,
    timelineSummaryFa: String,
    draftTextFa: String,
    parentNoteFa: String,
    childProfile: ChildProfileDemo?,
    snapshot: FeverDoseTimelineSnapshot,
  ): Result<CachedVisitPdf> =
    runCatching {
      val doc = PdfDocument()
      val pageWidth = 595
      val pageHeight = 842
      val margin = 44
      val usableWidth = pageWidth - margin * 2

      val titlePaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = AndroidColor.rgb(16, 76, 58)
          textSize = 18f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
      val metaPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = AndroidColor.rgb(55, 143, 120)
          textSize = 12f
        }
      val bodyPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = AndroidColor.rgb(18, 40, 35)
          textSize = 13f
        }

      val fullContent =
        buildString {
          appendLine("مانو کید (MANO Kid) — پیش‌نویس آماده ویزیت برای مرور پزشک/داروساز")
          appendLine("توجه ایمنی: این سند خلاصه‌سازی کمکی است و جایگزین تشخیص یا نسخه پزشک نیست.")
          appendLine()
          if (childProfile != null) {
            appendLine("کودک: ${childProfile.aliasFa} | سن: ${childProfile.birthDateOrAgeFa} | وزن: ${childProfile.weightKgText} kg")
            appendLine("حساسیت‌ها: ${childProfile.allergiesFa} | داروهای فعلی: ${childProfile.currentMedsFa}")
            appendLine()
          }
          if (symptomContextFa.isNotBlank()) {
            appendLine("زمینه علائم: $symptomContextFa")
            appendLine()
          }
          if (timelineSummaryFa.isNotBlank()) {
            appendLine("خلاصه خط زمانی تب و دارو: $timelineSummaryFa")
            appendLine()
          }
          if (snapshot.tempPoints.isNotEmpty()) {
            appendLine("ثبت‌های دمای ۲۴ ساعت اخیر:")
            snapshot.tempPoints.takeLast(6).forEach { tp ->
              appendLine("• ${tp.tempCelsius}°C (${tp.siteFa}) ساعت ${tp.timeLabelFa}")
            }
            appendLine()
          }
          if (parentNoteFa.isNotBlank()) {
            appendLine("یادداشت والد: $parentNoteFa")
            appendLine()
          }
          appendLine(draftTextFa)
        }

      val layout =
        StaticLayout.Builder.obtain(fullContent, 0, fullContent.length, bodyPaint, usableWidth)
          .setAlignment(Layout.Alignment.ALIGN_NORMAL)
          .setLineSpacing(4f, 1.25f)
          .build()

      var lineIdx = 0
      var pageNumber = 0
      while (lineIdx < layout.lineCount) {
        pageNumber += 1
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        var y = margin.toFloat()
        if (pageNumber == 1) {
          canvas.drawText(titleFa.ifBlank { "پیش‌نویس ویزیت مانو کید" }, margin.toFloat(), y + 16f, titlePaint)
          y += 28f
          val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
          canvas.drawText("MANO Kid Visit Draft • $ts", margin.toFloat(), y + 10f, metaPaint)
          y += 22f
        }
        val maxY = pageHeight - margin
        while (lineIdx < layout.lineCount) {
          val lineTop = layout.getLineTop(lineIdx)
          val lineBottom = layout.getLineBottom(lineIdx)
          val lineHeight = (lineBottom - lineTop).toFloat()
          if (y + lineHeight > maxY) break
          val start = layout.getLineStart(lineIdx)
          val end = layout.getLineEnd(lineIdx)
          val lineText = fullContent.substring(start, end).trimEnd('\n', '\r')
          canvas.drawText(lineText, margin.toFloat(), y + bodyPaint.textSize, bodyPaint)
          y += lineHeight
          lineIdx += 1
        }
        doc.finishPage(page)
      }

      val outFile = File(context.cacheDir, "mano_visit_draft_${System.currentTimeMillis()}.pdf")
      FileOutputStream(outFile).use { doc.writeTo(it) }
      doc.close()

      CachedVisitPdf(
        file = outFile,
        fileName = outFile.name,
        pageCount = pageNumber.coerceAtLeast(1),
        sizeBytes = outFile.length(),
      )
    }

  private fun uriFor(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

  fun shareCachedPdf(context: Context, cached: CachedVisitPdf, titleFa: String): Result<Unit> =
    runCatching {
      val uri = uriFor(context, cached.file)
      val intent =
        Intent(Intent.ACTION_SEND).apply {
          type = "application/pdf"
          putExtra(Intent.EXTRA_SUBJECT, titleFa)
          putExtra(Intent.EXTRA_STREAM, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      val chooser =
        Intent.createChooser(intent, titleFa).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(chooser)
    }

  fun openCachedPdf(context: Context, cached: CachedVisitPdf): Result<Unit> =
    runCatching {
      val uri = uriFor(context, cached.file)
      val intent =
        Intent(Intent.ACTION_VIEW).apply {
          setDataAndType(uri, "application/pdf")
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
    }

  fun printCachedPdf(context: Context, cached: CachedVisitPdf, titleFa: String): Result<Unit> =
    runCatching {
      val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
      if (printManager != null) {
        val adapter =
          object : android.print.PrintDocumentAdapter() {
            override fun onLayout(
              oldAttributes: PrintAttributes?,
              newAttributes: PrintAttributes?,
              cancellationSignal: android.os.CancellationSignal?,
              callback: LayoutResultCallback,
              extras: android.os.Bundle?,
            ) {
              if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
              }
              val info =
                android.print.PrintDocumentInfo.Builder(cached.fileName)
                  .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                  .setPageCount(cached.pageCount)
                  .build()
              callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
              pages: Array<out android.print.PageRange>,
              destination: android.os.ParcelFileDescriptor,
              cancellationSignal: android.os.CancellationSignal?,
              callback: WriteResultCallback,
            ) {
              runCatching {
                cached.file.inputStream().use { input ->
                  FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                  }
                }
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
              }.onFailure { err ->
                callback.onWriteFailed(err.localizedMessage)
              }
            }
          }
        printManager.print(titleFa, adapter, PrintAttributes.Builder().build())
      } else {
        openCachedPdf(context, cached).getOrThrow()
      }
    }
}
