package com.maxeydev.picklelog.ui.match

import com.maxeydev.picklelog.domain.datetime.AppDate
import com.maxeydev.picklelog.domain.datetime.AppTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant

fun AppDate.toUtcEpochMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun utcEpochMillisToAppDate(epochMillis: Long): AppDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.UTC).date

fun formatMatchDate(
    date: AppDate,
    locale: Locale,
): String = date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

fun formatMatchTime(
    time: AppTime,
    locale: Locale,
): String = time.toJavaLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
