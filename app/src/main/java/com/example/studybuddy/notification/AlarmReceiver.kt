package com.example.studybuddy.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.studybuddy.R

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarma activada")

        val taskTitle = intent.getStringExtra("task_title")
            ?: context.getString(R.string.notification_task)
        val taskDescription = intent.getStringExtra("task_description") ?: ""
        val taskId = intent.getIntExtra("task_id", -1)

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showTaskReminderNotification(
            context.getString(R.string.notification_reminder_title),
            context.getString(
                R.string.notification_reminder_body, taskTitle,
                if (taskDescription.isBlank()) "" else "\n$taskDescription"
            ),
            taskId
        )
        // La recurrencia se gestiona al completar la tarea (TaskRepository.setCompleted),
        // no al sonar el recordatorio, para no adelantar fechas de tareas sin hacer.
    }
}
