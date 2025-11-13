package com.dailyflow.app

import android.app.Application
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class DailyFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val locale = Locale("ru")
        Locale.setDefault(locale)
        val russianLocale = LocaleListCompat.forLanguageTags("ru")
        if (AppCompatDelegate.getApplicationLocales() != russianLocale) {
            AppCompatDelegate.setApplicationLocales(russianLocale)
        }
        val config: Configuration = resources.configuration
        if (config.locales.isEmpty || config.locales[0] != locale) {
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}
