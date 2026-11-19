package com.example.studybuddy

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studybuddy.databinding.ActivitySubjectDetailBinding
import com.example.studybuddy.game.GameUi
import com.example.studybuddy.model.CategoryEntity
import com.example.studybuddy.model.ClassSession
import com.example.studybuddy.model.Exam
import com.example.studybuddy.model.Note
import com.example.studybuddy.model.displayName
import com.example.studybuddy.subjects.ClassAdapter
import com.example.studybuddy.subjects.ExamAdapter
import com.example.studybuddy.subjects.NoteAdapter
import com.example.studybuddy.subjects.SubjectForms
import com.example.studybuddy.subjects.SubjectViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Detalle de una asignatura con pestañas de horario, exámenes y notas. */
class SubjectDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SUBJECT_ID = "extra_subject_id"
    }

    private lateinit var binding: ActivitySubjectDetailBinding
    private lateinit var viewModel: SubjectViewModel
    private var subject: CategoryEntity? = null

    private var classes: List<ClassSession> = emptyList()
    private var exams: List<Exam> = emptyList()
    private var notes: List<Note> = emptyList()

    private val classAdapter = ClassAdapter(showWeekday = true) { c ->
        SubjectForms.confirmDelete(this) { viewModel.deleteClass(c) }
    }
    private val examAdapter = ExamAdapter(horizontal = false) { e ->
        SubjectForms.confirmDelete(this) { viewModel.deleteExam(e) }
    }
    private val noteAdapter = NoteAdapter(
        onClick = { n -> viewModel.togglePin(n) },
        onLongClick = { n -> SubjectForms.confirmDelete(this) { viewModel.deleteNote(n) } }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubjectDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerSection.layoutManager = LinearLayoutManager(this)

        val id = intent.getIntExtra(EXTRA_SUBJECT_ID, -1)
        if (id <= 0) {
            finish()
            return
        }
        viewModel = ViewModelProvider(this)[SubjectViewModel::class.java]
        viewModel.loadSubject(id) { loaded ->
            if (loaded == null) {
                finish()
                return@loadSubject
            }
            subject = loaded
            applyHeader(loaded)
        }
        viewModel.classesFor(id).observe(this) { classes = it; render() }
        viewModel.examsFor(id).observe(this) { exams = it; render() }
        viewModel.notesFor(id).observe(this) { notes = it; render() }

        binding.toggleSection.addOnButtonCheckedListener { _, _, isChecked -> if (isChecked) render() }
        binding.fabSectionAdd.setOnClickListener { addForCurrentSection() }
        GameUi.observeEvents(this)
    }

    private fun applyHeader(s: CategoryEntity) {
        binding.toolbar.title = s.displayName(this)
        binding.appBar.setBackgroundColor(s.color)
        window.statusBarColor = s.color
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }

    private fun render() {
        binding.textSubjectSummary.text =
            getString(R.string.subject_summary, classes.size, exams.size, notes.size)
        val byId = subject?.let { mapOf(it.id to it) } ?: emptyMap()
        val (adapter, empty, emptyText) = when (binding.toggleSection.checkedButtonId) {
            R.id.btn_section_exams -> {
                examAdapter.submit(exams, byId)
                Triple(examAdapter, exams.isEmpty(), R.string.subject_empty_exams)
            }
            R.id.btn_section_notes -> {
                noteAdapter.submit(notes)
                Triple(noteAdapter, notes.isEmpty(), R.string.subject_empty_notes)
            }
            else -> {
                classAdapter.submit(classes, byId)
                Triple(classAdapter, classes.isEmpty(), R.string.subject_empty_schedule)
            }
        }
        if (binding.recyclerSection.adapter !== adapter) binding.recyclerSection.adapter = adapter
        binding.textSectionEmpty.setText(emptyText)
        binding.textSectionEmpty.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun addForCurrentSection() {
        val s = subject ?: return
        when (binding.toggleSection.checkedButtonId) {
            R.id.btn_section_exams -> SubjectForms.addExam(this, s) { viewModel.addExam(it) }
            R.id.btn_section_notes -> SubjectForms.addNote(this, s) { viewModel.addNote(it) }
            else -> SubjectForms.addClass(this, s) { viewModel.addClass(it) }
        }
    }
}
