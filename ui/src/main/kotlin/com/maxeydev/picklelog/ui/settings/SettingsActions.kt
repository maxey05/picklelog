package com.maxeydev.picklelog.ui.settings

import com.maxeydev.picklelog.domain.datetime.AppTime

class SettingsActions(
    val onClose: () -> Unit,
    val onSeePro: () -> Unit,
    val onOpenBackup: () -> Unit,
    val onOpenPrivacy: () -> Unit,
    val onOpenAbout: () -> Unit,
    val onRateUs: () -> Unit,
    val onClearCache: () -> Unit,
    val onEnableReminder: () -> Unit,
    val onDisableReminder: () -> Unit,
    val onReminderTimeChanged: (AppTime) -> Unit,
    val onNameChanged: (String) -> Unit,
    val onSaveName: () -> Unit,
    val onNameEditCancelled: () -> Unit,
    val onDarkThemeChanged: (Boolean) -> Unit,
    val onEraseConfirmed: () -> Unit,
    val onEraseFailureDismissed: () -> Unit,
)
