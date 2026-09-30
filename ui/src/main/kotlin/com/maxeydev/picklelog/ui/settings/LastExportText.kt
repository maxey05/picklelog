package com.maxeydev.picklelog.ui.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.ui.R
import java.time.ZoneId

@Composable
fun LastExportText(
    lastExportAt: AppInstant?,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val text =
        if (lastExportAt == null) {
            stringResource(R.string.backup_never_exported)
        } else {
            val date = formatBackupDate(lastExportAt, ZoneId.systemDefault(), locale)
            stringResource(R.string.backup_last_export, date)
        }
    Text(
        text = text,
        modifier = modifier.testTag(BackupTestTags.LAST_EXPORT),
    )
}
