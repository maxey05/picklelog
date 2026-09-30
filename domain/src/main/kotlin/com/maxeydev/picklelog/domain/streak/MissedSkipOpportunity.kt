package com.maxeydev.picklelog.domain.streak

data class MissedSkipOpportunity(
    val missedWeek: WeekKey,
    val brokenStreakWeeks: Int,
)
