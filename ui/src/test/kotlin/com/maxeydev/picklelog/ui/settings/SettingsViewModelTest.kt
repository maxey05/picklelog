@file:OptIn(ExperimentalCoroutinesApi::class)

package com.maxeydev.picklelog.ui.settings

import com.maxeydev.picklelog.domain.billing.RestoreOutcome
import com.maxeydev.picklelog.domain.billing.StoreProblem
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.datetime.AppTime
import com.maxeydev.picklelog.domain.erase.EraseAllData
import com.maxeydev.picklelog.domain.profile.DisplayName
import com.maxeydev.picklelog.domain.reminder.StreakReminder
import com.maxeydev.picklelog.domain.streak.InsuredStreakEngine
import com.maxeydev.picklelog.ui.fakes.FakeAppSettingsStore
import com.maxeydev.picklelog.ui.fakes.FakeBackupRepository
import com.maxeydev.picklelog.ui.fakes.FakeEntitlementRepository
import com.maxeydev.picklelog.ui.fakes.FakeLocalDataEraser
import com.maxeydev.picklelog.ui.fakes.FakeProStore
import com.maxeydev.picklelog.ui.fakes.FakeProfileRepository
import com.maxeydev.picklelog.ui.fakes.FakeReminderNotifier
import com.maxeydev.picklelog.ui.fakes.FakeReminderScheduling
import com.maxeydev.picklelog.ui.fakes.FakeReminderStore
import com.maxeydev.picklelog.ui.fakes.FixedClock
import com.maxeydev.picklelog.ui.paywall.StoreMessage
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

class SettingsViewModelTest {
    private val entitlements = FakeEntitlementRepository()
    private val store = FakeProStore(entitlements)
    private val backup = FakeBackupRepository()
    private val reminderStore = FakeReminderStore()
    private val scheduling = FakeReminderScheduling()
    private val clock = FixedClock(AppInstant.parse("2026-09-30T09:00:00Z"))

    private val streakReminder =
        StreakReminder(
            store = reminderStore,
            matchDates = { emptyList() },
            insuranceStart = { null },
            engine = InsuredStreakEngine(clock) { TimeZone.UTC },
            scheduling = scheduling,
            notifier = FakeReminderNotifier(),
            clock = clock,
            timeZone = { TimeZone.UTC },
        )

    private val profile = FakeProfileRepository()
    private val appSettings = FakeAppSettingsStore()
    private val eraser = FakeLocalDataEraser()
    private val eraseAllData = EraseAllData(streakReminder, eraser)

    private fun viewModel(entitlementRepository: FakeEntitlementRepository = entitlements) =
        SettingsViewModel(
            store,
            entitlementRepository,
            backup,
            reminderStore,
            streakReminder,
            profile,
            appSettings,
            eraseAllData,
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `restore is available without any account and unlocks pro`() {
        store.restoreOutcome = RestoreOutcome.Restored
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.hasPro)

        viewModel.restore()

        assertTrue(viewModel.uiState.value.hasPro)
        assertEquals(StoreMessage.RESTORED, viewModel.uiState.value.restoreMessage)
        assertEquals(1, store.restoreAttempts)
    }

    @Test
    fun `offline restore never tells the user they lost anything`() {
        store.restoreOutcome = RestoreOutcome.CouldNotCheck(StoreProblem.OFFLINE)
        val viewModel = viewModel()

        viewModel.restore()

        assertEquals(StoreMessage.OFFLINE, viewModel.uiState.value.restoreMessage)
        assertFalse(viewModel.uiState.value.isRestoring)
    }

    @Test
    fun `settings shows that nothing has been exported until an export happens`() {
        val viewModel = viewModel()

        assertNull(viewModel.uiState.value.lastExportAt)

        val exportedAt = AppInstant.parse("2026-09-30T02:00:00Z")
        backup.setLastExport(exportedAt)

        assertEquals(exportedAt, viewModel.uiState.value.lastExportAt)
    }

    @Test
    fun `a pro user sees pro as unlocked`() {
        val viewModel = viewModel(FakeEntitlementRepository(isPro = true))

        assertTrue(viewModel.uiState.value.hasPro)
    }

    @Test
    fun `the reminder starts switched off`() {
        assertFalse(viewModel().uiState.value.reminderEnabled)
    }

