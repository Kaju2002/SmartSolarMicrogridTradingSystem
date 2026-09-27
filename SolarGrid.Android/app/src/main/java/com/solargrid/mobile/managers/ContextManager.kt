/*
 * File: ContextManager.kt
 * Description: Singleton holder for the application context used by other managers
 */
package com.solargrid.mobile.managers

import android.content.Context

class ContextManager private constructor() {

    private var applicationContext: Context? = null

    // Store the application context (not an Activity, to avoid leaks)
    fun setApplicationContext(context: Context) {
        applicationContext = context.applicationContext
    }

    // Return the stored context or fail fast if the app was not initialised
    fun getApplicationContext(): Context =
        applicationContext ?: error("ContextManager not initialised. Check SolarGridApplication.")

    companion object {
        @Volatile
        private var instance: ContextManager? = null

        // Single shared ContextManager for the whole app
        fun getInstance(): ContextManager =
            instance ?: synchronized(this) {
                instance ?: ContextManager().also { instance = it }
            }
    }
}
