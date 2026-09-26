package com.maxeydev.picklelog.ui.match.edit

data class GameScoreRowUiState(
    val gameNumber: Int,
    val myScore: String,
    val opponentScore: String,
    val isIncomplete: Boolean,
)
