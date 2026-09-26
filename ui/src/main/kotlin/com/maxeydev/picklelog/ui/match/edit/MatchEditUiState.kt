package com.maxeydev.picklelog.ui.match.edit

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.match.crossesMidnight
import com.maxeydev.picklelog.domain.match.deriveDuration
import com.maxeydev.picklelog.domain.match.resultAdvisory
import kotlin.time.Duration

data class MatchEditUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val format: MatchFormat = LastUsedFormatStore.FIRST_LAUNCH_FORMAT,
    val date: AppDate? = null,
    val result: MatchResult? = null,
    val advisory: MatchResult? = null,
    val startTime: AppTime? = null,
    val endTime: AppTime? = null,
    val duration: Duration? = null,
    val endsNextDay: Boolean = false,
    val personSlots: List<PersonSlotUiState> = emptyList(),
    val games: List<GameScoreRowUiState> = emptyList(),
    val location: String = "",
    val paddle: String = "",
    val notes: String = "",
    val clearedOnFormatSwitch: Boolean = false,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val isFinished: Boolean = false,
)

fun MatchDraft.toUiState(
    isSaving: Boolean,
    isFinished: Boolean,
): MatchEditUiState {
    val start = startTime?.let(AppTime::parse)
    val end = endTime?.let(AppTime::parse)
    val duplicates = duplicateSlots()
    return MatchEditUiState(
        isLoading = false,
        isEditing = !isNew,
        format = format,
        date = AppDate.parse(date),
        result = result,
        advisory = result?.let { tapped -> resultAdvisory(tapped, completeGames()) },
        startTime = start,
        endTime = end,
        duration = deriveDuration(start, end),
        endsNextDay = crossesMidnight(start, end),
        personSlots = visibleSlots().map { slot -> PersonSlotUiState(slot, nameIn(slot), duplicates[slot]) },
        games =
            games.mapIndexed { index, game ->
                GameScoreRowUiState(
                    gameNumber = index + 1,
                    myScore = game.myScore,
                    opponentScore = game.opponentScore,
                    isIncomplete = game.isIncomplete,
                )
            },
        location = location,
        paddle = paddle,
        notes = notes,
        clearedOnFormatSwitch = clearedOnFormatSwitch,
        canSave = canSave() && !isSaving && !isFinished,
        isSaving = isSaving,
        isFinished = isFinished,
    )
}
