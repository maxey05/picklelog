package com.maxeydev.picklelog.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.resultLabel
import com.maxeydev.picklelog.ui.theme.PicklelogTheme
import kotlinx.coroutines.delay

private val MIN_TOUCH_TARGET = 48.dp
private val DOT_SIZE = 12.dp
private val DOT_RING_WIDTH = 2.dp
private val FLAME_SIZE = 16.dp
private val DOT_SPACING = 6.dp
private val FLAME_CENTER_X = 12.dp + FLAME_SIZE / 2
private const val FLAME_FLARE_GAIN = 0.5f
private const val LAYOUT_FADE_IN_MILLIS = 220
private const val LAYOUT_FADE_DELAY_MILLIS = 60
private const val LAYOUT_FADE_OUT_MILLIS = 140
private const val LAYOUT_SIZE_MILLIS = 320
private const val DOTS_SLIDE_MILLIS = 420
private const val DOTS_SLIDE_DELAY_MILLIS = 360L
private const val WIN_RATE_ROLL_DELAY_MILLIS = 240L
private const val RECORD_ROLL_DELAY_MILLIS = 320L
private const val STREAK_ROLL_DELAY_MILLIS = 360L
private const val RESULTS_SEPARATOR = ","

@Stable
private class HeaderFigures(
    val winPercent: Int?,
    val wins: Int,
    val losses: Int,
    val streak: Int,
    val shownStreak: Int,
    val recent: List<MatchResult>,
    val winRateStep: RollStep,
    val recordStep: RollStep,
    val streakStep: RollStep,
    val burstProgress: () -> Float,
)

private fun resultsKey(results: List<MatchResult>): String = results.joinToString(RESULTS_SEPARATOR) { it.name }

private fun resultsFrom(key: String): List<MatchResult> =
    if (key.isEmpty()) emptyList() else key.split(RESULTS_SEPARATOR).map(MatchResult::valueOf)

@Composable
private fun rememberHeaderFigures(state: DashboardUiState): HeaderFigures {
    val overall = state.overallStats.overall
    val current = state.streak.current
    val winRateStep = remember { RollStep() }
    val recordStep = remember { RollStep() }
    val streakStep = remember { RollStep() }
    val winPercent = overall.winPercent
    val shownWinPercent =
        rememberRolledValue(target = winPercent ?: 0, step = winRateStep, startDelayMillis = WIN_RATE_ROLL_DELAY_MILLIS)
    val shownWins =
        rememberRolledValue(target = overall.wins, step = recordStep, startDelayMillis = RECORD_ROLL_DELAY_MILLIS)
    val shownLosses =
        rememberRolledValue(target = overall.losses, step = recordStep, startDelayMillis = RECORD_ROLL_DELAY_MILLIS)
    val shownStreak =
        rememberRolledValue(target = current, step = streakStep, startDelayMillis = STREAK_ROLL_DELAY_MILLIS)
    val burst = remember { Animatable(0f) }
    var previousStreak by rememberSaveable { mutableIntStateOf(current) }
    LaunchedEffect(current) {
        val before = previousStreak
        previousStreak = current
        if (current > before && motionScale() > 0f) {
            delay(BURST_DELAY_MILLIS)
            burst.snapTo(0f)
            burst.animateTo(1f, tween(durationMillis = BURST_MILLIS, easing = LinearEasing))
            burst.snapTo(0f)
        }
    }
    val targetResults = resultsKey(state.recentResults)
    var shownResults by rememberSaveable { mutableStateOf(targetResults) }
    LaunchedEffect(targetResults) {
        if (shownResults != targetResults) {
            if (motionScale() > 0f) {
                delay(DOTS_SLIDE_DELAY_MILLIS)
            }
            shownResults = targetResults
        }
    }
    return HeaderFigures(
        winPercent = winPercent?.let { shownWinPercent },
        wins = shownWins,
        losses = shownLosses,
        streak = current,
        shownStreak = if (current > 0) shownStreak.coerceAtLeast(1) else 0,
        recent = resultsFrom(shownResults),
        winRateStep = winRateStep,
        recordStep = recordStep,
        streakStep = streakStep,
        burstProgress = { burst.value },
    )
}

@Composable
fun DashboardHeader(
    state: DashboardUiState,
    isCollapsed: Boolean,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading || !state.hasAnyMatches) {
        return
    }
    val figures = rememberHeaderFigures(state)
    val openLabel = stringResource(R.string.dashboard_a11y_open)
    val summary = headerSummary(state)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MIN_TOUCH_TARGET)
                .testTag(DashboardTestTags.HEADER)
                .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpenStats)
                .semantics(mergeDescendants = true) { contentDescription = summary },
    ) {
        AnimatedContent(
            targetState = isCollapsed,
            transitionSpec = {
                val enter = fadeIn(tween(LAYOUT_FADE_IN_MILLIS, LAYOUT_FADE_DELAY_MILLIS, FinalRollEasing))
                val exit = fadeOut(tween(LAYOUT_FADE_OUT_MILLIS, easing = FinalRollEasing))
                (enter togetherWith exit).using(
                    SizeTransform(clip = true) { _, _ -> tween(LAYOUT_SIZE_MILLIS, easing = FinalRollEasing) },
                )
            },
            label = "dashboardHeaderLayout",
        ) { collapsed ->
            if (collapsed) {
                CollapsedStats(state = state, figures = figures)
            } else {
                ExpandedStats(state = state, figures = figures)
            }
        }
    }
}

