/*
 * File: PasswordResetModels.kt
 * Module: Identity and Access (Vithusha)
 * Description: Bodies for the three forgot-password calls. Match PasswordResetDtos in SolarGrid.API.
 */
package com.solargrid.mobile.auth.models

import com.google.gson.annotations.SerializedName

data class ForgotPasswordRequest(
    @SerializedName("email") val email: String
)

data class VerifyResetCodeRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("resetToken") val resetToken: String,
    @SerializedName("newPassword") val newPassword: String
)

// Same shape on success and on errors (400, 503)
data class PasswordResetResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    // Only from verify-reset-code; needed to set the new password
    @SerializedName("resetToken") val resetToken: String?
)
