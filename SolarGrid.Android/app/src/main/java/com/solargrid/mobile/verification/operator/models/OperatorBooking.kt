/*
 * File: OperatorBooking.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: One booking from GET api/reservations/operator.
 *              Matches OperatorReservationDto in SolarGrid.API.
 */
package com.solargrid.mobile.verification.operator.models

import com.google.gson.annotations.SerializedName

data class OperatorBooking(
    @SerializedName("id") val id: String?,
    @SerializedName("prosumerNic") val prosumerNic: String?,
    @SerializedName("stationId") val stationId: String?,
    @SerializedName("stationName") val stationName: String?,
    // UTC, e.g. "2026-09-29T03:30:00Z"
    @SerializedName("reservationDateTime") val reservationDateTime: String?,
    @SerializedName("requestedKWh") val requestedKWh: Double?,
    // LKR
    @SerializedName("estimatedCost") val estimatedCost: Double?,
    // Pending | Approved | Completed | Cancelled
    @SerializedName("status") val status: String?,
    @SerializedName("qrCode") val qrCode: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("lastModifiedAt") val lastModifiedAt: String?
)
