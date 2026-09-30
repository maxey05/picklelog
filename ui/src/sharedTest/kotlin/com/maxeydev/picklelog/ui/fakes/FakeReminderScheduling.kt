package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.reminder.ReminderScheduling

class FakeReminderScheduling : ReminderScheduling {
    val scheduled = mutableListOf<AppInstant>()
    var cancelCount = 0
        private set

    override fun scheduleNext(fireAt: AppInstant) {
        scheduled += fireAt
    }

    override fun cancel() {
        cancelCount += 1
    }
}
