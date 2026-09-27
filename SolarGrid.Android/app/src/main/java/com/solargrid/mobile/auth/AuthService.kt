/*
 * File: AuthService.kt
 * Module: Identity and Access (Vithusha)
 * Description: Auth API calls (login, register)
 */
package com.solargrid.mobile.auth

import com.solargrid.mobile.auth.models.LoginRequest
import com.solargrid.mobile.auth.models.LoginResponse
import com.solargrid.mobile.auth.models.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    // POST login - Response<> so we can read the 401 message too
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // POST register 
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>
}
