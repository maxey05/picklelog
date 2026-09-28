package com.maxeydev.picklelog.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.stats.WinLoss
import com.maxeydev.picklelog.ui.R

private val LOCK_ICON_SIZE = 20.dp
private val MIN_ROW_HEIGHT = 48.dp

private val PRO_SECTIONS =
    listOf(
        R.string.stats_pro_head_to_head,
        R.string.stats_pro_partner,
        R.string.stats_pro_location,
        R.string.stats_pro_paddle,
        R.string.stats_pro_monthly,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedStatsScreen(
    state: DashboardUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(DashboardTestTags.STATS_SCREEN),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!state.isLoading) {
            Column(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FilterIndicator(filter = state.filter, opponentName = state.filteredOpponentName)
                RecordSection(state)
                StreakSection(state)
                FormatSection(state)
                if (!state.isPro) {
                    LockedProSection()
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    testTag: String,
    supporting: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_ROW_HEIGHT)
                .testTag(testTag)
                .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            supporting?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun percentText(record: WinLoss): String =
    record.winPercent?.let { stringResource(R.string.stats_percent, it) } ?: stringResource(R.string.stats_no_value)

@Composable
private fun RecordSection(state: DashboardUiState) {
    val overall = state.stats.overall
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(stringResource(R.string.stats_record))
        StatRow(
            label = stringResource(R.string.stats_matches),
            value = overall.total.toString(),
            testTag = DashboardTestTags.STATS_MATCHES,
        )
        StatRow(
            label = stringResource(R.string.stats_wins_losses),
            value = stringResource(R.string.stats_split_record, overall.wins, overall.losses),
            testTag = DashboardTestTags.STATS_RECORD,
        )
        StatRow(
            label = stringResource(R.string.stats_win_rate),
            value = percentText(overall),
            testTag = DashboardTestTags.STATS_WIN_RATE,
        )
    }
}

@Composable
private fun weeksText(weeks: Int): String =
    if (weeks > 0) {
        pluralStringResource(R.plurals.stats_weeks, weeks, weeks)
    } else {
        stringResource(R.string.stats_no_streak)
    }

@Composable
private fun StreakSection(state: DashboardUiState) {
    val streak = state.streak
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(stringResource(R.string.stats_streaks))
        if (state.isFiltered) {
            Text(
                text = stringResource(R.string.stats_streak_scope_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (streak.isCurrentTheLongest) {
            StatRow(
                label = stringResource(R.string.stats_current_streak),
                value = weeksText(streak.current),
                supporting = stringResource(R.string.stats_current_is_longest),
                testTag = DashboardTestTags.STATS_CURRENT_IS_LONGEST,
            )
        } else {
            StatRow(
                label = stringResource(R.string.stats_current_streak),
                value = weeksText(streak.current),
                testTag = DashboardTestTags.STATS_CURRENT_STREAK,
            )
            StatRow(
                label = stringResource(R.string.stats_longest_streak),
                value = weeksText(streak.longest),
                testTag = DashboardTestTags.STATS_LONGEST_STREAK,
            )
        }
    }
}

@Composable
private fun FormatSection(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(stringResource(R.string.stats_by_format))
        SplitRow(
            label = stringResource(R.string.format_singles),
            record = state.stats.singles,
            testTag = DashboardTestTags.STATS_SINGLES,
        )
        SplitRow(
            label = stringResource(R.string.format_doubles),
            record = state.stats.doubles,
            testTag = DashboardTestTags.STATS_DOUBLES,
        )
    }
}

@Composable
private fun SplitRow(
    label: String,
    record: WinLoss,
    testTag: String,
) {
    StatRow(
        label = label,
        value = percentText(record),
        supporting =
            if (record.hasMatches) {
                stringResource(R.string.stats_split_record, record.wins, record.losses)
            } else {
                stringResource(R.string.stats_split_empty)
            },
        testTag = testTag,
    )
}

@Composable
private fun LockedProSection() {
    Column(
        modifier = Modifier.testTag(DashboardTestTags.STATS_PRO_SECTION),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HorizontalDivider()
        SectionTitle(stringResource(R.string.stats_pro_title))
        PRO_SECTIONS.forEachIndexed { index, title ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MIN_ROW_HEIGHT)
                        .testTag(DashboardTestTags.proRow(index))
                        .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(LOCK_ICON_SIZE),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.stats_pro_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
