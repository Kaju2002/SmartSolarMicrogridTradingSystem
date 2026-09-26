/*
 * File: ReservationsApi.kt
 * Description: Retrofit reservation create and list endpoints
 */
package com.solargrid.prosumer.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ReservationsApi {
    @POST("api/reservations")
    suspend fun create(
        @Body body: CreateReservationRequest,
    ): Response<ReservationResponse>

    @GET("api/reservations/prosumer/{nic}")
    suspend fun listByProsumer(
        @Path("nic") nic: String,
    ): Response<List<ReservationItem>>
}
