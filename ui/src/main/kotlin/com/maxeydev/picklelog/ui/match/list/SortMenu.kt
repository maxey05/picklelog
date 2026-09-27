package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.match.MatchSort
import com.maxeydev.picklelog.ui.R

private val MIN_TOUCH_TARGET = 48.dp

@Composable
fun SortMenu(
    activeSort: MatchSort,
    onSortSelected: (MatchSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(
            onClick = { isExpanded = true },
            modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET).testTag(MatchListTestTags.SORT_BUTTON),
        ) {
            Icon(painter = painterResource(R.drawable.ic_sort), contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.sort_button, sortLabel(activeSort)))
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            MatchSort.entries.forEach { sort ->
                val isActive = sort == activeSort
                DropdownMenuItem(
                    text = { Text(text = sortLabel(sort)) },
                    onClick = {
                        isExpanded = false
                        onSortSelected(sort)
                    },
                    trailingIcon =
                        if (isActive) {
                            { Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null) }
                        } else {
                            null
                        },
                    modifier =
                        Modifier
                            .testTag(MatchListTestTags.sortOption(sort))
                            .semantics { selected = isActive },
                )
            }
        }
    }
}

@Composable
fun sortLabel(sort: MatchSort): String =
    when (sort) {
        MatchSort.DATE_NEWEST -> stringResource(R.string.sort_date_newest)
        MatchSort.DATE_OLDEST -> stringResource(R.string.sort_date_oldest)
        MatchSort.RESULT_WINS_FIRST -> stringResource(R.string.sort_result_wins_first)
        MatchSort.RESULT_LOSSES_FIRST -> stringResource(R.string.sort_result_losses_first)
        MatchSort.OPPONENT_A_TO_Z -> stringResource(R.string.sort_opponent_a_to_z)
    }
