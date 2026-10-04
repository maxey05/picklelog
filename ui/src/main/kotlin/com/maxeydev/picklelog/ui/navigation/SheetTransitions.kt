package com.maxeydev.picklelog.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute

private const val SHEET_OPEN_MILLIS = 400
private const val SHEET_CLOSE_MILLIS = 260
private const val UNDERLAY_SHIFT_DIVISOR = 20

private val SheetOpenEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val SheetCloseEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

private val SheetOpenSpec = tween<IntOffset>(durationMillis = SHEET_OPEN_MILLIS, easing = SheetOpenEasing)
private val SheetCloseSpec = tween<IntOffset>(durationMillis = SHEET_CLOSE_MILLIS, easing = SheetCloseEasing)

internal val SHEET_CORNER = 28.dp

internal val SheetEnter: EnterTransition =
    slideInVertically(animationSpec = SheetOpenSpec, initialOffsetY = { fullHeight -> fullHeight })

internal val SheetExit: ExitTransition =
    slideOutVertically(animationSpec = SheetCloseSpec, targetOffsetY = { fullHeight -> fullHeight })

private val UnderlayExit: ExitTransition =
    slideOutVertically(
        animationSpec = SheetOpenSpec,
        targetOffsetY = { fullHeight -> -fullHeight / UNDERLAY_SHIFT_DIVISOR },
    )

private val UnderlayReturn: EnterTransition =
    slideInVertically(
        animationSpec = SheetCloseSpec,
        initialOffsetY = { fullHeight -> -fullHeight / UNDERLAY_SHIFT_DIVISOR },
    )

internal fun <T> sheetCornerSpec(target: EnterExitState): FiniteAnimationSpec<T> =
    if (target == EnterExitState.Visible) {
        tween(durationMillis = SHEET_OPEN_MILLIS, easing = SheetOpenEasing)
    } else {
        tween(durationMillis = SHEET_CLOSE_MILLIS, easing = SheetCloseEasing)
    }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.stayUnderSheet(): ExitTransition? =
    if (targetState.destination.hasRoute<MatchEditRoute>()) {
        UnderlayExit
    } else {
        null
    }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.returnFromSheet(): EnterTransition? =
    if (initialState.destination.hasRoute<MatchEditRoute>()) {
        UnderlayReturn
    } else {
        null
    }
