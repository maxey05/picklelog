package com.maxeydev.picklelog.ui.match

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.ui.R
import java.util.Locale
import kotlin.time.Duration

private const val MINUTES_PER_HOUR = 60L

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun resultLabel(result: MatchResult): String =
    when (result) {
        MatchResult.WIN -> stringResource(R.string.result_win)
        MatchResult.LOSS -> stringResource(R.string.result_loss)
    }

@Composable
fun formatLabel(format: MatchFormat): String =
    when (format) {
        MatchFormat.SINGLES -> stringResource(R.string.format_singles)
        MatchFormat.DOUBLES -> stringResource(R.string.format_doubles)
    }

@Composable
fun durationLine(
    duration: Duration,
    endsNextDay: Boolean,
): String {
    val totalMinutes = duration.inWholeMinutes
    val hours = (totalMinutes / MINUTES_PER_HOUR).toInt()
    val minutes = (totalMinutes % MINUTES_PER_HOUR).toInt()
    val amount =
        if (hours > 0) {
            stringResource(R.string.duration_hours_minutes, hours, minutes)
        } else {
            stringResource(R.string.duration_minutes, minutes)
        }
    return if (endsNextDay) {
        stringResource(R.string.duration_line_next_day, amount)
    } else {
        stringResource(R.string.duration_line, amount)
    }
}
