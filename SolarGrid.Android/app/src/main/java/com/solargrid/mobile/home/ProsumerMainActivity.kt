/*
 * File: ProsumerMainActivity.kt
 * Module: Team
 * Description: Prosumer shell after login. Bottom nav switches between the
 *              Home, Stations, Bookings and Profile tabs.
 */
package com.solargrid.mobile.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.LoginActivity
import com.solargrid.mobile.auth.LoginManager
import com.solargrid.mobile.auth.ProfileFragment
import com.solargrid.mobile.databinding.ActivityProsumerMainBinding
import com.solargrid.mobile.reservation.BookingsFragment
import com.solargrid.mobile.station.StationsFragment
import kotlinx.coroutines.launch

class ProsumerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProsumerMainBinding
    private var statusBarHeight = 0

    // Set up insets, tabs and back handling, then make sure someone is logged in
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProsumerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupBottomNav()
        setupBackHandling()

        // After rotation the fragment manager and the nav restore the open tab themselves
        if (savedInstanceState == null) {
            showTab(R.id.nav_home)
        }
        checkSession()
    }

    // Tabs start below the status bar; the nav pads itself for the gesture bar
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainer) { _, insets ->
            statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            updateTopPadding(binding.bottomNav.selectedItemId)
            insets
        }
    }

    // Home's hero goes behind the status bar and pads its own text; other tabs sit below it
    private fun updateTopPadding(itemId: Int) {
        val top = if (itemId == R.id.nav_home) 0 else statusBarHeight
        binding.fragmentContainer.updatePadding(top = top)
    }

    // Lets a tab (e.g. Home shortcuts) switch to another tab
    fun openTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    // Tapping a tab shows its fragment; tapping the open tab does nothing for now
    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId)
            true
        }
        binding.bottomNav.setOnItemReselectedListener { }
    }

    // Back from another tab goes Home first, Back on Home leaves the app
    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNav.selectedItemId != R.id.nav_home) {
                    binding.bottomNav.selectedItemId = R.id.nav_home
                } else {
                    finish()
                }
            }
        })
    }

    // Hide the other tabs and show this one, creating it the first time.
    // Hide/show (not replace) keeps each tab's scroll and loaded data.
    private fun showTab(itemId: Int) {
        val tag = tagFor(itemId)
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction().setReorderingAllowed(true)

        fragmentManager.fragments
            .filter { it.tag != tag }
            .forEach { transaction.hide(it) }

        val existing = fragmentManager.findFragmentByTag(tag)
        if (existing == null) {
            transaction.add(R.id.fragmentContainer, createFragment(itemId), tag)
        } else {
            transaction.show(existing)
        }
        transaction.commit()
        updateTopPadding(itemId)
    }

    // Fragment tag for each nav item
    private fun tagFor(itemId: Int): String = when (itemId) {
        R.id.nav_stations -> TAG_STATIONS
        R.id.nav_bookings -> TAG_BOOKINGS
        R.id.nav_profile -> TAG_PROFILE
        else -> TAG_HOME
    }

    // New fragment for each nav item
    private fun createFragment(itemId: Int): Fragment = when (itemId) {
        R.id.nav_stations -> StationsFragment()
        R.id.nav_bookings -> BookingsFragment()
        R.id.nav_profile -> ProfileFragment()
        else -> HomeFragment()
    }

    // Android may reopen this screen directly, so check the saved session here too
    private fun checkSession() {
        lifecycleScope.launch {
            if (LoginManager.getInstance().restoreSession() == null) {
                openLogin()
            }
        }
    }

    // Login becomes the only screen, so Back cannot return here
    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_STATIONS = "stations"
        private const val TAG_BOOKINGS = "bookings"
        private const val TAG_PROFILE = "profile"
    }
}
