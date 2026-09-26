@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklelog.ui.match.edit

import androidx.lifecycle.SavedStateHandle
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.domain.match.GameScore
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.person.Person
import com.maxeydev.picklelog.domain.photo.PhotoRef
import com.maxeydev.picklelog.ui.fakes.FakeLastUsedFormatStore
import com.maxeydev.picklelog.ui.fakes.FakeMatchRepository
import com.maxeydev.picklelog.ui.fakes.FakePersonRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MatchEditViewModelTest {
    private val clock = FixedClock(Instant.parse("2026-09-24T12:30:45Z"))
    private val manila = TimeZone.of("Asia/Manila")
    private lateinit var matches: FakeMatchRepository
    private lateinit var people: FakePersonRepository
    private lateinit var formats: FakeLastUsedFormatStore

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        matches = FakeMatchRepository()
        people = FakePersonRepository()
        formats = FakeLastUsedFormatStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()): MatchEditViewModel =
        MatchEditViewModel(
            savedStateHandle = handle,
            matchRepository = matches,
            personRepository = people,
            lastUsedFormatStore = formats,
            clock = clock,
            timeZone = { manila },
        )

    private fun person(name: String): Person = Person(Uuid.random(), name, AppInstant.fromEpochMilliseconds(0))

    private fun existingMatch(): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.DOUBLES,
            date = AppDate.parse("2026-09-20"),
            result = MatchResult.LOSS,
            createdAt = AppInstant.fromEpochMilliseconds(1_000),
            updatedAt = AppInstant.fromEpochMilliseconds(1_000),
            startTime = AppTime.parse("18:00"),
            endTime = AppTime.parse("19:30"),
            location = "Ayala Triangle",
            opponents = listOf(person("Ana"), person("Ben")),
            partner = person("Cy"),
            games = listOf(GameScore(1, 9, 11), GameScore(2, 8, 11)),
            paddle = "Selkirk",
            notes = "Windy",
            photos = listOf(PhotoRef(Uuid.random(), "photos/a.jpg", 10, 10, 100, 0)),
        )

    private fun copyOf(handle: SavedStateHandle): SavedStateHandle =
        SavedStateHandle(handle.keys().associateWith { key -> handle.get<Any?>(key) })

    @Test
    fun `a new match opens on today and now with the last used format`() {
        formats.format = MatchFormat.SINGLES

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isEditing)
        assertEquals(AppDate.parse("2026-09-24"), state.date)
        assertEquals(AppTime.parse("20:30"), state.startTime)
        assertEquals(MatchFormat.SINGLES, state.format)
        assertNull(state.result)
        assertFalse(state.canSave)
    }

    @Test
    fun `a first launch with no stored format opens as doubles`() {
        assertEquals(MatchFormat.DOUBLES, viewModel().uiState.value.format)
    }

    @Test
    fun `saving with only format date and result produces a valid match`() {
        val viewModel = viewModel()

        viewModel.selectResult(MatchResult.WIN)
        viewModel.save()

        val saved = matches.saved.single()
        assertEquals(MatchFormat.DOUBLES, saved.format)
        assertEquals(AppDate.parse("2026-09-24"), saved.date)
        assertEquals(MatchResult.WIN, saved.result)
        assertEquals(clock.instant, saved.createdAt)
        assertEquals(clock.instant, saved.updatedAt)
        assertTrue(saved.opponents.isEmpty())
        assertNull(saved.partner)
        assertTrue(saved.games.isEmpty())
        assertNull(saved.location)
        assertTrue(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `saving a new match records its format as the last used`() {
        val viewModel = viewModel()

        viewModel.selectFormat(MatchFormat.SINGLES)
        viewModel.selectResult(MatchResult.LOSS)
        viewModel.save()

        assertEquals(listOf(MatchFormat.SINGLES), formats.recorded)
    }

    @Test
    fun `a typed name resolves to the existing person with the same normalized name`() {
        val dave = person("Dave")
        people = FakePersonRepository(listOf(dave))
        val viewModel = viewModel()

        viewModel.changePersonName(PersonSlot.OPPONENT_1, "  dave ")
        viewModel.selectResult(MatchResult.WIN)
        viewModel.save()

        assertEquals(listOf(dave), matches.saved.single().opponents)
        assertEquals(1, people.current.size)
    }

    @Test
    fun `switching to singles clears the partner and second opponent before anything is saved`() {
        val viewModel = viewModel()
        viewModel.changePersonName(PersonSlot.OPPONENT_1, "Ana")
        viewModel.changePersonName(PersonSlot.OPPONENT_2, "Ben")
        viewModel.changePersonName(PersonSlot.PARTNER, "Cy")

        viewModel.selectFormat(MatchFormat.SINGLES)

        assertTrue(viewModel.uiState.value.clearedOnFormatSwitch)
        assertEquals(listOf(PersonSlot.OPPONENT_1), viewModel.uiState.value.personSlots.map { it.slot })
        viewModel.selectResult(MatchResult.WIN)
        viewModel.save()
        val saved = matches.saved.single()
        assertEquals(listOf("Ana"), saved.opponents.map { it.displayName })
        assertNull(saved.partner)
    }

    @Test
    fun `the tapped result is saved even when the scores suggest otherwise`() {
        val viewModel = viewModel()
        viewModel.selectResult(MatchResult.WIN)
        viewModel.addGame()
        viewModel.changeGameScores(0, "4", "11")

        assertEquals(MatchResult.LOSS, viewModel.uiState.value.advisory)
        viewModel.save()

        assertEquals(MatchResult.WIN, matches.saved.single().result)
    }

    @Test
    fun `the same person in two slots blocks save and writes nothing`() {
        val viewModel = viewModel()
        viewModel.selectResult(MatchResult.WIN)
        viewModel.changePersonName(PersonSlot.OPPONENT_1, "Dave")
        viewModel.changePersonName(PersonSlot.PARTNER, "dave")

        val partnerSlot = viewModel.uiState.value.personSlots.single { it.slot == PersonSlot.PARTNER }
        assertEquals(PersonSlot.OPPONENT_1, partnerSlot.duplicateOf)
        assertFalse(viewModel.uiState.value.canSave)
        viewModel.save()

        assertTrue(matches.saved.isEmpty())
    }

    @Test
    fun `saved games are numbered contiguously from one after a middle removal`() {
        val viewModel = viewModel()
        viewModel.selectResult(MatchResult.WIN)
        repeat(3) { viewModel.addGame() }
        viewModel.changeGameScores(0, "11", "5")
        viewModel.changeGameScores(1, "3", "11")
        viewModel.changeGameScores(2, "11", "9")
        viewModel.removeGame(1)
        viewModel.save()

        assertEquals(listOf(GameScore(1, 11, 5), GameScore(2, 11, 9)), matches.saved.single().games)
    }

    @Test
    fun `editing keeps id and createdAt refreshes updatedAt and keeps photos`() {
        val existing = existingMatch()
        matches = FakeMatchRepository(listOf(existing))
        val viewModel = viewModel(SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to existing.id.toString())))
        val loaded = viewModel.uiState.value
        assertTrue(loaded.isEditing)
        assertEquals(existing.date, loaded.date)
        assertEquals(listOf("Ana", "Ben", "Cy"), loaded.personSlots.map { it.name })
        assertEquals(2, loaded.games.size)

        val later = Instant.parse("2026-09-25T01:00:00Z")
        clock.instant = later
        viewModel.selectFormat(MatchFormat.SINGLES)
        viewModel.selectResult(MatchResult.WIN)
        viewModel.selectDate(AppDate.parse("2026-09-21"))
        viewModel.changeEndTime(null)
        viewModel.changeLocation("")
        viewModel.save()

        val saved = matches.saved.single()
        assertEquals(existing.id, saved.id)
        assertEquals(existing.createdAt, saved.createdAt)
        assertEquals(later, saved.updatedAt)
        assertEquals(existing.photos, saved.photos)
        assertEquals(MatchFormat.SINGLES, saved.format)
        assertEquals(MatchResult.WIN, saved.result)
        assertEquals(AppDate.parse("2026-09-21"), saved.date)
        assertNull(saved.endTime)
        assertNull(saved.location)
        assertEquals(listOf("Ana"), saved.opponents.map { it.displayName })
        assertNull(saved.partner)
        assertTrue(formats.recorded.isEmpty())
    }

    @Test
    fun `editing a match that no longer exists finishes without showing a form`() {
        val viewModel = viewModel(SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to Uuid.random().toString())))

        assertTrue(viewModel.uiState.value.isFinished)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `an in progress form survives process death through the saved state handle`() {
        val handle = SavedStateHandle()
        val before = viewModel(handle)
        before.selectFormat(MatchFormat.SINGLES)
        before.selectResult(MatchResult.LOSS)
        before.changePersonName(PersonSlot.OPPONENT_1, "Ana")
        before.addGame()
        before.changeGameScores(0, "9", "11")
        before.changeEndTime(AppTime.parse("21:45"))
        before.changeNotes("Wind from the east")
        val expected = before.uiState.value

        formats.format = MatchFormat.DOUBLES
        clock.instant = Instant.parse("2026-09-24T15:00:00Z")
        val after = viewModel(copyOf(handle))

        assertEquals(expected, after.uiState.value)
    }

    @Test
    fun `a restored new match saves under the same id rather than creating a second match`() {
        val handle = SavedStateHandle()
        val before = viewModel(handle)
        before.selectResult(MatchResult.WIN)
        before.save()
        val firstId = matches.saved.single().id

        val after = viewModel(copyOf(handle))
        after.save()

        assertEquals(1, matches.current.size)
        assertEquals(firstId, matches.current.single().id)
    }

    @Test
    fun `a repeated save tap writes the match once`() {
        val viewModel = viewModel()
        viewModel.selectResult(MatchResult.WIN)

        viewModel.save()
        viewModel.save()

        assertEquals(1, matches.saved.size)
    }
}
