package com.maxeydev.picklelog.ui.match.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.currentLocale
import com.maxeydev.picklelog.ui.match.formatLabel
import com.maxeydev.picklelog.ui.match.formatMatchDate
import com.maxeydev.picklelog.ui.match.resultLabel
import com.maxeydev.picklelog.ui.match.toUtcEpochMillis
import com.maxeydev.picklelog.ui.match.utcEpochMillisToAppDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchEditScreen(
    state: MatchEditUiState,
    actions: MatchEditActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.edit_title_existing else R.string.edit_title_new,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onClose) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = actions.onSave,
                        enabled = state.canSave,
                        modifier = Modifier.testTag(MatchEditTestTags.SAVE),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            MatchEditForm(
                state = state,
                actions = actions,
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
            )
        }
    }
}

@Composable
private fun MatchEditForm(
    state: MatchEditUiState,
    actions: MatchEditActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        FormatSection(
            format = state.format,
            clearedOnFormatSwitch = state.clearedOnFormatSwitch,
            onFormatSelected = actions.onFormatSelected,
        )
        ResultSection(
            result = state.result,
            advisory = state.advisory,
            onResultSelected = actions.onResultSelected,
        )
        state.date?.let { date ->
            DateSection(date = date, onDateSelected = actions.onDateSelected)
        }
        TimeRangeField(
            startTime = state.startTime,
            endTime = state.endTime,
            duration = state.duration,
            endsNextDay = state.endsNextDay,
            onStartTimeChanged = actions.onStartTimeChanged,
            onEndTimeChanged = actions.onEndTimeChanged,
        )
        PlayersSection(
            format = state.format,
            slots = state.personSlots,
            onPersonNameChanged = actions.onPersonNameChanged,
        )
        ScoresSection(
            games = state.games,
            onGameAdded = actions.onGameAdded,
            onGameRemoved = actions.onGameRemoved,
            onGameScoresChanged = actions.onGameScoresChanged,
        )
        DetailsSection(
            location = state.location,
            paddle = state.paddle,
            notes = state.notes,
            onLocationChanged = actions.onLocationChanged,
            onPaddleChanged = actions.onPaddleChanged,
            onNotesChanged = actions.onNotesChanged,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon =
            if (selected) {
                { Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null) }
            } else {
                null
            },
        modifier = Modifier.testTag(testTag),
    )
}

@Composable
private fun FormatSection(
    format: MatchFormat,
    clearedOnFormatSwitch: Boolean,
    onFormatSelected: (MatchFormat) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(stringResource(R.string.label_format))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(
                label = formatLabel(MatchFormat.SINGLES),
                selected = format == MatchFormat.SINGLES,
                onClick = { onFormatSelected(MatchFormat.SINGLES) },
                testTag = MatchEditTestTags.FORMAT_SINGLES,
            )
            ChoiceChip(
                label = formatLabel(MatchFormat.DOUBLES),
                selected = format == MatchFormat.DOUBLES,
                onClick = { onFormatSelected(MatchFormat.DOUBLES) },
                testTag = MatchEditTestTags.FORMAT_DOUBLES,
            )
        }
        if (clearedOnFormatSwitch) {
            Text(
                text = stringResource(R.string.format_switch_cleared),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun ResultSection(
    result: MatchResult?,
    advisory: MatchResult?,
    onResultSelected: (MatchResult) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(stringResource(R.string.label_result))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(
                label = resultLabel(MatchResult.WIN),
                selected = result == MatchResult.WIN,
                onClick = { onResultSelected(MatchResult.WIN) },
                testTag = MatchEditTestTags.RESULT_WIN,
            )
            ChoiceChip(
                label = resultLabel(MatchResult.LOSS),
                selected = result == MatchResult.LOSS,
                onClick = { onResultSelected(MatchResult.LOSS) },
                testTag = MatchEditTestTags.RESULT_LOSS,
            )
        }
        if (advisory != null) {
            Text(
                text =
                    when (advisory) {
                        MatchResult.WIN -> stringResource(R.string.advisory_suggests_win)
                        MatchResult.LOSS -> stringResource(R.string.advisory_suggests_loss)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun DateSection(
    date: AppDate,
    onDateSelected: (AppDate) -> Unit,
) {
    var isPicking by rememberSaveable { mutableStateOf(false) }
    val shown = formatMatchDate(date, currentLocale())
    val buttonDescription = stringResource(R.string.date_button_description, shown)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(stringResource(R.string.label_date))
        OutlinedButton(
            onClick = { isPicking = true },
            modifier = Modifier.semantics { contentDescription = buttonDescription },
        ) {
            Text(shown)
        }
    }
    if (isPicking) {
        MatchDatePickerDialog(
            initial = date,
            onConfirm = { picked ->
                onDateSelected(picked)
                isPicking = false
            },
            onDismiss = { isPicking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchDatePickerDialog(
    initial: AppDate,
    onConfirm: (AppDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial.toUtcEpochMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = pickerState.selectedDateMillis
                    if (selected == null) {
                        onDismiss()
                    } else {
                        onConfirm(utcEpochMillisToAppDate(selected))
                    }
                },
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
        DatePicker(state = pickerState)
    }
}

@Composable
private fun PlayersSection(
    format: MatchFormat,
    slots: List<PersonSlotUiState>,
    onPersonNameChanged: (PersonSlot, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.label_players))
        slots.forEach { slot ->
            PersonSlotField(
                slot = slot,
                format = format,
                onNameChanged = { name -> onPersonNameChanged(slot.slot, name) },
            )
        }
    }
}

@Composable
private fun ScoresSection(
    games: List<GameScoreRowUiState>,
    onGameAdded: () -> Unit,
    onGameRemoved: (Int) -> Unit,
    onGameScoresChanged: (Int, String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.label_scores))
        games.forEachIndexed { index, row ->
            GameScoreRow(
                row = row,
                onScoresChanged = { myScore, opponentScore -> onGameScoresChanged(index, myScore, opponentScore) },
                onRemove = { onGameRemoved(index) },
            )
        }
        TextButton(onClick = onGameAdded, modifier = Modifier.testTag(MatchEditTestTags.ADD_GAME)) {
            Text(stringResource(R.string.add_game))
        }
    }
}

@Composable
private fun DetailsSection(
    location: String,
    paddle: String,
    notes: String,
    onLocationChanged: (String) -> Unit,
    onPaddleChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = location,
            onValueChange = onLocationChanged,
            label = { Text(stringResource(R.string.label_location)) },
            singleLine = true,
            keyboardOptions = sentenceKeyboard(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = paddle,
            onValueChange = onPaddleChanged,
            label = { Text(stringResource(R.string.label_paddle)) },
            singleLine = true,
            keyboardOptions = sentenceKeyboard(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChanged,
            label = { Text(stringResource(R.string.label_notes)) },
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun sentenceKeyboard(): KeyboardOptions =
    KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
