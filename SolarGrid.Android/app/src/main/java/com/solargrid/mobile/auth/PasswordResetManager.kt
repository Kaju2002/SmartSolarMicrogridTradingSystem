/*
 * File: PasswordResetManager.kt
 * Module: Identity and Access (Vithusha)
 * Description: Forgot password through the API: e-mail a code, verify it, set the new password.
 *              The reset token only lives in memory on the forgot password screen.
 */
package com.solargrid.mobile.auth

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.models.ForgotPasswordRequest
import com.solargrid.mobile.auth.models.PasswordResetResponse
import com.solargrid.mobile.auth.models.ResetPasswordRequest
import com.solargrid.mobile.auth.models.VerifyResetCodeRequest
import com.solargrid.mobile.core.managers.ContextManager
import com.solargrid.mobile.core.managers.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class PasswordResetManager private constructor() {

    private val service =
        NetworkManager.getInstance().createService(PasswordResetService::class.java)
    private val gson = Gson()

    // Ask the API to e-mail a code. It answers the same whether or not the e-mail has an account.
    suspend fun sendCode(email: String): Result<Unit> =
        call { service.forgotPassword(ForgotPasswordRequest(email)) }.map { }

    // Check the code; success gives the reset token for the last step
    suspend fun verifyCode(email: String, code: String): Result<String> =
        call { service.verifyResetCode(VerifyResetCodeRequest(email, code)) }.mapCatching { body ->
            body.resetToken?.takeIf { it.isNotBlank() } ?: throw Exception(text(R.string.login_error_server))
        }

    // Save the new password; the API's success message is shown to the user
    suspend fun resetPassword(email: String, resetToken: String, newPassword: String): Result<String> =
        call { service.resetPassword(ResetPasswordRequest(email, resetToken, newPassword)) }.map { body ->
            body.message?.takeIf { it.isNotBlank() } ?: text(R.string.forgot_password_saved)
        }

    // Shared network handling: offline check, server errors, and the API's own message on 400/503
    private suspend fun call(
        request: suspend () -> Response<PasswordResetResponse>
    ): Result<PasswordResetResponse> = withContext(Dispatchers.IO) {
        if (!NetworkManager.getInstance().isNetworkAvailable()) {
            return@withContext Result.failure(Exception(text(R.string.login_error_offline)))
        }

        val response = try {
            request()
        } catch (e: IOException) {
            return@withContext Result.failure(Exception(text(R.string.login_error_server)))
        } catch (e: JsonParseException) {
            return@withContext Result.failure(Exception(text(R.string.login_error_server)))
        }

        val body = response.body()
        if (response.isSuccessful && body != null && body.success) {
            Result.success(body)
        } else {
            val message = body?.message?.takeIf { it.isNotBlank() }
                ?: readErrorMessage(response)
                ?: text(R.string.login_error_server)
            Result.failure(Exception(message))
        }
    }

    private fun readErrorMessage(response: Response<PasswordResetResponse>): String? {
        val json = response.errorBody()?.string() ?: return null
        return try {
            gson.fromJson(json, PasswordResetResponse::class.java)?.message?.takeIf { it.isNotBlank() }
        } catch (e: JsonParseException) {
            null
        }
    }

    private fun text(messageRes: Int): String =
        ContextManager.getInstance().getApplicationContext().getString(messageRes)

    companion object {
        @Volatile
        private var instance: PasswordResetManager? = null

        // One shared PasswordResetManager for the app
        fun getInstance(): PasswordResetManager =
            instance ?: synchronized(this) {
                instance ?: PasswordResetManager().also { instance = it }
            }
    }
}
