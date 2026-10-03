package com.maxeydev.picklelog.ui.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val darkChoices = mutableListOf<Boolean>()
    private var saves = 0
    private var eraseConfirmations = 0
    private var aboutOpens = 0
    private var backupOpens = 0

    private val actions =
        SettingsActions(
            onBack = {},
            onSeePro = {},
            onRestore = {},
            onOpenBackup = { backupOpens++ },
            onOpenAbout = { aboutOpens++ },
            onEnableReminder = {},
            onDisableReminder = {},
            onReminderTimeChanged = {},
            onNameChanged = {},
            onSaveName = { saves++ },
            onDarkThemeChanged = { darkChoices += it },
            onEraseConfirmed = { eraseConfirmations++ },
            onEraseFailureDismissed = {},
        )

    private fun show(state: SettingsUiState) {
        compose.setContent { SettingsScreen(state = state, actions = actions) }
    }

    @Test
    fun the_dark_mode_switch_reflects_an_explicit_dark_choice() {
        show(SettingsUiState(darkTheme = true))

        compose.onNodeWithTag(SettingsTestTags.DARK_THEME_TOGGLE).performScrollTo().assertIsOn()
    }

    @Test
    fun the_dark_mode_switch_reflects_an_explicit_light_choice() {
        show(SettingsUiState(darkTheme = false))

        compose.onNodeWithTag(SettingsTestTags.DARK_THEME_TOGGLE).performScrollTo().assertIsOff()
    }

    @Test
    fun flipping_the_switch_reports_the_new_choice() {
        show(SettingsUiState(darkTheme = false))

        compose.onNodeWithTag(SettingsTestTags.DARK_THEME_TOGGLE).performScrollTo().performClick()

        assertEquals(listOf(true), darkChoices)
    }

    @Test
    fun saving_the_name_is_disabled_when_there_is_nothing_new_to_save() {
        show(SettingsUiState(displayName = "Matthew", nameDraft = "Matthew"))

        compose.onNodeWithTag(SettingsTestTags.NAME_SAVE).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun an_edited_name_can_be_saved() {
        show(SettingsUiState(displayName = "Matthew", nameDraft = "Matt"))

        compose.onNodeWithTag(SettingsTestTags.NAME_SAVE).performScrollTo().assertIsEnabled().performClick()

        assertEquals(1, saves)
    }

    @Test
    fun a_blank_name_cannot_be_saved() {
        show(SettingsUiState(displayName = "Matthew", nameDraft = ""))

        compose.onNodeWithTag(SettingsTestTags.NAME_SAVE).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun the_reminder_time_is_only_offered_while_the_reminder_is_on() {
        show(SettingsUiState(reminderEnabled = false))
        compose.onAllNodesWithTag(SettingsTestTags.REMINDER_TIME).assertCountEquals(0)
    }

    @Test
    fun the_reminder_time_appears_once_the_reminder_is_on() {
        show(SettingsUiState(reminderEnabled = true))

        compose.onNodeWithTag(SettingsTestTags.REMINDER_TIME).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun about_opens_from_settings() {
        show(SettingsUiState())

        compose.onNodeWithTag(SettingsTestTags.ABOUT_OPEN).performScrollTo().performClick()

        assertEquals(1, aboutOpens)
    }

    @Test
    fun erasing_needs_the_confirmation_word_before_it_can_proceed() {
        show(SettingsUiState())

        compose.onNodeWithTag(SettingsTestTags.ERASE_OPEN).performScrollTo().performClick()
        compose.onNodeWithTag(SettingsTestTags.ERASE_CONFIRM).assertIsNotEnabled()

        compose.onNodeWithTag(SettingsTestTags.ERASE_CONFIRM_FIELD).performTextInput("nope")
        compose.onNodeWithTag(SettingsTestTags.ERASE_CONFIRM).assertIsNotEnabled()
        assertEquals(0, eraseConfirmations)
    }

    @Test
    fun typing_the_word_enables_the_erase_and_confirming_reports_it() {
        show(SettingsUiState())
        compose.onNodeWithTag(SettingsTestTags.ERASE_OPEN).performScrollTo().performClick()

        compose.onNodeWithTag(SettingsTestTags.ERASE_CONFIRM_FIELD).performTextInput("erase")
        compose.onNodeWithTag(SettingsTestTags.ERASE_CONFIRM).assertIsEnabled().performClick()

        assertEquals(1, eraseConfirmations)
    }

    @Test
    fun the_erase_dialog_offers_an_export_first() {
        show(SettingsUiState())
        compose.onNodeWithTag(SettingsTestTags.ERASE_OPEN).performScrollTo().performClick()

        compose.onNodeWithTag(SettingsTestTags.ERASE_EXPORT_FIRST).performClick()

        assertEquals(1, backupOpens)
    }

    @Test
    fun a_failed_erase_is_explained_inside_the_dialog() {
        show(SettingsUiState(eraseFailed = true))
        compose.onNodeWithTag(SettingsTestTags.ERASE_OPEN).performScrollTo().performClick()

        compose.onNodeWithTag(SettingsTestTags.ERASE_ERROR).assertIsDisplayed()
    }
}
