package com.maxeydev.picklelog.domain.onboarding

import com.maxeydev.picklelog.domain.profile.DisplayName
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import com.maxeydev.picklelog.domain.settings.AppSettingsStore
import kotlinx.coroutines.flow.first

class Onboarding(
    private val settings: AppSettingsStore,
    private val profile: ProfileRepository,
    private val matchCount: suspend () -> Int,
) {
    suspend fun isRequired(): Boolean {
        if (settings.observe().first().onboardingComplete) {
            return false
        }
        val hasName = profile.observeProfile().first().displayName.isNotBlank()
        if (hasName || matchCount() > 0) {
            settings.setOnboardingComplete(true)
            return false
        }
        return true
    }

    suspend fun complete(rawName: String): Boolean {
        if (!DisplayName.isValid(rawName)) {
            return false
        }
        profile.updateDisplayName(DisplayName.clean(rawName))
        settings.setOnboardingComplete(true)
        return true
    }
}
