@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.ui.fakes.FakeDependencies
import com.maxeydev.picklelog.ui.fakes.FakeMatchRepository
import com.maxeydev.picklelog.ui.home.HomePlaceholderTestTags
import com.maxeydev.picklelog.ui.match.detail.MatchDetailTestTags
import com.maxeydev.picklelog.ui.match.edit.MatchEditTestTags
import com.maxeydev.picklelog.ui.match.edit.PersonSlot
import com.maxeydev.picklelog.ui.navigation.PicklelogNavHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val WAIT_MILLIS = 5_000L

@RunWith(AndroidJUnit4::class)
class MatchLoggingFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private fun waitForTag(tag: String) {
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun storedMatch(): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.DOUBLES,
            date = AppDate.parse("2026-09-20"),
            result = MatchResult.LOSS,
            createdAt = AppInstant.fromEpochMilliseconds(1_000),
            updatedAt = AppInstant.fromEpochMilliseconds(1_000),
        )

    @Test
    fun `the_J1_path_saves_a_match_in_three_in_app_taps`() {
        val dependencies = FakeDependencies()
        compose.setContent { PicklelogNavHost(dependencies = dependencies) }
        var taps = 0

        fun tap(tag: String) {
            waitForTag(tag)
            compose.onNodeWithTag(tag).performClick()
            taps++
        }

        tap(HomePlaceholderTestTags.NEW_MATCH)
        tap(MatchEditTestTags.RESULT_WIN)
        tap(MatchEditTestTags.SAVE)

        compose.waitUntil(WAIT_MILLIS) { dependencies.matchRepository.saved.isNotEmpty() }
        assertEquals(3, taps)
        val saved = dependencies.matchRepository.saved.single()
        assertEquals(MatchResult.WIN, saved.result)
        assertEquals(MatchFormat.DOUBLES, saved.format)
        waitForTag(HomePlaceholderTestTags.NEW_MATCH)
    }

    @Test
    fun `save_stays_disabled_while_two_slots_name_the_same_person`() {
        compose.setContent { PicklelogNavHost(dependencies = FakeDependencies()) }
        waitForTag(HomePlaceholderTestTags.NEW_MATCH)
        compose.onNodeWithTag(HomePlaceholderTestTags.NEW_MATCH).performClick()
        waitForTag(MatchEditTestTags.RESULT_WIN)
        compose.onNodeWithTag(MatchEditTestTags.RESULT_WIN).performClick()
        compose.onNodeWithTag(MatchEditTestTags.SAVE).assertIsEnabled()

        compose.onNodeWithTag(MatchEditTestTags.personSlot(PersonSlot.OPPONENT_1)).performTextInput("Dave")
        compose.onNodeWithTag(MatchEditTestTags.personSlot(PersonSlot.PARTNER)).performTextInput("dave")

        compose.onNodeWithTag(MatchEditTestTags.SAVE).assertIsNotEnabled()
        compose.onNodeWithText("Already entered as Opponent 1").assertExists()
    }

    @Test
    fun `cancelling_the_delete_confirmation_leaves_the_match_untouched`() {
        val match = storedMatch()
        val dependencies = FakeDependencies(matchRepository = FakeMatchRepository(listOf(match)))
        compose.setContent { PicklelogNavHost(dependencies = dependencies) }
        val row = HomePlaceholderTestTags.matchRow(match.id.toString())
        waitForTag(row)
        compose.onNodeWithTag(row).performClick()
        waitForTag(MatchDetailTestTags.DELETE)

        compose.onNodeWithTag(MatchDetailTestTags.DELETE).performClick()
        waitForTag(MatchDetailTestTags.CANCEL_DELETE)
        compose.onNodeWithText("The match and its photos will be permanently deleted. This can't be undone.")
            .assertExists()
        compose.onNodeWithTag(MatchDetailTestTags.CANCEL_DELETE).performClick()
        compose.waitForIdle()

        assertTrue(dependencies.matchRepository.deletedIds.isEmpty())
        assertEquals(listOf(match), dependencies.matchRepository.current)
        compose.onNodeWithTag(MatchDetailTestTags.DELETE).assertExists()
    }

    @Test
    fun `confirming_the_delete_removes_the_match_and_returns_home`() {
        val match = storedMatch()
        val dependencies = FakeDependencies(matchRepository = FakeMatchRepository(listOf(match)))
        compose.setContent { PicklelogNavHost(dependencies = dependencies) }
        val row = HomePlaceholderTestTags.matchRow(match.id.toString())
        waitForTag(row)
        compose.onNodeWithTag(row).performClick()
        waitForTag(MatchDetailTestTags.DELETE)

        compose.onNodeWithTag(MatchDetailTestTags.DELETE).performClick()
        waitForTag(MatchDetailTestTags.CONFIRM_DELETE)
        compose.onNodeWithTag(MatchDetailTestTags.CONFIRM_DELETE).performClick()

        compose.waitUntil(WAIT_MILLIS) { dependencies.matchRepository.current.isEmpty() }
        assertEquals(listOf(match.id), dependencies.matchRepository.deletedIds)
        waitForTag(HomePlaceholderTestTags.NEW_MATCH)
    }
}
