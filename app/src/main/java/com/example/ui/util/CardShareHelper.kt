package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object CardShareHelper {
  private const val TAG = "CardShareHelper"
  private const val MAX_INPUT_DIMENSION = 1024

  fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
      }
      val maxDim = max(bounds.outWidth, bounds.outHeight)
      val sampleSize =
        if (maxDim > MAX_INPUT_DIMENSION) {
          var s = 1
          while (maxDim / s > MAX_INPUT_DIMENSION) {
            s *= 2
          }
          s
        } else {
          1
        }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val decoded =
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return@runCatching null

      if (max(decoded.width, decoded.height) <= MAX_INPUT_DIMENSION) {
        decoded
      } else {
        val ratio = MAX_INPUT_DIMENSION.toFloat() / max(decoded.width, decoded.height).toFloat()
        val targetW = (decoded.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
        decoded.scale(targetW, targetH)
      }
    }.onFailure { err ->
      Log.e(TAG, "Failed to decode picked bitmap", err)
    }.getOrNull()
  }

  fun shareCareCard(context: Context, bitmap: Bitmap, title: String): Result<Unit> {
    return runCatching {
      val cacheDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
      val cutoff = System.currentTimeMillis() - (2L * 60L * 60L * 1000L)
      cacheDir.listFiles()?.forEach { file ->
        if (file.isFile && file.name.startsWith("mano_card_") && file.lastModified() < cutoff) {
          runCatching { file.delete() }
        }
      }
      val outFile = File(cacheDir, "mano_card_${System.currentTimeMillis()}.png")
      FileOutputStream(outFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
      }
      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
      val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
          type = "image/png"
          putExtra(Intent.EXTRA_STREAM, uri)
          putExtra(Intent.EXTRA_SUBJECT, "کارت آموزشی کودک مانو کید")
          putExtra(Intent.EXTRA_TEXT, title)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری کارت آموزشی")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    }
  }
}
