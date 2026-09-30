package com.maxeydev.picklelog.data.backup

import androidx.room.withTransaction
import com.maxeydev.picklelog.data.db.PicklelogDatabase
import com.maxeydev.picklelog.domain.backup.ExportOutcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.IOException
import kotlin.time.Clock

internal class JsonExporter(
    private val database: PicklelogDatabase,
    private val exportDirectory: File,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val matchDao = database.matchDao()
    private val personDao = database.personDao()

    suspend fun export(): ExportOutcome =
        withContext(ioDispatcher) {
            val snapshot = database.withTransaction { personDao.all() to matchDao.allWithRelations() }
            val now = clock.now()
            val text = ExportCodec.encode(ExportCodec.build(snapshot.first, snapshot.second, now))
            val fileName = "picklelog-export-${now.toLocalDateTime(TimeZone.currentSystemDefault()).date}.json"
            try {
                exportDirectory.mkdirs()
                exportDirectory.listFiles().orEmpty().forEach { it.delete() }
                val file = File(exportDirectory, fileName)
                file.writeText(text)
                ExportOutcome.Ready(filePath = file.path, fileName = fileName, matchCount = snapshot.second.size)
            } catch (unwritable: IOException) {
                ExportOutcome.Failed
            }
        }
}
