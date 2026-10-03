package com.maxeydev.picklelog.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.theme.PicklelogTheme

@Composable
fun EraseDataSection(
    isErasing: Boolean,
    eraseFailed: Boolean,
    onOpenBackup: () -> Unit,
    onEraseConfirmed: () -> Unit,
    onEraseFailureDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    Surface(
        shape = SETTINGS_CARD_SHAPE,
        color = settingsCardColor(),
        border = BorderStroke(1.dp, PicklelogTheme.colors.cardBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        SettingsRow(
            icon = R.drawable.ic_warning,
            label = stringResource(R.string.settings_erase_open),
            contentColor = MaterialTheme.colorScheme.error,
            modifier =
                Modifier
                    .clickable(role = Role.Button) { confirming = true }
                    .testTag(SettingsTestTags.ERASE_OPEN),
        )
    }
    if (confirming) {
        EraseConfirmDialog(
            isErasing = isErasing,
            eraseFailed = eraseFailed,
            onExportFirst = {
                confirming = false
                onOpenBackup()
            },
            onConfirm = onEraseConfirmed,
            onDismiss = {
                confirming = false
                onEraseFailureDismissed()
            },
        )
    }
}

@Composable
private fun EraseConfirmDialog(
    isErasing: Boolean,
    eraseFailed: Boolean,
    onExportFirst: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var typed by rememberSaveable { mutableStateOf("") }
    val confirmWord = stringResource(R.string.settings_erase_confirm_word)
    val confirmed = typed.trim().equals(confirmWord, ignoreCase = true)
    AlertDialog(
        onDismissRequest = { if (!isErasing) onDismiss() },
        title = { Text(stringResource(R.string.settings_erase_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings_erase_dialog_body))
                TextButton(
                    onClick = onExportFirst,
                    enabled = !isErasing,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(SettingsTestTags.ERASE_EXPORT_FIRST),
                ) {
                    Text(stringResource(R.string.settings_erase_export_first))
                }
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = { Text(stringResource(R.string.settings_erase_type_prompt, confirmWord)) },
                    singleLine = true,
                    enabled = !isErasing,
                    modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.ERASE_CONFIRM_FIELD),
                )
                if (eraseFailed) {
                    Text(
                        text = stringResource(R.string.settings_erase_failed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(SettingsTestTags.ERASE_ERROR),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmed && !isErasing,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.heightIn(min = 48.dp).testTag(SettingsTestTags.ERASE_CONFIRM),
            ) {
                Text(stringResource(R.string.settings_erase_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isErasing, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
