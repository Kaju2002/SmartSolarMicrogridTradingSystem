/*
 * File: LoginManager.kt
 * Module: Identity and Access (Vithusha)
 * Description: Login through the API, and the saved session (restore / logout) in SQLite
 */
package com.solargrid.mobile.auth

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.models.LoginRequest
import com.solargrid.mobile.auth.models.LoginResponse
import com.solargrid.mobile.auth.models.UserEntity
import com.solargrid.mobile.core.managers.ContextManager
import com.solargrid.mobile.core.managers.DatabaseManager
import com.solargrid.mobile.core.managers.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class LoginManager private constructor() {

    private val authService =
        NetworkManager.getInstance().createService(AuthService::class.java)
    private val gson = Gson()

    // Login with NIC/username + password. Password rules and account status are checked by the API.
    suspend fun login(identifier: String, password: String): Result<UserEntity> =
        withContext(Dispatchers.IO) {
            if (!NetworkManager.getInstance().isNetworkAvailable()) {
                return@withContext failure(R.string.login_error_offline)
            }

            val response = try {
                authService.login(LoginRequest(identifier, password))
            } catch (e: IOException) {
                return@withContext failure(R.string.login_error_server)
            } catch (e: JsonParseException) {
                return@withContext failure(R.string.login_error_server)
            }

            val body = response.body()
            if (response.isSuccessful && body != null && body.success && !body.token.isNullOrBlank()) {
                // Backoffice has the web portal, not the mobile app
                if (body.userType == USER_TYPE_BACKOFFICE) {
                    return@withContext failure(R.string.login_error_backoffice)
                }
                val user = saveSession(body, identifier)
                    ?: return@withContext failure(R.string.login_error_server)
                Result.success(user)
            } else {
                val apiMessage = readErrorMessage(response)
                if (apiMessage != null) {
                    Result.failure(Exception(apiMessage))
                } else {
                    failure(R.string.login_error_server)
                }
            }
        }

    // Saved user from SQLite if the JWT is still valid, otherwise null (show login)
    suspend fun restoreSession(): UserEntity? = withContext(Dispatchers.IO) {
        val userDao = DatabaseManager.getInstance().userDao()
        val user = userDao.getCurrentUser() ?: return@withContext null

        val age = System.currentTimeMillis() - user.loggedInAt
        if (age !in 0..SESSION_DURATION_MS) {
            userDao.clear()
            return@withContext null
        }

        NetworkManager.getInstance().setAuthToken(user.token)
        user
    }

    // Forget the user and their saved bookings (QR codes) on this phone
    suspend fun logout() = withContext(Dispatchers.IO) {
        DatabaseManager.getInstance().userDao().clear()
        DatabaseManager.getInstance().bookingDao().clear()
        NetworkManager.getInstance().clearAuthToken()
    }

    // Keep the logged-in user in SQLite (never the password) and use the token for API calls
    private suspend fun saveSession(body: LoginResponse, identifier: String): UserEntity? {
        val userId = body.userId ?: return null
        val token = body.token ?: return null
        val userType = body.userType ?: return null

        val user = UserEntity(
            userId = userId,
            fullName = body.fullName.orEmpty(),
            userType = userType,
            nic = identifier.takeIf { userType == USER_TYPE_PROSUMER },
            username = identifier.takeIf { userType == USER_TYPE_OPERATOR },
            token = token,
            loggedInAt = System.currentTimeMillis()
        )

        val userDao = DatabaseManager.getInstance().userDao()
        userDao.clear()
        userDao.insert(user)
        NetworkManager.getInstance().setAuthToken(token)
        return user
    }

    // 401 body has the same shape as LoginResponse, e.g. "Invalid credentials"
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
        const val USER_TYPE_PROSUMER = "Prosumer"
        const val USER_TYPE_OPERATOR = "GridOperator"
        const val USER_TYPE_BACKOFFICE = "Backoffice"

        // API JWT lasts 120 min; stop 5 min early so a call never goes out with a dead token
        private const val SESSION_DURATION_MS = 115 * 60 * 1000L

        @Volatile
        private var instance: LoginManager? = null

        // One shared LoginManager for the app
        fun getInstance(): LoginManager =
            instance ?: synchronized(this) {
                instance ?: LoginManager().also { instance = it }
            }
    }
}
