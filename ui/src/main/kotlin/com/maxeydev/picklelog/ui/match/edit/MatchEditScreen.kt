package com.maxeydev.picklelog.ui.match.edit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.common.AutocompleteField
import com.maxeydev.picklelog.ui.common.PillChip
import com.maxeydev.picklelog.ui.match.formatLabel
import com.maxeydev.picklelog.ui.match.resultLabel
import com.maxeydev.picklelog.ui.paywall.UpgradePrompt
import com.maxeydev.picklelog.ui.paywall.UpgradeReason
import com.maxeydev.picklelog.ui.theme.PicklelogTheme

private val CARD_SHAPE = RoundedCornerShape(16.dp)
private val ICON_SIZE = 22.dp
private val FIELD_ICON_TOP_PADDING = 17.dp
private val MIN_TOUCH_TARGET = 48.dp
private val ADD_ICON_SIZE = 16.dp

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
                    Button(
                        onClick = actions.onSave,
                        enabled = state.canSave,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier.padding(end = 12.dp).testTag(MatchEditTestTags.SAVE),
                    ) {
                        Text(text = stringResource(R.string.action_save), style = MaterialTheme.typography.labelLarge)
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
        state.upgradePrompt?.let { reason ->
            UpgradePrompt(
                reason = reason,
                onDismiss = actions.onUpgradePromptDismissed,
                onSeePro = actions.onSeePro.takeIf { reason != UpgradeReason.PRO_PHOTO_LIMIT },
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
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SetupCard(state = state, actions = actions)
        PlayersCard(state = state, actions = actions)
        ScoresCard(
            games = state.games,
            onGameAdded = actions.onGameAdded,
            onGameRemoved = actions.onGameRemoved,
            onGameScoresChanged = actions.onGameScoresChanged,
        )
        FormCard {
            IconRow(
                icon = R.drawable.ic_photo_camera,
                groupLabel = stringResource(R.string.label_photos),
            ) {
                PhotoPickerSection(
                    photos = state.photos,
                    hasPhotoError = state.hasPhotoError,
                    actions = actions.photoActions,
                )
            }
        }
        LocationCard(state = state, actions = actions)
        NotesCard(notes = state.notes, onNotesChanged = actions.onNotesChanged)
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = CARD_SHAPE,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, PicklelogTheme.colors.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun IconRow(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    groupLabel: String? = null,
    iconAlignment: Alignment.Vertical = Alignment.CenterVertically,
    iconTopPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val groupModifier =
        if (groupLabel != null) {
            Modifier.semantics { contentDescription = groupLabel }
        } else {
            Modifier
        }
    Row(
        modifier = modifier.fillMaxWidth().then(groupModifier),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = iconAlignment,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = iconTopPadding).size(ICON_SIZE),
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun SetupCard(
    state: MatchEditUiState,
    actions: MatchEditActions,
) {
    FormCard {
        IconRow(icon = R.drawable.ic_players, groupLabel = stringResource(R.string.label_format)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SegmentedToggle(
                    options = listOf(MatchFormat.SINGLES, MatchFormat.DOUBLES),
                    selected = state.format,
                    optionLabel = { format -> formatLabel(format) },
                    optionTag = { format ->
                        if (format == MatchFormat.SINGLES) {
                            MatchEditTestTags.FORMAT_SINGLES
                        } else {
                            MatchEditTestTags.FORMAT_DOUBLES
                        }
                    },
                    onSelected = actions.onFormatSelected,
                )
                if (state.clearedOnFormatSwitch) {
                    Text(
                        text = stringResource(R.string.format_switch_cleared),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
        IconRow(icon = R.drawable.ic_trophy, groupLabel = stringResource(R.string.label_result)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SegmentedToggle(
                    options = listOf(MatchResult.WIN, MatchResult.LOSS),
                    selected = state.result,
                    optionLabel = { result -> resultLabel(result) },
                    optionTag = { result ->
                        if (result == MatchResult.WIN) MatchEditTestTags.RESULT_WIN else MatchEditTestTags.RESULT_LOSS
                    },
                    onSelected = actions.onResultSelected,
                )
                state.advisory?.let { advisory ->
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
        state.date?.let { date ->
            IconRow(icon = R.drawable.ic_calendar, groupLabel = stringResource(R.string.label_date_time)) {
                DateTimeField(
                    date = date,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    onDateSelected = actions.onDateSelected,
                    onStartTimeChanged = actions.onStartTimeChanged,
                    onEndTimeChanged = actions.onEndTimeChanged,
                )
            }
        }
    }
}

@Composable
private fun PlayersCard(
    state: MatchEditUiState,
    actions: MatchEditActions,
) {
    FormCard {
        IconRow(
            icon = R.drawable.ic_person,
            groupLabel = stringResource(R.string.label_players),
            iconAlignment = Alignment.Top,
            iconTopPadding = FIELD_ICON_TOP_PADDING,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.personSlots.forEach { slot ->
                    val target = SuggestionTarget.forSlot(slot.slot)
                    PersonSlotField(
                        slot = slot,
                        format = state.format,
                        suggestions = state.suggestionsFor(target),
                        onNameChanged = { name -> actions.onPersonNameChanged(slot.slot, name) },
                        onSuggestionSelected = { suggestion -> actions.onSuggestionSelected(target, suggestion) },
                        onFocusChanged = { isFocused -> actions.onSuggestionFocusChanged(target, isFocused) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoresCard(
    games: List<GameScoreRowUiState>,
    onGameAdded: () -> Unit,
    onGameRemoved: (Int) -> Unit,
    onGameScoresChanged: (Int, String, String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    FormCard {
        IconRow(
            icon = R.drawable.ic_scoreboard,
            groupLabel = stringResource(R.string.label_scores),
            iconAlignment = Alignment.Top,
            iconTopPadding = FIELD_ICON_TOP_PADDING,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                games.forEachIndexed { index, row ->
                    GameScoreRow(
                        row = row,
                        onScoresChanged = { myScore, opponentScore ->
                            onGameScoresChanged(index, myScore, opponentScore)
                        },
                        onRemove = { onGameRemoved(index) },
                    )
                }
                FilledTonalButton(
                    onClick = onGameAdded,
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                        ),
                    modifier = Modifier.fillMaxWidth().testTag(MatchEditTestTags.ADD_GAME),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(ADD_ICON_SIZE),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.add_game), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun LocationCard(
    state: MatchEditUiState,
    actions: MatchEditActions,
) {
    FormCard {
        IconRow(
            icon = R.drawable.ic_location,
            iconAlignment = Alignment.Top,
            iconTopPadding = FIELD_ICON_TOP_PADDING,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AutocompleteField(
                    value = state.location,
                    onValueChange = actions.onLocationChanged,
                    suggestions = state.suggestionsFor(SuggestionTarget.LOCATION),
                    onSuggestionSelected = { actions.onSuggestionSelected(SuggestionTarget.LOCATION, it) },
                    onFocusChanged = { actions.onSuggestionFocusChanged(SuggestionTarget.LOCATION, it) },
                    label = stringResource(R.string.label_location),
                    placeholder = stringResource(R.string.location_hint),
                    fieldTestTag = MatchEditTestTags.LOCATION,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                )
                RecentLocations(
                    recent = state.recentLocations,
                    current = state.location,
                    onPicked = actions.onLocationChanged,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentLocations(
    recent: List<String>,
    current: String,
    onPicked: (String) -> Unit,
) {
    val choices = recent.filterNot { it.equals(current.trim(), ignoreCase = true) }
    if (choices.isEmpty()) {
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.location_recent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        choices.forEach { place ->
            PillChip(
                label = place,
                isSelected = false,
                onClick = { onPicked(place) },
            )
        }
    }
}

@Composable
private fun NotesCard(
    notes: String,
    onNotesChanged: (String) -> Unit,
) {
    FormCard {
        IconRow(
            icon = R.drawable.ic_notes,
            iconAlignment = Alignment.Top,
            iconTopPadding = FIELD_ICON_TOP_PADDING,
        ) {
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                label = { Text(stringResource(R.string.label_notes)) },
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().testTag(MatchEditTestTags.NOTES),
            )
        }
    }
}
