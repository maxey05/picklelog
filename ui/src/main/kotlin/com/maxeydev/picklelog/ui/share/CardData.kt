package com.maxeydev.picklelog.ui.share

import kotlinx.serialization.Serializable

@Serializable
data class CardData(
    val brand: String,
    val displayName: String,
    val meta: String,
    val result: String,
    val isWin: Boolean,
    val opponents: String? = null,
    val partner: String? = null,
    val score: String? = null,
    val location: String? = null,
    val streak: String? = null,
    val photo: String? = null,
)
