package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PrintedPdfDocument
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
import com.example.domain.util.ManoDomainLinks
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.FileOutputStream

/**
 * Offline, non-identifying pharmacy QR and Android native-print card.
 *
 * A public invitation contains ONLY the public pharmacy registry ID; no child
 * identity, consultation details, medical information, tokens or tracking IDs.
 * QR generation does not call an external endpoint.
 */
object PharmacyQrCard {
  private const val PREFIX = "https://manokid.ir/pharmacist?pharmacy="

  fun isSafeInvitation(url: String): Boolean {
    if (!url.startsWith(PREFIX)) return false
    val id = url.removePrefix(PREFIX)
    return ManoDomainLinks.pharmacyInvitationUrl(id) == url
  }

  fun createQrBitmap(url: String, sizePx: Int = 420): Bitmap? {
    if (!isSafeInvitation(url) || sizePx !in 180..1200) return null
    val encoded = QRCodeWriter().encode(
      url,
      BarcodeFormat.QR_CODE,
      sizePx,
      sizePx,
      mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 4,
      ),
    )
    val pixels = IntArray(sizePx * sizePx) { index ->
      if (encoded[index % sizePx, index / sizePx]) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
  }

  /** Uses Android PrintManager. A PDF is created by the system print workflow, not uploaded. */
  fun printStarterCard(
    context: Context,
    invitationUrl: String,
    pharmacyNameFa: String,
  ): Result<Unit> = runCatching {
    require(isSafeInvitation(invitationUrl)) { "Invalid pharmacy invitation URL" }
    val qr = requireNotNull(createQrBitmap(invitationUrl, 760)) { "QR rendering failed" }
    val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
      ?: error("Android PrintManager unavailable")
    val safeName = pharmacyNameFa.trim().take(70).ifBlank { "داروخانه منتخب" }
    manager.print(
      "MANOKID Pharmacy starter card",
      object : PrintDocumentAdapter() {
        private var attributes: PrintAttributes? = null

        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes,
          cancellationSignal: CancellationSignal,
          callback: LayoutResultCallback,
          extras: android.os.Bundle?,
        ) {
          if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
          }
          attributes = newAttributes
          callback.onLayoutFinished(
            PrintDocumentInfo.Builder("manokid-pharmacy-card.pdf")
              .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
              .setPageCount(1)
              .build(),
            true,
          )
        }

        override fun onWrite(
          pages: Array<out PageRange>,
          destination: ParcelFileDescriptor,
          cancellationSignal: CancellationSignal,
          callback: WriteResultCallback,
        ) {
          if (cancellationSignal.isCanceled) {
            callback.onWriteCancelled()
            return
          }
          val options = attributes
          if (options == null) {
            callback.onWriteFailed("Print layout has not been created.")
            return
          }
          val pdf = PrintedPdfDocument(context, options)
          try {
            val page = pdf.startPage(0)
            drawCard(page.canvas, page.info.contentRect, qr, safeName, invitationUrl)
            pdf.finishPage(page)
            FileOutputStream(destination.fileDescriptor).use { stream ->
              pdf.writeTo(stream)
            }
            if (cancellationSignal.isCanceled) callback.onWriteCancelled()
            else callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (_: Exception) {
            callback.onWriteFailed("Could not create the PDF card.")
          } finally {
            pdf.close()
          }
        }
      },
      PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        .build(),
    )
  }

  private fun drawCard(
    canvas: Canvas,
    content: android.graphics.Rect,
    qr: Bitmap,
    nameFa: String,
    url: String,
  ) {
    canvas.drawColor(Color.WHITE)
    val center = content.exactCenterX()
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(14, 90, 85)
      textSize = 33f
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    canvas.drawText("MANOKID", center, content.top + 75f, titlePaint)
    drawPersian(canvas, "همراه آموزشی سلامت کودک", content, content.top + 93f, 17f)
    drawPersian(canvas, nameFa, content, content.top + 138f, 21f)

    val qrSide = minOf(content.width() * 0.60f, 290f)
    val qrTop = content.top + 180f
    canvas.drawBitmap(
      qr,
      null,
      RectF(center - qrSide / 2, qrTop, center + qrSide / 2, qrTop + qrSide),
      Paint(Paint.FILTER_BITMAP_FLAG),
    )

    drawPersian(canvas, "برای مشاهده اطلاعات عمومی و انتخاب داروخانه، کد را اسکن کنید.",
      content, qrTop + qrSide + 17f, 14f)
    val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(44, 68, 75)
      textSize = 10f
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText(url, center, qrTop + qrSide + 67f, urlPaint)
    drawPersian(canvas,
      "این کد نوبت‌دهی، پاسخ فوری یا تجویز دارو را تضمین نمی‌کند. " +
        "ثبت پرسش غیراضطراری نیازمند ورود، رضایت والد و تأیید پذیرش داروخانه است.",
      content, qrTop + qrSide + 100f, 12f)
    drawPersian(canvas,
      "در وضعیت اورژانسی از این لینک استفاده نکنید و فوراً با خدمات درمانی تماس بگیرید.",
      content, qrTop + qrSide + 151f, 12f)
    drawPersian(canvas, "تا زمان فعال‌شدن دامنه و تأیید App Links، لینک را عمومی توزیع نکنید.",
      content, qrTop + qrSide + 184f, 11f)
  }

  private fun drawPersian(
    canvas: Canvas,
    text: String,
    content: android.graphics.Rect,
    top: Float,
    size: Float,
  ) {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(38, 56, 62)
      textSize = size
      typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, content.width())
      .setAlignment(Layout.Alignment.ALIGN_CENTER)
      .setTextDirection(TextDirectionHeuristics.RTL)
      .setIncludePad(false)
      .build()
    canvas.save()
    canvas.translate(content.left.toFloat(), top)
    layout.draw(canvas)
    canvas.restore()
  }
}
