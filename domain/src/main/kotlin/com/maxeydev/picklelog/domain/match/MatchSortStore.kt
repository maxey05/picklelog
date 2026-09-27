package com.maxeydev.picklelog.domain.match

import kotlinx.coroutines.flow.Flow

interface MatchSortStore {
    fun observeSort(): Flow<MatchSort>

    suspend fun saveSort(sort: MatchSort)
}
