package com.solargrid.prosumer.data.api

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("identifier") val identifier: String,
    @SerializedName("password") val password: String,
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("userType") val userType: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("token") val token: String? = null,
)
