/*
 * File: LoginRequest.kt
 * Module: Identity and Access (Vithusha)
 * Description: JSON body sent to POST api/auth/login.
 *              Matches LoginRequestDto in SolarGrid.API.
 */
package com.solargrid.mobile.auth.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    // NIC for Prosumers, username for Grid Operators
    @SerializedName("identifier") val identifier: String,
    @SerializedName("password") val password: String
)
