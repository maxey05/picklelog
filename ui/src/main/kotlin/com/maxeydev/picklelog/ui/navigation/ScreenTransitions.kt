package com.maxeydev.picklelog.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val SCREEN_MOVE_MILLIS = 300
private const val SCREEN_FADE_OUT_MILLIS = 90
private const val SCREEN_FADE_IN_MILLIS = 210
private const val SCREEN_SHIFT_DIVISOR = 12

private val ScreenEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun <T> moveSpec() = tween<T>(durationMillis = SCREEN_MOVE_MILLIS, easing = ScreenEasing)

private fun <T> fadeOutSpec() = tween<T>(durationMillis = SCREEN_FADE_OUT_MILLIS, easing = LinearEasing)

private fun <T> fadeInSpec() =
    tween<T>(
        durationMillis = SCREEN_FADE_IN_MILLIS,
        delayMillis = SCREEN_FADE_OUT_MILLIS,
        easing = LinearEasing,
    )

internal val ScreenEnter: EnterTransition =
    slideInHorizontally(animationSpec = moveSpec()) { fullWidth -> fullWidth / SCREEN_SHIFT_DIVISOR } +
        fadeIn(animationSpec = fadeInSpec())

internal val ScreenExit: ExitTransition =
    slideOutHorizontally(animationSpec = moveSpec()) { fullWidth -> -fullWidth / SCREEN_SHIFT_DIVISOR } +
        fadeOut(animationSpec = fadeOutSpec())

internal val ScreenPopEnter: EnterTransition =
    slideInHorizontally(animationSpec = moveSpec()) { fullWidth -> -fullWidth / SCREEN_SHIFT_DIVISOR } +
        fadeIn(animationSpec = fadeInSpec())

internal val ScreenPopExit: ExitTransition =
    slideOutHorizontally(animationSpec = moveSpec()) { fullWidth -> fullWidth / SCREEN_SHIFT_DIVISOR } +
        fadeOut(animationSpec = fadeOutSpec())
