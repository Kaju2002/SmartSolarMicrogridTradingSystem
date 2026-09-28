/*
 * File: Reservation.kt
 * Module: Reservation Management (Kajanthan)
 * Description: One booking from GET api/reservations/prosumer/{nic}.
 *              Matches EnergyReservation in SolarGrid.API.
 */
package com.solargrid.mobile.reservation.models

import com.google.gson.annotations.SerializedName

data class Reservation(
    @SerializedName("id") val id: String?,
    @SerializedName("stationId") val stationId: String?,
    // UTC, e.g. "2026-09-29T03:30:00Z"
    @SerializedName("reservationDateTime") val reservationDateTime: String?,
    // 0 on bookings made before kWh was added
    @SerializedName("requestedKWh") val requestedKWh: Double?,
    // LKR, worked out by the API when booked
    @SerializedName("estimatedCost") val estimatedCost: Double?,
    // Pending | Approved | Completed | Cancelled
    @SerializedName("status") val status: String?,
    // Set once a Grid Operator approves the booking
    @SerializedName("qrCode") val qrCode: String?,
    @SerializedName("createdAt") val createdAt: String?
) {

    // Pending and Approved bookings can still be changed or cancelled
    fun isLive(): Boolean = status == STATUS_PENDING || status == STATUS_APPROVED

    companion object {
        const val STATUS_PENDING = "Pending"
        const val STATUS_APPROVED = "Approved"
        const val STATUS_COMPLETED = "Completed"
        const val STATUS_CANCELLED = "Cancelled"
    }
}
