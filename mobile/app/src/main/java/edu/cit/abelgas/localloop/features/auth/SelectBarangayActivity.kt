package edu.cit.abelgas.localloop.features.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.databinding.ActivitySelectBarangayBinding
import edu.cit.abelgas.localloop.features.dashboard.DashboardActivity
import edu.cit.abelgas.localloop.shared.api.ApiClient
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import kotlinx.coroutines.launch
import java.io.IOException

class SelectBarangayActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectBarangayBinding
    private lateinit var prefs: SharedPreferencesHelper
    private var selectedBarangay: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectBarangayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPreferencesHelper(this)
        ApiClient.init(prefs)

        // Show personalized subtitle if we have the user's name
        val userName = prefs.getUser()?.name?.split(" ")?.firstOrNull()
        if (!userName.isNullOrBlank()) {
            binding.tvSubtitle.text = "Hi $userName! Please select your barangay to continue."
        }

        setupBarangayDropdown()
        setupClickListeners()
    }

    private fun setupBarangayDropdown() {
        val barangays = resources.getStringArray(R.array.cebu_city_barangays)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            barangays
        )
        binding.actvBarangay.setAdapter(adapter)
        binding.actvBarangay.setOnItemClickListener { _, _, position, _ ->
            selectedBarangay = if (position == 0) "" else barangays[position]
            binding.tilBarangay.error = null
            hideError()
        }
    }

    private fun setupClickListeners() {
        binding.btnContinue.setOnClickListener { attemptSaveBarangay() }
    }

    private fun attemptSaveBarangay() {
        if (selectedBarangay.isBlank()) {
            binding.tilBarangay.error = "Please select your barangay"
            return
        }

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.service.updateProfile(
                    edu.cit.abelgas.localloop.features.profile.model.ProfileUpdateRequest(
                        barangay = selectedBarangay
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        // ── Update saved user with new barangay ───────────────
                        val currentUser = prefs.getUser()
                        if (currentUser != null) {
                            val updatedUser = currentUser.copy(barangay = selectedBarangay)
                            prefs.saveUser(updatedUser)
                        }
                        goToDashboard()
                    } else {
                        showError(body?.error?.message ?: "Failed to save barangay. Please try again.")
                    }
                } else {
                    when (response.code()) {
                        401 -> showError("Session expired. Please log in again.")
                        500 -> showError("Server error. Please try again later.")
                        else -> showError("Error ${response.code()}. Please try again.")
                    }
                }
            } catch (e: IOException) {
                showError("No internet connection. Please try again.")
            } catch (e: Exception) {
                showError("Something went wrong. Please try again.")
            } finally {
                setLoadingState(false)
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnContinue.isEnabled = !isLoading
        binding.btnContinue.text = if (isLoading) "Saving…" else "Continue to Dashboard →"
        binding.actvBarangay.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.cardError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.cardError.visibility = View.GONE
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}