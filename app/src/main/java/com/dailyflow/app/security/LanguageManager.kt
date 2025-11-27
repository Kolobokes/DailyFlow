package com.dailyflow.app.security

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // We can use a StateFlow to expose the current language, though AppCompatDelegate handles persistence.
    // Initial value is determined from AppCompatDelegate or System default.
    private val _language = MutableStateFlow(getCurrentLanguageCode())
    val language: Flow<String> = _language.asStateFlow()

    fun setLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        // Update the flow locally so UI can react if needed before restart (though restart is usually immediate)
        _language.value = languageCode
    }

    private fun getCurrentLanguageCode(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            return appLocales.get(0)?.language ?: "en"
        }
        
        // If app-specific locale is not set, fall back to system default logic
        val systemLang = Locale.getDefault().language
        return if (systemLang == "ru") "ru" else "en"
    }
    
    // No longer needed as AppCompatDelegate handles it, but kept for compatibility if ViewModel calls it
    fun restartApp() {
        // AppCompatDelegate.setApplicationLocales triggers recreation automatically
    }
}
