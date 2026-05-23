package edu.cit.abelgas.localloop.features.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.shared.api.ApiClient
import edu.cit.abelgas.localloop.databinding.ActivityLoginBinding
import edu.cit.abelgas.localloop.features.auth.model.LoginRequest
import edu.cit.abelgas.localloop.features.dashboard.DashboardActivity
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import kotlinx.coroutines.launch
import java.io.IOException


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: SharedPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPreferencesHelper(this)

        // ── CRITICAL FIX: init ApiClient BEFORE isLoggedIn() check ───────────
        // Without this, the auth interceptor has no prefs reference and never
        // attaches the Bearer token to any request — causing 401 on everything.
        ApiClient.init(prefs)

        if (prefs.isLoggedIn()) {
            goToDashboard()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.btnGoogle.setOnClickListener {
            showGeneralError("Google login will be available soon.")
        }
        binding.tvForgotPassword.setOnClickListener {
            showGeneralError("Password reset coming soon.")
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        hideGeneralError()

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_required)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_required)
            isValid = false
        } else if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.error_password_length)
            isValid = false
        }

        return isValid
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (!validateInputs(email, password)) return

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.service.login(
                    LoginRequest(email = email, password = password)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {

                        // ── CRITICAL FIX: save token and user separately ──────
                        // body.data is AuthData, not UserDto.
                        // We save the token directly, and save the user fields
                        // that AuthData exposes. This way getToken() works
                        // immediately on the next request in DashboardActivity.
                        val authData = body.data

                        // Save token so interceptor can attach it immediately
                        prefs.saveToken(authData.accessToken)

                        // Save user — AuthData.user is already a UserDto
                        // (share your AuthData model if this still causes an error)
                        prefs.saveUser(authData.user)

                        goToDashboard()
                    } else {
                        showGeneralError(body?.error?.message ?: getString(R.string.error_generic))
                    }
                } else {
                    handleHttpError(response.code())
                }
            } catch (e: IOException) {
                showGeneralError(getString(R.string.error_no_internet))
            } catch (e: Exception) {
                showGeneralError(getString(R.string.error_generic))
            } finally {
                setLoadingState(false)
            }
        }
    }

    private fun handleHttpError(code: Int) {
        when (code) {
            401 -> showGeneralError(getString(R.string.error_invalid_credentials))
            400 -> showGeneralError("Please check your email and password.")
            500 -> showGeneralError("Server error. Please try again later.")
            else -> showGeneralError("Error $code. Please try again.")
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "Logging in…" else getString(R.string.btn_login)
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun showGeneralError(message: String) {
        binding.tvLoginError.text = message
        binding.cardLoginError.visibility = View.VISIBLE
    }

    private fun hideGeneralError() {
        binding.cardLoginError.visibility = View.GONE
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}