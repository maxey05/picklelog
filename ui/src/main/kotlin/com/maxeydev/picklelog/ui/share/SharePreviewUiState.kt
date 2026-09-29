package com.maxeydev.picklelog.ui.share

import android.graphics.Bitmap
import com.maxeydev.picklelog.domain.share.CardFormat
import com.maxeydev.picklelog.domain.share.CardLayout

data class SharePreviewUiState(
    val isRendering: Boolean = true,
    val card: Bitmap? = null,
    val cardDescription: String = "",
    val hasFailed: Boolean = false,
    val isGone: Boolean = false,
    val hasShareFailed: Boolean = false,
    val format: CardFormat = CardFormat.DEFAULT,
    val hasPhoto: Boolean = false,
) {
    val layout: CardLayout
        get() = format.layoutFor(hasPhoto)

    val canShare: Boolean
        get() = card != null && !isRendering
}
