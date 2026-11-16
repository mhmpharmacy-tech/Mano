package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyrupImageHelper {

  fun createTempScannerImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "scanned_syrups").apply { mkdirs() }
    val photoFile = File(dir, "syrup_scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
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
        if (longest > maxEdgePx) {
          val scale = maxEdgePx.toFloat() / longest.toFloat()
          val targetW = (rotated.width * scale).roundToInt().coerceAtLeast(1)
          val targetH = (rotated.height * scale).roundToInt().coerceAtLeast(1)
          val scaled = Bitmap.createScaledBitmap(rotated, targetW, targetH, true)
          if (scaled != rotated) rotated.recycle()
          scaled
        } else {
          rotated
        }
      }.getOrNull()
    }
}
