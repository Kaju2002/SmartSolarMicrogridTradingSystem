/*
 * File: NetworkManager.kt
 * Description: Retrofit/OkHttp setup, JWT header and connectivity check
 */
package com.solargrid.mobile.core.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.solargrid.mobile.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkManager private constructor() {

    // JWT from the last successful login, kept in memory for every request
    @Volatile
    private var authToken: String? = null

    private val retrofit: Retrofit

    init {
        val authInterceptor = Interceptor { chain ->
            val token = authToken
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Build a Retrofit implementation of an API interface (e.g. AuthService)
    fun <T> createService(serviceClass: Class<T>): T = retrofit.create(serviceClass)

    // Save the JWT after login so later calls are authorised
    fun setAuthToken(token: String?) {
        authToken = token
    }

    // Forget the JWT on logout
    fun clearAuthToken() {
        authToken = null
    }

    // True when the device has a working internet connection
    fun isNetworkAvailable(): Boolean {
        val context = ContextManager.getInstance().getApplicationContext()
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        // Emulator alias for the PC running the API; physical phones need the PC LAN IP
        const val BASE_URL = "http://10.0.2.2:8081/"

        @Volatile
        private var instance: NetworkManager? = null

        // Single shared NetworkManager so every service reuses one HTTP client
        fun getInstance(): NetworkManager =
            instance ?: synchronized(this) {
                instance ?: NetworkManager().also { instance = it }
            }
    }
}
