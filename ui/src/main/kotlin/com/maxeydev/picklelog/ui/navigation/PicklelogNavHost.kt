package com.maxeydev.picklelog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.match.detail.MatchDetailScreen
import com.maxeydev.picklelog.ui.match.detail.MatchDetailViewModel
import com.maxeydev.picklelog.ui.match.edit.MatchEditActions
import com.maxeydev.picklelog.ui.match.edit.MatchEditScreen
import com.maxeydev.picklelog.ui.match.edit.MatchEditViewModel
import com.maxeydev.picklelog.ui.match.list.MatchListFilterActions
import com.maxeydev.picklelog.ui.match.list.MatchListScreen
import com.maxeydev.picklelog.ui.match.list.MatchListViewModel

@Composable
fun PicklelogNavHost(
    dependencies: PicklelogDependencies,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> { backStackEntry ->
            val viewModel: MatchListViewModel =
                viewModel(factory = MatchListViewModel.factory(dependencies, backStackEntry.savedStateHandle))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MatchListScreen(
                state = state,
                onNewMatch = { navController.navigate(MatchEditRoute()) },
                onOpenMatch = { matchId -> navController.navigate(MatchDetailRoute(matchId)) },
                onSortSelected = viewModel::selectSort,
                onLastVisibleIndexChanged = viewModel::loadMoreIfNeeded,
                onLogAnother = { savedMatchId ->
                    navController.navigate(MatchEditRoute(logAnotherFrom = savedMatchId))
                },
                onSavedConfirmationDismissed = viewModel::dismissSavedConfirmation,
                filterActions =
                    remember(viewModel) {
                        MatchListFilterActions(
                            onSearchChanged = viewModel::changeSearch,
                            onFilterChanged = viewModel::changeFilter,
                            onFilterCleared = viewModel::clearFilter,
                            onAllFiltersCleared = viewModel::clearAllFilters,
                            onFiltersAndSearchCleared = viewModel::clearFiltersAndSearch,
                        )
                    },
            )
        }
        composable<MatchEditRoute> {
            val viewModel: MatchEditViewModel = viewModel(factory = MatchEditViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isFinished) {
                if (state.isFinished) {
                    state.savedNewMatchId?.let { savedMatchId ->
                        navController.previousBackStackEntry
                            ?.takeIf { it.destination.hasRoute<HomeRoute>() }
                            ?.savedStateHandle
                            ?.set(JUST_SAVED_MATCH_ID_KEY, savedMatchId)
                    }
                    navController.popBackStack()
                }
            }
            val actions =
                remember(viewModel) {
                    MatchEditActions(
                        onFormatSelected = viewModel::selectFormat,
                        onResultSelected = viewModel::selectResult,
                        onDateSelected = viewModel::selectDate,
                        onStartTimeChanged = viewModel::changeStartTime,
                        onEndTimeChanged = viewModel::changeEndTime,
                        onPersonNameChanged = viewModel::changePersonName,
                        onGameAdded = viewModel::addGame,
                        onGameRemoved = viewModel::removeGame,
                        onGameScoresChanged = viewModel::changeGameScores,
                        onLocationChanged = viewModel::changeLocation,
                        onPaddleChanged = viewModel::changePaddle,
                        onNotesChanged = viewModel::changeNotes,
                        onSuggestionFocusChanged = { target, isFocused ->
                            if (isFocused) {
                                viewModel.focusSuggestionTarget(target)
                            } else {
                                viewModel.leaveSuggestionTarget(target)
                            }
                        },
                        onSuggestionSelected = viewModel::selectSuggestion,
                        onSave = viewModel::save,
                        onClose = { navController.popBackStack() },
                    )
                }
            MatchEditScreen(state = state, actions = actions)
        }
        composable<MatchDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<MatchDetailRoute>()
            val viewModel: MatchDetailViewModel = viewModel(factory = MatchDetailViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isGone) {
                if (state.isGone) {
                    navController.popBackStack()
                }
            }
            MatchDetailScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(MatchEditRoute(route.matchId)) },
                onDeleteRequested = viewModel::requestDelete,
                onDeleteConfirmed = viewModel::confirmDelete,
                onDeleteDismissed = viewModel::dismissDelete,
            )
        }
    }
}
