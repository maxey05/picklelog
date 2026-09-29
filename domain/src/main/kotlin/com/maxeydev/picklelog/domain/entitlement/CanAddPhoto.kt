package com.maxeydev.picklelog.domain.entitlement

import com.maxeydev.picklelog.domain.profile.Entitlement
import com.maxeydev.picklelog.domain.profile.EntitlementRepository
import kotlinx.coroutines.flow.first

class CanAddPhoto(
    private val entitlements: EntitlementRepository,
) {
    suspend fun remainingFor(photosOnMatch: Int): Int =
        remaining(photosOnMatch, entitlements.observeEntitlement().first())

    companion object {
        fun remaining(
            photosOnMatch: Int,
            entitlement: Entitlement,
        ): Int =
            if (entitlement.isPro) {
                Int.MAX_VALUE
            } else {
                (FreeTier.PHOTOS_PER_MATCH - photosOnMatch).coerceAtLeast(0)
            }
    }
}
