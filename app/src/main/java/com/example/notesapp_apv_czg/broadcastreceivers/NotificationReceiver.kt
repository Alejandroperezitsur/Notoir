package com.example.notesapp_apv_czg.broadcastreceivers

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.notesapp_apv_czg.R
import com.example.notesapp_apv_czg.data.AndroidStructuredLogger
import com.example.notesapp_apv_czg.data.Note

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        const val NOTIFICATION_ID = "notificationId"
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val CHANNEL_ID = "task_reminders_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(TITLE) ?: context.getString(R.string.task_reminder)
        val description = intent.getStringExtra(DESCRIPTION) ?: context.getString(R.string.task_due_notification)
        val notificationId = intent.getIntExtra(NOTIFICATION_ID, 0)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}

fun scheduleTaskReminder(context: Context, note: Note) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra(NotificationReceiver.TITLE, note.title)
        putExtra(NotificationReceiver.DESCRIPTION, note.description)
        putExtra(NotificationReceiver.NOTIFICATION_ID, note.id.toInt())
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        note.id.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val dueAt = note.dueDateMillis

    if (!note.isTask || note.isCompleted || dueAt == null) {
        cancelTaskReminder(context, note)
        return
    }

    if (dueAt <= System.currentTimeMillis()) {
        cancelTaskReminder(context, note)
        AndroidStructuredLogger.info(
            "reminder_not_scheduled_past_due",
            mapOf("noteId" to note.id.toString())
        )
        Toast.makeText(
            context,
            context.getString(R.string.reminder_time_in_past_warning),
            Toast.LENGTH_LONG
        ).show()
        return
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            AndroidStructuredLogger.error(
                "exact_alarms_not_allowed",
                null,
                mapOf("noteId" to note.id.toString())
            )
            Toast.makeText(
                context,
                context.getString(R.string.reminder_exact_alarm_not_allowed),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
    } catch (e: SecurityException) {
        AndroidStructuredLogger.error(
            "reminder_schedule_security_exception",
            e,
            mapOf("noteId" to note.id.toString())
        )
        Toast.makeText(
            context,
            context.getString(R.string.reminder_permission_denied_warning),
            Toast.LENGTH_LONG
        ).show()
    }
}

fun cancelTaskReminder(context: Context, note: Note) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        note.id.toInt(),
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null) {
        alarmManager.cancel(pendingIntent)
    }
}
