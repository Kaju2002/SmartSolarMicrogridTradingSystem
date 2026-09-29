/*
 * File: LoginActivity.kt
 * Module: Identity and Access (Vithusha)
 * Description: Sign-in screen. Checks the fields are filled and passes them to LoginManager.
 */
package com.solargrid.mobile.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.models.UserEntity
import com.solargrid.mobile.databinding.ActivityLoginBinding
import com.solargrid.mobile.home.ProsumerMainActivity
import com.solargrid.mobile.onboarding.OnboardingActivity
import com.solargrid.mobile.onboarding.OnboardingManager
import com.solargrid.mobile.verification.operator.OperatorMainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // Register screen sends back the new NIC so the user only types the password later
    private val registerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val nic = result.data?.getStringExtra(RegisterActivity.EXTRA_NIC)
            if (result.resultCode == RESULT_OK && !nic.isNullOrBlank()) {
                binding.etIdentifier.setText(nic)
                binding.etPassword.requestFocus()
            }
        }

    // Inflates the layout and wires up insets and click listeners
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // First launch: show the intro before anything else
        if (!OnboardingManager.getInstance().isDone()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupListeners()
        checkSavedSession()
    }

    // Already logged in on this phone? Then skip the form.
    // Form stays hidden while checking so it does not flash on screen.
    private fun checkSavedSession() {
        binding.root.isInvisible = true
        lifecycleScope.launch {
            val user = LoginManager.getInstance().restoreSession()
            if (user != null) {
                openHome(user)
            } else {
                binding.root.isInvisible = false
            }
        }
    }

    // Send each role to its own home screen
    private fun openHome(user: UserEntity) {
        val target = when (user.userType) {
            LoginManager.USER_TYPE_PROSUMER -> ProsumerMainActivity::class.java
            LoginManager.USER_TYPE_OPERATOR -> OperatorMainActivity::class.java
            else -> {
                lifecycleScope.launch { LoginManager.getInstance().logout() }
                binding.root.isInvisible = false
                showError(getString(R.string.login_error_role))
                return
            }
        }
        startActivity(Intent(this, target))
        // Close login so Back from home does not come back here
        finish()
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
            registerLauncher.launch(Intent(this, RegisterActivity::class.java))
        }

        // Social buttons are design only; the API has no social login
        val socialClick = View.OnClickListener {
            Toast.makeText(this, R.string.social_coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.btnGoogle.setOnClickListener(socialClick)
        binding.btnMicrosoft.setOnClickListener(socialClick)
        binding.btnApple.setOnClickListener(socialClick)
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

        lifecycleScope.launch {
            setLoading(true)
            val result = LoginManager.getInstance().login(identifier, password)
            setLoading(false)

            result
                .onSuccess { user -> openHome(user) }
                .onFailure { error ->
                    showError(error.message ?: getString(R.string.login_error_server))
                }
        }
    }

    // Spinner on, inputs locked while waiting for the API
    private fun setLoading(loading: Boolean) {
        binding.pbLoading.isVisible = loading
        binding.btnSignIn.isEnabled = !loading
        binding.btnSignIn.text = if (loading) "" else getString(R.string.sign_in)
        binding.etIdentifier.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.tvRegister.isEnabled = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.btnMicrosoft.isEnabled = !loading
        binding.btnApple.isEnabled = !loading
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
