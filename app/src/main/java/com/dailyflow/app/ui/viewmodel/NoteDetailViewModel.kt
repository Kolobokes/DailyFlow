package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Note
import com.dailyflow.app.data.repository.CategoryRepository
import com.dailyflow.app.data.repository.NoteRepository
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
    val isLoading: Boolean = true
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String? = savedStateHandle.get("noteId")

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = categoryRepository.getNoteCategories().first()
            if (noteId != null) {
                val note = noteRepository.getNoteById(noteId)
                _uiState.value = NoteDetailUiState(note = note, categories = categories, isNewNote = false, isLoading = false)
            } else {
                _uiState.value = NoteDetailUiState(categories = categories, isNewNote = true, isLoading = false)
            }
        }
    }

    fun saveNote(
        title: String,
        content: String,
        categoryId: String?,
        dateTime: LocalDateTime?,
        isCompleted: Boolean
    ) {
        viewModelScope.launch {
            val noteToSave = if (noteId == null) {
                Note(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    content = content,
                    categoryId = categoryId,
                    dateTime = dateTime,
                    isCompleted = isCompleted
                )
            } else {
                _uiState.value.note!!.copy(
                    title = title,
                    content = content,
                    categoryId = categoryId,
                    dateTime = dateTime,
                    isCompleted = isCompleted
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
                noteRepository.deleteNote(note)
            }
        }
    }
}