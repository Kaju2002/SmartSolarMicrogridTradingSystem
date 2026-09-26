/*
 * File: DashboardDtos.kt
 * Description: Dashboard summary response model
 */
package com.solargrid.prosumer.data.api

import com.google.gson.annotations.SerializedName

data class DashboardSummary(
    @SerializedName("pendingCount") val pendingCount: Int = 0,
    @SerializedName("approvedCount") val approvedCount: Int = 0,
    @SerializedName("completedCount") val completedCount: Int = 0,
)
