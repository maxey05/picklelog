@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.share

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.GameScore
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.person.Person
import com.maxeydev.picklelog.domain.streak.StreakResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CardDataTest {
    private fun person(name: String): Person = Person(Uuid.random(), name, AppInstant.fromEpochMilliseconds(0))

    private fun match(
        format: MatchFormat = MatchFormat.DOUBLES,
        result: MatchResult = MatchResult.WIN,
        opponents: List<Person> = emptyList(),
        partner: Person? = null,
        games: List<GameScore> = emptyList(),
        location: String? = null,
    ): Match =
        Match(
            id = Uuid.random(),
            format = format,
            date = AppDate.parse("2026-09-20"),
            result = result,
            createdAt = AppInstant.fromEpochMilliseconds(0),
            updatedAt = AppInstant.fromEpochMilliseconds(0),
            opponents = opponents,
            partner = partner,
            games = games,
            location = location,
        )

    @Test
    fun `a doubles card names both opponents the partner the score and the whole history streak`() {
        val data =
            buildCardData(
                match =
                    match(
                        opponents = listOf(person("Ana"), person("Ben")),
                        partner = person("Cy"),
                        games = listOf(GameScore(2, 8, 11), GameScore(1, 11, 9)),
                        location = "  Ayala Triangle ",
                    ),
                displayName = " Matty ",
                streak = StreakResult(current = 3, longest = 5),
                photoDataUri = null,
                labels = FakeCardLabels(),
            )

        assertEquals("Matty", data.displayName)
        assertEquals("vs Ana & Ben", data.opponents)
        assertEquals("with Cy", data.partner)
        assertEquals("11–9 · 8–11", data.score)
        assertEquals("Ayala Triangle", data.location)
        assertEquals("3-week streak", data.streak)
        assertEquals("Win", data.result)
        assertTrue(data.isWin)
    }

    @Test
    fun `optional fields are absent rather than blank placeholders`() {
        val data =
            buildCardData(
                match = match(format = MatchFormat.SINGLES, result = MatchResult.LOSS, location = "   "),
                displayName = "",
                streak = StreakResult.NONE,
                photoDataUri = null,
                labels = FakeCardLabels(),
            )

        assertEquals("", data.displayName)
        assertNull(data.opponents)
        assertNull(data.partner)
        assertNull(data.score)
        assertNull(data.location)
        assertNull(data.streak)
        assertNull(data.photo)
        assertFalse(data.isWin)
        assertEquals("Loss", data.result)
    }

    @Test
    fun `hostile characters in names survive as data and cannot close the script call`() {
        val hostile = "Dave \"The Wall\" O'Neil \\ </script><img src=x onerror=alert(1)>   end"
        val data =
            CardData(
                brand = "Picklelog",
                displayName = hostile,
                meta = "m",
                result = "Win",
                isWin = true,
                opponents = "vs José 🏓 Ñuñez & 李雷",
            )

        val call = CardDataSerializer.renderCall(data, "0f1e2d3c-token")
        val encoded = CardDataSerializer.toJson(data)

        assertTrue(call.startsWith("window.renderCard({"))
        assertTrue(call.endsWith("}, \"0f1e2d3c-token\");"))
        assertFalse(encoded.contains("<"))
        assertFalse(encoded.contains(">"))
        assertFalse(encoded.contains(" "))
        assertEquals(data, CardDataSerializer.decode(encoded))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a render token that is not a plain identifier is refused`() {
        CardDataSerializer.renderCall(
            CardData(brand = "b", displayName = "", meta = "m", result = "r", isWin = true),
            "x\"); alert(1); (\"",
        )
    }

    @Test
    fun `only bundled asset urls are allowed to load`() {
        assertTrue(isAllowedCardUrl("file:///android_asset/card/index.html"))
        assertTrue(isAllowedCardUrl("file:///android_asset/card/card.css"))
        assertFalse(isAllowedCardUrl("https://example.com/tracker.png"))
        assertFalse(isAllowedCardUrl("http://10.0.2.2/x"))
        assertFalse(isAllowedCardUrl("file:///data/data/com.maxeydev.picklelog/files/photos/a.jpg"))
        assertFalse(isAllowedCardUrl("file:///android_asset/../../data/x"))
        assertFalse(isAllowedCardUrl("content://com.maxeydev.picklelog.fileprovider/cards/a.jpg"))
    }
}
