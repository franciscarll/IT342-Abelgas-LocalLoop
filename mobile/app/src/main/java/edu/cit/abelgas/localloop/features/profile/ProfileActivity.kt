package edu.cit.abelgas.localloop.features.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.databinding.ActivityProfileBinding
import edu.cit.abelgas.localloop.features.announcements.AnnouncementsActivity
import edu.cit.abelgas.localloop.features.dashboard.DashboardActivity
import edu.cit.abelgas.localloop.features.favorfeed.FavorFeedActivity
import edu.cit.abelgas.localloop.features.myactivity.MyActivityActivity
import edu.cit.abelgas.localloop.shared.util.BadgeManager
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import edu.cit.abelgas.localloop.shared.util.applyActivityBadge
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var prefs: SharedPreferencesHelper
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var recentActivityAdapter: RecentActivityAdapter

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let { handleImageSelected(it) }
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openImagePicker()
            else Snackbar.make(
                binding.root,
                "Storage permission is needed to upload a photo.",
                Snackbar.LENGTH_LONG
            ).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPreferencesHelper(this)

        // FIX: inject prefs into ViewModel so it can persist user changes
        viewModel.prefs = prefs

        setupAvatar(prefs.getUser()?.name ?: "")
        setupBottomNav()
        setupRecentActivityRecycler()
        setupPasswordToggleIcons()
        setupPasswordValidationWatchers()
        setupClickListeners()
        setupObservers()

        viewModel.loadProfile()
    }

    override fun onResume() {
        super.onResume()
        BadgeManager.refresh()
        binding.bottomNav.selectedItemId = R.id.nav_profile
    }

    private fun setupAvatar(name: String) {
        binding.tvAvatar.text = initials(name)
        binding.tvAvatar.backgroundTintList = ColorStateList.valueOf(avatarColor(name))
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
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
                R.id.nav_activity -> {
                    startActivity(Intent(this, MyActivityActivity::class.java))
                    overridePendingTransition(0, 0)
                    false
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
        BadgeManager.badgeCount.observe(this) { count ->
            binding.bottomNav.applyActivityBadge(count)
        }
    }

    private fun setupRecentActivityRecycler() {
        recentActivityAdapter = RecentActivityAdapter()
        binding.rvRecentActivity.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = recentActivityAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupPasswordToggleIcons() {
        // TextInputLayout handles eye-toggle automatically via XML
    }

    private fun setupPasswordValidationWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validatePasswordFields() }
        }
        binding.etNewPassword.addTextChangedListener(watcher)
        binding.etConfirmPassword.addTextChangedListener(watcher)
        binding.etCurrentPassword.addTextChangedListener(watcher)
    }

    private fun validatePasswordFields(): Boolean {
        val current = binding.etCurrentPassword.text.toString()
        val newPass  = binding.etNewPassword.text.toString()
        val confirm  = binding.etConfirmPassword.text.toString()

        val anyPasswordTyped = current.isNotEmpty() || newPass.isNotEmpty() || confirm.isNotEmpty()
        if (!anyPasswordTyped) {
            clearPasswordErrors()
            return true
        }

        var valid = true
        val hasPassword = viewModel.profile.value?.hasPassword ?: true

        if (hasPassword && current.isEmpty()) {
            binding.tilCurrentPassword.error = "Current password is required."
            valid = false
        } else {
            binding.tilCurrentPassword.error = null
        }

        if (newPass.isNotEmpty() && newPass.length < 8) {
            binding.tilNewPassword.error = "Min. 8 characters"
            valid = false
        } else {
            binding.tilNewPassword.error = null
        }

        if (confirm.isNotEmpty() && newPass != confirm) {
            binding.tilConfirmPassword.error = "Passwords do not match."
            valid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return valid
    }

    private fun clearPasswordErrors() {
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun setupClickListeners() {
        binding.btnUploadPhoto.setOnClickListener { requestImagePermissionOrPick() }
        binding.ivAvatar.setOnClickListener { requestImagePermissionOrPick() }
        binding.btnSaveChanges.setOnClickListener { onSaveChanges() }
    }

    private fun onSaveChanges() {
        val name     = binding.etFullName.text.toString().trim()
        val current  = binding.etCurrentPassword.text.toString()
        val newPass  = binding.etNewPassword.text.toString()
        val confirm  = binding.etConfirmPassword.text.toString()

        if (name.isEmpty()) {
            binding.tilFullName.error = "Full name is required."
            return
        } else {
            binding.tilFullName.error = null
        }

        val anyPasswordTyped = current.isNotEmpty() || newPass.isNotEmpty() || confirm.isNotEmpty()
        if (anyPasswordTyped && !validatePasswordFields()) return

        viewModel.updateProfile(
            name            = name,
            currentPassword = if (anyPasswordTyped) current else null,
            newPassword     = if (anyPasswordTyped) newPass else null,
            confirmPassword = if (anyPasswordTyped) confirm else null
        )
    }

    private fun requestImagePermissionOrPick() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED -> openImagePicker()
            else -> permissionLauncher.launch(permission)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    private fun handleImageSelected(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Snackbar.make(binding.root, "Could not read image.", Snackbar.LENGTH_SHORT).show()
                return
            }

            // Preview immediately in profile hero
            binding.ivAvatar.setImageBitmap(bitmap)
            binding.tvAvatarInitials.visibility = View.GONE

            // Also preview in top bar avatar immediately
            // tvAvatar is a TextView — photo shown in ivAvatar hero only

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

            viewModel.uploadPhoto(bytes, mimeType)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Failed to load image.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {

        viewModel.profileLoading.observe(this) { loading ->
            binding.profileLoadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.profile.observe(this) { profile ->
            if (profile == null) return@observe

            if (!profile.profileImageUrl.isNullOrEmpty()) {
                loadAvatarFromUrl(profile.profileImageUrl)
            } else {
                showAvatarInitials(profile.name)
            }

            binding.tvProfileName.text  = profile.name
            binding.tvProfileEmail.text = profile.email
            binding.etFullName.setText(profile.name)
            binding.etEmail.setText(profile.email)
            binding.etBarangay.setText(profile.barangay ?: "")
            binding.tvMemberSince.text  = "Member since ${formatMemberSince(profile.createdAt)}"
            binding.tvReputationScore.text = (profile.reputationScore ?: 0).toString()
            binding.tvPostedCount.text     = (profile.favorsPosted    ?: 0).toString()
            binding.tvClaimedCount.text    = (profile.favorsClaimed   ?: 0).toString()
            binding.tvCompletedCount.text  = (profile.favorsCompleted ?: 0).toString()
            setStarRating(profile.reputationScore ?: 0)

            // FIX: also refresh top bar initials whenever name changes
            setupAvatar(profile.name)
        }

        viewModel.recentActivity.observe(this) { items ->
            recentActivityAdapter.submitList(items)
            binding.tvNoRecentActivity.visibility =
                if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.saveLoading.observe(this) { loading ->
            binding.btnSaveChanges.isEnabled = !loading
            binding.btnSaveChanges.text = if (loading) "Saving…" else "✓  Save Changes"
        }

        viewModel.saveSuccess.observe(this) { success ->
            if (success == true) {
                Snackbar.make(binding.root, "Profile updated!", Snackbar.LENGTH_SHORT).show()
                binding.etCurrentPassword.setText("")
                binding.etNewPassword.setText("")
                binding.etConfirmPassword.setText("")
                clearPasswordErrors()
                viewModel.clearSaveSuccess()
            }
        }

        viewModel.saveError.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.clearSaveError()
            }
        }

        viewModel.photoUploading.observe(this) { uploading ->
            binding.uploadProgressBar.visibility = if (uploading) View.VISIBLE else View.GONE
        }

        // FIX: observe upload success to update top bar avatar
        viewModel.photoUploadSuccess.observe(this) { imageUrl ->
            if (imageUrl == null) return@observe
            if (imageUrl.isNotEmpty()) {
                // Load photo into top bar avatar circle
                setupAvatar(prefs.getUser()?.name ?: "")
            } else {
                // Uploaded but no URL returned — refresh initials
                val name = prefs.getUser()?.name ?: ""
                setupAvatar(name)
            }
            Snackbar.make(binding.root, "Photo uploaded!", Snackbar.LENGTH_SHORT).show()
            viewModel.clearPhotoUploadSuccess()
        }

        viewModel.photoError.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.clearPhotoError()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Avatar helpers
    // ─────────────────────────────────────────────────────────────────────

    private fun showAvatarInitials(name: String) {
        binding.ivAvatar.setImageDrawable(null)
        binding.tvAvatarInitials.visibility = View.VISIBLE
        binding.tvAvatarInitials.text = initials(name)
        binding.tvAvatarInitials.backgroundTintList =
            ColorStateList.valueOf(avatarColor(name))
    }

    private fun loadAvatarFromUrl(url: String) {
        try {
            if (url.startsWith("data:")) {
                val base64Part = url.substringAfter("base64,")
                val bytes = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    binding.ivAvatar.setImageBitmap(bmp)
                    binding.tvAvatarInitials.visibility = View.GONE
                    // Also update top bar
                    // top bar is TextView — initials only
                    return
                }
            }
        } catch (_: Exception) { }
        showAvatarInitials(binding.tvProfileName.text.toString())
    }

    private fun initials(name: String): String =
        name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("").take(2).uppercase()

    private fun avatarColor(name: String): Int {
        val colors = listOf(
            0xFFC8601A.toInt(), 0xFF2E86AB.toInt(), 0xFFA23B72.toInt(), 0xFFF18F01.toInt(),
            0xFF44BBA4.toInt(), 0xFFE94F37.toInt(), 0xFF6B4226.toInt(), 0xFF3A86FF.toInt()
        )
        var hash = 0
        for (c in name) hash = c.code + ((hash shl 5) - hash)
        return colors[Math.abs(hash) % colors.size]
    }

    private fun setStarRating(score: Int) {
        val filled = when {
            score >= 100 -> 5
            score >= 60  -> 4
            score >= 30  -> 3
            score >= 10  -> 2
            else         -> 1
        }
        val stars = listOf(
            binding.star1, binding.star2, binding.star3, binding.star4, binding.star5
        )
        stars.forEachIndexed { idx, star ->
            star.setImageResource(
                if (idx < filled) R.drawable.ic_star_filled
                else R.drawable.ic_star_outline
            )
        }
    }

    private fun formatMemberSince(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "—"
        return try {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            val date   = parser.parse(dateStr) ?: return "—"
            java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US).format(date)
        } catch (e: Exception) { "—" }
    }
}