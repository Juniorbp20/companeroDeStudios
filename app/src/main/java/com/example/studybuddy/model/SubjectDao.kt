package com.example.studybuddy.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    // Clases
    @Query("SELECT * FROM class_sessions ORDER BY weekday ASC, start_minutes ASC")
    fun allClasses(): Flow<List<ClassSession>>

    @Query("SELECT * FROM class_sessions WHERE category_id = :categoryId ORDER BY weekday ASC, start_minutes ASC")
    fun classesFor(categoryId: Int): Flow<List<ClassSession>>

    @Insert
    suspend fun insertClass(session: ClassSession): Long

    @Delete
    suspend fun deleteClass(session: ClassSession)

    // Exámenes
    @Query("SELECT * FROM exams WHERE date >= :since ORDER BY date ASC")
    fun upcomingExams(since: Long): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE category_id = :categoryId ORDER BY date ASC")
    fun examsFor(categoryId: Int): Flow<List<Exam>>

    @Insert
    suspend fun insertExam(exam: Exam): Long

    @Update
    suspend fun updateExam(exam: Exam)

    @Delete
    suspend fun deleteExam(exam: Exam)

    // Notas
    @Query("SELECT * FROM notes WHERE category_id = :categoryId ORDER BY pinned DESC, created_at DESC")
    fun notesFor(categoryId: Int): Flow<List<Note>>

    @Query("SELECT category_id AS categoryId, COUNT(*) AS count FROM notes GROUP BY category_id")
    fun noteCounts(): Flow<List<CategoryCount>>

    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}

data class CategoryCount(val categoryId: Int, val count: Int)
