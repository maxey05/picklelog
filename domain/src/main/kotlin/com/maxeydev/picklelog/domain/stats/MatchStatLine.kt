package com.maxeydev.picklelog.domain.stats

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult

data class MatchStatLine(
    val date: AppDate,
    val format: MatchFormat,
    val result: MatchResult,
)
