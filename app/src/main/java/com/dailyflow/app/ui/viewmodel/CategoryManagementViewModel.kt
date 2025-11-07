package com.dailyflow.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailyflow.app.data.model.Category
import com.dailyflow.app.data.repository.CategoryRepository
import com.dailyflow.app.ui.screen.CategoryFilterType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _filterType = MutableStateFlow(CategoryFilterType.ALL)
    val filterType: StateFlow<CategoryFilterType> = _filterType.asStateFlow()

    private val allCategories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = combine(
        allCategories, 
        _showArchived,
        _filterType
    ) { categories, showArchived, filterType ->
        val filteredByArchive = if (showArchived) {
            categories
        } else {
            categories.filter { !it.isArchived }
        }

        when (filterType) {
            CategoryFilterType.ALL -> filteredByArchive
            CategoryFilterType.FOR_TASKS -> filteredByArchive.filter { it.forTasks }
            CategoryFilterType.FOR_NOTES -> filteredByArchive.filter { it.forNotes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleShowArchived(show: Boolean) {
        _showArchived.value = show
    }

    fun setFilterType(type: CategoryFilterType) {
        _filterType.value = type
    }

    fun saveCategory(id: String?, name: String, color: String, icon: String, forTasks: Boolean, forNotes: Boolean) {
        viewModelScope.launch {
            val category = Category(
                id = id ?: UUID.randomUUID().toString(),
                name = name,
                color = color,
                icon = icon,
                forTasks = forTasks,
                forNotes = forNotes
            )
            if (id == null) {
                categoryRepository.insertCategory(category)
            } else {
                categoryRepository.updateCategory(category)
            }
        }
    }

    fun archiveCategory(id: String) {
        viewModelScope.launch {
            categoryRepository.archiveCategory(id)
        }
    }

    fun unarchiveCategory(id: String) {
        viewModelScope.launch {
            categoryRepository.unarchiveCategory(id)
        }
    }
}