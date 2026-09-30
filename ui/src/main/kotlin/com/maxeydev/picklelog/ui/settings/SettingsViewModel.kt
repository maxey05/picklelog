package com.maxeydev.picklelog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklelog.domain.backup.BackupRepository
import com.maxeydev.picklelog.domain.billing.ProStore
import com.maxeydev.picklelog.domain.profile.EntitlementRepository
import com.maxeydev.picklelog.domain.reminder.ReminderStore
import com.maxeydev.picklelog.domain.reminder.StreakReminder
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.paywall.PaywallViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val proStore: ProStore,
    entitlementRepository: EntitlementRepository,
    backupRepository: BackupRepository,
    reminderStore: ReminderStore,
    private val streakReminder: StreakReminder,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            entitlementRepository.observeEntitlement().collect { entitlement ->
                mutableUiState.update { it.copy(hasPro = entitlement.isPro) }
            }
        }
        viewModelScope.launch {
            backupRepository.observeStatus().collect { status ->
                mutableUiState.update { it.copy(lastExportAt = status.lastExportAt) }
            }
        }
        viewModelScope.launch {
            reminderStore.observe().collect { state ->
                mutableUiState.update { it.copy(reminderEnabled = state.enabled) }
            }
        }
    }

    fun enableReminder() {
        viewModelScope.launch { streakReminder.enable() }
    }

    fun disableReminder() {
        viewModelScope.launch { streakReminder.disable() }
    }

    fun restore() {
        if (mutableUiState.value.isRestoring) {
            return
        }
        mutableUiState.update { it.copy(isRestoring = true, restoreMessage = null) }
        viewModelScope.launch {
            val message = PaywallViewModel.restoreMessage(proStore.restore())
            mutableUiState.update { it.copy(isRestoring = false, restoreMessage = message) }
        }
    }

    companion object {
        fun factory(dependencies: PicklelogDependencies): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SettingsViewModel(
                        proStore = dependencies.proStore,
                        entitlementRepository = dependencies.entitlementRepository,
                        backupRepository = dependencies.backupRepository,
                        reminderStore = dependencies.reminderStore,
                        streakReminder = dependencies.streakReminder,
                    )
                }
            }
    }
}
