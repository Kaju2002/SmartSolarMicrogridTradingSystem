/*
 * File: SessionStore.kt
 * Description: SharedPreferences JWT session storage
 */
package com.solargrid.prosumer.data

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        private set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
        }

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        private set(value) {
            prefs.edit().putString(KEY_USER_ID, value).apply()
        }

    var fullName: String?
        get() = prefs.getString(KEY_FULL_NAME, null)
        private set(value) {
            prefs.edit().putString(KEY_FULL_NAME, value).apply()
        }

    var userType: String?
        get() = prefs.getString(KEY_USER_TYPE, null)
        private set(value) {
            prefs.edit().putString(KEY_USER_TYPE, value).apply()
        }

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()

    fun saveSession(
        token: String,
        userId: String?,
        fullName: String?,
        userType: String?,
    ) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_USER_TYPE, userType)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "solar_grid_session"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "userId"
        private const val KEY_FULL_NAME = "fullName"
        private const val KEY_USER_TYPE = "userType"
    }
}
