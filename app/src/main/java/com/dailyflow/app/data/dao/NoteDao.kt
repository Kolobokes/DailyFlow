package com.dailyflow.app.data.dao

import androidx.room.*
import com.dailyflow.app.data.model.Note
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface NoteDao {
    
    @Query("SELECT * FROM notes ORDER BY dateTime DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE dateTime IS NOT NULL AND date(dateTime) = date(:date) ORDER BY dateTime ASC")
    fun getNotesForDate(date: LocalDateTime): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): Note?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)
    
    @Update
    suspend fun updateNote(note: Note)
    
    @Delete
    suspend fun deleteNote(note: Note)
    
    @Query("UPDATE notes SET isCompleted = :isCompleted, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNoteCompletion(id: String, isCompleted: Boolean, updatedAt: LocalDateTime)
}
