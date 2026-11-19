package com.example.studybuddy

import com.example.studybuddy.agenda.AgendaGrouper
import com.example.studybuddy.agenda.AgendaRow
import com.example.studybuddy.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AgendaGrouperTest {

    private fun at(day: Int, hour: Int): Long = Calendar.getInstance().apply {
        clear(); set(2026, Calendar.MARCH, day, hour, 0)
    }.timeInMillis

    private val now = at(10, 12)

    private fun task(id: Int, due: Long, done: Boolean = false) =
        Task(id = id, title = "t$id", dueDate = due, isCompleted = done)

    @Test
    fun groupsIntoSectionsInOrder() {
        val tasks = listOf(
            task(1, at(8, 9)),       // vencida
            task(2, at(10, 18)),     // hoy
            task(3, at(11, 9)),      // mañana
            task(4, at(13, 9)),      // esta semana
            task(5, at(25, 9)),      // más adelante
            task(6, 0L),             // sin fecha
            task(7, at(10, 9), done = true) // completada: fuera
        )
        val headers = AgendaGrouper.group(tasks, now, 7)
            .filterIsInstance<AgendaRow.Header>().map { it.kind }
        assertEquals(
            listOf(
                AgendaRow.Kind.OVERDUE, AgendaRow.Kind.TODAY, AgendaRow.Kind.TOMORROW,
                AgendaRow.Kind.DAY, AgendaRow.Kind.LATER, AgendaRow.Kind.NO_DATE
            ),
            headers
        )
        val ids = AgendaGrouper.group(tasks, now, 7)
            .filterIsInstance<AgendaRow.Item>().map { it.task.id }
        assertEquals(listOf(1, 2, 3, 4, 5, 6), ids)
    }

    @Test
    fun todayOnlyHorizonPutsRestInLater() {
        val rows = AgendaGrouper.group(listOf(task(2, at(10, 18)), task(3, at(11, 9))), now, 1)
        val headers = rows.filterIsInstance<AgendaRow.Header>().map { it.kind }
        assertEquals(listOf(AgendaRow.Kind.TODAY, AgendaRow.Kind.LATER), headers)
    }

    @Test
    fun earlierTodayIsStillToday() {
        val rows = AgendaGrouper.group(listOf(task(1, at(10, 8))), now, 7)
        assertEquals(AgendaRow.Kind.TODAY, (rows.first() as AgendaRow.Header).kind)
    }

    @Test
    fun postponeMovesOneDayIntoTheFuture() {
        assertEquals(at(11, 18), AgendaGrouper.postponedDueDate(at(10, 18), now))
        // Vencida hace días: salta hasta quedar en el futuro, misma hora.
        assertEquals(at(11, 9), AgendaGrouper.postponedDueDate(at(5, 9), now))
        assertTrue(AgendaGrouper.postponedDueDate(0L, now) > now)
    }
}
