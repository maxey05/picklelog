package com.maxeydev.picklelog.ui.match.detail

import com.maxeydev.picklelog.domain.match.Match
import kotlin.time.Duration

data class MatchDetailUiState(
    val isLoading: Boolean = true,
    val match: Match? = null,
    val duration: Duration? = null,
    val endsNextDay: Boolean = false,
    val isConfirmingDelete: Boolean = false,
    val isGone: Boolean = false,
)
