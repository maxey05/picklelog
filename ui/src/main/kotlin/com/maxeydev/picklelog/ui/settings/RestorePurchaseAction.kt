package com.maxeydev.picklelog.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.paywall.StoreMessage

@Composable
fun RestorePurchaseAction(
    isRestoring: Boolean,
    message: StoreMessage?,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onRestore,
            enabled = !isRestoring,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(SettingsTestTags.RESTORE),
        ) {
            if (isRestoring) {
                CircularProgressIndicator(modifier = Modifier.heightIn(max = 20.dp))
            } else {
                Text(stringResource(R.string.settings_restore))
            }
        }
        message?.let {
            Text(
                text = stringResource(it.text),
                modifier =
                    Modifier
                        .testTag(SettingsTestTags.RESTORE_MESSAGE)
                        .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
