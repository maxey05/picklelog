package com.maxeydev.picklelog.domain.reminder

import com.maxeydev.picklelog.domain.datetime.AppTime

data class ReminderState(
    val enabled: Boolean = false,
    val lastNotifiedWeek: Long? = null,
    val fireTime: AppTime = ReminderSchedule.DEFAULT_FIRE_TIME,
)
