/*
 * File: StationService.kt
 * Module: Station Management (Gabilan)
 * Description: Station API calls used by the Prosumer app
 */
package com.solargrid.mobile.station

import com.solargrid.mobile.station.models.Station
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StationService {

    // GET active stations within radiusKm of a point, nearest first
    @GET("api/stations/nearby")
    suspend fun getNearby(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radiusKm") radiusKm: Double
    ): Response<List<Station>>

    // GET one station (also deactivated ones, for old bookings)
    @GET("api/stations/{id}")
    suspend fun getById(@Path("id") stationId: String): Response<Station>
}
