/*
 * File: ReservationRequests.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Bodies for booking and changing a slot.
 *              Match CreateReservationDto and UpdateReservationDto in SolarGrid.API.
 */
package com.solargrid.mobile.reservation.models

import com.google.gson.annotations.SerializedName

// The API fills the NIC from the login token
data class CreateReservationRequest(
    @SerializedName("stationId") val stationId: String,
    // UTC slot start from the availability call
    @SerializedName("reservationDateTime") val reservationDateTime: String,
    @SerializedName("requestedKWh") val requestedKWh: Double
)

data class UpdateReservationRequest(
    @SerializedName("newReservationDateTime") val newReservationDateTime: String
)
