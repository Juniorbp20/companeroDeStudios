package com.example.studybuddy.game

import java.util.Calendar

/** Métricas acumuladas del estudiante que alimentan niveles y logros. */
data class GameStats(
    val xp: Int = 0,
    val tasksCompleted: Int = 0,
    val focusSessions: Int = 0,
    val focusMinutes: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val goalDaysMet: Int = 0,
    val earlyTasks: Int = 0,
    val nightSessions: Int = 0,
    val examsCreated: Int = 0,
    val notesCreated: Int = 0
) {
    val level: Int get() = GamificationEngine.levelFor(xp)
}

/** Métrica sobre la que se mide un logro. */
enum class Metric(val read: (GameStats) -> Int) {
    TASKS({ it.tasksCompleted }),
    FOCUS_SESSIONS({ it.focusSessions }),
    FOCUS_MINUTES({ it.focusMinutes }),
    STREAK({ maxOf(it.currentStreak, it.bestStreak) }),
    LEVEL({ it.level }),
    GOAL_DAYS({ it.goalDaysMet }),
    EARLY_TASKS({ it.earlyTasks }),
    NIGHT_SESSIONS({ it.nightSessions }),
    EXAMS({ it.examsCreated }),
    NOTES({ it.notesCreated })
}

/** Definición de un logro. Los textos visibles se resuelven en la capa de UI por [id]. */
data class Achievement(
    val id: String,
    val emoji: String,
    val metric: Metric,
    val target: Int,
    val xpReward: Int
) {
    fun progress(stats: GameStats): Int = minOf(metric.read(stats), target)
    fun isUnlocked(stats: GameStats): Boolean = metric.read(stats) >= target
}

/** Resultado de una acción: XP ganada, subida de nivel y logros recién desbloqueados. */
data class GameEvent(
    val xpGained: Int,
    val newLevel: Int?,
    val unlocked: List<Achievement>
) {
    val isEmpty: Boolean get() = xpGained == 0 && newLevel == null && unlocked.isEmpty()
}

object GamificationEngine {

    const val XP_TASK = 10
    const val XP_TASK_HIGH_PRIORITY_BONUS = 5
    const val XP_GOAL_MET = 25
    const val XP_EXAM = 5
    const val XP_NOTE = 2

    val ACHIEVEMENTS: List<Achievement> = listOf(
        Achievement("first_task", "🎯", Metric.TASKS, 1, 10),
        Achievement("tasks_10", "✅", Metric.TASKS, 10, 25),
        Achievement("tasks_50", "🏅", Metric.TASKS, 50, 60),
        Achievement("tasks_100", "🏆", Metric.TASKS, 100, 120),
        Achievement("first_focus", "⏱️", Metric.FOCUS_SESSIONS, 1, 10),
        Achievement("focus_25", "🧠", Metric.FOCUS_SESSIONS, 25, 60),
        Achievement("focus_hours_10", "📚", Metric.FOCUS_MINUTES, 600, 80),
        Achievement("streak_3", "🔥", Metric.STREAK, 3, 20),
        Achievement("streak_7", "⚡", Metric.STREAK, 7, 50),
        Achievement("streak_30", "🌋", Metric.STREAK, 30, 150),
        Achievement("goal_1", "🥇", Metric.GOAL_DAYS, 1, 15),
        Achievement("goal_7", "🌟", Metric.GOAL_DAYS, 7, 60),
        Achievement("level_5", "⭐", Metric.LEVEL, 5, 40),
        Achievement("level_10", "👑", Metric.LEVEL, 10, 100),
        Achievement("early_bird", "🌅", Metric.EARLY_TASKS, 1, 15),
        Achievement("night_owl", "🦉", Metric.NIGHT_SESSIONS, 1, 15),
        Achievement("first_exam", "📝", Metric.EXAMS, 1, 10),
        Achievement("notes_10", "🗒️", Metric.NOTES, 10, 25)
    )

    /** XP acumulada necesaria para alcanzar el nivel [level] (nivel 1 = 0 XP; 2 = 100; 3 = 300; 4 = 600…). */
    fun xpForLevel(level: Int): Int = if (level <= 1) 0 else 50 * level * (level - 1)

    fun levelFor(xp: Int): Int {
        var level = 1
        while (xpForLevel(level + 1) <= xp) level++
        return level
    }

    /** Progreso dentro del nivel actual: (XP ganada en el nivel, XP necesaria para el siguiente). */
    fun levelProgress(xp: Int): Pair<Int, Int> {
        val level = levelFor(xp)
        val base = xpForLevel(level)
        return (xp - base) to (xpForLevel(level + 1) - base)
    }

    fun xpForTask(highPriority: Boolean): Int =
        XP_TASK + if (highPriority) XP_TASK_HIGH_PRIORITY_BONUS else 0

    /** XP por una sesión de enfoque: 1 XP cada 2 minutos, mínimo 5. */
    fun xpForFocus(minutes: Int): Int = maxOf(5, minutes / 2)

    /** Clave de día local (yyyymmdd como entero) para registrar actividad. */
    fun dayKey(epochMillis: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return c.get(Calendar.YEAR) * 10_000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    }

    private fun previousDayKey(key: Int): Int {
        val c = Calendar.getInstance().apply {
            clear()
            set(key / 10_000, (key / 100) % 100 - 1, key % 100, 12, 0)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return dayKey(c.timeInMillis)
    }

    /**
     * Racha actual: días consecutivos con actividad que terminan hoy o ayer
     * (si hoy aún no hay actividad, la racha de ayer sigue viva).
     */
    fun currentStreak(activeDays: Set<Int>, today: Int): Int {
        var day = if (today in activeDays) today else previousDayKey(today)
        var streak = 0
        while (day in activeDays) {
            streak++
            day = previousDayKey(day)
        }
        return streak
    }

    fun isEarly(epochMillis: Long): Boolean =
        Calendar.getInstance().apply { timeInMillis = epochMillis }.get(Calendar.HOUR_OF_DAY) < 8

    fun isNight(epochMillis: Long): Boolean =
        Calendar.getInstance().apply { timeInMillis = epochMillis }.get(Calendar.HOUR_OF_DAY) >= 22

    /** Logros que [after] ya cumple y que aún no están en [alreadyUnlocked]. */
    fun newlyUnlocked(after: GameStats, alreadyUnlocked: Set<String>): List<Achievement> =
        ACHIEVEMENTS.filter { it.id !in alreadyUnlocked && it.isUnlocked(after) }

    /** Índice del título por nivel (0..n) para mostrar "Novato", "Aprendiz"… */
    fun rankIndex(level: Int): Int = when {
        level >= 15 -> 5
        level >= 10 -> 4
        level >= 7 -> 3
        level >= 4 -> 2
        level >= 2 -> 1
        else -> 0
    }
}
