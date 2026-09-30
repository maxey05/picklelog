package com.maxeydev.picklelog.domain.reminder

interface ReminderNotifier {
    fun notifyStreakAtRisk(skipAvailable: Boolean): Boolean
}
