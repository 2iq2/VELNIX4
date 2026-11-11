package com.velnix.tv

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private const val REMINDER_CHANNEL_ID = "velnix_program_reminders"
private const val EXTRA_CHANNEL_NAME = "channel_name"
private const val EXTRA_PROGRAM_TITLE = "program_title"
private const val EXTRA_REMINDER_KEY = "reminder_key"

object ProgramReminderScheduler {
    fun schedule(
        context: Context,
        channelName: String,
        programTitle: String,
        startUtcMillis: Long,
        reminderKey: String
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_CHANNEL_NAME, channelName)
            putExtra(EXTRA_PROGRAM_TITLE, programTitle)
            putExtra(EXTRA_REMINDER_KEY, reminderKey)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reminderKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = maxOf(startUtcMillis - 5 * 60 * 1000L, System.currentTimeMillis() + 1000L)
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    fun cancel(context: Context, reminderKey: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            reminderKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pending)
        pending.cancel()
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "VELNIX"
        val programTitle = intent.getStringExtra(EXTRA_PROGRAM_TITLE) ?: "برنامجك المفضل"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                REMINDER_CHANNEL_ID,
                "تذكيرات البرامج",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "تنبيهات قبل بدء البرامج والمباريات" }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("اقترب موعد البرنامج")
            .setContentText("$programTitle على قناة $channelName")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$programTitle سيبدأ قريباً على قناة $channelName."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(
            (intent.getStringExtra(EXTRA_REMINDER_KEY) ?: programTitle).hashCode(),
            notification
        )
    }
}
