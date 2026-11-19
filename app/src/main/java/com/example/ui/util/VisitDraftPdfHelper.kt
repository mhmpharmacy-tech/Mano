package com.example.ui.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.domain.model.ChildProfileDemo
import com.example.domain.util.FeverDoseTimelineEngine
import com.example.domain.util.FeverDoseTimelineSnapshot
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CachedVisitPdf(
  val file: File,
  val contentUri: Uri?,
  val fileName: String,
  val sizeBytes: Long,
  val pageCount: Int,
  val createdAtLabelFa: String,
) {
  fun formattedSizeLabelFa(): String {
    if (sizeBytes <= 0L) return "آماده"
    val kb = max(1, ceil(sizeBytes / 1024.0).toInt())
    return "$kb کیلوبایت"
  }
}

data class VisitPdfDocumentData(
  val titleFa: String,
  val generatedByModel: String,
  val createdAtLabelFa: String,
  val childSummaryLineFa: String,
  val timelineSummaryFa: String,
  val symptomContextFa: String,
  val hasHighFeverAlert: Boolean,
  val highFeverBannerTextFa: String?,
  val draftBodyFa: String,
  val parentNoteFa: String,
  val tempLogLinesFa: List<String>,
  val doseLogLinesFa: List<String>,
)

object VisitDraftPdfHelper {
  private const val TAG = "VisitDraftPdfHelper"
  private const val PDF_CACHE_SUBDIR = "visit_draft_pdfs"
  private const val MAX_CACHED_PDF_FILES = 10

