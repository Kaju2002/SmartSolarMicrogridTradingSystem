/*
 * File: ReservationService.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Reservation API calls used by the Prosumer app
 */
package com.solargrid.mobile.reservation

import com.solargrid.mobile.reservation.models.CreateReservationRequest
import com.solargrid.mobile.reservation.models.Reservation
import com.solargrid.mobile.reservation.models.ReservationResult
import com.solargrid.mobile.reservation.models.SlotAvailability
import com.solargrid.mobile.reservation.models.UpdateReservationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReservationService {

    // GET hourly slots for a station on one Sri Lanka date ("yyyy-MM-dd")
    @GET("api/reservations/availability")
    suspend fun getAvailability(
        @Query("stationId") stationId: String,
        @Query("date") date: String
    ): Response<SlotAvailability>

    // POST book a slot
    @POST("api/reservations")
    suspend fun create(@Body request: CreateReservationRequest): Response<ReservationResult>

    // PUT move a booking to another slot
    @PUT("api/reservations/{id}")
    suspend fun update(
        @Path("id") reservationId: String,
        @Body request: UpdateReservationRequest
    ): Response<ReservationResult>

    // DELETE cancel a booking
    @DELETE("api/reservations/{id}")
    suspend fun cancel(@Path("id") reservationId: String): Response<ReservationResult>

    // GET the signed-in prosumer's bookings
    @GET("api/reservations/prosumer/{nic}")
    suspend fun getByProsumer(@Path("nic") nic: String): Response<List<Reservation>>
}
