@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.match.edit

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.domain.match.GameScore
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.person.Person
import com.maxeydev.picklelog.domain.person.normalizePersonName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data class MatchDraft(
    val matchId: String,
    val isNew: Boolean,
    val format: MatchFormat,
    val date: String,
    val result: MatchResult? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val opponentNames: List<String> = listOf("", ""),
    val opponentIds: List<String?> = listOf(null, null),
    val partnerName: String = "",
    val partnerId: String? = null,
    val games: List<GameDraft> = emptyList(),
    val location: String = "",
    val paddle: String = "",
    val notes: String = "",
    val clearedOnFormatSwitch: Boolean = false,
) {
    fun withFormat(newFormat: MatchFormat): MatchDraft {
        if (newFormat == format) {
            return this
        }
        return when (newFormat) {
            MatchFormat.SINGLES -> {
                val clearsSomething = nameIn(PersonSlot.OPPONENT_2).isNotBlank() || partnerName.isNotBlank()
                copy(
                    format = MatchFormat.SINGLES,
                    opponentNames = listOf(nameIn(PersonSlot.OPPONENT_1), ""),
                    opponentIds = listOf(idIn(PersonSlot.OPPONENT_1), null),
                    partnerName = "",
                    partnerId = null,
                    clearedOnFormatSwitch = clearsSomething,
                )
            }
            MatchFormat.DOUBLES -> copy(format = MatchFormat.DOUBLES, clearedOnFormatSwitch = false)
        }
    }

    fun withPersonName(
        slot: PersonSlot,
        name: String,
    ): MatchDraft {
        if (name == nameIn(slot)) {
            return this
        }
        return withPerson(slot, name, id = null)
    }

    fun withPersonSelected(
        slot: PersonSlot,
        person: Person,
    ): MatchDraft = withPerson(slot, person.displayName, person.id.toString())

    private fun withPerson(
        slot: PersonSlot,
        name: String,
        id: String?,
    ): MatchDraft =
        when (slot) {
            PersonSlot.OPPONENT_1 ->
                copy(
                    opponentNames = listOf(name, nameIn(PersonSlot.OPPONENT_2)),
                    opponentIds = listOf(id, idIn(PersonSlot.OPPONENT_2)),
                )
            PersonSlot.OPPONENT_2 ->
                copy(
                    opponentNames = listOf(nameIn(PersonSlot.OPPONENT_1), name),
                    opponentIds = listOf(idIn(PersonSlot.OPPONENT_1), id),
                )
            PersonSlot.PARTNER -> copy(partnerName = name, partnerId = id)
        }

    fun withGameAdded(): MatchDraft = copy(games = games + GameDraft())

    fun withGameRemoved(index: Int): MatchDraft = copy(games = games.filterIndexed { position, _ -> position != index })

    fun withGameScores(
        index: Int,
        myScore: String,
        opponentScore: String,
    ): MatchDraft =
        copy(
            games =
                games.mapIndexed { position, game ->
                    if (position == index) {
                        GameDraft.of(myScore, opponentScore)
                    } else {
                        game
                    }
                },
        )

    fun nameIn(slot: PersonSlot): String =
        when (slot) {
            PersonSlot.OPPONENT_1 -> opponentNames.getOrElse(0) { "" }
            PersonSlot.OPPONENT_2 -> opponentNames.getOrElse(1) { "" }
            PersonSlot.PARTNER -> partnerName
        }

    fun idIn(slot: PersonSlot): String? =
        when (slot) {
            PersonSlot.OPPONENT_1 -> opponentIds.getOrNull(0)
            PersonSlot.OPPONENT_2 -> opponentIds.getOrNull(1)
            PersonSlot.PARTNER -> partnerId
        }

    fun boundIdsOutside(slot: PersonSlot): Set<String> =
        visibleSlots()
            .filter { it != slot }
            .mapNotNull { idIn(it) }
            .toSet()

    fun visibleSlots(): List<PersonSlot> =
        when (format) {
            MatchFormat.SINGLES -> listOf(PersonSlot.OPPONENT_1)
            MatchFormat.DOUBLES -> listOf(PersonSlot.OPPONENT_1, PersonSlot.OPPONENT_2, PersonSlot.PARTNER)
        }

    fun duplicateSlots(): Map<PersonSlot, PersonSlot> {
        val firstSlotByName = mutableMapOf<String, PersonSlot>()
        val duplicates = mutableMapOf<PersonSlot, PersonSlot>()
        visibleSlots().forEach { slot ->
            val normalized = normalizePersonName(nameIn(slot))
            if (normalized.isNotEmpty()) {
                val earlier = firstSlotByName[normalized]
                if (earlier == null) {
                    firstSlotByName[normalized] = slot
                } else {
                    duplicates[slot] = earlier
                }
            }
        }
        return duplicates
    }

    fun completeGames(): List<GameScore> =
        games
            .filter { it.isComplete }
            .mapIndexed { index, game ->
                GameScore(
                    gameNumber = index + 1,
                    myScore = game.myScore.toInt(),
                    opponentScore = game.opponentScore.toInt(),
                )
            }

    fun hasIncompleteGame(): Boolean = games.any { it.isIncomplete }

    fun canSave(): Boolean = result != null && duplicateSlots().isEmpty() && !hasIncompleteGame()

    companion object {
        fun forNewMatch(
            matchId: String,
            format: MatchFormat,
            date: AppDate,
            startTime: AppTime,
        ): MatchDraft =
            MatchDraft(
                matchId = matchId,
                isNew = true,
                format = format,
                date = date.toString(),
                startTime = startTime.toString(),
            )

        fun fromMatch(match: Match): MatchDraft =
            MatchDraft(
                matchId = match.id.toString(),
                isNew = false,
                format = match.format,
                date = match.date.toString(),
                result = match.result,
                startTime = match.startTime?.toString(),
                endTime = match.endTime?.toString(),
                opponentNames =
                    listOf(
                        match.opponents.getOrNull(0)?.displayName.orEmpty(),
                        match.opponents.getOrNull(1)?.displayName.orEmpty(),
                    ),
                opponentIds =
                    listOf(
                        match.opponents.getOrNull(0)?.id?.toString(),
                        match.opponents.getOrNull(1)?.id?.toString(),
                    ),
                partnerName = match.partner?.displayName.orEmpty(),
                partnerId = match.partner?.id?.toString(),
                games =
                    match.games
                        .sortedBy { it.gameNumber }
                        .map {
                            GameDraft(
                                myScore = it.myScore.toString(),
                                opponentScore = it.opponentScore.toString(),
                            )
                        },
                location = match.location.orEmpty(),
                paddle = match.paddle.orEmpty(),
                notes = match.notes.orEmpty(),
            )
    }
}
