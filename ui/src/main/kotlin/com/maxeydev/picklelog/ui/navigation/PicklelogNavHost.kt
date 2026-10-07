package com.maxeydev.picklelog.ui.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.maxeydev.picklelog.ui.PicklelogDependencies
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
import com.maxeydev.picklelog.ui.onboarding.OnboardingScreen
import com.maxeydev.picklelog.ui.onboarding.OnboardingViewModel
import com.maxeydev.picklelog.ui.paywall.CapWarningBanner
import com.maxeydev.picklelog.ui.paywall.PaywallScreen
import com.maxeydev.picklelog.ui.paywall.PaywallViewModel
import com.maxeydev.picklelog.ui.settings.AboutScreen
import com.maxeydev.picklelog.ui.settings.BackupActions
import com.maxeydev.picklelog.ui.settings.BackupSettingsScreen
import com.maxeydev.picklelog.ui.settings.BackupViewModel
import com.maxeydev.picklelog.ui.settings.ExportPromptBanner
import com.maxeydev.picklelog.ui.settings.ExportPromptViewModel
import com.maxeydev.picklelog.ui.settings.PrivacyPolicyScreen
import com.maxeydev.picklelog.ui.settings.SettingsActions
import com.maxeydev.picklelog.ui.settings.SettingsDrawer
import com.maxeydev.picklelog.ui.settings.SettingsDrawerHost
import com.maxeydev.picklelog.ui.settings.SettingsViewModel
import com.maxeydev.picklelog.ui.settings.openStoreListing
import com.maxeydev.picklelog.ui.share.ResourceCardLabels
import com.maxeydev.picklelog.ui.share.ShareIntentLauncher
import com.maxeydev.picklelog.ui.share.SharePreviewScreen
import com.maxeydev.picklelog.ui.share.SharePreviewViewModel
import com.maxeydev.picklelog.ui.share.VariantPickerActions
import com.maxeydev.picklelog.ui.streak.MissedSkipNotice
import com.maxeydev.picklelog.ui.streak.SkipUsedNotice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

@Composable
fun PicklelogNavHost(
    dependencies: PicklelogDependencies,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openLogging: Boolean = false,
    onOpenLoggingHandled: () -> Unit = {},
) {
    val requiresOnboarding by produceState<Boolean?>(initialValue = null, dependencies) {
        value = dependencies.onboarding.isRequired()
    }
    requiresOnboarding?.let { required ->
        PicklelogNavGraph(
            dependencies = dependencies,
            requiresOnboarding = required,
            modifier = modifier,
            navController = navController,
            openLogging = openLogging,
            onOpenLoggingHandled = onOpenLoggingHandled,
        )
    }
}

