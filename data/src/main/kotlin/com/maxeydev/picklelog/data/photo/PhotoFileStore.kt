package com.maxeydev.picklelog.data.photo

import java.io.File
import java.io.IOException

class PhotoFileStore(
    private val rootDirectory: File,
) {
    fun deletePhotoFiles(relativePaths: List<String>) {
        val root = rootDirectory.canonicalFile
        relativePaths.forEach { relativePath ->
            val file = File(root, relativePath).canonicalFile
            require(file != root && file.toPath().startsWith(root.toPath())) {
                "A photo path must resolve to a file inside the app's own storage."
            }
            if (file.exists() && !file.delete()) {
                throw IOException("A photo file belonging to a deleted match could not be removed.")
            }
        }
    }
}
