package com.dailyflow.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailyflow.app.notification.NotificationManager

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId")
        val title = intent.getStringExtra("title") ?: "Напоминание о задаче"
        val message = intent.getStringExtra("message") ?: "У вас запланирована задача."

        val notificationManager = NotificationManager(context)
        notificationManager.createNotificationChannel()
        notificationManager.showNotification(title, message, taskId)
    }
}