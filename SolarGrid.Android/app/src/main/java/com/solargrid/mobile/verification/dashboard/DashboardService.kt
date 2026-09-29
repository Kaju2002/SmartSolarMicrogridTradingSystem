/*
 * File: DashboardService.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Prosumer dashboard API calls. The API only answers for the caller's own NIC.
 */
package com.solargrid.mobile.verification.dashboard

import com.solargrid.mobile.reservation.models.Reservation
import com.solargrid.mobile.verification.dashboard.models.DashboardSummary
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DashboardService {

    // GET pending, approved and completed counts
    @GET("api/dashboard/summary/{nic}")
    suspend fun getSummary(@Path("nic") nic: String): Response<DashboardSummary>

    // GET completed bookings, newest first
    @GET("api/dashboard/history/{nic}")
    suspend fun getHistory(@Path("nic") nic: String): Response<List<Reservation>>

    // GET bookings whose status matches the query; no query returns all of them
    @GET("api/dashboard/search/{nic}")
    suspend fun search(
        @Path("nic") nic: String,
        @Query("query") query: String?
    ): Response<List<Reservation>>
}
