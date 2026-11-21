package com.example.domain.util

import com.example.domain.model.PublicPharmacyOption

/**
 * A public pharmacy invite is a navigation preference, never an authorization.
 * Only the server-provided, currently accepting pharmacy registry may resolve it.
 */
object PharmacyInvitationPolicy {
  fun resolveAcceptingPharmacyId(
    publicId: String?,
    acceptingPharmacies: List<PublicPharmacyOption>,
  ): String? {
    val id = publicId?.trim() ?: return null
    if (!ManoDomainLinks.isValidPublicPharmacyId(id)) return null
    return acceptingPharmacies.firstOrNull { it.pharmacyId == id && it.acceptingCases }?.pharmacyId
  }
}
