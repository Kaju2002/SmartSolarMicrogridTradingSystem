/*
 * File: StationDtos.kt
 * Description: Nearby station API response models
 */
package com.solargrid.prosumer.data.api

import com.google.gson.annotations.SerializedName

data class NearbyStation(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String = "",
    @SerializedName("stationId") val stationId: String? = null,
    @SerializedName("stationName") val stationName: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("distanceKm") val distanceKm: Double? = null,
    @SerializedName("ratePerKwh") val ratePerKwh: Double? = null,
)
