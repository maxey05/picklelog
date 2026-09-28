package com.maxeydev.picklelog.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePreviewScreen(
    state: SharePreviewUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(ShareTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_title)) },
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
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val card = state.card
                when {
                    card != null -> {
                        val image = remember(card) { card.asImageBitmap() }
                        val description = stringResource(R.string.share_preview_description, state.cardDescription)
                        Image(
                            bitmap = image,
                            contentDescription = description,
                            contentScale = ContentScale.Fit,
                            modifier =
                                Modifier
                                    .aspectRatio(card.width.toFloat() / card.height)
                                    .clip(MaterialTheme.shapes.medium)
                                    .testTag(ShareTestTags.PREVIEW),
                        )
                    }
                    state.hasFailed ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.testTag(ShareTestTags.FAILED),
                        ) {
                            Text(
                                text = stringResource(R.string.share_failed),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                            OutlinedButton(onClick = onRetry, modifier = Modifier.testTag(ShareTestTags.RETRY)) {
                                Text(stringResource(R.string.share_retry))
                            }
                        }
                    else ->
                        CircularProgressIndicator(
                            modifier =
                                Modifier
                                    .testTag(ShareTestTags.RENDERING)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                        )
                }
            }
            if (state.hasShareFailed) {
                Text(
                    text = stringResource(R.string.share_send_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Button(
                onClick = onShare,
                enabled = state.card != null,
                modifier = Modifier.fillMaxWidth().testTag(ShareTestTags.SHARE),
            ) {
                Icon(painter = painterResource(R.drawable.ic_share), contentDescription = null)
                Text(text = stringResource(R.string.share_action), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
