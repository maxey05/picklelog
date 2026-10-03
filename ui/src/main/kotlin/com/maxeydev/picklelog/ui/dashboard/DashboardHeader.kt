package com.maxeydev.picklelog.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.stats.WinLoss
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.resultLabel
import com.maxeydev.picklelog.ui.theme.PicklelogTheme

private val MIN_TOUCH_TARGET = 48.dp
private val DOT_SIZE = 12.dp
private val DOT_RING_WIDTH = 2.dp
private val FLAME_SIZE = 16.dp

@Composable
fun DashboardHeader(
    state: DashboardUiState,
    isCollapsed: Boolean,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading || !state.hasAnyMatches) {
        return
    }
    val overall = state.overallStats.overall
    val openLabel = stringResource(R.string.dashboard_a11y_open)
    val summary = headerSummary(state)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_TARGET)
                .testTag(DashboardTestTags.HEADER)
                .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpenStats)
                .semantics(mergeDescendants = true) { contentDescription = summary },
    ) {
        if (isCollapsed) {
            CollapsedStats(state = state, overall = overall)
        } else {
            ExpandedStats(state = state, overall = overall)
        }
    }
}

@Composable
private fun ExpandedStats(
    state: DashboardUiState,
    overall: WinLoss,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.hasDisplayName) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.testTag(DashboardTestTags.NAME),
                )
            }
            StreakPill(current = state.streak.current, isCompact = false)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            WinRateFigure(winPercent = overall.winPercent, isCompact = false)
            RecordFigure(overall = overall, isCompact = false)
            LastMatchesDots(results = state.recentResults)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollapsedStats(
    state: DashboardUiState,
    overall: WinLoss,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.hasDisplayName) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(DashboardTestTags.NAME),
                )
            }
            StreakPill(current = state.streak.current, isCompact = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WinRateFigure(winPercent = overall.winPercent, isCompact = true)
            RecordFigure(overall = overall, isCompact = true)
        }
    }
}

@Composable
private fun StreakPill(
    current: Int,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = PicklelogTheme.colors
    val text =
        when {
            current > 0 && isCompact -> stringResource(R.string.dashboard_streak_compact, current)
            current > 0 -> pluralStringResource(R.plurals.dashboard_streak_weeks, current, current)
            isCompact -> stringResource(R.string.dashboard_no_streak_compact)
            else -> stringResource(R.string.dashboard_no_streak)
        }
    Surface(
        color = colors.headerPill,
        contentColor = colors.onHeaderPill,
        shape = CircleShape,
        modifier = modifier.testTag(DashboardTestTags.STREAK),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (current > 0) {
                Icon(
                    painter = painterResource(R.drawable.ic_flame),
                    contentDescription = null,
                    tint = colors.streakFlame,
                    modifier = Modifier.size(FLAME_SIZE),
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun WinRateFigure(
    winPercent: Int?,
    isCompact: Boolean,
) {
    if (winPercent == null) {
        return
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag(DashboardTestTags.WIN_PERCENT),
    ) {
        Text(
            text = stringResource(R.string.dashboard_win_percent_short, winPercent),
            style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
            color = PicklelogTheme.colors.headerAccent,
        )
        Text(
            text = stringResource(R.string.dashboard_win_rate_label),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun RecordFigure(
    overall: WinLoss,
    isCompact: Boolean,
) {
    Text(
        text = stringResource(R.string.dashboard_record_compact, overall.wins, overall.losses),
        style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag(DashboardTestTags.RECORD),
    )
}

@Composable
private fun LastMatchesDots(results: List<MatchResult>) {
    if (results.isEmpty()) {
        return
    }
    val color = PicklelogTheme.colors.onHeaderMuted
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.dashboard_last_matches, results.size),
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
        results.forEach { result -> ResultDot(isWin = result == MatchResult.WIN, color = color) }
    }
}

@Composable
private fun ResultDot(
    isWin: Boolean,
    color: Color,
) {
    val base = Modifier.size(DOT_SIZE)
    Box(modifier = if (isWin) base.background(color, CircleShape) else base.border(DOT_RING_WIDTH, color, CircleShape))
}

@Composable
private fun headerSummary(state: DashboardUiState): String {
    val separator = stringResource(R.string.dashboard_a11y_header_separator)
    val overall = state.overallStats.overall
    val percent = overall.winPercent?.let { stringResource(R.string.dashboard_a11y_percent, it) }
    val record = stringResource(R.string.dashboard_a11y_record, overall.wins, overall.losses)
    val current = state.streak.current
    val streak =
        if (current > 0) {
            pluralStringResource(R.plurals.dashboard_a11y_streak, current, current)
        } else {
            stringResource(R.string.dashboard_a11y_no_streak)
        }
    val recent =
        if (state.recentResults.isEmpty()) {
            null
        } else {
            val spoken = state.recentResults.map { resultLabel(it) }.joinToString(separator)
            stringResource(R.string.dashboard_a11y_last_five, state.recentResults.size, spoken)
        }
    return listOfNotNull(
        state.displayName.takeIf { it.isNotBlank() },
        percent,
        record,
        streak,
        recent,
    ).joinToString(separator)
}
