@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.match.list

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.FilterKind
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.person.Person
import com.maxeydev.picklelog.ui.fakes.FakeDependencies
import com.maxeydev.picklelog.ui.fakes.FakeMatchRepository
import com.maxeydev.picklelog.ui.fakes.FakePersonRepository
import com.maxeydev.picklelog.ui.navigation.PicklelogNavHost
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val WAIT_MILLIS = 5_000L

@RunWith(AndroidJUnit4::class)
class FilterUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val ana = Person(Uuid.random(), "Ana", AppInstant.fromEpochMilliseconds(0))
    private val ben = Person(Uuid.random(), "Ben", AppInstant.fromEpochMilliseconds(0))

    private fun match(
        day: Int,
        format: MatchFormat = MatchFormat.DOUBLES,
        result: MatchResult = MatchResult.WIN,
        opponents: List<Person> = emptyList(),
        notes: String? = null,
    ): Match =
        Match(
            id = Uuid.random(),
            format = format,
            date = AppDate.parse("2026-01-01").plus(day, DateTimeUnit.DAY),
            result = result,
            createdAt = AppInstant.fromEpochMilliseconds(1_000),
            updatedAt = AppInstant.fromEpochMilliseconds(1_000),
            opponents = opponents,
            notes = notes,
        )

    private fun showHome(vararg stored: Match) {
        val repository = FakeMatchRepository(stored.toList())
        val dependencies =
            FakeDependencies(
                matchRepository = repository,
                personRepository = FakePersonRepository(initial = listOf(ana, ben), matchSource = repository),
            )
        compose.setContent { PicklelogNavHost(dependencies = dependencies) }
        waitForTag(MatchListTestTags.LIST)
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForTagGone(tag: String) {
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }
    }

    private fun openSheet() {
        compose.onNodeWithTag(MatchListTestTags.FILTER_BUTTON).performClick()
        waitForTag(MatchListTestTags.FILTER_SHEET)
    }

    private fun closeSheet() {
        compose.onNodeWithTag(MatchListTestTags.SHEET_DONE).performClick()
        waitForTagGone(MatchListTestTags.FILTER_SHEET)
    }

    @Test
    fun `active_filters_show_as_chips_each_clearable_on_its_own_with_a_clear_all`() {
        val singlesWin = match(day = 1, format = MatchFormat.SINGLES)
        val singlesLoss = match(day = 2, format = MatchFormat.SINGLES, result = MatchResult.LOSS)
        val doublesWin = match(day = 3)
        showHome(singlesWin, singlesLoss, doublesWin)

        openSheet()
        compose.onNodeWithTag(MatchListTestTags.formatOption(MatchFormat.SINGLES)).performClick()
        compose.onNodeWithTag(MatchListTestTags.resultOption(MatchResult.WIN)).performClick()
        closeSheet()

        compose.onNodeWithTag(MatchListTestTags.filterChip(FilterKind.FORMAT)).assertIsDisplayed()
        compose.onNodeWithTag(MatchListTestTags.filterChip(FilterKind.RESULT)).assertIsDisplayed()
        compose.onNodeWithTag(MatchListTestTags.FILTER_BUTTON).assert(hasText("Filters (2)"))
        waitForTagGone(MatchListTestTags.row(doublesWin.id.toString()))
        compose.onNodeWithTag(MatchListTestTags.row(singlesWin.id.toString())).assertIsDisplayed()

        compose.onNodeWithTag(MatchListTestTags.filterChip(FilterKind.RESULT)).performClick()

        waitForTag(MatchListTestTags.row(singlesLoss.id.toString()))
        compose.onNodeWithTag(MatchListTestTags.filterChip(FilterKind.RESULT)).assertDoesNotExist()
        compose.onNodeWithTag(MatchListTestTags.filterChip(FilterKind.FORMAT)).assertIsDisplayed()

        compose.onNodeWithTag(MatchListTestTags.CLEAR_ALL_FILTERS).performClick()

        waitForTag(MatchListTestTags.row(doublesWin.id.toString()))
        compose.onNodeWithTag(MatchListTestTags.FILTER_CHIPS).assertDoesNotExist()
        compose.onNodeWithTag(MatchListTestTags.FILTER_BUTTON).assert(hasText("Filters"))
    }

    @Test
    fun `the_opponent_filter_offers_known_people_and_keeps_only_matches_against_the_chosen_one`() {
        val againstAna = match(day = 1, opponents = listOf(ana))
        val againstBen = match(day = 2, opponents = listOf(ben))
        showHome(againstAna, againstBen)

        openSheet()
        compose.onNodeWithTag(MatchListTestTags.OPPONENT_PICKER).performClick()
        compose.onNodeWithTag(MatchListTestTags.opponentOption(ben.id.toString())).performClick()
        closeSheet()

        waitForTagGone(MatchListTestTags.row(againstAna.id.toString()))
        compose.onNodeWithTag(MatchListTestTags.row(againstBen.id.toString())).assertIsDisplayed()
        compose.onNodeWithTag(MatchListTestTags.filterChip(FilterKind.OPPONENT)).assert(hasText("vs Ben"))
    }

    @Test
    fun `a_filter_matching_nothing_shows_its_own_empty_state_whose_button_restores_the_list`() {
        val doubles = match(day = 1)
        showHome(doubles)

        openSheet()
        compose.onNodeWithTag(MatchListTestTags.formatOption(MatchFormat.SINGLES)).performClick()
        closeSheet()

        waitForTag(MatchListTestTags.NO_RESULTS)
        compose.onNodeWithText("No matches with these filters").assertIsDisplayed()
        compose.onNodeWithTag(MatchListTestTags.EMPTY_STATE).assertDoesNotExist()
        compose.onNodeWithTag(MatchListTestTags.FILTER_BUTTON).assertIsDisplayed()

        compose.onNodeWithTag(MatchListTestTags.NO_RESULTS_CLEAR).performClick()

        waitForTag(MatchListTestTags.row(doubles.id.toString()))
        compose.onNodeWithTag(MatchListTestTags.NO_RESULTS).assertDoesNotExist()
    }

    @Test
    fun `typing_a_search_narrows_the_list_and_clearing_it_brings_everything_back`() {
        val windy = match(day = 1, notes = "Windy afternoon")
        val calm = match(day = 2, notes = "Calm evening")
        showHome(windy, calm)

        compose.onNodeWithTag(MatchListTestTags.SEARCH_FIELD).performTextInput("windy")

        waitForTagGone(MatchListTestTags.row(calm.id.toString()))
        compose.onNodeWithTag(MatchListTestTags.row(windy.id.toString())).assertIsDisplayed()

        compose.onNodeWithTag(MatchListTestTags.SEARCH_CLEAR).performClick()

        waitForTag(MatchListTestTags.row(calm.id.toString()))
    }

    @Test
    fun `the_filter_sheet_states_that_filters_reset_when_the_app_is_reopened`() {
        showHome(match(day = 1))

        openSheet()

        compose.onNodeWithTag(
            MatchListTestTags.FILTER_RESET_NOTICE,
        ).assert(hasText("Filters reset each time you reopen Picklelog."))
    }
}
