package com.maxeydev.picklelog.domain.backup

import com.maxeydev.picklelog.domain.datetime.AppInstant

data class ExportPromptState(
    val trackingSince: AppInstant? = null,
    val lastExportAt: AppInstant? = null,
    val lastPromptAt: AppInstant? = null,
    val anchorCount: Int = 0,
    val pending: ExportPromptReason? = null,
)
