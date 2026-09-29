package com.maxeydev.picklelog.ui.share

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklelog.domain.share.CardFormat
import com.maxeydev.picklelog.domain.share.CardLayout
import com.maxeydev.picklelog.domain.share.CardRatio
import com.maxeydev.picklelog.domain.share.CardTheme
import com.maxeydev.picklelog.ui.R

@Composable
fun VariantPicker(
    format: CardFormat,
    layout: CardLayout,
    hasPhoto: Boolean,
    actions: VariantPickerActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag(ShareTestTags.VARIANT_PICKER),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OptionRow(
            title = R.string.card_ratio_label,
            options = CardRatio.entries,
            selected = format.ratio,
            label = ::ratioLabel,
            tag = ShareTestTags::ratio,
            onSelected = actions.onRatioSelected,
        )
        OptionRow(
            title = R.string.card_theme_label,
            options = CardTheme.entries,
            selected = format.theme,
            label = ::themeLabel,
            tag = ShareTestTags::theme,
            onSelected = actions.onThemeSelected,
        )
        if (hasPhoto) {
            OptionRow(
                title = R.string.card_layout_label,
                options = CardLayout.entries,
                selected = layout,
                label = ::layoutLabel,
                tag = ShareTestTags::layout,
                onSelected = actions.onLayoutSelected,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionRow(
    @StringRes title: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    tag: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.semantics { heading() },
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelected(option) },
                label = { Text(stringResource(label(option))) },
                modifier = Modifier.testTag(tag(option)),
            )
        }
    }
}

private fun ratioLabel(ratio: CardRatio): Int =
    when (ratio) {
        CardRatio.TALL -> R.string.card_ratio_tall
        CardRatio.SQUARE -> R.string.card_ratio_square
    }

private fun themeLabel(theme: CardTheme): Int =
    when (theme) {
        CardTheme.DARK -> R.string.card_theme_dark
        CardTheme.LIGHT -> R.string.card_theme_light
    }

private fun layoutLabel(layout: CardLayout): Int =
    when (layout) {
        CardLayout.PHOTO -> R.string.card_layout_photo
        CardLayout.NO_PHOTO -> R.string.card_layout_no_photo
    }
