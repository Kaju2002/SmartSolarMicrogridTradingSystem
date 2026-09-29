/*
 * File: DashboardSummary.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Booking counts from GET api/dashboard/summary/{nic}.
 *              Matches DashboardSummaryDto in SolarGrid.API.
 */
package com.solargrid.mobile.verification.dashboard.models

import com.google.gson.annotations.SerializedName

data class DashboardSummary(
    @SerializedName("pendingCount") val pendingCount: Int,
    @SerializedName("approvedCount") val approvedCount: Int,
    @SerializedName("completedCount") val completedCount: Int
)
