/*
 * File: OperatorService.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Grid Operator API calls. The API limits bookings, approve and scan
 *              to the operator's own stations.
 */
package com.solargrid.mobile.verification.operator

import com.solargrid.mobile.reservation.models.ReservationResult
import com.solargrid.mobile.verification.operator.models.OperatorBooking
import com.solargrid.mobile.verification.operator.models.OperatorStation
import com.solargrid.mobile.verification.operator.models.ScanQrRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface OperatorService {

    // GET bookings at the signed-in operator's stations
    @GET("api/reservations/operator")
    suspend fun getBookings(): Response<List<OperatorBooking>>

    // GET every station (staff list); the app keeps the operator's own
    @GET("api/stations")
    suspend fun getStations(): Response<List<OperatorStation>>

    // PUT approve a Pending booking; the reply carries the new QR code
    @PUT("api/reservations/{id}/approve")
    suspend fun approve(@Path("id") reservationId: String): Response<ReservationResult>

    // POST a scanned QR code to complete the energy transfer
    @POST("api/verification/scan-qr")
    suspend fun scanQr(@Body request: ScanQrRequest): Response<ReservationResult>
}
