/*
 * File: ProfileManager.kt
 * Module: Identity and Access (Vithusha)
 * Description: Logged-in user's profile through the API: view, edit name/e-mail/phone,
 *              and deactivate the account. Field rules are checked by the API.
 */
package com.solargrid.mobile.auth

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.models.LoginResponse
import com.solargrid.mobile.auth.models.UpdateProfileRequest
import com.solargrid.mobile.auth.models.UserEntity
import com.solargrid.mobile.auth.models.UserProfile
import com.solargrid.mobile.core.managers.ContextManager
import com.solargrid.mobile.core.managers.DatabaseManager
import com.solargrid.mobile.core.managers.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class ProfileManager private constructor() {

    private val authService =
        NetworkManager.getInstance().createService(AuthService::class.java)
    private val gson = Gson()

    // Latest details from the API. A name changed on the web is copied into SQLite too.
    suspend fun getProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        val user = LoginManager.getInstance().restoreSession()
            ?: return@withContext failure(R.string.profile_error_session)
        if (!NetworkManager.getInstance().isNetworkAvailable()) {
            return@withContext failure(R.string.login_error_offline)
        }

        val response = try {
            authService.getProfile(user.userId)
        } catch (e: IOException) {
            return@withContext failure(R.string.login_error_server)
        } catch (e: JsonParseException) {
            return@withContext failure(R.string.login_error_server)
        }

        val profile = response.body()
        if (response.isSuccessful && profile != null) {
            saveName(user, profile.fullName)
            Result.success(profile)
        } else {
            failure(R.string.profile_error_load)
        }
    }

    // Send the edited fields; returns the API message on success
    suspend fun updateProfile(
        fullName: String,
        email: String,
        phoneNumber: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val user = LoginManager.getInstance().restoreSession()
            ?: return@withContext failure(R.string.profile_error_session)
        if (!NetworkManager.getInstance().isNetworkAvailable()) {
            return@withContext failure(R.string.login_error_offline)
        }

        val request = UpdateProfileRequest(fullName, email, phoneNumber)
        val response = try {
            authService.updateProfile(user.userId, request)
        } catch (e: IOException) {
            return@withContext failure(R.string.login_error_server)
        } catch (e: JsonParseException) {
            return@withContext failure(R.string.login_error_server)
        }

        val body = response.body()
        if (response.isSuccessful && body != null && body.success) {
            saveName(user, body.fullName ?: fullName)
            Result.success(body.message.orEmpty())
        } else {
            apiFailure(response, R.string.profile_error_save)
        }
    }

    // Deactivate on the server, then clear this phone like a logout
    suspend fun deactivateAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val user = LoginManager.getInstance().restoreSession()
            ?: return@withContext failure(R.string.profile_error_session)
        if (!NetworkManager.getInstance().isNetworkAvailable()) {
            return@withContext failure(R.string.login_error_offline)
        }

        val response = try {
            authService.requestDeactivation(user.userId)
        } catch (e: IOException) {
            return@withContext failure(R.string.login_error_server)
        } catch (e: JsonParseException) {
            return@withContext failure(R.string.login_error_server)
        }

        val body = response.body()
        if (response.isSuccessful && body != null && body.success) {
            LoginManager.getInstance().logout()
            Result.success(Unit)
        } else {
            apiFailure(response, R.string.profile_error_deactivate)
        }
    }

    // Home greeting and avatar read the name from SQLite
    private suspend fun saveName(user: UserEntity, fullName: String?) {
        val name = fullName?.trim().orEmpty()
        if (name.isNotEmpty() && name != user.fullName) {
            DatabaseManager.getInstance().userDao().updateFullName(user.userId, name)
        }
    }

    // Use the API's message (e.g. "Phone number must be 9 to 15 digits") when there is one
    private fun <T> apiFailure(response: Response<LoginResponse>, fallbackRes: Int): Result<T> {
        val apiMessage = response.body()?.message?.takeIf { it.isNotBlank() }
            ?: readErrorMessage(response)
        return if (apiMessage != null) Result.failure(Exception(apiMessage)) else failure(fallbackRes)
    }

    // 400/404 body has the same shape as LoginResponse
    private fun readErrorMessage(response: Response<LoginResponse>): String? {
        val json = response.errorBody()?.string() ?: return null
        return try {
            gson.fromJson(json, LoginResponse::class.java)?.message?.takeIf { it.isNotBlank() }
        } catch (e: JsonParseException) {
            null
        }
    }

    // Wrap a string resource as a failed Result
    private fun <T> failure(messageRes: Int): Result<T> {
        val message = ContextManager.getInstance().getApplicationContext().getString(messageRes)
        return Result.failure(Exception(message))
    }

    companion object {
        @Volatile
        private var instance: ProfileManager? = null

        // One shared ProfileManager for the app
        fun getInstance(): ProfileManager =
            instance ?: synchronized(this) {
                instance ?: ProfileManager().also { instance = it }
            }
    }
}
