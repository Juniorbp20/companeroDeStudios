package com.example.studybuddy

import com.example.studybuddy.subjects.SubjectTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class SubjectTimeTest {

    private fun at(month: Int, day: Int, hour: Int) = Calendar.getInstance().apply {
        clear(); set(2026, month, day, hour, 0)
    }.timeInMillis

    @Test
    fun daysUntilCountsCalendarDays() {
        val now = at(Calendar.MARCH, 10, 23)
        assertEquals(0, SubjectTime.daysUntil(now, at(Calendar.MARCH, 10, 8)))
        assertEquals(1, SubjectTime.daysUntil(now, at(Calendar.MARCH, 11, 8)))
        assertEquals(22, SubjectTime.daysUntil(now, at(Calendar.APRIL, 1, 8)))
        assertEquals(-2, SubjectTime.daysUntil(now, at(Calendar.MARCH, 8, 12)))
    }

    @Test
    fun formatsMinutes() {
        assertEquals("08:05", SubjectTime.formatMinutes(8 * 60 + 5))
        assertEquals("23:30", SubjectTime.formatMinutes(23 * 60 + 30))
    }

    @Test
    fun mondayFirstOrdering() {
        assertEquals(0, SubjectTime.mondayFirstIndex(Calendar.MONDAY))
        assertEquals(6, SubjectTime.mondayFirstIndex(Calendar.SUNDAY))
    }

    @Test
    fun urgencyLevels() {
        assertEquals(SubjectTime.Urgency.PAST, SubjectTime.urgency(-1))
        assertEquals(SubjectTime.Urgency.TODAY, SubjectTime.urgency(0))
        assertEquals(SubjectTime.Urgency.SOON, SubjectTime.urgency(3))
        assertEquals(SubjectTime.Urgency.UPCOMING, SubjectTime.urgency(4))
    }
}
