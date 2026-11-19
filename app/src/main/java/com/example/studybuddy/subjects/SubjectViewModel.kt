package com.example.studybuddy.subjects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.studybuddy.data.ServiceLocator
import com.example.studybuddy.model.CategoryCount
import com.example.studybuddy.model.CategoryEntity
import com.example.studybuddy.model.ClassSession
import com.example.studybuddy.model.Exam
import com.example.studybuddy.model.Note
import com.example.studybuddy.util.DateUtils
import kotlinx.coroutines.launch

class SubjectViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: SubjectRepository = ServiceLocator.getSubjectRepository(application)

    val subjects: LiveData<List<CategoryEntity>> = repo.subjects.asLiveData()
    val allClasses: LiveData<List<ClassSession>> = repo.allClasses.asLiveData()
    val noteCounts: LiveData<List<CategoryCount>> = repo.noteCounts.asLiveData()
    /** Exámenes desde el inicio de hoy (incluye los de hoy ya empezados). */
    val upcomingExams: LiveData<List<Exam>> =
        repo.upcomingExams(DateUtils.startOfToday()).asLiveData()

    fun classesFor(id: Int): LiveData<List<ClassSession>> = repo.classesFor(id).asLiveData()
    fun examsFor(id: Int): LiveData<List<Exam>> = repo.examsFor(id).asLiveData()
    fun notesFor(id: Int): LiveData<List<Note>> = repo.notesFor(id).asLiveData()

    fun addSubject(name: String, color: Int, icon: String, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch { onDone(repo.addSubject(name, color, icon)) }
    }

    fun loadSubject(id: Int, onLoaded: (CategoryEntity?) -> Unit) {
        viewModelScope.launch { onLoaded(repo.getSubject(id)) }
    }

    fun addClass(s: ClassSession) { viewModelScope.launch { repo.addClass(s) } }
    fun deleteClass(s: ClassSession) { viewModelScope.launch { repo.deleteClass(s) } }
    fun addExam(e: Exam) { viewModelScope.launch { repo.addExam(e) } }
    fun deleteExam(e: Exam) { viewModelScope.launch { repo.deleteExam(e) } }
    fun addNote(n: Note) { viewModelScope.launch { repo.addNote(n) } }
    fun togglePin(n: Note) { viewModelScope.launch { repo.updateNote(n.copy(pinned = !n.pinned)) } }
    fun deleteNote(n: Note) { viewModelScope.launch { repo.deleteNote(n) } }
}
