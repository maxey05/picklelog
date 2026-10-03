package com.maxeydev.picklelog.data.settings

import com.maxeydev.picklelog.domain.settings.AppCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class DirectoryAppCache(
    private val root: File,
    private val ioDispatcher: CoroutineDispatcher,
) : AppCache {
    override suspend fun sizeBytes(): Long =
        withContext(ioDispatcher) {
            root
                .walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        }

    override suspend fun clear() {
        withContext(ioDispatcher) {
            root.listFiles().orEmpty().forEach { it.deleteRecursively() }
        }
    }
}
