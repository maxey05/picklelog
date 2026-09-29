package com.maxeydev.picklelog.domain.share

enum class CardRatio {
    TALL,
    SQUARE,
}

enum class CardTheme {
    DARK,
    LIGHT,
}

enum class CardLayout {
    PHOTO,
    NO_PHOTO,
}

data class CardFormat(
    val ratio: CardRatio = CardRatio.TALL,
    val theme: CardTheme = CardTheme.DARK,
    val layoutOverride: CardLayout? = null,
) {
    fun layoutFor(hasPhoto: Boolean): CardLayout =
        when {
            !hasPhoto -> CardLayout.NO_PHOTO
            else -> layoutOverride ?: CardLayout.PHOTO
        }

    companion object {
        val DEFAULT: CardFormat = CardFormat()
    }
}
