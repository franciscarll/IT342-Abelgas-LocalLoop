package edu.cit.abelgas.localloop.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.api.ApiClient
import edu.cit.abelgas.localloop.databinding.ActivityLoginBinding
import edu.cit.abelgas.localloop.model.LoginRequest
import edu.cit.abelgas.localloop.util.SharedPreferencesHelper
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
                        prefs.saveAuthData(body.data.accessToken, body.data.user)
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
        binding.tvLoginError.visibility = View.VISIBLE
    }

    private fun hideGeneralError() {
        binding.tvLoginError.visibility = View.GONE
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}