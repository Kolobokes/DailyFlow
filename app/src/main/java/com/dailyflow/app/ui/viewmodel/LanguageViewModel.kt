package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.security.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageManager: LanguageManager
) : ViewModel() {

    val language: StateFlow<String> = languageManager.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    fun setLanguageAndRestart(languageCode: String) {
        // AppCompatDelegate.setApplicationLocales handles the recreation/restart
        languageManager.setLanguage(languageCode)
    }
}
