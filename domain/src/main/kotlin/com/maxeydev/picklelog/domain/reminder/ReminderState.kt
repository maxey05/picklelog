package com.maxeydev.picklelog.domain.reminder

data class ReminderState(
    val enabled: Boolean = false,
    val lastNotifiedWeek: Long? = null,
)
