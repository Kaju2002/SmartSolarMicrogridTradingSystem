/*
 * File: EditProfileActivity.kt
 * Module: Identity and Access (Vithusha)
 * Description: Edit name, phone and e-mail. Checks the form is filled and something changed,
 *              sends it through ProfileManager, and returns the API's message to the Profile tab.
 */
package com.solargrid.mobile.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    private var originalName = ""
    private var originalEmail = ""
    private var originalPhone = ""

    // Show the form with the current values and hook up the buttons
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        originalName = intent.getStringExtra(EXTRA_FULL_NAME).orEmpty()
        originalEmail = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        originalPhone = intent.getStringExtra(EXTRA_PHONE).orEmpty()

        // After rotation the fields restore what the user typed
        if (savedInstanceState == null) {
            binding.etFullName.setText(originalName)
            binding.etPhone.setText(originalPhone)
            binding.etEmail.setText(originalEmail)
        }

        applyWindowInsets()
        setupListeners()
    }

    // Keep the top bar below the status bar and the form above the keyboard
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }
    }

    // Back, Save, keyboard Done key and typing
    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { attemptSave() }

        binding.etEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptSave()
                true
            } else {
                false
            }
        }

        listOf(binding.etFullName, binding.etPhone, binding.etEmail)
            .forEach { field -> field.doAfterTextChanged { hideError() } }
    }

    // Phone only checks "is it filled" and "did anything change".
    // Name length, phone and e-mail rules are checked by the API.
    private fun attemptSave() {
        hideKeyboard()

        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()

        if (listOf(fullName, phone, email).any { it.isEmpty() }) {
            showError(getString(R.string.profile_error_empty))
            return
        }
        if (fullName == originalName && phone == originalPhone && email == originalEmail) {
            finish()
            return
        }

        lifecycleScope.launch {
            setLoading(true)
            val result = ProfileManager.getInstance().updateProfile(fullName, email, phone)
            setLoading(false)

            result
                .onSuccess { message ->
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_MESSAGE, message))
                    finish()
                }
                .onFailure { error ->
                    showError(error.message ?: getString(R.string.profile_error_save))
                }
        }
    }

    // Spinner on, inputs locked while waiting for the API
    private fun setLoading(loading: Boolean) {
        binding.pbLoading.isVisible = loading
        binding.btnSave.isEnabled = !loading
        binding.btnSave.text = if (loading) "" else getString(R.string.profile_save)
        listOf(binding.etFullName, binding.etPhone, binding.etEmail, binding.btnBack)
            .forEach { it.isEnabled = !loading }
    }

    // Red message above the Save button
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
        private const val EXTRA_FULL_NAME = "extra_full_name"
        private const val EXTRA_EMAIL = "extra_email"
        private const val EXTRA_PHONE = "extra_phone"

        // Result key ProfileFragment reads for the snackbar
        const val EXTRA_MESSAGE = "extra_message"

        // Open the form filled with the current values
        fun newIntent(context: Context, fullName: String, email: String, phoneNumber: String): Intent =
            Intent(context, EditProfileActivity::class.java)
                .putExtra(EXTRA_FULL_NAME, fullName)
                .putExtra(EXTRA_EMAIL, email)
                .putExtra(EXTRA_PHONE, phoneNumber)
    }
}