@Composable
private fun ExpandedStats(
    state: DashboardUiState,
    figures: HeaderFigures,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.hasDisplayName) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.testTag(DashboardTestTags.NAME),
                )
            }
            StreakPill(figures = figures, isCompact = false)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            WinRateFigure(figures = figures, isCompact = false)
            RecordFigure(figures = figures, isCompact = false)
            LastMatchesDots(results = figures.recent)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollapsedStats(
    state: DashboardUiState,
    figures: HeaderFigures,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.hasDisplayName) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(DashboardTestTags.NAME),
                )
            }
            StreakPill(figures = figures, isCompact = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WinRateFigure(figures = figures, isCompact = true)
            RecordFigure(figures = figures, isCompact = true)
        }
    }
}

@Composable
private fun StreakPill(
    figures: HeaderFigures,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = PicklelogTheme.colors
    val burstProgress = figures.burstProgress
    Surface(
        color = colors.headerChip,
        contentColor = colors.onHeaderPill,
        shape = CircleShape,
        modifier =
            modifier
                .testTag(DashboardTestTags.STREAK)
                .streakBurst(progress = burstProgress, color = colors.streakFlame, originX = FLAME_CENTER_X),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (figures.streak > 0) {
                Icon(
                    painter = painterResource(R.drawable.ic_flame),
                    contentDescription = null,
                    tint = colors.streakFlame,
                    modifier =
                        Modifier
                            .size(FLAME_SIZE)
                            .graphicsLayer {
                                val flare = 1f + FLAME_FLARE_GAIN * burstPulse(burstProgress())
                                scaleX = flare
                                scaleY = flare
                                rotationZ = flameWiggle(burstProgress())
                            },
                )
                val text =
                    if (isCompact) {
                        stringResource(R.string.dashboard_streak_compact, figures.shownStreak)
                    } else {
                        pluralStringResource(R.plurals.dashboard_streak_weeks, figures.shownStreak, figures.shownStreak)
                    }
                RollingText(
                    value = text,
                    step = figures.streakStep,
                    style = MaterialTheme.typography.labelLarge,
                    color = LocalContentColor.current,
                )
            } else {
                val text =
                    if (isCompact) {
                        stringResource(R.string.dashboard_no_streak_compact)
                    } else {
                        stringResource(R.string.dashboard_no_streak)
                    }
                Text(text = text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun WinRateFigure(
    figures: HeaderFigures,
    isCompact: Boolean,
) {
    val winPercent = figures.winPercent ?: return
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag(DashboardTestTags.WIN_PERCENT),
    ) {
        RollingText(
            value = stringResource(R.string.dashboard_win_percent_short, winPercent),
            step = figures.winRateStep,
            style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
            color = PicklelogTheme.colors.headerAccent,
        )
        Text(
            text = stringResource(R.string.dashboard_win_rate_label),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun RecordFigure(
    figures: HeaderFigures,
    isCompact: Boolean,
) {
    RollingText(
        value = stringResource(R.string.dashboard_record_compact, figures.wins, figures.losses),
        step = figures.recordStep,
        style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
        color = LocalContentColor.current,
        modifier = Modifier.testTag(DashboardTestTags.RECORD),
    )
}

@Composable
private fun LastMatchesDots(results: List<MatchResult>) {
    if (results.isEmpty()) {
        return
    }
    val color = PicklelogTheme.colors.onHeaderMuted
    val shiftPx = with(LocalDensity.current) { (DOT_SIZE + DOT_SPACING).roundToPx() }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DOT_SPACING)) {
        Text(
            text = stringResource(R.string.dashboard_last_matches, results.size),
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
        AnimatedContent(
            targetState = results,
            transitionSpec = {
                val slideSpec = tween<IntOffset>(durationMillis = DOTS_SLIDE_MILLIS, easing = FinalRollEasing)
                val fadeSpec = tween<Float>(durationMillis = DOTS_SLIDE_MILLIS, easing = FinalRollEasing)
                val enter = slideInHorizontally(animationSpec = slideSpec) { shiftPx } + fadeIn(fadeSpec)
                val exit = slideOutHorizontally(animationSpec = slideSpec) { -shiftPx } + fadeOut(fadeSpec)
                enter togetherWith exit
            },
            label = "lastMatchesDots",
        ) { dots ->
            Row(horizontalArrangement = Arrangement.spacedBy(DOT_SPACING)) {
                dots.forEach { result -> ResultDot(isWin = result == MatchResult.WIN, color = color) }
            }
        }
    }
}

@Composable
private fun ResultDot(
    isWin: Boolean,
    color: Color,
) {
    val base = Modifier.size(DOT_SIZE)
    Box(modifier = if (isWin) base.background(color, CircleShape) else base.border(DOT_RING_WIDTH, color, CircleShape))
}

@Composable
private fun headerSummary(state: DashboardUiState): String {
    val separator = stringResource(R.string.dashboard_a11y_header_separator)
    val overall = state.overallStats.overall
    val percent = overall.winPercent?.let { stringResource(R.string.dashboard_a11y_percent, it) }
    val record = stringResource(R.string.dashboard_a11y_record, overall.wins, overall.losses)
    val current = state.streak.current
    val streak =
        if (current > 0) {
            pluralStringResource(R.plurals.dashboard_a11y_streak, current, current)
        } else {
            stringResource(R.string.dashboard_a11y_no_streak)
        }
    val recent =
        if (state.recentResults.isEmpty()) {
            null
        } else {
            val spoken = state.recentResults.map { resultLabel(it) }.joinToString(separator)
            stringResource(R.string.dashboard_a11y_last_five, state.recentResults.size, spoken)
        }
    return listOfNotNull(
        state.displayName.takeIf { it.isNotBlank() },
        percent,
        record,
        streak,
        recent,
    ).joinToString(separator)
}
