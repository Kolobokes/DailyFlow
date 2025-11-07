package com.dailyflow.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dailyflow.app.MainActivity
import com.dailyflow.app.R
import com.dailyflow.app.data.model.Task
// import com.google.firebase.messaging.FirebaseMessagingService
// import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : android.app.Service() {
    
    @Inject
    lateinit var reminderManager: ReminderManager
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    // Firebase методы временно отключены
    /*
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Handle FCM messages here
        remoteMessage.data.isNotEmpty().let {
            // Process data payload
        }
        
        remoteMessage.notification?.let {
            // Process notification payload
            showNotification(
                title = it.title ?: "DailyFlow",
                message = it.body ?: "",
                channelId = CHANNEL_ID_GENERAL
            )
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server if needed
    }
    */
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // General notifications channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "Общие уведомления",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Общие уведомления приложения"
            }
            
            // Reminder notifications channel
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                "Напоминания",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о задачах и событиях"
                enableVibration(true)
                enableLights(true)
            }
            
            notificationManager.createNotificationChannels(listOf(generalChannel, reminderChannel))
        }
    }
    
    fun showNotification(
        title: String,
        message: String,
        channelId: String,
        taskId: String? = null
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            taskId?.let { putExtra("task_id", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), notification)
        }
    }
    
    companion object {
        const val CHANNEL_ID_GENERAL = "general"
        const val CHANNEL_ID_REMINDER = "reminder"
    }
}
