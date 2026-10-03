package com.maxeydev.picklelog.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.ui.R

@Composable
fun MascotIllustration(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.illustration_mascot),
        contentDescription = null,
        modifier = modifier.size(width = 208.dp, height = 160.dp),
    )
}
