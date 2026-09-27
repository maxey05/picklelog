package com.maxeydev.picklelog.domain.match

import org.junit.Assert.assertEquals
import org.junit.Test

class MatchSortTest {
    @Test
    fun `a user who never chose a sort sees newest matches first`() {
        assertEquals(MatchSort.DATE_NEWEST, MatchSort.DEFAULT)
    }

    @Test
    fun `exactly the five core sorts are offered`() {
        assertEquals(
            listOf(
                MatchSort.DATE_NEWEST,
                MatchSort.DATE_OLDEST,
                MatchSort.RESULT_WINS_FIRST,
                MatchSort.RESULT_LOSSES_FIRST,
                MatchSort.OPPONENT_A_TO_Z,
            ),
            MatchSort.entries.toList(),
        )
    }
}
