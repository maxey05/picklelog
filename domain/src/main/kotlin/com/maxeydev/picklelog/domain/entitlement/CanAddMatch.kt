package com.maxeydev.picklelog.domain.entitlement

import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.profile.Entitlement
import com.maxeydev.picklelog.domain.profile.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class CanAddMatch(
    private val matches: MatchRepository,
    private val entitlements: EntitlementRepository,
) {
    fun observe(): Flow<Boolean> =
        combine(matches.observeMatchCount(), entitlements.observeEntitlement()) { savedMatches, entitlement ->
            allows(savedMatches, entitlement)
        }

    suspend operator fun invoke(): Boolean = observe().first()

    companion object {
        fun allows(
            savedMatches: Int,
            entitlement: Entitlement,
        ): Boolean = entitlement.isPro || savedMatches < FreeTier.MATCH_LIMIT
    }
}
