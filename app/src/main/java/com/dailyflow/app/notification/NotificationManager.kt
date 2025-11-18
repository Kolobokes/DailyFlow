package com.dailyflow.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dailyflow.app.MainActivity
import com.dailyflow.app.R
import com.dailyflow.app.ui.navigation.Screen

class NotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Напоминания о задачах"
            val descriptionText = "Канал для уведомлений о напоминаниях задач"
            val importance = NotificationManager.IMPORTANCE_HIGH // Высокий приоритет для точных уведомлений
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(title: String, message: String, taskId: String? = null) {
        // Создаем Intent для навигации к задаче
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            taskId?.let { putExtra("taskId", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId?.hashCode() ?: System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Высокий приоритет
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Звук, вибрация, свет
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        val notificationId = taskId?.hashCode()?.let { Math.abs(it) } ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        const val CHANNEL_ID = "task_reminder_channel"
        const val NOTIFICATION_ID = 1
    }
}