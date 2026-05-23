    package edu.cit.abelgas.localloop.features.dashboard
    
    import android.content.Intent
    import android.os.Bundle
    import android.widget.PopupMenu
    import android.view.View
    import androidx.activity.viewModels
    import androidx.appcompat.app.AppCompatActivity
    import androidx.recyclerview.widget.LinearLayoutManager
    import com.google.android.material.snackbar.Snackbar
    import edu.cit.abelgas.localloop.R
    import edu.cit.abelgas.localloop.databinding.ActivityDashboardBinding
    import edu.cit.abelgas.localloop.features.auth.LoginActivity
    import edu.cit.abelgas.localloop.features.dashboard.adapter.AnnouncementAdapter
    import edu.cit.abelgas.localloop.features.dashboard.adapter.CategoryChipAdapter
    import edu.cit.abelgas.localloop.features.dashboard.adapter.FavorAdapter
    import edu.cit.abelgas.localloop.features.favordetail.FavorDetailActivity
    import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedActivity
    import edu.cit.abelgas.localloop.features.postfavor.PostFavorActivity
    import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
    import edu.cit.abelgas.localloop.shared.util.WeatherUtils
    import java.util.Calendar
    
    class DashboardActivity : AppCompatActivity() {
    
        private lateinit var binding: ActivityDashboardBinding
        private lateinit var prefs: SharedPreferencesHelper
        private val viewModel: DashboardViewModel by viewModels()
    
        private lateinit var favorAdapter: FavorAdapter
        private lateinit var categoryAdapter: CategoryChipAdapter
        private lateinit var announcementAdapter: AnnouncementAdapter
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)
    
            prefs = SharedPreferencesHelper(this)
            val user = prefs.getUser()
    
            // ── Trigger reputation load with user fallback score ──────────────────
            viewModel.loadReputation(fallbackScore = user?.reputationScore ?: 0)
    
            setupHeader(user)
            setupAvatarMenu()
            setupFab()
            setupBottomNav()
            setupCategoryChips()
            setupFavorsRecycler(user?.id)
            setupAnnouncementsRecycler()
            setupObservers(user?.id)
        }
    
        override fun onResume() {
            super.onResume()
            // Re-assert Home tab every time Dashboard comes to front
            // This fires when user presses back from FavorFeedActivity
            // fixing the "Feed stays highlighted" bug
            binding.bottomNav.selectedItemId = R.id.nav_home
        }
    
        // ─────────────────────────────────────────────────────────────────────────
        // Header: greeting + barangay indicator
        // Matches web: getGreeting() + user.name.split(' ')[0] + user.barangay
        // ─────────────────────────────────────────────────────────────────────────
        private fun setupHeader(user: edu.cit.abelgas.localloop.features.profile.model.UserDto?) {
            val firstName = user?.name?.split(" ")?.firstOrNull() ?: "there"
            val greeting = getGreeting()
            binding.tvGreeting.text = "$greeting, $firstName! 👋"
            binding.tvBarangay.text = user?.barangay ?: "Your Barangay"
    
            // Avatar initials + color (matches web avatarColor / initials logic)
            val name = user?.name ?: ""
            binding.tvAvatar.text = initials(name)
            binding.tvAvatar.backgroundTintList =
                android.content.res.ColorStateList.valueOf(avatarColor(name))
        }
    
        // ─────────────────────────────────────────────────────────────────────────
        // FAB — navigate to post new favor
        // ─────────────────────────────────────────────────────────────────────────
        private fun setupFab() {
            binding.fabPost.setOnClickListener {
                startActivity(
                    Intent(this, PostFavorActivity::class.java)
                )
                // Matches project-wide pattern: no slide animation
                overridePendingTransition(0, 0)
            }
        }
    
        // ─────────────────────────────────────────────────────────────────────────
        // Bottom navigation
        // ─────────────────────────────────────────────────────────────────────────
        private fun setupBottomNav() {
            binding.bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> true
                    R.id.nav_feed -> {
                        startActivity(Intent(this, FavorFeedActivity::class.java))
                        // ✅ Instantly kill the slide animation
                        overridePendingTransition(0, 0)
                        false
                    }
                    R.id.nav_announce -> { /* TODO */ true }
                    R.id.nav_activity -> { /* TODO */ true }
                    R.id.nav_profile  -> { /* TODO */ true }
                    else -> false
                }
            }
        }
    
        // ─────────────────────────────────────────────────────────────────────────
        // Category chips — horizontal scrollable RecyclerView
        // ─────────────────────────────────────────────────────────────────────────
        private fun setupCategoryChips() {
            categoryAdapter = CategoryChipAdapter(viewModel.categories) { selected ->
                viewModel.setCategory(selected)
            }
            binding.rvCategories.apply {
                layoutManager = LinearLayoutManager(
                    this@DashboardActivity, LinearLayoutManager.HORIZONTAL, false
                )
                adapter = categoryAdapter
            }
        }
    
        // ─────────────────────────────────────────────────────────────────────────
        // Favors RecyclerView
        // ─────────────────────────────────────────────────────────────────────────
        private fun setupFavorsRecycler(currentUserId: Long?) {
            favorAdapter = FavorAdapter(
                currentUserId = currentUserId,
                onClaim = { favorId -> viewModel.claimFavor(favorId, currentUserId) },
                onCardClick = { favor ->
                    // Pass favorId via Intent extra — mirrors web navigate(`/favors/${favor.id}`)
                    startActivity(
                        Intent(this, FavorDetailActivity::class.java).apply {
                            putExtra(FavorDetailActivity.EXTRA_FAVOR_ID, favor.id)
                        }
                    )
                }
            )
            binding.rvFavors.apply {
                layoutManager = LinearLayoutManager(this@DashboardActivity)
                adapter = favorAdapter
                isNestedScrollingEnabled = false
            }
            binding.btnLoadMore.setOnClickListener { viewModel.loadMoreFavors() }
        }
    
        // ─────────────────────────────────────────────────────────────────────────
        // Announcements RecyclerView
        // ─────────────────────────────────────────────────────────────────────────
        private fun setupAnnouncementsRecycler() {
            announcementAdapter = AnnouncementAdapter { ann ->
                // TODO: navigate to AnnouncementDetailActivity
                Snackbar.make(binding.root, ann.title, Snackbar.LENGTH_SHORT).show()
            }
            binding.rvAnnouncements.apply {
                layoutManager = LinearLayoutManager(this@DashboardActivity)
                adapter = announcementAdapter
                isNestedScrollingEnabled = false
            }
            binding.tvViewAllAnnouncements.setOnClickListener {
                // TODO: navigate to AnnouncementsActivity
            }
            binding.tvViewAllFavors.setOnClickListener {
                startActivity(Intent(this, FavorFeedActivity::class.java))
            }
        }
    
        // ─────────────────────────────────────────────────────────────────────────
        // Observe ViewModel LiveData
        // ─────────────────────────────────────────────────────────────────────────
        private fun setupObservers(currentUserId: Long?) {
    
            // ── Active category → update chip selection ───────────────────────────
            viewModel.activeCategory.observe(this) { cat ->
                categoryAdapter.setActive(cat)
            }
    
            // ── Weather ───────────────────────────────────────────────────────────
            viewModel.weatherLoading.observe(this) { loading ->
                binding.weatherLoading.visibility = if (loading) View.VISIBLE else View.GONE
                binding.weatherContent.visibility = if (loading) View.GONE else View.VISIBLE
            }
            viewModel.weather.observe(this) { weather ->
                if (weather == null) {
                    binding.tvWeatherUnavailable.visibility = View.VISIBLE
                    binding.weatherContent.visibility = View.GONE
                    return@observe
                }
                binding.tvWeatherUnavailable.visibility = View.GONE
                binding.tvTemperature.text = "${Math.round(weather.resolvedTemp)}°C"
                binding.tvCondition.text = weather.resolvedCondition
                binding.tvHumidity.text = "${weather.humidity ?: "--"}%"
                binding.tvWindSpeed.text = "${weather.resolvedWindSpeed} km/h"
                binding.tvFeelsLike.text = "${Math.round(weather.resolvedFeelsLike)}°C"
    
                // Weather icon — set based on condition string (matches web WeatherIcon logic)
                val iconRes = WeatherUtils.getWeatherIcon(weather.resolvedCondition)
                binding.ivWeatherIcon.setImageResource(iconRes)
            }
    
            // ── Favors ────────────────────────────────────────────────────────────
            viewModel.favorsLoading.observe(this) { loading ->
                binding.favorsLoading.visibility = if (loading) View.VISIBLE else View.GONE
                if (loading) {
                    binding.rvFavors.visibility = View.GONE
                    binding.tvFavorsError.visibility = View.GONE
                    binding.tvFavorsEmpty.visibility = View.GONE
                }
            }
            viewModel.favors.observe(this) { list ->
                favorAdapter.submitList(list)
                binding.rvFavors.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                binding.tvFavorsEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
            viewModel.favorsError.observe(this) { error ->
                if (error.isNotEmpty()) {
                    binding.tvFavorsError.text = error
                    binding.tvFavorsError.visibility = View.VISIBLE
                } else {
                    binding.tvFavorsError.visibility = View.GONE
                }
            }
            viewModel.hasMore.observe(this) { hasMore ->
                binding.btnLoadMore.visibility = if (hasMore) View.VISIBLE else View.GONE
            }
            viewModel.loadingMore.observe(this) { loading ->
                binding.btnLoadMore.text = if (loading) "Loading…" else "Load more"
                binding.btnLoadMore.isEnabled = !loading
            }
            viewModel.claimError.observe(this) { error ->
                if (!error.isNullOrEmpty()) {
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    viewModel.clearClaimError()
                }
            }
    
            // ── Announcements ─────────────────────────────────────────────────────
            viewModel.announcementsLoading.observe(this) { loading ->
                binding.announcementsLoading.visibility = if (loading) View.VISIBLE else View.GONE
                binding.rvAnnouncements.visibility = if (loading) View.GONE else View.VISIBLE
            }
            viewModel.announcements.observe(this) { list ->
                announcementAdapter.submitList(list)
            }
    
            // ── Reputation ────────────────────────────────────────────────────────
            viewModel.reputationLoading.observe(this) { loading ->
                binding.reputationLoading.visibility = if (loading) View.VISIBLE else View.GONE
                binding.reputationContent.visibility = if (loading) View.GONE else View.VISIBLE
            }
            viewModel.reputation.observe(this) { rep ->
                binding.tvReputationScore.text = (rep?.reputationScore ?: 0).toString()
                binding.tvPostedCount.text = (rep?.favorsPosted ?: 0).toString()
                binding.tvCompletedCount.text = (rep?.favorsCompleted ?: 0).toString()
            }
        }
    
        // ─────────────────────────────────────────────────────────────────────────
        // Helpers — match web utility functions exactly
        // ─────────────────────────────────────────────────────────────────────────
    
        /** Mirrors web getGreeting() */
        private fun getGreeting(): String {
            return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 0..11  -> "Good morning"
                in 12..17 -> "Good afternoon"
                else      -> "Good evening"
            }
        }
    
        /** Mirrors web initials() — first letter of each word, max 2 chars */
        private fun initials(name: String): String =
            name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("").take(2).uppercase()
    
        /** Mirrors web avatarColor() — deterministic color from name hash */
        private fun avatarColor(name: String): Int {
            val colors = listOf(
                0xFFC8601A.toInt(), 0xFF2E86AB.toInt(), 0xFFA23B72.toInt(), 0xFFF18F01.toInt(),
                0xFF44BBA4.toInt(), 0xFFE94F37.toInt(), 0xFF6B4226.toInt(), 0xFF3A86FF.toInt()
            )
            var hash = 0
            for (c in name) hash = c.code + ((hash shl 5) - hash)
            return colors[Math.abs(hash) % colors.size]
        }
    
        private fun setupAvatarMenu() {
            binding.tvAvatar.setOnClickListener { anchor ->
                val popup = PopupMenu(this@DashboardActivity, anchor)
                popup.menuInflater.inflate(R.menu.menu_avatar, popup.menu)
    
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_logout -> {
                            prefs.clearAll()
                            val intent = Intent(this@DashboardActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            true
                        }
                        else -> false
                    }
                }
                try {
                    val field = popup.javaClass.getDeclaredField("mPopup")
                    field.isAccessible = true
                    val helper = field.get(popup)
                    helper?.javaClass?.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                        ?.apply { isAccessible = true }
                        ?.invoke(helper, true)
                } catch (_: Exception) { }
                popup.show()
            }
        }
    }