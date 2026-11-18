package com.example.studybuddy

import com.example.studybuddy.model.RepeatInterval
import com.example.studybuddy.model.Task
import com.example.studybuddy.util.RecurrenceUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurrenceUtilsTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 9): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis

    @Test
    fun nonRecurringTaskHasNoNextDate() {
        assertEquals(0L, RecurrenceUtils.nextDueDate(at(2026, 0, 10), RepeatInterval.NONE, at(2026, 0, 9)))
    }

    @Test
    fun dailyTaskAdvancesOneDayKeepingTime() {
        val due = at(2026, 0, 10)
        val now = at(2026, 0, 10, 8)
        assertEquals(at(2026, 0, 11), RecurrenceUtils.nextDueDate(due, RepeatInterval.DAILY, now))
    }

    @Test
    fun weeklyTaskAdvancesSevenDays() {
        val due = at(2026, 0, 10)
        assertEquals(at(2026, 0, 17), RecurrenceUtils.nextDueDate(due, RepeatInterval.WEEKLY, at(2026, 0, 10, 8)))
    }

    @Test
    fun lateCompletionSkipsToTheFuture() {
        val due = at(2026, 0, 1)
        val now = at(2026, 0, 5, 12)
        val next = RecurrenceUtils.nextDueDate(due, RepeatInterval.DAILY, now)
        assertEquals(at(2026, 0, 6), next)
        assertTrue(next > now)
    }

    @Test
    fun nextOccurrenceIsPendingNewTask() {
        val task = Task(
            id = 42, title = "Repasar", dueDate = at(2026, 0, 10),
            isCompleted = true, completedAt = at(2026, 0, 10, 8),
            repeatInterval = RepeatInterval.DAILY, tags = "mate"
        )
        val next = RecurrenceUtils.nextOccurrence(task, at(2026, 0, 10, 8))!!
        assertEquals(0, next.id)
        assertFalse(next.isCompleted)
        assertEquals(0L, next.completedAt)
        assertEquals(at(2026, 0, 11), next.dueDate)
        assertEquals("Repasar", next.title)
        assertEquals("mate", next.tags)
    }

    @Test
    fun nextOccurrenceIsNullForNonRecurring() {
        assertNull(RecurrenceUtils.nextOccurrence(Task(title = "x")))
    }

    @Test
    fun expectedDueOnReopenMatchesGeneratedOccurrence() {
        // TaskRepository localiza la ocurrencia generada recalculando la fecha con completedAt.
        val completedAt = at(2026, 0, 12, 18)
        val task = Task(
            title = "Leer", dueDate = at(2026, 0, 10),
            repeatInterval = RepeatInterval.WEEKLY, completedAt = completedAt, isCompleted = true
        )
        val generated = RecurrenceUtils.nextOccurrence(task, completedAt)!!
        assertEquals(
            generated.dueDate,
            RecurrenceUtils.nextDueDate(task.dueDate, task.repeatInterval, task.completedAt)
        )
        assertEquals(completedAt, generated.createdAt)
    }
}
