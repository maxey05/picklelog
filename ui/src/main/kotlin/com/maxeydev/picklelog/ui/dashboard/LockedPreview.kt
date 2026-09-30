@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R
import kotlin.uuid.ExperimentalUuidApi

private const val MASK_ALPHA = 0.28f
private val MASK_WIDTH = 72.dp
private val MASK_HEIGHT = 20.dp
private val PREVIEW_ICON_SIZE = 24.dp
private val PREVIEW_SHAPE = RoundedCornerShape(12.dp)

@Composable
fun LockedPreview(
    state: DashboardUiState,
    onSeePro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = state.advanced.topOpponent ?: return
    val name = state.nameOf(top.personId) ?: return
    val description = stringResource(R.string.stats_preview_a11y, name)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MIN_ROW_HEIGHT)
                .clip(PREVIEW_SHAPE)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(role = Role.Button, onClick = onSeePro)
                .padding(16.dp)
                .testTag(DashboardTestTags.STATS_LOCKED_PREVIEW)
                .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(PREVIEW_ICON_SIZE),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.stats_preview_title, name),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(DashboardTestTags.STATS_LOCKED_PREVIEW_NAME),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier =
                        Modifier
                            .size(width = MASK_WIDTH, height = MASK_HEIGHT)
                            .clip(RoundedCornerShape(MASK_HEIGHT / 2))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = MASK_ALPHA))
                            .testTag(DashboardTestTags.STATS_LOCKED_PREVIEW_RECORD)
                            .clearAndSetSemantics {},
                )
                Text(
                    text = stringResource(R.string.stats_preview_cta),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
