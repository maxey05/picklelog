package com.maxeydev.picklelog.domain.settings

data class AppSettings(
    val darkTheme: Boolean? = null,
    val onboardingComplete: Boolean = false,
    val soundEffectsEnabled: Boolean = true,
)
