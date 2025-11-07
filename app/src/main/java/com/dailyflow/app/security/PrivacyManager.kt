package com.dailyflow.app.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_preferences")
    
    private val privacyAcceptedKey = booleanPreferencesKey("privacy_accepted")
    private val analyticsEnabledKey = booleanPreferencesKey("analytics_enabled")
    private val crashReportingEnabledKey = booleanPreferencesKey("crash_reporting_enabled")
    
    val privacyAccepted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[privacyAcceptedKey] ?: false
        }
    
    val analyticsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[analyticsEnabledKey] ?: false
        }
    
    val crashReportingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[crashReportingEnabledKey] ?: false
        }
    
    suspend fun acceptPrivacyPolicy() {
        context.dataStore.edit { preferences ->
            preferences[privacyAcceptedKey] = true
        }
    }
    
    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[analyticsEnabledKey] = enabled
        }
    }
    
    suspend fun setCrashReportingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[crashReportingEnabledKey] = enabled
        }
    }
    
    suspend fun resetAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    fun getPrivacyPolicyUrl(): String {
        return "https://yourwebsite.com/privacy-policy"
    }
    
    fun getTermsOfServiceUrl(): String {
        return "https://yourwebsite.com/terms-of-service"
    }
    
    fun getDataExportInstructions(): String {
        return """
            Для экспорта ваших данных:
            1. Откройте главный экран приложения
            2. Нажмите на кнопку экспорта
            3. Выберите формат (Markdown, текст или HTML)
            4. Поделитесь файлом через любое приложение
            
            Для полного удаления данных:
            1. Откройте настройки приложения
            2. Перейдите в раздел "Конфиденциальность"
            3. Нажмите "Удалить все данные"
            4. Подтвердите действие
        """.trimIndent()
    }
}
