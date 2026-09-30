package com.maxeydev.picklelog.ui.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import com.maxeydev.picklelog.domain.backup.ExportOutcome
import com.maxeydev.picklelog.ui.R
import com.maxeydev.picklelog.ui.common.shareableUriFor
import java.io.File

const val EXPORT_MIME_TYPE = "application/json"

object ExportShareLauncher {
    fun launch(
        context: Context,
        export: ExportOutcome.Ready,
    ) {
        val uri = shareableUriFor(context, File(export.filePath))
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = EXPORT_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, export.fileName)
                clipData = ClipData.newRawUri(null, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.backup_share_chooser_title)))
    }
}
