package com.example.studybuddy.util

import com.example.studybuddy.model.RepeatInterval
import com.example.studybuddy.model.Task
import java.util.Calendar

object RecurrenceUtils {

    /**
     * Calcula la siguiente fecha de vencimiento de una tarea recurrente.
     * Avanza en días de calendario (respeta cambios de horario) hasta quedar en el futuro,
     * para no generar ocurrencias ya vencidas si la tarea se completa con retraso.
     * Devuelve 0 si la tarea no es recurrente.
     */
    fun nextDueDate(dueDate: Long, repeatInterval: Int, now: Long = System.currentTimeMillis()): Long {
        if (repeatInterval <= RepeatInterval.NONE) return 0L
        val cal = Calendar.getInstance()
        cal.timeInMillis = if (dueDate > 0) dueDate else now
        do {
            cal.add(Calendar.DAY_OF_MONTH, repeatInterval)
        } while (cal.timeInMillis <= now)
        return cal.timeInMillis
    }

    /** Crea la siguiente ocurrencia (pendiente) de una tarea recurrente, o null si no lo es. */
    fun nextOccurrence(task: Task, now: Long = System.currentTimeMillis()): Task? {
        if (task.repeatInterval <= RepeatInterval.NONE) return null
        return task.copy(
            id = 0,
            dueDate = nextDueDate(task.dueDate, task.repeatInterval, now),
            isCompleted = false,
            completedAt = 0L,
            createdAt = now
        )
    }
}
