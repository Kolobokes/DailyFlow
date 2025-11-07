package com.dailyflow.app.data.repository

import com.dailyflow.app.data.dao.CategoryDao
import com.dailyflow.app.data.model.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    fun getTaskCategories(): Flow<List<Category>> = categoryDao.getTaskCategories()

    fun getNoteCategories(): Flow<List<Category>> = categoryDao.getNoteCategories()
    
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    
    suspend fun archiveCategory(id: String) = categoryDao.archiveCategory(id)

    suspend fun unarchiveCategory(id: String) = categoryDao.unarchiveCategory(id)

    suspend fun deleteCategoryById(id: String) = categoryDao.deleteCategoryById(id)
}
