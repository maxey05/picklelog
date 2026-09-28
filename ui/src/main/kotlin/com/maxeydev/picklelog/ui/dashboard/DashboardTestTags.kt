package com.maxeydev.picklelog.ui.dashboard

object DashboardTestTags {
    const val HEADER = "dashboard_header"
    const val NAME = "dashboard_name"
    const val EMPTY = "dashboard_empty"
    const val FILTERED_EMPTY = "dashboard_filtered_empty"
    const val MATCH_COUNT = "dashboard_match_count"
    const val RECORD = "dashboard_record"
    const val WIN_PERCENT = "dashboard_win_percent"
    const val STREAK = "dashboard_streak"
    const val FILTER_INDICATOR = "dashboard_filter_indicator"
    const val STATS_SCREEN = "stats_screen"
    const val STATS_MATCHES = "stats_matches"
    const val STATS_RECORD = "stats_record"
    const val STATS_WIN_RATE = "stats_win_rate"
    const val STATS_CURRENT_STREAK = "stats_current_streak"
    const val STATS_LONGEST_STREAK = "stats_longest_streak"
    const val STATS_CURRENT_IS_LONGEST = "stats_current_is_longest"
    const val STATS_SINGLES = "stats_singles"
    const val STATS_DOUBLES = "stats_doubles"
    const val STATS_PRO_SECTION = "stats_pro_section"

    fun proRow(index: Int): String = "stats_pro_row_$index"
}
