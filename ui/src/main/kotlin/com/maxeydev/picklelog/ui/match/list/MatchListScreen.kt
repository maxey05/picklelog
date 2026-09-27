package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.match.MatchSort
import com.maxeydev.picklelog.ui.R
import kotlinx.coroutines.flow.distinctUntilChanged

private val LIST_BOTTOM_PADDING = 88.dp
private const val MATCH_ROW_CONTENT_TYPE = "match_row"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MatchListScreen(
    state: MatchListUiState,
    onNewMatch: () -> Unit,
    onOpenMatch: (String) -> Unit,
    onSortSelected: (MatchSort) -> Unit,
    onLastVisibleIndexChanged: (Int) -> Unit,
    onLogAnother: (String) -> Unit,
    onSavedConfirmationDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    SavedMatchSnackbarEffect(
        savedMatchId = state.savedMatchId,
        snackbarHostState = snackbarHostState,
        onLogAnother = onLogAnother,
        onDismissed = onSavedConfirmationDismissed,
    )
    Scaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewMatch,
                modifier = Modifier.testTag(MatchListTestTags.NEW_MATCH),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.new_match),
                )
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when {
            state.isLoading -> Box(modifier = contentModifier)
            state.isEmpty -> MatchListEmptyState(onNewMatch = onNewMatch, modifier = contentModifier)
            else ->
                MatchListContent(
                    state = state,
                    onOpenMatch = onOpenMatch,
                    onSortSelected = onSortSelected,
                    onLastVisibleIndexChanged = onLastVisibleIndexChanged,
                    modifier = contentModifier,
                )
        }
    }
}

@Composable
private fun SavedMatchSnackbarEffect(
    savedMatchId: String?,
    snackbarHostState: SnackbarHostState,
    onLogAnother: (String) -> Unit,
    onDismissed: () -> Unit,
) {
    val message = stringResource(R.string.match_saved)
    val actionLabel = stringResource(R.string.log_another)
    val latestOnLogAnother by rememberUpdatedState(onLogAnother)
    val latestOnDismissed by rememberUpdatedState(onDismissed)
    LaunchedEffect(savedMatchId) {
        if (savedMatchId == null) {
            return@LaunchedEffect
        }
        val result =
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
        latestOnDismissed()
        if (result == SnackbarResult.ActionPerformed) {
            latestOnLogAnother(savedMatchId)
        }
    }
}

@Composable
private fun MatchListContent(
    state: MatchListUiState,
    onOpenMatch: (String) -> Unit,
    onSortSelected: (MatchSort) -> Unit,
    onLastVisibleIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val latestOnLastVisibleIndexChanged by rememberUpdatedState(onLastVisibleIndexChanged)
    var sortOnScreen by rememberSaveable { mutableStateOf(state.sort) }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to layoutInfo.totalItemsCount
        }.distinctUntilChanged()
            .collect { (lastVisibleIndex, _) -> latestOnLastVisibleIndexChanged(lastVisibleIndex) }
    }
    LaunchedEffect(state.sort) {
        if (state.sort != sortOnScreen) {
            sortOnScreen = state.sort
            listState.scrollToItem(0)
        }
    }

    Column(modifier = modifier) {
        SortMenu(
            activeSort = state.sort,
            onSortSelected = onSortSelected,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = LIST_BOTTOM_PADDING),
            modifier = Modifier.fillMaxSize().testTag(MatchListTestTags.LIST),
        ) {
            items(
                items = state.matches,
                key = { row -> row.id },
                contentType = { MATCH_ROW_CONTENT_TYPE },
            ) { row ->
                MatchRow(state = row, onClick = { onOpenMatch(row.id) })
            }
        }
    }
}

@Composable
private fun MatchListEmptyState(
    onNewMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .testTag(MatchListTestTags.EMPTY_STATE),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.list_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.list_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(
                onClick = onNewMatch,
                modifier = Modifier.testTag(MatchListTestTags.EMPTY_LOG_MATCH),
            ) {
                Text(text = stringResource(R.string.new_match))
            }
        }
    }
}
