package com.dailyflow.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.dailyflow.app.data.model.Task
import com.dailyflow.app.data.model.TaskStatus
import com.dailyflow.app.receiver.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService<AlarmManager>() ?: error("AlarmManager not available")
    }

    fun schedule(task: Task) {
        if (!shouldSchedule(task)) {
            cancel(task.id)
            return
        }

        val reminderTime = computeReminderTime(task) ?: run {
            cancel(task.id)
            return
        }

        val triggerAtMillis = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerAtMillis <= System.currentTimeMillis()) {
            cancel(task.id)
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", task.title)
            putExtra("message", task.description ?: "")
            putExtra("taskId", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        // Используем setExactAndAllowWhileIdle для точных будильников, даже когда устройство в режиме энергосбережения
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(taskId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun shouldSchedule(task: Task): Boolean {
        return task.reminderEnabled &&
                task.startDateTime != null &&
                task.reminderMinutes != null &&
                task.status == TaskStatus.PENDING
    }

    private fun computeReminderTime(task: Task): LocalDateTime? {
        val start = task.startDateTime ?: return null
        val offsetMinutes = task.reminderMinutes ?: return null
        return start.minusMinutes(offsetMinutes.toLong())
    }

    private fun mutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}

