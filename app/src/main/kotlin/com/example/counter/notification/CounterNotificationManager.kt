package com.example.counter.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.counter.MainActivity
import com.example.counter.R
import com.example.counter.data.CounterEntity

object CounterNotificationManager {
    private const val CHANNEL_ID = "counter_notifications"
    private const val TRACKING_PREFERENCES = "counter_notification_tracking"
    private const val TRACKED_COUNTER_IDS = "tracked_counter_ids"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.counter_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.counter_notification_channel_description)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun getTrackedCounterIds(context: Context): Set<Int> =
        preferences(context).getStringSet(TRACKED_COUNTER_IDS, emptySet())
            .orEmpty().mapNotNull(String::toIntOrNull).filter { it > 0 }.toSet()

    fun enableTracking(context: Context, counterId: Int) {
        if (counterId <= 0) return
        val ids = getTrackedCounterIds(context) + counterId
        preferences(context).edit()
            .putStringSet(TRACKED_COUNTER_IDS, ids.map(Int::toString).toSet()).apply()
    }

    fun removeTrackedCounter(context: Context, counterId: Int) {
        if (counterId <= 0) return
        val ids = getTrackedCounterIds(context) - counterId
        preferences(context).edit()
            .putStringSet(TRACKED_COUNTER_IDS, ids.map(Int::toString).toSet()).apply()
        dismissCounterNotification(context, counterId)
    }

    fun clearTrackedCounters(context: Context) {
        getTrackedCounterIds(context).forEach { dismissCounterNotification(context, it) }
        preferences(context).edit().remove(TRACKED_COUNTER_IDS).apply()
    }

    fun syncCounterIfTracked(context: Context, counter: CounterEntity) {
        if (counter.id in getTrackedCounterIds(context)) showCounterNotification(context, counter)
    }

    fun syncTrackedCounters(context: Context, counters: List<CounterEntity>) {
        val countersById = counters.associateBy(CounterEntity::id)
        getTrackedCounterIds(context).forEach { counterId ->
            val counter = countersById[counterId]
            if (counter == null) removeTrackedCounter(context, counterId)
            else showCounterNotification(context, counter)
        }
    }

    private fun showCounterNotification(context: Context, counter: CounterEntity) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse("counter-app://counter/${counter.id}")
            putExtra(CounterActionReceiver.EXTRA_COUNTER_ID, counter.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, counter.id, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val incrementPendingIntent = actionIntent(
            context, CounterActionReceiver.ACTION_INCREMENT, counter.id
        )
        val decrementPendingIntent = actionIntent(
            context, CounterActionReceiver.ACTION_DECREMENT, counter.id
        )

        val countText = context.getString(R.string.counter_notification_count, counter.count)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_counter)
            .setContentTitle(counter.name)
            .setContentText(countText)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_minus,
                context.getString(R.string.counter_notification_decrement),
                decrementPendingIntent
            )
            .addAction(
                R.drawable.ic_plus,
                context.getString(R.string.counter_notification_increment),
                incrementPendingIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(counter.name)
                    .bigText(countText)
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(counter.id, notification)
        } catch (_: SecurityException) {
            // Permission may be revoked between the permission check and notify().
        }
    }

    private fun actionIntent(context: Context, action: String, counterId: Int): PendingIntent {
        val intent = Intent(context, CounterActionReceiver::class.java).apply {
            this.action = action
            putExtra(CounterActionReceiver.EXTRA_COUNTER_ID, counterId)
        }
        return PendingIntent.getBroadcast(
            context, counterId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun dismissCounterNotification(context: Context, counterId: Int) {
        NotificationManagerCompat.from(context).cancel(counterId)
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(TRACKING_PREFERENCES, Context.MODE_PRIVATE)
}
