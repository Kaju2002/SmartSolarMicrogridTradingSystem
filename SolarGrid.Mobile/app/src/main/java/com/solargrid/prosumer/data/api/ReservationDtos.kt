/*
 * File: ReservationDtos.kt
 * Description: Create reservation request/response models
 */
package com.solargrid.prosumer.data.api

import com.google.gson.annotations.SerializedName

data class CreateReservationRequest(
    @SerializedName("prosumerNic") val prosumerNic: String,
    @SerializedName("stationId") val stationId: String,
    /** ISO-8601 UTC, e.g. 2026-09-28T10:00:00Z */
    @SerializedName("reservationDateTime") val reservationDateTime: String,
)

data class ReservationResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = "",
    @SerializedName("reservationId") val reservationId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("reservationDateTime") val reservationDateTime: String? = null,
    @SerializedName("qrCode") val qrCode: String? = null,
)
