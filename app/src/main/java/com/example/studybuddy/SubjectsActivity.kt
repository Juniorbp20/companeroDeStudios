package com.example.studybuddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studybuddy.databinding.ActivitySubjectsBinding
import com.example.studybuddy.game.GameUi
import com.example.studybuddy.model.CategoryEntity
import com.example.studybuddy.model.ClassSession
import com.example.studybuddy.subjects.ClassAdapter
import com.example.studybuddy.subjects.ExamAdapter
import com.example.studybuddy.subjects.SubjectCardAdapter
import com.example.studybuddy.subjects.SubjectForms
import com.example.studybuddy.subjects.SubjectTime
import com.example.studybuddy.subjects.SubjectViewModel
import com.example.studybuddy.model.Exam

/** Panel de asignaturas: próximos exámenes, clases de hoy y tarjetas por asignatura. */
class SubjectsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectsBinding
    private lateinit var viewModel: SubjectViewModel

    private val subjectAdapter = SubjectCardAdapter { openSubject(it) }
    private val examAdapter = ExamAdapter(horizontal = true)
    private val todayAdapter = ClassAdapter(showWeekday = false)

    private var subjects: List<CategoryEntity> = emptyList()
    private var classes: List<ClassSession> = emptyList()
    private var exams: List<Exam> = emptyList()
    private var noteCounts: Map<Int, Int> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recyclerExams.adapter = examAdapter
        binding.recyclerTodayClasses.layoutManager = LinearLayoutManager(this)
        binding.recyclerTodayClasses.adapter = todayAdapter
        binding.recyclerSubjects.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerSubjects.adapter = subjectAdapter

        viewModel = ViewModelProvider(this)[SubjectViewModel::class.java]
        viewModel.subjects.observe(this) { subjects = it; render() }
        viewModel.allClasses.observe(this) { classes = it; render() }
        viewModel.upcomingExams.observe(this) { exams = it; render() }
        viewModel.noteCounts.observe(this) { list ->
            noteCounts = list.associate { it.categoryId to it.count }
            render()
        }

        binding.fabAddSubject.setOnClickListener {
            SubjectForms.addSubject(this) { name, color ->
                viewModel.addSubject(name, color, "ic_cat_book") { id -> openSubject(id) }
            }
        }
        GameUi.observeEvents(this)
    }

    private fun render() {
        val byId = subjects.associateBy { it.id }
        examAdapter.submit(exams, byId)
        binding.textExamsEmpty.visibility = if (exams.isEmpty()) View.VISIBLE else View.GONE

        val today = SubjectTime.weekdayOf(System.currentTimeMillis())
        val todayClasses = classes.filter { it.weekday == today }
        todayAdapter.submit(todayClasses, byId)
        binding.textClassesEmpty.visibility = if (todayClasses.isEmpty()) View.VISIBLE else View.GONE

        val classCounts = classes.groupingBy { it.categoryId }.eachCount()
        subjectAdapter.submit(subjects, classCounts, noteCounts)
    }

    private fun openSubject(subject: CategoryEntity) = openSubject(subject.id)

    private fun openSubject(id: Int) {
        startActivity(
            Intent(this, SubjectDetailActivity::class.java)
                .putExtra(SubjectDetailActivity.EXTRA_SUBJECT_ID, id)
        )
    }
}
