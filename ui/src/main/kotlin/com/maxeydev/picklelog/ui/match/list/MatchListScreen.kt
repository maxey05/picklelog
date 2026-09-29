package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
private val MIN_TOUCH_TARGET = 48.dp
private const val MATCH_ROW_CONTENT_TYPE = "match_row"
private const val HEADER_KEY = "match_list_header"
private const val NO_RESULTS_KEY = "match_list_no_results"
private const val ITEMS_ABOVE_MATCHES = 1
private val NO_RESULTS_TOP_PADDING = 48.dp

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
    filterActions: MatchListFilterActions,
    modifier: Modifier = Modifier,
    dashboard: @Composable () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var searchText by rememberSaveable { mutableStateOf("") }
    SavedMatchSnackbarEffect(
        savedMatchId = state.savedMatchId,
        snackbarHostState = snackbarHostState,
        onLogAnother = onLogAnother,
        onDismissed = onSavedConfirmationDismissed,
    )
    Scaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    if (onOpenSettings != null) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag(MatchListTestTags.SETTINGS)) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.settings_open),
                            )
                        }
                    }
                },
            )
        },
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
        MatchListBody(
            state = state,
            searchText = searchText,
            onSearchTextChanged = { searchText = it },
            onNewMatch = onNewMatch,
            onOpenMatch = onOpenMatch,
            onSortSelected = onSortSelected,
            onLastVisibleIndexChanged = onLastVisibleIndexChanged,
            filterActions = filterActions,
            dashboard = dashboard,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
private fun MatchListBody(
    state: MatchListUiState,
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onNewMatch: () -> Unit,
    onOpenMatch: (String) -> Unit,
    onSortSelected: (MatchSort) -> Unit,
    onLastVisibleIndexChanged: (Int) -> Unit,
    filterActions: MatchListFilterActions,
    dashboard: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> Box(modifier = modifier)
        state.isEmpty && searchText.isBlank() ->
            Column(modifier = modifier) {
                dashboard()
                MatchListEmptyState(onNewMatch = onNewMatch, modifier = Modifier.weight(1f).fillMaxWidth())
            }
        else ->
            MatchListContent(
                state = state,
                searchText = searchText,
                onSearchChanged = { text ->
                    onSearchTextChanged(text)
                    filterActions.onSearchChanged(text)
                },
                onFiltersAndSearchCleared = {
                    onSearchTextChanged("")
                    filterActions.onFiltersAndSearchCleared()
                },
                filterActions = filterActions,
                onOpenMatch = onOpenMatch,
                onSortSelected = onSortSelected,
                onLastVisibleIndexChanged = onLastVisibleIndexChanged,
                dashboard = dashboard,
                modifier = modifier,
            )
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
    searchText: String,
    onSearchChanged: (String) -> Unit,
    onFiltersAndSearchCleared: () -> Unit,
    filterActions: MatchListFilterActions,
    onOpenMatch: (String) -> Unit,
    onSortSelected: (MatchSort) -> Unit,
    onLastVisibleIndexChanged: (Int) -> Unit,
    dashboard: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val latestOnLastVisibleIndexChanged by rememberUpdatedState(onLastVisibleIndexChanged)
    val queryKey = listOf(state.sort, state.filter, state.appliedSearch).toString()
    var queryOnScreen by rememberSaveable { mutableStateOf(queryKey) }
    var isFilterSheetOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to layoutInfo.totalItemsCount
        }.distinctUntilChanged()
            .collect { (lastVisibleIndex, _) ->
                latestOnLastVisibleIndexChanged((lastVisibleIndex - ITEMS_ABOVE_MATCHES).coerceAtLeast(0))
            }
    }
    LaunchedEffect(queryKey) {
        if (queryKey != queryOnScreen) {
            queryOnScreen = queryKey
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = LIST_BOTTOM_PADDING),
        modifier = modifier.testTag(MatchListTestTags.LIST),
    ) {
        item(key = HEADER_KEY, contentType = HEADER_KEY) {
            MatchListHeader(
                state = state,
                dashboard = dashboard,
                searchText = searchText,
                onSearchChanged = onSearchChanged,
                onSortSelected = onSortSelected,
                onOpenFilters = { isFilterSheetOpen = true },
                filterActions = filterActions,
            )
        }
        if (state.hasNoResults) {
            item(key = NO_RESULTS_KEY, contentType = NO_RESULTS_KEY) {
                NoResultsState(onClear = onFiltersAndSearchCleared, modifier = Modifier.fillMaxWidth())
            }
        } else {
            items(
                items = state.matches,
                key = { row -> row.id },
                contentType = { MATCH_ROW_CONTENT_TYPE },
            ) { row ->
                MatchRow(state = row, onClick = { onOpenMatch(row.id) })
            }
        }
    }
    if (isFilterSheetOpen) {
        FilterSheet(
            filter = state.filter,
            opponentChoices = state.opponentChoices,
            locationChoices = state.locationChoices,
            onFilterChanged = filterActions.onFilterChanged,
            onAllFiltersCleared = filterActions.onAllFiltersCleared,
            onDismiss = { isFilterSheetOpen = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchListHeader(
    state: MatchListUiState,
    dashboard: @Composable () -> Unit,
    searchText: String,
    onSearchChanged: (String) -> Unit,
    onSortSelected: (MatchSort) -> Unit,
    onOpenFilters: () -> Unit,
    filterActions: MatchListFilterActions,
) {
    Column {
        dashboard()
        MatchSearchBar(
            text = searchText,
            onTextChanged = onSearchChanged,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
        ) {
            SortMenu(activeSort = state.sort, onSortSelected = onSortSelected)
            FilterButton(activeCount = state.filter.activeKinds.size, onClick = onOpenFilters)
        }
        if (state.filter.isActive) {
            FilterChips(
                filter = state.filter,
                opponentName = state.filteredOpponentName,
                onFilterCleared = filterActions.onFilterCleared,
                onAllFiltersCleared = filterActions.onAllFiltersCleared,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun FilterButton(
    activeCount: Int,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET).testTag(MatchListTestTags.FILTER_BUTTON),
    ) {
        Icon(painter = painterResource(R.drawable.ic_filter), contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text =
                if (activeCount == 0) {
                    stringResource(R.string.filter_button)
                } else {
                    stringResource(R.string.filter_button_active, activeCount)
                },
        )
    }
}

@Composable
private fun NoResultsState(
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = NO_RESULTS_TOP_PADDING, bottom = 24.dp)
                    .testTag(MatchListTestTags.NO_RESULTS),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.no_results_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.no_results_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(
                onClick = onClear,
                modifier = Modifier.testTag(MatchListTestTags.NO_RESULTS_CLEAR),
            ) {
                Text(text = stringResource(R.string.no_results_clear))
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
