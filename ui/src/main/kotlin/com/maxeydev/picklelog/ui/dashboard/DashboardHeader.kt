package com.maxeydev.picklelog.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R

private val MIN_TOUCH_TARGET = 48.dp

@Composable
fun DashboardHeader(
    state: DashboardUiState,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        return
    }
    val openLabel = stringResource(R.string.dashboard_a11y_open)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_TARGET)
                .testTag(DashboardTestTags.HEADER)
                .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpenStats)
                .semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (state.hasDisplayName) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag(DashboardTestTags.NAME),
                )
            }
            when {
                !state.hasAnyMatches ->
                    Text(
                        text = stringResource(R.string.dashboard_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(DashboardTestTags.EMPTY),
                    )
                state.hasNoFilteredMatches ->
                    Text(
                        text = stringResource(R.string.dashboard_filtered_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(DashboardTestTags.FILTERED_EMPTY),
                    )
                else -> RecordLine(state)
            }
            if (state.hasAnyMatches) {
                StreakLine(state)
            }
            FilterIndicator(filter = state.filter, opponentName = state.filteredOpponentName)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordLine(state: DashboardUiState) {
    val overall = state.stats.overall
    val recordDescription = stringResource(R.string.dashboard_a11y_record, overall.wins, overall.losses)
    val percent = overall.winPercent
    val percentText =
        if (percent == null) {
            stringResource(R.string.dashboard_win_percent_none)
        } else {
            stringResource(R.string.dashboard_win_percent, percent)
        }
    val percentDescription =
        if (percent == null) {
            stringResource(R.string.dashboard_a11y_no_percent)
        } else {
            stringResource(R.string.dashboard_a11y_percent, percent)
        }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = pluralStringResource(R.plurals.dashboard_match_count, overall.total, overall.total),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(DashboardTestTags.MATCH_COUNT),
        )
        Text(
            text = stringResource(R.string.dashboard_record, overall.wins, overall.losses),
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier
                    .testTag(DashboardTestTags.RECORD)
                    .semantics { contentDescription = recordDescription },
        )
        Text(
            text = percentText,
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier
                    .testTag(DashboardTestTags.WIN_PERCENT)
                    .semantics { contentDescription = percentDescription },
        )
    }
}

@Composable
private fun StreakLine(state: DashboardUiState) {
    val current = state.streak.current
    val streakText =
        if (current > 0) {
            pluralStringResource(R.plurals.dashboard_streak_weeks, current, current)
        } else {
            stringResource(R.string.dashboard_no_streak)
        }
    val description =
        if (current > 0) {
            pluralStringResource(R.plurals.dashboard_a11y_streak, current, current)
        } else {
            stringResource(R.string.dashboard_a11y_no_streak)
        }
    Text(
        text = stringResource(R.string.dashboard_streak_scope, streakText),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(DashboardTestTags.STREAK)
                .semantics { contentDescription = description },
    )
}
