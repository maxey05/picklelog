package com.maxeydev.picklelog.ui.settings

import com.maxeydev.picklelog.domain.datetime.AppTime

class SettingsActions(
    val onBack: () -> Unit,
    val onSeePro: () -> Unit,
    val onRestore: () -> Unit,
    val onOpenBackup: () -> Unit,
    val onOpenAbout: () -> Unit,
    val onEnableReminder: () -> Unit,
    val onDisableReminder: () -> Unit,
    val onReminderTimeChanged: (AppTime) -> Unit,
    val onNameChanged: (String) -> Unit,
    val onSaveName: () -> Unit,
    val onDarkThemeChanged: (Boolean) -> Unit,
    val onEraseConfirmed: () -> Unit,
    val onEraseFailureDismissed: () -> Unit,
)
