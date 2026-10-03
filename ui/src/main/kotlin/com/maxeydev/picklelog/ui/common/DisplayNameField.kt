package com.maxeydev.picklelog.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.maxeydev.picklelog.domain.profile.DisplayName
import com.maxeydev.picklelog.ui.R

@Composable
fun DisplayNameField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.name_field_label)) },
        singleLine = true,
        isError = value.isNotEmpty() && value.isBlank(),
        supportingText = {
            Text(
                if (value.isBlank()) {
                    stringResource(R.string.name_field_required)
                } else {
                    stringResource(R.string.name_field_hint, DisplayName.MAX_LENGTH)
                },
            )
        },
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}
