package com.example.studybuddy.agenda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studybuddy.R
import com.example.studybuddy.databinding.ItemAgendaHeaderBinding
import com.example.studybuddy.databinding.ItemAgendaTaskBinding
import com.example.studybuddy.model.CategoryEntity
import com.example.studybuddy.model.Priority
import com.example.studybuddy.model.Task
import com.example.studybuddy.model.displayName
import com.example.studybuddy.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgendaAdapter(
    private val onOpen: (Task) -> Unit,
    private val onComplete: (Task) -> Unit
) : ListAdapter<AgendaRow, RecyclerView.ViewHolder>(Diff) {

    private var categories: Map<Int, CategoryEntity> = emptyMap()

    fun setCategories(list: List<CategoryEntity>) {
        categories = list.associateBy { it.id }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (getItem(position) is AgendaRow.Header) TYPE_HEADER else TYPE_TASK

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemAgendaHeaderBinding.inflate(inflater, parent, false))
        } else {
            TaskHolder(ItemAgendaTaskBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is AgendaRow.Header -> (holder as HeaderHolder).bind(row)
            is AgendaRow.Item -> (holder as TaskHolder).bind(row.task)
        }
    }

    /** Tarea en una posición, o null si es una cabecera (para el gesto de deslizar). */
    fun taskAt(position: Int): Task? = (getItem(position) as? AgendaRow.Item)?.task

    class HeaderHolder(private val b: ItemAgendaHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(h: AgendaRow.Header) {
            val ctx = b.root.context
            b.textHeaderTitle.text = when (h.kind) {
                AgendaRow.Kind.OVERDUE -> ctx.getString(R.string.agenda_overdue)
                AgendaRow.Kind.TODAY -> ctx.getString(R.string.agenda_today)
                AgendaRow.Kind.TOMORROW -> ctx.getString(R.string.agenda_tomorrow)
                AgendaRow.Kind.LATER -> ctx.getString(R.string.agenda_later)
                AgendaRow.Kind.NO_DATE -> ctx.getString(R.string.agenda_no_date)
                AgendaRow.Kind.DAY -> "📅 " + SimpleDateFormat("EEEE d", Locale.getDefault())
                    .format(Date(h.dayStart ?: 0L))
                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
            }
            b.textHeaderCount.text = h.count.toString()
        }
    }

    inner class TaskHolder(private val b: ItemAgendaTaskBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(task: Task) {
            val ctx = b.root.context
            val category = categories[task.categoryId]
            b.textAgendaTitle.text = task.title
            val time = if (task.dueDate > 0) DateUtils.formatTime(task.dueDate) else ctx.getString(R.string.agenda_no_time)
            val priority = when (task.priority) {
                Priority.HIGH -> "🔴 " + ctx.getString(R.string.filter_high)
                Priority.LOW -> "🟢 " + ctx.getString(R.string.filter_low)
                else -> "🟡 " + ctx.getString(R.string.filter_medium)
            }
            b.textAgendaSubtitle.text = listOfNotNull(time, category?.displayName(ctx), priority)
                .joinToString("  ·  ")
            b.viewCategoryBar.setBackgroundColor(category?.color ?: CategoryEntity.DEFAULT_COLOR)
            b.root.setOnClickListener { onOpen(task) }
            b.buttonAgendaComplete.setOnClickListener { onComplete(task) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<AgendaRow>() {
        override fun areItemsTheSame(a: AgendaRow, b: AgendaRow): Boolean = when {
            a is AgendaRow.Item && b is AgendaRow.Item -> a.task.id == b.task.id
            a is AgendaRow.Header && b is AgendaRow.Header -> a.kind == b.kind && a.dayStart == b.dayStart
            else -> false
        }

        override fun areContentsTheSame(a: AgendaRow, b: AgendaRow): Boolean = a == b
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TASK = 1
    }
}
