package com.maxeydev.picklelog.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.common.DisplayNameField
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onNameChanged: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val pages = IntroPage.entries
    val pagerState = rememberPagerState(initialPage = initialPage) { pages.size }
    val scope = rememberCoroutineScope()
    val showPage: (Int) -> Unit = { target -> scope.launch { pagerState.animateScrollToPage(target) } }
    BackHandler(enabled = pagerState.currentPage > 0) { showPage(pagerState.currentPage - 1) }
    Scaffold(
        modifier = modifier.testTag(OnboardingTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .imePadding(),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag(OnboardingTestTags.PAGER),
                verticalAlignment = Alignment.Top,
            ) { index ->
                IntroPageContent(
                    page = pages[index],
                    state = state,
                    onNameChanged = onNameChanged,
                    onContinue = onContinue,
                )
            }
            IntroFooter(
                pagerState = pagerState,
                state = state,
                onNext = { showPage(pagerState.currentPage + 1) },
                onSkip = { showPage(pages.lastIndex) },
                onContinue = onContinue,
            )
        }
    }
}

@Composable
private fun IntroPageContent(
    page: IntroPage,
    state: OnboardingUiState,
    onNameChanged: (String) -> Unit,
    onContinue: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val share = if (page == IntroPage.PRIVACY) 0.38f else 0.56f
        val illustrationHeight: Dp = (maxHeight * share).coerceIn(140.dp, 420.dp)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IntroIllustration(
                page = page,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(illustrationHeight),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(page.title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .semantics { heading() }
                            .testTag(OnboardingTestTags.title(page)),
                )
                Text(
                    text = stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (page == IntroPage.PRIVACY) {
                Spacer(modifier = Modifier.height(20.dp))
                DisplayNameField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    onDone = onContinue,
                    modifier =
                        Modifier
                            .padding(horizontal = 24.dp)
                            .testTag(OnboardingTestTags.NAME_FIELD),
                    required = false,
                )
                if (state.saveFailed) {
                    Text(
                        text = stringResource(R.string.onboarding_save_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier =
                            Modifier
                                .padding(horizontal = 24.dp)
                                .testTag(OnboardingTestTags.SAVE_ERROR),
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroFooter(
    pagerState: PagerState,
    state: OnboardingUiState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    val onLastPage = pagerState.currentPage == pagerState.pageCount - 1
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PageIndicator(
            pageCount = pagerState.pageCount,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(bottom = 20.dp),
        )
        if (onLastPage) {
            Button(
                onClick = onContinue,
                enabled = state.canContinue,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag(OnboardingTestTags.CONTINUE),
            ) {
                Text(text = stringResource(R.string.onboarding_continue), fontWeight = FontWeight.Bold)
            }
            Text(
                text = stringResource(R.string.onboarding_backup_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .padding(top = 14.dp)
                        .testTag(OnboardingTestTags.BACKUP_NOTE),
            )
        } else {
            Button(
                onClick = onNext,
                modifier =
                    Modifier
                        .widthIn(min = 220.dp)
                        .heightIn(min = 52.dp)
                        .testTag(OnboardingTestTags.NEXT),
            ) {
                Text(text = stringResource(R.string.onboarding_next), fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onSkip,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .widthIn(min = 96.dp)
                        .heightIn(min = 48.dp)
                        .testTag(OnboardingTestTags.SKIP),
            ) {
                Text(text = stringResource(R.string.onboarding_skip), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.onboarding_page_indicator, currentPage + 1, pageCount)
    Row(
        modifier =
            modifier
                .semantics(mergeDescendants = true) { contentDescription = description }
                .testTag(OnboardingTestTags.PAGE_INDICATOR),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 22.dp else 8.dp,
                label = "introIndicatorWidth",
            )
            Box(
                modifier =
                    Modifier
                        .size(width = width, height = 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
            )
        }
    }
}
