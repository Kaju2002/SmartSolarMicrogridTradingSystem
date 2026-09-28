/*
 * File: AuthService.kt
 * Module: Identity and Access (Vithusha)
 * Description: Auth API calls (login, register, profile, deactivation)
 */
package com.solargrid.mobile.auth

import com.solargrid.mobile.auth.models.LoginRequest
import com.solargrid.mobile.auth.models.LoginResponse
import com.solargrid.mobile.auth.models.RegisterRequest
import com.solargrid.mobile.auth.models.UpdateProfileRequest
import com.solargrid.mobile.auth.models.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthService {

    // POST login - Response<> so we can read the 401 message too
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // POST register 
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    // GET own profile (the API only allows your own id)
    @GET("api/auth/profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): Response<UserProfile>

    // PUT name, e-mail and phone - 400 body has the rule that failed
    @PUT("api/auth/profile/{userId}")
    suspend fun updateProfile(
        @Path("userId") userId: String,
        @Body request: UpdateProfileRequest
    ): Response<LoginResponse>

    // PUT deactivate own account; only Backoffice can turn it back on
    @PUT("api/auth/request-deactivation/{userId}")
    suspend fun requestDeactivation(@Path("userId") userId: String): Response<LoginResponse>
}
