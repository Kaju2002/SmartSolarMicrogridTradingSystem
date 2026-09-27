/*
 * File: SolarGridApplication.kt
 * Description: App start hook that shares the application context with managers
 */
package com.solargrid.mobile

import android.app.Application
import com.solargrid.mobile.managers.ContextManager

class SolarGridApplication : Application() {

    // Runs once before any Activity, so every manager can reach the context
    override fun onCreate() {
        super.onCreate()
        ContextManager.getInstance().setApplicationContext(applicationContext)
    }
}
