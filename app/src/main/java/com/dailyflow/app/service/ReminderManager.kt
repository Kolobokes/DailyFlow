package com.dailyflow.app.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.receiver.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = NotificationManagerCompat.from(context)
    
    fun scheduleReminder(task: Task) {
        if (task.reminderMinutes == null || task.startDateTime == null) return
        
        val reminderTime = task.startDateTime.minusMinutes(task.reminderMinutes.toLong())
        
        // Don't schedule past reminders
        if (reminderTime.isBefore(LocalDateTime.now())) return
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
            putExtra("task_description", task.description)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerAtMillis = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
    
    fun cancelReminder(taskId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    fun rescheduleReminder(task: Task) {
        cancelReminder(task.id)
        scheduleReminder(task)
    }
    
    fun scheduleAllReminders(tasks: List<Task>) {
        tasks.forEach { task ->
            if (task.status == com.dailyflow.app.data.model.TaskStatus.PENDING) {
                scheduleReminder(task)
            }
        }
    }
    
    fun cancelAllReminders() {
        // This would typically involve canceling all pending intents
        // In a real implementation, you'd track all scheduled reminders
    }
}
