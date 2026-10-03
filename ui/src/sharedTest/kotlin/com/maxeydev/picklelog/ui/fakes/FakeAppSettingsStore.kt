package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.domain.settings.AppSettings
import com.maxeydev.picklelog.domain.settings.AppSettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeAppSettingsStore(
    initial: AppSettings = AppSettings(onboardingComplete = true),
) : AppSettingsStore {
    private val state = MutableStateFlow(initial)

    val current: AppSettings
        get() = state.value

    override fun observe(): Flow<AppSettings> = state

    override suspend fun setDarkTheme(dark: Boolean) {
        state.update { it.copy(darkTheme = dark) }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        state.update { it.copy(onboardingComplete = complete) }
    }
}
