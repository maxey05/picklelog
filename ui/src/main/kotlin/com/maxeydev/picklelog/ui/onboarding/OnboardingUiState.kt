package com.maxeydev.picklelog.ui.onboarding

import com.maxeydev.picklelog.domain.profile.DisplayName

data class OnboardingUiState(
    val name: String = "",
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val isFinished: Boolean = false,
) {
    val canContinue: Boolean
        get() = !isSaving && (DisplayName.clean(name).isEmpty() || DisplayName.isValid(name))
}
