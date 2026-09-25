/*
 * File: AuthApi.kt
 * Description: Retrofit auth endpoints (login, register)
 */
package com.solargrid.prosumer.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<LoginResponse>
}
