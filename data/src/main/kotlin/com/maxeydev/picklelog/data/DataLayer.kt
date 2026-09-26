package com.maxeydev.picklelog.data

import android.content.Context
import com.maxeydev.picklelog.data.db.createPicklelogDatabase
import com.maxeydev.picklelog.data.match.DataStoreLastUsedFormatStore
import com.maxeydev.picklelog.data.match.RoomMatchRepository
import com.maxeydev.picklelog.data.match.createMatchPreferencesDataStore
import com.maxeydev.picklelog.data.person.RoomPersonRepository
import com.maxeydev.picklelog.data.photo.PhotoFileStore
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.person.PersonRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

class DataLayer(
    val matchRepository: MatchRepository,
    val personRepository: PersonRepository,
    val lastUsedFormatStore: LastUsedFormatStore,
)

suspend fun createDataLayer(
    context: Context,
    applicationScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
): DataLayer {
    val database = createPicklelogDatabase(context, ioDispatcher)
    val preferences = createMatchPreferencesDataStore(context, applicationScope + ioDispatcher)
    return DataLayer(
        matchRepository = RoomMatchRepository(database, PhotoFileStore(context.filesDir), ioDispatcher),
        personRepository = RoomPersonRepository(database, ioDispatcher),
        lastUsedFormatStore = DataStoreLastUsedFormatStore(preferences),
    )
}