  // Standard ISO A4 in PostScript points (72 dpi)
  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN_H = 38
  private const val MARGIN_TOP = 38
  private const val FOOTER_HEIGHT = 44
  private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2)
  private const val CONTENT_BOTTOM_LIMIT = PAGE_HEIGHT - FOOTER_HEIGHT - 16

  fun cacheDirectory(context: Context): File =
    File(context.cacheDir, PDF_CACHE_SUBDIR).apply { mkdirs() }

  fun buildDocumentData(
    titleFa: String,
    draftTextFa: String,
    symptomContextFa: String,
    timelineSummaryFa: String,
    parentNoteFa: String,
    childProfile: ChildProfileDemo?,
    snapshot: FeverDoseTimelineSnapshot?,
    modelName: String,
    nowMs: Long = System.currentTimeMillis(),
  ): VisitPdfDocumentData {
    val cleanTitle = titleFa.trim().ifBlank { "گزارش پیش از ویزیت پزشک" }
    val cleanBody = sanitizeMarkdownForPdf(draftTextFa).ifBlank { "متنی ثبت نشده است." }
    val cleanParentNote = sanitizeMarkdownForPdf(parentNoteFa)
    val timeLabel = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.US).format(Date(nowMs))

    val childSummaryParts = buildList {
      val alias = childProfile?.aliasFa?.trim().orEmpty()
      if (alias.isNotBlank()) add("نام کودک: $alias")
      val age = childProfile?.birthDateOrAgeFa?.trim().orEmpty()
      if (age.isNotBlank()) add("سن: $age")
      val weight = childProfile?.weightKgText?.trim().orEmpty()
      if (weight.isNotBlank()) add("وزن: $weight کیلوگرم")
      val allergies = childProfile?.allergiesFa?.trim().orEmpty()
      if (allergies.isNotBlank()) add("حساسیت‌ها: $allergies")
      val meds = childProfile?.currentMedsFa?.trim().orEmpty()
      if (meds.isNotBlank()) add("داروهای هم‌زمان: $meds")
    }
    val childSummaryLine =
      if (childSummaryParts.isEmpty()) "پروفایل کودک تکمیل نشده است."
      else childSummaryParts.joinToString("  •  ")

    val highFeverAlert =
      snapshot?.let { FeverDoseTimelineEngine.evaluateHighFeverAlert(it, childProfile) }
    val hasHighFever =
      highFeverAlert != null ||
        timelineSummaryFa.contains("هشدار تب") ||
        (snapshot?.peakTemp24h ?: 0f) >= 39.0f
    val bannerText =
      when {
        highFeverAlert != null ->
          "هشدار بالینی ثبت‌شده: دمای ${FeverDoseTimelineEngine.formatTempC(highFeverAlert.triggerTempCelsius)} در ساعت ${highFeverAlert.timeLabelFa} (بالاتر از ۳۹°C)"
        hasHighFever -> "هشدار بالینی: تب بالای ۳۹ درجه سانتی‌گراد در داده‌های ۲۴ ساعت اخیر ثبت شده است."
        else -> null
      }

    val tempLines =
      snapshot?.tempPoints?.sortedBy { it.timestampMs }?.map { pt ->
        val noteSuffix = pt.noteFa.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""
        "• ساعت ${pt.timeLabelFa}: ${FeverDoseTimelineEngine.formatTempC(pt.tempCelsius)} (${pt.siteFa}، ثبت: ${pt.caregiverFa})$noteSuffix"
      }.orEmpty()

    val doseLines =
      snapshot?.dosePoints?.sortedBy { it.timestampMs }?.map { dose ->
        val noteSuffix = dose.noteFa.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""
        "• ساعت ${dose.givenAtLabelFa}: ${dose.medicationNameFa} (${dose.dosageFormFa}) — ${dose.volumeMlText}$noteSuffix"
      }.orEmpty()

    return VisitPdfDocumentData(
      titleFa = cleanTitle,
      generatedByModel = modelName,
      createdAtLabelFa = timeLabel,
      childSummaryLineFa = childSummaryLine,
      timelineSummaryFa = timelineSummaryFa.trim(),
      symptomContextFa = symptomContextFa.trim(),
      hasHighFeverAlert = hasHighFever,
      highFeverBannerTextFa = bannerText,
      draftBodyFa = cleanBody,
      parentNoteFa = cleanParentNote,
      tempLogLinesFa = tempLines,
      doseLogLinesFa = doseLines,
    )
  }

  fun sanitizeMarkdownForPdf(raw: String): String {
    if (raw.isBlank()) return ""
    val normalizedNewlines = raw.replace("\r\n", "\n").replace('\r', '\n')
    val lines =
      normalizedNewlines.lines().map { rawLine ->
        var line = rawLine.trimEnd()
        line = line.replace(Regex("^\\s{0,3}#{1,6}\\s+"), "")
        line = line.replace(Regex("^\\s*[-*+]\\s+"), "• ")
        line = line.replace("**", "").replace("__", "").replace("`", "")
        line = line.replace(Regex("(?<!\\*)\\*(?!\\*)([^*\\n]+)\\*"), "$1")
        line
      }
    return lines.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
  }

  fun buildPdfFileName(nowMs: Long = System.currentTimeMillis()): String {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(nowMs))
    return "mano_visit_draft_$stamp.pdf"
  }

  suspend fun generatePdfInCache(
    context: Context,
    data: VisitPdfDocumentData,
    nowMs: Long = System.currentTimeMillis(),
    maxCachedFiles: Int = MAX_CACHED_PDF_FILES,
  ): Result<CachedVisitPdf> =
    withContext(Dispatchers.IO) {
      runCatching {
        val dir = cacheDirectory(context)
        val fileName = buildPdfFileName(nowMs)
        val outFile = File(dir, fileName)

        val baseTypeface =
          runCatching { ResourcesCompat.getFont(context, R.font.vazirmatn) }.getOrNull()
            ?: Typeface.DEFAULT

        val pageCount =
          try {
            val renderer = PdfCanvasRenderer(baseTypeface)
            renderer.writeDocument(data, outFile)
          } catch (nativeErr: Throwable) {
            val isRobolectric = Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
            if (isRobolectric && (nativeErr is IllegalStateException || nativeErr is UnsatisfiedLinkError)) {
              writeJvmRobolectricPdf(data, outFile)
            } else {
              throw nativeErr
            }
          }

        pruneOldCachedFiles(dir, keepCount = maxCachedFiles)

        val uri =
          runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
          }.getOrNull()

        CachedVisitPdf(
          file = outFile,
          contentUri = uri,
          fileName = fileName,
          sizeBytes = outFile.length(),
          pageCount = pageCount,
          createdAtLabelFa = data.createdAtLabelFa,
        )
      }.onFailure { err ->
        Log.e(TAG, "Failed to generate cached visit draft PDF", err)
      }
    }

  fun printCachedPdf(
    context: Context,
    pdf: CachedVisitPdf,
    jobName: String = "گزارش ویزیت مانو کید",
  ): Result<Unit> =
    runCatching {
      require(pdf.file.exists()) { "فایل PDF در حافظه موقت یافت نشد." }
      val activityContext = findActivity(context) ?: context
      val printManager =
        activityContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
          ?: error("سرویس چاپ در این دستگاه در دسترس نیست.")
      val adapter = CachedPdfPrintAdapter(pdf.file, pdf.fileName, pdf.pageCount.coerceAtLeast(1))
      val attributes =
        PrintAttributes.Builder()
          .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
          .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
          .build()
      printManager.print(jobName.ifBlank { pdf.fileName }, adapter, attributes)
      Unit
    }.onFailure { err ->
      Log.e(TAG, "Failed to launch system PrintManager for PDF", err)
    }

  fun shareCachedPdf(
    context: Context,
    pdf: CachedVisitPdf,
    titleFa: String,
  ): Result<Unit> =
    runCatching {
      require(pdf.file.exists()) { "فایل PDF در حافظه موقت یافت نشد." }
      val uri =
        pdf.contentUri
          ?: FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdf.file)
      val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
          type = "application/pdf"
          putExtra(Intent.EXTRA_STREAM, uri)
          putExtra(Intent.EXTRA_SUBJECT, titleFa.ifBlank { "گزارش پیش از ویزیت پزشک — مانو کید" })
          putExtra(Intent.EXTRA_TEXT, "گزارش آماده ویزیت پزشک (تنظیم‌شده در مانو کید)")
          clipData = ClipData.newRawUri("visit_draft_pdf", uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری فایل PDF ویزیت پزشک")
      if (findActivity(context) == null) {
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    }.onFailure { err ->
      Log.e(TAG, "Failed to launch share sheet for PDF", err)
    }

  fun openCachedPdf(context: Context, pdf: CachedVisitPdf): Result<Unit> =
    runCatching {
      require(pdf.file.exists()) { "فایل PDF در حافظه موقت یافت نشد." }
      val uri =
        pdf.contentUri
          ?: FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdf.file)
      val viewIntent =
        Intent(Intent.ACTION_VIEW).apply {
          setDataAndType(uri, "application/pdf")
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          if (findActivity(context) == null) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        }
      context.startActivity(viewIntent)
    }.onFailure { err ->
      Log.e(TAG, "No viewer available or failed to open cached PDF", err)
    }

  private fun writeJvmRobolectricPdf(data: VisitPdfDocumentData, outFile: File): Int {
    val allLines = buildList {
      add(data.titleFa)
      add(data.childSummaryLineFa)
      if (data.timelineSummaryFa.isNotBlank()) add(data.timelineSummaryFa)
      if (data.symptomContextFa.isNotBlank()) add(data.symptomContextFa)
      data.highFeverBannerTextFa?.let { add(it) }
      addAll(data.draftBodyFa.split('\n').filter { it.isNotBlank() })
      if (data.parentNoteFa.isNotBlank()) add(data.parentNoteFa)
      addAll(data.tempLogLinesFa)
      addAll(data.doseLogLinesFa)
    }
    val linesPerPage = 32
    val pageChunks = allLines.chunked(linesPerPage).ifEmpty { listOf(listOf(data.titleFa)) }
    val pageCount = pageChunks.size
    val totalObjects = 3 + (pageCount * 2) // 1=Catalog, 2=Pages, 3=Font, then pairs of (Page, Content)

    val pdfBuilder = StringBuilder()
    pdfBuilder.append("%PDF-1.4\n")
    val offsets = IntArray(totalObjects + 1)

    fun startObj(id: Int, body: String) {
      offsets[id] = pdfBuilder.toString().toByteArray(Charsets.ISO_8859_1).size
      pdfBuilder.append("$id 0 obj\n$body\nendobj\n")
    }

    val kidsRefs = (0 until pageCount).joinToString(" ") { idx -> "${4 + (idx * 2)} 0 R" }
    startObj(1, "<< /Type /Catalog /Pages 2 0 R >>")
    startObj(2, "<< /Type /Pages /Kids [ $kidsRefs ] /Count $pageCount >>")
    startObj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")

    pageChunks.forEachIndexed { idx, chunk ->
      val pageObjId = 4 + (idx * 2)
      val contentObjId = pageObjId + 1
      startObj(
        pageObjId,
        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_WIDTH $PAGE_HEIGHT] /Resources << /Font << /F1 3 0 R >> >> /Contents $contentObjId 0 R >>",
      )
      val streamBody =
        buildString {
          append("BT\n/F1 10 Tf\n40 780 Td\n16 TL\n")
          chunk.forEach { rawLine ->
            val safeAscii =
              rawLine
                .map { ch -> if (ch.code in 32..126 && ch != '(' && ch != ')' && ch != '\\') ch else ' ' }
                .joinToString("")
                .ifBlank { "Mano Visit Draft Line" }
            append("($safeAscii) Tj\nT*\n")
          }
          append("ET\n")
        }
      val streamBytesLen = streamBody.toByteArray(Charsets.ISO_8859_1).size
      startObj(contentObjId, "<< /Length $streamBytesLen >>\nstream\n$streamBody\nendstream")
    }

    val xrefOffset = pdfBuilder.toString().toByteArray(Charsets.ISO_8859_1).size
    pdfBuilder.append("xref\n0 ${totalObjects + 1}\n0000000000 65535 f \n")
    for (id in 1..totalObjects) {
      pdfBuilder.append(String.format(Locale.US, "%010d 00000 n \n", offsets[id]))
    }
    pdfBuilder.append("trailer\n<< /Size ${totalObjects + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")

    outFile.writeBytes(pdfBuilder.toString().toByteArray(Charsets.ISO_8859_1))
    return pageCount
  }

  private fun pruneOldCachedFiles(dir: File, keepCount: Int) {
    val pdfFiles =
      dir.listFiles { file -> file.isFile && file.extension.equals("pdf", ignoreCase = true) }
        ?.sortedByDescending { it.lastModified() }
        ?: return
    if (pdfFiles.size <= keepCount) return
    pdfFiles.drop(keepCount).forEach { stale ->
      runCatching { stale.delete() }
    }
  }

  private tailrec fun findActivity(context: Context): Activity? =
    when (context) {
      is Activity -> context
      is ContextWrapper -> findActivity(context.baseContext)
      else -> null
    }

  private class CachedPdfPrintAdapter(
    private val pdfFile: File,
    private val fileName: String,
    private val estimatedPageCount: Int,
  ) : PrintDocumentAdapter() {
    override fun onLayout(
      oldAttributes: PrintAttributes?,
      newAttributes: PrintAttributes?,
      cancellationSignal: CancellationSignal?,
      callback: LayoutResultCallback,
      extras: Bundle?,
    ) {
      if (cancellationSignal?.isCanceled == true) {
        callback.onLayoutCancelled()
        return
      }
      val info =
        PrintDocumentInfo.Builder(fileName)
          .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
          .setPageCount(estimatedPageCount)
          .build()
      callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
      pages: Array<out PageRange>?,
      destination: ParcelFileDescriptor,
      cancellationSignal: CancellationSignal?,
      callback: WriteResultCallback,
    ) {
      if (cancellationSignal?.isCanceled == true) {
        callback.onWriteCancelled()
        return
      }
      runCatching {
        FileInputStream(pdfFile).use { input ->
          FileOutputStream(destination.fileDescriptor).use { output ->
            input.copyTo(output)
          }
        }
      }.onSuccess {
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
      }.onFailure { err ->
        callback.onWriteFailed(err.localizedMessage ?: "خطا در خواندن فایل PDF برای چاپ")
      }
    }
  }

  private class PdfCanvasRenderer(baseTypeface: Typeface) {
    private val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)

    private val tealColor = Color.parseColor("#0D7377")
    private val tealSoftColor = Color.parseColor("#EDF7F6")
    private val infoBgColor = Color.parseColor("#F1F5FB")
    private val infoStrokeColor = Color.parseColor("#C5D6EE")
    private val dangerColor = Color.parseColor("#B71C1C")
    private val dangerBgColor = Color.parseColor("#FFEBEE")
    private val slateTextColor = Color.parseColor("#1D2939")
    private val mutedTextColor = Color.parseColor("#475467")
    private val dividerColor = Color.parseColor("#D0D5DD")

    private val headerBrandPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldTypeface
        textSize = 13f
        color = Color.WHITE
      }

    private val headerDatePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = baseTypeface
        textSize = 9.5f
        color = Color.parseColor("#DDF4F2")
      }

    private val titlePaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldTypeface
        textSize = 14.5f
        color = slateTextColor
      }

    private val sectionHeaderPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldTypeface
        textSize = 11.5f
        color = tealColor
      }

    private val bodyPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = baseTypeface
        textSize = 10.5f
        color = slateTextColor
      }

    private val metaPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = baseTypeface
        textSize = 9.5f
        color = mutedTextColor
      }

    private val dangerPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldTypeface
        textSize = 10f
        color = dangerColor
      }

    private val footerPaint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = baseTypeface
        textSize = 8.5f
        color = mutedTextColor
      }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
      }

    fun writeDocument(data: VisitPdfDocumentData, outFile: File): Int {
      val document = PdfDocument()
      var pageNumber = 1
      try {
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var cursorY = drawFirstPageHeader(canvas, data)

        fun startNextPage(): Float {
          drawPageFooter(canvas, pageNumber)
          document.finishPage(page)
          pageNumber += 1
          page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
          canvas = page.canvas
          return drawContinuationHeader(canvas, data, pageNumber)
        }

        fun ensureSpace(neededHeight: Int): Float {
          if (cursorY + neededHeight > CONTENT_BOTTOM_LIMIT) {
            cursorY = startNextPage()
          }
          return cursorY
        }

        // 1. Optional Urgent Fever Banner
        data.highFeverBannerTextFa?.let { alertText ->
          val alertLayout = buildRtlLayout(alertText, dangerPaint, CONTENT_WIDTH - 24)
          val boxHeight = alertLayout.height + 18
          ensureSpace(boxHeight + 8)
          drawRoundedBox(canvas, MARGIN_H.toFloat(), cursorY, CONTENT_WIDTH.toFloat(), boxHeight.toFloat(), dangerBgColor, dangerColor)
          drawLayout(canvas, alertLayout, MARGIN_H + 12f, cursorY + 9f)
          cursorY += boxHeight + 10f
        }

        // 2. Child Profile & Timeline Context Card
        val metaText =
          buildString {
            appendLine(data.childSummaryLineFa)
            if (data.timelineSummaryFa.isNotBlank()) {
              appendLine("خلاصه خط زمانی ۲۴س: ${data.timelineSummaryFa}")
            }
            if (data.symptomContextFa.isNotBlank()) {
              append("زمینه علائم: ${data.symptomContextFa}")
            }
          }.trim()
        val metaLayout = buildRtlLayout(metaText, metaPaint, CONTENT_WIDTH - 24)
        val metaBoxHeight = metaLayout.height + 18
        ensureSpace(metaBoxHeight + 10)
        drawRoundedBox(canvas, MARGIN_H.toFloat(), cursorY, CONTENT_WIDTH.toFloat(), metaBoxHeight.toFloat(), infoBgColor, infoStrokeColor)
        drawLayout(canvas, metaLayout, MARGIN_H + 12f, cursorY + 9f)
        cursorY += metaBoxHeight + 14f

        // 3. Gemini Visit Draft Body (multi-page safe paragraph & line pagination)
        cursorY = drawSectionHeading({ canvas }, "خلاصه بالینی تنظیم‌شده برای پزشک", cursorY, ::startNextPage)
        cursorY = drawMultilineRtlContent(data.draftBodyFa, bodyPaint, cursorY, { canvas }, ::startNextPage)
        cursorY += 10f

        // 4. Optional Parent Note
        if (data.parentNoteFa.isNotBlank()) {
          cursorY = drawSectionHeading({ canvas }, "پرسش و یادداشت والد برای پزشک", cursorY, ::startNextPage)
          cursorY = drawMultilineRtlContent(data.parentNoteFa, bodyPaint, cursorY, { canvas }, ::startNextPage)
          cursorY += 10f
        }

        // 5. Recorded 24h Timeline Entries (Temperatures + Medication Doses)
        if (data.tempLogLinesFa.isNotEmpty() || data.doseLogLinesFa.isNotEmpty()) {
          cursorY = drawSectionHeading({ canvas }, "فهرست زمانی دما و داروهای ثبت‌شده (۲۴ ساعت اخیر)", cursorY, ::startNextPage)
          if (data.tempLogLinesFa.isNotEmpty()) {
            cursorY =
              drawMultilineRtlContent(
                "ثبت‌های دما:\n${data.tempLogLinesFa.joinToString("\n")}",
                metaPaint,
                cursorY,
                { canvas },
                ::startNextPage,
              )
            cursorY += 6f
          }
          if (data.doseLogLinesFa.isNotEmpty()) {
            cursorY =
              drawMultilineRtlContent(
                "نوبت‌های دارو:\n${data.doseLogLinesFa.joinToString("\n")}",
                metaPaint,
                cursorY,
                { canvas },
                ::startNextPage,
              )
            cursorY += 8f
          }
        }

        drawPageFooter(canvas, pageNumber)
        document.finishPage(page)

        FileOutputStream(outFile).use { stream ->
          document.writeTo(stream)
          stream.flush()
        }
      } finally {
        runCatching { document.close() }
      }
      return pageNumber
    }

    private fun drawFirstPageHeader(canvas: Canvas, data: VisitPdfDocumentData): Float {
      val bannerTop = MARGIN_TOP.toFloat()
      val bannerHeight = 52f
      drawRoundedBox(canvas, MARGIN_H.toFloat(), bannerTop, CONTENT_WIDTH.toFloat(), bannerHeight, tealColor, null)

      val brandLayout = buildRtlLayout("مانو کید • برگه خلاصه ویزیت پزشک", headerBrandPaint, CONTENT_WIDTH - 24)
      drawLayout(canvas, brandLayout, MARGIN_H + 12f, bannerTop + 8f)

      val metaSub = "زمان تهیه: ${data.createdAtLabelFa}  •  مدل: ${data.generatedByModel}"
      val dateLayout = buildRtlLayout(metaSub, headerDatePaint, CONTENT_WIDTH - 24)
      drawLayout(canvas, dateLayout, MARGIN_H + 12f, bannerTop + 30f)

      var y = bannerTop + bannerHeight + 12f
      val titleLayout = buildRtlLayout(data.titleFa, titlePaint, CONTENT_WIDTH)
      drawLayout(canvas, titleLayout, MARGIN_H.toFloat(), y)
      y += titleLayout.height + 10f
      return y
    }

    private fun drawContinuationHeader(canvas: Canvas, data: VisitPdfDocumentData, pageNumber: Int): Float {
      val headerText = "مانو کید — ادامه گزارش ویزیت (صفحه $pageNumber) • ${data.titleFa}"
      val layout = buildRtlLayout(headerText, metaPaint, CONTENT_WIDTH)
      drawLayout(canvas, layout, MARGIN_H.toFloat(), MARGIN_TOP.toFloat())
      val lineY = MARGIN_TOP + layout.height + 6f
      strokePaint.color = dividerColor
      canvas.drawLine(MARGIN_H.toFloat(), lineY, (PAGE_WIDTH - MARGIN_H).toFloat(), lineY, strokePaint)
      return lineY + 12f
    }

    private fun drawSectionHeading(
      activeCanvas: () -> Canvas,
      heading: String,
      startY: Float,
      onNewPage: () -> Float,
    ): Float {
      val layout = buildRtlLayout(heading, sectionHeaderPaint, CONTENT_WIDTH)
      var y = startY
      if (y + layout.height + 24 > CONTENT_BOTTOM_LIMIT) {
        y = onNewPage()
      }
      val canvas = activeCanvas()
      drawLayout(canvas, layout, MARGIN_H.toFloat(), y)
      y += layout.height + 4f
      strokePaint.color = tealColor
      strokePaint.alpha = 80
      canvas.drawLine(MARGIN_H.toFloat(), y, (PAGE_WIDTH - MARGIN_H).toFloat(), y, strokePaint)
      strokePaint.alpha = 255
      return y + 8f
    }

    private fun drawMultilineRtlContent(
      text: String,
      paint: TextPaint,
      startY: Float,
      activeCanvas: () -> Canvas,
      onNewPage: () -> Float,
    ): Float {
      var y = startY
      val paragraphs = text.split('\n')
      for (rawPara in paragraphs) {
        val para = rawPara.trimEnd()
        if (para.isEmpty()) {
          y += 6f
          if (y > CONTENT_BOTTOM_LIMIT) {
            y = onNewPage()
          }
          continue
        }
        val layout = buildRtlLayout(para, paint, CONTENT_WIDTH)
        for (lineIdx in 0 until layout.lineCount) {
          val lineTop = layout.getLineTop(lineIdx)
          val lineBottom = layout.getLineBottom(lineIdx)
          val lineHeight = (lineBottom - lineTop).toFloat()
          if (y + lineHeight > CONTENT_BOTTOM_LIMIT) {
            y = onNewPage()
          }
          val canvas = activeCanvas()
          canvas.save()
          canvas.clipRect(MARGIN_H.toFloat(), y, (PAGE_WIDTH - MARGIN_H).toFloat(), y + lineHeight)
          canvas.translate(MARGIN_H.toFloat(), y - lineTop)
          layout.draw(canvas)
          canvas.restore()
          y += lineHeight
        }
        y += 3f
      }
      return y
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int) {
      val footerTop = (PAGE_HEIGHT - FOOTER_HEIGHT).toFloat()
      strokePaint.color = dividerColor
      canvas.drawLine(MARGIN_H.toFloat(), footerTop, (PAGE_WIDTH - MARGIN_H).toFloat(), footerTop, strokePaint)
      val disclaimer =
        "صفحه $pageNumber • تذکر بالینی: این گزارش خلاصه‌ای از داده‌های ثبت‌شده والدین برای تسهیل ویزیت پزشک است و جایگزین معاینه یا تشخیص پزشکی نیست."
      val footerLayout = buildRtlLayout(disclaimer, footerPaint, CONTENT_WIDTH)
      drawLayout(canvas, footerLayout, MARGIN_H.toFloat(), footerTop + 6f)
    }

    private fun buildRtlLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
      StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(40))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setTextDirection(TextDirectionHeuristics.RTL)
        .setLineSpacing(2f, 1.15f)
        .setIncludePad(false)
        .build()

    private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
      canvas.save()
      canvas.translate(x, y)
      layout.draw(canvas)
      canvas.restore()
    }

    private fun drawRoundedBox(
      canvas: Canvas,
      left: Float,
      top: Float,
      width: Float,
      height: Float,
      fillColor: Int,
      borderColor: Int?,
    ) {
      val rect = RectF(left, top, left + width, top + height)
      fillPaint.color = fillColor
      canvas.drawRoundRect(rect, 10f, 10f, fillPaint)
      if (borderColor != null) {
        strokePaint.color = borderColor
        canvas.drawRoundRect(rect, 10f, 10f, strokePaint)
      }
    }
  }
}
