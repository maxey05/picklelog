package com.maxeydev.picklelog.ui.dashboard

import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.stats.BasicStats
import com.maxeydev.picklelog.domain.streak.StreakResult

data class DashboardUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val stats: BasicStats = BasicStats.EMPTY,
    val streak: StreakResult = StreakResult.NONE,
    val hasAnyMatches: Boolean = false,
    val filter: FilterState = FilterState.NONE,
    val filteredOpponentName: String? = null,
    val isPro: Boolean = false,
) {
    val isFiltered: Boolean
        get() = filter.isActive

    val hasDisplayName: Boolean
        get() = displayName.isNotBlank()

    val hasNoFilteredMatches: Boolean
        get() = hasAnyMatches && stats.totalMatches == 0
}