@Composable
private fun PicklelogNavGraph(
    dependencies: PicklelogDependencies,
    requiresOnboarding: Boolean,
    modifier: Modifier,
    navController: NavHostController,
    openLogging: Boolean,
    onOpenLoggingHandled: () -> Unit,
) {
    val startDestination: Any = if (requiresOnboarding) OnboardingRoute else HomeRoute
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { ScreenEnter },
        exitTransition = { ScreenExit },
        popEnterTransition = { ScreenPopEnter },
        popExitTransition = { ScreenPopExit },
        predictivePopEnterTransition = { ScreenPredictivePopEnter },
        predictivePopExitTransition = { ScreenPredictivePopExit },
    ) {
        composable<OnboardingRoute> {
            val viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isFinished) {
                if (state.isFinished) {
                    navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
                }
            }
            OnboardingScreen(
                state = state,
                onNameChanged = viewModel::changeName,
                onContinue = viewModel::continueToApp,
            )
        }
        composable<HomeRoute>(
            exitTransition = { stayUnderSheet() },
            popEnterTransition = { returnFromSheet() },
        ) { backStackEntry ->
            val viewModel: MatchListViewModel =
                viewModel(factory = MatchListViewModel.factory(dependencies, backStackEntry.savedStateHandle))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val dashboardViewModel: DashboardViewModel =
                viewModel(factory = DashboardViewModel.factory(dependencies, backStackEntry.savedStateHandle))
            val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
            val exportPromptViewModel: ExportPromptViewModel =
                viewModel(factory = ExportPromptViewModel.factory(dependencies))
            val exportPromptState by exportPromptViewModel.uiState.collectAsStateWithLifecycle()
            var settingsOpen by rememberSaveable { mutableStateOf(false) }
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(dependencies))
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            LaunchedEffect(settingsState.isErased) {
                if (settingsState.isErased) {
                    settingsViewModel.erasedHandled()
                    settingsOpen = false
                    navController.navigate(OnboardingRoute) { popUpTo<HomeRoute> { inclusive = true } }
                }
            }
            LaunchedEffect(settingsOpen) {
                if (settingsOpen) {
                    settingsViewModel.refreshCacheSize()
                }
            }
            val settingsActions =
                remember(settingsViewModel) {
                    SettingsActions(
                        onClose = { settingsOpen = false },
                        onSeePro = { navController.navigate(PaywallRoute) },
                        onOpenBackup = { navController.navigate(BackupRoute) },
                        onOpenPrivacy = { navController.navigate(PrivacyRoute) },
                        onOpenAbout = { navController.navigate(AboutRoute) },
                        onRateUs = { openStoreListing(context) },
                        onClearCache = settingsViewModel::clearCache,
                        onEnableReminder = settingsViewModel::enableReminder,
                        onDisableReminder = settingsViewModel::disableReminder,
                        onReminderTimeChanged = settingsViewModel::changeReminderTime,
                        onNameChanged = settingsViewModel::changeName,
                        onSaveName = settingsViewModel::saveName,
                        onNameEditCancelled = settingsViewModel::discardNameDraft,
                        onDarkThemeChanged = settingsViewModel::changeDarkTheme,
                        onEraseConfirmed = settingsViewModel::eraseAll,
                        onEraseFailureDismissed = settingsViewModel::dismissEraseFailure,
                    )
                }
            SettingsDrawerHost(
                open = settingsOpen,
                onDismiss = { settingsOpen = false },
                drawer = { drawerModifier ->
                    SettingsDrawer(
                        state = settingsState,
                        versionName = dependencies.appVersionName,
                        actions = settingsActions,
                        modifier = drawerModifier,
                    )
                },
            ) {
                MatchListScreen(
                    state = state,
                    dashboard = dashboardState,
                    onOpenStats = { navController.navigate(StatsRoute) },
                    notices = {
                        Column {
                            CapWarningBanner(
                                warning = state.capWarning,
                                remainingFreeMatches = state.remainingFreeMatches,
                                onDismiss = viewModel::dismissCapWarning,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            )
                            ExportPromptBanner(
                                reason = exportPromptState.reason,
                                onExport = {
                                    exportPromptViewModel.dismiss()
                                    navController.navigate(BackupRoute)
                                },
                                onDismiss = exportPromptViewModel::dismiss,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            )
                            SkipUsedNotice(
                                skippedWeek = dashboardState.usedSkipWeek,
                                skipsHeld = dashboardState.skipsHeld,
                                onDismiss = dashboardViewModel::dismissUsedSkip,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            )
                            MissedSkipNotice(
                                opportunity = dashboardState.missedOpportunity,
                                onSeePro = { navController.navigate(PaywallRoute) },
                                onDismiss = dashboardViewModel::dismissMissedOpportunity,
                                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            )
                        }
                    },
                    onNewMatch = { navController.navigate(MatchEditRoute()) },
                    onOpenMatch = { matchId -> navController.navigate(MatchDetailRoute(matchId)) },
                    onSortSelected = viewModel::selectSort,
                    onLastVisibleIndexChanged = viewModel::loadMoreIfNeeded,
                    onLogAnother = { savedMatchId ->
                        navController.navigate(MatchEditRoute(logAnotherFrom = savedMatchId))
                    },
                    onSavedConfirmationDismissed = viewModel::dismissSavedConfirmation,
                    onOpenSettings = { settingsOpen = true },
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
        }
        composable<StatsRoute> {
            val homeEntry = remember(it) { navController.getBackStackEntry<HomeRoute>() }
            val viewModel: DashboardViewModel =
                viewModel(
                    viewModelStoreOwner = homeEntry,
                    factory = DashboardViewModel.factory(dependencies, homeEntry.savedStateHandle),
                )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ExpandedStatsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onSeePro = { navController.navigate(PaywallRoute) },
            )
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
                onDismissUpgrade = viewModel::dismissUpgrade,
                onSeePro = {
                    viewModel.dismissUpgrade()
                    navController.navigate(PaywallRoute)
                },
                variantActions =
                    remember(viewModel) {
                        VariantPickerActions(
                            onRatioSelected = viewModel::selectRatio,
                            onThemeSelected = viewModel::selectTheme,
                            onLayoutSelected = viewModel::selectLayout,
                            onDetailShownChanged = viewModel::setDetailShown,
                        )
                    },
            )
        }
        composable<PaywallRoute> {
            val viewModel: PaywallViewModel = viewModel(factory = PaywallViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.isUnlocked) {
                if (state.isUnlocked) {
                    navController.popBackStack()
                }
            }
            PaywallScreen(
                state = state,
                onBuy = viewModel::buy,
                onRestore = viewModel::restore,
                onRetryPrice = viewModel::retryPrice,
                onClose = { navController.popBackStack() },
            )
        }
        composable<PrivacyRoute> {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable<AboutRoute> {
            AboutScreen(
                versionName = dependencies.appVersionName,
                onBack = { navController.popBackStack() },
            )
        }
        composable<BackupRoute> {
            val viewModel: BackupViewModel = viewModel(factory = BackupViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val actions =
                remember(viewModel) {
                    BackupActions(
                        onBack = { navController.popBackStack() },
                        onExportRequested = viewModel::requestExport,
                        onExportConfirmed = viewModel::confirmExport,
                        onExportDialogDismissed = viewModel::dismissExportDialog,
                        onShareLaunched = viewModel::shareLaunched,
                        onShareFailed = viewModel::shareFailed,
                        onImportPicked = viewModel::importPicked,
                        onImportSummaryDismissed = viewModel::dismissImportSummary,
                        onMessageDismissed = viewModel::dismissMessage,
                    )
                }
            BackupSettingsScreen(state = state, actions = actions)
        }
        composable<MatchEditRoute>(
            enterTransition = { SheetEnter },
            popExitTransition = { SheetExit },
        ) { backStackEntry ->
            val viewModel: MatchEditViewModel = viewModel(factory = MatchEditViewModel.factory(dependencies))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            var hasSheetOpened by rememberSaveable { mutableStateOf(false) }
            val sheetProgress = remember { Animatable(if (hasSheetOpened) 1f else 0f) }
            val isLeaving = transition.targetState == EnterExitState.PostExit
            LaunchedEffect(isLeaving) {
                if (isLeaving) {
                    val isPopped = navController.currentBackStack.value.none { entry -> entry.id == backStackEntry.id }
                    if (isPopped) {
                        sheetProgress.animateTo(0f, SheetCloseSpec)
                    }
                } else if (sheetProgress.value < 1f) {
                    withTimeoutOrNull(SHEET_READY_TIMEOUT_MILLIS) {
                        snapshotFlow { viewModel.uiState.value.isLoading }.first { isLoading -> !isLoading }
                    }
                    withFrameNanos { }
                    withFrameNanos { }
                    sheetProgress.animateTo(1f, SheetOpenSpec)
                    hasSheetOpened = true
                }
            }
            LaunchedEffect(state.isPaywallRequested) {
                if (state.isPaywallRequested) {
                    viewModel.paywallOpened()
                    navController.navigate(PaywallRoute)
                }
            }
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
                        onUpgradePromptDismissed = viewModel::dismissUpgradePrompt,
                        onSeePro = {
                            viewModel.dismissUpgradePrompt()
                            navController.navigate(PaywallRoute)
                        },
                    )
                }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = SHEET_SCRIM_ALPHA * sheetProgress.value }
                            .background(Color.Black),
                )
                MatchEditScreen(
                    state = state,
                    actions = actions,
                    modifier =
                        Modifier.graphicsLayer {
                            val remaining = 1f - sheetProgress.value
                            val corner = SHEET_CORNER * remaining
                            translationY = remaining * size.height
                            shape = RoundedCornerShape(topStart = corner, topEnd = corner)
                            clip = true
                        },
                )
            }
        }
        composable<MatchDetailRoute>(
            exitTransition = { stayUnderSheet() },
            popEnterTransition = { returnFromSheet() },
        ) { backStackEntry ->
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
    LaunchedEffect(openLogging) {
        if (openLogging) {
            if (!requiresOnboarding) {
                navController.navigate(MatchEditRoute()) { launchSingleTop = true }
            }
            onOpenLoggingHandled()
        }
    }
}
