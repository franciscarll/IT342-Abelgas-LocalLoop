package edu.cit.abelgas.localloop.features.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.shared.api.ApiClient
import edu.cit.abelgas.localloop.databinding.ActivityLoginBinding
import edu.cit.abelgas.localloop.features.auth.model.GoogleAuthRequest
import edu.cit.abelgas.localloop.features.auth.model.LoginRequest
import edu.cit.abelgas.localloop.features.dashboard.DashboardActivity
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import kotlinx.coroutines.launch
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: SharedPreferencesHelper
    private lateinit var googleSignInClient: GoogleSignInClient

    // ── Activity Result Launcher for Google Sign-In ───────────────────────
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleGoogleSignInResult(account)
        } catch (e: ApiException) {
            showGeneralError("Google Sign-In failed: ${e.statusCode}")
            setGoogleLoadingState(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = SharedPreferencesHelper(this)
        ApiClient.init(prefs)

        if (prefs.isLoggedIn()) {
            goToDashboard()
            return
        }

        setupGoogleSignIn()
        setupClickListeners()
    }

    // ── Google Sign-In Setup ──────────────────────────────────────────────
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.btnGoogle.setOnClickListener {
            startGoogleSignIn()           // ← was "coming soon", now wired
        }
    }

    // ── Google Sign-In Flow ───────────────────────────────────────────────
    private fun startGoogleSignIn() {
        hideGeneralError()
        setGoogleLoadingState(true)

        // Always sign out first so the account picker always shows
        // — prevents being stuck on a previously selected account
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun handleGoogleSignInResult(account: GoogleSignInAccount) {
        val idToken = account.idToken

        if (idToken == null) {
            showGeneralError("Failed to get Google token. Please try again.")
            setGoogleLoadingState(false)
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.service.googleSignIn(
                    GoogleAuthRequest(idToken = idToken)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        val authData = body.data
                        prefs.saveToken(authData.accessToken)
                        prefs.saveUser(authData.user)

                        // ── Route based on barangay ───────────────────────────
                        if (authData.user.barangay.isNullOrBlank() ||
                            authData.user.barangay == "Not set") {
                            // New Google user — needs to pick barangay
                            val intent = Intent(this@LoginActivity, SelectBarangayActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            // Returning user — go straight to dashboard
                            goToDashboard()
                        }
                    } else {
                        showGeneralError(
                            body?.error?.message ?: getString(R.string.error_generic)
                        )
                    }
                } else {
                    handleHttpError(response.code())
                }
            } catch (e: IOException) {
                showGeneralError(getString(R.string.error_no_internet))
            } catch (e: Exception) {
                showGeneralError(getString(R.string.error_generic))
            } finally {
                setGoogleLoadingState(false)
            }
        }
    }

    // ── Email/Password Login (unchanged) ──────────────────────────────────
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
                        val authData = body.data
                        prefs.saveToken(authData.accessToken)
                        prefs.saveUser(authData.user)
                        goToDashboard()
                    } else {
                        showGeneralError(
                            body?.error?.message ?: getString(R.string.error_generic)
                        )
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

    private fun setGoogleLoadingState(isLoading: Boolean) {
        binding.btnGoogle.isEnabled = !isLoading
        binding.btnGoogle.text = if (isLoading) "Signing in…" else "Continue with Google"
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