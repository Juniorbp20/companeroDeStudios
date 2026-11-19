package com.example.studybuddy

import com.example.studybuddy.game.GameStats
import com.example.studybuddy.game.GamificationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GamificationEngineTest {

    private fun key(y: Int, m: Int, d: Int) = y * 10_000 + m * 100 + d

    private fun at(hour: Int): Long = Calendar.getInstance().apply {
        clear(); set(2026, Calendar.MARCH, 10, hour, 30)
    }.timeInMillis

    @Test
    fun levelThresholdsGrow() {
        assertEquals(1, GamificationEngine.levelFor(0))
        assertEquals(1, GamificationEngine.levelFor(99))
        assertEquals(2, GamificationEngine.levelFor(100))
        assertEquals(3, GamificationEngine.levelFor(300))
        assertEquals(4, GamificationEngine.levelFor(600))
    }

    @Test
    fun levelProgressIsRelativeToCurrentLevel() {
        assertEquals(50 to 200, GamificationEngine.levelProgress(150))
        assertEquals(0 to 100, GamificationEngine.levelProgress(0))
    }

    @Test
    fun xpRules() {
        assertEquals(10, GamificationEngine.xpForTask(false))
        assertEquals(15, GamificationEngine.xpForTask(true))
        assertEquals(12, GamificationEngine.xpForFocus(25))
        assertEquals(5, GamificationEngine.xpForFocus(3))
    }

    @Test
    fun streakCountsConsecutiveDaysAcrossMonths() {
        val days = setOf(key(2026, 2, 27), key(2026, 2, 28), key(2026, 3, 1), key(2026, 3, 2))
        assertEquals(4, GamificationEngine.currentStreak(days, key(2026, 3, 2)))
    }

    @Test
    fun streakSurvivesUntilEndOfToday() {
        val days = setOf(key(2026, 3, 8), key(2026, 3, 9))
        assertEquals(2, GamificationEngine.currentStreak(days, key(2026, 3, 10)))
        assertEquals(0, GamificationEngine.currentStreak(days, key(2026, 3, 11)))
    }

    @Test
    fun dayKeyUsesLocalDate() {
        assertEquals(key(2026, 3, 10), GamificationEngine.dayKey(at(23)))
    }

    @Test
    fun earlyAndNightDetection() {
        assertTrue(GamificationEngine.isEarly(at(7)))
        assertFalse(GamificationEngine.isEarly(at(8)))
        assertTrue(GamificationEngine.isNight(at(22)))
        assertFalse(GamificationEngine.isNight(at(21)))
    }

    @Test
    fun achievementsUnlockOnceAndReportProgress() {
        val stats = GameStats(tasksCompleted = 10, currentStreak = 3)
        val unlocked = GamificationEngine.newlyUnlocked(stats, setOf("first_task")).map { it.id }
        assertTrue("tasks_10" in unlocked)
        assertTrue("streak_3" in unlocked)
        assertFalse("first_task" in unlocked)
        val fifty = GamificationEngine.ACHIEVEMENTS.first { it.id == "tasks_50" }
        assertEquals(10, fifty.progress(stats))
    }

    @Test
    fun achievementIdsAreUnique() {
        val ids = GamificationEngine.ACHIEVEMENTS.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
