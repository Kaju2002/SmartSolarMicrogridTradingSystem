/*
 * File: OnboardingManager.kt
 * Module: Onboarding (shared)
 * Description: Remembers whether the intro was already shown on this phone.
 */
package com.solargrid.mobile.onboarding

import android.content.Context
import androidx.core.content.edit
import com.solargrid.mobile.core.managers.ContextManager

class OnboardingManager private constructor() {

    // One true/false flag, so SharedPreferences instead of Room
    private val prefs = ContextManager.getInstance().getApplicationContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // True once the user finished or skipped the intro
    fun isDone(): Boolean = prefs.getBoolean(KEY_DONE, false)

    // Don't show the intro again (cleared only by uninstall / clear data)
    fun markDone() {
        prefs.edit { putBoolean(KEY_DONE, true) }
    }

    companion object {
        private const val PREFS_NAME = "solargrid_prefs"
        private const val KEY_DONE = "onboarding_done"

        @Volatile
        private var instance: OnboardingManager? = null

        // One shared OnboardingManager for the app
        fun getInstance(): OnboardingManager =
            instance ?: synchronized(this) {
                instance ?: OnboardingManager().also { instance = it }
            }
    }
}
