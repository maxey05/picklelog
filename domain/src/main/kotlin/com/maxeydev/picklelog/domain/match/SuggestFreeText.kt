package com.maxeydev.picklelog.domain.match

import com.maxeydev.picklelog.domain.suggest.MAX_SUGGESTIONS
import com.maxeydev.picklelog.domain.suggest.matchesPrefixOrWordStart
import com.maxeydev.picklelog.domain.suggest.suggestionKey

private val MOST_RECENT_FIRST =
    compareByDescending<FreeTextUsage> { it.lastPlayedOn }
        .thenByDescending { it.lastLoggedAt }
        .thenBy { suggestionKey(it.value) }

fun suggestFreeText(
    query: String,
    candidates: List<FreeTextUsage>,
    limit: Int = MAX_SUGGESTIONS,
): List<String> {
    val queryKey = suggestionKey(query)
    if (queryKey.isEmpty() || limit <= 0) {
        return emptyList()
    }
    return candidates
        .asSequence()
        .filter { it.value.isNotBlank() }
        .sortedWith(MOST_RECENT_FIRST)
        .distinctBy { suggestionKey(it.value) }
        .filter { suggestionKey(it.value) != queryKey }
        .filter { matchesPrefixOrWordStart(suggestionKey(it.value), queryKey) }
        .map { it.value }
        .take(limit)
        .toList()
}
