/*
 * File: RetrofitClient.kt
 * Description: OkHttp/Retrofit client with JWT interceptor
 */
package com.solargrid.prosumer.data.api

import com.solargrid.prosumer.data.SessionStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    /**
     * Emulator → host machine localhost.
     * Physical device → use your PC LAN IP, e.g. http://192.168.1.10:5204/
     */
    const val BASE_URL = "http://10.0.2.2:5204/"

    fun createAuthApi(sessionStore: SessionStore): AuthApi =
        createRetrofit(sessionStore).create(AuthApi::class.java)

    fun createDashboardApi(sessionStore: SessionStore): DashboardApi =
        createRetrofit(sessionStore).create(DashboardApi::class.java)

    fun createStationsApi(sessionStore: SessionStore): StationsApi =
        createRetrofit(sessionStore).create(StationsApi::class.java)

    private fun createRetrofit(sessionStore: SessionStore): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val token = sessionStore.token
            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
