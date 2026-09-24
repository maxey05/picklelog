package com.maxeydev.picklelog.data.match

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.maxeydev.picklelog.data.photo.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Transaction
    @Query("SELECT * FROM `match` ORDER BY `date` DESC, created_at DESC")
    fun observeAll(): Flow<List<MatchWithRelationsEntity>>

    @Transaction
    @Query("SELECT * FROM `match` WHERE id = :id")
    fun observeById(id: String): Flow<MatchWithRelationsEntity?>

    @Upsert
    suspend fun upsertMatch(match: MatchEntity)

    @Insert
    suspend fun insertPeople(rows: List<MatchPersonEntity>)

    @Insert
    suspend fun insertGames(rows: List<GameScoreEntity>)

    @Insert
    suspend fun insertPhotos(rows: List<PhotoEntity>)

    @Query("DELETE FROM match_person WHERE match_id = :matchId")
    suspend fun deletePeopleFor(matchId: String)

    @Query("DELETE FROM game_score WHERE match_id = :matchId")
    suspend fun deleteGamesFor(matchId: String)

    @Query("DELETE FROM photo WHERE match_id = :matchId")
    suspend fun deletePhotosFor(matchId: String)

    @Query("DELETE FROM `match` WHERE id = :id")
    suspend fun deleteMatch(id: String)
}
