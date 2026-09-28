package com.maxeydev.picklelog.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklelog.ui.R

object PermissionTestTags {
    const val RATIONALE = "permission_rationale"
    const val RATIONALE_CONTINUE = "permission_rationale_continue"
    const val SETTINGS = "permission_settings"
    const val SETTINGS_OPEN = "permission_settings_open"
}

@Composable
fun PermissionRationale(
    title: String,
    body: String,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onContinue,
                modifier = Modifier.testTag(PermissionTestTags.RATIONALE_CONTINUE),
            ) {
                Text(stringResource(R.string.permission_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.permission_not_now))
            }
        },
        modifier = Modifier.testTag(PermissionTestTags.RATIONALE),
    )
}

@Composable
fun PermissionSettingsRedirect(
    title: String,
    body: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag(PermissionTestTags.SETTINGS_OPEN),
            ) {
                Text(stringResource(R.string.permission_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.permission_not_now))
            }
        },
        modifier = Modifier.testTag(PermissionTestTags.SETTINGS),
    )
}
