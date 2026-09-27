/*
 * File: OnboardingActivity.kt
 * Module: Onboarding (shared)
 * Description: First-launch intro. Three pages (stations, reservations, QR),
 *              then the login screen.
 */
package com.solargrid.mobile.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.widget.ViewPager2
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.LoginActivity
import com.solargrid.mobile.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter

    private val pages = listOf(
        OnboardingPage(R.drawable.img_onboarding_1, R.string.onboarding_title_1, R.string.onboarding_body_1),
        OnboardingPage(R.drawable.img_onboarding_2, R.string.onboarding_title_2, R.string.onboarding_body_2),
        OnboardingPage(R.drawable.img_onboarding_3, R.string.onboarding_title_3, R.string.onboarding_body_3)
    )

    // Set up the pager, Skip link and back handling
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = OnboardingAdapter(pages) { position -> onNextClick(position) }
        binding.vpOnboarding.adapter = adapter

        applyWindowInsets()
        setupListeners()
    }

    // Photos go behind the system bars; Skip and the card stay clear of them
    private fun applyWindowInsets() {
        val skipTopMargin = resources.getDimensionPixelSize(R.dimen.space_sm)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.tvSkip.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = skipTopMargin + bars.top
            }
            adapter.setBottomInset(bars.bottom)
            insets
        }
    }

    // Skip, page changes and the back button
    private fun setupListeners() {
        binding.tvSkip.setOnClickListener { finishOnboarding() }

        // No Skip on the last page, the arrow already finishes
        binding.vpOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tvSkip.isInvisible = position == pages.lastIndex
            }
        })

        // Back goes to the previous page first, then leaves the app
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = binding.vpOnboarding.currentItem
                if (current > 0) {
                    binding.vpOnboarding.currentItem = current - 1
                } else {
                    finish()
                }
            }
        })
    }

    // Arrow: next page, or login on the last page
    private fun onNextClick(position: Int) {
        if (position < pages.lastIndex) {
            binding.vpOnboarding.currentItem = position + 1
        } else {
            finishOnboarding()
        }
    }

    // Remember it was seen, then go to the login screen
    private fun finishOnboarding() {
        OnboardingManager.getInstance().markDone()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
