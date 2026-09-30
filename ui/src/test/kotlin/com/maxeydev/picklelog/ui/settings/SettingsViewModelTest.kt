@file:OptIn(ExperimentalCoroutinesApi::class)

package com.maxeydev.picklelog.ui.settings

import com.maxeydev.picklelog.domain.billing.RestoreOutcome
import com.maxeydev.picklelog.domain.billing.StoreProblem
import com.maxeydev.picklelog.domain.datetime.AppInstant
import com.maxeydev.picklelog.domain.reminder.StreakReminder
import com.maxeydev.picklelog.domain.streak.InsuredStreakEngine
import com.maxeydev.picklelog.ui.fakes.FakeBackupRepository
import com.maxeydev.picklelog.ui.fakes.FakeEntitlementRepository
import com.maxeydev.picklelog.ui.fakes.FakeProStore
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

    private fun viewModel(entitlementRepository: FakeEntitlementRepository = entitlements) =
        SettingsViewModel(store, entitlementRepository, backup, reminderStore, streakReminder)

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
}
