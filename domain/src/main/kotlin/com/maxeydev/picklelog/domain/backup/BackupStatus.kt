package com.maxeydev.picklelog.domain.backup

import com.maxeydev.picklelog.domain.datetime.AppInstant

data class BackupStatus(
    val lastExportAt: AppInstant? = null,
    val pendingPrompt: ExportPromptReason? = null,
)
