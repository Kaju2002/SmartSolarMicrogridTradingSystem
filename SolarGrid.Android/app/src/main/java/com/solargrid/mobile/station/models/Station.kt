/*
 * File: Station.kt
 * Module: Station Management (Gabilan)
 * Description: Solar station from GET api/stations/nearby and api/stations/{id}.
 *              Matches StationResponseDto in SolarGrid.API.
 */
package com.solargrid.mobile.station.models

import com.google.gson.annotations.SerializedName

data class Station(
    @SerializedName("stationId") val stationId: String?,
    @SerializedName("stationName") val stationName: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    // Only filled by the nearby search
    @SerializedName("distanceKm") val distanceKm: Double?,
    // LKR per kWh
    @SerializedName("ratePerKwh") val ratePerKwh: Double?,
    @SerializedName("capacityKWh") val capacityKWh: Double?,
    @SerializedName("batterySlots") val batterySlots: Int?,
    @SerializedName("availableSlots") val availableSlots: Int?,
    // "HH:mm", e.g. "06:00" and "18:00"
    @SerializedName("openTime") val openTime: String?,
    @SerializedName("closeTime") val closeTime: String?,
    // "Active" or "Deactivated"
    @SerializedName("status") val status: String?
) {

    // True when the given time (minutes after midnight) is inside the opening hours
    fun isOpenAt(minuteOfDay: Int): Boolean {
        val open = toMinutes(openTime) ?: return false
        val close = toMinutes(closeTime) ?: return false
        return minuteOfDay in open until close
    }

    // "06:30" -> 390, bad text -> null
    private fun toMinutes(time: String?): Int? {
        val parts = time?.split(":") ?: return null
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return hours * 60 + minutes
    }
}
