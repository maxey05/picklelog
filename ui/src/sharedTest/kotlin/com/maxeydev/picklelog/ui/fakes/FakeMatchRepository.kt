@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.domain.match.FreeTextField
import com.maxeydev.picklelog.domain.match.FreeTextUsage
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchListItem
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.match.MatchSort
import com.maxeydev.picklelog.domain.match.requireValidRoster
import com.maxeydev.picklelog.domain.person.normalizePersonName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FakeMatchRepository(
    initial: List<Match> = emptyList(),
) : MatchRepository {
    private val matches = MutableStateFlow(initial.associateBy { it.id })

    val saved = mutableListOf<Match>()
    val deletedIds = mutableListOf<Uuid>()
    val requestedPages: MutableList<Pair<MatchSort, Int>> = CopyOnWriteArrayList()

    val current: List<Match>
        get() = matches.value.values.toList()

    override fun observeListPage(
        sort: MatchSort,
        limit: Int,
    ): Flow<List<MatchListItem>> {
        require(limit > 0) { "A list page needs a positive limit, but was $limit." }
        requestedPages += sort to limit
        return matches.map { byId ->
            byId.values
                .sortedWith(orderFor(sort))
                .take(limit)
                .map { it.toListItem() }
        }
    }

    override fun observeById(id: Uuid): Flow<Match?> = matches.map { byId -> byId[id] }

    fun observeAllMatches(): Flow<List<Match>> = matches.map { byId -> byId.values.toList() }

    override fun observePriorValues(field: FreeTextField): Flow<List<FreeTextUsage>> =
        matches.map { byId ->
            byId.values
                .mapNotNull { match ->
                    val value =
                        when (field) {
                            FreeTextField.LOCATION -> match.location
                            FreeTextField.PADDLE -> match.paddle
                        }
                    value?.takeIf { it.isNotEmpty() }?.let { it to match }
                }.groupBy({ it.first }, { it.second })
                .map { (value, used) ->
                    FreeTextUsage(
                        value = value,
                        lastPlayedOn = used.maxOf { it.date },
                        lastLoggedAt = used.maxOf { it.createdAt },
                    )
                }
        }

    override suspend fun saveMatch(match: Match) {
        match.requireValidRoster()
        saved += match
        matches.update { byId -> byId + (match.id to match) }
    }

    override suspend fun deleteMatch(id: Uuid) {
        deletedIds += id
        matches.update { byId -> byId - id }
    }

    private fun orderFor(sort: MatchSort): Comparator<Match> {
        val newestFirst =
            compareByDescending<Match> { it.date }
                .thenByDescending { it.createdAt }
                .thenBy { it.id.toString() }
        return when (sort) {
            MatchSort.DATE_NEWEST -> newestFirst
            MatchSort.DATE_OLDEST ->
                compareBy<Match> { it.date }
                    .thenBy { it.createdAt }
                    .thenBy { it.id.toString() }
            MatchSort.RESULT_WINS_FIRST -> compareBy<Match> { it.result != MatchResult.WIN }.then(newestFirst)
            MatchSort.RESULT_LOSSES_FIRST -> compareBy<Match> { it.result != MatchResult.LOSS }.then(newestFirst)
            MatchSort.OPPONENT_A_TO_Z ->
                compareBy<Match> { it.opponents.isEmpty() }
                    .thenBy { match -> match.opponents.firstOrNull()?.let { normalizePersonName(it.displayName) } }
                    .then(newestFirst)
        }
    }

    private fun Match.toListItem(): MatchListItem =
        MatchListItem(
            id = id,
            date = date,
            format = format,
            result = result,
            opponentNames = opponents.map { it.displayName },
            games = games.sortedBy { it.gameNumber },
            primaryPhotoPath = photos.minByOrNull { it.sortIndex }?.relativePath,
        )
}
