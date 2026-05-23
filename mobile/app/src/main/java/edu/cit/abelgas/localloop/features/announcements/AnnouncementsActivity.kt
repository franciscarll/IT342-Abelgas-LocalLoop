package edu.cit.abelgas.localloop.features.announcements

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.databinding.ActivityAnnouncementsBinding
import edu.cit.abelgas.localloop.features.announcements.adapter.AnnouncementListAdapter
import edu.cit.abelgas.localloop.features.announcements.model.AnnouncementUiState
import edu.cit.abelgas.localloop.features.dashboard.DashboardActivity
import edu.cit.abelgas.localloop.features.dashboard.model.AnnouncementDto
import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedActivity
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import java.text.SimpleDateFormat
import java.util.Locale

class AnnouncementsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnnouncementsBinding
    private lateinit var prefs: SharedPreferencesHelper
    private val viewModel: AnnouncementsViewModel by viewModels()
    private lateinit var listAdapter: AnnouncementListAdapter

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnnouncementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPreferencesHelper(this)

        setupAvatar()
        setupSearch()
        setupRecyclerView()
        setupSwipeRefresh()
        setupBottomNav()
        observeViewModel()

        viewModel.load()

        onBackPressedDispatcher.addCallback(this) {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_announce
    }

    private fun setupAvatar() {
        val name = prefs.getUser()?.name ?: ""
        binding.tvAvatar.text = initials(name)
        binding.tvAvatar.backgroundTintList = ColorStateList.valueOf(avatarColor(name))
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    viewModel.onSearch(s?.toString()?.trim() ?: "")
                }
                searchHandler.postDelayed(searchRunnable!!, 400)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupRecyclerView() {
        listAdapter = AnnouncementListAdapter { announcement ->
            AnnouncementDetailBottomSheet.show(supportFragmentManager, announcement)
        }
        binding.rvAnnouncements.apply {
            layoutManager = LinearLayoutManager(this@AnnouncementsActivity)
            adapter = listAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(Color.parseColor("#C8601A"))
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.load(forceRefresh = true)
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
                R.id.nav_announce -> true
                R.id.nav_activity -> true
                R.id.nav_profile  -> true
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.visibility     = View.GONE
            binding.layoutError.visibility     = View.GONE
            binding.layoutEmpty.visibility     = View.GONE
            binding.pinnedCard.visibility      = View.GONE
            binding.rvAnnouncements.visibility = View.GONE
            binding.paginationRow.visibility   = View.GONE

            when (state) {
                is AnnouncementUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AnnouncementUiState.Success -> {
                    if (state.pinned != null) {
                        binding.pinnedCard.visibility = View.VISIBLE
                        bindPinnedCard(state.pinned)
                    }
                    binding.rvAnnouncements.visibility = View.VISIBLE
                    listAdapter.submitList(state.items)
                    buildPaginationRow(state.currentPage, state.totalPages)
                }
                is AnnouncementUiState.Empty -> {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text =
                        if (state.query.isNotBlank())
                            "No announcements found for \"${state.query}\""
                        else
                            "No announcements yet."
                }
                is AnnouncementUiState.Error -> {
                    binding.layoutError.visibility = View.VISIBLE
                    binding.tvErrorMessage.text = state.message
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        binding.btnRetry.setOnClickListener {
            viewModel.load(forceRefresh = true)
        }
    }

    private fun bindPinnedCard(ann: AnnouncementDto) {
        binding.tvPinnedTitle.text   = ann.title
        binding.tvPinnedDate.text    = formatDate(ann.resolvedDate)
        // FIX 3 & 4: content is nullable — use ?: "" before calling .take() and .length
        val body = ann.content ?: ""
        binding.tvPinnedSnippet.text = if (body.length > 80) body.take(80) + "…" else body
        binding.btnPinnedReadMore.setOnClickListener {
            AnnouncementDetailBottomSheet.show(supportFragmentManager, ann)
        }
        // Also make the whole pinned card tappable (optional but recommended):
        binding.pinnedCard.setOnClickListener {
            AnnouncementDetailBottomSheet.show(supportFragmentManager, ann)
        }
    }

    private fun buildPaginationRow(current: Int, total: Int) {
        binding.paginationRow.removeAllViews()
        if (total <= 1) {
            binding.paginationRow.visibility = View.GONE
            return
        }
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
                        viewModel.onPageSelected(targetPage)
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

    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "—"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date   = parser.parse(dateStr) ?: return "—"
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        } catch (e: Exception) { "—" }
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