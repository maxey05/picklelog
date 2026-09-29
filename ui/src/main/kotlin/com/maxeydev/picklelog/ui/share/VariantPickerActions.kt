package com.maxeydev.picklelog.ui.share

import com.maxeydev.picklelog.domain.share.CardLayout
import com.maxeydev.picklelog.domain.share.CardRatio
import com.maxeydev.picklelog.domain.share.CardTheme

data class VariantPickerActions(
    val onRatioSelected: (CardRatio) -> Unit,
    val onThemeSelected: (CardTheme) -> Unit,
    val onLayoutSelected: (CardLayout) -> Unit,
)
