package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyrupImageHelper {

  private const val MAX_SCAN_CACHE_AGE_MS = 60L * 60L * 1000L
  private const val LOW_LIGHT_LUMA_THRESHOLD = 92f

  fun createTempScannerImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "scanned_syrups").apply { mkdirs() }
    val cutoff = System.currentTimeMillis() - MAX_SCAN_CACHE_AGE_MS
    dir.listFiles()?.forEach { file ->
      if (file.isFile && file.name.startsWith("syrup_scan_") && file.lastModified() < cutoff) {
        runCatching { file.delete() }
      }
    }
    val photoFile = File(dir, "syrup_scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
  }

  /**
   * Applies a lightweight shadow & contrast lift when a bottle/label photo is captured in dim
   * indoor lighting so Persian concentration text stays crisp for Gemini Vision.
   */
  fun enhanceLowLightIfNeeded(bitmap: Bitmap): Bitmap {
    if (bitmap.width <= 0 || bitmap.height <= 0) return bitmap
    val meanLuma = estimateMeanLuma(bitmap)
    if (meanLuma >= LOW_LIGHT_LUMA_THRESHOLD) return bitmap

    val gain = if (meanLuma < 52f) 1.38f else 1.24f
    val bias = if (meanLuma < 52f) 34f else 22f
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val cm =
      ColorMatrix(
        floatArrayOf(
          gain, 0f, 0f, 0f, bias,
          0f, gain, 0f, 0f, bias,
          0f, 0f, gain, 0f, bias,
          0f, 0f, 0f, 1f, 0f,
        )
      )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(cm) }
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return output
  }

  private fun estimateMeanLuma(bitmap: Bitmap): Float {
    val stepX = (bitmap.width / 16).coerceAtLeast(1)
    val stepY = (bitmap.height / 16).coerceAtLeast(1)
    var sum = 0.0
    var count = 0
    var y = 0
    while (y < bitmap.height) {
      var x = 0
      while (x < bitmap.width) {
        val px = bitmap.getPixel(x, y)
        val r = Color.red(px)
        val g = Color.green(px)
        val b = Color.blue(px)
        sum += 0.299 * r + 0.587 * g + 0.114 * b
        count++
        x += stepX
      }
      y += stepY
    }
    return if (count == 0) 255f else (sum / count).toFloat()
  }

  suspend fun decodeScaledBitmap(
    context: Context,
    uri: Uri,
    maxEdgePx: Int = 1280,
  ): Bitmap? =
    withContext(Dispatchers.IO) {
      runCatching {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return@runCatching null

        var sampleSize = 1
        var halfW = origW / 2
        var halfH = origH / 2
        while (max(halfW / sampleSize, halfH / sampleSize) >= maxEdgePx) {
          sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded =
          resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
          } ?: return@runCatching null

        val orientation =
          resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
              ExifInterface.TAG_ORIENTATION,
              ExifInterface.ORIENTATION_NORMAL,
            )
          } ?: ExifInterface.ORIENTATION_NORMAL

        val rotationDegrees =
          when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
          }

        val rotated =
          if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (result != decoded) decoded.recycle()
            result
          } else {
            decoded
          }

        val longest = max(rotated.width, rotated.height)
        val sized =
          if (longest > maxEdgePx) {
            val scale = maxEdgePx.toFloat() / longest.toFloat()
            val targetW = (rotated.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (rotated.height * scale).roundToInt().coerceAtLeast(1)
            val scaled = rotated.scale(targetW, targetH)
            if (scaled != rotated) rotated.recycle()
            scaled
          } else {
            rotated
          }

        val enhanced = enhanceLowLightIfNeeded(sized)
        if (enhanced != sized) sized.recycle()
        enhanced
      }.getOrNull()
    }
}
