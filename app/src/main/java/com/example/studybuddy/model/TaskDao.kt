package com.example.studybuddy.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks ORDER BY is_completed ASC, due_date ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY due_date ASC")
    fun getPendingTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY due_date ASC LIMIT :limit")
    fun getUpcomingTasks(limit: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE is_completed = 1 ORDER BY due_date DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' ORDER BY is_completed ASC, due_date ASC")
    fun searchTasks(query: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY is_completed ASC, due_date ASC")
    fun getTasksByPriority(priority: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE category_id = :categoryId ORDER BY is_completed ASC, due_date ASC")
    fun getTasksByCategory(categoryId: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE category_id = :categoryId AND priority = :priority ORDER BY is_completed ASC, due_date ASC")
    fun getTasksByCategoryAndPriority(categoryId: Int, priority: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE (',' || tags || ',') LIKE '%,' || :tag || ',%' ORDER BY is_completed ASC, due_date ASC")
    fun getTasksByTag(tag: String): Flow<List<Task>>

    @Query("SELECT COUNT(*) FROM tasks")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 0")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 0 AND due_date < :now AND due_date > 0")
    fun getOverdueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 1 AND completed_at >= :startOfDay AND completed_at < :endOfDay")
    fun getCompletedToday(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 1 AND completed_at >= :weekAgo")
    fun getCompletedThisWeek(weekAgo: Long): Flow<Int>

    /** Marcas de completado desde [since]; se agrupan por día local en Kotlin (no en UTC). */
    @Query("SELECT completed_at FROM tasks WHERE is_completed = 1 AND completed_at >= :since ORDER BY completed_at ASC")
    fun getCompletedTimestampsSince(since: Long): Flow<List<Long>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Task?

    @Query("SELECT * FROM tasks WHERE is_completed = 0 AND reminder_enabled = 1 AND due_date > :now")
    suspend fun getPendingRemindersAfter(now: Long): List<Task>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 0 AND due_date >= :dayStart AND due_date < :dayEnd")
    fun getTasksCountForDay(dayStart: Long, dayEnd: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 0 AND due_date >= :rangeStart AND due_date < :rangeEnd")
    fun getPendingCountInRange(rangeStart: Long, rangeEnd: Long): Flow<Int>

    @Query("SELECT * FROM tasks WHERE is_completed = 0 AND due_date >= :rangeStart AND due_date < :rangeEnd ORDER BY due_date ASC")
    fun getTasksInRange(rangeStart: Long, rangeEnd: Long): Flow<List<Task>>
}

/** [day] es la medianoche local (epoch en ms) del día agrupado. */
data class DayCount(val day: Long, val count: Int)