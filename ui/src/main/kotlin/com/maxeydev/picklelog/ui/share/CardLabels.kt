package com.maxeydev.picklelog.ui.share

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.GameScore
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult

interface CardLabels {
    val brand: String

    fun result(result: MatchResult): String

    fun meta(
        format: MatchFormat,
        date: AppDate,
    ): String

    fun opponents(names: List<String>): String?

    fun partner(name: String): String

    fun score(games: List<GameScore>): String?

    fun streak(weeks: Int): String?
}
