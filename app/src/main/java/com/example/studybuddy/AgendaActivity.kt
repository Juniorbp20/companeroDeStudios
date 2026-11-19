package com.example.studybuddy

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studybuddy.agenda.AgendaAdapter
import com.example.studybuddy.agenda.AgendaGrouper
import com.example.studybuddy.databinding.ActivityAgendaBinding
import com.example.studybuddy.game.GameUi
import com.example.studybuddy.model.Task
import com.example.studybuddy.ui.TaskViewModel
import com.example.studybuddy.util.DateUtils
import com.google.android.material.snackbar.Snackbar

/** Agenda Hoy / Semana / Todo con gestos para posponer y completar. */
class AgendaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgendaBinding
    private lateinit var viewModel: TaskViewModel
    private lateinit var adapter: AgendaAdapter
    private var latestTasks: List<Task> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgendaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        viewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        adapter = AgendaAdapter(
            onOpen = { task ->
                startActivity(
                    Intent(this, TaskDetailActivity::class.java)
                        .putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.id)
                )
            },
            onComplete = { complete(it) }
        )
        binding.recyclerAgenda.layoutManager = LinearLayoutManager(this)
        binding.recyclerAgenda.adapter = adapter
        attachSwipeGestures()

        binding.toggleRange.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) render()
        }
        binding.fabAgendaAdd.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        viewModel.allTasks.observe(this) { tasks ->
            latestTasks = tasks
            render()
        }
        viewModel.categories.observe(this) { adapter.setCategories(it) }
        GameUi.observeEvents(this)
    }

    private fun horizonDays(): Int = when (binding.toggleRange.checkedButtonId) {
        R.id.btn_range_today -> 1
        R.id.btn_range_week -> 7
        else -> 30
    }

    private fun render() {
        val rows = AgendaGrouper.group(
            latestTasks,
            System.currentTimeMillis(),
            horizonDays(),
            includeLater = binding.toggleRange.checkedButtonId == R.id.btn_range_all
        )
        adapter.submitList(rows)
        val empty = rows.isEmpty()
        binding.layoutAgendaEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerAgenda.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun complete(task: Task) {
        viewModel.setCompleted(task, true)
        Snackbar.make(binding.root, R.string.agenda_completed, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { viewModel.setCompleted(task, false) }
            .show()
    }

    private fun postpone(task: Task) {
        viewModel.postpone(task) { moved ->
            Snackbar.make(
                binding.root,
                getString(R.string.agenda_postponed, DateUtils.formatFull(moved.dueDate)),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun attachSwipeGestures() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ) = false

            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int =
                if (adapter.taskAt(vh.bindingAdapterPosition) == null) 0
                else super.getSwipeDirs(rv, vh)

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.bindingAdapterPosition
                val task = adapter.taskAt(position) ?: return
                // Se repinta la fila; la lista se actualiza sola cuando cambia la base de datos.
                adapter.notifyItemChanged(position)
                if (direction == ItemTouchHelper.RIGHT) postpone(task) else complete(task)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerAgenda)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_agenda, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_month_calendar) {
            startActivity(Intent(this, CalendarActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
