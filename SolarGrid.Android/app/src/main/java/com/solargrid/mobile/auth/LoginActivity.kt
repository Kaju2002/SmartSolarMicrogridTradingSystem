/*
 * File: LoginActivity.kt
 * Module: Identity and Access (Vithusha)
 * Description: Sign-in screen. Shows activity_login.xml, checks the fields are
 *              filled and (from Step 6) hands the credentials to LoginManager.
 */
package com.solargrid.mobile.auth

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // Inflates the layout and wires up insets and click listeners
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupListeners()
    }

    // Hero image stays behind the status bar; bottom padding keeps the form
    // above the navigation bar and the keyboard
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, 0, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }
    }

    // Connects button taps, the keyboard Done key and typing to their actions
    private fun setupListeners() {
        binding.btnSignIn.setOnClickListener { attemptLogin() }

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }

        binding.etIdentifier.doAfterTextChanged { hideError() }
        binding.etPassword.doAfterTextChanged { hideError() }

        binding.tvRegister.setOnClickListener {
            Toast.makeText(this, R.string.register_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }

    // Reads the fields, rejects empty input, otherwise continues to login.
    // Only an "is it empty" check lives here; real validation is done by the API.
    private fun attemptLogin() {
        hideKeyboard()

        val identifier = binding.etIdentifier.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (identifier.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.login_error_empty))
            return
        }

        // Step 6 replaces this with LoginManager.login(identifier, password)
        Toast.makeText(this, R.string.login_ready_placeholder, Toast.LENGTH_SHORT).show()
    }

    // Shows a red message above the Sign In button
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true
    }

    // Hides the red message, e.g. once the user starts typing again
    private fun hideError() {
        binding.tvError.isVisible = false
    }

    // Closes the soft keyboard so the error text and button are not covered
    private fun hideKeyboard() {
        val view = currentFocus ?: return
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }
}
