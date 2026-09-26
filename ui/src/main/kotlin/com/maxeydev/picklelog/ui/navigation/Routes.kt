package com.maxeydev.picklelog.ui.navigation

import kotlinx.serialization.Serializable

const val MATCH_ID_ARGUMENT = "matchId"

@Serializable
data object HomeRoute

@Serializable
data class MatchEditRoute(
    val matchId: String? = null,
)

@Serializable
data class MatchDetailRoute(
    val matchId: String,
)
