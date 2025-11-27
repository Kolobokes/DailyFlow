package com.dailyflow.app.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    private val defaultReminderMinutesKey = intPreferencesKey("default_reminder_minutes")

    val defaultReminderMinutes: Flow<Int> = context.dataStore.data.map {
        it[defaultReminderMinutesKey] ?: 60
    }

    suspend fun setDefaultReminderMinutes(minutes: Int) {
        context.dataStore.edit {
            it[defaultReminderMinutesKey] = minutes
        }
    }
}
