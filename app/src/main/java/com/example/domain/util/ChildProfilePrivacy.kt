package com.example.domain.util

import com.example.domain.model.ChildProfileDemo
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Explicit child-profile export. Never interpolate untrusted fields into JSON:
 * quotes, newlines, backslashes and Unicode must remain valid JSON strings.
 *
 * This builds a local-only preview; sharing is a separate action controlled by the parent.
 */
object ChildProfilePrivacy {
  fun exportProfileSummaryJson(profile: ChildProfileDemo): String =
    buildJsonObject {
      put("source", "MANO_KIDS_LOCAL_SUMMARY")
      put("alias", profile.aliasFa)
      put("ageBand", profile.ageBand.labelFa)
      put("birthOrAge", profile.birthDateOrAgeFa)
      put("weightKg", profile.weightKgText)
      put("weightDate", profile.weightDateFa)
      put("allergies", profile.allergiesFa)
      put("currentMedications", profile.currentMedsFa)
    }.toString()
}
