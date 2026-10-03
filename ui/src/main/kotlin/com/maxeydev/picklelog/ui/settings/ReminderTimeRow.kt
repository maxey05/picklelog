package com.maxeydev.picklelog.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.currentLocale
import com.maxeydev.picklelog.ui.match.edit.MatchTimePickerDialog
import com.maxeydev.picklelog.ui.match.formatMatchTime

@Composable
fun ReminderTimeRow(
    time: AppTime,
    onTimeChanged: (AppTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val shown = formatMatchTime(time, currentLocale())
    val buttonDescription = stringResource(R.string.settings_reminder_time_button, shown)
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_reminder_time_label),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.settings_reminder_time_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = { picking = true },
            modifier =
                Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = buttonDescription }
                    .testTag(SettingsTestTags.REMINDER_TIME),
        ) {
            Text(shown)
        }
    }
    if (picking) {
        MatchTimePickerDialog(
            initial = time,
            onConfirm = { picked ->
                picking = false
                onTimeChanged(picked)
            },
            onDismiss = { picking = false },
        )
    }
}
