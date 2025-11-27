package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.security.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val defaultReminderMinutes: StateFlow<Int> = settingsManager.defaultReminderMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    fun setDefaultReminderMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsManager.setDefaultReminderMinutes(minutes)
        }
    }
}
