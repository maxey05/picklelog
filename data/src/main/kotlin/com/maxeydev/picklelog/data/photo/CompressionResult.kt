package com.maxeydev.picklelog.data.photo

sealed interface CompressionResult {
    data class Compressed(
        val width: Int,
        val height: Int,
        val byteSize: Long,
    ) : CompressionResult

    data object Unreadable : CompressionResult
}
