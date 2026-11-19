package com.example.studybuddy.data

import android.content.Context
import com.example.studybuddy.model.AppDatabase

object ServiceLocator {

    @Volatile
    private var repository: TaskRepository? = null

    fun getRepository(context: Context): TaskRepository {
        return repository ?: synchronized(this) {
            repository ?: TaskRepository(context.applicationContext).also { repository = it }
        }
    }

    @Volatile
    private var subjectRepository: com.example.studybuddy.subjects.SubjectRepository? = null

    fun getSubjectRepository(context: Context): com.example.studybuddy.subjects.SubjectRepository {
        return subjectRepository ?: synchronized(this) {
            subjectRepository ?: com.example.studybuddy.subjects.SubjectRepository(context.applicationContext)
                .also { subjectRepository = it }
        }
    }

    fun clearForTests() {
        repository = null
        subjectRepository = null
    }
}