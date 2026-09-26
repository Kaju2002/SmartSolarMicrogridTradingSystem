/*
 * File: ReservationsApi.kt
 * Description: Retrofit reservation create endpoint
 */
package com.solargrid.prosumer.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReservationsApi {
    @POST("api/reservations")
    suspend fun create(
        @Body body: CreateReservationRequest,
    ): Response<ReservationResponse>
}
