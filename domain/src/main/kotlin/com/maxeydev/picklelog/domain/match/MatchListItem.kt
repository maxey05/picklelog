@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.domain.match

import com.maxeydev.picklelog.domain.datetime.AppDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class MatchListItem(
    val id: Uuid,
    val date: AppDate,
    val format: MatchFormat,
    val result: MatchResult,
    val opponentNames: List<String>,
    val games: List<GameScore>,
    val primaryPhotoPath: String?,
)
