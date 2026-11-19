package com.example.studybuddy.data

import android.content.Context
import androidx.room.withTransaction
import com.example.studybuddy.agenda.AgendaGrouper
import com.example.studybuddy.game.GamificationStore
import com.example.studybuddy.model.AppDatabase
import com.example.studybuddy.model.CategoryDao
import com.example.studybuddy.model.CategoryEntity
import com.example.studybuddy.model.DayCount
import com.example.studybuddy.model.Priority
import com.example.studybuddy.model.RepeatInterval
import com.example.studybuddy.model.SubTask
import com.example.studybuddy.model.SubTaskDao
import com.example.studybuddy.model.Task
import com.example.studybuddy.model.TaskDao
import com.example.studybuddy.notification.AlarmManagerHelper
import com.example.studybuddy.ui.TaskFilter
import com.example.studybuddy.util.DateUtils
import com.example.studybuddy.util.RecurrenceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepository(private val context: Context) {

    private val database: AppDatabase = AppDatabase.getInstance(context)
    private val taskDao: TaskDao = database.taskDao()
    private val subTaskDao: SubTaskDao = database.subTaskDao()
    private val categoryDao: CategoryDao = database.categoryDao()
    private val alarmHelper = AlarmManagerHelper(context)

    /** Las categorías por defecto se siembran en AppDatabase (callback onOpen). */
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val totalCount: Flow<Int> = taskDao.getTotalCount()
    val completedCount: Flow<Int> = taskDao.getCompletedCount()
    val pendingCount: Flow<Int> = taskDao.getPendingCount()

    fun upcomingTasks(limit: Int = 5): Flow<List<Task>> = taskDao.getUpcomingTasks(limit)

    fun overdueCount(now: Long): Flow<Int> = taskDao.getOverdueCount(now)

    fun completedToday(startOfDay: Long, endOfDay: Long): Flow<Int> =
        taskDao.getCompletedToday(startOfDay, endOfDay)

    fun completedThisWeek(weekAgo: Long): Flow<Int> = taskDao.getCompletedThisWeek(weekAgo)

    fun completedPerDay(since: Long): Flow<List<DayCount>> =
        taskDao.getCompletedTimestampsSince(since).map { timestamps ->
            DateUtils.countByLocalDay(timestamps)
                .map { (day, count) -> DayCount(day, count) }
                .sortedBy { it.day }
        }

    fun tasksInRange(rangeStart: Long, rangeEnd: Long): Flow<List<Task>> =
        taskDao.getTasksInRange(rangeStart, rangeEnd)

    fun pendingCountInRange(rangeStart: Long, rangeEnd: Long): Flow<Int> =
        taskDao.getPendingCountInRange(rangeStart, rangeEnd)

    fun subtasksForTask(taskId: Int): Flow<List<SubTask>> = subTaskDao.getSubtasksForTask(taskId)

    fun getTasks(filter: TaskFilter): Flow<List<Task>> {
        return when {
            !filter.query.isNullOrBlank() -> taskDao.searchTasks(filter.query.trim())
            !filter.tag.isNullOrBlank() -> taskDao.getTasksByTag(filter.tag)
            filter.category != null && filter.priority != null ->
                taskDao.getTasksByCategoryAndPriority(filter.category, filter.priority)
            filter.category != null -> taskDao.getTasksByCategory(filter.category)
            filter.priority != null -> taskDao.getTasksByPriority(filter.priority)
            else -> taskDao.getAllTasks()
        }
    }

    suspend fun upsertCategory(category: CategoryEntity): Int = withContext(Dispatchers.IO) {
        val existing = categoryDao.getByName(category.name)
        if (existing != null) {
            existing.id
        } else {
            categoryDao.insert(category).toInt()
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.reassignTasksToGeneral(category.id)
        categoryDao.delete(category)
    }

    suspend fun insert(task: Task): Long = withContext(Dispatchers.IO) {
        taskDao.insert(task)
    }

    suspend fun update(task: Task) = withContext(Dispatchers.IO) {
        taskDao.update(task)
    }

    suspend fun delete(task: Task) = withContext(Dispatchers.IO) {
        taskDao.delete(task)
    }

    /**
     * Elimina la tarea devolviendo sus subtareas, que se borran en cascada,
     * para poder restaurarlas con [restore] (deshacer).
     */
    suspend fun deleteWithSubtasks(task: Task): List<SubTask> = withContext(Dispatchers.IO) {
        val subtasks = subTaskDao.getSubtasksForTaskOnce(task.id)
        taskDao.delete(task)
        alarmHelper.cancelAlarm(task.id)
        subtasks
    }

    /** Reinserta una tarea eliminada (con su id original) y sus subtareas. */
    suspend fun restore(task: Task, subtasks: List<SubTask>): Long = withContext(Dispatchers.IO) {
        val id = taskDao.insert(task)
        subtasks.forEach { subTaskDao.insert(it.copy(id = 0, taskId = id.toInt())) }
        scheduleReminderIfNeeded(task.copy(id = id.toInt()))
        id
    }

    /**
     * Marca una tarea como completada o pendiente. Al completar una tarea recurrente
     * crea su siguiente ocurrencia y programa su recordatorio.
     * Devuelve la nueva ocurrencia creada, si la hay.
     */
    suspend fun setCompleted(task: Task, completed: Boolean): Task? = database.withTransaction {
        // Idempotente y atómico: usa el estado actual en BD (el objeto recibido puede estar
        // desfasado) para no generar ocurrencias duplicadas si el evento llega dos veces.
        val current = taskDao.getById(task.id) ?: return@withTransaction null
        if (current.isCompleted == completed) return@withTransaction null
        val now = System.currentTimeMillis()
        taskDao.update(
            current.copy(isCompleted = completed, completedAt = if (completed) now else 0L)
        )
        if (!completed) {
            removeGeneratedOccurrence(current)
            scheduleReminderIfNeeded(current.copy(isCompleted = false))
            return@withTransaction null
        }
        alarmHelper.cancelAlarm(current.id)
        GamificationStore.recordTaskCompleted(context, current.id, current.priority == Priority.HIGH, now)
        val next = RecurrenceUtils.nextOccurrence(current, now) ?: return@withTransaction null
        val nextId = taskDao.insert(next).toInt()
        subTaskDao.getSubtasksForTaskOnce(current.id).forEach {
            subTaskDao.insert(it.copy(id = 0, taskId = nextId, isCompleted = false))
        }
        next.copy(id = nextId).also { scheduleReminderIfNeeded(it) }
    }

    /**
     * Al reabrir una tarea recurrente, elimina la ocurrencia que se generó al completarla
     * (si sigue pendiente), para no dejar dos tareas activas de la misma serie.
     * La fecha esperada es determinista: se calculó con la hora de completado como "ahora".
     */
    private suspend fun removeGeneratedOccurrence(reopened: Task) {
        if (reopened.repeatInterval <= RepeatInterval.NONE || reopened.completedAt <= 0L) return
        val expectedDue = RecurrenceUtils.nextDueDate(
            reopened.dueDate, reopened.repeatInterval, reopened.completedAt
        )
        val generated = taskDao.findGeneratedOccurrence(
            excludeId = reopened.id,
            title = reopened.title,
            categoryId = reopened.categoryId,
            repeatInterval = reopened.repeatInterval,
            dueDate = expectedDue,
            createdAfter = reopened.completedAt
        ) ?: return
        alarmHelper.cancelAlarm(generated.id)
        taskDao.delete(generated)
    }

    /** Pospone una tarea pendiente un día (o a mañana si no tenía fecha) y reprograma su recordatorio. */
    suspend fun postpone(task: Task): Task = withContext(Dispatchers.IO) {
        val current = taskDao.getById(task.id) ?: task
        val moved = current.copy(
            dueDate = AgendaGrouper.postponedDueDate(current.dueDate, System.currentTimeMillis())
        )
        taskDao.update(moved)
        alarmHelper.cancelAlarm(moved.id)
        scheduleReminderIfNeeded(moved)
        moved
    }

    /** Reprograma los recordatorios de tareas pendientes (tras reinicio o cambio de hora). */
    suspend fun rescheduleAllReminders() = withContext(Dispatchers.IO) {
        taskDao.getPendingRemindersAfter(System.currentTimeMillis()).forEach {
            scheduleReminderIfNeeded(it)
        }
    }

    private fun scheduleReminderIfNeeded(task: Task) {
        if (task.reminderEnabled && !task.isCompleted && task.dueDate > System.currentTimeMillis()) {
            alarmHelper.setAlarm(task.id, task.title, task.description, task.dueDate, task.repeatInterval)
        }
    }

    suspend fun insertSubTask(subTask: SubTask): Long = withContext(Dispatchers.IO) {
        subTaskDao.insert(subTask)
    }

    suspend fun updateSubTask(subTask: SubTask) = withContext(Dispatchers.IO) {
        subTaskDao.update(subTask)
    }

    suspend fun deleteSubTask(subTask: SubTask) = withContext(Dispatchers.IO) {
        subTaskDao.delete(subTask)
    }
}
