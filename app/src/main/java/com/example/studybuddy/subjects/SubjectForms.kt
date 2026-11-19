package com.example.studybuddy.subjects

import android.content.res.ColorStateList
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studybuddy.R
import com.example.studybuddy.databinding.DialogSubjectFormBinding
import com.example.studybuddy.model.CategoryEntity
import com.example.studybuddy.model.ClassSession
import com.example.studybuddy.model.Exam
import com.example.studybuddy.model.Note
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Diálogos de alta para asignaturas, clases, exámenes y notas. */
object SubjectForms {

    /** Paleta vibrante para asignaturas nuevas. */
    val SUBJECT_COLORS = listOf(
        0xFF7C3AED.toInt(), 0xFFF43F5E.toInt(), 0xFFF59E0B.toInt(), 0xFF10B981.toInt(),
        0xFF0EA5E9.toInt(), 0xFF6366F1.toInt(), 0xFFEC4899.toInt(), 0xFF14B8A6.toInt()
    )

    private fun inflate(activity: AppCompatActivity) = DialogSubjectFormBinding.inflate(activity.layoutInflater)

    fun addSubject(activity: AppCompatActivity, onSave: (name: String, color: Int) -> Unit) {
        val b = inflate(activity)
        b.inputLayoutPrimary.setHint(R.string.subject_name_hint)
        b.editPrimary.inputType = android.text.InputType.TYPE_CLASS_TEXT or
            android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        b.chipGroupOptions.visibility = View.VISIBLE
        SUBJECT_COLORS.forEachIndexed { index, color ->
            b.chipGroupOptions.addView(Chip(activity).apply {
                id = View.generateViewId()
                tag = color
                text = "●"
                isCheckable = true
                isChecked = index == 0
                setTextColor(color)
                chipStrokeColor = ColorStateList.valueOf(color)
                chipStrokeWidth = 4f
            })
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.subjects_add)
            .setView(b.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val name = b.editPrimary.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                b.inputLayoutPrimary.error = activity.getString(R.string.subject_name_required)
                return@setOnClickListener
            }
            val checked = b.chipGroupOptions.findViewById<Chip>(b.chipGroupOptions.checkedChipId)
            onSave(name, (checked?.tag as? Int) ?: SUBJECT_COLORS.first())
            dialog.dismiss()
        }
    }

    fun addClass(activity: AppCompatActivity, subject: CategoryEntity, onSave: (ClassSession) -> Unit) {
        val b = inflate(activity)
        b.inputLayoutPrimary.setHint(R.string.class_room_hint)
        b.editPrimary.inputType = android.text.InputType.TYPE_CLASS_TEXT
        b.chipGroupOptions.visibility = View.VISIBLE
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        // Lunes a domingo.
        listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        ).forEach { day ->
            b.chipGroupOptions.addView(Chip(activity).apply {
                id = View.generateViewId()
                tag = day
                text = weekdayName(day).take(3)
                isCheckable = true
                isChecked = day == today
            })
        }
        var start = 8 * 60
        var end = 9 * 60
        b.layoutPickers.visibility = View.VISIBLE
        fun refresh() {
            b.buttonPickFirst.text = activity.getString(R.string.class_start, SubjectTime.formatMinutes(start))
            b.buttonPickSecond.text = activity.getString(R.string.class_end, SubjectTime.formatMinutes(end))
        }
        refresh()
        b.buttonPickFirst.setOnClickListener {
            pickTime(activity, start) { start = it; if (end <= start) end = start + 60; refresh() }
        }
        b.buttonPickSecond.setOnClickListener { pickTime(activity, end) { end = it; refresh() } }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.class_add_title)
            .setView(b.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (end <= start) {
                Toast.makeText(activity, R.string.class_invalid_time, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val chip = b.chipGroupOptions.findViewById<Chip>(b.chipGroupOptions.checkedChipId)
            onSave(
                ClassSession(
                    categoryId = subject.id,
                    weekday = (chip?.tag as? Int) ?: today,
                    startMinutes = start,
                    endMinutes = end,
                    room = b.editPrimary.text?.toString()?.trim().orEmpty()
                )
            )
            dialog.dismiss()
        }
    }

    fun addExam(activity: AppCompatActivity, subject: CategoryEntity, onSave: (Exam) -> Unit) {
        val b = inflate(activity)
        b.inputLayoutPrimary.setHint(R.string.exam_title_hint)
        b.inputLayoutSecondary.visibility = View.VISIBLE
        b.inputLayoutSecondary.setHint(R.string.exam_notes_hint)
        b.layoutPickers.visibility = View.VISIBLE
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 7)
            set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        fun refresh() {
            b.buttonPickFirst.text = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(cal.timeInMillis))
            b.buttonPickSecond.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(cal.timeInMillis))
        }
        refresh()
        b.buttonPickFirst.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.exam_pick_date)
                .setSelection(localDateAsUtcMidnight(cal))
                .build()
            picker.addOnPositiveButtonClickListener { utc ->
                // El selector devuelve medianoche UTC del día elegido.
                val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
                cal.set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
                refresh()
            }
            picker.show(activity.supportFragmentManager, "exam_date")
        }
        b.buttonPickSecond.setOnClickListener {
            pickTime(activity, cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) {
                cal.set(Calendar.HOUR_OF_DAY, it / 60); cal.set(Calendar.MINUTE, it % 60); refresh()
            }
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.exam_add_title)
            .setView(b.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = b.editPrimary.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                b.inputLayoutPrimary.error = activity.getString(R.string.subject_name_required)
                return@setOnClickListener
            }
            onSave(
                Exam(
                    categoryId = subject.id,
                    title = title,
                    date = cal.timeInMillis,
                    notes = b.editSecondary.text?.toString()?.trim().orEmpty()
                )
            )
            dialog.dismiss()
        }
    }

    fun addNote(activity: AppCompatActivity, subject: CategoryEntity, onSave: (Note) -> Unit) {
        val b = inflate(activity)
        b.inputLayoutPrimary.setHint(R.string.note_hint)
        b.editPrimary.minLines = 3
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.note_add_title)
            .setView(b.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val text = b.editPrimary.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@setOnClickListener
            onSave(Note(categoryId = subject.id, content = text))
            dialog.dismiss()
        }
    }

    fun confirmDelete(activity: AppCompatActivity, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setMessage(R.string.item_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pickTime(activity: AppCompatActivity, minutes: Int, onPicked: (Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(minutes / 60)
            .setMinute(minutes % 60)
            .build()
        picker.addOnPositiveButtonClickListener { onPicked(picker.hour * 60 + picker.minute) }
        picker.show(activity.supportFragmentManager, "time_picker")
    }

    private fun localDateAsUtcMidnight(local: Calendar): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
}
