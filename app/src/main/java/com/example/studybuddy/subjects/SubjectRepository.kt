package com.example.studybuddy.subjects

import android.content.Context
import com.example.studybuddy.game.GamificationStore
import com.example.studybuddy.model.AppDatabase
import com.example.studybuddy.model.CategoryCount
import com.example.studybuddy.model.CategoryEntity
import com.example.studybuddy.model.ClassSession
import com.example.studybuddy.model.Exam
import com.example.studybuddy.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SubjectRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.subjectDao()
    private val categoryDao = db.categoryDao()

    val subjects: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allClasses: Flow<List<ClassSession>> = dao.allClasses()
    val noteCounts: Flow<List<CategoryCount>> = dao.noteCounts()

    fun upcomingExams(since: Long): Flow<List<Exam>> = dao.upcomingExams(since)
    fun classesFor(id: Int): Flow<List<ClassSession>> = dao.classesFor(id)
    fun examsFor(id: Int): Flow<List<Exam>> = dao.examsFor(id)
    fun notesFor(id: Int): Flow<List<Note>> = dao.notesFor(id)

    suspend fun addSubject(name: String, color: Int, icon: String): Int = withContext(Dispatchers.IO) {
        categoryDao.getByName(name)?.id
            ?: categoryDao.insert(CategoryEntity(name = name, icon = icon, color = color)).toInt()
    }

    suspend fun getSubject(id: Int): CategoryEntity? = withContext(Dispatchers.IO) { categoryDao.getById(id) }

    suspend fun addClass(session: ClassSession) = withContext(Dispatchers.IO) { dao.insertClass(session) }
    suspend fun deleteClass(session: ClassSession) = withContext(Dispatchers.IO) { dao.deleteClass(session) }

    suspend fun addExam(exam: Exam) = withContext(Dispatchers.IO) {
        dao.insertExam(exam)
        GamificationStore.recordExamCreated(context)
    }
    suspend fun deleteExam(exam: Exam) = withContext(Dispatchers.IO) { dao.deleteExam(exam) }

    suspend fun addNote(note: Note) = withContext(Dispatchers.IO) {
        dao.insertNote(note)
        GamificationStore.recordNoteCreated(context)
    }
    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) { dao.updateNote(note) }
    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) { dao.deleteNote(note) }
}
