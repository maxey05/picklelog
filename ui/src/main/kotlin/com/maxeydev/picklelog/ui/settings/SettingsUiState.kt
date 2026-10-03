package com.maxeydev.picklelog.ui.settings

import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.domain.profile.DisplayName
import com.maxeydev.picklelog.domain.reminder.ReminderSchedule
import com.maxeydev.picklelog.ui.paywall.StoreMessage

data class SettingsUiState(
    val hasPro: Boolean = false,
    val isRestoring: Boolean = false,
    val restoreMessage: StoreMessage? = null,
    val lastExportAt: AppInstant? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: AppTime = ReminderSchedule.DEFAULT_FIRE_TIME,
    val displayName: String = "",
    val nameDraft: String = "",
    val darkTheme: Boolean? = null,
    val isErasing: Boolean = false,
    val eraseFailed: Boolean = false,
    val isErased: Boolean = false,
) {
    val canSaveName: Boolean
        get() = DisplayName.isValid(nameDraft) && DisplayName.clean(nameDraft) != displayName
}
