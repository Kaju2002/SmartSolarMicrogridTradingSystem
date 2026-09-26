/*
 * File: StationRepository.kt
 * Description: Nearby stations API calls
 */
package com.solargrid.prosumer.data

import com.solargrid.prosumer.data.api.NearbyStation
import com.solargrid.prosumer.data.api.StationsApi
import java.io.IOException

class StationRepository(
    private val api: StationsApi,
) {
    suspend fun getNearby(
        lat: Double,
        lng: Double,
        radiusKm: Double = 15.0,
    ): Result<List<NearbyStation>> {
        return try {
            val http = api.getNearby(lat, lng, radiusKm)
            if (!http.isSuccessful) {
                return Result.failure(
                    Exception(
                        when (http.code()) {
                            401 -> "Session expired. Sign in again."
                            403 -> "Not allowed to view stations."
                            else -> "Could not load stations (${http.code()})"
                        },
                    ),
                )
            }
            Result.success(http.body().orEmpty())
        } catch (_: IOException) {
            Result.failure(
                Exception("Network error. Is the API running? Emulator uses http://10.0.2.2:5204"),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
