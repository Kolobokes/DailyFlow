package com.dailyflow.app.data.repository

import com.dailyflow.app.data.dao.NoteDao
import com.dailyflow.app.data.model.Note
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()
    
    fun getNotesForDate(date: LocalDateTime): Flow<List<Note>> = noteDao.getNotesForDate(date)
    
    suspend fun getNoteById(id: String): Note? = noteDao.getNoteById(id)
    
    suspend fun insertNote(note: Note) = noteDao.insertNote(note)
    
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)
    
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
    
    suspend fun updateNoteCompletion(id: String, isCompleted: Boolean) = 
        noteDao.updateNoteCompletion(id, isCompleted)
}
