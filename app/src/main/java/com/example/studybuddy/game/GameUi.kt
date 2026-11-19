package com.example.studybuddy.game

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.studybuddy.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/** Textos y celebraciones de la gamificación. */
object GameUi {

    fun rankName(context: Context, level: Int): String = context.getString(
        when (GamificationEngine.rankIndex(level)) {
            0 -> R.string.rank_0
            1 -> R.string.rank_1
            2 -> R.string.rank_2
            3 -> R.string.rank_3
            4 -> R.string.rank_4
            else -> R.string.rank_5
        }
    )

    fun title(context: Context, a: Achievement): String = context.getString(texts(a.id).first)

    fun description(context: Context, a: Achievement): String = context.getString(texts(a.id).second)

    private fun texts(id: String): Pair<Int, Int> = when (id) {
        "first_task" -> R.string.ach_first_task_title to R.string.ach_first_task_desc
        "tasks_10" -> R.string.ach_tasks_10_title to R.string.ach_tasks_10_desc
        "tasks_50" -> R.string.ach_tasks_50_title to R.string.ach_tasks_50_desc
        "tasks_100" -> R.string.ach_tasks_100_title to R.string.ach_tasks_100_desc
        "first_focus" -> R.string.ach_first_focus_title to R.string.ach_first_focus_desc
        "focus_25" -> R.string.ach_focus_25_title to R.string.ach_focus_25_desc
        "focus_hours_10" -> R.string.ach_focus_hours_10_title to R.string.ach_focus_hours_10_desc
        "streak_3" -> R.string.ach_streak_3_title to R.string.ach_streak_3_desc
        "streak_7" -> R.string.ach_streak_7_title to R.string.ach_streak_7_desc
        "streak_30" -> R.string.ach_streak_30_title to R.string.ach_streak_30_desc
        "goal_1" -> R.string.ach_goal_1_title to R.string.ach_goal_1_desc
        "goal_7" -> R.string.ach_goal_7_title to R.string.ach_goal_7_desc
        "level_5" -> R.string.ach_level_5_title to R.string.ach_level_5_desc
        "level_10" -> R.string.ach_level_10_title to R.string.ach_level_10_desc
        "early_bird" -> R.string.ach_early_bird_title to R.string.ach_early_bird_desc
        "night_owl" -> R.string.ach_night_owl_title to R.string.ach_night_owl_desc
        "first_exam" -> R.string.ach_first_exam_title to R.string.ach_first_exam_desc
        else -> R.string.ach_notes_10_title to R.string.ach_notes_10_desc
    }

    /**
     * Celebra en la pantalla visible los eventos de juego mientras esté en primer plano:
     * subida de nivel o logros → diálogo; solo XP → snackbar breve.
     */
    fun observeEvents(activity: AppCompatActivity, onEvent: () -> Unit = {}) {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                GamificationStore.events.collect { event ->
                    show(activity, event)
                    onEvent()
                }
            }
        }
    }

    fun show(activity: AppCompatActivity, event: GameEvent) {
        if (event.isEmpty || activity.isFinishing) return
        val level = event.newLevel
        when {
            event.unlocked.isNotEmpty() -> {
                val body = buildString {
                    event.unlocked.forEach { a ->
                        append(a.emoji).append("  ").append(title(activity, a)).append('\n')
                        append(description(activity, a)).append("  ·  ")
                        append(activity.getString(R.string.game_achievement_reward, a.xpReward))
                        append("\n\n")
                    }
                    if (level != null) {
                        append(activity.getString(R.string.game_level_up_message, level, rankName(activity, level)))
                    }
                }.trim()
                MaterialAlertDialogBuilder(activity)
                    .setTitle("🏆  " + activity.getString(R.string.game_achievement_unlocked_title))
                    .setMessage(body)
                    .setPositiveButton(R.string.game_awesome, null)
                    .show()
            }
            level != null -> {
                MaterialAlertDialogBuilder(activity)
                    .setTitle("⭐  " + activity.getString(R.string.game_level_up_title))
                    .setMessage(activity.getString(R.string.game_level_up_message, level, rankName(activity, level)))
                    .setPositiveButton(R.string.game_awesome, null)
                    .show()
            }
            else -> {
                val root = activity.findViewById<android.view.View>(android.R.id.content) ?: return
                Snackbar.make(root, "✨ " + activity.getString(R.string.game_xp_gained, event.xpGained), Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }
}
