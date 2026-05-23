package edu.cit.abelgas.localloop.features.favorfeed.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cit.abelgas.localloop.databinding.ItemFavorCardBinding
import edu.cit.abelgas.localloop.features.dashboard.model.FavorDto
import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedConstants.AVATAR_COLORS
import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedConstants.CATEGORY_ICONS
import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedConstants.CATEGORY_TAG_COLORS
import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedConstants.STATUS_COLORS
import java.time.Instant
import java.time.temporal.ChronoUnit

class FavorCardAdapter(
    private val currentUserId: Long?,
    private val onCardClick : (FavorDto) -> Unit,
    private val onClaimClick: (FavorDto) -> Unit
) : ListAdapter<FavorDto, FavorCardAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemFavorCardBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(favor: FavorDto) {
            val category = favor.category.ifEmpty { "Other" }
            val status   = favor.status.ifEmpty { "OPEN" }
            val isOwn    = favor.requesterId == currentUserId

            // ── Category icon emoji ───────────────────────────────────────────
            b.tvCategoryIcon.text = CATEGORY_ICONS[category] ?: "📦"

            // ── Title + description ───────────────────────────────────────────
            b.tvTitle.text       = favor.title
            b.tvDescription.text = favor.description

            // ── Status badge ──────────────────────────────────────────────────
            val (statusBg, statusText) = STATUS_COLORS[status]
                ?: Pair("#FFF3E0", "#E65100")
            b.tvStatus.text = status
            b.tvStatus.setTextColor(Color.parseColor(statusText))
            b.tvStatus.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(statusBg))

            // ── Category tag (right column) ───────────────────────────────────
            val (catBg, catText) = CATEGORY_TAG_COLORS[category]
                ?: Pair("#F5F5F5", "#424242")
            b.tvCategoryTag.text = category
            b.tvCategoryTag.setTextColor(Color.parseColor(catText))
            b.tvCategoryTag.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(catBg))

            // ── Meta row ──────────────────────────────────────────────────────
            val name = favor.requesterName ?: "?"
            b.tvMiniAvatar.text = initials(name)
            b.tvMiniAvatar.backgroundTintList =
                ColorStateList.valueOf(avatarColor(name))
            b.tvRequesterName.text = name
            b.tvBarangay.text      = favor.barangay ?: ""
            b.tvTimeAgo.text       = timeAgo(favor.createdAt)

            // ── Claim button states ───────────────────────────────────────────
            // Mirrors web: OPEN+!isOwn → Claim | OPEN+isOwn → Your Favor | else → Claimed/Completed
            when {
                status == "OPEN" && !isOwn -> {
                    b.btnClaim.visibility = View.VISIBLE
                    b.btnClaim.isEnabled  = true
                    b.btnClaim.text       = "Claim"
                    b.btnClaim.setTextColor(Color.WHITE)
                    b.btnClaim.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#C8601A"))
                }
                status == "OPEN" && isOwn -> {
                    // mirrors web ownFavorBtn
                    b.btnClaim.visibility = View.VISIBLE
                    b.btnClaim.isEnabled  = false
                    b.btnClaim.text       = "Your Favor"
                    b.btnClaim.setTextColor(Color.parseColor("#AAAAAA"))
                    b.btnClaim.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#F0F0F0"))
                }
                else -> {
                    // mirrors web claimedBtn
                    b.btnClaim.visibility = View.VISIBLE
                    b.btnClaim.isEnabled  = false
                    b.btnClaim.text       =
                        if (status == "CLAIMED") "Claimed" else "Completed"
                    b.btnClaim.setTextColor(Color.parseColor("#888888"))
                    b.btnClaim.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#F0F0F0"))
                }
            }

            b.btnClaim.setOnClickListener {
                if (status == "OPEN" && !isOwn) onClaimClick(favor)
            }

            // ── Whole card tap → detail ───────────────────────────────────────
            b.root.setOnClickListener { onCardClick(favor) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemFavorCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    // ── Utility helpers — exact mirrors of DashboardActivity helpers ──────────

    private fun initials(name: String): String =
        name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("").take(2).uppercase()

    private fun avatarColor(name: String): Int {
        var hash = 0
        for (c in name) hash = c.code + ((hash shl 5) - hash)
        return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.size]
    }

    // Mirrors web timeAgo() exactly
    private fun timeAgo(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return ""
        return try {
            val then = Instant.parse(dateStr)
            val diff = ChronoUnit.SECONDS.between(then, Instant.now())
            when {
                diff < 60    -> "${diff}s ago"
                diff < 3600  -> "${diff / 60} min ago"
                diff < 86400 -> "${diff / 3600} hr ago"
                else         -> "${diff / 86400} d ago"
            }
        } catch (e: Exception) { "" }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FavorDto>() {
            override fun areItemsTheSame(a: FavorDto, b: FavorDto) = a.id == b.id
            override fun areContentsTheSame(a: FavorDto, b: FavorDto) = a == b
        }
    }
}