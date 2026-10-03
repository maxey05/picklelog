@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.stats.LabelRecord
import com.maxeydev.picklelog.domain.stats.MonthRecord
import com.maxeydev.picklelog.domain.stats.PersonRecord
import com.maxeydev.picklelog.domain.stats.WinLoss
import com.maxeydev.picklelog.domain.stats.percentIfEnoughMatches
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.currentLocale
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.uuid.ExperimentalUuidApi

private const val COLLAPSED_ROWS = 5
private const val COLLAPSED_MONTHS = 12
private const val MONTH_PATTERN = "LLLL yyyy"

private data class BreakdownRow(
    val label: String,
    val record: WinLoss,
)

@Composable
fun AdvancedStatsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    val advanced = state.advanced
    val unknownPerson = stringResource(R.string.stats_advanced_unknown_person)
    Column(
        modifier = modifier.testTag(DashboardTestTags.STATS_ADVANCED_SECTION),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HorizontalDivider()
        SectionTitle(stringResource(R.string.stats_advanced_title))
        if (advanced.isEmpty) {
            Text(
                text = stringResource(R.string.stats_advanced_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(DashboardTestTags.STATS_ADVANCED_EMPTY),
            )
        } else {
            BreakdownGroup(
                title = stringResource(R.string.stats_advanced_head_to_head),
                group = DashboardTestTags.GROUP_HEAD_TO_HEAD,
                emptyText = stringResource(R.string.stats_advanced_no_opponents),
                rows = advanced.headToHead.map { it.toRow(state, unknownPerson) },
            )
            BreakdownGroup(
                title = stringResource(R.string.stats_advanced_partner),
                group = DashboardTestTags.GROUP_PARTNER,
                emptyText = stringResource(R.string.stats_advanced_no_partners),
                rows = advanced.withPartner.map { it.toRow(state, unknownPerson) },
            )
            BreakdownGroup(
                title = stringResource(R.string.stats_advanced_location),
                group = DashboardTestTags.GROUP_LOCATION,
                emptyText = stringResource(R.string.stats_advanced_no_locations),
                rows = advanced.byLocation.map { it.toRow() },
            )
            BreakdownGroup(
                title = stringResource(R.string.stats_advanced_paddle),
                group = DashboardTestTags.GROUP_PADDLE,
                emptyText = stringResource(R.string.stats_advanced_no_paddles),
                rows = advanced.byPaddle.map { it.toRow() },
            )
            MonthlyGroup(advanced.monthly)
        }
    }
}

private fun PersonRecord.toRow(
    state: DashboardUiState,
    unknownPerson: String,
): BreakdownRow = BreakdownRow(label = state.nameOf(personId) ?: unknownPerson, record = record)

private fun LabelRecord.toRow(): BreakdownRow = BreakdownRow(label = label, record = record)

@Composable
private fun BreakdownGroup(
    title: String,
    group: String,
    emptyText: String,
    rows: List<BreakdownRow>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(title)
        if (rows.isEmpty()) {
            EmptyNote(text = emptyText, testTag = DashboardTestTags.advancedEmpty(group))
        } else {
            ExpandableRows(group = group, total = rows.size, collapsedCount = COLLAPSED_ROWS) { visible ->
                rows.take(visible).forEachIndexed { index, row ->
                    RecordRow(row = row, testTag = DashboardTestTags.advancedRow(group, index))
                }
            }
        }
    }
}

@Composable
private fun MonthlyGroup(months: List<MonthRecord>) {
    val group = DashboardTestTags.GROUP_MONTH
    val locale = currentLocale()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(stringResource(R.string.stats_advanced_monthly))
        ExpandableRows(group = group, total = months.size, collapsedCount = COLLAPSED_MONTHS) { visible ->
            months.take(visible).forEachIndexed { index, month ->
                val label = monthLabel(month, locale)
                val record = month.record
                val tag = DashboardTestTags.advancedRow(group, index)
                if (record == null) {
                    StatRow(
                        label = label,
                        value = stringResource(R.string.stats_no_value),
                        supporting = stringResource(R.string.stats_advanced_month_gap),
                        testTag = tag,
                    )
                } else {
                    StatRow(
                        label = label,
                        value = valueOf(record),
                        supporting = pluralStringResource(R.plurals.dashboard_match_count, record.total, record.total),
                        testTag = tag,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableRows(
    group: String,
    total: Int,
    collapsedCount: Int,
    content: @Composable (visible: Int) -> Unit,
) {
    var expanded by rememberSaveable(group) { mutableStateOf(false) }
    content(if (expanded) total else collapsedCount)
    if (total > collapsedCount) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.testTag(DashboardTestTags.advancedToggle(group)),
        ) {
            Text(
                if (expanded) {
                    stringResource(R.string.stats_advanced_show_less)
                } else {
                    stringResource(R.string.stats_advanced_show_all, total)
                },
            )
        }
    }
}

@Composable
private fun RecordRow(
    row: BreakdownRow,
    testTag: String,
) {
    StatRow(
        label = row.label,
        value = valueOf(row.record),
        supporting =
            if (row.record.percentIfEnoughMatches() == null) {
                stringResource(R.string.stats_advanced_small_sample)
            } else {
                stringResource(R.string.stats_split_record, row.record.wins, row.record.losses)
            },
        testTag = testTag,
    )
}

@Composable
private fun valueOf(record: WinLoss): String =
    record.percentIfEnoughMatches()?.let { stringResource(R.string.stats_percent, it) }
        ?: stringResource(R.string.stats_split_record, record.wins, record.losses)

@Composable
private fun EmptyNote(
    text: String,
    testTag: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MIN_ROW_HEIGHT)
                .padding(vertical = 12.dp)
                .testTag(testTag),
    )
}

internal fun monthLabel(
    month: MonthRecord,
    locale: Locale,
): String = YearMonth.of(month.year, month.month).format(DateTimeFormatter.ofPattern(MONTH_PATTERN, locale))
