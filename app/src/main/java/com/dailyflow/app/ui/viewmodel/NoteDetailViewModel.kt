package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Note
import com.dailyflow.app.data.model.ChecklistItem
import com.dailyflow.app.data.repository.CategoryRepository
import com.dailyflow.app.data.repository.NoteRepository
import com.dailyflow.app.export.TextExportManager
import com.dailyflow.app.util.FileStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class NoteDetailUiState(
    val note: Note? = null,
    val categories: List<Category> = emptyList(),
    val isNewNote: Boolean = true,
    val isLoading: Boolean = true,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList()
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    private val exportManager: TextExportManager,
    private val fileStorageManager: FileStorageManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String? = savedStateHandle.get("noteId")
    private val categoryId: String? = savedStateHandle.get("categoryId")

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()
    
    val initialCategoryId: String? = categoryId

    init {
        viewModelScope.launch {
            val categories = categoryRepository.getNoteCategories().first()
            if (noteId != null) {
                val note = noteRepository.getNoteById(noteId)
                _uiState.value = NoteDetailUiState(
                    note = note,
                    categories = categories,
                    isNewNote = false,
                    isLoading = false,
                    isChecklist = note?.isChecklist ?: false,
                    checklistItems = note?.checklistItems ?: emptyList()
                )
            } else {
                _uiState.value = NoteDetailUiState(categories = categories, isNewNote = true, isLoading = false)
            }
        }
    }

    suspend fun copyFileToStorage(sourceUri: android.net.Uri, noteId: String): String? {
        return fileStorageManager.copyFileToStorage(sourceUri, noteId)
    }

    fun saveNote(
        title: String,
        content: String,
        categoryId: String?,
        dateTime: LocalDateTime?,
        isCompleted: Boolean,
        isChecklist: Boolean,
        checklistItems: List<ChecklistItem>,
        attachedFileName: String? = null,
        noteIdForNewNote: String? = null
    ) {
        viewModelScope.launch {
            val finalNoteId = noteId ?: noteIdForNewNote ?: UUID.randomUUID().toString()
            val now = LocalDateTime.now()
            
            // Если есть старый файл и он отличается от нового, удаляем старый
            val oldNote = _uiState.value.note
            if (oldNote != null && oldNote.attachedFileUri != null && oldNote.attachedFileUri != attachedFileName) {
                fileStorageManager.deleteFile(oldNote.attachedFileUri!!)
            }
            
            val noteToSave = if (noteId == null) {
                Note(
                    id = finalNoteId,
                    title = title,
                    content = content,
                    categoryId = categoryId,
                    dateTime = dateTime,
                    isCompleted = isCompleted,
                    isChecklist = isChecklist,
                    checklistItems = checklistItems,
                    attachedFileUri = attachedFileName,
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                _uiState.value.note!!.copy(
                    title = title,
                    content = content,
                    categoryId = categoryId,
                    dateTime = dateTime,
                    isCompleted = isCompleted,
                    isChecklist = isChecklist,
                    checklistItems = checklistItems,
                    attachedFileUri = attachedFileName,
                    updatedAt = now
                )
            }
            if (noteId == null) {
                noteRepository.insertNote(noteToSave)
            } else {
                noteRepository.updateNote(noteToSave)
            }
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            if (noteId != null) {
                val note = _uiState.value.note!!
                // Удаляем прикрепленный файл, если он есть
                note.attachedFileUri?.let { fileName ->
                    fileStorageManager.deleteFile(fileName)
                }
                noteRepository.deleteNote(note)
            }
        }
    }
    
    fun getFileUri(fileName: String): android.net.Uri? {
        return fileStorageManager.getFileUri(fileName)
    }
    
    fun getFile(fileName: String): java.io.File? {
        return fileStorageManager.getFile(fileName)
    }

    suspend fun exportCurrentNote(): String? {
        val id = noteId ?: return null
        return exportManager.exportNote(id)
    }
}