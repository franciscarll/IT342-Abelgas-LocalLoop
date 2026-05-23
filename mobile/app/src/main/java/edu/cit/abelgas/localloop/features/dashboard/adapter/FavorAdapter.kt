package edu.cit.abelgas.localloop.features.dashboard.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.databinding.ItemFavorBinding
import edu.cit.abelgas.localloop.features.dashboard.model.FavorDto
import edu.cit.abelgas.localloop.shared.util.TimeUtils
import java.util.Calendar

/**
 * FavorAdapter
 *
 * Mirrors web FavorCard component behaviour:
 *  • Own favor → shows "Your Favor" badge (disabled, gray)
 *  • Other's favor → shows orange "Claim" button
 *  • Full card is clickable → onCardClick
 *  • Claim button click stops propagation (matches web e.stopPropagation())
 */
class FavorAdapter(
    private val currentUserId: Long?,
    private val onClaim: (Long) -> Unit,
    private val onCardClick: (FavorDto) -> Unit
) : ListAdapter<FavorDto, FavorAdapter.FavorViewHolder>(FavorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavorViewHolder {
        val binding = ItemFavorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FavorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FavorViewHolder(private val b: ItemFavorBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(favor: FavorDto) {
            // ── Title & description ───────────────────────────────────────────
            b.tvFavorTitle.text = favor.title
            b.tvFavorDescription.text = favor.description

            // ── Category icon background (orange tint circle) ─────────────────
            val iconRes = categoryIcon(favor.category)
            b.ivCategoryIcon.setImageResource(iconRes)

            // ── Category tag chip ─────────────────────────────────────────────
            val (tagBg, tagText) = categoryTagColors(favor.category)
            b.tvCategoryTag.text = favor.category
            b.tvCategoryTag.backgroundTintList = ColorStateList.valueOf(tagBg)
            b.tvCategoryTag.setTextColor(tagText)

            // ── Meta: avatar + name + barangay + time ─────────────────────────
            val name = favor.requesterName ?: "?"
            b.tvAvatarMini.text = initials(name)
            b.tvAvatarMini.backgroundTintList = ColorStateList.valueOf(avatarColor(name))
            b.tvRequesterName.text = name
            b.tvBarangay.text = favor.barangay ?: ""
            b.tvTimeAgo.text = TimeUtils.timeAgo(favor.createdAt)

            // ── Own favor vs claimable ────────────────────────────────────────
            val isOwn = currentUserId != null && favor.requesterId == currentUserId
            b.btnClaim.visibility = if (isOwn) View.GONE else View.VISIBLE
            b.tvOwnFavorBadge.visibility = if (isOwn) View.VISIBLE else View.GONE

            // ── Claim button ──────────────────────────────────────────────────
            b.btnClaim.setOnClickListener {
                // stopPropagation equivalent — listener is on button only
                onClaim(favor.id)
            }

            // ── Card click → detail ───────────────────────────────────────────
            b.root.setOnClickListener { onCardClick(favor) }
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        private fun initials(name: String): String =
            name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("").take(2).uppercase()

        private fun avatarColor(name: String): Int {
            val colors = listOf(
                0xFFC8601A.toInt(), 0xFF2E86AB.toInt(), 0xFFA23B72.toInt(),
                0xFFF18F01.toInt(), 0xFF44BBA4.toInt(), 0xFFE94F37.toInt(),
                0xFF6B4226.toInt(), 0xFF3A86FF.toInt()
            )
            var hash = 0
            for (c in name) hash = c.code + ((hash shl 5) - hash)
            return colors[Math.abs(hash) % colors.size]
        }

        /** Maps web CATEGORY_ICONS to Android drawable resources */
        private fun categoryIcon(category: String?): Int = when (category) {
            "Errand"          -> R.drawable.ic_category_errand
            "Pet Care"        -> R.drawable.ic_category_pet
            "Tool Borrowing"  -> R.drawable.ic_category_tool
            "Plant Watering"  -> R.drawable.ic_category_plant
            else              -> R.drawable.ic_category_other
        }

        /** Maps web CATEGORY_TAG_COLORS to Android color ints */
        private fun categoryTagColors(category: String?): Pair<Int, Int> = when (category) {
            "Errand"          -> Pair(0xFFFFF3E0.toInt(), 0xFFE65100.toInt())
            "Pet Care"        -> Pair(0xFFF3E5F5.toInt(), 0xFF7B1FA2.toInt())
            "Tool Borrowing"  -> Pair(0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt())
            "Plant Watering"  -> Pair(0xFFE0F7FA.toInt(), 0xFF00695C.toInt())
            else              -> Pair(0xFFF5F5F5.toInt(), 0xFF424242.toInt())
        }
    }

    class FavorDiffCallback : DiffUtil.ItemCallback<FavorDto>() {
        override fun areItemsTheSame(old: FavorDto, new: FavorDto) = old.id == new.id
        override fun areContentsTheSame(old: FavorDto, new: FavorDto) = old == new
    }
}