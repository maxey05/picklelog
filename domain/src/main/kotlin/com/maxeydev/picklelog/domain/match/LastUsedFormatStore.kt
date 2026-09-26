package com.maxeydev.picklelog.domain.match

interface LastUsedFormatStore {
    suspend fun lastUsedFormat(): MatchFormat

    suspend fun recordLastUsedFormat(format: MatchFormat)

    companion object {
        val FIRST_LAUNCH_FORMAT: MatchFormat = MatchFormat.DOUBLES
    }
}
