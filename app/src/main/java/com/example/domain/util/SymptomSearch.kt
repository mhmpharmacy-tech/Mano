package com.example.domain.util

import com.example.domain.model.SymptomFlow

/** Local navigation vocabulary. Search only filters choices; the family selects the pathway. */
object SymptomSearch {
  private val aliases = mapOf(
    "ear" to listOf("گوششو می‌گیره", "گوشش رو می‌کشه", "گوشش درد می‌کنه", "گوشش چرک کرده", "گوش درد"),
    "eye" to listOf("چشمش قی می‌کنه", "چشمش قرمزه", "چشمش چرک کرده", "چشماش چسبیده"),
    "runny_nose" to listOf("دماغش راه افتاده", "آب دماغ", "آب از بینیش میاد", "بینیش گرفته"),
    "cough" to listOf("سرفه می‌کنه", "خس خس می‌کنه"),
    "wet_cough" to listOf("سرفه خلطی", "سرفه با خلط", "خلط داره"),
    "fever" to listOf("تب داره", "بدنش داغه"),
    "diarrhea_vomiting" to listOf("بالا میاره", "شکمش کار می‌کنه", "مدفوع آبکی"),
    "constipation" to listOf("شکمش کار نمی‌کنه", "مدفوع سفت", "یبوست داره"),
    "diaper_rash" to listOf("پاش سوخته", "جای پوشک قرمزه"),
  )

  fun filter(flows: List<SymptomFlow>, query: String): List<SymptomFlow> {
    val term = PersianNormalizer.compact(query)
    if (term.isEmpty()) return flows
    return flows.filter { flow ->
      (listOf(flow.titleFa) + aliases[flow.id].orEmpty()).any { phrase ->
        PersianNormalizer.compact(phrase).let { it.contains(term) || (it.length >= 4 && term.contains(it)) }
      }
    }
  }
}
