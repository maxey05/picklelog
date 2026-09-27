package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.match.GameScore
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.common.MatchThumbnail
import com.maxeydev.picklelog.ui.match.currentLocale
import com.maxeydev.picklelog.ui.match.formatLabel
import com.maxeydev.picklelog.ui.match.formatMatchDate
import com.maxeydev.picklelog.ui.match.formatMatchDateLong
import com.maxeydev.picklelog.ui.match.resultLabel
import java.util.Locale

private val ROW_MIN_HEIGHT = 72.dp
private val BADGE_MIN_WIDTH = 56.dp
private val BADGE_ICON_SIZE = 18.dp
private val SCORE_SPACING = 12.dp

@Composable
fun MatchRow(
    state: MatchRowUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val sentence = matchRowSentence(state, locale)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ROW_MIN_HEIGHT)
                .clickable(onClickLabel = stringResource(R.string.open_match_details), onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = sentence }
                .testTag(MatchListTestTags.row(state.id))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ResultBadge(result = state.result)
        MatchRowText(state = state, locale = locale, modifier = Modifier.weight(1f))
        state.thumbnailPath?.let { path ->
            MatchThumbnail(path = path, modifier = Modifier.testTag(MatchListTestTags.THUMBNAIL))
        }
    }
}

@Composable
private fun ResultBadge(result: MatchResult) {
    val isWin = result == MatchResult.WIN
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (isWin) colors.primaryContainer else colors.surfaceVariant,
        contentColor = if (isWin) colors.onPrimaryContainer else colors.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.widthIn(min = BADGE_MIN_WIDTH).testTag(MatchListTestTags.RESULT_BADGE),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Icon(
                painter = painterResource(if (isWin) R.drawable.ic_check else R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier.size(BADGE_ICON_SIZE),
            )
            Text(text = resultLabel(result), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MatchRowText(
    state: MatchRowUiState,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val format = formatLabel(state.format)
    val date = formatMatchDate(state.date, locale)
    val versus = versusLine(state.opponentNames)
    Column(
        modifier = modifier.testTag(MatchListTestTags.TEXT_COLUMN),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = versus ?: format,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(MatchListTestTags.HEADLINE),
        )
        Text(
            text = if (versus == null) date else stringResource(R.string.list_format_and_date, format, date),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(MatchListTestTags.DETAILS),
        )
        if (state.games.isNotEmpty()) {
            ScoreSummary(games = state.games, modifier = Modifier.testTag(MatchListTestTags.SCORES))
        }
    }
}

@Composable
private fun versusLine(opponentNames: List<String>): String? =
    when (opponentNames.size) {
        0 -> null
        1 -> stringResource(R.string.list_versus_one, opponentNames[0])
        else -> stringResource(R.string.list_versus_two, opponentNames[0], opponentNames[1])
    }

@Composable
private fun ScoreSummary(
    games: List<GameScore>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SCORE_SPACING),
    ) {
        games.forEach { game ->
            Text(
                text = stringResource(R.string.list_score, game.myScore, game.opponentScore),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(MatchListTestTags.SCORE_ENTRY),
            )
        }
    }
}

@Composable
private fun matchRowSentence(
    state: MatchRowUiState,
    locale: Locale,
): String {
    val separator = stringResource(R.string.a11y_row_separator)
    val opponents =
        when (state.opponentNames.size) {
            0 -> null
            1 -> stringResource(R.string.a11y_against_one, state.opponentNames[0])
            else -> stringResource(R.string.a11y_against_two, state.opponentNames[0], state.opponentNames[1])
        }
    val scores =
        if (state.games.isEmpty()) {
            null
        } else {
            val spokenGames =
                state.games
                    .map { game -> stringResource(R.string.a11y_score, game.myScore, game.opponentScore) }
                    .joinToString(separator)
            stringResource(R.string.a11y_scores, spokenGames)
        }
    val photo =
        if (state.thumbnailPath == null) {
            null
        } else {
            stringResource(R.string.a11y_with_photo)
        }
    return listOfNotNull(
        resultLabel(state.result),
        formatLabel(state.format),
        formatMatchDateLong(state.date, locale),
        opponents,
        scores,
        photo,
    ).joinToString(separator)
}
