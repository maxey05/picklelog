package com.maxeydev.picklelog.ui.settings

import com.maxeydev.picklelog.domain.backup.ExportPromptReason

data class ExportPromptUiState(
    val reason: ExportPromptReason? = null,
)
