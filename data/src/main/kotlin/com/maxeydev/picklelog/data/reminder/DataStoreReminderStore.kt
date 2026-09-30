package com.maxeydev.picklelog.data.reminder

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.maxeydev.picklelog.domain.reminder.ReminderState
import com.maxeydev.picklelog.domain.reminder.ReminderStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val REMINDER_ENABLED = booleanPreferencesKey("streak_reminder_enabled")
private val REMINDER_LAST_NOTIFIED_WEEK = longPreferencesKey("streak_reminder_last_notified_week")

class DataStoreReminderStore(
    private val dataStore: DataStore<Preferences>,
) : ReminderStore {
    override fun observe(): Flow<ReminderState> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                ReminderState(
                    enabled = preferences[REMINDER_ENABLED] ?: false,
                    lastNotifiedWeek = preferences[REMINDER_LAST_NOTIFIED_WEEK],
                )
            }.distinctUntilChanged()

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[REMINDER_ENABLED] = enabled }
    }

    override suspend fun markNotified(weekOrdinal: Long?) {
        dataStore.edit { preferences ->
            if (weekOrdinal == null) {
                preferences.remove(REMINDER_LAST_NOTIFIED_WEEK)
            } else {
                preferences[REMINDER_LAST_NOTIFIED_WEEK] = weekOrdinal
            }
        }
    }
}
