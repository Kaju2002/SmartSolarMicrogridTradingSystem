/*
 * File: RegisterManager.kt
 * Module: Identity and Access (Vithusha)
 * Description: Prosumer sign-up through the API. The new account waits for Backoffice approval.
 */
package com.solargrid.mobile.auth

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.models.LoginResponse
import com.solargrid.mobile.auth.models.RegisterRequest
import com.solargrid.mobile.core.managers.ContextManager
import com.solargrid.mobile.core.managers.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class RegisterManager private constructor() {

    private val authService =
        NetworkManager.getInstance().createService(AuthService::class.java)
    private val gson = Gson()

    // Send the form to the API. NIC, e-mail and password rules are checked there.
    // Nothing is saved on the phone: the user signs in after approval.
    suspend fun register(
        fullName: String,
        nic: String,
        phoneNumber: String,
        email: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!NetworkManager.getInstance().isNetworkAvailable()) {
            return@withContext failure(R.string.login_error_offline)
        }

        val request = RegisterRequest(
            nic = nic,
            password = password,
            fullName = fullName,
            email = email,
            phoneNumber = phoneNumber
        )

        val response = try {
            authService.register(request)
        } catch (e: IOException) {
            return@withContext failure(R.string.login_error_server)
        } catch (e: JsonParseException) {
            return@withContext failure(R.string.login_error_server)
        }

        val body = response.body()
        if (response.isSuccessful && body != null && body.success) {
            Result.success(Unit)
        } else {
            val apiMessage = body?.message?.takeIf { it.isNotBlank() } ?: readErrorMessage(response)
            if (apiMessage != null) {
                Result.failure(Exception(apiMessage))
            } else {
                failure(R.string.register_error_failed)
            }
        }
    }

    // 409/400 body has the same shape as LoginResponse, e.g. "Username or NIC already exists"
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
        private var instance: RegisterManager? = null

        // One shared RegisterManager for the app
        fun getInstance(): RegisterManager =
            instance ?: synchronized(this) {
                instance ?: RegisterManager().also { instance = it }
            }
    }
}
