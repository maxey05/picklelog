package com.maxeydev.picklelog.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.maxeydev.picklelog.ui.R

@Composable
fun DarkModeRow(
    darkTheme: Boolean?,
    onDarkThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val checked = darkTheme ?: isSystemInDarkTheme()
    SettingsRow(
        icon = R.drawable.ic_dark_mode,
        label = stringResource(R.string.settings_dark_mode_label),
        modifier =
            modifier
                .toggleable(value = checked, role = Role.Switch, onValueChange = onDarkThemeChanged)
                .testTag(SettingsTestTags.DARK_THEME_TOGGLE),
    ) {
        Switch(checked = checked, onCheckedChange = null)
    }
}
