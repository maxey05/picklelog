@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.common.PillChip
import com.maxeydev.picklelog.ui.match.formatLabel
import com.maxeydev.picklelog.ui.match.toUtcEpochMillis
import com.maxeydev.picklelog.ui.match.todayInDeviceZone
import com.maxeydev.picklelog.ui.match.utcEpochMillisToAppDate
import com.maxeydev.picklelog.ui.theme.PicklelogTheme
import kotlin.uuid.ExperimentalUuidApi

private val MIN_TOUCH_TARGET = 48.dp
private val FIELD_MIN_HEIGHT = 56.dp
private val ICON_SIZE = 20.dp

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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.testTag(MatchListTestTags.FILTER_SHEET),
    ) {
        SheetHeader(canReset = filter.isActive, onReset = onAllFiltersCleared)
        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SegmentedChoice(
                label = stringResource(R.string.label_format),
                options = listOf(null, MatchFormat.SINGLES, MatchFormat.DOUBLES),
                selected = filter.format,
                optionLabel = { format -> format?.let { formatLabel(it) } ?: stringResource(R.string.filter_all) },
                optionTag = { format -> MatchListTestTags.formatOption(format) },
                onSelected = { format -> onFilterChanged(filter.copy(format = format)) },
            )
            SegmentedChoice(
                label = stringResource(R.string.label_result),
                options = listOf(null, MatchResult.WIN, MatchResult.LOSS),
                selected = filter.result,
                optionLabel = { result -> resultFilterLabel(result) },
                optionTag = { result -> MatchListTestTags.resultOption(result) },
                onSelected = { result -> onFilterChanged(filter.copy(result = result)) },
            )
            DateSection(
                filter = filter,
                onRangeChosen = { from, to -> onFilterChanged(filter.copy(fromDate = from, toDate = to)) },
                onRangeCleared = { onFilterChanged(filter.copy(fromDate = null, toDate = null)) },
            )
            DropdownField(
                label = stringResource(R.string.label_opponents),
                shown =
                    filter.opponentId?.let { id -> opponentChoices.firstOrNull { it.id == id }?.name }
                        ?: stringResource(R.string.filter_any_opponent),
                anyLabel = stringResource(R.string.filter_any_opponent),
                emptyLabel = stringResource(R.string.filter_no_opponents),
                options = opponentChoices.map { it.name },
                fieldTag = MatchListTestTags.OPPONENT_PICKER,
                optionTag = { index -> MatchListTestTags.opponentOption(opponentChoices[index].id.toString()) },
                onAnyChosen = { onFilterChanged(filter.copy(opponentId = null)) },
                onOptionChosen = { index -> onFilterChanged(filter.copy(opponentId = opponentChoices[index].id)) },
            )
            DropdownField(
                label = stringResource(R.string.label_location),
                shown = filter.location ?: stringResource(R.string.filter_any_location),
                anyLabel = stringResource(R.string.filter_any_location),
                emptyLabel = stringResource(R.string.filter_no_locations),
                options = locationChoices,
                fieldTag = MatchListTestTags.LOCATION_PICKER,
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
        }
        Button(
            onClick = onDismiss,
            shape = MaterialTheme.shapes.large,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .heightIn(min = FIELD_MIN_HEIGHT)
                    .testTag(MatchListTestTags.SHEET_DONE),
        ) {
            Text(text = stringResource(R.string.filter_done), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SheetHeader(
    canReset: Boolean,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.filter_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        TextButton(
            onClick = onReset,
            enabled = canReset,
            modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET).testTag(MatchListTestTags.SHEET_CLEAR_ALL),
        ) {
            Text(stringResource(R.string.filter_reset))
        }
    }
}

@Composable
private fun resultFilterLabel(result: MatchResult?): String =
    when (result) {
        MatchResult.WIN -> stringResource(R.string.filter_wins)
        MatchResult.LOSS -> stringResource(R.string.filter_losses)
        null -> stringResource(R.string.filter_all)
    }

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun <T> SegmentedChoice(
    label: String,
    options: List<T?>,
    selected: T?,
    optionLabel: @Composable (T?) -> String,
    optionTag: (T?) -> String,
    onSelected: (T?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = PicklelogTheme.colors.cardBorder
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, borderColor, MaterialTheme.shapes.medium)
                    .selectableGroup(),
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selected
                if (index > 0) {
                    VerticalDivider(color = borderColor)
                }
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isSelected) colors.primaryContainer else colors.surfaceContainerLowest)
                            .heightIn(min = MIN_TOUCH_TARGET)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelected(option) },
                            )
                            .testTag(optionTag(option)),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(ICON_SIZE),
                            )
                        }
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSection(
    filter: FilterState,
    onRangeChosen: (AppDate, AppDate) -> Unit,
    onRangeCleared: () -> Unit,
) {
    var isPicking by rememberSaveable { mutableStateOf(false) }
    val today = remember { todayInDeviceZone() }
    val activePreset = filter.matchingDatePreset(today)
    val hasCustomRange = (filter.fromDate != null || filter.toDate != null) && activePreset == null
    val label = stringResource(R.string.label_date)
    val shown = dateRangeLabel(filter) ?: stringResource(R.string.filter_any_date)
    val customDescription = stringResource(R.string.filter_picker_description, label, shown)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            DatePreset.entries.forEach { preset ->
                val isSelected = preset == activePreset
                PillChip(
                    label = datePresetLabel(preset),
                    isSelected = isSelected,
                    showsCheck = isSelected,
                    onClick = {
                        if (isSelected) {
                            onRangeCleared()
                        } else {
                            val (from, to) = preset.rangeEndingOn(today)
                            onRangeChosen(from, to)
                        }
                    },
                    modifier = Modifier.testTag(MatchListTestTags.datePreset(preset)),
                )
            }
            PillChip(
                label = if (hasCustomRange) shown else stringResource(R.string.filter_date_custom),
                isSelected = hasCustomRange,
                showsCheck = hasCustomRange,
                onClick = { isPicking = true },
                modifier =
                    Modifier
                        .testTag(MatchListTestTags.DATE_PICKER)
                        .semantics { contentDescription = customDescription },
            )
            if (hasCustomRange) {
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
private fun DropdownField(
    label: String,
    shown: String,
    anyLabel: String,
    emptyLabel: String,
    options: List<String>,
    fieldTag: String,
    optionTag: (Int) -> String,
    onAnyChosen: () -> Unit,
    onOptionChosen: (Int) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val hasOptions = options.isNotEmpty()
    val fieldText = if (hasOptions) shown else emptyLabel
    val fieldDescription = stringResource(R.string.filter_picker_description, label, fieldText)
    val borderColor = if (isExpanded) colors.primary else PicklelogTheme.colors.cardBorder
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label)
        Box {
            Surface(
                onClick = { isExpanded = true },
                enabled = hasOptions,
                shape = MaterialTheme.shapes.medium,
                color = colors.surfaceContainerLowest,
                border = BorderStroke(if (isExpanded) 2.dp else 1.dp, borderColor),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = FIELD_MIN_HEIGHT)
                        .testTag(fieldTag)
                        .semantics { contentDescription = fieldDescription },
            ) {
                Row(
                    modifier = Modifier.heightIn(min = FIELD_MIN_HEIGHT).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = fieldText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (hasOptions) colors.onSurface else colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        painter =
                            painterResource(
                                if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down,
                            ),
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
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
