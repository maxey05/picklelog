package com.maxeydev.picklelog.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.common.DisplayNameField
import com.maxeydev.picklelog.ui.common.MascotIllustration

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onNameChanged: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.testTag(OnboardingTestTags.SCREEN)) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MascotIllustration()
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.backup_honesty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(OnboardingTestTags.BACKUP_NOTE),
            )
            DisplayNameField(
                value = state.name,
                onValueChange = onNameChanged,
                onDone = onContinue,
                modifier = Modifier.testTag(OnboardingTestTags.NAME_FIELD),
            )
            if (state.saveFailed) {
                Text(
                    text = stringResource(R.string.onboarding_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(OnboardingTestTags.SAVE_ERROR),
                )
            }
            Button(
                onClick = onContinue,
                enabled = state.canContinue,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(OnboardingTestTags.CONTINUE),
            ) {
                Text(stringResource(R.string.onboarding_continue))
            }
        }
    }
}
