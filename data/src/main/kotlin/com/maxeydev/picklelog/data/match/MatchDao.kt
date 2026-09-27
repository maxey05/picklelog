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
    @Query("$MATCH_LIST_SELECT ORDER BY $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByDateNewest(limit: Int): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$MATCH_LIST_SELECT ORDER BY $OLDEST_FIRST $PAGE_LIMIT")
    fun observeListByDateOldest(limit: Int): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$MATCH_LIST_SELECT ORDER BY $WINS_BEFORE_LOSSES, $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByWinsFirst(limit: Int): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$MATCH_LIST_SELECT ORDER BY $LOSSES_BEFORE_WINS, $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByLossesFirst(limit: Int): Flow<List<MatchListRowEntity>>

    @Transaction
    @Query("$MATCH_LIST_SELECT ORDER BY $NO_OPPONENT_LAST_THEN_A_TO_Z, $NEWEST_FIRST $PAGE_LIMIT")
    fun observeListByOpponent(limit: Int): Flow<List<MatchListRowEntity>>

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

    @Query("SELECT relative_path FROM photo WHERE match_id = :matchId")
    suspend fun photoPathsFor(matchId: String): List<String>

    @Query("DELETE FROM `match` WHERE id = :id")
    suspend fun deleteMatch(id: String)
}
