package com.example.studybuddy.game

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.studybuddy.R
import com.example.studybuddy.databinding.ItemAchievementBinding
import com.google.android.material.color.MaterialColors

class AchievementAdapter : RecyclerView.Adapter<AchievementAdapter.Holder>() {

    private var items: List<Achievement> = emptyList()
    private var stats: GameStats = GameStats()
    private var unlockedIds: Set<String> = emptySet()

    fun submit(achievements: List<Achievement>, stats: GameStats, unlocked: Set<String>) {
        // Desbloqueados primero; después, los que están más cerca de conseguirse.
        items = achievements.sortedWith(
            compareByDescending<Achievement> { it.id in unlocked }
                .thenByDescending { it.progress(stats).toFloat() / it.target }
        )
        this.stats = stats
        unlockedIds = unlocked
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(private val b: ItemAchievementBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: Achievement) {
            val ctx = b.root.context
            val unlocked = a.id in unlockedIds
            b.textAchievementEmoji.text = a.emoji
            b.textAchievementTitle.text = GameUi.title(ctx, a)
            b.textAchievementDesc.text = GameUi.description(ctx, a)
            b.progressAchievement.max = a.target
            b.progressAchievement.progress = a.progress(stats)
            val reward = ctx.getString(R.string.game_achievement_reward, a.xpReward)
            if (unlocked) {
                b.textAchievementStatus.text = ctx.getString(R.string.game_unlocked) + " · " + reward
                b.cardAchievement.strokeColor = ContextCompat.getColor(ctx, R.color.game_xp)
                b.cardAchievement.strokeWidth = ctx.resources.getDimensionPixelSize(R.dimen.achievement_stroke_unlocked)
                b.cardAchievement.setCardBackgroundColor(
                    MaterialColors.getColor(b.root, com.google.android.material.R.attr.colorTertiaryContainer)
                )
                b.progressAchievement.setIndicatorColor(ContextCompat.getColor(ctx, R.color.game_xp))
                b.root.alpha = 1f
            } else {
                b.textAchievementStatus.text = ctx.getString(
                    R.string.game_achievement_progress, a.progress(stats), a.target
                ) + " · " + reward
                b.cardAchievement.strokeColor =
                    MaterialColors.getColor(b.root, com.google.android.material.R.attr.colorOutline)
                b.cardAchievement.strokeWidth = ctx.resources.getDimensionPixelSize(R.dimen.achievement_stroke_locked)
                b.cardAchievement.setCardBackgroundColor(
                    MaterialColors.getColor(b.root, com.google.android.material.R.attr.colorSurface)
                )
                b.progressAchievement.setIndicatorColor(
                    MaterialColors.getColor(b.root, androidx.appcompat.R.attr.colorPrimary)
                )
                b.root.alpha = 0.65f
            }
            b.textAchievementStatus.setTextColor(
                if (unlocked) ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.game_streak))
                else ColorStateList.valueOf(MaterialColors.getColor(b.root, androidx.appcompat.R.attr.colorPrimary))
            )
        }
    }
}
