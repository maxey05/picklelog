package com.maxeydev.picklelog.ui.share

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.GameScore
import com.maxeydev.picklelog.domain.match.MatchFormat
import kotlin.time.Duration

interface CardLabels {
    val brand: String

    fun meta(
        format: MatchFormat,
        date: AppDate,
    ): String

    fun partner(name: String): CardEntry

    fun time(duration: Duration): CardEntry

    fun opponents(names: List<String>): CardEntry?

    fun games(games: List<GameScore>): CardEntry?
}
