@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.data.match

import androidx.room.withTransaction
import com.maxeydev.picklelog.data.db.PicklelogDatabase
import com.maxeydev.picklelog.data.photo.PhotoFileStore
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.requireValidRoster
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RoomMatchRepository(
    private val database: PicklelogDatabase,
    private val photoFileStore: PhotoFileStore,
    private val ioDispatcher: CoroutineDispatcher,
) : MatchRepository {
    private val matchDao = database.matchDao()

    override fun observeAll(): Flow<List<Match>> =
        matchDao
            .observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeById(id: Uuid): Flow<Match?> =
        matchDao
            .observeById(id.toString())
            .map { row -> row?.toDomain() }
            .flowOn(ioDispatcher)

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
