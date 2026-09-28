/*
 * File: SlotAvailability.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Hourly slots for one station and day from GET api/reservations/availability.
 *              Matches SlotAvailabilityDto in SolarGrid.API.
 */
package com.solargrid.mobile.reservation.models

import com.google.gson.annotations.SerializedName

data class SlotAvailability(
    @SerializedName("stationId") val stationId: String?,
    // "yyyy-MM-dd", Sri Lanka date
    @SerializedName("date") val date: String?,
    @SerializedName("openTime") val openTime: String?,
    @SerializedName("closeTime") val closeTime: String?,
    // Bookings allowed at the same hour
    @SerializedName("spotsPerSlot") val spotsPerSlot: Int?,
    // kWh shared by all bookings in the same hour
    @SerializedName("capacityKWh") val capacityKWh: Double?,
    // LKR per kWh, for the cost preview
    @SerializedName("ratePerKwh") val ratePerKwh: Double?,
    // Smallest request per booking
    @SerializedName("minKWh") val minKWh: Double?,
    @SerializedName("slots") val slots: List<BookingSlot>?
)

data class BookingSlot(
    // UTC start; send it back unchanged when booking
    @SerializedName("slotDateTime") val slotDateTime: String?,
    // Sri Lanka time, e.g. "09:00" and "10:00"
    @SerializedName("startTime") val startTime: String?,
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("spotsLeft") val spotsLeft: Int?,
    // Most a new booking can ask for in this hour
    @SerializedName("kWhLeft") val kWhLeft: Double?,
    // Available | Full | Unavailable
    @SerializedName("status") val status: String?
) {

    // Only slots the API marks Available can be booked
    fun isAvailable(): Boolean = status == STATUS_AVAILABLE && slotDateTime != null

    companion object {
        const val STATUS_AVAILABLE = "Available"
        const val STATUS_FULL = "Full"
        const val STATUS_UNAVAILABLE = "Unavailable"
    }
}
