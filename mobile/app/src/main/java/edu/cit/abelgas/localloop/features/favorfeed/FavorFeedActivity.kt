package edu.cit.abelgas.localloop.features.favorfeed

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.databinding.ActivityFavorFeedBinding
import edu.cit.abelgas.localloop.features.announcements.AnnouncementsActivity
import edu.cit.abelgas.localloop.features.auth.LoginActivity
import edu.cit.abelgas.localloop.features.dashboard.DashboardActivity
import edu.cit.abelgas.localloop.features.favordetail.FavorDetailActivity
import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedConstants.AVATAR_COLORS
import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedConstants.CATEGORIES
import edu.cit.abelgas.localloop.features.favorfeed.adapter.CategoryChipAdapter
import edu.cit.abelgas.localloop.features.favorfeed.adapter.FavorCardAdapter
import edu.cit.abelgas.localloop.features.favorfeed.model.FeedUiState
import edu.cit.abelgas.localloop.features.myactivity.MyActivityActivity
import edu.cit.abelgas.localloop.features.postfavor.PostFavorActivity
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper

// ✅ Removed: OkHttpClient, Retrofit, GsonConverterFactory, TimeUnit imports
//    — no longer needed since we use ApiClient.service via the ViewModel directly

class FavorFeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavorFeedBinding
    private lateinit var prefs: SharedPreferencesHelper
    private val viewModel: FavorFeedViewModel by viewModels()

    private lateinit var favorAdapter: FavorCardAdapter
    private lateinit var chipAdapter: CategoryChipAdapter
    private var currentUserId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavorFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPreferencesHelper(this)
        val user      = prefs.getUser()
        currentUserId = user?.id

        // ✅ Removed: entire OkHttpClient + Retrofit builder block (was ~20 lines)
        // ✅ Removed: viewModel.apiService = retrofit.create(FavorApiService::class.java)
        // ViewModel now uses ApiClient.service internally — token always attached.

        setupAvatar(user?.name ?: "")
        setupSearch()
        setupCategoryChips()
        setupFavorsList()
        setupPostBanner()
        setupBottomNav()
        observeViewModel()

        viewModel.fetchFavors(0)

        onBackPressedDispatcher.addCallback(this) {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_feed
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Avatar
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupAvatar(name: String) {
        binding.tvAvatar.text = initials(name)
        binding.tvAvatar.backgroundTintList = ColorStateList.valueOf(avatarColor(name))

        binding.tvAvatar.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menuInflater.inflate(R.menu.menu_avatar, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_logout -> {
                        prefs.clearAll()
                        startActivity(
                            Intent(this, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                        true
                    }
                    else -> false
                }
            }
            try {
                val field = popup.javaClass.getDeclaredField("mPopup")
                field.isAccessible = true
                val helper = field.get(popup)
                helper?.javaClass
                    ?.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    ?.apply { isAccessible = true }
                    ?.invoke(helper, true)
            } catch (_: Exception) { }
            popup.show()
        }
    }

    private fun initials(name: String): String =
        name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("").take(2).uppercase()

    private fun avatarColor(name: String): Int {
        var hash = 0
        for (c in name) hash = c.code + ((hash shl 5) - hash)
        return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.size]
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            viewModel.searchQuery = binding.etSearch.text.toString().trim()
            viewModel.applyFilters()
            true
        }
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty() && viewModel.searchQuery.isNotEmpty()) {
                    viewModel.searchQuery = ""
                    viewModel.applyFilters()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Category chips
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupCategoryChips() {
        chipAdapter = CategoryChipAdapter(CATEGORIES) { selected ->
            viewModel.activeCategory = selected
            viewModel.applyFilters()
        }
        binding.rvCategoryChips.apply {
            layoutManager = LinearLayoutManager(
                this@FavorFeedActivity, LinearLayoutManager.HORIZONTAL, false
            )
            adapter = chipAdapter
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Favors RecyclerView
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupFavorsList() {
        favorAdapter = FavorCardAdapter(
            currentUserId = currentUserId,
            onCardClick = { favor ->
                startActivity(
                    Intent(this, FavorDetailActivity::class.java).apply {
                        putExtra(FavorDetailActivity.EXTRA_FAVOR_ID, favor.id)
                    }
                )
            },
            onClaimClick = { favor ->
                viewModel.claimFavor(favor.id, currentUserId)
            }
        )
        binding.rvFavors.apply {
            layoutManager = LinearLayoutManager(this@FavorFeedActivity)
            adapter = favorAdapter
            isNestedScrollingEnabled = false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Post banner
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupPostBanner() {
        binding.btnPostFavor.setOnClickListener {
            startActivity(Intent(this, PostFavorActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bottom nav
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(
                        Intent(this, DashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    overridePendingTransition(0, 0)
                    false
                }
                R.id.nav_feed     -> true
                R.id.nav_announce -> {
                    startActivity(Intent(this, AnnouncementsActivity::class.java))
                    overridePendingTransition(0, 0)
                    false
                }
                R.id.nav_activity -> {
                    startActivity(Intent(this, MyActivityActivity::class.java))
                    overridePendingTransition(0, 0)
                    false
                }
                R.id.nav_profile  -> { true }
                else -> false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Observe ViewModel
    // ─────────────────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.favorsLoading.visibility = View.GONE
            binding.tvFavorsError.visibility = View.GONE
            binding.layoutEmpty.visibility   = View.GONE
            binding.rvFavors.visibility      = View.GONE

            when (state) {
                is FeedUiState.Loading -> binding.favorsLoading.visibility = View.VISIBLE
                is FeedUiState.Success -> binding.rvFavors.visibility      = View.VISIBLE
                is FeedUiState.Empty   -> binding.layoutEmpty.visibility   = View.VISIBLE
                is FeedUiState.Error   -> {
                    binding.tvFavorsError.visibility = View.VISIBLE
                    binding.tvFavorsError.text       = state.message
                }
            }
        }

        viewModel.favors.observe(this) { list ->
            favorAdapter.submitList(list)
        }

        viewModel.claimError.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.clearClaimError()
            }
        }

        viewModel.totalPages.observe(this) { total ->
            val current = viewModel.currentPage.value ?: 0
            rebuildPaginationRow(current, total)
        }
        viewModel.currentPage.observe(this) { current ->
            val total = viewModel.totalPages.value ?: 1
            rebuildPaginationRow(current, total)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pagination row
    // ─────────────────────────────────────────────────────────────────────────
    private fun rebuildPaginationRow(current: Int, total: Int) {
        binding.paginationRow.removeAllViews()
        if (total <= 1) { binding.paginationRow.visibility = View.GONE; return }
        binding.paginationRow.visibility = View.VISIBLE

        fun makeBtn(label: String, targetPage: Int, isActive: Boolean, disabled: Boolean) =
            Button(this).apply {
                text      = label
                isEnabled = !disabled
                textSize  = 13f
                stateListAnimator = null
                setTextColor(when {
                    disabled -> Color.parseColor("#CCCCCC")
                    isActive -> Color.WHITE
                    else     -> Color.parseColor("#555555")
                })
                backgroundTintList = ColorStateList.valueOf(when {
                    disabled -> Color.parseColor("#F0F0F0")
                    isActive -> Color.parseColor("#C8601A")
                    else     -> Color.WHITE
                })
                setPadding(28, 8, 28, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = 6 }
                setOnClickListener {
                    if (!disabled && targetPage != current) {
                        viewModel.fetchFavors(targetPage)
                        binding.nestedScrollView.smoothScrollTo(0, 0)
                    }
                }
            }

        binding.paginationRow.addView(
            makeBtn("←", current - 1, isActive = false, disabled = current == 0))
        for (i in 0 until total) {
            binding.paginationRow.addView(
                makeBtn("${i + 1}", i, isActive = i == current, disabled = false))
        }
        binding.paginationRow.addView(
            makeBtn("→", current + 1, isActive = false, disabled = current >= total - 1))
    }
}