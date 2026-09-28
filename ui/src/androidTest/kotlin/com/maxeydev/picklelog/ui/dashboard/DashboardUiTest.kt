@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.FilterState
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.stats.BasicStats
import com.maxeydev.picklelog.domain.stats.WinLoss
import com.maxeydev.picklelog.domain.streak.StreakResult
import com.maxeydev.picklelog.ui.fakes.FakeDependencies
import com.maxeydev.picklelog.ui.fakes.FakeMatchRepository
import com.maxeydev.picklelog.ui.fakes.FakeProfileRepository
import com.maxeydev.picklelog.ui.navigation.PicklelogNavHost
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val WAIT_MILLIS = 5_000L
private const val PIXEL_TOLERANCE = 1f

@RunWith(AndroidJUnit4::class)
class DashboardUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val record =
        BasicStats(
            overall = WinLoss(wins = 8, losses = 4),
            singles = WinLoss(wins = 8, losses = 4),
            doubles = WinLoss.NONE,
        )

    private fun state(
        filter: FilterState = FilterState.NONE,
        streak: StreakResult = StreakResult(current = 3, longest = 5),
        displayName: String = "Matty",
        stats: BasicStats = record,
        hasAnyMatches: Boolean = true,
    ): DashboardUiState =
        DashboardUiState(
            isLoading = false,
            displayName = displayName,
            stats = stats,
            streak = streak,
            hasAnyMatches = hasAnyMatches,
            filter = filter,
        )

    private fun showHeader(
        state: DashboardUiState,
        fontScale: Float = 1f,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Box(modifier = Modifier.width(360.dp)) {
                        DashboardHeader(state = state, onOpenStats = {})
                    }
                }
            }
        }
    }

    private fun spokenHeader(): String {
        val node = compose.onNodeWithTag(DashboardTestTags.HEADER).fetchSemanticsNode()
        val descriptions = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        return (texts + descriptions).joinToString(", ")
    }

    private fun SemanticsNodeInteraction.assertTextFits() {
        val results = mutableListOf<TextLayoutResult>()
        val node = fetchSemanticsNode()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        val layout = results.single()
        val widestLineEnd = (0 until layout.lineCount).maxOf { layout.getLineRight(it) }
        val textHeight = layout.multiParagraph.height
        val measured = "'${layout.layoutInput.text}' is ${layout.size}, lines end at $widestLineEnd, height $textHeight"
        assertTrue("$measured: cut off at the side", widestLineEnd <= layout.size.width + PIXEL_TOLERANCE)
        assertTrue("$measured: cut off at the bottom", textHeight <= layout.size.height + PIXEL_TOLERANCE)
        val header = compose.onNodeWithTag(DashboardTestTags.HEADER).fetchSemanticsNode().boundsInRoot
        val bounds = node.boundsInRoot
        assertTrue("$measured escapes the header $header", bounds.bottom <= header.bottom + PIXEL_TOLERANCE)
        assertTrue("$measured escapes the header $header", bounds.right <= header.right + PIXEL_TOLERANCE)
    }

    @Test
    fun the_header_shows_name_count_record_percentage_and_streak() {
        showHeader(state())

        compose.onNodeWithTag(DashboardTestTags.NAME, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("12 matches", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("8 W · 4 L", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("67% won", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("3-week streak · all matches", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun with_no_filter_there_is_no_filter_indicator_at_all() {
        showHeader(state())

        compose.onAllNodesWithTag(DashboardTestTags.FILTER_INDICATOR, useUnmergedTree = true).assertCountEquals(0)
        assertFalse(spokenHeader().contains("filtered"))
    }

    @Test
    fun an_active_filter_is_named_in_words_next_to_an_icon() {
        showHeader(state(filter = FilterState(format = MatchFormat.SINGLES, result = MatchResult.WIN)))

        compose
            .onNodeWithTag(DashboardTestTags.FILTER_INDICATOR, useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("Filtered: Singles · Wins", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(spokenHeader().contains("figures filtered by Singles · Wins"))
    }

    @Test
    fun zero_matches_show_an_empty_message_not_zero_figures() {
        showHeader(state(stats = BasicStats.EMPTY, streak = StreakResult.NONE, hasAnyMatches = false))

        compose.onNodeWithTag(DashboardTestTags.EMPTY, useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithTag(DashboardTestTags.WIN_PERCENT, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasText("0%", substring = true), useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodes(hasText("NaN", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun an_empty_display_name_leaves_no_label_behind() {
        showHeader(state(displayName = ""))

        compose.onAllNodesWithTag(DashboardTestTags.NAME, useUnmergedTree = true).assertCountEquals(0)
        assertFalse(spokenHeader().startsWith(","))
    }

    @Test
    fun the_header_reads_as_one_sentence_for_talkback() {
        showHeader(state(filter = FilterState(format = MatchFormat.SINGLES)))

        val spoken = spokenHeader()
        listOf(
            "Matty",
            "12 matches",
            "8 won, 4 lost",
            "67 percent won",
            "current streak 3 weeks, counting all matches",
            "figures filtered by Singles",
        ).forEach { part -> assertTrue("missing '$part' in: $spoken", spoken.contains(part)) }
    }

    @Test
    fun at_double_font_size_nothing_in_the_header_is_clipped() {
        showHeader(
            state(
                filter = FilterState(format = MatchFormat.DOUBLES, result = MatchResult.LOSS, location = "Court A"),
                displayName = "Maximiliano Alejandro de la Cruz",
            ),
            fontScale = 2f,
        )

        listOf(
            DashboardTestTags.NAME,
            DashboardTestTags.MATCH_COUNT,
            DashboardTestTags.RECORD,
            DashboardTestTags.WIN_PERCENT,
            DashboardTestTags.STREAK,
        ).forEach { tag ->
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed().assertTextFits()
        }
        compose.onNodeWithText("Filtered:", substring = true, useUnmergedTree = true).assertTextFits()
    }

    @Test
    fun the_stats_screen_says_when_the_current_streak_is_the_longest() {
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(
                    state = state(streak = StreakResult(current = 4, longest = 4)),
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("This is your longest streak ever.", useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithTag(DashboardTestTags.STATS_LONGEST_STREAK).assertCountEquals(0)
    }

    @Test
    fun the_stats_screen_shows_a_dash_for_a_format_with_no_matches_and_locked_pro_rows() {
        compose.setContent {
            MaterialTheme {
                ExpandedStatsScreen(state = state(), onBack = {})
            }
        }

        compose.onNodeWithTag(DashboardTestTags.STATS_DOUBLES).assert(hasText("—", substring = true))
        compose.onNodeWithTag(DashboardTestTags.STATS_DOUBLES).assert(hasText("No matches", substring = true))
        compose.onNodeWithTag(DashboardTestTags.STATS_SINGLES).assert(hasText("67%", substring = true))
        compose.onNodeWithTag(DashboardTestTags.STATS_CURRENT_STREAK).assert(hasText("3 weeks", substring = true))
        compose.onNodeWithTag(DashboardTestTags.STATS_LONGEST_STREAK).assert(hasText("5 weeks", substring = true))
        compose.onNodeWithTag(DashboardTestTags.proRow(0)).performScrollToAndAssertLocked()
    }

    private fun SemanticsNodeInteraction.performScrollToAndAssertLocked() {
        performScrollTo()
        assert(hasText("Locked · Pro", substring = true))
    }

    @Test
    fun tapping_the_header_opens_the_stats_screen_with_the_same_filtered_figures() {
        val day = AppDate.parse("2026-09-01")
        val stored =
            listOf(MatchResult.WIN, MatchResult.LOSS, MatchResult.WIN).mapIndexed { index, result ->
                Match(
                    id = Uuid.random(),
                    format = if (index == 1) MatchFormat.DOUBLES else MatchFormat.SINGLES,
                    date = day,
                    result = result,
                    createdAt = AppInstant.fromEpochMilliseconds(index.toLong()),
                    updatedAt = AppInstant.fromEpochMilliseconds(index.toLong()),
                )
            }
        val dependencies =
            FakeDependencies(
                matchRepository = FakeMatchRepository(stored),
                profileRepository = FakeProfileRepository(displayName = "Matty"),
            )
        compose.setContent { MaterialTheme { PicklelogNavHost(dependencies = dependencies) } }
        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodesWithTag(DashboardTestTags.HEADER).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag(DashboardTestTags.HEADER).performClick()

        compose.waitUntil(WAIT_MILLIS) {
            compose.onAllNodesWithTag(DashboardTestTags.STATS_SCREEN).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(DashboardTestTags.STATS_MATCHES).assert(hasText("3"))
        compose.onNodeWithTag(DashboardTestTags.STATS_RECORD).assert(hasText("2–1"))
    }
}
