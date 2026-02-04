package com.example.counter.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.counter.MainActivity
import com.example.counter.R
import com.example.counter.data.CounterEntity

object CounterNotificationManager {
    
    private const val CHANNEL_ID = "counter_notifications"
    private const val NOTIFICATION_ID_BASE = 1000
    private const val ACTION_INCREMENT = "ACTION_INCREMENT"
    private const val ACTION_DECREMENT = "ACTION_DECREMENT"
    private const val EXTRA_COUNTER_ID = "counter_id"
    
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Counter Notifications"
            val descriptionText = "Notifications for counter app"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showCounterNotification(context: Context, counter: CounterEntity) {
        createNotificationChannel(context)
        
        // Intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("counter_id", counter.id)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            counter.id, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Increment action
        val incrementIntent = Intent(context, CounterActionReceiver::class.java).apply {
            action = ACTION_INCREMENT
            putExtra(EXTRA_COUNTER_ID, counter.id)
        }
        
        val incrementPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            counter.id * 1000,
            incrementIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Decrement action
        val decrementIntent = Intent(context, CounterActionReceiver::class.java).apply {
            action = ACTION_DECREMENT
            putExtra(EXTRA_COUNTER_ID, counter.id)
        }
        
        val decrementPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            (counter.id * 1000) + 1,
            decrementIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_counter)
            .setContentTitle(counter.name)
            .setContentText("Count: ${counter.count}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_minus,
                "−",
                decrementPendingIntent
            )
            .addAction(
                R.drawable.ic_plus,
                "+",
                incrementPendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle(counter.name)
                .bigText("Current count: ${counter.count}")
            )
            .build()
        
        // Use try-catch for notification posting to handle edge cases
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_BASE + counter.id, notification)
            }
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            e.printStackTrace()
        }
    }
    
    fun dismissCounterNotification(context: Context, counterId: Int) {
        try {
            with(NotificationManagerCompat.from(context)) {
                cancel(NOTIFICATION_ID_BASE + counterId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun dismissAllNotifications(context: Context) {
        try {
            with(NotificationManagerCompat.from(context)) {
                cancelAll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
