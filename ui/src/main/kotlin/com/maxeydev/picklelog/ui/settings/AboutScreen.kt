package com.maxeydev.picklelog.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(AboutTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(AboutTestTags.VERSION),
            )
            Text(
                text = stringResource(R.string.about_privacy_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp).semantics { heading() },
            )
            Text(
                text = stringResource(R.string.about_privacy_body),
                modifier = Modifier.testTag(AboutTestTags.PRIVACY),
            )
            Text(
                text = stringResource(R.string.about_licences_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp).semantics { heading() },
            )
            Column(
                modifier = Modifier.testTag(AboutTestTags.LICENCES),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OPEN_SOURCE_LIBRARIES.forEach { library ->
                    Column {
                        Text(text = library.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = library.license,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
