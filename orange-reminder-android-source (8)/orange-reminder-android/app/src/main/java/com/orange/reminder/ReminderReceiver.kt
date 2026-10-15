package com.orange.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Reminder"
        val note = intent.getStringExtra("note") ?: "It's time!"
        val sound = intent.getStringExtra("sound") ?: "default"
        val channelId = "orange_reminders_$sound"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(channelId) == null) {
            val uri = when (sound) {
                "alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                "notification" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            val channel = NotificationChannel(
                channelId, "Reminders ($sound)", NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Orange Reminder alerts"
            channel.setSound(uri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build())
            manager.createNotificationChannel(channel)
        }

        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(note.ifBlank { "It's time!" })
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(
            intent.getIntExtra("id", (System.currentTimeMillis() % Int.MAX_VALUE).toInt()),
            notification
        )
    }
}
