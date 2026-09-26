/*
 * File: DashboardApi.kt
 * Description: Retrofit dashboard summary endpoint
 */
package com.solargrid.prosumer.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface DashboardApi {
    @GET("api/dashboard/summary/{prosumerNic}")
    suspend fun getSummary(
        @Path("prosumerNic") prosumerNic: String,
    ): Response<DashboardSummary>
}
