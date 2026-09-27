/*
 * File: RegisterActivity.kt
 * Module: Identity and Access (Vithusha)
 * Description: Prosumer sign-up screen. Checks the form is filled and the passwords match,
 *              sends it through RegisterManager, and returns the NIC to login on success.
 */
package com.solargrid.mobile.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    // Show the form and hook up the buttons
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupListeners()
    }

    // Hero goes behind the status bar; keep the form above the nav bar and keyboard
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, 0, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }
    }

    // Buttons, keyboard Done key and typing
    private fun setupListeners() {
        binding.btnCreateAccount.setOnClickListener { attemptRegister() }

        binding.etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister()
                true
            } else {
                false
            }
        }

        listOf(
            binding.etFullName, binding.etNic, binding.etPhone,
            binding.etEmail, binding.etPassword, binding.etConfirmPassword
        ).forEach { field -> field.doAfterTextChanged { hideError() } }

        // Back to the login screen that opened this one
        binding.tvLogin.setOnClickListener { finish() }

        // Social buttons are design only; the API has no social sign-up
        val socialClick = View.OnClickListener {
            Toast.makeText(this, R.string.social_coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.btnGoogle.setOnClickListener(socialClick)
        binding.btnMicrosoft.setOnClickListener(socialClick)
        binding.btnApple.setOnClickListener(socialClick)
    }

    // Phone only checks "is it filled" and "do passwords match".
    // NIC, e-mail and password rules are checked by the API.
    private fun attemptRegister() {
        hideKeyboard()

        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val nic = binding.etNic.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        if (listOf(fullName, nic, phone, email, password, confirmPassword).any { it.isEmpty() }) {
            showError(getString(R.string.register_error_empty))
            return
        }
        if (password != confirmPassword) {
            showError(getString(R.string.register_error_password_mismatch))
            return
        }

        lifecycleScope.launch {
            setLoading(true)
            val result = RegisterManager.getInstance()
                .register(fullName, nic, phone, email, password)
            setLoading(false)

            result
                .onSuccess { showSuccessDialog(nic) }
                .onFailure { error ->
                    showError(error.message ?: getString(R.string.register_error_failed))
                }
        }
    }

    // Tell the user to wait for approval, then go back to login with the NIC filled in
    private fun showSuccessDialog(nic: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.register_success_title)
            .setMessage(R.string.register_success_message)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ ->
                // API saves the NIC in upper case, so send it back the same way
                setResult(RESULT_OK, Intent().putExtra(EXTRA_NIC, nic.uppercase()))
                finish()
            }
            .show()
    }

    // Spinner on, inputs locked while waiting for the API
    private fun setLoading(loading: Boolean) {
        binding.pbLoading.isVisible = loading
        binding.btnCreateAccount.isEnabled = !loading
        binding.btnCreateAccount.text = if (loading) "" else getString(R.string.create_account)
        listOf(
            binding.etFullName, binding.etNic, binding.etPhone,
            binding.etEmail, binding.etPassword, binding.etConfirmPassword,
            binding.tvLogin, binding.btnGoogle, binding.btnMicrosoft, binding.btnApple
        ).forEach { it.isEnabled = !loading }
    }

    // Red message above the Create Account button
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true
    }

    // Hide the red message once the user edits a field
    private fun hideError() {
        binding.tvError.isVisible = false
    }

    // Close the keyboard so the error and button are visible
    private fun hideKeyboard() {
        val view = currentFocus ?: return
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    companion object {
        // Result key LoginActivity reads to prefill the NIC
        const val EXTRA_NIC = "extra_nic"
    }
}
