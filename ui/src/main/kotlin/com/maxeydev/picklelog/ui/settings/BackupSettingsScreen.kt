package com.maxeydev.picklelog.ui.settings

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.backup.ImportSummary
import com.maxeydev.picklelog.domain.entitlement.FreeTier
import com.maxeydev.picklelog.ui.R

private val IMPORT_MIME_TYPES = arrayOf("*/*")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    state: BackupUiState,
    actions: BackupActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            actions.onImportPicked(uri?.toString())
        }
    LaunchedEffect(state.pendingShare) {
        val share = state.pendingShare ?: return@LaunchedEffect
        try {
            ExportShareLauncher.launch(context, share)
            actions.onShareLaunched()
        } catch (noShareTarget: ActivityNotFoundException) {
            actions.onShareFailed()
        }
    }
    Scaffold(
        modifier = modifier.testTag(BackupTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.backup_honesty),
                modifier = Modifier.testTag(BackupTestTags.HONESTY),
            )
            LastExportText(lastExportAt = state.lastExportAt)
            state.message?.let { MessageCard(message = it, onDismiss = actions.onMessageDismissed) }
            state.importSummary?.let { ImportSummaryCard(summary = it, onDismiss = actions.onImportSummaryDismissed) }
            ExportSection(state = state, onExport = actions.onExportRequested)
            ImportSection(state = state, onImport = { picker.launch(IMPORT_MIME_TYPES) })
        }
    }
    if (state.isExportDialogVisible) {
        ExportDialog(onConfirm = actions.onExportConfirmed, onDismiss = actions.onExportDialogDismissed)
    }
}

@Composable
private fun ExportSection(
    state: BackupUiState,
    onExport: () -> Unit,
) {
    SectionHeading(R.string.backup_export_heading)
    Text(stringResource(R.string.backup_export_body))
    Text(
        text = stringResource(R.string.backup_photos_not_included),
        fontWeight = FontWeight.SemiBold,
    )
    Button(
        onClick = onExport,
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(BackupTestTags.EXPORT),
    ) {
        if (state.isExporting) {
            WorkingIndicator(R.string.backup_export_working)
        } else {
            Text(stringResource(R.string.backup_export_button))
        }
    }
}

@Composable
private fun ImportSection(
    state: BackupUiState,
    onImport: () -> Unit,
) {
    SectionHeading(R.string.backup_import_heading)
    Text(stringResource(R.string.backup_import_body))
    Text(
        text = stringResource(R.string.backup_import_free_note, FreeTier.MATCH_LIMIT),
        modifier = Modifier.testTag(BackupTestTags.IMPORT_FREE_NOTE),
    )
    OutlinedButton(
        onClick = onImport,
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(BackupTestTags.IMPORT),
    ) {
        if (state.isImporting) {
            WorkingIndicator(R.string.backup_import_working)
        } else {
            Text(stringResource(R.string.backup_import_button))
        }
    }
}

@Composable
private fun SectionHeading(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun WorkingIndicator(label: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(modifier = Modifier.heightIn(max = 20.dp))
        Text(text = stringResource(label), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MessageCard(
    message: BackupMessage,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag(BackupTestTags.MESSAGE),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(message.text),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp).testTag(BackupTestTags.MESSAGE_DISMISS),
            ) {
                Text(stringResource(R.string.backup_result_ok))
            }
        }
    }
}

@Composable
private fun ImportSummaryCard(
    summary: ImportSummary,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag(BackupTestTags.IMPORT_SUMMARY),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = pluralStringResource(R.plurals.backup_import_added, summary.added, summary.added),
                    fontWeight = FontWeight.SemiBold,
                )
                if (summary.alreadyPresent > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.backup_import_skipped,
                            summary.alreadyPresent,
                            summary.alreadyPresent,
                        ),
                    )
                }
                if (summary.heldBack > 0) {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.backup_import_held_back,
                                summary.heldBack,
                                summary.heldBack,
                                FreeTier.MATCH_LIMIT,
                            ),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(BackupTestTags.IMPORT_HELD_BACK),
                    )
                }
                if (summary.photosNotRestored > 0) {
                    Text(
                        pluralStringResource(
                            R.plurals.backup_import_photos_missing,
                            summary.photosNotRestored,
                            summary.photosNotRestored,
                        ),
                    )
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp).testTag(BackupTestTags.IMPORT_SUMMARY_DISMISS),
            ) {
                Text(stringResource(R.string.backup_result_ok))
            }
        }
    }
}

@Composable
private fun ExportDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(BackupTestTags.EXPORT_DIALOG),
        title = { Text(stringResource(R.string.backup_export_dialog_title)) },
        text = {
            Text(
                text = stringResource(R.string.backup_photos_not_included),
                modifier = Modifier.testTag(BackupTestTags.EXPORT_DIALOG_PHOTOS),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 48.dp).testTag(BackupTestTags.EXPORT_DIALOG_CONFIRM),
            ) {
                Text(stringResource(R.string.backup_export_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp).testTag(BackupTestTags.EXPORT_DIALOG_CANCEL),
            ) {
                Text(stringResource(R.string.backup_dialog_cancel))
            }
        },
    )
}
