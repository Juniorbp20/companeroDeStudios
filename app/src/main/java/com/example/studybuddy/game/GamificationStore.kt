package com.example.studybuddy.game

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistencia (SharedPreferences) del progreso de gamificación y bus de eventos
 * para que la UI celebre XP, subidas de nivel y logros.
 */
object GamificationStore {

    private const val PREFS = "gamification"
    private const val KEY_XP = "xp"
    private const val KEY_TASKS = "tasks_completed"
    private const val KEY_FOCUS_SESSIONS = "focus_sessions"
    private const val KEY_FOCUS_MINUTES = "focus_minutes"
    private const val KEY_BEST_STREAK = "best_streak"
    private const val KEY_GOAL_DAYS = "goal_days"
    private const val KEY_EARLY = "early_tasks"
    private const val KEY_NIGHT = "night_sessions"
    private const val KEY_EXAMS = "exams_created"
    private const val KEY_NOTES = "notes_created"
    private const val KEY_ACTIVE_DAYS = "active_days"
    private const val KEY_UNLOCKED = "unlocked"
    private const val KEY_AWARDED_TASKS = "awarded_tasks"
    private const val KEY_GOAL_AWARDED_DAY = "goal_awarded_day"

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    /** Eventos de juego para mostrar celebraciones en la pantalla visible. */
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _stats = MutableStateFlow<GameStats?>(null)
    /** Estadísticas actuales; se actualiza tras cada acción. */
    val stats: StateFlow<GameStats?> = _stats.asStateFlow()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun activeDays(context: Context): Set<Int> =
        prefs(context).getStringSet(KEY_ACTIVE_DAYS, emptySet())!!
            .mapNotNull { it.toIntOrNull() }.toSet()

