package com.maxeydev.picklelog.data.erase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.maxeydev.picklelog.data.db.PicklelogDatabase
import com.maxeydev.picklelog.data.photo.PhotoStore
import com.maxeydev.picklelog.domain.erase.LocalDataEraser
import com.maxeydev.picklelog.domain.profile.ProfileRepository
import com.maxeydev.picklelog.domain.settings.AppSettingsStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class RoomLocalDataEraser(
    private val database: PicklelogDatabase,
    private val photoStore: PhotoStore,
    private val exportDirectory: File,
    private val databaseBackupDirectory: File,
    private val preferences: DataStore<Preferences>,
    private val profileRepository: ProfileRepository,
    private val appSettings: AppSettingsStore,
    private val ioDispatcher: CoroutineDispatcher,
) : LocalDataEraser {
    override suspend fun erase() {
        withContext(ioDispatcher) {
            database.clearAllTables()
            photoStore.deleteAll()
            removeDirectory(exportDirectory)
            removeDirectory(databaseBackupDirectory)
        }
        preferences.edit { it.clear() }
        profileRepository.updateDisplayName("")
        appSettings.setOnboardingComplete(false)
    }

    private fun removeDirectory(directory: File) {
        if (directory.exists() && !directory.deleteRecursively()) {
            throw IOException("Saved data could not be removed from ${directory.name}.")
        }
    }
}
