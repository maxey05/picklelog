package com.maxeydev.picklelog.ui.match.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.currentLocale
import com.maxeydev.picklelog.ui.match.durationLine
import com.maxeydev.picklelog.ui.match.formatLabel
import com.maxeydev.picklelog.ui.match.formatMatchDate
import com.maxeydev.picklelog.ui.match.formatMatchTime
import com.maxeydev.picklelog.ui.match.resultLabel
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    state: MatchDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onEdit,
                        enabled = state.match != null,
                        modifier = Modifier.testTag(MatchDetailTestTags.EDIT),
                    ) {
                        Text(stringResource(R.string.action_edit))
                    }
                    TextButton(
                        onClick = onDeleteRequested,
                        enabled = state.match != null,
                        modifier = Modifier.testTag(MatchDetailTestTags.DELETE),
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                },
            )
        },
    ) { innerPadding ->
        state.match?.let { match ->
            MatchDetailContent(
                match = match,
                duration = state.duration,
                endsNextDay = state.endsNextDay,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
    if (state.isConfirmingDelete) {
        DeleteConfirmationDialog(onConfirm = onDeleteConfirmed, onDismiss = onDeleteDismissed)
    }
}

@Composable
private fun MatchDetailContent(
    match: Match,
    duration: Duration?,
    endsNextDay: Boolean,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_match_headline, resultLabel(match.result), formatLabel(match.format)),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        DetailRow(label = stringResource(R.string.label_date), value = formatMatchDate(match.date, locale))
        match.startTime?.let { start ->
            DetailRow(label = stringResource(R.string.label_start_time), value = formatMatchTime(start, locale))
        }
        match.endTime?.let { end ->
            DetailRow(label = stringResource(R.string.label_end_time), value = formatMatchTime(end, locale))
        }
        duration?.let { Text(text = durationLine(it, endsNextDay), style = MaterialTheme.typography.bodyLarge) }
        if (match.opponents.isNotEmpty()) {
            DetailRow(
                label = stringResource(R.string.label_opponents),
                value = match.opponents.joinToString { it.displayName },
            )
        }
        match.partner?.let { partner ->
            DetailRow(label = stringResource(R.string.slot_partner), value = partner.displayName)
        }
        if (match.games.isNotEmpty()) {
            DetailRow(
                label = stringResource(R.string.label_scores),
                value =
                    match.games
                        .map { game ->
                            stringResource(R.string.score_line, game.gameNumber, game.myScore, game.opponentScore)
                        }.joinToString(separator = "\n"),
            )
        }
        match.location?.let { DetailRow(label = stringResource(R.string.label_location), value = it) }
        match.paddle?.let { DetailRow(label = stringResource(R.string.label_paddle), value = it) }
        match.notes?.let { DetailRow(label = stringResource(R.string.label_notes), value = it) }
        if (match.photos.isNotEmpty()) {
            DetailRow(
                label = stringResource(R.string.label_photos),
                value = pluralStringResource(R.plurals.photo_count, match.photos.size, match.photos.size),
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
