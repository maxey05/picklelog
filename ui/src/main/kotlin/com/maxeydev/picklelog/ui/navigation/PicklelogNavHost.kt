package com.maxeydev.picklelog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.dashboard.DashboardHeader
import com.maxeydev.picklelog.ui.dashboard.DashboardViewModel
import com.maxeydev.picklelog.ui.dashboard.ExpandedStatsScreen
import com.maxeydev.picklelog.ui.match.detail.MatchDetailScreen
import com.maxeydev.picklelog.ui.match.detail.MatchDetailViewModel
import com.maxeydev.picklelog.ui.match.edit.MatchEditActions
import com.maxeydev.picklelog.ui.match.edit.MatchEditScreen
import com.maxeydev.picklelog.ui.match.edit.MatchEditViewModel
import com.maxeydev.picklelog.ui.match.edit.PhotoPickerActions
import com.maxeydev.picklelog.ui.match.list.MatchListFilterActions
import com.maxeydev.picklelog.ui.match.list.MatchListScreen
import com.maxeydev.picklelog.ui.match.list.MatchListViewModel
import com.maxeydev.picklelog.ui.share.ResourceCardLabels
import com.maxeydev.picklelog.ui.share.ShareIntentLauncher
import com.maxeydev.picklelog.ui.share.SharePreviewScreen
import com.maxeydev.picklelog.ui.share.SharePreviewViewModel
import kotlinx.coroutines.launch
import java.io.IOException

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
            val dashboardViewModel: DashboardViewModel =
                viewModel(factory = DashboardViewModel.factory(dependencies, backStackEntry.savedStateHandle))
            val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
            MatchListScreen(
                state = state,
                dashboard = {
                    DashboardHeader(
                        state = dashboardState,
                        onOpenStats = { navController.navigate(StatsRoute) },
                    )
                },
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
        composable<StatsRoute> {
            val homeEntry = remember(it) { navController.getBackStackEntry<HomeRoute>() }
            val viewModel: DashboardViewModel =
                viewModel(factory = DashboardViewModel.factory(dependencies, homeEntry.savedStateHandle))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ExpandedStatsScreen(state = state, onBack = { navController.popBackStack() })
        }
        composable<ShareRoute> {
            val context = LocalContext.current
            val labels = remember(context) { ResourceCardLabels(context) }
            val viewModel: SharePreviewViewModel =
                viewModel(factory = SharePreviewViewModel.factory(dependencies, labels))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val launcher = remember { ShareIntentLauncher(dependencies.ioDispatcher) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(state.isGone) {
                if (state.isGone) {
                    navController.popBackStack()
                }
            }
            SharePreviewScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onShare = {
                    state.card?.let { card ->
                        scope.launch {
                            try {
                                launcher.launch(context, launcher.prepare(context, card))
                            } catch (unwritable: IOException) {
                                viewModel.reportShareFailed()
                            }
                        }
                    }
                },
                onRetry = viewModel::retry,
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
                        photoActions =
                            PhotoPickerActions(
                                newCaptureUri = dependencies::newCaptureUri,
                                onPhotosPicked = viewModel::addPickedPhotos,
                                onPhotoCaptured = viewModel::addCapturedPhoto,
                                onPhotoMoved = viewModel::movePhoto,
                                onPhotoRemoved = viewModel::removePhoto,
                                onPhotoErrorDismissed = viewModel::dismissPhotoError,
                            ),
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
                onShare = { navController.navigate(ShareRoute(route.matchId)) },
                onDeleteRequested = viewModel::requestDelete,
                onDeleteConfirmed = viewModel::confirmDelete,
                onDeleteDismissed = viewModel::dismissDelete,
            )
        }
    }
}
