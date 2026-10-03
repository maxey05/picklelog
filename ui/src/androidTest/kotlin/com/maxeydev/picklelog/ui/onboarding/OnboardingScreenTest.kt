package com.maxeydev.picklelog.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
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
class OnboardingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var continues = 0
    private val typed = mutableListOf<String>()

    private fun show(state: OnboardingUiState) {
        compose.setContent {
            OnboardingScreen(
                state = state,
                onNameChanged = { typed += it },
                onContinue = { continues++ },
            )
        }
    }

    @Test
    fun the_screen_says_automatic_backup_may_not_be_running() {
        show(OnboardingUiState())

        compose.onNodeWithTag(OnboardingTestTags.BACKUP_NOTE).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun continue_is_disabled_until_a_name_is_entered() {
        show(OnboardingUiState(name = ""))

        compose.onNodeWithTag(OnboardingTestTags.CONTINUE).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun a_blank_name_still_cannot_continue() {
        show(OnboardingUiState(name = "   "))

        compose.onNodeWithTag(OnboardingTestTags.CONTINUE).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun a_name_enables_continue_and_tapping_it_continues() {
        show(OnboardingUiState(name = "Matthew"))

        compose.onNodeWithTag(OnboardingTestTags.CONTINUE).performScrollTo().assertIsEnabled().performClick()

        assertEquals(1, continues)
    }

    @Test
    fun typing_reports_the_text_to_the_view_model() {
        show(OnboardingUiState())

        compose.onNodeWithTag(OnboardingTestTags.NAME_FIELD).performScrollTo().performTextInput("Sam")

        assertEquals(listOf("Sam"), typed)
    }

    @Test
    fun a_failed_save_is_reported() {
        show(OnboardingUiState(name = "Matthew", saveFailed = true))

        compose.onNodeWithTag(OnboardingTestTags.SAVE_ERROR).performScrollTo().assertIsDisplayed()
    }
}
