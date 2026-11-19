package com.example.studybuddy.game

import android.view.View
import com.example.studybuddy.R
import com.example.studybuddy.databinding.ViewGameHeroBinding
import java.util.Locale

/** Rellena la tarjeta de progreso (view_game_hero.xml) con las estadísticas actuales. */
object GameHeroBinder {

    fun bind(hero: ViewGameHeroBinding, stats: GameStats, showCta: Boolean) {
        val ctx = hero.root.context
        val level = stats.level
        val (inLevel, needed) = GamificationEngine.levelProgress(stats.xp)
        hero.textHeroLevel.text = level.toString()
        hero.textHeroRank.text = ctx.getString(R.string.game_level_rank, level, GameUi.rankName(ctx, level))
        hero.textHeroXp.text = ctx.getString(R.string.game_xp_progress, inLevel, needed)
        hero.progressHeroXp.max = needed
        hero.progressHeroXp.setProgressCompat(inLevel, true)
        hero.textHeroStreak.text = ctx.getString(R.string.game_streak_days, stats.currentStreak)
        hero.textHeroTasks.text = stats.tasksCompleted.toString()
        hero.textHeroSessions.text = stats.focusSessions.toString()
        hero.textHeroHours.text = String.format(Locale.getDefault(), "%.1f", stats.focusMinutes / 60f)
        hero.textHeroCta.visibility = if (showCta) View.VISIBLE else View.GONE
    }
}
