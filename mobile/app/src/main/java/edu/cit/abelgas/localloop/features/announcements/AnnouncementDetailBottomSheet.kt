package edu.cit.abelgas.localloop.features.announcements

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.databinding.FragmentAnnouncementDetailBinding
import edu.cit.abelgas.localloop.features.dashboard.model.AnnouncementDto
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Parcelable wrapper so AnnouncementDto can travel via Bundle safely.
// AnnouncementDto itself is a plain data class (no Parcelable); we wrap it here
// to avoid modifying the shared model.
// ─────────────────────────────────────────────────────────────────────────────
@Parcelize
data class AnnouncementParcel(
    val id: Long,
    val title: String,
    val content: String?,
    val category: String?,
    val isPinned: Boolean,
    val resolvedDate: String?,
    val resolvedAuthor: String
) : Parcelable {
    companion object {
        fun from(dto: AnnouncementDto) = AnnouncementParcel(
            id             = dto.id,
            title          = dto.title,
            content        = dto.content,
            category       = dto.category,
            isPinned       = dto.isPinned,
            resolvedDate   = dto.resolvedDate,
            resolvedAuthor = dto.resolvedAuthor
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AnnouncementDetailBottomSheet
//
// Usage from AnnouncementsActivity / adapter:
//
//   AnnouncementDetailBottomSheet.show(supportFragmentManager, announcement)
//
// ─────────────────────────────────────────────────────────────────────────────
class AnnouncementDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentAnnouncementDetailBinding? = null
    private val binding get() = _binding!!

    // ── Companion — factory + tag ──────────────────────────────────────────
    companion object {
        const val TAG = "AnnouncementDetailBottomSheet"
        private const val ARG_ANNOUNCEMENT = "arg_announcement"

        /**
         * Create and show the bottom sheet in one call.
         *
         * @param manager  The FragmentManager from the host Activity/Fragment.
         * @param dto      The AnnouncementDto tapped by the user.
         */
        fun show(
            manager: androidx.fragment.app.FragmentManager,
            dto: AnnouncementDto
        ) {
            AnnouncementDetailBottomSheet().apply {
                arguments = bundleOf(ARG_ANNOUNCEMENT to AnnouncementParcel.from(dto))
            }.show(manager, TAG)
        }
    }

    // ── Bottom sheet style — rounded top, dim backdrop ────────────────────
    override fun getTheme(): Int = R.style.Theme_LocalLoop_BottomSheet

    // ── Inflate ───────────────────────────────────────────────────────────
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnnouncementDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ── Bind data + configure sheet behaviour ─────────────────────────────
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Expand sheet to HALF_EXPANDED then allow full on scroll
        val sheet = (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)
            ?.behavior
        sheet?.apply {
            state              = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed      = true
            isDraggable        = true
            isFitToContents    = false
            halfExpandedRatio  = 0.55f
        }

        // Close button
        binding.btnClose.setOnClickListener { dismiss() }

        // Populate content
        val ann = arguments?.getParcelable<AnnouncementParcel>(ARG_ANNOUNCEMENT)
        if (ann != null) bindContent(ann) else dismiss()
    }

    // ── Content binding ───────────────────────────────────────────────────
    private fun bindContent(ann: AnnouncementParcel) {

        // ── Date chip ────────────────────────────────────────────────────
        binding.tvDetailDate.text = formatDate(ann.resolvedDate)

        // ── Category badge ───────────────────────────────────────────────
        val (bgColor, textColor) = categoryColors(ann.category)
        binding.tvDetailCategory.text = ann.category ?: "General"
        binding.tvDetailCategory.backgroundTintList = ColorStateList.valueOf(bgColor)
        binding.tvDetailCategory.setTextColor(textColor)

        // ── Pinned indicator ─────────────────────────────────────────────
        binding.layoutPinnedBadge.visibility =
            if (ann.isPinned) View.VISIBLE else View.GONE

        // ── Title ────────────────────────────────────────────────────────
        binding.tvDetailTitle.text = ann.title

        // ── Author ───────────────────────────────────────────────────────
        binding.tvDetailAuthor.text = "Posted by ${ann.resolvedAuthor}"
        binding.tvDetailAuthorInitial.text =
            ann.resolvedAuthor.firstOrNull()?.toString()?.uppercase() ?: "A"

        // ── Body ─────────────────────────────────────────────────────────
        val body = ann.content
        if (body.isNullOrBlank()) {
            binding.tvDetailBody.text = "No additional details provided."
            binding.tvDetailBody.alpha = 0.5f
        } else {
            binding.tvDetailBody.text  = body
            binding.tvDetailBody.alpha = 1f
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "—"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date   = parser.parse(dateStr) ?: return "—"
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        } catch (e: Exception) { "—" }
    }

    /** Returns (backgroundColorInt, textColorInt) for the category badge. */
    private fun categoryColors(category: String?): Pair<Int, Int> =
        when (category?.lowercase()) {
            "event"    -> Pair(0xFFFFF3E0.toInt(), 0xFFE65100.toInt())
            "health"   -> Pair(0xFFF3E5F5.toInt(), 0xFF7B1FA2.toInt())
            "reminder" -> Pair(0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt())
            else       -> Pair(0xFFF5F5F5.toInt(), 0xFF424242.toInt())
        }

    // ── Cleanup ───────────────────────────────────────────────────────────
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}