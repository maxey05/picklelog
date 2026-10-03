package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.match.MatchSort
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.theme.PicklelogTheme

private val MIN_TOUCH_TARGET = 48.dp
private val ICON_SIZE = 18.dp

@Composable
fun SortMenu(
    activeSort: MatchSort,
    onSortSelected: (MatchSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val activeLabel = sortLabel(activeSort)
    val description = stringResource(R.string.sort_button, activeLabel)
    val borderColor = if (isExpanded) colors.primary else PicklelogTheme.colors.cardBorder
    val borderWidth = if (isExpanded) 2.dp else 1.dp
    Box(modifier = modifier) {
        Surface(
            onClick = { isExpanded = true },
            shape = MaterialTheme.shapes.medium,
            color = colors.surfaceContainerLowest,
            contentColor = colors.onSurface,
            border = BorderStroke(borderWidth, borderColor),
            modifier =
                Modifier
                    .heightIn(min = MIN_TOUCH_TARGET)
                    .testTag(MatchListTestTags.SORT_BUTTON)
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    },
        ) {
            Row(
                modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sort),
                    contentDescription = null,
                    modifier = Modifier.size(ICON_SIZE),
                )
                Text(text = activeLabel, style = MaterialTheme.typography.labelLarge)
                Icon(
                    painter =
                        painterResource(
                            if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down,
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(ICON_SIZE),
                )
            }
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = colors.surfaceContainerLowest,
        ) {
            visibleSorts(activeSort).forEach { sort ->
                SortMenuItem(
                    sort = sort,
                    isActive = sort == activeSort,
                    onClick = {
                        isExpanded = false
                        onSortSelected(sort)
                    },
                )
            }
        }
    }
}

@Composable
private fun SortMenuItem(
    sort: MatchSort,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val rowColor =
        if (isActive) PicklelogTheme.colors.winRow else MaterialTheme.colorScheme.surfaceContainerLowest
    DropdownMenuItem(
        text = {
            Text(
                text = sortLabel(sort),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        },
        onClick = onClick,
        trailingIcon =
            if (isActive) {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
        modifier =
            Modifier
                .background(rowColor)
                .testTag(MatchListTestTags.sortOption(sort))
                .semantics { selected = isActive },
    )
}

private fun visibleSorts(activeSort: MatchSort): List<MatchSort> =
    MatchSort.entries.filter { sort -> sort != MatchSort.DURATION_SHORTEST || sort == activeSort }

@Composable
fun sortLabel(sort: MatchSort): String =
    when (sort) {
        MatchSort.DATE_NEWEST -> stringResource(R.string.sort_date_newest)
        MatchSort.DATE_OLDEST -> stringResource(R.string.sort_date_oldest)
        MatchSort.RESULT_WINS_FIRST -> stringResource(R.string.sort_result_wins_first)
        MatchSort.RESULT_LOSSES_FIRST -> stringResource(R.string.sort_result_losses_first)
        MatchSort.OPPONENT_A_TO_Z -> stringResource(R.string.sort_opponent_a_to_z)
        MatchSort.LOCATION_A_TO_Z -> stringResource(R.string.sort_location_a_to_z)
        MatchSort.DURATION_SHORTEST -> stringResource(R.string.sort_duration_shortest)
        MatchSort.DURATION_LONGEST -> stringResource(R.string.sort_duration_longest)
    }
