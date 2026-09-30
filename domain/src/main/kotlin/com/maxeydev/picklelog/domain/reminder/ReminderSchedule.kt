package com.maxeydev.picklelog.domain.reminder

import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.domain.datetime.AppTimeZone
import com.maxeydev.picklelog.domain.datetime.atTimeIn
import com.maxeydev.picklelog.domain.datetime.plusDays
import com.maxeydev.picklelog.domain.streak.WeekKey

object ReminderSchedule {
    private const val FRIDAY_OFFSET_FROM_MONDAY = 4
    private const val SUNDAY_OFFSET_FROM_MONDAY = 6
    private const val FIRE_HOUR = 18

    val FIRE_TIME: AppTime = AppTime(FIRE_HOUR, 0)

    fun nextFire(
        now: AppInstant,
        zone: AppTimeZone,
    ): AppInstant {
        val thisWeek = WeekKey.containing(now, zone)
        val thisFriday = fireOn(thisWeek, FRIDAY_OFFSET_FROM_MONDAY, zone)
        return if (thisFriday > now) thisFriday else fireOn(thisWeek.next(), FRIDAY_OFFSET_FROM_MONDAY, zone)
    }

    fun isLateInWeek(
        now: AppInstant,
        zone: AppTimeZone,
    ): Boolean {
        val week = WeekKey.containing(now, zone)
        val opens = fireOn(week, FRIDAY_OFFSET_FROM_MONDAY, zone)
        val closes = fireOn(week, SUNDAY_OFFSET_FROM_MONDAY, zone)
        return now >= opens && now < closes
    }

    private fun fireOn(
        week: WeekKey,
        offsetFromMonday: Int,
        zone: AppTimeZone,
    ): AppInstant = week.monday.plusDays(offsetFromMonday).atTimeIn(FIRE_TIME, zone)
}
