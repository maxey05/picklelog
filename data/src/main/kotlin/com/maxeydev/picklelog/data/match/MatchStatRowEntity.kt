package com.maxeydev.picklelog.data.match

import androidx.room.ColumnInfo
import com.maxeydev.picklelog.domain.match.MatchFormat
import com.maxeydev.picklelog.domain.match.MatchResult
import com.maxeydev.picklelog.domain.stats.MatchStatLine
import kotlinx.datetime.LocalDate

data class MatchStatRowEntity(
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "format")
    val format: String,
    @ColumnInfo(name = "result")
    val result: String,
)

internal fun MatchStatRowEntity.toDomain(): MatchStatLine =
    MatchStatLine(
        date = date,
        format = MatchFormat.valueOf(format),
        result = MatchResult.valueOf(result),
    )
