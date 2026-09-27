/*
 * File: RegisterRequest.kt
 * Module: Identity and Access (Vithusha)
 * Description: JSON body sent to POST api/auth/register.
 *              Matches RegisterRequestDto in SolarGrid.API.
 */
package com.solargrid.mobile.auth.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    // Mobile sign-up is Prosumer only; the API also forces this
    @SerializedName("userType") val userType: String = "Prosumer",
    @SerializedName("nic") val nic: String,
    // Only Grid Operators have a username, so null here
    @SerializedName("username") val username: String? = null,
    @SerializedName("password") val password: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phoneNumber") val phoneNumber: String
)
