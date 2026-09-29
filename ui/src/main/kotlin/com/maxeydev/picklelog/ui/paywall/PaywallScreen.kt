package com.maxeydev.picklelog.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.entitlement.FreeTier
import com.maxeydev.picklelog.ui.R

private val MINIMUM_TOUCH_TARGET = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    state: PaywallUiState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onRetryPrice: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(PaywallTestTags.PAYWALL),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.paywall_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag(PaywallTestTags.PAYWALL_CLOSE)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.paywall_close),
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.paywall_headline),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            RecordSummary(state)
            Text(stringResource(R.string.paywall_data_is_yours))
            Text(
                text = stringResource(R.string.paywall_benefits_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.paywall_benefit_matches, FreeTier.MATCH_LIMIT))
            Text(stringResource(R.string.paywall_benefit_photos))
            PriceTerms(state = state, onRetryPrice = onRetryPrice)
            state.message?.let { message ->
                Text(
                    text = stringResource(message.text),
                    color =
                        if (message == StoreMessage.UNLOCKED) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier =
                        Modifier
                            .testTag(PaywallTestTags.STORE_MESSAGE)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Button(
                onClick = onBuy,
                enabled = state.canBuy,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MINIMUM_TOUCH_TARGET)
                        .testTag(PaywallTestTags.PAYWALL_BUY),
            ) {
                Text(
                    state.price?.let { stringResource(R.string.paywall_buy_with_price, it) }
                        ?: stringResource(R.string.paywall_buy),
                )
            }
            OutlinedButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MINIMUM_TOUCH_TARGET)
                        .testTag(PaywallTestTags.PAYWALL_NOT_NOW),
            ) {
                Text(stringResource(R.string.paywall_not_now))
            }
            TextButton(
                onClick = onRestore,
                enabled = !state.isWorking,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MINIMUM_TOUCH_TARGET)
                        .testTag(PaywallTestTags.PAYWALL_RESTORE),
            ) {
                Text(stringResource(R.string.paywall_restore))
            }
        }
    }
}

@Composable
private fun RecordSummary(state: PaywallUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag(PaywallTestTags.PAYWALL_RECORD).semantics(mergeDescendants = true) { },
    ) {
        val matches = state.savedMatches
        Text(
            text = pluralStringResource(R.plurals.paywall_record, matches, matches, state.wins, state.losses),
            style = MaterialTheme.typography.titleMedium,
        )
        if (state.streakWeeks > 0) {
            Text(pluralStringResource(R.plurals.paywall_streak, state.streakWeeks, state.streakWeeks))
        }
    }
}

@Composable
private fun PriceTerms(
    state: PaywallUiState,
    onRetryPrice: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text =
                state.price?.let { stringResource(R.string.paywall_price_terms, it) }
                    ?: stringResource(R.string.paywall_price_terms_no_price),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(PaywallTestTags.PAYWALL_PRICE_TERMS),
        )
        if (state.price == null && !state.isLoadingPrice) {
            TextButton(onClick = onRetryPrice, modifier = Modifier.heightIn(min = MINIMUM_TOUCH_TARGET)) {
                Text(stringResource(R.string.paywall_retry_price))
            }
        }
    }
}
