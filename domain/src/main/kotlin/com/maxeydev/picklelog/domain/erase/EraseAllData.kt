package com.maxeydev.picklelog.domain.erase

import com.maxeydev.picklelog.domain.reminder.StreakReminder

class EraseAllData(
    private val reminder: StreakReminder,
    private val eraser: LocalDataEraser,
) {
    suspend operator fun invoke() {
        reminder.disable()
        eraser.erase()
    }
}
