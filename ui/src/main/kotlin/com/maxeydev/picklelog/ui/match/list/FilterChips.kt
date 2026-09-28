package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.match.FilterKind
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.currentLocale
import com.maxeydev.picklelog.ui.match.formatLabel
import com.maxeydev.picklelog.ui.match.formatMatchDate

private val CHIP_ICON_SIZE = 18.dp
private val MIN_TOUCH_TARGET = 48.dp

@Composable
fun FilterChips(
    filter: FilterState,
    opponentName: String?,
    onFilterCleared: (FilterKind) -> Unit,
    onAllFiltersCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).testTag(MatchListTestTags.FILTER_CHIPS),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filter.activeKinds.forEach { kind ->
            val label = filterChipLabel(kind, filter, opponentName)
            val removeDescription = stringResource(R.string.filter_remove, label)
            InputChip(
                selected = true,
                onClick = { onFilterCleared(kind) },
                label = { Text(label) },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        modifier = Modifier.size(CHIP_ICON_SIZE),
                    )
                },
                modifier =
                    Modifier
                        .testTag(MatchListTestTags.filterChip(kind))
                        .semantics { contentDescription = removeDescription },
            )
        }
        TextButton(
            onClick = onAllFiltersCleared,
            modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET).testTag(MatchListTestTags.CLEAR_ALL_FILTERS),
        ) {
            Text(stringResource(R.string.filter_clear_all))
        }
    }
}

@Composable
fun filterChipLabel(
    kind: FilterKind,
    filter: FilterState,
    opponentName: String?,
): String =
    when (kind) {
        FilterKind.FORMAT -> filter.format?.let { formatLabel(it) }.orEmpty()
        FilterKind.RESULT ->
            when (filter.result) {
                MatchResult.WIN -> stringResource(R.string.filter_wins)
                MatchResult.LOSS -> stringResource(R.string.filter_losses)
                null -> ""
            }
        FilterKind.DATE_RANGE -> dateRangeLabel(filter).orEmpty()
        FilterKind.OPPONENT ->
            if (opponentName == null) {
                stringResource(R.string.filter_unknown_opponent)
            } else {
                stringResource(R.string.filter_opponent_chip, opponentName)
            }
        FilterKind.LOCATION -> filter.location?.let { stringResource(R.string.filter_location_chip, it) }.orEmpty()
    }

@Composable
fun dateRangeLabel(filter: FilterState): String? {
    val locale = currentLocale()
    val from = filter.fromDate?.let { formatMatchDate(it, locale) }
    val to = filter.toDate?.let { formatMatchDate(it, locale) }
    return when {
        from != null && to != null && from == to -> from
        from != null && to != null -> stringResource(R.string.filter_date_range, from, to)
        from != null -> stringResource(R.string.filter_date_from, from)
        to != null -> stringResource(R.string.filter_date_until, to)
        else -> null
    }
}
