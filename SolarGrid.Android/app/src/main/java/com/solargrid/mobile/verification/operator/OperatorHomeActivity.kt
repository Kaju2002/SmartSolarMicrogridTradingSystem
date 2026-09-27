/*
 * File: OperatorHomeActivity.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Grid Operator home after login (placeholder until QR scan is built)
 */
package com.solargrid.mobile.verification.operator

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.LoginActivity
import com.solargrid.mobile.auth.LoginManager
import com.solargrid.mobile.databinding.ActivityOperatorHomeBinding
import kotlinx.coroutines.launch

class OperatorHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorHomeBinding

    // Show the screen, then load the saved user
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogout.setOnClickListener { logout() }
        loadUser()
    }

    // Android may reopen this screen directly, so read the session here too
    private fun loadUser() {
        lifecycleScope.launch {
            val user = LoginManager.getInstance().restoreSession()
            if (user == null) {
                openLogin()
                return@launch
            }
            binding.tvWelcome.text = getString(R.string.login_welcome, user.fullName)
        }
    }

    // Clear the session and go back to login
    private fun logout() {
        lifecycleScope.launch {
            LoginManager.getInstance().logout()
            openLogin()
        }
    }

    // Login becomes the only screen, so Back cannot return here
    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
