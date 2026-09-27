@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklelog.ui.match.list

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.match.MatchSort
import com.maxeydev.picklelog.domain.photo.PhotoRef
import com.maxeydev.picklelog.ui.fakes.FakeMatchRepository
import com.maxeydev.picklelog.ui.fakes.FakeMatchSortStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MatchListViewModelTest {
    private val photoRoot = File("/data/user/0/picklelog/files")
    private lateinit var matches: FakeMatchRepository
    private lateinit var sorts: FakeMatchSortStore

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        sorts = FakeMatchSortStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun match(
        day: Int,
        result: MatchResult = MatchResult.WIN,
        photos: List<PhotoRef> = emptyList(),
    ): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.DOUBLES,
            date = AppDate.parse("2026-01-01").plus(day, DateTimeUnit.DAY),
            result = result,
            createdAt = AppInstant.fromEpochMilliseconds(1_000),
            updatedAt = AppInstant.fromEpochMilliseconds(1_000),
            photos = photos,
        )

    private fun TestScope.subscribedViewModel(stored: List<Match>): MatchListViewModel {
        matches = FakeMatchRepository(stored)
        val viewModel =
            MatchListViewModel(
                matchRepository = matches,
                matchSortStore = sorts,
                photoFile = { relativePath -> File(photoRoot, relativePath) },
                defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun `the list opens newest first and asks the repository for exactly one page`() =
        runTest {
            val older = match(day = 1)
            val newer = match(day = 2)

            val state = subscribedViewModel(listOf(older, newer)).uiState.value

            assertEquals(MatchSort.DATE_NEWEST, state.sort)
            assertEquals(listOf(newer.id.toString(), older.id.toString()), state.matches.map { it.id })
            assertEquals(listOf(MatchSort.DATE_NEWEST to MATCH_LIST_PAGE_SIZE), matches.requestedPages)
            assertFalse(state.isLoading)
        }

    @Test
    fun `a full first page holds exactly one page of rows even when far more exist`() =
        runTest {
            val state = subscribedViewModel(List(1_000) { match(day = it) }).uiState.value

            assertEquals(MATCH_LIST_PAGE_SIZE, state.matches.size)
            assertTrue(state.canLoadMore)
        }

    @Test
    fun `scrolling near the end of a full page asks for the next page`() =
        runTest {
            val viewModel = subscribedViewModel(List(120) { match(day = it) })

            viewModel.loadMoreIfNeeded(MATCH_LIST_PAGE_SIZE - MATCH_LIST_PREFETCH_DISTANCE)

            assertEquals(2 * MATCH_LIST_PAGE_SIZE, matches.requestedPages.last().second)
            assertEquals(2 * MATCH_LIST_PAGE_SIZE, viewModel.uiState.value.matches.size)
        }

    @Test
    fun `scrolling well above the end loads nothing more`() =
        runTest {
            val viewModel = subscribedViewModel(List(120) { match(day = it) })

            viewModel.loadMoreIfNeeded(10)

            assertEquals(1, matches.requestedPages.size)
        }

    @Test
    fun `the same scroll position never asks for another page twice`() =
        runTest {
            val viewModel = subscribedViewModel(List(300) { match(day = it) })
            val nearEndOfFirstPage = MATCH_LIST_PAGE_SIZE - 1

            viewModel.loadMoreIfNeeded(nearEndOfFirstPage)
            viewModel.loadMoreIfNeeded(nearEndOfFirstPage)

            val requestedLimits = matches.requestedPages.map { it.second }
            assertEquals(listOf(MATCH_LIST_PAGE_SIZE, 2 * MATCH_LIST_PAGE_SIZE), requestedLimits)
        }

    @Test
    fun `reaching the end of a short list asks for nothing more`() =
        runTest {
            val viewModel = subscribedViewModel(List(10) { match(day = it) })

            viewModel.loadMoreIfNeeded(9)

            assertFalse(viewModel.uiState.value.canLoadMore)
            assertEquals(1, matches.requestedPages.size)
        }

    @Test
    fun `choosing a sort saves it re_queries with that sort and starts again from one page`() =
        runTest {
            val loss = match(day = 1, result = MatchResult.LOSS)
            val win = match(day = 2, result = MatchResult.WIN)
            val viewModel = subscribedViewModel(listOf(loss, win) + List(118) { match(day = 10 + it) })
            viewModel.loadMoreIfNeeded(MATCH_LIST_PAGE_SIZE - 1)

            viewModel.selectSort(MatchSort.RESULT_LOSSES_FIRST)

            val state = viewModel.uiState.value
            assertEquals(listOf(MatchSort.RESULT_LOSSES_FIRST), sorts.saved)
            assertEquals(MatchSort.RESULT_LOSSES_FIRST, state.sort)
            assertEquals(MatchSort.RESULT_LOSSES_FIRST to MATCH_LIST_PAGE_SIZE, matches.requestedPages.last())
            assertEquals(loss.id.toString(), state.matches.first().id)
        }

    @Test
    fun `a sort chosen in an earlier session is the one the list opens with`() =
        runTest {
            sorts = FakeMatchSortStore(initial = MatchSort.OPPONENT_A_TO_Z)

            val state = subscribedViewModel(listOf(match(day = 1))).uiState.value

            assertEquals(MatchSort.OPPONENT_A_TO_Z, state.sort)
            assertEquals(listOf(MatchSort.OPPONENT_A_TO_Z to MATCH_LIST_PAGE_SIZE), matches.requestedPages)
        }

    @Test
    fun `a stored photo path resolves to a file in app storage and a match without one has no thumbnail`() =
        runTest {
            val withPhoto =
                match(
                    day = 2,
                    photos = listOf(PhotoRef(Uuid.random(), "photos/a.jpg", 2048, 1536, 400_000, 0)),
                )
            val withoutPhoto = match(day = 1)

            val rows = subscribedViewModel(listOf(withPhoto, withoutPhoto)).uiState.value.matches

            assertEquals(File(photoRoot, "photos/a.jpg").path, rows[0].thumbnailPath)
            assertNull(rows[1].thumbnailPath)
        }

    @Test
    fun `an empty log is an empty state rather than a loading state`() =
        runTest {
            val state = subscribedViewModel(emptyList()).uiState.value

            assertTrue(state.isEmpty)
            assertFalse(state.canLoadMore)
        }
}
