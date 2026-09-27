package com.maxeydev.picklelog.ui.match.list

import com.maxeydev.picklelog.domain.match.MatchSort

data class MatchListUiState(
    val isLoading: Boolean = true,
    val sort: MatchSort = MatchSort.DEFAULT,
    val matches: List<MatchRowUiState> = emptyList(),
    val pageLimit: Int = 0,
) {
    val canLoadMore: Boolean
        get() = pageLimit > 0 && matches.size >= pageLimit

    val isEmpty: Boolean
        get() = !isLoading && matches.isEmpty()
}
