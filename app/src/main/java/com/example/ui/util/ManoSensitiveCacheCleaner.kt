package com.example.ui.util

import android.content.Context
import java.io.File

/**
 * Removes only MANO-owned temporary clinical exports and camera/share images.
 * Caller must separately clear DataStore, widget state and any active in-memory views.
 * This is a device-cache cleanup, NOT Firebase account deletion.
 */
object ManoSensitiveCacheCleaner {
  private val sensitiveSubdirectories = listOf(
    "visit_draft_pdfs",
    "scanned_syrups",
    "shared_cards",
  )

  fun clear(context: Context): Boolean {
    val root = context.cacheDir.canonicalFile
    val results = sensitiveSubdirectories.map { childName ->
      runCatching {
        val directory = File(root, childName)
        // Never follow a replaced directory symlink outside this app's private cache root.
        val canonical = directory.canonicalFile
        if (canonical.parentFile != root) {
          false
        } else {
          !directory.exists() || directory.deleteRecursively()
        }
      }.getOrDefault(false)
    }
    return results.all { it }
  }
}
