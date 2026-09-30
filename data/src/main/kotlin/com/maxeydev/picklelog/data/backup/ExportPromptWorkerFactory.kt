package com.maxeydev.picklelog.data.backup

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.maxeydev.picklelog.domain.backup.BackupRepository

class ExportPromptWorkerFactory(
    private val repository: suspend () -> BackupRepository,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == ExportPromptWorker::class.java.name) {
            ExportPromptWorker(appContext, workerParameters, repository)
        } else {
            null
        }
}
