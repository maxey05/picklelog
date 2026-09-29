package com.maxeydev.picklelog.ui.share

import com.maxeydev.picklelog.domain.share.CardLayout
import com.maxeydev.picklelog.domain.share.CardRatio
import com.maxeydev.picklelog.domain.share.CardTheme

object ShareTestTags {
    const val SCREEN = "share_screen"
    const val PREVIEW = "share_preview"
    const val RENDERING = "share_rendering"
    const val FAILED = "share_failed"
    const val RETRY = "share_retry"
    const val SHARE = "share_send"
    const val VARIANT_PICKER = "share_variant_picker"

    fun ratio(ratio: CardRatio): String = "share_ratio_${ratio.name}"

    fun theme(theme: CardTheme): String = "share_theme_${theme.name}"

    fun layout(layout: CardLayout): String = "share_layout_${layout.name}"
}
