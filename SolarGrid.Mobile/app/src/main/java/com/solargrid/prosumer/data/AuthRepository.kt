package com.solargrid.prosumer.data

import com.solargrid.prosumer.data.api.AuthApi
import com.solargrid.prosumer.data.api.LoginRequest
import com.solargrid.prosumer.data.api.LoginResponse
import java.io.IOException

class AuthRepository(
    private val api: AuthApi,
    private val sessionStore: SessionStore,
) {
    suspend fun login(identifier: String, password: String): Result<LoginResponse> {
        return try {
            val http = api.login(LoginRequest(identifier.trim(), password))
            val body = http.body()

            if (!http.isSuccessful) {
                val msg = body?.message?.takeIf { it.isNotBlank() }
                    ?: when (http.code()) {
                        401 -> "Invalid credentials"
                        else -> "Server error (${http.code()})"
                    }
                return Result.failure(Exception(msg))
            }

            if (body == null || !body.success || body.token.isNullOrBlank()) {
                return Result.failure(
                    Exception(body?.message?.ifBlank { null } ?: "Login failed"),
                )
            }

            val role = body.userType.orEmpty()
            if (!role.equals("Prosumer", ignoreCase = true)) {
                return Result.failure(
                    Exception("This app is for Prosumer accounts. Signed in as $role."),
                )
            }

            sessionStore.saveSession(
                token = body.token,
                userId = body.userId,
                fullName = body.fullName,
                userType = body.userType,
            )
            Result.success(body)
        } catch (_: IOException) {
            Result.failure(
                Exception("Network error. Is the API running? Emulator uses http://10.0.2.2:5204"),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        sessionStore.clear()
    }
}
