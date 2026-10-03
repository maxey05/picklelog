@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.match.MatchSort
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.dashboard.DashboardUiState
import com.maxeydev.picklelog.ui.theme.PicklelogTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.uuid.ExperimentalUuidApi

private val LIST_BOTTOM_SPACE = 88.dp
private const val MATCH_ROW_CONTENT_TYPE = "match_row"

@OptIn(ExperimentalComposeUiApi::class)
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
    dashboard: DashboardUiState = DashboardUiState(),
    notices: @Composable () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var searchText by rememberSaveable { mutableStateOf("") }
    var isFilterSheetOpen by rememberSaveable { mutableStateOf(false) }
    var isScrolledCollapse by remember { mutableStateOf(false) }
    val queryKey = listOf(state.sort, state.filter, state.appliedSearch).toString()
    var queryOnScreen by rememberSaveable { mutableStateOf(queryKey) }
    val isEmptyState = state.isEmpty && searchText.isBlank()
    val showsList = !state.isLoading && !isEmptyState
    val isCollapsed = showsList && (searchText.isNotBlank() || (isScrolledCollapse && !state.hasNoResults))

    SavedMatchSnackbarEffect(
        savedMatchId = state.savedMatchId,
        snackbarHostState = snackbarHostState,
        onLogAnother = onLogAnother,
        onDismissed = onSavedConfirmationDismissed,
    )
    LaunchedEffect(listState) {
        snapshotFlow { listState.canScrollBackward to listState.canScrollForward }
            .collect { (canScrollBackward, canScrollForward) ->
                if (canScrollBackward) {
                    isScrolledCollapse = true
                } else if (canScrollForward) {
                    isScrolledCollapse = false
                }
            }
    }
    LaunchedEffect(queryKey) {
        if (queryKey != queryOnScreen) {
            queryOnScreen = queryKey
            isScrolledCollapse = false
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showsList) {
                FloatingActionButton(
                    onClick = onNewMatch,
                    shape = MaterialTheme.shapes.large,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag(MatchListTestTags.NEW_MATCH),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.new_match),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HomeHeader(
                dashboard = dashboard,
                isCollapsed = isCollapsed,
                showDetails = showsList,
                searchText = searchText,
                onSearchChanged = { text ->
                    searchText = text
                    filterActions.onSearchChanged(text)
                },
                onOpenStats = onOpenStats,
                onOpenSettings = onOpenSettings,
            )
            notices()
            when {
                state.isLoading -> Box(modifier = Modifier.weight(1f).fillMaxWidth())
                isEmptyState ->
                    MatchListEmptyState(onNewMatch = onNewMatch, modifier = Modifier.weight(1f).fillMaxWidth())
                else ->
                    MatchListContent(
                        state = state,
                        listState = listState,
                        onOpenMatch = onOpenMatch,
                        onSortSelected = onSortSelected,
                        onOpenFilters = { isFilterSheetOpen = true },
                        onLastVisibleIndexChanged = onLastVisibleIndexChanged,
                        onFiltersAndSearchCleared = {
                            searchText = ""
                            filterActions.onFiltersAndSearchCleared()
                        },
                        filterActions = filterActions,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
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
    listState: LazyListState,
    onOpenMatch: (String) -> Unit,
    onSortSelected: (MatchSort) -> Unit,
    onOpenFilters: () -> Unit,
    onLastVisibleIndexChanged: (Int) -> Unit,
    onFiltersAndSearchCleared: () -> Unit,
    filterActions: MatchListFilterActions,
    modifier: Modifier = Modifier,
) {
    val latestOnLastVisibleIndexChanged by rememberUpdatedState(onLastVisibleIndexChanged)
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to layoutInfo.totalItemsCount
        }.distinctUntilChanged()
            .collect { (lastVisibleIndex, _) -> latestOnLastVisibleIndexChanged(lastVisibleIndex) }
    }
    Column(modifier = modifier) {
        MatchListToolbar(state = state, onSortSelected = onSortSelected, onOpenFilters = onOpenFilters)
        if (state.filter.isActive) {
            FilterChips(
                filter = state.filter,
                opponentName = state.filteredOpponentName,
                onFilterCleared = filterActions.onFilterCleared,
                onAllFiltersCleared = filterActions.onAllFiltersCleared,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (state.hasNoResults) {
            NoResultsState(onClear = onFiltersAndSearchCleared, modifier = Modifier.weight(1f).fillMaxWidth())
        } else {
            val cardShape = MaterialTheme.shapes.large
            Box(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 16.dp)
                        .clip(cardShape)
                        .border(1.dp, PicklelogTheme.colors.cardBorder, cardShape),
            ) {
                LazyColumn(state = listState, modifier = Modifier.testTag(MatchListTestTags.LIST)) {
                    itemsIndexed(
                        items = state.matches,
                        key = { _, row -> row.id },
                        contentType = { _, _ -> MATCH_ROW_CONTENT_TYPE },
                    ) { index, row ->
                        Column {
                            if (index > 0) {
                                HorizontalDivider(color = PicklelogTheme.colors.cardBorder)
                            }
                            MatchRow(state = row, onClick = { onOpenMatch(row.id) })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(LIST_BOTTOM_SPACE))
        }
    }
}
