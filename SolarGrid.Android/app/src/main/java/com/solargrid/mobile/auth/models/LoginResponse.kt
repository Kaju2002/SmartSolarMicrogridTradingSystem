/*
 * File: LoginResponse.kt
 * Module: Identity and Access (Vithusha)
 * Description: JSON returned by POST api/auth/login. Matches LoginResponseDto
 *              in SolarGrid.API. The API sends this same shape on 200 (success)
 *              and on 401 (wrong credentials or inactive account).
 */
package com.solargrid.mobile.auth.models

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    // "Prosumer", "GridOperator" or "Backoffice"; null when login fails
    @SerializedName("userType") val userType: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("fullName") val fullName: String?,
    // JWT sent as "Authorization: Bearer <token>" on later requests
    @SerializedName("token") val token: String?
)
