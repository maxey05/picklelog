package com.maxeydev.picklelog.data.photo

import java.io.File
import java.io.IOException

class PhotoFileStore(
    private val rootDirectory: File,
) {
    fun resolve(relativePath: String): File {
        val root = rootDirectory.canonicalFile
        val file = File(root, relativePath).canonicalFile
        require(file != root && file.toPath().startsWith(root.toPath())) {
            "A photo path must resolve to a file inside the app's own storage."
        }
        return file
    }

    fun deletePhotoFiles(relativePaths: List<String>) {
        relativePaths.forEach { relativePath ->
            val file = resolve(relativePath)
            if (file.exists() && !file.delete()) {
                throw IOException("A photo file belonging to a deleted match could not be removed.")
            }
        }
    }
}
