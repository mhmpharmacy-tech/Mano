package com.example.domain.util

import kotlin.math.min

/**
 * Normalizes Persian/Arabic text for resilient caregiver search:
 * - Unifies Arabic ي/ى -> ی and ك -> ک
 * - Converts Persian (۰-۹) & Arabic-Indic (٠-٩) digits -> ASCII 0-9
 * - Removes Arabic diacritics and tatweel
 * - Produces both spaced and ZWNJ-stripped compact forms so "شیر خشک" and "شیرخشک" match equally
 */
object PersianNormalizer {

  private val diacriticsRegex = Regex("[\\u064B-\\u065F\\u0670\\u0640]")
  private val zwnjRegex = Regex("[\\u200C\\u200D\\u200E\\u200F]")
  private val multiSpaceRegex = Regex("\\s+")

  fun normalize(input: String): String {
    if (input.isBlank()) return ""
    val sb = StringBuilder(input.length)
    for (ch in input) {
      when (ch) {
        'ي', 'ى', 'ئ' -> sb.append('ی')
        'ك' -> sb.append('ک')
        'ة' -> sb.append('ه')
        'أ', 'إ', 'آ' -> sb.append('ا')
        'ؤ' -> sb.append('و')
        in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
        in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
        else -> sb.append(ch.lowercaseChar())
      }
    }
    val noDiacritics = diacriticsRegex.replace(sb.toString(), "")
    val zwnjAsSpace = zwnjRegex.replace(noDiacritics, " ")
    return multiSpaceRegex.replace(zwnjAsSpace, " ").trim()
  }

  fun compact(input: String): String = normalize(input).replace(" ", "")

  fun scoreMatch(query: String, vararg fields: String): Int {
    val normQuery = normalize(query)
    val compactQuery = compact(query)
    if (normQuery.isBlank()) return 1

    var bestScore = 0
    val tokens = normQuery.split(" ").filter { it.length >= 2 }

    for ((index, rawField) in fields.withIndex()) {
      val weightBoost = if (index == 0) 40 else 15
      val normField = normalize(rawField)
      val compactField = compact(rawField)

      if (normField == normQuery || compactField == compactQuery) {
        bestScore += 100 + weightBoost
      } else if (normField.contains(normQuery) || compactField.contains(compactQuery)) {
        bestScore += 65 + weightBoost
      } else {
        var tokenHits = 0
        for (token in tokens) {
          val compactToken = token.replace(" ", "")
          if (normField.contains(token) || compactField.contains(compactToken)) {
            tokenHits++
          } else if (hasMildTypoMatch(compactToken, normField)) {
            tokenHits++
          }
        }
        if (tokenHits > 0) {
          bestScore += tokenHits * 25 + (if (index == 0) 15 else 0)
        } else if (compactQuery.length >= 4 && hasMildTypoMatch(compactQuery, normField)) {
          bestScore += 20
        }
      }
    }
    return bestScore
  }

  fun isHighRiskQuery(query: String): Boolean {
    val compactQ = compact(query)
    if (compactQ.length < 2) return false
    val riskKeywords =
      listOf(
        "تبنوزاد",
        "تب",
        "تشنج",
        "تنگینفس",
        "نفس",
        "خسخس",
        "کبودی",
        "کما",
        "بیهوش",
        "کمابی",
        "مسمومیت",
        "خونریزی",
        "اورژانس",
        "استامینوفن",
        "ایبوپروفن",
        "دوز",
      )
    return riskKeywords.any { compactQ.contains(it) }
  }

  private fun hasMildTypoMatch(needle: String, haystackNorm: String): Boolean {
    if (needle.length < 4) return false
    val words = haystackNorm.split(" ").filter { it.length >= 4 }
    return words.any { word ->
      if (kotlin.math.abs(word.length - needle.length) > 1) return@any false
      levenshtein(needle, word) <= 1
    }
  }

  private fun levenshtein(a: String, b: String): Int {
    val dp = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
      var prev = dp[0]
      dp[0] = i
      for (j in 1..b.length) {
        val temp = dp[j]
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + cost)
        prev = temp
      }
    }
    return dp[b.length]
  }
}
