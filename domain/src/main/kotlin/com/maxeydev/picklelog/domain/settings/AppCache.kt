package com.maxeydev.picklelog.domain.settings

interface AppCache {
    suspend fun sizeBytes(): Long

    suspend fun clear()
}
