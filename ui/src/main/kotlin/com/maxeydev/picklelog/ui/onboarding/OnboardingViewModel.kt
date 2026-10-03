package com.maxeydev.picklelog.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklelog.domain.onboarding.Onboarding
import com.maxeydev.picklelog.domain.profile.DisplayName
import com.maxeydev.picklelog.ui.PicklelogDependencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class OnboardingViewModel(
    private val onboarding: Onboarding,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    fun changeName(raw: String) {
        mutableUiState.update { it.copy(name = DisplayName.limit(raw), saveFailed = false) }
    }

    fun continueToApp() {
        val current = mutableUiState.value
        if (!current.canContinue) {
            return
        }
        mutableUiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val completed = onboarding.complete(current.name)
                mutableUiState.update { it.copy(isSaving = false, isFinished = completed) }
            } catch (unwritable: IOException) {
                mutableUiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    companion object {
        fun factory(dependencies: PicklelogDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { OnboardingViewModel(dependencies.onboarding) }
            }
    }
}
