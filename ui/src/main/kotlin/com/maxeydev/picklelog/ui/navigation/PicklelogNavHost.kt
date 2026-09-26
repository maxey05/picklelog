package com.maxeydev.picklelog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.maxeydev.picklelog.ui.PicklelogDependencies
import com.maxeydev.picklelog.ui.home.HomePlaceholderScreen
import com.maxeydev.picklelog.ui.home.HomePlaceholderViewModel
import com.maxeydev.picklelog.ui.match.detail.MatchDetailScreen
import com.maxeydev.picklelog.ui.match.detail.MatchDetailViewModel
import com.maxeydev.picklelog.ui.match.edit.MatchEditActions
import com.maxeydev.picklelog.ui.match.edit.MatchEditScreen
import com.maxeydev.picklelog.ui.match.edit.MatchEditViewModel

@Composable
fun PicklelogNavHost(
    dependencies: PicklelogDependencies,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            val viewModel: HomePlaceholderViewModel =
                viewModel(factory = HomePlaceholderViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            HomePlaceholderScreen(
                state = state,
                onNewMatch = { navController.navigate(MatchEditRoute()) },
                onOpenMatch = { matchId -> navController.navigate(MatchDetailRoute(matchId)) },
            )
        }
        composable<MatchEditRoute> {
            val viewModel: MatchEditViewModel = viewModel(factory = MatchEditViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isFinished) {
                if (state.isFinished) {
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
