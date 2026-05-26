package edu.cit.abelgas.localloop.features.myactivity

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.databinding.ActivityMyActivityBinding
import edu.cit.abelgas.localloop.features.announcements.AnnouncementsActivity
import edu.cit.abelgas.localloop.features.dashboard.DashboardActivity
import edu.cit.abelgas.localloop.features.favordetail.FavorDetailActivity
import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedActivity
import edu.cit.abelgas.localloop.features.myactivity.adapter.MyActivityFavorAdapter
import edu.cit.abelgas.localloop.features.myactivity.model.ActivityListState
import edu.cit.abelgas.localloop.features.myactivity.model.ActivityTab
import edu.cit.abelgas.localloop.features.profile.ProfileActivity
import edu.cit.abelgas.localloop.shared.util.BadgeManager
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import edu.cit.abelgas.localloop.shared.util.applyActivityBadge

class MyActivityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyActivityBinding
    private lateinit var prefs: SharedPreferencesHelper
    private val viewModel: MyActivityViewModel by viewModels()
    private var currentUserId: Long? = null

    private lateinit var postedAdapter: MyActivityFavorAdapter
    private lateinit var claimedAdapter: MyActivityFavorAdapter
    private lateinit var completedAdapter: MyActivityFavorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPreferencesHelper(this)
        val user      = prefs.getUser()
        currentUserId = user?.id

        setupAvatar(user?.name ?: "")
        setupAdapters()
        setupTabs()
        setupSwipeRefresh()
        setupBottomNav()
        observeViewModel()

        onBackPressedDispatcher.addCallback(this) {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        BadgeManager.refresh()
        binding.bottomNav.selectedItemId = R.id.nav_activity
    }

    private fun setupAvatar(name: String) {
        binding.tvAvatar.text = initials(name)
        binding.tvAvatar.backgroundTintList = ColorStateList.valueOf(avatarColor(name))
    }

    private fun setupAdapters() {
        postedAdapter = MyActivityFavorAdapter(
            tab           = ActivityTab.POSTED,
            currentUserId = currentUserId,
            onCardClick   = { favor ->
                startActivity(
                    Intent(this, FavorDetailActivity::class.java).apply {
                        putExtra(FavorDetailActivity.EXTRA_FAVOR_ID, favor.id)
                    }
                )
            },
            onDelete          = { favor -> viewModel.deleteFavor(favor.id) },
            onConfirmComplete = { favor -> viewModel.confirmComplete(favor.id) },
            onReopen          = { favor -> viewModel.reopenFavor(favor.id) },
            onCancelClaim     = { }
        )

        claimedAdapter = MyActivityFavorAdapter(
            tab           = ActivityTab.CLAIMED,
            currentUserId = currentUserId,
            onCardClick   = { favor ->
                startActivity(
                    Intent(this, FavorDetailActivity::class.java).apply {
                        putExtra(FavorDetailActivity.EXTRA_FAVOR_ID, favor.id)
                    }
                )
            },
            onDelete          = { },
            onConfirmComplete = { },
            onReopen          = { },
            onCancelClaim     = { favor -> viewModel.cancelClaim(favor.id) }
        )

        completedAdapter = MyActivityFavorAdapter(
            tab           = ActivityTab.COMPLETED,
            currentUserId = currentUserId,
            onCardClick   = { favor ->
                startActivity(
                    Intent(this, FavorDetailActivity::class.java).apply {
                        putExtra(FavorDetailActivity.EXTRA_FAVOR_ID, favor.id)
                    }
                )
            },
            onDelete          = { },
            onConfirmComplete = { },
            onReopen          = { },
            onCancelClaim     = { }
        )

        binding.rvFavors.apply {
            layoutManager = LinearLayoutManager(this@MyActivityActivity)
            adapter = postedAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupTabs() {
        binding.tabPosted.setOnClickListener    { viewModel.setTab(ActivityTab.POSTED) }
        binding.tabClaimed.setOnClickListener   { viewModel.setTab(ActivityTab.CLAIMED) }
        binding.tabCompleted.setOnClickListener { viewModel.setTab(ActivityTab.COMPLETED) }
    }

    private fun applyTabStyle(activeTab: ActivityTab) {
        val activeTextColor   = getColor(R.color.primary)
        val inactiveTextColor = getColor(R.color.text_secondary)
        val activeBadgeBg     = 0xFFFFF3E0.toInt()
        val activeBadgeText   = getColor(R.color.primary)
        val inactiveBadgeBg   = 0xFFF0ECE6.toInt()
        val inactiveBadgeText = getColor(R.color.text_hint)

        listOf(
            Triple(binding.tvTabPosted,    binding.tvTabPostedBadge,    ActivityTab.POSTED),
            Triple(binding.tvTabClaimed,   binding.tvTabClaimedBadge,   ActivityTab.CLAIMED),
            Triple(binding.tvTabCompleted, binding.tvTabCompletedBadge, ActivityTab.COMPLETED)
        ).forEach { (label, badge, tab) ->
            val isActive = tab == activeTab
            label.setTextColor(if (isActive) activeTextColor else inactiveTextColor)
            badge.backgroundTintList = ColorStateList.valueOf(
                if (isActive) activeBadgeBg else inactiveBadgeBg)
            badge.setTextColor(if (isActive) activeBadgeText else inactiveBadgeText)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(Color.parseColor("#C8601A"))
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadAll()
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(
                        Intent(this, DashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    overridePendingTransition(0, 0)
                    false
                }
                R.id.nav_feed -> {
                    startActivity(Intent(this, FavorFeedActivity::class.java))
                    overridePendingTransition(0, 0)
                    false
                }
                R.id.nav_announce -> {
                    startActivity(Intent(this, AnnouncementsActivity::class.java))
                    overridePendingTransition(0, 0)
                    false
                }
                R.id.nav_activity -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    false
                }
                else -> false
            }
        }
        BadgeManager.badgeCount.observe(this) { count ->
            binding.bottomNav.applyActivityBadge(count)
        }
    }

    private fun observeViewModel() {

        viewModel.activeTab.observe(this) { tab ->
            applyTabStyle(tab)
            binding.rvFavors.adapter = when (tab) {
                ActivityTab.POSTED    -> postedAdapter
                ActivityTab.CLAIMED   -> claimedAdapter
                ActivityTab.COMPLETED -> completedAdapter
            }
            val state = when (tab) {
                ActivityTab.POSTED    -> viewModel.postedState.value
                ActivityTab.CLAIMED   -> viewModel.claimedState.value
                ActivityTab.COMPLETED -> viewModel.completedState.value
            }
            state?.let { renderListState(it, tab) }
        }

        viewModel.postedState.observe(this) { state ->
            if (viewModel.activeTab.value == ActivityTab.POSTED) {
                renderListState(state, ActivityTab.POSTED)
            }
        }

        viewModel.claimedState.observe(this) { state ->
            if (viewModel.activeTab.value == ActivityTab.CLAIMED) {
                renderListState(state, ActivityTab.CLAIMED)
            }
        }

        viewModel.completedState.observe(this) { state ->
            if (viewModel.activeTab.value == ActivityTab.COMPLETED) {
                renderListState(state, ActivityTab.COMPLETED)
            }
        }

        viewModel.summary.observe(this) { s ->
            binding.tvStatReputation.text        = s.reputationScore.toString()
            binding.tvStatPosted.text            = s.postedCount.toString()
            binding.tvStatClaimed.text           = s.claimedCount.toString()
            binding.tvStatCompleted.text         = s.completedCount.toString()
            binding.tvTabPostedBadge.text        = s.postedCount.toString()
            binding.tvTabClaimedBadge.text       = s.claimedCount.toString()
            binding.tvTabCompletedBadge.text     = s.completedCount.toString()
            binding.tvSummaryOpen.text           = s.openCount.toString()
            binding.tvSummaryPostedClaimed.text  = s.postedClaimedCount.toString()
            binding.tvSummaryPostedCompleted.text = s.postedCompletedCount.toString()
            binding.tvSummaryTotalPosted.text    = s.postedCount.toString()
            binding.tvSummaryReputation.text     = "⭐ ${s.reputationScore} pts"
            binding.progressCompletion.progress  = s.completionRate
            binding.tvCompletionText.text = when {
                s.postedCount == 0 -> "No favors posted yet"
                else -> "${s.completionRate}% of your posted favors were completed"
            }
            // Refresh badge after data loads — counts are now accurate
            BadgeManager.refresh()
        }

        viewModel.actionLoading.observe(this) { loadingId ->
            postedAdapter.actionLoadingId    = loadingId
            claimedAdapter.actionLoadingId   = loadingId
            completedAdapter.actionLoadingId = loadingId
            postedAdapter.notifyDataSetChanged()
            claimedAdapter.notifyDataSetChanged()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.actionError.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.clearActionError()
            }
        }

        viewModel.actionSuccess.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                viewModel.clearActionSuccess()
                // Refresh badge after any action (delete, complete, reopen, cancel)
                BadgeManager.refresh()
            }
        }
    }

    private fun renderListState(state: ActivityListState, tab: ActivityTab) {
        binding.progressBar.visibility    = View.GONE
        binding.layoutEmpty.visibility    = View.GONE
        binding.tvError.visibility        = View.GONE
        binding.rvFavors.visibility       = View.GONE
        binding.swipeRefresh.isRefreshing = false

        when (state) {
            is ActivityListState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }
            is ActivityListState.Success -> {
                binding.rvFavors.visibility = View.VISIBLE
                when (tab) {
                    ActivityTab.POSTED    -> postedAdapter.submitList(state.items)
                    ActivityTab.CLAIMED   -> claimedAdapter.submitList(state.items)
                    ActivityTab.COMPLETED -> completedAdapter.submitList(state.items)
                }
            }
            is ActivityListState.Empty -> {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.tvEmptyMessage.text = when (tab) {
                    ActivityTab.POSTED    -> "You haven't posted any favors yet."
                    ActivityTab.CLAIMED   -> "You haven't claimed any favors yet."
                    ActivityTab.COMPLETED -> "You haven't completed any favors yet."
                }
            }
            is ActivityListState.Error -> {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = state.message
            }
        }
    }

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
}