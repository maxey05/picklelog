package com.maxeydev.picklelog.ui.share

import com.maxeydev.picklelog.domain.share.CardRatio
import com.maxeydev.picklelog.domain.share.CardTheme
import kotlinx.serialization.Serializable

@Serializable
data class CardEntry(
    val caption: String,
    val values: List<String>,
)

@Serializable
data class CardData(
    val brand: String,
    val displayName: String,
    val meta: String,
    val partner: CardEntry? = null,
    val time: CardEntry? = null,
    val opponents: CardEntry? = null,
    val games: CardEntry? = null,
    val location: String? = null,
    val photo: String? = null,
    val ratio: CardRatio = CardRatio.TALL,
    val theme: CardTheme = CardTheme.DARK,
)
