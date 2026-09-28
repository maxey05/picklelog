package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.profile.Entitlement
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import com.maxeydev.picklelog.domain.profile.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeProfileRepository(
    displayName: String = "",
    isPro: Boolean = false,
) : ProfileRepository {
    private val profile =
        MutableStateFlow(
            UserProfile(
                displayName = displayName,
                createdAt = AppInstant.fromEpochMilliseconds(0),
                entitlement = Entitlement(isPro = isPro),
            ),
        )

    override fun observeProfile(): Flow<UserProfile> = profile

    override suspend fun updateDisplayName(displayName: String) {
        profile.update { it.copy(displayName = displayName) }
    }
}
