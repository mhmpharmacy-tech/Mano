package com.example.domain.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe, PII-free in-memory audit recorder for all AI Gateway decisions.
 */
class AiAuditTrailRecorder(
  private val maxEntries: Int = 200,
) {
  private val lock = Any()
  private val _entries = MutableStateFlow<List<AiAuditEntry>>(emptyList())
  val entries: StateFlow<List<AiAuditEntry>> = _entries.asStateFlow()

  fun record(entry: AiAuditEntry) {
    synchronized(lock) {
      val updated = (listOf(entry) + _entries.value).take(maxEntries)
      _entries.value = updated
    }
  }

  fun snapshot(): List<AiAuditEntry> = synchronized(lock) { _entries.value }

  fun clear() {
    synchronized(lock) { _entries.value = emptyList() }
  }
}
