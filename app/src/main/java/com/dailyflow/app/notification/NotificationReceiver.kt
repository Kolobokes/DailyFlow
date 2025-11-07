package com.dailyflow.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Напоминание о задаче"
        val message = intent.getStringExtra("message") ?: "У вас запланирована задача."

        val notificationManager = NotificationManager(context)
        notificationManager.createNotificationChannel()
        notificationManager.showNotification(title, message)
    }
}