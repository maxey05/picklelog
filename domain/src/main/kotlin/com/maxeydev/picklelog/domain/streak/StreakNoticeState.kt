package com.maxeydev.picklelog.domain.streak

data class StreakNoticeState(
    val acknowledgedSkipWeek: Long? = null,
    val acknowledgedMissedWeek: Long? = null,
)
