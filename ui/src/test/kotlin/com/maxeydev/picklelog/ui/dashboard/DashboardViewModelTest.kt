@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.FilterKind
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.person.Person
import com.maxeydev.picklelog.domain.streak.StreakEngine
import com.maxeydev.picklelog.ui.fakes.FakeEntitlementRepository
import com.maxeydev.picklelog.ui.fakes.FakeMatchRepository
import com.maxeydev.picklelog.ui.fakes.FakeMatchSortStore
import com.maxeydev.picklelog.ui.fakes.FakePersonRepository
import com.maxeydev.picklelog.ui.fakes.FakeProfileRepository
import com.maxeydev.picklelog.ui.fakes.FixedClock
import com.maxeydev.picklelog.ui.match.list.MatchListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
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

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val wednesday = AppInstant.parse("2026-03-04T12:00:00Z")
    private lateinit var handle: SavedStateHandle
    private lateinit var matches: FakeMatchRepository
    private lateinit var people: FakePersonRepository
    private lateinit var profile: FakeProfileRepository
    private val dave = Person(Uuid.random(), "Dave", AppInstant.fromEpochMilliseconds(0))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        handle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun match(
        date: String,
        format: MatchFormat = MatchFormat.SINGLES,
        result: MatchResult = MatchResult.WIN,
        opponents: List<Person> = emptyList(),
    ): Match =
        Match(
            id = Uuid.random(),
            format = format,
            date = AppDate.parse(date),
            result = result,
            createdAt = AppInstant.fromEpochMilliseconds(1_000),
            updatedAt = AppInstant.fromEpochMilliseconds(1_000),
            opponents = opponents,
        )

    private fun TestScope.dashboard(
        stored: List<Match>,
        displayName: String = "",
    ): DashboardViewModel {
        matches = FakeMatchRepository(stored)
        people = FakePersonRepository(initial = listOf(dave))
        profile = FakeProfileRepository(displayName = displayName)
        val viewModel =
            DashboardViewModel(
                homeEntryState = handle,
                matchRepository = matches,
                personRepository = people,
                profileRepository = profile,
                streakEngine = StreakEngine(FixedClock(wednesday)) { TimeZone.UTC },
                defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun TestScope.listFor(dashboardMatches: FakeMatchRepository): MatchListViewModel {
        val list =
            MatchListViewModel(
                savedStateHandle = handle,
                matchRepository = dashboardMatches,
                personRepository = people,
                matchSortStore = FakeMatchSortStore(),
                entitlementRepository = FakeEntitlementRepository(),
                photoFile = { File(it) },
                defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { list.uiState.collect {} }
        return list
    }

    @Test
    fun `with no matches the dashboard says so instead of showing zero figures`() =
        runTest {
            val state = dashboard(emptyList()).uiState.value

            assertFalse(state.isLoading)
            assertFalse(state.hasAnyMatches)
            assertNull(state.stats.overall.winPercent)
            assertEquals(0, state.streak.current)
        }

    @Test
    fun `the header shows the whole record and the current streak`() =
        runTest {
            val state =
                dashboard(
                    listOf(
                        match("2026-03-02"),
                        match("2026-02-24", result = MatchResult.LOSS),
                        match("2026-02-17", format = MatchFormat.DOUBLES),
                    ),
                    displayName = "  Matty ",
                ).uiState.value

            assertEquals("Matty", state.displayName)
            assertEquals(3, state.stats.totalMatches)
            assertEquals(2, state.stats.overall.wins)
            assertEquals(67, state.stats.overall.winPercent)
            assertEquals(3, state.streak.current)
            assertFalse(state.isFiltered)
        }

    @Test
    fun `an empty display name is kept empty rather than replaced with a placeholder`() =
        runTest {
            val state = dashboard(listOf(match("2026-03-02")), displayName = "   ").uiState.value

            assertEquals("", state.displayName)
            assertFalse(state.hasDisplayName)
        }

    @Test
    fun `adding editing and deleting matches update the figures without a refresh`() =
        runTest {
            val first = match("2026-03-02")
            val viewModel = dashboard(listOf(first))

            val second = match("2026-03-03", result = MatchResult.LOSS)
            matches.saveMatch(second)
            assertEquals(2, viewModel.uiState.value.stats.totalMatches)

            matches.saveMatch(second.copy(result = MatchResult.WIN))
            assertEquals(2, viewModel.uiState.value.stats.overall.wins)

            matches.deleteMatch(second.id)
            assertEquals(1, viewModel.uiState.value.stats.totalMatches)

            matches.saveMatch(second)
            assertEquals(2, viewModel.uiState.value.stats.totalMatches)
        }

    @Test
    fun `a filter narrows the counts but the streak stays whole history`() =
        runTest {
            val viewModel =
                dashboard(
                    listOf(
                        match("2026-03-02", format = MatchFormat.DOUBLES),
                        match("2026-02-24", format = MatchFormat.DOUBLES),
                        match("2026-02-17", format = MatchFormat.SINGLES, result = MatchResult.LOSS),
                    ),
                )
            val whole = viewModel.uiState.value

            listFor(matches).changeFilter(FilterState(format = MatchFormat.SINGLES))

            val filtered = viewModel.uiState.value
            assertTrue(filtered.isFiltered)
            assertEquals(1, filtered.stats.totalMatches)
            assertEquals(0, filtered.stats.overall.winPercent)
            assertEquals(whole.streak, filtered.streak)
            assertEquals(3, filtered.streak.current)
        }

    @Test
    fun `clearing the filter restores whole history figures immediately`() =
        runTest {
            val viewModel =
                dashboard(listOf(match("2026-03-02"), match("2026-03-03", result = MatchResult.LOSS)))
            val list = listFor(matches)

            list.changeFilter(FilterState(result = MatchResult.WIN))
            assertEquals(1, viewModel.uiState.value.stats.totalMatches)

            list.clearFilter(FilterKind.RESULT)

            assertFalse(viewModel.uiState.value.isFiltered)
            assertEquals(2, viewModel.uiState.value.stats.totalMatches)
        }

    @Test
    fun `the dashboard and the list read the same filter and cannot disagree`() =
        runTest {
            val stored =
                List(40) { index ->
                    match(
                        date = "2026-02-${(index % 28 + 1).toString().padStart(2, '0')}",
                        format = if (index % 3 == 0) MatchFormat.DOUBLES else MatchFormat.SINGLES,
                        result = if (index % 4 == 0) MatchResult.LOSS else MatchResult.WIN,
                        opponents = if (index % 2 == 0) listOf(dave) else emptyList(),
                    )
                }
            val viewModel = dashboard(stored)
            val list = listFor(matches)
            val filters =
                listOf(
                    FilterState(format = MatchFormat.DOUBLES),
                    FilterState(result = MatchResult.LOSS),
                    FilterState(opponentId = dave.id),
                    FilterState(fromDate = AppDate.parse("2026-02-10"), toDate = AppDate.parse("2026-02-20")),
                    FilterState(format = MatchFormat.SINGLES, result = MatchResult.WIN, opponentId = dave.id),
                    FilterState.NONE,
                )

            filters.forEach { filter ->
                list.changeFilter(filter)

                val listed = list.uiState.value
                val shown = viewModel.uiState.value
                assertEquals(listed.filter, shown.filter)
                assertEquals("count for $filter", listed.matches.size, shown.stats.totalMatches)
                assertEquals(
                    "wins for $filter",
                    listed.matches.count { it.result == MatchResult.WIN },
                    shown.stats.overall.wins,
                )
            }
        }

    @Test
    fun `an opponent filter is named with the opponent's display name`() =
        runTest {
            val viewModel = dashboard(listOf(match("2026-03-02", opponents = listOf(dave))))

            listFor(matches).changeFilter(FilterState(opponentId = dave.id))

            assertEquals("Dave", viewModel.uiState.value.filteredOpponentName)
        }

    @Test
    fun `a filter that matches nothing is reported as such while the streak survives`() =
        runTest {
            val viewModel = dashboard(listOf(match("2026-03-02", format = MatchFormat.SINGLES)))

            listFor(matches).changeFilter(FilterState(format = MatchFormat.DOUBLES))

            val state = viewModel.uiState.value
            assertTrue(state.hasNoFilteredMatches)
            assertNull(state.stats.overall.winPercent)
            assertEquals(1, state.streak.current)
        }

    @Test
    fun `current and longest are both exposed so the equal case can be worded`() =
        runTest {
            val state = dashboard(listOf(match("2026-03-02"), match("2026-02-24"))).uiState.value

            assertTrue(state.streak.isCurrentTheLongest)
            assertEquals(2, state.streak.longest)
        }
}
