package com.maxeydev.picklelog.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute

private const val SHEET_OPEN_MILLIS = 400
private const val SHEET_CLOSE_MILLIS = 260

private val SheetOpenEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val SheetCloseEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

internal val SHEET_CORNER = 28.dp
internal const val SHEET_SCRIM_ALPHA = 0.32f

internal val SheetEnter: EnterTransition = EnterTransition.None

internal val SheetExit: ExitTransition = ExitTransition.KeepUntilTransitionsFinished

internal fun <T> sheetCornerSpec(target: EnterExitState): FiniteAnimationSpec<T> =
    if (target == EnterExitState.Visible) {
        tween(durationMillis = SHEET_OPEN_MILLIS, easing = SheetOpenEasing)
    } else {
        tween(durationMillis = SHEET_CLOSE_MILLIS, easing = SheetCloseEasing)
    }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.stayUnderSheet(): ExitTransition? =
    if (targetState.destination.hasRoute<MatchEditRoute>()) {
        ExitTransition.KeepUntilTransitionsFinished
    } else {
        null
    }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.returnFromSheet(): EnterTransition? =
    if (initialState.destination.hasRoute<MatchEditRoute>()) {
        EnterTransition.None
    } else {
        null
    }
