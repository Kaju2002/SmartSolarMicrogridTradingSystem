/*
 * File: ReservationResult.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Reply to book / change / cancel, also the body of 400 and 404 errors.
 *              Matches ReservationResponseDto in SolarGrid.API.
 */
package com.solargrid.mobile.reservation.models

import com.google.gson.annotations.SerializedName

data class ReservationResult(
    @SerializedName("success") val success: Boolean,
    // Rule message from the API, e.g. "This time slot is full"
    @SerializedName("message") val message: String?,
    @SerializedName("reservationId") val reservationId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("reservationDateTime") val reservationDateTime: String?,
    @SerializedName("requestedKWh") val requestedKWh: Double?,
    // LKR, worked out by the API
    @SerializedName("estimatedCost") val estimatedCost: Double?,
    @SerializedName("qrCode") val qrCode: String?
)
