@file:OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)

package com.maxeydev.picklelog.ui.share

import androidx.lifecycle.SavedStateHandle
import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.match.Match
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.photo.PhotoRef
import com.maxeydev.picklelog.domain.share.CardFormat
import com.maxeydev.picklelog.domain.share.CardLayout
import com.maxeydev.picklelog.domain.share.CardRatio
import com.maxeydev.picklelog.domain.share.CardTheme
import com.maxeydev.picklelog.domain.streak.InsuredStreakEngine
import com.maxeydev.picklelog.ui.fakes.FakeCardFormatStore
import com.maxeydev.picklelog.ui.fakes.FakeCardRenderer
import com.maxeydev.picklelog.ui.fakes.FakeMatchRepository
import com.maxeydev.picklelog.ui.fakes.FakeProfileRepository
import com.maxeydev.picklelog.ui.fakes.FixedClock
import com.maxeydev.picklelog.ui.navigation.MATCH_ID_ARGUMENT
import com.maxeydev.picklelog.ui.paywall.UpgradeReason
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
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SharePreviewViewModelTest {
    private val renderer = FakeCardRenderer()
    private val formats = FakeCardFormatStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun match(
        date: String,
        photos: List<PhotoRef> = emptyList(),
    ): Match =
        Match(
            id = Uuid.random(),
            format = MatchFormat.SINGLES,
            date = AppDate.parse(date),
            result = MatchResult.WIN,
            createdAt = AppInstant.fromEpochMilliseconds(0),
            updatedAt = AppInstant.fromEpochMilliseconds(0),
            photos = photos,
        )

    private fun viewModel(
        id: Uuid,
        matches: FakeMatchRepository,
        isPro: Boolean = false,
        proSince: AppInstant? = null,
    ): SharePreviewViewModel =
        SharePreviewViewModel(
            savedStateHandle = SavedStateHandle(mapOf(MATCH_ID_ARGUMENT to id.toString())),
            matchRepository = matches,
            profileRepository = FakeProfileRepository(displayName = "Matty", isPro = isPro, proSince = proSince),
            formatStore = formats,
            streakEngine = InsuredStreakEngine(FixedClock(AppInstant.parse("2026-03-04T12:00:00Z"))) { TimeZone.UTC },
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

    @Test
    fun `the remembered ratio and variant are used for the first render`() {
        val shared = match("2026-03-02")
        formats.store(CardFormat(CardRatio.SQUARE, CardTheme.LIGHT))

        val state = viewModel(shared.id, FakeMatchRepository(listOf(shared))).uiState.value

        val data = renderer.rendered.single()
        assertEquals(CardRatio.SQUARE, data.ratio)
        assertEquals(CardTheme.LIGHT, data.theme)
        assertEquals(CardRatio.SQUARE, state.format.ratio)
    }

    @Test
    fun `picking a ratio re-renders the real match and remembers the choice`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        viewModel.selectRatio(CardRatio.SQUARE)
        viewModel.selectTheme(CardTheme.LIGHT)

        val last = renderer.rendered.last()
        assertEquals(3, renderer.rendered.size)
        assertEquals(CardRatio.SQUARE, last.ratio)
        assertEquals(CardTheme.LIGHT, last.theme)
        assertEquals("Matty", last.displayName)
        assertEquals(CardFormat(CardRatio.SQUARE, CardTheme.LIGHT), formats.saved.last())
    }

    @Test
    fun `picking the same option again does not re-render`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        viewModel.selectRatio(CardRatio.TALL)

        assertEquals(1, renderer.rendered.size)
        assertTrue(formats.saved.isEmpty())
    }

    @Test
    fun `a match with a photo is offered the photo layout and can override it off`() {
        val photo = PhotoRef(Uuid.random(), "photos/missing.jpg", 10, 10, 10, 0)
        val shared = match("2026-03-02", photos = listOf(photo))
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        assertTrue(viewModel.uiState.value.hasPhoto)
        assertEquals(CardLayout.PHOTO, viewModel.uiState.value.layout)

        viewModel.selectLayout(CardLayout.NO_PHOTO)

        assertEquals(CardLayout.NO_PHOTO, viewModel.uiState.value.layout)
        assertEquals(CardLayout.NO_PHOTO, formats.saved.last().layoutOverride)
        assertEquals(null, renderer.rendered.last().photo)
    }

    @Test
    fun `a match without a photo gets the no-photo layout automatically`() {
        val shared = match("2026-03-02")

        val state = viewModel(shared.id, FakeMatchRepository(listOf(shared))).uiState.value

        assertFalse(state.hasPhoto)
        assertEquals(CardLayout.NO_PHOTO, state.layout)
    }

    @Test
    fun `a free user picking a pro theme is offered the upgrade and nothing changes`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)))

        viewModel.selectTheme(CardTheme.COURT)

        assertEquals(UpgradeReason.PRO_THEME, viewModel.uiState.value.upgradeReason)
        assertEquals(CardTheme.DARK, viewModel.uiState.value.format.theme)
        assertEquals(1, renderer.rendered.size)
        assertTrue(formats.saved.isEmpty())

        viewModel.dismissUpgrade()

        assertNull(viewModel.uiState.value.upgradeReason)
    }

    @Test
    fun `a pro user can pick a pro theme and it is remembered`() {
        val shared = match("2026-03-02")
        val viewModel = viewModel(shared.id, FakeMatchRepository(listOf(shared)), isPro = true)

        viewModel.selectTheme(CardTheme.SUNSET)

        assertNull(viewModel.uiState.value.upgradeReason)
        assertEquals(CardTheme.SUNSET, renderer.rendered.last().theme)
        assertEquals(CardTheme.SUNSET, formats.saved.last().theme)
        assertEquals(CardTheme.SUNSET, viewModel.uiState.value.format.theme)
    }

    @Test
    fun `a free user with a stored pro theme sees and renders dark and the choice is kept`() {
        val shared = match("2026-03-02")
        formats.store(CardFormat(theme = CardTheme.COURT))

        val state = viewModel(shared.id, FakeMatchRepository(listOf(shared))).uiState.value

        assertEquals(CardTheme.DARK, renderer.rendered.single().theme)
        assertEquals(CardTheme.DARK, state.format.theme)
        assertTrue(formats.saved.isEmpty())
    }

    @Test
    fun `a pro user with a stored pro theme gets it on the first render`() {
        val shared = match("2026-03-02")
        formats.store(CardFormat(theme = CardTheme.COURT))

        viewModel(shared.id, FakeMatchRepository(listOf(shared)), isPro = true)

        assertEquals(CardTheme.COURT, renderer.rendered.single().theme)
    }

    @Test
    fun `pro removes the wordmark and free keeps it`() {
        val shared = match("2026-03-02")

        viewModel(shared.id, FakeMatchRepository(listOf(shared)), isPro = false)
        viewModel(shared.id, FakeMatchRepository(listOf(shared)), isPro = true)

        assertEquals(listOf("Picklelog", ""), renderer.rendered.map { it.brand })
    }

    @Test
    fun `a pro card carries the insured streak and a free card carries the plain one`() {
        val shared = match("2026-02-17")
        val history = listOf(match("2026-02-03"), match("2026-02-10"), shared)
        val proSince = AppInstant.parse("2026-02-01T00:00:00Z")

        viewModel(shared.id, FakeMatchRepository(history), isPro = false)
        viewModel(shared.id, FakeMatchRepository(history), isPro = true, proSince = proSince)

        assertNull(renderer.rendered[0].streak)
        assertEquals("3-week streak", renderer.rendered[1].streak)
    }
}
