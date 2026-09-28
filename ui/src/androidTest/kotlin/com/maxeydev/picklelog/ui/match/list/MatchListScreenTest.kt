@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.match.list

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.match.MatchSort
import com.maxeydev.picklelog.ui.fakes.FakeDependencies
import com.maxeydev.picklelog.ui.fakes.FakeMatchRepository
import com.maxeydev.picklelog.ui.fakes.FakeMatchSortStore
import com.maxeydev.picklelog.ui.navigation.PicklelogNavHost
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val WAIT_MILLIS = 5_000L

@RunWith(AndroidJUnit4::class)
class MatchListScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun match(
        day: Int,
        result: MatchResult = MatchResult.WIN,
    ): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.DOUBLES,
            date = AppDate.parse("2026-01-01").plus(day, DateTimeUnit.DAY),
            result = result,
            createdAt = AppInstant.fromEpochMilliseconds(1_000),
            updatedAt = AppInstant.fromEpochMilliseconds(1_000),
        )

    private fun showScreen(
        state: MatchListUiState,
        onNewMatch: () -> Unit = {},
        onSortSelected: (MatchSort) -> Unit = {},
    ) {
        compose.setContent {
            MaterialTheme {
                MatchListScreen(
                    state = state,
                    onNewMatch = onNewMatch,
                    onOpenMatch = {},
                    onSortSelected = onSortSelected,
                    onLastVisibleIndexChanged = {},
                    onLogAnother = {},
                    onSavedConfirmationDismissed = {},
                    filterActions =
                        MatchListFilterActions(
                            onSearchChanged = {},
                            onFilterChanged = {},
                            onFilterCleared = {},
                            onAllFiltersCleared = {},
                            onFiltersAndSearchCleared = {},
                        ),
                )
            }
        }
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun rowTop(match: Match): Float =
        compose
            .onNodeWithTag(MatchListTestTags.row(match.id.toString()))
            .fetchSemanticsNode()
            .boundsInRoot.top

    private fun contentState(sort: MatchSort = MatchSort.DEFAULT): MatchListUiState =
        MatchListUiState(
            isLoading = false,
            sort = sort,
            matches =
                listOf(
                    MatchRowUiState(
                        id = "only",
                        date = AppDate.parse("2026-09-20"),
                        format = MatchFormat.SINGLES,
                        result = MatchResult.WIN,
                        opponentNames = emptyList(),
                        games = emptyList(),
                        thumbnailPath = null,
                    ),
                ),
            pageLimit = MATCH_LIST_PAGE_SIZE,
        )

    @Test
    fun `an_empty_log_shows_an_inviting_empty_state_whose_button_starts_a_new_match`() {
        var newMatchRequests = 0
        showScreen(MatchListUiState(isLoading = false), onNewMatch = { newMatchRequests++ })

        compose.onNodeWithTag(MatchListTestTags.EMPTY_STATE).assertIsDisplayed()
        compose.onNodeWithText("No matches yet").assertIsDisplayed()
        compose.onNodeWithTag(MatchListTestTags.NEW_MATCH).assertIsDisplayed()
        compose.onNodeWithTag(MatchListTestTags.LIST).assertDoesNotExist()
        compose.onNodeWithTag(MatchListTestTags.SORT_BUTTON).assertDoesNotExist()

        compose.onNodeWithTag(MatchListTestTags.EMPTY_LOG_MATCH).performClick()

        assertEquals(1, newMatchRequests)
    }

    @Test
    fun `while_the_first_page_loads_neither_the_empty_state_nor_the_list_flashes`() {
        showScreen(MatchListUiState())

        compose.onNodeWithTag(MatchListTestTags.EMPTY_STATE).assertDoesNotExist()
        compose.onNodeWithTag(MatchListTestTags.LIST).assertDoesNotExist()
    }

    @Test
    fun `the_active_sort_is_visible_without_opening_the_menu`() {
        showScreen(contentState(sort = MatchSort.OPPONENT_A_TO_Z))

        compose
            .onNodeWithTag(MatchListTestTags.SORT_BUTTON)
            .assert(hasText("Sort: Opponent A–Z — by first opponent"))
    }

    @Test
    fun `the_sort_menu_offers_all_eight_sorts_marks_the_active_one_and_reports_a_choice`() {
        val chosen = mutableListOf<MatchSort>()
        showScreen(contentState(sort = MatchSort.DATE_OLDEST), onSortSelected = { chosen += it })

        compose.onNodeWithTag(MatchListTestTags.SORT_BUTTON).performClick()

        MatchSort.entries.forEach { sort ->
            val option = compose.onNodeWithTag(MatchListTestTags.sortOption(sort))
            if (sort == MatchSort.DATE_OLDEST) option.assertIsSelected() else option.assertIsNotSelected()
        }
        compose.onNodeWithText("Date — newest first").assertIsDisplayed()
        compose.onNodeWithText("Date — oldest first").assertIsDisplayed()
        compose.onNodeWithText("Result — wins first").assertIsDisplayed()
        compose.onNodeWithText("Result — losses first").assertIsDisplayed()
        compose.onNodeWithText("Opponent A–Z — by first opponent").assertIsDisplayed()
        compose.onNodeWithText("Location A–Z").assertIsDisplayed()
        compose.onNodeWithText("Duration — shortest first").assertIsDisplayed()
        compose.onNodeWithText("Duration — longest first").assertIsDisplayed()

        compose.onNodeWithTag(MatchListTestTags.sortOption(MatchSort.RESULT_WINS_FIRST)).performClick()

        assertEquals(listOf(MatchSort.RESULT_WINS_FIRST), chosen)
    }

    @Test
    fun `the_home_screen_lists_matches_newest_first`() {
        val oldest = match(day = 1)
        val middle = match(day = 50)
        val newest = match(day = 100)
        val dependencies = FakeDependencies(matchRepository = FakeMatchRepository(listOf(middle, oldest, newest)))
        compose.setContent { PicklelogNavHost(dependencies = dependencies) }
        waitForTag(MatchListTestTags.row(oldest.id.toString()))

        assertTrue(rowTop(newest) < rowTop(middle))
        assertTrue(rowTop(middle) < rowTop(oldest))
    }

    @Test
    fun `choosing_a_sort_reorders_the_list_and_remembers_the_choice`() {
        val newWin = match(day = 100, result = MatchResult.WIN)
        val oldLoss = match(day = 1, result = MatchResult.LOSS)
        val sorts = FakeMatchSortStore()
        val dependencies =
            FakeDependencies(matchRepository = FakeMatchRepository(listOf(newWin, oldLoss)), matchSortStore = sorts)
        compose.setContent { PicklelogNavHost(dependencies = dependencies) }
        waitForTag(MatchListTestTags.SORT_BUTTON)
        assertTrue(rowTop(newWin) < rowTop(oldLoss))

        compose.onNodeWithTag(MatchListTestTags.SORT_BUTTON).performClick()
        compose.onNodeWithTag(MatchListTestTags.sortOption(MatchSort.RESULT_LOSSES_FIRST)).performClick()
        compose.waitForIdle()

        assertEquals(listOf(MatchSort.RESULT_LOSSES_FIRST), sorts.saved)
        assertTrue(rowTop(oldLoss) < rowTop(newWin))
        compose
            .onNodeWithTag(MatchListTestTags.SORT_BUTTON)
            .assert(hasText("Sort: Result — losses first"))
    }

    @Test
    fun `list_items_are_keyed_by_match_id`() {
        val stored = List(80) { match(day = it) }
        val target = stored[40]
        val dependencies = FakeDependencies(matchRepository = FakeMatchRepository(stored))
        compose.setContent { PicklelogNavHost(dependencies = dependencies) }
        waitForTag(MatchListTestTags.LIST)

        compose.onNodeWithTag(MatchListTestTags.LIST).performScrollToKey(target.id.toString())

        compose.onNodeWithTag(MatchListTestTags.row(target.id.toString())).assertIsDisplayed()
    }

    @Test
    fun `scrolling_towards_the_end_of_the_first_page_pulls_in_the_next_page`() {
        val stored = List(120) { match(day = it) }
        val repository = FakeMatchRepository(stored)
        compose.setContent { PicklelogNavHost(dependencies = FakeDependencies(matchRepository = repository)) }
        waitForTag(MatchListTestTags.LIST)
        assertEquals(listOf(MATCH_LIST_PAGE_SIZE), repository.requestedPages.map { it.second })

        compose.onNodeWithTag(MatchListTestTags.LIST).performScrollToIndex(MATCH_LIST_PAGE_SIZE - 1)
        compose.waitUntil(WAIT_MILLIS) { repository.requestedPages.any { it.second == 2 * MATCH_LIST_PAGE_SIZE } }
        compose.onNodeWithTag(MatchListTestTags.LIST).performScrollToIndex(2 * MATCH_LIST_PAGE_SIZE - 1)
        compose.waitUntil(WAIT_MILLIS) { repository.requestedPages.any { it.second == 3 * MATCH_LIST_PAGE_SIZE } }

        val oldest = stored.first()
        compose.onNodeWithTag(MatchListTestTags.LIST).performScrollToKey(oldest.id.toString())
        compose.onNodeWithTag(MatchListTestTags.row(oldest.id.toString())).assertIsDisplayed()
    }

    @Test
    fun `the_search_and_filter_header_scrolls_away_with_the_list_so_rows_get_the_screen`() {
        val stored = List(80) { match(day = it) }
        compose.setContent {
            PicklelogNavHost(
                dependencies = FakeDependencies(matchRepository = FakeMatchRepository(stored)),
            )
        }
        waitForTag(MatchListTestTags.LIST)
        compose.onNodeWithTag(MatchListTestTags.SEARCH_FIELD).assertIsDisplayed()

        compose.onNodeWithTag(MatchListTestTags.LIST).performScrollToIndex(40)

        compose.onNodeWithTag(MatchListTestTags.SEARCH_FIELD).assertDoesNotExist()
        compose.onNodeWithTag(MatchListTestTags.LIST).performScrollToIndex(0)
        compose.onNodeWithTag(MatchListTestTags.SEARCH_FIELD).assertIsDisplayed()
    }

    @Test
    fun `at_200_percent_font_the_no_results_button_is_not_covered_by_the_new_match_button`() {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                MaterialTheme {
                    MatchListScreen(
                        state = MatchListUiState(isLoading = false, filter = FilterState(format = MatchFormat.SINGLES)),
                        onNewMatch = {},
                        onOpenMatch = {},
                        onSortSelected = {},
                        onLastVisibleIndexChanged = {},
                        onLogAnother = {},
                        onSavedConfirmationDismissed = {},
                        filterActions =
                            MatchListFilterActions(
                                onSearchChanged = {},
                                onFilterChanged = {},
                                onFilterCleared = {},
                                onAllFiltersCleared = {},
                                onFiltersAndSearchCleared = {},
                            ),
                    )
                }
            }
        }
        compose.onNodeWithTag(MatchListTestTags.LIST).performScrollToIndex(1)

        val clear = compose.onNodeWithTag(MatchListTestTags.NO_RESULTS_CLEAR).fetchSemanticsNode().boundsInRoot
        val newMatch = compose.onNodeWithTag(MatchListTestTags.NEW_MATCH).fetchSemanticsNode().boundsInRoot

        assertTrue("clear button $clear overlaps new match button $newMatch", !clear.overlaps(newMatch))
    }
}
