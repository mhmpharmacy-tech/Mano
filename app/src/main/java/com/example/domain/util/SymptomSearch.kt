package com.example.domain.util

import com.example.domain.model.SymptomFlow

object SymptomSearch {
  fun filter(flows: List<SymptomFlow>, query: String): List<SymptomFlow> {
    val normalized = PersianNormalizer.normalize(query)
    if (normalized.isBlank()) return flows
    val tokens = normalized.split(' ').filter { it.isNotBlank() }
    return flows.filter { flow ->
      val haystack =
        PersianNormalizer.normalize(
          buildString {
            append(flow.titleFa)
            append(' ')
            append(flow.subtitleFa)
            append(' ')
            append(flow.redFlags.joinToString(" ") { it.labelFa })
            append(' ')
            append(flow.educationalStepsFa.joinToString(" "))
            append(' ')
            append(flow.escalationCriteriaFa.joinToString(" "))
          }
        )
      tokens.all { token -> haystack.contains(token) }
    }
  }
}
