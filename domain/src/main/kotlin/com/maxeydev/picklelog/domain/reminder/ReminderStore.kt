package com.maxeydev.picklelog.domain.reminder

import kotlinx.coroutines.flow.Flow

interface ReminderStore {
    fun observe(): Flow<ReminderState>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun markNotified(weekOrdinal: Long?)
}
