/*
 * File: OperatorStation.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: One station from GET api/stations (staff list).
 *              Matches SolarStationInfo in SolarGrid.API.
 */
package com.solargrid.mobile.verification.operator.models

import com.google.gson.annotations.SerializedName
import com.solargrid.mobile.station.models.Station

data class OperatorStation(
    @SerializedName("id") val id: String?,
    @SerializedName("stationName") val stationName: String?,
    @SerializedName("capacityKWh") val capacityKWh: Double?,
    // LKR per kWh
    @SerializedName("ratePerKwh") val ratePerKwh: Double?,
    @SerializedName("batterySlots") val batterySlots: Int?,
    @SerializedName("availableSlots") val availableSlots: Int?,
    // "HH:mm", e.g. "06:00" and "18:00"
    @SerializedName("openTime") val openTime: String?,
    @SerializedName("closeTime") val closeTime: String?,
    // "Active" or "Deactivated"
    @SerializedName("status") val status: String?,
    // Users id of the Grid Operator in charge
    @SerializedName("assignedOperatorId") val assignedOperatorId: String?
) {

    fun isActive(): Boolean = status == STATUS_ACTIVE

    // Same station as the prosumer model, so the shared station formatting works (no map position)
    fun toStation() = Station(
        stationId = id,
        stationName = stationName,
        latitude = null,
        longitude = null,
        distanceKm = null,
        ratePerKwh = ratePerKwh,
        capacityKWh = capacityKWh,
        batterySlots = batterySlots,
        availableSlots = availableSlots,
        openTime = openTime,
        closeTime = closeTime,
        status = status
    )

    companion object {
        const val STATUS_ACTIVE = "Active"
    }
}
