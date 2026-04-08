package edu.cit.abelgas.localloop.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.abelgas.localloop.api.ApiClient
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.databinding.ActivityDashboardBinding
import edu.cit.abelgas.localloop.util.SharedPreferencesHelper
import kotlinx.coroutines.launch
import java.io.IOException

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var prefs: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = SharedPreferencesHelper(this)

        if (!prefs.isLoggedIn()) {
            goToLogin()
            return
        }

        displayUserInfo()
        setupLogout()
    }

    private fun displayUserInfo() {
        val name = prefs.getUserName()
        val barangay = prefs.getUserBarangay()
        val reputation = prefs.getUserReputation()
        val role = prefs.getUserRole().removePrefix("ROLE_")

        binding.tvWelcomeMessage.text = getGreeting(name)
        binding.tvBarangayInfo.text = getString(R.string.barangay_display, barangay)
        binding.tvReputationScore.text = reputation.toString()
        binding.tvUserRole.text = role
    }

    private fun getGreeting(name: String): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        val firstName = name.split(" ").firstOrNull() ?: name
        return "$timeGreeting, $firstName! 👋"
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle("Log out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log out") { _, _ -> performLogout() }
                    .setNegativeButton("Cancel", null)
                    .show()
        }
    }

    private fun performLogout() {
        val bearerToken = prefs.getBearerToken()

        if (bearerToken != null) {
            lifecycleScope.launch {
                try {
                    ApiClient.service.logout(bearerToken)
                } catch (e: IOException) {
                    // Ignored — we log out locally regardless of network
                } catch (e: Exception) {
                    // Ignored — we log out locally regardless
                }

                prefs.clearAll()
                goToLogin()
            }
        } else {
            prefs.clearAll()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // We intentionally do nothing extra here —
        // the user must use the Logout button to exit.
    }
}