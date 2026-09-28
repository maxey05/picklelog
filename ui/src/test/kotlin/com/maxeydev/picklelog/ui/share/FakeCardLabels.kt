package com.maxeydev.picklelog.ui.share

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.GameScore
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult

class FakeCardLabels : CardLabels {
    override val brand: String = "Picklelog"

    override fun result(result: MatchResult): String = result.name.lowercase().replaceFirstChar { it.uppercase() }

    override fun meta(
        format: MatchFormat,
        date: AppDate,
    ): String = "${format.name} · $date"

    override fun opponents(names: List<String>): String? = names.takeIf { it.isNotEmpty() }?.joinToString(" & ", "vs ")

    override fun partner(name: String): String = "with $name"

    override fun score(games: List<GameScore>): String? =
        games.takeIf { it.isNotEmpty() }?.joinToString(" · ") { "${it.myScore}–${it.opponentScore}" }

    override fun streak(weeks: Int): String? = if (weeks > 0) "$weeks-week streak" else null
}
