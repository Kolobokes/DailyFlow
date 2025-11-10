package com.dailyflow.app.data.dao

import androidx.room.*
import com.dailyflow.app.data.model.RecurringTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTemplateDao {

    @Query("SELECT * FROM recurring_templates")
    fun getAllTemplates(): Flow<List<RecurringTemplate>>

    @Query("SELECT * FROM recurring_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): RecurringTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: RecurringTemplate)

    @Update
    suspend fun updateTemplate(template: RecurringTemplate)

    @Delete
    suspend fun deleteTemplate(template: RecurringTemplate)
}

