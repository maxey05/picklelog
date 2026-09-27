package com.maxeydev.picklelog.domain.match

enum class MatchSort {
    DATE_NEWEST,
    DATE_OLDEST,
    RESULT_WINS_FIRST,
    RESULT_LOSSES_FIRST,
    OPPONENT_A_TO_Z,
    ;

    companion object {
        val DEFAULT: MatchSort = DATE_NEWEST
    }
}
