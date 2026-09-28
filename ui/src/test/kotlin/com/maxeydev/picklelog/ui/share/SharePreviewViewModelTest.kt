@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklelog.ui.share

import androidx.lifecycle.SavedStateHandle
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.streak.StreakEngine
import com.maxeydev.picklelog.ui.fakes.FakeCardRenderer
import com.maxeydev.picklelog.ui.fakes.FakeMatchRepository
import com.maxeydev.picklelog.ui.fakes.FakeProfileRepository
import com.maxeydev.picklelog.ui.fakes.FixedClock
import com.maxeydev.picklelog.ui.navigation.MATCH_ID_ARGUMENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SharePreviewViewModelTest {
    private val renderer = FakeCardRenderer()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun match(date: String): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.SINGLES,
            date = AppDate.parse(date),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(0),
            updatedAt = AppInstant.fromEpochMilliseconds(0),
        )

    private fun viewModel(
        id: Uuid,
        matches: FakeMatchRepository,
    ): SharePreviewViewModel =
        SharePreviewViewModel(
            savedStateHandle = SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to id.toString())),
            matchRepository = matches,
            profileRepository = FakeProfileRepository(displayName = "Matty"),
            streakEngine = StreakEngine(FixedClock(AppInstant.parse("2026-03-04T12:00:00Z"))) { TimeZone.UTC },
            photoFile = { File("/nonexistent", it) },
            renderer = renderer,
            labels = FakeCardLabels(),
            ioDispatcher = Dispatchers.Main,
        )

    @Test
    fun `the card carries the whole history streak even for an older match`() {
        val shared = match("2026-02-17")
        val matches = FakeMatchRepository(listOf(shared, match("2026-02-24"), match("2026-03-02")))

        viewModel(shared.id, matches)

        val data = renderer.rendered.single()
        assertEquals("3-week streak", data.streak)
        assertEquals("Matty", data.displayName)
    }

    @Test
    fun `a failed render offers a retry instead of a broken card`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        val failed = viewModel.uiState.value
        assertTrue(failed.hasFailed)
        assertFalse(failed.isRendering)

        viewModel.retry()

        assertEquals(2, renderer.rendered.size)
    }

    @Test
    fun `a match that no longer exists closes the share screen`() {
        val state = viewModel(Uuid.random(), FakeMatchRepository()).uiState.value

        assertTrue(state.isGone)
        assertTrue(renderer.rendered.isEmpty())
    }

    @Test
    fun `a sharing failure is reported without losing the card state`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        viewModel.reportShareFailed()

        assertTrue(viewModel.uiState.value.hasShareFailed)
        assertTrue(viewModel.uiState.value.hasFailed)
    }
}
