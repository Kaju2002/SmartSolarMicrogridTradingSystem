/*
 * File: StationSlots.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Body and reply of PUT api/stations/{id}/available-slots, used when a
 *              Grid Operator sets how many battery slots are free at their station.
 */
package com.solargrid.mobile.verification.operator.models

import com.google.gson.annotations.SerializedName

data class UpdateSlotsRequest(
    @SerializedName("availableSlots") val availableSlots: Int
)

// Matches StationResponseDto in SolarGrid.API (only the fields the app uses)
data class StationSlotsResult(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("availableSlots") val availableSlots: Int?,
    @SerializedName("batterySlots") val batterySlots: Int?
)
