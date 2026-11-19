package com.example.studybuddy

import com.example.studybuddy.util.DateUtils
import com.example.studybuddy.util.UpdateChecker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class LocalDayGroupingTest {

    private lateinit var originalTz: TimeZone

    @Before
    fun setUp() {
        originalTz = TimeZone.getDefault()
        // UTC-4: una tarea completada a las 22:00 locales cae al día siguiente en UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santo_Domingo"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTz)
    }

    private fun at(day: Int, hour: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.MARCH, day, hour, 0, 0)
    }.timeInMillis

    @Test
    fun lateEveningCompletionStaysOnLocalDay() {
        val counts = DateUtils.countByLocalDay(listOf(at(10, 9), at(10, 22), at(11, 1)))
        assertEquals(2, counts[DateUtils.startOfDay(at(10, 12))])
        assertEquals(1, counts[DateUtils.startOfDay(at(11, 12))])
    }

    @Test
    fun lastNDayStartsAreConsecutiveMidnights() {
        val starts = DateUtils.lastNDayStarts(7, at(10, 15))
        assertEquals(7, starts.size)
        assertEquals(DateUtils.startOfDay(at(4, 12)), starts.first())
        assertEquals(DateUtils.startOfDay(at(10, 12)), starts.last())
    }

    @Test
    fun parsesReleaseTags() {
        assertEquals(8, UpdateChecker.parseVersionCode("v8"))
        assertEquals(8, UpdateChecker.parseVersionCode("v8.0"))
        assertEquals(12, UpdateChecker.parseVersionCode("12.1.3"))
        assertNull(UpdateChecker.parseVersionCode("release-final"))
    }
}
