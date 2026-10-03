package com.maxeydev.picklelog.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
class PicklelogColors(
    val header: Color,
    val onHeader: Color,
    val onHeaderMuted: Color,
    val headerAccent: Color,
    val headerPill: Color,
    val onHeaderPill: Color,
    val streakFlame: Color,
    val winRow: Color,
    val winBadge: Color,
    val onWinBadge: Color,
    val lossBadge: Color,
    val onLossBadge: Color,
    val cardBorder: Color,
    val countBadge: Color,
    val onCountBadge: Color,
)

internal val LIGHT_PICKLELOG_COLORS =
    PicklelogColors(
        header = Color(0xFF007A43),
        onHeader = Color(0xFFFFFFFF),
        onHeaderMuted = Color(0xFFDEF9E9),
        headerAccent = Color(0xFFA5E8C1),
        headerPill = Color(0xFF1F8557),
        onHeaderPill = Color(0xFFFFFFFF),
        streakFlame = Color(0xFFFFB84D),
        winRow = Color(0xFFE7F7ED),
        winBadge = Color(0xFF007A43),
        onWinBadge = Color(0xFFFFFFFF),
        lossBadge = Color(0xFFDCE1DD),
        onLossBadge = Color(0xFF4D5651),
        cardBorder = Color(0xFFDDE7E1),
        countBadge = Color(0xFFC7F4D5),
        onCountBadge = Color(0xFF0B3A24),
    )

internal val DARK_PICKLELOG_COLORS =
    PicklelogColors(
        header = Color(0xFF0B4A2C),
        onHeader = Color(0xFFFFFFFF),
        onHeaderMuted = Color(0xFFCDEEDB),
        headerAccent = Color(0xFFA5E8C1),
        headerPill = Color(0xFF1E6B47),
        onHeaderPill = Color(0xFFFFFFFF),
        streakFlame = Color(0xFFFFB84D),
        winRow = Color(0xFF15301F),
        winBadge = Color(0xFF1F7A4D),
        onWinBadge = Color(0xFFFFFFFF),
        lossBadge = Color(0xFF34433A),
        onLossBadge = Color(0xFFC9D6CE),
        cardBorder = Color(0xFF34433A),
        countBadge = Color(0xFFB4F2CB),
        onCountBadge = Color(0xFF00391F),
    )

internal val LocalPicklelogColors = staticCompositionLocalOf { LIGHT_PICKLELOG_COLORS }

object PicklelogTheme {
    val colors: PicklelogColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPicklelogColors.current
}
