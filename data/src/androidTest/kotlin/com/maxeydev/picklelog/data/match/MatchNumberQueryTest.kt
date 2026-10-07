@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.data.match

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklelog.data.db.PicklelogDatabase
import com.maxeydev.picklelog.data.photo.PhotoStore
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class MatchNumberQueryTest {
    private lateinit var database: PicklelogDatabase
    private lateinit var photoRoot: File
    private lateinit var matches: RoomMatchRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PicklelogDatabase::class.java).build()
        photoRoot = File(context.cacheDir, "match-number-query-test-${Uuid.random()}").apply { mkdirs() }
        matches = RoomMatchRepository(database, PhotoStore(photoRoot), Dispatchers.IO)
    }

    @After
    fun tearDown() {
        database.close()
        photoRoot.deleteRecursively()
    }

    private fun match(
        date: String,
        createdAtMillis: Long = 1_000,
    ): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.DOUBLES,
            date = AppDate.parse(date),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(createdAtMillis),
            updatedAt = AppInstant.fromEpochMilliseconds(createdAtMillis),
        )

    @Test
    fun `numbers follow date then creation time`() =
        runBlocking {
            val first = match("2026-09-01")
            val secondLoggedFirst = match("2026-09-10", createdAtMillis = 1_000)
            val secondLoggedLater = match("2026-09-10", createdAtMillis = 2_000)
            val last = match("2026-10-01")
            listOf(last, secondLoggedLater, first, secondLoggedFirst).forEach { matches.saveMatch(it) }

            assertEquals(1, matches.observeMatchNumber(first.id).first())
            assertEquals(2, matches.observeMatchNumber(secondLoggedFirst.id).first())
            assertEquals(3, matches.observeMatchNumber(secondLoggedLater.id).first())
            assertEquals(4, matches.observeMatchNumber(last.id).first())
        }

    @Test
    fun `deleting an earlier match renumbers later ones`() =
        runBlocking {
            val earlier = match("2026-09-01")
            val later = match("2026-09-02")
            matches.saveMatch(earlier)
            matches.saveMatch(later)

            matches.deleteMatch(earlier.id)

            assertEquals(1, matches.observeMatchNumber(later.id).first())
        }

    @Test
    fun `an unknown match has no number`() =
        runBlocking {
            assertEquals(0, matches.observeMatchNumber(Uuid.random()).first())
        }
}
