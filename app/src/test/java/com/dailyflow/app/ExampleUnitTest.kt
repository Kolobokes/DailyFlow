package com.dailyflow.app

import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.TaskStatus
import com.dailyflow.app.data.model.Priority
import com.dailyflow.app.util.ExportManager
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

class ExampleUnitTest {
    
    @Test
    fun testTaskCreation() {
        val task = Task(
            id = "1",
            title = "Тестовая задача",
            description = "Описание задачи",
            categoryId = "work",
            priority = Priority.HIGH,
            status = TaskStatus.PENDING
        )
        
        assertEquals("Тестовая задача", task.title)
        assertEquals(Priority.HIGH, task.priority)
        assertEquals(TaskStatus.PENDING, task.status)
    }
    
    @Test
    fun testExportToMarkdown() {
        val tasks = listOf(
            Task(
                id = "1",
                title = "Задача 1",
                categoryId = "work",
                status = TaskStatus.COMPLETED
            )
        )
        
        val notes = emptyList<com.dailyflow.app.data.model.Note>()
        val categories = emptyList<com.dailyflow.app.data.model.Category>()
        val date = LocalDateTime.now()
        
        val markdown = ExportManager.exportToMarkdown(tasks, notes, categories, date)
        
        assertTrue(markdown.contains("Задача 1"))
        assertTrue(markdown.contains("✅ Выполнено:"))
    }
    
    @Test
    fun testVoiceParser() {
        val text = "создай задачу купить молоко завтра в 10 утра"
        val parsed = com.dailyflow.app.util.VoiceParser.parseTaskFromVoice(text)
        
        assertEquals("купить молоко", parsed.title)
        assertNotNull(parsed.dateTime)
    }
}
