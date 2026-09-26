package com.maxeydev.picklelog.ui.match.edit

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.currentLocale
import com.maxeydev.picklelog.ui.match.durationLine
import com.maxeydev.picklelog.ui.match.formatMatchTime
import kotlin.time.Duration

private val FALLBACK_PICKER_TIME = AppTime(12, 0)

private enum class TimeEndpoint {
    START,
    END,
}

@Composable
fun TimeRangeField(
    startTime: AppTime?,
    endTime: AppTime?,
    duration: Duration?,
    endsNextDay: Boolean,
    onStartTimeChanged: (AppTime?) -> Unit,
    onEndTimeChanged: (AppTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf<TimeEndpoint?>(null) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.label_time), style = MaterialTheme.typography.titleSmall)
        TimeEndpointRow(
            label = stringResource(R.string.label_start_time),
            time = startTime,
            clearDescription = stringResource(R.string.clear_start_time),
            onPick = { editing = TimeEndpoint.START },
            onClear = { onStartTimeChanged(null) },
        )
        TimeEndpointRow(
            label = stringResource(R.string.label_end_time),
            time = endTime,
            clearDescription = stringResource(R.string.clear_end_time),
            onPick = { editing = TimeEndpoint.END },
            onClear = { onEndTimeChanged(null) },
        )
        if (duration != null) {
            Text(text = durationLine(duration, endsNextDay), style = MaterialTheme.typography.bodyMedium)
        }
    }
    when (editing) {
        TimeEndpoint.START ->
            MatchTimePickerDialog(
                initial = startTime ?: endTime ?: FALLBACK_PICKER_TIME,
                onConfirm = { picked ->
                    onStartTimeChanged(picked)
                    editing = null
                },
                onDismiss = { editing = null },
            )
        TimeEndpoint.END ->
            MatchTimePickerDialog(
                initial = endTime ?: startTime ?: FALLBACK_PICKER_TIME,
                onConfirm = { picked ->
                    onEndTimeChanged(picked)
                    editing = null
                },
                onDismiss = { editing = null },
            )
        null -> Unit
    }
}

@Composable
private fun TimeEndpointRow(
    label: String,
    time: AppTime?,
    clearDescription: String,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val shown = time?.let { formatMatchTime(it, currentLocale()) } ?: stringResource(R.string.time_not_set)
    val buttonDescription = stringResource(R.string.time_button_description, label, shown)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.widthIn(min = 56.dp))
        OutlinedButton(
            onClick = onPick,
            modifier = Modifier.semantics { contentDescription = buttonDescription },
        ) {
            Text(shown)
        }
        if (time != null) {
            IconButton(onClick = onClear) {
                Icon(painter = painterResource(R.drawable.ic_close), contentDescription = clearDescription)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchTimePickerDialog(
    initial: AppTime,
    onConfirm: (AppTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pickerState =
        rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(AppTime(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        text = { TimePicker(state = pickerState) },
    )
}
