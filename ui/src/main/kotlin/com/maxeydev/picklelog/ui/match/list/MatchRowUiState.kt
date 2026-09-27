package com.maxeydev.picklelog.ui.match.list

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.GameScore
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult

data class MatchRowUiState(
    val id: String,
    val date: AppDate,
    val format: MatchFormat,
    val result: MatchResult,
    val opponentNames: List<String>,
    val games: List<GameScore>,
    val thumbnailPath: String?,
)
