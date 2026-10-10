package com.maxeydev.picklelog.ui.paywall

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.common.Mascot
import com.maxeydev.picklelog.ui.common.MascotSheet
import com.maxeydev.picklelog.ui.sound.Cue

@Composable
fun ProUnlockedSheet(
    restored: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MascotSheet(
        mascot = Mascot.BALL_BUDDY,
        title = stringResource(if (restored) R.string.store_restored else R.string.store_unlocked),
        body = stringResource(R.string.pro_unlocked_body),
        onDismiss = onDismiss,
        sheetTag = PaywallTestTags.UNLOCKED_SHEET,
        entranceCue = if (restored) Cue.PRO_RESTORED else Cue.PRO_UNLOCKED,
        modifier = modifier,
    )
}
