package com.maxeydev.picklelog.ui.match.edit

data class PhotoUiState(
    val key: String,
    val filePath: String?,
) {
    val isImporting: Boolean
        get() = filePath == null
}
