package edu.cit.abelgas.localloop.features.myactivity.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cit.abelgas.localloop.databinding.ItemActivityFavorCardBinding
import edu.cit.abelgas.localloop.features.myactivity.model.ActivityFavorDto
import edu.cit.abelgas.localloop.features.myactivity.model.ActivityTab
import java.text.SimpleDateFormat
import java.util.Locale

class MyActivityFavorAdapter(
    private val tab: ActivityTab,
    private val currentUserId: Long?,
    private val onCardClick:       (ActivityFavorDto) -> Unit,
    private val onDelete:          (ActivityFavorDto) -> Unit,
    private val onConfirmComplete: (ActivityFavorDto) -> Unit,
    private val onReopen:          (ActivityFavorDto) -> Unit,
    private val onCancelClaim:     (ActivityFavorDto) -> Unit
) : ListAdapter<ActivityFavorDto, MyActivityFavorAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ActivityFavorDto>() {
            override fun areItemsTheSame(a: ActivityFavorDto, b: ActivityFavorDto) = a.id == b.id
            override fun areContentsTheSame(a: ActivityFavorDto, b: ActivityFavorDto) = a == b
        }

        // Status badge colors — mirrors web STATUS_COLORS
        private fun statusColors(status: String): Pair<Int, Int> = when (status) {
            "OPEN"      -> Pair(0xFFFFF3E0.toInt(), 0xFFE65100.toInt())
            "CLAIMED"   -> Pair(0xFFFFF8E1.toInt(), 0xFFF57F17.toInt())
            "COMPLETED" -> Pair(0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt())
            "EXPIRED"   -> Pair(0xFFF5F5F5.toInt(), 0xFF757575.toInt())
            else        -> Pair(0xFFF5F5F5.toInt(), 0xFF424242.toInt())
        }
    }

    // Tracks which card is currently showing delete confirmation
    private var deleteConfirmId: Long? = null
    // Tracks which card is currently loading an action
    var actionLoadingId: Long? = null

    inner class ViewHolder(private val b: ItemActivityFavorCardBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(favor: ActivityFavorDto) {
            val isActing = actionLoadingId == favor.id

            // ── Card click ────────────────────────────────────────────────
            b.root.setOnClickListener { onCardClick(favor) }

            // ── Status badge ──────────────────────────────────────────────
            val (bgColor, textColor) = statusColors(favor.status)
            b.tvStatus.text = favor.status
            b.tvStatus.backgroundTintList = ColorStateList.valueOf(bgColor)
            b.tvStatus.setTextColor(textColor)

            // ── Title + description ───────────────────────────────────────
            b.tvTitle.text       = favor.title
            b.tvDescription.text = favor.description

            // ── Date metadata ─────────────────────────────────────────────
            b.tvMeta.text = when (tab) {
                ActivityTab.POSTED    -> "Posted ${formatDateShort(favor.createdAt)}"
                ActivityTab.CLAIMED   -> "Claimed by you · ${favor.requesterName ?: "Unknown"}"
                ActivityTab.COMPLETED -> "Completed ${formatDateShort(favor.completedAt)}"
            }

            // ── Claimer info row (only for POSTED tab when CLAIMED) ───────
            if (tab == ActivityTab.POSTED && favor.status == "CLAIMED") {
                b.layoutClaimerRow.visibility = View.VISIBLE
                b.tvClaimerName.text = "Claimed by ${favor.claimerName ?: "Unknown"}"
                b.tvClaimerInitial.text =
                    (favor.claimerName?.firstOrNull()?.toString() ?: "?").uppercase()
            } else if (tab == ActivityTab.COMPLETED) {
                b.layoutClaimerRow.visibility = View.VISIBLE
                b.tvClaimerName.text = "Helped by ${favor.claimerName ?: "Unknown"}"
                b.tvClaimerInitial.text =
                    (favor.claimerName?.firstOrNull()?.toString() ?: "?").uppercase()
            } else {
                b.layoutClaimerRow.visibility = View.GONE
            }

            // ── +1 rep badge (Completed tab only) ────────────────────────
            b.tvRepBadge.visibility =
                if (tab == ActivityTab.COMPLETED) View.VISIBLE else View.GONE

            // ── Action buttons — state-conditional per tab + status ───────
            resetAllActionViews()

            when (tab) {
                ActivityTab.POSTED -> bindPostedActions(favor, isActing)
                ActivityTab.CLAIMED -> bindClaimedActions(favor, isActing)
                ActivityTab.COMPLETED -> { /* read-only — no actions */ }
            }
        }

        private fun bindPostedActions(favor: ActivityFavorDto, isActing: Boolean) {
            when (favor.status) {
                "OPEN" -> {
                    if (deleteConfirmId == favor.id) {
                        // Delete confirmation UI
                        b.layoutDeleteConfirm.visibility = View.VISIBLE
                        b.btnConfirmDelete.isEnabled     = !isActing
                        b.btnCancelDelete.isEnabled      = !isActing
                        b.btnConfirmDelete.text          = if (isActing) "Deleting…" else "Yes, Delete"
                        b.btnConfirmDelete.setOnClickListener {
                            onDelete(favor)
                        }
                        b.btnCancelDelete.setOnClickListener {
                            deleteConfirmId = null
                            notifyItemChanged(currentList.indexOfFirst { it.id == favor.id })
                        }
                    } else {
                        // Normal Edit + Delete row
                        b.layoutOpenActions.visibility = View.VISIBLE
                        b.btnDelete.isEnabled          = !isActing
                        b.btnDelete.setOnClickListener {
                            deleteConfirmId = favor.id
                            notifyItemChanged(currentList.indexOfFirst { it.id == favor.id })
                        }
                    }
                }
                "CLAIMED" -> {
                    // Confirm Completion + Reopen buttons
                    b.layoutClaimedActions.visibility = View.VISIBLE
                    b.btnConfirmComplete.isEnabled    = !isActing
                    b.btnReopen.isEnabled             = !isActing
                    b.btnConfirmComplete.text = if (isActing) "Confirming…" else "✓ Confirm Completion"
                    b.btnReopen.text          = if (isActing) "Processing…" else "↺ Re-open Favor"
                    b.btnConfirmComplete.setOnClickListener { onConfirmComplete(favor) }
                    b.btnReopen.setOnClickListener          { onReopen(favor) }
                }
                "COMPLETED" -> {
                    b.tvCompletedInfo.visibility = View.VISIBLE
                    b.tvCompletedInfo.text =
                        "Completed ${formatDateShort(favor.completedAt)}"
                }
                "EXPIRED" -> {
                    b.tvExpiredInfo.visibility = View.VISIBLE
                }
            }
        }

        private fun bindClaimedActions(favor: ActivityFavorDto, isActing: Boolean) {
            b.layoutCancelClaimActions.visibility = View.VISIBLE
            b.btnCancelClaim.isEnabled            = !isActing
            b.btnCancelClaim.text = if (isActing) "Cancelling…" else "✕ Cancel Claim"
            b.btnCancelClaim.setOnClickListener { onCancelClaim(favor) }
        }

        private fun resetAllActionViews() {
            b.layoutOpenActions.visibility        = View.GONE
            b.layoutDeleteConfirm.visibility      = View.GONE
            b.layoutClaimedActions.visibility     = View.GONE
            b.layoutCancelClaimActions.visibility = View.GONE
            b.tvCompletedInfo.visibility          = View.GONE
            b.tvExpiredInfo.visibility            = View.GONE
        }

        private fun formatDateShort(dateStr: String?): String {
            if (dateStr.isNullOrEmpty()) return "—"
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val date   = parser.parse(dateStr) ?: return "—"
                SimpleDateFormat("MMM d", Locale.US).format(date)
            } catch (e: Exception) { "—" }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemActivityFavorCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))
}