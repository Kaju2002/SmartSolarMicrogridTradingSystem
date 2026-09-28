/*
 * File: UpdateProfileRequest.kt
 * Module: Identity and Access (Vithusha)
 * Description: Body for PUT api/auth/profile/{userId}. Matches UpdateProfileDto in SolarGrid.API.
 *              NIC cannot be changed, so it is not sent.
 */
package com.solargrid.mobile.auth.models

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phoneNumber") val phoneNumber: String
)
