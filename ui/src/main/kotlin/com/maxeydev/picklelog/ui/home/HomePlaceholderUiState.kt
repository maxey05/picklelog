package com.maxeydev.picklelog.ui.home

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult

data class HomePlaceholderUiState(
    val isLoading: Boolean = true,
    val matches: List<MatchSummary> = emptyList(),
)

data class MatchSummary(
    val id: String,
    val date: AppDate,
    val format: MatchFormat,
    val result: MatchResult,
)
