@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.domain.match

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface MatchRepository {
    fun observeListPage(
        sort: MatchSort,
        limit: Int,
        filter: FilterState = FilterState.NONE,
        search: SearchTerm? = null,
    ): Flow<List<MatchListItem>>

    fun observeById(id: Uuid): Flow<Match?>

    fun observePriorValues(field: FreeTextField): Flow<List<FreeTextUsage>>

    suspend fun saveMatch(match: Match)

    suspend fun deleteMatch(id: Uuid)
}
