package com.maxeydev.picklelog.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.common.Mascot
import com.maxeydev.picklelog.ui.common.MascotImage
import com.maxeydev.picklelog.ui.dashboard.motionScale
import kotlinx.coroutines.delay

private const val WAVE_START_DELAY_MILLIS = 250L
private const val WAVE_STEP_MILLIS = 150
private const val WAVE_DEGREES = 7f
private val MASCOT_TOP_GAP = 16.dp
private val MASCOT_BOTTOM_GAP = 8.dp
private val MASCOT_SIDE_GAP = 48.dp

@Composable
internal fun PrivacyMascot(
    topInset: Dp,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(IntroPage.PRIVACY.illustrationDescription)
    var hasWaved by rememberSaveable { mutableStateOf(false) }
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!hasWaved && motionScale() > 0f) {
            delay(WAVE_START_DELAY_MILLIS)
            tilt.animateTo(WAVE_DEGREES, tween(durationMillis = WAVE_STEP_MILLIS))
            tilt.animateTo(-WAVE_DEGREES, tween(durationMillis = WAVE_STEP_MILLIS))
            tilt.animateTo(WAVE_DEGREES / 2f, tween(durationMillis = WAVE_STEP_MILLIS))
            tilt.animateTo(0f, tween(durationMillis = WAVE_STEP_MILLIS))
        }
        hasWaved = true
    }
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        MascotImage(
            mascot = Mascot.READY,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = MASCOT_SIDE_GAP)
                    .padding(top = topInset + MASCOT_TOP_GAP, bottom = MASCOT_BOTTOM_GAP)
                    .graphicsLayer {
                        rotationZ = tilt.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
        )
    }
}
