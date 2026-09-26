/*
 * File: ReservationDtos.kt
 * Description: Reservation create/list API models
 */
package com.solargrid.prosumer.data.api

import com.google.gson.annotations.SerializedName

data class CreateReservationRequest(
    @SerializedName("prosumerNic") val prosumerNic: String,
    @SerializedName("stationId") val stationId: String,
    /** ISO-8601 UTC, e.g. 2026-09-28T10:00:00Z */
    @SerializedName("reservationDateTime") val reservationDateTime: String,
)

data class UpdateReservationRequest(
    /** ISO-8601 UTC */
    @SerializedName("newReservationDateTime") val newReservationDateTime: String,
)

data class ReservationResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("reservationId") val reservationId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("reservationDateTime") val reservationDateTime: String? = null,
    @SerializedName("qrCode") val qrCode: String? = null,
)

/** GET /api/reservations/prosumer/{nic} item (EnergyReservation). */
data class ReservationItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("prosumerNic") val prosumerNic: String? = null,
    @SerializedName("stationId") val stationId: String? = null,
    @SerializedName("bookingSlotId") val bookingSlotId: String? = null,
    @SerializedName("reservationDateTime") val reservationDateTime: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("qrCode") val qrCode: String? = null,
    @SerializedName("approvedBy") val approvedBy: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)
