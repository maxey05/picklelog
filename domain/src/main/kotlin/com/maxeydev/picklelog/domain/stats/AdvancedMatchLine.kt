@file:OptIn(ExperimentalUuidApi::class)

package com.maxeydev.picklelog.domain.stats

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class AdvancedMatchLine(
    val date: AppDate,
    val format: MatchFormat,
    val result: MatchResult,
    val opponentIds: List<Uuid> = emptyList(),
    val partnerId: Uuid? = null,
    val location: String? = null,
    val paddle: String? = null,
)
