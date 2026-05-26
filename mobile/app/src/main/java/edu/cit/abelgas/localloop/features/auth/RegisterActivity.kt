package edu.cit.abelgas.localloop.features.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
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
import edu.cit.abelgas.localloop.databinding.ActivityRegisterBinding
import edu.cit.abelgas.localloop.features.auth.model.GoogleAuthRequest
import edu.cit.abelgas.localloop.features.auth.model.RegisterRequest
import edu.cit.abelgas.localloop.features.dashboard.DashboardActivity
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import kotlinx.coroutines.launch
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var prefs: SharedPreferencesHelper
    private lateinit var googleSignInClient: GoogleSignInClient
    private var selectedBarangay: String = ""

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
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = SharedPreferencesHelper(this)
        setupGoogleSignIn()
        setupBarangayDropdown()
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

    private fun setupBarangayDropdown() {
        val barangays = resources.getStringArray(R.array.cebu_city_barangays)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, barangays)
        binding.actvBarangay.setAdapter(adapter)
        binding.actvBarangay.setOnItemClickListener { _, _, position, _ ->
            selectedBarangay = if (position == 0) "" else barangays[position]
            binding.tilBarangay.error = null
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateAccount.setOnClickListener { attemptRegister() }
        binding.tvLogIn.setOnClickListener { finish() }
        binding.btnGoogle.setOnClickListener {
            startGoogleSignIn()
        }
    }

    // ── Google Sign-In Flow ───────────────────────────────────────────────
    private fun startGoogleSignIn() {
        hideGeneralError()
        setGoogleLoadingState(true)

        // Always sign out first so the account picker always shows
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
                            val intent = Intent(this@RegisterActivity, SelectBarangayActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            // Returning Google user — go straight to dashboard
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

    // ── Email/Password Registration ───────────────────────────────────────
    private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true
        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tilBarangay.error = null
        hideGeneralError()

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_required)
            isValid = false
        } else if (name.length < 2) {
            binding.tilName.error = "Name must be at least 2 characters"
            isValid = false
        }

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

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.error_required)
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            isValid = false
        }

        if (selectedBarangay.isEmpty()) {
            binding.tilBarangay.error = getString(R.string.error_select_barangay)
            isValid = false
        }

        return isValid
    }

    private fun attemptRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (!validateInputs(name, email, password, confirmPassword)) return

        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.service.register(
                    RegisterRequest(
                        name = name,
                        email = email,
                        password = password,
                        barangay = selectedBarangay
                    )
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
            409 -> {
                binding.tilEmail.error = getString(R.string.error_duplicate_email)
                showGeneralError(getString(R.string.error_duplicate_email))
            }
            400 -> showGeneralError("Please check your inputs and try again.")
            500 -> showGeneralError("Server error. Please try again later.")
            else -> showGeneralError("Error $code. Please try again.")
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnCreateAccount.isEnabled = !isLoading
        binding.btnCreateAccount.text =
            if (isLoading) "Creating account…" else getString(R.string.btn_create_account)
        binding.etName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.actvBarangay.isEnabled = !isLoading
    }

    private fun setGoogleLoadingState(isLoading: Boolean) {
        binding.btnGoogle.isEnabled = !isLoading
        binding.btnGoogle.text = if (isLoading) "Signing in…" else "Continue with Google"
    }

    private fun showGeneralError(message: String) {
        binding.tvRegisterError.text = message
        binding.cardRegisterError.visibility = View.VISIBLE
    }

    private fun hideGeneralError() {
        binding.cardRegisterError.visibility = View.GONE
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}