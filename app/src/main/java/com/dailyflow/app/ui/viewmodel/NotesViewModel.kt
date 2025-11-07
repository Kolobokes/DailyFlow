package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.model.Note
import com.dailyflow.app.data.repository.CategoryRepository
import com.dailyflow.app.data.repository.NoteRepository
import com.dailyflow.app.ui.screen.CategoryFilterType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val selectedDate: String? = savedStateHandle.get<String?>("date")

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _showCompleted = MutableStateFlow(false)
    val showCompleted: StateFlow<Boolean> = _showCompleted.asStateFlow()

    private val allNotes: StateFlow<List<Note>> = run {
        val parsedDate = selectedDate
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it).atStartOfDay() }.getOrNull() }

        if (parsedDate != null) {
            noteRepository.getNotesForDate(parsedDate)
        } else {
            noteRepository.getAllNotes()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<Category>> = categoryRepository.getNoteCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredNotes: StateFlow<List<Note>> = combine(
        allNotes, 
        selectedCategoryId,
        showCompleted
    ) { notes, categoryId, showCompleted ->
        val categoryFilteredNotes = if (categoryId == null) {
            notes
        } else {
            notes.filter { it.categoryId == categoryId }
        }
        
        if (showCompleted) {
            categoryFilteredNotes
        } else {
            categoryFilteredNotes.filter { !it.isCompleted }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun toggleShowCompleted(show: Boolean) {
        _showCompleted.value = show
    }
}