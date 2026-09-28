@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.data.match

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklelog.data.db.PicklelogDatabase
import com.maxeydev.picklelog.data.person.RoomPersonRepository
import com.maxeydev.picklelog.data.photo.PhotoStore
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.match.MatchSort
import com.maxeydev.picklelog.domain.stats.BasicStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class DashboardStatsQueryTest {
    private lateinit var database: PicklelogDatabase
    private lateinit var photoRoot: File
    private lateinit var matches: RoomMatchRepository
    private lateinit var people: RoomPersonRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PicklelogDatabase::class.java).build()
        photoRoot = File(context.cacheDir, "dashboard-stats-test-${Uuid.random()}").apply { mkdirs() }
        matches = RoomMatchRepository(database, PhotoStore(photoRoot), Dispatchers.IO)
        people = RoomPersonRepository(database, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        database.close()
        photoRoot.deleteRecursively()
    }

    @Test
    fun stat_lines_and_the_match_list_agree_for_every_filter() =
        runBlocking {
            val dave = people.findOrCreatePerson("Dave")
            val maria = people.findOrCreatePerson("María")
            val random = Random(8)
            val start = AppDate.parse("2026-01-01")
            repeat(120) { index ->
                val format = if (random.nextBoolean()) MatchFormat.SINGLES else MatchFormat.DOUBLES
                val opponent = if (random.nextBoolean()) dave else maria
                matches.saveMatch(
                    Match(
                        id = Uuid.random(),
                        format = format,
                        date = start.plus(random.nextInt(0, 200), DateTimeUnit.DAY),
                        result = if (random.nextInt(3) == 0) MatchResult.LOSS else MatchResult.WIN,
                        createdAt = AppInstant.fromEpochMilliseconds(index.toLong()),
                        updatedAt = AppInstant.fromEpochMilliseconds(index.toLong()),
                        location = if (random.nextBoolean()) "Court A" else "Court B",
                        opponents = listOf(opponent),
                    ),
                )
            }
            val filters =
                listOf(
                    FilterState.NONE,
                    FilterState(format = MatchFormat.SINGLES),
                    FilterState(result = MatchResult.LOSS),
                    FilterState(opponentId = dave.id),
                    FilterState(location = "Court B", format = MatchFormat.DOUBLES),
                    FilterState(fromDate = AppDate.parse("2026-03-01"), toDate = AppDate.parse("2026-05-31")),
                    FilterState(opponentId = maria.id, result = MatchResult.WIN, location = "Court A"),
                )

            filters.forEach { filter ->
                val listed = matches.observeListPage(MatchSort.DATE_NEWEST, 10_000, filter).first()
                val stats = BasicStats.from(matches.observeStatLines(filter).first())

                assertEquals("total for $filter", listed.size, stats.totalMatches)
                assertEquals(
                    "wins for $filter",
                    listed.count { it.result == MatchResult.WIN },
                    stats.overall.wins,
                )
                assertEquals(
                    "singles for $filter",
                    listed.count { it.format == MatchFormat.SINGLES },
                    stats.singles.total,
                )
            }
        }

    @Test
    fun stat_lines_update_when_a_match_is_deleted() =
        runBlocking {
            val kept = saveSimple(MatchResult.WIN)
            val removed = saveSimple(MatchResult.LOSS)
            assertEquals(2, matches.observeStatLines().first().size)

            matches.deleteMatch(removed.id)

            val remaining = BasicStats.from(matches.observeStatLines().first())
            assertEquals(1, remaining.totalMatches)
            assertEquals(1, remaining.overall.wins)
            assertEquals(kept.format, MatchFormat.SINGLES)
        }

    private suspend fun saveSimple(result: MatchResult): Match {
        val match =
            Match(
                id = Uuid.random(),
                format = MatchFormat.SINGLES,
                date = AppDate.parse("2026-02-02"),
                result = result,
                createdAt = AppInstant.fromEpochMilliseconds(1),
                updatedAt = AppInstant.fromEpochMilliseconds(1),
            )
        matches.saveMatch(match)
        return match
    }
}
