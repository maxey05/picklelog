@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.formatLabel
import com.maxeydev.picklelog.ui.match.toUtcEpochMillis
import com.maxeydev.picklelog.ui.match.utcEpochMillisToAppDate
import kotlin.uuid.ExperimentalUuidApi

private val MIN_TOUCH_TARGET = 48.dp

@Composable
fun FilterSheet(
    filter: FilterState,
    opponentChoices: List<OpponentChoice>,
    locationChoices: List<String>,
    onFilterChanged: (FilterState) -> Unit,
    onAllFiltersCleared: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag(MatchListTestTags.FILTER_SHEET),
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.filter_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            ChoiceSection(
                label = stringResource(R.string.label_format),
                options = listOf(null, MatchFormat.SINGLES, MatchFormat.DOUBLES),
                selected = filter.format,
                optionLabel = { format -> format?.let { formatLabel(it) } ?: stringResource(R.string.filter_any) },
                optionTag = { format -> MatchListTestTags.formatOption(format) },
                onSelected = { format -> onFilterChanged(filter.copy(format = format)) },
            )
            ChoiceSection(
                label = stringResource(R.string.label_result),
                options = listOf(null, MatchResult.WIN, MatchResult.LOSS),
                selected = filter.result,
                optionLabel = { result -> resultFilterLabel(result) },
                optionTag = { result -> MatchListTestTags.resultOption(result) },
                onSelected = { result -> onFilterChanged(filter.copy(result = result)) },
            )
            DateRangeSection(
                filter = filter,
                onRangeChosen = { from, to -> onFilterChanged(filter.copy(fromDate = from, toDate = to)) },
                onRangeCleared = { onFilterChanged(filter.copy(fromDate = null, toDate = null)) },
            )
            PickerSection(
                label = stringResource(R.string.label_opponents),
                shown =
                    filter.opponentId?.let { id -> opponentChoices.firstOrNull { it.id == id }?.name }
                        ?: stringResource(R.string.filter_any_opponent),
                anyLabel = stringResource(R.string.filter_any_opponent),
                emptyLabel = stringResource(R.string.filter_no_opponents),
                options = opponentChoices.map { it.name },
                buttonTag = MatchListTestTags.OPPONENT_PICKER,
                optionTag = { index -> MatchListTestTags.opponentOption(opponentChoices[index].id.toString()) },
                onAnyChosen = { onFilterChanged(filter.copy(opponentId = null)) },
                onOptionChosen = { index -> onFilterChanged(filter.copy(opponentId = opponentChoices[index].id)) },
            )
            PickerSection(
                label = stringResource(R.string.label_location),
                shown = filter.location ?: stringResource(R.string.filter_any_location),
                anyLabel = stringResource(R.string.filter_any_location),
                emptyLabel = stringResource(R.string.filter_no_locations),
                options = locationChoices,
                buttonTag = MatchListTestTags.LOCATION_PICKER,
                optionTag = { index -> MatchListTestTags.locationOption(index) },
                onAnyChosen = { onFilterChanged(filter.copy(location = null)) },
                onOptionChosen = { index -> onFilterChanged(filter.copy(location = locationChoices[index])) },
            )
            Text(
                text = stringResource(R.string.filter_reset_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(MatchListTestTags.FILTER_RESET_NOTICE),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onAllFiltersCleared,
                    enabled = filter.isActive,
                    modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET).testTag(MatchListTestTags.SHEET_CLEAR_ALL),
                ) {
                    Text(stringResource(R.string.filter_clear_all))
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET).testTag(MatchListTestTags.SHEET_DONE),
                ) {
                    Text(stringResource(R.string.filter_done))
                }
            }
        }
    }
}

@Composable
private fun resultFilterLabel(result: MatchResult?): String =
    when (result) {
        MatchResult.WIN -> stringResource(R.string.filter_wins)
        MatchResult.LOSS -> stringResource(R.string.filter_losses)
        null -> stringResource(R.string.filter_any)
    }

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun <T> ChoiceSection(
    label: String,
    options: List<T?>,
    selected: T?,
    optionLabel: @Composable (T?) -> String,
    optionTag: (T?) -> String,
    onSelected: (T?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(option) },
                    label = { Text(optionLabel(option)) },
                    leadingIcon =
                        if (isSelected) {
                            { Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null) }
                        } else {
                            null
                        },
                    modifier = Modifier.testTag(optionTag(option)),
                )
            }
        }
    }
}

@Composable
private fun DateRangeSection(
    filter: FilterState,
    onRangeChosen: (AppDate, AppDate) -> Unit,
    onRangeCleared: () -> Unit,
) {
    var isPicking by rememberSaveable { mutableStateOf(false) }
    val label = stringResource(R.string.label_date)
    val shown = dateRangeLabel(filter) ?: stringResource(R.string.filter_any_date)
    val buttonDescription = stringResource(R.string.filter_picker_description, label, shown)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { isPicking = true },
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .heightIn(min = MIN_TOUCH_TARGET)
                        .testTag(MatchListTestTags.DATE_PICKER)
                        .semantics { contentDescription = buttonDescription },
            ) {
                Text(shown)
            }
            if (filter.fromDate != null || filter.toDate != null) {
                IconButton(onClick = onRangeCleared, modifier = Modifier.testTag(MatchListTestTags.DATE_CLEAR)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.filter_clear_date),
                    )
                }
            }
        }
    }
    if (isPicking) {
        DateRangePickerDialog(
            initialFrom = filter.fromDate,
            initialTo = filter.toDate,
            onConfirm = { from, to ->
                isPicking = false
                onRangeChosen(from, to)
            },
            onDismiss = { isPicking = false },
        )
    }
}

@Composable
private fun DateRangePickerDialog(
    initialFrom: AppDate?,
    initialTo: AppDate?,
    onConfirm: (AppDate, AppDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialFrom?.toUtcEpochMillis(),
            initialSelectedEndDateMillis = initialTo?.toUtcEpochMillis(),
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = pickerState.selectedStartDateMillis
                    if (start == null) {
                        onDismiss()
                    } else {
                        val from = utcEpochMillisToAppDate(start)
                        val to = pickerState.selectedEndDateMillis?.let(::utcEpochMillisToAppDate) ?: from
                        onConfirm(from, to)
                    }
                },
                modifier = Modifier.testTag(MatchListTestTags.DATE_CONFIRM),
            ) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        DateRangePicker(
            state = pickerState,
            title = {
                Text(
                    text = stringResource(R.string.filter_choose_dates),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PickerSection(
    label: String,
    shown: String,
    anyLabel: String,
    emptyLabel: String,
    options: List<String>,
    buttonTag: String,
    optionTag: (Int) -> String,
    onAnyChosen: () -> Unit,
    onOptionChosen: (Int) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val hasOptions = options.isNotEmpty()
    val buttonText = if (hasOptions) shown else emptyLabel
    val buttonDescription = stringResource(R.string.filter_picker_description, label, buttonText)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(label)
        Box {
            OutlinedButton(
                onClick = { isExpanded = true },
                enabled = hasOptions,
                modifier =
                    Modifier
                        .heightIn(min = MIN_TOUCH_TARGET)
                        .testTag(buttonTag)
                        .semantics { contentDescription = buttonDescription },
            ) {
                Text(buttonText)
            }
            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(anyLabel) },
                    onClick = {
                        isExpanded = false
                        onAnyChosen()
                    },
                )
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            isExpanded = false
                            onOptionChosen(index)
                        },
                        modifier = Modifier.testTag(optionTag(index)),
                    )
                }
            }
        }
    }
}
