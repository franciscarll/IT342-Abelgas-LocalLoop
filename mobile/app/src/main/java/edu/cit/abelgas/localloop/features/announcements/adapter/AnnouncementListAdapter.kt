package edu.cit.abelgas.localloop.features.announcements.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cit.abelgas.localloop.databinding.ItemAnnouncementCardBinding
import edu.cit.abelgas.localloop.features.dashboard.model.AnnouncementDto
import java.text.SimpleDateFormat
import java.util.Locale

class AnnouncementListAdapter(
    private val onReadMore: (AnnouncementDto) -> Unit
) : ListAdapter<AnnouncementDto, AnnouncementListAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AnnouncementDto>() {
            override fun areItemsTheSame(a: AnnouncementDto, b: AnnouncementDto) = a.id == b.id
            override fun areContentsTheSame(a: AnnouncementDto, b: AnnouncementDto) = a == b
        }

        private fun categoryColors(category: String?): Pair<Int, Int> =
            when (category?.lowercase()) {
                "event"    -> Pair(0xFFFFF3E0.toInt(), 0xFFE65100.toInt())
                "health"   -> Pair(0xFFF3E5F5.toInt(), 0xFF7B1FA2.toInt())
                "reminder" -> Pair(0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt())
                else       -> Pair(0xFFF5F5F5.toInt(), 0xFF424242.toInt()) // General / null
            }
    }

    inner class ViewHolder(private val b: ItemAnnouncementCardBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(ann: AnnouncementDto) {
            // Date — uses resolvedDate (createdAt ?: date)
            b.tvDate.text = formatDate(ann.resolvedDate)

            // Category badge
            val (bgColor, textColor) = categoryColors(ann.category)
            b.tvCategory.text = ann.category ?: "General"
            b.tvCategory.backgroundTintList = ColorStateList.valueOf(bgColor)
            b.tvCategory.setTextColor(textColor)

            // Title
            b.tvTitle.text = ann.title

            // Snippet — content can be null in this DTO
            val body = ann.content ?: ""
            b.tvSnippet.text = if (body.length > 120) body.take(120) + "…" else body

            // Author — uses resolvedAuthor (postedBy ?: "Admin")
            b.tvAuthor.text = "Posted by ${ann.resolvedAuthor}"

            // Clicks
            b.btnReadMore.setOnClickListener { onReadMore(ann) }
            b.root.setOnClickListener { onReadMore(ann) }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAnnouncementCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))
}