package com.maxeydev.picklelog.ui.match

import com.maxeydev.picklelog.domain.datetime.AppDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchDatesTest {
    @Test
    fun `a date survives the round trip through the date picker's utc millis`() {
        listOf("2026-09-24", "2026-01-01", "2024-02-29", "2026-12-31").forEach { value ->
            val date = AppDate.parse(value)
            assertEquals(date, utcEpochMillisToAppDate(date.toUtcEpochMillis()))
        }
    }

    @Test
    fun `utc midnight maps to that calendar day`() {
        assertEquals(1_790_208_000_000L, AppDate.parse("2026-09-24").toUtcEpochMillis())
    }
}
