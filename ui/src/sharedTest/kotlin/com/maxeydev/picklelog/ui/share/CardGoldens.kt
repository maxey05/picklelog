package com.maxeydev.picklelog.ui.share

import com.maxeydev.picklelog.domain.share.CardLayout
import com.maxeydev.picklelog.domain.share.CardRatio
import com.maxeydev.picklelog.domain.share.CardTheme

enum class GoldenContent {
    STANDARD,
    LONG_NAMES,
    EMOJI_NAMES,
}

data class CardGolden(
    val name: String,
    val ratio: CardRatio,
    val theme: CardTheme,
    val layout: CardLayout,
    val content: GoldenContent = GoldenContent.STANDARD,
)

val CARD_GOLDENS: List<CardGolden> =
    listOf(
        CardGolden("tall-dark-photo", CardRatio.TALL, CardTheme.DARK, CardLayout.PHOTO),
        CardGolden("tall-dark-no-photo", CardRatio.TALL, CardTheme.DARK, CardLayout.NO_PHOTO),
        CardGolden("tall-light-photo", CardRatio.TALL, CardTheme.LIGHT, CardLayout.PHOTO),
        CardGolden("tall-light-no-photo", CardRatio.TALL, CardTheme.LIGHT, CardLayout.NO_PHOTO),
        CardGolden("square-dark-photo", CardRatio.SQUARE, CardTheme.DARK, CardLayout.PHOTO),
        CardGolden("square-dark-no-photo", CardRatio.SQUARE, CardTheme.DARK, CardLayout.NO_PHOTO),
        CardGolden("square-light-photo", CardRatio.SQUARE, CardTheme.LIGHT, CardLayout.PHOTO),
        CardGolden("square-light-no-photo", CardRatio.SQUARE, CardTheme.LIGHT, CardLayout.NO_PHOTO),
        CardGolden(
            "tall-dark-photo-long-names",
            CardRatio.TALL,
            CardTheme.DARK,
            CardLayout.PHOTO,
            GoldenContent.LONG_NAMES,
        ),
        CardGolden(
            "tall-dark-photo-emoji-names",
            CardRatio.TALL,
            CardTheme.DARK,
            CardLayout.PHOTO,
            GoldenContent.EMOJI_NAMES,
        ),
    )
