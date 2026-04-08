package edu.cit.abelgas.localloop.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.api.ApiClient
import edu.cit.abelgas.localloop.databinding.ActivityRegisterBinding
import edu.cit.abelgas.localloop.model.RegisterRequest
import edu.cit.abelgas.localloop.util.SharedPreferencesHelper
import kotlinx.coroutines.launch
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var prefs: SharedPreferencesHelper
    private var selectedBarangay: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = SharedPreferencesHelper(this)
        setupBarangayDropdown()
        setupClickListeners()
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
            showGeneralError("Google sign-up will be available soon.")
        }
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true
        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tilBarangay.error = null
        hideGeneralError()

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_required); isValid = false
        } else if (name.length < 2) {
            binding.tilName.error = "Name must be at least 2 characters"; isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_required); isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email); isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_required); isValid = false
        } else if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.error_password_length); isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.error_required); isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch); isValid = false
        }

        if (selectedBarangay.isEmpty()) {
            binding.tilBarangay.error = getString(R.string.error_select_barangay); isValid = false
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
                        RegisterRequest(name = name, email = email, password = password, barangay = selectedBarangay)
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
        binding.btnCreateAccount.text = if (isLoading) "Creating account…" else getString(R.string.btn_create_account)
        binding.etName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.actvBarangay.isEnabled = !isLoading
    }

    private fun showGeneralError(message: String) {
        binding.tvRegisterError.text = message
        binding.tvRegisterError.visibility = View.VISIBLE
    }

    private fun hideGeneralError() {
        binding.tvRegisterError.visibility = View.GONE
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}