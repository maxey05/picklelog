@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.domain.match

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface MatchRepository {
    fun observeListPage(
        sort: MatchSort,
        limit: Int,
    ): Flow<List<MatchListItem>>

    fun observeById(id: Uuid): Flow<Match?>

    suspend fun saveMatch(match: Match)

    suspend fun deleteMatch(id: Uuid)
}
