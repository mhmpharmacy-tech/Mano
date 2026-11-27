package com.example.domain.util

import java.text.Normalizer

object PersianNormalizer {
  private val HIGH_RISK_TERMS = listOf(
    "تشنج",
    "بیهوش",
    "بی هوش",
    "تنگی نفس",
    "نفس تنگی",
    "کبودی",
    "تورم لب",
    "مسمومیت",
    "خوردن دارو",
    "اورژانس",
    "خونریزی",
    "کم آبی شدید",
    "لکه بنفش",
    "پتشی",
    "تب نوزاد",
    "زیر ۳ ماه",
    "زیر سه ماه",
    "واکنش شدید",
    "آنافیلاکسی",
    "هذیان",
  )

  fun toAsciiDigits(input: String): String {
    if (input.isEmpty()) return ""
    val sb = StringBuilder(input.length)
    for (ch in input) {
      val mapped = when (ch) {
        in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
        in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
        '٫', '،', ',' -> '.'
        else -> ch
      }
      sb.append(mapped)
    }
    return sb.toString()
  }

  fun toPersianDigits(input: String): String {
    if (input.isEmpty()) return ""
    val sb = StringBuilder(input.length)
    for (ch in input) {
      sb.append(
        when (ch) {
          in '0'..'9' -> ('۰'.code + (ch.code - '0'.code)).toChar()
          else -> ch
        }
      )
    }
    return sb.toString()
  }

  fun normalize(input: String): String {
    if (input.isBlank()) return ""
    val asciiDigits = toAsciiDigits(input)
    val nfc = Normalizer.normalize(asciiDigits, Normalizer.Form.NFC)
    val sb = StringBuilder(nfc.length)
    for (ch in nfc) {
      when (ch) {
        'ي', 'ى', 'ئ' -> sb.append('ی')
        'ك' -> sb.append('ک')
        'ة', 'ۀ' -> sb.append('ه')
        'أ', 'إ', 'آ' -> sb.append('ا')
        'ؤ' -> sb.append('و')
        '\u200C', '\u200D', '\u200E', '\u200F' -> sb.append(' ')
        in '\u064B'..'\u065F', '\u0670' -> Unit // strip Arabic diacritics
        else -> sb.append(ch.lowercaseChar())
      }
    }
    return sb.toString().replace(Regex("\\s+"), " ").trim()
  }

  fun isHighRiskQuery(input: String): Boolean {
    val norm = normalize(input)
    if (norm.isEmpty()) return false
    return HIGH_RISK_TERMS.any { norm.contains(normalize(it)) }
  }
}

object ClinicalNumberInput {
  fun decimal(raw: String): Double? {
    val clean = PersianNormalizer.toAsciiDigits(raw)
      .trim()
      .replace(" ", "")
    if (clean.isEmpty()) return null
    val parsed = clean.toDoubleOrNull() ?: return null
    return parsed.takeIf { it.isFinite() }
  }

  fun durationHours(raw: String): Int? {
    val clean = PersianNormalizer.toAsciiDigits(raw)
      .trim()
      .replace(" ", "")
    if (clean.isEmpty()) return null
    return clean.toIntOrNull() ?: clean.toDoubleOrNull()?.toInt()
  }
}
