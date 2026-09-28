package com.maxeydev.picklelog.ui.share

import android.content.Context
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.GameScore
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.formatMatchDate

class ResourceCardLabels(
    private val context: Context,
) : CardLabels {
    private val resources = context.resources

    override val brand: String
        get() = resources.getString(R.string.card_brand)

    override fun result(result: MatchResult): String =
        when (result) {
            MatchResult.WIN -> resources.getString(R.string.result_win)
            MatchResult.LOSS -> resources.getString(R.string.result_loss)
        }

    override fun meta(
        format: MatchFormat,
        date: AppDate,
    ): String {
        val formatName =
            when (format) {
                MatchFormat.SINGLES -> resources.getString(R.string.format_singles)
                MatchFormat.DOUBLES -> resources.getString(R.string.format_doubles)
            }
        val locale = resources.configuration.locales[0]
        return resources.getString(R.string.list_format_and_date, formatName, formatMatchDate(date, locale))
    }

    override fun opponents(names: List<String>): String? =
        when (names.size) {
            0 -> null
            1 -> resources.getString(R.string.list_versus_one, names[0])
            else -> resources.getString(R.string.list_versus_two, names[0], names[1])
        }

    override fun partner(name: String): String = resources.getString(R.string.card_partner, name)

    override fun score(games: List<GameScore>): String? {
        if (games.isEmpty()) {
            return null
        }
        val separator = resources.getString(R.string.card_score_separator)
        return games.joinToString(separator) { resources.getString(R.string.list_score, it.myScore, it.opponentScore) }
    }

    override fun streak(weeks: Int): String? =
        if (weeks > 0) {
            resources.getQuantityString(R.plurals.dashboard_streak_weeks, weeks, weeks)
        } else {
            null
        }
}
