package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.repository.TaskRepository
import com.dailyflow.app.data.repository.NoteRepository
import com.dailyflow.app.data.repository.CategoryRepository
import com.dailyflow.app.security.PrivacyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val privacyManager: PrivacyManager,
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    val privacyAccepted: StateFlow<Boolean> = privacyManager.privacyAccepted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val analyticsEnabled: StateFlow<Boolean> = privacyManager.analyticsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val crashReportingEnabled: StateFlow<Boolean> = privacyManager.crashReportingEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    fun acceptPrivacyPolicy() {
        viewModelScope.launch {
            privacyManager.acceptPrivacyPolicy()
        }
    }
    
    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            privacyManager.setAnalyticsEnabled(enabled)
        }
    }
    
    fun setCrashReportingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            privacyManager.setCrashReportingEnabled(enabled)
        }
    }
    
    fun resetAllData() {
        viewModelScope.launch {
            // Delete all tasks
            val tasks = taskRepository.getAllActiveTasks().first()
            tasks.forEach { task ->
                taskRepository.deleteTask(task)
            }
            
            // Delete all notes
            val notes = noteRepository.getAllNotes().first()
            notes.forEach { note ->
                noteRepository.deleteNote(note)
            }
            
            // Reset privacy preferences
            privacyManager.resetAllPreferences()
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
