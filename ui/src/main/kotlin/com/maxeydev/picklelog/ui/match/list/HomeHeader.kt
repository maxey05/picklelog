package com.maxeydev.picklelog.ui.match.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.dashboard.DashboardHeader
import com.maxeydev.picklelog.ui.dashboard.DashboardUiState
import com.maxeydev.picklelog.ui.theme.PicklelogFonts
import com.maxeydev.picklelog.ui.theme.PicklelogTheme
import com.maxeydev.picklelog.ui.theme.StatusBarIcons

private val APP_BAR_MIN_HEIGHT = 56.dp
private val EXPANDED_SEARCH_TOP_PADDING = 16.dp
private val COLLAPSED_SEARCH_TOP_PADDING = 12.dp
private val HEADER_HORIZONTAL_PADDING = 20.dp
private const val DIVIDER_ALPHA = 0.22f

@Composable
fun HomeHeader(
    dashboard: DashboardUiState,
    isCollapsed: Boolean,
    showDetails: Boolean,
    searchText: String,
    onSearchChanged: (String) -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = PicklelogTheme.colors
    val searchTopPadding = if (isCollapsed) COLLAPSED_SEARCH_TOP_PADDING else EXPANDED_SEARCH_TOP_PADDING
    StatusBarIcons(useLightIcons = true)
    val gradient = Brush.verticalGradient(listOf(colors.headerTop, colors.headerBottom))
    Surface(
        color = Color.Transparent,
        contentColor = colors.onHeader,
        modifier = modifier.fillMaxWidth().background(gradient),
    ) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(bottom = if (showDetails) 20.dp else 0.dp),
        ) {
            if (!isCollapsed) {
                HomeAppBar(onOpenSettings = onOpenSettings)
                if (showDetails) {
                    HeaderDivider(color = colors.onHeader.copy(alpha = DIVIDER_ALPHA))
                }
            }
            if (showDetails) {
                DashboardHeader(
                    state = dashboard,
                    isCollapsed = isCollapsed,
                    onOpenStats = onOpenStats,
                    modifier =
                        Modifier
                            .padding(horizontal = HEADER_HORIZONTAL_PADDING)
                            .padding(top = if (isCollapsed) 12.dp else 0.dp),
                )
                MatchSearchBar(
                    text = searchText,
                    onTextChanged = onSearchChanged,
                    modifier =
                        Modifier
                            .padding(horizontal = HEADER_HORIZONTAL_PADDING)
                            .padding(top = searchTopPadding),
                )
            }
        }
    }
}

@Composable
private fun HeaderDivider(color: Color) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = HEADER_HORIZONTAL_PADDING)
                .padding(top = 8.dp, bottom = 16.dp)
                .height(1.dp)
                .background(color),
    )
}

@Composable
private fun HomeAppBar(onOpenSettings: (() -> Unit)?) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = APP_BAR_MIN_HEIGHT)
                .padding(start = HEADER_HORIZONTAL_PADDING, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style =
                TextStyle(
                    fontFamily = PicklelogFonts.wordmark,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                ),
            modifier = Modifier.weight(1f).padding(top = 4.dp).semantics { heading() },
        )
        if (onOpenSettings != null) {
            IconButton(onClick = onOpenSettings, modifier = Modifier.testTag(MatchListTestTags.SETTINGS)) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(R.string.settings_open),
                )
            }
        }
    }
}