    fun unlockedIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_UNLOCKED, emptySet())!!.toSet()

    fun getStats(context: Context, now: Long = System.currentTimeMillis()): GameStats {
        val p = prefs(context)
        val days = activeDays(context)
        val current = GamificationEngine.currentStreak(days, GamificationEngine.dayKey(now))
        return GameStats(
            xp = p.getInt(KEY_XP, 0),
            tasksCompleted = p.getInt(KEY_TASKS, 0),
            focusSessions = p.getInt(KEY_FOCUS_SESSIONS, 0),
            focusMinutes = p.getInt(KEY_FOCUS_MINUTES, 0),
            currentStreak = current,
            bestStreak = maxOf(p.getInt(KEY_BEST_STREAK, 0), current),
            goalDaysMet = p.getInt(KEY_GOAL_DAYS, 0),
            earlyTasks = p.getInt(KEY_EARLY, 0),
            nightSessions = p.getInt(KEY_NIGHT, 0),
            examsCreated = p.getInt(KEY_EXAMS, 0),
            notesCreated = p.getInt(KEY_NOTES, 0)
        ).also { _stats.value = it }
    }

    /** Publica el estado actual (p. ej. al abrir la app, para que la racha se recalcule). */
    fun refresh(context: Context) {
        getStats(context)
    }

    @Synchronized
    fun recordTaskCompleted(
        context: Context,
        taskId: Int,
        highPriority: Boolean,
        now: Long = System.currentTimeMillis()
    ): GameEvent {
        val p = prefs(context)
        val awarded = p.getStringSet(KEY_AWARDED_TASKS, emptySet())!!.toMutableSet()
        // Evita "farmear" XP marcando y desmarcando la misma tarea.
        if (!awarded.add(taskId.toString())) return GameEvent(0, null, emptyList())
        p.edit().putStringSet(KEY_AWARDED_TASKS, awarded).apply()
        return apply(context, now, GamificationEngine.xpForTask(highPriority)) { e ->
            e.putInt(KEY_TASKS, p.getInt(KEY_TASKS, 0) + 1)
            if (GamificationEngine.isEarly(now)) e.putInt(KEY_EARLY, p.getInt(KEY_EARLY, 0) + 1)
        }
    }

    @Synchronized
    fun recordFocusSession(
        context: Context,
        minutes: Int,
        goalMetToday: Boolean,
        now: Long = System.currentTimeMillis()
    ): GameEvent {
        val p = prefs(context)
        val today = GamificationEngine.dayKey(now)
        val awardGoal = goalMetToday && p.getInt(KEY_GOAL_AWARDED_DAY, 0) != today
        val xp = GamificationEngine.xpForFocus(minutes) +
            if (awardGoal) GamificationEngine.XP_GOAL_MET else 0
        return apply(context, now, xp) { e ->
            e.putInt(KEY_FOCUS_SESSIONS, p.getInt(KEY_FOCUS_SESSIONS, 0) + 1)
            e.putInt(KEY_FOCUS_MINUTES, p.getInt(KEY_FOCUS_MINUTES, 0) + minutes)
            if (GamificationEngine.isNight(now)) e.putInt(KEY_NIGHT, p.getInt(KEY_NIGHT, 0) + 1)
            if (awardGoal) {
                e.putInt(KEY_GOAL_DAYS, p.getInt(KEY_GOAL_DAYS, 0) + 1)
                e.putInt(KEY_GOAL_AWARDED_DAY, today)
            }
        }
    }

    @Synchronized
    fun recordExamCreated(context: Context): GameEvent =
        apply(context, System.currentTimeMillis(), GamificationEngine.XP_EXAM, markActive = false) { e ->
            e.putInt(KEY_EXAMS, prefs(context).getInt(KEY_EXAMS, 0) + 1)
        }

    @Synchronized
    fun recordNoteCreated(context: Context): GameEvent =
        apply(context, System.currentTimeMillis(), GamificationEngine.XP_NOTE, markActive = false) { e ->
            e.putInt(KEY_NOTES, prefs(context).getInt(KEY_NOTES, 0) + 1)
        }

    /**
     * Aplica XP y contadores de forma síncrona (commit), marca el día como activo,
     * suma la XP de los logros desbloqueados y emite el evento resultante.
     */
    private fun apply(
        context: Context,
        now: Long,
        xp: Int,
        markActive: Boolean = true,
        counters: (android.content.SharedPreferences.Editor) -> Unit
    ): GameEvent {
        val p = prefs(context)
        val levelBefore = GamificationEngine.levelFor(p.getInt(KEY_XP, 0))
        val editor = p.edit()
        counters(editor)
        editor.putInt(KEY_XP, p.getInt(KEY_XP, 0) + xp)
        if (markActive) {
            val days = activeDays(context).toMutableSet()
            days.add(GamificationEngine.dayKey(now))
            // Solo se conservan los últimos ~400 días para no crecer sin límite.
            val trimmed = days.sortedDescending().take(400).map { it.toString() }.toSet()
            editor.putStringSet(KEY_ACTIVE_DAYS, trimmed)
        }
        editor.commit()

        var stats = getStats(context, now)
        p.edit().putInt(KEY_BEST_STREAK, stats.bestStreak).commit()

        // Los logros dan XP extra, que a su vez puede desbloquear logros de nivel.
        val unlocked = mutableListOf<Achievement>()
        var bonus = 0
        while (true) {
            val known = unlockedIds(context) + unlocked.map { it.id }
            val fresh = GamificationEngine.newlyUnlocked(stats, known)
            if (fresh.isEmpty()) break
            unlocked += fresh
            val extra = fresh.sumOf { it.xpReward }
            bonus += extra
            p.edit().putInt(KEY_XP, p.getInt(KEY_XP, 0) + extra).commit()
            stats = getStats(context, now)
        }
        if (unlocked.isNotEmpty()) {
            p.edit().putStringSet(KEY_UNLOCKED, unlockedIds(context) + unlocked.map { it.id }).commit()
        }
        val levelAfter = stats.level
        val event = GameEvent(
            xpGained = xp + bonus,
            newLevel = if (levelAfter > levelBefore) levelAfter else null,
            unlocked = unlocked
        )
        if (!event.isEmpty) _events.tryEmit(event)
        return event
    }
}
