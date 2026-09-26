@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.requireValidRoster
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FakeMatchRepository(
    initial: List<Match> = emptyList(),
) : MatchRepository {
    private val matches = MutableStateFlow(initial.associateBy { it.id })

    val saved = mutableListOf<Match>()
    val deletedIds = mutableListOf<Uuid>()

    val current: List<Match>
        get() = matches.value.values.toList()

    override fun observeAll(): Flow<List<Match>> = matches.map { byId -> byId.values.sortedByDescending { it.date } }

    override fun observeById(id: Uuid): Flow<Match?> = matches.map { byId -> byId[id] }

    override suspend fun saveMatch(match: Match) {
        match.requireValidRoster()
        saved += match
        matches.update { byId -> byId + (match.id to match) }
    }

    override suspend fun deleteMatch(id: Uuid) {
        deletedIds += id
        matches.update { byId -> byId - id }
    }
}
