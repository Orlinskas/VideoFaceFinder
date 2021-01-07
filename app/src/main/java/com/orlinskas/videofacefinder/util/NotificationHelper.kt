package com.orlinskas.videofacefinder.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.videofacefinder.BuildConfig
import com.example.videofacefinder.R

import com.orlinskas.videofacefinder.ui.activity.MainActivity

private const val VIDEO_PROCESS_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".VIDEO_PROCESS_CHANNEL_ID"
private const val VIDEO_PROCESS_CHANNEL_NAME = "Video Progress Channel"
private const val VIDEO_PROCESS_CHANNEL_DESCRIPTION = "Notification channel for video progress"

object NotificationHelper {

    fun createDownloadNotification(context: Context): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = NotificationChannel(
                    VIDEO_PROCESS_CHANNEL_ID,
                    VIDEO_PROCESS_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.description =
                    VIDEO_PROCESS_CHANNEL_DESCRIPTION
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(
                context,
                VIDEO_PROCESS_CHANNEL_ID
        )
                .setContentTitle(context.getString(R.string.notification_video_processing_title))
                .setContentText(context.getString(R.string.notification_video_processing_title_sub))
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_baseline_slow_motion_video_24)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
    }

    fun createDownloadDoneNotification(context: Context): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = NotificationChannel(
                    VIDEO_PROCESS_CHANNEL_ID,
                    VIDEO_PROCESS_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.description =
                    VIDEO_PROCESS_CHANNEL_DESCRIPTION
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(
                context,
                VIDEO_PROCESS_CHANNEL_ID
        )
                .setContentTitle(context.getString(R.string.notification_video_processing_title))
                .setContentText(context.getString(R.string.notification_video_processing_done_message))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_baseline_slow_motion_video_24)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
    }

    fun createDownloadFailNotification(context: Context): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = NotificationChannel(
                    VIDEO_PROCESS_CHANNEL_ID,
                    VIDEO_PROCESS_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.description =
                    VIDEO_PROCESS_CHANNEL_DESCRIPTION
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(
                context,
                VIDEO_PROCESS_CHANNEL_ID
        )
                .setContentTitle(context.getString(R.string.notification_video_processing_title))
                .setContentText(context.getString(R.string.notification_video_processing_fail_message))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_baseline_slow_motion_video_24)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
    }

    fun sendNotification(context: Context, notification: Notification, id: Int) {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.notify(id, notification)
    }
}