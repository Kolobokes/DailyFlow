package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.security.PrivacyManager
import com.dailyflow.app.security.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val privacyManager: PrivacyManager,
    private val languageManager: LanguageManager
) : ViewModel() {
    
    val privacyAccepted: StateFlow<Boolean> = privacyManager.privacyAccepted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val currentLanguage: StateFlow<String> = languageManager.language
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en"
        )
    
    fun acceptPrivacyPolicy() {
        viewModelScope.launch {
            privacyManager.acceptPrivacyPolicy()
        }
    }
    
    fun getPrivacyPolicyUrl(): String {
        return privacyManager.getPrivacyPolicyUrl()
    }
    
    fun getTermsOfServiceUrl(): String {
        return privacyManager.getTermsOfServiceUrl()
    }
    
    fun getDataExportInstructions(): String {
        return privacyManager.getDataExportInstructions()
    }
}
