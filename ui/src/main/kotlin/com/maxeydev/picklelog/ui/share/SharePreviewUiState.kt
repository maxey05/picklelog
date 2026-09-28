package com.maxeydev.picklelog.ui.share

import android.graphics.Bitmap

data class SharePreviewUiState(
    val isRendering: Boolean = true,
    val card: Bitmap? = null,
    val cardDescription: String = "",
    val hasFailed: Boolean = false,
    val isGone: Boolean = false,
    val hasShareFailed: Boolean = false,
)
