package com.maxeydev.picklelog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.maxeydev.picklelog.data.match.GameScoreEntity
import com.maxeydev.picklelog.data.match.MatchDao
import com.maxeydev.picklelog.data.match.MatchEntity
import com.maxeydev.picklelog.data.match.MatchPersonEntity
import com.maxeydev.picklelog.data.person.PersonDao
import com.maxeydev.picklelog.data.person.PersonEntity
import com.maxeydev.picklelog.data.photo.PhotoEntity
import kotlinx.coroutines.CoroutineDispatcher

const val PICKLELOG_DB_NAME = "picklelog.db"

const val PICKLELOG_DB_VERSION = 1

@Database(
    entities = [
        MatchEntity::class,
        PersonEntity::class,
        MatchPersonEntity::class,
        GameScoreEntity::class,
        PhotoEntity::class,
    ],
    version = PICKLELOG_DB_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PicklelogDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao

    abstract fun personDao(): PersonDao
}

suspend fun createPicklelogDatabase(
    context: Context,
    ioDispatcher: CoroutineDispatcher,
): PicklelogDatabase {
    DatabaseBackup(context, ioDispatcher).backupIfMigrationPending(PICKLELOG_DB_VERSION)
    return Room
        .databaseBuilder(
            context.applicationContext,
            PicklelogDatabase::class.java,
            PICKLELOG_DB_NAME,
        ).build()
}
