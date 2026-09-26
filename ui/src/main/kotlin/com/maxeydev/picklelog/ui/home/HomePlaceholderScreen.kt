package com.maxeydev.picklelog.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.match.currentLocale
import com.maxeydev.picklelog.ui.match.formatLabel
import com.maxeydev.picklelog.ui.match.formatMatchDate
import com.maxeydev.picklelog.ui.match.resultLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePlaceholderScreen(
    state: HomePlaceholderUiState,
    onNewMatch: () -> Unit,
    onOpenMatch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewMatch,
                modifier = Modifier.testTag(HomePlaceholderTestTags.NEW_MATCH),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.new_match),
                )
            }
        },
    ) { innerPadding ->
        if (!state.isLoading && state.matches.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.home_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            val locale = currentLocale()
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(items = state.matches, key = { it.id }) { summary ->
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(
                                    R.string.home_match_headline,
                                    resultLabel(summary.result),
                                    formatLabel(summary.format),
                                ),
                            )
                        },
                        supportingContent = { Text(formatMatchDate(summary.date, locale)) },
                        modifier =
                            Modifier
                                .clickable { onOpenMatch(summary.id) }
                                .testTag(HomePlaceholderTestTags.matchRow(summary.id)),
                    )
                }
            }
        }
    }
}
