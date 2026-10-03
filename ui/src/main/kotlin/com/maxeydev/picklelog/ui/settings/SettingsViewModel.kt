package com.maxeydev.picklelog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maxeydev.picklelog.domain.backup.BackupRepository
import com.maxeydev.picklelog.domain.billing.ProStore
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.domain.erase.EraseAllData
import com.maxeydev.picklelog.domain.profile.DisplayName
import com.maxeydev.picklelog.domain.profile.EntitlementRepository
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import com.maxeydev.picklelog.domain.reminder.ReminderStore
import com.maxeydev.picklelog.domain.reminder.StreakReminder
import com.maxeydev.picklelog.domain.settings.AppSettingsStore
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.paywall.PaywallViewModel
import kotlinx.coroutines.CancellationException
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
    private val profileRepository: ProfileRepository,
    private val appSettingsStore: AppSettingsStore,
    private val eraseAllData: EraseAllData,
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
                mutableUiState.update { it.copy(reminderEnabled = state.enabled, reminderTime = state.fireTime) }
            }
        }
        viewModelScope.launch {
            profileRepository.observeProfile().collect { profile ->
                mutableUiState.update { current ->
                    val draftFollowsSavedName = current.nameDraft == current.displayName
                    current.copy(
                        displayName = profile.displayName,
                        nameDraft = if (draftFollowsSavedName) profile.displayName else current.nameDraft,
                    )
                }
            }
        }
        viewModelScope.launch {
            appSettingsStore.observe().collect { settings ->
                mutableUiState.update { it.copy(darkTheme = settings.darkTheme) }
            }
        }
    }

    fun changeName(raw: String) {
        mutableUiState.update { it.copy(nameDraft = DisplayName.limit(raw)) }
    }

    fun saveName() {
        val draft = mutableUiState.value.nameDraft
        if (!mutableUiState.value.canSaveName) {
            return
        }
        val cleaned = DisplayName.clean(draft)
        mutableUiState.update { it.copy(nameDraft = cleaned) }
        viewModelScope.launch { profileRepository.updateDisplayName(cleaned) }
    }

    fun changeDarkTheme(dark: Boolean) {
        viewModelScope.launch { appSettingsStore.setDarkTheme(dark) }
    }

    fun enableReminder() {
        viewModelScope.launch { streakReminder.enable() }
    }

    fun disableReminder() {
        viewModelScope.launch { streakReminder.disable() }
    }

    fun changeReminderTime(time: AppTime) {
        viewModelScope.launch { streakReminder.setFireTime(time) }
    }

    fun eraseAll() {
        if (mutableUiState.value.isErasing) {
            return
        }
        mutableUiState.update { it.copy(isErasing = true, eraseFailed = false) }
        viewModelScope.launch {
            try {
                eraseAllData()
                mutableUiState.update { it.copy(isErasing = false, isErased = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableUiState.update { it.copy(isErasing = false, eraseFailed = true) }
            }
        }
    }

    fun dismissEraseFailure() {
        mutableUiState.update { it.copy(eraseFailed = false) }
    }

    fun erasedHandled() {
        mutableUiState.update { it.copy(isErased = false) }
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
                        profileRepository = dependencies.profileRepository,
                        appSettingsStore = dependencies.appSettingsStore,
                        eraseAllData = dependencies.eraseAllData,
                    )
                }
            }
    }
}
