package com.maxeydev.picklelog.domain.backup

import kotlinx.coroutines.flow.Flow

interface ExportPromptStore {
    fun observe(): Flow<ExportPromptState>

    suspend fun update(transform: (ExportPromptState) -> ExportPromptState)
}
