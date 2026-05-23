package edu.cit.abelgas.localloop.features.dashboard.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cit.abelgas.localloop.databinding.ItemAnnouncementBinding
import edu.cit.abelgas.localloop.databinding.ItemCategoryChipBinding
import edu.cit.abelgas.localloop.features.dashboard.model.AnnouncementDto
import java.text.SimpleDateFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// CategoryChipAdapter
// Renders the horizontal scrollable chip row matching web catBtn / catBtnActive
// ─────────────────────────────────────────────────────────────────────────────
class CategoryChipAdapter(
    private val categories: List<String>,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<CategoryChipAdapter.ChipViewHolder>() {

    private var activeCategory = "All"

    fun setActive(category: String) {
        val oldIndex = categories.indexOf(activeCategory)
        val newIndex = categories.indexOf(category)
        activeCategory = category
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0) notifyItemChanged(newIndex)
    }

    override fun getItemCount() = categories.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemCategoryChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        holder.bind(categories[position], categories[position] == activeCategory)
    }

    inner class ChipViewHolder(private val b: ItemCategoryChipBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(label: String, isActive: Boolean) {
            b.tvChipLabel.text = label

            if (isActive) {
                // Active: orange fill, white text — matches web catBtnActive
                b.root.setCardBackgroundColor(Color.parseColor("#C8601A"))
                b.root.strokeColor = Color.parseColor("#C8601A")
                b.tvChipLabel.setTextColor(Color.WHITE)
                b.tvChipLabel.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                // Inactive: white fill, grey border, dark text — matches web catBtn
                b.root.setCardBackgroundColor(Color.WHITE)
                b.root.strokeColor = Color.parseColor("#E8E8E8")
                b.tvChipLabel.setTextColor(Color.parseColor("#555555"))
                b.tvChipLabel.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            b.root.setOnClickListener { onSelect(label) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AnnouncementAdapter
// Renders announcement rows matching web announcementItem style
// ─────────────────────────────────────────────────────────────────────────────
class AnnouncementAdapter(
    private val onClick: (AnnouncementDto) -> Unit
) : ListAdapter<AnnouncementDto, AnnouncementAdapter.AnnouncementViewHolder>(AnnDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val binding = ItemAnnouncementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AnnouncementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AnnouncementViewHolder(private val b: ItemAnnouncementBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(ann: AnnouncementDto) {
            b.tvAnnouncementTitle.text = ann.title

            // Matches web: formatDate(ann.createdAt || ann.date) · ann.postedBy || 'Admin'
            val dateStr = formatDate(ann.resolvedDate)
            // Using string format instead of concatenation to avoid lint warning
            b.tvAnnouncementMeta.text = String.format("%s · %s", dateStr, ann.resolvedAuthor)

            b.root.setOnClickListener { onClick(ann) }
        }

        /**
         * Mirrors web formatDate():
         * new Date(dateStr).toLocaleDateString('en-US', { month:'short', day:'numeric' })
         * → "Feb 20"
         */
        private fun formatDate(dateStr: String?): String {
            if (dateStr.isNullOrEmpty()) return ""
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val date = parser.parse(dateStr) ?: return ""
                SimpleDateFormat("MMM d", Locale.US).format(date)
            } catch (e: Exception) {
                ""
            }
        }
    }

    class AnnDiffCallback : DiffUtil.ItemCallback<AnnouncementDto>() {
        override fun areItemsTheSame(old: AnnouncementDto, new: AnnouncementDto) =
            old.id == new.id
        override fun areContentsTheSame(old: AnnouncementDto, new: AnnouncementDto) =
            old == new
    }
}