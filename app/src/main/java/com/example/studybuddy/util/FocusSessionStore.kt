package com.example.studybuddy.util

import android.content.Context
import com.example.studybuddy.game.GamificationStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FocusSessionStore {

    private const val PREFS = "focus_sessions"
    private const val KEY_TOTAL = "total_sessions"
    private const val KEY_DAILY_PREFIX = "day_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recordSession(context: Context) {
        val prefs = prefs(context)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        prefs.edit()
            .putInt(KEY_TOTAL, prefs.getInt(KEY_TOTAL, 0) + 1)
            .putInt(KEY_DAILY_PREFIX + today, prefs.getInt(KEY_DAILY_PREFIX + today, 0) + 1)
            .commit()
        val linked = getLinkedTaskId(context)
        if (linked > 0) addTaskMinutes(context, linked, getWorkMinutes(context))
        GamificationStore.recordFocusSession(
            context,
            getWorkMinutes(context),
            StudyGoalManager.isGoalMetToday(context)
        )

    }

    fun totalSessions(context: Context): Int =
        prefs(context).getInt(KEY_TOTAL, 0)

    fun sessionsToday(context: Context): Int {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        return prefs(context).getInt(KEY_DAILY_PREFIX + today, 0)
    }

    fun getWorkMinutes(context: Context): Int {
        val focusPrefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        return focusPrefs.getInt("work_minutes", 25)
    }

    fun focusMinutesToday(context: Context): Int {
        return sessionsToday(context) * getWorkMinutes(context)
    }

    // --- Tarea vinculada a la sesión de enfoque ---

    private const val KEY_LINKED_TASK_ID = "linked_task_id"
    private const val KEY_LINKED_TASK_TITLE = "linked_task_title"
    private const val KEY_TASK_MINUTES_PREFIX = "task_minutes_"

    fun getLinkedTaskId(context: Context): Int = prefs(context).getInt(KEY_LINKED_TASK_ID, 0)

    fun getLinkedTaskTitle(context: Context): String? =
        prefs(context).getString(KEY_LINKED_TASK_TITLE, null)

    fun setLinkedTask(context: Context, taskId: Int, title: String?) {
        prefs(context).edit()
            .putInt(KEY_LINKED_TASK_ID, taskId)
            .putString(KEY_LINKED_TASK_TITLE, title)
            .apply()
    }

    fun clearLinkedTask(context: Context) = setLinkedTask(context, 0, null)

    /** Minutos de enfoque acumulados en una tarea. */
    fun taskMinutes(context: Context, taskId: Int): Int =
        prefs(context).getInt(KEY_TASK_MINUTES_PREFIX + taskId, 0)

    private fun addTaskMinutes(context: Context, taskId: Int, minutes: Int) {
        val key = KEY_TASK_MINUTES_PREFIX + taskId
        prefs(context).edit().putInt(key, prefs(context).getInt(key, 0) + minutes).commit()
    }
}
