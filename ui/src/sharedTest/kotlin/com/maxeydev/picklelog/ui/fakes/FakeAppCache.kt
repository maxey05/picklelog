package com.maxeydev.picklelog.ui.fakes

import com.maxeydev.picklelog.domain.settings.AppCache

class FakeAppCache(
    var bytes: Long = 0L,
) : AppCache {
    var clearCount = 0
        private set

    override suspend fun sizeBytes(): Long = bytes

    override suspend fun clear() {
        clearCount++
        bytes = 0L
    }
}
