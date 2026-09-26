@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.data.match

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklelog.domain.match.MatchFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
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
class DataStoreLastUsedFormatStoreTest {
    private lateinit var directory: File
    private lateinit var preferencesFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "last-used-format-test-${Uuid.random()}").apply { mkdirs() }
        preferencesFile = File(directory, "match-preferences.preferences_pb")
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun openDataStore(job: Job): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { preferencesFile },
        )

    @Test
    fun `the_first_ever_launch_defaults_to_doubles`() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreLastUsedFormatStore(openDataStore(job))

            assertEquals(MatchFormat.DOUBLES, store.lastUsedFormat())
            job.cancelAndJoin()
        }

    @Test
    fun `the_last_used_format_survives_an_app_restart`() =
        runBlocking {
            val firstLaunch = SupervisorJob()
            DataStoreLastUsedFormatStore(openDataStore(firstLaunch)).recordLastUsedFormat(MatchFormat.SINGLES)
            firstLaunch.cancelAndJoin()

            val secondLaunch = SupervisorJob()
            val reopened = DataStoreLastUsedFormatStore(openDataStore(secondLaunch))

            assertEquals(MatchFormat.SINGLES, reopened.lastUsedFormat())
            secondLaunch.cancelAndJoin()
        }

    @Test
    fun `recording_a_new_format_replaces_the_previous_one`() =
        runBlocking {
            val job = SupervisorJob()
            val store = DataStoreLastUsedFormatStore(openDataStore(job))

            store.recordLastUsedFormat(MatchFormat.SINGLES)
            store.recordLastUsedFormat(MatchFormat.DOUBLES)

            assertEquals(MatchFormat.DOUBLES, store.lastUsedFormat())
            job.cancelAndJoin()
        }
}
