package com.maxeydev.picklelog.domain.reminder

import com.maxeydev.picklelog.domain.datetime.AppInstant

interface ReminderScheduling {
    fun scheduleNext(fireAt: AppInstant)

    fun cancel()
}
