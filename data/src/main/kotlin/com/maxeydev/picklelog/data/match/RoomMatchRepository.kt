@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.data.match

import androidx.room.withTransaction
import com.maxeydev.picklelog.data.db.PicklelogDatabase
import com.maxeydev.picklelog.data.photo.PhotoFileStore
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.FreeTextField
import com.maxeydev.picklelog.domain.match.FreeTextUsage
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchListItem
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchSort
import com.maxeydev.picklelog.domain.match.SearchTerm
import com.maxeydev.picklelog.domain.match.requireValidRoster
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private typealias FilteredListQuery = (
    format: String?,
    result: String?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    opponentId: String?,
    location: String?,
    textPattern: String?,
    namePattern: String?,
    limit: Int,
) -> Flow<List<MatchListRowEntity>>

class RoomMatchRepository(
    private val database: PicklelogDatabase,
    private val photoFileStore: PhotoFileStore,
    private val ioDispatcher: CoroutineDispatcher,
) : MatchRepository {
    private val matchDao = database.matchDao()

    override fun observeListPage(
        sort: MatchSort,
        limit: Int,
        filter: FilterState,
        search: SearchTerm?,
    ): Flow<List<MatchListItem>> {
        require(limit > 0) { "A list page needs a positive limit, but was $limit." }
        val query = listQueryFor(sort)
        val rows =
            query(
                filter.format?.name,
                filter.result?.name,
                filter.fromDate,
                filter.toDate,
                filter.opponentId?.toString(),
                filter.location,
                search?.textPattern,
                search?.namePattern,
                limit,
            )
        return rows
            .map { page -> page.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    private fun listQueryFor(sort: MatchSort): FilteredListQuery =
        when (sort) {
            MatchSort.DATE_NEWEST -> matchDao::observeListByDateNewest
            MatchSort.DATE_OLDEST -> matchDao::observeListByDateOldest
            MatchSort.RESULT_WINS_FIRST -> matchDao::observeListByWinsFirst
            MatchSort.RESULT_LOSSES_FIRST -> matchDao::observeListByLossesFirst
            MatchSort.OPPONENT_A_TO_Z -> matchDao::observeListByOpponent
            MatchSort.LOCATION_A_TO_Z -> matchDao::observeListByLocation
            MatchSort.DURATION_SHORTEST -> matchDao::observeListByDurationShortest
            MatchSort.DURATION_LONGEST -> matchDao::observeListByDurationLongest
        }

    override fun observeById(id: Uuid): Flow<Match?> =
        matchDao
            .observeById(id.toString())
            .map { row -> row?.toDomain() }
            .flowOn(ioDispatcher)

    override fun observePriorValues(field: FreeTextField): Flow<List<FreeTextUsage>> {
        val rows =
            when (field) {
                FreeTextField.LOCATION -> matchDao.observePriorLocations()
                FreeTextField.PADDLE -> matchDao.observePriorPaddles()
            }
        return rows
            .map { values -> values.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveMatch(match: Match) {
        match.requireValidRoster()
        val matchId = match.id.toString()
        withContext(ioDispatcher) {
            database.withTransaction {
                matchDao.upsertMatch(match.toEntity())
                matchDao.deletePeopleFor(matchId)
                matchDao.deleteGamesFor(matchId)
                matchDao.deletePhotosFor(matchId)
                matchDao.insertPeople(match.toMatchPersonRows())
                matchDao.insertGames(match.toGameScoreRows())
                matchDao.insertPhotos(match.toPhotoRows())
            }
        }
    }

    override suspend fun deleteMatch(id: Uuid) {
        val matchId = id.toString()
        withContext(ioDispatcher) {
            val photoPaths =
                database.withTransaction {
                    val paths = matchDao.photoPathsFor(matchId)
                    matchDao.deleteMatch(matchId)
                    paths
                }
            photoFileStore.deletePhotoFiles(photoPaths)
        }
    }
}
