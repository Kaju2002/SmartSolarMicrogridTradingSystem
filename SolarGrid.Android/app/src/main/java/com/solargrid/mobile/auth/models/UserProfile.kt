/*
 * File: UserProfile.kt
 * Module: Identity and Access (Vithusha)
 * Description: JSON returned by GET api/auth/profile/{userId}.
 *              Matches the User model in SolarGrid.API (the password hash is sent empty).
 */
package com.solargrid.mobile.auth.models

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("id") val id: String?,
    // "Prosumer" or "GridOperator"
    @SerializedName("userType") val userType: String?,
    @SerializedName("nic") val nic: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phoneNumber") val phoneNumber: String?,
    // "Active", "Deactivated" or "PendingApproval"
    @SerializedName("status") val status: String?,
    // UTC, e.g. "2026-09-20T08:15:00Z"
    @SerializedName("createdAt") val createdAt: String?
)
