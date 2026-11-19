package com.example.studybuddy.subjects

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studybuddy.R
import com.example.studybuddy.databinding.ItemClassRowBinding
import com.example.studybuddy.databinding.ItemExamCardBinding
import com.example.studybuddy.databinding.ItemNoteBinding
import com.example.studybuddy.databinding.ItemSubjectCardBinding
import com.example.studybuddy.model.CategoryEntity
import com.example.studybuddy.model.ClassSession
import com.example.studybuddy.model.Exam
import com.example.studybuddy.model.Note
import com.example.studybuddy.model.categoryIconRes
import com.example.studybuddy.model.displayName
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Nombre del día de la semana (numeración de Calendar) en el idioma del dispositivo. */
fun weekdayName(weekday: Int): String =
    DateFormatSymbols.getInstance(Locale.getDefault()).weekdays[weekday]
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

fun examCountdown(context: Context, exam: Exam, now: Long = System.currentTimeMillis()): String {
    val days = SubjectTime.daysUntil(now, exam.date)
    return when {
        days < 0 -> context.getString(R.string.exam_past)
        days == 0 -> context.getString(R.string.exam_today)
        days == 1 -> context.getString(R.string.exam_tomorrow)
        else -> context.getString(R.string.exam_in_days, days)
    }
}

class SubjectCardAdapter(
    private val onClick: (CategoryEntity) -> Unit
) : RecyclerView.Adapter<SubjectCardAdapter.Holder>() {

    private var items: List<CategoryEntity> = emptyList()
    private var classCounts: Map<Int, Int> = emptyMap()
    private var noteCounts: Map<Int, Int> = emptyMap()

    fun submit(subjects: List<CategoryEntity>, classes: Map<Int, Int>, notes: Map<Int, Int>) {
        items = subjects
        classCounts = classes
        noteCounts = notes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemSubjectCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(private val b: ItemSubjectCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: CategoryEntity) {
            val ctx = b.root.context
            b.layoutSubjectBand.setBackgroundColor(s.color)
            b.imageSubjectIcon.setImageResource(s.icon.categoryIconRes() ?: R.drawable.ic_cat_book)
            b.textSubjectName.text = s.displayName(ctx)
            b.textSubjectMeta.text = ctx.getString(
                R.string.subjects_meta, classCounts[s.id] ?: 0, noteCounts[s.id] ?: 0
            )
            b.root.setOnClickListener { onClick(s) }
        }
    }
}

class ExamAdapter(
    private val horizontal: Boolean,
    private val onLongClick: (Exam) -> Unit = {}
) : RecyclerView.Adapter<ExamAdapter.Holder>() {

    private var items: List<Exam> = emptyList()
    private var subjects: Map<Int, CategoryEntity> = emptyMap()

    fun submit(exams: List<Exam>, subjects: Map<Int, CategoryEntity>) {
        items = exams
        this.subjects = subjects
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val b = ItemExamCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (!horizontal) {
            b.root.layoutParams = b.root.layoutParams.apply { width = ViewGroup.LayoutParams.MATCH_PARENT }
        }
        return Holder(b)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(private val b: ItemExamCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: Exam) {
            val ctx = b.root.context
            val subject = subjects[e.categoryId]
            b.cardExam.setCardBackgroundColor(subject?.color ?: CategoryEntity.DEFAULT_COLOR)
            b.textExamSubject.text = subject?.displayName(ctx)?.uppercase(Locale.getDefault()) ?: ""
            b.textExamTitle.text = "📝 " + e.title
            b.textExamCountdown.text = examCountdown(ctx, e)
            b.textExamDate.text = SimpleDateFormat("EEE d MMM · HH:mm", Locale.getDefault()).format(Date(e.date))
            b.root.alpha = if (SubjectTime.urgency(SubjectTime.daysUntil(System.currentTimeMillis(), e.date)) ==
                SubjectTime.Urgency.PAST) 0.6f else 1f
            b.root.setOnLongClickListener { onLongClick(e); true }
        }
    }
}

class ClassAdapter(
    private val showWeekday: Boolean,
    private val onLongClick: (ClassSession) -> Unit = {}
) : RecyclerView.Adapter<ClassAdapter.Holder>() {

    private var items: List<ClassSession> = emptyList()
    private var subjects: Map<Int, CategoryEntity> = emptyMap()

    fun submit(classes: List<ClassSession>, subjects: Map<Int, CategoryEntity>) {
        items = classes.sortedWith(
            compareBy<ClassSession> { SubjectTime.mondayFirstIndex(it.weekday) }.thenBy { it.startMinutes }
        )
        this.subjects = subjects
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemClassRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(private val b: ItemClassRowBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: ClassSession) {
            val ctx = b.root.context
            val subject = subjects[c.categoryId]
            b.viewClassDot.backgroundTintList =
                ColorStateList.valueOf(subject?.color ?: CategoryEntity.DEFAULT_COLOR)
            b.textClassTitle.text = if (showWeekday) weekdayName(c.weekday) else subject?.displayName(ctx) ?: ""
            b.textClassSubtitle.text = listOfNotNull(
                if (showWeekday) null else weekdayName(c.weekday),
                c.room.ifBlank { null }
            ).joinToString(" · ")
            b.textClassTime.text =
                SubjectTime.formatMinutes(c.startMinutes) + "–" + SubjectTime.formatMinutes(c.endMinutes)
            b.root.setOnLongClickListener { onLongClick(c); true }
        }
    }
}

class NoteAdapter(
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.Holder>() {

    private var items: List<Note> = emptyList()

    fun submit(notes: List<Note>) {
        items = notes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(private val b: ItemNoteBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(n: Note) {
            b.textNoteContent.text = n.content
            val date = SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()).format(Date(n.createdAt))
            b.textNoteMeta.text = if (n.pinned) "📌 $date" else date
            b.root.setOnClickListener { onClick(n) }
            b.root.setOnLongClickListener { onLongClick(n); true }
        }
    }
}
