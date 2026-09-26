/*
 * File: ReservationsApi.kt
 * Description: Retrofit reservation create, list, update and cancel endpoints
 */
package com.solargrid.prosumer.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @PUT("api/reservations/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: UpdateReservationRequest,
    ): Response<ReservationResponse>

    @DELETE("api/reservations/{id}")
    suspend fun cancel(
        @Path("id") id: String,
    ): Response<ReservationResponse>
}
