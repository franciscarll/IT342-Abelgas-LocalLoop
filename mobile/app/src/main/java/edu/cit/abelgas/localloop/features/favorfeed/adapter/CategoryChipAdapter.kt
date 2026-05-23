package edu.cit.abelgas.localloop.features.favorfeed.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.cit.abelgas.localloop.databinding.ItemCategoryChipBinding

class CategoryChipAdapter(
    private val categories: List<String>,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<CategoryChipAdapter.ChipViewHolder>() {

    private var activeCategory = "All"

    inner class ChipViewHolder(private val b: ItemCategoryChipBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(label: String, isActive: Boolean) {
            b.tvChipLabel.text = label

            if (isActive) {
                // Active: orange fill + white text — mirrors web catBtnActive
                b.root.setCardBackgroundColor(Color.parseColor("#C8601A"))
                b.root.strokeColor = Color.parseColor("#C8601A")
                b.tvChipLabel.setTextColor(Color.WHITE)
                b.tvChipLabel.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                // Inactive: white fill + grey border — mirrors web catBtn
                b.root.setCardBackgroundColor(Color.WHITE)
                b.root.strokeColor = Color.parseColor("#E8E8E8")
                b.tvChipLabel.setTextColor(Color.parseColor("#555555"))
                b.tvChipLabel.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            b.root.setOnClickListener {
                if (label == activeCategory) return@setOnClickListener
                activeCategory = label
                notifyDataSetChanged()
                onSelect(label)
            }
        }
    }

    /** Called from Activity to sync chip selection with ViewModel state */
    fun setActive(category: String) {
        if (activeCategory == category) return
        val oldIndex = categories.indexOf(activeCategory)
        val newIndex = categories.indexOf(category)
        activeCategory = category
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ChipViewHolder(
        ItemCategoryChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) =
        holder.bind(categories[position], categories[position] == activeCategory)

    override fun getItemCount() = categories.size
}