package edu.cit.abelgas.localloop.features.favordetail

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.databinding.ActivityFavorDetailBinding
import edu.cit.abelgas.localloop.features.auth.LoginActivity
import edu.cit.abelgas.localloop.features.dashboard.model.FavorDetailDto
import edu.cit.abelgas.localloop.features.dashboard.model.RequesterStatsDto
import edu.cit.abelgas.localloop.features.favorddetail.DetailUiState
import edu.cit.abelgas.localloop.features.favorddetail.ActionState
import edu.cit.abelgas.localloop.features.favorddetail.FavorDetailViewModel
import edu.cit.abelgas.localloop.shared.api.ApiClient
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import java.text.SimpleDateFormat
import java.util.Locale

class FavorDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FAVOR_ID = "extra_favor_id"
    }

    private lateinit var binding: ActivityFavorDetailBinding
    private lateinit var prefs: SharedPreferencesHelper
    private val viewModel: FavorDetailViewModel by viewModels()
    private var currentUserId: Long? = null
    private var favorId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavorDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPreferencesHelper(this)
        val user = prefs.getUser()
        currentUserId = user?.id

        favorId = intent.getLongExtra(EXTRA_FAVOR_ID, -1L)
        if (favorId == -1L) {
            showError("Invalid favor ID.")
            return
        }

        viewModel.apiService = ApiClient.service

        // ✅ Both of these were defined but never called in onCreate
        setupBackButton()
        observeViewModel()

        viewModel.loadFavor(favorId)

        binding.btnRetry.setOnClickListener {
            viewModel.loadFavor(favorId)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back button
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Observe ViewModel
    // ─────────────────────────────────────────────────────────────────────────
    private fun observeViewModel() {

        viewModel.uiState.observe(this) { state ->
            binding.detailLoading.visibility    = View.GONE
            binding.layoutError.visibility      = View.GONE
            binding.nestedScrollView.visibility = View.GONE
            binding.footerCta.visibility        = View.GONE

            when (state) {
                is DetailUiState.Loading -> {
                    binding.detailLoading.visibility = View.VISIBLE
                }
                is DetailUiState.Success -> {
                    binding.nestedScrollView.visibility = View.VISIBLE
                    binding.footerCta.visibility        = View.VISIBLE
                }
                is DetailUiState.Error -> {
                    binding.layoutError.visibility  = View.VISIBLE
                    binding.tvErrorMessage.text     = state.message
                }
            }
        }

        viewModel.favor.observe(this) { favor ->
            favor ?: return@observe
            bindFavorData(favor)
        }

        viewModel.requesterStats.observe(this) { stats ->
            bindRequesterStats(stats)
        }

        viewModel.actionState.observe(this) { state ->
            when (state) {
                is ActionState.Idle -> {
                    binding.btnClaimFavor.isEnabled = true
                    binding.btnClaimFavor.text = "🤝  Claim This Favor"
                    binding.tvActionError.visibility = View.GONE
                }
                is ActionState.Loading -> {
                    binding.btnClaimFavor.isEnabled = false
                    binding.btnClaimFavor.text = "Claiming…"
                    binding.tvActionError.visibility = View.GONE
                }
                is ActionState.ClaimSuccess -> {
                    Snackbar.make(
                        binding.root,
                        "Favor claimed! The requester has been notified.",
                        Snackbar.LENGTH_LONG
                    ).show()
                    viewModel.resetActionState()
                }
                is ActionState.Error -> {
                    binding.tvActionError.visibility = View.VISIBLE
                    binding.tvActionError.text = state.message
                    binding.btnClaimFavor.isEnabled = true
                    binding.btnClaimFavor.text = "🤝  Claim This Favor"
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bind favor data
    // ─────────────────────────────────────────────────────────────────────────
    private fun bindFavorData(favor: FavorDetailDto) {
        val category  = favor.category.ifEmpty { "Other" }
        val status    = favor.status.ifEmpty { "OPEN" }
        val isOwner   = favor.requesterId == currentUserId
        val isClaimer = favor.claimerId  == currentUserId

        binding.tvTitle.text       = favor.title
        binding.tvDescription.text = favor.description

        binding.ivCategoryIcon.setImageResource(categoryIcon(category))

        val (catBg, catText) = categoryTagColors(category)
        binding.tvCategoryBadge.text = category
        binding.tvCategoryBadge.setTextColor(catText)
        binding.tvCategoryBadge.backgroundTintList = ColorStateList.valueOf(catBg)

        val (statusBg, statusText) = statusColors(status)
        binding.tvStatusBadge.text = status
        binding.tvStatusBadge.setTextColor(Color.parseColor(statusText))
        binding.tvStatusBadge.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(statusBg))

        updateStatusTabs(status, isOwner)

        binding.tvDateNeeded.text = formatDateLong(favor.dateNeeded) ?: "—"
        binding.tvPostedDate.text = formatDateTime(favor.createdAt)  ?: "—"
        binding.tvBarangay.text   = favor.barangay ?: "—"
        binding.tvCategory.text   = category

        val claimedDone   = status == "CLAIMED" || status == "COMPLETED"
        val completedDone = status == "COMPLETED"
        updateTimeline(
            postedDate    = favor.createdAt,
            claimedDate   = favor.claimedAt ?: (if (claimedDone) favor.updatedAt else null),
            completedDate = favor.completedAt,
            claimedDone   = claimedDone,
            completedDone = completedDone
        )

        val name = favor.requesterName ?: "?"
        binding.tvRequesterName.text = name
        binding.tvRequesterAvatar.text = initials(name)
        binding.tvRequesterAvatar.backgroundTintList =
            ColorStateList.valueOf(avatarColor(name))

        setupFooterCta(favor, isOwner, isClaimer, status)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Status tabs
    // ─────────────────────────────────────────────────────────────────────────
    private fun updateStatusTabs(status: String, isOwner: Boolean) {
        listOf(binding.tabOpen, binding.tabClaimed, binding.tabOwner).forEach { tab ->
            tab.setTextColor(Color.parseColor("#555555"))
            tab.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
        }
        val isOpen    = status == "OPEN"
        val activeTab = if (isOpen) binding.tabOpen else binding.tabClaimed
        activeTab.setTextColor(Color.WHITE)
        activeTab.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor("#C8601A"))
        if (isOwner) {
            binding.tabOwner.setTextColor(Color.WHITE)
            binding.tabOwner.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#C8601A"))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Timeline
    // ─────────────────────────────────────────────────────────────────────────
    private fun updateTimeline(
        postedDate: String?,
        claimedDate: String?,
        completedDate: String?,
        claimedDone: Boolean,
        completedDone: Boolean
    ) {
        val orange = Color.parseColor("#C8601A")
        val grey   = Color.parseColor("#E0E0E0")

        binding.timelineCircle1.backgroundTintList = ColorStateList.valueOf(orange)
        binding.timelineCircle1.text = "✓"
        binding.timelineCircle1.setTextColor(Color.WHITE)
        binding.tvTimelineLabel1.setTextColor(orange)
        binding.tvTimelineDate1.text = formatDateShort(postedDate) ?: "—"

        if (claimedDone) {
            binding.timelineCircle2.backgroundTintList = ColorStateList.valueOf(orange)
            binding.timelineCircle2.text = "✓"
            binding.timelineCircle2.setTextColor(Color.WHITE)
            binding.tvTimelineLabel2.setTextColor(orange)
            binding.tvTimelineDate2.text = formatDateShort(claimedDate) ?: "—"
            binding.timelineLine1.setBackgroundColor(orange)
        } else {
            binding.timelineCircle2.backgroundTintList = ColorStateList.valueOf(grey)
            binding.timelineCircle2.text = "2"
            binding.timelineCircle2.setTextColor(Color.parseColor("#BBBBBB"))
            binding.tvTimelineLabel2.setTextColor(Color.parseColor("#AAAAAA"))
            binding.tvTimelineDate2.text = "Pending"
            binding.timelineLine1.setBackgroundColor(grey)
        }

        if (completedDone) {
            binding.timelineCircle3.backgroundTintList = ColorStateList.valueOf(orange)
            binding.timelineCircle3.text = "✓"
            binding.timelineCircle3.setTextColor(Color.WHITE)
            binding.tvTimelineLabel3.setTextColor(orange)
            binding.tvTimelineDate3.text = formatDateShort(completedDate) ?: "—"
            binding.timelineLine2.setBackgroundColor(orange)
        } else {
            binding.timelineCircle3.backgroundTintList = ColorStateList.valueOf(grey)
            binding.timelineCircle3.text = "3"
            binding.timelineCircle3.setTextColor(Color.parseColor("#BBBBBB"))
            binding.tvTimelineLabel3.setTextColor(Color.parseColor("#AAAAAA"))
            binding.tvTimelineDate3.text = "Pending"
            binding.timelineLine2.setBackgroundColor(grey)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Requester stats
    // ─────────────────────────────────────────────────────────────────────────
    private fun bindRequesterStats(stats: RequesterStatsDto?) {
        binding.tvRequesterRep.text =
            "⭐ ${stats?.reputationScore ?: 0} reputation points"
        binding.tvMemberSince.text =
            "Member since ${formatMonthYear(stats?.memberSince)}"
        binding.tvFavorsPosted.text    = (stats?.favorsPosted    ?: "—").toString()
        binding.tvFavorsCompleted.text = (stats?.favorsCompleted ?: "—").toString()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Footer CTA
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupFooterCta(
        favor: FavorDetailDto,
        isOwner: Boolean,
        isClaimer: Boolean,
        status: String
    ) {
        binding.tvActionNote.visibility  = View.GONE
        binding.btnClaimFavor.visibility = View.GONE
        binding.tvActionError.visibility = View.GONE

        when {
            status == "OPEN" && !isOwner -> {
                binding.btnClaimFavor.visibility = View.VISIBLE
                binding.btnClaimFavor.text = "🤝  Claim This Favor"
                binding.btnClaimFavor.isEnabled = true
                binding.btnClaimFavor.setOnClickListener {
                    viewModel.claimFavor(favor.id)
                }
            }
            status == "OPEN" && isOwner -> {
                binding.tvActionNote.visibility = View.VISIBLE
                binding.tvActionNote.text =
                    "This is your favor. Wait for a neighbor to claim it."
            }
            status == "CLAIMED" && isClaimer -> {
                binding.tvActionNote.visibility = View.VISIBLE
                binding.tvActionNote.text =
                    "✅ You claimed this favor. Complete the task and wait for confirmation."
            }
            status == "CLAIMED" && !isOwner && !isClaimer -> {
                binding.tvActionNote.visibility = View.VISIBLE
                binding.tvActionNote.text = "This favor has already been claimed."
            }
            status == "COMPLETED" -> {
                binding.tvActionNote.visibility = View.VISIBLE
                binding.tvActionNote.text = "✅ This favor has been completed!"
            }
            else -> {
                binding.tvActionNote.visibility = View.VISIBLE
                binding.tvActionNote.text = "This favor has expired."
            }
        }
    }

    private fun showError(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.tvErrorMessage.text    = message
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
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

    private fun categoryIcon(category: String): Int = when (category) {
        "Errand"         -> R.drawable.ic_category_errand
        "Pet Care"       -> R.drawable.ic_category_pet
        "Tool Borrowing" -> R.drawable.ic_category_tool
        "Plant Watering" -> R.drawable.ic_category_plant
        else             -> R.drawable.ic_category_other
    }

    private fun categoryTagColors(category: String): Pair<Int, Int> = when (category) {
        "Errand"         -> Pair(0xFFFFF3E0.toInt(), 0xFFE65100.toInt())
        "Pet Care"       -> Pair(0xFFF3E5F5.toInt(), 0xFF7B1FA2.toInt())
        "Tool Borrowing" -> Pair(0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt())
        "Plant Watering" -> Pair(0xFFE0F7FA.toInt(), 0xFF00695C.toInt())
        else             -> Pair(0xFFF5F5F5.toInt(), 0xFF424242.toInt())
    }

    private fun statusColors(status: String): Pair<String, String> = when (status) {
        "CLAIMED"   -> Pair("#FFF8E1", "#F57F17")
        "COMPLETED" -> Pair("#E8F5E9", "#2E7D32")
        else        -> Pair("#FFF3E0", "#E65100")
    }

    private fun formatDateTime(dateStr: String?): String? {
        if (dateStr.isNullOrEmpty()) return null
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date   = parser.parse(dateStr) ?: return null
            SimpleDateFormat("MMMM d, yyyy · h:mm a", Locale.US).format(date)
        } catch (e: Exception) { null }
    }

    private fun formatDateLong(dateStr: String?): String? {
        if (dateStr.isNullOrEmpty()) return null
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date   = parser.parse(dateStr) ?: return null
            SimpleDateFormat("MMMM d, yyyy", Locale.US).format(date)
        } catch (e: Exception) { null }
    }

    private fun formatDateShort(dateStr: String?): String? {
        if (dateStr.isNullOrEmpty()) return null
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date   = parser.parse(dateStr) ?: return null
            SimpleDateFormat("MMM d, h:mm a", Locale.US).format(date)
        } catch (e: Exception) { null }
    }

    private fun formatMonthYear(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "—"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date   = parser.parse(dateStr) ?: return "—"
            SimpleDateFormat("MMM yyyy", Locale.US).format(date)
        } catch (e: Exception) { "—" }
    }
}