package com.kema.k2look.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kema.k2look.MainActivity
import com.kema.k2look.R

/**
 * Manages notifications for app updates
 */
class UpdateNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel for updates (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Updates"
            val descriptionText = "Notifications about available app updates"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show notification that an update is available
     */
    fun showUpdateAvailableNotification(update: AppUpdate) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_update", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("K2Look Update Available")
            .setContentText("Version ${update.version} is now available")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Version ${update.version} is now available. Tap to view details and install.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted on Android 13+
        }
    }

    /**
     * Cancel update notification
     */
    fun cancelUpdateNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}

