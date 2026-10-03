package com.maxeydev.picklelog.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.common.DisplayNameField

@Composable
fun ProfileSection(
    nameDraft: String,
    canSave: Boolean,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_profile_heading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        DisplayNameField(
            value = nameDraft,
            onValueChange = onNameChanged,
            onDone = { if (canSave) onSave() },
            modifier = Modifier.testTag(SettingsTestTags.NAME_FIELD),
        )
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.heightIn(min = 48.dp).padding(top = 4.dp).testTag(SettingsTestTags.NAME_SAVE),
        ) {
            Text(stringResource(R.string.settings_name_save))
        }
    }
}
