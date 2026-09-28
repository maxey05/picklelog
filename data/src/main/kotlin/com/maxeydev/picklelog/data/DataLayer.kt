package com.maxeydev.picklelog.data

import android.content.Context
import android.net.Uri
import com.maxeydev.picklelog.data.db.createPicklelogDatabase
import com.maxeydev.picklelog.data.match.DataStoreLastUsedFormatStore
import com.maxeydev.picklelog.data.match.DataStoreMatchSortStore
import com.maxeydev.picklelog.data.match.RoomMatchRepository
import com.maxeydev.picklelog.data.match.createMatchPreferencesDataStore
import com.maxeydev.picklelog.data.person.RoomPersonRepository
import com.maxeydev.picklelog.data.photo.ImageCompressor
import com.maxeydev.picklelog.data.photo.PhotoStore
import com.maxeydev.picklelog.data.photo.QueuedPhotoImporter
import com.maxeydev.picklelog.data.profile.DataStoreProfileRepository
import com.maxeydev.picklelog.data.profile.createUserProfileDataStore
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchRepository
import com.maxeydev.picklelog.domain.match.MatchSortStore
import com.maxeydev.picklelog.domain.person.PersonRepository
import com.maxeydev.picklelog.domain.photo.PhotoImportQueue
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

const val ORPHAN_PHOTO_AGE_MILLIS = 24L * 60 * 60 * 1000

class DataLayer(
    private val roomMatchRepository: RoomMatchRepository,
    val personRepository: PersonRepository,
    val lastUsedFormatStore: LastUsedFormatStore,
    val matchSortStore: MatchSortStore,
    val photoStore: PhotoStore,
    val profileRepository: ProfileRepository,
    val photoImportQueue: PhotoImportQueue,
    private val ioDispatcher: CoroutineDispatcher,
) {
    val matchRepository: MatchRepository = roomMatchRepository

    suspend fun sweepOrphanPhotos(nowMillis: Long): Int {
        val referenced = roomMatchRepository.referencedPhotoPaths()
        return withContext(ioDispatcher) {
            photoStore.deleteOrphans(referenced, olderThanMillis = nowMillis - ORPHAN_PHOTO_AGE_MILLIS).size
        }
    }
}

suspend fun createDataLayer(
    context: Context,
    applicationScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
): DataLayer {
    val appContext = context.applicationContext
    val database = createPicklelogDatabase(appContext, ioDispatcher)
    val preferences = createMatchPreferencesDataStore(appContext, applicationScope + ioDispatcher)
    val photoStore = PhotoStore(appContext.filesDir)
    val profileStore = createUserProfileDataStore(appContext, applicationScope + ioDispatcher)
    val matchRepository = RoomMatchRepository(database, photoStore, ioDispatcher)
    val resolver = appContext.contentResolver
    val importer =
        QueuedPhotoImporter(
            scope = applicationScope,
            ioDispatcher = ioDispatcher,
            compressor = ImageCompressor(openSource = { uri -> resolver.openInputStream(Uri.parse(uri)) }),
            photoStore = photoStore,
            matchRepository = matchRepository,
            deleteTemporaryCapture = { uri -> resolver.delete(Uri.parse(uri), null, null) },
        )
    return DataLayer(
        roomMatchRepository = matchRepository,
        personRepository = RoomPersonRepository(database, ioDispatcher),
        lastUsedFormatStore = DataStoreLastUsedFormatStore(preferences),
        matchSortStore = DataStoreMatchSortStore(preferences),
        photoStore = photoStore,
        profileRepository = DataStoreProfileRepository(profileStore),
        photoImportQueue = importer,
        ioDispatcher = ioDispatcher,
    )
}
