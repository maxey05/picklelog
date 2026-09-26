package com.maxeydev.picklelog.ui.match.edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.ui.R

@Composable
fun PersonSlotField(
    slot: PersonSlotUiState,
    format: MatchFormat,
    onNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val duplicateOf = slot.duplicateOf
    OutlinedTextField(
        value = slot.name,
        onValueChange = onNameChanged,
        label = { Text(personSlotLabel(slot.slot, format)) },
        singleLine = true,
        isError = duplicateOf != null,
        supportingText =
            if (duplicateOf != null) {
                { Text(stringResource(R.string.duplicate_person, personSlotLabel(duplicateOf, format))) }
            } else {
                null
            },
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        modifier = modifier.fillMaxWidth().testTag(MatchEditTestTags.personSlot(slot.slot)),
    )
}

@Composable
fun personSlotLabel(
    slot: PersonSlot,
    format: MatchFormat,
): String =
    when (slot) {
        PersonSlot.OPPONENT_1 ->
            if (format == MatchFormat.SINGLES) {
                stringResource(R.string.slot_opponent)
            } else {
                stringResource(R.string.slot_opponent_1)
            }
        PersonSlot.OPPONENT_2 -> stringResource(R.string.slot_opponent_2)
        PersonSlot.PARTNER -> stringResource(R.string.slot_partner)
    }
