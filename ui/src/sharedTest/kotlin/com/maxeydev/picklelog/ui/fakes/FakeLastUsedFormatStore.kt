package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchFormat

class FakeLastUsedFormatStore(
    var format: MatchFormat = LastUsedFormatStore.FIRST_LAUNCH_FORMAT,
    var cached: MatchFormat? = null,
) : LastUsedFormatStore {
    val recorded = mutableListOf<MatchFormat>()

    override suspend fun lastUsedFormat(): MatchFormat = format

    override fun cachedLastUsedFormat(): MatchFormat? = cached

    override suspend fun recordLastUsedFormat(format: MatchFormat) {
        recorded += format
        this.format = format
    }
}
