package com.maxeydev.picklelog.ui.settings

import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.ui.paywall.StoreMessage

data class SettingsUiState(
    val hasPro: Boolean = false,
    val isRestoring: Boolean = false,
    val restoreMessage: StoreMessage? = null,
    val lastExportAt: AppInstant? = null,
    val reminderEnabled: Boolean = false,
)
