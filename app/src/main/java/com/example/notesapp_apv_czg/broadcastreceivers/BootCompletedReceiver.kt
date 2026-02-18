package com.example.notesapp_apv_czg.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notesapp_apv_czg.data.AppDatabase
import com.example.notesapp_apv_czg.data.AndroidStructuredLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val db = AppDatabase.getInstance(context)
        val noteDao = db.noteDao()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pendingTasks = noteDao.getPendingTasksForReminders()
                pendingTasks.forEach { note ->
                    scheduleTaskReminder(context, note)
                }
            } catch (e: Exception) {
                AndroidStructuredLogger.error(
                    "boot_reschedule_failed",
                    e,
                    emptyMap()
                )
            }
        }
    }
}

