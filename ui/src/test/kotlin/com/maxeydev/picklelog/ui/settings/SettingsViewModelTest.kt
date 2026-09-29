@file:OptIn(ExperimentalCoroutinesApi::class)

package com.maxeydev.picklelog.ui.settings

import com.maxeydev.picklelog.domain.billing.RestoreOutcome
import com.maxeydev.picklelog.domain.billing.StoreProblem
import com.maxeydev.picklelog.ui.fakes.FakeEntitlementRepository
import com.maxeydev.picklelog.ui.fakes.FakeProStore
import com.maxeydev.picklelog.ui.paywall.StoreMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {
    private val entitlements = FakeEntitlementRepository()
    private val store = FakeProStore(entitlements)

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
        val viewModel = SettingsViewModel(store, entitlements)
        assertFalse(viewModel.uiState.value.hasPro)

        viewModel.restore()

        assertTrue(viewModel.uiState.value.hasPro)
        assertEquals(StoreMessage.RESTORED, viewModel.uiState.value.restoreMessage)
        assertEquals(1, store.restoreAttempts)
    }

    @Test
    fun `offline restore never tells the user they lost anything`() {
        store.restoreOutcome = RestoreOutcome.CouldNotCheck(StoreProblem.OFFLINE)
        val viewModel = SettingsViewModel(store, entitlements)

        viewModel.restore()

        assertEquals(StoreMessage.OFFLINE, viewModel.uiState.value.restoreMessage)
        assertFalse(viewModel.uiState.value.isRestoring)
    }

    @Test
    fun `a pro user sees pro as unlocked`() {
        val viewModel = SettingsViewModel(store, FakeEntitlementRepository(isPro = true))

        assertTrue(viewModel.uiState.value.hasPro)
    }
}
