package edu.cit.abelgas.localloop.features.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cit.abelgas.localloop.databinding.ItemRecentActivityBinding
import edu.cit.abelgas.localloop.features.profile.model.RecentActivityItem
import java.text.SimpleDateFormat
import java.util.Locale

class RecentActivityAdapter :
    ListAdapter<RecentActivityItem, RecentActivityAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RecentActivityItem>() {
            override fun areItemsTheSame(a: RecentActivityItem, b: RecentActivityItem) =
                a.id == b.id && a.role == b.role
            override fun areContentsTheSame(a: RecentActivityItem, b: RecentActivityItem) =
                a == b
        }
    }

    inner class VH(private val binding: ItemRecentActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecentActivityItem) {
            binding.tvActivityTitle.text = item.title
            binding.tvActivityRole.text  = item.role
            binding.tvActivityDate.text  = formatDate(item.dateDisplay)

            // Status badge — text + color
            val (label, bgColor, textColor) = statusAppearance(item.status)
            binding.tvActivityStatus.text = label
            binding.tvActivityStatus.backgroundTintList =
                android.content.res.ColorStateList.valueOf(bgColor)
            binding.tvActivityStatus.setTextColor(textColor)

            // Status dot color (left of title row)
            binding.viewStatusDot.backgroundTintList =
                android.content.res.ColorStateList.valueOf(dotColor(item.status))
        }

        /**
         * Returns (label, bgColorInt, textColorInt) for the status chip.
         * Matches web ProfilePage status pill colors exactly.
         */
        private fun statusAppearance(status: String): Triple<String, Int, Int> =
            when (status.uppercase()) {
                "COMPLETED" -> Triple("COMPLETED", 0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt())
                "CLAIMED"   -> Triple("CLAIMED",   0xFFE3F2FD.toInt(), 0xFF1565C0.toInt())
                "OPEN"      -> Triple("OPEN",      0xFFFDE8D8.toInt(), 0xFFC8601A.toInt())
                "EXPIRED"   -> Triple("EXPIRED",   0xFFF5F5F5.toInt(), 0xFF757575.toInt())
                else        -> Triple(status,      0xFFF5F5F5.toInt(), 0xFF424242.toInt())
            }

        /** Dot accent color on the left — mirrors web timeline dot colors */
        private fun dotColor(status: String): Int =
            when (status.uppercase()) {
                "COMPLETED" -> 0xFF4CAF50.toInt()  // green
                "CLAIMED"   -> 0xFF2196F3.toInt()  // blue
                "OPEN"      -> 0xFFC8601A.toInt()  // orange (primary)
                "EXPIRED"   -> 0xFF9E9E9E.toInt()  // grey
                else        -> 0xFFAAAAAA.toInt()
            }

        private fun formatDate(dateStr: String?): String {
            if (dateStr.isNullOrEmpty()) return "—"
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val date   = parser.parse(dateStr) ?: return "—"
                SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
            } catch (e: Exception) { "—" }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRecentActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}