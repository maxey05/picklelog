package com.maxeydev.picklelog.ui.match.edit

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.match.crossesMidnight
import com.maxeydev.picklelog.domain.match.deriveDuration
import com.maxeydev.picklelog.domain.match.resultAdvisory
import com.maxeydev.picklelog.ui.common.SuggestionUiState
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
    val suggestionTarget: SuggestionTarget? = null,
    val suggestions: List<SuggestionUiState> = emptyList(),
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val isFinished: Boolean = false,
    val savedNewMatchId: String? = null,
    val photos: List<PhotoUiState> = emptyList(),
    val hasPhotoError: Boolean = false,
) {
    fun suggestionsFor(target: SuggestionTarget): List<SuggestionUiState> =
        if (target == suggestionTarget) {
            suggestions
        } else {
            emptyList()
        }
}

fun MatchDraft.toUiState(
    isSaving: Boolean,
    isFinished: Boolean,
    suggestionTarget: SuggestionTarget? = null,
    suggestions: List<SuggestionUiState> = emptyList(),
    savedNewMatchId: String? = null,
    photoFilePath: (String) -> String = { it },
    hasPhotoError: Boolean = false,
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
        suggestionTarget = suggestionTarget,
        suggestions = suggestions,
        canSave = canSave() && !isSaving && !isFinished,
        isSaving = isSaving,
        isFinished = isFinished,
        savedNewMatchId = savedNewMatchId,
        photos = photos.map { PhotoUiState(key = it.key, filePath = it.relativePath?.let(photoFilePath)) },
        hasPhotoError = hasPhotoError,
    )
}
