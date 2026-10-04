package com.maxeydev.picklelog.ui.settings

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.maxeydev.picklelog.domain.entitlement.FreeTier
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.theme.PicklelogTheme

private const val DRAWER_WIDTH_FRACTION = 0.84f
private const val SCRIM_ALPHA = 0.48f
private val DRAWER_MAX_WIDTH = 360.dp
private val DRAWER_SHAPE = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)

@Composable
fun SettingsDrawerHost(
    open: Boolean,
    onDismiss: () -> Unit,
    drawer: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BackHandler(enabled = open, onBack = onDismiss)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val drawerWidth = min(maxWidth * DRAWER_WIDTH_FRACTION, DRAWER_MAX_WIDTH)
        content()
        AnimatedVisibility(visible = open, enter = fadeIn(), exit = fadeOut()) {
            val closeLabel = stringResource(R.string.settings_close)
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = closeLabel,
                            onClick = onDismiss,
                        ).testTag(SettingsTestTags.SCRIM),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally { fullWidth -> fullWidth },
            exit = slideOutHorizontally { fullWidth -> fullWidth },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            drawer(Modifier.width(drawerWidth).fillMaxHeight())
        }
    }
}

@Composable
fun SettingsDrawer(
    state: SettingsUiState,
    versionName: String,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.settings_title)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = DRAWER_SHAPE,
        shadowElevation = 12.dp,
        modifier = modifier.semantics { paneTitle = title }.testTag(SettingsTestTags.SCREEN),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
                    ),
        ) {
            DrawerHeader(title = title, onClose = actions.onClose)
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProCard(hasPro = state.hasPro, savedMatches = state.savedMatches, onSeePro = actions.onSeePro)
                SettingsCard {
                    NameRow(
                        displayName = state.displayName,
                        nameDraft = state.nameDraft,
                        canSave = state.canSaveName,
                        onNameChanged = actions.onNameChanged,
                        onSave = actions.onSaveName,
                        onCancel = actions.onNameEditCancelled,
                    )
                    DarkModeRow(darkTheme = state.darkTheme, onDarkThemeChanged = actions.onDarkThemeChanged)
                    ReminderToggle(
                        enabled = state.reminderEnabled,
                        onEnable = actions.onEnableReminder,
                        onDisable = actions.onDisableReminder,
                    )
                    if (state.reminderEnabled) {
                        ReminderTimeRow(time = state.reminderTime, onTimeChanged = actions.onReminderTimeChanged)
                    }
                }
                DataCard(state = state, actions = actions)
                InfoCard(versionName = versionName, actions = actions)
                EraseDataSection(
                    isErasing = state.isErasing,
                    eraseFailed = state.eraseFailed,
                    savedMatches = state.savedMatches,
                    onOpenBackup = actions.onOpenBackup,
                    onEraseConfirmed = actions.onEraseConfirmed,
                    onEraseFailureDismissed = actions.onEraseFailureDismissed,
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    title: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        IconButton(onClick = onClose, modifier = Modifier.testTag(SettingsTestTags.CLOSE)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.settings_close),
            )
        }
    }
}

@Composable
private fun ProCard(
    hasPro: Boolean,
    savedMatches: Int,
    onSeePro: () -> Unit,
) {
    Surface(
        shape = SETTINGS_CARD_SHAPE,
        color = PicklelogTheme.colors.winRow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.PRO_CARD),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_crown),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { }) {
                Text(text = stringResource(R.string.settings_pro_heading), style = MaterialTheme.typography.titleMedium)
                Text(
                    text =
                        if (hasPro) {
                            stringResource(R.string.settings_pro_row_active)
                        } else {
                            stringResource(
                                R.string.settings_pro_row_free,
                                savedMatches.coerceAtMost(FreeTier.MATCH_LIMIT),
                                FreeTier.MATCH_LIMIT,
                            )
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(SettingsTestTags.PRO_STATUS),
                )
            }
            if (!hasPro) {
                MoreInfoLink(
                    description = stringResource(R.string.settings_pro_more_info),
                    onClick = onSeePro,
                    modifier = Modifier.testTag(SettingsTestTags.SEE_PRO),
                )
            }
        }
    }
}

@Composable
private fun DataCard(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    val context = LocalContext.current
    SettingsCard {
        SettingsRow(icon = R.drawable.ic_upload, label = stringResource(R.string.settings_backup_row)) {
            MoreInfoLink(
                description = stringResource(R.string.settings_backup_more_info),
                onClick = actions.onOpenBackup,
                modifier = Modifier.testTag(SettingsTestTags.BACKUP_MORE_INFO),
            )
        }
        SettingsRow(
            icon = R.drawable.ic_delete,
            label = stringResource(R.string.settings_clear_cache),
            modifier =
                Modifier
                    .clickable(enabled = !state.isClearingCache, role = Role.Button, onClick = actions.onClearCache)
                    .testTag(SettingsTestTags.CLEAR_CACHE),
        ) {
            state.cacheBytes?.let { bytes ->
                TrailingValue(
                    text = Formatter.formatShortFileSize(context, bytes),
                    modifier = Modifier.testTag(SettingsTestTags.CACHE_SIZE),
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    versionName: String,
    actions: SettingsActions,
) {
    SettingsCard {
        SettingsRow(
            icon = R.drawable.ic_shield,
            label = stringResource(R.string.settings_privacy),
            modifier =
                Modifier
                    .clickable(role = Role.Button, onClick = actions.onOpenPrivacy)
                    .testTag(SettingsTestTags.PRIVACY_OPEN),
        ) {
            TrailingChevron()
        }
        SettingsRow(
            icon = R.drawable.ic_star,
            label = stringResource(R.string.settings_rate_us),
            modifier =
                Modifier
                    .clickable(role = Role.Button, onClick = actions.onRateUs)
                    .testTag(SettingsTestTags.RATE_US),
        )
        SettingsRow(
            icon = R.drawable.ic_info,
            label = stringResource(R.string.settings_about),
            modifier =
                Modifier
                    .clickable(role = Role.Button, onClick = actions.onOpenAbout)
                    .testTag(SettingsTestTags.ABOUT_OPEN),
        ) {
            TrailingValue(text = stringResource(R.string.settings_version_short, versionName))
        }
    }
}
