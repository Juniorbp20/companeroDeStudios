package com.example.studybuddy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.studybuddy.databinding.ActivityAchievementsBinding
import com.example.studybuddy.game.AchievementAdapter
import com.example.studybuddy.game.GameHeroBinder
import com.example.studybuddy.game.GameUi
import com.example.studybuddy.game.GamificationEngine
import com.example.studybuddy.game.GamificationStore

/** Pantalla de logros: nivel, racha y todas las insignias con su progreso. */
class AchievementsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementsBinding
    private val adapter = AchievementAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerAchievements.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerAchievements.adapter = adapter
        GameUi.observeEvents(this) { render() }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val stats = GamificationStore.getStats(this)
        val unlocked = GamificationStore.unlockedIds(this)
        GameHeroBinder.bind(binding.hero, stats, showCta = false)
        binding.textBestStreak.text = if (stats.currentStreak == 0) {
            getString(R.string.game_streak_zero)
        } else {
            getString(R.string.game_best_streak, stats.bestStreak)
        }
        val total = GamificationEngine.ACHIEVEMENTS.size
        binding.textAchievementsCount.text =
            getString(R.string.game_achievements_count, unlocked.size.coerceAtMost(total), total)
        adapter.submit(GamificationEngine.ACHIEVEMENTS, stats, unlocked)
    }
}
