package com.example.studybuddy.agenda

import com.example.studybuddy.model.Task
import com.example.studybuddy.util.DateUtils
import java.util.Calendar

/** Fila de la agenda: cabecera de sección o tarea. */
sealed class AgendaRow {
    /** [dayStart] es la medianoche local del día; null en las secciones especiales. */
    data class Header(val kind: Kind, val dayStart: Long?, val count: Int) : AgendaRow()
    data class Item(val task: Task) : AgendaRow()

    enum class Kind { OVERDUE, TODAY, TOMORROW, DAY, LATER, NO_DATE }
}

object AgendaGrouper {

    /**
     * Agrupa las tareas pendientes en: vencidas, hoy, mañana, cada día de la semana,
     * más adelante y sin fecha. [days] es el horizonte en días (1 = solo hoy).
     */
    fun group(tasks: List<Task>, now: Long, days: Int, includeLater: Boolean = true): List<AgendaRow> {
        val pending = tasks.filter { !it.isCompleted }.sortedBy { it.dueDate }
        val today = DateUtils.startOfDay(now)
        val dayStarts = (0 until days).map { addDays(today, it) }
        val horizonEnd = addDays(today, days)

        val rows = mutableListOf<AgendaRow>()
        fun section(kind: AgendaRow.Kind, dayStart: Long?, items: List<Task>) {
            if (items.isEmpty()) return
            rows += AgendaRow.Header(kind, dayStart, items.size)
            items.forEach { rows += AgendaRow.Item(it) }
        }

        section(AgendaRow.Kind.OVERDUE, null, pending.filter { it.dueDate in 1 until now && it.dueDate < today })
        dayStarts.forEachIndexed { index, start ->
            val end = addDays(start, 1)
            val kind = when (index) {
                0 -> AgendaRow.Kind.TODAY
                1 -> AgendaRow.Kind.TOMORROW
                else -> AgendaRow.Kind.DAY
            }
            section(kind, start, pending.filter { it.dueDate in start until end })
        }
        if (includeLater) {
            section(AgendaRow.Kind.LATER, null, pending.filter { it.dueDate >= horizonEnd })
        }
        section(AgendaRow.Kind.NO_DATE, null, pending.filter { it.dueDate <= 0L })
        return rows
    }

    /** Suma días de calendario respetando cambios de horario. */
    fun addDays(epoch: Long, days: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = epoch
            add(Calendar.DAY_OF_MONTH, days)
        }.timeInMillis

    /** Nueva fecha al posponer: un día después; si no tenía fecha o estaba vencida, mañana a la misma hora. */
    fun postponedDueDate(dueDate: Long, now: Long): Long {
        if (dueDate <= 0L) {
            return addDays(DateUtils.startOfDay(now), 1) + 9 * 60 * 60 * 1000L
        }
        var next = addDays(dueDate, 1)
        while (next <= now) next = addDays(next, 1)
        return next
    }
}