    @Test
    fun `enabling the reminder stores the choice and arms the next friday`() {
        val viewModel = viewModel()

        viewModel.enableReminder()

        assertTrue(viewModel.uiState.value.reminderEnabled)
        assertEquals(listOf(AppInstant.parse("2026-10-02T18:00:00Z")), scheduling.scheduled)
    }

    @Test
    fun `disabling the reminder cancels the scheduled work`() {
        val viewModel = viewModel()
        viewModel.enableReminder()

        viewModel.disableReminder()

        assertFalse(viewModel.uiState.value.reminderEnabled)
        assertEquals(1, scheduling.cancelCount)
    }

    @Test
    fun `a saved name is trimmed and stored on the profile`() {
        val viewModel = viewModel()

        viewModel.changeName("  Matthew  ")
        assertTrue(viewModel.uiState.value.canSaveName)
        viewModel.saveName()

        assertEquals("Matthew", viewModel.uiState.value.displayName)
        assertEquals("Matthew", viewModel.uiState.value.nameDraft)
        assertFalse(viewModel.uiState.value.canSaveName)
    }

    @Test
    fun `a blank name can never be saved`() {
        val viewModel = viewModel()
        viewModel.changeName("Matthew")
        viewModel.saveName()

        viewModel.changeName("   ")
        viewModel.saveName()

        assertFalse(viewModel.uiState.value.canSaveName)
        assertEquals("Matthew", viewModel.uiState.value.displayName)
    }

    @Test
    fun `an unchanged name offers nothing to save`() {
        val viewModel = viewModel()
        viewModel.changeName("Matthew")
        viewModel.saveName()

        viewModel.changeName("Matthew ")

        assertFalse(viewModel.uiState.value.canSaveName)
    }

    @Test
    fun `the name draft is cut at the length limit`() {
        val viewModel = viewModel()

        viewModel.changeName("x".repeat(DisplayName.MAX_LENGTH + 10))

        assertEquals(DisplayName.MAX_LENGTH, viewModel.uiState.value.nameDraft.length)
    }

    @Test
    fun `dark mode follows the system until the user chooses`() {
        val viewModel = viewModel()
        assertNull(viewModel.uiState.value.darkTheme)

        viewModel.changeDarkTheme(true)
        assertEquals(true, viewModel.uiState.value.darkTheme)

        viewModel.changeDarkTheme(false)
        assertEquals(false, viewModel.uiState.value.darkTheme)
    }

    @Test
    fun `changing the reminder time re-arms the next friday at that time`() {
        val viewModel = viewModel()
        viewModel.enableReminder()

        viewModel.changeReminderTime(AppTime(8, 30))

        assertEquals(AppTime(8, 30), viewModel.uiState.value.reminderTime)
        assertEquals(AppInstant.parse("2026-10-02T08:30:00Z"), scheduling.scheduled.last())
    }

    @Test
    fun `changing the time while the reminder is off arms nothing`() {
        val viewModel = viewModel()

        viewModel.changeReminderTime(AppTime(8, 30))

        assertEquals(AppTime(8, 30), viewModel.uiState.value.reminderTime)
        assertTrue(scheduling.scheduled.isEmpty())
    }

    @Test
    fun `erasing all data cancels the reminder, erases and flags completion`() {
        val viewModel = viewModel()
        viewModel.enableReminder()

        viewModel.eraseAll()

        assertEquals(1, eraser.eraseCount)
        assertEquals(1, scheduling.cancelCount)
        assertTrue(viewModel.uiState.value.isErased)
        assertFalse(viewModel.uiState.value.isErasing)
        assertFalse(viewModel.uiState.value.eraseFailed)
    }

    @Test
    fun `erasing all data never revokes pro`() {
        val proEntitlements = FakeEntitlementRepository(isPro = true)
        val viewModel = viewModel(proEntitlements)

        viewModel.eraseAll()

        assertTrue(proEntitlements.current.isPro)
        assertTrue(viewModel.uiState.value.hasPro)
    }

    @Test
    fun `a failed erase reports the failure and does not claim completion`() {
        eraser.failure = IllegalStateException("disk error")
        val viewModel = viewModel()

        viewModel.eraseAll()

        assertTrue(viewModel.uiState.value.eraseFailed)
        assertFalse(viewModel.uiState.value.isErased)
        assertFalse(viewModel.uiState.value.isErasing)
        viewModel.dismissEraseFailure()
        assertFalse(viewModel.uiState.value.eraseFailed)
    }
}
