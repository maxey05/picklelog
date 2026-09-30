package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.domain.reminder.ReminderState
import com.maxeydev.picklelog.domain.reminder.ReminderStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeReminderStore(
    initial: ReminderState = ReminderState(),
) : ReminderStore {
    private val state = MutableStateFlow(initial)

    val current: ReminderState
        get() = state.value

    override fun observe(): Flow<ReminderState> = state

    override suspend fun setEnabled(enabled: Boolean) {
        state.update { it.copy(enabled = enabled) }
    }

    override suspend fun markNotified(weekOrdinal: Long?) {
        state.update { it.copy(lastNotifiedWeek = weekOrdinal) }
    }
}
