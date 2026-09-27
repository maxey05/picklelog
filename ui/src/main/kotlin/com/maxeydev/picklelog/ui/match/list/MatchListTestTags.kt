package com.maxeydev.picklelog.ui.match.list

import com.maxeydev.picklelog.domain.match.MatchSort

object MatchListTestTags {
    const val NEW_MATCH = "match_list_new_match"
    const val LIST = "match_list"
    const val EMPTY_STATE = "match_list_empty"
    const val EMPTY_LOG_MATCH = "match_list_empty_log_match"
    const val SORT_BUTTON = "match_list_sort"
    const val RESULT_BADGE = "match_row_result"
    const val TEXT_COLUMN = "match_row_text"
    const val HEADLINE = "match_row_headline"
    const val DETAILS = "match_row_details"
    const val SCORES = "match_row_scores"
    const val SCORE_ENTRY = "match_row_score_entry"
    const val THUMBNAIL = "match_row_thumbnail"

    fun row(id: String): String = "match_row_$id"

    fun sortOption(sort: MatchSort): String = "match_list_sort_${sort.name.lowercase()}"
}
