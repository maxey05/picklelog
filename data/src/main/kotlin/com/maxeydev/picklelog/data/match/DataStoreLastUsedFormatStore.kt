package com.maxeydev.picklelog.data.match

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.maxeydev.picklelog.domain.match.LastUsedFormatStore
import com.maxeydev.picklelog.domain.match.MatchFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

const val MATCH_PREFERENCES_NAME = "match-preferences"

private val LAST_USED_FORMAT = stringPreferencesKey("last_used_format")

fun createMatchPreferencesDataStore(
    context: Context,
    scope: CoroutineScope,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.applicationContext.preferencesDataStoreFile(MATCH_PREFERENCES_NAME) },
    )

class DataStoreLastUsedFormatStore(
    private val dataStore: DataStore<Preferences>,
) : LastUsedFormatStore {
    override suspend fun lastUsedFormat(): MatchFormat {
        val stored = dataStore.data.first()[LAST_USED_FORMAT]
        return MatchFormat.entries.firstOrNull { it.name == stored } ?: LastUsedFormatStore.FIRST_LAUNCH_FORMAT
    }

    override suspend fun recordLastUsedFormat(format: MatchFormat) {
        dataStore.edit { preferences -> preferences[LAST_USED_FORMAT] = format.name }
    }
}
