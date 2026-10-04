/*
 * File: ForgotPasswordActivity.kt
 * Module: Identity and Access
 * Description: Forgot password screen in three steps: e-mail, 4-digit code, new password.
 *              UI only for now. The API has no reset endpoint, so the last step tells the
 *              user it is not live yet instead of pretending the password changed.
 */
package com.solargrid.mobile.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Patterns
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    private enum class Step { EMAIL, CODE, RESET }

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var codeBoxes: List<EditText>
    private var step = Step.EMAIL
    private var resendTimer: CountDownTimer? = null

    // Inflate, wire the steps and start on the e-mail step
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        codeBoxes = listOf(binding.etCode1, binding.etCode2, binding.etCode3, binding.etCode4)
        applyWindowInsets()
        setupCodeBoxes()
        setupListeners()
        showStep(Step.EMAIL)
    }

    // Hero runs behind the status bar; the form stays above the keyboard
    private fun applyWindowInsets() {
        val backTop = resources.getDimensionPixelSize(R.dimen.space_sm)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, 0, bars.right, maxOf(bars.bottom, ime.bottom))
            binding.btnBack.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = backTop + bars.top
            }
            insets
        }
    }

    // Button, keyboard Done, back arrow and system Back
    private fun setupListeners() {
        binding.btnPrimary.setOnClickListener { onPrimaryClick() }
        binding.btnBack.setOnClickListener { goBack() }
        binding.tvResend.setOnClickListener { resendCode() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBack()
        })

        listOf(binding.etEmail, codeBoxes.last(), binding.etConfirmPassword).forEach { field ->
            field.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onPrimaryClick()
                    true
                } else {
                    false
                }
            }
        }

        listOf(binding.etEmail, binding.etNewPassword, binding.etConfirmPassword)
            .forEach { it.doAfterTextChanged { hideError() } }
    }

    // A digit jumps to the next box; delete on an empty box jumps back and clears it
    private fun setupCodeBoxes() {
        codeBoxes.forEachIndexed { index, box ->
            box.contentDescription = getString(R.string.forgot_code_digit, index + 1)
            box.doAfterTextChanged { text ->
                hideError()
                if (text?.length == 1 && index < codeBoxes.lastIndex) codeBoxes[index + 1].requestFocus()
            }
            box.setOnKeyListener { _, keyCode, event ->
                val backOnEmpty = keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN && box.text.isEmpty() && index > 0
                if (backOnEmpty) {
                    codeBoxes[index - 1].apply { text.clear(); requestFocus() }
                }
                backOnEmpty
            }
        }
    }

    // Show only the current step and set its subtitle and button text
    private fun showStep(newStep: Step) {
        step = newStep
        hideError()
        binding.layoutStepEmail.isVisible = step == Step.EMAIL
        binding.layoutStepCode.isVisible = step == Step.CODE
        binding.layoutStepReset.isVisible = step == Step.RESET
        binding.tvResend.isVisible = step == Step.CODE

        when (step) {
            Step.EMAIL -> {
                binding.tvSubtitle.setText(R.string.forgot_email_subtitle)
                binding.btnPrimary.setText(R.string.forgot_send_email)
                resendTimer?.cancel()
            }
            Step.CODE -> {
                binding.tvSubtitle.text = getString(R.string.forgot_code_subtitle, maskEmail(enteredEmail()))
                binding.btnPrimary.setText(R.string.forgot_verify)
                codeBoxes.forEach { it.text.clear() }
                codeBoxes.first().requestFocus()
                startResendTimer()
            }
            Step.RESET -> {
                binding.tvSubtitle.setText(R.string.forgot_reset_subtitle)
                binding.btnPrimary.setText(R.string.forgot_save_password)
                resendTimer?.cancel()
                binding.etNewPassword.requestFocus()
            }
        }
    }

    // Check the current step, then move on
    private fun onPrimaryClick() {
        when (step) {
            Step.EMAIL -> {
                if (!Patterns.EMAIL_ADDRESS.matcher(enteredEmail()).matches()) {
                    showError(getString(R.string.forgot_error_email))
                } else {
                    showStep(Step.CODE)
                }
            }
            Step.CODE -> {
                if (codeBoxes.any { it.text.length != 1 }) {
                    showError(getString(R.string.forgot_error_code))
                } else {
                    showStep(Step.RESET)
                }
            }
            Step.RESET -> savePassword()
        }
    }

    // Same rules as sign-up: both filled, long enough, matching
    private fun savePassword() {
        val password = binding.etNewPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()
        when {
            password.isEmpty() || confirm.isEmpty() ->
                showError(getString(R.string.forgot_error_password_empty))
            password.length < MIN_PASSWORD_LENGTH ->
                showError(getString(R.string.forgot_error_password_short, MIN_PASSWORD_LENGTH))
            password != confirm ->
                showError(getString(R.string.register_error_password_mismatch))
            else -> {
                hideKeyboard()
                Toast.makeText(this, R.string.forgot_not_live, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    // 30 second wait before the code can be asked for again
    private fun startResendTimer() {
        resendTimer?.cancel()
        binding.tvResend.isEnabled = false
        binding.tvResend.setTextColor(getColor(R.color.text_hint))
        resendTimer = object : CountDownTimer(RESEND_WAIT_MS, 1_000L) {
            override fun onTick(millisLeft: Long) {
                val seconds = ((millisLeft + 999) / 1_000).toInt()
                binding.tvResend.text = getString(R.string.forgot_resend_in, seconds)
            }

            override fun onFinish() {
                binding.tvResend.isEnabled = true
                binding.tvResend.setText(R.string.forgot_resend)
                binding.tvResend.setTextColor(getColor(R.color.text_primary))
            }
        }.start()
    }

    private fun resendCode() {
        codeBoxes.forEach { it.text.clear() }
        codeBoxes.first().requestFocus()
        startResendTimer()
    }

    // Back walks to the previous step, and leaves from the first one
    private fun goBack() {
        when (step) {
            Step.EMAIL -> finish()
            Step.CODE -> showStep(Step.EMAIL)
            Step.RESET -> showStep(Step.CODE)
        }
    }

    private fun enteredEmail(): String = binding.etEmail.text?.toString()?.trim().orEmpty()

    // "kajanthan@gmail.com" -> "ka***@gmail.com"
    private fun maskEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 0) return email
        return email.take(minOf(2, at)) + "***" + email.substring(at)
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true
    }

    private fun hideError() {
        binding.tvError.isVisible = false
    }

    private fun hideKeyboard() {
        val view = currentFocus ?: return
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Stop the countdown so it does not touch a closed screen
    override fun onDestroy() {
        resendTimer?.cancel()
        super.onDestroy()
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        const val RESEND_WAIT_MS = 30_000L
    }
}
