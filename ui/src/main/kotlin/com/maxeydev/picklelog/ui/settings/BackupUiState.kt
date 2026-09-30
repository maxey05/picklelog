package com.maxeydev.picklelog.ui.settings

import com.maxeydev.picklelog.domain.backup.ExportOutcome
import com.maxeydev.picklelog.domain.backup.ImportSummary
import com.maxeydev.picklelog.domain.datetime.AppInstant

data class BackupUiState(
    val lastExportAt: AppInstant? = null,
    val isExportDialogVisible: Boolean = false,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val pendingShare: ExportOutcome.Ready? = null,
    val importSummary: ImportSummary? = null,
    val message: BackupMessage? = null,
) {
    val isBusy: Boolean
        get() = isExporting || isImporting
}
