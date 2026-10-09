@file:OptIn(ExperimentalMaterial3Api::class)

package com.maxeydev.picklelog.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.dashboard.motionScale
import com.maxeydev.picklelog.ui.theme.PicklelogSpacing
import com.maxeydev.picklelog.ui.theme.PicklelogTextStyles
import com.maxeydev.picklelog.ui.theme.PicklelogTheme

private val STAGE_HEIGHT = 196.dp
private val MASCOT_HEIGHT = 172.dp
private val ACTION_MIN_HEIGHT = 52.dp
private val SECONDARY_MIN_HEIGHT = 48.dp
private val TITLE_ICON_SIZE = 28.dp
private val ENTRANCE_RISE = 40.dp
private const val ENTRANCE_MILLIS = 600
private const val ENTRANCE_START_SCALE = 0.7f
private val EntranceEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

object MascotSheetTestTags {
    const val DISMISS = "mascot_sheet_dismiss"
    const val SECONDARY = "mascot_sheet_secondary"
}

@Composable
fun MascotSheet(
    mascot: Mascot,
    title: String,
    body: String,
    onDismiss: () -> Unit,
    sheetTag: String,
    modifier: Modifier = Modifier,
    @DrawableRes titleIcon: Int? = null,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.testTag(sheetTag),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PicklelogSpacing.xl)
                    .padding(bottom = PicklelogSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(STAGE_HEIGHT)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                EnteringMascot(
                    mascot = mascot,
                    modifier = Modifier.height(MASCOT_HEIGHT).padding(bottom = PicklelogSpacing.sm),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PicklelogSpacing.sm),
                modifier = Modifier.padding(top = PicklelogSpacing.lg),
            ) {
                if (titleIcon != null) {
                    Icon(
                        painter = painterResource(titleIcon),
                        contentDescription = null,
                        tint = PicklelogTheme.colors.streakFlame,
                        modifier = Modifier.size(TITLE_ICON_SIZE),
                    )
                }
                Text(
                    text = title,
                    style = PicklelogTextStyles.display,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = PicklelogSpacing.sm),
            )
            Button(
                onClick = onDismiss,
                shape = CircleShape,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = ACTION_MIN_HEIGHT)
                        .padding(top = PicklelogSpacing.xl)
                        .testTag(MascotSheetTestTags.DISMISS),
            ) {
                Text(
                    text = stringResource(R.string.mascot_sheet_dismiss),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (secondaryLabel != null) {
                TextButton(
                    onClick = onSecondary,
                    modifier =
                        Modifier
                            .heightIn(min = SECONDARY_MIN_HEIGHT)
                            .padding(top = PicklelogSpacing.xs)
                            .testTag(MascotSheetTestTags.SECONDARY),
                ) {
                    Text(text = secondaryLabel, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun EnteringMascot(
    mascot: Mascot,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (motionScale() > 0f) {
            progress.animateTo(1f, tween(durationMillis = ENTRANCE_MILLIS, easing = EntranceEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    MascotImage(
        mascot = mascot,
        modifier =
            modifier.graphicsLayer {
                val fraction = progress.value
                alpha = fraction
                translationY = (1f - fraction) * ENTRANCE_RISE.toPx()
                val scale = lerp(ENTRANCE_START_SCALE, 1f, fraction)
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
    )
}
