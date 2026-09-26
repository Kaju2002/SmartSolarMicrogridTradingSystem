/*
 * File: StationsApi.kt
 * Description: Retrofit stations nearby endpoint
 */
package com.solargrid.prosumer.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface StationsApi {
    @GET("api/stations/nearby")
    suspend fun getNearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double = 15.0,
    ): Response<List<NearbyStation>>
}
