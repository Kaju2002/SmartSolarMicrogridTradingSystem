/*
 * File: PasswordResetService.kt
 * Module: Identity and Access (Vithusha)
 * Description: Forgot-password API calls (send code, verify code, set new password). No login needed.
 */
package com.solargrid.mobile.auth

import com.solargrid.mobile.auth.models.ForgotPasswordRequest
import com.solargrid.mobile.auth.models.PasswordResetResponse
import com.solargrid.mobile.auth.models.ResetPasswordRequest
import com.solargrid.mobile.auth.models.VerifyResetCodeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PasswordResetService {

    // POST e-mail a 4-digit code
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<PasswordResetResponse>

    // POST check the code; success carries the reset token
    @POST("api/auth/verify-reset-code")
    suspend fun verifyResetCode(@Body request: VerifyResetCodeRequest): Response<PasswordResetResponse>

    // POST save the new password with the reset token
    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<PasswordResetResponse>
}
