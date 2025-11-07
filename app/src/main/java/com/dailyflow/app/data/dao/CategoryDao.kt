package com.dailyflow.app.data.dao

import androidx.room.*
import com.dailyflow.app.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY name")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE forTasks = 1 AND isArchived = 0 ORDER BY name")
    fun getTaskCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE forNotes = 1 AND isArchived = 0 ORDER BY name")
    fun getNoteCategories(): Flow<List<Category>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)
    
    @Update
    suspend fun updateCategory(category: Category)
    
    @Query("UPDATE categories SET isArchived = 1 WHERE id = :id")
    suspend fun archiveCategory(id: String)

    @Query("UPDATE categories SET isArchived = 0 WHERE id = :id")
    suspend fun unarchiveCategory(id: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)
}
